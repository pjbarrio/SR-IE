package evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jblas.exceptions.UnsupportedArchitectureException;

import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;
import weka.core.UnsupportedAttributeTypeException;

public class OutputCombiner {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		//add relation, extractor, model...
		
		int splits = 1000;
		
		String output_folder = "data/evaluation/";
		
		Map<String,Set<String>> mapExpts = (Map<String, Set<String>>) SerializationHelper.deserialize(args[0]==null?"exp_summarize.summary":args[0]+"_exp_summarize.summary");
		
		for (Entry<String, Set<String>> exp_files : mapExpts.entrySet()) {
			
			if (!exp_files.getKey().equals(args[0]))
				continue;
			
			
			String outp_file = output_folder + exp_files.getKey();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(outp_file + ".splits"));
			
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(outp_file + ".useful"));
			
			String hdr = "data,relation,extractor,model,modelsize,modelphrase,target,targetsummary,targetsize,sample,split";
						
			bw.write(hdr + ",position,lo_freq,hi_freq,sents,localpos,candidate,length,is.useful,useful");
			
			bw2.write(hdr + ",position,lo_freq,hi_freq,sents,localpos,candidate,length,is.useful");
			
			for (String prefix : exp_files.getValue()) {
				
				if (new File(prefix).isDirectory())
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
				String sents = getSentences(prefix);
				
				String prf = data + "," + relation + "," + extractor + "," + model + "," + modelsize + "," + modelphrase + "," + target +
						"," + targetSummary + "," + targetSize + "," + sample + "," + split + ",";
				
				int iter = 0;
				
				File f = new File(prefix + "." + iter);
									
				int position = 1;
				
				int totaluseful = 0;
				
				int currentSplit = splits;
				
				String lastLine = null;
				
				System.out.println("\n" + f.getAbsolutePath());
				
				while (f.exists()){
					
					//Need to check if it's mistakenly added as LAST
					
					BufferedReader brt = new BufferedReader(new FileReader(f));
					
					brt.readLine();
					
					String lineds = brt.readLine();
					
					if (lineds != null && (lineds.contains("2826631") || lineds.contains("2717840"))){
						
						if (new File(prefix + "." + (iter+1)).exists()){
							System.out.println("Mistakenly last file");
							iter++;
							f = new File(prefix + "." + iter);
							continue;
							
						}
												
					}
						
					
					brt.close();
										
					System.out.print(".");
					
//					System.out.println("Exists - " + iter);
					
					if (iter > 0){
						String samp = f.getAbsolutePath().replace("/outp/","/inp/") + ".sample"; 
						
						if (!new File(samp).exists()){
							System.out.println("\nSample does not exists for " + samp);
							break;
						}
					}
					
					BufferedReader br = new BufferedReader(new FileReader(f));
					
					String line = br.readLine(); // skip header line
					
					while ((line = br.readLine()) != null){
								
						if (line.startsWith("lo_freq")){
							continue;
						}
						
						if (line.endsWith("1")){ //is useful
							totaluseful++;
							bw2.newLine();
							bw2.write(prf + position + "," +  line.replace(" ", "").replaceAll("2826631",sents).replaceAll("2717840",sents));
						}
						
						if (currentSplit == position){
							bw.newLine();
							bw.write(prf + position + "," +  line.replace(" ", "") + "," + totaluseful);
							currentSplit+=splits;
							
						}
						
						lastLine = line;
						
						position++;
					}
					
					
					br.close();
					
					iter++;
					
					f = new File(prefix + "." + iter);
					
				}
				
				bw.newLine();
				bw.write(prf + currentSplit + "," + lastLine.replace(" ", "") + "," + totaluseful);

				
			}
			

			bw2.close();
			
			
			bw.close();
			
			
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
