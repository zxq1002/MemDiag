#ifndef GC_ROOT_TRACKER_H
#define GC_ROOT_TRACKER_H

#include <jni.h>
#include <jvmti.h>
#include <vector>
#include <string>
#include <mutex>
#include <unordered_map>
#include <unordered_set>

// GC Root 类型枚举（与 Java GcRootType 对应）
enum class GcRootType {
    SYSTEM_CLASS = 0,
    JNI_LOCAL = 1,
    JNI_GLOBAL = 2,
    STATIC_FIELD = 3,
    THREAD_STACK = 4,
    MONITOR = 5,
    OTHER = 6,
    INSTANCE_FIELD = 7  // 引用链中的实例字段
};

// 单个引用节点
struct ReferenceNode {
    jlong object_id;
    std::string class_name;
    std::string field_name;  // 此字段引用了下一个对象
    GcRootType node_type;
};

// 一条完整的 GC Root 路径
struct GcRootPath {
    GcRootType root_type;
    std::string root_description;  // 例如 "Thread: main"
    std::vector<ReferenceNode> nodes;
    jlong target_object_id;
    std::string target_class_name;
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

    // 查找指向特定类的 GC Root 路径
    std::vector<GcRootPath> findGcRootsForClass(const std::string& class_name, int max_paths, int max_depth);

    // 获取当前追踪到的所有 GC Root 路径
    std::vector<GcRootPath> getAllGcRootPaths();

    // JVMTI 回调处理
    static void JNICALL FollowReferenceCallback(
        jvmtiHeapReferenceKind reference_kind,
        jlong class_tag,
        jlong size,
        jlong* tag_ptr,
        jlong length,
        jint reference_index,
        jvmtiHeapReferenceInfo* reference_info);

    static void JNICALL HeapReferenceCallback(
        jvmtiHeapReferenceKind kind,
        jlong class_tag,
        jlong referrer_class_tag,
        jlong size,
        jlong* tag_ptr,
        jlong* referrer_tag_ptr,
        jint length);

private:
    jvmtiEnv* jvmti_;
    std::mutex mutex_;
    bool tracking_;

    // GC Root 统计
    std::unordered_map<GcRootType, jlong> root_counts_;

    // 收集到的 GC Root 路径
    std::vector<GcRootPath> root_paths_;

    // 当前正在构建的路径
    std::vector<ReferenceNode> current_path_;

    // 目标类名（用于筛选）
    std::string target_class_name_;
    int max_paths_;
    int max_depth_;

    // 已访问的对象（防止循环）
    std::unordered_set<jlong> visited_objects_;

    // 转换 JVMTI reference kind 到我们的类型
    static GcRootType convertReferenceKind(jvmtiHeapReferenceKind kind);

    // 获取类名
    std::string getClassName(jclass klass);

    // 获取字段名（如果可用）
    std::string getFieldName(jvmtiHeapReferenceInfo* ref_info);
};

#endif // GC_ROOT_TRACKER_H
