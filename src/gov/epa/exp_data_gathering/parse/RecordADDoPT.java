package gov.epa.exp_data_gathering.parse;

import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import gov.epa.api.ExperimentalConstants;

public class RecordADDoPT {
	String cas;
	String solubility;
	String temp;
	String date_accessed;
	
	public static final String sourceName = ExperimentalConstants.strSourceADDoPT;
	
	public static Vector<RecordADDoPT> parseADDoPTRecordsFromExcel() {
		Vector<RecordADDoPT> records = new Vector<RecordADDoPT>();
		String folderNameExcel = "excel files";
		String mainFolder = "Data"+File.separator+"Experimental"+ File.separator + sourceName;
		String excelFilePath = mainFolder + File.separator+folderNameExcel;
		File folder = new File(excelFilePath);
		String[] filenames = folder.list();
		for (String filename:filenames) {
			if (filename.endsWith(".xlsx")) {
				try {
					String filepath = excelFilePath+File.separator+filename;
					FileInputStream fis = new FileInputStream(new File(filepath));
					Workbook wb = new XSSFWorkbook(fis);
					Sheet sheet = wb.getSheetAt(0);
					Row headerRow = sheet.getRow(0);
					int solubilityIndex = -1;
					int casIndex = -1;
					int tempIndex = -1;
					String date = Parse.getStringCreationDate(filepath);
					for (Cell cell:headerRow) {
						cell.setCellType(Cell.CELL_TYPE_STRING);
						String header = cell.getStringCellValue().toLowerCase();
						int col = cell.getColumnIndex();
						
						if (header.contains("observed solubility")) { solubilityIndex = col;
						} else if (header.equals("cas number")) { casIndex = col;
						} else if (header.equals("t")) { tempIndex = col;
						}
					}
					int rows = sheet.getLastRowNum();
					for (int i = 1; i < rows; i++) {
						Row row = sheet.getRow(i);
						for (Cell cell:row) { cell.setCellType(Cell.CELL_TYPE_STRING); }
						RecordADDoPT ar = new RecordADDoPT();
						ar.date_accessed = date;
						ar.cas = row.getCell(casIndex,MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
						ar.solubility = row.getCell(solubilityIndex,MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
						ar.temp = row.getCell(tempIndex,MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
						records.add(ar);
					}
					wb.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return records;
	}
}
