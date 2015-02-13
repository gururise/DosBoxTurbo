package com.fishstix.dosboxfree;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

public class OpenGLRenderer implements GLSurfaceView.Renderer {

    private int[] mTextureName = new int[1];       // Hold our texture id
    //private Context mContext;         // context handle for resource id
    private int mViewWidth;
    private int mViewHeight;
    private Bitmap mBitmap;
    public int[] mCropWorkspace = {0,0,0,0};
    public int x,y,width,height;
   // public boolean npot_extension = true;
    public int error_cnt = 0;
    public boolean filter_on = false;
    private Context mContext;
    
    /**
     * Constructor
     *
     * @param context
     * @param width of surface
     * @param height if surface
     */
    public OpenGLRenderer(Context context) {
    	mContext = context;
    }
    
 
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
    	//Log.i("DosBoxTurbo", "onSurfaceCreated - Default Form");

        gl10.glClearColor(0.0f, 0.0f, 0.0f, 1);
        gl10.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl10.glShadeModel(GL10.GL_FLAT);
        gl10.glDisable(GL10.GL_DEPTH_TEST);
        gl10.glEnable(GL10.GL_BLEND);
        gl10.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA); 

        gl10.glViewport(0, 0, mViewWidth,  mViewHeight);
        gl10.glMatrixMode(GL10.GL_PROJECTION);
        gl10.glLoadIdentity();
        gl10.glEnable(GL10.GL_BLEND);
        gl10.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl10.glShadeModel(GL10.GL_FLAT);
        gl10.glEnable(GL10.GL_TEXTURE_2D);

        GLU.gluOrtho2D(gl10, 0, mViewWidth, mViewHeight, 0);
        // Generate TEXTURE
    	gl10.glGenTextures(1, mTextureName, 0);
    	/*if(gl10.glGetString(GL10.GL_EXTENSIONS).contains("GL_ARB_texture_non_power_of_two") || (gl10.glGetString(GL10.GL_EXTENSIONS).contains("npot"))) {
    		Log.i("DosBoxTurbo","GL_ARB_texture_non_power_of_two extension found!");
    		npot_extension = true;
    	} else {
    		Log.i("DosBoxTurbo","GL_ARB_texture_non_power_of_two extension not found");
    		npot_extension = false;
    	}*/
    	error_cnt = 0;
    }

    /**
     * Called when the surface has changed, for example the
     * @param gl10 openGl handle
     * @param width - width
     * @param height - height
     */
    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
    	//Log.i("DosBoxTurbo", "onSurfaceChanged");
       // gl10.glViewport(0, 0, i, i1);
        /*
         * Set our projection matrix. This doesn't have to be done each time we
         * draw, but usually a new projection needs to be set when the viewport
         * is resized.
         */
       /* float ratio = (float) i / i1;
        gl10.glMatrixMode(GL10.GL_PROJECTION);
        gl10.glLoadIdentity();
        gl10.glFrustumf(-ratio, ratio, -1, 1, 1, 10); */
        mViewWidth = width;
        mViewHeight = height;

        gl10.glViewport(0, 0, mViewWidth,  mViewHeight);
        gl10.glLoadIdentity();
        GLU.gluOrtho2D(gl10, 0, mViewWidth, mViewHeight, 0);
    }

    public boolean setBitmap(Bitmap b) {
       //	Log.i("DosBoxTurbo", "setBitmap");
    	//if (npot_extension)
    		mBitmap = b;
    		//mBitmap = Bitmap.createBitmap(b,0,0,getNearestPowerOfTwoWithShifts(b.getWidth()), getNearestPowerOfTwoWithShifts(b.getHeight()), false);
       	return true; 
    }
    
    public static int getNearestPowerOfTwoWithShifts(int x){
    	// return X if it is a power of 2
    	if ((x & (x-1)) == 0 ) return x;

    	// integers in java are represented in 32 bits, so:
    	for(int i=1; i<32; i = i*2){
    	  x |= ( x >>> i);
    	}
    	return x + 1; 
    }
    
 // Notice that I don't allocate the int[] at the beginning but use the one of the image
    protected void loadSingleTexture(GL10 gl, Bitmap bmp) {
    	//int textureName = -1;
    	//gl.glDeleteTextures(1,mTextureName, 0);
    	//gl.glGenTextures(1, mTextureName, 0);
    	//textureName = mTextureName[0];

    	//Log.d("DosBoxTurbo", "Generated texture: " + mTextureName[0]);
    	gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureName[0]);
    	gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
    	gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, (filter_on)?GL10.GL_LINEAR:GL10.GL_NEAREST);
    	gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
    	gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
    	gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

    	GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
/*
    	mCropWorkspace[0] = 0;
    	mCropWorkspace[1] = bmp.getHeight();
    	mCropWorkspace[2] = bmp.getWidth();
    	mCropWorkspace[3] = -bmp.getHeight();
*/
    	//mCropWorkspace[3] = -bmp.getHeight();
    	((GL11) gl).glTexParameteriv(GL10.GL_TEXTURE_2D, 
            GL11Ext.GL_TEXTURE_CROP_RECT_OES, mCropWorkspace, 0);

    	int error = gl.glGetError();
    	if (error != GL10.GL_NO_ERROR)
    	{ 
    		Log.e("DosBoxTurbo", "GL Texture Load Error: " + error);
    		error_cnt++;
    		if (error_cnt > 10) {
    	    	// send msg
    	    	Message msg = new Message(); 
    	    	msg.what = DBMain.HANDLER_DISABLE_GPU;
    	    	Bundle b = new Bundle();

    	    	b.putString("msg", "GPU Rendering Not Supported. GPU Preference Disabled. Please Restart DosBox Turbo.");
    	    	msg.setData(b);
    	    	((DBMain)mContext).mHandler.sendMessage(msg);
    		}
    	} 
    }
    
    @Override
    public void onDrawFrame(GL10 gl) {
       //	Log.i("DosBoxTurbo", "onDrawFrame");
        // Just clear the screen and depth buffer.
       	loadSingleTexture(gl,mBitmap);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        // Begin drawing
        //--------------
        // These function calls can be experimented with for various effects such as transparency
        // although certain functionality maybe device specific.
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);

        // Setup correct projection matrix
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrthof(0.0f, mViewWidth, 0.0f, mViewHeight, 0.0f, 1.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        
        gl.glEnable(GL10.GL_TEXTURE_2D);
        
        gl.glBindTexture(GL10.GL_TEXTURE_2D,mTextureName[0]);
        ((GL11Ext) gl).glDrawTexfOES(mViewWidth-width-x, mViewHeight-height-y, 0, width, height);
        
        //Log.i("DosBoxTurbo","height: "+height + " ViewHeight: "+mViewHeight);
        //Log.i("DosBoxTurbo","width: "+width + " ViewWidth: "+mViewWidth);
        // Finish drawing
        gl.glDisable(GL10.GL_BLEND);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPopMatrix();
    }
}