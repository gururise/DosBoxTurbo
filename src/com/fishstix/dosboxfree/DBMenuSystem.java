/*
 *  Copyright (C) 2012 Fishstix (ruebsamen.gene@gmail.com)
 *  
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

package com.fishstix.dosboxfree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.dosboxprefs.preference.GamePreference;
import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class DBMenuSystem {
	private static final String mPrefCycleString = "max";	// default slow system
	private static final Uri CONTENT_URI=Uri.parse("content://com.fishstix.dosboxlauncher.files/");
	private final static int JOYSTICK_CENTER_X = 0;
	private final static int JOYSTICK_CENTER_Y = 0;

	public static final int KEYCODE_F1 = 131;
	//public static final int KEYCODE_F12 = 142;

	public final static int CONTEXT_MENU_SPECIAL_KEYS = 1;
	public final static int CONTEXT_MENU_CYCLES = 2;
	public final static int CONTEXT_MENU_FRAMESKIP = 3;
	public final static int CONTEXT_MENU_MEMORY_SIZE = 4;
	public final static int CONTEXT_MENU_TRACKING = 5;
	public final static int CONTEXT_MENU_INPUTMODE = 6;
	 
	private final static int MENU_KEYBOARD_CTRL = 61;
	private final static int MENU_KEYBOARD_ALT = 62;
	private final static int MENU_KEYBOARD_SHIFT = 63;
	
	private final static int MENU_KEYBOARD_ESC = 65;
	private final static int MENU_KEYBOARD_TAB = 66;
	private final static int MENU_KEYBOARD_DEL = 67;
	private final static int MENU_KEYBOARD_INSERT = 68;
	private final static int MENU_KEYBOARD_PAUSE_BREAK = 82;
	private final static int MENU_KEYBOARD_SCROLL_LOCK = 83;

	private final static int MENU_KEYBOARD_F1 = 70;
	private final static int MENU_KEYBOARD_F12 = 81;
	private final static int MENU_KEYBOARD_SWAP_MEDIA = 91;
	private final static int MENU_KEYBOARD_TURBO = 92;
	
	private final static int MENU_CYCLE_AUTO = 150;
	private final static int MENU_CYCLE_55000 = 205;
	
	private final static int MENU_TRACKING_ABS = 220;
	private final static int MENU_TRACKING_REL = 221;
	
	private final static int MENU_FRAMESKIP_0 = 206;
	private final static int MENU_FRAMESKIP_10 = 216;

	private final static String PREF_KEY_FRAMESKIP = "dosframeskip";
	private final static String PREF_KEY_CYCLES = "doscycles";
	//private final static String PREF_KEY_KEY_MAPPING = "pref_key_key_mapping"; 
	
	public final static int INPUT_MOUSE = 0;
	public final static int INPUT_JOYSTICK = 1;
	public final static int INPUT_REAL_MOUSE = 2;
	public final static int INPUT_REAL_JOYSTICK = 3;
	public final static int INPUT_SCROLL = 4;
	
	//following must sync with AndroidOSfunc.cpp
	public final static int DOSBOX_OPTION_ID_SOUND_MODULE_ON = 1;
	public final static int DOSBOX_OPTION_ID_MEMORY_SIZE = 2;
	public final static int DOSBOX_OPTION_ID_CYCLES = 10;
	public final static int DOSBOX_OPTION_ID_FRAMESKIP = 11;
	public final static int DOSBOX_OPTION_ID_REFRESH_HACK_ON = 12;
	public final static int DOSBOX_OPTION_ID_CYCLE_HACK_ON = 13;
	public final static int DOSBOX_OPTION_ID_MIXER_HACK_ON = 14;
	public final static int DOSBOX_OPTION_ID_AUTO_CPU_ON = 15;
	public final static int DOSBOX_OPTION_ID_TURBO_ON = 16;
	public final static int DOSBOX_OPTION_ID_CYCLE_ADJUST = 17;
	public final static int DOSBOX_OPTION_ID_JOYSTICK_ENABLE = 18;
	public final static int DOSBOX_OPTION_ID_GLIDE_ENABLE = 19;
	public final static int DOSBOX_OPTION_ID_SWAP_MEDIA = 21;
	public final static int DOSBOX_OPTION_ID_START_COMMAND = 50;
	
	static public void loadPreference(DBMain context, final SharedPreferences prefs) {	
		// gracefully handle upgrade from previous versions, fishstix
		/*if (Integer.valueOf(prefs.getString("confcontroller", "-1")) >= 0) {
			DosBoxPreferences.upgrade(prefs);
		}*/
		Runtime rt = Runtime.getRuntime();
		long maxMemory = rt.maxMemory();
		Log.v("DosBoxTurbo", "maxMemory:" + Long.toString(maxMemory));
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		int memoryClass = am.getMemoryClass();
		Log.v("DosBoxTurbo", "memoryClass:" + Integer.toString(memoryClass));
		int maxMem = (int) Math.max(maxMemory/1024, memoryClass) * 4;
		if (!prefs.getBoolean("dosmanualconf", false)) {  // only write conf if not in manual config mode
			// Build DosBox config
			// Set Application Prefs
			PrintStream out;
			InputStream myInput; 
			try {
				myInput = context.getAssets().open(DosBoxPreferences.CONFIG_FILE);
				Scanner scanner = new Scanner(myInput);
				out = new PrintStream(new FileOutputStream(context.mConfPath+context.mConfFile));
				// Write text to file
				out.println("[dosbox]");
				if (Integer.valueOf(prefs.getString("dosmemsize", "8")) < maxMem) { 
					out.println("memsize="+prefs.getString("dosmemsize", "8"));
				} else {
					out.println("memsize="+maxMem);
				}
				out.println("vmemsize=4");
				out.println("machine="+prefs.getString("dosmachine", "svga_s3"));
				out.println();
				out.println("[render]");
				out.println("frameskip="+prefs.getString("dosframeskip","2"));
				out.println();
				out.println("[cpu]");
				//if (DBMain.nativeGetCPUFamily() == 3) { // mips cpu - disable dynamic core
				//	out.println("core=normal");					
				//} else {
					out.println("core="+prefs.getString("doscpu", "dynamic"));
				//}
				out.println("cputype="+prefs.getString("doscputype", "auto"));
				if (prefs.getString("doscycles", "-1").contentEquals("-1")) {
					out.println("cycles="+mPrefCycleString);	// auto performance
				} else {
					out.println("cycles="+prefs.getString("doscycles", "3000"));
				}
				out.println("cycleup=500");
				out.println("cycledown=500");
				out.print("isapnpbios=");
				if (prefs.getBoolean("dospnp", true)) {
					out.println("true");
				} else {
					out.println("false");
				}				
				out.println();
				out.println("[sblaster]");
				out.println("sbtype=" + prefs.getString("dossbtype","sb16"));
				out.println("mixer=true");
				out.println("oplmode=auto");
				out.println("oplemu=fast");
				out.println("oplrate=" + prefs.getString("dossbrate", "22050"));
				out.println();
				out.println("[mixer]");
				try {
					out.println("prebuffer=" + prefs.getInt("dosmixerprebuffer", 15));
				} catch (Exception e) {
					out.println("prebuffer=15");
				}
				out.println("rate=" + prefs.getString("dossbrate", "22050"));
				out.println("blocksize=" + prefs.getString("dosmixerblocksize", "1024"));
				out.println();
				out.println("[dos]");
				out.print("xms=");
				if (prefs.getBoolean("dosxms", true)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.print("ems=");
				if (prefs.getBoolean("dosems", true)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.print("umb=");
				if (prefs.getBoolean("dosumb", true)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.println("keyboardlayout="+prefs.getString("doskblayout", "auto"));
				out.println();
				out.println("[ipx]");
				out.print("ipx=");
				if (prefs.getBoolean("dosipx", false)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.println();
				out.println("[joystick]");
				out.println("joysticktype=2axis");
				out.print("timed=");
				if (prefs.getBoolean("dostimedjoy", false)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.println();
				out.println("[midi]");
				if (prefs.getBoolean("dosmt32", false)) {
					out.println("mpu401=intelligent");
					out.println("mididevice=mt32");
					out.println("mt32.thread=on");
					out.println("mt32.verbose=off");
				} else {
					out.println("mpu401=none");
					out.println("mididevice=none");					
				}
				out.println();
				out.println("[speaker]");
				out.print("pcspeaker=");
				if (prefs.getBoolean("dospcspeaker", false)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.println("tandyrate=" + prefs.getString("dossbrate", "22050"));

				// concat dosbox conf
				while (scanner.hasNextLine()){
					out.println(scanner.nextLine());
				}
				// handle autoexec
				if (prefs.getString("dosautoexec","-1").contains("-1")) {
					out.println("mount c: "+DosBoxPreferences.getExternalDosBoxDir(context)+" \nc:");
				} else {
					out.println(prefs.getString("dosautoexec", "mount c: "+DosBoxPreferences.getExternalDosBoxDir(context)+" \nc:"));
				}
				out.flush();
				out.close();
				myInput.close();
				scanner.close();
				Log.i("DosBoxTurbo","finished writing: "+ context.mConfPath+context.mConfFile);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
		// SCALE SCREEN
		context.mSurfaceView.mScale = prefs.getBoolean("confscale", false);
		
		if (Integer.valueOf(prefs.getString("confscalelocation", "0")) == 0)
			context.mSurfaceView.mScreenTop = false;
		else 
			context.mSurfaceView.mScreenTop = true;
		
		// SCREEN SCALE FACTOR
		context.mPrefScaleFactor = prefs.getInt("confresizefactor", 100);
		
		// SCALE MODE
		if (Integer.valueOf(prefs.getString("confscalemode", "0"))==0) {
			context.mPrefScaleFilterOn = false;
		} else {
			context.mPrefScaleFilterOn = true;
		}
		 
		// ASPECT Ratio 
		context.mSurfaceView.mMaintainAspect = prefs.getBoolean("confkeepaspect", true);
		  
		// SET Cycles
		if (!prefs.getBoolean("dosmanualconf", false)) {
			try {
				DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_CYCLES, Integer.valueOf(prefs.getString("doscycles", "5000")),null, true);
			} catch (NumberFormatException e) {
				// set default to 5000 cycles on exception
				DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_CYCLES, 2000 ,null, true);
			}
		}
		
	/*	if (!DBMain.mLicenseResult || !DBMain.mSignatureResult) {
			prefs.edit().putString("doscycles", "2000").commit();
			DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_CYCLES, 5000 ,null, DBMain.getLicResult());			
		} */
		
		
		// Set Frameskip
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_FRAMESKIP, Integer.valueOf(prefs.getString("dosframeskip", "2")),null, true);		
		
		// TURBO CYCLE
		DBMain.nativeSetOption(DOSBOX_OPTION_ID_CYCLE_HACK_ON, prefs.getBoolean("confturbocycle", false)?1:0,null,true);
		// TURBO VGA
		DBMain.nativeSetOption(DOSBOX_OPTION_ID_REFRESH_HACK_ON, prefs.getBoolean("confturbovga", false)?1:0,null,true);
		// TURBO AUDIO
		context.mPrefMixerHackOn = prefs.getBoolean("confturbomixer", true);
		DBMain.nativeSetOption(DOSBOX_OPTION_ID_MIXER_HACK_ON, context.mPrefMixerHackOn?1:0,null,true);
		// 3DFX (GLIDE) EMULATION
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_GLIDE_ENABLE, prefs.getBoolean("dosglide", false)?1:0,null, true);
		// SOUND
		context.mPrefSoundModuleOn = prefs.getBoolean("confsound", true);
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_SOUND_MODULE_ON, context.mPrefSoundModuleOn?1:0,null,true);
		// AUTO CPU 
		//context.mPrefAutoCPUOn = prefs.getBoolean("dosautocpu", false);  
		//DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_AUTO_CPU_ON, context.mPrefAutoCPUOn?1:0,null,DBMain.getLicResult());
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_AUTO_CPU_ON, 0,null,true);

		// INPUT MODE
		switch (Integer.valueOf(prefs.getString("confinputmode", "0"))) { 
		case INPUT_MOUSE:
			context.mSurfaceView.mInputMode = DBGLSurfaceView.INPUT_MODE_MOUSE;
			break;
		case INPUT_JOYSTICK:
			context.mSurfaceView.mInputMode = DBGLSurfaceView.INPUT_MODE_JOYSTICK;
			DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 1 ,null, true);
			break;
		case INPUT_REAL_MOUSE:
			context.mSurfaceView.mInputMode = DBGLSurfaceView.INPUT_MODE_REAL_MOUSE;
			break;
		case INPUT_REAL_JOYSTICK:
			context.mSurfaceView.mInputMode = DBGLSurfaceView.INPUT_MODE_REAL_JOYSTICK;
			DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 1 ,null, true);
			break;
		case INPUT_SCROLL:
			context.mSurfaceView.mInputMode = DBGLSurfaceView.INPUT_MODE_SCROLL;
			break;
		}

		// VIRTUAL JOYSTICK
		// test enabled
		if (prefs.getBoolean("confjoyoverlay", false)) {
			context.mHandler.sendMessage(context.mHandler.obtainMessage(DBMain.HANDLER_ADD_JOYSTICK,0,0));
		} else {
			context.mHandler.sendMessage(context.mHandler.obtainMessage(DBMain.HANDLER_REMOVE_JOYSTICK,0,0));			
		}
		// size & transparency & mode of joystick
		int joysize = prefs.getInt("confjoysize", 5);
		context.mJoystickView.setSize(joysize);
		LayoutParams params = context.mJoystickView.getLayoutParams();
		//Log.i("DosBoxTurbo","Joysize: " + (int)(175+((joysize-5)*5)));
		params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (int)(175+((joysize-5)*5)), context.getResources().getDisplayMetrics());
		context.mJoystickView.setTransparency(prefs.getInt("confjoytrans", 160)); 
		context.mJoystickView.invalidate();
		
		// Joystick Center
		context.mSurfaceView.mJoyCenterX = (prefs.getInt("confjoyx", 100)-100)+JOYSTICK_CENTER_X;
		context.mSurfaceView.mJoyCenterY = (prefs.getInt("confjoyy", 100)-100)+JOYSTICK_CENTER_Y;
		
		// Joystick Mouse Emulation
		if (prefs.getBoolean("confjoymousemode", false)) {
			context.mSurfaceView.mJoyEmuMouse = true;
		} else {
			context.mSurfaceView.mJoyEmuMouse = false;
		}
		
		
		// Mouse Tracking Mode
		if (Integer.valueOf(prefs.getString("confmousetracking", "0")) == 0) {
			// absolute tracking
			context.mSurfaceView.mAbsolute = true;
		} else {
			context.mSurfaceView.mAbsolute = false;
		}
		
		// mouse sensitivity
		context.mSurfaceView.mMouseSensitivityX = ((float)prefs.getInt("confmousesensitivityx", 50)/100f)+0.5f;
		context.mSurfaceView.mMouseSensitivityY = ((float)prefs.getInt("confmousesensitivityy", 50)/100f)+0.5f;
		
		// Absolute Tracking Calibration function
		/*if (prefs.getBoolean("conf_doReset",false)) {
			// reset calibration data
			context.mSurfaceView.mWarpX = 0f;
			context.mSurfaceView.mWarpY = 0f;
			prefs.edit().putBoolean("conf_doReset", false);
			prefs.edit().putBoolean("conf_doCalibrate", false).commit();
		} else if (prefs.getBoolean("conf_doCalibrate", false)) {
			context.mSurfaceView.mCalibrate = true;
			Toast.makeText(context, R.string.abscalibrate, Toast.LENGTH_SHORT).show();
			prefs.edit().putBoolean("conf_doReset", false);
			prefs.edit().putBoolean("conf_doCalibrate", false).commit();
		}*/
		
		//context.mSurfaceView.mWarpX = Float.valueOf(prefs.getString("confwarpX", "0"));
		//context.mSurfaceView.mWarpY = Float.valueOf(prefs.getString("confwarpY", "0"));
		
		// Input Resolution
		if (Integer.valueOf(prefs.getString("confinputlatency", "0")) == 0) {
			// absolute tracking
			context.mSurfaceView.mInputLowLatency = false;
		} else {
			context.mSurfaceView.mInputLowLatency = true;
		}
		
		// Emulate Mouse Click
		//context.mSurfaceView.mEmulateClick = prefs.getBoolean("confmousetapclick", false);
		// VOL BUTTONS
		//context.mPrefHardkeyOn = prefs.getBoolean("confvolbuttons", true);

