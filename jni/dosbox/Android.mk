LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := dosbox_main

CG_SUBDIRS := \
src/dos \
src/hardware \
src/hardware/serialport \
src \
src/cpu \
src/cpu/core_dynrec \
src/cpu/core_dyn_x86 \
src/cpu/core_full \
src/cpu/core_normal \
src/fpu \
src/gui \
src/gui/gui_tk \
src/gui/zmbv \
src/ints \
src/libs \
src/misc \
src/shell \

MY_PATH := $(LOCAL_PATH)

LOCAL_PATH := $(abspath $(LOCAL_PATH))

CG_SRCDIR := $(LOCAL_PATH)
LOCAL_CFLAGS :=	-I$(LOCAL_PATH)/include \
				$(foreach D, $(CG_SUBDIRS), -I$(CG_SRCDIR)/$(D)) \
				-I$(LOCAL_PATH)/../sdl/include \
				-I$(LOCAL_PATH)/../fishstix/include \
				-I$(LOCAL_PATH) 
#				-I$(LOCAL_PATH)/../sdl_net/include \
#				-I$(LOCAL_PATH)/../sdl_sound/include \				

LOCAL_PATH := $(MY_PATH)

#Change C++ file extension as appropriate
LOCAL_CPP_EXTENSION := .cpp

LOCAL_SRC_FILES := $(foreach F, $(CG_SUBDIRS), $(addprefix $(F)/,$(notdir $(wildcard $(LOCAL_PATH)/$(F)/*.cpp))))
LOCAL_SRC_FILES += src/snprintf.c
LOCAL_ARM_MODE := arm

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	# NEON OPTIMIZATIONS - enable neon in config as well
	#FILE_LIST := $(wildcard $(LOCAL_PATH)/src/math_neon/*.cpp)
	#LOCAL_SRC_FILES += $(FILE_LIST:$(LOCAL_PATH)/%=%)
	#LOCAL_ARM_NEON := true
endif

LOCAL_CPPFLAGS := $(LOCAL_CFLAGS)
LOCAL_CXXFLAGS := $(LOCAL_CFLAGS)


LOCAL_STATIC_LIBRARIES := snprintf
#LOCAL_STATIC_LIBRARIES := sdl sdl_net sdl_sound mt32emu snprintf


include $(BUILD_STATIC_LIBRARY)

