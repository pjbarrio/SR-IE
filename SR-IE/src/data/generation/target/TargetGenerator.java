package data.generation.target;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import utils.wordmodel.DataGenerationParameterUtils;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import data.generation.InputFilesGenerator;
import data.generation.candidates.CandidatesGenerator;
import data.generation.samples.FromSplitSampleGenerator;
import data.generation.target.selection.FeatureRankerTargetGeneration;
import data.generation.target.selection.FeatureSelection;
import data.generation.target.selection.UsefulOnlyFeatureSelection;
import data.generation.target.utils.SMOClassifierFeatSelection;
import edu.columbia.cs.ltrie.sampling.queries.generation.ChiSquaredWithYatesCorrectionAttributeEval;

public class TargetGenerator {

	private static Set<String> stopWords;
	public static int USEFUL_INDEX = 0;

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		InputFilesGenerator.resetLineSeparator();

		String data = args[0];
		String extractor = DataGenerationParameterUtils.extractors[Integer.valueOf(args[2])];
		
		String relation;
		
		if (extractor.equals("default")){
			relation = DataGenerationParameterUtils.relations[Integer.valueOf(args[1])];
		}else{
			relation = DataGenerationParameterUtils.reelrelations[Integer.valueOf(args[1])];
		}
		
		
				
		String word2vec = args[3];
		String word2Name = InputFilesGenerator.normalizeWord2Vec(new File(word2vec).getName());
		String outprefix = args[4]; 
		String targetGenerationMethod = args[5];
		String sampleMethod = args[6];

		//For FromSplit, size (args[6]) , fractionuseful (args[7]), and seed (args[8])

		//For DocumentSampler, size (args[6]), sampling_algorithm (args[7]) , split (args[8]), docsPerQuerySample (args[9]), and numQueries (args[10]) 

		String targetOutput = getTargetOutputName(outprefix, targetGenerationMethod, sampleMethod, data, relation, extractor, word2Name, Arrays.copyOfRange(args, 7, args.length));		
		
		if (new File(targetOutput).exists()){
			System.out.println("Target Exists");
			return;
		}
				
		Instances instances;
		
		String reducedDataFile = CandidatesGenerator.getReducedDataFile(outprefix,sampleMethod,data,relation,extractor,word2Name,Arrays.copyOfRange(args, 7, args.length));
		
		if (!new File(reducedDataFile).exists()){

			String dataFile = CandidatesGenerator.getDataFile(outprefix,sampleMethod,data,relation,extractor,word2Name,Arrays.copyOfRange(args, 7, args.length));

			BufferedReader reader = new BufferedReader(
					new FileReader(dataFile));

			instances = new Instances(reader);
			instances.setClass(instances.attribute(USEFUL_INDEX));

			instances = cleanInstance(instances);
			
			ArffSaver saver = new ArffSaver();
			saver.setInstances(instances);
			saver.setFile(new File(reducedDataFile));
			saver.writeBatch();
			
		}else{
			
			BufferedReader reader = new BufferedReader(
					new FileReader(reducedDataFile));

			instances = new Instances(reader);
			instances.setClass(instances.attribute(USEFUL_INDEX));
			
		}
		
		TargetGeneration t = getTargetGenerationMethod(targetGenerationMethod);

		List<String> list = t.selectFeatures(instances);
		
		FileUtils.writeLines(new File(targetOutput), list);
		
