package swiss.sib.swissprot;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(subcommands = { MakeIndex.class })
public class CEUR {
	public static void main(String... args) {
		int exitCode = new CommandLine(new CEUR()).execute(args);
		System.exit(exitCode);
	}
}
