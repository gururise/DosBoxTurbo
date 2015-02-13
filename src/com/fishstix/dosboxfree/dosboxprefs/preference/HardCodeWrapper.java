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
package com.fishstix.dosboxfree.dosboxprefs.preference;

import com.fishstix.dosboxfree.dosboxprefs.preference.HardCodeWrapper;
import com.fishstix.dosboxfree.dosboxprefs.preference.wrapper.CupCakeKeyEvent;
import com.fishstix.dosboxfree.dosboxprefs.preference.wrapper.GingerbreadEvent;
import com.fishstix.dosboxfree.dosboxprefs.preference.wrapper.ICSKeyEvent;

import android.os.Build;

public abstract class HardCodeWrapper {
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
	// handle dpad diagonals (gamepad)
	public static final int KEYCODE_DPAD_UP_LEFT = 504;
	public static final int KEYCODE_DPAD_UP_RIGHT = 506;
	public static final int KEYCODE_DPAD_DOWN_RIGHT = 508;
	public static final int KEYCODE_DPAD_DOWN_LEFT = 510;
	// keycodes for virtual buttons
	public static final int KEYCODE_VIRTUAL_A = 3920;
	public static final int KEYCODE_VIRTUAL_B = 3921;
	public static final int KEYCODE_VIRTUAL_C = 3922;
	public static final int KEYCODE_VIRTUAL_D = 3923;
	public abstract String hardCodeToString(int keycode);
	public abstract int[] getDeviceIds();

	public static HardCodeWrapper newInstance() {
	    final int sdkVersion = Build.VERSION.SDK_INT;
	    HardCodeWrapper keyevent = null;
	    if (sdkVersion < Build.VERSION_CODES.GINGERBREAD) {
	    	keyevent = new CupCakeKeyEvent();
	    }
	    else if (sdkVersion < Build.VERSION_CODES.HONEYCOMB_MR1){
	        keyevent = new GingerbreadEvent();
	    } else {
	    	keyevent = new ICSKeyEvent();
	    }

	    return keyevent;
	}

}
