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


public class RmsRecord {
	
	private byte[] data;
	private int id;
	
	
	public RmsRecord(int id, byte[] data) {
		this.data = data;
		this.id = id;
	}
	

	public RmsRecord(int id, byte[] src, int start, int size ) {
		this.id = id;
		data = new byte[size];
		System.arraycopy(src, start, data, 0, size);
	}

	public int getId() {
		return id;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] src, int pos, int length) {
		data = new byte[length];
		if (length > 0) {
			System.arraycopy(src, pos, data, 0, length);	
		}
	}

}
