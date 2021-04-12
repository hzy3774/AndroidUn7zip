LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# include $(CLEAR_VARS)
LOCAL_MODULE := un7zip

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/src \
    $(LOCAL_PATH)/un7zip \

LOCAL_SRC_FILES := \
	$(wildcard $(LOCAL_PATH)/src/*.c) \
	$(wildcard $(LOCAL_PATH)/un7zip/*.c) \
    $(wildcard $(LOCAL_PATH)/un7zip/*.cpp) \

LOCAL_CFLAGS += -Wall -ffunction-sections -fdata-sections
LOCAL_CXXFLAGS += -Wall -frtti -fexceptions -ffunction-sections -fdata-sections
LOCAL_LDFLAGS += -Wl,--gc-sections

LOCAL_LDLIBS := -llog -landroid

include $(BUILD_SHARED_LIBRARY)