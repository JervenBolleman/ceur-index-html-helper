package swiss.sib.swissprot;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static swiss.sib.swissprot.sjh.Attributes.clazz;
import static swiss.sib.swissprot.sjh.Attributes.ga;
import static swiss.sib.swissprot.sjh.Attributes.href;
import static swiss.sib.swissprot.sjh.Attributes.id;
import static swiss.sib.swissprot.sjh.Attributes.lang;
import static swiss.sib.swissprot.sjh.Attributes.rel;
import static swiss.sib.swissprot.sjh.Elements.a;
import static swiss.sib.swissprot.sjh.Elements.address;
import static swiss.sib.swissprot.sjh.Elements.article;
import static swiss.sib.swissprot.sjh.Elements.br;
import static swiss.sib.swissprot.sjh.Elements.dd;
import static swiss.sib.swissprot.sjh.Elements.div;
import static swiss.sib.swissprot.sjh.Elements.dl;
import static swiss.sib.swissprot.sjh.Elements.dt;
import static swiss.sib.swissprot.sjh.Elements.footer;
import static swiss.sib.swissprot.sjh.Elements.h1;
import static swiss.sib.swissprot.sjh.Elements.h2;
import static swiss.sib.swissprot.sjh.Elements.header;
import static swiss.sib.swissprot.sjh.Elements.li;
import static swiss.sib.swissprot.sjh.Elements.link;
import static swiss.sib.swissprot.sjh.Elements.meta;
import static swiss.sib.swissprot.sjh.Elements.p;
import static swiss.sib.swissprot.sjh.Elements.section;
import static swiss.sib.swissprot.sjh.Elements.span;
import static swiss.sib.swissprot.sjh.Elements.sup;
import static swiss.sib.swissprot.sjh.Elements.text;
import static swiss.sib.swissprot.sjh.Elements.time;
import static swiss.sib.swissprot.sjh.Elements.ul;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;

import swiss.sib.swissprot.PdfDataExtractor.Author;
import swiss.sib.swissprot.PdfDataExtractor.PdfData;
import swiss.sib.swissprot.sjh.Attributes;
import swiss.sib.swissprot.sjh.Comment;
import swiss.sib.swissprot.sjh.Elements;
import swiss.sib.swissprot.sjh.attributes.DateTime;
import swiss.sib.swissprot.sjh.attributes.content.Href;
import swiss.sib.swissprot.sjh.attributes.content.Rel;
import swiss.sib.swissprot.sjh.attributes.global.Clazz;
import swiss.sib.swissprot.sjh.attributes.global.GlobalAttribute;
import swiss.sib.swissprot.sjh.attributes.global.Id;
import swiss.sib.swissprot.sjh.attributes.grouping.Value;
import swiss.sib.swissprot.sjh.attributes.rdfa.RdfaAttribute;
import swiss.sib.swissprot.sjh.attributes.rdfa.core.Datatype;
import swiss.sib.swissprot.sjh.attributes.rdfa.lite.Property;
import swiss.sib.swissprot.sjh.attributes.rdfa.lite.RdfaLiteAttribute;
import swiss.sib.swissprot.sjh.attributes.rdfa.lite.Resource;
import swiss.sib.swissprot.sjh.attributes.rdfa.lite.Typeof;
import swiss.sib.swissprot.sjh.elements.Element;
import swiss.sib.swissprot.sjh.elements.HTML;
import swiss.sib.swissprot.sjh.elements.Text;
import swiss.sib.swissprot.sjh.elements.contenttype.FlowContent;
import swiss.sib.swissprot.sjh.elements.embedded.Img;
import swiss.sib.swissprot.sjh.elements.grouping.DD;
import swiss.sib.swissprot.sjh.elements.grouping.DL;
import swiss.sib.swissprot.sjh.elements.grouping.DT;
import swiss.sib.swissprot.sjh.elements.grouping.Div;
import swiss.sib.swissprot.sjh.elements.grouping.DtOrDd;
import swiss.sib.swissprot.sjh.elements.grouping.LI;
import swiss.sib.swissprot.sjh.elements.grouping.OL;
import swiss.sib.swissprot.sjh.elements.grouping.P;
import swiss.sib.swissprot.sjh.elements.meta.Head;
import swiss.sib.swissprot.sjh.elements.meta.Link;
import swiss.sib.swissprot.sjh.elements.meta.Meta;
import swiss.sib.swissprot.sjh.elements.meta.Title;
import swiss.sib.swissprot.sjh.elements.sections.Body;
import swiss.sib.swissprot.sjh.elements.sections.Footer;
import swiss.sib.swissprot.sjh.elements.sections.Header;
import swiss.sib.swissprot.sjh.elements.sections.Section;
import swiss.sib.swissprot.sjh.elements.sections.header.H1;
import swiss.sib.swissprot.sjh.elements.sections.header.H2;
import swiss.sib.swissprot.sjh.elements.sections.header.H3;
import swiss.sib.swissprot.sjh.elements.text.A;
import swiss.sib.swissprot.sjh.elements.text.Span;
import swiss.sib.swissprot.sjh.elements.text.Sup;
import swiss.sib.swissprot.sjh.elements.text.Time;

