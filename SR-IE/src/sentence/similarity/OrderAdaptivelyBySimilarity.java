package sentence.similarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import utils.databaseWriter;
import utils.wordmodel.DataGenerationParameterUtils;
import distance.measure.DistanceMeasure;
import feature.extractor.FeatureExtractor;

public class OrderAdaptivelyBySimilarity {

	private class ScoredNode implements Comparable<ScoredNode>{

		private int node;
		private double value;

		public ScoredNode(int node, double value){
			this.node = node;
			this.value = value;
		}

		@Override
		public int compareTo(ScoredNode o) {
			if (o.node == this.node)
				return 0;
			return Double.compare(o.value,this.value);
		}

		@Override
		public int hashCode() {
			return node;
		}
		
		@Override
		public boolean equals(Object obj) {
			return ((ScoredNode)obj).node == node;
		}
		
	}

	private class HalfMatrix{

		private double[] data;
		private int number;

		public HalfMatrix(int index) {
			number = index;
			data = new double[index*(index-1)/2]; //not saving comparison to same, which is 1.0
		}

		public void setValue(int i, int j, double value) {

			if (i > j){
				int aux = i;
				i = j;
				j = aux;
			}

			data[getIndex(i,j)] = value;

		}

		private int getIndex(int i, int j) {
			return (i*number) - (i * (i+1)/2) + (j-i-1);
		}

		public int length() {
			return number;
		}

		public double getValue(int i, int j) {

			if (i > j){
				int aux = i;
				i = j;
				j = aux;
			}

			return data[getIndex(i, j)];
		}

	}

	private class UsefulOnlyMatrix{

		private double[][] data;
		private int number;
		private int usefuls;

		public UsefulOnlyMatrix(int index, int usefuls) {

			System.err.println("Usefuls are always first");

			number = index;
			this.usefuls = usefuls;
			data = new double[usefuls][index]; //not saving comparison to same, which is 1.0
		}

		public void setValue(int usefulIndex, int other, double value) {

			data[usefulIndex][other] = value;

		}

		public int length() {
			return number;
		}

		public double getValue(int usefulIndex, int other) {

			return data[usefulIndex][other];

		}

	}

