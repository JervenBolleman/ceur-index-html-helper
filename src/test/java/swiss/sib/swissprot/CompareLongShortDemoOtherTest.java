package swiss.sib.swissprot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class CompareLongShortDemoOtherTest {

	@Test
	void test() {
		List<String> bad = List.of("Demonstration", "Poster paper", "Long paper", "Short paper");
		List<String> expected = List.of("Long paper", "Short paper", "Demonstration", "Poster paper");
		Iterator<String> unsorted = expected.iterator();
		Iterator<String> sorted = bad.stream().sorted(new CompareLongShortDemoOther()).iterator();
		
		assertTrue(unsorted.hasNext());
		assertTrue(sorted.hasNext());
		assertEquals(unsorted.next(), sorted.next());
		
		assertTrue(unsorted.hasNext());
		assertTrue(sorted.hasNext());
		assertEquals(unsorted.next(), sorted.next());
		
		assertTrue(unsorted.hasNext());
		assertTrue(sorted.hasNext());
		assertEquals(unsorted.next(), sorted.next());
		
		assertTrue(unsorted.hasNext());
		assertTrue(sorted.hasNext());
		assertEquals(unsorted.next(), sorted.next());
		
		assertFalse(unsorted.hasNext());
		assertFalse(sorted.hasNext());
	}

}
