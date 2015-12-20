package sentence.similarity;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.JSONLoader;
import distance.measure.DistanceMeasure;
import distance.measure.impl.CosineSimilarity;
import feature.extractor.FeatureExtractor;
import feature.extractor.impl.POSFeatureExtractor;
import feature.extractor.impl.TokensFeatureExtractor;

public class OrderBySimilarity {

	public static void main(String[] args) throws IOException {
		
		System.setOut(new PrintStream(new File("data\\static.csv")));
		
		int startpoints = 25;
		
		String[] relations = {"20131104-education-degree","20131104-date_of_birth","20131104-place_of_death",
				"20130403-place_of_birth","20130403-institution"};
		
		for (String relation : relations) {
			
			String file = "D:\\OneDrive\\SentenceRanking\\"+relation+".json";
			
			Map<String,List<String>> map = JSONLoader.loadMap(file);
			
			DistanceMeasure m = new CosineSimilarity();
			
			FeatureExtractor<String> fe = new TokensFeatureExtractor();
			
			Map<String, List<Map<Integer, Double>>> mVec = fe.createFeatureVectors(map);
		
			//Obtain Starting Points
			
			List<Map<Integer,Double>> spoints = mVec.get("yes");

			//Populate matrix
			
			List<Map<Integer,Double>> useless = mVec.get("no");
			
			Map<Integer,Map<Integer,Double>> feats = new HashMap<Integer,Map<Integer,Double>>(spoints.size() + useless.size());
			
			boolean[] usef = new boolean[spoints.size() + useless.size()];
			
			int index = 0;
			
			for (int i = 0; i < spoints.size(); i++) {
				feats.put(index, spoints.get(i));
				usef[index] = true;
				index++;
			}
			
			for (int i = 0; i < useless.size(); i++) {
				feats.put(index, useless.get(i));
				usef[index] = false;
				index++;
			}
	
			double[][] dmatrix = new double[index][index];
			
			for (int i = 0; i < index-1; i++) {
				
				/*for (int j = 0; j <= index; j++) {
					dmatrix[i][j] = -1.0;
				}
				
				for (int j = index+1; j < index; j++) {
					
					dmatrix[i][j] = m.distance(feats.get(i), feats.get(j));
					
				}*/
				
				for (int j = 0; j < index; j++) {
					dmatrix[i][j] = m.distance(feats.get(i), feats.get(j));
				}
				
			}
			
			System.err.println("Relation Created...");
			
			int[] randoms = OrderAdaptivelyBySimilarity.createRandoms(spoints.size(),startpoints);
			
			for (int i = 0; i < randoms.length; i++) {
				
				System.out.println(relation+","+randoms[i]+","+calculateRPrecision(dmatrix[randoms[i]],spoints.size(),usef));
				
			}
			
		}
		
	}

	private static double calculateRPrecision(double[] meas, int recallUseful,
			boolean[] usef) {
		
		int[] indexes = new int[meas.length];
		double[] measures = new double[meas.length];
		for (int i = 0; i < measures.length; i++) {
			indexes[i] = i;
			measures[i] = meas[i];
		}		
				
		for (int i = 0; i < measures.length-1; i++) {
			for (int j = i+1; j < measures.length; j++) {
				if (measures[i] < measures[j]){
					double aux = measures[j];
					measures[j] = measures[i];
					measures[i] = aux;
					int auxI = indexes[j];
					indexes[j] = indexes[i];
					indexes[i] = auxI;
				}
			}
		}
		double total = 0.0;
		for (int i = 0; i < recallUseful; i++) {
			if (usef[indexes[i]])
				total++;
		}
		
		return total/recallUseful;
	}
	
}
