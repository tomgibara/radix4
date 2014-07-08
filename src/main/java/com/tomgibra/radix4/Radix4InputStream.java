package com.tomgibra.radix4;

import static com.tomgibra.radix4.Radix4.lookupByte;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

abstract class Radix4InputStream extends InputStream {

	private int i = 0;
	private int j = 3;
	private int[] bs = new int[3];
	
	@Override
	public int read() throws IOException {
		if (i == j) return -1;
		if (i == 0) {
			int radix = lookupNonWS();
			if (radix == -1) {
				j = 0;
				return -1;
			}
			int b0 = lookupNonWS();
			if (b0 == -1) throw new IOException("unexpected end of stream");
			bs[0] = b0 | ((radix << 2) & 0xc0);
			int b1 = lookupNonWS();
			if (b1 == -1) {
				j = 1;
			} else {
				bs[1] = b1 | ((radix << 4) & 0xc0);
				int b2 = lookupNonWS();
				if (b2 == -1) {
					j = 2;
				} else {
					bs[2] = b2 | ((radix << 6) & 0xc0);
				}
			}
		}
		int b = bs[i];
		if (++i == 3) i = 0;
		return b;
	}

	abstract int readChar() throws IOException;
	
	private int lookupNonWS() throws IOException {
		while (true) {
			int b = lookupByte( readChar() );
			if (b != -2) return b;
		}
	}
	
	final static class ByteStream extends Radix4InputStream {

		private final InputStream in;

		public ByteStream(InputStream in) {
			this.in = in;
		}
		
		@Override
		int readChar() throws IOException {
			return in.read();
		}
		
	}

	final static class CharStream extends Radix4InputStream {

		private final Reader reader;
		
		CharStream(Reader reader) {
			this.reader = reader;
		}
		
		@Override
		int readChar() throws IOException {
			return reader.read();
		}
	}

	final static class Chars extends Radix4InputStream {

		private final CharSequence chars;
		private final int length;
		private int position;
		
		public Chars(CharSequence chars) {
			this.chars = chars;
			length = chars.length();
			position = 0;
		}
		
		@Override
		int readChar() throws IOException {
			if (position >= length) return -1;
			return chars.charAt(position++);
		}
		
	}
	
}
