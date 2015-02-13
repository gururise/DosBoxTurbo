/*
 *  Copyright (C) 2012 Fishstix - (ruebsamen.gene@gmail.com)
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
#define CPU_AUTODETERMINE_NONE		0x00
#define CPU_AUTODETERMINE_CORE		0x01
#define CPU_AUTODETERMINE_CYCLES	0x02
#define CPU_CYCLE_LIMIT		40000
//#define DEBUG

#ifdef DEBUG
#include <android/log.h>
#define LOGD(LOG_TAG, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGV(LOG_TAG, ...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGE(LOG_TAG, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_TAG "DosBoxTurbo"
#endif

#include <jni.h>
#include <cstring>
#include "util.h"
#include "config.h"
#include "loader.h"
#include "render.h"
//#include "prof.h"
int dosbox_main(int argc, const char* argv[]);
void swapInNextDisk(bool pressed);
extern void DOSBOX_UnlockSpeed( bool pressed ) __attribute__((visibility("hidden")));
Bitu MEM_TotalPages(void) __attribute__((visibility("hidden")));
extern void CPU_CycleIncrease(bool pressed);
extern void CPU_CycleDecrease(bool pressed);
extern void JOYSTICK_Enable(Bitu which,bool enabled);
char arg_start_command[256]="";
extern struct loader_config myLoader;
extern struct loader_config *loadf;

extern "C"
JNIEXPORT void JNICALL Java_com_fishstix_dosboxfree_DBMain_nativeStart(JNIEnv * env, jobject obj, jobject ctx, jobject bitmap, jint width, jint height, jstring confpath)
{
	Android_Init(env, obj, bitmap, width, height);
	const char * argv[] = { "dosbox", "-conf", (env)->GetStringUTFChars(confpath,NULL), "-c", arg_start_command  };
	dosbox_main((!arg_start_command[0])?3:5, argv);
	
	Android_ShutDown();
}

extern Render_t render;
extern bool CPU_CycleAutoAdjust;
extern bool CPU_SkipCycleAutoAdjust;
//bool fastforward_on = false;
//extern Bit32s CPU_Cycles;
extern Bit32s CPU_CycleMax;
extern Bit32s CPU_CycleLimit;
extern Bit32s CPU_OldCycleMax;
extern Bit32s CPU_CyclePercUsed;
extern Bitu CPU_AutoDetermineMode;
extern bool ticksLocked;
extern Bit32s MEM_RealMemory;

extern "C"
JNIEXPORT void JNICALL Java_com_fishstix_dosboxfree_DBMain_nativeSetOption(JNIEnv * env, jobject obj, jint option, jint value, jobject value2, jboolean lic)
{
	//mouseValid = mouseWarpS(env,obj,ctx);
	switch (option) {
		case 1:
			myLoader.soundEnable = value;
			enableSound = (value != 0);
			break;
		case 2:
			myLoader.memsize = value;
			break;
		case 10:
				if (value == -1) {
					CPU_CycleAutoAdjust = true;
					CPU_SkipCycleAutoAdjust = false;
					if (enableCycleHack) {
						CPU_CyclePercUsed = 105;
					} else {
						CPU_CyclePercUsed = 100;
					}
					CPU_CycleLimit = CPU_CYCLE_LIMIT;
					CPU_CycleMax = CPU_CYCLE_LIMIT;
				} else {
					CPU_CycleAutoAdjust = false;
					CPU_SkipCycleAutoAdjust = false;
					CPU_CycleMax = (int)value;
					CPU_OldCycleMax = (int)value;
					CPU_CycleLimit = (int)value;
					myLoader.cycles = (int)value;
				}
			break;
		case 11:
			myLoader.frameskip = value;
			render.frameskip.max = value;
			break;
		case 12:
			myLoader.refreshHack = value;
			enableRefreshHack = (value != 0);
			break;
		case 13:
			myLoader.cycleHack = value;
			enableCycleHack = (value != 0);
			if (enableCycleHack) {
				CPU_CyclePercUsed = 105;
			} else {
				CPU_CyclePercUsed = 100;
			}
			break;
		case 14:
			myLoader.mixerHack = value;
			enableMixerHack = (value != 0);
			break;
		case 15:
			//myLoader.autoCPU = value;

			if (!CPU_CycleAutoAdjust) {
				if (!value) {
					CPU_AutoDetermineMode=CPU_AUTODETERMINE_NONE;
				} else {
					CPU_AutoDetermineMode|=CPU_AUTODETERMINE_CYCLES;
					CPU_CyclePercUsed = 100;
				}
			}

			if (!lic) {
				CPU_AutoDetermineMode=CPU_AUTODETERMINE_NONE;
			}
			break;
		case 16:
			DOSBOX_UnlockSpeed((value != 0));
			break;
		case 17:	// cycleadjust
			if (!lic) {
			} else {
				if (value) {
					CPU_CycleIncrease(true);
				} else {
					CPU_CycleDecrease(true);
				}
			}
			break;
		case 18:	// joystick enable
			if (value) {
				// enable
				JOYSTICK_Enable(0, true);
			} else {
				// disable
				JOYSTICK_Enable(0, false);
			}
			break;
		case 19:	// 3DFX Emulation (glide) enable
			myLoader.glideEnable = value;
			enableGlide = (value != 0);
			break;
		case 21:
			swapInNextDisk(true);
			break;
		case 50:
			strcpy(arg_start_command, (env)->GetStringUTFChars((jstring)value2, 0));
			break;
	}
}

extern "C"
JNIEXPORT jint JNICALL Java_com_fishstix_dosboxfree_DosBoxControl_nativeGetMemSize(JNIEnv * env, jobject obj) {
	return MEM_TotalPages()/256;
}

extern "C"
JNIEXPORT jint JNICALL Java_com_fishstix_dosboxfree_DosBoxControl_nativeGetCycleCount(JNIEnv * env, jobject obj) {
	if (CPU_CycleAutoAdjust) {
		return CPU_CyclePercUsed;
	}
	return CPU_CycleMax;
}

extern "C"
JNIEXPORT jint JNICALL Java_com_fishstix_dosboxfree_DosBoxControl_nativeGetFrameSkipCount(JNIEnv * env, jobject obj) {
	return render.frameskip.max;
}

extern "C"
JNIEXPORT jboolean JNICALL Java_com_fishstix_dosboxfree_DosBoxControl_nativeGetAutoAdjust(JNIEnv * env, jobject obj) {
	CPU_CycleLimit = CPU_CycleLimit;
	CPU_CycleMax = CPU_CycleMax;

	return (jboolean)CPU_CycleAutoAdjust;
}

extern "C"
JNIEXPORT void JNICALL Java_com_fishstix_dosboxfree_DBMain_nativeInit(JNIEnv * env, jobject obj, jobject ctx)
{
	// continue
	const char* utf_string;

	loadf = 0;
	myLoader.memsize = 4;
	MEM_RealMemory = 2;
	myLoader.bmph = 0;
	myLoader.videoBuffer = 0;

	myLoader.abort = 0;
	myLoader.pause = 0;
	myLoader.cycles = 2000;

	myLoader.frameskip = 0;
	myLoader.soundEnable = 1;
	myLoader.cycleHack = 1;
	myLoader.refreshHack = 1;
}

extern "C"
JNIEXPORT void JNICALL Java_com_fishstix_dosboxfree_DBMain_nativePause(JNIEnv * env, jobject obj, jint state)
{
	if ((state == 0) || (state == 1))
		myLoader.pause = state;
	else
		myLoader.pause = (myLoader.pause)?0:1;
}

extern "C"
JNIEXPORT void JNICALL Java_com_fishstix_dosboxfree_DBMain_nativeStop(JNIEnv * env, jobject obj)
{
	myLoader.abort = 1;
}

extern "C"
JNIEXPORT void JNICALL Java_com_fishstix_dosboxfree_DBMain_nativeShutDown(JNIEnv * env, jobject obj)
{
	myLoader.bmph = 0;
	myLoader.videoBuffer = 0;
}
