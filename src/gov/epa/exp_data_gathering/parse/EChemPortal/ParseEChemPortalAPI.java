package gov.epa.exp_data_gathering.parse.EChemPortal;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
//import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.epa.api.ExperimentalConstants;
import gov.epa.eChemPortalAPI.Query.APIConstants;
import gov.epa.exp_data_gathering.parse.ExperimentalRecord;
import gov.epa.exp_data_gathering.parse.ExperimentalRecords;
import gov.epa.exp_data_gathering.parse.Parse;
import gov.epa.exp_data_gathering.parse.ParseUtilities;
import gov.epa.exp_data_gathering.parse.PressureCondition;
import gov.epa.exp_data_gathering.parse.TemperatureCondition;
import gov.epa.exp_data_gathering.parse.TextUtilities;

/**
 * Parses downloaded results from eChemPortal API into RecordEChemPortal API objects and translates them to ExperimentalRecords
 * @author GSINCL01 (Gabriel Sinclair)
 *
 */
public class ParseEChemPortalAPI extends Parse {
	
	public ParseEChemPortalAPI() {
		sourceName = ExperimentalConstants.strSourceEChemPortalAPI;
		this.init();
		fileNameSourceExcel=null;
		folderNameWebpages=null;
		folderNameExcel=null;
	}
	
//	private void benchmarkParse(int reps) {
//		double[] results = new double[reps];
//		for (int i = 0; i < reps; i++) {
//			long start = System.currentTimeMillis();
//			RecordEChemPortalAPI.parseResultsInDatabase();
//			long end = System.currentTimeMillis();
//			results[i] = (double) (end-start)/1000.0;
//		}
//		System.out.println("Time to parse all records (s): "+Arrays.toString(results));
//	}
	
	/**
	 * Parses JSON entries in database to RecordPubChem objects, then saves them to a JSON file
	 */
	@Override
	protected void createRecords() {
		List<RecordEChemPortalAPI> records = RecordEChemPortalAPI.parseResultsInDatabase();
		writeOriginalRecordsToFile(new Vector<RecordEChemPortalAPI>(records));
	}
	
