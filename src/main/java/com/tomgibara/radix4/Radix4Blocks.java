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

	private final Radix4Policy policy;
	
	Radix4Blocks(Radix4Policy policy) {
		this.policy = policy;
	}
	
	@Override
	public String encodeToString(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		return new Radix4BlockEncoder.CharsEncoder(policy).encode(bytes);
	}

	@Override
	public byte[] encodeToBytes(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		return new Radix4BlockEncoder.BytesEncoder(policy).encode(bytes);
	}

	@Override
	public byte[] decodeFromString(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		return new Radix4BlockDecoder.CharsDecoder(policy, chars).decode();
	}

	@Override
	public byte[] decodeFromBytes(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		return new Radix4BlockDecoder.BytesDecoder(policy, bytes).decode();
	}

}
