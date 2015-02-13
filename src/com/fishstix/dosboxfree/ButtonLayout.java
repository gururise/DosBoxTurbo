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
package com.fishstix.dosboxfree;

import com.fishstix.dosboxfree.dosboxprefs.preference.HardCodeWrapper;

import android.content.Context;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class ButtonLayout extends LinearLayout {
	// Create a hash map 
	private SparseIntArray virtualbutton_hm = new SparseIntArray();
	public DBMain mDBLauncher;

	public ButtonLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
 
	public ButtonLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public ButtonLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs); 
		// TODO Auto-generated constructor stub
	}
	
	@Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
      return true; // With this i tell my layout to consume all the touch events from its childs
   }
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
    	final int action = MotionEventCompat.getActionMasked(ev);
    	final int pointerIndex = MotionEventCompat.getActionIndex(ev);//((action & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
        final int pId = ev.getPointerId(pointerIndex)+1;
    	KeyEvent evt = null;
    	Message msg = Message.obtain();
    	msg.what = DBMain.HANDLER_SEND_KEYCODE;
       	switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: 
            case MotionEvent.ACTION_POINTER_DOWN: {
    			Log.i("DosBoxTurbo","button onDown()");
            	int x = (int) ev.getX(pointerIndex);//(int) mWrap.getX(ev,pointerIndex);
            	int width = getWidth();
                //int y = (int) mWrap.getY(ev,pointerIndex);
                float val = ((float)x/(float)width)*4f;
                if (val < 1.0) {
                	// first button
                	if (virtualbutton_hm.indexOfValue(HardCodeWrapper.KEYCODE_VIRTUAL_A) > 0)
                		return false;
            		evt = new KeyEvent(action, HardCodeWrapper.KEYCODE_VIRTUAL_A);
            		virtualbutton_hm.put(pId, HardCodeWrapper.KEYCODE_VIRTUAL_A);
            		msg.arg2 = HardCodeWrapper.KEYCODE_VIRTUAL_A;
            		mDBLauncher.bButtonA.setBackgroundColor(0x80FF0000);
                } else if (val < 2.0) {
                	if (virtualbutton_hm.indexOfValue(HardCodeWrapper.KEYCODE_VIRTUAL_B) > 0)
                		return false;
                	evt = new KeyEvent(action, HardCodeWrapper.KEYCODE_VIRTUAL_B);
            		virtualbutton_hm.put(pId, HardCodeWrapper.KEYCODE_VIRTUAL_B);
            		msg.arg2 = HardCodeWrapper.KEYCODE_VIRTUAL_B;
            		mDBLauncher.bButtonB.setBackgroundColor(0x80FF0000);
                } else if (val < 3.0) {
                	if (virtualbutton_hm.indexOfValue(HardCodeWrapper.KEYCODE_VIRTUAL_C) > 0)
                		return false;
                	evt = new KeyEvent(action, HardCodeWrapper.KEYCODE_VIRTUAL_C);
            		virtualbutton_hm.put(pId, HardCodeWrapper.KEYCODE_VIRTUAL_C);
            		msg.arg2 = HardCodeWrapper.KEYCODE_VIRTUAL_C;
            		mDBLauncher.bButtonC.setBackgroundColor(0x80FF0000);
                } else {
                	if (virtualbutton_hm.indexOfValue(HardCodeWrapper.KEYCODE_VIRTUAL_D) > 0)
                		return false;
                	evt = new KeyEvent(action, HardCodeWrapper.KEYCODE_VIRTUAL_D);
            		virtualbutton_hm.put(pId, HardCodeWrapper.KEYCODE_VIRTUAL_D);
            		msg.arg2 = HardCodeWrapper.KEYCODE_VIRTUAL_D;
            		mDBLauncher.bButtonD.setBackgroundColor(0x80FF0000);
                }
            	msg.obj = evt;
            	msg.arg1 = 0;
            	mDBLauncher.mSurfaceView.virtButton[pointerIndex]= true;
            	mDBLauncher.mSurfaceView.mFilterLongClick = true; 		// prevent long click listener from getting in the way
            	mDBLauncher.mHandler.sendMessage(msg);
            	return true;
            }
            
            case MotionEvent.ACTION_CANCEL: 
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
            	Log.i("DosBoxTurbo","button onUp()");
            	msg.arg2 = virtualbutton_hm.get(pId);
            	switch (msg.arg2) {
            	case HardCodeWrapper.KEYCODE_VIRTUAL_A:
            		mDBLauncher.bButtonA.setBackgroundColor(0x80FFFF00);
            	break;
            	case HardCodeWrapper.KEYCODE_VIRTUAL_B:
            		mDBLauncher.bButtonB.setBackgroundColor(0x80FFFF00);
            	break;
            	case HardCodeWrapper.KEYCODE_VIRTUAL_C:
            		mDBLauncher.bButtonC.setBackgroundColor(0x80FFFF00);
            	break;
            	case HardCodeWrapper.KEYCODE_VIRTUAL_D:
            		mDBLauncher.bButtonD.setBackgroundColor(0x80FFFF00);
            	break;
            	}
            	virtualbutton_hm.delete(pId);
            	if (msg.arg2 == 0) 
            		return false;
            	evt = new KeyEvent(action, msg.arg2);
            	msg.obj = evt;
            	msg.arg1 = 1;
            	mDBLauncher.mSurfaceView.virtButton[pointerIndex]= false;
            	mDBLauncher.mSurfaceView.mFilterLongClick = false; 
            	mDBLauncher.mHandler.sendMessage(msg);
            	return true;
            }
       	}
		return false;
	}
}
