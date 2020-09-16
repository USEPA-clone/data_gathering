package gov.epa.ghs_data_gathering.Parse.ToxVal.ParseTable_toxval;

import java.util.ArrayList;

import gov.epa.api.Chemical;
import gov.epa.api.ScoreRecord;

public class CreateOrganOrSystemicToxRecords {


	/* I want to convert this Stata code to Java code:

	 gen study_dur_in_days=.
	 replace study_dur_in_days=study_duration_value if study_duration_units=="day"
	 replace study_dur_in_days=study_duration_value*7 if study_duration_units=="week"
	 replace study_dur_in_days=study_duration_value*30 if study_duration_units=="month"
	 replace study_dur_in_days=study_duration_value*365 if study_duration_units=="year"
	 replace study_dur_in_days=study_duration_value/24 if study_duration_units=="hour"
	 replace study_dur_in_days=study_duration_value/1440 if study_duration_units=="minute"

	 Here is my attempt...

	 */

	public static void createDurationRecord(Chemical chemical, RecordToxVal tr) {

		double study_dur_in_days=-1.0;
		/* I think there is no way to make a variable blank so I made it -1, which is not ideal. */

		double study_duration_value = Double.parseDouble(tr.study_duration_value);
		/* Do I need to do this to change it from whatever format it was in into double? */

		if (tr.study_duration_units.contentEquals("day")) {
			study_dur_in_days=study_duration_value;
		} else if (tr.study_duration_units.contentEquals("week")) {
			study_dur_in_days=study_duration_value*7.0;
		} else if (tr.study_duration_units.contentEquals("month")) {
			study_dur_in_days=study_duration_value*30.0;
		} else if (tr.study_duration_units.contentEquals("year")) {
			study_dur_in_days=study_duration_value*365.0;
		} else if (tr.study_duration_units.contentEquals("hour")) {
			study_dur_in_days=study_duration_value/24.0;
		} else if (tr.study_duration_units.contentEquals("minute")) {
			study_dur_in_days=study_duration_value/1440.0;
		} else if (tr.study_duration_units.contentEquals("-")) {
			return;
		} else {
			System.out.println("unknown units="+tr.study_duration_units);
			return;
		}

		
		if(!CreateAcuteMammalianToxicityRecords.isOkMammalianSpecies(tr)) return;

		if (!tr.toxval_type.contentEquals("NOAEL") && !tr.toxval_type.contentEquals("LOAEL")) {
			return;//not a valid record for systemic tox
		}

		ScoreRecord sr=createScoreRecord(chemical, tr);//create generic score record

		sr.duration=study_dur_in_days;
		sr.durationUnits="days";

		//		System.out.println("duration="+sr.duration+" days");


		if (tr.exposure_route.contentEquals("oral") && tr.toxval_units.contentEquals("mg/kg-day")) {
			/* if (study_dur_in_days <= 91.0 && study_dur_in_days >= 89.0) { 
			   Broadening the range to be more inclusive (90 + or - 5).
			   Also switching the order for more logical reading. */			
			if (study_dur_in_days >= 85.0 && study_dur_in_days <= 95.0)	{
				setNinetyOralScore(sr, chemical);
			} else if (study_dur_in_days >= 40.0 && study_dur_in_days <= 50.0) {
				/*All three of these time categories now have a range of 10. */		
				setFortyFiftyOralScore(sr, chemical);
				/* } else if (study_dur_in_days <= 31.0 && study_dur_in_days >= 27.0) {
			   Broadening the range to be more inclusive (28 + or - 5)*/
			} else if (study_dur_in_days >= 23.0 && study_dur_in_days <= 33.0) {
				setTwentyEightOralScore(sr,chemical);
			}


		} else if (tr.exposure_route.contentEquals("dermal")  && tr.toxval_units.contentEquals("mg/kg-day")) {
			/* if (study_dur_in_days <= 91.0 && study_dur_in_days >= 89.0) {
				Broadening the range to be more inclusive (90 + or - 5).
				Also switching the order for more logical reading. */
			if (study_dur_in_days >= 85.0 && study_dur_in_days <= 95.0) {
				/*Got error when tried to use "=". It said "<=" was expected.*/			
				setNinetyDermalScore(sr, chemical);
			} else if (study_dur_in_days >= 40.0 && study_dur_in_days <= 50.0) {
				setFortyFiftyDermalScore(sr, chemical);
				/* } else if (study_dur_in_days <= 31.0 && study_dur_in_days >= 27.0) {
			Broadening the range to be more inclusive (28 + or - 5)*/
			} else if (study_dur_in_days >= 23.0 && study_dur_in_days <= 33.0) {
				setTwentyEightDermalScore(sr,chemical);
			}

		} else if (tr.exposure_route.contentEquals("inhalation")  && tr.toxval_units.contentEquals("mg/L-day")) {
			/* if (study_dur_in_days <= 91.0 && study_dur_in_days >= 89.0) {
			Broadening the range to be more inclusive (90 + or - 5).
			Also switching the order for more logical reading. */
			if (study_dur_in_days >= 85.0 && study_dur_in_days <= 95.0) {
				/*Got error when tried to use "=". It said "<=" was expected.*/			
				setNinetyInhalationScore(sr, chemical);
			} else if (study_dur_in_days >= 40.0 && study_dur_in_days <= 50.0) {
				setFortyFiftyInhalationScore(sr, chemical);
				/* } else if (study_dur_in_days <= 31.0 && study_dur_in_days >= 27.0) {
			Broadening the range to be more inclusive (28 + or - 5)*/
			} else if (study_dur_in_days >= 23.0 && study_dur_in_days <= 33.0) {
				setTwentyEightInhalationScore(sr,chemical);
			}

		}


		if (tr.study_type.contentEquals("single limit dose") ||
				tr.study_duration_class.contentEquals("single dose")) {
			if (tr.toxval_units.contentEquals("mg/kg") &&
					tr.exposure_route.contentEquals("oral")) {
				//	study_dur_in_days >= 0.0 && study_dur_in_days <= 1.0) {		
				setSingleDoseOralScore(sr,chemical);	
			} else if (tr.toxval_units.contentEquals("mg/kg") &&
					tr.exposure_route.contentEquals("dermal")) {	
				setSingleDoseDermalScore(sr,chemical);
			} else if ((tr.toxval_units.contentEquals("mg/L") ||
					tr.toxval_units.contentEquals("mg/m3")) &&
					tr.exposure_route.contentEquals("inhalation")) {
				setSingleDoseInhalationScore(sr,chemical);
			}
		}


		if (sr.score==null) return;

		//		System.out.println(sr.scoreToInt());


		if(tr.study_type.toLowerCase().contains("neuro") ||
				isNeuroCriticalEffect(tr)) {
			handleNeuro(chemical, tr, sr);
		} else
			handleOther(chemical, tr, sr);
	}

