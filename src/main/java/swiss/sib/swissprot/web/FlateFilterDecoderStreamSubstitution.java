package swiss.sib.swissprot.web;

import java.io.IOException;
import java.io.InputStream;

import com.oracle.svm.core.annotate.Inject;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The WebAssembly image has no zlib: Web Image replaces the natives of
 * java.util.zip.Inflater with methods that throw
 * UnsupportedOperationException, already from its static initializer. PDFBox
 * inflates every Flate compressed stream in FlateFilterDecoderStream, so in the
 * WebAssembly build that class delegates to {@link JzlibFlateDecoderStream}.
 * The substituted constructor never creates the original Inflater.
 *
 * Only active when the image is built with -Dceur.web-image=true (see the wasm
 * profile in the pom), normal native images keep using PDFBox as is.
 */
@TargetClass(className = "org.apache.pdfbox.filter.FlateFilterDecoderStream", onlyWith = ForWebImage.class)
final class FlateFilterDecoderStreamSubstitution {

	@Inject
	@RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
	private JzlibFlateDecoderStream jzlib;

	@Substitute
	public FlateFilterDecoderStreamSubstitution(InputStream inputStream) throws IOException {
		this.jzlib = new JzlibFlateDecoderStream(inputStream);
	}

	@Substitute
	public int read() throws IOException {
		return jzlib.read();
	}

	@Substitute
	public int read(byte[] data, int offset, int length) throws IOException {
		return jzlib.read(data, offset, length);
	}

	@Substitute
	public void close() throws IOException {
		jzlib.close();
	}

	@Substitute
	public long skip(long n) {
		return jzlib.skip(n);
	}

	@Substitute
	public int available() {
		return jzlib.available();
	}
}
