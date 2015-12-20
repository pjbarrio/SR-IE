package data.statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;

public class NYTimesRelationStatistics {

	public static void main(String[] args) throws IOException {

		String task = args[0];
		
		generateStatistics(task);
		
		printStatistics();
		
	}

	private static void generateStatistics(String task) throws IOException {
		
		String folder = DataGenerationParameterUtils.getSourceFolder(task);
		
		File[] f = new File(folder).listFiles();
		
		Map<String,Integer> relMap = new HashMap<String,Integer>();
		
		int i = 0;
		
		for (File file : f) {
			
			if (i % 1000 == 0){
				System.err.format("%.2f processed: %s \n",(double)i / (double)f.length , file.getName());
				SerializationHelper.serialize("data/statistics/nytrainstats.ser", relMap);
			}
			
			i++;
			
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			String line = br.readLine();
			
			br.close();
			
			String[] spls = line.split("<!--Relations: ");
			
			if (spls.length > 1){
				String[] rels = spls[1].split(", ");
				for (String rel : rels) {
					
					Integer freq = relMap.get(rel);
					
					if (freq == null) {
						freq = 0;
					}
					
					relMap.put(rel, freq+1);
				
				}
			}
		}

		SerializationHelper.serialize("data/statistics/nytrainstats.ser", relMap);
		
	}

	private static void printStatistics() {
		
		Map<String,Integer> relfreqMap = (Map<String,Integer>)SerializationHelper.deserialize("data/statistics/nytrainstats.ser");
		
		System.out.println("relation,frequency");
		for (Entry<String,Integer> relfreq : relfreqMap.entrySet()) {
			System.out.println(relfreq.getKey() + "," + relfreq.getValue());
		}

		for (String rel : relfreqMap.keySet()) {
			System.out.print("\"" + rel + "\",");
		}
		
	}
	
}
