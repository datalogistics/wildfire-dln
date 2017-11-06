LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := conversions
LOCAL_SRC_FILES := conversions.cpp geoid.cpp geocent.cpp loccart.cpp xform_convert.cpp mini_blas_vec.cpp linalg.cpp ElevationConversions.cpp utm.cpp tranmerc.cpp mgrs.cpp ups.cpp polarst.cpp WMM_SubLibrary.cpp convergence.cpp
LOCAL_LDLIBS    := -llog
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/shapelib-1.3.0/Android.mk
