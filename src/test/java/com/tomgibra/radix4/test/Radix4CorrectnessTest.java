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
package com.tomgibra.radix4.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Pattern;

import com.tomgibra.radix4.Radix4;
import com.tomgibra.radix4.Radix4Policy;


import junit.framework.TestCase;

public class Radix4CorrectnessTest extends TestCase {

	private static final boolean inspect = System.getProperty("inspect", "false").equals("true");
	private static final Charset ASCII = Charset.forName("ASCII");
	private static final byte[] buffer = new byte[1024];
	private static final Pattern pattern = Pattern.compile("^([-_A-Za-z0-9.]+(\\s*[-_A-Za-z0-9.]+)*)?$");
	private static final int TEST_COUNT = 10000;

	private static void report(Object... objs) {
		if (inspect) {
			for (Object obj : objs) {
				if (obj instanceof byte[]) {
					byte[] bs = (byte[]) obj;
					System.out.print(bs.length + " bytes: " + Arrays.toString(bs));
				} else {
					System.out.print(obj);
				}
			}
			System.out.println();
		}
	}
	
	private Random rand = new Random(0L);
	
	public void testSimple() {	
		report(Radix4.use().encodeToString("Hello World!".getBytes()));
	}

	public void testNoTrailingLineBreaks() {
		byte[] bytes = new byte[30];
		rand.nextBytes(bytes);
		Radix4Policy policy = new Radix4Policy();
		policy.setLineLength(10);
		String str = Radix4.use(policy).encodeToString(bytes);
		assertEquals("superflous line breaks", str.trim(), str);
	}
	
	public void testWriteFailsAfterClose() throws IOException {
		OutputStream out = Radix4.use().outputToStream(new ByteArrayOutputStream());
		out.write(1);
		out.close();
		try {
			out.write(2);
			fail("write successful after close");
		} catch (IOException e) {
			/* expected */
		}
	}
	
	public void testBijection() throws IOException {
		report("* BIJECTION");
		Iterator<byte[]> tests = new TestData(0L).iterator();
		int testCount = TEST_COUNT;
		for (int i = 0; i < testCount; i++) {
			report("TEST " + i);
			Radix4Policy policy = new Radix4Policy();
			policy.setLineLength(1 + rand.nextInt(50));
			policy.setBufferSize(rand.nextInt(100));
			testBytes(tests.next(), policy);
		}
	}
	
	public void testTermination() throws IOException {
		report("* SELF TERMINATION");
		Radix4Policy policy = new Radix4Policy();
		policy.setTerminated(true);
		Iterator<byte[]> tests = new TestData(1L).iterator();
		int testCount = TEST_COUNT;
		for (int i = 0; i < testCount; i++) {
			report("TEST ", i);
			testBytes(tests.next(), policy);
		}
	}
	
	public void testChars() throws IOException {
		report("* CHARS");
		Iterator<byte[]> tests = new TestData(0L).iterator();
		int testCount = TEST_COUNT;
		for (int i = 0; i < testCount; i++) {
			report("TEST ", i);
			testChars(tests.next(), Radix4Policy.DEFAULT);
		}
	}
	
	private void testBytes(byte[] bytesIn, Radix4Policy policy) throws IOException {
		report("IN   ", bytesIn);
		report("PLCY ", " BUF:", policy.getBufferSize(), " LEN:", policy.getLineLength());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStream out = Radix4.use(policy).outputToStream(baos);
		out.write(bytesIn);
		out.close();
		String suffix = "";
		if (policy.isTerminated()) {
			while (rand.nextBoolean()) {
				suffix += (char) (32 + rand.nextInt(96));
			}
			report(suffix);
			baos.write(suffix.getBytes(ASCII));
			baos.close();
		}
		byte[] bytesOut = baos.toByteArray();
		report("OUT  ", bytesOut);
		String str = new String(bytesOut, ASCII);
		report("STR  ", str.length(), " chars ", str);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytesOut);
		InputStream in = Radix4.use(policy).inputFromStream(bais);
		baos = new ByteArrayOutputStream();
		transfer(in, baos);
		byte[] bytesBack = baos.toByteArray();
		report("BACK ", bytesBack);
		
		// check returned data matches
		assertTrue("Bytes back did not match", Arrays.equals(bytesIn, bytesBack));
		// check output contained only valid values
		assertTrue("String didn't include suffix", str.endsWith(suffix));
		str = str.substring(0, str.length() - suffix.length());
		assertTrue("String contains illegal characters: "  + str, pattern.matcher(str).matches());
		// check length is as expected
		int length = bytesIn.length;
		// adjust for suffix
		long expectedLength = Radix4.use(policy).computeEncodedLength(length) + suffix.length();
		assertEquals("Incorrect output length", expectedLength, bytesOut.length);
	}

	private void testChars(byte[] bytesIn, Radix4Policy policy) throws IOException {
		report("IN   ", bytesIn);
		Radix4 radix4 = Radix4.use(policy);
		
		StringWriter writer = new StringWriter();
		OutputStream out = radix4.outputToWriter(writer);
		out.write(bytesIn);
		out.close();
		writer.close();
		String str1 = writer.toString();
		report("STR1 ", str1);

		String str2 = radix4.encodeToString(bytesIn);
		report("STR2 ", str2);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out = radix4.outputToStream(baos);
		out.write(bytesIn);
		out.close();
		baos.close();
		byte[] bytesOut = baos.toByteArray();
		report("OUT  ", bytesOut);
		String str3 = new String(bytesOut, ASCII);
		report("STR3 ", str3);
		assertEquals(str3, str1);
	}

    private static void transfer(final InputStream in, final OutputStream out) throws IOException {
        try {
            int r;
            while ((r = in.read(buffer)) > -1) {
                out.write(buffer, 0, r);
            }
        } finally {
            in.close();
            out.close();
        }
    }
    
    private static class TestData implements Iterable<byte[]> {

    	private final long seed;
    	
    	public TestData(long seed) {
    		this.seed = seed;
		}
    	
		@Override
		public Iterator<byte[]> iterator() {
			return new Iterator<byte[]>() {

				private Random r = new Random(seed);

				@Override
				public boolean hasNext() {
					return true;
				}

				@Override
				public byte[] next() {
					int scale = r.nextInt(11);
					int length = r.nextInt(1 << scale);
					byte[] bytes = new byte[length];
					r.nextBytes(bytes);
					return bytes;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
    	
    }
}
