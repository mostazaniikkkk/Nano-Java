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

import java.util.Vector;


public class RmsGroup {
	
	private String name;		//group name (Record Store name)
	private String app;		//application name (Midlet name)
	private Vector records;		//record list
	private boolean publicGroup;
	
	private int recordCounter;
	
	public RmsGroup(String appName, String name) {
		this.app = appName;
		this.name = name;
		records = new Vector();
		recordCounter = 1;
	}
	
	public String getName() {
		return name;
	} 
	public String getApplicationName() {
		return app;
	} 
	
	
	public int addRecord(byte[] data) {
		//System.out.println("EmsGroup: addRecord 1");
		RmsRecord record = new RmsRecord(recordCounter, data);
		recordCounter++; 
		records.addElement(record);
		return record.getId();
	}
	
	public int addRecord(byte[] data, int pos, int length) {
		//System.out.println("EmsGroup: addRecord 2 rc=" + recordCounter);
		RmsRecord record = new RmsRecord(recordCounter, data, pos, length);
		recordCounter++; 
		records.addElement(record);
		return record.getId();
	}
	
	public boolean removeRecord(int recordId) {
		RmsRecord record =  findRecord(recordId);
		if (record == null) {
			return false;
		}
		return records.removeElement(record);
	}
	
	public RmsRecord findRecord(int recordId) {
		int size = records.size();
		RmsRecord item ;
		for (int i = 0; i < size; i++) {
			item = (RmsRecord) records.elementAt(i);
			if (item.getId() == recordId) {
				return item;
			}
		}
		return null;
	}
	
	public int getNextRecordId() {
		return recordCounter;
	}
	public int getSize() {
		return records.size();
	}
	
	public byte[] getRecordData(int recordId) {
		RmsRecord result = findRecord(recordId);
		if (result == null) {
			return null;
		}
		return result.getData();
	}	
	public RmsRecord getRecord(int recordId) {
		RmsRecord result = findRecord(recordId);
		if (result == null) {
			return null;
		}
		return result;
	}	

	public int getDataSize() {
		int total = 0;
		int size = records.size();
		RmsRecord item ;
		for (int i = 0; i < size; i++) {
			item = (RmsRecord) records.elementAt(i);
			total += item.getData().length;
		}
		return total;		
	}
	public void setAuthMode(boolean publicMode) {
		publicGroup = publicMode;
	}
	public boolean isPublic() {
		return publicGroup;
	}
	public boolean isPrivate() {
		return !publicGroup;
	}
}
