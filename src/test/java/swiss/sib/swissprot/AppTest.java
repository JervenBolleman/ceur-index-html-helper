package swiss.sib.swissprot;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AppTest {
	@TempDir
	Path in;

	@TempDir
	Path out;

	@Test
	void createMini() throws IOException {

		App app = new App();

		Path shortDir = in.resolve("short");
		copy(PdfDataExtractorTest.LIBREOFFICE_PDF, shortDir);

		Path longDir = in.resolve("long");
		copy(PdfDataExtractorTest.LATEX_PDF, longDir);

		copy(PdfDataExtractorTest.LATEX_ORCIDS_PDF, in);
		Files.move(in.resolve(PdfDataExtractorTest.LATEX_ORCIDS_PDF), in.resolve("preface.pdf"));

		Path editors = in.resolve("editors.csv");
		List<CharSequence> ea = List.of("WESO Lab - University of Oviedo, Spain",
				"Barcelona Supercomputing Center (BSC), Barcelona, Spain",
				"SIB Swiss Institute of Bioinformatis, Switzerland",
				"ZB MED Information Centre for Life Sciences, Cologne, Germany", "TriplyDB, Amsterdam, Netherlands",
				"IQVIA, Kirschgartenstrasse 14, Basel, Switzerland", "Micelio BV, Ekeren, Belgium");
		Files.write(editors, ea, StandardCharsets.UTF_8);

		File editorsAffiliations = editors.toFile();
		app.convert(in.toFile(), out.toFile(),
				"SWAT4HCLS 2025: 16th International Conference on Semantic Web Applications and Tools for Health Care and Life Sciences 2025",
				"SWAT4HCLS2025", "https://www.swat4ls.org/workshops/barcelona2025", "Barcelona", "Feb 24-27", 2025,
				editorsAffiliations, "Jerven Bolleman");

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