	public static void main(String[] args) throws IOException {

		OrderAdaptivelyBySimilarity insta = new OrderAdaptivelyBySimilarity();

		String task = args[0]; //e.g., "Train";
		String relation = DataGenerationParameterUtils.relations[Integer.valueOf(args[1])];// e.g., 0 for PersonParty;
		String extractor = args[2]; //e.g., "default";
		String outprefix = args[3]; //e.g., "data/omp/test1/";
		int updateEvery = Integer.valueOf(args[4]);
		
		boolean limit = false;
		int my_size = 1000000;

		
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outprefix + "sentence-comparison_" + task + "_" + relation +"_" + extractor + ".csv")));

		int startpoints = 5;

		ArrayList<FeatureExtractor<String>> fes = CompareSentences.loadFeatureExtractors(outprefix,task);

		bw.write("FeatureExtractor,Relation,Seed,ProcessedDocuments,Recall");
		bw.newLine();


		Map<String,List<String>> map = CompareSentences.readMap(outprefix,relation,extractor,task);

		int maxSize = limit? Math.min(my_size, map.get("yes").size()) : map.get("yes").size() + map.get("no").size(); 

		int[] randoms = createRandoms(map.get("yes").size(),startpoints);			

		for (FeatureExtractor<String> fe : fes) {

			DistanceMeasure m = fe.getDistanceMeasure();

			fe.initialize(CompareSentences.getWordSetName(relation,task));

			Map<String, List<Map<Integer, Double>>> mVec = fe.createFeatureVectors(map, maxSize);

			//Obtain Starting Points

			List<Map<Integer,Double>> spoints = mVec.get("yes");

			//Populate matrix

			List<Map<Integer,Double>> useless = mVec.get("no");

			Map<Integer,Map<Integer,Double>> feats = new HashMap<Integer,Map<Integer,Double>>(spoints.size() + useless.size());

			int index = 0;

			for (int i = 0; i < spoints.size(); i++) {
				feats.put(index, spoints.get(i));
				index++;
			}

			int allUsefuls = index;

			for (int i = 0; i < useless.size(); i++) {
				feats.put(index, useless.get(i));
				index++;
			}

			int nodes = spoints.size() + useless.size();

			databaseWriter dW = new databaseWriter();
			
			int idMeasure = dW.getIdMeasure(m.getClass().getSimpleName(), task);
			
			boolean isSparse = fe.isSparse();
			
			if (isSparse && !dW.hasLoadedOverlap(idMeasure)){
				
				Map<Integer, Set<Integer>> toCompute = new HashMap<Integer,Set<Integer>>();
				
				for (int i = 0; i < allUsefuls; i++) {
					toCompute.put(i, new HashSet<Integer>());
				}

				for (int i = 0; i < allUsefuls; i++) {

					Set<Integer> myset = toCompute.remove(i);

					if (i % 100 == 0){
						System.out.println("Computing overlap of " + i + " of " + allUsefuls);
					}

					for (int j = i+1; j < nodes; j++) {
						if (overlap(feats.get(i),feats.get(j))){
							myset.add(j);
							if (j <allUsefuls){ //isUseful
								toCompute.get(j).add(i);
							}
						}
					}

					dW.saveOverlaps(idMeasure,i,myset);

					myset.clear();
					
				}

			}

			System.err.println("Relation Created...");

			for (int i = 0; i < randoms.length; i++) {
				int[] visitedIndexes = new int[allUsefuls];
				int[] all_visited = new int[nodes];

				boolean[] visited_bool = new boolean[nodes];

				int[] active_nodes = new int[nodes];
				int[] where_is_node = new int[nodes];
				
				PriorityQueue<ScoredNode> pr = new PriorityQueue<>(nodes);
				
				visitedIndexes[0] = randoms[i];
				all_visited[0] = randoms[i];
				visited_bool[randoms[i]] = true;
				
				for (int j = 0; j < active_nodes.length; j++) {
					active_nodes[j] = j;
					where_is_node[j] = j;
					if (j != randoms[i]){
						pr.add(insta.new ScoredNode(j, -1.0));
						visited_bool[j] = false;
					}
				}

				
				
				int all_vi_index = 1;
				int visited = 1;

				int li = nodes-all_vi_index;
				int last_active_node = active_nodes[li];
				int lv = all_visited[all_vi_index-1];
				int wi = where_is_node[lv];

				int chosen = active_nodes[wi];
				active_nodes[li] = chosen;
				active_nodes[wi] = last_active_node;

				where_is_node[last_active_node] = wi;
				where_is_node[lv] = li;

				int last_visited = visitedIndexes[0];
				
				double[] cachedSums = new double[nodes];
				
				
				String prefix = fe.getSimpleName() + "," + relation+","+randoms[i]+",";

				bw.write(prefix + 0 + "," + visited/allUsefuls);
				bw.newLine();

				long time_millis = System.currentTimeMillis();

				int visited_until_next_update = 1;
				
				int last_updated_useful_index = 0;
				
				while(all_vi_index < nodes){

					visited_until_next_update--;
					
					if (all_vi_index % 100 == 0){
						long ts = System.currentTimeMillis();
						System.out.println("Visited: " + all_vi_index + " of " + nodes + " ("+ visited + " of " +allUsefuls+") in " + (ts - time_millis)/1000.0 + " seconds.");
						time_millis = ts;
					}

					last_visited = visitNext(last_updated_useful_index, visited_until_next_update,visited_bool,idMeasure,isSparse,dW,active_nodes,cachedSums,insta,m,allUsefuls,pr,nodes,visitedIndexes,all_vi_index,last_visited,feats,visited);

					if (visited_until_next_update == 0){
						last_updated_useful_index = visited;
					}
					
					visited_bool[last_visited] = true;
					
					if (last_visited < allUsefuls){
						
						visitedIndexes[visited] = last_visited;
						visited++;
					
					}

					all_visited[all_vi_index] = last_visited;
					all_vi_index++;

					li = nodes-all_vi_index;
					last_active_node = active_nodes[li];
					lv = all_visited[all_vi_index-1];
					wi = where_is_node[lv];

					chosen = active_nodes[wi];
					active_nodes[li] = chosen;
					active_nodes[wi] = last_active_node;

					where_is_node[last_active_node] = wi;
					where_is_node[lv] = li;

					bw.write(prefix + all_vi_index + "," + (double)visited/(double)allUsefuls);
					bw.newLine();

					if (visited_until_next_update == 0)
						visited_until_next_update = updateEvery;
				}

				System.err.println(calculateRPrecision(prefix, all_visited,spoints.size(),allUsefuls));

			}


		}

		bw.close();

	}

	private static boolean overlap(Map<Integer, Double> map,
			Map<Integer, Double> map2) {

		if (map.size() < map2.size()){
			for (Integer val : map.keySet()) {
				if (map2.containsKey(val))
					return true;
			}
		}

		return false;
	}

	static int[] createRandoms(int maxIndex, int total) {

		int[] ret = new int[total];

		for (int i = 0; i < total; i++) {

			ret[i] = (int) Math.floor(Math.random()*maxIndex);

		}

		return ret;
	}

	private static int visitNext(int last_updated_useful_index, int visited_until_next_update, boolean[] visited_bool, int idMeasure, boolean isSparse, databaseWriter dW, int[] active_nodes,double[] cached_sum, OrderAdaptivelyBySimilarity insta, DistanceMeasure m, 
			int allUsefuls, PriorityQueue<ScoredNode> pr, int nodes,int[] visitedIndexes, int visited, int last_visited, Map<Integer, Map<Integer, Double>> feats, int usefulVisited) {

		if (visited_until_next_update == 0 && last_updated_useful_index < usefulVisited){

			if (isSparse){
			
				Set<Integer> toCompute_local = dW.getOverlap(idMeasure,last_visited);
				
				for (Integer node : toCompute_local) {
					
					if (!visited_bool[node]){
						
						pr.remove(insta.new ScoredNode(node, -1.0));
						
						Double val = m.distance(feats.get(last_visited),feats.get(node));

						if (val == null)
							val = 0.0;

						cached_sum[node] += val;

						double measure = cached_sum[node] / usefulVisited;

						pr.add(insta.new ScoredNode(node, measure));
						
					}
					
				}
				
			}else{
				
					pr.clear();
					
					for (int ind = 0; ind < nodes - visited; ind++) {

						int i = active_nodes[ind];

						for (int j =  last_updated_useful_index; j < usefulVisited; j++) {

							int uful_node = visitedIndexes[j];
							
							Double val = m.distance(feats.get(uful_node),feats.get(i));

							if (val == null)
								val = 0.0;

							cached_sum[i] += val;
							
						}
						
						double measure = cached_sum[i] / usefulVisited;

						pr.add(insta.new ScoredNode(i, measure));

					}
				
			}
			

		}

		ScoredNode sn = pr.remove();
		return sn.node;

	}


	private static double calculateRPrecision(String prefix, int[] visitedIndexes, int recallUseful,
			int usef) throws IOException {

		double Rprecision = 0.0;

		double total = 0.0;
		for (int i = 0; i < visitedIndexes.length; i++) {
			if (visitedIndexes[i] < usef)
				total++;
			if (i == recallUseful){
				Rprecision = total / recallUseful;
				break;
			}
			
		}

		return Rprecision;
	}

}
