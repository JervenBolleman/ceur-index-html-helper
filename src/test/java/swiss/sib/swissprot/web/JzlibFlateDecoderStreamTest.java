package swiss.sib.swissprot.web;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;

import org.junit.jupiter.api.Test;

public class JzlibFlateDecoderStreamTest {

	@Test
	void inflatesLikeZlib() throws IOException {
		byte[] original = new byte[100_000];
		Random r = new Random(42);
		for (int i = 0; i < original.length; i++) {
			// compressible, but not trivially so
			original[i] = (byte) ("BT /F1 12 Tf (Hello) Tj ET\n".charAt(i % 27) + (r.nextInt(10) == 0 ? 1 : 0));
		}
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		try (DeflaterOutputStream out = new DeflaterOutputStream(compressed)) {
			out.write(original);
		}
		try (InputStream in = new JzlibFlateDecoderStream(new ByteArrayInputStream(compressed.toByteArray()))) {
			assertArrayEquals(original, in.readAllBytes());
		}
		try (InputStream in = new JzlibFlateDecoderStream(new ByteArrayInputStream(compressed.toByteArray()))) {
			ByteArrayOutputStream singleBytes = new ByteArrayOutputStream();
			for (int b; (b = in.read()) != -1;) {
				singleBytes.write(b);
			}
			assertArrayEquals(original, singleBytes.toByteArray());
		}
	}

	@Test
	void truncatedStreamKeepsWhatWasDecoded() throws IOException {
		byte[] original = "some text that will be cut short".repeat(100).getBytes();
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		try (DeflaterOutputStream out = new DeflaterOutputStream(compressed)) {
			out.write(original);
		}
		byte[] truncated = java.util.Arrays.copyOf(compressed.toByteArray(), compressed.size() / 2);
		try (InputStream in = new JzlibFlateDecoderStream(new ByteArrayInputStream(truncated))) {
			byte[] read = in.readAllBytes();
			assertEquals(new String(original, 0, read.length), new String(read));
		}
	}
}
