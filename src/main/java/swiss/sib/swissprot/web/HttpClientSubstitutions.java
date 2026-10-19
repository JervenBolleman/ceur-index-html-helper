package swiss.sib.swissprot.web;

import java.io.IOException;
import java.net.URI;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * There are no sockets in WebAssembly, WebMain lets the browser fetch ORCID
 * records instead. Cutting the default java.net.http fetcher keeps HttpClient
 * (and the virtual threads it uses, which older Web Image versions can't
 * compile) out of the image.
 *
 * Only active when the image is built with -Dceur.web-image=true (see the wasm
 * profile in the pom).
 */
@TargetClass(className = "swiss.sib.swissprot.orcid.OrcidChecker", onlyWith = ForWebImage.class)
final class Target_swiss_sib_swissprot_orcid_OrcidChecker {
	@Substitute
	private static Target_swiss_sib_swissprot_orcid_OrcidChecker_Response fetchWithHttpClient(URI uri)
			throws IOException {
		throw new IOException("java.net.http is not available in the browser");
	}
}

@TargetClass(className = "swiss.sib.swissprot.orcid.OrcidChecker$Response", onlyWith = ForWebImage.class)
final class Target_swiss_sib_swissprot_orcid_OrcidChecker_Response {
}
