package data.generation.samples;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.opencsv.CSVReader;

import data.generation.InputFilesGenerator;

import evaluation.TuplesNovelty;

public class InterationSampleGenerator {

	public static String SAMPLE = ".sample";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		String previousSample = args[0];
		String file = args[1];
		String output = args[2] + SAMPLE;
				
		List<String> previous = FileUtils.readLines(new File(previousSample + SAMPLE));
				
		CSVReader reader = new CSVReader(new FileReader(file));

		//get candidate position

		String [] line = reader.readNext();

		int i=0;

		while(!line[i].trim().equals(TuplesNovelty.CAND_SENTENCE_STRING)) i++;
		
		while ((line = reader.readNext()) != null) {

			String sentence = line[i].trim();

			previous.add(sentence);
			
		}

		reader.close();
		
		FileUtils.writeLines(new File(output), previous);
		
	}
	
}
