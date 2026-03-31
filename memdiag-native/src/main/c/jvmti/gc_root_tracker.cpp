#include "gc_root_tracker.h"
#include <cstring>
#include <iostream>

// 全局指针用于回调访问
static GcRootTracker* g_gc_root_tracker = nullptr;

GcRootTracker::GcRootTracker(jvmtiEnv* jvmti)
    : jvmti_(jvmti), tracking_(false), max_paths_(10), max_depth_(10) {
    // 初始化统计
    root_counts_[GcRootType::SYSTEM_CLASS] = 0;
    root_counts_[GcRootType::JNI_LOCAL] = 0;
    root_counts_[GcRootType::JNI_GLOBAL] = 0;
    root_counts_[GcRootType::STATIC_FIELD] = 0;
    root_counts_[GcRootType::THREAD_STACK] = 0;
    root_counts_[GcRootType::MONITOR] = 0;
    root_counts_[GcRootType::OTHER] = 0;
}

GcRootTracker::~GcRootTracker() {
    stopTracking();
}

bool GcRootTracker::startTracking() {
    std::lock_guard<std::mutex> lock(mutex_);

    if (tracking_) {
        return true;
    }

    g_gc_root_tracker = this;
    tracking_ = true;

    // 重置统计
    for (auto& pair : root_counts_) {
        pair.second = 0;
    }
    root_paths_.clear();

    return true;
}

void GcRootTracker::stopTracking() {
    std::lock_guard<std::mutex> lock(mutex_);

    if (!tracking_) {
        return;
    }

    tracking_ = false;
    g_gc_root_tracker = nullptr;
}

std::unordered_map<GcRootType, jlong> GcRootTracker::getGcRootStats() {
    std::lock_guard<std::mutex> lock(mutex_);
    return root_counts_;
}

std::vector<GcRootPath> GcRootTracker::findGcRootsForClass(
    const std::string& class_name, int max_paths, int max_depth) {

    std::lock_guard<std::mutex> lock(mutex_);

    target_class_name_ = class_name;
    max_paths_ = max_paths;
    max_depth_ = max_depth;
    root_paths_.clear();
    visited_objects_.clear();

    if (!tracking_) {
        return root_paths_;
    }

    // 使用 JVMTI FollowReferences 遍历堆
    jvmtiHeapCallbacks callbacks;
    std::memset(&callbacks, 0, sizeof(callbacks));
    callbacks.follow_reference = &FollowReferenceCallback;
    callbacks.heap_reference = &HeapReferenceCallback;

    jvmtiError error = jvmti_->FollowReferences(
        0,  // heap filter (0 = all)
        nullptr,  // start object (null = GC roots)
        nullptr,  // class filter
        &callbacks,
        nullptr);  // user data

    if (error != JVMTI_ERROR_NONE) {
        std::cerr << "[MemDiag] FollowReferences failed: " << error << std::endl;
    }

    return root_paths_;
}

std::vector<GcRootPath> GcRootTracker::getAllGcRootPaths() {
    std::lock_guard<std::mutex> lock(mutex_);
    return root_paths_;
}

GcRootType GcRootTracker::convertReferenceKind(jvmtiHeapReferenceKind kind) {
    switch (kind) {
        case JVMTI_HEAP_REFERENCE_CLASS:
            return GcRootType::SYSTEM_CLASS;
        case JVMTI_HEAP_REFERENCE_JNI_LOCAL:
            return GcRootType::JNI_LOCAL;
        case JVMTI_HEAP_REFERENCE_JNI_GLOBAL:
            return GcRootType::JNI_GLOBAL;
        case JVMTI_HEAP_REFERENCE_STATIC_FIELD:
            return GcRootType::STATIC_FIELD;
        case JVMTI_HEAP_REFERENCE_STACK_LOCAL:
        case JVMTI_HEAP_REFERENCE_THREAD:
            return GcRootType::THREAD_STACK;
        case JVMTI_HEAP_REFERENCE_MONITOR:
            return GcRootType::MONITOR;
        case JVMTI_HEAP_REFERENCE_FIELD:
            return GcRootType::INSTANCE_FIELD;
        default:
            return GcRootType::OTHER;
    }
}

std::string GcRootTracker::getClassName(jclass klass) {
    if (klass == nullptr || jvmti_ == nullptr) {
        return "";
    }

    char* signature;
    char* generic;
    jvmtiError error = jvmti_->GetClassSignature(klass, &signature, &generic);

    if (error != JVMTI_ERROR_NONE || signature == nullptr) {
        return "";
    }

    std::string result(signature);

    // 转换 JNI 签名为类名: Lcom/example/MyClass; -> com.example.MyClass
    if (result.length() > 2 && result[0] == 'L' && result[result.length()-1] == ';') {
        result = result.substr(1, result.length() - 2);
        for (size_t i = 0; i < result.length(); ++i) {
            if (result[i] == '/') {
                result[i] = '.';
            }
        }
    }

    jvmti_->Deallocate((unsigned char*)signature);
    if (generic != nullptr) {
        jvmti_->Deallocate((unsigned char*)generic);
    }

    return result;
}

std::string GcRootTracker::getFieldName(jvmtiHeapReferenceInfo* ref_info) {
    if (ref_info == nullptr) {
        return "";
    }

    // 字段名信息存储在 reference_info 的不同位置，取决于 reference_kind
    // 这是一个简化实现
    return "";
}

void JNICALL GcRootTracker::FollowReferenceCallback(
    jvmtiHeapReferenceKind reference_kind,
    jlong class_tag,
    jlong size,
    jlong* tag_ptr,
    jlong length,
    jint reference_index,
    jvmtiHeapReferenceInfo* reference_info) {

    if (g_gc_root_tracker == nullptr) {
        return;
    }

    std::lock_guard<std::mutex> lock(g_gc_root_tracker->mutex_);

    // 检查是否达到路径数量限制
    if (g_gc_root_tracker->root_paths_.size() >=
        (size_t)g_gc_root_tracker->max_paths_) {
        return;
    }

    // 转换引用类型
    GcRootType type = convertReferenceKind(reference_kind);

    // 更新统计
    if (type != GcRootType::INSTANCE_FIELD) {
        g_gc_root_tracker->root_counts_[type]++;
    }

    // 对于 GC Roots，创建新的路径
    if (type != GcRootType::INSTANCE_FIELD) {
        GcRootPath path;
        path.root_type = type;

        // 构建 root 描述
        switch (type) {
            case GcRootType::THREAD_STACK:
                path.root_description = "Thread Stack";
                break;
            case GcRootType::JNI_GLOBAL:
                path.root_description = "JNI Global";
                break;
            case GcRootType::JNI_LOCAL:
                path.root_description = "JNI Local";
                break;
            case GcRootType::STATIC_FIELD:
                path.root_description = "Static Field";
                break;
            case GcRootType::SYSTEM_CLASS:
                path.root_description = "System Class";
                break;
            case GcRootType::MONITOR:
                path.root_description = "Monitor";
                break;
            default:
                path.root_description = "Other";
        }

        g_gc_root_tracker->root_paths_.push_back(path);
    }
}

void JNICALL GcRootTracker::HeapReferenceCallback(
    jvmtiHeapReferenceKind kind,
    jlong class_tag,
    jlong referrer_class_tag,
    jlong size,
    jlong* tag_ptr,
    jlong* referrer_tag_ptr,
    jint length) {

    // 这个回调用于堆中的对象引用
    // 简化实现中暂不做详细处理
}
