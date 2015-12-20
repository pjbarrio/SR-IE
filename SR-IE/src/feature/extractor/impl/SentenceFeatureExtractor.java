package feature.extractor.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.activity.InvalidActivityException;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import feature.extractor.FeatureExtractor;

public class SentenceFeatureExtractor extends FeatureExtractor<String>{

	private static SentenceDetectorME sentenceDetector;

	@Override
	protected Map<String, Double> getFeatures(String snippet) {
		try {
			throw new InvalidActivityException("This should not be used as a feature extractor");
		} catch (InvalidActivityException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected String[] getFeatureSequences(String input) {
		return getSentenceDetector().sentDetect(input);
	}

	
	private SentenceDetectorME getSentenceDetector(){

		if (sentenceDetector == null){
			try {
				InputStream modelIn = new FileInputStream("model//en-sent.bin");
				SentenceModel model = new SentenceModel(modelIn);
				sentenceDetector = new SentenceDetectorME(model);
				modelIn.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		return sentenceDetector;
	}


}
