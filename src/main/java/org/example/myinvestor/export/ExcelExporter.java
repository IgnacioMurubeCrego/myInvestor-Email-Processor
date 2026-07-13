package org.example.myinvestor.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.myinvestor.model.StockOperation;
import org.example.myinvestor.model.UnparsedEmail;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExcelExporter {

    private static final String[] OPERATION_HEADERS = {
            "Fecha", "Valor", "ISIN", "Cantidad", "Importe Neto", "Divisa", "Importe Neto EUR"
    };

    private static final String[] UNPARSED_HEADERS = {"Asunto correo", "Fecha correo", "Motivo", "Fragmento"};

    public void export(Path outputFile, List<StockOperation> operations, List<UnparsedEmail> unparsed)
            throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dateStyle = dateStyle(workbook, "dd/mm/yyyy");
            CellStyle dateTimeStyle = dateStyle(workbook, "dd/mm/yyyy hh:mm:ss");
            CellStyle moneyStyle = moneyStyle(workbook);

            writeOperationsSheet(workbook, "Compras", filterByType(operations, "COMPRA"), headerStyle, dateStyle,
                    moneyStyle);
            writeOperationsSheet(workbook, "Ventas", filterByType(operations, "VENTA"), headerStyle, dateStyle,
                    moneyStyle);
            writeUnparsedSheet(workbook, unparsed, headerStyle, dateTimeStyle);

            try (OutputStream out = Files.newOutputStream(outputFile)) {
                workbook.write(out);
            }
        }
    }

    private List<StockOperation> filterByType(List<StockOperation> operations, String tipo) {
        List<StockOperation> filtered = new ArrayList<>();
        for (StockOperation op : operations) {
            if (tipo.equals(op.tipoOperacion)) {
                filtered.add(op);
            }
        }
        filtered.sort(Comparator.comparing((StockOperation op) -> op.fechaOperacion));
        return filtered;
    }

    private void writeOperationsSheet(XSSFWorkbook workbook, String sheetName, List<StockOperation> operations,
                                       CellStyle headerStyle, CellStyle dateStyle, CellStyle moneyStyle) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeHeaderRow(sheet, OPERATION_HEADERS, headerStyle);

        int rowIdx = 1;
        for (StockOperation op : operations) {
            Row row = sheet.createRow(rowIdx++);
            int col = 0;
            setDateCell(row, col++, op.fechaOperacion, dateStyle);
            setStringCell(row, col++, op.valorNombre);
            setStringCell(row, col++, op.isin);
            setNumericCell(row, col++, op.cantidad.doubleValue(), null);
            setNumericCell(row, col++, op.importeNeto.amount().doubleValue(), moneyStyle);
            setStringCell(row, col++, op.importeNeto.currency());
            setNumericCell(row, col++, op.importeNetoEur.doubleValue(), moneyStyle);
        }

        finalizeSheet(sheet, OPERATION_HEADERS.length, rowIdx);
    }

    private void writeUnparsedSheet(XSSFWorkbook workbook, List<UnparsedEmail> unparsed, CellStyle headerStyle,
                                     CellStyle dateTimeStyle) {
        Sheet sheet = workbook.createSheet("No procesados");
        writeHeaderRow(sheet, UNPARSED_HEADERS, headerStyle);

        int rowIdx = 1;
        for (UnparsedEmail email : unparsed) {
            Row row = sheet.createRow(rowIdx++);
            setStringCell(row, 0, email.subject());
            setDateTimeCell(row, 1, email.emailDate(), dateTimeStyle);
            setStringCell(row, 2, email.reason());
            setStringCell(row, 3, email.snippet());
        }

        finalizeSheet(sheet, UNPARSED_HEADERS.length, rowIdx);
    }

    private void writeHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void finalizeSheet(Sheet sheet, int columnCount, int rowCount) {
        if (rowCount > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, rowCount - 1, 0, columnCount - 1));
        }
        sheet.createFreezePane(0, 1);
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void setStringCell(Row row, int col, String value) {
        row.createCell(col).setCellValue(value == null ? "" : value);
    }

    private void setNumericCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void setDateCell(Row row, int col, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }

    private void setDateTimeCell(Row row, int col, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle dateStyle(XSSFWorkbook workbook, String pattern) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(pattern));
        return style;
    }

    private CellStyle moneyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
        return style;
    }
}
