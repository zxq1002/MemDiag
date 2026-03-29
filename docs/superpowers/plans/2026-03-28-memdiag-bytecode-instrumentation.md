# MemDiag Bytecode Instrumentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement core bytecode instrumentation for tracking heap array allocations, direct memory allocations, and method execution times using ASM.

**Architecture:** Use a "Bootstrap Bridge" class (`MemDiagSpy`) to allow JDK core classes to call into the Agent. Use ASM to inject recording calls into target classes. Heap allocations are sampled, while direct memory allocations are tracked 100%.

**Tech Stack:** Java, ASM 9.5, JVM Instrumentation API.

---

### Task 1: Implement MemDiagSpy Bridge Class

**Files:**
- Create: `memdiag-agent/src/main/java/com/memdiag/agent/instrument/MemDiagSpy.java`

- [ ] **Step 1: Create MemDiagSpy class**
Create the bridge class that will be injected into the bootstrap classloader.

```java
package com.memdiag.agent.instrument;

public class MemDiagSpy {
    private static final ThreadLocal<Boolean> IN_PROGRESS = ThreadLocal.withInitial(() -> false);
    private static volatile AllocationTransformer allocationTransformer;
    private static volatile MethodMonitorTransformer methodMonitorTransformer;

    public static void init(AllocationTransformer allocTransformer, MethodMonitorTransformer methodTransformer) {
        allocationTransformer = allocTransformer;
        methodMonitorTransformer = methodTransformer;
    }

    public static void recordAllocation(long size, String type) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (allocationTransformer != null) {
                allocationTransformer.recordAllocation(size, type);
            }
        } finally {
            IN_PROGRESS.set(false);
        }
    }

    public static void recordMethodEntry(String className, String methodName, String descriptor) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (methodMonitorTransformer != null) {
                methodMonitorTransformer.recordMethodEntry(className, methodName, descriptor);
            }
        } finally {
            IN_PROGRESS.set(false);
        }
    }

    public static void recordMethodExit(String className, String methodName, String descriptor, long durationNanos) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (methodMonitorTransformer != null) {
                methodMonitorTransformer.recordMethodExit(className, methodName, descriptor, durationNanos);
            }
        } finally {
            IN_PROGRESS.set(false);
        }
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add memdiag-agent/src/main/java/com/memdiag/agent/instrument/MemDiagSpy.java
git commit -m "feat: add MemDiagSpy bridge class"
```

---

### Task 2: Inject Bridge into Bootstrap ClassLoader

**Files:**
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/MemDiagAgent.java`

- [ ] **Step 1: Locate Agent JAR and append to bootstrap search path**
Modify the `initialize` method to find the current JAR and inject it.

```java
// Inside MemDiagAgent.java, add import
import java.io.File;
import java.net.URL;
import java.util.jar.JarFile;

// In initializeOptionalComponents method
    private static void initializeOptionalComponents(AgentConfig config, AgentContext context) {
        // ... existing data collector init ...

        // NEW: Inject into bootstrap
        try {
            URL agentJarUrl = MemDiagAgent.class.getProtectionDomain().getCodeSource().getLocation();
            if (agentJarUrl != null) {
                File jarFile = new File(agentJarUrl.toURI());
                if (jarFile.exists()) {
                    System.out.println("[MemDiag] Appending agent JAR to bootstrap search path: " + jarFile.getAbsolutePath());
                    context.getInstrumentation().appendToBootstrapClassLoaderSearch(new JarFile(jarFile));
                }
            }
        } catch (Exception e) {
            System.err.println("[MemDiag] Failed to append to bootstrap search path: " + e.getMessage());
        }

        // ... existing instrumentation manager init ...
        // After instrumentManager.initialize()
        if (context.getInstrumentManager() != null) {
             MemDiagSpy.init(
                 context.getInstrumentManager().getAllocationTransformer(),
                 context.getInstrumentManager().getMethodMonitorTransformer()
             );
        }
    }
```

- [ ] **Step 2: Commit**
```bash
git add memdiag-agent/src/main/java/com/memdiag/agent/MemDiagAgent.java
git commit -m "feat: inject MemDiagSpy into bootstrap classloader"
```

---

### Task 3: Implement Array Allocation Instrumentation

**Files:**
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/instrument/AllocationTransformer.java`

- [ ] **Step 1: Implement ASM ClassVisitor for array allocations**
Modify `transform` method and add internal `ClassVisitor`.

