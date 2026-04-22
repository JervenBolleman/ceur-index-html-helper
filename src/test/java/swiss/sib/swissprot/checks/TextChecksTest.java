package swiss.sib.swissprot.checks;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class TextChecksTest {
	String page1 = """
			 		Towards HealthDCAT-AP compliant provenance
			description of clinical trial data flows according to ICH E6
			Matthias Löbe1,∗ and Judith Wodke3
			1 Institute for Medical Informatics, Statistics and Epidemiology (IMISE), Leipzig University, Härtelstraße 16-18,
			04107 Leipzig, Germany
			2 Institute for Community Medicine, University Hospital Greifswald, Walther-Rathenau-Straße 48, 17475 Greifswald,
			Germany
			Abstract
			Many researchers consider the provenance of data sets to be very important when it comes to trust and
			reusability of medical data sets. While the topic is being intensively studied in academia, information on
			data origin is often lacking in practice. One reason for this is the lack of coordinated, easily applicable
			recommendations for guidelines to produce provenance statements that can be interpreted comparatively
			across data sets. The HealthDCAT-AP vocabulary was developed to describe data sets in national catalogs
			and enables simple as well as complex provenance statements using the W3C PROV model. The objective
			of this work is to utilize HealthDCAT-AP and PROV-O to trace lineage from a clinical trial data collection
			according to the ICH E6 guideline. For this, the data flows of an exemplary clinical trial were
			implemented. Since the adoption of Revision 3 of the ICH E6 guideline, data flow diagrams should be
			included as part of sponsors' data management plans. The results show that data flows can be generated
			and visualized straightforwardly with PROV. However, there is a lack of coordinated vocabularies for the
			types of actors, activities, and entities that occur in such data flows to make data flow diagrams easy to
			read across studies and to enable queries, for example, on the characteristics of data collection.
			Keywords
			provenance, EHDS, PROV-O, DCAT, HealthDCAT-AP 1
			1. Introduction and
			The provenance of a data set is generally understood to refer to statements about the origin of the
			data and data transformation processes. Provenance is often explicitly required, e.g., by the FAIR
			Data Principles [1]. The reason for this is that identical data sets in medical research must be
			interpreted differently. In contrast to clinical documentation, billing data deliberately contains gaps
			and simplifications in diagnoses. Estimated or self-reported observations of body weight are subject
			to understandable deviations when compared to measured weight.
			The European Health Data Space (EHDS) [2] is an initiative based on a European Union
			regulation with a binding legal framework for the establishment and networking of large national
			data catalogs, which enable interested researchers to search for and request data sets. All holders of
			medical data are required to enter all their data sets into the catalogs and to update the entries. The
			requested personal data sets are made available for analysis by the designated stakeholders in so-
			called Secure Processing Environments in a complicated and costly process and cannot be accessed
			directly. It is obvious that the success of the EHDS will depend largely on the quality of the data set
			descriptions.
			HealthDCAT-AP [3], a vocabulary based on DCAT-AP and W3C DCAT, was developed to
			describe the characteristics and contents of medical data sets. A vocabulary is currently being
			designed in the QUANTUM project [4] to label data quality. Both vocabularies contain explicit
			requirements for labeling provenance using the W3C PROV data model [5]. However, they do not
			specify the type and scope, which can lead to provenance descriptions that vary greatly in detail
			and are difficult to compare.
			1∗ Corresponding author.
			matthias.loebe@imise.uni-leipzig.de (M. Löbe)
			0000-0002-2344-0426 (M. Löbe), 0009-0009-9712-060X (J. Wodke)
			© {{YEAR}} Copyright for this paper by its authors. Use permitted under Creative Commons License Attribution 4.0 International (CC BY """;

	@Test
	void test() {
		List<Issue> check = TextChecks
				.check(List.of(page1.replace("{{YEAR}}", Integer.toString(LocalDateTime.now().getYear()))));
		assertEquals(1, check.size());
		assertTrue(check.getFirst().message().contains("AI"));
		check = TextChecks.check(List.of(page1.replace("{{YEAR}}", "2020")));
		assertEquals(2, check.size());
		assertTrue(check.getFirst().message().contains("Year"));
	}

	private String failingAiDel = """
						Oryzabase Knowledge Graph: A FAIR Linked Data
			Resource for Rice Genomics and Phenotypic Traits
			Pierre Larmande1,∗, Shoko Kawamoto2, Toshiaki Katayama3 and Yutaka Sato2
			1DIADE, IRD, Univ. Montpellier, France – pierre.larmande@ird.fr
			2NIG, Mishima, Shizuoka, JAPAN
			3DBCLS, Univ of TOKYO, Wakashiba, Kashiwa, JAPAN
			Abstract
			Rice (Oryza sativa) is both a major staple crop feeding more than half of the global population and a key model
			species for monocot plant biology. Over the past decades, extensive genetic, genomic, and phenotypic knowledge
			has been curated in specialized databases, among which Oryzabase represents one of the most comprehensive
			resources for rice genetics. Oryzabase contains curated information on genes, mutant lines, phenotypes, traits,
			wild accessions, and bibliographic references accumulated through more than twenty years of expert curation.
			However, like many legacy biological databases, much of this information remains difficult to integrate with
			other genomic resources due to heterogeneous formats, limited machine-actionability, and insufficient semantic
			interoperability.
			To address these challenges, we developed the Oryzabase RDF Knowledge Graph, a FAIR Linked Open
			Data (LOD) representation of Oryzabase that enables semantic integration, interoperability, and computational
			reuse of rice genetics knowledge. The knowledge graph models Oryzabase entities using W3C Semantic Web
			standards, including RDF for data representation and SPARQL for querying. Biological concepts such as genes,
			phenotypes, traits, and taxonomic entities are linked using community-supported ontologies and persistent
			identifiers, enabling consistent semantic annotation and integration with external plant science resources.
			The resulting knowledge graph transforms curated Oryzabase records into a machine-readable semantic
			network that allows complex queries across biological entities and relationships. For example, researchers can
			retrieve genes associated with specific phenotypic traits, explore mutant lines linked to developmental processes,
			or connect Oryzabase information with external genomic databases through shared ontological references. By
			adopting FAIR principles—making data Findable, Accessible, Interoperable, and Reusable—the Oryzabase RDF KG
			facilitates advanced data discovery and computational analyses.
			Beyond improving accessibility, this semantic representation provides a foundation for knowledge integration
			with other plant knowledge graphs, including resources such as AgroLD and other Linked Data initiatives in
			plant genomics. Such interoperability opens new opportunities for integrative analyses combining genotype,
			phenotype, and trait data across species and databases. Moreover, knowledge graph infrastructures provide a
			promising substrate for emerging AI approaches, including graph-based machine learning and large language
			model–assisted biological knowledge discovery.
			The Oryzabase RDF Knowledge Graph therefore represents an important step toward FAIR, interoperable,
			and AI-ready plant genomic data infrastructures, enabling richer exploration of rice genetics knowledge and
			supporting integrative crop science research.
			Keywords
			Rice, Knowledge Graph, RDF, Plant Genomics, Traits, Phenotypes,
			Declaration on Generative AI
			Generative AI tools were used to assist in manuscript drafting and editing. All scientific content,
			interpretations, and conclusions were verified and approved by the authors
			SWAT4HCLS 2026: The 17th International Conference on Semantic Web Applications and Tools for Health Care and Life Sciences,
			March 23-26, 2026, Amsterdam, The Netherlands
			∗Corresponding author.
			Envelope-Open pierre.larmande@ird.fr (P. Larmande); skawamot@nig.ac.jp (S. Kawamoto); ktym@dbcls.jp (T. Katayama);
			yusato@nig.ac.jp (Y. Sato)
			Orcid 0000-0002-2923-9790 (P. Larmande); 0000-0002-6404-3443 (S. Kawamoto); 0000-0003-2391-0384 (T. Katayama);
			0000-0002-6607-8519 (Y. Sato)
			© 2025 Copyright for this paper by its authors. Use permitted under Creative Commons License Attribution 4.0 International (CC BY 4.0).
						""";
	
	@Test
	void testFailingAIDeclaration() {
		List<Issue> check = TextChecks.check(List.of(failingAiDel));
		assertTrue(check.isEmpty());
	}
}
