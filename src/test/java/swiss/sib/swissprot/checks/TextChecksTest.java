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
		List<Issue> check = TextChecks.check(List.of(page1.replace("{{YEAR}}", Integer.toString(LocalDateTime.now().getYear()))));
		assertEquals(1, check.size());
		assertTrue(check.getFirst().message().contains("AI"));
		check = TextChecks.check(List.of(page1.replace("{{YEAR}}", "2020")));
		assertEquals(2, check.size());
		assertTrue(check.getFirst().message().contains("Year"));
	}

}
