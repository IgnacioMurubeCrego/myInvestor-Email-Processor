package org.example.myinvestor.parser;

import org.example.myinvestor.mail.MailContent;
import org.example.myinvestor.model.Money;
import org.example.myinvestor.model.StockOperation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses myInvestor "DETALLE OPERACIÓN" transaction emails. The email body renders as
 * a table of label-row / value-row pairs (see LineNormalizer / HtmlLineExtractor), so
 * each field is located by matching the label row and reading the very next line.
 */
public class OperationParser {

    private static final Pattern ISIN_PATTERN = Pattern.compile("(?i)C[oó]digo\\s+ISIN:\\s*([A-Z0-9]+)");
    private static final Pattern EUR_ONLY = Pattern.compile("(?i)^([-\\d.,]+)\\s*EUR$");
    private static final Pattern OPERATION_TYPE = Pattern.compile("\\b(COMPRA|VENTA)\\b");
    private static final Pattern ISIN_SUFFIX = Pattern.compile("(?i)\\s*C[oó]digo\\s+ISIN:.*$");
    private static final Pattern COMBINED_NETO = Pattern.compile(
            "(?i)^([-\\d.,]+)\\s*([A-Za-z]{3})\\s+([-\\d.,]+)\\s*EUR$");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public Optional<ParseOutcome> tryParse(MailContent mail) {
        List<String> plainLines = LineNormalizer.normalize(mail.plainText());
        List<String> lines = isRelevant(plainLines) ? plainLines : null;

        if (lines == null) {
            List<String> htmlLines = HtmlLineExtractor.extract(mail.htmlText());
            if (isRelevant(htmlLines)) {
                lines = htmlLines;
            }
        }

        if (lines == null) {
            return Optional.empty();
        }

        try {
            StockOperation operation = extract(lines, mail);
            return Optional.of(ParseOutcome.success(operation));
        } catch (Exception e) {
            return Optional.of(ParseOutcome.failure(e.getMessage()));
        }
    }

    /**
     * myInvestor reuses the "DETALLE OPERACIÓN" template for non-trade notices (e.g. interest
     * settlements), which have no COMPRA/VENTA to report. Requiring both avoids flooding the
     * "no procesados" review sheet with emails that were never meant to be stock trades.
     */
    private boolean isRelevant(List<String> lines) {
        boolean hasMarker = false;
        boolean hasOperationType = false;
        for (String line : lines) {
            String upper = line.toUpperCase(Locale.ROOT);
            if (upper.contains("DETALLE OPERACI")) {
                hasMarker = true;
            }
            if (OPERATION_TYPE.matcher(upper).find()) {
                hasOperationType = true;
            }
        }
        return hasMarker && hasOperationType;
    }

    private StockOperation extract(List<String> lines, MailContent mail) {
        String[] mercadoValor = requireHeader(lines, "Mercado", "Valor").values;
        String mercado = mercadoValor[0];
        String valorNombre = ISIN_SUFFIX.matcher(mercadoValor[1]).replaceFirst("").trim();

        String isin = findIsin(lines);

        String[] opRef = requireHeader(lines, "Operación", "Referencia Operación").values;
        String tipoOperacion = opRef[0].toUpperCase(Locale.ROOT);
        String referencia = opRef[1];

        String[] fechas = requireHeader(lines, "Fecha Operación", "Fecha Valor", "Fecha y Hora Ejecución").values;
        LocalDate fechaOperacion = parseDate(fechas[0]);
        LocalDate fechaValor = parseDate(fechas[1]);
        LocalDateTime fechaHoraEjecucion = parseDateTime(fechas[2]);

        String[] cantidadPrecioImporte = requireHeader(lines, "títulos", "Precio Bruto", "Importe Bruto").values;
        BigDecimal cantidad = Money.parseAmount(cantidadPrecioImporte[0]);
        Money precioBruto = Money.parse(cantidadPrecioImporte[1]);
        Money importeBruto = Money.parse(cantidadPrecioImporte[2]);

        String[] comisionesGastosTasas = requireHeader(lines, "Comisiones", "Gastos", "Tasas").values;
        Money comisiones = Money.parse(comisionesGastosTasas[0]);
        Money gastos = Money.parse(comisionesGastosTasas[1]);
        Money tasas = Money.parse(comisionesGastosTasas[2]);

        HeaderMatch retHeader = requireHeader(lines, "Retención Origen", "Retención Destino", "Importe Efectivo Neto");
        Money retencionOrigen = Money.parse(retHeader.values[0]);
        Money retencionDestino = Money.parse(retHeader.values[1]);

        NetoResult neto = resolveImporteNeto(lines, retHeader);

        return new StockOperation(fechaOperacion, fechaValor, fechaHoraEjecucion, tipoOperacion, mercado,
                valorNombre, isin, referencia, cantidad, precioBruto, importeBruto, comisiones, gastos, tasas,
                retencionOrigen, retencionDestino, neto.importeNeto, neto.importeNetoEur, mail.subject(),
                mail.date());
    }

