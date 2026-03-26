#ifndef CLASS_TRANSFORMER_H
#define CLASS_TRANSFORMER_H

#include <jni.h>
#include <jvmti.h>
#include <string>
#include <unordered_set>
#include <vector>

class ClassTransformer {
public:
    explicit ClassTransformer(jvmtiEnv* jvmti);
    ~ClassTransformer();

    bool shouldTransform(const char* class_name);
    bool transform(const char* class_name,
                   const unsigned char* class_data,
                   jint class_data_len,
                   unsigned char** new_class_data,
                   jint* new_class_data_len);

    void addTargetClass(const char* class_name);
    void removeTargetClass(const char* class_name);

private:
    jvmtiEnv* jvmti_;
    std::unordered_set<std::string> target_classes_;

    bool transformUnsafe(const unsigned char* class_data,
                        jint class_data_len,
                        unsigned char** new_class_data,
                        jint* new_class_data_len);

    bool transformByteBuffer(const unsigned char* class_data,
                            jint class_data_len,
                            unsigned char** new_class_data,
                            jint* new_class_data_len);

    void* allocateClassFile(jint size);
};

#endif // CLASS_TRANSFORMER_H
