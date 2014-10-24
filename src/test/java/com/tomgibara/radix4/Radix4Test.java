package com.tomgibara.radix4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import com.tomgibara.radix4.Radix4;

import junit.framework.TestCase;

public class Radix4Test extends TestCase {

	public void testMappingInverse() {
		Radix4Mapping mapping = Radix4Mapping.DEFAULT;
		 for (int i = 0; i < 256; i++) {
			assertEquals(i, mapping.encmap[mapping.decmap[i]]);
			assertEquals(i, mapping.decmap[mapping.encmap[i]]);
		}
	}
	
	public void testSerialization() throws IOException, ClassNotFoundException {
		String message = "%^&*()";

		{
			byte[] streamBytes = serialize(Radix4.stream());
			Radix4 stream = (Radix4) deserialize(streamBytes);
			assertEquals(Radix4.stream(), stream);
			assertTrue(stream.isStreaming());
			String streamEnc = stream.coding().encodeToString(message.getBytes());
			String streamDec = new String(stream.coding().decodeFromString(streamEnc));
			assertEquals(message, streamDec);
		}
		
		{
			byte[] blockBytes = serialize(Radix4.block());
			Radix4 block = (Radix4) deserialize(blockBytes);
			assertEquals(Radix4.block(), block);
			assertFalse(block.isStreaming());
			String blockEnc = block.coding().encodeToString(message.getBytes());
			String blockDec = new String(block.coding().decodeFromString(blockEnc));
			assertEquals(message, blockDec);
		}

//		TEMPORARILY DISABLED - no whitepsace control
//		{
//			Radix4Mapping mapping = Radix4MappingTest.randomMapping(new Random(777));
//			Radix4 original = Radix4.block().configure().setMapping(mapping).setOptimistic(true).use();
//			int[] map = original.getMapping().getDecodingMap();
//			Radix4 copy = (Radix4) deserialize( serialize(original) );
//			assertEquals(original, copy);
//			StringBuilder sb = new StringBuilder(64);
//			for (int i = 0; i < 64; i++) {
//				sb.append((char) (map[i] & 0xff));
//			}
//			String source = sb.toString();
//			String result = copy.coding().encodeToString(source.getBytes("ASCII"));
//			assertEquals(source, result);
//		}
		
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
