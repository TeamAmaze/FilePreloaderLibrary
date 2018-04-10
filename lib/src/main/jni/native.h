#ifndef FILEPRELOADERLIBRARY_NATIVE_H
#define FILEPRELOADERLIBRARY_NATIVE_H

#include <string>
#include <vector>

#include <jni.h>

#include <sys/types.h>
#include <dirent.h>
#include <sys/stat.h>
#include <unistd.h>

using std::string;
using std::vector;

#define MAKE_FN_NAME(x) Java_com_amaze_filepreloaderlibrary_Native_ ## x
#define FUN(ret, name) JNIEXPORT ret JNICALL MAKE_FN_NAME(name)

extern "C" {
    FUN(jobjectArray, getDirectoriesInDirectory)(JNIEnv *env, jobject obj, jstring jdirectory);
}

#endif //FILEPRELOADERLIBRARY_NATIVE_H
