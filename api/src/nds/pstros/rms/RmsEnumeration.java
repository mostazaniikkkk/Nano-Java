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

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordComparator;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordFilter;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;

import nds.pstros.MainApp;


public class RmsEnumeration implements RecordEnumeration {
	RmsGroup group;
	private int groupSize;
	private int currentIndex;
	private boolean keepUpdated;
	
	private RecordFilter filter;
	private RecordComparator comparator;

	public RmsEnumeration(RecordFilter filter, RecordComparator comparator, RmsGroup group) {
		this.group = group;
		this.filter = filter;
		this.comparator = comparator;
		groupSize = group.getSize();
		currentIndex = 0;
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration created groupSize=" + groupSize + " comparator=" + comparator);
		}
	}

	public void destroy() {
		group = null;
		filter = null;
		comparator = null;
	}

	public boolean hasNextElement() {
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration: hasNextElement=" + (currentIndex < groupSize) + " groupSize=" + groupSize + " currentIndex=" + currentIndex);
		}
		return currentIndex < groupSize;
	}

	public boolean hasPreviousElement() {
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration: hasPreviousElement");
		}
		return currentIndex > 0;
	}

	public boolean isKeptUpdated() {
		return keepUpdated;
	}

	public void keepUpdated(boolean keepUpdated) {
		this.keepUpdated = keepUpdated;
		rebuild();
	}

	public byte[] nextRecord()
		throws
			InvalidRecordIDException,
			RecordStoreNotOpenException,
			RecordStoreException {
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration: nextRecord");
		}
		if (!hasNextElement()) {
			throw new InvalidRecordIDException();
		}
		byte[] result = group.getRecordData(currentIndex); 
		currentIndex++;
		return result;
	}

	public int nextRecordId() throws InvalidRecordIDException {
		if (!hasNextElement()) {
			throw new InvalidRecordIDException();
		}
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration: nextRecordId=" + (currentIndex+1));
		}
		currentIndex++;
		return currentIndex;
	}

	public int numRecords() {
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration: numRecord=" + groupSize);
		}
		return groupSize;
	}

	public byte[] previousRecord()
		throws
			InvalidRecordIDException,
			RecordStoreNotOpenException,
			RecordStoreException {
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration: previousRecord");
		}
		if (!hasPreviousElement()) {
			throw new InvalidRecordIDException();
		}
		currentIndex--;
		return group.getRecordData(currentIndex);
	}

	public int previousRecordId() throws InvalidRecordIDException {
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration: previousRecordId");
		}
		if (!hasPreviousElement()) {
			throw new InvalidRecordIDException();
		}
		return currentIndex-1;
	}

	public void rebuild() {
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration: rebuild");
		}
		groupSize = group.getSize();
	}

	public void reset() {
		if (MainApp.verbose) {
			System.out.println("RmsEnumeration: reset");
		}
		currentIndex = -1;
	}

}
