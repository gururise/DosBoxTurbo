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

package com.fishstix.dosboxfree.dosboxprefs;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fishstix.dosboxfree.R;
import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.dosboxprefs.preference.GamePreference;
import com.fishstix.dosboxfree.dosboxprefs.preference.HardCodeWrapper;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.Toast;

public class DosBoxPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener {
	private Preference doscpu = null;
	private Preference doscycles = null;
	private Preference dosframeskip = null;
	private Preference dosmemsize = null;
	private Preference dossbtype = null;
	private Preference dossbrate = null;
	private Preference dosmixerprebuffer = null;
	private Preference dosmixerblocksize = null;
	private Preference doskblayout = null;
	private Preference dosautoexec = null;
	private Preference dosems = null;
	private Preference dosxms = null;
	private Preference dosumb = null;
	private Preference dosipx = null;
	private Preference dospnp = null;
	private Preference dosglide = null;
	private Preference dosmt32 = null;
	private Preference dospcspeaker = null;
	private Preference dostimedjoy = null;
	private Preference dosmachine = null;
	private Preference doscputype = null;
	private Preference dosmanualconf_file = null;
	private Preference doseditconf_file = null;
   // private Preference confmousetapclick = null;
	private Preference confbuttonoverlay = null;
	private Preference confcustom_add = null;
	private Preference confcustom_clear = null;
	private Preference confjoyoverlay = null;
	private Preference confenabledpad = null;
	private Preference confdpadsensitivity = null;
	private Preference confmousetracking = null;
	private PreferenceScreen dpad_mappings = null;
	private Preference confgpu = null;
	private Preference confreset = null;
	private Preference version = null;
	
	//public static final int NUM_KEYBOARD_KEYMAPPINGS = 13;
	public static final int NUM_USB_MAPPINGS = 30; 
	public static final int XPERIA_BACK_BUTTON = 72617;
	
	public static final String CONFIG_FILE = "dosbox.conf";
	public static String CONFIG_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/com.fishstix.dosbox/files/";//"/sdcard/";
	public static String STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Downloads/";//"/sdcard/";
    // mappings
	private GamePreference confmap_custom[] = new GamePreference[NUM_USB_MAPPINGS];
	    
	private PreferenceCategory prefCatOther = null;
    
	private SharedPreferences prefs;
	private static HardCodeWrapper kw = HardCodeWrapper.newInstance();

    private Context ctx = null;
    private static boolean isExperiaPlay = false;
    
    private static final int TOUCHSCREEN_MOUSE = 0;
    private static final int TOUCHSCREEN_JOY = 1;
    private static final int PHYSICAL_MOUSE = 2;
    private static final int PHYSICAL_JOY = 3;
    private static final int SCROLL_SCREEN = 4;

