LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := shapelib-1.3.0
LOCAL_SRC_FILES := shapelib-jni.c safileio.c shpopen.c dbfopen.c
LOCAL_LDLIBS    := -llog
include $(BUILD_SHARED_LIBRARY)


