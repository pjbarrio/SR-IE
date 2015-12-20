package evaluation;

import it.unimi.dsi.parser.Entity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import model.NormalizedTuple;
import model.Tuple;

import org.apache.commons.io.FileUtils;
import org.ietf.jgss.Oid;
import org.jblas.exceptions.UnsupportedArchitectureException;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import data.generation.SentenceUsefulnessSplit;

import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;

public class TuplesNovelty {

	public static final String CAND_SENTENCE_STRING = "candidate";
	private static final Object RELATION_STRING = "relation";
	private static final Object EXTRACTOR_STRING = "extractor";
	private static final Object DATA_STRING = "data";
	private static final Object POSITION_STRING = "position";

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO The idea is that for each position, we can say if att_1, att_2, att_n is new (0,1)
		// Then in R, we can do cumsums and stuff.

		String output_folder = "data/evaluation/";

		int numOfSkips = 1000;

		int currentSplit = numOfSkips;

		Map<String,Set<String>> mapExpts = (Map<String, Set<String>>) SerializationHelper.deserialize(args[0]==null?"exp_summarize.summary":args[0]+"_exp_summarize.summary");

		for (String experiment : mapExpts.keySet()) {

			if (!experiment.equals(args[0]))
				continue;

			Map<Long,List<Tuple>> tuples = null;
			List<String> attributes = null;
			Map<String,Set<String>> tuples_atts = null;
			List<NormalizedTuple> all_unique_tuples = null;


			String usef_file = output_folder + experiment + ".useful";

			String outputFile = output_folder + experiment + ".novel";
			String outputFileSpls = output_folder + experiment + ".novel.splits";

			CSVReader reader = new CSVReader(new FileReader(usef_file));

			//get candidate position

			String [] nextLine = reader.readNext();

			int datai=-1,relationi=-1,extractori=-1,candi=-1,positioni = -1;

			for (int i = 0; i < nextLine.length; i++) {

				if (nextLine[i].trim().equals(CAND_SENTENCE_STRING)){
					candi = i;
				} else if (nextLine[i].trim().equals(RELATION_STRING)){
					relationi = i;
				} else if (nextLine[i].trim().equals(EXTRACTOR_STRING)){
					extractori=i;
				} else if (nextLine[i].trim().equals(DATA_STRING)){
					datai=i;
				} else if (nextLine[i].trim().equals(POSITION_STRING)){
					positioni=i;
				}

			}

			CSVWriter writer = new CSVWriter(new FileWriter(outputFile));
			CSVWriter writerspls = new CSVWriter(new FileWriter(outputFileSpls));

			List<String> entries = new ArrayList<String>();

			for (int j = 0; j < nextLine.length; j++) {
				entries.add(nextLine[j].trim());

			}

			entries.add("attribute");
			entries.add("is.novel");
			entries.add("total");

			writer.writeNext(entries.toArray(new String[entries.size()]));
			writerspls.writeNext(entries.toArray(new String[entries.size()]));

			String curdata = ""; //e.g., Train
			String currelation = "";// e.g., 0 for PersonParty;
			String curextractor = ""; //e.g., "default";


			String data = ""; //e.g., Train
			String relation = "";// e.g., 0 for PersonParty;
			String extractor = ""; //e.g., "default";
			String outprefix = ""; //e.g., data/omp/test1/

			String line[],sentence;

			int position = Integer.MAX_VALUE;

			Map<String,List<String>> ltsentriesspl = null;

			while ((line = reader.readNext()) != null) {

				//Check if relations, etc. has changed.

				if (!currelation.equals(line[relationi]) || !curextractor.equals(line[extractori]) || 
						!curdata.equals(line[datai]) || Integer.valueOf(line[positioni].trim())<position){

					curdata = line[datai];
					curextractor = line[extractori];
					currelation = line[relationi];

					data = line[datai].trim();
					extractor = getExtractor(line[extractori].trim());
					relation = getRelation(line[relationi].trim(),extractor);
					outprefix = data.equals("Train")?"data/train/":"data/ltrain/";

					System.out.println(experiment + " - " + data + " - " + extractor + " - " + relation);

					tuples = (Map<Long,List<Tuple>>)SerializationHelper.deserialize(outprefix + data + "." + relation + "." + extractor + SentenceUsefulnessSplit.TUPLES + ".ser");
					attributes = (List<String>)SerializationHelper.deserialize(outprefix + DataGenerationParameterUtils.getAttributesFileName(relation, extractor, data)); 

					tuples_atts = new HashMap<String, Set<String>>();

					if (!attributes.isEmpty()){

						for (String att : attributes) {
							tuples_atts.put(att, new HashSet<String>());
							tuples_atts.put("norm_" + att, new HashSet<String>());
						}

					}else{

						attributes = new ArrayList<String>();

						for (List<Tuple> tupls : tuples.values()) {

							for (Tuple tuple : tupls) {

								for (String field : tuple.getFieldNames()) {

									if (!tuples_atts.containsKey(field)){
										tuples_atts.put(field, new HashSet<String>());
										tuples_atts.put("norm_" + field, new HashSet<String>());
									}

								}

							}

						}

						SerializationHelper.serialize(outprefix + DataGenerationParameterUtils.getAttributesFileName(relation, extractor, data),attributes);

					}

					all_unique_tuples = new ArrayList<NormalizedTuple>();

					currentSplit = numOfSkips;

					ltsentriesspl = new HashMap<String, List<String>>();				

					for (String att : attributes) {
						ltsentriesspl.put(att, new ArrayList<String>());
						ltsentriesspl.put("norm_" + att, new ArrayList<String>());
					}
					ltsentriesspl.put("full-tuple", new ArrayList<String>());

				}
				if (candi >= line.length){
					System.err.println(Arrays.toString(line));
					continue;
				}
				sentence = line[candi].trim();
				position = Integer.valueOf(line[positioni].trim());

				entries.clear();

				for (int j = 0; j < line.length; j++) {
					entries.add(line[j].trim());
				}

				while (position > currentSplit){

					for (Entry<String,List<String>> lastspl : ltsentriesspl.entrySet()) {

						if (lastspl.getValue().isEmpty()){

							for (int j = 0; j < line.length; j++) {
								lastspl.getValue().add(line[j].trim());
							}

							lastspl.getValue().add(lastspl.getKey());
							lastspl.getValue().add("0");
							lastspl.getValue().add("0");



						}

						String[] ent = lastspl.getValue().toArray(new String[lastspl.getValue().size()]);

						ent[positioni] = Integer.toString(currentSplit);

						writerspls.writeNext(ent);

					}

					currentSplit+=numOfSkips;
				}

				List<Tuple> tups = tuples.get(Long.valueOf(sentence));

				Map<String,Boolean> isNov = new HashMap<String,Boolean>();

				if (tups != null){

					for (Tuple tuple : tups) {

						NormalizedTuple n_tuple;

						try{
							n_tuple = (NormalizedTuple) tuple;	
						}catch (ClassCastException e){
							n_tuple = new NormalizedTuple();

							for (String field : tuple.getFieldNames()) {
								n_tuple.setTupleField(field,tuple.getFieldValue(field).toLowerCase(),null);
							}

						}

						boolean added = false;

						for (String field : tuple.getFieldNames()) {

							boolean ad = tuples_atts.get(field).add(tuple.getFieldValue(field).toLowerCase());

							if (isNov.get(field) == null || ad)
								isNov.put(field, ad);

							ad = tuples_atts.get("norm_" + field).add(n_tuple.getNormalizedFieldValue(field));

							if (isNov.get("norm_" + field) == null || ad)
								isNov.put("norm_" + field, ad);

							added = added || ad;
						}

						if (added || isNovel(all_unique_tuples,n_tuple)){

							all_unique_tuples.add(n_tuple);

							isNov.put("full-tuple", true);
						}else{
							if (isNov.get("full-tuple") == null)
								isNov.put("full-tuple", false);
						}

					}

				} else {
					System.err.println("Why is it empty!?");
				}


				for (Entry<String,Boolean> novelty : isNov.entrySet()) {

					List<String> tmpentries = new ArrayList<String>(entries);

					tmpentries.add(novelty.getKey());
					tmpentries.add(novelty.getValue()? "1":"0");
					if ("full-tuple".equals(novelty.getKey())){
						tmpentries.add(Integer.toString(all_unique_tuples.size()));
					}else{
						tmpentries.add(Integer.toString(tuples_atts.get(novelty.getKey()).size()));
					}
					writer.writeNext(tmpentries.toArray(new String[tmpentries.size()]));

					ltsentriesspl.remove(novelty.getKey());
					ltsentriesspl.put(novelty.getKey(), tmpentries);

				}				


			}




			reader.close();
			writer.close();		
			writerspls.close();


		}



	}

	private static String getRelation(String trim, String extractor) {
		if (extractor.equals("default")){
			return trim;
		}

		for (int i = 0; i < DataGenerationParameterUtils.reelprintablerelatons.length; i++) {

			if (trim.equals(DataGenerationParameterUtils.reelprintablerelatons[i])){
				return DataGenerationParameterUtils.reelrelations[i];
			}

		}

		throw new UnsupportedArchitectureException("No relation!");

	}

	private static String getExtractor(String trim) {

		if (trim.equals("OC")){
			return "default";
		}else if (trim.equals("BONG")){
			return "Pablo-N-Grams";
		}else if (trim.equals("SSK")){
			return "Pablo-Sub-sequences";
		}

		return null;

	}

	public static boolean isNovel(List<NormalizedTuple> all_tuples,
			NormalizedTuple n_tuple) {

		boolean novel = true;

		for (int i = 0; i < all_tuples.size(); i++) {

			if (isSameTuple(all_tuples.get(i),n_tuple)){
				return false;
			}

		}

		return novel;
	}

	private static boolean isSameTuple(NormalizedTuple existing_tuple,
			NormalizedTuple new_tuple) {

		if (existing_tuple.getFieldNames().length < new_tuple.getFieldNames().length)
			return false;

		for (String field : new_tuple.getFieldNames()) {

			String existing_value = existing_tuple.getFieldValue(field);

			if (existing_value == null){

				return false;

			}else{ //The field exists

				//Need to check if it refers to the same (normalized) entity
				String new_value = new_tuple.getNormalizedFieldValue(field);

				if (NormalizedTuple.NON_NORMALIZABLE.equals(new_value)){

					new_value = new_tuple.getFieldValue(field);

				} else{

					existing_value = existing_tuple.getNormalizedFieldValue(field);

				}

				//what about existing_value being non_normalizable? Next condition will say FALSE.

				if (!existing_value.toLowerCase().equals(new_value.toLowerCase())) //it's all in lowercase
					return false;

			}

		}

		return true;

	}

	public static String getOutputFileName(String outprefix, String fileName) {
		return outprefix + "output/" + fileName;
	}

}
