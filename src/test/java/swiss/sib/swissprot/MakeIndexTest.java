package swiss.sib.swissprot;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MakeIndexTest {
	@TempDir
	Path in;

	@TempDir
	Path out;

	@Test
	void createMini() throws IOException {

		MakeIndex app = new MakeIndex();

		Path shortDir = in.resolve("short");
		copy(PdfDataExtractorTest.LIBREOFFICE_PDF, shortDir);

		Path longDir = in.resolve("long");
		copy(PdfDataExtractorTest.LATEX_PDF, longDir);

		copy(PdfDataExtractorTest.LATEX_ORCIDS_PDF, in);
		Files.move(in.resolve(PdfDataExtractorTest.LATEX_ORCIDS_PDF), in.resolve("preface.pdf"));

		app.inputDir = in.toFile();
		app.outputDir = out.toFile();
		app.fullConferenceTitle = "SWAT4HCLS 2025: 16th International Conference on Semantic Web Applications and Tools for Health Care and Life Sciences 2025";
		app.confurl = "https://www.swat4ls.org/workshops/barcelona2025";
		app.city = "Barcelona";
		app.period ="Feb 24-27";
		app.year = 2025;
		app.shortConferenceTitle="SWAT4HCLS2025";
		app.submittingEditor ="Jerven Bolleman";
		app.convert();

		assertTrue(Files.exists(out.resolve("index.html")));
		assertNotEquals(0, Files.size(out.resolve("index.html")));
		assertTrue(Files.exists(out.resolve("paper_1.pdf")));
		assertTrue(Files.exists(out.resolve("paper_2.pdf")));
		assertTrue(Files.exists(out.resolve("paper_3.pdf")));
	}

	private Path copy(String filename, Path temp) throws IOException {
		if (!Files.exists(temp))
			Files.createDirectory(temp);
		Path file = Files.createFile(temp.resolve(filename));
		try (var in = PdfDataExtractorTest.class.getClassLoader().getResourceAsStream(filename)) {
			Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
		}
		return file;
	}
	
	
}
