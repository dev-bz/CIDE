LOCAL_PATH:=$(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE:= termux
LOCAL_SRC_FILES:= termux.c
LOCAL_CFLAGS+= -std=c11 -Wall -Wextra -Os -fno-stack-protector
LOCAL_LDLIBS+= -nostdlib -Wl,--gc-sections
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE   := loader
LOCAL_SRC_FILES:= loader.c
ifeq ($(TARGET_ARCH_ABI),x86)
  LOCAL_CFLAGS+= -ffast-math -mtune=atom -mssse3 -mfpmath=sse
endif
LOCAL_LDLIBS := -landroid
#LOCAL_CFLAGS += -v
include $(BUILD_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := sdl2util
#LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libsdl2util.so
#include $(PREBUILT_SHARED_LIBRARY)