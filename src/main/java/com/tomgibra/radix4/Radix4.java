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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Provides binary-to-text and text-to-binary functions. It is the entry-point
 * for accessing the functions provided by this package. Instances of this class
 * are safe for concurrent use by multiple threads.
 * 
 * Unless otherwise indicated, passing a null parameter to any method of this
 * class will raise an {@link IllegalArgumentException}.
 * 
 * @author tomgibara
 * 
 */

public final class Radix4 {

	static final Charset ASCII = Charset.forName("ASCII");
	
	static final byte[] decmap = new byte[] {
		(byte) 0x5f, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45,
		(byte) 0x46, (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x4a, (byte) 0x4b, (byte) 0x4c, (byte) 0x4d, (byte) 0x4e, (byte) 0x4f, (byte) 0x50, (byte) 0x51, (byte) 0x52, (byte) 0x53, (byte) 0x54, (byte) 0x55,
		(byte) 0x56, (byte) 0x57, (byte) 0x58, (byte) 0x59, (byte) 0x5a, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6a, (byte) 0x6b,
		(byte) 0x6c, (byte) 0x6d, (byte) 0x6e, (byte) 0x6f, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7a, (byte) 0x2d,
		(byte) 0x00, (byte) 0x25, (byte) 0x1f, (byte) 0x1e, (byte) 0x1d, (byte) 0x1c, (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05,
		(byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f, (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15,
		(byte) 0x16, (byte) 0x17, (byte) 0x18, (byte) 0x19, (byte) 0x1a, (byte) 0x27, (byte) 0x5c, (byte) 0x3a, (byte) 0x85, (byte) 0x21, (byte) 0x86, (byte) 0x3e, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8a,
		(byte) 0x3c, (byte) 0x8b, (byte) 0x23, (byte) 0x8c, (byte) 0x28, (byte) 0x22, (byte) 0x5d, (byte) 0x24, (byte) 0x8d, (byte) 0x8e, (byte) 0x8f, (byte) 0x90, (byte) 0x2a, (byte) 0x91, (byte) 0x92, (byte) 0x20,
		(byte) 0x2c, (byte) 0x93, (byte) 0x94, (byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9a, (byte) 0x9b, (byte) 0x9c, (byte) 0x9d, (byte) 0x9e, (byte) 0x9f, (byte) 0x7f, (byte) 0x1b,
		(byte) 0xa0, (byte) 0xa1, (byte) 0xa2, (byte) 0xa3, (byte) 0xa4, (byte) 0xa5, (byte) 0xa6, (byte) 0xa7, (byte) 0xa8, (byte) 0xa9, (byte) 0xaa, (byte) 0xab, (byte) 0xac, (byte) 0xad, (byte) 0xae, (byte) 0xaf,
		(byte) 0xb0, (byte) 0xb1, (byte) 0xb2, (byte) 0xb3, (byte) 0xb4, (byte) 0x40, (byte) 0xb5, (byte) 0x5e, (byte) 0xb6, (byte) 0x26, (byte) 0xb7, (byte) 0x60, (byte) 0xb8, (byte) 0xb9, (byte) 0xba, (byte) 0xbb,
		(byte) 0x5b, (byte) 0xbc, (byte) 0xbd, (byte) 0xbe, (byte) 0x2b, (byte) 0x29, (byte) 0x7d, (byte) 0x2f, (byte) 0xbf, (byte) 0xc0, (byte) 0xc1, (byte) 0xc2, (byte) 0xc3, (byte) 0xc4, (byte) 0xc5, (byte) 0x7c,
		(byte) 0x2e, (byte) 0xc6, (byte) 0xc7, (byte) 0xc8, (byte) 0xc9, (byte) 0xca, (byte) 0xcb, (byte) 0xcc, (byte) 0xcd, (byte) 0xce, (byte) 0xcf, (byte) 0xd0, (byte) 0xd1, (byte) 0xd2, (byte) 0xd3, (byte) 0xd4,
		(byte) 0xd5, (byte) 0xd6, (byte) 0xd7, (byte) 0xd8, (byte) 0xd9, (byte) 0xda, (byte) 0xdb, (byte) 0xdc, (byte) 0xdd, (byte) 0xde, (byte) 0xdf, (byte) 0xe0, (byte) 0xe1, (byte) 0xe2, (byte) 0xe3, (byte) 0xe4,
		(byte) 0xe5, (byte) 0xe6, (byte) 0xe7, (byte) 0xe8, (byte) 0xe9, (byte) 0xea, (byte) 0xeb, (byte) 0xec, (byte) 0xed, (byte) 0x3d, (byte) 0xee, (byte) 0xef, (byte) 0xf0, (byte) 0xf1, (byte) 0xf2, (byte) 0xf3,
		(byte) 0x7b, (byte) 0xf4, (byte) 0xf5, (byte) 0xf6, (byte) 0xf7, (byte) 0x3f, (byte) 0xf8, (byte) 0x3b, (byte) 0xf9, (byte) 0xfa, (byte) 0xfb, (byte) 0xfc, (byte) 0xfd, (byte) 0xfe, (byte) 0xff, (byte) 0x7e,
	};

	static final byte[] encmap = new byte[256];
	
	static {
		for (int i = 0; i < 256; i++) {
			encmap[decmap[i] & 0xff] = (byte) i;
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
	
	private static final byte[] bytes = new byte[256];
	
	static {
		Arrays.fill(bytes, (byte) -1);
		for (byte i = 0; i < 64; i++) {
			bytes[chars[i]] = i;
		}
		for (char c : whitespace) {
			bytes[c] = -2;
		}
	}
	
	static int lookupByte(int c) throws IOException {
		return c >=0 && c < 256 ? bytes[c] : -1;
	}
	
	static boolean isWhitespace(char c) {
		return c < 256 && bytes[c] == -2;
	}
	
	static boolean isTerminator(char c) {
		return c < 256 && bytes[c] == -1;
	}
	
	private static final Radix4 instance = new Radix4(Radix4Policy.DEFAULT);

	/**
	 * Obtain an instance which uses the default policy.
	 * 
	 * @return a Radix4 instance
	 * @see Radix4Policy#DEFAULT
	 */
	
	public static Radix4 use() {
		return instance;
	}
	
	/**
	 * Obtain an instance which uses the supplied policy. The policy may be used
	 * control the operating parameters of the encoding/decoding.
	 * 
	 * @return a Radix4 instance
	 * @see Radix4Policy
	 */
	
	public static Radix4 use(Radix4Policy policy) {
		if (policy == null) throw new IllegalArgumentException("null policy");
		return policy == Radix4Policy.DEFAULT ? instance : new Radix4(policy.immutableCopy());
	}
	
	private final Radix4Policy policy;
	
	private Radix4(Radix4Policy policy) {
		this.policy = policy;
	}
	
	/**
	 * The policy under which the Radix4 encoding and decoding are operating.
	 * 
	 * @return the policy, never null
	 */
	
	public Radix4Policy getPolicy() {
		return policy;
	}

	/**
	 * Provides encoding from an {@link OutputStream} containing binary data to
	 * an {@link OutputStream} to which character data will be written.
	 * 
	 * @param out
	 *            an output stream to which Radix4 encoded data should be
	 *            written
	 * @return an output stream to which binary data may be written for encoding
	 */
	
	public OutputStream outputToStream(OutputStream out) {
		if (out == null) throw new IllegalArgumentException("null out");
		return new Radix4OutputStream.ByteStream(policy, out);
	}
	
	/**
	 * Provides encoding from an {@link OutputStream} containing binary data to
	 * a {@link Writer} to which character data will be written.
	 * 
	 * @param writer
	 *            a writer to which Radix4 encoded data should be written
	 * @return an output stream to which binary data may be written for encoding
	 */
	
	public OutputStream outputToWriter(Writer writer) {
		if (writer == null) throw new IllegalArgumentException("null writer");
		return new Radix4OutputStream.CharStream(policy, writer);
	}
	
	/**
	 * Provides encoding from an {@link OutputStream} containing binary data to
	 * a {@link StringBuilder} to which character data will be appended.
	 * 
	 * @param writer
	 *            a writer to which Radix4 encoded data should be written
	 * @return an output stream to which binary data may be written for encoding
	 */
	
	public OutputStream outputToBuilder(StringBuilder builder) {
		if (builder == null) throw new IllegalArgumentException("null builder");
		return new Radix4OutputStream.Chars(policy, builder);
	}
	
	/**
	 * Provides decoding from an {@link InputStream} containing Radix4 encoded
	 * data via an {@link InputStream} from which the decoded binary data may be
	 * read.
	 * 
	 * @param in
	 *            an input stream from which Radix4 encoded data may be read
	 * @return an input stream from which the decoded binary data can be read
	 */
	
	public InputStream inputFromStream(InputStream in) {
		if (in == null) throw new IllegalArgumentException("null in");
		return new Radix4InputStream.ByteStream(in);
	}
	
	/**
	 * Provides decoding from a {@link Reader} containing Radix4 encoded
	 * data via an {@link InputStream} from which the decoded binary data may be
	 * read.
	 * 
	 * @param in
	 *            an reader from which Radix4 encoded data may be read
	 * @return an input stream from which the decoded binary data can be read
	 */
	
	public InputStream inputFromReader(Reader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		return new Radix4InputStream.CharStream(reader);
	}
	
	/**
	 * Provides decoding from a {@link CharSequence} containing Radix4 encoded
	 * data via an {@link InputStream} from which the decoded binary data may be
	 * read.
	 * 
	 * @param in
	 *            an character sequence from which Radix4 encoded data may be
	 *            read
	 * @return an input stream from which the decoded binary data can be read
	 */
	
	public InputStream inputFromChars(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		return new Radix4InputStream.Chars(chars);
	}

	/**
	 * Computes the number of ASCII characters required to encode a specified
	 * number of bytes. The character count includes the terminating sequence if
	 * one is specified by the policy.
	 * 
	 * @param byteLength
	 *            the number of bytes to be encoded
	 * @return the number characters required to Radix4 encode the specified
	 *         number of bytes
	 */

	public long computeEncodedLength(long byteLength) {
		long encodedLength = byteLength / 3 * 4;

		// adjust for remainder
		switch ((int)(byteLength % 3)) {
		case 1 : encodedLength += 2; break;
		case 2 : encodedLength += 3; break;
		}
		
		// adjust for termination
		if (policy.terminated) encodedLength ++;
		
		// adjust for line breaks
		int lineLength = policy.lineLength;
		if (lineLength != Radix4Policy.NO_LINE_BREAK && encodedLength > 0) {
			encodedLength += ((encodedLength - 1) / lineLength) * policy.lineBreakBytes.length;
		}
		
		return encodedLength;
	}
	
	/**
	 * Encodes a byte array to a string using a Radix4 encoding.
	 * 
	 * @param bytes
	 *            the bytes to encode
	 * @return a string containing the encoded bytes
	 */

	public String encodeToString(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		StringBuilder builder = new StringBuilder();
		try {
			Radix4OutputStream out = new Radix4OutputStream.Chars(policy, builder);
			out.write(bytes);
			out.close();
		} catch (IOException e) {
			// this shouldn't be possible
			throw new RuntimeException(e);
		}
		return builder.toString();
	}
	
	/**
	 * Encodes a byte array using a Radix4 encoding. The encoded data is
	 * returned as a byte array of ASCII characters.
	 * 
	 * @param bytes
	 *            the bytes to encode
	 * @return encoding characters as bytes
	 */

	public byte[] encodeToBytes(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		long encodedLength = computeEncodedLength(bytes.length);
		if (encodedLength > Integer.MAX_VALUE) throw new IllegalArgumentException("bytes too long");
		ByteArrayOutputStream baos = new ByteArrayOutputStream((int) (encodedLength));
		try {
			Radix4OutputStream out = new Radix4OutputStream.ByteStream(policy, baos);
			out.write(bytes);
			out.close();
		} catch (IOException e) {
			// this shouldn't be possible
			throw new RuntimeException(e);
		}
		return baos.toByteArray();
	}

	/**
	 * Decodes a {@link CharSequence} of Radix4 encoded binary data into a byte
	 * array.
	 * 
	 * @param chars
	 *            the Radix4 encoded data
	 * @return the decoded data as a byte array
	 */

	public byte[] decodeToBytes(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Radix4InputStream in = new Radix4InputStream.Chars(chars);
		transfer(in, out);
		return out.toByteArray();
	}
	
	/**
	 * Decodes a byte array containing of Radix4 encoded data into a byte array.
	 * 
	 * @param bytes
	 *            the Radix4 encoded data
	 * @return the decoded data as a byte array
	 */

	public byte[] decodeToBytes(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Radix4InputStream in = new Radix4InputStream.ByteStream(new ByteArrayInputStream(bytes));
		transfer(in, out);
		return out.toByteArray();
	}

	private void transfer(Radix4InputStream in, OutputStream out) {
		try {
			while (true) {
				int r = in.read();
				if (r < 0) break;
				out.write(r);
			}
			out.close();
			in.close();
		} catch (IOException e) {
			// this shouldn't be possible
			throw new RuntimeException(e);
		}
	}
	
}
