/*
 *   Copyright 2014 Tom Gibara
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package com.tomgibra.radix4;

import java.io.Serializable;

/**
 * Provides tailored encoding and decoding by a {@link Radix4}. Attempting to
 * modify the settings of an immutable policy will raise an
 * {@link IllegalStateException}.
 * 
 * Immutable instances of this class are safe for concurrent access from
 * multiple threads.
 * 
 * @author tomgibara
 * 
 */

public final class Radix4Policy implements Serializable {

	private static final long serialVersionUID = 3743746958617647872L;

	/**
	 * A default policy instance. The default policy declares that no
	 * line-breaks and no terminating characters will be written (though they
	 * may still be read).
	 */
	
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
	boolean optimistic;
	char terminator;
	byte terminatorByte;
	
	/**
	 * Creates a new mutable policy with default settings.
	 */
	
	public Radix4Policy() {
		mutable = true;
		bufferSize = DEFAULT_BUFFER_SIZE;
		lineLength = NO_LINE_BREAK;
		lineBreak = "\n";
		lineBreakBytes = LF_BYTES;
		optimistic = true;
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
		this.optimistic = that.optimistic;
		this.terminator = that.terminator;
		this.terminatorByte = that.terminatorByte;
	}
	
	/**
	 * Specifies the number of bytes used to buffer stream operations. Note that
	 * currently buffering is only applied to writing and not reading.
	 * 
	 * It's possible to restore the default buffer size by supplying a
	 * non-positive size.
	 * 
	 * @param bufferSize
	 *            the buffer size to use in bytes or a non-positive number to
	 *            indicate the default size.
	 */
	
	public void setBufferSize(int bufferSize) {
		if (bufferSize < 1) bufferSize = DEFAULT_BUFFER_SIZE;
		this.bufferSize = bufferSize;
	}
	
	/**
	 * The number of bytes used to buffer stream operations
	 * @return the size in bytes.
	 */

	public int getBufferSize() {
		return bufferSize;
	}
	
	public void setOptimistic(boolean optimistic) {
		checkMutable();
		this.optimistic = optimistic;
	}
	
	public boolean isOptimistic() {
		return optimistic;
	}
	
	/**
	 * The number of characters output before a line-break is inserted. A
	 * non-negative line length indicates that no line-breaks should be
	 * inserted. Note that any whitespace encountered during decoding will be
	 * skipped irrespective of whether line-breaks are inserted during encoding.
	 * 
	 * @param lineLength
	 *            the length of line into which encoded output should be split.
	 */
	
	public void setLineLength(int lineLength) {
		checkMutable();
		this.lineLength = lineLength < 1 ? NO_LINE_BREAK : lineLength;
	}

	/**
	 * The character sequence used to delimit lines in encoded output. The
	 * string must be non-empty and contain only whitespace characters. In this
	 * context whitespace characters are one of: '\r', '\n', '\t' and '&nbsp;'.
	 * 
	 * By default, the line breaks consist of a single character '\n'.
	 * 
	 * @param lineBreak
	 * @throws IllegalArgumentException
	 *             if the supplied string is null or empty or contains
	 *             non-whitespace characters.
	 */
	
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

	/**
	 * Whether encoded output should be terminated with some number of
	 * termination characters to unambiguously indicate the end of an encoded
	 * stream.
	 * 
	 * @param terminated
	 *            whether the output should be terminated
	 */

	public void setTerminated(boolean terminated) {
		checkMutable();
		this.terminated = terminated;
	}

	/**
	 * Specifies a character to be used for terminating encodings.
	 * 
	 * By default, the character '.' is used to terminate encodings.
	 * 
	 * @param terminator
	 *            a termination character
	 * @throws IllegalArgumentException
	 *             if the termination character might appear in an encoding, or
	 *             as a whitespace
	 */
	
	public void setTerminator(char terminator) {
		if (!Radix4.isTerminator(terminator)) throw new IllegalArgumentException("invalid terminator");
		this.terminator = terminator;
		this.terminatorByte = (byte) terminator;
	}

	/**
	 * The number of characters between line breaks in encoded output or zero to
	 * indicate that line-breaks should not be output.
	 * 
	 * @return the line length in ASCII characters, or zero
	 */

	public int getLineLength() {
		return lineLength;
	}
	
	/**
	 * The characters inserted to form a line-break.
	 * 
	 * @return a string of one or more whitespace characters
	 */

	public String getLineBreak() {
		return lineBreak;
	}

	/**
	 * Whether encoded output should be terminated
	 * 
	 * @return true iff termination characters should be output
	 */

	public boolean isTerminated() {
		return terminated;
	}

	/**
	 * The character used to indicate termination
	 * 
	 * @return the termination character
	 */

	public char getTerminator() {
		return terminator;
	}

	/**
	 * Creates a new policy which is identical to this policy and mutable.
	 * 
	 * @return a mutable copy of this policy
	 */

	public Radix4Policy mutableCopy() {
		return new Radix4Policy(this, true);
	}
	
	/**
	 * Returns a policy which is identical to this policy and immutable. This
	 * may or may not result in a new policy object being created.
	 * 
	 * @return an immutable copy of this policy
	 */

	public Radix4Policy immutableCopy() {
		return mutable ? new Radix4Policy(this, false) : this;
	}

	/**
	 * Whether this policy is mutable.
	 * 
	 * @return true iff this policy is mutable
	 */

	public boolean isMutable() {
		return mutable;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Radix4Policy)) return false;
		Radix4Policy that = (Radix4Policy) obj;
		if (this.lineLength != that.lineLength) return false;
		if (!this.lineBreak.equals(that.lineBreak)) return false;
		if (this.terminated != that.terminated) return false;
		if (this.optimistic != that.optimistic) return false;
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