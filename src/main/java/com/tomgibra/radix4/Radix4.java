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

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * It is the entry-point for accessing the functions provided by this package.
 * Instances of this class are safe for concurrent use by multiple threads.
 * 
 * Unless otherwise indicated, passing a null parameter to any method of this
 * class will raise an {@link IllegalArgumentException}.
 * 
 * @author tomgibara
 * 
 */

public final class Radix4 {

	static final Charset ASCII = Charset.forName("ASCII");
	
	static final int[] decmap = new int[] {
		0x5f, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45,
		0x46, 0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55,
		0x56, 0x57, 0x58, 0x59, 0x5a, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b,
		0x6c, 0x6d, 0x6e, 0x6f, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x2d,
		0x00, 0x25, 0x1f, 0x1e, 0x1d, 0x1c, 0x80, 0x81, 0x82, 0x83, 0x84, 0x01, 0x02, 0x03, 0x04, 0x05,
		0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15,
		0x16, 0x17, 0x18, 0x19, 0x1a, 0x27, 0x5c, 0x3a, 0x85, 0x21, 0x86, 0x3e, 0x87, 0x88, 0x89, 0x8a,
		0x3c, 0x8b, 0x23, 0x8c, 0x28, 0x22, 0x5d, 0x24, 0x8d, 0x8e, 0x8f, 0x90, 0x2a, 0x91, 0x92, 0x20,
		0x2c, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0x9b, 0x9c, 0x9d, 0x9e, 0x9f, 0x7f, 0x1b,
		0xa0, 0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xab, 0xac, 0xad, 0xae, 0xaf,
		0xb0, 0xb1, 0xb2, 0xb3, 0xb4, 0x40, 0xb5, 0x5e, 0xb6, 0x26, 0xb7, 0x60, 0xb8, 0xb9, 0xba, 0xbb,
		0x5b, 0xbc, 0xbd, 0xbe, 0x2b, 0x29, 0x7d, 0x2f, 0xbf, 0xc0, 0xc1, 0xc2, 0xc3, 0xc4, 0xc5, 0x7c,
		0x2e, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xcb, 0xcc, 0xcd, 0xce, 0xcf, 0xd0, 0xd1, 0xd2, 0xd3, 0xd4,
		0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xdb, 0xdc, 0xdd, 0xde, 0xdf, 0xe0, 0xe1, 0xe2, 0xe3, 0xe4,
		0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xeb, 0xec, 0xed, 0x3d, 0xee, 0xef, 0xf0, 0xf1, 0xf2, 0xf3,
		0x7b, 0xf4, 0xf5, 0xf6, 0xf7, 0x3f, 0xf8, 0x3b, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xff, 0x7e,
	};

	static final int[] encmap = new int[256];
	
	static {
		for (int i = 0; i < 256; i++) {
			encmap[decmap[i]] = i;
		}
	}
	
	static final byte[] chars = {
		/*  0 -  7 */
		'_', '0', '1', '2', '3', '4', '5', '6',
		/*  8 - 15 */
		'7', '8', '9', 'A', 'B', 'C', 'D', 'E',
		/* 16 - 23 */
		'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
		/* 24 - 31 */
		'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
		/* 32 - 39 */
		'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c',
		/* 40 - 47 */
		'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
		/* 48 - 55 */
		'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
		/* 56 - 63 */
		't', 'u', 'v', 'w', 'x', 'y', 'z', '-',
	};
	
	static final char[] whitespace = { '\r', '\n', '\t', ' ' };
	
	static final byte[] bytes = new byte[256];
	
	static {
		Arrays.fill(bytes, (byte) -1);
		for (byte i = 0; i < 64; i++) {
			bytes[chars[i]] = i;
		}
		for (char c : whitespace) {
			bytes[c] = -2;
		}
	}
	
	static int lookupByte(int c) {
		return c >=0 && c < 256 ? bytes[c] : -1;
	}
	
	static boolean isWhitespace(char c) {
		return c < 256 && bytes[c] == -2;
	}
	
	static boolean isTerminator(char c) {
		return c < 256 && bytes[c] == -1;
	}

	static boolean isFixedByte(byte b) {
		//TODO could optimize with a separate table?
		return (encmap[b & 0xff] & 0xc0) == 0;
	}
	
	private static final Radix4Streams streams = new Radix4Streams(Radix4Policy.DEFAULT);
	private static final Radix4Blocks blocks = new Radix4Blocks();

	/**
	 * Obtain an object that can process Radix4 encoded streams according to the
	 * default policy. The policy controls the operating parameters of the
	 * encoding/decoding.
	 * 
	 * @return a {@link Radix4Streams} instance
	 * @see Radix4Policy#DEFAULT
	 */
	
	public static Radix4Streams useStreams() {
		return streams;
	}
	
	/**
	 * Obtain an object that can process Radix4 encoded streams according to the
	 * supplied policy. The policy controls the operating parameters of the
	 * encoding/decoding.
	 * 
	 * @return a {@link Radix4Streams} instance which uses the supplied policy
	 * @see Radix4Policy
	 */

	public static Radix4Streams useStreams(Radix4Policy policy) {
		if (policy == null) throw new IllegalArgumentException("null policy");
		return policy == Radix4Policy.DEFAULT ? streams : new Radix4Streams(policy.immutableCopy());
	}
	
	/**
	 * Obtain an object that can process Radix4 encoded blocks.
	 * 
	 * @return a {@link Radix4Blocks}
	 */

	public static Radix4Blocks useBlocks() {
		return blocks;
	}

}
