package swiss.sib.swissprot;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import swiss.sib.swissprot.checks.Issue;
import swiss.sib.swissprot.checks.TextChecks;

public class PdfDataExtractor {
	private static final Pattern ORCID_PATTERN = Pattern
			.compile("(\\d{4}\n?-\\n?\\d{4}\\n?-\\n?\\d{4}\\n?-\\n?\\d{3}[\\dX])\\s\\(([^\\)]+)\\)");
	/**
	 * Creator of the ODF template, not likely to be the author the actual paper.
	 */
	private static final String ALEKSANDR_OMETOV_TAU = "Aleksandr Ometov (TAU)";

	public static class Author {
		private String name;
		private String orcid;
		private String affiliation;

		public Author(String name) {
			this.name = name;
		}

		public String orcid() {
			return orcid;
		}

		public String name() {
			return name;
		}

		public String affiliation() {
			return affiliation;
		}
		
		public String toString() {
			return "A: "+name+ '('+orcid+") at "+affiliation;
		}
	}

	public static record PdfData(String title, List<Author> authors, boolean hasLibertinus, List<Issue> failures, List<String> pages) {

	}

	public static PdfData extract(PDDocument doc) throws IOException {
		PDDocumentInformation doci = doc.getDocumentInformation();
		String title = doci.getTitle();
		PDDocumentCatalog cata = doc.getDocumentCatalog();
		boolean hasLibertinus = detectLibertinusFonts(doc);
		PDMetadata metadata = cata.getMetadata();
		List<String> authorNames = new ArrayList<>();
		FontAwareStripper stripper = new FontAwareStripper();
		stripper.getText(doc);
		if (title == null) {
			title = findTitleByLargestFont(stripper);
		}
		if (metadata != null)
			extractAuthorNamesFromMetadata(metadata, authorNames);
		if (authorNames.isEmpty()) {
			authorNames = findAuthorsBeforeAbstract(stripper);
		}
		List<Author> authors = authorNames.stream().map(Author::new).toList();
		findEmailsAndOrcids(stripper.pages(), authors);
		List<Issue> failures = TextChecks.check(stripper.pages());
		if (title != null) {
			return new PdfData(title, authors, hasLibertinus, failures, stripper.pages());
		}
		return null;
	}

	private static boolean detectLibertinusFonts(PDDocument doc) throws IOException {
		boolean hasLibertinus = false;
		for (PDPage page : doc.getPages()) {
            PDResources resources = page.getResources();
            if (resources != null) {
                for (COSName fontName : resources.getFontNames()) {
                    PDFont font = resources.getFont(fontName);
                    if (font.isEmbedded() && font.getName() != null && font.getName().contains("Libertinus")) {
                    	hasLibertinus = true;
                    }
                }
            }
        }
		return hasLibertinus;
	}

	static void findEmailsAndOrcids(List<String> pages, List<Author> authorsOrig) {
		List<Author> authors = new ArrayList<>();
		authors.addAll(authorsOrig);
		String text = pages.getFirst();
		var orcMatcher = ORCID_PATTERN.matcher(text);
		while (orcMatcher.find()) {
			String orcid = orcMatcher.group(1).replaceAll("\n", "");
			String person = orcMatcher.group(2).replaceAll("\n", "");
			Pattern orcidAsRegex = Pattern.compile(person.replace(".", "[\\p{L}\\p{Mn}\\p{Nd}\\p{Pc}\\.]* ?"));
			matchOrcids(authors, orcid, orcidAsRegex);
		}
	}

	private static void matchOrcids(List<Author> authors, String orcid, Pattern orcidAsRegex) {
		for (Iterator<Author> iterator = authors.iterator(); iterator.hasNext();) {
			Author author = iterator.next();
			boolean match = orcidAsRegex.asMatchPredicate().test(author.name());
			
			if (match) {
				author.orcid = orcid;
				iterator.remove();
				return;
			}
		}
	}

	private static final Pattern TILDE = Pattern.compile("~", Pattern.LITERAL);
	private static void extractAuthorNamesFromMetadata(PDMetadata metadata, List<String> authorNames) {
		try (var in = metadata.createInputStream()) {
			
			String xmlAndrdfxml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			int start = xmlAndrdfxml.indexOf("<rdf");
			int end = xmlAndrdfxml.lastIndexOf("RDF>");
			String rdfxml = xmlAndrdfxml.substring(start, end + 4);
			Model model = Rio.parse(new StringReader(rdfxml), "https://example.org/", RDFFormat.RDFXML);
			for (var set : model.getStatements(null, DC.CREATOR, null)) {
				for (var seq : model.getStatements((Resource) set.getObject(), null, null)) {
					if (!seq.getPredicate().equals(RDF.TYPE)
							&& seq.getPredicate().getNamespace().equals(RDF.NAMESPACE)) {
						String authorName = seq.getObject().stringValue();
						// This is the person whom made the template not the actual author.
						
						if (!authorName.equals(ALEKSANDR_OMETOV_TAU)) {
							authorNames.add(TILDE.matcher(authorName).replaceAll(" "));
						}
					}
				}
			}
		} catch (IOException | RDF4JException e) {
			// Ignore, because we are going to try and get the names out in a different way.
		}
	}

