package data.statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.opencsv.CSVWriter;

import model.NormalizedTuple;
import model.Tuple;
import data.generation.SentenceUsefulnessSplit;

import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;

public class NYTimesRelationStatisticsDiversity {

	public static void main(String[] args) throws IOException {

		String data = args[0]; //e.g., Train
		String extractor = args[1]; //e.g., "default";
		String outprefix = args[2]; //e.g., data/omp/test1/

		System.setOut(new PrintStream(new File(outprefix + data + "-" + extractor + "-novalue.diversity")));
		
		System.out.println("data,relation,extractor,normalized,attribute,frequency");
		
		
		CSVWriter writer = new CSVWriter(new FileWriter(new File(outprefix + data + "-" + extractor + ".diversity")));
		writer.writeNext(new String[]{"data","relation","extractor","normalized","attribute","frequency","value"});
		
		for (int rela = 0; rela < DataGenerationParameterUtils.relations.length; rela++) {

			System.err.println(rela + "." + DataGenerationParameterUtils.relations.length);
			
			String relation = DataGenerationParameterUtils.relations[rela];// e.g., 0 for PersonParty;


			Map<Long,List<Tuple>> tuples = (Map<Long,List<Tuple>>)SerializationHelper.deserialize(outprefix + data + "." + relation + "." + extractor + SentenceUsefulnessSplit.TUPLES + ".ser");

			Map<String, Map<String,Integer>> vals = new HashMap<String, Map<String,Integer>>();
			Map<String, Map<String,Integer>> refvals = new HashMap<String, Map<String,Integer>>();

			for (List<Tuple> tups : tuples.values()) {

				for (Tuple tuple : tups) {

					NormalizedTuple n_tuple = (NormalizedTuple) tuple;

					for (String field : n_tuple.getFieldNames()) {

						update(get(vals,field),n_tuple.getFieldValue(field));
						update(get(refvals,field),n_tuple.getNormalizedFieldValue(field));

					}

				}

			}



			String prefix = data + "," + relation + "," + extractor;
			String[] prf = new String[]{data,relation,extractor,"","","",""};
			print(prf,writer, prefix,vals,false);
			print(prf,writer,prefix,refvals,true);

		}

		writer.close();
		
	}

	private static void print(String[] prf, CSVWriter writer, String prefix,
			Map<String, Map<String, Integer>> vals, boolean isRef) {

		prf[3] = Boolean.toString(isRef);
		
		for (Entry<String,Map<String,Integer>> entry : vals.entrySet()) {
			
			prf[4] = entry.getKey();
			
			for (Entry<String,Integer> entry_2 : entry.getValue().entrySet()) {

				prf[5] = entry_2.getValue().toString();
				prf[6] = entry_2.getKey();
				
				writer.writeNext(prf);
				
				System.out.println(prefix + "," + isRef + "," + entry.getKey() + "," + entry_2.getValue());
				
			}
		}

	}

	private static Map<String, Integer> get(
			Map<String, Map<String, Integer>> vals, String field) {

		Map<String,Integer> ret = vals.get(field);
		if (ret == null){
			ret = new HashMap<String,Integer>();
			vals.put(field, ret);
		}

		return ret;

	}

	private static void update(Map<String, Integer> vals,
			String fieldValue) {

		Integer freq = vals.get(fieldValue);
		if (freq == null){
			freq = 0;
		}

		vals.put(fieldValue, freq + 1);

	}




}
