package swiss.sib.swissprot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgreementMatcher {
	private static final String AGREE_THAT_MY_OUR_CONTRIBUTION = "agree that my/our contribution:";
	private static final Logger log = LoggerFactory.getLogger(AgreementMatcher.class);

	public record AgreementForSubmission(Agreement agg, Submission sub) {
	}

	public record Agreement(File file, String title) {
	}

	public static Stream<AgreementForSubmission> match(File dirWithAgreements, List<Submission> subs) {
		try {
			Path tmp = Files.createTempDirectory("ceur-tesseract-temp");
			try {
				for (File agreement : dirWithAgreements.listFiles()) {
					if (agreement.getName().endsWith(".png") || agreement.getName().endsWith(".jpg")
							|| agreement.getName().endsWith(".jpeg")) {
						checkImage(agreement);
					} else if (agreement.getName().endsWith(".pdf")) {
						checkPdf(tmp, agreement);
					}
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

		return Stream.empty();
	}

	private static String checkImage(File imageFile) throws IOException, InterruptedException {
		File txt = new File(imageFile.getParentFile(), imageFile.getName() + ".txt");
		ProcessBuilder pb = new ProcessBuilder("tesseract", imageFile.getAbsolutePath(),
				imageFile.getAbsolutePath(), "-l", "eng");
		Process start = pb.start();
		start.waitFor();
		String string = Files.readString(txt.toPath());
		return string;
		
	}

	private static void checkPdf(Path tmp, File file) throws InterruptedException {
		try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file))) {
			String string = totext(tmp, file, document);
			int beforeTitleIdx = string.indexOf(AGREE_THAT_MY_OUR_CONTRIBUTION)
					+ AGREE_THAT_MY_OUR_CONTRIBUTION.length();
			int afterTitle = string.indexOf("authored by:");
			if (beforeTitleIdx > 0 && afterTitle > 0 && afterTitle < string.length()) {
				String pt = string.substring(beforeTitleIdx, afterTitle).replaceAll("\n", "").trim();
				System.err.println(pt);
			}
		} catch (IOException e) {
			// Can't check this PDF.
		}
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
