package gov.epa.exp_data_gathering.parse.NICEATM;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.google.gson.JsonObject;

import gov.epa.api.ExperimentalConstants;
import gov.epa.exp_data_gathering.parse.ExperimentalRecord;
import gov.epa.exp_data_gathering.parse.ExperimentalRecords;
import gov.epa.exp_data_gathering.parse.Parse;

public class ParseNICEATMOld extends Parse {

	String recordTypeToParse;
	
	public ParseNICEATMOld(String recordTypeToParse) {
		sourceName = ExperimentalConstants.strSourceNICEATM;
		this.recordTypeToParse=recordTypeToParse;
		removeDuplicates=false;
		this.init();
		
		String toxNote = recordTypeToParse.toLowerCase().contains("tox") ? " Toxicity" : "";
				
		fileNameJSON_Records = sourceName +toxNote + " Original Records.json";
		fileNameFlatExperimentalRecords = sourceName +toxNote + " Experimental Records.txt";
		fileNameFlatExperimentalRecordsBad = sourceName +toxNote + " Experimental Records-Bad.txt";
		fileNameJsonExperimentalRecords = sourceName +toxNote + " Experimental Records.json";
		fileNameJsonExperimentalRecordsBad = sourceName +toxNote + " Experimental Records-Bad.json";
		fileNameExcelExperimentalRecords = sourceName +toxNote + " Experimental Records.xlsx";
						
	}
	
	@Override
	protected void createRecords() {
		Vector<JsonObject> records = RecordNICEATMOld.parseNICEATMRecordsFromExcel();
		writeOriginalRecordsToFile(records);
	}
	
	@Override
	protected ExperimentalRecords goThroughOriginalRecords() {
		ExperimentalRecords recordsExperimental=new ExperimentalRecords();
		
		try {
			String jsonFileName = jsonFolder + File.separator + fileNameJSON_Records;
			File jsonFile = new File(jsonFileName);
			
			List<RecordNICEATMOld> recordsNICEATM = new ArrayList<RecordNICEATMOld>();
			RecordNICEATMOld[] tempRecords = null;
			if (howManyOriginalRecordsFiles==1) {
				tempRecords = gson.fromJson(new FileReader(jsonFile), RecordNICEATMOld[].class);
				for (int i = 0; i < tempRecords.length; i++) {
					recordsNICEATM.add(tempRecords[i]);
				}
			} else {
				for (int batch = 1; batch <= howManyOriginalRecordsFiles; batch++) {
					String batchFileName = jsonFileName.substring(0,jsonFileName.indexOf(".")) + " " + batch + ".json";
					File batchFile = new File(batchFileName);
					tempRecords = gson.fromJson(new FileReader(batchFile), RecordNICEATMOld[].class);
					for (int i = 0; i < tempRecords.length; i++) {
						recordsNICEATM.add(tempRecords[i]);
					}
				}
			}
			
			Iterator<RecordNICEATMOld> it = recordsNICEATM.iterator();
			while (it.hasNext()) {
				RecordNICEATMOld r = it.next();
				addExperimentalRecords(r,recordsExperimental);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return recordsExperimental;
	}

	/**
	 * This method adds binary LLNA records
	 * 
	 * @param recN
	 * @param records
	 */
	private void addExperimentalRecords(RecordNICEATMOld recN, ExperimentalRecords records) {

		ExperimentalRecord er=new ExperimentalRecord();
		
		er.source_name=sourceName;		
		er.chemical_name=recN.Compound_name;
		er.casrn=recN.CASRN;
		er.smiles=recN.SMILES;
		er.property_name=ExperimentalConstants.strSkinSensitizationLLNA;
				
		er.property_value_string=recN.EC3;
				
		if (recN.EC3.contentEquals("NC")) {
			er.property_value_point_estimate_final=Double.valueOf(0);
			er.property_value_units_final="binary";
		} else if (recN.EC3.contains(">") || recN.EC3.isEmpty() || recN.EC3.contentEquals("IDR")) {
			er.keep=false;
			er.reason="Ambiguous value";	
		} else {
			try {
				double EC3=Double.parseDouble(recN.EC3);							
				er.property_value_point_estimate_final=Double.valueOf(1);
				er.property_value_units_final="binary";
				er.property_value_string+=" %";
				
			}  catch (Exception ex) {				
				er.keep=false;
				er.reason="Ambiguous value";
				System.out.println(er.casrn+"\t"+recN.EC3+"\tAmbiguous");				
			}
		}
		
		records.add(er);
	}
	
	public static void main(String[] args) {
		ParseNICEATMOld p = new ParseNICEATMOld("tox");
		p.createFiles();
	}
}
