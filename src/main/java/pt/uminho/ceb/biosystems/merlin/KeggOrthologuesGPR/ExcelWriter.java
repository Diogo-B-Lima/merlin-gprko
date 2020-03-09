package pt.uminho.ceb.biosystems.merlin.KeggOrthologuesGPR;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelWriter {

	public static void main(String[] header, List<Object[]> contents, String outputFilePath) throws IOException {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("ExcelWriter");

		int headerColumnCount = 0;
		Row headerRow = sheet.createRow(0);
		for(String column:header) {
			Cell cell = headerRow.createCell(headerColumnCount++);
			cell.setCellValue(column);
		}

		int rowCount = 0;

		for (Object[] aBook : contents) {
			Row row = sheet.createRow(++rowCount);

			int columnCount = -1;

			for (Object field : aBook) {
				Cell cell = row.createCell(++columnCount);
				if (field instanceof String) {
					cell.setCellValue((String) field);
				} else if (field instanceof Integer) {
					cell.setCellValue((Integer) field);
				}
			}

		}

		try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
			workbook.write(outputStream);
			workbook.close();
		}
	}

}
