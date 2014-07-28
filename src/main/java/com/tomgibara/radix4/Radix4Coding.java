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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public interface Radix4Coding {

	// accessors
	
	/**
	 * The definition of the Radix4 coding being used.
	 * 
	 * @return the Radix4 definition, never null
	 */

	Radix4 getRadix4();

	// stream based methods
	
	/**
	 * Provides encoding from an {@link OutputStream} containing binary data to
	 * an {@link OutputStream} to which character data will be written.
	 * 
	 * @param out
	 *            an output stream to which Radix4 encoded data should be
	 *            written
	 * @return an output stream to which binary data may be written for encoding
	 */
	
	OutputStream outputToStream(OutputStream out);

	/**
	 * Provides encoding from an {@link OutputStream} containing binary data to
	 * a {@link Writer} to which character data will be written.
	 * 
	 * @param writer
	 *            a writer to which Radix4 encoded data should be written
	 * @return an output stream to which binary data may be written for encoding
	 */
	
	OutputStream outputToWriter(Writer writer);
	
	/**
	 * Provides encoding from an {@link OutputStream} containing binary data to
	 * a {@link StringBuilder} to which character data will be appended.
	 * 
	 * @param writer
	 *            a writer to which Radix4 encoded data should be written
	 * @return an output stream to which binary data may be written for encoding
	 */
	
	OutputStream outputToBuilder(StringBuilder builder);
	
	/**
	 * Provides decoding from an {@link InputStream} containing Radix4 encoded
	 * data via an {@link InputStream} from which the decoded binary data may be
	 * read.
	 * 
	 * @param in
	 *            an input stream from which Radix4 encoded data may be read
	 * @return an input stream from which the decoded binary data can be read
	 */
	
	InputStream inputFromStream(InputStream in);
	
	/**
	 * Provides decoding from a {@link Reader} containing Radix4 encoded
	 * data via an {@link InputStream} from which the decoded binary data may be
	 * read.
	 * 
	 * @param in
	 *            an reader from which Radix4 encoded data may be read
	 * @return an input stream from which the decoded binary data can be read
	 */
	
	InputStream inputFromReader(Reader reader);
	
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
	
	InputStream inputFromChars(CharSequence chars);

	// array based methods
	
	/**
	 * Encodes a byte array to a string using a Radix4 encoding.
	 * 
	 * @param bytes
	 *            the bytes to encode
	 * @return a string containing the encoded bytes
	 */
	String encodeToString(byte[] bytes);

	/**
	 * Encodes a byte array using a Radix4 encoding. The encoded data is
	 * returned as a byte array of ASCII characters.
	 * 
	 * @param bytes
	 *            the bytes to encode
	 * @return encoding characters as bytes
	 */

	byte[] encodeToBytes(byte[] bytes);
	
	/**
	 * Decodes a {@link CharSequence} of Radix4 encoded binary data into a byte
	 * array.
	 * 
	 * @param chars
	 *            the Radix4 encoded data
	 * @return the decoded data as a byte array
	 */

	byte[] decodeFromString(CharSequence chars);
	
	/**
	 * Decodes a byte array containing of Radix4 encoded data into a byte array.
	 * 
	 * @param bytes
	 *            the Radix4 encoded data
	 * @return the decoded data as a byte array
	 */

	byte[] decodeFromBytes(byte[] bytes);
}
