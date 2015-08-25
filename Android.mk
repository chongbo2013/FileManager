LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional


LOCAL_SRC_FILES := $(call all-java-files-under, src)

ifeq ($(AOSP_PLATFORM), AOSP2)
    LOCAL_JAVA_LIBRARIES := telephony-common
endif

LOCAL_JAVA_LIBRARIES += telephony-common lewa-framework

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v13 \
    android-support-v4 \
    lewa-support-v7-appcompat \
    com.lewa.themes

LOCAL_RESOURCE_DIR = \
    $(LOCAL_PATH)/res \
    vendor/lewa/apps/LewaSupportLib/actionbar_4.4/res \

LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --extra-packages lewa.support.v7.appcompat
LOCAL_PACKAGE_NAME := LewaFileManager
LOCAL_CERTIFICATE  := platform

#LOCAL_PROGUARD_FLAG_FILES := proguard.cfg
#LOCAL_PROGUARD_ENABLED := full

include $(BUILD_PACKAGE)

