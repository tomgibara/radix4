package com.tomgibra.radix4;

public interface Radix4Coding {

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
