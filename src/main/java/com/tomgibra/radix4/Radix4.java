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

public final class Radix4 {

	static final Charset ASCII = Charset.forName("ASCII");
	
	static final byte[] chars = {
		/*  0 -  7 */
		'_', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
		/*  8 - 15 */
		'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
		/* 16 - 23 */
		'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
		/* 24 - 31 */
		'X', 'Y', 'Z', '0', '1', '2', '3', '4',
		/* 32 - 39 */
		'-', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
		/* 40 - 47 */
		'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
		/* 48 - 55 */
		'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
		/* 56 - 63 */
		'x', 'y', 'z', '5', '6', '7', '8', '9',
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

	public static Radix4 use() {
		return instance;
	}
	
	public static Radix4 use(Radix4Policy policy) {
		return new Radix4(policy.immutableCopy());
	}
	
	private final Radix4Policy policy;
	
	private Radix4(Radix4Policy policy) {
		this.policy = policy;
	}
	
	public Radix4Policy getPolicy() {
		return policy;
	}
	
	public OutputStream createStreamOutput(OutputStream out) {
		if (out == null) throw new IllegalArgumentException("null out");
		return new Radix4OutputStream.ByteStream(policy, out);
	}
	
	public OutputStream createWriterOutput(Writer writer) {
		if (writer == null) throw new IllegalArgumentException("null writer");
		return new Radix4OutputStream.CharStream(policy, writer);
	}
	
	public OutputStream createStringOutput(StringBuilder builder) {
		if (builder == null) throw new IllegalArgumentException("null builder");
		return new Radix4OutputStream.Chars(policy, builder);
	}
	
	public InputStream createStreamInput(InputStream in) {
		if (in == null) throw new IllegalArgumentException("null in");
		return new Radix4InputStream.ByteStream(in);
	}
	
	public InputStream createReaderInput(Reader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		return new Radix4InputStream.CharStream(reader);
	}
	
	public InputStream createStringInput(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		return new Radix4InputStream.Chars(chars);
	}
	
	public long computeEncodedLength(long length) {
		long encodedLength = length / 3 * 4;

		// adjust for remainder
		switch ((int)(length % 3)) {
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

	public byte[] decodeToBytes(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Radix4InputStream in = new Radix4InputStream.Chars(chars);
		transfer(in, out);
		return out.toByteArray();
	}
	
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
