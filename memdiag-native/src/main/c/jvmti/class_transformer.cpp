#include "class_transformer.h"
#include <cstring>

ClassTransformer::ClassTransformer(jvmtiEnv* jvmti)
    : jvmti_(jvmti) {

    // Default target classes
    addTargetClass("sun/misc/Unsafe");
    addTargetClass("jdk/internal/misc/Unsafe");
    addTargetClass("java/nio/ByteBuffer");
    addTargetClass("java/nio/DirectByteBuffer");
}

ClassTransformer::~ClassTransformer() {
}

bool ClassTransformer::shouldTransform(const char* class_name) {
    if (class_name == nullptr) {
        return false;
    }
    return target_classes_.find(class_name) != target_classes_.end();
}

bool ClassTransformer::transform(const char* class_name,
                                const unsigned char* class_data,
                                jint class_data_len,
                                unsigned char** new_class_data,
                                jint* new_class_data_len) {

    // For now, just return a copy of the original class
    // Real implementation would use ASM or similar to modify bytecode
    *new_class_data_len = class_data_len;
    *new_class_data = (unsigned char*)allocateClassFile(class_data_len);

    if (*new_class_data == nullptr) {
        return false;
    }

    std::memcpy(*new_class_data, class_data, class_data_len);
    return true;
}

void ClassTransformer::addTargetClass(const char* class_name) {
    if (class_name != nullptr) {
        target_classes_.insert(class_name);
    }
}

void ClassTransformer::removeTargetClass(const char* class_name) {
    if (class_name != nullptr) {
        target_classes_.erase(class_name);
    }
}

bool ClassTransformer::transformUnsafe(const unsigned char* class_data,
                                      jint class_data_len,
                                      unsigned char** new_class_data,
                                      jint* new_class_data_len) {
    // Placeholder for Unsafe transformation
    return transform("", class_data, class_data_len, new_class_data, new_class_data_len);
}

bool ClassTransformer::transformByteBuffer(const unsigned char* class_data,
                                          jint class_data_len,
                                          unsigned char** new_class_data,
                                          jint* new_class_data_len) {
    // Placeholder for ByteBuffer transformation
    return transform("", class_data, class_data_len, new_class_data, new_class_data_len);
}

void* ClassTransformer::allocateClassFile(jint size) {
    unsigned char* result = nullptr;
    jvmtiError err = jvmti_->Allocate(size, &result);
    if (err != JVMTI_ERROR_NONE) {
        return nullptr;
    }
    return result;
}
