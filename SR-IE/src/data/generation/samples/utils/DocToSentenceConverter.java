package data.generation.samples.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import utils.SerializationHelper;
import utils.databaseWriter;
import utils.wordmodel.DataGenerationParameterUtils;

import com.google.gdata.util.common.base.Pair;

import data.generation.CreateSentences;
import data.generation.InputFilesGenerator;

public class DocToSentenceConverter {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		InputFilesGenerator.resetLineSeparator();

		String task = args[0]; //e.g., "Train";
		String outprefix = args[1]; //e.g., "data/omp/test1/";
		String sampleName = args[2]; //e.g., "sample/...";

		List<String> finalSample = new ArrayList<String>();

		if (DataGenerationParameterUtils.isLuceneBased(task)){

			Map<String,List<Long>> docSentencesMap = (Map<String,List<Long>>)SerializationHelper.deserialize(CreateSentences.getDocSentMapName(outprefix, task));

			List<String> sample = (List<String>)SerializationHelper.deserialize(sampleName);

			for (String doc : sample) {

				List<Long> sents = docSentencesMap.get(doc);

				for (int j = 0; j < sents.size(); j++) {

					finalSample.add(Long.toString(sents.get(j)));

				}

			}

		}else{

			databaseWriter dW = new databaseWriter();
			
//			Map<String,Pair<Integer,Integer>> docSentencesMap = (Map<String,Pair<Integer,Integer>>)SerializationHelper.deserialize(CreateSentences.getDocSentMapName(outprefix, task));

			List<String> sample = (List<String>)SerializationHelper.deserialize(sampleName);
			List<String> cl = new ArrayList<String>();
			for (String doc : sample) {
				cl.add(doc.substring(0, doc.indexOf('.')));
			}
					
			Map<String,Pair<Integer,Integer>> limitMap = dW.getBoundaryMap(1, cl);
			
			for (String name : cl) {
				
				Pair<Integer, Integer> bounds = limitMap.get(name);

				if (bounds == null){
					System.out.println(name + converToRDF(name));
					continue;
				}
				
				for (long i = (long)bounds.first; i <= (long)bounds.second; i++) {

					finalSample.add(Long.toString(i));

				}

			}

		}

		FileUtils.writeLines(new File(outprefix,sampleName.replaceAll(".ser", ".sample")), finalSample);

	}

	private static String converToRDF(String doc) {
		
		if (!doc.endsWith(".rdf")){
			return doc + ".rdf";
		}
		return doc;
	}

}