	private static void handleNeuro(Chemical chemical, RecordToxVal tr, ScoreRecord sr) {
		if (tr.study_type.contentEquals("single limit dose") ||
				tr.study_duration_class.contentEquals("single dose")) {	
			chemical.scoreNeurotoxicity_Single_Exposure.records.add(sr);
		} else; {
			chemical.scoreNeurotoxicity_Repeat_Exposure.records.add(sr);
		}
	}

	private static void handleOther(Chemical chemical, RecordToxVal tr, ScoreRecord sr) {
		if (tr.study_type.contentEquals("single limit dose") ||
				tr.study_duration_class.contentEquals("single dose")) {	
			chemical.scoreSystemic_Toxicity_Single_Exposure.records.add(sr);
		} else; {
			chemical.scoreSystemic_Toxicity_Repeat_Exposure.records.add(sr);
		}
	}





	//		} else {
	//			//TODO- are there any records where this happends?
	//			chemical.scoreSystemic_Toxicity_Single_Exposure.records.add(sr);
	//		}


	public static boolean isNeuroCriticalEffect(RecordToxVal tr) {
		String ce=tr.critical_effect;
		//more keywords added
		if (ce.contains("ataxia") || 
				ce.contains("brain") ||
				ce.contains("cholinesterase") ||
				ce.contains("CNS") ||
				ce.contains("COMA") ||
				ce.contains("convulsions") ||
				ce.contains("decreased retention (memory)") ||
				ce.contains("demyelination") ||
				ce.contains("HALLUCINATIONS") ||
				ce.contains("headache, dizziness, weakness") ||
				ce.contains("impaired reflex") ||
				ce.contains("jerking movements") ||
				ce.contains("motor and sensory function") ||
				ce.contains("nerve") ||
				ce.contains("NERVOUS SYSTEM") ||
				ce.contains("neuritis")||
				ce.contains("neuro")||
				ce.contains("paralysis") ||
				ce.contains("Psychomotor") ||
				ce.contains("seizure") ||
				ce.contains("SENSE ORGANS") ||
				ce.contains("Spinal cord") ||
				ce.contains("TOXIC PSYCHOSIS") ||
				ce.contains("tremor") ) {
			return true;
		} else {
			return false;
		}
	}






