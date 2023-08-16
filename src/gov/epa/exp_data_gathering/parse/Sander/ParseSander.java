package gov.epa.exp_data_gathering.parse.Sander;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.epa.api.Chemical;
import gov.epa.api.ExperimentalConstants;
import gov.epa.exp_data_gathering.parse.ExperimentalRecord;
import gov.epa.exp_data_gathering.parse.ExperimentalRecords;
import gov.epa.exp_data_gathering.parse.LiteratureSource;
import gov.epa.exp_data_gathering.parse.Parse;
import kong.unirest.json.JSONObject;

public class ParseSander extends Parse {
	
	public ParseSander() {
		sourceName = ExperimentalConstants.strSourceSander;
		this.init();
	}
	@Override
	protected void createRecords() {
		Vector<RecordSander> records = RecordSander.parseWebpagesInDatabase();
		writeOriginalRecordsToFile(records);
	}
	
	@Override
	protected ExperimentalRecords goThroughOriginalRecords() {
		ExperimentalRecords recordsExperimental=new ExperimentalRecords();
		
		try {
			String jsonFileName = jsonFolder + File.separator + fileNameJSON_Records;
			File jsonFile = new File(jsonFileName);
			
			List<RecordSander> recordsSander = new ArrayList<RecordSander>();
			RecordSander[] tempRecords = null;
			if (howManyOriginalRecordsFiles==1) {
				tempRecords = gson.fromJson(new FileReader(jsonFile), RecordSander[].class);
				for (int i = 0; i < tempRecords.length; i++) {
					recordsSander.add(tempRecords[i]);
				}
			} else {
				for (int batch = 1; batch <= howManyOriginalRecordsFiles; batch++) {
					String batchFileName = jsonFileName.substring(0,jsonFileName.indexOf(".")) + " " + batch + ".json";
					File batchFile = new File(batchFileName);
					tempRecords = gson.fromJson(new FileReader(batchFile), RecordSander[].class);
					for (int i = 0; i < tempRecords.length; i++) {
						recordsSander.add(tempRecords[i]);
					}
				}
			}
			
			Iterator<RecordSander> it = recordsSander.iterator();
			while (it.hasNext()) {
				RecordSander r = it.next();
				addExperimentalRecords(r,recordsExperimental);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return recordsExperimental;
	}
	
	
	void makeFullRefTxt() {
		BufferedWriter bw = null;
		try {
			File jsonFile = new File(jsonFolder + File.separator + fileNameJSON_Records);
			File txtfile = new File(mainFolder + File.separator + "General" + "SanderReferences.txt");
			FileWriter fw = new FileWriter(txtfile);
			bw = new BufferedWriter(fw);
			RecordSander[] recordsSander = gson.fromJson(new FileReader(jsonFile), RecordSander[].class);
			
			for (int i = 0; i < recordsSander.length; i++) { // recordsSander.length
				RecordSander rec = recordsSander[i];
				String temp = Gabrieldemo(rec);
				bw.write(temp);
			}
			
			bw.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
	/**
	 * populates experimentalrecord fields with data from the recordSander object.
	 * @param rs
	 * @param records
	 */
	private void addExperimentalRecords(RecordSander rs,ExperimentalRecords records) {
		String CAS = rs.CASRN;
		for (int i = 0; i < rs.recordCount; i++) {
			if (!(rs.hcp.get(i).isBlank())) {
				ExperimentalRecord er = new ExperimentalRecord();
				er.date_accessed = rs.date_accessed;
				er.keep = true;
				er.url = rs.url;
				er.casrn = CAS;
				if (er.casrn.contains("???")) {
					er.casrn = "";
				}
				
				// er.reference = Gabrieldemo(rs);
				
				er.property_value_string = rs.hcp.get(i) + " " + ExperimentalConstants.str_mol_m3_atm;
				er.chemical_name = rs.chemicalName.replace("? ? ? ", "");
				er.property_name = ExperimentalConstants.strHenrysLawConstant;
				String propertyValue = rs.hcp.get(i);
				er.property_value_units_original = ExperimentalConstants.str_mol_m3_atm;
				er.property_value_units_final = ExperimentalConstants.str_atm_m3_mol;
				getnumericalhcp(er, propertyValue);
				// below converts Sander's weird inverted units to atm*m3/mol
				if (!(er.property_value_point_estimate_original == null)) {
					er.property_value_point_estimate_final = 1/(er.property_value_point_estimate_original*101325);
				}
				
				
				er.temperature_C = (double)25;

				/*
				 * CR: (Wednesday August 18) temperature and pressure information, if it is given at all is found in the notes.
				er.pressure_mmHg = "760";
				*/
				
				
				er.source_name=ExperimentalConstants.strSourceSander;
				er.original_source_name = rs.referenceAbbreviated.get(i);
				
				LiteratureSource ls=new LiteratureSource();
				er.literatureSource=ls;
				ls.citation=rs.referenceFull.get(i);
				ls.name=ls.citation;
				
				//TODO store full citation from reference (it's in the HTML)
				
				getnotes(er,rs.type.get(i));
				records.add(er);
			}
		}
	}

	public static void main(String[] args) {
		ParseSander p = new ParseSander();
		p.createFiles();
		p.makeFullRefTxt();
		
	}


	/**
	 * converts strings of the form 5.8�10-4 to the correct value as a double.
	 * @param er
	 * @param propertyValue
	 */
	public static void getnumericalhcp(ExperimentalRecord er, String propertyValue) {
		Matcher sanderhcpMatcher = Pattern.compile("([0-9]*\\.?[0-9]+)(\\�10(\\-)?([0-9]+))?").matcher(propertyValue);
		if (sanderhcpMatcher.find()) {
			String strMantissa = sanderhcpMatcher.group(1);
			String strNegMagnitude = sanderhcpMatcher.group(3);
			String strMagnitude = sanderhcpMatcher.group(4);
			if (!(strMagnitude == null)){
				if (!(strNegMagnitude == null)) { // ? corresponds to negative magnitude (e.g. 3.4 * 10^-4), otherwise positive
					Double mantissa = Double.parseDouble(strMantissa.replaceAll("\\s",""));
					Double magnitude =  Double.parseDouble(strMagnitude.replaceAll("\\s","").replaceAll("\\+", ""));
					er.property_value_point_estimate_original = mantissa*Math.pow(10, (-1)*magnitude);
				} else {
					Double mantissa = Double.parseDouble(strMantissa.replaceAll("\\s",""));
					Double magnitude =  Double.parseDouble(strMagnitude.replaceAll("\\s","").replaceAll("\\+", ""));
					er.property_value_point_estimate_original = mantissa*Math.pow(10, magnitude);
				}
			}
			else {
				er.property_value_point_estimate_original = Double.parseDouble(strMantissa.replaceAll("\\s",""));
			}
		}
	}
	
	public static String Gabrieldemo(RecordSander rs) {
		
		Vector<String> Referenceshort = rs.referenceAbbreviated;
		Vector<String> Referencelong = rs.referenceFull;
		
		String output = "";
		
		for (String Reference:Referenceshort) {
		Pattern p = Pattern.compile("(([^ ]+) .*?)([^\\s]+$)");
		Matcher m = p.matcher(Reference);
		
		if (m.find()) {
			String name = m.group(2);
			String year = m.group(3);
			for (int i = 0; i < Referencelong.size(); i++) {
				if ((Referencelong.get(i).contains(name)) && (Referencelong.get(i).contains(year))) {
					output = output + rs.chemicalName.replace("? ? ? ", "") + "|" + Reference + "|" + Referencelong.get(i) + "\n";
					return Referencelong.get(i);
				}
			}
		}
		}
	return null;	
	}

	/**
	 * Keeps the Henry's law constants that were derived by measurement, VP/AS, literature, or citation.
	 * @param er
	 * @param type
	 */
	public static void getnotes(ExperimentalRecord er, String type) {
		/**
		 * Table entries are sorted according to reliability of the data, listing the
		 * most reliable type first: L) literature review, M) measured, V) VP/AS = vapor
		 * pressure/aqueous solubility, R) recalculation, T) thermodynamical
		 * calculation, X) original paper not available, C) citation, Q) QSPR, E)
		 * estimate, ?) unknown, W) wrong. See Section 3.1 of Sander (2015) for further
		 * details.
		 * 
		 * TMM 2020-12-23: Keeping L, M, V (most reliable)
		 * 
		 */
		if (type.contains("L")) {
			er.keep=true;
			er.note = "literature review";
		} else if (type.contains("M")) {
			er.note = "measured";
			er.keep = true;
		} else if (type.contains("V")) {
			er.note = "VP/AS = vapor pressure/aqueous solubility";
			er.keep = true;
		} else if (type.contains("R")) {
			er.note = "recalculation";
			er.keep = false;
			er.reason = "recalculation";
		} else if (type.contains("T")) {
			er.note = "thermodynamical calculation";
			er.keep = false;
			er.reason= "thermodynamical calculation";
		} else if (type.contains("X")) {
			er.reason = "original paper not available";
			er.keep = false;
		} else if (type.contains("C")) {
			er.note = "citation";
			er.keep = false;		
		} else if (type.contains("Q")) {
			er.note="QSPR";
			er.reason = "QSPR";
			er.keep = false;
		} else if (type.contains("E")) {
			er.note="estimate";
			er.reason = "estimate";
			er.keep=false;
		} else if (type.contains("?")) {
			er.note = "unknown";
			er.reason = "unknown";
			er.keep=false;
		} else if (type.contains("W")) {
			er.note = "wrong";
			er.reason = "wrong";
			er.keep=false;
		} else {
			er.note = "";
			er.keep = false;
		}
	}

}