    /**
     * The net-amount cell holds the original-currency figure and, when it isn't already EUR, the
     * converted EUR figure too. Depending on the source those two values land either as one
     * combined string (HTML table cells collapse a &lt;br&gt; to a space via jsoup's text())
     * or as a separate following line (plain-text emails keep the physical line break).
     */
    private NetoResult resolveImporteNeto(List<String> lines, HeaderMatch retHeader) {
        String rawNeto = retHeader.values[2];
        Matcher combined = COMBINED_NETO.matcher(rawNeto.trim());
        if (combined.matches()) {
            Money importeNeto = new Money(Money.parseAmount(combined.group(1)), combined.group(2).toUpperCase(Locale.ROOT));
            return new NetoResult(importeNeto, Money.parseAmount(combined.group(3)));
        }

        Money importeNeto = Money.parse(rawNeto);
        if ("EUR".equals(importeNeto.currency())) {
            return new NetoResult(importeNeto, importeNeto.amount());
        }

        int continuationIdx = retHeader.headerIndex() + 2;
        if (continuationIdx < lines.size()) {
            Matcher eurMatch = EUR_ONLY.matcher(lines.get(continuationIdx).trim());
            if (eurMatch.matches()) {
                return new NetoResult(importeNeto, Money.parseAmount(eurMatch.group(1)));
            }
        }
        throw new IllegalStateException("No se ha encontrado el importe neto convertido a EUR");
    }

    private record NetoResult(Money importeNeto, BigDecimal importeNetoEur) {
    }

    private String findIsin(List<String> lines) {
        for (String line : lines) {
            Matcher m = ISIN_PATTERN.matcher(line);
            if (m.find()) {
                return m.group(1).toUpperCase(Locale.ROOT);
            }
        }
        throw new IllegalStateException("No se ha encontrado el código ISIN");
    }

    private LocalDate parseDate(String s) {
        return LocalDate.parse(s.trim(), DATE_FMT);
    }

    private LocalDateTime parseDateTime(String s) {
        return LocalDateTime.parse(s.trim(), DATETIME_FMT);
    }

    private HeaderMatch requireHeader(List<String> lines, String... labels) {
        return findHeader(lines, labels).orElseThrow(() ->
                new IllegalStateException("No se ha encontrado la sección: " + String.join(" / ", labels)));
    }

    private Optional<HeaderMatch> findHeader(List<String> lines, String... labels) {
        for (int i = 0; i < lines.size() - 1; i++) {
            String[] cells = lines.get(i).split("\t");
            if (cells.length != labels.length) {
                continue;
            }
            boolean allMatch = true;
            for (int j = 0; j < labels.length; j++) {
                if (!cells[j].toLowerCase(Locale.ROOT).contains(labels[j].toLowerCase(Locale.ROOT))) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                return Optional.of(new HeaderMatch(i, lines.get(i + 1).split("\t")));
            }
        }
        return Optional.empty();
    }

    private record HeaderMatch(int headerIndex, String[] values) {
    }
}
