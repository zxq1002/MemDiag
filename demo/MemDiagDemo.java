import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;
import java.lang.reflect.Field;

public class MemDiagDemo {
    private static final List<Object> HEAP_HOLDER = new ArrayList<>();
    private static final List<ByteBuffer> DIRECT_HOLDER = new ArrayList<>();
    private static Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String mode = System.getProperty("mode", "heap-leak").toLowerCase();
        int rateMb = Integer.getInteger("rate", 10); // 每秒增长量
        int limitMb = Integer.getInteger("limit", 500); // 停止增长的上限

        long pid = ProcessHandle.current().pid();
        System.out.println("==========================================");
        System.out.println("MemDiag Simulator Started");
        System.out.println("PID: " + pid);
        System.out.println("Mode: " + mode.toUpperCase());
        System.out.println("Rate: " + rateMb + " MB/s");
        System.out.println("Limit: " + limitMb + " MB");
        System.out.println("==========================================");

        switch (mode) {
            case "heap-leak":
                simulateHeapLeak(rateMb, limitMb);
                break;
            case "heap-high":
                simulateHeapHigh(limitMb);
                break;
            case "native-leak":
                simulateNativeLeak(rateMb, limitMb);
                break;
            case "native-high":
                simulateNativeHigh(limitMb);
                break;
            default:
                System.err.println("Unknown mode: " + mode);
                System.exit(1);
        }

        // 保持进程存活供分析
        while (true) {
            TimeUnit.SECONDS.sleep(10);
            System.out.println("Checking in... Process is alive. Current Holder Size: " 
                + (mode.contains("heap") ? HEAP_HOLDER.size() : DIRECT_HOLDER.size()));
        }
    }

    private static void simulateHeapLeak(int rateMb, int limitMb) throws InterruptedException {
        System.out.println("Simulating Heap Leak...");
        for (int i = 0; i < limitMb / rateMb; i++) {
            HEAP_HOLDER.add(new byte[rateMb * 1024 * 1024]);
            System.out.printf("Heap usage increased by %d MB. Total: %d MB%n", rateMb, (i + 1) * rateMb);
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("Reached limit. Holding memory.");
    }

    private static void simulateHeapHigh(int limitMb) {
        System.out.println("Simulating High Heap Usage...");
        HEAP_HOLDER.add(new byte[limitMb * 1024 * 1024]);
        System.out.println("Allocated " + limitMb + " MB on heap. Holding.");
    }

    private static void simulateNativeLeak(int rateMb, int limitMb) throws InterruptedException {
        System.out.println("Simulating Native (Unsafe) Leak...");
        for (int i = 0; i < limitMb / rateMb; i++) {
            long address = unsafe.allocateMemory(rateMb * 1024 * 1024);
            // 写入数据确保 RSS 增长
            for (int j = 0; j < 100; j++) {
                unsafe.putByte(address + j, (byte) 1);
            }
            System.out.printf("Native usage increased by %d MB. Total: %d MB%n", rateMb, (i + 1) * rateMb);
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("Reached limit. Holding native memory.");
    }

    private static void simulateNativeHigh(int limitMb) {
        System.out.println("Simulating High Native Usage (DirectByteBuffer)...");
        DIRECT_HOLDER.add(ByteBuffer.allocateDirect(limitMb * 1024 * 1024));
        System.out.println("Allocated " + limitMb + " MB direct memory. Holding.");
    }
}
