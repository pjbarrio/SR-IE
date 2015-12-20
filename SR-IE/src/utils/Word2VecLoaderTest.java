package utils;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.loader.Word2VecLoader;

import com.google.gdata.util.common.base.Pair;

import data.generation.CreateSentences;

import utils.wordmodel.MyWord2VecLoader;


public class Word2VecLoaderTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		HashMap<String,Pair<Integer, Integer>> totSents = (HashMap<String,Pair<Integer, Integer>>)SerializationHelper.deserialize(CreateSentences.getDocSentMapName("data/omp/train/", "Train"));
		
		int maxSent = -1;
		
		for (Pair<Integer,Integer> sentz : totSents.values()) {
			if (sentz.second > maxSent){
				maxSent = sentz.second;
			}
		}
		
		System.out.println("Saving only once");
		
		SerializationHelper.serialize(CreateSentences.getSentenceSizeName("data/omp/train/", "Train"), Integer.valueOf(maxSent+1));
		
		System.out.println(maxSent);
		
		System.exit(0);
		
		MyWord2VecLoader w2vl = new MyWord2VecLoader();
		
		Word2Vec model = w2vl.loadModel("C:/Users/Pablo/Downloads/GoogleNews-vectors-negative300.bin", true,0.1);

		System.out.println(model.wordsNearest("France", 10));
		
		System.out.println("\n" + Arrays.toString(model.getWordVector("france")));
		System.out.println("\n" + Arrays.toString(model.getWordVector("spain")));
	}

}
