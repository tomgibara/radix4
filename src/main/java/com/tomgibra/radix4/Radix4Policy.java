package com.tomgibra.radix4;

import java.io.Serializable;

public final class Radix4Policy implements Serializable {

	private static final long serialVersionUID = 3743746958617647872L;

	public static final Radix4Policy DEFAULT = new Radix4Policy().immutableCopy();
	
	static final int NO_LINE_BREAK = 0;
	private static final int DEFAULT_BUFFER_SIZE = 64;
	private static final byte[] LF_BYTES = { '\n' };
	
	private final boolean mutable;
	int bufferSize;
	int lineLength;
	String lineBreak;
	byte[] lineBreakBytes;
	boolean terminated;
	char terminator;
	byte terminatorByte;
	
	public Radix4Policy() {
		mutable = true;
		bufferSize = DEFAULT_BUFFER_SIZE;
		lineLength = NO_LINE_BREAK;
		lineBreak = "\n";
		lineBreakBytes = LF_BYTES;
		terminated = false;
		terminator = '.';
		terminatorByte = '.';
	}
	
	private Radix4Policy(Radix4Policy that, boolean mutable) {
		this.mutable = mutable;
		this.bufferSize = that.bufferSize;
		this.lineLength = that.lineLength;
		this.lineBreak = that.lineBreak;
		this.lineBreakBytes = that.lineBreakBytes;
		this.terminated = that.terminated;
		this.terminator = that.terminator;
		this.terminatorByte = that.terminatorByte;
	}
	
	public void setBufferSize(int bufferSize) {
		if (bufferSize < 1) bufferSize = DEFAULT_BUFFER_SIZE;
		this.bufferSize = bufferSize;
	}

	public int getBufferSize() {
		return bufferSize;
	}
	
	public void setLineLength(int lineLength) {
		checkMutable();
		this.lineLength = lineLength < 1 ? NO_LINE_BREAK : lineLength;
	}
	
	public void setLineBreak(String lineBreak) {
		checkMutable();
		if (lineBreak == null) throw new IllegalArgumentException("null lineBreak");
		int length = lineBreak.length();
		if (length == 0) throw new IllegalArgumentException("empty lineBreak");
		// probably a short string - avoid intermediate object creation and iterate simply
		for (int i = 0; i < length; i++) {
			if ( !Radix4.isWhitespace(lineBreak.charAt(i)) ) throw new IllegalArgumentException("invalid lineBreak");
		}
		this.lineBreak = lineBreak;
		lineBreakBytes = lineBreak.getBytes(Radix4.ASCII);
	}

	public void setTerminated(boolean terminated) {
		checkMutable();
		this.terminated = terminated;
	}
	
	public void setTerminator(char terminator) {
		if (!Radix4.isTerminator(terminator)) throw new IllegalArgumentException("invalid terminator");
		this.terminator = terminator;
		this.terminatorByte = (byte) terminator;
	}
	
	public int getLineLength() {
		return lineLength;
	}
	
	public String getLineBreak() {
		return lineBreak;
	}
	
	public boolean isTerminated() {
		return terminated;
	}
	
	public char getTerminator() {
		return terminator;
	}
	
	public Radix4Policy mutableCopy() {
		return new Radix4Policy(this, true);
	}
	
	public Radix4Policy immutableCopy() {
		return mutable ? new Radix4Policy(this, false) : this;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Radix4Policy)) return false;
		Radix4Policy that = (Radix4Policy) obj;
		if (this.lineLength != that.lineLength) return false;
		if (!this.lineBreak.equals(that.lineBreak)) return false;
		if (this.terminated != that.terminated) return false;
		if (this.terminator != that.terminator) return false;
		if (this.bufferSize != that.bufferSize) return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return lineLength ^ bufferSize * 31 ^ lineBreak.hashCode() ^ terminator;
	}
	
	private void checkMutable() {
		if (!mutable) throw new IllegalStateException("immutable policy");
	}
}