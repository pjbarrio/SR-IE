package data.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import model.Tuple;

import com.google.gdata.util.common.base.Pair;

import edu.stanford.nlp.ie.machinereading.structure.Span;

import sentence.splitter.StanfordCoreNLPSentenceSplitter;
import utils.RDFPESExtractor;
import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;

public class SentenceUsefulnessSplit {

	public static final String TUPLES = ".tuples.txt";
	private static final String USEFULNESS = ".usefulness.txt";
	public static final String SPLIT_USELESS =  ".splituseless";
	public static final String SPLIT_USEFUL = ".splituseful";
	private static final String POSITION_DIST = ".positionDist";
	private static final String PERC_POSITION_DIST = ".percentagepositionDist";

	public static void main(String[] args) throws IOException {
		
		InputFilesGenerator.resetLineSeparator();
		
		String task = args[0]; //e.g., "Train";
		String relation = DataGenerationParameterUtils.relations[Integer.valueOf(args[1])];// e.g., 0 for PersonParty;
		String extractor = args[2]; //e.g., "default";
		String outprefix = args[3]; //e.g., "data/omp/test1/";
		
		create(outprefix, relation, extractor,task);
		
		
//		merge(relation, task);
		

//		obtainDistribution(relation,task);
		
//		obtainPositionDistribution(relation,task);
			
	}

	

	private static void obtainPositionDistribution(String relation, String task) {
		
		Map<Integer, Integer> positions = (Map<Integer,Integer>)SerializationHelper.deserialize("data/example/positionDist." + task + "-" + relation + ".ser");
		Map<Double, Integer> percentagepositions = (Map<Double,Integer>)SerializationHelper.deserialize("data/example/percentagepositionDist." + task + "-" + relation + ".ser");
		
		System.out.println("position, frequency");
		
		for (Entry<Integer,Integer> entry : positions.entrySet()) {
			System.out.format("%d,%d\n",entry.getKey(),entry.getValue());
		}

		System.out.println("positionpercentage, frequency");
		
		for (Entry<Double,Integer> entry : percentagepositions.entrySet()) {
			System.out.format("%f,%d\n",entry.getKey(),entry.getValue());
		}

		
	}

	private static void obtainDistribution(String relation, String task) throws FileNotFoundException {
		
		Map<String,List<String>> uselessSentences = (Map<String,List<String>>)SerializationHelper.deserialize("data/example/splituseless." + task + "-" + relation + ".ser");
		
		Map<String,List<Pair<String,Tuple>>> usefulSentences = (Map<String,List<Pair<String,Tuple>>>)SerializationHelper.deserialize("data/example/splituseful." + task + "-" + relation + ".ser");

		Map<Integer,Integer> freqMap = new HashMap<Integer,Integer>();
		
		freqMap.put(0, uselessSentences.size());
		
		uselessSentences.clear();
		
		int maxTuples = -1;
		
		for (List<Pair<String,Tuple>> tuples : usefulSentences.values()) {
			
			Integer count = freqMap.get(tuples.size());
			
			if (count == null){
				count = 0;
				if (tuples.size() > maxTuples)
					maxTuples = tuples.size();
			}
			
			
			
			freqMap.put(tuples.size(),count+1);
			
		}
		
		System.setOut(new PrintStream("data/example/distribution." + task + "-" + relation + ".csv"));
		
		System.out.format("number.of.tuples,frequency");
		
		for (int i = 0; i <= maxTuples; i++) {
			if (freqMap.containsKey(i)){
				System.out.format("\n%d,%d",i,freqMap.get(i));
			}else{
				System.out.format("\n%d,NA",i);
			}
		}
		
	}

	private static void merge(String relation, String task) {
		
		Map<String,List<String>> uselessSentences = (Map<String,List<String>>)SerializationHelper.deserialize("data/example/splituseless." + task + "-" + relation + ".ser");
				
		Map<String,List<Pair<String,Tuple>>> usefulSentences = (Map<String,List<Pair<String,Tuple>>>)SerializationHelper.deserialize("data/example/splituseful." + task + "-" + relation + ".ser");
		
		Map<String,List<String>> uselessSentences2 = (Map<String,List<String>>)SerializationHelper.deserialize("data/example/splituseless.fixed." + task + "-" + relation + ".ser");
		
		Map<String,List<Pair<String,Tuple>>> usefulSentences2 = (Map<String,List<Pair<String,Tuple>>>)SerializationHelper.deserialize("data/example/splituseful.fixed." + task + "-" + relation + ".ser");
		
		System.out.println(uselessSentences.size());
		
	}