/**
 * Hello world!
 *
 */
public class App {
	private static final String PREFACE_KEY = "preface";
	private static final Rel SCHEMA_PAGE_START = new Rel("schema:pageStart");
	private static final Rel SCHEMA_PAGE_END = new Rel("schema:pageEnd");
	private static final Clazz CUER_VOL_ACRONYM = clazz("CEURVOLACRONYM");
	private static final Clazz CUERTITLE = clazz("CEURTITLE");
	private static final Typeof ARTICLE_PROCEEDINGS = new Typeof("schema:Article bibo:Proceedings");
	private static final Typeof SCHOLARTLY_ARTICLE = new Typeof("schema:ScholarlyArticle");
	private static final RdfaLiteAttribute SHORT_TITLE = new Property("bibo:shortTitle schema:alternateName");
	private static final Property SCHEMA_NAME = new Property("schema:name");
	private static final Property SCHEMA_IN_LANGUAGE = new Property("schema:inLanguage");
	private static final Property BIBO_VOLUME = new Property("bibo:volume");
	private static final Property BIBO_URI = new Property("bibo:uri");
	private static final Rel SCHEMA_HAS_PART = new Rel("schema:hasPart");
	private static final Rel SCHEMA_AUTHOR = new Rel("schema:author");
	private static final Rel BIBO_EDITOR = new Rel("bibo:editor");
	private static final Property SCHEMA_EDITOR = new Property("schema:editor");
	private static final Property SCHEMA_DESCRIPTION = new Property("schema:description");
	private static final Rel OWL_SAME_AS = new Rel("owl:sameAs");
	private static final Clazz ORCID_CLAZZ = clazz("orcid");

	public static void main(String[] args) throws IOException {
		File inputDir = new File(args[0]);
		File outputDir = new File(args[1]);
		if (!outputDir.exists() && !outputDir.mkdir()) {
			System.err.println("Output directory does not exist and can't be made:" + outputDir.getAbsolutePath());
			System.exit(1);
		}
		String fullConferenceTitle = args[1];
		String shortConferenceTitle = args[2];
		String confurl = args[3];
		String city = args[4];
		String monthday = args[5];
		int year = Integer.parseInt(args[6]);
		File editors = new File(args[7]);
		String submittingEditor = args[8];
		new App().convert(inputDir, outputDir, fullConferenceTitle, shortConferenceTitle, confurl, city, monthday, year,
				editors, submittingEditor);
	}

	public int pageStart = 1;

