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


import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

public class Radix4MappingTest extends TestCase {

	private static final Random random = new Random(0);
	
	private static final char[] ASCII = new char[128];
	static {
		for (char c = 0; c < ASCII.length; c++) {
			ASCII[c] = c;
		}
	}

	private static void shuffle(char[] chars, Random r) {
		for (int i = chars.length; i > 1; i--) {
			int j = r.nextInt(i);
			char t = chars[j];
			chars[j] = chars[i - 1];
			chars[i - 1] = t;
		}
	}
	
	private static char[] randomAscii(Random random) {
		char[] ascii = ASCII.clone();
		shuffle(ascii, random);
		return ascii;
	}
	
	private static char[] randomChars(Random random) {
		return Arrays.copyOfRange(randomAscii(random), 0, 64);
	}
	
	static Radix4Mapping randomMapping(Random random) {
		return new Radix4Mapping(randomChars(random));
	}
	
	public void testCharCons() {
		testCharCons(ASCII);
		testCharCons(randomAscii(random));
		
		try {
			new Radix4Mapping(Arrays.copyOfRange(ASCII, 49, 49 + 63));
			fail("short chars");
		} catch (IllegalArgumentException e) {
			/* expected */
		}
		
		try {
			new Radix4Mapping(Arrays.copyOfRange(ASCII, 49, 49 + 65));
			fail("long chars");
		} catch (IllegalArgumentException e) {
			/* expected */
		}

//		TEMPORARILY UNVERIFIED
//		try {
//			new Radix4Mapping(Arrays.copyOfRange(ASCII, 0, 64));
//			fail("overlapping whitespace");
//		} catch (IllegalArgumentException e) {
//			/* expected */
//		}
	}
	
	private void testCharCons(char[] ascii) {
		char[] chars = Arrays.copyOfRange(ascii, 0, 64);
		Radix4Mapping mapping = new Radix4Mapping(chars);
		for (int i = 0; i < 256; i++) {
			int e = mapping.encmap[i];
			int d = mapping.decmap[e];
			assertEquals("Mapping was " + i + " to " + e + " to " + d, i, d);
		}
	}
	
}
