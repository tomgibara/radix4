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
 * Allows the encoding and decoding of a {@link Radix4} to be configured.
 * 
 * Unless otherwise indicated, passing a null parameter to any method of this
 * class will raise an {@link IllegalArgumentException}.
 * 
 * @author tomgibara
 * 
 */

public final class Radix4Config implements Serializable {

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
	
	Radix4Config(boolean streaming) {
		bufferSize = DEFAULT_BUFFER_SIZE;
		lineLength = NO_LINE_BREAK;
		lineBreak = DEFAULT_LINE_BREAK;
		this.streaming = streaming;
		optimistic = true;
		terminated = false;
		terminator = DEFAULT_TERMINATOR;
	}
	
	Radix4Config(Radix4 radix4) {
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
	 * @return the modified configuration
	 */
	
	public Radix4Config setBufferSize(int bufferSize) {
		if (bufferSize < 1) bufferSize = DEFAULT_BUFFER_SIZE;
		this.bufferSize = bufferSize;
		return this;
	}
	
	/**
	 * Specifies whether Radix4 stream formatting should be used.
	 * 
	 * @param streaming
	 *            true if data is to be coded for streaming, false otherwise
	 * @return the modified configuration
	 */

	public Radix4Config setStreaming(boolean streaming) {
		this.streaming = streaming;
		return this;
	}
	
	/**
	 * Specifies whether Radix4 encoding should be optimistic about the absence
	 * of non-zero radices.
	 * 
	 * @param streaming
	 *            true iff data should initially be assumed to be radix-free
	 * @return the modified configuration
	 */

	public Radix4Config setOptimistic(boolean optimistic) {
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
	 * @return the modified configuration
	 */
	
	public Radix4Config setLineLength(int lineLength) {
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
	 * @return the modified configuration
	 * @throws IllegalArgumentException
	 *             if the supplied string is null or empty or contains
	 *             non-whitespace characters.
	 */
	
	public Radix4Config setLineBreak(String lineBreak) {
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
	 * @return the modified configuration
	 */

	public Radix4Config setTerminated(boolean terminated) {
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
	 * @return the modified configuration
	 * @throws IllegalArgumentException
	 *             if the termination character might appear in an encoding, or
	 *             as a whitespace
	 */
	
	public Radix4Config setTerminator(char terminator) {
		if (!Radix4.isTerminator(terminator)) throw new IllegalArgumentException("invalid terminator");
		this.terminator = terminator;
		return this;
	}
	
	/**
	 * Create a new Radix4 definition using the current configuration. After
	 * calling this method, the configuration may continue to be changed, and
	 * new definitions created.
	 * 
	 * @return a Radix4 coding definition using based on this configuration
	 */

	public Radix4 use() {
		return new Radix4(this);
	}
	
	/**
	 * Two configurations are equal if all parameters of the configuration are
	 * identical.
	 */
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Radix4Config)) return false;
		Radix4Config that = (Radix4Config) obj;
		if (this.lineLength != that.lineLength) return false;
		if (!this.lineBreak.equals(that.lineBreak)) return false;
		if (this.streaming != that.streaming) return false;
		if (this.terminated != that.terminated) return false;
		if (this.optimistic != that.optimistic) return false;
		if (this.terminator != that.terminator) return false;
		if (this.bufferSize != that.bufferSize) return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		hash += lineLength;
		hash *= 31;
		hash += bufferSize;
		hash *= 31;
		hash += lineBreak.hashCode();
		hash *= 31;
		hash += terminator;
		hash *= 31;
		if (streaming) hash += 1;
		hash *= 31;
		if (optimistic) hash += 1;
		hash *= 31;
		if (terminated) hash += 1;
		hash *= 31;
		return hash;
	}
	
}