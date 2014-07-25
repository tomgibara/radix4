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

	private final Radix4Policy policy;
	
	Radix4BlockEncoder(Radix4Policy policy) {
		this.policy = policy;
	}
	
	T encode(byte[] bytes) {
		long longLength = policy.computeEncodedLength(bytes);
		if (longLength > Integer.MAX_VALUE) throw new IllegalArgumentException("bytes too long");
		int length = (int) longLength;
		allocate(length);

		// index at which to read the next byte
		int i = 0;
		// position at which to write next byte
		int position = 0;

		// first deal with any optimistic bytes
		if (policy.optimistic) {
			while (i < bytes.length) {
				int b = bytes[i];
				// map the byte
				b = Radix4.encmap[b & 0xff];
				int c = b & 0x3f;
				if (c == b) {
					// still radix free
					writeByte(position++, Radix4.chars[ c ]);
					i++;
				} else {
					// no longer radix free
					break;
				}
			}
			// indicate the end of radix free bytes unless it's unecessary
			if (i < bytes.length || policy.terminated) {
				writeByte(position++, policy.terminatorByte);
			}
		}
		
		// then deal with the rest
		{
			// offset to radices
			int offset = position + bytes.length - i;
			// index within the triple: 0,1 or 2
			int index = 0;
			// accumulates the radices of byte tripples
			int radix = 0;
			while (i < bytes.length) {
				int b = bytes[i++];
				// map the byte
				b = Radix4.encmap[b & 0xff];
				int c = b & 0x3f;
				writeByte(position++, Radix4.chars[ c ]);
				radix |= (b & 0xc0) >> ((++index) << 1);
				// write a complete radix
				if (index == 3) {
					writeByte(offset++, Radix4.chars[ radix ]);
					index = 0;
					radix = 0;
				}
			}
			// output any remaining radix part
			if (index != 0) {
				writeByte(offset++, Radix4.chars[ radix ]);
			}
		}

		// finally terminate if necessary
		if (policy.terminated) {
			writeByte(length - 1, policy.terminatorByte);
		}

		return generate();
	}
	
	abstract void allocate(int length);
	
	abstract void writeByte(int i, byte b);

	abstract T generate();
	
	final static class BytesEncoder extends Radix4BlockEncoder<byte[]> {
		
		private byte[] bytes = null;
		
		BytesEncoder(Radix4Policy policy) {
			super(policy);
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
		byte[] generate() {
			return bytes;
		}
		
	}

	final static class CharsEncoder extends Radix4BlockEncoder<String> {
		
		private StringBuilder chars = null;
		
		CharsEncoder(Radix4Policy policy) {
			super(policy);
		}

		@Override
		void allocate(int length) {
			chars = new StringBuilder(length);
			chars.setLength(length);
		}
		
		@Override
		void writeByte(int i, byte b) {
			chars.setCharAt(i, (char) (b & 0xff));
		}
		
		@Override
		String generate() {
			return chars.toString();
		}
		
	}

}
