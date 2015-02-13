#include <jni.h>
#include <cpu-features.h>
#include <string>
#include <functional>
#include <string>
#include <cctype>
#include <stdlib.h>

#ifdef DEBUG
#include <android/log.h>
#define LOGD(LOG_TAG, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGV(LOG_TAG, ...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGE(LOG_TAG, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_TAG "DosBoxTurbo"
#endif


extern "C"
JNIEXPORT jboolean JNICALL Java_com_fishstix_dosboxfree_DBMain_nativeHasNEON(JNIEnv *env, jobject obj, jobject ctx)
{
	uint64_t features = android_getCpuFeatures();
	//LOGD(LOG_TAG, "NEON CHECK %d",features);
	if (android_getCpuFamily() != ANDROID_CPU_FAMILY_ARM) {
		return JNI_FALSE;
	}
	if ((features & ANDROID_CPU_ARM_FEATURE_NEON) == 0) {
		return JNI_FALSE;
	} else {
		// valid signature
		return JNI_TRUE;
	}
}

extern "C"
JNIEXPORT jboolean JNICALL Java_com_fishstix_dosboxfree_DBMain_nativeIsARMv15(JNIEnv *env, jobject obj, jobject ctx)
{
	uint64_t features = android_getCpuFeatures();
	//LOGD(LOG_TAG, "NEON CHECK %d",features);
	if (android_getCpuFamily() != ANDROID_CPU_FAMILY_ARM) {
		return JNI_FALSE;
	}
	if ((features & ANDROID_CPU_ARM_FEATURE_NEON_FMA) == 0) {
		return JNI_FALSE;
/*	}
	if ((features & ANDROID_CPU_ARM_FEATURE_IDIV_ARM) == 0) {
		return JNI_FALSE; */
	} else {
		// valid signature
		return JNI_TRUE;
	}
}

extern "C"
JNIEXPORT jboolean JNICALL Java_com_fishstix_dosboxfree_DBMain_nativeIsARMv7(JNIEnv *env, jobject obj, jobject ctx)
{
	uint64_t features = android_getCpuFeatures();
	//LOGD(LOG_TAG, "ARM7 CHECK %d",features);
	if (android_getCpuFamily() != ANDROID_CPU_FAMILY_ARM) {
		return JNI_FALSE;
	}
	if ((features & ANDROID_CPU_ARM_FEATURE_ARMv7) == 0) {
		return JNI_FALSE;
	} else {
		// valid signature
		return JNI_TRUE;
	}
}

extern "C"
JNIEXPORT jint JNICALL Java_com_fishstix_dosboxfree_DBMain_nativeGetCPUFamily(JNIEnv *env, jobject obj)
{
	AndroidCpuFamily family = android_getCpuFamily();
	if (family == ANDROID_CPU_FAMILY_X86)
		return 2;
	if (family == ANDROID_CPU_FAMILY_ARM)
		return 1;
	if (family == ANDROID_CPU_FAMILY_MIPS)
		return 3;
	if (family == ANDROID_CPU_FAMILY_UNKNOWN)
		return 0;
}

