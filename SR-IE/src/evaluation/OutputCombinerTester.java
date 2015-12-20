package evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jblas.exceptions.UnsupportedArchitectureException;

import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;
import weka.core.UnsupportedAttributeTypeException;

public class OutputCombinerTester {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		//add relation, extractor, model...
		
		int splits = 1000;
		
		String output_folder = "data/evaluation/";
		
		Map<String,Set<String>> mapExpts = (Map<String, Set<String>>) SerializationHelper.deserialize("exp_summarize.summary");
		
		for (Entry<String, Set<String>> exp_files : mapExpts.entrySet()) {
			
			if (!exp_files.getKey().equals("impact_target"))
				continue;
			
			for (String prefix : exp_files.getValue()) {
				
				if (new File(prefix).isDirectory())
					continue;
				
				if (!prefix.equals("data/ltrain/outp/impact_target/SMOClassifierFeatSelection.input=data.txt-cbow=0-dim=50.bin.LTrain.DocumentSampler.Indictment-Arrest-Trial.Pablo-N-Grams.2000.Smart.5.10.50.PrincipalComponents-0-0.0001-0.0001-0.9-25-500-ALL"))
					continue;
				
				String data = getData(prefix);//Train
				String extractor = getExtractor(prefix); //Pablo- ... OpenCalais
				String relation = getRelation(prefix,extractor); //NaturalDisaster
				String model = getModel(prefix);
				String modelsize = getModelSize(prefix);
				String modelphrase = getModelPhrase(prefix);
				String target = getTarget(prefix);
				String targetSummary = getTargetSummary(prefix);
				String targetSize = getTargetSize(prefix);
				String sample = getSample(prefix);
				String split = getSplit(prefix);
				
				String prf = data + "," + relation + "," + extractor + "," + model + "," + modelsize + "," + modelphrase + "," + target +
						"," + targetSummary + "," + targetSize + "," + sample + "," + split + ",";
				
				int iter = 150;
				
				File f = new File(prefix + "." + iter);
									
				int position = 1;
				
				int totaluseful = 0;
				
				int currentSplit = splits;
				
				String lastLine = null;
				
				System.out.println("\n" + f.getAbsolutePath());
				
				int lines = 0;
				
				while (f.exists()){
					System.out.println("lines: " + lines);
					System.out.println(f.getName());
					BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
					System.out.println(attr.creationTime());
//					System.out.println("Exists - " + iter);
					
					if (iter > 0){
						String samp = f.getAbsolutePath().replace("/outp/","/inp/") + ".sample"; 
						
						if (!new File(samp).exists()){
							System.out.println("\nSample does not exists for " + samp);
							break;
						}
					}
					
					BufferedReader br = new BufferedReader(new FileReader(f));
					
					br.readLine();
					
					String line = br.readLine();
					
					if (line.contains("2826631"))
						System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
					
					br.close();

					iter++;
					
					f = new File(prefix + "." + iter);
					
				}
				
			}
			
		}
		
		
	}

	public static String getRelation(String prefix, String extractor) {
		
		String[] rel = DataGenerationParameterUtils.reelrelations;
		String[] report = DataGenerationParameterUtils.reelprintablerelatons;
		
		if ("OC".equals(extractor)){
			rel = DataGenerationParameterUtils.relations;
			report = rel;
		}
		
		for (int i = 0; i < rel.length; i++) {
			
			if (prefix.contains(rel[i])){
				return report[i];
			}
			
		}
		
		throw new UnsupportedArchitectureException("Can't find the relation!");
		
	}

	private static String getExtractor(String prefix) {
		
		if (prefix.contains("default")){
			return "OC";
		}else if (prefix.contains("Pablo-N-Grams")){
			return "BONG";
		}else if (prefix.contains("Pablo-Sub-sequences")){
			return "SSK";
		}
		
		throw new UnsupportedArchitectureException("Not found Extraction System in" +  prefix);
		
	}

	private static String getSentences(String prefix) {
		
		String[] sps = prefix.split("-");
		
		return sps[sps.length-3];
		
	}

	private static String getSplit(String prefix) {
		
		for (int i = 1; i <= 5; i++) {
			
			if (prefix.contains("Smart." + i) || prefix.contains("Explicit." + i))
				return Integer.toString(i);
			
		}
		
		throw new UnsupportedArchitectureException("Can't recognize split");
				
	}

	private static String getTargetSize(String prefix) {
		
		return prefix.substring(prefix.lastIndexOf('-')+1);
		
	}

	private static String getSample(String prefix) {
		if (prefix.contains("Smart")){
			return "Cyclic";
		}else if (prefix.contains("Explicit")){
			return "Random";
		}
		
		throw new UnsupportedArchitectureException("Can't recognize the sample!");
	}

	private static String getTargetSummary(String prefix) {
		
		if (prefix.contains("PrincipalComponents")){
			return "Sum-PCA";
		}else{
			return "Raw";
		}
		
	}

	private static String getTarget(String prefix) {
		
		if (prefix.contains("SMOClassifierFeatSelection")){
			return "Selected-SMO";
		} else if (prefix.contains("UsefulOnlyFeatureSelection")){
			return "All";
		}
		
		throw new UnsupportedArchitectureException("Where is target?");
	}

	private static String getModelPhrase(String prefix) {
		
		if (prefix.contains("trigram")){
			return "4-Gram";
		}else if (prefix.contains("bigram")){
			return "3-Gram";
		}else if (prefix.contains("unigram")){
			return "2-Gram";
		}
		
		return "1-Gram";
		
	}

	private static String getModelSize(String prefix) {
		
		if (prefix.contains("1000.bin")|| prefix.contains("txt-1000-10-vectors")){
			return "1000";
		}else if (prefix.contains("500.bin")|| prefix.contains("txt-500-10-vectors")){
			return "500";
		}else if (prefix.contains("300.bin")|| prefix.contains("txt-300-10-vectors")){
			return "300";
		}else if (prefix.contains("100.bin")|| prefix.contains("txt-100-10-vectors")){
			return "100";
		}else if (prefix.contains("50.bin") || prefix.contains("txt-50-10-vectors")){
			return "50";
		}
		
		throw new UnsupportedArchitectureException("Where is the model size? in " + prefix);
		
	}

	private static String getModel(String prefix) {
		if (prefix.contains("vectors.txt")){
			return "GloVe";
		}
		
		return "Word2Vec";
	}

	private static String getData(String prefix) {
		if (prefix.contains("LTrain")){
			return "LTrain";
		}
		
		return "Train";
	}

}
