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
import swiss.sib.swissprot.MakeIndex;
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
			selectInput(i);
			Files.write(p, unpack(fileLength(i)));
		}
		List<String> makeIndexArgs = new ArrayList<>(List.of("-i", IN.toString(), "-o", OUT.toString()));
		makeIndexArgs.addAll(List.of(args));
		// MakeIndex itself rather than the CEUR command, so the other subcommands are not compiled in
		int exitCode = new CommandLine(new MakeIndex()).execute(makeIndexArgs.toArray(String[]::new));

		if (Files.isDirectory(OUT)) {
			try (Stream<Path> walk = Files.walk(OUT)) {
				for (Path p : walk.filter(Files::isRegularFile).toList()) {
					String relative = OUT.relativize(p).toString();
					if (!relative.startsWith(".")) {
						byte[] content = Files.readAllBytes(p);
						startOutput(relative, content.length);
						output(pack(content));
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

	@JS.Coerce
	@JS("globalThis.ceurTransfer = i;")
	private static native void selectInput(int i);

	/*
	 * File content goes between JavaScript and Java as char arrays, two bytes per
	 * char, written and read with the array exports the Web Image runtime uses for
	 * strings. That avoids a Proxy trap per byte, and works with Web Image versions
	 * that can't coerce or proxy byte arrays.
	 */

	private static byte[] unpack(int length) {
		char[] packed = new char[(length + 1) / 2];
		fillChars(packed);
		byte[] content = new byte[length];
		for (int i = 0; i < length; i++) {
			char c = packed[i / 2];
			content[i] = (byte) (i % 2 == 0 ? c : c >> 8);
		}
		return content;
	}

	private static char[] pack(byte[] content) {
		char[] packed = new char[(content.length + 1) / 2];
		for (int i = 0; i < content.length; i++) {
			packed[i / 2] |= (char) (i % 2 == 0 ? content[i] & 0xFF : (content[i] & 0xFF) << 8);
		}
		return packed;
	}

	/**
	 * Fills the array with the content of the file chosen with selectInput.
	 */
	@JS("""
			const u = globalThis.ceurInput.files[globalThis.ceurTransfer].bytes;
			const raw = c[runtime.symbol.javaNative];
			const write = getExport("array.char.write");
			const pairs = u.length >> 1;
			for (let j = 0; j < pairs; j++) { write(raw, j, u[2 * j] | (u[2 * j + 1] << 8)); }
			if (u.length & 1) { write(raw, pairs, u[u.length - 1]); }
			""")
	private static native void fillChars(char[] c);

	@JS.Coerce
	@JS("globalThis.ceurTransfer = { path: path, length: length };")
	private static native void startOutput(String path, int length);

	/**
	 * Hands the content of the file named with startOutput to the page.
	 */
	@JS("""
			const t = globalThis.ceurTransfer;
			const u = new Uint8Array(t.length);
			const raw = c[runtime.symbol.javaNative];
			const read = getExport("array.char.read");
			for (let j = 0; j < u.length; j += 2) {
				const v = read(raw, j >> 1);
				u[j] = v & 0xFF;
				if (j + 1 < u.length) { u[j + 1] = v >> 8; }
			}
			globalThis.ceurOutput.files.push({ path: t.path, bytes: u });
			""")
	private static native void output(char[] c);

	@JS.Coerce
	@JS("globalThis.ceurOutput.exitCode = code;")
	private static native void exitCode(int code);
}
