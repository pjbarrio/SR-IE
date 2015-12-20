package data.generation.target;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deeplearning4j.models.word2vec.Word2Vec;

import data.generation.InputFilesGenerator;

import utils.MapBasedComparator;
import utils.SerializationHelper;
import utils.wordmodel.MyWord2VecLoader;

public class TfIdfTargetBaselineGenerator {

	public static final String TF_IDF_VALUES = "tf.idf.unfiltered.txt";
	
	private static final String TARGET_BASELINE_TF_IDF = ".target.baseline." + TF_IDF_VALUES;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		String data = args[0];
		String relation = args[1];
		String extractor = args[2];
		String word2vec = args[3]; //e.g., "C:/Users/Pablo/Downloads/GoogleNews-vectors-negative300.bin"
		String outprefix = args[4]; //
		
		System.out.println("Loading word2vec");
		
		Set<String> wordset = InputFilesGenerator.loadWordSet(data);
		
		MyWord2VecLoader w2vl = new MyWord2VecLoader();
		
		Word2Vec model = w2vl.loadModel(word2vec, true,wordset,false);

		Map<Integer, Double> tfIdfMap = (Map<Integer, Double>) SerializationHelper.deserialize(outprefix + InputFilesGenerator.TF_IDF_VALUES);
		
		Map<Integer, String> invertedIndexMap = (Map<Integer, String>) SerializationHelper.deserialize(outprefix + InputFilesGenerator.INVERTED_INDEX_MAP);
	
		System.out.println(tfIdfMap.size() + " - " + invertedIndexMap.size());
		
		List<Integer> indexes = new ArrayList<Integer>(tfIdfMap.keySet());
		
		Collections.sort(indexes, new MapBasedComparator<>(tfIdfMap, true));
		
		List<Integer> terms = Arrays.asList(new Integer[]{1000});
		
		for (Integer term : terms) {
		
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outprefix + "." + term + TARGET_BASELINE_TF_IDF)));

			boolean first = true;
			
			for (Integer index : indexes) {
				
				if (term-- == 0)
					break;
				
				if (!first)
					bw.newLine();

				bw.write(InputFilesGenerator.prettyPrint(model.getWordVector(invertedIndexMap.get(index))));
				first = false;
				
			}
			
			bw.close();
			
		}
		
		
		
	}

}