	  @SuppressWarnings("deprecation")
	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.config);
	    ctx = this;
	    STORAGE_PATH = DosBoxPreferences.getExternalDosBoxDir(ctx);
	    if (isExternalStorageWritable()) {
	    	CONFIG_PATH = ctx.getExternalFilesDir(null).getAbsolutePath() + "/";
	    } else {
			CONFIG_PATH =  ctx.getFilesDir().getAbsolutePath()+"/";
		}
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    
	    if (prefs.getString("dosautoexec", "-1").contentEquals("-1")) 
	    	prefs.edit().putString("dosautoexec","mount c: "+STORAGE_PATH+" \nc:").commit();
	    if (prefs.getString("dosmanualconf_file", "-1").contentEquals("-1")) 
	    	prefs.edit().putString("dosmanualconf_file",CONFIG_PATH+CONFIG_FILE).commit();
	    
	    addPreferencesFromResource(R.xml.preferences);
	    doscpu = (Preference) findPreference("doscpu");
	    doscputype = (Preference) findPreference("doscputype");
	    doscycles = (Preference) findPreference("doscycles");
	    dosframeskip = (Preference) findPreference("dosframeskip");
	    dosmemsize = (Preference) findPreference("dosmemsize");
	    dossbtype = (Preference) findPreference("dossbtype");
	    dossbrate = (Preference) findPreference("dossbrate");
	    dosmixerprebuffer = (Preference) findPreference("dosmixerprebuffer");
	    dosmixerblocksize = (Preference) findPreference("dosmixerblocksize");
	    doskblayout = (Preference) findPreference("doskblayout");
	    dosautoexec = (Preference) findPreference("dosautoexec");
	    dospcspeaker = (Preference) findPreference("dospcspeaker");
	    dosmachine = (Preference) findPreference("dosmachine");
	    dostimedjoy = (Preference) findPreference("dostimedjoy");
	    dosxms = (Preference) findPreference("dosxms");
	    dosems = (Preference) findPreference("dosems");
	    dosumb = (Preference) findPreference("dosumb");
	    dosipx = (Preference) findPreference("dosipx");
	    dospnp = (Preference) findPreference("dospnp");
	    dosglide = (Preference) findPreference("dosglide");
	    dosmt32 = (Preference) findPreference("dosmt32");
	    doseditconf_file = (Preference) findPreference("doseditconf_file");
	    confreset = (Preference) findPreference("confreset");
	    confgpu = (Preference) findPreference("confgpu");
	    confreset.setOnPreferenceClickListener(this);
	    dosmanualconf_file = (Preference) findPreference("dosmanualconf_file");
	    //confmousetapclick = (Preference) findPreference("confmousetapclick");
	    confbuttonoverlay = (Preference) findPreference("confbuttonoverlay");
	    confjoyoverlay = (Preference) findPreference("confjoyoverlay");
	    confenabledpad = (Preference) findPreference("confenabledpad");
	    confdpadsensitivity = (Preference) findPreference("confdpadsensitivity");
	    confmousetracking = (Preference) findPreference("confmousetracking");
	    dpad_mappings = (PreferenceScreen) findPreference("dpad_mappings");
	    confcustom_add = (Preference) findPreference("confcustom_add");
	    confcustom_clear = (Preference) findPreference("confcustom_clear");
