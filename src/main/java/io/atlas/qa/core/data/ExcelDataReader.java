package io.atlas.qa.core.data;

import io.atlas.qa.core.exception.AtlasException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads an .xlsx sheet into a list of row maps keyed by the header names.
 *
 * <p>Spreadsheets are not the best format for test data, but they are the format
 * business analysts actually use. Supporting them is how automation gets test
 * cases from the people who own the requirements — while JSON stays the default
 * for anything the engineers maintain.
 */
public final class ExcelDataReader {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelDataReader() {
    }

    public static List<Map<String, String>> read(String classpathResource, String sheetName) {
        try (InputStream in = open(classpathResource); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new AtlasException("Sheet '%s' does not exist in %s (available: %s)"
                        .formatted(sheetName, classpathResource, sheetNames(workbook)));
            }
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                return List.of();
            }

            List<String> columns = new ArrayList<>();
            header.forEach(cell -> columns.add(FORMATTER.formatCellValue(cell).trim()));

            List<Map<String, String>> rows = new ArrayList<>();
            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (int column = 0; column < columns.size(); column++) {
                    Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values.put(columns.get(column), cell == null ? "" : FORMATTER.formatCellValue(cell).trim());
                }
                if (values.values().stream().anyMatch(value -> !value.isEmpty())) {
                    rows.add(values);
                }
            }
            return rows;
        } catch (IOException e) {
            throw new AtlasException("Unable to read the workbook " + classpathResource, e);
        }
    }

    /** Same content, shaped for a TestNG {@code @DataProvider}. */
    public static Object[][] asRows(String classpathResource, String sheetName) {
        List<Map<String, String>> rows = read(classpathResource, sheetName);
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = rows.get(i);
        }
        return data;
    }

    private static String sheetNames(Workbook workbook) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            names.add(workbook.getSheetName(i));
        }
        return String.join(", ", names);
    }

    private static InputStream open(String classpathResource) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource);
        if (in == null) {
            throw new AtlasException("Workbook not found on the classpath: " + classpathResource);
        }
        return in;
    }
}
