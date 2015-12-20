package data.generation.candidates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import utils.SerializationHelper;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffSaver;
import data.generation.InputFilesGenerator;
import data.generation.SentenceUsefulnessSplit;
import data.generation.samples.InterationSampleGenerator;
import data.generation.target.TargetGenerator;

public class IntermediateCandidatesGenerator {

	private static final String ARFF_SUFFIX = ".arff";
	private static final String TERMINATE_SUFFIX = ".terminate";

	private static final int max_samples = 10000;
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		InputFilesGenerator.resetLineSeparator();

		String data = args[0]; //LTrain or Train
		String relation = args[1];
		String extractor = args[2];
		String word2Name = args[3];
		String outprefix = args[4]; 
		String sampleFile = args[5];
		
		String dataFile = getDataFile(sampleFile);
		
		if (new File(dataFile).exists()){
			System.out.println("Data File Exists: " + dataFile);
			System.out.println("But we compute it again");
//			return;
		}
		
		List<String> sortedSample = FileUtils.readLines(new File(sampleFile + InterationSampleGenerator.SAMPLE));
		
		Set<String> sample = new HashSet<String>(sortedSample);

		BufferedReader candidates = new BufferedReader(new FileReader(InputFilesGenerator.getCandidatesFileName(outprefix, data, word2Name)));

		Map<Integer,String> invertedIndexMap = (Map<Integer,String>)SerializationHelper.deserialize(InputFilesGenerator.getInvertedIndexMap(outprefix, data, word2Name));
		
		/*Balance Sample*/
		
		BufferedReader usefulness = new BufferedReader(new FileReader(SentenceUsefulnessSplit.getUsefulnessFile(outprefix, data, relation, extractor)));

		String ufu;
		
		List<String> ufulSample = new ArrayList<String>(); 
		List<String> ulessSample = new ArrayList<String>();
		
		String cand;
		
		boolean allUsefulDone = true;
		
		int tot = 0;
		int sam = 0;
		
		while ((ufu = usefulness.readLine())!=null){
			
			cand = ufu.split(" ")[0];
			
			int us = Integer.valueOf(ufu.split(" ")[1]);
					
			tot+=us;
			
			if (!sample.contains(cand)){
				if (us > 0){
					allUsefulDone = false;
				}
					
				continue;
			}
			
			sam += us;
			
			if (us > 0){
				ufulSample.add(cand);
			}else{
				ulessSample.add(cand);
			}
			
		}
		
		usefulness.close();		
		double percentoftot = (double)sam / (double)tot;
				
		
		System.out.println(sam + " useful out of " + tot + " (" + percentoftot + ") useful sentences have been processed already...");
		
		if (allUsefulDone || percentoftot > 0.95){
			
			new File(getTerminateDataFile(sampleFile)).createNewFile();
			new File(getTerminateErrDataFile(sampleFile)).createNewFile();
			System.out.println("Should be finished because there are no more useful.");
			Thread.sleep(10000);
			System.exit(0);
		
		}
		
		
		
		Set<String> uful = new HashSet<String>(ufulSample.size() > max_samples ? getLastUsefuls(ufulSample,sortedSample) : ufulSample);
		Set<String> uless = new HashSet<String>(ufulSample.size());
		if (ulessSample.size() > uful.size()){
			int seed = dataFile.length();
			Collections.shuffle(ulessSample,new Random(seed));
			uless.addAll(ulessSample.subList(0, ufulSample.size()));
		}else{
			uless.addAll(ulessSample);
		}
		
		StringBuilder sb = new StringBuilder();

		sb.append("@DATA\n");

		String lcand;

		Map<Integer,Integer> featIndex = new HashMap<Integer,Integer>();
		Map<Integer,Integer> invertedFeatIndex = new HashMap<Integer,Integer>();

		int us;
		
		while ((lcand = candidates.readLine())!=null){

			String[] feats = lcand.split(" ");
			
			cand = feats[0];

			boolean useful = uful.contains(cand);
			
			if(!useful && !uless.contains(cand))
				continue;

			int[] featsI = new int[feats.length - 1];

			for (int i = 1; i < feats.length; i++) {

				Integer val = Integer.valueOf(feats[i]);

				Integer index = featIndex.get(val);

				if (index == null){
					index = featIndex.size()+1;
					featIndex.put(val, index);
					invertedFeatIndex.put(index, val);
				}

				featsI[i-1] = index;

			}

			Arrays.sort(featsI);

			sb.append("{");

			if (useful)
				us = 1;
			else
				us=0;

			sb.append(0 + " " + us);

			for (int i = 0; i < featsI.length; i++) {

				sb.append("," + featsI[i] + " " + 1);

			}

			sb.append("}\n");

		}

		candidates.close();
		

		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(dataFile)));

		bw.write("@RELATION " + relation + "-" + extractor);
		bw.newLine();

		bw.write("@ATTRIBUTE usefulness-class {0,1}");
		bw.newLine();

		for (int i = 1; i <= invertedFeatIndex.size(); i++) {

			String att = Utils.quote(invertedIndexMap.get(invertedFeatIndex.get(i)));
			if (att.length() == 1){
				int chcode = (int)att.charAt(0);
				if (65533 == chcode){
					att = "_?";
				}
			}
			
			bw.write("@ATTRIBUTE "+ att +" NUMERIC");
			bw.newLine();
			
		}

		bw.write(sb.toString());

		bw.close();

		
		BufferedReader reader = new BufferedReader(
				new FileReader(dataFile));

		Instances instances = new Instances(reader);
		instances.setClass(instances.attribute(TargetGenerator.USEFUL_INDEX));

		instances = TargetGenerator.cleanInstance(instances);
		
		ArffSaver saver = new ArffSaver();
		saver.setInstances(instances);
		saver.setFile(new File(getReducedDataFile(sampleFile)));
		saver.writeBatch();
		
		
		
	}

	private static List<String> getLastUsefuls(List<String> ufulSample,
			List<String> sortedSample) {
		
		Set<String> tmp = new HashSet<String>(ufulSample);
		
		List<String> samp = new ArrayList<String>();
		
		for (int i = sortedSample.size()-1; i >= 0; i--) {
			
			if (tmp.contains(sortedSample.get(i))){ //is useful
				samp.add(sortedSample.get(i));
				if (samp.size() == max_samples)
					return samp;
			}
			
		}
		
		return samp;
	}

	private static String getTerminateDataFile(String sampleFile) {
		return sampleFile.substring(0,sampleFile.lastIndexOf('.')) + TERMINATE_SUFFIX;
	}
	
	private static String getTerminateErrDataFile(String sampleFile) {
		return sampleFile.substring(0,sampleFile.lastIndexOf('-')+1) + TERMINATE_SUFFIX;
	}
	
	private static String getDataFile(String sampleFile) {
		return sampleFile + ARFF_SUFFIX;
	}

	public static String getReducedDataFile(String sampleFile) {
		return sampleFile + "-reduced" + ARFF_SUFFIX;
	}

}
