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

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import nds.pstros.ConfigData;
import nds.pstros.MainApp;


public class Manager {
	public static final String TONE_DEVICE_LOCATOR = "device://tone";
	private static Vector players = new Vector(); 	
	
	public static synchronized Player createPlayer(InputStream stream, String type) 
		throws IOException, MediaException {
		/*	
		if (ConfigData.forceMute) {
			return new DummyPlayer();
		}			
		if (MainApp.verbose) {
			System.out.println("Manager.createPlayer(InputStream,..) type =" + type);
		}
		if (type.equals("audio/midi")) {
			Player p = new MidiPlayer(stream);
			players.add(p); 
			return p;
		} else 
		
		if (type.equals("audio/x-wav")) {
			Player p = new SampledPlayer(stream);
			players.add(p);
			return p;
		} else
		if (type.equals("audio/amr")) {
			if (ConfigData.storeImages) {
				SampledPlayer.saveAudio(stream, ConfigData.storeImagePath, ".amr");
			}
		}
		*/
		return new DummyPlayer();
	}
	public static synchronized  Player createPlayer(String locator) throws IOException, MediaException {
		if (MainApp.verbose) {
			System.out.println("Manager.createPlayer(locator) locator=" + locator);
		}
		
		
		//hack - switch mp3 names to wav
		//if (locator.startsWith("file:///") && locator.endsWith(".mp3")) {
		//	locator = locator.substring(0,locator.length()-4) + ".wav";
		//}
		/*
		InputStream is = Connector.openInputStream(locator);
		if (is == null) {
			return new DummyPlayer();
		} else {
			Player p = new SampledPlayer(is);
			players.add(p);
			return p;
		}
		*/
		return new DummyPlayer();
	} 
		
	public static synchronized  String[] getSupportedContentTypes(String protocol) {
		System.out.println("Manager.getSupportedContentTypes protocol=" + protocol);
		return new String[0];
	}	
	
	public static synchronized  String[] getSupportedProtocols(String content_type) {
		System.out.println("Manager.getSupportedPotocol content type=" + content_type);
		return new String[0];
	}
	
	public static synchronized  void playTone(int note, int duration, int volume) throws MediaException {
	}
	
	public static synchronized void emuRemovePlayer(Player p) {
		players.removeElement(p);
	}
	public static synchronized void emuUpdatePlayers() {
		int size = players.size();
		Player player;
		//System.out.println("Manager.emuUpdatePlayers... size=" + size);
		for  (int i = 0; i < size; i++) {
			player = (Player) players.elementAt(i);
			player.emuUpdatePlayer();
		}
	}	
	public static void emuStopPlayers() {
		int size = players.size();
		Player player;
		//System.out.println("Manager.emuUpdatePlayers... size=" + size);
		for  (int i = 0; i < size; i++) {
			player = (Player) players.elementAt(i);
			try {
				player.stop();
			} catch (Exception e) {};
		}
		
	}	
	
	

}
