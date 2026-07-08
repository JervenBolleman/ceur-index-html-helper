package swiss.sib.swissprot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import swiss.sib.swissprot.PdfDataExtractor.PdfData;

public record ProceedingsData(Submission preface, Map<String, List<Submission>> sections) {

	private static final String PREFACE_KEY = "preface";
	private static final Logger log = LoggerFactory.getLogger(ProceedingsData.class);

	/**
	 * Looks for PDFs in the given input directory. Copies them to the output
	 * directory, in a named and numbered way.
	 * 
	 * @param inputDir
	 * @param outputDir
	 * @param git
	 * @return a ProccedingsData object, containing the original and new locations
	 *         of all pdfs.
	 * @throws IOException
	 */
	static ProceedingsData collectAndCopy(File inputDir, File outputDir, boolean git) throws IOException {
		Map<String, List<Submission>> grouped = groupPdfsPerDirectory(inputDir);
		Submission preface = collect(outputDir, grouped, git);
		return new ProceedingsData(preface, grouped);
	}

	static ProceedingsData collect(File inputDir) throws IOException {
		Map<String, List<Submission>> grouped = groupPdfsPerDirectory(inputDir);
		Submission preface = collect(null, grouped, false);
		return new ProceedingsData(preface, grouped);
	}

	private static Submission collect(File outputDir, Map<String, List<Submission>> grouped, boolean git)
			throws IOException {
		if (outputDir != null) {
			copy(outputDir, grouped, git);
		}
		Submission preface = null;
		List<Submission> remove = grouped.remove(PREFACE_KEY);
		if (remove != null && remove.size() == 1) {
			preface = remove.getFirst();
			File paperPdf = new File(outputDir, "preface.pdf");
			log.info("Copying preface to {}", paperPdf.toPath());
			Files.copy(preface.pdfFile().toPath(), paperPdf.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return preface;
	}

	private record Copy(Path source, Path target, int id, byte[] hash) {
		public void copy() throws IOException {
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void copy(File outputDir, Map<String, List<Submission>> grouped, boolean git) throws IOException {
		List<Copy> copies = new ArrayList<>();
		Map<String, byte[]> existingHashes = new TreeMap<>();
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			for (Map.Entry<String, List<Submission>> en : grouped.entrySet()) {
				for (Submission sub : en.getValue()) {
					File paperPdf = new File(outputDir, "paper_" + sub.id() + ".pdf");
					Path source = sub.pdfFile().toPath();
					sha256.update(Files.readAllBytes(source));
					byte[] hash = sha256.digest();
					sha256.reset();
					Copy cp = new Copy(source, paperPdf.toPath(), sub.id(), hash);
					copies.add(cp);
				}
			}

			if (git) {
				Files.find(outputDir.toPath(), 1, (p, b) -> {
					String fn = p.getFileName().toString();
					return fn.startsWith("paper_") && fn.endsWith(".pdf");
				}).forEach(p -> {
					try {
						sha256.update(Files.readAllBytes(p));
						byte[] hash = sha256.digest();
						sha256.reset();
//					String output = String.format("%032X", new BigInteger(1, hash));
						existingHashes.put(p.getFileName().toString(), hash);
//					Files.delete(p);
					} catch (IOException e) {
//					log.warn("Could not delete {}", p, e);
					}
				});
			}
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		copies.sort(Comparator.comparingInt(Copy::id).reversed());
		try {
			for (Copy copy : copies) {
				copy.copy();
				log.info("Copying {} to {}", copy.source, copy.target);
				addToGit(outputDir, git, existingHashes, copy);
			}
		} catch (GitAPIException e) {
			throw new IOException(e);
		}
	}

	private static void addToGit(File outputDir, boolean git, Map<String, byte[]> existingHashes, Copy copy)
			throws IOException, GitAPIException, NoFilepatternException {
		if (git) {
			Git repo = Git.open(outputDir);
			boolean moved = move(existingHashes, copy, repo);
			if (!moved) {
				var add = repo.add();
				add.addFilepattern(copy.target.getFileName().toString());
				add.call();
			}
		}
	}

	private static boolean move(Map<String, byte[]> existingHashes, Copy copy, Git repo)
			throws GitAPIException, NoFilepatternException {
		for (Map.Entry<String, byte[]> en : existingHashes.entrySet()) {
			if (MessageDigest.isEqual(en.getValue(), copy.hash)) {
				var rm = repo.rm();
				rm.addFilepattern(en.getKey());
				rm.call();
				var add = repo.add();
				add.addFilepattern(copy.target.getFileName().toString());
				add.call();
				return true;
			}
		}
		return false;
	}

	private static Map<String, List<Submission>> groupPdfsPerDirectory(File inputDir) throws IOException {
		Comparator<String> papersFirst = new CompareLongShortDemoOther();
		Map<String, List<Submission>> groupedPdfs = new TreeMap<>(papersFirst);
		Map<String, List<Submission>> groupedSubmissions = new TreeMap<>(papersFirst);
		for (File f : inputDir.listFiles()) {
			if (f.isFile() && "preface.pdf".equals(f.getName())) {
				log.info("Found preface.pdf");
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
