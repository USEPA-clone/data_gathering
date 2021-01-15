package gov.epa.exp_data_gathering.eChemPortalAPI;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Replicates the QueryBlock object of an eChemPortal API search query JSON
 * @author GSINCL01 (Gabriel Sinclair)
 *
 */
public class QueryBlock {
	@SerializedName("endpointKind")
	@Expose
	public String endpointKind;
	@SerializedName("queryFields")
	@Expose
	public List<QueryField> queryFields = null;
	
	// Up to three different versions of the endpointKind string are used in constructing queries
	// Transient fields will not be serialized/deserialized in the JSON
	private transient String endpointKind2;
	private transient String endpointKind3;
	
	public QueryBlock() {
		queryFields = new ArrayList<QueryField>();
	}
	
	public QueryBlock(String endpointKind) {
		this.endpointKind = endpointKind;
		
		if (this.endpointKind.equals("Melting")) {
			endpointKind2 = "MeltingPoint";
		} else if (this.endpointKind.equals("Vapour")) {
			endpointKind2 = "Vapourpr";
		} else if (this.endpointKind.equals("Partition")) {
			endpointKind2 = "Partcoeff";
		} else if (this.endpointKind.equals("HenrysLawConstant")) {
			endpointKind2 = "HenrysLawConstantH";
		} else {
			endpointKind2 = this.endpointKind;
		}
		
		if (this.endpointKind.equals("FlashPoint")) {
			endpointKind3 = "FPoint";
		} else if (this.endpointKind.equals("Vapour")) {
			endpointKind3 = "Pressure";
		} else if (this.endpointKind.equals("WaterSolubility")) {
			endpointKind3 = "Solubility";
		} else if (this.endpointKind.equals("DissociationConstant")) {
			endpointKind3 = "pka";
		} else if (this.endpointKind.equals("HenrysLawConstant")) {
			endpointKind3 = "H";
		} else if (this.endpointKind.equals("Partition")) {
			endpointKind3 = this.endpointKind;
		} else {
			endpointKind3 = endpointKind2;
		}
		
		queryFields = new ArrayList<QueryField>();
	}
	
	/**
	 * Adds a QueryField to guarantee only experimental study results
	 */
	public void addInfoTypeField() {
		Value infoTypeValue = new Value("EQUALS","experimental study");
		QueryField infoType = new QueryField("ENDPOINT_STUDY_RECORD."+endpointKind+".AdministrativeData.StudyResultType","string","InfoType",infoTypeValue);
		queryFields.add(infoType);
	}

	/**
	 * Adds a QueryField to guarantee results with reliability <= maxReliabilityLevel (2 recommended)
	 * @param maxReliabilityLevel	1 = reliable w/o restrictions, 2 = reliable w/ restrictions, 3 = not reliable, 4 = not assignable
	 */
	public void addReliabilityField(int maxReliabilityLevel,boolean includeOther) {
		List<Value> reliabilityValues = new ArrayList<Value>();
		Value reliabilityValue1 = new Value("EQUALS","1 (reliable without restriction)");
		Value reliabilityValue2 = new Value("EQUALS","2 (reliable with restrictions)");
		Value reliabilityValue3 = new Value("EQUALS","3 (not reliable)");
		Value reliabilityValue4 = new Value("EQUALS","4 (not assignable)");
		Value[] reliabilityValuesArray = {reliabilityValue1,reliabilityValue2,reliabilityValue3,reliabilityValue4};
		for (int i = 0; i < maxReliabilityLevel; i++) {
			reliabilityValues.add(reliabilityValuesArray[i]);
		}
		if (includeOther) {
			reliabilityValues.add(new Value("LIKE","other:*"));
		}
		QueryField reliability = new QueryField("ENDPOINT_STUDY_RECORD."+endpointKind+".AdministrativeData.Reliability","string","Reliability",reliabilityValues);
		queryFields.add(reliability);
	}
	
