package utils.wordmodel;

import java.util.HashMap;
import java.util.Map;

import org.deeplearning4j.models.word2vec.Word2Vec;

public class GloVe extends Word2Vec{

	private Map<String, double[]> map;
	
	public GloVe() {
		map = new HashMap<String, double[]>();
	}
	
	public void setWordVector(String word, double[] vector){
		map.put(word, vector);
	}
	
	@Override
	public double[] getWordVector(String word) {
		return map.get(word.toLowerCase());
	}
	
}
