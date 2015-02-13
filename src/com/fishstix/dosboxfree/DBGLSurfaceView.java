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

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.joystick.JoystickClickedListener;
import com.fishstix.dosboxfree.joystick.JoystickMovedListener;
import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

 
class DBGLSurfaceView extends GLSurfaceView implements SurfaceHolder.Callback {
	private final static int DEFAULT_WIDTH = 512;//800;
	private final static int DEFAULT_HEIGHT = 512;//600;  
	public int mJoyCenterX = 0;
	public int mJoyCenterY = 0;

	private final static int BUTTON_REPEAT_DELAY = 100;	
	private final static int EVENT_THRESHOLD_DECAY = 100;
	private final static int DEADZONE=8;
	
	public final static int INPUT_MODE_MOUSE = 0xf1;
	public final static int INPUT_MODE_SCROLL = 0xf2;
	public final static int INPUT_MODE_JOYSTICK = 0xf3;
	public final static int INPUT_MODE_REAL_MOUSE = 0xf4;
	public final static int INPUT_MODE_REAL_JOYSTICK = 0xf5;
	
	public final static int ACTION_DOWN = 0;
	public final static int ACTION_UP = 1;
	public final static int ACTION_MOVE = 2;
	
	public final static int PIXEL_BYTES = 2;

	private static final int MAX_POINT_CNT = 4;
	
	private DBMain mParent = null;	
	private boolean mSurfaceViewRunning = false;
	public DosBoxVideoThread mVideoThread = null;
	public DosBoxMouseThread mMouseThread = null;
	public KeyHandler mKeyHandler = null;
	private GestureDetector gestureScanner;
	
	boolean mScale = false;   
	int mInputMode = INPUT_MODE_MOUSE;
	boolean mEnableDpad = false;
	boolean mJoyEmuMouse = false;
	boolean mAbsolute = true;
	boolean mInputLowLatency = false;
	boolean mUseLeftAltOn = false;
	public boolean mLongPress = true;
	public boolean mDebug = false;
	private static final int CLICK_DELAY = 125;	// in ms 
    private boolean mDoubleLong = false;
    public float mMouseSensitivityX = 1.0f;
    public float mMouseSensitivityY = 1.0f;
    public boolean mScreenTop = false;
    public boolean mGPURendering = false;
    public boolean mKeyboardVisible = false;
    public short mAnalogStickPref = 0;
    
	int mDpadRate = 7;
	private boolean mLongClick = false;
	//boolean mCalibrate = false;
	boolean mMaintainAspect = true;
	//private boolean mHasMoved = false;
	private boolean mSPenButton = false;
	
	int	mContextMenu = 0;

	Bitmap mBitmap = null; 
	private Paint mBitmapPaint = null;
	private Paint mTextPaint = null;

	int mSrc_width = 0;
	int mSrc_height = 0;
	final AtomicBoolean bDirtyCoords = new AtomicBoolean(false);
	private int mScroll_x = 0;
	private int mScroll_y = 0;
	
	final AtomicBoolean mDirty = new AtomicBoolean(false);
	boolean isLandscape = false;
	int mStartLine = 0;
	int mEndLine = 0;
	boolean mFilterLongClick = false;

	boolean mModifierCtrl = false;
	boolean mModifierAlt = false;
	boolean mModifierShift = false;
	public int mActionBarHeight;
	public OpenGLRenderer mRenderer;
	
	static class KeyHandler extends Handler {
		private final WeakReference<DBGLSurfaceView> mSurface; 
		boolean mReCheck = false;
		
		KeyHandler(DBGLSurfaceView surface) {
	        mSurface = new WeakReference<DBGLSurfaceView>(surface);
	    }
		
		@Override
		public void handleMessage (Message msg) {
			DBGLSurfaceView surf = mSurface.get();
			if (msg.what == DBMain.SPLASH_TIMEOUT_MESSAGE) {
				surf.setBackgroundResource(0);				
			}
			else {
				if (DosBoxControl.sendNativeKey(msg.what, false, surf.mModifierCtrl, surf.mModifierAlt, surf.mModifierShift)) {
					surf.mModifierCtrl = false;
					surf.mModifierAlt = false;
					surf.mModifierShift = false;					
				}
			}
		}		
	}

	class DosBoxVideoThread extends Thread {
		private static final int UPDATE_INTERVAL = 40; 
		private static final int UPDATE_INTERVAL_MIN = 20; 
		private static final int RESET_INTERVAL = 100; 

		private boolean mVideoRunning = false;

		private long startTime = 0;
		private int frameCount = 0;
		private long curTime, nextUpdateTime, sleepTime;

		void setRunning(boolean running) {
			mVideoRunning = running;
		}
		
		public void run() {
			mVideoRunning = true;
			
			while (mVideoRunning) {
				if (mSurfaceViewRunning) {

					curTime = System.currentTimeMillis();

					if (frameCount > RESET_INTERVAL)
						frameCount = 0;					
					
					if (frameCount == 0) {
						startTime = curTime - UPDATE_INTERVAL;
					}
					
					frameCount++;
					
					synchronized (mDirty) {
						if (mDirty.get()) {
							if (bDirtyCoords.get()) {
								calcScreenCoordinates(mSrc_width, mSrc_height, mStartLine, mEndLine);
							}
							VideoRedraw(mBitmap, mSrc_width, mSrc_height, mStartLine, mEndLine);
							mDirty.set(false);				
						}
					}
			        
					try {
						nextUpdateTime = startTime + (frameCount+1) * UPDATE_INTERVAL;
						sleepTime = nextUpdateTime - System.currentTimeMillis();
						Thread.sleep(Math.max(sleepTime, UPDATE_INTERVAL_MIN));
					} catch (InterruptedException e) {
					}
				}
				else {
					try {
						frameCount = 0;
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}					
				}
			}
		}		
	}	

	public DBGLSurfaceView(Context context) {
		super(context);
		mDirty.set(false);
		if (!this.isInEditMode()) {
			setup(context);
		}
		Log.i("DosBoxTurbo", "Surface constructor - Default Form");
	}
	
	public DBGLSurfaceView(Context context, AttributeSet attrs) {
		super(context,attrs);		
		if (!this.isInEditMode()) {
			setup(context);
		}
		Log.i("DosBoxTurbo", "Surface constructor - Default Form");
	}
	
