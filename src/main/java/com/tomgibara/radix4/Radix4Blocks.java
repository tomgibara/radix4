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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

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

class Radix4Blocks implements Radix4Coding {

	private final Radix4Policy policy;
	
	Radix4Blocks(Radix4Policy policy) {
		this.policy = policy;
	}

	@Override
	public Radix4Policy getPolicy() {
		return policy;
	}
	
	@Override
	public OutputStream outputToStream(final OutputStream out) {
		if (out == null) throw new IllegalArgumentException("null out");
		return new BlockOutputStream() {
			@Override
			void flush(byte[] bytes) throws IOException {
				out.write( new Radix4BlockEncoder.BytesEncoder(policy).encode(bytes) );
			}
		};
	}
	
	@Override
	public OutputStream outputToBuilder(final StringBuilder builder) {
		if (builder == null) throw new IllegalArgumentException("null builder");
		return new BlockOutputStream() {
			@Override
			void flush(byte[] bytes) throws IOException {
				builder.append( new Radix4BlockEncoder.CharsEncoder(policy).encode(bytes) );
			}
		};
	}
	
	@Override
	public OutputStream outputToWriter(final Writer writer) {
		if (writer == null) throw new IllegalArgumentException("null writer");
		return new BlockOutputStream() {
			@Override
			void flush(byte[] bytes) throws IOException {
				writer.write( new Radix4BlockEncoder.CharsEncoder(policy).encode(bytes) );
			}
		};
	}
	
	@Override
	public InputStream inputFromStream(final InputStream in) {
		if (in == null) throw new IllegalArgumentException("null in");
		return new BlockInputStream() {
			@Override
			byte[] slurp() throws IOException {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] bytes;
				if (policy.terminated) {
					boolean seekingFirstRadix = policy.optimistic;
					int term = policy.terminator;
					while (true) {
						int b = in.read();
						if (b == -1) throw new IOException("Unexpected end of stream");
						if (Radix4.isWhitespace(b)) continue; // ignore whitespace
						out.write(b);
						if (b == term) {
							if (seekingFirstRadix) {
								seekingFirstRadix = false;
							} else {
								break;
							}
						}
					}
				} else {
					byte[] buffer = new byte[policy.bufferSize];
					while (true) {
						int r = in.read(buffer);
						if (r == -1) break;
						r = stripWhitespace(buffer, r);
						out.write(buffer, 0, r);
					}
				}
				bytes = out.toByteArray();
				return new Radix4BlockDecoder.BytesDecoder(policy, bytes, false).decode();
			}
			
			private int stripWhitespace(byte[] buffer, int length) {
				int j = 0;
				for (int i = 0; i < length; i++) {
					if (!Radix4.isWhitespace(buffer[i] & 0xff)) {
						if (i != j) buffer[j] = buffer[i];
						j++;
					}
				}
				return j;
			}
		};
	}
	
	@Override
	public InputStream inputFromReader(final Reader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		return new BlockInputStream() {
			@Override
			byte[] slurp() throws IOException {
				StringBuilder sb = new StringBuilder();
				if (policy.terminated) {
					boolean seekingFirstRadix = policy.optimistic;
					int term = policy.terminator;
					while (true) {
						int c = reader.read();
						if (c == -1) throw new IOException("Unexpected end of stream");
						if (Radix4.isWhitespace(c)) continue; // ignore whitespace
						sb.append(c);
						if (c == term) {
							if (seekingFirstRadix) {
								seekingFirstRadix = false;
							} else {
								break;
							}
						}
					}
				} else {
					char[] buffer = new char[policy.bufferSize];
					while (true) {
						int r = reader.read(buffer);
						if (r == -1) break;
						r = stripWhitespace(buffer, r);
						sb.append(buffer, 0, r);
					}
				}
				return new Radix4BlockDecoder.CharsDecoder(policy, sb, false).decode();
			}
			
			private int stripWhitespace(char[] buffer, int length) {
				int j = 0;
				for (int i = 0; i < length; i++) {
					if (!Radix4.isWhitespace(buffer[i])) {
						if (i != j) buffer[j] = buffer[i];
						j++;
					}
				}
				return j;
			}
		};
	}
	
	@Override
	public InputStream inputFromChars(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		return new ByteArrayInputStream( new Radix4BlockDecoder.CharsDecoder(policy, chars, true).decode() );
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
		return new Radix4BlockDecoder.CharsDecoder(policy, chars, true).decode();
	}

	@Override
	public byte[] decodeFromBytes(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		return new Radix4BlockDecoder.BytesDecoder(policy, bytes, true).decode();
	}

	//TODO could replace with a more efficient implementation that avoids byte[] copy
	private abstract class BlockOutputStream extends ByteArrayOutputStream {
		
		private boolean closed = false;
		
		@Override
		public void close() throws IOException {
			if (closed) return;
			flush(toByteArray());
		}
		
		abstract void flush(byte[] bytes) throws IOException;
		
	}
	
	//TODO should steam optimistic bytes
	private abstract class BlockInputStream extends InputStream {
		
		private byte[] bytes;
		private int length;
		private int position;
		private int mark = -1;
		
		@Override
		public int read() throws IOException {
			ensureBytes();
			return position < length ? bytes[position++] & 0xff : -1;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (b == null) throw new NullPointerException();
			if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();

			ensureBytes();
			if (position == length) return -1;

			int available = Math.min(length - position, len);
			if (available == 0) return 0;

			System.arraycopy(bytes, position, b, off, available);
			position += available;
			return available;
		}
		
		@Override
		public void mark(int readlimit) {
			mark = position;
		}
		
		@Override
		public int available() throws IOException {
			ensureBytes();
			return length - position;
		}
		
		@Override
		public boolean markSupported() {
			return true;
		}
		
		@Override
		public void close() {
			/* does nothing */
		}

		@Override
		public synchronized void reset() throws IOException {
			if (mark == -1) throw new IOException("mark not set");
			position = mark;
		}
		
		@Override
		public long skip(long n) throws IOException {
			if (n <= 0L) return 0L;
			ensureBytes();
			int available = length - position;
			if (n > available) {
				position = length;
				return available;
			} else {
				position += (int) n;
				return n;
			}
		}

		private void ensureBytes() throws IOException {
			if (bytes != null) return;
			bytes = slurp();
			length = bytes.length;
		}
		
		abstract byte[] slurp() throws IOException;
		
	}

}
