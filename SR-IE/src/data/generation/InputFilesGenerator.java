package data.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.deeplearning4j.models.word2vec.Word2Vec;

import com.google.gdata.data.threading.Total;

import edu.stanford.nlp.ie.machinereading.structure.Span;

import sentence.similarity.CompareSentences;
import sentence.splitter.StanfordCoreNLPSentenceSplitter;
import utils.JSONLoader;
import utils.RDFPESExtractor;
import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;
import utils.wordmodel.MyWord2VecLoader;
import weka.core.UnassignedClassException;

public class InputFilesGenerator {

	private static final Set<String> google_relations = new HashSet<String>(Arrays.asList(new String[]{"20131104-education-degree","20131104-date_of_birth","20131104-place_of_death",
			"20130403-place_of_birth","20130403-institution"}));

	
	private static final String CANDIDATE = "candidates.txt";
	private static final String FREQUENCY = "frequency.txt";
	private static final String INVERTED_INDEX_MAP = "inverted.index.map";
		
	/**
	 * I will save all words and store the frequency of words, so that we can choose the prohibit indexes
	 * by frequency and by list (e.g., stopwords)
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
				
		resetLineSeparator();
		
		String data = args[0]; //e.g., Train
		String word2vec = args[1]; //e.g., "C:/Users/Pablo/Downloads/GoogleNews-vectors-negative300.bin"
		int longestNGram = DataGenerationParameterUtils.getLongestNGram(word2vec);
		String outprefix = args[2]; //e.g., data/omp/test1/

		String word2Name = normalizeWord2Vec(new File(word2vec).getName());
		
		String candidateFile = getCandidatesFileName(outprefix,data,word2Name);
		
		if (new File(candidateFile).exists()){
			System.out.println("Input File Exists");
			return;
		}
		
		System.out.println("Loading word2vec");

		Set<String> wordset = CreateWordMap.loadWordSet(outprefix,data, word2Name);
		
		int index = 0;
		
		Map<String,Integer> indexMap = new HashMap<String,Integer>(wordset.size());
		
		Map<Integer,String> invertedIndexMap = new HashMap<Integer,String>(wordset.size());
		
		Map<Integer,Integer> mapFreq = new HashMap<Integer,Integer>();
		
		System.out.println("Saving values matrix");
		
		for (String word : wordset) {
			
			mapFreq.put(index, 0);
			
			invertedIndexMap.put(index,word);
			
			indexMap.put(word, index++);
					
		}
		
		SerializationHelper.serialize(getInvertedIndexMap(outprefix,data,word2Name), invertedIndexMap);
		
		int currentSentence;
		
		System.out.println("Saving candidate matrix");
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(candidateFile));
		
		boolean first = true;
		
		Integer maxWords = Integer.MIN_VALUE;
		
		Integer totalWords = 0;
		
		String longestSentence = null;
		
		currentSentence = 0;
				
		StanfordCoreNLPSentenceSplitter splitter = new StanfordCoreNLPSentenceSplitter();
		
		File[] files = (File[])SerializationHelper.deserialize(outprefix + DataGenerationParameterUtils.getFileListName(data));
		
		String content;
		
		Tokenizer tokenizer = CreateWordMap.getTokenizer();
		
		for (int i = 0; i < files.length; i++) {
			
			if (i % 1000 == 0)
				System.out.format("Processing: %f, %d\n",(double)i * 100/(double)files.length, currentSentence);
			
			content = RDFPESExtractor.extractContent(files[i].toURI());
			
			if (content == null){
				System.err.format("Empty file: %s\n", files[i].getName());
				continue;
			}
			
			List<Span> sentences = splitter.tokenizeSentences(content);
			
			for (int j = 0; j < sentences.size(); j++) {
				
				String sentence = content.substring(sentences.get(j).start(), sentences.get(j).end());
				
				Set<String> words = CreateWordMap.obtainWords(sentence, tokenizer, longestNGram, wordset);
						
				Set<String> chosenWords = new HashSet<String>();
				
				if (!first){
					bw.newLine();
				}
				
				bw.write(Integer.toString(currentSentence));
				
				for (String word : words) {
					if (chosenWords.add(word))
						updateWord(" ",word,indexMap,mapFreq,bw);

				}

				totalWords+=chosenWords.size();
				
				if (maxWords < chosenWords.size()){
					maxWords = chosenWords.size();
					longestSentence = sentence;
				}

				chosenWords.clear();
				first = false;
				
				currentSentence++;
				
			}
			
		}
		
		bw.close();
		
		double totalSentences = (double)currentSentence;
		
		System.out.println("Saving frequency data");
		
		bw = new BufferedWriter(new FileWriter(new File(outprefix + data + "." + word2Name + "." + FREQUENCY)));
		
		bw.write(Double.toString((double)mapFreq.get(0)/totalSentences));
		
		for (int i = 1; i < index; i++) {
			
			bw.write(" " + (double)mapFreq.get(i)/totalSentences);
			
		}
		
		bw.close();
		
		System.out.format("Max words in Sentence: %d \n", maxWords);

		System.out.println("Longest Sentence: " + longestSentence);
		
		System.out.format("Average words in Sentence: %f \n", (double)totalWords / (double)totalSentences);
		
	}

	public static String normalizeWord2Vec(String name) {
		
		String prefix = "";
		if (name.contains("vectors.txt") || name.contains("glo")){
			prefix = "glo.";
		}
		
		if (name.contains("data.txt")){
			return prefix + "data.txt";
		}else if (name.contains("data-phrase-unigram.txt")){
			return prefix + "data-phrase-unigram.txt";
		}else if (name.contains("data-phrase-bigram.txt")){
			return prefix + "data-phrase-bigram.txt";
		}else if (name.contains("data-phrase-trigram.txt")){
			return prefix + "data-phrase-trigram.txt";
		}
		
		throw new UnassignedClassException();
		
	}

	public static String getInvertedIndexMap(String outprefix, String data,
			String word2Name) {
		return outprefix + data + "." + word2Name + "." + INVERTED_INDEX_MAP;
	}

	public static String getCandidatesFileName(String outprefix, String data,
			String word2Name) {
		return outprefix + data + "." + word2Name + "." + CANDIDATE;
	}

	public static void resetLineSeparator() {
		System.setProperty("line.separator", "\n");
	}

	private static void updateWord(String prefix, String word,
			Map<String, Integer> indexMap, Map<Integer, Integer> mapFreq,
			BufferedWriter bw) throws IOException {
		
		int index = indexMap.get(word);
		
		bw.write(prefix + index);
		
		Integer prev = mapFreq.get(index);
		
		mapFreq.put(index, prev+1);
		
	}

	public static String prettyPrint(double[] wordVector) {
		
		return Arrays.toString(wordVector).replaceAll("]|,|\\[","");
		
	}
	
}
