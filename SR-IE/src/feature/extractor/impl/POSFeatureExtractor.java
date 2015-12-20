package feature.extractor.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.InvalidFormatException;

import feature.extractor.FeatureExtractor;

public class POSFeatureExtractor extends FeatureExtractor<String[]> {

	private static POSTaggerME tagger;

	private static SentenceFeatureExtractor sfe = new SentenceFeatureExtractor();
	private static TokensFeatureExtractor tfe = new TokensFeatureExtractor();
	@Override
	protected Map<String, Double> getFeatures(String snippet) {

		Map<String,Double> feats = new HashMap<String,Double>();

		String[] sentences = sfe.getFeatureSequences(snippet);

		for (String sentence : sentences) {

			String[] tags = getFeatureSequences(tfe.getFeatureSequences(sentence));

			for (String tag : tags) {

				feats.put(tag, 1.0);

			}

		}
		
		return feats;
	
	}

	private POSTaggerME getPOStagger() {

		if (tagger == null){
			try {
				FileInputStream modelIn = new FileInputStream("model\\en-pos-maxent.bin");
				POSModel model = new POSModel(modelIn);
				tagger = new POSTaggerME(model);
				modelIn.close();
			} catch (InvalidFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return tagger;
	}

	@Override
	protected String[] getFeatureSequences(String[] input){
		return getPOStagger().tag(input);
	}

	@Override
	public void initialize(String relation) {
		;
	}

}