	public static String findTitleByLargestFont(FontAwareStripper stripper) throws IOException {

		List<TextLine> lines = stripper.getLines();
		if (lines.isEmpty())
			return "";

		// Find the title size (assume it's the absolute largest font on page 1)
		float maxTitleSize = 0;
		for (TextLine line : lines) {
			if (line.fontSize > maxTitleSize) {
				maxTitleSize = line.fontSize;
			}
		}
		StringBuilder title = new StringBuilder();

		for (TextLine line : lines) {
			if (line.fontSize == maxTitleSize) {
				if (!title.isEmpty()) {
					title.append(" ");
				}
				title.append(line.text);
			}
		}
		return title.toString();
	}

	private static final Pattern COR_MARKS_ETC = Pattern.compile("[\\*†‡\\s]+");
	private static final Pattern MULTIPLE_COMMA = Pattern.compile(",+");
	private static final Pattern COMMA = Pattern.compile(",");

	public static List<String> findAuthorsBeforeAbstract(FontAwareStripper stripper) throws IOException {
		List<TextLine> lines = stripper.getLines();
		if (lines.isEmpty())
			return List.of();

		// Find the title size (assume it's the absolute largest font on page 1)
		float maxTitleSize = 0;
		int lastTitleLine = 0;
		for (int i = 0; i < lines.size(); i++) {
			TextLine line = lines.get(i);
			if (line.fontSize >= maxTitleSize) {
				maxTitleSize = line.fontSize;
				lastTitleLine = i;
			}
		}
		StringBuilder authors = new StringBuilder();
		int firstAuthorLine = lastTitleLine + 1;
		float authorLineFontSize = lines.get(firstAuthorLine).fontSize;
		int lastAuthorLine = extractLinesWithAuthorNames(lines, lastTitleLine, authors, authorLineFontSize);
		List<String> affiliations = extractAffiliations(lines, lastAuthorLine, authorLineFontSize);
		int maxAffiliationDigits = Integer.toString(affiliations.size()).length();
		// Remove a dot at the end.
		if (authors.charAt(authors.length() - 1) == '.')
			authors.setLength(authors.length() - 2);
		int lastAnd = authors.lastIndexOf(" and ");
		if (lastAnd >0) {
			authors.replace(lastAnd, lastAnd + 5, ", ");
		}
		Pattern removeAffiliations = Pattern.compile("\\d{1," + maxAffiliationDigits + "} ?,");
		String noTokens = COR_MARKS_ETC.matcher(authors).replaceAll(" ");
		Matcher matcher = removeAffiliations.matcher(noTokens);
		while(matcher.find()) {
			String group = matcher.group();
			
		}
		matcher.reset();
		String noAffi = matcher.replaceAll(",");
		String noDouble = MULTIPLE_COMMA.matcher(noAffi).replaceAll(",");
		return COMMA.splitAsStream(noDouble).map(String::trim).toList();
	}

	private static final Pattern START_DIGITS = Pattern.compile("^\\d+");

	private static List<String> extractAffiliations(List<TextLine> lines, int lastAuthorLine, float authorLineFontSize) {
		List<String> affiliations = new ArrayList<>();
		for (int i = lastAuthorLine; i < lines.size(); i++) {
			TextLine line = lines.get(i);

			if ("Abstract".equalsIgnoreCase(line.text)) {
				return affiliations;
			} else {
				Matcher matcher = START_DIGITS.matcher(line.text);
				if (! matcher.find()) {
					affiliations.add(line.text);
				}
			}
		}
		return affiliations;
	}

	private static int extractLinesWithAuthorNames(List<TextLine> lines, int lastTitleLine, StringBuilder authors,
			float authorLineFontSize) {
		int lastAuthorLine = lastTitleLine + 1;
		for (int i = lastTitleLine + 1; i < lines.size(); i++) {
			TextLine line = lines.get(i);

			if (line.fontSize != authorLineFontSize) {
				lastAuthorLine = i;
				break;
			}
			if (!authors.isEmpty()) {
				authors.append(" ");
			}
			authors.append(line.text);
		}
		return lastAuthorLine;
	}

	public static class FontAwareStripper extends PDFTextStripper {
		private final List<TextLine> lines = new ArrayList<>();
		private final List<String> pages = new ArrayList<>();

		public FontAwareStripper() throws IOException {
			super();
			setSortByPosition(true); // Crucial for getting lines in visual reading order
		}

		public List<String> pages() {
			return pages;
		}

		@Override
		protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
			if (text.trim().isEmpty() || textPositions.isEmpty()) {
				return;
			}

			// Find the maximum font size in this specific string of text
			float maxFontSize = 0;
			for (TextPosition position : textPositions) {
				if (position.getFontSizeInPt() > maxFontSize) {
					maxFontSize = position.getFontSizeInPt();
				}
			}

			lines.add(new TextLine(text.trim(), maxFontSize));
			super.writeString(text, textPositions);
		}

		public List<TextLine> getLines() {
			return lines;
		}

		@Override
		public void processPage(PDPage page) throws IOException {
			output = new StringWriter();
			super.processPage(page);
			output.flush();
			pages.add(output.toString());
		}
	}

	public static class TextLine {
		public String text;
		public float fontSize;

		public TextLine(String text, float fontSize) {
			this.text = text;
			this.fontSize = fontSize;
		}

		@Override
		public String toString() {
			return String.format("[Size: %.2f] %s", fontSize, text);
		}
	}
}
