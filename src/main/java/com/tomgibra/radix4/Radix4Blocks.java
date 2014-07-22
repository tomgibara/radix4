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

/**
 * Provides methods for binary-to-text and text-to-binary using Radix4 encoding.
 * Instances of this class are safe for concurrent use by multiple threads.
 * 
 * Unless otherwise indicated, passing a null parameter to any method of this
 * class will raise an {@link IllegalArgumentException}.
 * 
 * @author tomgibara
 * 
 */

public class Radix4Blocks implements Radix4Coding {

	private static byte[] decodeToBytes(CharSequence chars, int len) {
		byte[] out = new byte[len];
		int index = 2;
		int offset = len;
		int radix = 0;
		for (int i = 0; i < len; i++) {
			if (++index == 3) {
				radix = Radix4.lookupByte(chars.charAt(++offset));
				if (radix < 0) throw new IllegalArgumentException("invalid character at index " + (offset - 1));
				index = 0;
			}
			int b = Radix4.bytes[ chars.charAt(i) ] & 0x3f | radix << ((index + 1) << 1) & 0xc0;
			out[i] = (byte) Radix4.decmap[b];
		}
		return out;
	}
	
	private static byte[] decodeToBytes(byte[] bytes, int len) {
		byte[] out = new byte[len];
		int index = 2;
		int offset = len;
		int radix = 0;
		for (int i = 0; i < len; i++) {
			if (++index == 3) {
				radix = Radix4.lookupByte(bytes[++offset] & 0xff);
				if (radix < 0) throw new IllegalArgumentException("invalid character at index " + (offset - 1));
				index = 0;
			}
			int b = Radix4.bytes[ bytes[i] & 0xff ] & 0x3f | radix << ((index + 1) << 1) & 0xc0;
			out[i] = (byte) Radix4.decmap[b];
		}
		return out;
	}

	Radix4Blocks() {
	}
	
	@Override
	public String encodeToString(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		if (Radix4.isRadixFree(bytes)) return new String(bytes, Radix4.ASCII);
		int length = bytes.length;
		StringBuilder sb = new StringBuilder(length + 1 + (length + 2) / 3);
		sb.setLength(length);
		sb.append('.');
		int index = 0;
		int radix = 0;
		for (int i = 0; i < length; i++) {
			int b = Radix4.encmap[bytes[i] & 0xff];
			sb.setCharAt(i, (char) (Radix4.chars[ b & 0x3f ] & 0xff));
			radix |= (b & 0xc0) >> ((++index) << 1);
			if (index == 3) {
				sb.append((char) (Radix4.chars[ radix ] & 0xff));
				index = 0;
				radix = 0;
			}
		}
		if (index != 0) sb.append((char) Radix4.chars[ radix ]);
		return sb.toString();
	}

	@Override
	public byte[] encodeToBytes(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		if (Radix4.isRadixFree(bytes)) return bytes.clone();
		int length = bytes.length;
		byte[] out = new byte[ length + 1 + (length + 2) / 3 ];
		int index = 0;
		int offset = length;
		int radix = 0;
		for (int i = 0; i < length; i++) {
			int b = Radix4.encmap[bytes[i] & 0xff];
			out[i] = Radix4.chars[ b & 0x3f ];
			radix |= (b & 0xc0) >> ((++index) << 1);
			if (index == 3) {
				out[++offset] = Radix4.chars[ radix ];
				index = 0;
				radix = 0;
			}
		}
		out[length] = '.';
		if (index != 0) out[++offset] = Radix4.chars[ radix ];
		return out;
	}

	@Override
	public byte[] decodeFromString(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		int length = chars.length();
		for (int i = 0; i < length; i++) {
			char c = chars.charAt(i);
			if (c == '.') {
				if (length != (i + 1 + (i + 2) / 3)) {
					throw new IllegalArgumentException("misplaced '.' at index " + i);
				}
				return decodeToBytes(chars, i);
			} else if (Radix4.lookupByte(c) < 0) {
				throw new IllegalArgumentException("invalid character at index " + i);
			}
		}
		return chars.toString().getBytes(Radix4.ASCII);
	}

	@Override
	public byte[] decodeFromBytes(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		int length = bytes.length;
		for (int i = 0; i < length; i++) {
			int b = bytes[i] & 0xff;
			if (b == '.') {
				if (length != (i + 1 + (i + 2) / 3)) {
					throw new IllegalArgumentException("misplaced '.' at index " + i);
				}
				return decodeToBytes(bytes, i);
			} else if (Radix4.lookupByte(b) < 0) {
				throw new IllegalArgumentException("invalid character at index " + i);
			}
		}
		return bytes.clone();
	}

}
