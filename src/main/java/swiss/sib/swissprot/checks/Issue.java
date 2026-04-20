package swiss.sib.swissprot.checks;

import static swiss.sib.swissprot.sjh.Attributes.clazz;

import swiss.sib.swissprot.sjh.attributes.global.Clazz;
import swiss.sib.swissprot.sjh.elements.text.Span;
import static swiss.sib.swissprot.sjh.Elements.text;
import static swiss.sib.swissprot.sjh.Elements.span;

public record Issue(Type t, String message) {
	public static enum Type{
		FAILURE,
		WARNING
	}
	private static final Clazz FAILURE = clazz("failure");
	private static final Clazz WARNING = clazz("warning");

	public Span render() {
		return switch (t) {
			case FAILURE ->	span(FAILURE, text(message));
			case WARNING -> span(WARNING, text(message));
		};
	}

}
