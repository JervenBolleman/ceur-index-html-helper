package swiss.sib.swissprot.web;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.graalvm.webimage.api.JS;

import picocli.CommandLine;
import swiss.sib.swissprot.CEUR;
import swiss.sib.swissprot.orcid.OrcidChecker;

/**
 * Entry point for the GraalVM Web Image (WebAssembly) build.
 *
 * The browser page puts the selected files on {@code globalThis.ceurInput} and
 * then starts the VM with the make-index arguments. The files are written into
 * the in-memory file system of the WASM image, the normal make-index command is
 * run, and everything it wrote is handed back on {@code globalThis.ceurOutput}.
 *
 * When the VM is started without input (e.g. on page load) it does nothing.
 */
public class WebMain {
	private static final Path IN = Path.of("/in");
	private static final Path OUT = Path.of("/out");

	public static void main(String[] args) throws IOException {
		int files = fileCount();
		if (files == 0) {
			return;
		}
		// there are no sockets in WebAssembly, let the browser look up ORCID records
		OrcidChecker.setFetcher(WebMain::fetchWithBrowser);
		for (int i = 0; i < files; i++) {
			Path p = IN.resolve(filePath(i));
			Files.createDirectories(p.getParent());
			byte[] content = new byte[fileLength(i)];
			fillBytes(i, content);
			Files.write(p, content);
		}
		List<String> makeIndexArgs = new ArrayList<>(List.of("make-index", "-i", IN.toString(), "-o", OUT.toString()));
		makeIndexArgs.addAll(List.of(args));
		int exitCode = new CommandLine(new CEUR()).execute(makeIndexArgs.toArray(String[]::new));

		if (Files.isDirectory(OUT)) {
			try (Stream<Path> walk = Files.walk(OUT)) {
				for (Path p : walk.filter(Files::isRegularFile).toList()) {
					String relative = OUT.relativize(p).toString();
					if (!relative.startsWith(".")) {
						output(relative, Files.readAllBytes(p));
					}
				}
			}
		}
		exitCode(exitCode);
	}

	private static OrcidChecker.Response fetchWithBrowser(URI uri) throws IOException {
		String reply = syncGet(uri.toString(), "text/turtle");
		int newline = reply.indexOf('\n');
		int status = Integer.parseInt(reply.substring(0, newline));
		if (status == 0) {
			throw new IOException("network error fetching " + uri);
		}
		return new OrcidChecker.Response(status, reply.substring(newline + 1).getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Synchronous requests are allowed in web workers, where this VM runs.
	 *
	 * @return the status code, a newline and the response text
	 */
	@JS.Coerce
	@JS("""
			try {
				const xhr = new XMLHttpRequest();
				xhr.open("GET", url, false);
				xhr.setRequestHeader("Accept", accept);
				xhr.send();
				return xhr.status + "\\n" + xhr.responseText;
			} catch (e) {
				return "0\\n" + e;
			}
			""")
	private static native String syncGet(String url, String accept);

	@JS.Coerce
	@JS("return globalThis.ceurInput === undefined ? 0 : globalThis.ceurInput.files.length;")
	private static native int fileCount();

	@JS.Coerce
	@JS("return globalThis.ceurInput.files[i].path;")
	private static native String filePath(int i);

	@JS.Coerce
	@JS("return globalThis.ceurInput.files[i].bytes.length;")
	private static native int fileLength(int i);

	/**
	 * Copies the file content into the Java array. Writing through the array
	 * proxy costs a Proxy trap per byte, so use the raw array export when this
	 * Web Image version provides it.
	 */
	@JS.Coerce
	@JS("""
			const u = globalThis.ceurInput.files[i].bytes;
			const src = new Int8Array(u.buffer, u.byteOffset, u.length);
			const raw = b[runtime.symbol.javaNative];
			if (raw !== undefined && typeof getExport === "function") {
				const write = getExport("array.byte.write");
				for (let j = 0; j < src.length; j++) { write(raw, j, src[j]); }
			} else {
				for (let j = 0; j < src.length; j++) { b[j] = src[j]; }
			}
			""")
	private static native void fillBytes(int i, byte[] b);

	@JS.Coerce
	@JS("""
			const out = globalThis.ceurOutput.files;
			const u = new Uint8Array(b.length);
			const raw = b[runtime.symbol.javaNative];
			if (raw !== undefined && typeof getExport === "function") {
				const read = getExport("array.byte.read");
				for (let j = 0; j < u.length; j++) { u[j] = read(raw, j); }
			} else {
				for (let j = 0; j < u.length; j++) { u[j] = b[j]; }
			}
			out.push({ path: path, bytes: u });
			""")
	private static native void output(String path, byte[] b);

	@JS.Coerce
	@JS("globalThis.ceurOutput.exitCode = code;")
	private static native void exitCode(int code);
}
