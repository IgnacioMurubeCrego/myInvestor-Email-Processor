package org.example.myinvestor.mbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a raw .mbox file into the byte ranges of its individual RFC822 messages.
 * The file is decoded as ISO-8859-1 only to locate "From " separator lines: that
 * charset maps every byte to exactly one char, so offsets found in the decoded
 * string line up 1:1 with byte offsets in the original array, letting us slice out
 * each message's original bytes untouched for proper MIME/charset parsing later.
 */
public class MboxReader {

    private static final Pattern FROM_LINE = Pattern.compile(
            "(?m)^From \\S+ +\\S{3} \\S{3} +\\d{1,2} \\d{2}:\\d{2}:\\d{2}( [+-]\\d{4})? \\d{4}\\r?$");

    public List<byte[]> readMessages(Path mboxFile) throws IOException {
        byte[] data = Files.readAllBytes(mboxFile);
        String latin1 = new String(data, StandardCharsets.ISO_8859_1);
        Matcher m = FROM_LINE.matcher(latin1);

        List<int[]> boundaries = new ArrayList<>();
        while (m.find()) {
            int lineStart = m.start();
            int newlineIdx = latin1.indexOf('\n', m.end());
            int contentStart = (newlineIdx == -1) ? latin1.length() : newlineIdx + 1;
            boundaries.add(new int[]{lineStart, contentStart});
        }

        List<byte[]> messages = new ArrayList<>();
        for (int i = 0; i < boundaries.size(); i++) {
            int contentStart = boundaries.get(i)[1];
            int contentEnd = (i + 1 < boundaries.size()) ? boundaries.get(i + 1)[0] : data.length;
            if (contentEnd > contentStart) {
                byte[] raw = Arrays.copyOfRange(data, contentStart, contentEnd);
                messages.add(unescapeFromLines(raw));
            }
        }
        return messages;
    }

    private static byte[] unescapeFromLines(byte[] raw) {
        String text = new String(raw, StandardCharsets.ISO_8859_1);
        String unescaped = text.replaceAll("(?m)^>(From )", "$1");
        return unescaped.getBytes(StandardCharsets.ISO_8859_1);
    }
}
