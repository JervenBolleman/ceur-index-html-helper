package swiss.sib.swissprot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;

import swiss.sib.swissprot.PdfDataExtractor.PdfData;

public record ProceedingsData(Submission preface, Map<String, List<Submission>> sections) {
	private static final String PREFACE_KEY = "preface";

	/**
	 * Looks for PDFs in the given input directory. Copies them to the output directory, in a 
	 * named and numbered way.
	 * @param inputDir
	 * @param outputDir
	 * @return a ProccedingsData object, containing the original and new locations of all pdfs.
	 * @throws IOException
	 */
	static ProceedingsData collectAndCopy(File inputDir, File outputDir) throws IOException {
		Map<String, List<Submission>> grouped = groupPdfsPerDirectory(inputDir);
		Submission preface = collect(outputDir, grouped);
		return new ProceedingsData(preface, grouped);
	}

	static ProceedingsData collect(File inputDir) throws IOException {
		Map<String, List<Submission>> grouped = groupPdfsPerDirectory(inputDir);
		Submission preface = collect(null, grouped);
		return new ProceedingsData(preface, grouped);
	}
	
	private static Submission collect(File outputDir, Map<String, List<Submission>> grouped) throws IOException {
		if (outputDir != null) {
			for (Map.Entry<String, List<Submission>> en : grouped.entrySet()) {
				for (Submission sub : en.getValue()) {
					File paperPdf = new File(outputDir, "paper_" + sub.id() + ".pdf");
					Files.copy(sub.pdfFile().toPath(), paperPdf.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
		Submission preface = null;
		List<Submission> remove = grouped.remove(PREFACE_KEY);
		if (remove != null && remove.size() == 1) {
			preface = remove.getFirst();
		}
		return preface;
	}

	private static Map<String, List<Submission>> groupPdfsPerDirectory(File inputDir) throws IOException {
		Comparator<String> papersFirst = new CompareLongShortDemoOther();
		Map<String, List<Submission>> groupedPdfs = new TreeMap<>(papersFirst);
		Map<String, List<Submission>> groupedSubmissions = new TreeMap<>(papersFirst);
		for (File f : inputDir.listFiles()) {
			if (f.isFile() && "preface.pdf".equals(f.getName())) {

				Submission pre = extract(f);
				pre.setId(1);
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
				sub.setId(id++);
			}
			groupedSubmissions.put(en.getKey(), en.getValue());
		}
		return groupedSubmissions;
	}

	private static Submission extract(File f) throws IOException {
		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(f))) {
			PdfData pdfData = PdfDataExtractor.extract(document);
			return new Submission(pdfData, document.getNumberOfPages(), f);
		}
	}
}
