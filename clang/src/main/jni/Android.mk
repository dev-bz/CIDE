LOCAL_PATH:=$(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := index
LOCAL_SRC_FILES := index.c
ifeq ($(TARGET_ARCH_ABI),x86)
  LOCAL_CFLAGS+= -ffast-math -mtune=atom -mssse3 -mfpmath=sse
endif
LOCAL_LDLIBS := -llog
LOCAL_STATIC_LIBRARIES :=clang
LOCAL_CFLAGS := -I$(LOCAL_PATH)/clang/include -std=c99
# LOCAL_LDFLAGS := -L$(LOCAL_PATH)/clang/lib/$(TARGET_ARCH_ABI) -lclang
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := clang
LOCAL_SRC_FILES := clang/lib/$(TARGET_ARCH_ABI)/libclang.so
include $(PREBUILT_SHARED_LIBRARY)