	public void addAfterYearField(String afterYear) {
		Value afterYearValue = new Value("numeric","GT_EQUALS",afterYear,null);
		QueryField afterYearField = new QueryField("ENDPOINT_STUDY_RECORD."+endpointKind+".DataSource.Reference+LITERATURE.GeneralInfo.ReferenceYear",
				"numeric","After Year",afterYearValue);
		queryFields.add(afterYearField);
	}
	
	public void addBeforeYearField(String beforeYear) {
		Value beforeYearValue = new Value("numeric","LT",beforeYear,null);
		QueryField beforeYearField = new QueryField("ENDPOINT_STUDY_RECORD."+endpointKind+".DataSource.Reference+LITERATURE.GeneralInfo.ReferenceYear",
				"numeric","Before Year",beforeYearValue);
		queryFields.add(beforeYearField);
	}

	public void addYearField(String after,String before) {
		List<Value> yearValues = new ArrayList<Value>();
		yearValues.add(new Value("numeric","GT_EQUALS",after,null));
		yearValues.add(new Value("numeric","LT",before,null));
		Value yearValue = new Value("range","0","1000000",null);
		QueryField yearField = new QueryField("ENDPOINT_STUDY_RECORD."+endpointKind+".DataSource.Reference+LITERATURE.GeneralInfo.ReferenceYear",
				"range","Year",yearValue);
		queryFields.add(yearField);
	}
	
	/**
	 * Adds a QueryField to set pressure condition bounds
	 * @param lower		Lower pressure condition bound
	 * @param upper		Upper pressure condition bound
	 * @param unit		Desired pressure condition unit from ExperimentalConstants
	 */
	public void addAtmPressureField(String lower,String upper,String unit) {
		String fieldName = "";
		if (endpointKind.equals("Melting") || endpointKind.equals("BoilingPoint")) {
			fieldName = "ENDPOINT_STUDY_RECORD."+endpointKind+".ResultsAndDiscussion."+endpointKind2+".Pressure";
		} else if (endpointKind.equals("FlashPoint") || endpointKind.equals("HenrysLawConstant")) {
			fieldName = "ENDPOINT_STUDY_RECORD."+endpointKind+".ResultsAndDiscussion."+endpointKind2+".AtmPressure";
		}
		String type = "range";
		Value atmPressureValue = new Value(type,lower,upper,new Unit(unit,"Vapour"));
		QueryField atmPressure = new QueryField(fieldName,type,"Pressure",atmPressureValue);
		queryFields.add(atmPressure);
	}
	
	/**
	 * Adds a QueryField to set temperature condition bounds
	 * @param lower		Lower temperature condition bound
	 * @param upper		Upper temperature condition bound
	 * @param unit		Desired temperature condition unit from ExperimentalConstants
	 */
	public void addTemperatureField(String lower,String upper,String unit) {
		String fieldName = "";
		if (endpointKind.equals("Density") || endpointKind.equals("Partition") || endpointKind.equals("WaterSolubility") || endpointKind.equals("DissociationConstant") ||
				endpointKind.equals("HenrysLawConstant")) {
			fieldName = "ENDPOINT_STUDY_RECORD."+endpointKind+".ResultsAndDiscussion."+endpointKind2+".Temp";
		} else if (endpointKind.equals("Vapour")) {
			fieldName = "ENDPOINT_STUDY_RECORD."+endpointKind+".ResultsAndDiscussion."+endpointKind2+".TempQualifier";
		}
		String type = "range";
		Value temperatureValue = new Value(type,lower,upper,new Unit(unit,"MeltingPoint"));
		QueryField temperature = new QueryField(fieldName,type,"Temperature",temperatureValue);
		queryFields.add(temperature);
	}
	
