package data.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.deeplearning4j.models.word2vec.Word2Vec;

import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;

public class InputFilesGeneratorValues {

	private static final String VALUES = "values.txt";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		InputFilesGenerator.resetLineSeparator();
		
		String data = args[0]; //e.g., Train
		String word2vec = args[1]; //e.g., "C:/Users/Pablo/Downloads/GoogleNews-vectors-negative300.bin"
		boolean binary = DataGenerationParameterUtils.isBinary(word2vec);
		String outprefix = args[2]; //e.g., data/omp/test1/
		
		String word2Name =new File(word2vec).getName();

		String valuesFile = outprefix + data + "." + word2Name + "." + VALUES;
		
		if (new File(valuesFile).exists()){
			System.out.println("Values file exists");
			return;
		}

		
		Set<String> wordset = CreateWordMap.loadWordSet(outprefix,data, InputFilesGenerator.normalizeWord2Vec(word2Name));
		
		Map<Integer,String> invertedIndexMap = (Map<Integer,String>) SerializationHelper.deserialize(InputFilesGenerator.getInvertedIndexMap(outprefix,data,InputFilesGenerator.normalizeWord2Vec(word2Name)));
				
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(valuesFile)));
		
		boolean first = true;

		int totalEntries = invertedIndexMap.size();
		
		Word2Vec model = DataGenerationParameterUtils.getLoader(word2vec).loadModel(word2vec, binary,wordset,false);
		
		for (int i = 0; i < totalEntries; i++) {
		
			if (!first)
				bw.newLine();

			bw.write(InputFilesGenerator.prettyPrint(model.getWordVector(invertedIndexMap.get(i))));
			first = false;
			
		}
		
		bw.close();

	}

}
