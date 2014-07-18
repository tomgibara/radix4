package com.tomgibra.radix4;

import junit.framework.TestCase;

public class Radix4Test extends TestCase {

	public void testMappingInverse() {
		 for (int i = 0; i < 256; i++) {
			assertEquals(i, Radix4.encmap[Radix4.decmap[i]]);
			assertEquals(i, Radix4.decmap[Radix4.encmap[i]]);
		}
	}
	
}
