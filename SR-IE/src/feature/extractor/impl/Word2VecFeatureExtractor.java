package feature.extractor.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deeplearning4j.models.word2vec.Word2Vec;

import data.generation.CreateWordMap;
import distance.measure.DistanceMeasure;
import distance.measure.impl.FullCosineSimilarity;

import utils.SerializationHelper;
import utils.wordmodel.MyWord2VecLoader;
import utils.wordmodel.WordModelLoader;

import feature.extractor.FeatureExtractor;

public class Word2VecFeatureExtractor extends FeatureExtractor<String> {

	private Word2Vec model;

	private int size;

	private String name;

	private WordModelLoader<? extends Word2Vec> loader;

	private String path;

	private String outprefix;

	private String task;

	private String word2Name;
	
	public Word2VecFeatureExtractor(int vectorsize, WordModelLoader<? extends Word2Vec> loader, String path, String name,
			String outprefix, String task, String word2name) {

		this.name = name;
		this.size = vectorsize;
		this.loader = loader;
		this.path = path;
		this.outprefix = outprefix;
		this.task = task;
		this.word2Name = word2name;
		
		
	}

	@Override
	protected Map<String, Double> getFeatures(String snippet) {
		
		Map<String, Double> feats = new HashMap<String, Double>();

		String[] sentences = sfe.getFeatureSequences(snippet);

		for (String sentence : sentences) {

			String[] tokens = getFeatureSequences(sentence);

			double[] vec = new double[size];
			
			int total = 0;
			
			for (String token : tokens) {

				double[] featvec = model.getWordVector(token);
				
				if (featvec != null){
					vec = add(vec,featvec);
					total++;

				}

			}

			vec = computeMean(vec,total);
			
			for (int i = 0; i < vec.length; i++) {
				feats.put(Integer.toString(i), vec[i]);
			}
			
		}

		return feats;
		
	}

	private double[] computeMean(double[] vec, int total) {
		if (total == 0)
			return vec;
		
		for (int i = 0; i < vec.length; i++) {
			vec[i] /= total;
		}
		
		return vec;
	}

	private double[] add(double[] vec, double[] featvec) {
		
		for (int i = 0; i < featvec.length; i++) {
			vec[i] += featvec[i];
		}
		
		return vec;
	}

	@Override
	protected String[] getFeatureSequences(String input) {

		String[] ret = getTokenizer().tokenize(input);
		
		List<String> arr = new ArrayList<String>();
		
		for (int i = 0; i < ret.length; i++) {
			if (ret[i].length() > 2 && !getStopWords().contains(ret[i].toLowerCase())){
				
				arr.add(ret[i]);
			}
		}
		
		return arr.toArray(new String[0]);
	}

	@Override
	public String getSimpleName() {
		return super.getSimpleName() + "." + name;
	}

	@Override
	public void initialize(String relation) {

		try {
			
			Set<String> wordSet = (Set<String>)SerializationHelper.deserialize(outprefix + task + "." + word2Name + CreateWordMap.WORDSET);
			
			model = loader.loadModel(path, true,wordSet, true);
		
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public DistanceMeasure getDistanceMeasure() {
		return new FullCosineSimilarity(size);
	}

	@Override
	public boolean isSparse() {
		return false;
	}
	
}
