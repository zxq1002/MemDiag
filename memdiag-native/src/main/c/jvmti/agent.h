#ifndef AGENT_H
#define AGENT_H

#include <jni.h>
#include <jvmti.h>
#include <atomic>
#include <mutex>
#include <unordered_map>
#include <unordered_set>
#include <string>

// Forward declarations
class ClassTransformer;
class AllocationTracker;
class GcRootTracker;

struct GlobalState {
    JavaVM* jvm;
    jvmtiEnv* jvmti;
    JNIEnv* jni;
    std::atomic<bool> initialized;
    std::atomic<bool> tracking_enabled;
    std::mutex state_mutex;
    std::atomic<bool> bytecode_transform_in_progress;
    std::mutex class_transform_mutex;

    ClassTransformer* class_transformer;
    AllocationTracker* allocation_tracker;
    GcRootTracker* gc_root_tracker;

    // Configuration
    size_t sampling_rate;

    // Track classes that were actually transformed
    std::unordered_set<std::string> transformed_classes;
    std::mutex transformed_classes_mutex;

    GlobalState()
        : jvm(nullptr), jvmti(nullptr), jni(nullptr),
          initialized(false), tracking_enabled(false),
          bytecode_transform_in_progress(false),
          class_transformer(nullptr), allocation_tracker(nullptr),
          gc_root_tracker(nullptr),
          sampling_rate(100000) {}
};

#endif // AGENT_H
