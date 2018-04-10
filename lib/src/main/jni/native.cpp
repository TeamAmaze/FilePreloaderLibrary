#include "native.h"

void directoriesindir(vector<string>& directories, std::string path) {
    DIR *startdir = opendir(path.data());

    if (startdir == NULL) {
        return;
    }

    struct dirent *dirent;
    while ((dirent = readdir(startdir)) != NULL) {
        if((dirent->d_name[0] == '.' && dirent->d_name[1] == '\0')
           || (dirent->d_name[0] == '.' && dirent->d_name[1] == '.' && dirent->d_name[2] == '\0')) continue;

        struct stat st;

        if (fstatat(dirfd(startdir), dirent->d_name, &st, 0) == -1) {
            continue;
        }

        if (S_ISDIR(st.st_mode)) {
            directories.push_back(dirent->d_name);
        }
    }
    closedir(startdir);
}

FUN(jobjectArray, getDirectoriesInDirectory)(JNIEnv *env, jobject obj, jstring jdirectory) {
    std::string directory = env->GetStringUTFChars(jdirectory, 0);

    vector<string> directories;
    directoriesindir(directories, directory);

    jobjectArray jstringArray = env->NewObjectArray(directories.size(), env->FindClass("java/lang/String"), 0);

    for(int i = 0; i < directories.size(); i++) {
        jstring jstring = env->NewStringUTF(directories[i].data());
        env->SetObjectArrayElement(jstringArray, i, jstring);
        env->DeleteLocalRef(jstring);
    }

    return jstringArray;
}