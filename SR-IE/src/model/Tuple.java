/*
 * Tuple.java
 *
 * Created on January 28, 2005, 10:18 PM
 */

package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author  Alpa
 */

// An in memory representation of a tuple. This class allows accessing and
// setting of the individual fields (values and names) in a tuple.

public class Tuple implements Serializable{

	private HashMap<String,String> theTuple;
	private String supportingDocId = null;
	private String toString = null;
	private ArrayList<String> sources;
	private String[] fieldNames;
	private boolean hashCodecal;
	private int hashCode;  

	/** Creates a new instance of Tuple */
	public Tuple() {
		sources = new ArrayList<String>();
		theTuple = new HashMap<String,String>();
		hashCodecal = false;
	}

	public String toString() {

		if (toString == null){
			toString  = generateString();
		}
		return toString;
	}

	private String generateString() {

		StringBuffer buffer = new StringBuffer();
		for (Iterator<String> iter = theTuple.keySet().iterator(); iter.hasNext(); ) {
			String name = (String )iter.next();
			buffer.append(name + ":" + theTuple.get(name).replaceAll("(\\p{Punct}|\\s|\\r|\\n|\\t)+", " "));
			if (iter.hasNext())
				buffer.append(";");
		}
		return buffer.toString();
	}

	public String[] getFieldNames() {

		if (fieldNames == null){
			fieldNames = calculateFieldNames();
		}

		return fieldNames;

	}

	private String[] calculateFieldNames() {

		return (theTuple.keySet().toArray(new String[theTuple.size()]));

	}

	public void setTupleField(String name, String value) {

		theTuple.put(name, value.toLowerCase().trim());

	}  

	public String getFieldValue(String field) {

		return theTuple.get(field);
	}

	public String supportingDocId () {
		return supportingDocId;
	}

	public void supportingDocId (String sd) {
		supportingDocId = sd;
	}

	public boolean equals (Object obj) {
		if(! (obj instanceof Tuple)) {
			return false;
		}

		Tuple second = (Tuple)obj;

		if (theTuple.size() != second.theTuple.size()){
			return false;
		}

		for (String field : second.theTuple.keySet()) {

			if (!theTuple.containsKey(field)){

				return false;

			}else{

				if (!theTuple.get(field).equals(second.theTuple.get(field))) //it's all in lowercase
				return false;

			}

		}

		return true;

	}

	public boolean isValid() {
		for (Iterator<String> iter = theTuple.keySet().iterator(); iter.hasNext(); ) {
			if(theTuple.get(iter.next()) != null)
				return true;
		}
		System.out.println("Found an invalid tuple");
		return false;

	}

	public int hashCode() {

		if (!hashCodecal){

			hashCode = theTuple.hashCode();
			hashCodecal = true;

		}

		return hashCode;
	}

	public void setDocument(String usefulDoc) {

		sources.add(usefulDoc);

	}  

	public ArrayList<String> getDocuments(){
		return sources;
	}

}
