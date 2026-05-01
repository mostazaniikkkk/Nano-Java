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
package javax.microedition.rms;

import java.util.Vector;

import nds.pstros.MainApp;
import nds.pstros.rms.RmsEnumeration;
import nds.pstros.rms.RmsGroup;
import nds.pstros.rms.RmsManager;
import nds.pstros.rms.RmsRecord;


public class RecordStore {
	private static final int EVENT_ADD = 0;
	private static final int EVENT_CHANGE = 1;
	private static final int EVENT_DELETE = 2;
	
	
	
	
	public static final int AUTHMODE_PRIVATE = 0;
	public static final int AUTHMODE_ANY = 1;
	
	private static RmsManager emuRmsManager = RmsManager.getInstance();

	private RmsGroup emuRmsGroup;
	private int version; 
	private Vector listeners;
	private boolean closed;
	
	
	private RecordStore(RmsGroup group) {
		emuRmsGroup = group;
		version = 1;
	} 
	
	public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
	   throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
		if (MainApp.verbose) {
			System.out.println("openRecordStore, midp1");
		}
	   	return openRecordStore(recordStoreName, MainApp.getApplicationName(), false, createIfNecessary);
	   }
	   
	public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary, int authmode, boolean writable)
	   throws RecordStoreException, RecordStoreFullException,  RecordStoreNotFoundException {
		if (MainApp.verbose) {
			System.out.println("openRecordStore, midp2");
		}
		boolean authMode = false;
		if (authmode == AUTHMODE_ANY) {
			authMode = true;
		}
		return openRecordStore(recordStoreName, MainApp.getApplicationName(), authMode, createIfNecessary);
	}	   
	   
	public static RecordStore openRecordStore(String recordStoreName, String vendorName, String suiteName) 
	   throws RecordStoreException, RecordStoreNotFoundException {
		boolean authMode = false;
		if (MainApp.verbose) {
			System.out.println("openRecordStore, midp2, vendor=" + vendorName);
		}
		if (suiteName == null || !suiteName.equals(MainApp.getApplicationName())) {
			authMode = true; //open public record store
		}			   	
		return openRecordStore(recordStoreName, suiteName, authMode, false);
	}
	   	
	private static RecordStore openRecordStore(String recordStoreName,String appName, boolean publicAuth, boolean createIfNecessary)
	   throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
		if (MainApp.verbose) {
			System.out.println("  -> openRecordStore: name=" + recordStoreName + " create=" + createIfNecessary + " application=" + MainApp.getApplicationName() + " public=" + publicAuth);
		}
	   	
		if (recordStoreName == null || recordStoreName.length() > 32) {
			throw new IllegalArgumentException("Invalid store name:" + recordStoreName);
		}
	   	
		RmsGroup group = emuRmsManager.getRmsGroup(appName, recordStoreName, publicAuth,  createIfNecessary);
 
		if (group == null) {
			if (MainApp.verbose) {
				System.out.println("openRecordStore: record store not found! name=" + recordStoreName );
			}
			throw new RecordStoreNotFoundException("Record store not found! name=" + recordStoreName);
		}
		return new RecordStore(group);		
	}
	
	public void setMode(int authmode, boolean writable) throws RecordStoreException {
		if (emuRmsGroup == null) {
			throw new RecordStoreException("record store is corrupted.");
		}
		if (!MainApp.getApplicationName().equals(emuRmsGroup.getApplicationName())) {
			throw new SecurityException("change is not allowed");
		} 
		if (authmode != AUTHMODE_ANY && authmode != AUTHMODE_PRIVATE) {
			throw new IllegalArgumentException("unsupported auth mode=" + authmode);
		}
		emuRmsGroup.setAuthMode(authmode == AUTHMODE_ANY);
	}
	
	public static void deleteRecordStore(String recordStoreName)
		throws RecordStoreException, RecordStoreNotFoundException {
			if (MainApp.verbose) {
				System.out.println("deleteRecordStore: name=" + recordStoreName + " application=" + MainApp.getApplicationName());
			}
			boolean result = emuRmsManager.removeGroup(MainApp.getApplicationName(), recordStoreName);
			if (result == false) {
				throw new RecordStoreNotFoundException("recordStoreName=" + recordStoreName);
			}
	}
	public static String[] listRecordStores() {
		if (MainApp.verbose) {
			System.out.println("Record store: listRecordStores()");
		}
		return emuRmsManager.getGroupNames(MainApp.getApplicationName());
	}

	
	public int addRecord(byte[] data, int offset, int numBytes)
		throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException {
			if (MainApp.verbose) {
				System.out.println("RecordStore: addRecord dataSize=" + data.length + " offset=" + offset + " numBytes=" + numBytes);
			}
			if (closed) {
				throw new RecordStoreNotOpenException();
			}
			version++;
			int result  = emuRmsGroup.addRecord(data, offset, numBytes);
			setEvent(EVENT_ADD, result);
			return result;
	}
	
	public void closeRecordStore() throws RecordStoreNotOpenException, RecordStoreException {
		if (MainApp.verbose) {
			System.out.println("RecordStore.closeRecordStore()");
		}
		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		closed = true;
		if (listeners != null) {
			listeners.removeAllElements();
		}
	}
	
	public void deleteRecord(int recordId)
		throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
			if (MainApp.verbose) {
				System.out.println("RecordStore.deleteRecord(id) " + recordId);
			}
			if (closed) {
				throw new RecordStoreNotOpenException();
			}
			boolean result = emuRmsGroup.removeRecord(recordId);
			if (result == false) {
				throw new InvalidRecordIDException("id=" + recordId);
			}
			version++;
			setEvent(EVENT_DELETE, recordId);
	}
	public String getName() throws RecordStoreNotOpenException {
		if (MainApp.verbose) {
			System.out.println("RecordStore.getName()");
		}
		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		return emuRmsGroup.getName();
	}
	
	public int getNextRecordID() throws RecordStoreNotOpenException, RecordStoreException {
		if (MainApp.verbose) {
			System.out.println("RecordStore.getNextRecordId()");
		}
		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		return emuRmsGroup.getNextRecordId();
	}
	
	public int getNumRecords() throws RecordStoreNotOpenException {
		if (MainApp.verbose) {
			System.out.println("RecordStore.getNumRecords()");
		}
		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		if (MainApp.verbose) {
			System.out.println("  -->returned " + emuRmsGroup.getSize());
		}
		return emuRmsGroup.getSize();
	}
	public byte[] getRecord(int recordId) 
		throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
		if (MainApp.verbose) {
			System.out.println("RecordStore.getRecord(id) groupName=" + emuRmsGroup.getName() + " groupSize=" + emuRmsGroup.getSize());
		}
		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		byte[] src = emuRmsGroup.getRecordData(recordId);
		if (src == null) {
			throw new InvalidRecordIDException("recordId=" + recordId);
		}
		byte[] dst = new byte[src.length];
		if (src.length > 0) {
			System.arraycopy(src, 0, dst, 0, src.length);
		}
		if (MainApp.verbose) {
			System.out.println("  --> returned size " + dst.length);
		}
		return dst;
	}
	
	public int getRecord(int recordId, byte[] buffer, int offset)
		throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
		if (MainApp.verbose) {
			System.out.println("RecordStore.getRecord(id, buffer, offset) groupName=" + emuRmsGroup.getName() + " groupSize=" + emuRmsGroup.getSize() + " id=" + recordId);
		}

		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		byte[] src = emuRmsGroup.getRecordData(recordId);
		if (src == null) {
			throw new InvalidRecordIDException("recordId=" + recordId);
		}
		
		if (buffer.length - offset < src.length) {
			throw new ArrayIndexOutOfBoundsException("recordId=" + recordId);
		}
		if (src.length > 0) {
			System.arraycopy(src,0, buffer, offset, src.length);
		}
		return src.length;
	}
	
	public int getRecordSize(int recordId) 
		throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
			if (MainApp.verbose) {
				System.out.println("RecordStore.getRecordSize()");
			}
		if (closed) {
			throw new RecordStoreNotOpenException();
		}

		byte[] src = emuRmsGroup.getRecordData(recordId);
		if (src == null) {
			throw new InvalidRecordIDException("recordId=" + recordId);
		}
		return src.length;
			
	}
	
	public int getSize() throws RecordStoreNotOpenException {
		if (MainApp.verbose) {
			System.out.println("RecordStore.getSize()");
		}
		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		return emuRmsGroup.getDataSize();
	}
	
	public int getSizeAvailable() throws RecordStoreNotOpenException {
		if (MainApp.verbose) {
			System.out.println("RecordStore.getSizeAvailable()");
		}
		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		return 1024 * 1024;
	}
	
	public int getVersion() throws RecordStoreNotOpenException {
		if (MainApp.verbose) {
			System.out.println("RecordStore.getVersion()");
		}
		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		return version;
	}
	
	public long getLastModified() {
		if (MainApp.verbose) {
			System.out.println("RecordStore.getLastModified()");
		}
		return 0L;
	}	

	public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
		throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException, RecordStoreFullException {
			if (MainApp.verbose) {
				System.out.println("RecordStore.setRecord() recordId=" + recordId + " dataSize=" + newData.length + " offset=" + offset + " numBytes=" + numBytes);
			}

		if (closed) {
			throw new RecordStoreNotOpenException();
		}
		RmsRecord record = emuRmsGroup.getRecord(recordId);
		if (record  == null) {
			throw new InvalidRecordIDException("recordId=" + recordId);
		}
		record.setData(newData, offset, numBytes);
		version++;
		setEvent(EVENT_CHANGE, recordId);
	}	
	
	public void addRecordListener(RecordListener listener) {
		if (MainApp.verbose) {
			System.out.println("RecordStore.addListener()");
		}
		if (listener == null) {
			return;
		}
		if (listeners == null) {
			listeners = new Vector();
		}
		if (!listeners.contains(listener)) {
			listeners.addElement(listener);
		}
	}
	
	public void removeRecordListener(RecordListener listener) {
		if (MainApp.verbose) {
			System.out.println("RecordStore.removeListener()");
		}
		if (listener == null || listeners == null) {
			return;
		}
		listeners.removeElement(listener);		
	}
	
	private void setEvent(int type, int recordId) {
		if (listeners == null) {
			return;
		}		
		int size  = listeners.size();
		RecordListener listener;
		for (int i = 0; i < size; i++) {
			listener = (RecordListener) listeners.elementAt(i);
			switch (type) {
				case EVENT_ADD: listener.recordAdded(this, recordId); break;
				case EVENT_CHANGE: listener.recordChanged(this, recordId); break;
				case EVENT_DELETE: listener.recordDeleted(this, recordId); break;
			}
		}
	}
	
	public RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator, boolean keepUpdated) 
		throws RecordStoreNotOpenException {
			
			if (MainApp.verbose) {
				System.out.println("RecordStore.enumerateRecord()");
			}
			if (closed) {
				throw new RecordStoreNotOpenException();
			}
			//System.out.println("Pstros: RecordStore.enumerateRecords(...) - function not implemented");
			return new RmsEnumeration(filter, comparator, emuRmsGroup);
	}

}