	/**
	 * Reads the JSON file created by createRecords() and translates it to an ExperimentalRecords object
	 */
	@Override
	protected ExperimentalRecords goThroughOriginalRecords() {
		ExperimentalRecords recordsExperimental=new ExperimentalRecords();
		
		try {
			String jsonFileName = jsonFolder + File.separator + fileNameJSON_Records;
			File jsonFile = new File(jsonFileName);
			
			List<RecordEChemPortalAPI> recordsEChemPortalAPI = new ArrayList<RecordEChemPortalAPI>();
			RecordEChemPortalAPI[] tempRecords = null;
			if (howManyOriginalRecordsFiles==1) {
				tempRecords = gson.fromJson(new FileReader(jsonFile), RecordEChemPortalAPI[].class);
				for (int i = 0; i < tempRecords.length; i++) {
					recordsEChemPortalAPI.add(tempRecords[i]);
				}
			} else {
				for (int batch = 1; batch <= howManyOriginalRecordsFiles; batch++) {
					String batchFileName = jsonFileName.substring(0,jsonFileName.indexOf(".")) + " " + batch + ".json";
					File batchFile = new File(batchFileName);
					tempRecords = gson.fromJson(new FileReader(batchFile), RecordEChemPortalAPI[].class);
					for (int i = 0; i < tempRecords.length; i++) {
						recordsEChemPortalAPI.add(tempRecords[i]);
					}
				}
			}
			
			Iterator<RecordEChemPortalAPI> it = recordsEChemPortalAPI.iterator();
			while (it.hasNext()) {
				RecordEChemPortalAPI r = it.next();
				addExperimentalRecords(r,recordsExperimental);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return recordsExperimental;
	}
	
	/**
	 * Creates and adds the ExperimentalRecord corresponding to a given RecordEChemPortalAPI object
	 * @param r			The RecordEChemPortalAPI object to translate
	 * @param records	The ExperimentalRecords to add created record to
	 */
	private void addExperimentalRecords(RecordEChemPortalAPI r, ExperimentalRecords records) {
		ExperimentalRecord er = new ExperimentalRecord();
		er.source_name = sourceName;
		er.url = r.endpointURL;
		er.original_source_name = r.participantAcronym;
		er.date_accessed = r.dateAccessed;
		er.reliability = r.reliability;
		er.keep = true;
		
		if (!r.name.equals("-") && !r.name.contains("unnamed")) {
			er.chemical_name = r.name;
		}
		
		if (r.numberType!=null) {
			switch (r.numberType) {
			case "CAS Number":
				er.casrn = r.number;
				break;
			case "EC Number":
				er.einecs = r.number;
				break;
			}
		}
		
		if (r.value==null) { return; }
		
		er.property_value_string = "Value: "+r.value;
		switch (r.endpointKind) {
		case APIConstants.meltingPoint:
			er.property_name = ExperimentalConstants.strMeltingPoint;
			ParseUtilities.getTemperatureProperty(er,r.value);
			break;
		case APIConstants.boilingPoint:
			er.property_name = ExperimentalConstants.strBoilingPoint;
			ParseUtilities.getTemperatureProperty(er,r.value);
			break;
		case APIConstants.flashPoint:
			er.property_name = ExperimentalConstants.strFlashPoint;
			ParseUtilities.getTemperatureProperty(er,r.value);
			break;
		case APIConstants.density:
			er.property_name = ExperimentalConstants.strDensity;
			// CR 9/8/2021 fixes strings of the form Value: 0.72 (corresponds to D4(20)); Temperature: 22 �C" being read as 20 for density
			if (r.value.contains("corresponds")) { 
				String temp = r.value;
				temp = temp.replaceAll("corresponds to D4(.*?)(\\d+)(.*?)", "");
				ParseUtilities.getDensity(er, temp);
			} else {
			
			ParseUtilities.getDensity(er,r.value);
			}
			break;
		case APIConstants.vaporPressure:
			er.property_name = ExperimentalConstants.strVaporPressure;
			ParseUtilities.getVaporPressure(er,r.value);
			break;
		case APIConstants.partitionCoefficient:
			er.property_name = ExperimentalConstants.strLogKOW;
			ParseUtilities.getLogProperty(er,r.value);
			if (!r.value.contains("log Pow")) {
				er.flag = true;
				er.reason = "Possible non-log Pow value";
			}
			break;
		case APIConstants.waterSolubility:
			er.property_name = ExperimentalConstants.strWaterSolubility;
			ParseUtilities.getWaterSolubility(er,r.value,sourceName);
			break;
		case APIConstants.dissociationConstant:
			er.property_name = ExperimentalConstants.str_pKA;
			ParseUtilities.getLogProperty(er,r.value);
			break;
		case APIConstants.henrysLawConstant:
			er.property_name = ExperimentalConstants.strHenrysLawConstant;
			ParseUtilities.getHenrysLawConstant(er,r.value);
			break;
		}
		
		if (r.pressure!=null) {
			PressureCondition.getPressureCondition(er,r.pressure,sourceName);
			er.property_value_string = er.property_value_string + "; Pressure: " + r.pressure;
		}
		
		if (r.temperature!=null) {
			try {
				er.temperature_C = Double.parseDouble(r.temperature);
			} catch (NumberFormatException ex) {
				TemperatureCondition.getTemperatureCondition(er,r.temperature);
			}
			er.property_value_string = er.property_value_string + "; Temperature: " + r.temperature;
		}
		
		// Handles all kinds of weird pH formatting
		if (r.pH!=null) {
			String pHStr = r.pH;
			er.property_value_string = er.property_value_string + "; pH: " + pHStr;
			boolean foundpH = false;
			try {
				double[] range = TextUtilities.extractFirstDoubleRangeFromString(pHStr,pHStr.length());
				er.pH = range[0]+"-"+range[1];
				foundpH = true;
			} catch (Exception ex) { }
			if (!foundpH) {
				try {
					double[] range = TextUtilities.extractAltFormatRangeFromString(pHStr,pHStr.length());
					er.pH = range[0]+"-"+range[1];
					foundpH = true;
				} catch (Exception ex) { }
			}
			if (!foundpH) {
				try {
					Matcher caMatcher = Pattern.compile(".*?(ca. )?([-]?[ ]?[0-9]*\\.?[0-9]+)( ca. )([-]?[ ]?[0-9]*\\.?[0-9]+)").matcher(pHStr);
					if (caMatcher.find()) {
						String numQual = caMatcher.group(1).isBlank() ? "" : "~";
						er.pH = numQual+Double.parseDouble(caMatcher.group(2))+"~"+Double.parseDouble(caMatcher.group(4));
						foundpH = true;
					}
				} catch (Exception ex) { }
			}
			// Handles J-CHECK entries with multiple discrete pH values
			if (!foundpH && pHStr.contains(",") && !pHStr.endsWith(",")) {
				er.pH = pHStr;
				foundpH = true;
			}
			if (!foundpH) {
				try {
					double pHDouble = TextUtilities.extractLastDoubleFromString(pHStr,pHStr.length());
					String pHDoubleStr = Double.toString(pHDouble);
					String numQual = "";
					if (pHDouble >= 0 && pHDouble < 1) {
						numQual = TextUtilities.getNumericQualifier(pHStr,pHStr.indexOf("0"));
					} else {
						numQual = TextUtilities.getNumericQualifier(pHStr,pHStr.indexOf(pHDoubleStr.charAt(0)));
					}
					er.pH = numQual+pHDoubleStr;
					foundpH = true;
				} catch (Exception ex) { }
			}
		}
		
		uc.convertRecord(er);
		
		if (er.keep && !ParseUtilities.hasIdentifiers(er)) {
			er.keep = false;
			er.reason = "No identifiers";
		} else if (er.keep && er.property_value_point_estimate_final==null && er.property_value_min_final==null && er.property_value_max_final==null) {
			er.keep = false;
			er.reason = "Unhandled units";
		}
		
		records.add(er);
	}

	public static void main(String[] args) {
		ParseEChemPortalAPI p = new ParseEChemPortalAPI();
		p.createFiles();
	}
}
