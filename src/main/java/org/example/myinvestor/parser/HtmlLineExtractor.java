package org.example.myinvestor.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback used only when an email has no text/plain part. Walks each table row's
 * direct td/th children so the resulting lines have the same shape LineNormalizer
 * produces from the plain-text version: one tab-joined line per row of non-empty cells.
 */
public class HtmlLineExtractor {

    public static List<String> extract(String html) {
        List<String> lines = new ArrayList<>();
        if (html == null || html.isBlank()) return lines;
        Document doc = Jsoup.parse(html);
        for (Element row : doc.select("tr")) {
            List<String> cells = new ArrayList<>();
            for (Element cell : row.children()) {
                String tag = cell.tagName();
                if (tag.equalsIgnoreCase("td") || tag.equalsIgnoreCase("th")) {
                    String text = cell.text().trim();
                    if (!text.isEmpty()) {
                        cells.add(text);
                    }
                }
            }
            if (!cells.isEmpty()) {
                lines.add(String.join("\t", cells));
            }
        }
        return lines;
    }
}