	void convert(File inputDir, File outputDir, String fullConferenceTitle, String shortConferenceTitle, String confurl,
			String city, String monthDay, int year, File editors, String submittingEditor) throws IOException {

		Map<String, List<Submission>> grouped = groupPdfsPerDirectory(inputDir);

		for (Map.Entry<String, List<Submission>> en : grouped.entrySet()) {
			for (Submission sub : en.getValue()) {
				File paperPdf = new File(outputDir, "paper_" + sub.id() + ".pdf");
				Files.copy(sub.pdfFile.toPath(), paperPdf.toPath());
			}
		}
		Submission preface = null;
		List<Submission> remove = grouped.remove(PREFACE_KEY);
		if (remove != null && remove.size() == 1) {
			preface = remove.getFirst();
		}

		List<Section> mainList = grouped.entrySet().stream().map(this::makeSection).toList();
		Head head = pageHead(fullConferenceTitle);

		Stream<FlowContent> tocHheader = of(h2(new Text("Table of Contents")), text("\n"));
		Stream<FlowContent> articles = Stream.concat(Stream.of(preface(preface)), mainList.stream());

		Section toc = section(id("table-of-contents"), clazz("CEURTOC"), Stream.concat(tocHheader, articles));
		Div content = div(id("content"), toc);
		H1 acronymTitle = h1(a(href(confurl), span(CUER_VOL_ACRONYM, text(shortConferenceTitle))), br(),
				span(clazz("CEURVOLTITLE"), fullConferenceTitle));

//		<span class="CEURFULLTITLE">Proceedings of the Workshop on Publishing Papers with CEUR-WS</span><br>
//		<pre># delete one of the following two CEURCOLOCATED lines:</pre>
//		<!-- co-located with <span class="CEURCOLOCATED">NONE</span> -->
//		co-located with 10th International Conference on Online Publishing (<span class="CEURCOLOCATED">OPub YYYY</span>)<br>
//		</h2>
		H2 fullTitle = h2(span(clazz("CEURFULLTITLE"), "Proceedings of the " + fullConferenceTitle),
				span(clazz("CEURCOLOCATED"), "NONE"));
		H2 ceurloctime = h2(span(clazz("CEURLOCTIME"), city + ", " + monthDay + ", " + year));

		var editorsElement = editors(preface, editors);

		Footer footer = footerSection(submittingEditor);
		Stream<Element> pc = of(headerSection(),
				Elements.main(article(acronymTitle, fullTitle, ceurloctime, editorsElement, content)), footer);

//				property="schema:dateCreated">

		Body body = new Body(empty(), pc);

		File index = new File(outputDir, "index.html");
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(index))) {
			new HTML(of(lang("en")), head, body).render(out);
		}
	}

	private Map<String, List<Submission>> groupPdfsPerDirectory(File inputDir) throws IOException {
		Comparator<String> papersFirst = new CompareLongShortDemoOther();
		Map<String, List<Submission>> groupedPdfs = new TreeMap<>(papersFirst);
		Map<String, List<Submission>> groupedSubmissions = new TreeMap<>(papersFirst);
		for (File f : inputDir.listFiles()) {
			if (f.isFile() && "preface.pdf".equals(f.getName())) {

				Submission pre = extract(f);
				pre.id = 1;
				groupedSubmissions.put(PREFACE_KEY, List.of(pre));
			} else if (f.isDirectory()) {
				for (File f1 : f.listFiles()) {
					if (f1.getName().endsWith(".pdf")) {
						List<Submission> pdfsPerType = groupedPdfs.computeIfAbsent(f.getName(),
								(s) -> new ArrayList<>());
						pdfsPerType.add(extract(f1));
					}
				}
			}
		}
		int id = 2;
		for (var en : groupedPdfs.entrySet()) {
			en.getValue().sort((a, b) -> a.title().compareTo(b.title()));
			for (var sub : en.getValue()) {
				sub.id = id++;
			}
			groupedSubmissions.put(en.getKey(), en.getValue());
		}
		return groupedSubmissions;
	}

	private Submission extract(File f) throws IOException {
		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(f))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			return new Submission(pdfData, document.getNumberOfPages(), f);
		}
	}

	private Footer footerSection(String submittingEditor) {
		LocalDateTime now = LocalDateTime.now();
		String nows = DateTimeFormatter.ISO_LOCAL_DATE.format(now);
		int year = now.getYear();
		Time pubtime = time(clazz("CEURPUBDATE"), new DateTime(nows), text(year + "-MM-DD"));
		Time subtime = time(clazz("CEURSUBDATE"), new DateTime(now), text(nows));
		P p2 = p(pubtime, text(": published on CEUR Workshop Proceedings (CEUR-WS.org, ISSN 1613-0073) |"),
				a(href("https://validator.w3.org/nu/?doc=http%3A%2F%2Fceur-ws.org%2FVol-XXX%2F"), text("valid HTML5"),
						text("|")));

		P p1 = p(subtime,
				text(": submitted by " + submittingEditor + ", metadata incl. bibliographic data published under "),
				a(href("https://creativecommons.org/publicdomain/zero/1.0/"), text("Creative Commons CC0")));
		Footer footer = footer(p1, p2);
		return footer;
	}

	private Head pageHead(String fullConferenceTitle) {
		String t = "CEUR-WS.org/Vol-XXX - " + fullConferenceTitle;
		Meta viewport = meta("viewport", "width=device-width, initial-scale=1.0", null);
		Link css = link(href("https://ceur-ws.org/ceur-ws.css"), new Rel("stylesheet"));
		Link csssemantic = link(href("https://ceur-ws.org/ceur-ws-semantic.css"), new Rel("stylesheet"));
		Head head = new Head(new Title(new Text(t)), Stream.of(new Comment("CEURVERSION=2020-07-09"), css, csssemantic,
				viewport, new Comment("CEURLANG=eng ")));
		return head;
	}

	private Div editors(Submission preface, File editors) throws IOException {
		Iterator<String> affiliations = Files.readAllLines(editors.toPath()).iterator();
		Iterator<Author> iter = preface.data.authors().iterator();
		List<DtOrDd> names = new ArrayList<>();
		DT editedBy = dt(text("Edited by"));
		names.add(editedBy);
		List<String> addresses = new ArrayList<>();
		List<LI> addressElements = new ArrayList<>();
		int authorIndex = 0;
		if (iter.hasNext()) {
			// skip header line
			iter.next();
			while (iter.hasNext()) {
				Author editor = iter.next();
				String name = editor.name();
				String orcid = editor.orcid();
				if (!affiliations.hasNext()) {
					System.err.println("Number of editors and affiliations does not correspond");
					System.exit(2);
				}
				String adress = affiliations.next();
				Span ns = span(clazz("CEURVOLEDITOR"), SCHEMA_EDITOR, name);

				if (!addresses.contains(adress)) {
					addresses.add(adress);
				}
				String addressIndex = Integer.toString(addresses.indexOf(adress) + 1);
				Sup sup = sup(a(href("#authors-org" + addressIndex), text(addressIndex)));
				RdfaAttribute about = new Resource("https://orcid.org/" + orcid);
				if (orcid.isBlank()) {
					about = new Resource("#editor-" + authorIndex);
				}
				DD authordd = new DD(of(id("author" + authorIndex++)), of(BIBO_EDITOR, about), of(ns, sup));
				names.add(authordd);
			}
		}
		for (int i = 0; i < addresses.size(); i++) {
			int index = i + 1;
			String address = addresses.get(i);
			addressElements
					.add(li(id("authors-org" + index), of(sup(text(Integer.toString(i + 1))), span(text(address)))));
		}

		DL authorsBlock = dl(id("author-name"), names.stream());
		Stream<FlowContent> concat = of(authorsBlock, ul(id("author-org"), addressElements.stream()));
		return div(id("authors"), concat);

	}

	private FlowContent preface(Submission preface) {

		A linkToPrefacePdf = a(href("preface.pdf"), new Text(PREFACE_KEY));
		LI li = new LI(of(id(PREFACE_KEY)), Stream.of(linkToPrefacePdf), new Value(Integer.toString(preface.id)));
		OL prefaceOl = new OL(Stream.empty(), hasPart(), Stream.of(li));
		// of(Datatype.RDF_HTML, SCHEMA_DESCRIPTION)
		return new Div(empty(), of(prefaceOl));
	}

	private Header headerSection() {

		Img logo = new Img(empty(), Attributes.alt("CEUR-WS"), Attributes.src("https://ceur-ws.org/CEUR-WS-logo.png"),
				null, null, null, Attributes.width("390"), Attributes.height("100"));
		Img ccbyLogo = new Img(empty(), Attributes.alt("CC BY"), Attributes.src("https://ceur-ws.org/cc-by_100x35.png"),
				null, null, null, Attributes.width("100"), Attributes.height("35"));
		A link = a(href("https://ceur-ws.org/Vol-XXX"), of(text("https://ceur-ws.org/Vol-XXX")), OWL_SAME_AS);
		DD cuervolnr = new DD(of(clazz("CEURVOLNR")), of(BIBO_VOLUME), of(text("Vol-XXX")));
		DD ceururn = new DD(of(clazz("CEURURN")), of(BIBO_URI), of(text("urn:nbn:de:0074-XXX-C")));
		Stream<DtOrDd> idLink = of(dt(text("Identifier")), dd(link), cuervolnr, dt(text("URN")), ceururn);
		DL docid = dl(id("document-identifier"), idLink);

		DD ceurlang = new DD(of(clazz("CEURLANG")), of(SCHEMA_IN_LANGUAGE), of(text("eng")));
		DL doclang = dl(id("document-language"), dt(text("language")), ceurlang);
		String year = Integer.toString(LocalDate.now().getYear());
		DL doclicense = dl(id("document-license"), dt(text("License")), dd(ccbyLogo, span(text("Copyright © "), span(
				clazz("CEURPUBYEAR"), text(year),
				text(" for the individual papers by the papers' authors. Copyright © "),
				span(clazz("CEURPUBYEAR"), text(year)),
				text(" for the volume as a collection by its editors. This volume and its papers are published under the Creative Commons License Attribution 4.0 International ")))));
		return header(address(a(href("https://ceur-ws.org/"), logo)), docid, doclang, doclicense);
	}

	private Section makeSection(Entry<String, List<Submission>> e) {

		Stream<LI> listItems = e.getValue().stream().map(this::convertSubmissionIntoListItem);
		OL mainList = new OL(empty(), hasPart(), listItems);
		String sectionId = "section-" + e.getKey().replace(" ", "_").replace(":", "").toLowerCase();
		Id id = id(sectionId);
		H3 h3 = new H3(empty(), of(new Text(e.getKey())));
		Section section = new Section(of(id, clazz("CEURSESSION")),
				Stream.concat(of(new Resource("#" + sectionId)), hasPart()), of(h3, text("\n"), mainList, text("\n")));
		return section;
	}

	private Stream<RdfaAttribute> hasPart() {
		return of(SCHEMA_HAS_PART);
	}

	private LI convertSubmissionIntoListItem(Submission sub) {
		Stream<RdfaAttribute> typeof = of(SCHOLARTLY_ARTICLE, new Resource("./paper_" + sub.id() + ".pdf"));

//			Stream<RdfaLiteAttribute> titleRdfa = of();
		String title = sub.title();
		while (title.endsWith(".")) {
			title = title.substring(0, title.length() - 2);
		}
		Span titleSpan = new Span(ga(CUERTITLE), of(SCHEMA_NAME), of(new Text(title)));

		String paperId = "paper_" + sub.id() + ".pdf";
		A paperTitle = new A(of(titleSpan), href(paperId), rel("schema:url"));
		PagesAndOrcids pages2 = pages(sub);
		FlowContent pages = pages2.content();

		DL authors = authors(sub.data.authors(), paperId);

		Stream<FlowContent> childeren = of(paperTitle, pages, authors);

		String listValue = Integer.toString(sub.id());
		Stream<GlobalAttribute> id = Stream.of(id("paper_" + listValue));
		LI article = new LI(id, typeof, childeren, new Value(listValue));
		return article;
	}

	private DL authors(List<Author> authors, String paperId) {
		List<DD> authorSections = new ArrayList<>();
		for (int i = 0; i < authors.size(); i++) {
			authorSections.add(authorSection(authors.get(i), paperId, i));
		}
		Stream<DD> as = authorSections.stream();
		return new DL(of(clazz("authors")), dt(text("Authors")), as);
	}

	private DD authorSection(Author a, String paperId, int authorIndex) {
		Clazz clazz = clazz("CEURAUTHOR");
		if (a.orcid() != null) {
			Stream<Element> linkContent = of(new Text(a.name()));
			return authorNameWithOrcid(clazz, a.orcid(), linkContent);
		}
		return authorWithoutOrcid(a, clazz, authorIndex, paperId);
	}

	private DD authorWithoutOrcid(Author a, Clazz clazz, int authorIndex, String paperId) {
		return dd(clazz,
				new Span(empty(), of(new Resource("#" + paperId + "-author-" + authorIndex)), of(new Text(a.name()))));
	}

	private DD authorNameWithOrcid(Clazz clazz, String orcid, Stream<Element> linkContent) {
		Href href = href("https://orcid.org/" + orcid);
//				About about = new About("https://orcid.org/" + orcid);

		Stream<GlobalAttribute> of = ga(ORCID_CLAZZ);
		A orcidLink = new A(of, linkContent, href, SCHEMA_AUTHOR);
		return dd(clazz, orcidLink);
	}

	private PagesAndOrcids pages(Submission sub) {
		Span start = new Span(empty(), of(SCHEMA_PAGE_START, Datatype.XSD_NON_NEGATIVE_INTEGER),
				of(new Text(Integer.toString(pageStart))));

		int numberOfPages = sub.pages;
		List<PdfDataExtractor.Author> authors = sub.data.authors();
		Span end = new Span(empty(), of(SCHEMA_PAGE_END, Datatype.XSD_NON_NEGATIVE_INTEGER),
				of(new Text(Integer.toString(pageStart + numberOfPages))));
		pageStart += numberOfPages + 1;
		Stream<? extends DtOrDd> pagesDtDD = of(dt(text("pages")),
				new DD(of(clazz("CEURPAGES")), of(start, new Text("-"), end)));

		DL pages = new DL(of(clazz("pages")), pagesDtDD);
		return new PagesAndOrcids(pages, authors);

	}

	record PagesAndOrcids(FlowContent content, List<PdfDataExtractor.Author> authors) {
	}

	private static class Submission {
		public Submission(PdfData data, int pages, File pdfFile) {
			this.data = data;
			this.pages = pages;
			this.pdfFile = pdfFile;
		}

		public int id() {
			return id;
		}

		public String title() {
			return data.title();
		}

		private int id;
		private PdfData data;
		private File pdfFile;
		private int pages;
	}
}
