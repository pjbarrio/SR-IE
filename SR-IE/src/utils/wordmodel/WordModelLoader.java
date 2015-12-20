package utils.wordmodel;

import java.io.IOException;
import java.util.Set;

import org.deeplearning4j.models.word2vec.Word2Vec;

public abstract class WordModelLoader<T extends Word2Vec> {

	public abstract T loadModel(String path, boolean binary, double fraction)
			throws IOException;

	public abstract T loadModel(String path, boolean binary, Set<String> wordSet,
			boolean normalized) throws IOException;

	public abstract Set<String> loadDictionary(String path, boolean binary) throws IOException;
	
}
