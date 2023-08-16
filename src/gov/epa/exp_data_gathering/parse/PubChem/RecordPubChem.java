package gov.epa.exp_data_gathering.parse.PubChem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.text.StringEscapeUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import gov.epa.api.ExperimentalConstants;
import gov.epa.database.SQLite_CreateTable;
import gov.epa.database.SQLite_GetRecords;
import gov.epa.database.SQLite_Utilities;
import gov.epa.exp_data_gathering.parse.DownloadWebpageUtilities;
import gov.epa.exp_data_gathering.parse.RecordDashboard;
import gov.epa.exp_data_gathering.parse.PubChem.JSONsForPubChem.Data;
import gov.epa.exp_data_gathering.parse.PubChem.JSONsForPubChem.IdentifierData;
import gov.epa.exp_data_gathering.parse.PubChem.JSONsForPubChem.Information;
import gov.epa.exp_data_gathering.parse.PubChem.JSONsForPubChem.Property;
import gov.epa.exp_data_gathering.parse.PubChem.JSONsForPubChem.Reference;
import gov.epa.exp_data_gathering.parse.PubChem.JSONsForPubChem.Section;
import gov.epa.exp_data_gathering.parse.PubChem.JSONsForPubChem.StringWithMarkup;

import gov.epa.ghs_data_gathering.Utilities.FileUtilities;

public class RecordPubChem {
	String cid;
	String iupacName;
	String smiles;
	String synonyms;
	Vector<String> cas;
	Vector<String> physicalDescription;
	Vector<String> density;
	Vector<String> meltingPoint;
	Vector<String> boilingPoint;
	Vector<String> flashPoint;
	Vector<String> solubility;
	Vector<String> vaporPressure;
	Vector<String> henrysLawConstant;
	Vector<String> logP;
	Vector<String> pKa;
	Hashtable<Integer, String> physicalDescriptionHT = new Hashtable<>();
	Hashtable<Integer, String> densityHT = new Hashtable<>();
	Hashtable<Integer, String> meltingPointHT = new Hashtable<>();
	Hashtable<Integer, String> boilingPointHT = new Hashtable<>();
	Hashtable<Integer, String> solubilityHT = new Hashtable<>();
	Hashtable<Integer, String> flashPointHT = new Hashtable<>();
	Hashtable<Integer, String> vaporPressureHT = new Hashtable<>();
	Hashtable<Integer, String> hlcHT = new Hashtable<>();
	Hashtable<Integer, String> logPHT = new Hashtable<>();
	Hashtable<Integer, String> pKaHT = new Hashtable<>();
	
	String reference;
	String date_accessed;
	
	static final String sourceName=ExperimentalConstants.strSourcePubChem;
	
	private RecordPubChem() {
		cas = new Vector<String>();
		physicalDescription = new Vector<String>();
		density = new Vector<String>();
		meltingPoint = new Vector<String>();
		boilingPoint = new Vector<String>();
		flashPoint = new Vector<String>();
		solubility = new Vector<String>();
		vaporPressure = new Vector<String>();
		henrysLawConstant = new Vector<String>();
		logP = new Vector<String>();
		pKa = new Vector<String>();
	}
	
