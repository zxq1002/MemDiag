#ifndef GC_ROOT_TRACKER_H
#define GC_ROOT_TRACKER_H

#include <jni.h>
#include <jvmti.h>
#include <vector>
#include <string>
#include <mutex>
#include <unordered_map>

// GC Root 类型枚举（与 Java GcRootType 对应）
enum class GcRootType {
    SYSTEM_CLASS = 0,
    JNI_LOCAL = 1,
    JNI_GLOBAL = 2,
    STATIC_FIELD = 3,
    THREAD_STACK = 4,
    MONITOR = 5,
    OTHER = 6,
    INSTANCE_FIELD = 7
};

class GcRootTracker {
public:
    GcRootTracker(jvmtiEnv* jvmti);
    ~GcRootTracker();

    // 开始追踪 GC Roots
    bool startTracking();

    // 停止追踪
    void stopTracking();

    // 获取所有 GC Root 统计
    std::unordered_map<GcRootType, jlong> getGcRootStats();

    // 检查是否正在追踪
    bool isTracking() const;

private:
    jvmtiEnv* jvmti_;
    std::mutex mutex_;
    bool tracking_;

    // GC Root 统计
    std::unordered_map<GcRootType, jlong> root_counts_;

    // 迭代所有 GC Roots 并收集统计
    void collectGcRootStats();
};

#endif // GC_ROOT_TRACKER_H
