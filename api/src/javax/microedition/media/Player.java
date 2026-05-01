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


public interface Player extends Controllable{
	
	public static final int UNREALIZED = 100;
	public static final int REALIZED = 200;
	public static final int PREFETCHED = 300;
	public static final int STARTED = 400;
	public static final int CLOSED = 0;
	public static final long TIME_UNKNOWN = -1;
	
	
	public void realize() throws MediaException;
	public void prefetch() throws MediaException;
	public void start() throws MediaException;
	public void stop() throws MediaException;
	public void deallocate() throws IllegalStateException;
	public void close();
	public long setMediaTime(long now) throws MediaException;
	public long getMediaTime();
	public int getState();
	public long getDuration() throws IllegalStateException;
	public String getContentType() throws IllegalStateException;
	public void setLoopCount(int count) throws Exception;
	public void addPlayerListener(PlayerListener playerListener) throws IllegalStateException;
	public void removePlayerListener(PlayerListener playerListener) throws IllegalStateException;
	
	public void emuUpdatePlayer();
	public int emuGetVolumeLevel();
	public int emuSetVolumeLevel(int level);
	

}
