package swiss.sib.swissprot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import swiss.sib.swissprot.AgreementMatcher.AgreementForSubmission;

class AgreementMatcherTest {

	@TempDir
	Path tmp;

	@Test
	@Disabled
	void test() {
		AgreementMatcher.match(new File("/home/jbollema/git/swat4hcls-proceedings/2026/agreements"), List.of());
	}

	@Test
	void testText() throws IOException, InterruptedException {
		Path agreement = tmp.resolve("aggreement.pdf");
		try (var in = PdfDataExtractorTest.class.getClassLoader().getResourceAsStream("electronic_signed.pdf")) {
			Files.copy(in, agreement, StandardCopyOption.REPLACE_EXISTING);
			ArrayList<AgreementForSubmission> afs = new ArrayList<>();
			AgreementMatcher.extractTitleAndTryToMatch(List.of(), afs, tmp, agreement.toFile());
			assertFalse(afs.isEmpty());
			assertTrue(afs.getFirst().isText().isPresent());
		}
	}
}