	private static void create(String outprefix, String relation, String extractor, String task) throws IOException {
		
		File[] files = (File[])SerializationHelper.deserialize(outprefix + DataGenerationParameterUtils.getFileListName(task));
		
		File f = new File(outprefix + DataGenerationParameterUtils.getAttributesFileName(relation,extractor,task));
		
		Set<String> attributes = new HashSet<String>();
		
		if (f.exists()){
			attributes.addAll((List<String>)SerializationHelper.deserialize(f.getAbsolutePath()));
		}
		
		StanfordCoreNLPSentenceSplitter splitter = new StanfordCoreNLPSentenceSplitter();

		List<Span> sentences;
		List<Pair<Tuple, String[]>> tuples;
		
		Map<String,List<String>> uselessSentences = new HashMap<String, List<String>>();
		
		Map<String,List<Pair<String,Tuple>>> usefulSentences = new HashMap<String,List<Pair<String,Tuple>>>();
		
		Map<Integer,Integer> positionFrequency = new HashMap<Integer,Integer>();
		
		Map<Double,Integer> percentagePositionFrequency = new HashMap<Double,Integer>();
		
		Map<Long, List<Tuple>> tuples_total = new HashMap<Long,List<Tuple>>();
		
		Set<Integer> added;
		
		String content;
		
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(getUsefulnessFile(outprefix,task,relation,extractor)));
		
		boolean first = true;
		
		int currentSentence = 0;
		
		int curr_sent_tuple = 0;
		
		for (int i = 0; i < files.length; i++) {
			
			if (i % 1000 == 0){
				System.out.println("processed: " + i);
				SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + TUPLES + ".ser" , tuples_total);
				SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + SPLIT_USELESS + ".ser" , uselessSentences);
				SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + SPLIT_USEFUL + ".ser" , usefulSentences);
				SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + POSITION_DIST + ".ser" , positionFrequency);
				SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + PERC_POSITION_DIST + ".ser" , percentagePositionFrequency);
			}

			List<String> listuseless = new ArrayList<String>();
			
			List<Pair<String,Tuple>> listuseful = new ArrayList<Pair<String,Tuple>>();
			
			content = RDFPESExtractor.extractContent(files[i].toURI());
			
			if (content == null){
				System.err.format("Empty file: %s\n", files[i].getName());
				continue;
			}
			
			sentences = splitter.tokenizeSentences(content);
			
			tuples = RDFPESExtractor.extract(files[i].toURI(), relation);
			
			added = new HashSet<Integer>(); 
			
			for (Pair<Tuple, String[]> pair : tuples) {
				
				attributes.addAll(Arrays.asList(pair.first.getFieldNames()));
				
				int offset = Integer.valueOf(pair.getSecond()[3]);
				
				for (int j = 0; j < sentences.size(); j++) {
					if (sentences.get(j).contains(offset)){
						added.add(j);
						listuseful.add(new Pair<String, Tuple>(content.substring(sentences.get(j).start(), sentences.get(j).end()), pair.getFirst()));
						update(positionFrequency,new Integer(j));
						update(percentagePositionFrequency,new Double((double)j/(double)sentences.size()));
						update(tuples_total,curr_sent_tuple+j,pair.first);
					} else if (sentences.get(j).start() > offset){ //sentence is after
						break;
					}
				
				}
				
			}
			
			curr_sent_tuple+=sentences.size(); //update the sentence
			
			for (int j = 0; j < sentences.size(); j++) {
				
				int ivalue = 1;
				
				if (!added.contains(j)){
					listuseless.add(content.substring(sentences.get(j).start(), sentences.get(j).end()));
					ivalue = 0;
				}
				
				if (!first){
					bw2.newLine();
				}

				bw2.write(currentSentence + " " + ivalue);

				first = false;

				currentSentence++;
				
			}
			
			if (files[i].getName().endsWith(".rdf")){ //For rdf.0 / rdf.1
				usefulSentences.put(files[i].getName(), listuseful);
				uselessSentences.put(files[i].getName(), listuseless);
			}else{
				String name = files[i].getName().substring(0, files[i].getName().lastIndexOf('.'));
				
				List<Pair<String, Tuple>> aux = usefulSentences.get(name);
				if (aux == null){
					aux = new ArrayList<Pair<String, Tuple>>();
					usefulSentences.put(name, aux);
				}
				aux.addAll(listuseful);
				
				List<String> aux2 = uselessSentences.get(name);
				if (aux2 == null){
					aux2 = new ArrayList<String>();
					uselessSentences.put(name, aux2);
				}
				aux2.addAll(listuseless);
				
			}
					
		}
		
		SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + TUPLES + ".ser" , tuples_total);
		SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + SPLIT_USELESS + ".ser" , uselessSentences);
		SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + SPLIT_USEFUL + ".ser" , usefulSentences);
		SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + POSITION_DIST + ".ser" , positionFrequency);
		SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + PERC_POSITION_DIST + ".ser" , percentagePositionFrequency);
		SerializationHelper.serialize(f.getAbsolutePath(), new ArrayList<String>(attributes));
		
		bw2.close();
		
	}

	public static String getUsefulnessFile(String outprefix, String task,
			String relation, String extractor) {
		
		return outprefix + task + "." + relation + "." + extractor + USEFULNESS;
	
	}



	private static void update(Map<Long, List<Tuple>> tuples,
			int sentence, Tuple first) {
		
		List<Tuple> tups = tuples.get(sentence);
		
		if (tups == null){
			tups = new ArrayList<Tuple>();
			tuples.put((long)sentence, tups);
		}
		
		tups.add(first);
		
	}



	private static <T> void update(Map<T, Integer> map,
			T key) {
		
		Integer freq = map.get(key);
		
		if (freq == null){
			map.put(key, 1);
		}else{
			map.put(key, freq+1);
		}
		
	}
	
}
