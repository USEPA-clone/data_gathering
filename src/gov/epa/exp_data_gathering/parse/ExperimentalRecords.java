package gov.epa.exp_data_gathering.parse;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import gov.epa.api.ExperimentalConstants;
import gov.epa.database.SQLite_GetRecords;
import gov.epa.database.SQLite_Utilities;
import gov.epa.eChemPortalAPI.Processing.FinalRecord;
import gov.epa.eChemPortalAPI.Query.APIConstants;
import gov.epa.ghs_data_gathering.Parse.Parse;


/**
 * Class to store chemicals
 * 
 * @author Todd Martin
 *
 */

public class ExperimentalRecords extends ArrayList<ExperimentalRecord> {
	
	public static final String tableName = "ExperimentalRecords";


	public ExperimentalRecords() { }
	
	public ExperimentalRecords(FinalRecord rec) {
		int size = 1;
		int speciesSize = 1;
		if (rec.valueTypes!=null && !rec.valueTypes.isEmpty()) {
			size = rec.valueTypes.size();
		} else if (rec.genotoxicity!=null && !rec.genotoxicity.isEmpty()) {
			size = rec.genotoxicity.size();
			if (rec.propertyName.equals(APIConstants.geneticToxicityVitro)) { speciesSize = size; }
		}
		for (int i = 0; i < size; i++) {
			ExperimentalRecord er = new ExperimentalRecord();
			er.source_name = ExperimentalConstants.strSourceEChemPortalAPI;
			er.url = rec.url;
			er.original_source_name = rec.participant;
			er.date_accessed = rec.dateAccessed;
			er.reliability = rec.reliability;
			er.fr_id = rec.id;
			
			if (!rec.name.equals("-") && !rec.name.contains("unnamed")) {
				er.chemical_name = rec.name;
			}
			
			if (rec.numberType!=null) {
				switch (rec.numberType) {
				case "CAS Number":
					er.casrn = rec.number;
					break;
				case "EC Number":
					er.einecs = rec.number;
					break;
				}
			}
			
			String species = null;
			if (speciesSize>1) {
				species = (rec.species==null || rec.species.isEmpty()) ? null : rec.species.get(i);
			} else {
				species = (rec.species==null || rec.species.isEmpty()) ? null : rec.species.get(0);
			}
			
			String valueType = (rec.valueTypes==null || rec.valueTypes.isEmpty()) ? "" : "_"+rec.valueTypes.get(i);
			if (species!=null) {
				if (!species.toLowerCase().contains("other")) {
					er.property_name=species.replaceAll(" ","_").replaceAll(",","")+"_"+rec.propertyName+valueType;
				} else {
					er.property_name="other_"+rec.propertyName+valueType;
					er.updateNote("Species: "+species.substring(rec.species.indexOf(":")+1));
				}
			} else {
				er.property_name = rec.propertyName+valueType;
			}
			
			String value = null;
			if (rec.experimentalValues!=null && !rec.experimentalValues.isEmpty()) {
				value = rec.experimentalValues.get(i);
			} else if (rec.genotoxicity!=null && !rec.genotoxicity.isEmpty()) {
				value = rec.genotoxicity.get(i);
			} else if (rec.interpretationOfResults!=null && !rec.interpretationOfResults.isEmpty()) {
				value = rec.interpretationOfResults;
			}
			
			if (value==null) { continue; }
			ParseUtilities.getToxicity(er,value);
			er.property_value_string = "Value: "+value;
			
			this.add(er);
		}
	}

	public JsonElement toJsonElement() {
		String strJSON=this.toJSON();
		Gson gson = new Gson();
		JsonElement json = gson.fromJson(strJSON, JsonElement.class);
		
		
		return json;
	}
	
	public Hashtable<String, List<ExperimentalRecord>> createExpRecordHashtableBySID(String desiredUnits) {
		
		Hashtable<String,List<ExperimentalRecord>>htER=new Hashtable<>();
		
		for (ExperimentalRecord er:this)  {
			
			if(!er.keep) continue;
			
			//Only use the ones with g/L in the stats calcs:
			if(er.property_value_units_final.equals(desiredUnits)) {
//				System.out.println(er.casrn+"\t"+er.property_value_point_estimate_final);
				
				if(er.dsstox_substance_id==null) continue;
				
				if(htER.containsKey(er.dsstox_substance_id) ) {
					List<ExperimentalRecord>recs=htER.get(er.dsstox_substance_id);
					recs.add(er);	
					
				} else {
					List<ExperimentalRecord>recs=new ArrayList<>();
					recs.add(er);
					htER.put(er.dsstox_substance_id, recs);
				}
				
			}
		}
		return htER;
	}
	

