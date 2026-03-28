#include "agent.h"
#include "class_transformer.h"
#include "allocation_tracker.h"
#include <cstring>
#include <cstdlib>

// Global state
static GlobalState* g_state = nullptr;

// Global JNI references for the bridge
static jclass g_bridge_class = nullptr;
static jmethodID g_on_native_alloc_method = nullptr;

// Forward declarations
static jint initialize_agent(JavaVM* vm, char* options, bool is_attach, size_t sampling_rate);
static void JNICALL OnClassFileLoadHook(
    jvmtiEnv* jvmti, JNIEnv* jni,
    jclass class_being_redefined,
    jobject loader, const char* name,
    jobject protection_domain,
    jint class_data_len,
    const unsigned char* class_data,
    jint* new_class_data_len,
    unsigned char** new_class_data);
static void JNICALL OnSampledObjectAlloc(
    jvmtiEnv* jvmti, JNIEnv* jni,
    jthread thread, jobject object,
    jclass object_klass, jlong size);
static void JNICALL OnVMDeath(jvmtiEnv* jvmti, JNIEnv* jni);
static void retransform_already_loaded_classes();
static bool is_target_class(const char* class_name);
static void restore_transformed_classes();

// Agent_OnLoad - called when agent is loaded at JVM startup
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM* vm, char* options, void* reserved) {
    size_t sampling_rate = 100000;
    if (options != nullptr && strlen(options) > 0) {
        sampling_rate = atoll(options);
        if (sampling_rate <= 0) {
            sampling_rate = 100000;
        }
    }
    return initialize_agent(vm, options, false, sampling_rate);
}

// Agent_OnAttach - called when agent is dynamically attached
JNIEXPORT jint JNICALL Agent_OnAttach(JavaVM* vm, char* options, void* reserved) {
    size_t sampling_rate = 100000;
    if (options != nullptr && strlen(options) > 0) {
        sampling_rate = atoll(options);
        if (sampling_rate <= 0) {
            sampling_rate = 100000;
        }
    }
    return initialize_agent(vm, options, true, sampling_rate);
}

// Agent_OnUnload - called when agent is unloaded
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM* vm) {
    if (g_state == nullptr) {
        return;
    }

    std::lock_guard<std::mutex> lock(g_state->state_mutex);
    if (!g_state->initialized) {
        return;
    }

    // Clean up resources
    g_state->initialized = false;

    // Note: In a real implementation, we would restore transformed classes here
    restore_transformed_classes();

    // Clean up allocations
    delete g_state->class_transformer;
    delete g_state->allocation_tracker;
    delete g_state;
    g_state = nullptr;
}

static jint initialize_agent(JavaVM* vm, char* options, bool is_attach, size_t sampling_rate) {
    // Create global state if not exists
    if (g_state == nullptr) {
        g_state = new GlobalState();
    }

    std::lock_guard<std::mutex> lock(g_state->state_mutex);

    if (g_state->initialized) {
        return JNI_OK;  // Already initialized
    }

    g_state->jvm = vm;
    g_state->sampling_rate = sampling_rate;

    // Get JVMTI environment
    jint result = vm->GetEnv((void**)&g_state->jvmti, JVMTI_VERSION_1_2);
    if (result != JNI_OK) {
        return result;
    }

    // Get JNI environment
    result = vm->GetEnv((void**)&g_state->jni, JNI_VERSION_1_8);
    if (result != JNI_OK) {
        return result;
    }

    // Initialize components
    g_state->class_transformer = new ClassTransformer(g_state->jvmti);
    g_state->allocation_tracker = new AllocationTracker(sampling_rate);

    // Set JVMTI callbacks
    jvmtiEventCallbacks callbacks;
    std::memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = &OnClassFileLoadHook;
    callbacks.SampledObjectAlloc = &OnSampledObjectAlloc;
    callbacks.VMDeath = &OnVMDeath;

    result = g_state->jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (result != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }

    // Enable ClassFileLoadHook event
    result = g_state->jvmti->SetEventNotificationMode(
        JVMTI_ENABLE,
        JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
        nullptr);
    if (result != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }

    // Enable SampledObjectAlloc event
    result = g_state->jvmti->SetEventNotificationMode(
        JVMTI_ENABLE,
        JVMTI_EVENT_SAMPLED_OBJECT_ALLOC,
        nullptr);
    if (result != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }

    // Set heap sampling interval (in bytes)
    result = g_state->jvmti->SetHeapSamplingInterval(sampling_rate);
    if (result != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }

    // If attaching dynamically, retransform already loaded classes
    if (is_attach) {
        retransform_already_loaded_classes();
    }

    g_state->initialized = true;
    return JNI_OK;
}

