LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS    := -llog
APP_CPPFLAGS    += -std=c++17

LOCAL_MODULE    := native-filesystem-functions
LOCAL_SRC_FILES := native.c

include $(BUILD_SHARED_LIBRARY)