	/**
	 * 
	 * 
	 * @param recs
	 * @return
	 */
	public static Double calculateSD(List<ExperimentalRecord> recs,boolean convertToLog) {
		
		Gson gson=new Gson();
		
		int count=0;
		double avg=0;
		
		
		for (ExperimentalRecord er:recs) {
			
			Double value=er.property_value_point_estimate_final;

			if (value==null) {
				if (er.property_value_min_final!=null && er.property_value_max_final!=null) {
					value=(er.property_value_min_final+er.property_value_max_final)/2.0;	
				} else {
					continue;
				}
			}
			
			if(convertToLog) {
				if(value==0) continue;
				avg+=Math.log10(value);
			} else {
				avg+=value;
			}
//			if (!er.property_value_units_final.equals(ExperimentalConstants.str_g_L)) continue;
			count++;
		}
		
		if(count==0) return null;
		
		avg/=(double)count;
		
//		System.out.println(count+"\t"+avg);
		
		
		double SD=0;
		
		for (ExperimentalRecord er:recs) {
			
			if (er.property_value_point_estimate_final==null) continue;
			
			if(convertToLog) {
				if(er.property_value_point_estimate_final==0) continue;
				SD+=Math.pow(Math.log10(er.property_value_point_estimate_final)-avg,2);
			} else {
				SD+=Math.pow(er.property_value_point_estimate_final-avg,2);
			}
		}
		SD/=(double)count;
		
		SD=Math.sqrt(SD);
//		System.out.println(count+"\t"+SD+"\t"+avg);
		
		
		
		return SD;
		
	}
	
	public static void calculateStdDev(Hashtable<String, List<ExperimentalRecord>> htER, boolean convertToLog) {
		double avgSD=0;
		int count=0;
		int countOverall=0;

		for (String dtxsid:htER.keySet()) {
			List<ExperimentalRecord> records=htER.get(dtxsid);
			Double SD=ExperimentalRecords.calculateSD(records,convertToLog);//TODO need to determine SD when converted to log values
			
			if(SD==null) continue;
			
//			System.out.println(count+"\t"+SD);
			
			avgSD+=SD;
			count++;
			countOverall+=records.size();
		}
		
		
		avgSD/=(double)count;
		System.out.println("Avg SD\t"+avgSD);
		System.out.println("Unique SIDs\t"+htER.size());
		System.out.println("Kept records with correct units\t"+countOverall);
	}
	
	public static Hashtable<String,Double> calculateMedian(Hashtable<String, List<ExperimentalRecord>> htER, boolean convertToLog) {
		Hashtable<String,Double> htMedian=new Hashtable<>();
		for (String dtxsid:htER.keySet()) {
			
			if(dtxsid==null || dtxsid.equals("-")) continue;
			
			Double median=ExperimentalRecords.calculateMedian(htER.get(dtxsid),convertToLog);//TODO need to determine SD when converted to log values			
			if(median==null) continue;

//			System.out.println(dtxsid+"\t"+median);
			
			htMedian.put(dtxsid, median);
		}
		return htMedian;
	}
	

	
	private static Double calculateMedian(List<ExperimentalRecord> records, boolean convertToLog) {

		List<Double>values=new ArrayList<>();
		
		for(ExperimentalRecord er:records) {
			
			Double value=er.property_value_point_estimate_final;

			if (value==null) {
				if (er.property_value_min_final!=null && er.property_value_max_final!=null) {
					value=(er.property_value_min_final+er.property_value_max_final)/2.0;	
				} else {
					continue;
				}
			}
			
			if(convertToLog) {
				if(value==0) continue;
				values.add(Math.log10(value));
			} else {
				values.add(value);
			}
		}
		
		Collections.sort(values);
		
		int n=values.size();
		
		if(n==0) return null;
		
		if(n%2==1){			
			int index=((n+1)/2)-1;
//			System.out.println(n+"\todd:\t"+index);
			return values.get(index);			
		} else	{			
//			System.out.println(n+"\teven:\t"+(n/2-1)+"\t"+(n/2));
			return (values.get(n/2-1)+values.get(n/2))/2.0;
		}
		
	}

	public ExperimentalRecord getRecord(String CAS) {
		
		for (ExperimentalRecord record:this) {
			if (record.casrn.equals(CAS)) return record;
		}
		return null;
	}
	