/*
		if (prefs.getBoolean("confbuttonoverlay", false)) {
			context.mSurfaceView.mShowInfo = true;
		} else {
			context.mSurfaceView.mShowInfo = false;
		}
	*/	
		if (prefs.getBoolean("confbuttonoverlay", false)) {
			context.mHandler.sendMessage(context.mHandler.obtainMessage(DBMain.HANDLER_ADD_BUTTONS,0,0));
		} else {
			context.mHandler.sendMessage(context.mHandler.obtainMessage(DBMain.HANDLER_REMOVE_BUTTONS,0,0));			
		}
		
		// enable/disable genericmotionevent handling for analog sticks
		//context.mSurfaceView.mGenericMotion = prefs.getBoolean("confgenericmotion", false);
		context.mSurfaceView.mAnalogStickPref = Short.valueOf(prefs.getString("confanalogsticks", "0"));

		// dpad / trackpad emulation
		context.mSurfaceView.mEnableDpad = prefs.getBoolean("confenabledpad", false);
		try {
			int tmp = Integer.valueOf(prefs.getString("confdpadsensitivity", "7").trim());
			if ((tmp >= 1) && (tmp <= 25)) {
				context.mSurfaceView.mDpadRate = tmp;
			} else {
				context.mSurfaceView.mDpadRate = 7;
			}
		} catch (NumberFormatException e) {
			context.mSurfaceView.mDpadRate = 7;
		}
		
		// OS 2.1 - 2.3 < > key fix
		//context.mSurfaceView.mEnableLTKeyFix = prefs.getBoolean("conffixgingerkey", false);
		
		
		// Add custom mappings to ArrayList 
		//context.mSurfaceView.customMapList.clear();
		context.mSurfaceView.customMap.clear();
		for (short i=0;i<DosBoxPreferences.NUM_USB_MAPPINGS;i++) {
			int hardkey = Integer.valueOf(prefs.getString("confmap_custom"+String.valueOf(i)+GamePreference.HARDCODE_KEY, "-1"));
			if ( hardkey > 0) {
				int doskey = Integer.valueOf(prefs.getString("confmap_custom"+String.valueOf(i)+GamePreference.DOSCODE_KEY, "-1"));
				if (doskey > 0) {
					context.mSurfaceView.customMap.put(hardkey,doskey);
				}
			}
		}
		Log.i("DosBoxTurbo","Found " + context.mSurfaceView.customMap.size() + " custom mappings.");
		
		// Sliding Menu Style
		if (prefs.getString("confslidingmenu", "0").contains("0")) {
			context.getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		} else {
			context.getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
		}
		
		// GESTURES
		context.mSurfaceView.mGestureUp = Short.valueOf(prefs.getString("confgesture_swipeup", "0"));
		context.mSurfaceView.mGestureDown = Short.valueOf(prefs.getString("confgesture_swipedown", "0"));
		
		// TOUCHSCREEN MOUSE
		context.mSurfaceView.mGestureSingleClick = Short.valueOf(prefs.getString("confgesture_singletap", "3"));
		context.mSurfaceView.mGestureDoubleClick = Short.valueOf(prefs.getString("confgesture_doubletap", "5"));
		context.mSurfaceView.mGestureTwoFinger = Short.valueOf(prefs.getString("confgesture_twofinger", "0"));
		context.mSurfaceView.mLongPress = prefs.getBoolean("confgesture_longpress", true);
		
		// FORCE Physical LEFT ALT
		context.mSurfaceView.mUseLeftAltOn = prefs.getBoolean("confaltfix", false);
		
		// SOUND
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_SOUND_MODULE_ON, (prefs.getBoolean("confsound", true))?1:0,null,true);
		// DEBUG
		context.mSurfaceView.mDebug = prefs.getBoolean("confdebug", false);
		
		if (context.mSurfaceView.mDebug) {
			// debug mode enabled, show warning
			Toast.makeText(context, R.string.debug, Toast.LENGTH_LONG).show();
		}
		
		context.mSurfaceView.forceRedraw();
	}
	


	static public void copyConfigFile(DBMain context) {
		try {
		      
			InputStream myInput = new FileInputStream(context.mConfPath + context.mConfFile);
			myInput.close();
			myInput = null;
		}
		catch (FileNotFoundException f) {
			try {
		    	InputStream myInput = context.getAssets().open(context.mConfFile);
		    	OutputStream myOutput = new FileOutputStream(context.mConfPath + context.mConfFile);
		    	byte[] buffer = new byte[1024];
		    	int length;
		    	while ((length = myInput.read(buffer))>0){
		    		myOutput.write(buffer, 0, length);
		    	}
		    	myOutput.flush();
		    	myOutput.close();
		    	myInput.close();
			} catch (IOException e) {
			}
		} catch (IOException e) {
		}
    }	
	
	
	static public void savePreference(DBMain context, String key, String value) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (sharedPrefs != null) {
			SharedPreferences.Editor editor = sharedPrefs.edit();			
			if (editor != null) {		
				//if (PREF_KEY_REFRESH_HACK_ON.equals(key)) {		
				//	editor.putBoolean(PREF_KEY_REFRESH_HACK_ON, context.mPrefRefreshHackOn);
				//}
				editor.putString(key, value);

				editor.commit();
			}
		}		
	} 
	
	static public void saveBooleanPreference(Context context, String key, boolean value) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (sharedPrefs != null) {
			SharedPreferences.Editor editor = sharedPrefs.edit();
		
			if (editor != null) {
				editor.putBoolean(key, value);
				editor.commit();
			}
		}				
	}
	
	static public boolean getBooleanPreference(Context context, String key) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPrefs.getBoolean(key, false);
	}
	
	/*
	static public boolean doCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_KEYBOARD, 0, "Keyboard").setIcon(R.drawable.ic_menu_keyboard);
		menu.add(Menu.NONE, MENU_INPUT_INPUT_METHOD, 0, "Input Method").setIcon(R.drawable.ic_menu_flag);
		menu.add(Menu.NONE, MENU_KEYBOARD_SPECIAL, 0, "Special Keys").setIcon(R.drawable.ic_menu_flash);
		menu.add(Menu.NONE, MENU_SETTINGS_SCALE, 0, "Scale: Off").setIcon(R.drawable.ic_menu_resize);
		menu.add(Menu.NONE, MENU_PREFS,Menu.NONE,"Config").setIcon(R.drawable.ic_menu_settings);
		menu.add(Menu.NONE, MENU_QUIT, 0, "Exit").setIcon(R.drawable.ic_menu_close_clear_cancel);
		return true;		
	}*/
	
	static public boolean doPrepareOptionsMenu(DBMain context, Menu menu) {
		//menu.findItem(MENU_SETTINGS_SCALE).setTitle((context.mSurfaceView.mScale)?"Scale: On":"Scale: Off");
		menu.findItem(R.id.menu_scale).setTitle((context.mSurfaceView.mScale)?"Scale: On":"Scale: Off");
		return true;
	}
	
	static public void doShowMenu(DBMain context) {
		context.openOptionsMenu();
	}

	static public void doHideMenu(DBMain context) {
		context.closeOptionsMenu();
	}
	
	static public void doShowKeyboard(DBMain context) {
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			if (!context.mSurfaceView.hasFocus()){ 
		        context.mSurfaceView.requestFocus();
			}
			imm.showSoftInput(context.mSurfaceView, 0);
		}
	}

	static public void doHideKeyboard(DBMain context) {
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow(context.mSurfaceView.getWindowToken(),0);
	}
	
	static public void doConfirmQuit(final DBMain context) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle(R.string.app_name);
		builder.setMessage("Exit DosBox?");
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				context.stopDosBox();
			}
		});
		builder.setNegativeButton("Cancel", null);				
		builder.create().show();		
	}
	
	static public void doShowTextDialog(final DBMain context, String message) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle(R.string.app_name);
		builder.setMessage(message);
		
		builder.setPositiveButton("OK", null);

		builder.create().show();		
	}
	/*
	static public void doShowHideInfo(DBMain context, boolean show) {
		context.mSurfaceView.mInfoHide = show;
		context.mSurfaceView.forceRedraw();
	}*/
	
	static public boolean doOptionsItemSelected(DBMain context, MenuItem item)
	{
		switch(item.getItemId()){
			case R.id.menu_exit:
				doConfirmQuit(context);
			    break;
			case R.id.menu_inputmethod:
			{
				InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null)
					imm.showInputMethodPicker();
			}
				break;

			case R.id.menu_specialkeys:
				context.mSurfaceView.mContextMenu = CONTEXT_MENU_SPECIAL_KEYS;				
				context.openContextMenu(context.mSurfaceView);
				break;
			case R.id.menu_keyboard:
				if (context.mSurfaceView.mKeyboardVisible)
					doHideKeyboard(context);
				else 
					doShowKeyboard(context);
				break;
			case R.id.menu_joystick:
				if (!getBooleanPreference(context,"confjoyoverlay")) {
					context.mHandler.sendMessage(context.mHandler.obtainMessage(DBMain.HANDLER_ADD_JOYSTICK,0,0));
				} else {
					context.mHandler.sendMessage(context.mHandler.obtainMessage(DBMain.HANDLER_REMOVE_JOYSTICK,0,0));
				}
				break;
			case R.id.menu_scale: 
				context.mSurfaceView.mScale = !context.mSurfaceView.mScale;
				saveBooleanPreference(context, "confscale",context.mSurfaceView.mScale);
				context.bScaling.setChecked(context.mSurfaceView.mScale);
				context.mSurfaceView.forceRedraw();
				break;
			case R.id.menu_settings:
				if (context.mPID != null) {
					Intent i = new Intent(context, DosBoxPreferences.class);
					Bundle b = new Bundle();
					b.putString("com.fishstix.dosboxlauncher.pid", context.mPID);
					b.putBoolean("com.fishstix.dosboxlauncher.mlic", true);
					i.putExtras(b);
					context.startActivity(i);
				} else {
					Intent i = new Intent(context, DosBoxPreferences.class);
					Bundle b = new Bundle();
					b.putBoolean("com.fishstix.dosboxlauncher.mlic", true);
					i.putExtras(b);
					context.startActivity(i);
				}
				break;
			default:
				break;
		  }
		  return true;
	}
	
	static public void doCreateContextMenu(DBMain context, ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		switch (context.mSurfaceView.mContextMenu) {
			case CONTEXT_MENU_SPECIAL_KEYS:
			{
				android.view.MenuItem item;
				item = menu.add(0, MENU_KEYBOARD_CTRL, 0, "Ctrl");
				item.setCheckable(true);
				item.setChecked(context.mSurfaceView.mModifierCtrl);

				item = menu.add(0, MENU_KEYBOARD_ALT, 0, "Alt");
				item.setCheckable(true);
				item.setChecked(context.mSurfaceView.mModifierAlt);
						
				item = menu.add(0, MENU_KEYBOARD_SHIFT, 0, "Shift");
				item.setCheckable(true);
				item.setChecked(context.mSurfaceView.mModifierShift);

				menu.add(0, MENU_KEYBOARD_ESC, 0, "ESC");
				menu.add(0, MENU_KEYBOARD_TAB, 0, "Tab");
				menu.add(0, MENU_KEYBOARD_DEL, 0, "Del");
				menu.add(0, MENU_KEYBOARD_INSERT, 0, "Ins");
				menu.add(0, MENU_KEYBOARD_PAUSE_BREAK, 0, "Break");
				menu.add(0, MENU_KEYBOARD_SCROLL_LOCK,0,"Scrl. Lck");
				
				for(int i = MENU_KEYBOARD_F1; i <= MENU_KEYBOARD_F12; i++)
					menu.add(0, i, 0, "F"+(i-MENU_KEYBOARD_F1+1));	

				menu.add(0, MENU_KEYBOARD_SWAP_MEDIA, 0, "Swap Media");
				context.mSurfaceView.mContextMenu = -1;

				item = menu.add(0, MENU_KEYBOARD_TURBO, 0, "Fast Forward");
				item.setCheckable(true);
				item.setChecked(context.mTurboOn);
			}
				break;
			case CONTEXT_MENU_CYCLES:
			{
				
				android.view.MenuItem item = menu.add(1, MENU_CYCLE_AUTO, 0, "Auto");
				if (DosBoxControl.nativeGetAutoAdjust()) {
					item.setChecked(true);
				}
				for(int i = MENU_CYCLE_AUTO+1; i <= MENU_CYCLE_55000; i++) {
					int value = (i-MENU_CYCLE_AUTO) * 1000;
					 item = menu.add(1, i, 0, ""+value);
					
					if (!DosBoxControl.nativeGetAutoAdjust() && (value == DosBoxControl.nativeGetCycleCount())) {
						item.setChecked(true);
					}
				}
				
				menu.setGroupCheckable(1, true, true);
			}
				break;
			case CONTEXT_MENU_FRAMESKIP:
			{
				for(int i = MENU_FRAMESKIP_0; i <= MENU_FRAMESKIP_10; i++) {
					int value = (i-MENU_FRAMESKIP_0);
					android.view.MenuItem item = menu.add(2, i, 0, ""+value);

					if (value == DosBoxControl.nativeGetFrameSkipCount()) {
						item.setChecked(true);
					}
				}
				menu.setGroupCheckable(2, true, true);
			}
			break;
			case CONTEXT_MENU_TRACKING:
			{
				android.view.MenuItem item = menu.add(3, MENU_TRACKING_ABS, 0, "Absolute");
				android.view.MenuItem item2 = menu.add(3, MENU_TRACKING_REL, 0, "Relative");
				if (context.mSurfaceView.mAbsolute) {
					item.setChecked(true);
				} else {
					item2.setChecked(true);
				}
				menu.setGroupCheckable(3, true, true);
			}
			break;
			case CONTEXT_MENU_INPUTMODE:
			{
				for(int i = INPUT_MOUSE; i <= INPUT_SCROLL; i++) {
					android.view.MenuItem item;
					switch (i) {
					case INPUT_MOUSE:
						item = menu.add(4, i, 0, context.getString(R.string.input_touchscreen));
						if (context.mSurfaceView.mInputMode == DBGLSurfaceView.INPUT_MODE_MOUSE) {
							item.setChecked(true);
						}
					break;
					case INPUT_REAL_MOUSE:
						item = menu.add(4, i, 0, context.getString(R.string.input_mouse));
						if (context.mSurfaceView.mInputMode == DBGLSurfaceView.INPUT_MODE_REAL_MOUSE) {
							item.setChecked(true);
						}
					break;
					case INPUT_REAL_JOYSTICK:
						item = menu.add(4, i, 0, context.getString(R.string.input_joystick));
						if (context.mSurfaceView.mInputMode == DBGLSurfaceView.INPUT_MODE_REAL_JOYSTICK) {
							item.setChecked(true);
						}
					break;
					case INPUT_SCROLL:
						item = menu.add(4, i, 0, context.getString(R.string.input_scroll));
						if (context.mSurfaceView.mInputMode == DBGLSurfaceView.INPUT_MODE_SCROLL) {
							item.setChecked(true);
						}
					break;
					}
				}
				menu.setGroupCheckable(2, true, true);
			}
			break;			
		}
	}
	
	static public void doSendDownUpKey(DBMain context, int keyCode) {
		DosBoxControl.sendNativeKey(keyCode , true, context.mSurfaceView.mModifierCtrl, context.mSurfaceView.mModifierAlt, context.mSurfaceView.mModifierShift);
		DosBoxControl.sendNativeKey(keyCode , false, context.mSurfaceView.mModifierCtrl, context.mSurfaceView.mModifierAlt, context.mSurfaceView.mModifierShift);
		context.mSurfaceView.mModifierCtrl = false;
		context.mSurfaceView.mModifierAlt = false;
		context.mSurfaceView.mModifierShift = false;
	}
	
	static public boolean doContextItemSelected(DBMain context, android.view.MenuItem item) {
		int itemID = item.getItemId();
		
		switch(itemID) {
		case MENU_KEYBOARD_CTRL:
			context.mSurfaceView.mModifierCtrl = !context.mSurfaceView.mModifierCtrl; 
			break;
		case MENU_KEYBOARD_ALT:
			context.mSurfaceView.mModifierAlt = !context.mSurfaceView.mModifierAlt; 
			break;		
		case MENU_KEYBOARD_SHIFT:
			context.mSurfaceView.mModifierShift = !context.mSurfaceView.mModifierShift; 
			break;		
		case MENU_KEYBOARD_TAB:
			doSendDownUpKey(context, KeyEvent.KEYCODE_TAB);
			break;
		case MENU_KEYBOARD_ESC:
			doSendDownUpKey(context, TouchEventWrapper.KEYCODE_ESCAPE);
			break;
		case MENU_KEYBOARD_DEL:
			doSendDownUpKey(context, TouchEventWrapper.KEYCODE_FORWARD_DEL);
			break;
		case MENU_KEYBOARD_INSERT:
			doSendDownUpKey(context, TouchEventWrapper.KEYCODE_INSERT);
			break;
		case MENU_KEYBOARD_PAUSE_BREAK:
			doSendDownUpKey(context, TouchEventWrapper.KEYCODE_PAUSE_BREAK);
			break;
		case MENU_KEYBOARD_SCROLL_LOCK:
			doSendDownUpKey(context, TouchEventWrapper.KEYCODE_SCROLL_LOCK);
			break;
		case MENU_KEYBOARD_TURBO:
			context.mTurboOn = !context.mTurboOn;
			DBMain.nativeSetOption(DOSBOX_OPTION_ID_TURBO_ON, context.mTurboOn?1:0, null,true);
		case MENU_KEYBOARD_SWAP_MEDIA:
			DBMain.nativeSetOption(DOSBOX_OPTION_ID_SWAP_MEDIA, 1,null,true);
			break;
		case MENU_TRACKING_ABS:
			context.mSurfaceView.mAbsolute = true;
			context.iTracking.setText("Absolute");
			savePreference(context, "confmousetracking", "0");
			break;
		case MENU_TRACKING_REL:
			context.mSurfaceView.mAbsolute = false;
			context.iTracking.setText("Relative");
			savePreference(context, "confmousetracking", "1");
			break;
		default:
			if ((itemID >= MENU_KEYBOARD_F1) && (itemID <= MENU_KEYBOARD_F12)) {
				doSendDownUpKey(context, KEYCODE_F1 + (itemID - MENU_KEYBOARD_F1));
			}
			else if ((itemID >= MENU_CYCLE_AUTO) && (itemID <= MENU_CYCLE_55000)) {
				if (context.mTurboOn) { 
					context.mTurboOn = false;
					DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON, context.mTurboOn?1:0, null,true);			
				}
				int cycles = -1;
				if (itemID == MENU_CYCLE_AUTO) {
					cycles = -1;
				} else {
					cycles = (itemID - MENU_CYCLE_AUTO) * 1000;
				}
				savePreference(context, PREF_KEY_CYCLES, String.valueOf(cycles));
				DBMain.nativeSetOption(DOSBOX_OPTION_ID_CYCLES,cycles,null,true);
				if (DosBoxControl.nativeGetAutoAdjust()) {
					context.iCycles.setText("Auto");
					Toast.makeText(context, "Auto Cycles ["+DosBoxControl.nativeGetCycleCount() +"%]", Toast.LENGTH_SHORT).show();
				} else {
					context.iCycles.setText(String.valueOf(DosBoxControl.nativeGetCycleCount()));
					Toast.makeText(context, "DosBox Cycles: "+DosBoxControl.nativeGetCycleCount(), Toast.LENGTH_SHORT).show();
				} 
				
			}
			else if ((itemID >= MENU_FRAMESKIP_0) && (itemID <= MENU_FRAMESKIP_10)) {
				int frameskip = (itemID - MENU_FRAMESKIP_0); 
				savePreference(context, PREF_KEY_FRAMESKIP,String.valueOf(frameskip));
				DBMain.nativeSetOption(DOSBOX_OPTION_ID_FRAMESKIP, frameskip ,null,true);
				context.iFrameSkip.setText(String.valueOf(frameskip));
			} else if ((itemID >= INPUT_MOUSE) && (itemID <= INPUT_SCROLL)) {
				savePreference(context,"confinputmode",String.valueOf(itemID));
				switch (itemID) {
				case INPUT_MOUSE:
					context.mSurfaceView.mInputMode = DBGLSurfaceView.INPUT_MODE_MOUSE;
					context.iInputMode.setText(R.string.input_touchscreen);
					if (!getBooleanPreference(context,"confjoyoverlay")) {
						DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 0 ,null, true);
					}
				break;
				case INPUT_REAL_MOUSE:
					context.mSurfaceView.mInputMode = DBGLSurfaceView.INPUT_MODE_REAL_MOUSE;
					context.iInputMode.setText(R.string.input_mouse);
					if (!getBooleanPreference(context,"confjoyoverlay")) {
						DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 0 ,null, true);
					}
				break;
				case INPUT_REAL_JOYSTICK:
					context.mSurfaceView.mInputMode = DBGLSurfaceView.INPUT_MODE_JOYSTICK;
					context.iInputMode.setText(R.string.input_joystick);
					DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 1 ,null, true);
				break;
				case INPUT_SCROLL:
					context.mSurfaceView.mInputMode = DBGLSurfaceView.INPUT_MODE_SCROLL;
					context.iInputMode.setText(R.string.input_scroll);
					if (!getBooleanPreference(context,"confjoyoverlay")) {
						DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 0 ,null, true);
					}
				break;
				}
			}
			break;
		}
		return true;
	}	
		
	public static void getData(DBMain context, String pid) {
		try {
			 InputStream is = context.getContentResolver().openInputStream(Uri.parse(CONTENT_URI + pid + ".xml"));
			 FileOutputStream fostream;
			 // Samsung workaround:
			 File file = new File("/dbdata/databases/com.fishstix.dosbox/shared_prefs/");
			 if (file.isDirectory() && file.exists()) {
				 // samsung
				 fostream = new FileOutputStream("/dbdata/databases/com.fishstix.dosbox/shared_prefs/"+pid+".xml");
			 } else {
				 // every one else.
				 fostream = new FileOutputStream(context.getFilesDir()+"/../shared_prefs/"+pid+".xml");
			 }

			 PrintStream out = new PrintStream(fostream);
			 Scanner scanner = new Scanner(is);
			 while (scanner.hasNextLine()){
				out.println(scanner.nextLine());
			 }
			 out.flush();
			 is.close();
			 out.close();
			 scanner.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    public static void CopyAsset(DBMain ctx, String assetfile) {
        AssetManager assetManager = ctx.getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
        	in = assetManager.open(assetfile);   // if files resides inside the "Files" directory itself
            out = ctx.openFileOutput(assetfile, Context.MODE_PRIVATE);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch(Exception e) {
             Log.e("DosBoxTurbo", e.getMessage());
        }
    }
    
    public static void CopyROM(DBMain ctx, File infile) {
        InputStream in = null;
        OutputStream out = null;
        try {
        	in = new FileInputStream(infile);   // if files resides inside the "Files" directory itself
            out = ctx.openFileOutput(infile.getName().toUpperCase(Locale.US), Context.MODE_PRIVATE);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch(Exception e) {
             Log.e("DosBoxTurbo", e.getMessage());
        }    	
    }
    
    public static boolean MT32_ROM_exists(DBMain ctx) {
    	File ctrlrom = new File(ctx.getFilesDir(), "MT32_CONTROL.ROM");
    	File pcmrom = new File(ctx.getFilesDir(), "MT32_PCM.ROM");
    	
    	if (ctrlrom.exists() && pcmrom.exists()) {
    		return true;
    	}
    	return false;
    }
    
    public static File openFile(String name) {
    	  File origFile = new File(name);
    	  File dir = origFile.getParentFile();
    	  if (dir.listFiles() != null) {
    		  for (File f : dir.listFiles()) {
    			  if (f.getName().equalsIgnoreCase(origFile.getName())) {
    				  return new File(f.getAbsolutePath());
    			  }
    		  }
    	  }  
		return new File(name);
    }
	
    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
          out.write(buffer, 0, read);
        }
    }
}
