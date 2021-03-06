package data.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import sentence.splitter.StanfordCoreNLPSentenceSplitter;
import utils.RDFPESExtractor;
import utils.SerializationHelper;
import utils.databaseWriter;
import utils.wordmodel.DataGenerationParameterUtils;
import utils.wordmodel.MyWord2VecLoader;
import edu.stanford.nlp.ie.machinereading.structure.Span;

public class CreateWordMapFromDB {

	public static final String WORDSET = ".wordset.txt";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String task = args[0]; //e.g.,  "Train";
		String word2vec =  args[1]; //e.g., "C:/Users/Pablo/Downloads/GoogleNews-vectors-negative300.bin";
		boolean binary = DataGenerationParameterUtils.isBinary(word2vec);
		int longestNgram = DataGenerationParameterUtils.getLongestNGram(word2vec);
		String outprefix = args[2];
		
		createWordMap(outprefix, task, word2vec, binary, longestNgram);
		
	}

	private static void createWordMap(String outprefix, String task, String word2vec, boolean binary, int longestNGram) throws IOException {
		
		String word2Name = InputFilesGenerator.normalizeWord2Vec(new File(word2vec).getName());
		
		String wordMap = outprefix + task + "." + word2Name + WORDSET;
		
		if (new File(wordMap).exists()){
			System.out.println("Word Set Exists");
			return;
		}
		
		Set<String> dictionary = DataGenerationParameterUtils.getLoader(word2vec).loadDictionary(word2vec, binary);
						
		Tokenizer tokenizer = getTokenizer();
		
		Set<String> words = new HashSet<String>();
		
		databaseWriter dW = new databaseWriter();
		
		int colId = DataGenerationParameterUtils.getCollectionId(task);
		
		int totalSentences = dW.getTotalSentences(colId);
		
		int split = 1000;
		
		int current = 0;
		
		while (current < totalSentences){
			
			System.out.format("Processing: %f, %d\n",(double)current * 100/(double)totalSentences, words.size());
			
			List<String> sentences = dW.getSentenceText(colId, current, current+split);
			
			while(!sentences.isEmpty()) {
				
				words.addAll(obtainWords(sentences.remove(0),tokenizer,longestNGram,dictionary));
								
			}
			
			current+=split;
		}
		
		SerializationHelper.serialize(wordMap, words);
			
	}

	public static Set<String> obtainWords(String text, Tokenizer tokenizer, int longestNGram, Set<String> dictionary) {
		
		String[] tokenized_text = tokenizer.tokenize(text);
		
		Set<String> words = new HashSet<String>();
		
		int ngram_size = 0;
		
		for (int k = 0; k < tokenized_text.length; k+=ngram_size) {
			
			ngram_size = longestNGram;
			
			while (ngram_size >= 1) {
				
				String term = buildNGram(tokenized_text,k,ngram_size);						
				
				if (term!=null){
					
					if (dictionary.contains(term)){
						words.add(term);
						break;
						
					}
					
				}

				if (ngram_size == 1){
					break;
				}
				
				ngram_size--;
				
			}
			
		}
		
		return words;
		
	}

	public static Set<String> loadWordSet(String outprefix, String task, String word2Name) {
		return (Set<String>)SerializationHelper.deserialize(outprefix + task + "." + word2Name + WORDSET);
	}
	
	private static String buildNGram(String[] tokenized_text, int k,
			int ngram_size) {
		
		if (k + ngram_size > tokenized_text.length)
			return null;
		
		StringBuilder sb = new StringBuilder(tokenized_text[k]);
		
		for (int i = 1; i < ngram_size; i++) {
			sb.append("_" + tokenized_text[k+i]);
		}
		
		return sb.toString();
		
		
	}

	public static Tokenizer getTokenizer() {
		
		Tokenizer tokenizer = null;
		
		try {
			InputStream modelIn = new FileInputStream("model//en-token.bin");
			TokenizerModel model = new TokenizerModel(modelIn);
			tokenizer = new TokenizerME(model);
			modelIn.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return tokenizer;
		
	}
	
	
	
}
