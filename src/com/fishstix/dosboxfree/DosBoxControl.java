/*
 *  Copyright (C) 2012 Fishstix - Based upon DosBox & anDosBox by Locnet
 *  
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
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

package com.fishstix.dosboxfree;

public class DosBoxControl {  
	public static native void nativeMouse(int x, int y, int down_x, int down_y, int action, int button);
	public static native int nativeKey(int keyCode, int down, int ctrl, int alt, int shift);
	public static native void nativeJoystick(int x, int y, int action, int button);
	//public static native void nativeMouseWarp(int x, int y, float xfactor, float yfactor, int src_left, int src_right, int src_top, int src_bottom, int dst_left, int dst_right, int dst_top, int dst_bottom);
	public static native void nativeMouseWarp(float x, float y, int dst_left, int dst_top, int width, int height);
	public static native int nativeGetCycleCount();
	public static native int nativeGetFrameSkipCount();
	public static native int nativeGetMemSize();
	public static native boolean nativeGetAutoAdjust();
	//return true to clear modifier 
	public static boolean sendNativeKey(int keyCode, boolean down, boolean ctrl, boolean alt, boolean shift) {
		return (nativeKey(keyCode, (down)?1:0, (ctrl)?1:0, (alt)?1:0, (shift)?1:0) != 0);
	}
}

