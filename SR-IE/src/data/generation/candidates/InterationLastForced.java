package data.generation.candidates;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import data.generation.samples.InterationSampleGenerator;

public class InterationLastForced {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String task = args[0];
		String minfreq = args[1];
		String maxfreq = args[2];
		String sents = args[3];
		
		Set<String> sample = new HashSet<String>(FileUtils.readLines(new File(args[4] + InterationSampleGenerator.SAMPLE)));
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(args[5]));
		
		int total = 2717839; //for Train
		
		if (task.equals("LTrain")){
			
			total = 2826630;
			
		}
		
		bw.write("lo_freq, hi_freq, sent_iter, position, candidate, length, is.useful");
		
		String pref = minfreq + "," + maxfreq + "," + sents + ",";
		String suff = ",-1, 0";
		
		int position = 0;
		
		for (int i = 0; i <= total; i++) {
						
			if (!sample.contains(Integer.toString(i))){
				
				position++;
				
				bw.newLine();
				bw.write(pref + position + "," + i + suff);
				
			}
			
			
		}
		
		
		bw.close();
		

	}

}
