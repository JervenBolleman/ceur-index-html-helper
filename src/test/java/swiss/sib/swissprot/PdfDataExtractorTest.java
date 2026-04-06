package swiss.sib.swissprot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import swiss.sib.swissprot.PdfDataExtractor.Author;
import swiss.sib.swissprot.PdfDataExtractor.PdfData;

class PdfDataExtractorTest {

	static final String LIBREOFFICE_PDF = "libreoffice.pdf";
	static final String LATEX_PDF = "latex.pdf";
	static final String LATEX_ORCIDS_PDF = "latex-oricds.pdf";
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
}
