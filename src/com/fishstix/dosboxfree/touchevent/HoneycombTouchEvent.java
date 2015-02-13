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

import android.annotation.SuppressLint;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.SurfaceView;

@SuppressLint("NewApi")
public class HoneycombTouchEvent extends TouchEventWrapper{
	@Override
	public int getPointerId(MotionEvent event, int pointerIndex) {
		return event.getPointerId(pointerIndex);
	}
	@Override
	public int getPointerCount(MotionEvent event) {
		return event.getPointerCount();
	}
	@Override
	public float getX(MotionEvent event, int id) {
		return event.getX(id);
	}
	@Override
	public float getY(MotionEvent event, int id) {
		return event.getY(id);
	}
	@Override
	public int getButtonState(MotionEvent event) {
		return 1;
	}
	@Override
	public int getSource(MotionEvent event) {
		return event.getSource();
	}
	@Override
	public boolean onGenericMotionEvent(SurfaceView view, MotionEvent event) {
		return view.onGenericMotionEvent(event);
	}
	@Override
	public int[] getDeviceIds() {
		return InputDevice.getDeviceIds();
	}
	@Override
	public int findPointerIndex(MotionEvent event, int pointerId) {
		return event.findPointerIndex(pointerId);
	}

}
