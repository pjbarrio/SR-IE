package sentence.similarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Tuple;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.word2vec.Word2Vec;

import com.google.gdata.util.common.base.Pair;

import data.generation.SentenceUsefulnessSplit;
import distance.measure.DistanceMeasure;
import distance.measure.impl.CosineSimilarity;
import edu.stanford.nlp.ling.Word;
import feature.extractor.FeatureExtractor;
import feature.extractor.impl.POSFeatureExtractor;
import feature.extractor.impl.TokensFeatureExtractor;
import feature.extractor.impl.Word2VecFeatureExtractor;

import utils.JSONLoader;
import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;
import utils.wordmodel.GloVe;
import utils.wordmodel.MyGloVeLoader;
import utils.wordmodel.MyWord2VecLoader;
import utils.wordmodel.WordModelLoader;

public class CompareSentences {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		boolean limit = true;
		int my_size = 1000;

		String[] relations = {"PersonParty"/*,"20131104-education-degree","20131104-date_of_birth","20131104-place_of_death",
				"20130403-place_of_birth","20130403-institution"*/};

		String[] tasks = {"Train"};

		ArrayList<FeatureExtractor<String>> fes = loadFeatureExtractors();		

		DistanceMeasure m = new CosineSimilarity();
		
		for (String task : tasks) {

			for (String relation : relations) {

				Map<String,List<String>> map = readMap(outprefix,relation,extractor,task);

				String[] keys = {"no","yes"};

				int maxSize = limit? Math.min(my_size, map.get("yes").size()) : Integer.MAX_VALUE; 
				
				for (int r = 0; r < fes.size(); r++) {

					FeatureExtractor<String> fe = fes.get(r);
					
					fe.initialize(getWordSetName(relation,task));
					
					System.out.println(relation);

					Map<String, List<Map<Integer, Double>>> mVec = fe.createFeatureVectors(map, maxSize);

					BufferedWriter bw = new BufferedWriter(new FileWriter(new File(getName(relation,task,fe))));

					for (int i = 0; i < keys.length; i++) {

						List<Map<Integer, Double>> snippets = mVec.get(keys[i]);

						for (int l = i; l < keys.length; l++) {

							List<Map<Integer, Double>> snippets2 = mVec.get(keys[l]);

							for (int j = 0; j < snippets.size(); j++) {

								if (j % 500 == 0)
									System.out.println(keys[i]+"-"+keys[l] + "-" + j);

								int k = 0;
								if (i == l)
									k = j+1;

								for (; k < snippets2.size(); k++) {

									bw.write(keys[i]+ "," + j +","+keys[l] + "," + k + "," + m.distance(snippets.get(j), snippets2.get(k)));
									bw.newLine();

								}

							}

						}



					}

					bw.close();

				}

			}

		}

	}

	public static ArrayList<FeatureExtractor<String>> loadFeatureExtractors(String outprefix, String task) {
		
		ArrayList<FeatureExtractor<String>> fes = new ArrayList<FeatureExtractor<String>>();
		
		fes.add(new TokensFeatureExtractor());
//		fes.add(new POSFeatureExtractor());
		
		WordModelLoader[] loaders = new WordModelLoader[9];
		WordModelLoader<Word2Vec> loaderw2v = new MyWord2VecLoader();
		WordModelLoader<GloVe> loadergv = new MyGloVeLoader();
//		loaders[0]=loaderw2v;
//		loaders[1]=loaders[2]=loaders[3]=loaders[4]=loaders[5]=loaders[6]=loaders[7]=loaders[7]=loaders[8]=loadergv;
//		int[] sizes = {300,200,300,300,300,200,25,100,50};		
//		String[] names = {"GoogleNews-vectors-negative"+sizes[0]+".bin",
//				"glove.twitter.27B."+sizes[1]+"d.txt.gz",
//				"glove.42B."+sizes[2]+"d.txt.gz",
//				"glove.840B."+sizes[3]+"d.txt.gz",
//				"glove.6B."+sizes[4]+"d.txt.gz",
//				"glove.6B."+sizes[5]+"d.txt.gz",
//				"glove.twitter.27B."+sizes[6]+"d.txt.gz",
//				"glove.6B."+sizes[7]+"d.txt.gz",
//				"glove.6B."+sizes[8]+"d.txt.gz"};

		String[] names = {"/proj/db-files2/NoBackup/pjbarrio/word2vec/word2vec/input=data-phrase-unigram.txt-cbow=0-dim=100.bin"};
		loaders[0] = loaderw2v;
		for (int r = 0; r < names.length; r++) {
			
			int size = DataGenerationParameterUtils.getVectorSize(names[r]);
			String name = names[r];
			WordModelLoader loader = loaders[r];
			String word2Name = new File(names[r]).getName();
			
			fes.add(new Word2VecFeatureExtractor(size, loader, name, word2Name, outprefix, task, word2Name));
			
		}

		return fes;
		
	}

	public static String getWordSetName(String relation, String task) {

		if (relation.startsWith("2013")){
			return relation;
		}else{
			return task;
		}

	}

	private static String getName(String relation, String task, FeatureExtractor<String> fe) {

		if (relation.startsWith("2013")){
			return "data\\"+ fe.getSimpleName() + "."+relation+".csv";
		} else{
			return "data\\"+ fe.getSimpleName() + "." + task + "-" +relation+".csv";
		}



	}

	public static Map<String, List<String>> readMap(String outprefix, String relation, String extractor,
			String task) throws IOException {

		if (relation.startsWith("2013")){

			String file = "D:\\OneDrive\\SentenceRanking\\"+relation+".json";

			return JSONLoader.loadMap(file);

		}

		Map<String,List<String>> uselessSentences = (Map<String,List<String>>)SerializationHelper.deserialize(outprefix + task + "." + relation + "." + extractor + SentenceUsefulnessSplit.SPLIT_USELESS + ".ser");

		List<String> ul = new ArrayList<String>();

		for (List<String> useless : uselessSentences.values()) {

			ul.addAll(useless);

		}

		Map<String,List<Pair<String,Tuple>>> usefulSentences = (Map<String,List<Pair<String,Tuple>>>)SerializationHelper.deserialize(outprefix + task + "." + relation + "." + extractor + SentenceUsefulnessSplit.SPLIT_USEFUL + ".ser");

		List<String> uf = new ArrayList<String>();

		for (List<Pair<String,Tuple>> pairs : usefulSentences.values()) {

			for (Pair<String, Tuple> pair : pairs) {

				uf.add(pair.getFirst());

			}

		}

		Map<String,List<String>> ret = new HashMap<String,List<String>>();

		ret.put("no", ul);

		ret.put("yes", uf);

		return ret;

	}

}
