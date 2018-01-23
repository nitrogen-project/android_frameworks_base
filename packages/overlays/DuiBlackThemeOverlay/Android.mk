LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_RRO_THEME := DuiBlackTheme

LOCAL_PACKAGE_NAME := DuiBlackThemeOverlay

include $(BUILD_RRO_PACKAGE)
