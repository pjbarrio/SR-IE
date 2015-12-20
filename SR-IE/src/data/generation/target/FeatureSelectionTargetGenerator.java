package data.generation.target;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deeplearning4j.models.word2vec.Word2Vec;

import utils.SerializationHelper;
import utils.wordmodel.MyWord2VecLoader;
import data.generation.InputFilesGenerator;

public class FeatureSelectionTargetGenerator {

	public static final String TARGET = "target.txt";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		String data = args[0]; //e.g., "Train"
		String relation = args[1];
		String extractor = args[2];
		String word2vec = args[3]; //e.g., "C:/Users/Pablo/Downloads/GoogleNews-vectors-negative300.bin"
		String outprefix = args[4]; //
		String featureselector = args[5]; //e.g., CfsSubsetEval
		String acceptable = args[6]; //e.g., 0.95

		System.out.println("Loading word2vec");

		Set<String> wordset = InputFilesGenerator.loadWordSet(data);

		MyWord2VecLoader w2vl = new MyWord2VecLoader();

		Word2Vec model = w2vl.loadModel(word2vec, true,wordset,false);

		List<Integer> indices = (ArrayList<Integer>)SerializationHelper.deserialize(outprefix + data + "." + relation + "." + extractor + "." + featureselector + "." + acceptable + "." + InputFilesGenerator.CANDIDATE + ".ser");

		Map<Integer, String> invertedIndexMap = (Map<Integer, String>) SerializationHelper.deserialize(outprefix + data + "." +  InputFilesGenerator.INVERTED_INDEX_MAP);

		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outprefix + data + "." +  relation + "." + extractor + "." + featureselector + "." + acceptable + "." + TARGET)));

		boolean first = true;

		for (Integer index : indices) {

			if (!first)
				bw.newLine();

			bw.write(InputFilesGenerator.prettyPrint(model.getWordVector(invertedIndexMap.get(index))));
			first = false;

		}

		bw.close();

	}

}
