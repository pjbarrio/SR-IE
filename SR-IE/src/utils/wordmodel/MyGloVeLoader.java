package utils.wordmodel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class MyGloVeLoader extends WordModelLoader<GloVe>{

	private static String ENCO = "ISO-8859-1"; //"UTF-8"; it opens faster with "ISO-8859-1";
	
	@Override
	public GloVe loadModel(String path, boolean binary, double fraction)
			throws IOException {
		throw new UnsupportedOperationException("See if you have an available wordSet" +
				"and run the other function");
	}

	@Override
	public GloVe loadModel(String path, boolean binary, Set<String> wordSet,
			boolean normalized) throws IOException {
		String encoding = ENCO;
		InputStream fileStream = new FileInputStream(path);
//		InputStream gzipStream = new GZIPInputStream(fileStream);
//		Reader decoder = new InputStreamReader(gzipStream, encoding);
		Reader decoder = new InputStreamReader(fileStream, encoding);
		BufferedReader buffered = new BufferedReader(decoder);
		
		GloVe model = new GloVe();
		
		String line;
		
		int loaded = 0;
		
		while ((line = buffered.readLine())!=null){
			
			if (loaded++ % 1000000 == 0)
				System.out.println("Loaded: " + loaded);
			
			String[] spl = line.split(" ");
			
			if (wordSet.contains(spl[0]))
				model.setWordVector(spl[0], toDoubleVector(normalized,spl,1));
			
		}
		
		buffered.close();
		
		return model;
		
	}
	
	private double[] toDoubleVector(boolean normalized, String[] vector, int initialPos) {
		
		double[] ret = new double[vector.length-initialPos];
		
		double len = 0.0;
		
		for (int i = initialPos; i < vector.length; i++) {
			ret[i-initialPos] = Double.valueOf(vector[i]);
			if (normalized)
				len+=ret[i-initialPos]*ret[i-initialPos];
		}
		
		if (normalized){
			len = Math.sqrt(len);
			
			for (int j = 0; j < ret.length; j++)
				ret[j] /= len; //normalize
		}
		
		return ret;
		
	}

	public static void main(String[] args) throws IOException {
		
		String path = "/proj/db-files2/NoBackup/pjbarrio/Project/GloVe/glove/data-phrase-trigram.txt-100-10-vectors.txt";
		
		MyGloVeLoader mgl = new MyGloVeLoader();
		
		mgl.loadDictionary(path, false);
		
		GloVe m = mgl.loadModel(path, false, new HashSet<String>(), true);
		
		System.out.println(m.getWordVector("house"));
		
		
	}

	@Override
	public Set<String> loadDictionary(String path, boolean binary)
			throws IOException {
		
		Set<String> ret = new HashSet<String>();
		
		String encoding = ENCO;
		InputStream fileStream = new FileInputStream(path);
//		InputStream gzipStream = new GZIPInputStream(fileStream);
//		Reader decoder = new InputStreamReader(gzipStream, encoding);
		Reader decoder = new InputStreamReader(fileStream, encoding);
		BufferedReader buffered = new BufferedReader(decoder);
		
		String line;
		
		int loaded = 0;
		
		while ((line = buffered.readLine())!=null){
			
			if (loaded++ % 1000000 == 0){
				System.out.println("Loaded: " + loaded + " with " + ret.size() + " words");
			}
			String word = line.split(" ")[0];
			
			ret.add(word);
			
		}
		
		buffered.close();
		
		return ret;
		
	}
	
}
