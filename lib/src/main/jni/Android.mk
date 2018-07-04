LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS    := -llog
APP_CPPFLAGS    += -std=c++17 -lstdc++

LOCAL_MODULE    := native-filesystem-functions
LOCAL_SRC_FILES := native.cpp

include $(BUILD_SHARED_LIBRARY)
