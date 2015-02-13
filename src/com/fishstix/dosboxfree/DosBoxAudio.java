/*
 *  Copyright (C) 2012 Fishstix - Based upon Dosbox & AnDOSBox by locnet
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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
 
public class DosBoxAudio 
{
	private boolean mAudioRunning=true;
	private AudioTrack mAudio = null;
	private DBMain mParent = null;

	public short[] mAudioBuffer = null;	

	public DosBoxAudio(DBMain ctx) {
		mParent = ctx;
	}
	
	@SuppressWarnings("deprecation")
	public int initAudio(int rate, int channels, int encoding, int bufSize)
	{
		if( mAudio == null )
		{ 
			channels = ( channels == 1 ) ? AudioFormat.CHANNEL_CONFIGURATION_MONO : 
											AudioFormat.CHANNEL_CONFIGURATION_STEREO;
			encoding = ( encoding == 1 ) ? AudioFormat.ENCODING_PCM_16BIT :
											AudioFormat.ENCODING_PCM_8BIT;
			int androidAudioBufSize = AudioTrack.getMinBufferSize( rate, channels, encoding );
			if( androidAudioBufSize > bufSize ) {
				androidAudioBufSize = Math.max(androidAudioBufSize, bufSize);
			}
			mAudioMinUpdateInterval = 1000*(bufSize >> 1)/((channels == AudioFormat.CHANNEL_CONFIGURATION_MONO)?1:2)/rate;
			mAudioBuffer = new short[bufSize >> ((mParent.mPrefMixerHackOn == true)?3:2)];
			//mAudioBuffer = new short[bufSize >> 2];
			mAudio = new AudioTrack(AudioManager.STREAM_MUSIC, 
										rate,
										channels,
										encoding,
										androidAudioBufSize, 
										AudioTrack.MODE_STREAM );
			mAudio.pause();
			
			return bufSize;
		}
		
		return 0;
	}
    
	public void shutDownAudio() {
	   if (mAudio != null) {
		   mAudio.stop();
		   mAudio.release();
		   mAudio = null;
	   }
	   mAudioBuffer = null;	
	}

	private long mLastWriteBufferTime = 0;	
	private int mAudioMinUpdateInterval = 50;

	public void AudioWriteBuffer(int size) {
		if ((mAudioBuffer != null) && mAudioRunning) {
			long now = System.currentTimeMillis();
			if ((!mParent.mTurboOn) || ((now - mLastWriteBufferTime) > mAudioMinUpdateInterval)) {
				if (size > 0)
					writeSamples( mAudioBuffer, (size << 1 ) );
				mLastWriteBufferTime = now;
			}
		}
	}
   
	public void setRunning() {
		mAudioRunning = !mAudioRunning;
		if (!mAudioRunning)
			mAudio.pause();
	}  
   
   public void writeSamples(short[] samples, int size) 
   {
	   if (mAudioRunning) {
	      if (mAudio != null) {
	    	  mAudio.write( samples, 0, size );
	    	  if (mAudio.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
	    		  play();
	      }
	   }
   }

   public void play() {
	   if (mAudio != null)
		   mAudio.play();
   }
   
   public void pause() {
	   if (mAudio != null)
		   mAudio.pause();
   }   
}

