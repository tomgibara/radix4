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

abstract class Radix4BlockEncoder<T> {

	final Radix4 radix4;
	private final boolean breakLines;
	final int lineBreakLength;
	private final int lineLength;
	private final int fullLineLength;
	
	Radix4BlockEncoder(Radix4 radix4) {
		this.radix4 = radix4;
		breakLines = radix4.lineLength != Radix4Policy.NO_LINE_BREAK;
		lineBreakLength = radix4.lineBreakBytes.length;
		lineLength = radix4.lineLength;
		fullLineLength = lineLength + lineBreakLength;
	}
	
	T encode(byte[] bytes) {
		long longLength = radix4.computeEncodedLength(bytes);
		if (longLength > Integer.MAX_VALUE) throw new IllegalArgumentException("bytes too long");
		int length = (int) longLength;
		allocate(length);

		// index at which to read the next byte
		int i = 0;
		// position at which to write next byte
		int position = 0;

		// first deal with any optimistic bytes
		if (radix4.optimistic) {
			while (i < bytes.length) {
				int b = bytes[i];
				// map the byte
				b = Radix4.encmap[b & 0xff];
				int c = b & 0x3f;
				if (c == b) {
					// still radix free
					position = writeWithBreaks(position, Radix4.chars[ c ]);
					i++;
				} else {
					// no longer radix free
					break;
				}
			}
			// indicate the end of radix free bytes unless it's unnecessary
			if (i < bytes.length || radix4.terminated) {
				position = writeWithBreaks(position, radix4.terminatorByte);
			}
		}

		// then deal with the rest
		{
			// offset to radices
			int offset = position + bytes.length - i;
			// adjust for line breaks
			if (breakLines) {
				int bytesSoFar = i;
				if (radix4.optimistic) bytesSoFar ++;
				offset -= radix4.extraLineBreakLength(bytesSoFar); // don't double count
				offset += radix4.extraLineBreakLength(offset);
			}

			// index within the triple: 0, 1 or 2
			int index = 0;
			// accumulates the radices of byte triples
			int radix = 0;
			while (i < bytes.length) {
				int b = bytes[i++];
				// map the byte
				b = Radix4.encmap[b & 0xff];
				int c = b & 0x3f;
				position = writeWithBreaks(position, Radix4.chars[ c ]);
				radix |= (b & 0xc0) >> ((++index) << 1);
				// write a complete radix
				if (index == 3) {
					offset = writeWithBreaks(offset, Radix4.chars[ radix ]);
					index = 0;
					radix = 0;
				}
			}
			// output any remaining radix part
			if (index != 0) {
				offset = writeWithBreaks(offset, Radix4.chars[ radix ]);
			}
		}

		// finally terminate if necessary
		if (radix4.terminated) {
			writeWithBreaks(length - 1, radix4.terminatorByte);
		}

		return generate();
	}
	
	private int writeWithBreaks(int i, byte b) {
//		writeByte(i++, b);
//		//TODO this is inefficient
//		if (breakLines && i > 0 && i % fullLineLength == 0) {
//			writeLineBreak(i);
//			i += lineBreakLength;
//		}
//		return i;
		//TODO this is inefficient
		if (breakLines && i % fullLineLength == lineLength) {
			writeLineBreak(i);
			i += lineBreakLength;
		}
		writeByte(i++, b);
		return i;
	}
	
	abstract void allocate(int length);

	abstract void writeByte(int i, byte b);

	abstract void writeLineBreak(int i);
	
	abstract T generate();
	
	abstract void dump();
	
	final static class BytesEncoder extends Radix4BlockEncoder<byte[]> {
		
		private byte[] bytes = null;
		
		BytesEncoder(Radix4 radix4) {
			super(radix4);
		}

		@Override
		void allocate(int length) {
			bytes = new byte[length];
		}
		
		@Override
		void writeByte(int i, byte b) {
			bytes[i] = b;
		}
		
		@Override
		void writeLineBreak(int i) {
			System.arraycopy(radix4.lineBreakBytes, 0, bytes, i, lineBreakLength);
		}
		
		@Override
		byte[] generate() {
			return bytes;
		}
		
		@Override
		void dump() {
			System.out.println(" ** DUMP ** ");
			if (bytes.length > 0) System.out.println((bytes[0]));
			System.out.println(new String(bytes, Radix4.ASCII));
		}
		
	}

	final static class CharsEncoder extends Radix4BlockEncoder<String> {
		
		private StringBuilder chars = null;
		
		CharsEncoder(Radix4 radix4) {
			super(radix4);
		}

		@Override
		void allocate(int length) {
			chars = new StringBuilder(length);
			chars.setLength(length);
		}
		
		@Override
		void writeLineBreak(int i) {
			byte[] bytes = radix4.lineBreakBytes;
			for (int j = 0; j < lineBreakLength; j++) {
				chars.setCharAt(i + j, (char) bytes[j]);
			}
		}
		
		@Override
		void writeByte(int i, byte b) {
			chars.setCharAt(i, (char) (b & 0xff));
		}
		
		@Override
		String generate() {
			return chars.toString();
		}
		
		@Override
		void dump() {
			System.out.println(" ** DUMP ** ");
			System.out.println(chars);
		}
		
	}

}
