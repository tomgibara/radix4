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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Defines a Radix4 encoding; various encodings are possible. The definitions
 * provided by this class are immutable and safe for concurrent use by multiple
 * threads.
 * 
 * This class is the entry-point for accessing the functions provided by this
 * package: the static methods {@link #stream()} and {@link #block()} can be
 * called to obtain standard Radix4 codings in streaming and block formats
 * respectively.
 * 
 * Alternative codings can be obtained by calling {@link #configure()} on a
 * standard instance.
 * 
 * @author tomgibara
 * 
 */

public final class Radix4 implements Serializable {

	// static fields
	
	private static final long serialVersionUID = -3598867342181148501L;

	// note order dependent - must be assigned before BLOCK & STREAM construction
	static final Charset ASCII = Charset.forName("ASCII");
	private static final byte[] DEFAULT_LINE_BREAK_BYTES = { '\n' };
	
	private static final Radix4 STREAM = new Radix4(new Radix4Config(true));
	private static final Radix4 BLOCK = new Radix4(new Radix4Config(false));

	// static methods
	
	//TODO move onto Util class?
	static String charStr(char c) {
		return String.format("%#02x", (int) c);
	}
	
	/**
	 * The standard Radix4 coding definition for streaming data.
	 * 
	 * @return the standard stream definition
	 */
	
	public static Radix4 stream() {
		return STREAM;
	}
	
	/**
	 * The standard Radix4 coding definition for block-encoded data.
	 * 
	 * @return the standard block definition
	 */
	
	public static Radix4 block() {
		return BLOCK;
	}
	
	// fields
	
	final Radix4Mapping mapping;
	final int bufferSize;
	final int lineLength;
	final char[] whitespace;
	final String lineBreak;
	final boolean streaming;
	final boolean terminated;
	final boolean optimistic;
	final char terminator;
	
	final byte[] bytes = new byte[256];
	final byte[] lineBreakBytes;
	final byte terminatorByte;

	private Radix4Coding coding = null;
	
	// constructors
	
	Radix4(Radix4Config config) {
		mapping = config.mapping;
		bufferSize = config.bufferSize;
		lineLength = config.lineLength;
		whitespace = config.whitespace;
		lineBreak  = config.lineBreak;
		streaming  = config.streaming;
		optimistic = config.optimistic;
		terminated = config.terminated;
		terminator = config.terminator;
		
		// optimization - line break commonly left untouched - avoid creating many small byte arrays
		lineBreakBytes = lineBreak.equals("\n") ? DEFAULT_LINE_BREAK_BYTES : lineBreak.getBytes(Radix4.ASCII);
		terminatorByte = (byte) terminator;
		
		// populate bytes
		Arrays.fill(bytes, (byte) -1);
		for (byte i = 0; i < 64; i++) {
			bytes[mapping.chars[i]] = i;
		}
		for (char c : whitespace) {
			// also checks for whitespace collision
			// bit untidy doing this outside the constructor, but it's more efficient to do it here
			if (bytes[c] != -1) throw new IllegalArgumentException("Encoding characters contain whitespace: " + charStr(c));
			bytes[c] = -2;
		}
	}
	
	// public accessors

	/**
	 * The mapping which will be used to generate the Radix4 coding. The mapping
	 * determines which characters will be preserved by the encoding.
	 * 
	 * @return the mapping used by the coding
	 */
	
	public Radix4Mapping getMapping() {
		return mapping;
	}
	
	/**
	 * The character that will be treated as whitespace by this mapping.
	 * 
	 * @return an array characters treated as whitespace, possibly empty but
	 *         never null
	 */

	public char[] getWhitespace() {
		return whitespace.clone();
	}
	
	/**
	 * Whether this Radix4 coding will organize coded data so that it can be
	 * streamed (true). Or whether coded data is structured may be structured as
	 * an unstreamable block (false).
	 * 
	 * @return true iff stream-formatting is used when coding data
	 */

	public boolean isStreaming() {
		return streaming;
	}

	/**
	 * Whether this Radix4 encoding/decoding will defer writing/reading radices
	 * until a non-zero radix is encountered.
	 * 
	 * @return whether the coding optimistically defers recording radices
	 */

	public boolean isOptimistic() {
		return optimistic;
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
	 * Whether encoded output should be explicitly terminated.
	 * 
	 * @return true iff termination characters should be output
	 */

	public boolean isTerminated() {
		return terminated;
	}

	/**
	 * The character used to indicate termination.
	 * 
	 * @return the termination character
	 */

	public char getTerminator() {
		return terminator;
	}

	/**
	 * The number of bytes used to buffer stream operations.
	 * 
	 * @return the buffer size in bytes, always positive
	 */
	
	public int getBufferSize() {
		return bufferSize;
	}
	
	// public methods

	/**
	 * Obtain an object that can process Radix4 encoded data according to this
	 * Radix4 configuration which controls the operating parameters of the
	 * encoding/decoding.
	 * 
	 * @return a {@link Radix4Coding} instance
	 * @see Radix4Config#DEFAULT
	 */
	
	public Radix4Coding coding() {
		if (coding != null) return coding;
		return coding = streaming ? new Radix4Streams(this) : new Radix4Blocks(this);
	}
	
	/**
	 * Creates a new mutable Radix4 configuration that can be used to create a
	 * modified definition. The configuration returned matches will be
	 * initialized to match this definition.
	 * 
	 * @return an object for configuring a new Radix4 coding.
	 */

	public Radix4Config configure() {
		return new Radix4Config(this);
	}
	
	/**
	 * Computes the number of ASCII characters required to encode the supplied
	 * bytes.
	 * 
	 * @param bytes
	 *            the bytes to be encoded.
	 * @return the length of this Radix4 encoding in bytes
	 */

	public long computeEncodedLength(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		long radixFreeLength = optimistic ? mapping.computeRadixFreeLength(bytes) : 0L;
		return computeEncodedLength(bytes.length, radixFreeLength);
	}

	/**
	 * Computes the number of ASCII characters required to encode a specified
	 * number of bytes. The character count includes the terminating sequence if
	 * one is specified.
	 * 
	 * The number of radix-free bytes present at the start of the byte sequence
	 * need only be supplied in the case where optimistic coding is used, in all
	 * other cases zero may be supplied for this parameter.
	 * 
	 * @param byteLength
	 *            the number of bytes to be encoded
	 * @param radixFreeLength
	 *            the number of leading radix-free bytes
	 * @return the number characters required to Radix4 encode the specified
	 *         number of bytes
	 */

	public long computeEncodedLength(long byteLength, long radixFreeLength) {
		if (byteLength < 0) throw new IllegalArgumentException("negative byteLength");
		if (radixFreeLength < 0) throw new IllegalArgumentException("negative radixFreeLength");
		if (radixFreeLength > byteLength) throw new IllegalArgumentException("radixFreeLength exceeds byteLength");
		
		if (!optimistic) radixFreeLength = 0L;
		// calculate length of radix encoded bytes
		long radixedLength = byteLength - radixFreeLength;
		long encodedLength = radixFreeLength + radixedLength / 3 * 4;

		// adjust for remainder
		switch ((int)(radixedLength % 3)) {
		case 1 : encodedLength += 2; break;
		case 2 : encodedLength += 3; break;
		}
		
		// adjust for termination
		if (terminated) encodedLength ++;
		
		// adjust for optimism
		if (optimistic && (terminated || radixFreeLength < byteLength)) encodedLength ++;
		
		// adjust for line breaks
		if (lineLength != Radix4Config.NO_LINE_BREAK && encodedLength > 0) {
			encodedLength += extraLineBreakLength(encodedLength);
		}
		
		return encodedLength;
	}

	// object methods
	
	/**
	 * Two Radix4 definitions are equal if they produce identical codings for
	 * all inputs.
	 */
	
	//TODO this definition doesn't cover decoding, so whitespace isn't included - should change?
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Radix4)) return false;
		Radix4 that = (Radix4) obj;
		if (this.streaming != that.streaming) return false;
		if (this.optimistic != that.optimistic) return false;
		if (this.terminated != that.terminated) return false;
		if (this.lineLength != that.lineLength) return false;
		if (lineLength != Radix4Config.NO_LINE_BREAK && !this.lineBreak.equals(that.lineBreak)) return false;
		if ((terminated || optimistic) && this.terminator != that.terminator) return false;
		if (!this.mapping.equals(that.mapping)) return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = mapping.hashCode();
		hash += lineLength;
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
	
	// package scoped methods
	
	int extraLineBreakLength(int encodedLength) {
		return encodedLength == 0 ? 0 : ((encodedLength - 1) / lineLength) * lineBreak.length();
	}
	
	long extraLineBreakLength(long encodedLength) {
		return encodedLength == 0L ? 0L : ((encodedLength - 1) / lineLength) * lineBreak.length();
	}
	
	int lookupByte(int c) {
		return c >=0 && c < 256 ? bytes[c] : -1;
	}
	
	boolean isWhitespace(int c) {
		return c < 256 && bytes[c] == -2;
	}
	
	// private helper methods
	
	// serialization
	
	private Object writeReplace() throws ObjectStreamException {
		return new Serial(this);
	}
	
	private static class Serial implements Serializable {
		
		private static final long serialVersionUID = 3209555487248076351L;

		private final Radix4Config config;
		
		Serial(Radix4 radix4) {
			config = new Radix4Config(radix4);
		}
		
		private Object readResolve() throws ObjectStreamException {
			return new Radix4(config);
		}
		
	}
}
