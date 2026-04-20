package swiss.sib.swissprot.orcid;

import static swiss.sib.swissprot.orcid.OrcidCheckResult.Status.FAIL;
import static swiss.sib.swissprot.orcid.OrcidCheckResult.Status.OK;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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
		for (File file : cacheDir.listFiles((n) -> orcidMatcher.test(n.getName()))) {
			try (FileInputStream fis = new FileInputStream(file)) {
				OrcidData orcidData = parseOrcidData(fis);
				cache.put(file.getName(), orcidData);
			}
		}
	}

	public Stream<OrcidCheckResult> check(Stream<Author> authors) {
		try (HttpClient hc = HttpClient.newHttpClient()) {
			return authors.map(a -> checkOrcid(a, hc));
		}
	}

	public OrcidCheckResult checkOne(Author author) {
		if (author.orcid() == null)
			return new OrcidCheckResult(FAIL, "no orcid");

		try (HttpClient hc = HttpClient.newHttpClient()) {
			OrcidCheckResult co = checkOrcid(author, hc);

			return co;
		}
	}

	private OrcidCheckResult checkOrcid(Author a, HttpClient hc) {
		if (a.orcid() != null && cache.containsKey(a.orcid())) {
			return validate(a, cache.get(a.orcid()));
		}
		if ("0000-0000-0000-0000".equals(a.orcid())) {
			return new OrcidCheckResult(FAIL, "All zero orcid is not a valid orcid");
		}
		URI uri;
		try {
			uri = new URI("https://pub.orcid.org/experimental_rdf_v1/" + a.orcid());
			HttpRequest r = HttpRequest.newBuilder().header("accept", "text/turtle")
					.setHeader("user-agent", "ceur-index-helper").uri(uri).build();
			try {
				HttpResponse<InputStream> i = hc.send(r, BodyHandlers.ofInputStream());
				if (i.statusCode() == 404) {
					return new OrcidCheckResult(FAIL, "orcid service replied 404 not found for " + a.orcid());
				} else if (i.statusCode() != 200) {
					return new OrcidCheckResult(FAIL, "orcid service did not reply");
				} else {
					try (InputStream body = i.body()) {
						ByteArrayOutputStream copy = new ByteArrayOutputStream();
						body.transferTo(copy);
						ByteArrayInputStream body2 = new ByteArrayInputStream(copy.toByteArray());
						OrcidData od = parseOrcidData(body2);
						cache.put(a.orcid(), od);
						try (ByteArrayInputStream body3 = new ByteArrayInputStream(copy.toByteArray());
								OutputStream os = new FileOutputStream(new File(cacheDir, a.orcid()))) {
							body3.transferTo(os);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						return validate(a, od);
					}
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

	public record OrcidData(String prefferedPubName, List<String> givenNames, List<String> familyNames) {
		public OrcidData(String prefferedPubName) {
			this(prefferedPubName, List.of(),List.of());
		}

		public String orcidName() {
			if (prefferedPubName != null) {
				return prefferedPubName;
			} else {
				return Stream.concat(givenNames.stream(), familyNames.stream()).collect(Collectors.joining(" "));
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
		if (prefferedNames.size() > 1){
			throw new IllegalArgumentException("ORCID record has more preferred names than expected."+prefferedNames.stream().collect(Collectors.joining(" ")));
		}
		List<String> givenNames = statements.stream().filter(s -> s.getPredicate().equals(FOAF.GIVEN_NAME))
				.map(Statement::getObject).map(Value::stringValue).toList();
		List<String> familyNames = statements.stream().filter(s -> s.getPredicate().equals(FOAF.FAMILY_NAME))
				.map(Statement::getObject).map(Value::stringValue).toList();
		if (! prefferedNames.isEmpty()) {
			return new OrcidData(prefferedNames.getFirst());
		} else {
			return new OrcidData(null, givenNames, familyNames);
		}
	}
}
