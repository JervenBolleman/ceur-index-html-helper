package swiss.sib.swissprot.checks;

import static swiss.sib.swissprot.checks.Issue.Kind.FAILURE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class TextChecks {
	private static final Pattern CP = Pattern.compile("Creative.*Commons.*License|Commons.*License.*Attribution");
	private static final String THIS_YEAR = Integer.toString(LocalDate.now().getYear());
	private static final String LAST_YEAR = Integer.toString(LocalDate.now().getYear() - 1);
	private static final String BEFORE_LAST_YEAR = Integer.toString(LocalDate.now().getYear() - 2);
	private static final Pattern LAST_TWO_YEAR = Pattern
			.compile("© ((" + THIS_YEAR + ")|(" + LAST_YEAR + ")|(" + BEFORE_LAST_YEAR + "))");
	private static final Pattern DECL_AI = Pattern
			.compile("(Declaration ..+ [G|g]enerative AI)|([G|g]enerative AI *[D|d]eclaration)");
	private static final Pattern CUER_BEFORE_PUBLICATION = Pattern.compile("(CEUR-WS.org)|CEUR Workshop Proceedings");
	private static final Pattern BAD_CONFERENCE = Pattern.compile("Woodstock.*22");

	public static List<Issue> check(List<String> pages) {
		List<Issue> failures = new ArrayList<>();
		Iterator<String> pagei = pages.iterator();
		if (!pagei.hasNext()) {
			failures.add(new Issue(FAILURE, "PDF is empty"));
		} else {
			String fp = pagei.next();
			checkFirstPage(failures, fp);

			boolean hasAiDeclaration = findDeclariationOfAIUse(fp, pagei);
			if (!hasAiDeclaration) {
				failures.add(new Issue(FAILURE, "Missing declaritive AI section"));
			}
		}

		return failures;
	}

	private static boolean findDeclariationOfAIUse(String fp, Iterator<String> pagei) {
		if (DECL_AI.matcher(fp).find()) {
			return true;
		}
		while (pagei.hasNext()) {
			String np = pagei.next();
			if (DECL_AI.matcher(np).find()) {
				return true;
			}
		}
		return false;
	}

	private static void checkFirstPage(List<Issue> failures, String text) {
		if (!CP.matcher(text).find()) {
			failures.add(new Issue(FAILURE, "Copyright statement not found"));
		}
		if (!LAST_TWO_YEAR.matcher(text).find()) {
			failures.add(new Issue(FAILURE, "Year of writing can't be right"));
		}
		if (CUER_BEFORE_PUBLICATION.matcher(text).find()) {
			failures.add(new Issue(FAILURE,
					"PDF contains CEUR.org before publication, looks like you used an old CEUR template not the current one"));
		} else if (BAD_CONFERENCE.matcher(text).find()) {
			failures.add(new Issue(FAILURE, "CEUR template conference was left at default, please change"));
		}
	}
}
