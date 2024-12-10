package gov.epa.exp_data_gathering.parse.PubChem;

import java.io.FileReader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import gov.epa.exp_data_gathering.parse.PubChem.JSONsForPubChem.*;

public class AnnotationQuery {

	Annotations Annotations;
	
	
	public class Annotation {
		
		String SourceName;
		String SourceID;
		String Name;
		String Description;
		String URL;
		String LicenseURL;
		
		@SerializedName("Data")
		List<Data> data;
		
		long ANID;
		
		@SerializedName("LinkedRecords")
		LinkedRecords linkedRecords;
				
	}
	
	public class LinkedRecords {
		@SerializedName("CID")
		public long [] cids;
	}

	
	class Data {
		TOCHeading TOCHeading;
		String Description;
		String []Reference;
		
		@SerializedName("Value")
		Value value;
	}
	
	
	class TOCHeading {
		String type;
		@SerializedName("#TOCHeading")
		String TOCHeading;
	}
	
	
	
	public class Annotations {
		List<Annotation> Annotation;
		int Page;
		int TotalPages;
	}
	
	
	void loadAnnotationFile(String annotationFilePath) {
		
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		
		try {
			AnnotationQuery aq=gson.fromJson(new FileReader(annotationFilePath), AnnotationQuery.class);
			System.out.println(gson.toJson(aq));
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		
	}
	
	public static void main(String[] args) {
		AnnotationQuery r = new AnnotationQuery();
		
		String folderMain="data\\experimental\\PubChem_2024_11_27\\";
		String folder=folderMain+"\\json\\physchem\\";
		String annotationFilePath=folder+"Henry's Law Constant 1.json";
		
		r.loadAnnotationFile(annotationFilePath);
		
		
	}


}
