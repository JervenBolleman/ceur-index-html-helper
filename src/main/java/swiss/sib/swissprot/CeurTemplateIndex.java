package swiss.sib.swissprot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import swiss.sib.swissprot.PdfDataExtractor.Author;
import swiss.sib.swissprot.checks.AuthorNameChecks;
import swiss.sib.swissprot.checks.Issue;
import swiss.sib.swissprot.html.Chrome;
import swiss.sib.swissprot.orcid.OrcidCheckResult;
import swiss.sib.swissprot.orcid.OrcidChecker;

/**
 * Writes an index.html that follows the plain CEUR-WS template
 * (https://ceur-ws.org/Vol-XXX/index.html), using only the CEUR* class tags and
 * schema.org microdata for ORCIDs, instead of the RDFa variant.
 */
class CeurTemplateIndex {
	private static final String CEUR_VERSION = "2026-02-04";

	private final MakeIndex options;
	private final OrcidChecker oc;
	private final StringBuilder html = new StringBuilder();
	private int pageStart = 1;

	CeurTemplateIndex(MakeIndex options, OrcidChecker oc) {
		this.options = options;
		this.oc = oc;
	}

	void write(ProceedingsData pd, File index) throws IOException {
		head();
		header();
		titles();
		editors(pd.preface());
		toc(pd);
		footer();
		try (BufferedWriter out = Files.newBufferedWriter(index.toPath(), StandardCharsets.UTF_8)) {
			out.write(html.toString());
		}
	}

