package evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import model.NormalizedTuple;
import model.Tuple;

import org.apache.commons.io.FileUtils;
import org.jblas.exceptions.UnsupportedArchitectureException;

import utils.SerializationHelper;
import data.generation.SentenceUsefulnessSplit;

public class createGoldDataTable {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		List<String> files = FileUtils.readLines(new File(args[0])); //"tuples_files.txt"
		
		BufferedWriter bwuseful = new BufferedWriter(new FileWriter(new File("data/evaluation/useful.gold")));
		BufferedWriter bwnovel = new BufferedWriter(new FileWriter(new File("data/evaluation/novel.gold")));
		
		bwuseful.write("data,relation,extractor,useful");
		bwnovel.write("data,relation,extractor,attribute,distinct");
		
		for (int i = 0; i < files.size(); i++) {
		
			System.out.println(files.get(i));
		
			String[] spl = files.get(i).split("\\.");

			String relation = spl[1];
		
			String extractor = spl[2];
					
			String[] dod = spl[0].split("/");
			
			String outprefix = "data/" + dod[0] + "/";
			String data = dod[1];
			
			Map<Long,List<Tuple>> tuples = (Map<Long,List<Tuple>>)SerializationHelper.deserialize(outprefix + data + "." + relation + "." + extractor + SentenceUsefulnessSplit.TUPLES + ".ser");
			
			extractor = getExtractor(spl[2]);
			relation = OutputCombiner.getRelation(spl[1],extractor);
			
			bwuseful.newLine();
			bwuseful.write(data + "," + relation + "," + extractor + "," + tuples.size());
			
			Set<NormalizedTuple> uniqueTups = new HashSet<NormalizedTuple>(); 
			
			Map<String,Set<String>> uniqueAtts = new HashMap<String,Set<String>>();
						
			for (List<Tuple> tupleslist : tuples.values()) {
				
				for (Tuple tuple : tupleslist) {
					
					NormalizedTuple n_tuple;
					
					try{
						n_tuple = (NormalizedTuple) tuple;	
					}catch (ClassCastException e){
						n_tuple = new NormalizedTuple();
						
						for (String field : tuple.getFieldNames()) {
							n_tuple.setTupleField(field,tuple.getFieldValue(field).toLowerCase(),null);
						}
						
					}
					
					for (String field : tuple.getFieldNames()) {
						get(uniqueAtts,field).add(tuple.getFieldValue(field).toLowerCase());
						get(uniqueAtts,"norm_" + field).add(n_tuple.getNormalizedFieldValue(field));
					}

					uniqueTups.add(n_tuple);
					
					
				}
				
			}
			
			for (Entry<String,Set<String>> att : uniqueAtts.entrySet()) {
			
				bwnovel.newLine();
				bwnovel.write(data + "," + relation + "," + extractor + "," + att.getKey() + "," + att.getValue().size());
				
			}
			
			bwnovel.newLine();
			bwnovel.write(data + "," + relation + "," + extractor + "," + "full-tuple" + "," + uniqueTups.size());
			
		}

		bwnovel.close();
		bwuseful.close();
		
	}

	private static Set<String> get(Map<String, Set<String>> uniqueAtts,
			String field) {
		
		Set<String> ret = uniqueAtts.get(field);
		
		if (ret == null){
			
			ret = new HashSet<String>();
			uniqueAtts.put(field, ret);
		
		}
		
		return ret;
		
	}

	private static String getExtractor(String extractor) {
		
		if (extractor.equals("default")){
			return "OC";
		}else if (extractor.equals("Pablo-N-Grams")){
			return "BONG";
		} else if (extractor.equals("Pablo-Sub-sequences")){
			return "SSK";
		}
		
		throw new UnsupportedArchitectureException("No Extractor");
		
	}

}
