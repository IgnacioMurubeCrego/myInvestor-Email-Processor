package org.example.myinvestor.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * myInvestor's plain-text operation emails are an HTML table flattened to text: each
 * table row becomes a text line where cells are separated by runs of tabs (often with
 * stray spaces mixed in), and empty spacer cells produce lines that are pure whitespace.
 * This collapses each raw line down to its non-empty cells (tab-joined) and drops lines
 * that carried no real content, so a header row is always immediately followed by its
 * data row in the resulting list.
 */
public class LineNormalizer {

    public static List<String> normalize(String text) {
        List<String> result = new ArrayList<>();
        if (text == null) return result;
        for (String rawLine : text.split("\\r?\\n")) {
            String[] cells = rawLine.split("\\t+");
            List<String> nonEmpty = new ArrayList<>();
            for (String cell : cells) {
                String trimmed = cell.trim();
                if (!trimmed.isEmpty()) {
                    nonEmpty.add(trimmed);
                }
            }
            if (!nonEmpty.isEmpty()) {
                result.add(String.join("\t", nonEmpty));
            }
        }
        return result;
    }
}
