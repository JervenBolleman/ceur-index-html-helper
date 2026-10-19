package swiss.sib.swissprot.orcid;

import static swiss.sib.swissprot.orcid.OrcidCheckResult.Status.FAIL;
import static swiss.sib.swissprot.orcid.OrcidCheckResult.Status.OK;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import swiss.sib.swissprot.PdfDataExtractor.Author;

public class OrcidChecker {
	private static final Logger log = LoggerFactory.getLogger(OrcidChecker.class);
	private static final Pattern ORCID = Pattern.compile("([0-9X]{4})-([0-9X]{4})-([0-9X]{4})-([0-9X]{4})");
	private final Map<String, OrcidData> cache = new HashMap<>();
	private final File cacheDir;

	public OrcidChecker(File cacheDir) throws FileNotFoundException, IOException {
		super();
		this.cacheDir = cacheDir;
		Predicate<String> orcidMatcher = ORCID.asMatchPredicate();
		long twoHoursAgo = Instant.now().minus(5L, ChronoField.MINUTE_OF_HOUR.getBaseUnit()).toEpochMilli();
		for (File file : cacheDir.listFiles((n) -> orcidMatcher.test(n.getName()))) {
			if (file.lastModified() < twoHoursAgo) {
				try (FileInputStream fis = new FileInputStream(file)) {
					OrcidData orcidData = parseOrcidData(fis);
					cache.put(file.getName(), orcidData);
				}
			}
		}
	}

	/**
	 * Fetches the turtle representation of an ORCID record.
	 */
	@FunctionalInterface
	public interface Fetcher {
		Response fetch(URI uri) throws IOException, InterruptedException;
	}

	public record Response(int statusCode, byte[] body) {
	}

	/**
	 * Uses java.net.http by default, the WebAssembly build replaces this with a
	 * request made by the browser.
	 */
	private static volatile Fetcher fetcher = OrcidChecker::fetchWithHttpClient;

	public static void setFetcher(Fetcher fetcher) {
		OrcidChecker.fetcher = fetcher;
	}

	private static Response fetchWithHttpClient(URI uri) throws IOException, InterruptedException {
		try (HttpClient hc = HttpClient.newHttpClient()) {
			HttpRequest r = HttpRequest.newBuilder().header("accept", "text/turtle")
					.setHeader("user-agent", "ceur-index-helper").uri(uri).build();
			HttpResponse<byte[]> i = hc.send(r, BodyHandlers.ofByteArray());
			return new Response(i.statusCode(), i.body());
		}
	}

	public Stream<OrcidCheckResult> check(Stream<Author> authors) {
		return authors.map(this::checkOrcid);
	}

	public OrcidCheckResult checkOne(Author author) {
		if (author.orcid() == null)
			return new OrcidCheckResult(OK, "no orcid");

		return checkOrcid(author);
	}

	private OrcidCheckResult checkOrcid(Author a) {
		if (a.orcid() != null && cache.containsKey(a.orcid())) {
			return validate(a, cache.get(a.orcid()));
		}
		if ("0000-0000-0000-0000".equals(a.orcid())) {
			return new OrcidCheckResult(FAIL, "All zero orcid is not a valid orcid");
		}
		URI uri;
		try {
			uri = new URI("https://pub.orcid.org/experimental_rdf_v1/" + a.orcid());
			try {
				Response i = fetcher.fetch(uri);
				if (i.statusCode() == 404) {
					return new OrcidCheckResult(FAIL, "orcid service replied 404 not found for " + a.orcid());
				} else if (i.statusCode() != 200) {
					return new OrcidCheckResult(FAIL, "orcid service did not reply");
				} else {
					OrcidData od = parseOrcidData(new ByteArrayInputStream(i.body()));
					cache.put(a.orcid(), od);
					try (OutputStream os = new FileOutputStream(new File(cacheDir, a.orcid()))) {
						os.write(i.body());
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					return validate(a, od);
				}
			} catch (InterruptedException e) {
				Thread.interrupted();
				return new OrcidCheckResult(FAIL, "orcid check interupted");
			} catch (IOException e) {
				log.error("IO issue with orcid {}", e.getMessage());
				return new OrcidCheckResult(FAIL, "orcid check io failure");
			}
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}

	}

	public record OtherName(String content) {
	}

	public record OrcidData(String prefferedPubName, String givenNames, String familyNames,
			List<OtherName> otherNames) {
		public OrcidData(String prefferedPubName) {
			this(prefferedPubName, null, null, List.of());
		}

		public String orcidName() {
			if (prefferedPubName != null) {
				return prefferedPubName;
			} else {
				return givenNames + " " + familyNames;
			}
		}
	}

	static OrcidCheckResult validate(Author a, OrcidData od) {

		if (od.orcidName().equals(a.name())) {
			return new OrcidCheckResult(OK);
		} else {
			return new OrcidCheckResult(FAIL, "ORCID record name:" + od.orcidName() + " pdf contained:" + a.name());
		}
	}

	static OrcidData parseOrcidData(InputStream body) throws IOException {
		StatementCollector m = new StatementCollector();
		Rio.createParser(RDFFormat.TURTLE).setRDFHandler(m).parse(body);
		Collection<Statement> statements = m.getStatements();
		List<String> prefferedNames = statements.stream().filter(s -> s.getPredicate().equals(FOAF.NAME))
				.map(Statement::getObject).map(Value::stringValue).toList();
		if (prefferedNames.size() > 1) {
			throw new IllegalArgumentException("ORCID record has more preferred names than expected."
					+ prefferedNames.stream().collect(Collectors.joining(" ")));
		}
		var givenNames = givenNames(statements);
		String givenName = givenNames.isEmpty() ? null : givenNames.getFirst();
		var familyNames = familyNames(statements);
		String familyName = familyNames.isEmpty() ? null : familyNames.getFirst();
		List<OtherName> otherNames = List.of();
		if (!prefferedNames.isEmpty()) {
			return new OrcidData(prefferedNames.getFirst());
		} else {
			return new OrcidData(null, givenName, familyName, otherNames);
		}
	}

	private static List<String> familyNames(Collection<Statement> statements) {
		return statements.stream().filter(s -> s.getPredicate().equals(FOAF.FAMILY_NAME)).map(Statement::getObject)
				.map(Value::stringValue).toList();
	}

	private static List<String> givenNames(Collection<Statement> statements) {
		return statements.stream().filter(s -> s.getPredicate().equals(FOAF.GIVEN_NAME)).map(Statement::getObject)
				.map(Value::stringValue).toList();
	}
}
