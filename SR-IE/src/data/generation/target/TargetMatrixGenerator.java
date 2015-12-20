package data.generation.target;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.word2vec.Word2Vec;

import utils.wordmodel.DataGenerationParameterUtils;
import data.generation.CreateWordMap;
import data.generation.InputFilesGenerator;

public class TargetMatrixGenerator {

	private static Set<String> wordset;
	private static Word2Vec word2Vec;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		
		InputFilesGenerator.resetLineSeparator();

		String data = args[0];
		String outprefix = args[1]; 
		String word2vec = args[2];
		String word2Name = new File(word2vec).getName();
		boolean binary = DataGenerationParameterUtils.isBinary(word2vec);
		String word2NameNorm = InputFilesGenerator.normalizeWord2Vec(word2Name);
		String rawTargets = args[3]; //Generated with find target/ | grep rawtarget > ../../ltrain_target.txt or train_target.txt
		
//		String relation = DataGenerationParameterUtils.relations[Integer.valueOf(args[3])];
//		String extractor = args[4];
//		String targetGenerationMethod = args[5];
//		String sampleMethod = args[6];

		//For FromSplit, size (args[6]) , fractionuseful (args[7]), and seed (args[8])

		//For DocumentSampler, size (args[6]), sampling_algorithm (args[7]) , split (args[8]), docsPerQuerySample (args[9]), and numQueries (args[10]) 

		List<String> rawtargets = FileUtils.readLines(new File(rawTargets));
		
		for (int i = 0; i < rawtargets.size(); i++) {
				
			String rawTargetRel = rawtargets.get(i);
			
			if (!validforWord2Vec(word2NameNorm,rawTargetRel)){
				continue;
			}
			
			System.out.println("Target: " + i + " of " + rawtargets.size());
			
			String rawTarget = outprefix + rawTargetRel;
			
			String targetOutput = rawTarget.replace(".rawtarget", ".target").replace("target/", "ftarget/").replace(word2NameNorm, word2Name);
			
//			String targetOutput = TargetGenerator.getTargetMatrixOutputName(outprefix, targetGenerationMethod, sampleMethod, data, relation, extractor, word2Name, Arrays.copyOfRange(args, 7, args.length));
			
			if (new File(targetOutput).exists()){
				System.out.println("Matrix exists: " + targetOutput);
				return;
			}
			
//			String rawTarget = TargetGenerator.getTargetOutputName(outprefix, targetGenerationMethod, sampleMethod, data, relation, extractor, word2NameNorm, Arrays.copyOfRange(args, 7, args.length));		

			Set<String> wordset = getWordMap(outprefix,data,word2NameNorm);
			
			Word2Vec model = getModel(word2vec,binary,wordset);
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(targetOutput));

			boolean first = true;

			List<String> feats = FileUtils.readLines(new File(rawTarget));
			
			for (String feat : feats) {

				if (!first)
					bw.newLine();

				bw.write(InputFilesGenerator.prettyPrint(model.getWordVector(feat)));

				first = false;
				
			}

			bw.close();
			
			
//			StringBuilder sb = new StringBuilder();
//			
//			for (String feat : feats) {
//
//				if (!first){
//					sb.append(eol); where String eol = System.getProperty("line.separator"); goes outside the loop.
//				}
//				sb.append(InputFilesGenerator.prettyPrint(model.getWordVector(feat)));
//				
//				first = false;
//				
//			}
//
//			BufferedWriter bw = new BufferedWriter(new FileWriter(targetOutput));
//
//			bw.write(sb.toString());
//			
//			bw.close();
			
			
		}
		



	}

	private static boolean validforWord2Vec(String word2NameNorm,
			String rawTargetRel) {
		return InputFilesGenerator.normalizeWord2Vec(rawTargetRel).equals(word2NameNorm);
	}

	static Set<String> getWordMap(String outprefix, String data, String word2Name) {
		
		if (wordset == null){
			wordset = CreateWordMap.loadWordSet(outprefix,data, word2Name);
		}
		return wordset;
		
	}

	static Word2Vec getModel(String word2vec,boolean binary, Set<String> feats) throws IOException {
		
		if (word2Vec == null){
			word2Vec = DataGenerationParameterUtils.getLoader(word2vec).loadModel(word2vec, binary,feats,false);
		}
		
		return word2Vec;
	}

}
