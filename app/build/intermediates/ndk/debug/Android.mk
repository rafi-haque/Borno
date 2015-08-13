LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := s9
LOCAL_SRC_FILES := \
	F:\Bangla Keyboard\S9\app\src\main\jni\Android.mk \
	F:\Bangla Keyboard\S9\app\src\main\jni\com_gilbertl_s9_BinaryDictionary.cpp \
	F:\Bangla Keyboard\S9\app\src\main\jni\dictionary.cpp \

LOCAL_C_INCLUDES += F:\Bangla Keyboard\S9\app\src\main\jni
LOCAL_C_INCLUDES += F:\Bangla Keyboard\S9\app\src\debug\jni

include $(BUILD_SHARED_LIBRARY)
