package swiss.sib.swissprot;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import swiss.sib.swissprot.oxfabsceur.OxfordAbstractsDownloader;

@Command(subcommands = { MakeIndex.class, AgreementMatcher.class, OxfordAbstractsDownloader.class })
public class CEUR {
	public static void main(String... args) {
		int exitCode = new CommandLine(new CEUR()).execute(args);
		System.exit(exitCode);
	}
}
