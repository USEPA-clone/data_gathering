package gov.epa.ghs_data_gathering.Parse.ToxVal;

import gov.epa.ghs_data_gathering.API.Chemical;
import gov.epa.ghs_data_gathering.API.ScoreRecord;

public class CreateDevelopmentalToxicityRecords {

	/* Inclusion criteria for Developmental Toxicity:
	 All routes
	 	human_eco: human health
	 	species_common: rat, mouse, rabbit, or guinea pig
	 		(another possible choice would be to just include all mammals:
	 		 species_supercategory = mammals, but those are the typical four)
	 	toxval_type: NOAEL or LOAEL
	 Oral
	   exposure_route: oral
	   toxval_units: mg/kg-day
	 Dermal
	   exposure_route: dermal
	   toxval_units: mg/kg-day
	 Inhalation
	   exposure_route: inhalation
	   toxval_units: mg/L-day
	-Leora
	*/	
	
	
	
	/* [Not doing this: Just doing if toxval_type_supercategory is "Point of Departure"
	 * rather than separating NOAEL, LOAEL, NEL, LEL.
	 * DfE is based on the NOAEL OR LOAEL.  Maybe we should omit NEL and LEL.]
	 * 
	 * toxval_type should be NOAEL or LOAEL
	 * 
	 * DfE criteria for Reproductive and Developmental Toxicity:
	 * DfE has no VH for Reproductive and Developmental Toxicity
	 * Dfe has a VL category, which will just be included in the L category here.
	 * 
	 * Route						H				M			L				(VL)
	 * Oral(mg/kg/day)				< 50		50 - 250	250 - 1000			(> 1000)
	 * Dermal (mg/kg/day)			< 100		100 - 500	> 500 - 2000		(> 2000)
	 * Inhalation (mg/L/day)
	 * 		vapor/gas				< 1			1 - 2.5		> 2.5 - 20			(> 20)
	 * 		dust/mist/fume			< 0.1		0.1 - 0.5	> 0.5 - 5			(> 5)
	 * 
	 * For inhalation, using DfE criteria for vapor/gas because that's what we did for acute toxicity.
	 * But if vapor/gas vs. dust/mist/fume is specified in ToxVal, then we should use the specific criteria.
	 * For acrylamide, data only includes oral (no inhalation data),
	 * so I need to look at the data for other chemicals.
	 * -Leora
	 * */						

		
	static void createDevelopmentalRecords(Chemical chemical, RecordToxVal r) {
		if (r.toxval_type.contentEquals("NOAEL") || r.toxval_type.contentEquals("LOAEL")) {
			if (r.toxval_units.contentEquals("mg/kg-day") && r.exposure_route.contentEquals("oral")) {
				createDevelopmentalOralRecord(chemical, r);	
			} else if(r.toxval_units.contentEquals("mg/kg-day") && r.exposure_route.contentEquals("dermal")) {
				createDevelopmentalDermalRecord(chemical, r);
			} else if(r.toxval_units.contentEquals("mg/L-day") && r.exposure_route.contentEquals("inhalation")) {
				createDevelopmentalInhalationRecord(chemical, r);
			}
		}
	}	

	
	private static void createDevelopmentalOralRecord(Chemical chemical, RecordToxVal tr) {
//		System.out.println("Creating Developmental Oral Record");
		//This is not printing.

		ScoreRecord sr = new ScoreRecord();
		sr = new ScoreRecord();
		sr.source = ScoreRecord.sourceToxVal;
		sr.sourceOriginal=tr.source;
		
		sr.route = "Oral";

		sr.valueMassOperator=tr.toxval_numeric_qualifier;
		sr.valueMass = Double.parseDouble(tr.toxval_numeric);
		sr.valueMassUnits = tr.toxval_units;

		setDevelopmentalOralScore(sr, chemical);

		sr.note=ParseToxVal.createNote(tr);
			
		chemical.scoreDevelopmental.records.add(sr);
			
	}
	
	private static void setDevelopmentalOralScore(ScoreRecord sr, Chemical chemical) {
		
		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);
							
