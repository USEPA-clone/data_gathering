package gov.epa.exp_data_gathering.parse;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import gov.epa.api.ExperimentalConstants;
import gov.epa.eChemPortalAPI.Processing.FinalRecord;
import gov.epa.eChemPortalAPI.Query.APIConstants;


public class ExperimentalRecord {

	public String id_physchem;//	Autonumbered record number for physchem data (generated by database later)
	public String id_record_source;//	Record number for reference that the physchem data came from (generated by database later- may only need for records from journal articles)
	
	//ID fields:
	public String comboID;//
	public String casrn;//Chemical abstracts service number (only if provided by the reference)
	public String einecs;
	public String chemical_name;//	Most systematic name (only if provided in the reference)
	public String synonyms;//	Pipe deliminated synonyms (only if provided in the reference)
	public String smiles;//Simplified Molecular Input Line Entry System for molecular structure (only if provided in the reference)
	
	public String property_name;//	Name of the property (use  "options_property_names" lookup table to consistently populate the field)

	//Property value fields:
	public String property_value_string;//Store original string from source for checking later
	public Double property_value_min_original;//The minimum value of a property when a range of values is given
	public Double property_value_max_original;//The maximum value of a property when a range of values is given
	public Double property_value_point_estimate_original;// Point estimate of the property (when a single value is given)
	public String property_value_units_original;//The units for the property value (convert to defined values in ExperimentalConstants class)

	//Converted property value fields:
	public String property_value_numeric_qualifier;// >, <, or ~
	public Double property_value_min_final;//The minimum value of a property when a range of values is given
	public Double property_value_max_final;//The maximum value of a property when a range of values is given
	public Double property_value_point_estimate_final;// Point estimate of the property (when a single value is given)
	public String property_value_units_final;//The units for the property value (convert to defined values in ExperimentalConstants class)
	public String property_value_qualitative;// Valid qualitative data: solubility descriptor, appearance
	
	//Conditions:
	public Double temperature_C;//The temperature in C that the property is measured at (vapor pressure might be given at 23 C for example)
	public String pressure_mmHg;//The pressure in kPa that the property is measured at (important for boiling points for example)
	public String pH;
	
	public String measurement_method;//	The experimental method used to measure the property
	public String reliability;
	public String dsstox_substance_id; //DSSTox substance identifier
	public String note;//	Any additional note

	public String url;
	public String source_name;//use Experimental constants
	public String original_source_name;//If specific reference/paper provided
								//"original_source_name" rather than "source_name_original" to avoid syntactic confusion with "*_original" vs "*_final" fields above
	public String date_accessed;//use Experimental constants
	
	public boolean keep=true;//Does the record contain useful data? keep might be different depending on whether goal is for database or for QSAR data set
	public boolean flag=false;
	public String reason;//If keep=false or flag=true, why?
	
	//TODO do we need parent url too? sometimes there are several urls we have to follow along the way to get to the final url

	public final static String [] outputFieldNames= {"id_physchem",
			"keep",
			"reason",
			"casrn",
			"einecs",
			"chemical_name",
			"synonyms",
			"smiles",
			"source_name",
			"property_name",
			"property_value_string",
			"property_value_numeric_qualifier",
			"property_value_point_estimate_final",
			"property_value_min_final",
			"property_value_max_final",
			"property_value_units_final",
			"pressure_mmHg",
			"temperature_C",
			"pH",
			"property_value_qualitative",
			"measurement_method",
			"note",
			"flag",
			"original_source_name",
			"url",
			"date_accessed"};
	
	public void setComboID(String del) {
		String CAS=casrn;
		if (CAS==null || CAS.trim().isEmpty()) CAS="casrn=null";//need placeholder so dont get spurious match in chemreg
		else {
			CAS=ParseUtilities.fixCASLeadingZero(CAS);
		}
		String name=StringEscapeUtils.escapeJava(chemical_name);
		
		String EINECS=einecs;
		if (EINECS==null || EINECS.trim().isEmpty()) EINECS="einecs=null";//need placeholder so dont get spurious match in chemreg
		EINECS=EINECS.trim();
		
		if (name==null || name.trim().isEmpty()) name="name=null";//need placeholder so dont get spurious match in chemreg
		name=name.trim();
		
		String SMILES=smiles;
		if (SMILES==null || SMILES.trim().isEmpty()) SMILES="smiles=null";//need placeholder so dont get spurious match in chemreg
		SMILES=SMILES.trim();
		
		//TODO omit chemicals where smiles indicates bad element....
		
		comboID=CAS+del+EINECS+del+name+del+SMILES;
		
	}
	
