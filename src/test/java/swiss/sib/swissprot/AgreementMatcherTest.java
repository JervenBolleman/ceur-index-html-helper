package swiss.sib.swissprot;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AgreementMatcherTest {

	@Test
	@Disabled
	void test() {
		AgreementMatcher.match(new File("/home/jbollema/git/swat4hcls-proceedings/2026/agreements"), List.of());
	}

}