	private void head() {
		line("<!DOCTYPE html>");
		line("<!-- CEURVERSION=" + CEUR_VERSION + " -->");
		line("<html lang=\"en\">");
		line("<head>");
		line("<meta http-equiv=\"Content-type\" content=\"text/html;charset=utf-8\">");
		line("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
		line("<link rel=\"stylesheet\" type=\"text/css\" href=\"../ceur-ws.css\">");
		line("<title>CEUR-WS.org/Vol-XXX - " + esc(options.fullConferenceTitle) + "</title>");
		if (options.runChecks) {
			line("<style>" + Chrome.ERR_CSS + "</style>");
		}
		line("</head>");
		line("<!--CEURLANG=eng -->");
		line("<body>");
	}

	private void header() {
		String year = Integer.toString(LocalDate.now().getYear());
		line("""
				<table style="border: 0; border-spacing: 0; border-collapse: collapse; width: 95%">
				<tbody><tr>
				<td style="text-align: left; vertical-align: middle">
				<a href="https://ceur-ws.org/"><div id="CEURWSLOGO"></div></a>
				</td>
				<td style="text-align: right; vertical-align: middle">
				<div style="float:left" id="CEURCCBY"></div>
				<span class="CEURVOLNR">Vol-XXX</span> <br>
				<span class="CEURURN">urn:nbn:de:0074-XXX-C</span>
				<p class="unobtrusive copyright" style="text-align: justify">Copyright &copy; JJJJ for
				the individual papers by the papers' authors.
				Copyright &copy; <span class="CEURPUBYEAR">JJJJ</span> for the volume
				as a collection by its editors.
				This volume and its papers are published under the
				Creative Commons License Attribution 4.0 International
				<a href="https://creativecommons.org/licenses/by/4.0/">(<span class="CEURLIC">CC BY 4.0</span>)</a>.</p>
				</td>
				</tr>
				</tbody></table>

				<hr>
				<br><br><br>
				""".replace("JJJJ", year));
	}

	private void titles() {
		line("<h1><a href=\"" + esc(options.confurl) + "\"><span class=\"CEURVOLACRONYM\">"
				+ esc(options.shortConferenceTitle) + "</span></a><br>");
		line("<span class=\"CEURVOLTITLE\">" + esc(options.fullConferenceTitle) + "</span></h1>");
		line("<br>");
		line("");
		line("<h2>");
		line("<span class=\"CEURFULLTITLE\">Proceedings of the " + esc(options.fullConferenceTitle) + "</span><br>");
		line("<!-- co-located with <span class=\"CEURCOLOCATED\">NONE</span> -->");
		line("</h2>");
		line("<h2><span class=\"CEURLOCTIME\">" + esc(options.city + ", " + options.period + ", " + options.year)
				+ "</span>.</h2>");
		line("");
		line("<br>");
	}

	private void editors(Submission preface) {
		line("<b> Edited by </b>");
		if (preface == null) {
			line(failure("Missing preface: can't extract editors"));
			return;
		}
		List<String> addresses = new ArrayList<>();
		line("<h3>");
		for (Author editor : preface.data().authors()) {
			StringBuilder e = new StringBuilder("   ");
			e.append(person("CEURVOLEDITOR", editor.name(), editor.orcid()));
			if (editor.affiliation().isEmpty()) {
				e.append(' ').append(failure("Missing affiliation"));
			} else {
				List<String> sups = new ArrayList<>();
				for (String address : editor.affiliation()) {
					if (!addresses.contains(address)) {
						addresses.add(address);
					}
					sups.add(Integer.toString(addresses.indexOf(address) + 1));
				}
				e.append(" <sup>").append(String.join(",", sups)).append("</sup>");
			}
			if (options.runChecks) {
				OrcidCheckResult checkOne = oc.checkOne(editor);
				if (!checkOne.isOk()) {
					e.append(' ').append(failure(checkOne.name()));
				}
			}
			line(e.append("<br>").toString());
		}
		line("</h3>");
		line("");
		for (int i = 0; i < addresses.size(); i++) {
			line("<sup>" + (i + 1) + "</sup> " + esc(addresses.get(i)) + "<br>");
		}
		line("<br>");
		line("");
		line("<hr>");
		line("");
		line("<br><br><br>");
		line("");
	}

	private void toc(ProceedingsData pd) {
		line("<div class=\"CEURTOC\">");
		line("<h2> Table of Contents </h2>");
		line("");
		line("<ul>");
		preface(pd);
		line("</ul>");
		line("<br>");
		line("");
		for (Entry<String, List<Submission>> e : pd.sections().entrySet()) {
			section(e.getKey(), e.getValue());
		}
		line("</div>");
		line("");
	}

	private void preface(ProceedingsData pd) {
		Submission preface = pd.preface();
		if (preface == null) {
			line(failure("Preface file is missing"));
			return;
		}
		int accepted = pd.sections().values().stream().mapToInt(List::size).sum();
		String submitted = options.submittedPapers == null ? "XX" : Integer.toString(options.submittedPapers);
		line("<li id=\"preface\">");
		line("<a href=\"" + esc(preface.pdfFile().getName()) + "\">Preface</a><br>");
		line("Summary: There were <span class=\"CEURSUBMITTEDPAPERS\">" + submitted
				+ "</span> papers submitted for peer-review to this workshop. Out of these,");
		line("<span class=\"CEURACCEPTEDPAPERS\">" + accepted + "</span> papers were accepted for this volume.");
		if (options.submittedPapers == null && options.runChecks) {
			line(failure("Number of submitted papers unknown, use --submitted-papers"));
		}
		line("</li>");
		line("");
	}

	private void section(String name, List<Submission> submissions) {
		String sectionId = "section-" + name.replace(" ", "_").replace(":", "").toLowerCase();
		line("<h3><span id=\"" + esc(sectionId) + "\" class=\"CEURSESSION\">" + esc(name) + "</span></h3>");
		line("");
		line("<ul>");
		for (Submission sub : submissions) {
			paper(sub);
		}
		line("</ul>");
		line("");
	}

	private void paper(Submission sub) {
		String paperId = "paper_" + sub.id();
		String title = sub.title();
		while (title.endsWith(".")) {
			title = title.substring(0, title.length() - 1);
		}
		line("<li id=\"" + paperId + "\"><a href=\"" + paperId + ".pdf\">");
		StringBuilder t = new StringBuilder("<span class=\"CEURTITLE\">").append(esc(title)).append("</span></a>");
		if (options.runChecks) {
			for (Issue issue : sub.data().failures()) {
				t.append(' ').append(issue(issue));
			}
		}
		line(t.toString());
		int pageEnd = pageStart + sub.pages() - 1;
		line("<span class=\"CEURPAGES\">" + (pageEnd > pageStart ? pageStart + "-" + pageEnd : pageStart)
				+ "</span>");
		pageStart = pageEnd + 1;
		line("<br>");
		List<Author> authors = sub.data().authors();
		for (int i = 0; i < authors.size(); i++) {
			Author a = authors.get(i);
			StringBuilder l = new StringBuilder(person("CEURAUTHOR", a.name(), a.orcid()));
			if (options.runChecks) {
				for (Issue issue : new AuthorNameChecks(oc).check(a)) {
					l.append(' ').append(issue(issue));
				}
			}
			if (i < authors.size() - 1) {
				l.append(',');
			}
			line(l.toString());
		}
		if (options.runChecks) {
			line("<!-- Originally " + sub.originalFileName().replace("--", "- -") + " -->");
		}
		line("</li>");
		line("");
	}

	private void footer() {
		String today = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now());
		line("<hr>");
		line("<span class=\"unobtrusive\">");
		line(today + ": submitted by " + esc(options.submittingEditor) + ",");
		line("            metadata incl. bibliographic data published under <a href=\"https://creativecommons.org/publicdomain/zero/1.0/\">Creative Commons CC0</a><br>");
		line("<span class=\"CEURPUBDATE\">yyyy-mm-dd</span>: published on CEUR Workshop Proceedings (CEUR-WS.org, ISSN 1613-0073)");
		line("\t|<a href=\"https://validator.w3.org/nu/?doc=https%3A%2F%2Fceur-ws.org%2FVol-XXX%2F\">valid HTML5</a>|");
		line("</span>");
		line("</body></html>");
	}

	private static String person(String clazz, String name, String orcid) {
		if (orcid == null || orcid.isBlank()) {
			return "<span class=\"" + clazz + "\">" + esc(name) + "</span>";
		}
		return "<span class=\"" + clazz + "\" itemscope itemtype=\"https://schema.org/Person\" itemid=\"https://orcid.org/"
				+ esc(orcid) + "\">" + esc(name) + "</span>";
	}

	private static String issue(Issue issue) {
		return switch (issue.t()) {
		case FAILURE -> failure(issue.message());
		case WARNING -> "<span class=\"warning\">" + esc(issue.message()) + "</span>";
		};
	}

	private static String failure(String message) {
		return "<span class=\"failure\">" + esc(message) + "</span>";
	}

	/**
	 * Minimal escaping, CEUR prefers plain unicode characters over HTML entities.
	 */
	private static String esc(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private void line(String s) {
		html.append(s).append('\n');
	}
}