	public void assignValue(String fieldName,String fieldValue) {
		
		if (fieldValue.isEmpty()) return;
		
		Field myField;
		try {
			myField = getClass().getDeclaredField(fieldName);
			if (myField.getType().getName().contentEquals("boolean")) {
				myField.setBoolean(this, Boolean.parseBoolean(fieldValue));
			} else if (myField.getType().getName().contentEquals("double")) {
				myField.setDouble(this, Double.parseDouble(fieldValue));
			} else if (myField.getType().getName().contentEquals("int")) {
				myField.setInt(this, Integer.parseInt(fieldValue));
			} else if (myField.getType().getName().contentEquals("java.lang.Double")) {
				Double dval=Double.parseDouble(fieldValue);						
				myField.set(this, dval);
			} else if (myField.getType().getName().contentEquals("java.lang.Integer")) {
				Integer ival=Integer.parseInt(fieldValue);
				myField.setInt(this,ival);
			} else if (myField.getType().getName().contentEquals("java.lang.String")) {
				myField.set(this, fieldValue);
			} else {
				System.out.println("Need to implement"+myField.getType().getName());
			}					
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}

	public String toString() {
		return toString("|");
	}
	public String toString(String del) {
		// TODO Auto-generated method stub
		return toString(del,outputFieldNames);
	}

	public boolean isValidConditions() {
		
		if (property_name.contentEquals(ExperimentalConstants.strWaterSolubility)) {			
			if (temperature_C==null) return true;			
			else if (temperature_C>=20 && temperature_C<=30) {
				return true;
			} else {
				return false;
			}
			
		} else {
			System.out.println("Need to add condition criteria for "+property_name);
			return false;
		}
		
		
	}

	
	public boolean isValidPointEstimatePossible() {
		if (property_value_numeric_qualifier!=null && !property_value_numeric_qualifier.equals("~")) { 
//			System.out.println("bad qualifier:"+this);
			return false;
		}
		if (property_value_point_estimate_final!=null) { return true; }
		
		boolean hasMinAndMax = property_value_min_final!=null && property_value_max_final!=null;
		if (!hasMinAndMax) { 
//			System.out.println("dont have min and max:"+this);
			return false; 
		}
		
		boolean good = true;
		
		double logTolerance = 1.0;//log units for properties that vary many orders of magnitude; if value was 1, then max would be 10x bigger than min
		double temperatureTolerance = 10.0;//C For Melting point, boiling point, flash point
		double densityTolerance = 0.1;//g/cm^3 for density
		double zeroTolerance = Math.pow(10.0, -6.0);
		
		//Properties which are usually modeled as log of the property value: pKA, logKow, WS, HLC, VP, LC50, LD50
		if (property_name.equals(ExperimentalConstants.str_pKA) || property_name.equals(ExperimentalConstants.strLogKow)) {
			good = isWithinTolerance(logTolerance);
		} else if ((property_name.equals(ExperimentalConstants.strMeltingPoint) || property_name.equals(ExperimentalConstants.strBoilingPoint) ||
				property_name.equals(ExperimentalConstants.strFlashPoint))) {
			good = isWithinTolerance(temperatureTolerance);
		} else if (property_name.equals(ExperimentalConstants.strDensity)) {
			good = isWithinTolerance(densityTolerance);
		} else if (property_name.equals(ExperimentalConstants.strVaporPressure) || property_name.equals(ExperimentalConstants.strHenrysLawConstant) ||
				property_name.equals(ExperimentalConstants.strWaterSolubility) || property_name.toLowerCase().contains("lc50") ||
				property_name.toLowerCase().contains("ld50")) {
			good = isWithinLogTolerance(logTolerance,zeroTolerance);
			
			if (!good) {
//				System.out.println("dont have min and max:"+this);
			} else {
//				System.out.println("ok:"+this);
			}
		}
		
		return good;
	}

	public String toJSON() {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();// makes it multiline and readable
		Gson gson = builder.create();
		return gson.toJson(this);//all in one line!
	}

	//convert to string by reflection:
	public String toString(String del,String [] fieldNames) {

		String Line = "";
		
		for (int i = 0; i < fieldNames.length; i++) {
			try {


				Field myField = this.getClass().getDeclaredField(fieldNames[i]);

				String val=null;

//				System.out.println(fieldNames[i]+"\t"+myField.getType().getName());

				if (myField.getType().getName().contains("Double")) {
					if (myField.get(this)==null) {
						val="";	
					} else {
						val=myField.get(this)+"";
					}

				} else if (myField.getType().getName().contains("Integer")) {
					if (myField.get(this)==null) {
						val="";	
					} else {
						val=myField.get(this)+"";
					}
				} else if (myField.getType().getName().contains("Boolean")) {
					if (myField.get(this)==null) {
						val="";	
					} else {
						val=myField.get(this)+"";
					}
					
				} else if (myField.getType().getName().contains("boolean")) {
					val=myField.getBoolean(this)+"";
				
				
				} else {//string
					if (myField.get(this)==null) {
						//								val="\"\"";
						val="";
					} else {
						//								val="\""+(String)myField.get(this)+"\"";
						val=(String)myField.get(this);
					} 
				}

				val=val.replace("\r\n","<br>");
				val=val.replace("\n","<br>");

				if (val.contains(del)) {
					val=val.replace(del,"_");
					System.out.println("***WARNING***"+this.casrn+"\t"+fieldNames[i]+"\t"+val+"\thas delimiter");
				}

				Line += val;
				if (i < fieldNames.length - 1) {
					Line += del;
				}


			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return Line;
	}
	
	/**
	 * Flexible method that converts ExperimentalRecord to string array (useful for writing to excel and sqlite)
	 *  
	 * @param fieldNames
	 * @return
	 */
	public String [] toStringArray(String [] fieldNames) {

		String Line = "";

		String [] array=new String [fieldNames.length];

		for (int i = 0; i < fieldNames.length; i++) {
			try {

				Field myField = this.getClass().getDeclaredField(fieldNames[i]);

				String val=null;
				String type=myField.getType().getName();

				
				switch (type) {
				
				case "java.lang.String":
					if (myField.get(this)==null) val="";	
					else val=myField.get(this)+"";						
					val=ParseUtilities.reverseFixChars(StringEscapeUtils.unescapeHtml4(val.replaceAll("(?<!\\\\)'", "\'")));					
					break;
				
				case "java.lang.Double":
					if (myField.get(this)==null) val="";	
					else {
						val=ParseUtilities.formatDouble((Double)myField.get(this));						
					}										
					break;
					
				case "java.lang.Integer":
				case "java.lang.Boolean": 							
					if (myField.get(this)==null) val="";	
					else val=myField.get(this)+"";						
					break;					
				case "boolean":
					val=myField.getBoolean(this)+"";
					break;
				case "int":
					val=myField.getInt(this)+"";
					break;
				case "double": 
					val=myField.getDouble(this)+"";

				}

				val=val.trim();
				val=val.replace("\r\n","<br>");
				val=val.replace("\n","<br>");

				array[i]=val;

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return array;
	}
	
	
	/**
	 * Adds a string to the note field of an ExperimentalRecord object
	 * @param er	The ExperimentalRecord object to be updated
	 * @param str	The string to be added
	 * @return		The updated ExperimentalRecord object
	 */
	public void updateNote(String str) {
		note = Objects.isNull(note) ? str : note+"; "+str;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this) {
			return true;
		}
		
		if (!(o instanceof ExperimentalRecord)) {
			return false;
		}
		
		ExperimentalRecord er = (ExperimentalRecord) o;
		if (Objects.equals(this.casrn, er.casrn) &&
				Objects.equals(this.einecs, er.einecs) &&
				Objects.equals(this.chemical_name, er.chemical_name) &&
				Objects.equals(this.synonyms, er.synonyms) &&
				Objects.equals(this.smiles, er.smiles) &&
				Objects.equals(this.property_name, er.property_name) &&
				Objects.equals(this.property_value_numeric_qualifier, er.property_value_numeric_qualifier) &&
				Objects.equals(this.temperature_C, er.temperature_C) &&
				Objects.equals(this.pressure_mmHg, er.pressure_mmHg) &&
				Objects.equals(this.pH, er.pH) &&
				Objects.equals(this.dsstox_substance_id, er.dsstox_substance_id) &&
				Objects.equals(this.property_value_min_original, er.property_value_min_original) &&
				Objects.equals(this.property_value_max_original, er.property_value_max_original) &&
				Objects.equals(this.property_value_point_estimate_original, er.property_value_point_estimate_original) &&
				Objects.equals(this.property_value_units_original, er.property_value_units_original)) {
			return true;
		} else {
			return false;
		}
	}

	public double rangeAverage() {
		return (property_value_min_final + property_value_max_final)/2.0;
	}

	public boolean isWithinTolerance(double tolerance) {		
		return property_value_max_final-property_value_min_final <= tolerance;
	}

	public boolean isWithinLogTolerance(double logTolerance,double zeroTolerance) {
		if (Math.abs(property_value_min_final) > zeroTolerance) {
			return Math.log10(property_value_max_final/property_value_min_final) <= logTolerance;
		} else {
			return false;
		}
	}

//	public String[] getValuesForDatabase() {
//		String name = chemical_name==null ? "" : chemical_name.replaceAll("(?<!\\\\)'", "\'");
//		String pointEstimate = property_value_point_estimate_final==null ? "" : Double.toString(property_value_point_estimate_final);
//		String min = property_value_min_final==null ? "" : Parse.formatDouble(property_value_min_final);
//		String max = property_value_max_final==null ? "" : Parse.formatDouble(property_value_max_final);
//		String temp = temperature_C==null ? "" : Parse.formatDouble(temperature_C);
//		String [] values= {Boolean.toString(keep),
//				reason,
//				casrn,
//				einecs,
//				name,
//				synonyms,
//				smiles,
//				source_name,
//				property_name,
//				property_value_string,
//				property_value_numeric_qualifier,
//				pointEstimate,
//				min,
//				max,
//				property_value_units_final,
//				pressure_mmHg,
//				temp,
//				pH,
//				property_value_qualitative,
//				measurement_method,
//				note,
//				Boolean.toString(flag),
//				original_source_name,
//				url,
//				date_accessed};
//		return values;
//	}
}
