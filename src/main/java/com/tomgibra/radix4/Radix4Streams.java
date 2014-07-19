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

/**
 * Provides Radix4 binary-to-text and text-to-binary conversion streams.
 * Instances of this class are safe for concurrent use by multiple threads.
 * 
 * Unless otherwise indicated, passing a null parameter to any method of this
 * class will raise an {@link IllegalArgumentException}.
 * 
 * @author tomgibara
 * 
 */

public class Radix4Streams {

	private final Radix4Policy policy;
	
	Radix4Streams(Radix4Policy policy) {
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