	public void addSourceBasedIDNumbers() {

		for (int i=0;i<size();i++) {
			ExperimentalRecord record=get(i);
			record.id_physchem=record.source_name+"_"+(i+1);
		}
	}
	
	
	public void toFlatFile(String filepath,String del) {
		
		try {
								
			FileWriter fw=new FileWriter(filepath);
			
			fw.write(getHeader(del)+"\r\n");
											
			for (ExperimentalRecord record:this) {				
				String line=record.toString("|");				
				line=Parse.fixChars(line);							
				fw.write(line+"\r\n");
			}
			fw.flush();
			fw.close();
						
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	public static String getHeader(String del) {
		// TODO Auto-generated method stub

		List<String> fieldNames=ExperimentalRecord.outputFieldNames;
		String Line = "";
		for (int i = 0; i < fieldNames.size(); i++) {
			Line += fieldNames.get(i);
			if (i < fieldNames.size() - 1) {
				Line += del;
			}
			
		}

		return Line;
	}
	
	public String toJSON() {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();// makes it multiline and readable
		Gson gson = builder.create();
		return gson.toJson(this);//all in one line!
	}
	

	
	public void toJSON_File(String filePath) {

		try {

			File file = new File(filePath);
			file.getParentFile().mkdirs();

			GsonBuilder builder = new GsonBuilder();
			builder.setPrettyPrinting().disableHtmlEscaping();
			Gson gson = builder.create();

			FileWriter fw = new FileWriter(file);
			fw.write(gson.toJson(this));
			fw.flush();
			fw.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
	
	public static ExperimentalRecords loadFromExcel(String excelFilePath) {

		try {

			ExperimentalRecords records = new ExperimentalRecords();

			FileInputStream inputStream = new FileInputStream(new File(excelFilePath));

			Workbook workbook = new XSSFWorkbook(inputStream);
			Sheet firstSheet = workbook.getSheetAt(0);

			DataFormatter formatter = new DataFormatter();
			
			Row rowHeader = firstSheet.getRow(1);

			Vector<String>fieldNames=new Vector<String>();
			
			for (int i=0;i<rowHeader.getLastCellNum();i++) {
				String fieldName=rowHeader.getCell(i).getStringCellValue();
				fieldNames.add(fieldName);
			}
			
			
			for (int i=2;i<firstSheet.getLastRowNum();i++) {
				ExperimentalRecord record=new ExperimentalRecord();
			
				Row row = firstSheet.getRow(i);
				
				for (int j=0;j<fieldNames.size();j++) {
					String fieldName=fieldNames.get(j);					

					if (row.getCell(j)!=null) {
						String fieldValue=formatter.formatCellValue(row.getCell(j));
						record.assignValue(fieldName, fieldValue);
					}
					
				}
				records.add(record);
//				System.out.println(record.toJSON());
				
			}
			
			inputStream.close();
			workbook.close();
			return records;

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	/**
	 * TODO rewrite this so it uses same code for both tabs to make edits easier
	 * 
	 * @param filePath
	 * @param fieldNames
	 */
	public void toExcel_File(String filePath,List<String> fieldNames) {

		

		int size = this.size();
		Workbook wb = new XSSFWorkbook();
		
		CellStyle styleURL=createStyleURL(wb);
		
		writeSheet(fieldNames, "Records",true,wb,styleURL);
		writeSheet(fieldNames, "Records_Bad",false,wb,styleURL);
		
		try (OutputStream fos = new FileOutputStream(filePath)) {
			wb.write(fos);
			wb.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		
	}
	
	static CellStyle createStyleURL(Workbook workbook) {
		CellStyle hlink_style = workbook.createCellStyle();
		Font hlink_font = workbook.createFont();
		hlink_font.setUnderline(Font.U_SINGLE);
		hlink_font.setColor(Font.COLOR_RED);
		hlink_style.setFont(hlink_font);
		return hlink_style;
	}

	private void writeSheet(List<String> headers, String sheetName, boolean keep, Workbook wb, CellStyle styleURL) {
		Sheet recSheet = wb.createSheet(sheetName);
		Row recSubtotalRow = recSheet.createRow(0);
		Row recHeaderRow = recSheet.createRow(1);
		CellStyle style = wb.createCellStyle();
		Font font = wb.createFont();;//Create font
		font.setBold(true);//Make font bold
		style.setFont(font);
		
		
		for (int i = 0; i < headers.size(); i++) {
			Cell recCell = recHeaderRow.createCell(i);
			recCell.setCellValue(headers.get(i));
			recCell.setCellStyle(style);
		}
		int recCurrentRow = 2;
		for (ExperimentalRecord er:this) {
			if (!er.keep==keep) continue;
			
			Class erClass = er.getClass();			
			Class htClass = er.experimental_parameters.getClass();
			
			Object value = null;
			try {
				Row row = recSheet.createRow(recCurrentRow);
				recCurrentRow++;
				 
				for (int i = 0; i < headers.size(); i++) {
					
					if(headers.get(i).contains("exp_param_")) {
						String fieldName=headers.get(i).substring(headers.get(i).indexOf("exp_param_")+"exp_param_".length(),headers.get(i).length());
						value = er.experimental_parameters.get(fieldName);
//					} else if(headers.get(i).equals("lit_source_citation")) {
//						if(er.literatureSource!=null) {
//							value = er.literatureSource.citation;
//						} else {
//							continue;
//						}
//
					} else {
						Field field = erClass.getDeclaredField(headers.get(i));
						value = field.get(er);
					}
					
					if (value==null) continue;
					
					if (headers.get(i).contentEquals("url")) {
						String strValue = (String) value;
						Cell cell = row.createCell(i);     						
						Hyperlink href = wb.getCreationHelper().createHyperlink(HyperlinkType.URL);
						href.setAddress(strValue);
						cell.setHyperlink(href);
						cell.setCellStyle(styleURL);
						
						if (strValue.length() > 32767) { strValue = strValue.substring(0,32767); }
						cell.setCellValue(strValue);

					} else if (!(value instanceof Double)) { 

						String strValue=null;
						
						if(headers.get(i).equals("chemical_name")) {//TODO is this the only one?
							strValue= ParseUtilities.reverseFixChars(StringEscapeUtils.unescapeHtml4(value.toString()));
						} else {
							strValue= StringEscapeUtils.unescapeHtml4(value.toString());
						}
						
						if (strValue.length() > 32767) { strValue = strValue.substring(0,32767); }
						row.createCell(i).setCellValue(strValue);
					} else { 
						row.createCell(i).setCellValue((double) value); 
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println(value.toString());
			}
		}
		
		String lastCol = CellReference.convertNumToColString(headers.size()-1);
		recSheet.setAutoFilter(CellRangeAddress.valueOf("A2:"+lastCol+recCurrentRow));
		recSheet.createFreezePane(0, 2);
		
		for (int i = 0; i < headers.size(); i++) {
			String col = CellReference.convertNumToColString(i);
			String recSubtotal = "SUBTOTAL(3,"+col+"$3:"+col+"$"+(recCurrentRow+1)+")";
			recSubtotalRow.createCell(i).setCellFormula(recSubtotal);
		}
	}
		
	public void toExcel_File(String filePath) {
		toExcel_File(filePath,ExperimentalRecord.outputFieldNames);
	}
	
	public void toExcel_FileDetailed(String filePath) {
		
		List<String>fieldNames=ExperimentalRecord.outputFieldNames;

		List<String>params=new ArrayList<>();

		//Figure out list of experimental parameters
		for (ExperimentalRecord er:this) {
			if(er.experimental_parameters!=null) {			
				for (String key:er.experimental_parameters.keySet()) {
					if(!params.contains(key)) params.add(key);
				}
			}
		}
		Collections.sort(params);
		
		for (String param:params) {
			fieldNames.add("exp_param_"+param);
//			System.out.println(fieldNames.get(fieldNames.size()-1));
		}
		
//		fieldNames.add("lit_source_citation");
		
		toExcel_File(filePath,fieldNames);
	}

	
	public void createCheckingFile(ExperimentalRecords records,String filePath, int maxRows) {
	
		System.out.println("Writing Checking Excel file for chemical records");

		double pct = 0.01;
		int min = 100;
		int max = 500;
		
		int size = records.size();
		int n = (int) Math.ceil(pct*size); // GS: Round up instead of just truncating
		if (size < min) {
			n = size;
		} else if (n < min) {
			n = min;
		} else if (n > max) {
			n = max;
		}
		
		// GS: You can't use clone() without implementing Cloneable! This doesn't actually create a deep copy.
		// GS: But also...why clone at all? The order of records is immaterial; we can just shuffle them in place.
//		ExperimentalRecords recordsClone=(ExperimentalRecords) records.clone();
		
		// GS: Why shuffle >2M records when we're only using 1% of them?
		// GS: Use Durstenfeld's algorithm: https://stackoverflow.com/questions/4702036/take-n-random-elements-from-a-liste
//		Collections.shuffle(records);
		
		Random rand = new Random();
		for (int i = size - 1; i >= size - n; --i) {
			Collections.swap(records, i, rand.nextInt(i + 1));
	    }
		
		ExperimentalRecords recordsCheck = new ExperimentalRecords();
		for (int j = size - n; j < size; j++) {
			recordsCheck.add(records.get(j));
		}
		recordsCheck.toExcel_File_Split(filePath,maxRows);
	
		createLogEntrySheet(filePath); 		
	}

	private void createLogEntrySheet(String filePath) {
		try {
//			XSSFWorkbook workbook = (XSSFWorkbook)WorkbookFactory.create(excelFile);
			
			FileInputStream fileinp = new FileInputStream(filePath);
			XSSFWorkbook workbook = new XSSFWorkbook(fileinp);
			
			XSSFSheet sheet = workbook.createSheet("Log entry");
			Row row=sheet.createRow(0);
			
			Cell cell = row.createCell(0);
			cell.setCellValue("Auditor name");
			
			cell = row.createCell(1);
			cell.setCellValue("Add name");

			row=sheet.createRow(1);

			cell = row.createCell(0);
			cell.setCellValue("Audit date");
			

            cell = row.createCell(1);  
			CellStyle cellStyle = workbook.createCellStyle();  
			CreationHelper createHelper = workbook.getCreationHelper();
			cellStyle.setDataFormat(  
                createHelper.createDataFormat().getFormat("m/d/yyyy"));  
            cell.setCellValue(new Date());  
            cell.setCellStyle(cellStyle);  
            
			
			cell.setCellValue(new Date());
			
			row=sheet.createRow(2);
			
			cell = row.createCell(0);
			cell.setCellValue("All cells have been checked for accuracy");
			
			cell = row.createCell(1);
			cell.setCellValue("No");
			
			sheet.setColumnWidth(0, 40*256);
			sheet.setColumnWidth(1, 15*256);
			
			OutputStream fos = new FileOutputStream(filePath);
			workbook.write(fos);
			workbook.close();

			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void toExcel_File_Split(String filePath, int maxRows) {
		
		File file=new File(filePath);
		String fileNameExcelExperimentalRecords=file.getName();
		String mainFolder=file.getParentFile().getAbsolutePath();
		
		
		if (size() <= maxRows) {
			System.out.println("<="+maxRows+" records,"+fileNameExcelExperimentalRecords);
			this.toExcel_File(filePath);
		} else {
			System.out.println(size()+" records, need to do batch");
			
			ExperimentalRecords temp = new ExperimentalRecords();
			Iterator<ExperimentalRecord> it = iterator();
			int i = 0;
			int batch = 0;
			while (it.hasNext()) {
				temp.add(it.next());
				i++;
				if (i!=0 && i%65000==0) {
					batch++;
					String batchFileName = fileNameExcelExperimentalRecords.substring(0,fileNameExcelExperimentalRecords.indexOf(".xlsx")) + " " + batch + ".xlsx";
					temp.toExcel_File(mainFolder+File.separator+batchFileName);
					temp.clear();
				}
			}
			batch++;
			String batchFileName = fileNameExcelExperimentalRecords.substring(0,fileNameExcelExperimentalRecords.indexOf(".xlsx")) + " " + batch + ".xlsx";
			temp.toExcel_File(mainFolder+File.separator+batchFileName);
			
			
		}
	}
	

	public ExperimentalRecords dumpBadRecords() {
		ExperimentalRecords recordsBad = new ExperimentalRecords();
		Iterator<ExperimentalRecord> it = this.iterator();
		while (it.hasNext() ) {
			ExperimentalRecord temp = it.next();
			if (!temp.keep) {
				recordsBad.add(temp);
				it.remove();
			}
		}
		return recordsBad;
	}

	public static ExperimentalRecords loadFromJSON(String jsonFilePath) {

		try {
			Gson gson = new Gson();

			File file = new File(jsonFilePath);

			if (!file.exists()) {
				return null;
			}

			ExperimentalRecords chemicals = gson.fromJson(new FileReader(jsonFilePath), ExperimentalRecords.class);			
			return chemicals;

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static Vector<String> getSourceList(Connection conn, String property) {
		Vector<String> sources = new Vector<>();

		String sql = "select distinct source_name from "+tableName+" where property_name=\"" + property
				+ "\" and keep=\"true\"";

		try {
			Statement stat=conn.createStatement();
			ResultSet rs = stat.executeQuery(sql);

			while (rs.next()) {
				String source = rs.getString(1);
//				System.out.println(source);
				sources.add(source);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return sources;
	}
	public static ExperimentalRecords getExperimentalRecordsFromDB_ForSource(String property, String source_name,
			String expRecordsDBPath) {

		ExperimentalRecords records = new ExperimentalRecords();

//		String sql = "select * from "+tableName+" where property_name=\"" + property + "\" and keep=\"true\" "
//				+ "and source_name=\"" + source_name + "\"\r\n" + "order by casrn";
		
		String sql = "select * from "+tableName+" where property_name=\"" + property + "\""
				+ "and source_name=\"" + source_name + "\"\r\n" + "order by casrn";

		
		System.out.println("sql for getExperimentalRecordsFromDB_ForSource:"+sql);
		try {
			Connection conn = SQLite_Utilities.getConnection(expRecordsDBPath);
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(sql);

			while (rs.next()) {
				ExperimentalRecord record = new ExperimentalRecord();
				SQLite_GetRecords.createRecord(rs, record);
				records.add(record);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return records;

	}
	public static ExperimentalRecords getExperimentalRecordsFromDB_Omit_Echemportal(String property, String expRecordsDBPath, boolean useKeep) {

		ExperimentalRecords records = new ExperimentalRecords();

		String sql="select * from "+tableName+" where property_name=\"" + property + "\" and source_name not like '%eChemPortal%'"
				+ "order by casrn";
		
		if (useKeep) {
			sql="select * from "+tableName+" where property_name=\"" + property + "\" and source_name not like '%eChemPortal%' and keep=\"true\" " + "\r\n"
					+ "order by casrn";
		}
		
		try {
			Connection conn = SQLite_Utilities.getConnection(expRecordsDBPath);
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(sql);

			while (rs.next()) {
				ExperimentalRecord record = new ExperimentalRecord();

				SQLite_GetRecords.createRecord(rs, record);
				records.add(record);
			}
			System.out.println(records.size() + " record for " + property + " where keep=true");

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return records;
	}
	
	
	public static ExperimentalRecords getExperimentalRecordsFromDB(String property, String expRecordsDBPath, boolean useKeep) {

		ExperimentalRecords records = new ExperimentalRecords();

		String sql="select * from "+tableName+" where property_name=\"" + property + "\""
				+ "order by casrn";
		
		if (useKeep) {
			sql="select * from "+tableName+" where property_name=\"" + property + "\" and keep=\"true\" " + "\r\n"
					+ "order by casrn";
		}
		
		try {
			Connection conn = SQLite_Utilities.getConnection(expRecordsDBPath);
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(sql);

			while (rs.next()) {
				ExperimentalRecord record = new ExperimentalRecord();

				SQLite_GetRecords.createRecord(rs, record);
				records.add(record);
			}
//			System.out.println(records.size() + " record for " + property + " where keep=true");

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return records;
	}
	
	public static ExperimentalRecords getAllExperimentalRecordsFromDBKeepEchemportal(String expRecordsDBPath, boolean useKeep) {

		ExperimentalRecords records = new ExperimentalRecords();

		String sql="select * from "+tableName+" order by casrn";
		
		if (useKeep) {
			sql="select * from "+tableName+" where keep=\"true\" " + "\r\n"
					+ "order by casrn";
//			System.out.println(sql);
		}
		
		try {
			Connection conn = SQLite_Utilities.getConnection(expRecordsDBPath);
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(sql);

			while (rs.next()) {
				ExperimentalRecord record = new ExperimentalRecord();

				SQLite_GetRecords.createRecord(rs, record);
				records.add(record);
			}
//			System.out.println(records.size() + " record for " + property + " where keep=true");

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return records;
	}
	
	ExperimentalRecords getRecordsWithProperty(String propertyName, ExperimentalRecords records) {
		ExperimentalRecords records2 = new ExperimentalRecords();

		for (ExperimentalRecord record : records) {
			if (record.property_name.contentEquals(propertyName)) {
				records2.add(record);
			}
		}
		return records2;
	}

	
	public static void main(String[] args) {
//		ExperimentalRecords records = loadFromJSON("sample.json");
//		System.out.println(records.toJSON());
//		chemicals.toJSONElement();
		
		ExperimentalRecords records = loadFromExcel("data\\experimental\\eChemPortalAPI\\eChemPortalAPI Toxicity Experimental Records.xlsx");
		
	}

}
