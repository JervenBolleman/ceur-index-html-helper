package swiss.sib.swissprot.web;

import java.io.File;
import java.util.Map;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The git integration (-g) is not offered in the browser. Cutting the only two
 * calls into JGit keeps it out of the WebAssembly image, where it would be the
 * largest library.
 *
 * Only active when the image is built with -Dceur.web-image=true (see the wasm
 * profile in the pom).
 */
final class GitSubstitutions {
	private GitSubstitutions() {
	}
}

@TargetClass(className = "swiss.sib.swissprot.GitIntegration", onlyWith = ForWebImage.class)
final class Target_swiss_sib_swissprot_GitIntegration {
	@Substitute
	public static void setupGit(File outputDir) {
		throw new UnsupportedOperationException("The git integration is not available in the browser");
	}
}

@TargetClass(className = "swiss.sib.swissprot.ProceedingsData$Copy", onlyWith = ForWebImage.class)
final class Target_swiss_sib_swissprot_ProceedingsData_Copy {
}

@TargetClass(className = "swiss.sib.swissprot.ProceedingsData", onlyWith = ForWebImage.class)
final class Target_swiss_sib_swissprot_ProceedingsData {
	@Substitute
	private static void addToGit(File outputDir, boolean git, Map<String, byte[]> existingHashes,
			Target_swiss_sib_swissprot_ProceedingsData_Copy copy) {
		if (git) {
			throw new UnsupportedOperationException("The git integration is not available in the browser");
		}
	}
}