```java
// Add imports
import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

// Inside AllocationTransformer
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!shouldTransform(className)) {
            return null;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, AsmUtils.getClassWriterFlags(cr));
            AllocationClassVisitor cv = new AllocationClassVisitor(cw, className);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        } catch (Exception e) {
            System.err.println("[MemDiag] Error transforming class " + className + ": " + e.getMessage());
            return null;
        }
    }

    private class AllocationClassVisitor extends ClassVisitor {
        private final String className;
        public AllocationClassVisitor(ClassVisitor cv, String className) {
            super(AsmUtils.getAsmApiVersion(Opcodes.V1_8), cv);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new AllocationMethodVisitor(mv);
        }
    }

    private class AllocationMethodVisitor extends MethodVisitor {
        public AllocationMethodVisitor(MethodVisitor mv) {
            super(AsmUtils.getAsmApiVersion(Opcodes.V1_8), mv);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            super.visitIntInsn(opcode, operand);
            if (opcode == NEWARRAY) {
                // Stack: [array_ref]
                recordArrayAlloc();
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            super.visitTypeInsn(opcode, type);
            if (opcode == ANEWARRAY) {
                // Stack: [array_ref]
                recordArrayAlloc();
            }
        }

        private void recordArrayAlloc() {
            mv.visitInsn(DUP);
            mv.visitInsn(ARRAYLENGTH);
            mv.visitInsn(I2L);
            mv.visitLdcInsn("array"); // Simplified type
            mv.visitMethodInsn(INVOKESTATIC, "com/memdiag/agent/instrument/MemDiagSpy", "recordAllocation", "(JLjava/lang/String;)V", false);
        }
    }
```

- [ ] **Step 2: Update `shouldTransform`**
Allow instrumentation for target classes (for arrays, maybe all except exclusions).

```java
    private boolean shouldTransform(String className) {
        if (className == null) return false;
        // Exclude system/agent classes
        if (className.startsWith("java/") || className.startsWith("sun/") || className.startsWith("com/memdiag/")) {
            return false;
        }
        return true;
    }
```

- [ ] **Step 3: Commit**
```bash
git add memdiag-agent/src/main/java/com/memdiag/agent/instrument/AllocationTransformer.java
git commit -m "feat: implement array allocation instrumentation"
```

---

### Task 4: Implement Direct Memory Instrumentation

**Files:**
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/instrument/AllocationTransformer.java`

- [ ] **Step 1: Intercept `ByteBuffer.allocateDirect`**
Add logic to `AllocationMethodVisitor` to intercept calls to `allocateDirect`.

```java
// Inside AllocationMethodVisitor
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if (opcode == INVOKESTATIC && owner.equals("java/nio/ByteBuffer") && name.equals("allocateDirect")) {
                // Stack: [ByteBuffer]
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/nio/ByteBuffer", "capacity", "()I", false);
                mv.visitInsn(I2L);
                mv.visitLdcInsn("java.nio.DirectByteBuffer");
                mv.visitMethodInsn(INVOKESTATIC, "com/memdiag/agent/instrument/MemDiagSpy", "recordAllocation", "(JLjava/lang/String;)V", false);
            }
        }
```

- [ ] **Step 2: Commit**
```bash
git add memdiag-agent/src/main/java/com/memdiag/agent/instrument/AllocationTransformer.java
git commit -m "feat: implement direct memory allocation instrumentation"
```

---

### Task 5: Implement Method Monitoring

**Files:**
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/instrument/MethodMonitorTransformer.java`

- [ ] **Step 1: Implement Entry/Exit instrumentation**
Use ASM to inject `nanoTime` and `recordMethodExit`.

```java
// Add imports and implementation in MethodMonitorTransformer
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!shouldTransform(className)) return null;

        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ClassVisitor(ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, exc);
                return new AdviceAdapter(ASM9, mv, access, name, desc) {
                    private int startTimeId;

                    @Override
                    protected void onMethodEnter() {
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                        startTimeId = newLocal(Type.LONG_TYPE);
                        mv.visitVarInsn(LSTORE, startTimeId);
                        
                        mv.visitLdcInsn(className);
                        mv.visitLdcInsn(name);
                        mv.visitLdcInsn(desc);
                        mv.visitMethodInsn(INVOKESTATIC, "com/memdiag/agent/instrument/MemDiagSpy", "recordMethodEntry", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
                    }

                    @Override
                    protected void onMethodExit(int opcode) {
                        mv.visitLdcInsn(className);
                        mv.visitLdcInsn(name);
                        mv.visitLdcInsn(desc);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                        mv.visitVarInsn(LLOAD, startTimeId);
                        mv.visitInsn(LSUB);
                        mv.visitMethodInsn(INVOKESTATIC, "com/memdiag/agent/instrument/MemDiagSpy", "recordMethodExit", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V", false);
                    }
                };
            }
        }, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
```

- [ ] **Step 2: Commit**
```bash
git add memdiag-agent/src/main/java/com/memdiag/agent/instrument/MethodMonitorTransformer.java
git commit -m "feat: implement method monitoring instrumentation"
```

---

### Task 6: End-to-End Validation

- [ ] **Step 1: Compile the project**
Run: `mvn clean package -DskipTests`

- [ ] **Step 2: Run MemDiagDemo with Agent**
Run the demo with the agent attached and verify that it records allocations.

Run: `java -javaagent:memdiag-agent/target/memdiag-agent-1.0.0-SNAPSHOT.jar -Dmode=heap-leak -Dlimit=50 MemDiagDemo`

- [ ] **Step 3: Verify logs/output**
Check stdout for `[MemDiag] recordAllocation` calls (add temporary print in `AllocationTransformer.recordAllocation` if needed to confirm).
