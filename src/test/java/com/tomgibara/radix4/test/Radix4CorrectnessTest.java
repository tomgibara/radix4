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
package com.tomgibara.radix4.test;

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

import com.tomgibara.radix4.Radix4;
import com.tomgibara.radix4.Radix4Coding;
import com.tomgibara.radix4.Radix4Policy;


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
		Radix4Policy stream = new Radix4Policy();
		Radix4Policy block = new Radix4Policy();
		block.setStreaming(false);
		
		// standard stream encoding will insert a terminator prior to the first unpreserved character (the space)
		String stdStream = Radix4.use(stream).encodeToString("Hello World!".getBytes());
		report(stdStream);
		assertTrue(stdStream.startsWith("Hello."));

		// optimism and no high bits means original input is preserved
		String goodOpt = Radix4.use(stream).encodeToString("ABC123".getBytes());
		report(goodOpt);
		assertEquals("ABC123", goodOpt);

		// termination and optimism and no high bits means two termination characters at end
		Radix4Policy policy = stream.mutableCopy();
		policy.setTerminated(true);
		String goodOptTerm = Radix4.use(policy).encodeToString("ABC123".getBytes());
		report(goodOptTerm);
		assertEquals("ABC123..", goodOptTerm);

		// block with optimistic encoding will also have a terminator prior to the space
		String stdBlock = Radix4.use(block).encodeToString("Hello World!".getBytes());
		report(stdBlock);
		assertTrue(stdBlock.startsWith("Hello."));

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
			policy.setOptimistic(rand.nextBoolean());
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
	
	public void testBlock() throws IOException {
		report("* BLOCK");
		Radix4Policy policy = new Radix4Policy();
		policy.setStreaming(false);
		Radix4Coding coding = Radix4.use(policy);
		Iterator<byte[]> tests = new TestData(0L).iterator();
		int testCount = TEST_COUNT;
		for (int i = 0; i < testCount; i++) {
			report("TEST " + i);
			byte[] bytes = tests.next();
			report("IN  ", bytes);
			String str = coding.encodeToString(bytes);
			report("STR ENC  ", str);
			byte[] bs = coding.encodeToBytes(bytes);
			report("BYTE ENC  ", bs);
			assertEquals(str, new String(bs, "ASCII"));
			byte[] decStr = coding.decodeFromString(str);
			report("STR DEC  ", decStr);
			assertTrue("byte processed result did not match", Arrays.equals(bytes, decStr));
			byte[] decBs = coding.decodeFromBytes(bs);
			report("BYTE DEC  ", decStr);
			assertTrue("byte processed result did not match", Arrays.equals(bytes, decBs));
		}
	}

	private void testBytes(byte[] bytesIn, Radix4Policy policy) throws IOException {
		testBytes(bytesIn, policy, true);
		// line breaks not supported in blocks
		if (policy.getLineLength() == 0) testBytes(bytesIn, policy, false);
	}
	
	private void testBytes(byte[] bytesIn, Radix4Policy policy, boolean streamed) throws IOException {
		report("IN   ", bytesIn);
		report("PLCY ", " BUF:", policy.getBufferSize(), " LEN:", policy.getLineLength(), " OPT:" + policy.isOptimistic(), " TRM:" + policy.isTerminated());
		policy = policy.mutableCopy();
		policy.setStreaming(streamed);
		Radix4Coding coding = Radix4.use(policy);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStream out = coding.outputToStream(baos);
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
		InputStream in = coding.inputFromStream(bais);
		baos = new ByteArrayOutputStream();
		transfer(in, baos);
		byte[] bytesBack = baos.toByteArray();
		report("BACK ", bytesBack);
		
		// check returned data matches
		assertTrue("Bytes back did not match", Arrays.equals(bytesIn, bytesBack));
		// check output contained only valid values
		assertTrue("String didn't include suffix", str.endsWith(suffix));
		str = str.substring(0, str.length() - suffix.length());
		try {
			assertTrue("String contains illegal characters: "  + str, pattern.matcher(str).matches());
		} catch (StackOverflowError e) {
			System.err.println("REGEXP overflow");
		}
		// check length is as expected
		// adjust for suffix
		long expectedLength = policy.computeEncodedLength(bytesIn) + suffix.length();
		assertEquals("Incorrect output length", expectedLength, bytesOut.length);
	}

	private void testChars(byte[] bytesIn, Radix4Policy policy) throws IOException {
		report("IN   ", bytesIn);
		Radix4Coding coding = Radix4.use(policy);
		
		StringWriter writer = new StringWriter();
		OutputStream out = coding.outputToWriter(writer);
		out.write(bytesIn);
		out.close();
		writer.close();
		String str1 = writer.toString();
		report("STR1 ", str1);

		String str2 = coding.encodeToString(bytesIn);
		report("STR2 ", str2);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out = coding.outputToStream(baos);
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