	/**
	 * Extracts DTXSIDs from CompTox dashboard records and translates them to PubChem CIDs
	 * @param records	A vector of RecordDashboard objects
	 * @param start		The index in the vector to start converting
	 * @param end		The index in the vector to stop converting
	 * @return			A vector of PubChem CIDs as strings
	 */
	private static Vector<String> getCIDsFromDashboardRecords(Vector<RecordDashboard> records,String dictFilePath,int start,int end) {
		Vector<String> cids = new Vector<String>();
		LinkedHashMap<String,String> dict = new LinkedHashMap<String, String>();
		
		try {
			File file = new File(dictFilePath);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line="";
			while ((line=br.readLine())!=null) {
				String[] cells=line.split(",");
				dict.put(cells[0], cells[1]);
			}
			br.close();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		int counter = 0;
		for (int i = start; i < end; i++) {
			String dtxsid = records.get(i).DTXSID;
			String cid = dict.get(dtxsid);
			if (cid!=null) {
				cids.add(cid);
				counter++;
			} else {
				boolean foundCID = false;
				try {
					String inchikey = records.get(i).INCHIKEY;
					String url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/"+inchikey+"/cids/TXT";
					String cidsTxt = FileUtilities.getText_UTF8(url);
					if (cidsTxt!=null) {
						cids.add(cidsTxt.split("\r\n")[0]);
						foundCID = true;
						counter++;
					}
					Thread.sleep(200);
				} catch (Exception ex) { ex.printStackTrace(); }
				if (!foundCID) {
					try {
						String smiles = records.get(i).SMILES;
						String url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/smiles/"+smiles+"/cids/TXT";
						String cidsTxt = FileUtilities.getText_UTF8(url);
						if (cidsTxt!=null) {
							cids.add(cidsTxt.split("\r\n")[0]);
							foundCID = true;
							counter++;
						}
						Thread.sleep(200);
					} catch (Exception ex) { ex.printStackTrace(); }
				}
			}
			if (counter % 100 == 0) {
				System.out.println("Found "+counter+" CIDs");
			}
		}
		System.out.println("Found "+counter+" CIDs");
		return cids;
	}
	
	private static void downloadJSONsToDatabase(Vector<String> cids, boolean startFresh) {
		ParsePubChem p = new ParsePubChem();
		String databaseName = p.sourceName+"_raw_json.db";
		String tableName = p.sourceName;
		String databasePath = p.databaseFolder+File.separator+databaseName;
		File db = new File(databasePath);
		if(!db.getParentFile().exists()) { db.getParentFile().mkdirs(); }
		java.sql.Connection conn=SQLite_CreateTable.create_table(databasePath, tableName, RawDataRecordPubChem.fieldNames, startFresh);
		HashSet<String> cidsAlreadyQueried = new HashSet<String>();
		ResultSet rs = SQLite_GetRecords.getAllRecords(SQLite_Utilities.getStatement(conn), tableName);
		try {
			long start = System.currentTimeMillis();
			while (rs.next()) {
				cidsAlreadyQueried.add(rs.getString("cid"));
			}
			long end = System.currentTimeMillis();
			System.out.println(cidsAlreadyQueried.size()+" CIDs already queried ("+(end-start)+" ms)");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		try {
			int counterSuccess = 0;
			int counterTotal = 0;
			int counterMissingExpData = 0;
			long start = System.currentTimeMillis();
			for (String cid:cids) {
				String experimentalURL = "https://pubchem.ncbi.nlm.nih.gov/rest/pug_view/data/compound/"+cid+"/JSON?heading=Experimental+Properties";
				String idURL = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/property/IUPACName,CanonicalSMILES/JSON?cid="+cid;
				String casURL = "https://pubchem.ncbi.nlm.nih.gov/rest/pug_view/data/compound/"+cid+"/JSON?heading=CAS";
				String synonymURL = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/"+cid+"/synonyms/TXT";
				
				SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");  
				Date date = new Date();  
				String strDate=formatter.format(date);
				
				RawDataRecordPubChem rec=new RawDataRecordPubChem(strDate, cid, "", "", "", "");
				if (cidsAlreadyQueried.add(cid) || startFresh) {
					counterTotal++;
					boolean keepLooking = true;
					try {
						rec.experimental=FileUtilities.getText_UTF8(experimentalURL);
						rec.experimental = rec.experimental.replaceAll("'", "''").replaceAll(";", "\\;");
					} catch (Exception ex) { 
						counterMissingExpData++;
						keepLooking = false;
					}
					Thread.sleep(200);
					if (keepLooking) {
						try {
//							rec.cas=FileUtilities.getText_UTF8(casURL).replaceAll("'", "\'").replaceAll(";", "\\;");
							rec.cas=FileUtilities.getText_UTF8(casURL);
							rec.cas = rec.cas.replaceAll("'", "''").replaceAll(";", "\\;");
						} catch (Exception ex) { }
						Thread.sleep(200);
						try {
//							rec.identifiers=FileUtilities.getText_UTF8(idURL).replaceAll("'", "\'").replaceAll(";", "\\;");
							rec.identifiers=FileUtilities.getText_UTF8(idURL);
							rec.identifiers = rec.identifiers.replaceAll("'", "''").replaceAll(";", "\\;");
						} catch (Exception ex) { }
						Thread.sleep(200);
						try {
//							rec.synonyms=FileUtilities.getText_UTF8(synonymURL).replaceAll("'", "\'").replaceAll(";", "\\;");
							rec.synonyms=StringEscapeUtils.escapeHtml4(FileUtilities.getText_UTF8(synonymURL));
							rec.synonyms = rec.synonyms.replaceAll("'", "''").replaceAll(";", "\\;");
						} catch (Exception ex) { }
						Thread.sleep(200);
					}
					if (rec.experimental!=null && !rec.experimental.isBlank()) {
						rec.addRecordToDatabase(tableName, conn);
						counterSuccess++;
					}
					if (counterTotal % 1000==0) {
						long batchEnd = System.currentTimeMillis();
						System.out.println("Attempted: "+counterTotal+" ("+cidsAlreadyQueried.size()+" total)");
						System.out.println("Succeeded: "+counterSuccess);
						System.out.println("Failed - no experimental properties: "+counterMissingExpData);
						System.out.println("---------- (~"+(batchEnd-start)/60000+" min)");
						start = batchEnd;
					}
				}
			}
			System.out.println("Attempted: "+counterTotal+" ("+cidsAlreadyQueried.size()+" total)");
			System.out.println("Succeeded: "+counterSuccess);
			System.out.println("Failed - no experimental properties: "+counterMissingExpData);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	protected static Vector<RecordPubChem> parseJSONsInDatabase() {
		String databaseFolder = "Data"+File.separator+"Experimental"+ File.separator + sourceName;
		String databasePath = databaseFolder+File.separator+sourceName+"_raw_json.db";
		Vector<RecordPubChem> records = new Vector<>();
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		try {
			Statement stat = SQLite_Utilities.getStatement(databasePath);
			ResultSet rs = SQLite_GetRecords.getAllRecords(stat,sourceName);
			while (rs.next()) {
				String date = rs.getString("date");
				String experimental = rs.getString("experimental");
				Data experimentalData = gson.fromJson(experimental,Data.class);
				RecordPubChem pcr = new RecordPubChem();
				pcr.date_accessed = date.substring(0,date.indexOf(" "));
				pcr.cid = experimentalData.record.recordNumber;
				List<Section> experimentalProperties = experimentalData.record.section.get(0).section.get(0).section;
				// Christian's new block
				if (experimentalData.record.reference != null) {
					Gson gson2 = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
					String s1 = gson2.toJson(experimentalData.record.reference);
					
					// to be honest I have no idea what this does
				    // MainResponse mainResponse = gson.fromJson(response, MainResponse.class);
					
					// System.out.println(s1);
					pcr.reference = s1;
					// no idea why i commented these out either
					// List<Reference> referenceProperties = experimentalData.record.reference;
					// pcr.getReferenceInfo(referenceProperties);
				}
				pcr.getExperimentalData(experimentalProperties);
				String identifiers = rs.getString("identifiers");
				IdentifierData identifierData = gson.fromJson(identifiers, IdentifierData.class);
				if (identifierData!=null) {
					Property identifierProperty = identifierData.propertyTable.properties.get(0);
					pcr.iupacName = identifierProperty.iupacName;
					pcr.smiles = identifierProperty.canonicalSMILES;
				}
				
				
				String cas = rs.getString("cas");
				Data casData = gson.fromJson(cas, Data.class);
				if (casData!=null) {
					List<Information> casInfo = casData.record.section.get(0).section.get(0).section.get(0).information;
					for (Information c:casInfo) {
						String newCAS = c.value.stringWithMarkup.get(0).string;
						if (!pcr.cas.contains(newCAS)) { pcr.cas.add(newCAS); }
					}
				}
				pcr.synonyms = rs.getString("synonyms").replaceAll("\r\n","|");
				records.add(pcr);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return records;
	}
	
		
	private void getExperimentalData(List<Section> properties) {
		for (Section prop:properties) {
			String heading = prop.tocHeading;
			List<Information> info = prop.information;
			if (heading.equals("Physical Description") || heading.equals("Color/Form")) {
				for (Information i:info) { physicalDescription.add(getStringFromInformation(i)); 
				getStringAndReferenceFromInformation(i,physicalDescriptionHT);

				}
			} else if (heading.equals("Density")) {
				for (Information i:info) { density.add(getStringFromInformation(i));
				getStringAndReferenceFromInformation(i,densityHT);

				}
			} else if (heading.equals("Melting Point")) {
				for (Information i:info) { meltingPoint.add(getStringFromInformation(i)); 
				getStringAndReferenceFromInformation(i,meltingPointHT);

				}
			} else if (heading.equals("Boiling Point")) {
				for (Information i:info) { boilingPoint.add(getStringFromInformation(i)); 
				getReferenceFromInformation(i); 
				getStringAndReferenceFromInformation(i,boilingPointHT);
}
			} else if (heading.equals("Flash Point")) {
				for (Information i:info) { flashPoint.add(getStringFromInformation(i)); 
				getStringAndReferenceFromInformation(i,flashPointHT);
}
			} else if (heading.equals("Solubility")) {
				for (Information i:info) { solubility.add(getStringFromInformation(i));
				getStringAndReferenceFromInformation(i,solubilityHT);
				}
			} else if (heading.equals("Vapor Pressure")) {
				for (Information i:info) { vaporPressure.add(getStringFromInformation(i)); 
				getStringAndReferenceFromInformation(i,vaporPressureHT);

				}
			} else if (heading.equals("Henrys Law Constant")) {
				for (Information i:info) { henrysLawConstant.add(getStringFromInformation(i)); 
				getStringAndReferenceFromInformation(i,hlcHT);

				}
			} else if (heading.equals("LogP")) {
				for (Information i:info) { logP.add(getStringFromInformation(i)); 
				getStringAndReferenceFromInformation(i,logPHT);

				}
			} else if (heading.equals("pKa")) {
				for (Information i:info) { pKa.add(getStringFromInformation(i)); 
				getStringAndReferenceFromInformation(i,pKaHT);

				}
			}
		}
	}
	
	// Christian's doing
	private Double getReferenceFromInformation(Information i) {
		if (i.referenceNumber != null) {
			String refStr = i.referenceNumber;
			System.out.println(i.referenceNumber);
			return Double.parseDouble(refStr);
		}
		else {
			return null;
		}
	}
	
	private void getStringAndReferenceFromInformation(Information i, Hashtable<Integer, String> ht) {
		List<StringWithMarkup> strings = i.value.stringWithMarkup;
		Integer refNum = Integer.parseInt(i.referenceNumber);
		if (strings != null && refNum != null) {
			ht.put(refNum, strings.get(0).string);
		}
	}
	
	
	private String getStringFromInformation(Information i) {
		List<StringWithMarkup> strings = i.value.stringWithMarkup;
		if (strings!= null) {
			return strings.get(0).string;
		} else {
			return null;
		}
	}
	
	public static void main(String[] args) {
		Vector<RecordDashboard> drs = DownloadWebpageUtilities.getDashboardRecordsFromExcel("Data"+"/PFASSTRUCT.xls");
//		Vector<String> cids = getCIDsFromDashboardRecords(drs,"Data"+"/CIDDICT.csv",1,8164);
		List<String> cidsList = gov.epa.QSAR.utilities.FileUtilities.readFile("Data\\Experimental\\PubChem\\solubilitycids.txt");
		Vector<String> cids = new Vector<String>(cidsList);
		downloadJSONsToDatabase(cids,false);
	}
	
	public void printObject(Object object) {
	    Gson gson = new GsonBuilder().setPrettyPrinting().create();
	    System.out.println(gson.toJson(object));
	}
}
