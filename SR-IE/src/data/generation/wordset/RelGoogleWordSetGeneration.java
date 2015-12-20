package data.generation.wordset;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import utils.JSONLoader;
import utils.SerializationHelper;

public class RelGoogleWordSetGeneration {
private static Tokenizer tokenizer;

/**
	 * @param args
 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String[] relations = {"20131104-education-degree","20131104-date_of_birth","20131104-place_of_death",
				"20130403-place_of_birth","20130403-institution"};

		for (String relation : relations) {
			
			Set<String> wordSet = new HashSet<String>();
			
			System.out.println(relation);
			
			String file = "D:\\OneDrive\\SentenceRanking\\"+relation+".json";
			
			Map<String,List<String>> map = JSONLoader.loadMap(file);

			for (List<String> entry : map.values()) {
				
				for (String sentence : entry) {
					
					for (String word : getTokenizer().tokenize(sentence)) {
						
						wordSet.add(word);
						
					}
					
				}
				
			}
			
			System.out.format("Size: %d\n",wordSet.size());
			
			SerializationHelper.serialize("data\\word.set."+relation, wordSet);
			
		}
			
	}

	protected static Tokenizer getTokenizer() {

		if (tokenizer == null){

			try {
				InputStream modelIn = new FileInputStream("model\\en-token.bin");
				TokenizerModel model = new TokenizerModel(modelIn);
				tokenizer = new TokenizerME(model);
				modelIn.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}

		} 

		return tokenizer;

	}
	
}
