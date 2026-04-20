package swiss.sib.swissprot.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import swiss.sib.swissprot.PdfDataExtractor.Author;
import swiss.sib.swissprot.checks.Issue.Type;
import swiss.sib.swissprot.orcid.OrcidCheckResult;
import swiss.sib.swissprot.orcid.OrcidChecker;

public class AuthorNameChecks {
	private OrcidChecker oc;
	// Team authors are not allowed at CEUR warn about that
	private static final Set<String> COULD_BE_A_TEAM = Set.of("team", "registry", "consortium", "project", "institute");

	
	public AuthorNameChecks(OrcidChecker oc) {
		this.oc = oc;
	}


	public List<Issue> check(Author a) {
		List<Issue> failures = new ArrayList<>();
		OrcidCheckResult checkOne = oc.checkOne(a);
		if (!checkOne.isOk()) {
			failures.add(new Issue(Type.FAILURE, checkOne.name()));
		}
		String lc = a.name().toLowerCase(Locale.ROOT);
		for (String teamTest : COULD_BE_A_TEAM) {
			if (lc.contains(teamTest)) {
				failures.add(new Issue(Type.WARNING, "Team authors are not allowed by CEUR"));
			}
		}
		return List.of();
	}
}
