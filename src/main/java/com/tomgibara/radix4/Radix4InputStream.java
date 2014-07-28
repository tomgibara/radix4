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

import static com.tomgibara.radix4.Radix4.lookupByte;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

abstract class Radix4InputStream extends InputStream {

	private final Radix4 radix4;
	private final int termChar;
	private boolean radixFree;
	private int i = 0;
	private int j = 3;
	private int[] bs = new int[3];
	
	Radix4InputStream(Radix4 radix4) {
		this.radix4 = radix4;
		termChar = radix4.terminator;
		radixFree = radix4.optimistic;
	}

	@Override
	public int read() throws IOException {
		if (i == j) return -1;
		if (radixFree) {
			int b = lookupNonWS();
			switch (b) {
			case -1: // eos
				if (radix4.terminated) throw new IOException("unexpected end of stream");
				j = 0;
				break;
			case -3: // terminator - end of radix free
				radixFree = false;
				break; // falling through to decoding
				default: // just unmap and return
					return Radix4.decmap[b];
			}
		}
		if (i == 0) {
			int radix = lookupNonWS();
			if (radix < 0) {
				// check for premature eos
				if (radix == -1 && radix4.terminated) throw new IOException("unexpected end of stream");
				if (radix == -3 && !radix4.terminated) throw new IOException("unexpected terminator");
				j = 0;
				return -1;
			}
			int b0 = lookupNonWS();
			//TODO need to distinguish termination & eos
			//TODO check for terminator too?
			if (b0 == -1) throw new IOException("unexpected end of stream");
			bs[0] = b0 | ((radix << 2) & 0xc0);
			int b1 = lookupNonWS();
			if (b1 < 0) {
				j = 1;
			} else {
				bs[1] = b1 | ((radix << 4) & 0xc0);
				int b2 = lookupNonWS();
				if (b2 < 0) {
					j = 2;
				} else {
					bs[2] = b2 | ((radix << 6) & 0xc0);
				}
			}
		}
		int b = bs[i];
		if (++i == 3) i = 0;
		// unmap the byte
		return Radix4.decmap[b];
	}

	abstract int readChar() throws IOException;
	
	private int lookupNonWS() throws IOException {
		while (true) {
			int c = readChar();
			if (c == -1) return -1;
			if (c == termChar) return -3;
			int b = lookupByte(c);
			if (b == -1) throw new IOException("invalid character");
			if (b == -2) continue;
			return b;
		}
	}
	
	final static class ByteStream extends Radix4InputStream {

		private final InputStream in;

		public ByteStream(Radix4 radix4, InputStream in) {
			super(radix4);
			this.in = in;
		}
		
		@Override
		int readChar() throws IOException {
			return in.read();
		}
		
	}

	final static class CharStream extends Radix4InputStream {

		private final Reader reader;
		
		CharStream(Radix4 radix4, Reader reader) {
			super(radix4);
			this.reader = reader;
		}
		
		@Override
		int readChar() throws IOException {
			return reader.read();
		}
	}

	final static class Chars extends Radix4InputStream {

		private final CharSequence chars;
		private final int length;
		private int position;
		
		public Chars(Radix4 radix4, CharSequence chars) {
			super(radix4);
			this.chars = chars;
			length = chars.length();
			position = 0;
		}
		
		@Override
		int readChar() throws IOException {
			if (position >= length) return -1;
			return chars.charAt(position++);
		}
		
	}
	
}
