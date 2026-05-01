package java.util;


public class StringTokenizer {
	
	private String data;
	private char[] delimiters;
	private int dataSize;
	private int position;
	
	public StringTokenizer(String data, String delims) {
		this(data, delims.toCharArray());
	}
	public StringTokenizer(String data, char[] delimiters) {
		this.data = data;
		this.delimiters = delimiters;
		position = 0;
		dataSize = data.length();
	}
	
	
	public boolean hasMoreTokens() {
		skipDelimiters();
		return (position < dataSize);
	}
	
	public String nextToken() {
		int start;
		int end;
		
		skipDelimiters();
		start = position;
		skipData();
		end = position;
		if (start == end) return null;
		return data.substring(start, end);
	}
	public int countTokens() {
		return getSize();
	}
	public int getSize() {
		int start;
		int end;
		int size = 0;
		position = 0;
		
		while (position < dataSize) {
			skipDelimiters();
			start = position;
			skipData(); 
			end = position;
			if (start != end) {
				size++;
			}
		}
		position = 0;
		return size;
	}

	private void skipDelimiters() {
		while (position < dataSize) {
			if (!isDelimiter(data.charAt(position)) ) {
				return;
			}
			position++;
		}
	}
	
	private void skipData() {
		while (position < dataSize) {
			if (isDelimiter(data.charAt(position)) ) {
				return;
			}
			position++;
		}
	}

	private boolean isDelimiter(char ch) {
		for (int i = 0; i < delimiters.length; i++) {
			if (ch == delimiters[i]) {
				return  true;
			}
		}
		return false;
	}
}