//	    confmapanalog_mouse = (AnalogPreference) findPreference("confmapanalog_mouse");
//	    confmapanalog_joy = (AnalogPreference) findPreference("confmapanalog_joy");
//	    confmapanalog_mouse.setOnPreferenceClickListener(this);
//	    confmapanalog_joy.setOnPreferenceClickListener(this);
	    version = (Preference) findPreference("version");
	    version.setOnPreferenceClickListener(this);
	    confcustom_add.setOnPreferenceClickListener(this);
	    confcustom_clear.setOnPreferenceClickListener(this);
	    
	    // get Custom Mappings
	    for(short i = 0; i<NUM_USB_MAPPINGS;i++){
	    	confmap_custom[i] = (GamePreference) findPreference("confmap_custom"+String.valueOf(i));
	    }

	    prefCatOther = (PreferenceCategory) findPreference("prefCatOther");
	    InputFilter[] filterArray = new InputFilter[2];
	    filterArray[0] = new InputFilter() { 
	    	@Override
	        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) { 
	            	for (int i = start; i < end; i++) { 
	            		char c = source.charAt(i);
	            		if (!Character.isLetterOrDigit(c)) { 
	            			return ""; 
	                    }
	            		if (Character.isLetter(c)) {
	            			if (!Character.isLowerCase(c)) {
	            				return "";
	            			}
	            		}
	                } 
	            return null; 
	        }
	    };
	    filterArray[1] = new InputFilter.LengthFilter(1);
	    // check for Xperia Play
	    Log.i("DosBoxTurbo", "Build.DEVICE: "+android.os.Build.DEVICE);
	    if (android.os.Build.DEVICE.equalsIgnoreCase("zeus") || android.os.Build.DEVICE.contains("R800"))
	    	isExperiaPlay = true;  
	}
	  	  
	@Override
	public void onResume() {
		super.onResume();
		
		// make updated for dosbox.conf manual mode
		update_dosmanualconf();
		// No Physical Dpad
		//if (getResources().getConfiguration().navigation == Configuration.NAVIGATION_NONAV) {
			// no physical dpad available, hide dpad options
			//dpad_mappings.setEnabled(false);
			//confenabledpad.setEnabled(false);
			//confdpadsensitivity.setEnabled(false);
		    final int sdkVersion = Build.VERSION.SDK_INT;
		/*    if (sdkVersion < Build.VERSION_CODES.GINGERBREAD) {
		    	updateGameController(0);	// show dpad only
		    } else {
		    	updateGameController(Integer.valueOf(prefs.getString("confcontroller", "0")));
		    } */
		//}
		
		// enable/disable settings based upon input mode
		configureInputSettings(Integer.valueOf(prefs.getString("confinputmode", "0")));

		// disable dpad sensitivity when dpad is not enabled
		update_confenabledpad();
		
		
		// update MT32 config

		boolean MTROM_valid = true;
		File rom = new File(getFilesDir().toString() +"/MT32_CONTROL.ROM");
		if (!rom.exists()) {
			MTROM_valid = false;
		} 
		rom = new File(getFilesDir().toString() +"/MT32_PCM.ROM");
		if (!rom.exists()) {
			MTROM_valid = false;
		}
		if (!MTROM_valid) {
			dosmt32.setSummary(R.string.mt32missing);
			dosmt32.setEnabled(false);
		}

		
	    // get the two custom preferences
	    Preference versionPref = (Preference) findPreference("version");
	    Preference helpPref = (Preference) findPreference("help");
	    doseditconf_file.setOnPreferenceClickListener(this);
	    //helpPref.setOnPreferenceClickListener(this);
	    String versionName = "";
	    try {
	    	versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	    versionPref.setSummary(versionName);
	   
	    // update button mapping summary
	    updateMapSummary();
	    prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
	    prefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences preference, String key) {
		//updateMapSummary();
		if (key.contentEquals("dosmanualconf")) {
			update_dosmanualconf();
			Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
		} else if (key.contentEquals("dosmanualconf_file")) {
			dosmanualconf_file.setSummary(preference.getString("dosmanualconf_file",""));
			Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
		} else if (key.contentEquals("confinputmode")) {
			configureInputSettings(Integer.valueOf(preference.getString(key, "0")));
		} else if (key.contentEquals("confenabledpad")) {
			update_confenabledpad();
		} else if (key.contentEquals("doscycles") && prefs.getString("doscycles", "").contentEquals("auto")) {
			// turn on cpuauto and disable it
			Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();	
		} else if ( (key.contentEquals("doscpu")) || (key.contentEquals("dosmemsize")) || (key.contentEquals("dossbtype")) ||
				(key.contentEquals("dosautoexec")) || (key.contentEquals("dossbrate")) || (key.contentEquals("confoptimization")) || 
				(key.contentEquals("doskblayout")) || (key.contentEquals("dosems")) || (key.contentEquals("dosxms")) || (key.contentEquals("dosumb")) || 
				(key.contentEquals("dospcspeaker")) || (key.contentEquals("dosmixerprebuffer")) || (key.contentEquals("dosipx")) || 
				(key.contentEquals("dosmixerblocksize")) || (key.contentEquals("confgpu")) || (key.contentEquals("conftimedjoy")) ||
				(key.contentEquals("dosmachine")) || (key.contentEquals("doscputype")) || (key.contentEquals("dosmt32")) || (key.contentEquals("dospnp")) || 
				(key.contentEquals("dosglide")) )  {
				Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
		} else {
			updateMapSummary();
		}
	}
	
	private void configureInputSettings(int input_mode) {
		switch (input_mode) {
		case TOUCHSCREEN_MOUSE:
			// enable tracking settings
			confmousetracking.setEnabled(true);
			confbuttonoverlay.setEnabled(true);
			//confjoyoverlay.setEnabled(false);
			//confjoyoverlay.getEditor().putBoolean("confjoyoverlay", false).commit();
			break;
		case TOUCHSCREEN_JOY:
			confmousetracking.setEnabled(false);
			confbuttonoverlay.setEnabled(false);
			confjoyoverlay.setEnabled(true);
			confbuttonoverlay.getEditor().putBoolean("confbuttonoverlay", false).commit();
			break;
		case PHYSICAL_MOUSE:
			confmousetracking.setEnabled(true);
			confbuttonoverlay.setEnabled(false);

			confbuttonoverlay.getEditor().putBoolean("confbuttonoverlay", false).commit();
			break;
		case PHYSICAL_JOY:
		case SCROLL_SCREEN:
			confmousetracking.setEnabled(false);
			confbuttonoverlay.setEnabled(false);

			confbuttonoverlay.getEditor().putBoolean("confbuttonoverlay", false).commit();
			break;		
		}		
	}

	private void update_dosmanualconf() {
		String configFile;
		if (prefs.getBoolean("dosmanualconf", false)) {
			doscpu.setEnabled(false);
			doscputype.setEnabled(false);
			doscycles.setEnabled(false);
			dosframeskip.setEnabled(false);
			dosmemsize.setEnabled(false);
			dossbtype.setEnabled(false);
			dossbrate.setEnabled(false);
			dosmt32.setEnabled(false);
			dosmachine.setEnabled(false);
			dostimedjoy.setEnabled(false);
			dosmixerprebuffer.setEnabled(false);
			dosmixerblocksize.setEnabled(false);
			dosautoexec.setEnabled(false);
			
			doskblayout.setEnabled(false);
			dosxms.setEnabled(false);
			dosems.setEnabled(false);
			dosumb.setEnabled(false);
			dosipx.setEnabled(false);
			dospnp.setEnabled(false);
			dospcspeaker.setEnabled(false);
			doseditconf_file.setEnabled(true);
			
			dosmanualconf_file.setEnabled(true);
			configFile = prefs.getString("dosmanualconf_file",CONFIG_PATH+CONFIG_FILE);	
		} else {
			doscpu.setEnabled(true);
			doscputype.setEnabled(true);
			doscycles.setEnabled(true);
			dosframeskip.setEnabled(true);
			dosmemsize.setEnabled(true);
			dossbtype.setEnabled(true);
			dossbrate.setEnabled(true);
			dosmt32.setEnabled(true);
			dosmachine.setEnabled(true);
			dostimedjoy.setEnabled(true);
			dosmixerprebuffer.setEnabled(true);
			dosmixerblocksize.setEnabled(true);
			dosautoexec.setEnabled(true);
			dosmanualconf_file.setEnabled(false);
			doseditconf_file.setEnabled(false);
			doskblayout.setEnabled(true);
			dosxms.setEnabled(true);
			dosems.setEnabled(true);
			dosumb.setEnabled(true);
			dosipx.setEnabled(true);
			dospnp.setEnabled(true);
			dospcspeaker.setEnabled(true);

			configFile = CONFIG_PATH+CONFIG_FILE;					

		}
		dosmanualconf_file.setSummary(configFile);
	}
	
	private void update_confenabledpad() {
		if (prefs.getBoolean("confenabledpad",false)) {
			confdpadsensitivity.setEnabled(true);
		} else {
			confdpadsensitivity.setEnabled(false);
		}
	}

	
	private void updateMapSummary() {
		try {
			// set button mapping descriptions
	    	for (short i=0;i<NUM_USB_MAPPINGS;i++) {
	    		confmap_custom[i].setSummary(getMapKey(Integer.valueOf(confmap_custom[i].getDosCode())));
	    		int hardcode = Integer.valueOf(confmap_custom[i].getHardCode());
	    		if (hardcode > 0)
	    			confmap_custom[i].setTitle(hardCodeToString(hardcode));
	    		if (Build.VERSION.SDK_INT > 9) {
	    			if ( (Integer.valueOf(confmap_custom[i].getHardCode()) <= 0) || (Integer.valueOf(confmap_custom[i].getDosCode()) <= 0) ) {
	    				dpad_mappings.removePreference(confmap_custom[i]);
	    			}
	    		} else {
	    			dpad_mappings.removePreference(confcustom_add);
	    		}

	    	}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}


	public static String getMapKey(int value) {
		switch (value) {
		case	0: 		return "NONE";
		case	23:		return "DPAD CENTER";
		case	20:		return "DPAD DOWN";
		case	21:		return "DPAD LEFT";
		case	22:		return "DPAD RIGHT";
		case	19:		return "DPAD UP";
		case	113:	return "L.CTRL";
		case	114:	return "R.CTRL";
		case	57:		return "L.ALT";
		case	58:		return "R.ALT";
		case 	59:		return "L. SHIFT";
		case	60:		return "R. SHIFT";
		case	111:	return "ESC";
		case	61:		return "TAB";
		case	66:		return "ENTER";
		case	62:		return "SPACE";
		case	131:
		case	132:
		case	133:
		case	134:
		case	135:
		case	136:
		case	137:
		case	138:
		case	139:
		case	140:
		case	141:
		case	142:	return "F"+String.valueOf(value-130);
		case	29:		return "a";
		case	30:		return "b";
		case	31:		return "c";
		case	32:		return "d";
		case	33:		return "e";
		case	34:		return "f";
		case	35:		return "g";
		case	36:		return "h";
		case	37:		return "i";
		case	38:		return "j";
		case	39:		return "k";
		case	40:		return "l";
		case	41:		return "m";
		case	42:		return "n";
		case	43:		return "o";
		case	44:		return "p";
		case	45:		return "q";
		case	46:		return "r";
		case	47:		return "s";
		case	48:		return "t";
		case	49:		return "u";
		case	50:		return "v";
		case	51:		return "w";
		case	52:		return "x";
		case	53:		return "y";
		case	54:		return "z";
		case	7:
		case	8:
		case	9:
		case	10: 
		case	11:
		case	12:
		case	13:
		case	14:
		case	15:
		case	16:		return String.valueOf(value-7);
		
		case	74:		return "SEMICOLON";
		case	75:		return "APOSTROPHE";
		case	55:		return "COMMA";
		case	56:		return "PERIOD";
		case	76:		return "SLASH";
		case	67:		return "BACKSPACE";
		case	112:	return "DELETE";
		case	71:		return "L.BRACKET";
		case	72:		return "R.BRACKET";
		case	81:		return "PLUS";
		case	69:		return "MINUS";
		
		case	144:	return "NUMPAD_0";
		case	145:	return "NUMPAD_1";
		case	146:	return "NUMPAD_2";
		case	147:	return "NUMPAD_3";
		case	148:	return "NUMPAD_4";
		case	149:	return "NUMPAD_5";
		case	150:	return "NUMPAD_6";
		case	151:	return "NUMPAD_7";
		case	152:	return "NUMPAD_8";
		case	153:	return "NUMPAD_9";
		case	157:	return "NUMPAD_ADD";
		case	154:	return "NUMPAD_DIVIDE";
		case	158:	return "NUMPAD_DOT";
		case	160:	return "NUMPAD_ENTER";
		case	155:	return "NUMPAD_MULTIPLY";
		case	156:	return "NUMPAD_SUBTRACT";
		case	143:	return "NUMPAD_NUMLOCK";
		
		case	92:		return "PAGE_UP";
		case	93:		return "PAGE_DOWN";
		case	122:	return "NUMPAD_HOME";
		case	123:	return "NUMPAD_END";
		case	124:	return "INSERT";
		
		case	20000:	return "LEFT MOUSE BTN";
		case	20001:	return "RIGHT MOUSE BTN";
		case	20002:	return "CYCLE UP";
		case	20003:	return "CYCLE DOWN";
		case	20004:  return "SHOW KEYBOARD";
		case	20005:	return "SHOW SPECIAL KEYS";
		case	20006:	return "SHOW CYCLES MENU";
		case	20007:	return "SHOW FRAMESKIP MENU";
		case	20008:  return "FAST FORWARD";
		case	20009:  return "JOY BTN A";
		case	20010:	return "JOY BTN B";
		
		default:	return "<undefined>";
		
		}
	}
	
	public static String hardCodeToString(int keycode) {	
		switch (keycode) {
		case KeyEvent.KEYCODE_DPAD_UP: 		return "KEYCODE_DPAD_UP";
		case KeyEvent.KEYCODE_DPAD_DOWN: 	return "KEYCODE_DPAD_DOWN";
		case KeyEvent.KEYCODE_DPAD_LEFT: 	return "KEYCODE_DPAD_LEFT";
		case KeyEvent.KEYCODE_DPAD_RIGHT:	return "KEYCODE_DPAD_RIGHT";
		case KeyEvent.KEYCODE_DPAD_CENTER:	
			if (isExperiaPlay) {
				if (isXOkeysSwapped()) {
					return "KEYCODE_SONY_O";
				} else {
					return "KEYCODE_SONY_X";
				}
			} else {
				return "KEYCODE_DPAD_CENTER";
			}
		case KeyEvent.KEYCODE_CAMERA:		return "KEYCODE_CAMERA";
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS: return "KEYCODE_MEDIA_PREVIOUS";
		case KeyEvent.KEYCODE_MEDIA_NEXT:	return "KEYCODE_MEDIA_NEXT";
		case KeyEvent.KEYCODE_MEDIA_REWIND:	return "KEYCODE_MEDIA_REWIND";
		case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:	return "KEYCODE_MEDIA_FAST_FORWARD";
		case KeyEvent.KEYCODE_MEDIA_STOP:	return "KEYCODE_MEDIA_STOP";
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:return "KEYCODE_MEDIA_PLAY_PAUSE";
		case KeyEvent.KEYCODE_MUTE:			return "KEYCODE_MUTE";
		case KeyEvent.KEYCODE_ALT_LEFT:		return "KEYCODE_ALT_LEFT";
		case KeyEvent.KEYCODE_ALT_RIGHT:	return "KEYCODE_ALT_RIGHT";
		case KeyEvent.KEYCODE_CLEAR:		return "KEYCODE_CLEAR";
		case KeyEvent.KEYCODE_ENVELOPE:		return "KEYCODE_ENVELOPE";			
		case KeyEvent.KEYCODE_EXPLORER:		return "KEYCODE_EXPLORER";
		case KeyEvent.KEYCODE_FOCUS:		return "KEYCODE_FOCUS";
		case KeyEvent.KEYCODE_BACK: 		return "KEYCODE_BACK";
		case KeyEvent.KEYCODE_VOLUME_UP:	return "KEYCODE_VOLUME_UP";
		case KeyEvent.KEYCODE_VOLUME_DOWN:	return "KEYCODE_VOLUME_DOWN";
		// handle virtual buttons
		case HardCodeWrapper.KEYCODE_VIRTUAL_A:	return "KEYCODE_VIRTUAL_A";
		case HardCodeWrapper.KEYCODE_VIRTUAL_B:	return "KEYCODE_VIRTUAL_B";
		case HardCodeWrapper.KEYCODE_VIRTUAL_C:	return "KEYCODE_VIRTUAL_C";
		case HardCodeWrapper.KEYCODE_VIRTUAL_D:	return "KEYCODE_VIRTUAL_D";
		
		// handle gamepad diagonals
		case HardCodeWrapper.KEYCODE_DPAD_UP_RIGHT: return "KEYCODE_DPAD_UP_RIGHT";
		case HardCodeWrapper.KEYCODE_DPAD_UP_LEFT: return "KEYCODE_DPAD_UP_LEFT";
		case HardCodeWrapper.KEYCODE_DPAD_DOWN_RIGHT: return "KEYCODE_DPAD_DOWN_RIGHT";
		case HardCodeWrapper.KEYCODE_DPAD_DOWN_LEFT: return "KEYCODE_DPAD_DOWN_LEFT";
		
		case XPERIA_BACK_BUTTON: 
			if (isXOkeysSwapped()) {
				return "KEYCODE_SONY_X";
			} else {
				return "KEYCODE_SONY_O";				
			}
		case HardCodeWrapper.KEYCODE_BUTTON_X:
			if (isExperiaPlay)
				return "KEYCODE_SONY_SQUARE";
			else 
				return "KEYCODE_BUTTON_X";
		case HardCodeWrapper.KEYCODE_BUTTON_Y:
			if (isExperiaPlay)
				return "KEYCODE_SONY_TRIANGLE";
			else 
				return "KEYCODE_BUTTON_Y";
		default:
			return kw.hardCodeToString(keycode);
		}
	}
	
	private static final char DEFAULT_O_BUTTON_LABEL = 0x25CB;   //hex for WHITE_CIRCLE
	private static boolean isXOkeysSwapped() {
		boolean flag = false;
		int[] ids = kw.getDeviceIds();
		for (int i= 0; ids != null && i<ids.length; i++) {
			KeyCharacterMap kcm = KeyCharacterMap.load(ids[i]);
			if ( kcm != null && DEFAULT_O_BUTTON_LABEL ==
					kcm.getDisplayLabel(KeyEvent.KEYCODE_DPAD_CENTER) ) {
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
			if (preference == confreset) {
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage(R.string.confirmreset)
	    	       .setCancelable(false)
	    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	   // reset prefs
	    	        	   PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().clear().commit();
	    	        	   PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, true);
	    	        	   //onContentChanged();
	    	         	   finish();
	    	           }
	    	       })
	    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                dialog.cancel();
	    	           }
	    	       });
	    	AlertDialog alert = builder.create();
	    	alert.show();
		} else if (preference == doseditconf_file) {
			// setup intent
			Intent intent = new Intent(Intent.ACTION_EDIT); 
			Uri uri = Uri.parse("file://"+prefs.getString("dosmanualconf_file","")); 
			intent.setDataAndType(uri, "text/plain"); 
			// Check if file exists, if not, copy template
			File f = new File(prefs.getString("dosmanualconf_file",""));
			if (!f.exists()) {
				try {
					InputStream in = getApplicationContext().getAssets().open("template.conf");
					FileOutputStream out = new FileOutputStream(f);
					byte[] buffer = new byte[1024];
					int len = in.read(buffer);
					while (len != -1) {
					    out.write(buffer, 0, len);
					    len = in.read(buffer);
					}
					in.close();
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// launch editor
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
	    		Toast.makeText(this, R.string.noeditor,Toast.LENGTH_SHORT).show(); 
	    	}
		} else if (preference == confcustom_add) {
			// add new custom mapping
			int mapcnt = 0;
	    	for (short i=0;i<NUM_USB_MAPPINGS;i++) {
	    		int hardcode = Integer.valueOf(confmap_custom[i].getHardCode());
	    		if (hardcode > 0) {
	    			if ( (Integer.valueOf(confmap_custom[i].getHardCode()) <= 0) || (Integer.valueOf(confmap_custom[i].getDosCode()) <= 0) ) {
		    			// found an unassigned mapping
	    				dpad_mappings.addPreference(confmap_custom[i]);
	    				confcustom_add.setSummary(confcustom_add.getSummary());

	    				return true;
	    			} else {
	    				mapcnt++;
	    			}
	    		} else {
	    			// found an unassigned mapping
	    			dpad_mappings.addPreference(confmap_custom[i]);
	    			confcustom_add.setSummary(confcustom_add.getSummary());
			
	    			return true;
	    		}
	    	}
	    	if (mapcnt == NUM_USB_MAPPINGS-1) {
	    		// no available mappings
	    		Toast.makeText(this, R.string.nomoremaps,Toast.LENGTH_SHORT).show();
	    	}
		} else if (preference == confcustom_clear) {
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage(R.string.confirmclear)
	    	       .setCancelable(false)
	    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	   // reset prefs
	    	   				for (short i=0;i<NUM_USB_MAPPINGS;i++) {
	    	   					confmap_custom[i].setHardCode("0");
	    	   					confmap_custom[i].setDosCode("0");
	    	   					confmap_custom[i].commit();
	    	   					if (Build.VERSION.SDK_INT > 9) {
	    	   						dpad_mappings.removePreference(confmap_custom[i]);
	    	   					}
	    	   				}
	    	   				confcustom_add.setSummary(confcustom_add.getSummary());

	    	         	   //finish();
	    	           }
	    	       })
	    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                dialog.cancel();
	    	           }
	    	       });
	    	AlertDialog alert = builder.create();
	    	alert.show();
		} 
		return false;
	}
	
	/* Checks if external storage is available for read and write */
	public static boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

	/* Checks if external storage is available to at least read */
	public static boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}

	public static String getExternalDosBoxDir(Context ctx) {
	    // Create a path where we will place our picture in the user's
	    // public pictures directory.  Note that you should be careful about
	    // what you place here, since the user often manages these files.  For
	    // pictures and other media owned by the application, consider
	    // Context.getExternalMediaDir().
	    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

	    // Make sure the Pictures directory exists.
	    if (!path.exists()) {
	    	if(path.mkdirs()) {
	    		return path.getAbsolutePath() + "/";
	    	}
	    } 
	    if (path.exists()) {
	    	return path.getAbsolutePath() + "/";
	    }
	    // doesnt exist and cant create... use internal memory
	    if (isExternalStorageWritable()) {
	    	return ctx.getExternalFilesDir(null).getAbsolutePath() + "/";
	    } else {
			// external storage does not exist, use internal storage
			return ctx.getFilesDir().getAbsolutePath()+"/";
		}
	}
}
