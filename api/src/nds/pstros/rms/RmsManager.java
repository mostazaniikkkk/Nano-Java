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
package nds.pstros.rms;

//import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;

import nds.File;
import nds.pstros.ConfigData;
import nds.pstros.MainApp;


public class RmsManager {
	
	public static final String PUBLIC_STORAGE = "--==PUBLIC_STORAGE==--";
	
	private static RmsManager instance;
	private Vector groups; 
	
	public static RmsManager getInstance() {
		if (instance == null) {
			instance = new RmsManager();
		}
		return instance;
	}
	
	
	private RmsManager() {
		groups  = new Vector();
	}
	
	public RmsGroup getRmsGroup(String appName, String groupName, boolean authPublic, boolean create) {
		RmsGroup group = findGroup(appName, groupName);
		if (group == null && create) {
			group = new RmsGroup(appName, groupName);
			group.setAuthMode(authPublic);
			groups.addElement(group);
		} else 
		//we have found a private group but public was requested
		if ( group != null && group.isPrivate() && authPublic ) {
			group = null;
		}
		//else: we have found a public group - it doesn't mater what auth mode was requested
		//or: we have found a private group and private access was requested - its ok, return the group
		
		return group;
	}
	
	public boolean removeGroup(String appName, String groupName) {
		RmsGroup group = findGroup(appName, groupName);
		if (group != null) {
			return groups.removeElement(group);
		}
		return false; 
	}
	
	public String[] getGroupNames(String appName) {
		int size = groups.size();
		int count = 0;
		RmsGroup group;
		for (int i = 0; i < size; i++) {
			group = (RmsGroup) groups.elementAt(i);
			if (group.getApplicationName().equals(appName)) {
					count ++;		
			} 
		}
		if (count == 0) {
			return null;
		}
		String[] result = new String[count];
		count = 0;
		for (int i = 0; i < size; i++) {
			group = (RmsGroup) groups.elementAt(i);
			if (group.getApplicationName().equals(appName)) {
					result[count] = group.getName();
					count ++;		
			} 
		}
		return result;
	}
	
	private RmsGroup findGroup(String appName, String groupName) {
		int size = groups.size();
		RmsGroup group;
		for (int i = 0; i < size; i++) {
			group = (RmsGroup) groups.elementAt(i);
			if (	group.getApplicationName().equals(appName) &&
				group.getName().equals(groupName) 
			) {
				return group;		
			} 
		}
		return null;
	}

	public void saveData() {
		if (ConfigData.readOnly) {
			return;
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(os);
		
		int size = groups.size();
		RmsGroup group;
		byte[] data;
		//System.out.println("rms size=" + size);
		//there should be only one group - the current application that is running right now
		try {
			//store number of records
			dos.writeShort(size);
			for (int i = 0; i < size; i++) {
				group = (RmsGroup) groups.elementAt(i);
				dos.writeUTF( group.getName());
				dos.writeByte(group.isPublic() ? 1 : 0);
				//write the data arrays
				int numRecords = group.getSize();
				dos.writeShort(numRecords);
				for (int j = 0; j < numRecords; j++) {
					data = group.getRecordData(j+1);
					if (data != null && data.length > 0) {
						dos.writeInt(data.length);
						dos.write(data);
					} else {
						dos.writeInt(0);
					}
				}
			}
			dos.flush();
			data = os.toByteArray();
			int result = File.save(getDataFilename(), data);
			dos.close();
			if (result < 0) {
				System.out.println("rms write failed");
			}
		} catch (Exception e) {
			System.out.println("Error saving the rms!");
		}
	}
	
	public void readData() {
		if (ConfigData.readOnly) {
			return;
		}
		String filename = getDataFilename();
		if (!File.exists(filename)) {
			if (MainApp.verbose) {
				System.out.println("rms file not found");
			}
			return;
		}
		int size = File.size(filename);
		byte[] data = new byte[size];
		int result = File.load(filename, data, data.length);
		if (result < 4) {
			if (MainApp.verbose) {
				System.out.println("rms file invalid");
			}
			return;
		}
		DataInputStream din = new DataInputStream(new ByteArrayInputStream(data));
		try {
			int groupSize = din.readShort();
			for (int i = 0; i < groupSize; i++) {
				//read group name
				String name = din.readUTF();
				boolean publicAuth = din.readByte() != 0;
				int numRecords = din.readShort();
				RmsGroup rmsGroup = getRmsGroup(MainApp.getApplicationName(), name, publicAuth, true);
				
				for (int j = 0; j < numRecords; j++) {
					int recordSize = din.readInt();
					byte[] recordData = new byte[recordSize];
					if (recordSize > 0) {
						din.readFully(recordData);
						if (rmsGroup != null) {
							rmsGroup.addRecord(recordData);		
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("rms read failed:" + e);
		}
	}
	
	private String toHex(byte[] data) {
		int size = data.length;
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < size; i++) {
			result.append(Integer.toString(data[i])).append(' ');
		}
		return result.toString();
	}
	
	public String getDataFilename() {
		String name = System.getProperty("MIDlet-Jar-URL");
		if (name == null) {
			name = MainApp.getApplicationName();
		} else {
			name = name.trim();
			int index = name.lastIndexOf('.');
			if (index > 1) {
				name = name.substring(0, index);
			}
		}
		return "./" + name + ".sav";
	}
}
