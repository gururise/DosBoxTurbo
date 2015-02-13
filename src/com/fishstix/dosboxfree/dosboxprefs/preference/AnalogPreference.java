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

import java.util.Arrays;

import com.fishstix.dosboxfree.R;
import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
 

public class AnalogPreference extends DialogPreference implements OnKeyListener, OnClickListener
{ 
  private static final String myns="http://schemas.android.com/apk/res/";
  private static final String androidns="http://schemas.android.com/apk/res/android";

  private TextView mDosMapText, mHardCodeText;
  private EditText mEditText;
  private Spinner mSpinner;
  private Button mClearButton;
  
  private String mAndroidPrefKey, mHardCodeDefault, mDosCodeDefault;
  private String mDosCode="0", mHardCode="0";
  private String mTitle;
  private Context ctx;
  
  //private HardCodeWrapper kw = HardCodeWrapper.newInstance();
  public static final String HARDCODE_KEY = "_hardcode";
  public static final String DOSCODE_KEY = "_doscode";
  
  public AnalogPreference(Context context, AttributeSet attrs) { 
    super(context,attrs);
    setDialogLayoutResource(R.layout.analogdialog);
    setDialogTitle("Analog Joy Mapping");
    setPersistent(false);
    
    mHardCodeDefault = attrs.getAttributeValue(myns,"defaultHardCode");
    mDosCodeDefault = attrs.getAttributeValue(myns,"defaultDosCode");
    mAndroidPrefKey = attrs.getAttributeValue(androidns,"key");
    mTitle = attrs.getAttributeValue(androidns,"title");
    ctx = context;
  }

  @Override
  protected void showDialog(Bundle state) {
	  super.showDialog(state);
      getDialog().setOnKeyListener(this);
      getDialog().takeKeyEvents(true);
  }
  
  @Override
  protected Parcelable onSaveInstanceState() {
	  super.onSaveInstanceState();
	  return null;
  }
  
  @Override
  protected void onRestoreInstanceState(Parcelable state) {
	  super.onRestoreInstanceState(state);
  }
  
  @Override 
  protected void onBindDialogView(View v) {
	super.onBindDialogView(v);
    mDosMapText = (TextView)v.findViewById(R.id.dosmap_text);
    mDosMapText.setText(R.string.select_dos_key);
    mHardCodeText = (TextView)v.findViewById(R.id.controlvalue_text);
    mSpinner = (Spinner)v.findViewById(R.id.spinmap);

    mEditText = (EditText)v.findViewById(R.id.editText1);
    InputFilter[] filterArray = new InputFilter[1];
    filterArray[0] = new InputFilter.LengthFilter(0);
    mEditText.setFilters(filterArray);

    mClearButton = (Button)v.findViewById(R.id.clear_button);
    v.setFocusable(true);
    v.setFocusableInTouchMode(true);
    mClearButton.setOnClickListener(new View.OnClickListener() {		
		@Override
		public void onClick(View v) {
			clear();
		}
	});
    
    
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource( v.getContext(), R.array.confmapbutton, android.R.layout.simple_spinner_item );
    adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
    		
    //v.getResources().getStringArray(R.array.confmapbutton);
    mSpinner.setAdapter(adapter);
    int mappos = Arrays.asList(v.getResources().getStringArray(R.array.confmapbutton_values)).indexOf(mDosCode);
    mSpinner.setSelection(mappos);
    
    
    if (Integer.valueOf(mHardCode) > 0) {
    	mHardCodeText.setText(DosBoxPreferences.hardCodeToString(Integer.valueOf(mHardCode)));
    	setTitle(DosBoxPreferences.hardCodeToString(Integer.valueOf(mHardCode)));
    }
    if (mappos > 0) {
    	setSummary(DosBoxPreferences.getMapKey(Integer.valueOf(mDosCode)));
    }
    mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			
		    mDosCode = view.getResources().getStringArray(R.array.confmapbutton_values)[pos];
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
    	
    });
  }

  
