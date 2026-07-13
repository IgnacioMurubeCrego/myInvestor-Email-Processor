package org.example.myinvestor.model;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Money(BigDecimal amount, String currency) {

    private static final Pattern MONEY_PATTERN = Pattern.compile("^([-\\d.,]+)\\s*([A-Za-z]{3})$");

    public static Money parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        Matcher m = MONEY_PATTERN.matcher(s);
        if (!m.matches()) {
            throw new IllegalArgumentException("Formato de importe no reconocido: '" + raw + "'");
        }
        return new Money(parseAmount(m.group(1)), m.group(2).toUpperCase());
    }

    public static BigDecimal parseAmount(String raw) {
        String s = raw.trim();
        int lastComma = s.lastIndexOf(',');
        int lastDot = s.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                s = s.replace(".", "").replace(",", ".");
            } else {
                s = s.replace(",", "");
            }
        } else if (lastComma >= 0) {
            s = s.replace(",", ".");
        }
        return new BigDecimal(s);
    }
}
