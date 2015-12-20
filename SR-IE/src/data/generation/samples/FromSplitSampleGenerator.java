package data.generation.samples;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import model.Tuple;

import data.generation.CreateSentences;
import data.generation.InputFilesGenerator;
import data.generation.SentenceUsefulnessSplit;

import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;

public class FromSplitSampleGenerator {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		InputFilesGenerator.resetLineSeparator();
		
		String data = args[0]; //e.g., "Train"
		String relation = DataGenerationParameterUtils.relations[Integer.valueOf(args[1])]; //e.g., "PersonParty"
		String extractor = args[2]; //e.g., "default"
		String outprefix = args[3];
		int size = Integer.valueOf(args[4]);
		double fractionuseful = Double.valueOf(args[5]);
		int seed = Integer.valueOf(args[6]);
		
		List<String> sample = new ArrayList<String>();
		
		Map<Long, List<Tuple>> tuples = (Map<Long, List<Tuple>>)SerializationHelper.deserialize(outprefix + data + "." + relation + "." + extractor + SentenceUsefulnessSplit.TUPLES + ".ser");

		Integer totSents = (Integer)SerializationHelper.deserialize(CreateSentences.getSentenceSizeName(outprefix, data));
		
		//Find useful sentences
		
		int numUseful = (int)Math.round(fractionuseful*size);
		
		List<Long> usefuls = new ArrayList<Long>(tuples.keySet());
		
		Collections.shuffle(usefuls,new Random(seed));
		
		for (int i = 0; i < usefuls.size() && i < numUseful; i++) {
			sample.add(usefuls.get(i).toString());
		}
		
		int numUseless = size - numUseful;
		
		int curSize = sample.size();
		
		Random generator = new Random(seed);
		
		while (sample.size() - curSize < numUseless){
			//Until we collect numUseless
			
			long sent = Math.round(generator.nextDouble()*totSents.doubleValue());
			
			if (!tuples.containsKey(sent)){
				sample.add(Long.toString(sent));
			}
			
		}
		
		new File(outprefix + "sample/").mkdirs();
		
		FileUtils.writeLines(new File(outprefix + "sample/" + data + ".FromSplit." + relation + "." + extractor + "." + size + "." + fractionuseful + "." + seed + ".sample"), sample);
		
	}

}
