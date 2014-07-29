package com.tomgibara.radix4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.tomgibara.radix4.Radix4;

import junit.framework.TestCase;

public class Radix4Test extends TestCase {

	public void testMappingInverse() {
		 for (int i = 0; i < 256; i++) {
			assertEquals(i, Radix4.encmap[Radix4.decmap[i]]);
			assertEquals(i, Radix4.decmap[Radix4.encmap[i]]);
		}
	}
	
	public void testSerialization() throws IOException, ClassNotFoundException {
		String message = "%^&*()";

		{
			byte[] streamBytes = serialize(Radix4.stream());
			Radix4 stream = (Radix4) deserialize(streamBytes);
			assertTrue(stream.isStreaming());
			String streamEnc = stream.coding().encodeToString(message.getBytes());
			String streamDec = new String(stream.coding().decodeFromString(streamEnc));
			assertEquals(message, streamDec);
		}
		
		{
			byte[] blockBytes = serialize(Radix4.block());
			Radix4 block = (Radix4) deserialize(blockBytes);
			assertFalse(block.isStreaming());
			String blockEnc = block.coding().encodeToString(message.getBytes());
			String blockDec = new String(block.coding().decodeFromString(blockEnc));
			assertEquals(message, blockDec);
		}
	}
	
	byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(obj);
		out.close();
		return baos.toByteArray();
	}
	
	Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream in = new ObjectInputStream(bais);
		try {
			return in.readObject();
		} finally {
			in.close();
		}
	}
}
