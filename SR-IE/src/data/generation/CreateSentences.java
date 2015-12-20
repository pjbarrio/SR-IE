package data.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gdata.util.common.base.Pair;

import edu.stanford.nlp.ie.machinereading.structure.Span;

import sentence.splitter.StanfordCoreNLPSentenceSplitter;
import utils.RDFPESExtractor;
import utils.SerializationHelper;
import utils.databaseWriter;
import utils.wordmodel.DataGenerationParameterUtils;

public class CreateSentences {

	private static final String SENTENCES = ".sentences.txt";
	private static final String SENTENCES_SIZE = ".sentences.size";
	private static final String DOC_SENT_MAP = ".doc_sent.map";

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		String task = args[0]; //e.g., "Train";
		String outprefix = args[1]; //e.g., "data/omp/test1/";

		new File(outprefix).mkdirs();
		
		StanfordCoreNLPSentenceSplitter splitter = new StanfordCoreNLPSentenceSplitter();

		System.out.println("Saving sentences");

		databaseWriter dW = new databaseWriter();
		
		int currentSentence = 0;

		String content;
		
		File[] files = (File[])SerializationHelper.deserialize(outprefix + DataGenerationParameterUtils.getFileListName(task));
		
		int collection = DataGenerationParameterUtils.getCollectionId(task);
		
		Map<String,Pair<Integer,Integer>> docSentencesMap = new HashMap<String,Pair<Integer,Integer>>();
		
		for (int i = 0; i < files.length; i++) {
			
			String name = files[i].getName().substring(0, files[i].getName().indexOf('.'));
			
			if (i % 1000 == 0)
				System.out.format("Processing: %f, %d\n",(double)i * 100/(double)files.length, currentSentence);
			
			content = RDFPESExtractor.extractContent(files[i].toURI());
			
			if (content == null){
				System.err.format("Empty file: %s\n", files[i].getName());
				continue;
			}
			
			List<Span> sentences = splitter.tokenizeSentences(content);
			
			int firstSentence = currentSentence;
			
			for (int j = 0; j < sentences.size(); j++) {
				
				String text = content.substring(sentences.get(j).start(), sentences.get(j).end());
				
				dW.insertSentence(collection, name, currentSentence, text);

				currentSentence++;
				
			}
			
			docSentencesMap.put(files[i].getName(), new Pair<Integer, Integer>(firstSentence, currentSentence-1));
			
		}
		
		dW.closeConnection();

		Integer totalNumberofSentences = currentSentence;
		
		SerializationHelper.serialize(getSentenceSizeName(outprefix,task), totalNumberofSentences);
		SerializationHelper.serialize(getDocSentMapName(outprefix,task), docSentencesMap);
		
		
	}

	public static String getDocSentMapName(String outprefix, String task) {
		return outprefix + task + DOC_SENT_MAP;
	}

	public static String getSentenceSizeName(String outprefix, String task) {
		
		return outprefix + task + SENTENCES_SIZE;
		
	}


}
