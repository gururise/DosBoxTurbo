LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := fishstix_util

CG_SUBDIRS := .\

# Add more subdirs here, like src/subdir1 src/subdir2

MY_PATH := $(LOCAL_PATH)

LOCAL_PATH := $(abspath $(LOCAL_PATH))

CG_SRCDIR := $(LOCAL_PATH)
LOCAL_CFLAGS :=	-I$(LOCAL_PATH)/include \
				-I$(LOCAL_PATH)/../fishstix/include \
				-I$(LOCAL_PATH)/../dosbox \
				-I$(LOCAL_PATH) \
				
LOCAL_PATH := $(MY_PATH)

LOCAL_SRC_FILES := $(foreach F, $(CG_SUBDIRS), $(addprefix $(F)/,$(notdir $(wildcard $(LOCAL_PATH)/$(F)/*.cpp))))
LOCAL_CPPFLAGS := $(LOCAL_CFLAGS)
LOCAL_CXXFLAGS := $(LOCAL_CFLAGS)
LOCAL_STATIC_LIBRARIES := cpufeatures

include $(BUILD_SHARED_LIBRARY)
$(call import-module,android/cpufeatures)
