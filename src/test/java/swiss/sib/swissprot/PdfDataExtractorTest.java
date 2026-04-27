package swiss.sib.swissprot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import swiss.sib.swissprot.PdfDataExtractor.Author;
import swiss.sib.swissprot.PdfDataExtractor.PdfData;
import swiss.sib.swissprot.checks.Issue;

class PdfDataExtractorTest {

	static final String LIBREOFFICE_PDF = "libreoffice.pdf";
	static final String LATEX_PDF = "latex.pdf";
	static final String LATEX_ORCIDS_PDF = "latex-oricds.pdf";
	static final String MISSING = "missing_one_orcid.pdf";
	static final String NBSP = "non_breaking_space_in_name.pdf";
	static final String ONE_ORCID_TO_MUCH = "extract_two_names_from_libreoffice_writer.pdf";
	static final String STRANGE_ORCID_EXTRACT_FAILURE="paper_12.pdf";
	
	@TempDir
	Path temp;

	@Test
	void libreoffice() throws IOException {
		Path file = copy(LIBREOFFICE_PDF);

		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			assertNotNull(pdfData);
			assertEquals(11, pdfData.authors().size());
			Author first = pdfData.authors().get(0);
			assertEquals("Joshua Valdez", first.name());
			Author second = pdfData.authors().get(1);
			assertEquals("Philippe Rocca-Serra", second.name());
			assertEquals("0000-0001-9853-5668", second.orcid());
			Author last = pdfData.authors().getLast();
			assertEquals("Ben Gardner", last.name());
			assertNull(last.orcid());
		}
	}
	
	@Test
	void libreoffice2() throws IOException {
		Path file = copy(ONE_ORCID_TO_MUCH);

		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			assertNotNull(pdfData);
			assertEquals(2, pdfData.authors().size());
			Author first = pdfData.authors().get(0);
			assertEquals("Walter Baccinelli", first.name());
			Author second = pdfData.authors().get(1);
			assertEquals("Vedran Kasalica", second.name());
			assertEquals("0000-0002-0097-1056", second.orcid());
			List<Issue> failures = pdfData.failures();
			assertFalse(failures.isEmpty());
			assertTrue(failures.getFirst().message().contains("template"));
		}
	}

	private Path copy(String filename) throws IOException {
		Path file = Files.createFile(temp.resolve(filename));
		try (var in = PdfDataExtractorTest.class.getClassLoader().getResourceAsStream(filename)) {
			Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
		}
		return file;
	}

	@Test
	void latex() throws IOException {
		Path file = copy(LATEX_PDF);

		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			assertNotNull(pdfData);
			assertEquals(pdfData.title(), "The Wheat and Rice Genomics Scientific Literature Knowledge Graphs");
			assertEquals(8, pdfData.authors().size());
			Author first = pdfData.authors().get(0);
			assertEquals("Nadia Yacoubi Ayadi", first.name());
			assertNull(first.orcid());

			Author last = pdfData.authors().getLast();
			assertEquals("Catherine Faron", last.name());
			assertNull(last.orcid());
		}
	}

	@Test
	void latexOrcid() throws IOException {
		Path file = copy(LATEX_ORCIDS_PDF);

		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			assertNotNull(pdfData);
			Author first = pdfData.authors().get(0);
			assertEquals("Jose Emilio Labra Gayo", first.name());
			assertEquals("0000-0001-8907-5348", first.orcid());

			Author second = pdfData.authors().get(1);
			assertEquals("Alberto Labarga", second.name());
			assertEquals("0000-0001-6781-893X", second.orcid());

			Author last = pdfData.authors().getLast();
			assertEquals("Andra Waagmeester", last.name());
			assertEquals("0000-0001-9773-4008", last.orcid());
		}
	}

	@Test
	void missing() throws IOException {
		Path file = copy(MISSING);

		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			assertNotNull(pdfData);
			Author first = pdfData.authors().get(4);
			assertEquals("Mark D. Wilkinson", first.name());
			assertEquals("0000-0001-6960-357X", first.orcid());
		}
	}
	
	@Disabled(value = "Not running because there are real errors in this PDF")
	@Test
	void kalt() throws IOException {
		Path file = copy(STRANGE_ORCID_EXTRACT_FAILURE);

		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			assertNotNull(pdfData);
			assertEquals(1, pdfData.failures().size());
			
		}
	}
	
	@Test
	void strangeCharacters() throws IOException {
		Path file = copy("paper_33.pdf");

		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			assertNotNull(pdfData);
			assertEquals(1, pdfData.failures().size());
			
		}
	}

	@Test
	void nbsp() throws IOException {
		Path file = copy(NBSP);

		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file.toFile()))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			assertNotNull(pdfData);
			Author first = pdfData.authors().get(1);
			assertEquals("Jelmer M. van Lieshout", first.name());
			assertEquals("0009-0008-2500-4717", first.orcid());
		}
	}

	String page1 = """
						A Standards‑Based Knowledge Graph that Bridges
			Scientific Workflows, Run‑Time Provenance, and
			Tool Registries
			Marie Schmit1, Ulysse Le Clanche2, George Marchment3, Sarah Cohen-Boulakia3,
			Olivier Dameron2, Alban Gaignard4,5, Frédéric Lemoine1 and Hervé Ménager1,5
			1Institut Pasteur, Université Paris Cité, Bioinformatics of Biostatistics Hub, F-75015 Paris, France
			2Université Rennes, Inria, CNRS, IRISA—UMR 6074, Rennes 35000, France
			3Université Paris-Saclay, CNRS, Laboratoire Interdisciplinaire des Sciences du Numérique, 91405, Orsay, France
			4Nantes Université, CNRS, INSERM, l’institut du thorax, F-44000 Nantes, France
			5IFB-core, Institut Français de Bioinformatique (IFB), CNRS, INSERM, INRAE, CEA, 94800 Villejuif, France
			Abstract
			Life science workflows are now prevalent for implementing, executing, and sharing complex data analy-
			ses, increasing their scalability and reproducibility. Adhering to the FAIR principles for software further
			reinforces their reproducibility and the reliability of their results. To maximize their FAIRness, consis-
			tent and standardised annotations are critical across several levels: workflows, individual steps, software
			tools, and input/output data. Such comprehensive metadata make workflows easier to understand, reuse
			and reproduce, while keeping track of the provenance of their results. However, a unified, queryable
			knowledge framework that integrates workflows with enriched metadata is lacking. To address this,
			we developed an integrated workflow knowledge base, that consolidates FAIR metadata from diverse
			sources and workflow languages into a standardised graph-based representation. It leverages estab-
			lished ontologies and standards (e.g. EDAM, schema.org) to enrich metadata, and link the workflow
			structure with its execution traces. Our approach provides FAIR-compliant metadata of publicly avail-
			able pipelines, enabling queries at every granularity level, while accounting for the quality of source
			data annotation.
			Keywords
			Knowledge graphs, Ontologies, FAIR, Workflows, SPARQL,
			SWAT4HCLS 2026
			£ marie.schmit@pasteur.fr (M. Schmit); herve.menager@pasteur.fr (H. Ménager)
			Ȉ 0009-0007-8119-1222 (M. Schmit); 0000-0002-4565-3940 (G. Marchment); 0000-0002-7439-1441
			(S. Cohen-Boulakia); 0000-0001-8959-7189 (O. Dameron); 0000-0002-3597-8557 (A. Gaignard);
			0000-0001-9576-4449 (F. Lemoine); 0000-0002-7552-1009 (H. Ménager)
			© 2026 Use permitted under Creative Commons License Attribution 4.0 International (CC BY 4.0).
			Comment
						""";

	@Test
	void namesWithAccents() {
		Pattern p = Pattern.compile("F[\\p{L}\\.]* ? Lemoine");
		Author flemoine = new Author("Frédéric Lemoine");
		assertTrue(p.asMatchPredicate().test(flemoine.name()));
		List<Author> authors = List.of(flemoine);
		ArrayList<Issue> issues = new ArrayList<>();
		PdfDataExtractor.findEmailsAndOrcids(List.of(page1), authors, issues);
		assertNotNull(authors.getFirst().orcid());
	}
	
//	@Test
//	void namesFromLibreOfficeWriter() {
//		Author walter = new Author("Walter Baccinelli");
//		Author vedran = new Author("Vedran Kasalica");
//		List<Author> authors = List.of(walter, vedran);
//		ArrayList<Issue> issues = new ArrayList<>();
//		PdfDataExtractor.findEmailsAndOrcids(List.of("Walter Baccinelli1,∗,†and Vedran Kasalica1,∗,†"), authors, issues);
//		assertEquals(2, authors.size());
//		assertNotNull(authors.getFirst().name());
//	}
	
	@Test
	void titleCaseDetector() {
		assertTrue(PdfDataExtractor.isSoft("Modular composition of SPARQL queries for focusing on what to look for rather than how to get it"));
		assertFalse(PdfDataExtractor.isSoft("KIK-V Indicator Explorer: Consistent, Reusable SPARQL for Health Indicators"));
		assertTrue(PdfDataExtractor.isSoft("Revisiting SIF abstraction rules with SPARQL for querying BioPAX"));
	}
}
