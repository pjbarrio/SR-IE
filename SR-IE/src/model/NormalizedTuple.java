package model;

import java.util.HashMap;
import java.util.Iterator;

public class NormalizedTuple extends Tuple {

	public static String NON_NORMALIZABLE = "NON_NORMALIZABLE";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3356134547475856159L;

	private HashMap<String,String> theRefTuple;
	
	public NormalizedTuple() {
		super();
		theRefTuple = new HashMap<String,String>();
	}
	
	public void setTupleField(String name, String value, String reference) {

		super.setTupleField(name, value);
		
		if (reference == null){
			theRefTuple.put(name, NON_NORMALIZABLE);
		}else{
			theRefTuple.put(name, reference);
		}
		
	}  
	
	public String getNormalizedFieldValue(String field) {

		return theRefTuple.get(field);
	}
	
}