		System.out.println(chemical.CAS+"\t"+strDose);
					
		System.out.println("****"+strDose);			
				
		if (dose < 50) {
			sr.score = ScoreRecord.scoreH;
			sr.rationale = "Oral POD" + " (" + strDose + " mg/kg) < 50 mg/kg";
		} else if (dose >= 50 && dose <= 250) {
			sr.score = ScoreRecord.scoreM;
			sr.rationale = "50 mg/kg <= Oral POD (" + strDose + " mg/kg) <=250 mg/kg";
		} else if (dose > 250) {
			sr.score = ScoreRecord.scoreL;
			sr.rationale = "Oral POD" + "(" + strDose + " mg/kg) > 250 mg/kg";
		} else { System.out.println(chemical.CAS + "\toral\t" + strDose);
				 
		}
			
		}

	private static void createDevelopmentalDermalRecord(Chemical chemical, RecordToxVal tr) {
		System.out.println("Creating Developmental Dermal Record");

		ScoreRecord sr = new ScoreRecord();
		sr = new ScoreRecord();
		sr.source = ScoreRecord.sourceToxVal;
		sr.sourceOriginal=tr.source;

		sr.valueMassOperator=tr.toxval_numeric_qualifier;
		sr.valueMass = Double.parseDouble(tr.toxval_numeric);
		sr.valueMassUnits = tr.toxval_units;

		setDevelopmentalDermalScore(sr, chemical);

		sr.note=ParseToxVal.createNote(tr);

		chemical.scoreDevelopmental.records.add(sr);

	}


	private static void setDevelopmentalDermalScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);

		if (dose < 100) {
			sr.score = ScoreRecord.scoreH;
			sr.rationale = "Dermal POD" + " (" + strDose + " mg/kg) < 100 mg/kg";
		} else if (dose >= 100 && dose <= 500) {
			sr.score = ScoreRecord.scoreM;
			sr.rationale = "100 mg/kg <= Dermal POD (" + strDose + " mg/kg) <=500 mg/kg";
		} else if (dose > 500) {
			sr.score = ScoreRecord.scoreL;
			sr.rationale = "Dermal POD" + "(" + strDose + " mg/kg) > 500 mg/kg";
			/*
			 * } else { System.out.println(chemical.CAS + "\toral\t" + strDose);
			 */
		}
	}



	private static void createDevelopmentalInhalationRecord(Chemical chemical, RecordToxVal tr){
		// System.out.println("Creating Developmental Inhalation Record");

		ScoreRecord sr = new ScoreRecord();
		sr = new ScoreRecord();
		sr.source = ScoreRecord.sourceToxVal;
		sr.sourceOriginal=tr.source;

		sr.valueMassOperator=tr.toxval_numeric_qualifier;
		sr.valueMass = Double.parseDouble(tr.toxval_numeric);
		sr.valueMassUnits = tr.toxval_units;

		setDevelopmentalDermalScore(sr, chemical);

		sr.note=ParseToxVal.createNote(tr);

		chemical.scoreDevelopmental.records.add(sr);

	}

	private void setDevelopmentalInhalationScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);

		//					System.out.println(chemical.CAS+"\t"+strDose);

		//					System.out.println("****"+strDose);					


		if (dose < 1) {
			sr.score = ScoreRecord.scoreH;
			sr.rationale = "Inhalation POD" + " (" + strDose + " mg/L) < 1 mg/L";
		} else if (dose >= 1 && dose <= 2.5) {
			sr.score = ScoreRecord.scoreM;
			sr.rationale = "1 mg/L <= Inhalation POD (" + strDose + " mg/L) <=2.5 mg/L";
		} else if (dose > 2.5) {
			sr.score = ScoreRecord.scoreL;
			sr.rationale = "Inhalation POD" + "(" + strDose + " mg/L) > 2.5 mg/L";
			/*
			 * } else { System.out.println(chemical.CAS + "\toral\t" + strDose);
			 */
		}
	}		
}
