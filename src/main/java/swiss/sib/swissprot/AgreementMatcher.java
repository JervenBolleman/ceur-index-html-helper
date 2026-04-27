package swiss.sib.swissprot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import swiss.sib.swissprot.checks.Issue;

@Command(name = "agreement-matcher")
public class AgreementMatcher implements Callable<Integer> {
	private static final String AGREE_THAT_MY_OUR_CONTRIBUTION = "agree that my/our contribution:";
	private static final Logger log = LoggerFactory.getLogger(AgreementMatcher.class);

	public record AgreementForSubmission(Agreement agg, Submission sub, Optional<Issue> isText, String matching) {

		public Optional<String> submissionId() {
			if (sub == null)
				return Optional.empty();
			return Optional.of(Integer.toString(sub.id()));
		}
	}

	public record Agreement(File file, String title) {
	}

	@Option(names = "-i", description = "input directory containing a preface.pdf and directories for each section containing all the paper pdfs", required = true)
	public File inputDir;

	@Option(names = "-a", description = "agreements input directory containing a bunch of pdfs and other image files containing the scanned copyright assignements", required = true)
	public File agreementsDir;

	@Option(names = "-o", description = "ouput directory, that will contain a agreements-status.html as well as numbered agreements", required = true)
	public File outputDir;

	public static void main(String[] args) throws IOException {
		int exitCode = new CommandLine(new AgreementMatcher()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		ProceedingsData collect = ProceedingsData.collect(inputDir);
		List<Submission> submissions = collect.sections().entrySet().stream().map(Entry::getValue).flatMap(List::stream)
				.toList();
		match(agreementsDir, submissions).forEach(as -> {
			String m = as.submissionId().orElseGet(() -> "None");
			System.out.println(as.agg.title + " in " + as.agg().file().getName() + " matches " + m);
		});
		;
		return 0;
	}

	public static Stream<AgreementForSubmission> match(File dirWithAgreements, List<Submission> subs) {
		List<AgreementForSubmission> l = new ArrayList<>();
		try {
			Path tmp = Files.createTempDirectory("ceur-tesseract-temp");
			try {
				for (File agreement : dirWithAgreements.listFiles()) {
					extractTitleAndTryToMatch(subs, l, tmp, agreement);
				}
			} finally {
				Files.walk(tmp).forEach(f -> {
					try {
						Files.deleteIfExists(f);
					} catch (IOException e) {
						log.error(e.getMessage());
					}
				});
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.interrupted();
		}

		return l.stream();
	}

	private static void extractTitleAndTryToMatch(List<Submission> subs, List<AgreementForSubmission> l, Path tmp,
			File agreement) throws IOException, InterruptedException {
		Optional<String> title = Optional.empty();
		if (agreement.getName().endsWith(".png") || agreement.getName().endsWith(".jpg")
				|| agreement.getName().endsWith(".jpeg")) {
			title = checkImage(agreement);
		} else if (agreement.getName().endsWith(".pdf")) {
			title = checkPdf(tmp, agreement);
		}
		if (title.isPresent()) {
			String t = title.get();
			boolean found = findByExactTitleMatch(subs, l, agreement, t);
			if (!found) {
				found = findByExactFileNameMatch(subs, l, agreement, t);
			}
			if (!found) {
				guessByLevensteinDistance(subs, l, agreement, t);
			}
		}
	}

	private static boolean findByExactFileNameMatch(List<Submission> subs, List<AgreementForSubmission> l,
			File agreement, String t) {
		String name = agreement.getName();
		for (Submission sub : subs) {
			if (sub.originalFileName().equals(name)) {
				l.add(new AgreementForSubmission(new Agreement(agreement, t), sub, Optional.empty(), "FileName match"));
				return true;
			}
		}
		return false;

	}

	private static void guessByLevensteinDistance(List<Submission> subs, List<AgreementForSubmission> l, File agreement,
			String t) {
		int score = 0;
		Submission mostLikely = null;
		LevenshteinDistance ld = LevenshteinDistance.getDefaultInstance();
		for (Submission sub : subs) {
			if (sub.title().equals(t)) {
				Integer ldc = ld.apply(sub.title(), t);
				if (score < ldc) {
					mostLikely = sub;
				}
			}
		}
		if (mostLikely != null) {

			l.add(new AgreementForSubmission(new Agreement(agreement, t), mostLikely, Optional.empty(),
					"Text distance"));
		}
	}

	private static boolean findByExactTitleMatch(List<Submission> subs, List<AgreementForSubmission> l, File agreement,
			String t) {
		boolean found = false;
		for (Submission sub : subs) {
			if (sub.title().equals(t)) {
				l.add(new AgreementForSubmission(new Agreement(agreement, t), sub, Optional.empty(), "Exact title match"));
				found = true;
			}
		}
		return found;
	}

	private static Optional<String> checkImage(File imageFile) throws IOException, InterruptedException {
		File txt = new File(imageFile.getParentFile(), imageFile.getName() + ".txt");
		ProcessBuilder pb = new ProcessBuilder("tesseract", imageFile.getAbsolutePath(), imageFile.getAbsolutePath(),
				"-l", "eng");
		Process start = pb.start();
		start.waitFor();
		String string = Files.readString(txt.toPath());
		return findTitle(string);
	}

	private static Optional<String> checkPdf(Path tmp, File file) throws InterruptedException {
		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file))) {

			String string = totext(tmp, file, document);

			return findTitle(string);
		} catch (IOException e) {
			// Can't check this PDF.
		}
		return Optional.empty();
	}

	private static Optional<String> findTitle(String string) {
		int beforeTitleIdx = string.indexOf(AGREE_THAT_MY_OUR_CONTRIBUTION) + AGREE_THAT_MY_OUR_CONTRIBUTION.length();
		int afterTitle = string.indexOf("authored by:");
		if (beforeTitleIdx > 0 && afterTitle > 0 && afterTitle < string.length()) {
			String pt = string.substring(beforeTitleIdx, afterTitle).replaceAll("\n", "").trim();
			log.info(pt);
			return Optional.of(pt);
		}
		return Optional.empty();
	}

	private static String totext(Path tmp, File file, PDDocument document) throws IOException, InterruptedException {
		Path imageFile = convert(tmp, file, document);
		File txt = new File(imageFile.getParent().toFile(), imageFile.toFile().getName() + ".txt");
		ProcessBuilder pb = new ProcessBuilder("tesseract", imageFile.toFile().getAbsolutePath(),
				imageFile.toFile().getAbsolutePath(), "-l", "eng");
		Process start = pb.start();
		start.waitFor();
		String string = Files.readString(txt.toPath());
		return string;
	}

	private static Path convert(Path tmp, File file, PDDocument document) throws IOException {
		PDFRenderer pdfRenderer = new PDFRenderer(document);
		BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
		Path tf = Files.createTempFile(tmp, file.getName(), "png");
		try (var os = Files.newOutputStream(tf, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				ImageOutputStream ios = ImageIO.createImageOutputStream(os);) {
			ImageIO.write(bim, "png", ios);
		}
		return tf;
	}
}
