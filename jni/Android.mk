LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := diffusion
LOCAL_SRC_FILES := diffusion.c

include $(BUILD_SHARED_LIBRARY)
