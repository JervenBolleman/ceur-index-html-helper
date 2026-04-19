package swiss.sib.swissprot.checks;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class TextChecks {
	private static final Pattern CP = Pattern.compile("Creative.*Commons.*License|Commons.*License.*Attribution");
	private static final String THIS_YEAR = Integer.toString(LocalDate.now().getYear());
	private static final String LAST_YEAR = Integer.toString(LocalDate.now().getYear() - 1);
	private static final String BEFORE_LAST_YEAR = Integer.toString(LocalDate.now().getYear() - 2);
	private static final Pattern LAST_TWO_YEAR = Pattern
			.compile('(' + THIS_YEAR + '|' + LAST_YEAR + '|' + BEFORE_LAST_YEAR+')');
	private static final Pattern DECL_AI = Pattern.compile("Declaration .. [G|g]enerative AI|[G|g]enerative AI *[D|d]eclaration");
	private static final Pattern CUER_BEFORE_PUBLICATION = Pattern.compile("(CEUR-WS.org)|CEUR Workshop Proceedings");
	private static final Pattern BAD_CONFERENCE = Pattern.compile("Woodstock.*22");
	
	
	public static List<Failure> check(PDDocument data) {
		PDFTextStripper stripper = new PDFTextStripper();
		stripper.setStartPage(1);
		stripper.setEndPage(2);
		List<Failure> failures = new ArrayList<>();
		try {
			String text = stripper.getText(data);
			if (!CP.matcher(text).find()) {
				failures.add(new Failure("Copyright statement not found"));
			} 
			if (!LAST_TWO_YEAR.matcher(text).find()) {
				failures.add(new Failure("Year of writing can't be right"));
			} 
			if (CUER_BEFORE_PUBLICATION.matcher(text).find()) {
				failures.add(new Failure("PDF contains CEUR.org before publication, does like you used an old CEUR template not the current one"));
			} else if (BAD_CONFERENCE.matcher(text).find()) {
				failures.add(new Failure("CEUR template conference was left at default, please change"));
			}
			if (! DECL_AI.matcher(text).find()) {
				stripper.setStartPage(3);
				stripper.setEndPage(data.getNumberOfPages());
				text = stripper.getText(data);
				if (! DECL_AI.matcher(text).find()) {
					failures.add(new Failure("Missing declaritive AI section"));
				}
			}
		} catch (IOException e) {
			failures.add(new Failure("Issue reading PDF"));
		}
		return failures;
	}
}
