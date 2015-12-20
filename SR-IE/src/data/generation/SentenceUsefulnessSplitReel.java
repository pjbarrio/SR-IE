package data.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Tuple;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import utils.SerializationHelper;
import utils.databaseWriter;
import utils.wordmodel.DataGenerationParameterUtils;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.extractor.wrapping.impl.CompressedAdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class SentenceUsefulnessSplitReel {

	public static final String TUPLES = ".tuples.txt";
	private static final String USEFULNESS = ".usefulness.txt";
	public static final String SPLIT_USELESS =  ".splituseless";
	public static final String SPLIT_USEFUL = ".splituseful";

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		InputFilesGenerator.resetLineSeparator();
		
		String task = args[0]; //e.g., "Train";
		String relation = DataGenerationParameterUtils.reelrelations[Integer.valueOf(args[1])];// e.g., 0 for PersonParty;
		String extractor = DataGenerationParameterUtils.extractors[Integer.valueOf(args[2])]; //e.g., "default";
		String outprefix = args[3]; //e.g., "data/omp/test1/";
		
		String directo = DataGenerationParameterUtils.getCollectionIndex(task);
		
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		Directory directory = new SimpleFSDirectory(new File(directo));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");

		int numdocs = conn.getNumDocuments();
		
		Map<Integer,String> mapIdName = new HashMap<Integer,String>();
		
		for (int i = 0; i < numdocs; i++) {
			
			mapIdName.put(i,conn.getPath(i));
			
		}
		
		create(outprefix, relation, extractor,task, mapIdName, numdocs);
			
	}

	private static void create(String outprefix, String relation, String extractor, String task, Map<Integer, String> mapIdName, int numdocs) throws IOException, ClassNotFoundException {
		
		String path = DataGenerationParameterUtils.getPath(task);
		String suffix = DataGenerationParameterUtils.getSuffix(task);
		
		databaseWriter dW = new utils.databaseWriter();
		int colId = DataGenerationParameterUtils.getCollectionId(task);
		
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%03d";//"%0" + String.valueOf(numPathsTotal).length() + "d";
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		
		String resultsPath = "/proj/dbNoBackup/pjbarrio/workspacedb-pc02/LearningToRankForIE/results" + relation;
		System.out.println("Initiating IE programs");
		CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
		
		for(String subPath : subPaths){
			extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relation + suffix + ".data");
		}		
		
		File f = new File(outprefix + DataGenerationParameterUtils.getAttributesFileName(relation,extractor,task));
		
		Set<String> attributes = new HashSet<String>();
		
		if (f.exists()){
			attributes.addAll((List<String>)SerializationHelper.deserialize(f.getAbsolutePath()));
		}
		
		Map<String,List<String>> uselessSentences = new HashMap<String, List<String>>();
		
		Map<String,List<Pair<String,Tuple>>> usefulSentences = new HashMap<String,List<Pair<String,Tuple>>>();
		
		Map<Long, List<Tuple>> tuples_total = new HashMap<Long,List<Tuple>>();
		
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(getUsefulnessFile(outprefix,task,relation,extractor)));
		
		boolean first = true;
		
		int currentSentence = 0;
			
		List<String> sentences = new ArrayList<String>();
		
		int split = 1000;
		
		for (int i = 0; i < numdocs; i++) {
		
			if (sentences.isEmpty()){
				sentences = dW.getSentenceText(colId, currentSentence, currentSentence+split);
				currentSentence+=split;
			}
			
			String stext = sentences.remove(0);
			
			String fname = mapIdName.get(i);
			
			String dname = fname.substring(0, fname.lastIndexOf('.'));
			
			List<Tuple> tups = convert(extractWrapper.getTuplesDocument(fname));
			
			if (tups.isEmpty()){
				List<String> aux = uselessSentences.get(dname);
				if (aux == null){
					aux = new ArrayList<String>();
					uselessSentences.put(dname, aux);
				}
				aux.add(stext);
			}else{
				
				tuples_total.put((long) i, tups);
				
				List<Pair<String, Tuple>> aux = usefulSentences.get(dname);
				if (aux == null){
					aux = new ArrayList<Pair<String,Tuple>>();
					usefulSentences.put(dname, aux);
				}
				for (Tuple tup : tups) {
					aux.add(new Pair<String, Tuple>(stext, tup));
				}

			}
			
			if (!first){
				bw2.newLine();
			}

			int ivalue = 0;
			if (!tups.isEmpty())
				ivalue = 1;
			
			bw2.write(i + " " + ivalue);

			first = false;
			
			
		}
		
		SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + TUPLES + ".ser" , tuples_total);
		SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + SPLIT_USELESS + ".ser" , uselessSentences);
		SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + SPLIT_USEFUL + ".ser" , usefulSentences);
		SerializationHelper.serialize(f.getAbsolutePath(), new ArrayList<String>(attributes));
		
		bw2.close();
		
	}

	private static List<Tuple> convert(
			List<edu.columbia.cs.ltrie.datamodel.Tuple> tuplesDocument) {
		
		List<Tuple> ret = new ArrayList<Tuple>();
		
		for (edu.columbia.cs.ltrie.datamodel.Tuple tuple : tuplesDocument) {
			
			Tuple tup = new Tuple();
			
			for (String field : tuple.getFieldNames()) {
				
				tup.setTupleField(field, tuple.getData(field).getValue());
				
			}
			
			ret.add(tup);
			
		}
		
		return ret;
		
		
	}

	public static String getUsefulnessFile(String outprefix, String task,
			String relation, String extractor) {
		
		return outprefix + task + "." + relation + "." + extractor + USEFULNESS;
	
	}



	
	
}
