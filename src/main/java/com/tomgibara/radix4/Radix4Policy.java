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
package com.tomgibara.radix4;

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

	private static final int    DEFAULT_BUFFER_SIZE =   64;
	private static final String DEFAULT_LINE_BREAK  = "\n";
	private static final char   DEFAULT_TERMINATOR  =  '.';
	
	static final int NO_LINE_BREAK = 0;
	
	int bufferSize;
	int lineLength;
	String lineBreak;
	boolean streaming;
	boolean terminated;
	boolean optimistic;
	char terminator;
	
	/**
	 * Creates a new mutable policy with default settings.
	 */
	
	Radix4Policy(boolean streaming) {
		bufferSize = DEFAULT_BUFFER_SIZE;
		lineLength = NO_LINE_BREAK;
		lineBreak = DEFAULT_LINE_BREAK;
		this.streaming = streaming;
		optimistic = true;
		terminated = false;
		terminator = DEFAULT_TERMINATOR;
	}
	
	Radix4Policy(Radix4 radix4) {
		bufferSize = radix4.bufferSize;
		lineLength = radix4.lineLength;
		lineBreak = radix4.lineBreak;
		streaming = radix4.streaming;
		optimistic = radix4.optimistic;
		terminated = radix4.terminated;
		terminator = radix4.terminator;
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
	
	public Radix4Policy setBufferSize(int bufferSize) {
		if (bufferSize < 1) bufferSize = DEFAULT_BUFFER_SIZE;
		this.bufferSize = bufferSize;
		return this;
	}
	
	/**
	 * The number of bytes used to buffer stream operations
	 * @return the size in bytes.
	 */

	public int getBufferSize() {
		return bufferSize;
	}
	
	public Radix4Policy setStreaming(boolean streaming) {
		this.streaming = streaming;
		return this;
	}
	
	public Radix4Policy setOptimistic(boolean optimistic) {
		this.optimistic = optimistic;
		return this;
	}
	
	/**
	 * The number of characters output before a line-break is inserted. A
	 * non-negative line length indicates that no line-breaks should be
	 * inserted. Note that any whitespace encountered during decoding will be
	 * skipped irrespective of whether line-breaks are inserted during encoding.
	 * 
	 * @param lineLength
	 *            the length of line into which encoded output should be split.
	 * @return 
	 */
	
	public Radix4Policy setLineLength(int lineLength) {
		this.lineLength = lineLength < 1 ? NO_LINE_BREAK : lineLength;
		return this;
	}

	/**
	 * The character sequence used to delimit lines in encoded output. The
	 * string must be non-empty and contain only whitespace characters. In this
	 * context whitespace characters are one of: '\r', '\n', '\t' and '&nbsp;'.
	 * 
	 * By default, the line breaks consist of a single character '\n'.
	 * 
	 * @param lineBreak
	 * @return 
	 * @throws IllegalArgumentException
	 *             if the supplied string is null or empty or contains
	 *             non-whitespace characters.
	 */
	
	public Radix4Policy setLineBreak(String lineBreak) {
		if (lineBreak == null) throw new IllegalArgumentException("null lineBreak");
		int length = lineBreak.length();
		if (length == 0) throw new IllegalArgumentException("empty lineBreak");
		// probably a short string - avoid intermediate object creation and iterate simply
		for (int i = 0; i < length; i++) {
			if ( !Radix4.isWhitespace(lineBreak.charAt(i)) ) throw new IllegalArgumentException("invalid lineBreak");
		}
		this.lineBreak = lineBreak;
		return this;
	}

	/**
	 * Whether encoded output should be terminated with some number of
	 * termination characters to unambiguously indicate the end of an encoded
	 * stream.
	 * 
	 * @param terminated
	 *            whether the output should be terminated
	 * @return 
	 */

	public Radix4Policy setTerminated(boolean terminated) {
		this.terminated = terminated;
		return this;
	}

	/**
	 * Specifies a character to be used for terminating encodings.
	 * 
	 * By default, the character '.' is used to terminate encodings.
	 * 
	 * @param terminator
	 *            a termination character
	 * @return 
	 * @throws IllegalArgumentException
	 *             if the termination character might appear in an encoding, or
	 *             as a whitespace
	 */
	
	public Radix4Policy setTerminator(char terminator) {
		if (!Radix4.isTerminator(terminator)) throw new IllegalArgumentException("invalid terminator");
		this.terminator = terminator;
		return this;
	}

	public Radix4 use() {
		return new Radix4(this);
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
	
}