	private static ScoreRecord createScoreRecord(Chemical chemical, RecordToxVal tr) {
		ScoreRecord sr = ParseToxVal.saveToxValInfo(tr);
		return sr;
	}



	// 90 day oral seems to be creating duplicates.

	private static void setNinetyOralScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 100) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "90 day Oral NOAEL or LOAEL ( > " + strDose + " mg/kg-day) > 100 mg/kg-day";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "90 day Oral NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=10) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "90 day Oral NOAEL or LOAEL ( < " + strDose + " mg/kg-day) < 10 mg/kg-day";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "90 day Oral NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {


			if (dose < 10) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "90 day Oral NOAEL or LOAEL" + " (" + strDose + " mg/kg-day) < 10 mg/kg-day";
			} else if (dose >= 10 && dose <= 100) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "10 mg/kg-day < 90 day Oral NOAEL or LOAEL (" + strDose + " mg/kg-day) <= 100 mg/kg-day";
			} else if (dose > 100) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "90 day Oral NOAEL or LOAEL" + "(" + strDose + " mg/kg-day) >  100 mg/kg-day";
			} else { 
				System.out.println(chemical.CAS + "\tNinetyOral\t" + strDose);
			}
		}
	}



	private static void setFortyFiftyOralScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);	

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 200) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "45 day Oral NOAEL or LOAEL ( > " + strDose + " mg/kg-day) > 200 mg/kg-day";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "45 day Oral NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=20) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "45 day Oral NOAEL or LOAEL ( < " + strDose + " mg/kg-day) < 20 mg/kg-day";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "45 day Oral NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {


			if (dose < 20) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "45 day Oral NOAEL or LOAEL" + " (" + strDose + " mg/kg-day) < mg/kg-day";
			} else if (dose >= 20 && dose <= 200) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "45 day Oral 20 mg/kg-day < NOAEL or LOAEL (" + strDose + " mg/kg-day) <= 200 mg/kg-day";
			} else if (dose > 200) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "45 day Oral NOAEL or LOAEL" + "(" + strDose + " mg/kg-day) >  100 mg/kg-day";
			} else { 
				System.out.println(chemical.CAS + "\tFortyFiftyOral\t" + strDose);
			}
		}
	}

	private static void setTwentyEightOralScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);	

		if (sr.valueMassOperator.equals(">")) {

			if (dose >=300) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "28 day Oral NOAEL or LOAEL ( > " + strDose + " mg/kg-day) > 300 mg/kg-day";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "28 day Oral NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=30) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "28 day Oral NOAEL or LOAEL ( < " + strDose + " mg/kg-day) < 30 mg/kg-day";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "28 day Oral NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {

			if (dose < 30) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "28 day Oral NOAEL or LOAEL" + " (" + strDose + " mg/kg-day) < 30 mg/kg-day";
			} else if (dose >= 30 && dose <= 300) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "30 mg/kg-day < 28 day Oral NOAEL or LOAEL (" + strDose + " mg/kg-day) <= 300 mg/kg-day";
			} else if (dose > 300) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "28 day Oral NOAEL or LOAEL" + "(" + strDose + " mg/kg-day) >  300 mg/kg-day";
			} else { 
				System.out.println(chemical.CAS + "\tTwentyEightOral\t" + strDose);
			}
		}
	}


	private static void setNinetyDermalScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);	

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 200) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "90 day Dermal NOAEL or LOAEL ( > " + strDose + " mg/kg-day) > 200 mg/kg-day";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "90 day Dermal NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=20) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "90 day Dermal NOAEL or LOAEL ( < " + strDose + " mg/kg-day) < 20 mg/kg-day";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "90 day Dermal NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {



			if (dose < 20) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "90 day Dermal NOAEL or LOAEL" + " (" + strDose + " mg/kg-day) < 20 mg/kg-day";
			} else if (dose >= 20 && dose <= 200) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "20 mg/kg-day < 90 day Dermal NOAEL or LOAEL (" + strDose + " mg/kg-day) <= 200 mg/kg-day";
			} else if (dose > 200) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "90 day Dermal NOAEL or LOAEL" + "(" + strDose + " mg/kg-day) >  200 mg/kg-day";
			} else { 
				System.out.println(chemical.CAS + "\tNinetyDermal\t" + strDose);

			}
		}
	}

	private static void setFortyFiftyDermalScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 400) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "45 day Dermal NOAEL or LOAEL ( > " + strDose + " mg/kg-day) > 400 mg/kg-day";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "45 day Dermal NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=40) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "45 day Dermal NOAEL or LOAEL ( < " + strDose + " mg/kg-day) < 40 mg/kg-day";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "45 day Dermal NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {


			if (dose < 40) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "45 day Dermal NOAEL or LOAEL" + " (" + strDose + " mg/kg-day) < 40 mg/kg-day";
			} else if (dose >= 40 && dose <= 400) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "40 mg/kg-day < 45 day Dermal NOAEL or LOAEL (" + strDose + " mg/kg-day) <= 400 mg/kg-day";
			} else if (dose > 400) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "45 day Dermal NOAEL or LOAEL" + "(" + strDose + " mg/kg-day) >  400 mg/kg-day";
			} else { 
				System.out.println(chemical.CAS + "\tFortyFiftyDermal\t" + strDose);
			}
		}
	}


	private static void setTwentyEightDermalScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 600) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "28 day Dermal NOAEL or LOAEL ( > " + strDose + " mg/kg-day) > 600 mg/kg-day";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "28 day Dermal NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=60) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "28 day Dermal NOAEL or LOAEL ( < " + strDose + " mg/kg-day) < 60 mg/kg-day";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "28 day Dermal NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {


			if (dose < 60) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "28 day Dermal NOAEL or LOAEL" + " (" + strDose + " mg/kg-day) < 60 mg/kg-day";
			} else if (dose >= 60 && dose <= 600) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "60 mg/kg-day < 28 day Dermal NOAEL or LOAEL (" + strDose + " mg/kg-day) <= 600 mg/kg-day";
			} else if (dose > 600) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "28 day Dermal NOAEL or LOAEL" + "(" + strDose + " mg/kg-day) >  600 mg/kg-day";
			} else { 
				System.out.println(chemical.CAS + "\tTwentyEightDermal\t" + strDose);
			}
		}
	}


	private static void setNinetyInhalationScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);	

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 1) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "90 day Inhalation NOAEL or LOAEL ( > " + strDose + " mg/L-day) > 1 mg/L-day";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "90 day Inhalation NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=0.2) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "90 day Inhalation NOAEL or LOAEL ( < " + strDose + " mg/L-day) < 0.2 mg/L-day";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "90 day Inhalation NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {



			if (dose < 0.2) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "90 day Inhalation NOAEL or LOAEL" + " (" + strDose + " mg/L-day) < 0.2 mg/L-day";
			} else if (dose >= 0.2 && dose <= 1) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "0.2 mg/L-day < 90 day Inhalation NOAEL or LOAEL (" + strDose + " mg/L-day) <= 1 mg/L-day";
			} else if (dose > 1) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "90 day Inhalation NOAEL or LOAEL" + "(" + strDose + " mg/L-day) >  1 mg/L-day";
			} else { 
				System.out.println(chemical.CAS + "\tNinetyInhalation\t" + strDose);
			}
		}
	}


	private static void setFortyFiftyInhalationScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);	

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 2) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "45 day Inhalation NOAEL or LOAEL ( > " + strDose + " mg/L-day) > 2 mg/L-day";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "45 day Inhalation NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=0.4) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "45 day Inhalation NOAEL or LOAEL ( < " + strDose + " mg/L-day) < 0.4 mg/L-day";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "45 day Inhalation NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {


			if (dose < 0.4) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "45 day Inhalation NOAEL or LOAEL" + " (" + strDose + " mg/L-day) < 0.4 mg/L-day";
			} else if (dose >= 0.4 && dose <= 2) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "0.4 mg/L-day < 45 day Inhalation NOAEL or LOAEL (" + strDose + " mg/L-day) <= 2 mg/L-day";
			} else if (dose > 2) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "45 day Inhalation NOAEL or LOAEL" + "(" + strDose + " mg/L-day) >  2 mg/L-day";
			} else { 
				System.out.println(chemical.CAS + "\tFortyFiftyInhalation\t" + strDose);
			}
		}
	}


	private static void setTwentyEightInhalationScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);	

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 3) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "28 day Inhalation NOAEL or LOAEL ( > " + strDose + " mg/L-day) > 3 mg/L-day";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "28 day Inhalation NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=0.6) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "28 day Inhalation NOAEL or LOAEL ( < " + strDose + " mg/L-day) < 0.6 mg/L-day";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "28 day Inhalation NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {


			if (dose < 0.6) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "28 day Inhalation NOAEL or LOAEL" + " (" + strDose + " mg/L-day) < 0.6 mg/L-day";
			} else if (dose >= 0.6 && dose <= 3) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "0.6 mg/L-day < 28 day Inhalation NOAEL or LOAEL (" + strDose + " mg/L-day) <= 3 mg/L-day";
			} else if (dose > 3) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "28 day Inhalation NOAEL or LOAEL" + "(" + strDose + " mg/L-day) >  3 mg/L-day";
			} else { 
				System.out.println(chemical.CAS + "\tTwentyEightInhalation\t" + strDose);
			}
		}
	}


	private static void setSingleDoseOralScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);	

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 3000) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "Single Dose Oral NOAEL or LOAEL ( > " + strDose + " mg/kg-day) > 3000 mg/kg";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "Single Dose Oral NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=300) {
				sr.score = ScoreRecord.scoreVH;
				sr.rationale = "Single Dose Oral NOAEL or LOAEL ( < " + strDose + " mg/kg-day) < 300 mg/kg";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "Single Dose Oral NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {


			if (dose <= 300) {
				sr.score = ScoreRecord.scoreVH;
				sr.rationale = "Single Dose Oral NOAEL or LOAEL" + " (" + strDose + " mg/kg) < 300 mg/kg";
			} else if (dose > 300 && dose <= 2000) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "300 mg/kg < Single Dose Oral NOAEL or LOAEL (" + strDose + " mg/kg) <= 2000 mg/kg";
			} else if (dose > 2000 && dose <= 3000) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "2000 mg/kg < Single Dose Oral NOAEL or LOAEL (" + strDose + " mg/kg) <= 3000 mg/kg";
			} else if (dose > 3000) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "Single Dose Oral NOAEL or LOAEL" + "(" + strDose + " mg/kg) >  3000 mg/kg";
			} else { 
				System.out.println(chemical.CAS + "\tSingleDoseOral\t" + strDose);
			}
		}
	}




	private static void setSingleDoseDermalScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);	

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 3000) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "Single Dose Dermal NOAEL or LOAEL ( > " + strDose + " mg/kg-day) > 3000 mg/kg";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "Single Dose Dermal NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=1000) {
				sr.score = ScoreRecord.scoreVH;
				sr.rationale = "Single Dose Dermal NOAEL or LOAEL ( < " + strDose + " mg/kg-day) < 1000 mg/kg";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "Single Dose Dermal NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")) {



			if (dose <= 1000) {
				sr.score = ScoreRecord.scoreVH;
				sr.rationale = "Single Dose Dermal NOAEL or LOAEL" + " (" + strDose + " mg/kg) < 1000 mg/kg";
			} else if (dose > 1000 && dose <= 2000) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "1000 mg/kg < Single Dose Dermal NOAEL or LOAEL (" + strDose + " mg/kg) <= 2000 mg/kg";
			} else if (dose > 2000 && dose <= 3000) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "2000 mg/kg < Single Dose Dermal NOAEL or LOAEL (" + strDose + " mg/kg) <= 3000 mg/kg";
			} else if (dose > 3000) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "Single Dose Dermal NOAEL or LOAEL" + "(" + strDose + " mg/kg) >  3000 mg/kg";
			} else { 
				System.out.println(chemical.CAS + "\tSingleDoseDermal\t" + strDose);
			}
		}
	}



	private static void setSingleDoseInhalationScore(ScoreRecord sr, Chemical chemical) {

		sr.rationale = "route: " + sr.route + ", ";
		double dose = sr.valueMass;
		String strDose = ParseToxVal.formatDose(dose);	

		if (sr.valueMassOperator.equals(">")) {

			if (dose >= 30) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "Single Dose Inhalation NOAEL or LOAEL ( > " + strDose + " mg/L-day) > 30 mg/L";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "Single Dose Inhalation NOAEL or LOAEL ( > " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";
				// System.out.println(chemical.CAS+"\t"+sr.rationale);
			}


		} else if (sr.valueMassOperator.equals("<")) {
			if (dose <=10) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "Single Dose Inhalation NOAEL or LOAEL ( < " + strDose + " mg/L-day) < 10 mg/L";
			} else {
				sr.score = ScoreRecord.scoreNA;
				sr.rationale = "Single Dose Inhalation NOAEL or LOAEL ( < " + strDose
						+ " mg/kg-day) does not provide enough information to assign a score";

				// System.out.println(chemical.CAS + "\tless than operator detected for oral\t" + dose);
			}

		} else if (sr.valueMassOperator.equals("") || sr.valueMassOperator.equals("=") || sr.valueMassOperator.equals("~") || sr.valueMassOperator.equals(">=") || sr.valueMassOperator.equals("<=")){

			if (dose <= 10) {
				sr.score = ScoreRecord.scoreVH;
				sr.rationale = "Single Dose Inhalation NOAEL or LOAEL" + " (" + strDose + " mg/L) < 10 mg/L";
			} else if (dose > 10 && dose <= 20) {
				sr.score = ScoreRecord.scoreH;
				sr.rationale = "10 mg/L < Single Dose Inhalation NOAEL or LOAEL (" + strDose + " mg/L) <= 20 mg/L";
			} else if (dose > 20 && dose <= 30) {
				sr.score = ScoreRecord.scoreM;
				sr.rationale = "20 mg/L < Single Dose Inhalation NOAEL or LOAEL (" + strDose + " mg/L) <= 30 mg/L";
			} else if (dose > 30) {
				sr.score = ScoreRecord.scoreL;
				sr.rationale = "Single Dose Inhalation NOAEL or LOAEL" + "(" + strDose + " mg/L) >  30 mg/L";
			} else { 
				System.out.println(chemical.CAS + "\tSingleDoseInhalation\t" + strDose);
			}
		}
	}
}

