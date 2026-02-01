package com.ipam.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilitaires d'export pour TableView (Excel et PDF)
 */
public final class ExportUtil {
    private ExportUtil() {}

    public static void exportTableViewToExcel(TableView<?> tableView, File file, String title) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = new FileOutputStream(file)) {
            Sheet sheet = workbook.createSheet(sanitizeSheetName(title));
            // Header row
            Row header = sheet.createRow(0);
            List<TableColumn<?, ?>> columns = (List<TableColumn<?, ?>>)(List<?>) tableView.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns.get(i).getText());
            }
            // Data rows
            for (int r = 0; r < tableView.getItems().size(); r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < columns.size(); c++) {
                    TableColumn<?, ?> col = columns.get(c);
                    Object value = col.getCellData(r);
                    Cell cell = row.createCell(c);
                    cell.setCellValue(value != null ? String.valueOf(value) : "");
                }
            }
            // Autosize columns
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
        }
    }

    public static void exportTableViewToPdf(TableView<?> tableView, File file, String title) throws Exception {
        try (OutputStream out = new FileOutputStream(file)) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Paragraph p = new Paragraph(title + " (" + now() + ")", titleFont);
            p.setAlignment(Element.ALIGN_LEFT);
            p.setSpacingAfter(10f);
            document.add(p);

            // Table
            List<TableColumn<?, ?>> columns = (List<TableColumn<?, ?>>)(List<?>) tableView.getColumns();
            PdfPTable pdfTable = new PdfPTable(columns.size());
            pdfTable.setWidthPercentage(100);

            // Header cells
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            for (TableColumn<?, ?> col : columns) {
                PdfPCell cell = new PdfPCell(new Paragraph(col.getText(), headerFont));
                pdfTable.addCell(cell);
            }

            // Data cells
            Font cellFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            for (int r = 0; r < tableView.getItems().size(); r++) {
                for (TableColumn<?, ?> col : columns) {
                    Object value = col.getCellData(r);
                    PdfPCell cell = new PdfPCell(new Paragraph(value != null ? String.valueOf(value) : "", cellFont));
                    pdfTable.addCell(cell);
                }
            }

            document.add(pdfTable);
            document.close();
        }
    }

    private static String now() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now());
    }

    private static String sanitizeSheetName(String name) {
        return name.replaceAll("[\\/*?\\n\\r\\t\\[\\]]", "_");
    }
}
