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
package javax.microedition.media;

import java.util.Vector;

import javax.microedition.media.control.PlayerVolumeControl;

import nds.pstros.MainApp;


class DummyPlayer implements Player{
	
	protected static final String EMU_CONTROL_VOLUME = "VolumeControl";
	protected static final String EMU_CONTROL_VOLUME_FULL = "javax.microedition.media.control.VolumeControl";
	
	protected int oldState;
	protected int state;
	private int level = 100;
	private Vector listeners;
	protected int xHash;
	
	public DummyPlayer() {
		oldState = UNREALIZED;
		state = UNREALIZED;
		listeners = new Vector(2);
	}

	public void addPlayerListener(PlayerListener playerListener) throws IllegalStateException {
		if (playerListener == null) {
			return;
		}
		if (MainApp.verbose) {
			System.out.println("Player="  + this + " addPlayerListener listener=" + playerListener);
		}
		listeners.addElement(playerListener);
	}

	public void close() {
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " close()");
		}
		state = CLOSED;
		emuReportEvent(PlayerListener.CLOSED, null);

	}

	public void deallocate() throws IllegalStateException {
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " deallocate()");
		}
		state = REALIZED;
	}

	public String getContentType() throws IllegalStateException {
		return null;
	}

	public long getDuration() throws IllegalStateException {
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " getDuration()");
		}
		return 0;
	}

	public long getMediaTime(){
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " getMediaTime()");
		}
		
		if (state == CLOSED) {
			throw new RuntimeException();
		}
		
		return 0;
	}

	public int getState() {
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " getState()");
		}
		return state;
	}

	public void prefetch() throws MediaException {
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " prefetch()");
		}
		state = PREFETCHED;
	}

	public void realize() throws MediaException {
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " realize()");
		}
		state = REALIZED;
	}

	public void removePlayerListener(PlayerListener playerListener)	throws IllegalStateException {
		if (playerListener == null) {
			return;
		}
		listeners.removeElement(playerListener);
	}

	public void setLoopCount(int count){
		if (count == 0) {
			throw new IllegalArgumentException("count is invalid");
		}
		
		if (state == STARTED || state == CLOSED) {
			throw new IllegalStateException();
		}
		
	}

	public long setMediaTime(long now) throws MediaException {
		if (state == UNREALIZED || state == CLOSED) {
			throw new IllegalStateException();
		}
		return 0;
	}

	public void start() throws MediaException {
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " start()");
		}
		
		if (state == CLOSED) {
			throw new IllegalStateException("Player is in the CLOSED state.");
		}

		startImpl();
		
		state = STARTED;
		emuReportEvent(PlayerListener.STARTED, null);
	}
	
	protected void startImpl() throws MediaException {
	}

	public void stop() throws MediaException {
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " stop()");
		}
		
		if (state == CLOSED) {
			throw new IllegalStateException("Player is in the CLOSED state.");
		}
	
		stopImpl();
		
		state = PREFETCHED;
		oldState = STARTED;
		xHash = hashCode();
	}
	public void stopImpl() throws MediaException {
	}

	public Control getControl(String controlType) {
		if (MainApp.soundVerbose) {
			System.out.println("Player@" + hashCode() + " getControl() " + controlType);
		}
		if (controlType.equals(EMU_CONTROL_VOLUME) || controlType.equals(EMU_CONTROL_VOLUME_FULL)) {
			return new PlayerVolumeControl(this);
		}
		return null;
	}

	public Control[] getControls() {
		//System.out.println("DummyPlayer: get controls...");
		return null;
	}
	public void emuUpdatePlayer() {
	}
	
	public int emuGetVolumeLevel() {
		return level;		
	}
	public int emuSetVolumeLevel(int level) {
		this.level = level;
		return level; 
	}
	
	protected void emuReportEvent(String event, Object data) {
		int size = listeners.size();
		if (size < 1) {
			return;
		}
		if (MainApp.verbose) {
			System.out.println("Player="  + this + " reporting the event=" + event);
		}
		PlayerListener listener;
		for (int i = 0; i < size; i++) {
			listener = (PlayerListener) listeners.elementAt(i);
			listener.playerUpdate(this, event, data);
		}
	} 

}