@Override
  protected void onDialogClosed(boolean positiveResult) {
      super.onDialogClosed(positiveResult);

      if (positiveResult) {
    	  if (Integer.valueOf(mHardCode) > 0) {
    		  // check if hardcode exists
    		  SharedPreferences prefs = getSharedPreferences();
    		  boolean error = false;
    		  for (int i=0;i<DosBoxPreferences.NUM_USB_MAPPINGS;i++) {
    			  int substr = 0;
    			  try {
    				  substr = Integer.valueOf(mAndroidPrefKey.substring(14));
    			  } catch (Exception e) {
    				  error = true;
    			  }
    			  if ( (Integer.valueOf(mHardCode) == Integer.valueOf(prefs.getString("confmap_custom"+i+HARDCODE_KEY, "0"))) &&
    				   (substr != i) ) {
    				  error = true;
    				  mHardCode = "-1";
    				  mDosCode = "-1";
    				  Toast.makeText(ctx, R.string.duplicatemap,Toast.LENGTH_SHORT).show();
    				  break;
    			  }
    		  }
    		  if ((Integer.valueOf(mDosCode) > 0) && !error) {
            	  //setSummary(mDosString);
        		  setSummary(DosBoxPreferences.getMapKey(Integer.valueOf(mDosCode)));
            	  setTitle(DosBoxPreferences.hardCodeToString(Integer.valueOf(mHardCode)));
            	  
                  commit();    			  
    		  }
    	  }
      }
  }

  public void setHardCode(String val) {
	  mHardCode = val;
  }
  
  public void setDosCode(String val) {
	  mDosCode = val;
  }
  
  public String getHardCode() {
	  return mHardCode;
  }
  
  public String getDosCode() {
	  return mDosCode;
  }
  
  public void commit() {
	  Editor editor = getEditor();
      editor.putString(mAndroidPrefKey+HARDCODE_KEY, getHardCode());
      editor.putString(mAndroidPrefKey+DOSCODE_KEY, getDosCode());
      if (Integer.valueOf(getHardCode()) > 0) {
    	  setTitle(DosBoxPreferences.hardCodeToString(Integer.valueOf(mHardCode)));    	  
          setSummary(DosBoxPreferences.getMapKey(Integer.valueOf(mDosCode)));
      } else {
    	  setTitle(mTitle);
    	  setSummary("<undefined>");
      }
      editor.commit();
  }
  
  private void clear() {
	  setHardCode("0");
	  setDosCode("0");
	  mDosCode = "0";
	  Editor editor = getEditor();
	  editor.putString(mAndroidPrefKey+HARDCODE_KEY, mHardCode);
	  editor.putString(mAndroidPrefKey+DOSCODE_KEY, mDosCode);
	  editor.commit();
	  mSpinner.setSelection(0);
	  setSummary(DosBoxPreferences.getMapKey(Integer.valueOf(mDosCode)));
	  mHardCodeText.setText(R.string.undefined);
  	  setTitle(mTitle);
  }
  
  @Override
  protected void onSetInitialValue(boolean restore, Object defaultValue)  
  {
	  super.onSetInitialValue(restore, defaultValue);
	  SharedPreferences prefs = getSharedPreferences();
	  String tmp = prefs.getString(mAndroidPrefKey+HARDCODE_KEY, "-1");
	  Editor editor = getEditor();
	  if (!tmp.contains("-1")) {
		  // valid value
		  setHardCode(tmp);
	  } else {
		  //default
		  setHardCode(mHardCodeDefault);
		  editor.putString(mAndroidPrefKey+HARDCODE_KEY, mHardCodeDefault);
	  }
	  tmp = prefs.getString(mAndroidPrefKey+DOSCODE_KEY, "-1");
	  if (!tmp.contains("-1")) {
		  setDosCode(tmp);
	  } else {
		  //default
		  setDosCode(mDosCodeDefault);
		  editor.putString(mAndroidPrefKey+DOSCODE_KEY, mDosCodeDefault);
	  }
	  editor.commit();
  }
  
  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
	  return a.getInteger(index, 0);
  }  

  @Override
  public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
	  // Dialog onKey
	  if (keyCode == KeyEvent.KEYCODE_BACK) {
		  // Handle Xperia Play O button
		  if (!event.isAltPressed()) {
			  // back button
			  mHardCode = String.valueOf(keyCode);
		  } else {
			  // circle button (Xperia Play)
			  mHardCode = String.valueOf(DosBoxPreferences.XPERIA_BACK_BUTTON);
		  }
	  } else {
		  mHardCode = String.valueOf(keyCode);		  
	  }

	  mHardCodeText.setText(DosBoxPreferences.hardCodeToString(Integer.valueOf(mHardCode)));
	  return true;	// consume event
  }


@Override
public void onClick(View v) {

	
}

}