	public DBGLSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context,attrs);
	
		if (!this.isInEditMode()) {
			setup(context);
		}
		Log.i("DosBoxTurbo", "Surface constructor - Default Form");
	} 
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setup(Context context) {
		mParent = (DBMain) context;
		//setRenderMode(RENDERMODE_WHEN_DIRTY);

		gestureScanner = new GestureDetector(context, new MyGestureDetector());
		mBitmapPaint = new Paint();
		mBitmapPaint.setFilterBitmap(true);		
		mTextPaint = new Paint();
		mTextPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
		mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mTextPaint.setTextAlign(Paint.Align.CENTER);
		mTextPaint.setStyle(Paint.Style.FILL);
		mTextPaint.setSubpixelText(false); 
		
		mBitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565);

		//setEGLContextClientVersion(1);
		mRenderer = new OpenGLRenderer(mParent);
		mRenderer.setBitmap(mBitmap);
		setRenderer(mRenderer);
		setRenderMode(RENDERMODE_WHEN_DIRTY);
		if (mGPURendering) {
			requestRender();
		}
  
		mMouseThread = new DosBoxMouseThread();
		mMouseThread.setPriority(Thread.MIN_PRIORITY);
		mMouseThread.setRunning(true);
		mMouseThread.start();
		mVideoThread = new DosBoxVideoThread();
		mKeyHandler = new KeyHandler(this); 				
	  
		// Receive keyboard events
		requestFocus();
		setFocusableInTouchMode(true);
		setFocusable(true);
		requestFocus(); 
		requestFocusFromTouch();

		getHolder().addCallback(this);
		getHolder().setFormat(PixelFormat.RGB_565);
		getHolder().setKeepScreenOn(true);
		if (Build.VERSION.SDK_INT >= 14) {
    	    	setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    		    setOnSystemUiVisibilityChangeListener(new MySystemUiVisibilityChangeListener());
		} else if (Build.VERSION.SDK_INT >= 11) {
			setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
		    setOnSystemUiVisibilityChangeListener(new MySystemUiVisibilityChangeListener());
		} 
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private class MySystemUiVisibilityChangeListener implements View.OnSystemUiVisibilityChangeListener {
	    @Override
	    public void onSystemUiVisibilityChange(int visibility) {
	        Timer timer = new Timer();
	        timer.schedule(new TimerTask() {
	            @Override
	            public void run() {
	                mParent.runOnUiThread(new Runnable() {
	                    @Override
	                    public void run() {
	                    	if (Build.VERSION.SDK_INT >= 14) {
	                	    	setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
	                	    } else if (Build.VERSION.SDK_INT >= 11) {
	                	    	setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
	                	    } 
	                    }
	                });
	            }
	        }, 6000);
	    }
	}
	
	public void shutDown() {
		mBitmap = null;		
		mVideoThread = null;
		mMouseThread = null;
		mKeyHandler = null;		
	}

	public JoystickMovedListener _listener = new JoystickMovedListener() {
		@Override
        public void OnMoved(int pan, int tilt) {
        	if (!mJoyEmuMouse) { 
        		DosBoxControl.nativeJoystick((int)(pan), (int)(tilt), ACTION_MOVE, -1);
        	} else {
           		mMouseThread.setCoord((int)((Math.abs(pan*0.3)>DEADZONE)?(-pan*mMouseSensitivityX*0.20):0), (int)((Math.abs(tilt*0.3)>DEADZONE)?(-tilt*mMouseSensitivityY*0.20):0));
        	}
        }

		@Override
        public void OnReleased() {
        }
        
		@Override
        public void OnReturnedToCenter() {
        	if (!mJoyEmuMouse) {
        		DosBoxControl.nativeJoystick(mJoyCenterX, mJoyCenterY, ACTION_MOVE, -1);
        	} else {
        		mMouseThread.setCoord(0,0);
        	}
        };
	};
	
	public JoystickClickedListener _buttonlistener = new JoystickClickedListener() {
		@Override
		public void OnClicked(int id) {
			if (!mJoyEmuMouse) { 
				DosBoxControl.nativeJoystick(0, 0, ACTION_DOWN, id);
			} else {
				DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_DOWN, id);
			}
		}

		@Override
		public void OnReleased(int id) {
			if (!mJoyEmuMouse) { 
				DosBoxControl.nativeJoystick(0, 0, ACTION_UP, id);
			} else {
				DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_UP, id);
			}
		}
	};
	
	class DosBoxMouseThread extends Thread {
		private static final int UPDATE_INTERVAL = 35; 
		private boolean mMouseRunning = false;
		private boolean mPaused;
		private Object mPauseLock = new Object();
		
		private int x=0,y=0;

		void setRunning(boolean running) {
			mMouseRunning = running;
		}
		
		void setCoord(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		/**
	     * Call this on pause.
	     */
	    public void onPause() {
	        synchronized (mPauseLock) {
	            mPaused = true;
	        }
	    }
	    
	    /**
	     * Call this on resume.
	     */
	    public void onResume() {
	        synchronized (mPauseLock) {
	            mPaused = false;
	            mPauseLock.notifyAll();
	        }
	    }
		
		public void run() {
			mMouseRunning = true;
			while (mMouseRunning) {
				
				if ((this.x != 0) || (this.y != 0)) {
					DosBoxControl.nativeMouse(0, 0, this.x, this.y, ACTION_MOVE, -1);
					DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_MOVE, -1);
				}
				try {
					Thread.sleep(UPDATE_INTERVAL);
				} catch (InterruptedException e) { }
				
	            synchronized (mPauseLock) {
	                while (mPaused) {
	                    try {
	                        mPauseLock.wait();
	                    } catch (InterruptedException e) {
	                    }
	                }
	            }
			}
		}		
	}

	public void calcScreenCoordinates(int src_width, int src_height, int startLine, int endLine) {
		Log.i("DosBoxTurbo","calcScreenCoordinates()");
		if ((src_width <= 0) || (src_height <= 0))
			return;
		
		mRenderer.width = getWidth();
		mRenderer.height = getHeight();

		isLandscape = (mRenderer.width > mRenderer.height);
		if (mScale) {
			if (!mMaintainAspect && isLandscape) {
				mRenderer.x = 0;
			} else {
				mRenderer.x = src_width * mRenderer.height /src_height;
				
				if (mRenderer.x < mRenderer.width) {
					mRenderer.width = mRenderer.x;
				}
				else if (mRenderer.x > mRenderer.width) {
					mRenderer.height = src_height * mRenderer.width /src_width;
				}
				mRenderer.x = (getWidth() - mRenderer.width)/2;
			}
			
			if (isLandscape) {
				mRenderer.width *= (mParent.mPrefScaleFactor * 0.01f);
				mRenderer.height *= (mParent.mPrefScaleFactor * 0.01f);
				mRenderer.x = (getWidth() - mRenderer.width)/2;
				if (!mScreenTop)
					mRenderer.y = (getHeight() - mRenderer.height)/2;
				else 
					mRenderer.y = 0;
			} else {
				// portrait
				mRenderer.y = mActionBarHeight;
			}
				// no power of two extenstion
				mRenderer.mCropWorkspace[0] = 0;
				mRenderer.mCropWorkspace[1] = src_height;
				mRenderer.mCropWorkspace[2] = src_width;
				mRenderer.mCropWorkspace[3] = -src_height; 				
		} else {
			if ((mScroll_x + src_width) < mRenderer.width)
				mScroll_x = mRenderer.width - src_width;

			if ((mScroll_y + src_height) < mRenderer.height)
				mScroll_y = mRenderer.height - src_height;

			mScroll_x = Math.min(mScroll_x, 0); 
			mScroll_y = Math.min(mScroll_y, 0);
				mRenderer.mCropWorkspace[0] = -mScroll_x; // left
				mRenderer.mCropWorkspace[1] = Math.min(mRenderer.height - mScroll_y, src_height) + mScroll_y; // bottom - top
				mRenderer.mCropWorkspace[2] = Math.min(mRenderer.width - mScroll_x, src_width); // right
				mRenderer.mCropWorkspace[3] = -mRenderer.mCropWorkspace[1]; // -(bottom - top)
			mRenderer.width = mRenderer.mCropWorkspace[2]-mRenderer.mCropWorkspace[0];//Math.min(mRenderer.width - mScroll_x, src_width) + mScroll_x;	
			mRenderer.height = (Math.max(-mScroll_y, 0) + mScroll_y + mRenderer.mCropWorkspace[1]) - (Math.max(-mScroll_y, 0) + mScroll_y);

			if (isLandscape) {
				mRenderer.x = (getWidth() - mRenderer.width)/2;
				mRenderer.y = 0;
			} else {
				mRenderer.x = (getWidth() - mRenderer.width)/2;
				mRenderer.y = mActionBarHeight;
			}
			
		}
		bDirtyCoords.set(false);
		mRenderer.filter_on = mParent.mPrefScaleFilterOn;
	}
	private Rect mSrcRect = new Rect();
	private Rect mDstRect = new Rect();
	private Rect mDirtyRect = new Rect();
	private int mDirtyCount = 0;
	private void canvasDraw(Bitmap bitmap, int src_width, int src_height, int startLine, int endLine) {
		SurfaceHolder surfaceHolder = getHolder();
		Surface surface = surfaceHolder.getSurface();
		Canvas canvas = null;
	 
		try {
			synchronized (surfaceHolder)
			{
				
				boolean isDirty = false;

				if (mDirtyCount < 3) {
					mDirtyCount++;
					isDirty =  true;
					startLine = 0;
					endLine = src_height;
				}
				
				if (mScale) {
					mDstRect.set(0, 0, mRenderer.width, mRenderer.height);
					mSrcRect.set(0, 0, src_width, src_height);
					mDstRect.offset(mRenderer.x, mRenderer.y);
					
					mDirtyRect.set(0, startLine * mRenderer.height / src_height, mRenderer.width, endLine * mRenderer.height / src_height+1);
					
					//locnet, 2011-04-21, a strip on right side not updated
					mDirtyRect.offset(mRenderer.x, mRenderer.y);
				} else {
					//L,T,R,B 
					mSrcRect.set(-mScroll_x, Math.max(-mScroll_y, startLine), mRenderer.mCropWorkspace[2], Math.min(Math.min(getHeight() - mScroll_y, src_height), endLine));
					mDstRect.set(0, mSrcRect.top + mScroll_y, mSrcRect.width(), mSrcRect.top + mScroll_y + mSrcRect.height());
					if (isLandscape) {
						mDstRect.offset((getWidth() - mSrcRect.width())/2, 0);						
					} else {
						mDstRect.offset((getWidth() - mSrcRect.width())/2, mActionBarHeight);
					}
					
					mDirtyRect.set(mDstRect);
				}						
				if (surface != null && surface.isValid()) { 
					if (isDirty) {
						canvas = surfaceHolder.lockCanvas(null);
						canvas.drawColor(0xff000000);
					} else {  
						canvas = surfaceHolder.lockCanvas(mDirtyRect);
					}

					if (mScale) {
						canvas.drawBitmap(bitmap, mSrcRect, mDstRect, (mParent.mPrefScaleFilterOn)?mBitmapPaint:null);
					} else {
						canvas.drawBitmap(bitmap, mSrcRect, mDstRect, null);					
					}
				
				}
			}
		} finally {
			if (canvas != null && surface != null && surface.isValid()) {
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		} 
		
		surfaceHolder = null; 
	}
	
	public void VideoRedraw(Bitmap bitmap, int src_width, int src_height, int startLine, int endLine) {
		if (!mSurfaceViewRunning || (bitmap == null) || (src_width <= 0) || (src_height <= 0))
			return;
				if (mGPURendering) { 
					mRenderer.setBitmap(bitmap);
					requestRender();
				} else {
					canvasDraw(bitmap,src_width,src_height,startLine,endLine);
				}
	} 
	
	
	private int[] mButtonDown = new int[MAX_POINT_CNT];
	
	private final static int BTN_A = 0;
	private final static int BTN_B = 1;
	
	float[] x = new float[MAX_POINT_CNT];
	float[] y = new float[MAX_POINT_CNT];
	   
	float[] x_last = new float[MAX_POINT_CNT];
	float[] y_last = new float[MAX_POINT_CNT];
	boolean[] virtButton = new boolean[MAX_POINT_CNT];

	private TouchEventWrapper mWrap = TouchEventWrapper.newInstance();
	
	float hatXlast = 0f;
	float hatYlast = 0f;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private void processJoystickInput(MotionEvent event, int historyPos) {
	float hatX = 0.0f;
	    InputDevice.MotionRange range = event.getDevice().getMotionRange(MotionEvent.AXIS_HAT_X, event.getSource());
		if (range != null) {
		    if (historyPos >= 0) {
		    	 hatX = InputDeviceState.ProcessAxis(range, event.getHistoricalAxisValue(MotionEvent.AXIS_HAT_X, historyPos));
		    } else {
		         hatX = InputDeviceState.ProcessAxis(range, event.getAxisValue(MotionEvent.AXIS_HAT_X));
		    }
		}
		
		float hatY = 0.0f;
	    range = event.getDevice().getMotionRange(MotionEvent.AXIS_HAT_Y, event.getSource());
		if (range != null) {
		    if (historyPos >= 0) {
		    	 hatY = InputDeviceState.ProcessAxis(range, event.getHistoricalAxisValue(MotionEvent.AXIS_HAT_Y, historyPos));
		    } else {
		         hatY = InputDeviceState.ProcessAxis(range, event.getAxisValue(MotionEvent.AXIS_HAT_Y));
		    }
		} 	
		
	    float joyX = 0.0f;
	    range = event.getDevice().getMotionRange(MotionEvent.AXIS_X, event.getSource());
		if (range != null) {
		    if (historyPos >= 0) {
		    	 joyX = InputDeviceState.ProcessAxis(range, event.getHistoricalAxisValue(MotionEvent.AXIS_X, historyPos));
		    } else {
		         joyX = InputDeviceState.ProcessAxis(range, event.getAxisValue(MotionEvent.AXIS_X));
		    }
		}

		float joyY = 0.0f;
		range = event.getDevice().getMotionRange(MotionEvent.AXIS_Y, event.getSource());
		if (range != null) {
		    if (historyPos >= 0) {
		    	joyY = InputDeviceState.ProcessAxis(range, event.getHistoricalAxisValue(MotionEvent.AXIS_Y, historyPos));
		    } else {
		    	joyY = InputDeviceState.ProcessAxis(range, event.getAxisValue(MotionEvent.AXIS_Y));
		    }
		}
		
	    float joy2X = 0.0f;
	    range = event.getDevice().getMotionRange(MotionEvent.AXIS_Z, event.getSource());
		if (range != null) {
		    if (historyPos >= 0) {
		   	 	joy2X = InputDeviceState.ProcessAxis(range, event.getHistoricalAxisValue(MotionEvent.AXIS_Z, historyPos));
		    } else {
		        joy2X = InputDeviceState.ProcessAxis(range, event.getAxisValue(MotionEvent.AXIS_Z));
		    }
		}

		float joy2Y = 0.0f;
		range = event.getDevice().getMotionRange(MotionEvent.AXIS_RZ, event.getSource());
		if (range != null) {
		     if (historyPos >= 0) {
		    	 joy2Y = InputDeviceState.ProcessAxis(range, event.getHistoricalAxisValue(MotionEvent.AXIS_RZ, historyPos));
		     } else {
		    	 joy2Y = InputDeviceState.ProcessAxis(range, event.getAxisValue(MotionEvent.AXIS_RZ));
		    }
		}
		
		
		if (mAnalogStickPref == 0) {
			mMouseThread.setCoord((int)((Math.abs(joyX*32.0f)>DEADZONE)?(-joyX*32.0f*mMouseSensitivityX):0), (int)((Math.abs(joyY*32.0f)>DEADZONE)?(-joyY*32.0f*mMouseSensitivityY):0));
			DosBoxControl.nativeJoystick((int)((joy2X*256.0f)+mJoyCenterX), (int)((joy2Y*256.0f)+mJoyCenterY), ACTION_MOVE, -1);
		} else {
			mMouseThread.setCoord((int)((Math.abs(joy2X*32.0f)>DEADZONE)?(-joy2X*32.0f*mMouseSensitivityX):0), (int)((Math.abs(joy2Y*32.0f)>DEADZONE)?(-joy2Y*32.0f*mMouseSensitivityY):0));
			DosBoxControl.nativeJoystick((int)((joyX*256.0f)+mJoyCenterX), (int)((joyY*256.0f)+mJoyCenterY), ACTION_MOVE, -1);
		}

		// Handle all other keyevents
		int value = 0;
		int tKeyCode = MAP_NONE;
		
		if (hatX < 0) {
			value = customMap.get(KeyEvent.KEYCODE_DPAD_LEFT);			
			if (value > 0) {
			// found a valid mapping
				tKeyCode = getMappedKeyCode(value,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
				if (tKeyCode > MAP_NONE) {
					DosBoxControl.sendNativeKey(tKeyCode, true, mModifierCtrl, mModifierAlt, mModifierShift);
				}					
			}
			hatXlast = hatX;
		} else if (hatX > 0) {
			value = customMap.get(KeyEvent.KEYCODE_DPAD_RIGHT);			
			if (value > 0) {
			// found a valid mapping
				tKeyCode = getMappedKeyCode(value,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
				if (tKeyCode > MAP_NONE) {
					DosBoxControl.sendNativeKey(tKeyCode, true, mModifierCtrl, mModifierAlt, mModifierShift);
				}					
			}				
			hatXlast = hatX;
		} else {
			// released
			if (hatX != hatXlast) {
				if (hatXlast < 0) {
					value = customMap.get(KeyEvent.KEYCODE_DPAD_LEFT);			
					if (value > 0) {
					// found a valid mapping
						tKeyCode = getMappedKeyCode(value,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
						if (tKeyCode > MAP_NONE) {
							DosBoxControl.sendNativeKey(tKeyCode, false, mModifierCtrl, mModifierAlt, mModifierShift);
						}					
					}
				} else if (hatXlast > 0) {
					value = customMap.get(KeyEvent.KEYCODE_DPAD_RIGHT);			
					if (value > 0) {
					// found a valid mapping
						tKeyCode = getMappedKeyCode(value,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
						if (tKeyCode > MAP_NONE) {
							DosBoxControl.sendNativeKey(tKeyCode, false, mModifierCtrl, mModifierAlt, mModifierShift);
						}					
					}
				}
			}
			hatXlast = hatX;
		}
		if (hatY < 0) {
			value = customMap.get(KeyEvent.KEYCODE_DPAD_UP);			
			if (value > 0) {
			// found a valid mapping
				tKeyCode = getMappedKeyCode(value,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
				if (tKeyCode > MAP_NONE) {
					DosBoxControl.sendNativeKey(tKeyCode, true, mModifierCtrl, mModifierAlt, mModifierShift);
				}					
			}
			hatYlast = hatY;
		} else if (hatY > 0) {
			value = customMap.get(KeyEvent.KEYCODE_DPAD_DOWN);			
			if (value > 0) {
			// found a valid mapping
				tKeyCode = getMappedKeyCode(value,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
				if (tKeyCode > MAP_NONE) {
					DosBoxControl.sendNativeKey(tKeyCode, true, mModifierCtrl, mModifierAlt, mModifierShift);
				}					
			}				
			hatYlast = hatY;
		} else {
			// released
			if (hatY != hatYlast) {
				if (hatYlast < 0) {
					value = customMap.get(KeyEvent.KEYCODE_DPAD_UP);			
					if (value > 0) {
					// found a valid mapping
						tKeyCode = getMappedKeyCode(value,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
						if (tKeyCode > MAP_NONE) {
							DosBoxControl.sendNativeKey(tKeyCode, false, mModifierCtrl, mModifierAlt, mModifierShift);
						}					
					}
				} else if (hatYlast > 0) {
					value = customMap.get(KeyEvent.KEYCODE_DPAD_DOWN);			
					if (value > 0) {
					// found a valid mapping
						tKeyCode = getMappedKeyCode(value,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
						if (tKeyCode > MAP_NONE) {
							DosBoxControl.sendNativeKey(tKeyCode, false, mModifierCtrl, mModifierAlt, mModifierShift);
						}					
					}
				}
			}
			hatYlast = hatY;
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (event.getEventTime()+EVENT_THRESHOLD_DECAY < SystemClock.uptimeMillis()) {
			//Log.i("DosBoxTurbo","eventtime: "+event.getEventTime() + " systemtime: "+SystemClock.uptimeMillis());
			return true;	// get rid of old events
		}
		final int pointerIndex = MotionEventCompat.getActionIndex(event);
		final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

		if ((MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_MOVE) &&  ((mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK) == TouchEventWrapper.SOURCE_CLASS_JOYSTICK)) {
			if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) && (mAnalogStickPref < 3)) {
				// use new 3.1 API to handle joystick movements
				int historySize = event.getHistorySize();
				for (int i = 0; i < historySize; i++) {
					processJoystickInput(event, i);
				}
				
				processJoystickInput(event, -1);
				return true;
			} else {
				// use older 2.2+ API to handle joystick movements
				if (mInputMode == INPUT_MODE_REAL_JOYSTICK) {
					x[pointerId] = mWrap.getX(event, pointerId);
					y[pointerId] = mWrap.getY(event, pointerId);
					DosBoxControl.nativeJoystick((int)((x[pointerId]*256f)+mJoyCenterX), (int)((y[pointerId]*256f)+mJoyCenterY), ACTION_MOVE, -1);
					return true;
				}
			}
		} else if ((MotionEventCompat.getActionMasked(event) == MotionEventCompat.ACTION_HOVER_MOVE) && ((mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK) == TouchEventWrapper.SOURCE_CLASS_POINTER) ) {
			if (mInputMode == INPUT_MODE_REAL_MOUSE) {
			    x_last[pointerId] = x[pointerId];
			    y_last[pointerId] = y[pointerId];
			    x[pointerId] = mWrap.getX(event, pointerId);
			    y[pointerId] = mWrap.getY(event, pointerId);
				if (mAbsolute) {
					DosBoxControl.nativeMouseWarp(x[pointerId], y[pointerId], mRenderer.x, mRenderer.y, mRenderer.width, mRenderer.height);
				} else {
					DosBoxControl.nativeMouse((int) (x[pointerId]*mMouseSensitivityX), (int) (y[pointerId]*mMouseSensitivityY), (int) (x_last[pointerId]*mMouseSensitivityX), (int) (y_last[pointerId]*mMouseSensitivityY), 2, -1);
				}
					
				int buttonState = mWrap.getButtonState(event);
				if (((buttonState & TouchEventWrapper.BUTTON_SECONDARY) != 0)&& !mSPenButton) {
					// Handle Samsung SPen Button (RMB) - DOWN
					DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_DOWN, BTN_B);
					mSPenButton = true;
				} else if (((buttonState & TouchEventWrapper.BUTTON_SECONDARY) == 0)&& mSPenButton) {
					// Handle Samsung SPen Button (RMB) - UP
					DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_UP, BTN_B);
					mSPenButton = false;
				} 
					
				if (mDebug)
					Log.d("DosBoxTurbo","onGenericMotionEvent() INPUT_MODE_REAL_MOUSE x: " + x[pointerId] + "  y: " + y[pointerId] + "  |  xL: "+ x_last[pointerId] + "  yL: "+ y_last[pointerId]);
				try {
			    	if (!mInputLowLatency) 
			    		Thread.sleep(95);
			    	else 
			    		Thread.sleep(65);
				} catch (InterruptedException e) {
				}
				return true;
			}
		} else if (MotionEventCompat.getActionMasked(event) == MotionEventCompat.ACTION_HOVER_EXIT) {
			if (mInputMode == INPUT_MODE_REAL_MOUSE) {
				// hover exit
				int buttonState = mWrap.getButtonState(event);
				if (((buttonState & TouchEventWrapper.BUTTON_SECONDARY) == 0)&& mSPenButton) {
					// Handle Samsung SPen Button (RMB) - UP
					DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_UP, BTN_B);
					mSPenButton = false;
					return true;
				}
			}
		}	

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
			return super.onGenericMotionEvent(event);
		} else {
			return false;
		}
	}
	
	
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		final int pointerIndex = MotionEventCompat.getActionIndex(event);
		final int pointCnt = mWrap.getPointerCount(event);
		final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);
		if (pointCnt < MAX_POINT_CNT){
			//if (pointerIndex <= MAX_POINT_CNT - 1){
			{
				for (int i = 0; i < pointCnt; i++) {
					int id = MotionEventCompat.getPointerId(event, i);
					if (id < MAX_POINT_CNT) {
						x_last[id] = x[id];
						y_last[id] = y[id];
						x[id] = mWrap.getX(event, i);
						y[id] = mWrap.getY(event, i);
					}
				} 
				switch (MotionEventCompat.getActionMasked(event)) {
				case MotionEvent.ACTION_DOWN:
				case MotionEventCompat.ACTION_POINTER_DOWN:
					int button = -1;
			        // Save the ID of this pointer
			        if (mInputMode == INPUT_MODE_MOUSE) {
					} else if (mInputMode == INPUT_MODE_REAL_JOYSTICK) {
						int buttonState = mWrap.getButtonState(event);
						if ((buttonState & TouchEventWrapper.BUTTON_PRIMARY) != 0) {
							button = BTN_A;
						} else
						if ((buttonState & TouchEventWrapper.BUTTON_SECONDARY) != 0) {
							button = BTN_B;
						}
						DosBoxControl.nativeJoystick(0, 0, ACTION_DOWN, button);
					} else if (mInputMode == INPUT_MODE_REAL_MOUSE) {
						int buttonState = mWrap.getButtonState(event);
						if ((buttonState & TouchEventWrapper.BUTTON_PRIMARY) != 0) {
							button = BTN_A;
						} else
						if ((buttonState & TouchEventWrapper.BUTTON_SECONDARY) != 0) {
							button = BTN_B;
						} else 
						if (buttonState == 0) {
							// handle trackpad presses as button clicks
							button = BTN_A;
						} 
						DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_DOWN, button);
					}
					mButtonDown[pointerId] = button;
				break;
				case MotionEvent.ACTION_UP: 
				case MotionEventCompat.ACTION_POINTER_UP:
					if (mInputMode == INPUT_MODE_MOUSE){
						if (mLongClick) {
							// single tap long click release
							DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_UP, mGestureSingleClick-GESTURE_LEFT_CLICK);
							mLongClick = false;
							Log.i("DosBoxTurbo","SingleTap Long Click Release");
							return true;
						} else if (mDoubleLong) {
							// double tap long click release
							try {
								Thread.sleep(CLICK_DELAY);
							} catch (InterruptedException e) {
							}
							DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_UP, mGestureDoubleClick-GESTURE_LEFT_CLICK);
							Log.i("DosBoxTurbo","DoubleTap Long Click Release");
							mDoubleLong = false;
							//return true;
						} else if (pointCnt == 2) {
							// handle 2 finger tap gesture
							if (mLongPress) {
								if (!mTwoFingerAction) {
									// press button down
									Log.i("DosBoxTurbo","2-Finger Long Click Down");
									DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_DOWN, mGestureTwoFinger-GESTURE_LEFT_CLICK);
									mTwoFingerAction = true;
								} else {
									// already pressing button - release and press again
									Log.i("DosBoxTurbo","2-Finger Long Click - AGAIN");
									DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_UP, mGestureTwoFinger-GESTURE_LEFT_CLICK);
									try {
										Thread.sleep(CLICK_DELAY);
									} catch (InterruptedException e) {
									}
									DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_DOWN, mGestureTwoFinger-GESTURE_LEFT_CLICK);
								}
							} else {
								Log.i("DosBoxTurbo","2-Finger Long Click Down-UP");
								mouseClick(mGestureTwoFinger-GESTURE_LEFT_CLICK);
							}
							return true;
						} else if ((pointCnt == 1)&& mTwoFingerAction) {
			        		// release two finger gesture
							Log.i("DosBoxTurbo","2-Finger Long Click Release");
			        		DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_UP, mGestureTwoFinger-GESTURE_LEFT_CLICK);
			        		mTwoFingerAction = false;
			        		//return true;
						}
					}
					else if (mInputMode == INPUT_MODE_REAL_MOUSE) {
						//Log.v("Mouse","BUTTON UP: " + (mButtonDown[pointerId]));
						DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_UP, mButtonDown[pointerId]);
						if (mWrap.getButtonState(event) > 0) {
							return true;	// capture button touches, pass screen touches through to gesture detetor
						}
					}
					else if (mInputMode == INPUT_MODE_REAL_JOYSTICK) {
						DosBoxControl.nativeJoystick(0, 0, ACTION_UP, (mButtonDown[pointerId]));
						if (mWrap.getButtonState(event) > 0) {
							return true;
						}
					}
				break;
				case MotionEvent.ACTION_MOVE: 
					//isTouch[pointerId] = true;
					switch(mInputMode) {
						case INPUT_MODE_SCROLL:
							mScroll_x += (int)(x[pointerId] - x_last[pointerId]);
							mScroll_y += (int)(y[pointerId] - y_last[pointerId]);
							forceRedraw();
						break;
						case INPUT_MODE_MOUSE: 
						case INPUT_MODE_REAL_MOUSE: 
							if (event.getEventTime()+EVENT_THRESHOLD_DECAY < SystemClock.uptimeMillis()) {
								Log.i("DosBoxTurbo","eventtime: "+event.getEventTime() + " systemtime: "+SystemClock.uptimeMillis());
								return true;	// get rid of old events
							}
							int idx = (!virtButton[0]) ? 0:1;
								if (mAbsolute) {
									DosBoxControl.nativeMouseWarp(x[idx], y[idx], mRenderer.x, mRenderer.y, mRenderer.width, mRenderer.height);
								} else {
									DosBoxControl.nativeMouse((int) (x[idx]*mMouseSensitivityX), (int) (y[idx]*mMouseSensitivityY), (int) (x_last[idx]*mMouseSensitivityX), (int) (y_last[idx]*mMouseSensitivityY), ACTION_MOVE, -1);
								}
								if (mDebug) { 
									Log.d("DosBoxTurbo", "mAbsolute="+mAbsolute+" MotionEvent MOVE("+pointerId+")"+" x[idx]="+x[idx] + " y[idx]"+y[idx] + " mRenderer.x="+mRenderer.x + " mRenderer.y="+mRenderer.y + " mRenderer.width="+mRenderer.width + " mRenderer.height="+mRenderer.height);
								}
								try {
							    	if (!mInputLowLatency) 
							    		Thread.sleep(95);
							    	else
							    		Thread.sleep(65);  
								} catch (InterruptedException e) {
								}

						break;
						default:
					}
				break;
				}
			}
		}
	    try {
	    	Thread.sleep(15);
	    } catch (InterruptedException e) {
	        e.printStackTrace();
	    } 
	    //Thread.yield();
	    return gestureScanner.onTouchEvent(event);
	}

	private final static int MAP_EVENT_CONSUMED = -1;
	private final static int MAP_NONE = 0;
	private final static int MAP_LEFTCLICK 	= 20000;
	private final static int MAP_RIGHTCLICK 	= 20001;
	private final static int MAP_CYCLEUP 		= 20002;
	private final static int MAP_CYCLEDOWN	= 20003;
	private final static int MAP_SHOWKEYBOARD = 20004;
	private final static int MAP_SPECIALKEYS	= 20005;
	private final static int MAP_ADJUSTCYCLES	= 20006;
	private final static int MAP_ADJUSTFRAMES	= 20007;
	private final static int MAP_UNLOCK_SPEED = 20008;
	private final static int MAP_JOYBTN_A = 20009;
	private final static int MAP_JOYBTN_B = 20010;
			
	
	//private boolean mMapCapture = false;
	
	// Map of Custom Maps
	public SparseIntArray customMap = new SparseIntArray(DosBoxPreferences.NUM_USB_MAPPINGS);

	private final int getMappedKeyCode(final int button, final KeyEvent event) {
		switch (button) {
		case MAP_LEFTCLICK:
			DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_A);			
			return MAP_EVENT_CONSUMED;
		case MAP_RIGHTCLICK: 
			DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_B);			
			return MAP_EVENT_CONSUMED;
		case MAP_JOYBTN_A:
			DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_A);				
			return MAP_EVENT_CONSUMED;
		case MAP_JOYBTN_B: 
			DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_B);				
			return MAP_EVENT_CONSUMED;
		case MAP_CYCLEUP: 
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (mParent.mTurboOn) { 
					mParent.mTurboOn = false;
					DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON, mParent.mTurboOn?1:0, null,true);			
				} 
				DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_CYCLE_ADJUST, 1, null,true);
				if (DosBoxControl.nativeGetAutoAdjust()) {
					Toast.makeText(mParent, "Auto Cycles ["+DosBoxControl.nativeGetCycleCount() +"%]", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mParent, "DosBox Cycles: "+DosBoxControl.nativeGetCycleCount(), Toast.LENGTH_SHORT).show();
				}
			}
			return MAP_EVENT_CONSUMED;
		case MAP_CYCLEDOWN:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (mParent.mTurboOn) { 
					mParent.mTurboOn = false;
					DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON, mParent.mTurboOn?1:0, null,true);			
				} 
				DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_CYCLE_ADJUST, 0, null,true);
				if (DosBoxControl.nativeGetAutoAdjust()) {
					Toast.makeText(mParent, "Auto Cycles ["+DosBoxControl.nativeGetCycleCount() +"%]", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mParent, "DosBox Cycles: "+DosBoxControl.nativeGetCycleCount(), Toast.LENGTH_SHORT).show();
				}
			}
			return MAP_EVENT_CONSUMED;
		case MAP_SHOWKEYBOARD:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				DBMenuSystem.doShowKeyboard(mParent);
			}
			return MAP_EVENT_CONSUMED;
		case MAP_SPECIALKEYS:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				mContextMenu = DBMenuSystem.CONTEXT_MENU_SPECIAL_KEYS;
				mParent.openContextMenu(this);
			}
			return MAP_EVENT_CONSUMED;
		case MAP_ADJUSTCYCLES:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				mContextMenu = DBMenuSystem.CONTEXT_MENU_CYCLES;
				mParent.openContextMenu(this);
			}
			return MAP_EVENT_CONSUMED;
		case MAP_ADJUSTFRAMES:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				mContextMenu = DBMenuSystem.CONTEXT_MENU_FRAMESKIP;
				mParent.openContextMenu(this);
			}
			return MAP_EVENT_CONSUMED;
		case MAP_UNLOCK_SPEED:
			if (mParent.mTurboOn) {
				if (event.getAction() == KeyEvent.ACTION_UP) {
					DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON, 0, null,true);	// turn off
					mParent.mTurboOn = false;
				}
			} else {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON, 1, null,true);	// turn on
					mParent.mTurboOn = true;
				}
			}
			return MAP_EVENT_CONSUMED;
		default:
			return button;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, final KeyEvent event) {
		if (mDebug)
			Log.d("DosBoxTurbo", "onKeyDown keyCode="+keyCode + " mEnableDpad=" + mEnableDpad);

		if (mEnableDpad) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					y[0] -= mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0], (int)y[0]+mDpadRate, 2, -1);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, -1024, 2, -1);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					y[0] += mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0], (int)y[0]-mDpadRate, 2, -1);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, 1024, 2, -1);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					x[0] -= mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0]+mDpadRate, (int)y[0], 2, -1);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(-1024, 0, 2, -1);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					x[0] += mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0]-mDpadRate, (int)y[0], 2, -1);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(1024, 0, 2, -1);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:	// button
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					DosBoxControl.nativeMouse(0, 0, 0, 0, 0, BTN_A);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, 0, 0, BTN_A);
					return true;
				}
				break;
			}
		}
		return handleKey(keyCode, event);			
	}
	
	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		if (mDebug)
			Log.d("DosBoxTurbo", "onKeyUp keyCode="+keyCode);

		if (mEnableDpad) {
			switch (keyCode) {
				// 	DPAD / TRACKBALL
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
						DosBoxControl.nativeJoystick(0, 0, 2, -1);
					}
				return true;
				case KeyEvent.KEYCODE_DPAD_CENTER:	// button
					if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
						DosBoxControl.nativeMouse(0, 0, 0, 0, 1, BTN_A);
					} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
						DosBoxControl.nativeJoystick(0, 0, 1, BTN_A);
					} 
				return true;
			}
		}
		return handleKey(keyCode, event);
	}
	 
	private boolean handleKey(int keyCode, final KeyEvent event) {
		if (mDebug)
			Log.d("DosBoxTurbo", "handleKey keyCode="+keyCode);
		int tKeyCode = 0;

		// check for xperia play back case
		if (keyCode == KeyEvent.KEYCODE_BACK && event.isAltPressed()) {
			int backval = customMap.get(DosBoxPreferences.XPERIA_BACK_BUTTON);
			if (backval > 0) {
				// Special Sony XPeria Play case
				if (mEnableDpad) {
					// FIRE2
					if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
						DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_B);
					} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
						DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_B);
					} 
				} else {
					// sony xperia play O (circle) button
					DosBoxControl.sendNativeKey(backval, (event.getAction() == KeyEvent.ACTION_DOWN), mModifierCtrl, mModifierAlt, mModifierShift);
					return true;	// consume event
				}
			}
			return true;	// consume event
		}
		
		// Handle all other keyevents
		int value = customMap.get(keyCode);
		
		if (value > 0) {
			// found a valid mapping
			tKeyCode = getMappedKeyCode(value,event);
			if (tKeyCode > MAP_NONE) {
				DosBoxControl.sendNativeKey(tKeyCode, (event.getAction() == KeyEvent.ACTION_DOWN), mModifierCtrl, mModifierAlt, mModifierShift);
				return true; // consume KeyEvent
			} else if (tKeyCode == MAP_EVENT_CONSUMED) {
				return true;
			}
		}
		
		if (keyCode == KeyEvent.KEYCODE_BACK) { 
			// fishstix, allow remap of Android back button
			// catch no mapping
			if (event.getAction() == KeyEvent.ACTION_UP) {
				DBMenuSystem.doConfirmQuit(mParent);
			}
			return true;					
		}

		switch (keyCode) {
		case KeyEvent.KEYCODE_UNKNOWN:
			break;
						
		default:
			boolean	down = (event.getAction() == KeyEvent.ACTION_DOWN);
			if (mDebug)
				Log.d("DosBoxTurbo", "handleKey (default) keyCode="+keyCode + " down="+down);
		
			if (!down || (event.getRepeatCount() == 0)) {
				int unicode = event.getUnicodeChar();
			
				// filter system generated keys, but not hardware keypresses
				if ((event.isAltPressed() || event.isShiftPressed()) && (unicode == 0) && ((event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) == 0))
					break;

				//fixed alt key problem for physical keyboard with only left alt
				if ((!mUseLeftAltOn) && (keyCode == KeyEvent.KEYCODE_ALT_LEFT)) {
					break;
				}
			
				if ((!mUseLeftAltOn) && (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)) {
					break;
				}
			
				if ((keyCode > 255) || (unicode > 255)) {
					//unknown keys
					break;
				}
							
				keyCode = keyCode | (unicode << 8);

				long diff = event.getEventTime() - event.getDownTime();
			
				if (!down && (diff < 50)) {
					//simulate as long press
					if (mDebug) 
						Log.d("DosBoxTurbo", "LongPress consumed keyCode="+keyCode + " down="+down);
					mKeyHandler.removeMessages(keyCode);
					mKeyHandler.sendEmptyMessageDelayed(keyCode, BUTTON_REPEAT_DELAY - diff);
				}
				else if (down && mKeyHandler.hasMessages(keyCode)) {
					if (mDebug)
						Log.d("DosBoxTurbo", "KeyUp consumed keyCode="+keyCode + " down="+down);
					//there is an key up in queue, should be repeated event
				} else {
					boolean result = DosBoxControl.sendNativeKey(keyCode, down, mModifierCtrl, mModifierAlt, mModifierShift);
					if (!down) {
						mModifierCtrl = false; 
						mModifierAlt = false;  
						mModifierShift = false;
					}
					return result;
				}
			}
		}

		return false;
	}
	
	public void setDirty() {
		mDirtyCount = 0;
		bDirtyCoords.set(true);
		mDirty.set(true);
	}
	
	public void resetScreen(boolean redraw) {
		setDirty();
		mScroll_x = 0;
		mScroll_y = 0;
		
		if (redraw)
			forceRedraw(); 	
	}
	
	public void forceRedraw() {
		setDirty();
		VideoRedraw(mBitmap, mSrc_width, mSrc_height, 0, mSrc_height);		
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		resetScreen(true);
		if (mGPURendering)
			super.surfaceChanged(holder, format, width, height);
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
		mSurfaceViewRunning = true;
		if (mGPURendering)
			super.surfaceCreated(holder);
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		mSurfaceViewRunning = false;
		if (mGPURendering)
			super.surfaceDestroyed(holder);
	}
	
	private final void mouseClick(int button) {
 		DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_DOWN, button);
		try {
			Thread.sleep(CLICK_DELAY);
		} catch (InterruptedException e) {
		}
		DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_UP, button);
	}
	
	// Fix for Motorola Keyboards!!! - fishstix
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		return new BaseInputConnection(this, false) {
			@Override
			public boolean sendKeyEvent(KeyEvent event) {
				return super.sendKeyEvent(event);
			}
			@Override
			public boolean deleteSurroundingText(int beforeLength, int afterLength) {       
			    // magic: in latest Android, deleteSurroundingText(1, 0) will be called for backspace
			    if (beforeLength == 1 && afterLength == 0) {
			        // backspace
			        super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
			        return super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
			    }

			    return super.deleteSurroundingText(beforeLength, afterLength);
			}
		};
	}
		
	private final static int GESTURE_NONE = 0;
	public final static int GESTURE_LEFT_CLICK = 3;
	public final static int GESTURE_RIGHT_CLICK = 4;
	public final static int GESTURE_DOUBLE_CLICK = 5;
	public int mGestureUp = GESTURE_NONE;
	public int mGestureDown = GESTURE_NONE;
	public int mGestureSingleClick = GESTURE_NONE;
	public int mGestureDoubleClick = GESTURE_NONE;
	public int mGestureTwoFinger = GESTURE_NONE;
	public boolean mTwoFingerAction = false;
	
    class MyGestureDetector extends SimpleOnGestureListener {
    	@Override
    	public boolean onDown(MotionEvent event) {
			Log.i("DosBoxTurbo","onDown()");
			if (mInputMode == INPUT_MODE_MOUSE) {
				if (mAbsolute) {
   	       			final int pointerId = mWrap.getPointerId(event, ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT));
   	       			DosBoxControl.nativeMouseWarp(x[pointerId], y[pointerId], mRenderer.x, mRenderer.y, mRenderer.width, mRenderer.height);
   	       			try {
   	       				Thread.sleep(85);
   	       			} catch (InterruptedException e) {
   	       			}
				}
			}
      		return true; 
    	}
    	private static final int SWIPE_MAX_OFF_PATH = 75;
        @Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
    			float velocityY) {
        	final float density = getResources().getDisplayMetrics().density;
        	int mMarginTouch = (int) (100 * density + 0.5f);		// 100dp top margin
        	if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        		return false;
        	}
        	if (e1.getY() < e2.getY()) { 
        		// swipe down
            	if (e1.getY() > mMarginTouch) {
            		return false;
            	}
            	if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH) {
                    return false;
        		}
       			mParent.getSupportActionBar().show();
       			return true;
        	} else {
        		// swipe up
            	if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH) {
                    return false;
        		}
        		if (mParent.getSupportActionBar().isShowing()) {
        			mParent.getSupportActionBar().hide();
        			return true;
        		}
        		return false;
        	}
    	}
        
        @Override
    	public boolean onDoubleTap(MotionEvent event) {
			//Log.i("DosBoxTurbo","onDoubleTap()");
			if (mInputMode == INPUT_MODE_MOUSE) {
        		switch (mGestureDoubleClick) {
        		case GESTURE_LEFT_CLICK:
        		case GESTURE_RIGHT_CLICK:
        			if (mLongPress) {
        				mDoubleLong = true;
        				DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_DOWN, mGestureDoubleClick-GESTURE_LEFT_CLICK);
        			} else 
        				mouseClick(mGestureDoubleClick-GESTURE_LEFT_CLICK);
        			return true;
        		case GESTURE_DOUBLE_CLICK:
        			mouseClick(BTN_A);
        			try{
        				Thread.sleep(CLICK_DELAY);
        			} catch (InterruptedException e) {
        			}
        			mouseClick(BTN_A);
        		}
        	}
    		return false;
    	}
        
        @Override
    	public boolean onSingleTapConfirmed(MotionEvent event) {
			Log.i("DosBoxTurbo","onSingleTapConfirmed()");
        	if (mInputMode == INPUT_MODE_MOUSE) {
        		if ((mGestureSingleClick != GESTURE_NONE)&&(mGestureDoubleClick != GESTURE_NONE)) {
        			mouseClick(mGestureSingleClick-GESTURE_LEFT_CLICK);
        			return true;
        		}
        	}
       		return false;
    	} 
        
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
        	Log.i("DosBoxTurbo","onSingleTapUp()");
        	if (mInputMode == INPUT_MODE_MOUSE) {
        		if ((mGestureDoubleClick == GESTURE_NONE)&&(mGestureSingleClick != GESTURE_NONE)) {	// fishstix,fire only when doubleclick gesture is disabled
        			mouseClick(mGestureSingleClick-GESTURE_LEFT_CLICK);
        			return true;
        		}
        	}

      		return false;  
        } 
        
        @Override
        public void onLongPress(MotionEvent event) {
			//Log.i("DosBoxTurbo","onLongPress()");
			if (mInputMode == INPUT_MODE_MOUSE)  {
				if (!mFilterLongClick && mLongPress && !mDoubleLong && !mTwoFingerAction) {
					mLongClick = true;
					if (mGestureSingleClick != GESTURE_NONE) {
						DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_DOWN, mGestureSingleClick-GESTURE_LEFT_CLICK);
					}
				}
				mFilterLongClick = false;
			}
        }
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	public static class InputDeviceState {
        private InputDevice	mDevice;
        private int[]		mAxes;
        private float[]		mAxisValues;
        private SparseIntArray	mKeys;
        
        
		public InputDeviceState(InputDevice device) {
            mDevice = device;
            int numAxes = 0;
            for (MotionRange range : device.getMotionRanges()) {
                 if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                      numAxes += 1;
                 }
            }

            mAxes		= new int[numAxes];
            mAxisValues	= new float[numAxes];
            mKeys		= new SparseIntArray();

            int i = 0;
            for (MotionRange range : device.getMotionRanges()) {
                 if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                      mAxes[i++] = range.getAxis();
                 }
            }
       }
        
       public static float ProcessAxis(InputDevice.MotionRange range, float axisvalue) {
       	    float absaxisvalue = Math.abs(axisvalue);
       	    float deadzone = range.getFlat();
       	    if (absaxisvalue <= deadzone) {
       	         return 0.0f;
       	    }
       	    float normalizedvalue;
       	    if (axisvalue < 0.0f) {
       	         normalizedvalue = absaxisvalue / range.getMin();
       	    } else {
       	         normalizedvalue = absaxisvalue / range.getMax();
       	    }

       	    return normalizedvalue;
       }
       

    }
}