static void JNICALL OnClassFileLoadHook(
    jvmtiEnv* jvmti, JNIEnv* jni,
    jclass class_being_redefined,
    jobject loader, const char* name,
    jobject protection_domain,
    jint class_data_len,
    const unsigned char* class_data,
    jint* new_class_data_len,
    unsigned char** new_class_data) {

    if (g_state == nullptr || !g_state->initialized) {
        return;
    }

    // Check if this is a class we want to transform
    if (g_state->class_transformer->shouldTransform(name)) {
        bool transformed = g_state->class_transformer->transform(
            name, class_data, class_data_len,
            new_class_data, new_class_data_len);

        // Track that we transformed this class
        if (transformed && name != nullptr) {
            std::lock_guard<std::mutex> lock(g_state->transformed_classes_mutex);
            g_state->transformed_classes.insert(name);
        }
    }
}

static void JNICALL OnSampledObjectAlloc(
    jvmtiEnv* jvmti, JNIEnv* jni,
    jthread thread, jobject object,
    jclass object_klass, jlong size) {

    if (g_bridge_class == nullptr || g_on_native_alloc_method == nullptr) {
        return;
    }

    // Get class signature
    char* signature;
    jvmtiError error = jvmti->GetClassSignature(object_klass, &signature, nullptr);
    if (error != JVMTI_ERROR_NONE) {
        return;
    }

    jstring type_string = jni->NewStringUTF(signature);
    jni->CallStaticVoidMethod(g_bridge_class, g_on_native_alloc_method, (jlong)size, type_string);

    // Free the signature memory
    jvmti->Deallocate((unsigned char*)signature);
}

static void JNICALL OnVMDeath(jvmtiEnv* jvmti, JNIEnv* jni) {
    // VM is exiting, clean up
    if (g_state != nullptr) {
        g_state->initialized = false;
    }
}

static void retransform_already_loaded_classes() {
    if (g_state == nullptr || g_state->jvmti == nullptr) {
        return;
    }

    // Get all loaded classes
    jint class_count;
    jclass* classes;
    jvmtiError result = g_state->jvmti->GetLoadedClasses(&class_count, &classes);
    if (result != JVMTI_ERROR_NONE) {
        return;
    }

    // Iterate through classes and retransform target classes
    for (jint i = 0; i < class_count; ++i) {
        jclass klass = classes[i];
        char* class_name;
        char* class_signature;

        result = g_state->jvmti->GetClassSignature(klass, &class_name, &class_signature);
        if (result != JVMTI_ERROR_NONE) {
            continue;
        }

        // Check if this is a target class
        if (is_target_class(class_name)) {
            bool expected = false;
            if (g_state->bytecode_transform_in_progress.compare_exchange_strong(expected, true)) {
                std::lock_guard<std::mutex> lock(g_state->class_transform_mutex);
                jvmtiError retransform_result = g_state->jvmti->RetransformClasses(1, &klass);
                if (retransform_result != JVMTI_ERROR_NONE) {
                    // Log warning but continue - some classes may be unmodifiable
                    if (retransform_result == JVMTI_ERROR_UNMODIFIABLE_CLASS) {
                        // Class is not modifiable, this is expected for some classes
                    } else if (retransform_result == JVMTI_ERROR_INVALID_CLASS) {
                        // Invalid class reference
                    } else {
                        // Other errors
                    }
                } else {
                    // Track successful transformation
                    if (class_name != nullptr) {
                        std::lock_guard<std::mutex> track_lock(g_state->transformed_classes_mutex);
                        g_state->transformed_classes.insert(class_name);
                    }
                }
                g_state->bytecode_transform_in_progress = false;
            }
        }

        // Free allocated memory
        if (class_name != nullptr) {
            g_state->jvmti->Deallocate((unsigned char*)class_name);
        }
        if (class_signature != nullptr) {
            g_state->jvmti->Deallocate((unsigned char*)class_signature);
        }
    }

    // Free the classes array
    if (classes != nullptr) {
        g_state->jvmti->Deallocate((unsigned char*)classes);
    }
}

static bool is_target_class(const char* class_name) {
    if (class_name == nullptr) {
        return false;
    }

    if (g_state != nullptr && g_state->class_transformer != nullptr) {
        return g_state->class_transformer->shouldTransform(class_name);
    }

    return false;
}

