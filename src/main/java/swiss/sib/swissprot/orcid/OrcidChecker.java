package swiss.sib.swissprot.orcid;

import static swiss.sib.swissprot.orcid.OrcidCheckResult.Status.FAIL;
import static swiss.sib.swissprot.orcid.OrcidCheckResult.Status.OK;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import swiss.sib.swissprot.PdfDataExtractor.Author;

public class OrcidChecker {

	public Stream<OrcidCheckResult> check(Stream<Author> authors) {
		try (HttpClient hc = HttpClient.newHttpClient()) {
			return authors.map(a -> checkOrcid(a, hc));
		}

	}
	
	public OrcidCheckResult checkOne(Author author) {
		try (HttpClient hc = HttpClient.newHttpClient()) {
			return checkOrcid(author, hc);
		}
	}

	private Map<String, OrcidData> cache = new HashMap<>();
	
	private OrcidCheckResult checkOrcid(Author a, HttpClient hc) {
		if (a.orcid() != null && cache.containsKey(a.orcid())) {
			return validate(a, cache.get(a.orcid()));
		}
		URI uri;
		try {
			uri = new URI("https://pub.orcid.org/" + a.orcid());
			HttpRequest r = HttpRequest.newBuilder().header("accept", "text/turtle")
					.setHeader("user-agent", "ceur-index-helper").uri(uri).build();
			try {
				HttpResponse<InputStream> i = hc.send(r, BodyHandlers.ofInputStream());
				if (i.statusCode() != 200) {
					return new OrcidCheckResult(FAIL);
				} else {
					try (InputStream body = i.body()) {
						OrcidData od = parseOrcidData(body);
						cache.put(a.orcid(), od);
						return validate(a, od);
					}
				}
			} catch (InterruptedException e) {
				Thread.interrupted();
				return new OrcidCheckResult(FAIL);
			} catch (IOException e) {
				return new OrcidCheckResult(FAIL);
			}
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}

	}

	public record OrcidData(List<String> givenNames, List<String> familyNames) {
		public String orcidName() {
			return Stream.concat(givenNames.stream(), familyNames.stream())
					.collect(Collectors.joining(" "));
		}
	}
	
	static OrcidCheckResult validate(Author a, OrcidData od){

		
		if (od.orcidName().equals(a.name())) {
			return new OrcidCheckResult(OK);
		} else {
			return new OrcidCheckResult(FAIL, "Expected name:"+od.orcidName()+" pdf contained "+ a.name());
		}
	}

	static OrcidData parseOrcidData(InputStream body) throws IOException {
		StatementCollector m = new StatementCollector();
		Rio.createParser(RDFFormat.TURTLE).setRDFHandler(m).parse(body);
		Collection<Statement> statements = m.getStatements();
		List<String> givenNames = statements.stream()
				.filter(s -> s.getPredicate().equals(FOAF.GIVEN_NAME)).map(Statement::getObject)
				.map(Value::stringValue).toList();
		List<String> familyNames = statements.stream()
				.filter(s -> s.getPredicate().equals(FOAF.FAMILY_NAME)).map(Statement::getObject)
				.map(Value::stringValue).toList();
		OrcidData od = new OrcidData(givenNames, familyNames);
		return od;
	}

	public void check(Author editor) {
		// TODO Auto-generated method stub
		
	}
}
