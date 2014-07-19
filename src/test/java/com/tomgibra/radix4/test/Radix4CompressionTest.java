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
package com.tomgibra.radix4.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.DatatypeConverter;

import com.tomgibra.radix4.Radix4;

import junit.framework.TestCase;

public class Radix4CompressionTest extends TestCase {

	private static final boolean inspect = System.getProperty("inspect", "false").equals("true");
	private static final byte[] buffer = new byte[1024];

	private static void report(String message) {
		if (inspect) System.out.println(message);
	}

	// radix4's impact on compression should not be significantly worse than that of base64
	public void testGzip() throws IOException {
		String compressFiles = System.getProperty("compressFiles", "").trim();
		if (compressFiles.isEmpty()) return;
		for (String filename : compressFiles.split("\\s+")) {
			testCompression(filename);
		}
		
	}
	
	private void testCompression(String name) throws IOException {
		report(name);
		InputStream in = getClass().getClassLoader().getResourceAsStream(name);
		if (in == null) {
			report("NO RESOURCE STREAM");
			return;
		}
		byte[] bytes = readAsBytes(in);
		report("ORIGINAL LENGTH: " + bytes.length);
		byte[] gzipped = gzip(bytes);
		report("GZIPPED LENGTH: " + gzipped.length);
		byte[] encoded = Radix4.useStreams().encodeToBytes(bytes);
		report("ENCODED LENGTH: " + encoded.length);
		byte[] gzipenc = gzip(encoded);
		report("GZIPENC LENGTH: " + gzipenc.length + " *");
		byte[] encgzip = Radix4.useStreams().encodeToBytes(gzipped);
		report("ENCGZIP LENGTH: " + encgzip.length);
		byte[] base64 = DatatypeConverter.printBase64Binary(bytes).getBytes();
		report("BASE64 LENGTH: " + base64.length);
		byte[] b64enc = gzip(base64);
		report("B64ENC LENGTH: " + b64enc.length +  " +");
		report ("Worse by: " + (gzipenc.length / (double) b64enc.length - 1.0) );
		report("");
		
		// check it's within 10% of the performance of base64
		assertTrue(gzipenc.length < 1.10 * b64enc.length);
	}
	
    private static void transfer(final InputStream in, final OutputStream out) throws IOException {
        try {
            int r;
            while ((r = in.read(buffer)) > -1) {
                out.write(buffer, 0, r);
            }
        } finally {
            in.close();
            out.close();
        }
    }
    
    private static byte[] readAsBytes(InputStream in) throws IOException {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	transfer(in, out);
    	return out.toByteArray();
    }

    private static byte[] gzip(byte[] in) throws IOException {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(in);
		gzip.close();
		return out.toByteArray();
    }
    
}
