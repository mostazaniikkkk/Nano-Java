/* **************************************************************************
 This file is part of the Pstros J2ME execution environment 
 
 Copyright (C) 2005-2007, Marek Olejnik
 
 Disclaimer of Warranty
 
 These software programs are available to the user without any license fee or
 royalty on an "as is" basis.  The copyright-holder disclaims any and all 
 warranties, whether express, implied, or statuary, including any implied 
 warranties or merchantability or of fitness for a particular  purpose. In no 
 event shall the copyright-holder be liable for any incidental, punitive, 
 or consequential damages of any kind whatsoever arising from the use 
 of these programs.
 
 This disclaimer of warranty extends to the user of these programs and user's
 customers, employees, agents, transferees, successors, and assigns.

 Maintainers:
 Marek Olejnik - ole00@post.cz

***************************************************************************/
package com.nokia.mid.sound;


public class Sound extends java.lang.Object{
	
	public static final int FORMAT_TONE = 1;
	public static final int FORMAT_WAV = 5;
	
	public static final int SOUND_PLAYING = 0;
	public static final int SOUND_STOPPED = 1;
	public static final int SOUND_UNINITIALIZED = 3;
	
	private static final int[] SUPPORTED_FORMATS = { FORMAT_TONE, FORMAT_WAV};
	
	private byte[] data;
	private int type;
	private int state;
	private int freq;
	private long duration;
	private int gain;
	private SoundListener listener;
	
	
	public Sound(byte[] data, int type) {
		init(data, type);
	}
	
	public Sound(int freq, long duration) {
		init(freq, duration);
	}
	
	public static int getConcurrentSoundCount(int type) {
		return 1;
	}
	
	public static int[] getSupportedFormats() {
		return SUPPORTED_FORMATS;
	}
	
	public void init(byte[] data, int type) {
		type = FORMAT_WAV;
		this.data = data;
		this.type = type;
		state = SOUND_STOPPED;
	}
	
	public void init(int freq, long duration) {
		type = FORMAT_TONE;
		this.freq = freq;
		this.duration = duration;
		state = SOUND_STOPPED;
	}

	public void play(int loop) {
		state = SOUND_PLAYING;
		emuNotifyListener();
	}	
	public void stop() {
		state = SOUND_STOPPED;
		emuNotifyListener();
	}
	public void resume() {
		state = SOUND_PLAYING;
		emuNotifyListener();
	}
	
	public void setGain(int gain) {
		this.gain = gain;
	}
	public int getGain() {
		return gain;
	}
	public int getState() {
		return state;
	}
	
	public void release() {
		data = null;
	}
	
	public void setSoundListener(SoundListener listener) {
		this.listener = listener;	
	}

	private void emuNotifyListener() {
		if (listener == null) {
			return;
		}
		listener.soundStateChanged(this, state);
	}	
}
