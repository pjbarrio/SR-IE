package data.generation.target;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import utils.wordmodel.DataGenerationParameterUtils;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import weka.core.converters.CSVLoader;

import data.generation.InputFilesGenerator;
import data.generation.target.utils.PrincipalComponents;

public class FeatureExtractionTargetGenerator {

	private static final String TARGET = "target";
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

//		String relation = args[0];
//		String extractor = args[1];
//		String outprefix = args[2]; //
//		Integer featSize = Integer.valueOf(args[3]); //e.g., 300
//		String featureselector = args[4]; //e.g., CfsSubsetEval or FULL
//		String acceptable = args[5]; //e.g., 0.95 or FULL
//		String data = args[6];
		
		
		String target = args[0];
		
		PrincipalComponents pca = new PrincipalComponents();
	
		String fName = getExtractedName(pca,target);
		
		System.out.println(fName);
		
		if (new File(fName).exists()){
			System.out.println("Exists...");
			return;
		}	
			
		int featSize = DataGenerationParameterUtils.getVectorSizeTarget(target);
		
//		BufferedReader br = new BufferedReader(new FileReader(new File(outprefix + data + "." + relation + "." + extractor + "." + featureselector + "." + acceptable + "." + FeatureSelectionTargetGenerator.TARGET)));

		BufferedReader br = new BufferedReader(new FileReader(new File(target)));
		
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

		
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fName)));

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

	private static String getExtractedName(PrincipalComponents pca,
			String target) {
		
		return target.replace("." + TARGET, "." + pca.getClass().getSimpleName() + "." + TARGET);
		
	}

}
