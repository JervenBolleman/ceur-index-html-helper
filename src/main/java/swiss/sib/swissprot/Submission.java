package swiss.sib.swissprot;

import java.io.File;

import swiss.sib.swissprot.PdfDataExtractor.PdfData;

class Submission {
	public Submission(PdfData data, int pages, File pdfFile) {
		this.data = data;
		this.pages = pages;
		this.pdfFile = pdfFile;
	}

	public String originalFileName() {
		return pdfFile.getName();
	}

	public int id() {
		return id;
	}

	public String title() {
		return data.title();
	}

	public PdfData data() {
		return data;
	}

	private int id;
	private PdfData data;
	private File pdfFile;
	private int pages;

	public void setId(int id) {
		this.id = id;

	}

	public int pages() {
		return pages;
	}

	public File pdfFile() {
		return pdfFile;
	}
}