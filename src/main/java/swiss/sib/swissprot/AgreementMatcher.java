package swiss.sib.swissprot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
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
				for (File file : dirWithAgreements.listFiles()) {
					if (file.getName().endsWith(".pdf") || file.getName().endsWith(".png")
							|| file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg")) {
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
							//Can't check this PDF.
						}
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

	private static String totext(Path tmp, File file, PDDocument document) throws IOException, InterruptedException {
		Path tf = convert(tmp, file, document);
		File txt = new File(tf.getParent().toFile(), tf.toFile().getName() + ".txt");
		ProcessBuilder pb = new ProcessBuilder("tesseract", tf.toFile().getAbsolutePath(),
				tf.toFile().getAbsolutePath(), "-l", "eng");
		Process start = pb.start();
		start.waitFor();
		String string = Files.readString(txt.toPath());
		return string;
	}

	private static Path convert(Path tmp, File file, PDDocument document) throws IOException {
		PDFRenderer pdfRenderer = new PDFRenderer(document);
		BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
		Path tf = Files.createTempFile(tmp, file.getName(), "png");
		try (var os = Files.newOutputStream(tf, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			boolean img = ImageIOUtil.writeImage(bim, "png", os, 300);
		}
		return tf;
	}
}
