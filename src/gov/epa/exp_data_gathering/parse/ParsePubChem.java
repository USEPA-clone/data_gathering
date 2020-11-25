package gov.epa.exp_data_gathering.parse;

import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import gov.epa.api.ExperimentalConstants;

public class ParsePubChem extends Parse {
	
	public ParsePubChem() {
		sourceName = ExperimentalConstants.strSourcePubChem;
		this.init();
		folderNameWebpages=null;
	}
	
	/**
	 * Parses JSON entries in database to RecordPubChem objects, then saves them to a JSON file
	 */
	@Override
	protected void createRecords() {
		Vector<RecordPubChem> records = RecordPubChem.parseJSONsInDatabase();
		writeOriginalRecordsToFile(records);
	}
	
	/**
	 * Reads the JSON file created by createRecords() and translates it to an ExperimentalRecords object
	 */
	@Override
	protected ExperimentalRecords goThroughOriginalRecords() {
		ExperimentalRecords recordsExperimental=new ExperimentalRecords();
		
		try {
			File jsonFile = new File(jsonFolder + File.separator + fileNameJSON_Records);
			
			RecordPubChem[] recordsPubChem = gson.fromJson(new FileReader(jsonFile), RecordPubChem[].class);
			
			for (int i = 0; i < recordsPubChem.length; i++) {
				RecordPubChem r = recordsPubChem[i];
				addExperimentalRecords(r,recordsExperimental);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return recordsExperimental;
	}
	
	private void addExperimentalRecords(RecordPubChem pcr, ExperimentalRecords recordsExperimental) {
		if (!pcr.physicalDescription.isEmpty()) {
			for (String s:pcr.physicalDescription) { addAppearanceRecord(pcr,s,recordsExperimental); }
	    }
		if (!pcr.density.isEmpty()) {
			for (String s:pcr.density) { addNewExperimentalRecord(pcr,ExperimentalConstants.strDensity,s,recordsExperimental); }
	    }
        if (!pcr.meltingPoint.isEmpty()) {
			for (String s:pcr.meltingPoint) { addNewExperimentalRecord(pcr,ExperimentalConstants.strMeltingPoint,s,recordsExperimental); }
        }
        if (!pcr.boilingPoint.isEmpty()) {
			for (String s:pcr.boilingPoint) { addNewExperimentalRecord(pcr,ExperimentalConstants.strBoilingPoint,s,recordsExperimental); }
	    }
        if (!pcr.flashPoint.isEmpty()) {
			for (String s:pcr.flashPoint) { addNewExperimentalRecord(pcr,ExperimentalConstants.strFlashPoint,s,recordsExperimental); }
	    }
        if (!pcr.solubility.isEmpty()) {
			for (String s:pcr.solubility) { addNewExperimentalRecord(pcr,ExperimentalConstants.strWaterSolubility,s,recordsExperimental); }
        }
        if (!pcr.vaporPressure.isEmpty()) {
			for (String s:pcr.vaporPressure) { addNewExperimentalRecord(pcr,ExperimentalConstants.strVaporPressure,s,recordsExperimental); }
        }
        if (!pcr.henrysLawConstant.isEmpty()) {
			for (String s:pcr.henrysLawConstant) { addNewExperimentalRecord(pcr,ExperimentalConstants.strHenrysLawConstant,s,recordsExperimental); }
        }
        if (!pcr.logP.isEmpty()) {
			for (String s:pcr.logP) { addNewExperimentalRecord(pcr,ExperimentalConstants.strLogKow,s,recordsExperimental); }
        }
        if (!pcr.pKa.isEmpty()) {
			for (String s:pcr.pKa) { addNewExperimentalRecord(pcr,ExperimentalConstants.str_pKA,s,recordsExperimental); }
        }
	}
	
	private static void addAppearanceRecord(RecordPubChem pcr,String physicalDescription,ExperimentalRecords records) {
		ExperimentalRecord er=new ExperimentalRecord();
		er.casrn=String.join("|", pcr.cas);
		er.chemical_name=pcr.iupacName;
		if (pcr.synonyms != null) { er.synonyms=pcr.synonyms; }
		er.property_name=ExperimentalConstants.strAppearance;
		er.property_value_string=physicalDescription;
		er.property_value_qualitative=physicalDescription.toLowerCase().replaceAll("colour","color").replaceAll("odour","odor").replaceAll("vapour","vapor");
		er.url="https://pubchem.ncbi.nlm.nih.gov/compound/"+pcr.cid;
		er.source_name=ExperimentalConstants.strSourcePubChem;
		er.keep = true;
		er.flag = false;
		
		records.add(er);
	}
	
	private void addNewExperimentalRecord(RecordPubChem pcr,String propertyName,String propertyValue,ExperimentalRecords recordsExperimental) {
		if (propertyValue==null) { return; }
		// Creates a new ExperimentalRecord object and sets all the fields that do not require advanced parsing
		ExperimentalRecord er=new ExperimentalRecord();
		er.casrn = String.join("|", pcr.cas);
		er.chemical_name=pcr.iupacName;
		er.smiles=pcr.smiles;
		if (pcr.synonyms != null) { er.synonyms=pcr.synonyms; }
		er.property_name=propertyName;
		er.property_value_string=propertyValue;
		er.url="https://pubchem.ncbi.nlm.nih.gov/compound/"+pcr.cid;
		er.source_name=ExperimentalConstants.strSourcePubChem;
		er.keep=true;
		
		boolean foundNumeric = false;
		propertyValue = propertyValue.replaceAll("greater than( or equal to )?", ">");
		propertyValue = propertyValue.replaceAll("less than( or equal to )?", "<");
		if (propertyName==ExperimentalConstants.strDensity) {
			foundNumeric = getDensity(er,propertyValue);
			getPressureCondition(er,propertyValue);
			getTemperatureCondition(er,propertyValue);
		} else if (propertyName==ExperimentalConstants.strMeltingPoint || propertyName==ExperimentalConstants.strBoilingPoint ||
				propertyName==ExperimentalConstants.strFlashPoint) {
			foundNumeric = getTemperatureProperty(er,propertyValue);
			getPressureCondition(er,propertyValue);
			if (propertyValue.contains("closed cup") || propertyValue.contains("c.c.")) { er.measurement_method = "closed cup"; }
		} else if (propertyName==ExperimentalConstants.strWaterSolubility) {
			foundNumeric = getWaterSolubility(er, propertyValue);
			getTemperatureCondition(er,propertyValue);
			getQualitativeSolubility(er, propertyValue);
		} else if (propertyName==ExperimentalConstants.strVaporPressure) {
			foundNumeric = getVaporPressure(er,propertyValue);
			getTemperatureCondition(er,propertyValue);
		} else if (propertyName==ExperimentalConstants.strHenrysLawConstant) {
			foundNumeric = getHenrysLawConstant(er,propertyValue);
		} else if (propertyName==ExperimentalConstants.strLogKow || propertyName==ExperimentalConstants.str_pKA) {
			foundNumeric = getLogProperty(er,propertyValue);
			getTemperatureCondition(er,propertyValue);
		}
		
		if (!propertyName.equals(ExperimentalConstants.strWaterSolubility) && propertyValue.toLowerCase().contains("decomposes")) {
			er.updateNote(ExperimentalConstants.str_dec);
		}
		if (propertyValue.toLowerCase().contains("est")) {
			er.updateNote(ExperimentalConstants.str_est);
			er.keep = false;
			}
		if ((propertyValue.toLowerCase().contains("ext") || propertyValue.toLowerCase().contains("from exp")) && !propertyValue.toLowerCase().contains("extreme")) {
			er.updateNote(ExperimentalConstants.str_ext);
			er.keep = false;
		}
		// Warns if there may be a problem with an entry
		er.flag = false;
		if (propertyValue.contains("?")) { er.flag = true; }
		
		if (foundNumeric) { er.finalizeUnits(); }
		if ((foundNumeric || er.property_value_qualitative!=null || er.note!=null) && er.keep!=false) {
			er.keep = true;
		} else {
			er.keep = false;
		}
		
		recordsExperimental.add(er);
	}

	public static void main(String[] args) {
		ParsePubChem p = new ParsePubChem();
		p.createFiles();
	}
}
