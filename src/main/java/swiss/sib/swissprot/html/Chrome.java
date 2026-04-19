package swiss.sib.swissprot.html;

import static swiss.sib.swissprot.sjh.Attributes.clazz;
import static swiss.sib.swissprot.sjh.Attributes.href;
import static swiss.sib.swissprot.sjh.Elements.a;
import static swiss.sib.swissprot.sjh.Elements.footer;
import static swiss.sib.swissprot.sjh.Elements.link;
import static swiss.sib.swissprot.sjh.Elements.meta;
import static swiss.sib.swissprot.sjh.Elements.p;
import static swiss.sib.swissprot.sjh.Elements.text;
import static swiss.sib.swissprot.sjh.Elements.time;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import swiss.sib.swissprot.sjh.Comment;
import swiss.sib.swissprot.sjh.attributes.DateTime;
import swiss.sib.swissprot.sjh.attributes.content.Rel;
import swiss.sib.swissprot.sjh.elements.Text;
import swiss.sib.swissprot.sjh.elements.contenttype.MetaContent;
import swiss.sib.swissprot.sjh.elements.grouping.P;
import swiss.sib.swissprot.sjh.elements.meta.Head;
import swiss.sib.swissprot.sjh.elements.meta.Style;
import swiss.sib.swissprot.sjh.elements.meta.Title;
import swiss.sib.swissprot.sjh.elements.sections.Footer;
import swiss.sib.swissprot.sjh.elements.text.Time;

public class Chrome {

	private static final String ERR_CSS = """
			.failure {color:red}
			.failure::before{ content:' ❌'} 
			.warning {color:orange} 
			.warning::before{ content:' ⚠️'} 
			""";

	public static Head head(String fullConferenceTitle, boolean runChecks) {
		String t = "CEUR-WS.org/Vol-XXX - " + fullConferenceTitle;
		List<MetaContent> meta = new ArrayList<>();
		meta.add(new Comment("CEURVERSION=2020-07-09"));
		meta.add(meta("viewport", "width=device-width, initial-scale=1.0", null));
		meta.add(link(href("https://ceur-ws.org/ceur-ws.css"), new Rel("stylesheet")));
		meta.add(link(href("https://ceur-ws.org/ceur-ws-semantic.css"), new Rel("stylesheet")));
		meta.add(new Comment("CEURLANG=eng "));
		if (runChecks) {
			meta.add(new Style(new Text(ERR_CSS)));
		}

		return new Head(new Title(new Text(t)), meta.stream());
	}

	public static Footer footerSection(String submittingEditor) {
		LocalDateTime now = LocalDateTime.now();
		String nows = DateTimeFormatter.ISO_LOCAL_DATE.format(now);
		int year = now.getYear();
		Time pubtime = time(clazz("CEURPUBDATE"), new DateTime(nows), text(year + "-MM-DD"));
		Time subtime = time(clazz("CEURSUBDATE"), new DateTime(now), text(nows));
		P p2 = p(pubtime, text(": published on CEUR Workshop Proceedings (CEUR-WS.org, ISSN 1613-0073) |"),
				a(href("https://validator.w3.org/nu/?doc=http%3A%2F%2Fceur-ws.org%2FVol-XXX%2F"), text("valid HTML5"),
						text("|")));
	
		P p1 = p(subtime,
				text(": submitted by " + submittingEditor + ", metadata incl. bibliographic data published under "),
				a(href("https://creativecommons.org/publicdomain/zero/1.0/"), text("Creative Commons CC0")));
		Footer footer = footer(p1, p2);
		return footer;
	}

}
