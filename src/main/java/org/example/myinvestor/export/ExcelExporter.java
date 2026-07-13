package org.example.myinvestor.export;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.myinvestor.model.StockOperation;
import org.example.myinvestor.model.UnparsedEmail;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;

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

    private static final int COLUMN_WIDTH_CHARACTERS = 30;

    public void export(Path outputFile, List<StockOperation> operations, List<UnparsedEmail> unparsed)
            throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dateStyle = dateStyle(workbook, "dd/mm/yyyy");
            CellStyle dateTimeStyle = dateStyle(workbook, "dd/mm/yyyy hh:mm:ss");
            CellStyle moneyStyle = moneyStyle(workbook);
            CellStyle plainStyle = plainStyle(workbook);

            writeOperationsSheet(workbook, "Compras", "TablaCompras", 1, filterByType(operations, "COMPRA"),
                    headerStyle, dateStyle, moneyStyle, plainStyle);
            writeOperationsSheet(workbook, "Ventas", "TablaVentas", 2, filterByType(operations, "VENTA"),
                    headerStyle, dateStyle, moneyStyle, plainStyle);
            writeUnparsedSheet(workbook, unparsed, headerStyle, dateTimeStyle, plainStyle);

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

    private void writeOperationsSheet(XSSFWorkbook workbook, String sheetName, String tableName, int tableId,
                                       List<StockOperation> operations, CellStyle headerStyle, CellStyle dateStyle,
                                       CellStyle moneyStyle, CellStyle plainStyle) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        writeHeaderRow(sheet, OPERATION_HEADERS, headerStyle);

        int rowIdx = 1;
        for (StockOperation op : operations) {
            Row row = sheet.createRow(rowIdx++);
            int col = 0;
            setDateCell(row, col++, op.fechaOperacion, dateStyle);
            setStringCell(row, col++, op.valorNombre, plainStyle);
            setStringCell(row, col++, op.isin, plainStyle);
            setNumericCell(row, col++, op.cantidad.doubleValue(), plainStyle);
            setNumericCell(row, col++, op.importeNeto.amount().doubleValue(), moneyStyle);
            setStringCell(row, col++, op.importeNeto.currency(), plainStyle);
            setNumericCell(row, col++, op.importeNetoEur.doubleValue(), moneyStyle);
        }

        finalizeSheet(sheet, tableName, tableId, OPERATION_HEADERS, rowIdx);
    }

    private void writeUnparsedSheet(XSSFWorkbook workbook, List<UnparsedEmail> unparsed, CellStyle headerStyle,
                                     CellStyle dateTimeStyle, CellStyle plainStyle) {
        XSSFSheet sheet = workbook.createSheet("No procesados");
        writeHeaderRow(sheet, UNPARSED_HEADERS, headerStyle);

        int rowIdx = 1;
        for (UnparsedEmail email : unparsed) {
            Row row = sheet.createRow(rowIdx++);
            setStringCell(row, 0, email.subject(), plainStyle);
            setDateTimeCell(row, 1, email.emailDate(), dateTimeStyle);
            setStringCell(row, 2, email.reason(), plainStyle);
            setStringCell(row, 3, email.snippet(), plainStyle);
        }

        finalizeSheet(sheet, "TablaNoProcesados", 3, UNPARSED_HEADERS, rowIdx);
    }

    private void writeHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Turns the written range into a real Excel Table (not just an AutoFilter range), so it
     * gets the banded "table format" look. XSSFTable needs its columns/autofilter/style
     * populated by hand: leaving them out makes Excel flag the file as corrupt and repair it.
     */
    private void finalizeSheet(XSSFSheet sheet, String tableName, int tableId, String[] headers, int rowCount) {
        sheet.createFreezePane(0, 1);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, COLUMN_WIDTH_CHARACTERS * 256);
        }

        // A table/autofilter whose range is only the header row (no data rows below) gets
        // silently stripped by real Excel on open, even though the file is schema-valid and
        // openpyxl reads it fine. Skip the table entirely when there's nothing to show.
        if (rowCount <= 1) {
            return;
        }

        AreaReference area = new AreaReference(
                new CellReference(0, 0), new CellReference(rowCount - 1, headers.length - 1),
                SpreadsheetVersion.EXCEL2007);

        XSSFTable table = sheet.createTable(area);
        table.setName(tableName);
        table.setDisplayName(tableName);

        CTTable ctTable = table.getCTTable();
        ctTable.setId(tableId);
        // createTable() doesn't reliably set this for a header-only (no data rows) range.
        ctTable.setRef(area.formatAsString());

        // createTable() already pre-populates autoFilter/tableColumns for ranges with data rows
        // (but not for a header-only range). Unset before adding ours so we never end up with a
        // duplicate element, which XMLBeans allows but Excel's schema (maxOccurs=1) rejects.
        if (ctTable.isSetAutoFilter()) {
            ctTable.unsetAutoFilter();
        }
        ctTable.addNewAutoFilter().setRef(area.formatAsString());

        // CT_Table requires this element sequence: autoFilter, tableColumns, tableStyleInfo.
        // tableColumns is mandatory (no isSet/unset pair), so reuse it if createTable() already
        // added one instead of appending a second (invalid) tableColumns element.
        CTTableColumns columns = ctTable.getTableColumns();
        if (columns == null) {
            columns = ctTable.addNewTableColumns();
        } else {
            columns.setTableColumnArray(new CTTableColumn[0]);
        }
        columns.setCount(headers.length);
        for (int i = 0; i < headers.length; i++) {
            CTTableColumn column = columns.addNewTableColumn();
            column.setId(i + 1);
            column.setName(headers[i]);
        }

        if (ctTable.isSetTableStyleInfo()) {
            ctTable.unsetTableStyleInfo();
        }
        CTTableStyleInfo styleInfo = ctTable.addNewTableStyleInfo();
        styleInfo.setName("TableStyleMedium9");
        styleInfo.setShowRowStripes(true);
        styleInfo.setShowColumnStripes(false);
    }

    private void setStringCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void setNumericCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
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
        style.setAlignment(HorizontalAlignment.LEFT);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle dateStyle(XSSFWorkbook workbook, String pattern) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(pattern));
        return style;
    }

    private CellStyle moneyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private CellStyle plainStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }
}
