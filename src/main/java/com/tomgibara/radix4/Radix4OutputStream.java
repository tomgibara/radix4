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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

abstract class Radix4OutputStream extends OutputStream {

	final Radix4Policy policy;
	// size of the buffer in bytes
	final int bufferSize;
	// accumulates the radices of byte tripples
	private int radix = 0;
	// position at which to write next byte into buffer
	private int position = 0;
	// the number of non-whitespace bytes written - used to track line breaking
	private long count = 0;
	// index within the triple: 0,1 or 2 -- rogue value of 3 when closed
	private int index = 0;
	// whether a byte with a non-zero radix has yet to be encountered
	private boolean radixFree;
	
	Radix4OutputStream(Radix4Policy policy) {
		if (policy == null) throw new IllegalArgumentException("null policy");
		this.policy = policy.immutableCopy();
		// set bufferSize to a multiple of 4
		// this way we avoid having to move remaining bytes around inside the buffer
		this.bufferSize = (policy.getBufferSize() + 3) & 0xfffffffc;
		this.radixFree = policy.optimistic;
	}

	@Override
	public void write(int b) throws IOException {
		// watch for close
		if (index == 3) throw new IOException("stream closed");
		// map the byte
		b = Radix4.encmap[b & 0xff];
		int c = b & 0x3f;
		if (radixFree) {
			if (c == b) {
				// still radix free
				bufferByte(position++, Radix4.chars[ c ]);
			} else {
				// no longer radix free
				flushBufferWithTerm();
				radixFree = false;
			}
		}
		if (!radixFree) {
			// make room for radices
			if (index == 0) position++;
			// check if still radix free
			bufferByte(position++, Radix4.chars[ c ]);
			// append to the radix and increment counter
			radix |= (b & 0xc0) >> ((++index) << 1);
			// store the radix when full and reset counter
			if (index == 3) {
				bufferByte(position - 4, Radix4.chars[ radix ]);
				index = 0;
				radix = 0;
			}
		}
		// if the buffer's full, empty it
		if (position == bufferSize) {
			flushBuffer();
		}
	}
	
	@Override
	public void flush() throws IOException {
		//TODO flush not simple if index non-zero
		if (index == 0) flushBuffer();
		flushUnderlying();
	}
	
	@Override
	public void close() throws IOException {
		// write back the radix
		if (index != 0) {
			bufferByte(position - index - 1, Radix4.chars[ radix ]);
		}
		if (policy.terminated) {
			// must be space in buffer here because write() never leaves it full
			bufferByte(position++, policy.terminatorByte);
			// if necessary, insert a second terminator to indicate end of radix free (ie. all) bytes
			if (radixFree) {
				flushBufferWithTerm();
			} else {
				flushBuffer();
			}
			// we don't close the underlying stream if termination is explicit
		} else {
			// for implicit termination we don't care about radix-free state
			flushBuffer();
			closeUnderlying();
		}
		// set index to a rogue value indicating that the stream is closed
		index = 3;
	}

	private void flushBufferWithTerm() throws IOException {
		// unlucky case - buffer is full, we need to flush twice
		if (position == bufferSize) flushBuffer();
		bufferByte(position++, policy.terminatorByte);
		flushBuffer();
	}
	
	// always called with index equal to zero; unless closing - in which case we don't care that radix may be flushed incomplete
	private void flushBuffer() throws IOException {
		if (position == 0) return;
		int lineLength = policy.lineLength;
		if (lineLength == Radix4Policy.NO_LINE_BREAK) {
			writeBuffer(0, position);
		} else {
			int last = 0;
			int offset = (int) (count % lineLength);
			int start = offset == 0 && count != 0 ? 0 : lineLength - offset;
			while (start < position) {
				writeBuffer(last, start);
				writeLineBreak();
				last = start;
				start += lineLength;
			}
			writeBuffer(last, position);
			count += position;
		}
		position = 0;
	}
	
	abstract void bufferByte(int i, byte b);

	abstract void writeBuffer(int from, int to) throws IOException;
	
	abstract void writeLineBreak() throws IOException;
	
	abstract void closeUnderlying() throws IOException;
	
	abstract void flushUnderlying() throws IOException;
	

	static final class ByteStream extends Radix4OutputStream {

		private final OutputStream out;
		private final byte[] buffer;
		
		ByteStream(Radix4Policy policy, OutputStream out) {
			super(policy);
			this.out = out;
			buffer = new byte[bufferSize];
		}

		@Override
		void bufferByte(int i, byte b) {
			buffer[i] = b;
		}

		@Override
		void writeBuffer(int from, int to) throws IOException {
			out.write(buffer, from, to - from);
		}

		@Override
		void writeLineBreak() throws IOException {
			out.write(policy.lineBreakBytes);
		}
		
		@Override
		void flushUnderlying() throws IOException {
			out.flush();
		}
		
		@Override
		void closeUnderlying() throws IOException {
			out.close();
		}
		
	}
	
	static final class CharStream extends Radix4OutputStream {

		private final char[] buffer;
		private final Writer writer;

		CharStream(Radix4Policy policy, Writer writer) {
			super(policy);
			this.writer = writer;
			buffer = new char[bufferSize];
		}

		@Override
		void bufferByte(int i, byte b) {
			buffer[i] = (char) b;
		}

		@Override
		void writeBuffer(int from, int to) throws IOException {
			writer.write(buffer, from, to - from);
		}
		
		@Override
		void writeLineBreak() throws IOException {
			writer.write(policy.lineBreak);
		}

		@Override
		void flushUnderlying() throws IOException {
			writer.flush();
		}
		
		@Override
		void closeUnderlying() throws IOException {
			writer.close();
		}

	}

	static final class Chars extends Radix4OutputStream {

		private final char[] buffer;
		private final StringBuilder builder;

		Chars(Radix4Policy policy, StringBuilder builder) {
			super(policy);
			this.builder = builder;
			buffer = new char[bufferSize];
		}

		@Override
		void bufferByte(int i, byte b) {
			buffer[i] = (char) b;
		}

		@Override
		void writeBuffer(int from, int to) throws IOException {
			builder.append(buffer, from, to - from);
		}
		
		@Override
		void writeLineBreak() throws IOException {
			builder.append(policy.lineBreak);
		}

		@Override
		void flushUnderlying() throws IOException {
			/* no-op */
		}
		
		@Override
		void closeUnderlying() throws IOException {
			/* no-op */
		}

	}

	
}
