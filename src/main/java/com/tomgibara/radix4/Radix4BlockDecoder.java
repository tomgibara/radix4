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

abstract class Radix4BlockDecoder<T> {

	private final Radix4 radix4;
	private final Radix4Mapping mapping;
	private final byte[] bytes;
	private final int[] decmap;
	
	Radix4BlockDecoder(Radix4 radix4) {
		this.radix4 = radix4;
		mapping = radix4.mapping;
		bytes = mapping.bytes;
		decmap = mapping.decmap;
	}
	
	public byte[] decode() {
		int length = length();
		if (radix4.terminated) {
			if (readByte(length - 1) != radix4.terminatorByte) {
				throw new IllegalArgumentException("missing terminator");
			} else {
				length--;
			}
		}
		int firstRadix;
		int termLength;
		if (radix4.optimistic) {
			firstRadix = length;
			termLength = 0;
			for (int i = length - 1; i >= 0; i--) {
				if (readByte(i) == radix4.terminatorByte) {
					firstRadix = i;
					termLength = 1;
					break;
				}
			}
		} else {
			firstRadix = 0;
			termLength = 0;
		}
		
		// successful optimism with redundant marker
		if (firstRadix == length - 1) length = firstRadix; 

		// compute the size of the output
		int size;
		if (firstRadix == length) {
			size = length;
		} else {
			int len = length - firstRadix - termLength;
			if ((len & 3) == 1) throw new IllegalArgumentException("invalid length");
			size = firstRadix + len * 3 / 4;
		}
		byte[] out = new byte[size];

		// transfer radix free bytes
		for (int i = 0; i < firstRadix; i++) {
			int b = mapping.lookupByte(readByte(i) & 0xff) & 0xff;
			if (b == -1) throw new IllegalArgumentException("invalid character at index " + i);
			out[i] = (byte) decmap[b];
		}
		
		// transfer radix encoded bytes
		if (firstRadix < size) {
			int start = firstRadix + termLength;
			int len = size - firstRadix;
			int index = 2;
			int offset = size + termLength; // start + len == (firstRadix + termLength) + (size - firstRadix) == size + termLength
			int radix = 0;
			for (int i = 0; i < len; i++) {
				if (++index == 3) {
					radix = mapping.lookupByte(readByte(offset) & 0xff);
					if (radix < 0) throw new IllegalArgumentException("invalid character at index " + offset);
					index = 0;
					offset ++;
				}
				int b = bytes[ readByte(start + i) & 0xff ] & 0x3f | radix << ((index + 1) << 1) & 0xc0;
				out[firstRadix + i] = (byte) decmap[b];
			}

		}
		return out;
	}

	abstract int length();
	
	abstract byte readByte(int i);
	
	final static class BytesDecoder extends Radix4BlockDecoder<byte[]> {

		private final byte[] bytes;
		private final int length;
		
		BytesDecoder(Radix4 radix4, byte[] bytes, boolean stripWhitespace) {
			super(radix4);
			final Radix4Mapping mapping = radix4.mapping;
			byte[] bs = null;
			int j = 0;
			int len = bytes.length;
			if (stripWhitespace) {
				for (int i = 0; i < len; i++) {
					byte b = bytes[i];
					if (mapping.isWhitespace(b & 0xff)) {
						if (bs == null) {
							bs = new byte[len];
							System.arraycopy(bytes, 0, bs, 0, i);
						}
					} else {
						if (bs != null) bs[j] = b;
						j++;
					}
				}
			}
			if (bs == null) {
				this.bytes = bytes;
				length = len;
			} else {
				this.bytes = bs;
				length = j;
			}
		}

		@Override
		int length() {
			return length;
		}
		
		@Override
		byte readByte(int i) {
			return bytes[i];
		}
		
	}
	
	final static class CharsDecoder extends Radix4BlockDecoder<CharSequence> {
		
		private final CharSequence chars;
		
		CharsDecoder(Radix4 radix4, CharSequence chars, boolean stripWhitespace) {
			super(radix4);
			final Radix4Mapping mapping = radix4.mapping;
			StringBuilder sb = null;
			if (stripWhitespace) {
				int len = chars.length();
				for (int i = 0; i < len; i++) {
					char c = chars.charAt(i);
//PEV					if (Radix4.isWhitespace(c)) {
					if (mapping.isWhitespace(c)) {
						if (sb == null) {
							sb = new StringBuilder(chars.subSequence(0, i));
						}
					} else if (sb != null) {
						sb.append(c);
					}
				}
			}
			this.chars = sb == null ? chars : sb;
		}

		@Override
		int length() {
			return chars.length();
		}
		
		@Override
		byte readByte(int i) {
			char c = chars.charAt(i);
			if (c > 127) throw new IllegalArgumentException("invalid character at index " + i);
			return (byte) c;
		}
		
		
	}
	
}
