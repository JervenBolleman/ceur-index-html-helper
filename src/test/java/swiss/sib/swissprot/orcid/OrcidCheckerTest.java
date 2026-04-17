package swiss.sib.swissprot.orcid;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import swiss.sib.swissprot.PdfDataExtractor.Author;

class OrcidCheckerTest {
	private String jerven = """
			@prefix foaf: <http://xmlns.com/foaf/0.1/> .
			@prefix gn:   <http://www.geonames.org/ontology#> .
			@prefix owl:  <http://www.w3.org/2002/07/owl#> .
			@prefix pav:  <http://purl.org/pav/> .
			@prefix prov: <http://www.w3.org/ns/prov#> .
			@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
			@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
			@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .
			
			<https://pub.orcid.org/orcid-pub-web/experimental_rdf_v1/0000-0002-7449-1266>
			        rdf:type              foaf:PersonalProfileDocument ;
			        pav:createdBy         <https://orcid.org/0000-0002-7449-1266> ;
			        pav:createdOn         "2018-09-13T14:59:16.352Z"^^xsd:dateTime ;
			        pav:createdWith       <https://orcid.org> ;
			        pav:lastUpdateOn      "2026-04-09T14:23:31.660Z"^^xsd:dateTime ;
			        prov:generatedAtTime  "2026-04-09T14:23:31.660Z"^^xsd:dateTime ;
			        prov:wasAttributedTo  <https://orcid.org/0000-0002-7449-1266> ;
			        foaf:maker            <https://orcid.org/0000-0002-7449-1266> ;
			        foaf:primaryTopic     <https://orcid.org/0000-0002-7449-1266> .
			
			<http://sws.geonames.org/2658434/>
			        rdfs:label      "Switzerland" , "Swiss Confederation" ;
			        gn:countryCode  "CH" ;
			        gn:name         "Swiss Confederation" , "Switzerland" .
			
			<https://orcid.org/0000-0002-7449-1266#workspace-works>
			        rdf:type  foaf:Document .
			
			<https://orcid.org/0000-0002-7449-1266#orcid-id>
			        rdf:type                     foaf:OnlineAccount ;
			        rdfs:label                   "0000-0002-7449-1266" ;
			        foaf:accountName             "0000-0002-7449-1266" ;
			        foaf:accountServiceHomepage  <https://orcid.org> .
			
			<https://orcid.org/0000-0002-7449-1266>
			        rdf:type           prov:Person , foaf:Person ;
			        rdfs:label         "Jerven Bolleman" ;
			        foaf:account       <https://orcid.org/0000-0002-7449-1266#orcid-id> ;
			        foaf:based_near    [ rdf:type          gn:Feature ;
			                             gn:countryCode    "CH" ;
			                             gn:parentCountry  <http://sws.geonames.org/2658434/>
			                           ] ;
			        foaf:familyName    "Bolleman" ;
			        foaf:givenName     "Jerven" ;
			        foaf:page          <https://akademienl.social/@jerven> ;
			        foaf:publications  <https://orcid.org/0000-0002-7449-1266#workspace-works> .
			""";
	@Test
	void test() {
		try(InputStream in=new ByteArrayInputStream(jerven.getBytes(StandardCharsets.UTF_8))){
			Author author = new Author("Jerven Bolleman");
			OrcidCheckResult validate = OrcidChecker.validate(author, OrcidChecker.parseOrcidData(in));
			assertTrue(validate.isOk());
		} catch (IOException e) {
			fail(e);
		}
	}

}