static void restore_transformed_classes() {
    if (g_state == nullptr || g_state->jvmti == nullptr) {
        return;
    }

    // Get all loaded classes
    jint class_count;
    jclass* classes;
    jvmtiError result = g_state->jvmti->GetLoadedClasses(&class_count, &classes);
    if (result != JVMTI_ERROR_NONE) {
        return;
    }

    // Make a copy of the transformed classes set under lock
    std::unordered_set<std::string> classes_to_restore;
    {
        std::lock_guard<std::mutex> lock(g_state->transformed_classes_mutex);
        classes_to_restore = g_state->transformed_classes;
    }

    // Iterate through classes and restore transformed classes
    for (jint i = 0; i < class_count; ++i) {
        jclass klass = classes[i];
        char* class_name;
        char* class_signature;

        result = g_state->jvmti->GetClassSignature(klass, &class_name, &class_signature);
        if (result != JVMTI_ERROR_NONE) {
            continue;
        }

        // Check if this class was transformed
        if (class_name != nullptr && classes_to_restore.find(class_name) != classes_to_restore.end()) {
            bool expected = false;
            if (g_state->bytecode_transform_in_progress.compare_exchange_strong(expected, true)) {
                std::lock_guard<std::mutex> lock(g_state->class_transform_mutex);
                // Retransform to restore original bytecode (since ClassTransformer
                // currently just returns the original)
                jvmtiError retransform_result = g_state->jvmti->RetransformClasses(1, &klass);
                // Ignore errors on cleanup
                (void)retransform_result;
                g_state->bytecode_transform_in_progress = false;
            }
        }

        // Free allocated memory
        if (class_name != nullptr) {
            g_state->jvmti->Deallocate((unsigned char*)class_name);
        }
        if (class_signature != nullptr) {
            g_state->jvmti->Deallocate((unsigned char*)class_signature);
        }
    }

    // Clear the transformed classes set
    {
        std::lock_guard<std::mutex> lock(g_state->transformed_classes_mutex);
        g_state->transformed_classes.clear();
    }

    // Free the classes array
    if (classes != nullptr) {
        g_state->jvmti->Deallocate((unsigned char*)classes);
    }
}

// JNI functions for Java access
extern "C" {

JNIEXPORT jboolean JNICALL Java_com_memdiag_nativeimpl_JVMTINativeAnalyzer_isAgentAttached0(
    JNIEnv* env, jclass cls) {
    return (g_state != nullptr && g_state->initialized) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_memdiag_nativeimpl_JVMTINativeAnalyzer_attachAgent0(
    JNIEnv* env, jclass cls, jint samplingRate) {
    // Agent is already attached if we're in this function
    // Store the sampling rate in global state
    if (g_state != nullptr) {
        std::lock_guard<std::mutex> lock(g_state->state_mutex);
        g_state->sampling_rate = static_cast<size_t>(samplingRate);
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_memdiag_nativeimpl_JVMTINativeAnalyzer_detachAgent0(
    JNIEnv* env, jclass cls) {
    if (g_state == nullptr) {
        return JNI_FALSE;
    }

    std::lock_guard<std::mutex> lock(g_state->state_mutex);
    if (!g_state->initialized) {
        return JNI_FALSE;
    }

    // Stop tracking first
    if (g_state->allocation_tracker != nullptr) {
        g_state->allocation_tracker->stopTracking();
    }

    // Disable JVMTI events
    if (g_state->jvmti != nullptr) {
        g_state->jvmti->SetEventNotificationMode(
            JVMTI_DISABLE,
            JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
            nullptr);
    }

    // Restore transformed classes
    restore_transformed_classes();

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_memdiag_nativeimpl_JVMTINativeAnalyzer_startAllocationTracking0(
    JNIEnv* env, jclass cls) {
    if (g_state != nullptr && g_state->allocation_tracker != nullptr) {
        g_state->allocation_tracker->startTracking();
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_memdiag_nativeimpl_JVMTINativeAnalyzer_stopAllocationTracking0(
    JNIEnv* env, jclass cls) {
    if (g_state != nullptr && g_state->allocation_tracker != nullptr) {
        g_state->allocation_tracker->stopTracking();
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_com_memdiag_nativeimpl_JVMTINativeAnalyzer_getTotalAllocated0(
    JNIEnv* env, jclass cls) {
    if (g_state != nullptr && g_state->allocation_tracker != nullptr) {
        return (jlong)g_state->allocation_tracker->getTotalAllocated();
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_com_memdiag_nativeimpl_JVMTINativeAnalyzer_getLiveBytes0(
    JNIEnv* env, jclass cls) {
    if (g_state != nullptr && g_state->allocation_tracker != nullptr) {
        return (jlong)g_state->allocation_tracker->getLiveBytes();
    }
    return 0;
}

JNIEXPORT void JNICALL Java_com_memdiag_agent_jvmti_JVMTIEventBridge_registerCallbacks(
    JNIEnv* env, jclass cls) {
    if (g_bridge_class != nullptr) {
        env->DeleteGlobalRef(g_bridge_class);
    }
    g_bridge_class = (jclass)env->NewGlobalRef(cls);
    g_on_native_alloc_method = env->GetStaticMethodID(g_bridge_class, "onNativeAllocation", "(JLjava/lang/String;)V");
}

} // extern "C"
