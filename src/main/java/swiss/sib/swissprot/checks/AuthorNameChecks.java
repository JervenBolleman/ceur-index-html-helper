package swiss.sib.swissprot.checks;

import static swiss.sib.swissprot.sjh.Elements.span;
import static swiss.sib.swissprot.sjh.Elements.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import swiss.sib.swissprot.PdfDataExtractor.Author;
import swiss.sib.swissprot.checks.Failure.Type;
import swiss.sib.swissprot.orcid.OrcidCheckResult;
import swiss.sib.swissprot.orcid.OrcidChecker;

public class AuthorNameChecks {
	private OrcidChecker oc;
	// Team authors are not allowed at CEUR warn about that
	private static final Set<String> COULD_BE_A_TEAM = Set.of("team", "registry", "consortium", "project", "institute");

	
	public AuthorNameChecks(OrcidChecker oc) {
		this.oc = oc;
	}


	public List<Failure> check(Author a) {
		List<Failure> failures = new ArrayList<>();
		OrcidCheckResult checkOne = oc.checkOne(a);
		if (!checkOne.isOk()) {
			failures.add(new Failure(Type.FAILURE, checkOne.name()));
		}
		String lc = a.name().toLowerCase(Locale.ROOT);
		for (String teamTest : COULD_BE_A_TEAM) {
			if (lc.contains(teamTest)) {
				failures.add(new Failure(Type.WARNING, "Team authors are not allowed by CEUR"));
			}
		}
		return List.of();
	}
}