	/**
	 * Adds a QueryField to set pH condition bounds
	 * @param lower		Lower pH condition bound
	 * @param upper		Upper pH condition bound
	 */
	public void addpHField(String lower,String upper) {
		String fieldName = "ENDPOINT_STUDY_RECORD."+endpointKind+".ResultsAndDiscussion."+endpointKind2+".Ph";
		String type = "range";
		Value pHValue = new Value(type,lower,upper,new Unit("","DissociationConstant"));
		QueryField pH = new QueryField(fieldName,type,"pH",pHValue);
		queryFields.add(pH);
	}
	
	/**
	 * Adds a QueryField to set endpoint value bounds
	 * @param lower		Lower endpoint value bound
	 * @param upper		Upper endpoint value bound
	 * @param unit		Desired endpoint value unit from ExperimentalConstants
	 */
	public void addEndpointField(String lower,String upper,String unit) {
		String fieldName = "ENDPOINT_STUDY_RECORD."+endpointKind+".ResultsAndDiscussion."+endpointKind2+"."+endpointKind3;
		String type = "range";
		Value endpointValue = new Value(type,lower,upper,new Unit(unit,endpointKind));
		QueryField endpoint = new QueryField(fieldName,type,"Value",endpointValue);
		queryFields.add(endpoint);
	}
	
	public void addAllUnitEndpointField(String lower,String upper) {
		String fieldName = "ENDPOINT_STUDY_RECORD."+endpointKind+".ResultsAndDiscussion."+endpointKind2+"."+endpointKind3;
		String type = "range";
		Value endpointValue = new Value(type,lower,upper,new Unit(endpointKind));
		QueryField endpoint = new QueryField(fieldName,type,"Value",endpointValue);
		queryFields.add(endpoint);
	}
	
	/**
	 * For partition coefficient queries, adds QueryFields to guarantee octanol-water partition coefficient and include both Pow and log Pow records
	 */
	public void addPartitionCoefficientFields() {
		Value type1Value = new Value("EQUALS","octanol-water");
		Value type2Value1 = new Value("EQUALS","log Pow");
		Value type2Value2 = new Value("EQUALS","Pow");
		QueryField type1 = new QueryField("ENDPOINT_STUDY_RECORD.Partition.MaterialsAndMethods.PartitionCoefficientType","string","Type",type1Value);
		QueryField type2 = new QueryField("ENDPOINT_STUDY_RECORD.Partition.ResultsAndDiscussion.Partcoeff.Type","string","isLog",type2Value1);
		type2.values.add(type2Value2);
		queryFields.add(type1);
		queryFields.add(type2);
	}
	
	/**
	 * For water solubility queries, adds a QueryField to guarantee water solubility rather than metal complex dissolution/reaction
	 */
	public void addWaterSolubilityFields() {
		Value typeValue = new Value("EQUALS","water solubility");
		QueryField type = new QueryField("ENDPOINT_STUDY_RECORD.WaterSolubility.AdministrativeData.Endpoint","string","Solvent",typeValue);
		queryFields.add(type);
	}
	
	public void addAllGLPComplianceField() {
		Value glpComplianceValue = new Value("LIKE","*");
		QueryField glpComplianceField = new QueryField("ENDPOINT_STUDY_RECORD."+endpointKind+".MaterialsAndMethods.GLPComplianceStatement",
				"string","GLP Compliance",glpComplianceValue);
		queryFields.add(glpComplianceField);
	}
	
	public void addAllTestGuidelineAndQualifierFields() {
		Value qualifierValue = new Value("LIKE","*");
		QueryField qualifierField = new QueryField("ENDPOINT_STUDY_RECORD."+endpointKind+".MaterialsAndMethods.Guideline.Qualifier",
				"string","Guideline Qualifier",qualifierValue);
		queryFields.add(qualifierField);
		
		Value guidelineValue = new Value("LIKE","*");
		QueryField guidelineField = new QueryField("ENDPOINT_STUDY_RECORD."+endpointKind+".MaterialsAndMethods.Guideline.Guideline",
				"string","Guideline",guidelineValue);
		queryFields.add(guidelineField);
	}
}