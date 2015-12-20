package data.generation.candidates;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import data.generation.samples.InterationSampleGenerator;

public class CleanExecutions {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String task = "train";
		
		String prefix = "data/" + task + "/inp/";
		String file = "data/" + task + "_terminate";
		
		List<String> f = FileUtils.readLines(new File(file));

		Set<String> cf = new HashSet<String>(f.size());
		
		Set<String> prefs = new HashSet<String>(); 
		
		for (String string : f) {
			
			String val = prefix + string.substring(2).replaceAll(".terminate", "");
			
			cf.add(val);
			
			String valc = val.substring(0, val.lastIndexOf('.'));
			
			if (!valc.endsWith("-0"))
				prefs.add(valc);
			
		}
		
		System.out.println(cf.toString());
		
		System.out.println(prefs.toString());
		
		for (String pref : prefs) {
			
			int iter = 1;
			
			while (!new File(pref + "." + iter + ".terminate").exists()){
				
				iter++;
				
			}
			
			System.out.println("First is " + new File(pref + "." + iter + ".terminate").getAbsolutePath());
			
			if (!new File(pref + "." + (iter+1) + ".terminate").exists()){ //Everything is fine, because there's only one terminate.
				continue;
			}
			
			String toRemove = pref.replace("/inp/", "/outp/") + "." + (iter+1); //This would finish the creation of files in OutputCombiner
			String sample = pref + "." + iter;
			String outp = pref.replaceAll("/inp/", "/outp/") + "." + iter;
			
			System.out.println(toRemove + " - " + new File(toRemove).exists());
			System.out.println(sample  + " - " + new File(sample + ".sample").exists());
			System.out.println(outp + " - " + new File(outp).exists());
			
			String[] input = new String[6];
			
			input[0] = task.equals("train")? "Train":"LTrain";
			input[1] = "0.0001";
			input[2] = "0.9";
			String[] spls = pref.split("-");
			input[3] = spls[spls.length-3];
			input[4] = sample;
			input[5] = outp;
	
			System.out.println(Arrays.toString(input));
						
			Files.delete(new File(toRemove).toPath());

			InterationLastForced.main(input);
			
		} 
		
	}

}
