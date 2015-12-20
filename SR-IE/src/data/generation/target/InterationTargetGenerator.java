package data.generation.target;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.word2vec.Word2Vec;

import utils.wordmodel.DataGenerationParameterUtils;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import data.generation.InputFilesGenerator;
import data.generation.candidates.CandidatesGenerator;
import data.generation.candidates.IntermediateCandidatesGenerator;
import data.generation.target.utils.PrincipalComponents;

public class InterationTargetGenerator {

	private static Set<String> stopWords;
	public static int USEFUL_INDEX = 0;

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		InputFilesGenerator.resetLineSeparator();

		String data = args[0];
		String word2vec = args[1];
		String word2Name = InputFilesGenerator.normalizeWord2Vec(new File(word2vec).getName());
		String outprefix = args[2]; 
		String targetGenerationMethod = args[3];
		String reducedFile = args[4];

		String targetOutput = getTargetOutputName(reducedFile);		

		if (!new File(targetOutput).exists()){
			System.out.println("Creating Target");

			Instances instances;

			String reducedDataFile = IntermediateCandidatesGenerator.getReducedDataFile(reducedFile);

			BufferedReader reader = new BufferedReader(
					new FileReader(reducedDataFile));

			instances = new Instances(reader);
			instances.setClass(instances.attribute(USEFUL_INDEX));

			TargetGeneration t = TargetGenerator.getTargetGenerationMethod(targetGenerationMethod);

			List<String> list = t.selectFeatures(instances);

			FileUtils.writeLines(new File(targetOutput), list);

			if (t.generatesWeights()){

				String weightOutput = getWeightOutputName(reducedFile);

				BufferedWriter bw = new BufferedWriter(new FileWriter(weightOutput));

				double[] weights = t.getWeights();

				bw.write(InputFilesGenerator.prettyPrint(weights));

				bw.close();

				weightOutput = getOwnWeightOutputName(reducedFile);

				bw = new BufferedWriter(new FileWriter(weightOutput));

				weights = t.getOwnWeights();

				bw.write(InputFilesGenerator.prettyPrint(weights));

				bw.close();

			}

		}

		String targetmatrix = getMatrixFile(reducedFile);
		
		if (new File(targetmatrix).exists()){
			System.out.println("Matrix exists: " + targetmatrix);
			System.out.println("But we compute it again");
			//			return;
		}
		
//		String rawTarget = TargetGenerator.getTargetOutputName(outprefix, targetGenerationMethod, sampleMethod, data, relation, extractor, word2NameNorm, Arrays.copyOfRange(args, 7, args.length));		

		Set<String> wordset = TargetMatrixGenerator.getWordMap(outprefix,data,word2Name);
		
		word2vec = getFullPath(word2vec);
		
		boolean binary = DataGenerationParameterUtils.isBinary(word2vec);
		
		Word2Vec model = TargetMatrixGenerator.getModel(word2vec,binary,wordset);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(targetmatrix));

		boolean first = true;

		List<String> feats = FileUtils.readLines(new File(targetOutput));
		
		for (String feat : feats) {

			if (!first)
				bw.newLine();

			bw.write(InputFilesGenerator.prettyPrint(model.getWordVector(feat)));

			first = false;
			
		}

		bw.close();
			
		/*Check if Extraction is required*/
		
		if (requiresExtraction(reducedFile)){
			
			PrincipalComponents pca = new PrincipalComponents();
							
			int featSize = 		DataGenerationParameterUtils.getVectorSizeTarget(word2vec);

			BufferedReader br = new BufferedReader(new FileReader(new File(targetmatrix)));
			
			StringBuilder sb = new StringBuilder();

			sb.append("att-" + 1);

			for (int i = 2; i <= featSize; i++) {
				sb.append(",att-" + i);
			}

			sb.append('\n');

			String line = null;

			while ((line = br.readLine())!=null){

				sb.append(line.replace(' ', ','));
				sb.append('\n');

			}

			br.close();

			CSVLoader loader = new CSVLoader();

			loader.setSource(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));

			Instances instances = loader.getDataSet();
			
			pca.buildEvaluator(instances);

			bw = new BufferedWriter(new FileWriter(new File(targetmatrix)));

			for (int i = instances.numAttributes() - 1; i > (instances.numAttributes() - pca.m_outputNumAtts - 1); i--) {
				
				bw.write("" + pca.m_eigenvectors[0][pca.m_sortedEigens[i]]);
				
				for (int j = 1; j < instances.numAttributes(); j++) {

					bw.write(" " + pca.m_eigenvectors[j][pca.m_sortedEigens[i]]);
				}
				
				if (i > (instances.numAttributes() - pca.m_outputNumAtts)){
					bw.newLine();
				}
				
			}

			bw.close();

			
		}
		
	}

	private static boolean requiresExtraction(String reducedFile) {
		return reducedFile.contains("PrincipalComponents");
	}

	private static String getFullPath(String word2vec) {
		
		if (word2vec.contains("input"))
			return "/proj/db-files2/NoBackup/pjbarrio/word2vec/word2vec/" + word2vec;
		else
			return "/proj/db-files2/NoBackup/pjbarrio/Project/GloVe/glove/" + word2vec;
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

	private static String getWeightOutputName(String file) {

		return file + ".weight";

	}

	private static String getMatrixFile(String file) {

		return file + ".target";

	}
	
	private static String getOwnWeightOutputName(String file) {

		return file + "-own.weight";

	}

	public static String getTargetOutputName(String file) {

		return  file +  ".rawtarget";

	}

	public static String getTargetMatrixOutputName(String outprefix, String targetGenerator, String sampleMethod, String data, String relation, String extractor, String word2Name, String[] params) {

		new File(outprefix + "target/").mkdirs();

		return  getTargetName(outprefix, targetGenerator, sampleMethod, data, relation, extractor, word2Name, params) + ".target";

	}

	private static String getTargetName(String outprefix, String targetGenerator, String sampleMethod, String data, String relation, String extractor, String word2Name, String[] params){
		return outprefix + "target/" + targetGenerator + "." + word2Name + "." + CandidatesGenerator.getSampleName(true,sampleMethod, data,
				relation, extractor, params);
	}



}
