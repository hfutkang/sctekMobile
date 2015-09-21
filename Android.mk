NEED_CHANGED_SO_PATH_TO := armeabi
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PROGUARD_ENABLED := disabled 
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES += telephony-common 

LOCAL_STATIC_JAVA_LIBRARIES := sync_framework \
	com.android.vcard \
	android-support-v7-appcompat \
	android-support-v4 \
	libammsdk       \
	mzxing	\
	android_api \
	universal-image-loader\
	zxing-core \
	IngenicGlassSDK  
	

LOCAL_RESOURCE_DIR = \
    $(LOCAL_PATH)/res \
    frameworks/support/v7/appcompat/res \
	packages/apps/HanLangSync/sync_framework/res

LOCAL_AAPT_FLAGS := \
	--auto-add-overlay \
	--extra-packages android.support.v7.appcompat

LOCAL_JNI_SHARED_LIBRARIES := liblocSDK5
LOCAL_JNI_SHARED_LIBRARIES += libffmpeg
LOCAL_JNI_SHARED_LIBRARIES += liblive_jni
LOCAL_PACKAGE_NAME := HanLang


# Builds against the public SDK
#LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)  
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES :=libammsdk:libs/libammsdk.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += mzxing:libs/zxing.jar android_api:libs/android_api.jar sync_framework:libs/sync_framework.jar universal-image-loader:libs/universal-image-loader-1.9.3.jar IngenicGlassSDK:libs/IngenicGlassSDK.jar zxing-core:libs/zxing-core.jar
LOCAL_PREBUILT_LIBS := liblocSDK5:libs/armeabi/liblocSDK5.so
LOCAL_PREBUILT_LIBS += libffmpeg:libs/armeabi/libffmpeg.so 
LOCAL_PREBUILT_LIBS += liblive_jni:libs/armeabi/liblive_jni.so 
LOCAL_MODULE_TAGS := optional  
include $(BUILD_MULTI_PREBUILT)  

# include $(call all-makefiles-under,$(LOCAL_PATH))