		if (t.generatesWeights()){

			String weightOutput = getWeightOutputName(outprefix, targetGenerationMethod, sampleMethod, data, relation, extractor, word2Name, Arrays.copyOfRange(args, 7, args.length));

			BufferedWriter bw = new BufferedWriter(new FileWriter(weightOutput));

			double[] weights = t.getWeights();

			bw.write(InputFilesGenerator.prettyPrint(weights));

			bw.close();

			weightOutput = getOwnWeightOutputName(outprefix, targetGenerationMethod, sampleMethod, data, relation, extractor, word2Name, Arrays.copyOfRange(args, 7, args.length));

			bw = new BufferedWriter(new FileWriter(weightOutput));

			weights = t.getOwnWeights();

			bw.write(InputFilesGenerator.prettyPrint(weights));

			bw.close();
			
		}

	}

	public static Instances cleanInstance(Instances instances) throws Exception {

		int[] counts = instances.attributeStats(instances.classIndex()).nominalCounts;
		double countUseless = (double)counts[0];
		double countUseful = (double)counts[1];

		double min = 0.003 * instances.numAttributes();
		double max = 0.9 * countUseless;

		List<Integer> attsToRemove = new ArrayList<Integer>();

		double[] classValues = instances.attributeToDoubleArray(instances.classIndex());

		int[] usefuls = new int[(int)countUseful];

		int added = 0;

		for (int i = 0; i < instances.numInstances(); i++) {

			if (classValues[i] > 0){//is Useful
				usefuls[added] = i;
				added++;
			}

		}

		for (int i = 0; i < instances.numAttributes(); i++) {

			if (i == USEFUL_INDEX)
				continue;

			if (isStopWord(instances.attribute(i).name())){
				attsToRemove.add(i);
			}
			
			AttributeStats aux = instances.attributeStats(i);

			if (aux.numericStats.sum > max){
				attsToRemove.add(i);
			} else if (aux.numericStats.sum < min){

				double[] attrValues = instances.attributeToDoubleArray(i);

				boolean appearsInUseful = false;

				for (int j = 0; j < usefuls.length && !appearsInUseful; j++) {

					if (attrValues[usefuls[j]] > 0){ //
						appearsInUseful = true;
					}

				}

				if (!appearsInUseful){
					attsToRemove.add(i);
				}

			}

		}

		System.out.println("ToRemove: " + attsToRemove.size());
		
		Remove filter = new Remove();

		int[] iToRemove = new int[attsToRemove.size()];

		for (int i = 0; i < attsToRemove.size(); i++) {
			iToRemove[i] = attsToRemove.get(i);
		}

		filter.setAttributeIndicesArray(iToRemove);
		filter.setInputFormat(instances);

		return Filter.useFilter(instances, filter);

	}

	private static boolean isStopWord(String word) {
		
		return getStopWords().contains(word);
		
	}

	private static Set<String> getStopWords() {
		
		if (stopWords == null){
			try {
				stopWords = new HashSet<String>(FileUtils.readLines(new File("/proj/dbNoBackup/pjbarrio/workspacedb-pc02/SentenceRanking/model/stopWords.txt")));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return stopWords;
		
	}

	private static String getWeightOutputName(String outprefix, String targetGenerator, String sampleMethod, String data, String relation, String extractor, String word2Name, String[] params) {

		new File(outprefix + "target/").mkdirs();

		return getTargetName(outprefix, targetGenerator, sampleMethod, data, relation, extractor, word2Name, params) + ".weight";

	}

	private static String getOwnWeightOutputName(String outprefix, String targetGenerator, String sampleMethod, String data, String relation, String extractor, String word2Name, String[] params) {

		new File(outprefix + "target/").mkdirs();

		return getTargetName(outprefix, targetGenerator, sampleMethod, data, relation, extractor, word2Name, params) + "-own.weight";

	}
	
	public static String getTargetOutputName(String outprefix, String targetGenerator, String sampleMethod, String data, String relation, String extractor, String word2Name, String[] params) {

		new File(outprefix + "target/").mkdirs();

		return  getTargetName(outprefix, targetGenerator, sampleMethod, data, relation, extractor, word2Name, params) + ".rawtarget";

	}

	public static String getTargetMatrixOutputName(String outprefix, String targetGenerator, String sampleMethod, String data, String relation, String extractor, String word2Name, String[] params) {

		new File(outprefix + "target/").mkdirs();

		return  getTargetName(outprefix, targetGenerator, sampleMethod, data, relation, extractor, word2Name, params) + ".target";

	}
	
	private static String getTargetName(String outprefix, String targetGenerator, String sampleMethod, String data, String relation, String extractor, String word2Name, String[] params){
		return outprefix + "target/" + targetGenerator + "." + word2Name + "." + CandidatesGenerator.getSampleName(true,sampleMethod, data,
				relation, extractor, params);
	}

	static TargetGeneration getTargetGenerationMethod(
			String targetGenerationMethod) {
		if (UsefulOnlyFeatureSelection.class.getSimpleName().equals(targetGenerationMethod)){
			return new UsefulOnlyFeatureSelection();
		} else if (ChiSquaredWithYatesCorrectionAttributeEval.class.getSimpleName().equals(targetGenerationMethod)){
			return new FeatureRankerTargetGeneration(new ChiSquaredWithYatesCorrectionAttributeEval(), true);
		} else if (InfoGainAttributeEval.class.getSimpleName().equals(targetGenerationMethod)){
			return new FeatureRankerTargetGeneration(new InfoGainAttributeEval(), true);
		} else if (SMOClassifierFeatSelection.class.getSimpleName().equals(targetGenerationMethod)){
			return new FeatureRankerTargetGeneration(new SMOClassifierFeatSelection(), false);
		}
		return null;
	}

}
