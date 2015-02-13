/*
 *  Copyright (C) 2012 Fishstix (Gene Ruebsamen - ruebsamen.gene@gmail.com)
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

package com.fishstix.dosboxfree.touchevent;

import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceView;


public abstract class TouchEventWrapper {    
	public abstract int getPointerId(MotionEvent event, int pointerIndex);
	public abstract int getPointerCount(MotionEvent event);
	public abstract float getX(MotionEvent event, int id);
	public abstract float getY(MotionEvent event, int id);
	public abstract int getButtonState(MotionEvent event);
	public abstract int getSource(MotionEvent event);
	public abstract boolean onGenericMotionEvent(SurfaceView view, MotionEvent event);
	public abstract int[] getDeviceIds();
	public abstract int findPointerIndex(MotionEvent event, int pointerId);
	
	public static final int BUTTON_PRIMARY = 1; 
	public static final int BUTTON_SECONDARY = 2; 
	public static final int SOURCE_CLASS_JOYSTICK = 16;
	public static final int SOURCE_CLASS_POINTER = 2;
	public static final int SOURCE_CLASS_TRACKBALL = 4;
	public static final int SOURCE_CLASS_MASK = 255;
	
	public static final int ACTION_POINTER_DOWN = 5; 
	public static final int ACTION_POINTER_UP = 6; 
	public static final int ACTION_HOVER_MOVE = 7; 
	
	public static final int KEYCODE_CTRL_LEFT = 113;
	public static final int KEYCODE_CTRL_RIGHT = 114;
	// fishtix, XPERIA PLAY KEYCODES
	public static final int KEYCODE_BUTTON_X = 99;
	public static final int KEYCODE_BUTTON_Y = 100;
	public static final int KEYCODE_BUTTON_SELECT = 109;
	public static final int KEYCODE_BUTTON_START = 108;
	public static final int KEYCODE_BUTTON_L1 = 102;
	public static final int KEYCODE_BUTTON_L2 = 103;
	// fishstix, XBOX Keycodes
	public static final int KEYCODE_BUTTON_A = 96;
	public static final int KEYCODE_BUTTON_B = 97;
	public static final int KEYCODE_BUTTON_THUMBL = 106; 
	public static final int KEYCODE_BUTTON_THUMBR = 107;
	// fishstix, USB HID
	public static final int KEYCODE_BUTTON_1 = 188;
	public static final int KEYCODE_BUTTON_2 = 189;
	public static final int KEYCODE_BUTTON_3 = 190;
	public static final int KEYCODE_BUTTON_4 = 191;
	public static final int KEYCODE_BUTTON_5 = 192;
	public static final int KEYCODE_BUTTON_6 = 193;
	public static final int KEYCODE_BUTTON_7 = 194;
	public static final int KEYCODE_BUTTON_8 = 195;
	public static final int KEYCODE_BUTTON_9 = 196;
	public static final int KEYCODE_BUTTON_10 = 197;
	public static final int KEYCODE_BUTTON_11 = 198;
	public static final int KEYCODE_BUTTON_12 = 199;
	
	public static final int KEYCODE_ESCAPE = 111;
	public static final int KEYCODE_FORWARD_DEL = 112;
	public static final int KEYCODE_SCROLL_LOCK = 116;
	public static final int KEYCODE_PAUSE_BREAK = 121;
	public static final int KEYCODE_INSERT = 124;
	
	
	public static TouchEventWrapper newInstance() {
	    final int sdkVersion = Build.VERSION.SDK_INT;
	    TouchEventWrapper touchevent = null;
	    if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
	        touchevent = new CupcakeTouchEvent();
	    } else if (sdkVersion < Build.VERSION_CODES.GINGERBREAD){
	        touchevent = new FroyoTouchEvent();
	    } else if (sdkVersion < Build.VERSION_CODES.HONEYCOMB_MR1){
	        touchevent = new GingerbreadTouchEvent();
	    } else if (sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH){
	        touchevent = new HoneycombTouchEvent();
	    } else {
	    	touchevent = new ICSTouchEvent();
	    }

	    return touchevent;
	}
}