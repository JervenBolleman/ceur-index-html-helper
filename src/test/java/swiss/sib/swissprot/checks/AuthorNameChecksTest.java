package swiss.sib.swissprot.checks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import swiss.sib.swissprot.PdfDataExtractor.Author;

class AuthorNameChecksTest {

	@Test
	void test() {
		Author a = new Author("Bits to Breakthroughs Study-a-Thon Co-Author Group");
		List<Issue> check = new AuthorNameChecks(null).check(a);
		assertFalse(check.isEmpty());
		assertTrue(check.getFirst().message().contains("Team"));
	}

}
