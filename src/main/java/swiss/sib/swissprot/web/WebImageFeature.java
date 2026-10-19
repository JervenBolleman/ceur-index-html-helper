package swiss.sib.swissprot.web;

import java.nio.charset.Charset;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.oracle.svm.hosted.jdk.localization.LocalizationFeature;

import swiss.sib.swissprot.MakeIndex;

/**
 * Build time configuration of the WebAssembly image (see the wasm profile in the
 * pom), keeping it small:
 * <ul>
 * <li>picocli's generated reflection config (excluded in the wasm profile)
 * registers every subcommand, which makes all their code reachable. Only
 * make-index is used in the browser, so only that is registered here.</li>
 * <li>Only the one extra charset PDFBox uses, instead of all charsets.</li>
 * </ul>
 */
public class WebImageFeature implements Feature {

	@Override
	public void beforeAnalysis(BeforeAnalysisAccess access) {
		for (Class<?> c : new Class<?>[] { MakeIndex.class, MakeIndex.OutputMode.class, Integer.class }) {
			registerForPicocli(c);
		}
		registerForPicocli(access.findClassByName("java.util.Collections$UnmodifiableRandomAccessList"));

		// PDFBox's BaseParser decodes strings that are not valid UTF-8 as Windows-1252
		LocalizationFeature.addCharset(Charset.forName("windows-1252"));
	}

	private static void registerForPicocli(Class<?> c) {
		RuntimeReflection.register(c);
		RuntimeReflection.register(c.getDeclaredConstructors());
		RuntimeReflection.register(c.getConstructors());
		RuntimeReflection.register(c.getDeclaredMethods());
		RuntimeReflection.register(c.getMethods());
		RuntimeReflection.register(c.getDeclaredFields());
	}
}
