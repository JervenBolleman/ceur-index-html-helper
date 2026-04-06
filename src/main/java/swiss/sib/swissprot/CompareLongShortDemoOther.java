package swiss.sib.swissprot;

import java.util.Comparator;

public final class CompareLongShortDemoOther implements Comparator<String> {
	@Override
	public int compare(String o1, String o2) {
		if ("Long paper".equalsIgnoreCase(o1) && "Long paper".equalsIgnoreCase(o2))
			return 0;
		else if ("Long paper".equalsIgnoreCase(o1))
			return -1;
		else if ("Long paper".equalsIgnoreCase(o2))
			return 1;
		else if ("Short paper".equalsIgnoreCase(o1) && "Short paper".equalsIgnoreCase(o2))
			return 0;
		else if ("Short paper".equalsIgnoreCase(o1))
			return -1;
		else if ("Short paper".equalsIgnoreCase(o2))
			return 1;
		return o1.compareTo(o2);
	}
}