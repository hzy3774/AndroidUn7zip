JNI_PATH := $(call my-dir)

LOCAL_PATH := $(JNI_PATH)/un7z/

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/un7z/src/

LOCAL_MODULE := un7z

LOCAL_CFLAGS := -DDEBUG 
				
LOCAL_SRC_FILES := \
		./src/7zAlloc.c \
		./src/7zBuf.c \
		./src/7zCrc.c \
		./src/7zDecode.c \
		./src/7zExtract.c \
		./src/7zFile.c \
		./src/7zHeader.c \
		./src/7zIn.c \
		./src/7zItem.c \
		./src/7zStream.c \
		./src/Bcj2.c \
		./src/Bra86.c \
		./src/LzmaDec.c \
\
		./src/7zMain.c \
		./com_hu_andun7z_AndUn7z.cpp
        
LOCAL_LDLIBS := -llog 

include $(BUILD_SHARED_LIBRARY)

