package swiss.sib.swissprot.web;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.Inflater;
import com.jcraft.jzlib.JZlib;

/**
 * Copy of PDFBox's FlateFilterDecoderStream that uses the pure Java jzlib
 * instead of java.util.zip.Inflater, for the WebAssembly build which has no
 * zlib. See {@link FlateFilterDecoderStreamSubstitution}.
 */
public final class JzlibFlateDecoderStream extends FilterInputStream {

	private boolean isEOF = false;
	private int currentDataIndex = 0;
	private int bytesDecoded = 0;

	private final byte[] buffer = new byte[2048];
	private final byte[] decodedData = new byte[4096];
	// use nowrap mode to bypass zlib-header and checksum, as PDFBox does
	private final Inflater inflater;
	private boolean finished = false;

	public JzlibFlateDecoderStream(InputStream inputStream) throws IOException {
		super(inputStream);
		try {
			inflater = new Inflater(true);
		} catch (GZIPException e) {
			throw new IOException(e);
		}
		// skip zlib header
		in.read();
		in.read();
	}

	private boolean fetch() throws IOException {
		currentDataIndex = 0;
		if (isEOF || finished) {
			isEOF = true;
			bytesDecoded = 0;
			return false;
		}
		if (inflater.avail_in <= 0) {
			int bytesRead = in.read(buffer);
			if (bytesRead > -1) {
				inflater.setInput(buffer, 0, bytesRead, false);
			} else {
				isEOF = true;
				return false;
			}
		}
		// overwrite formerly read bytes
		if (bytesDecoded > 0) {
			Arrays.fill(decodedData, 0, bytesDecoded, (byte) 0);
		}
		inflater.setOutput(decodedData, 0, decodedData.length);
		int err = inflater.inflate(JZlib.Z_NO_FLUSH);
		bytesDecoded = inflater.next_out_index;
		switch (err) {
		case JZlib.Z_STREAM_END -> finished = true;
		case JZlib.Z_OK, JZlib.Z_BUF_ERROR -> {
			// progress made, or more input needed
		}
		default -> {
			// like PDFBox: don't throw, use the already read data or an empty stream
			isEOF = true;
			return bytesDecoded > 0;
		}
		}
		return true;
	}

	@Override
	public int read() throws IOException {
		if (isEOF) {
			return -1;
		}
		while (currentDataIndex == bytesDecoded) {
			if (!fetch()) {
				return -1;
			}
		}
		return decodedData[currentDataIndex++] & 0xFF;
	}

	@Override
	public int read(byte[] data, int offset, int length) throws IOException {
		if (isEOF) {
			return -1;
		}
		int numberOfBytesRead = 0;
		while (numberOfBytesRead < length) {
			int available = bytesDecoded - currentDataIndex;
			if (available > 0) {
				int bytes2Copy = Math.min(length - numberOfBytesRead, available);
				System.arraycopy(decodedData, currentDataIndex, data, numberOfBytesRead + offset, bytes2Copy);
				currentDataIndex += bytes2Copy;
				numberOfBytesRead += bytes2Copy;
			} else if (!fetch()) {
				break;
			}
		}
		return numberOfBytesRead == 0 && isEOF ? -1 : numberOfBytesRead;
	}

	@Override
	public void close() throws IOException {
		inflater.end();
		super.close();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public long skip(long n) {
		return 0;
	}

	@Override
	public int available() {
		return 0;
	}

	@Override
	public synchronized void mark(int readlimit) {
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IOException("reset is not supported");
	}
}
