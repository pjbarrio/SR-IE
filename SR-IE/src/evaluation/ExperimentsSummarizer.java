package evaluation;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import utils.SerializationHelper;

public class ExperimentsSummarizer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String pref = "/proj/db-files2/NoBackup/pjbarrio/SentenceRanking/";
		
		String exp_out_folder = pref + args[0]; //data/train/output/
		String exp_out_lfolder = pref + args[1]; //data/ltrain/output/
		
		String which = args[2];
		
		File[] exps_pref = new File(exp_out_folder).listFiles();
		
		Map<String,Set<String>> mapExps = new HashMap<String,Set<String>>();
		
		for (int i = 0; i < exps_pref.length; i++) {
			
			if (which != null && !exps_pref[i].getName().equals(which))
				continue;
			
			Set<String> exps = new HashSet<String>();
			
			System.out.println(exps_pref[i].getName());
			
			String[] out_files = exps_pref[i].list();
			String[] out_lfiles = new File(exp_out_lfolder,exps_pref[i].getName()).list();
			
			for (int j = 0; j < out_files.length; j++) {
				exps.add(args[0] + exps_pref[i].getName() + "/" + getName(out_files[j]));
			}
			
			if (out_lfiles != null){
			
				for (int j = 0; j < out_lfiles.length; j++) {
					exps.add(args[1] +exps_pref[i].getName() + "/" + getName(out_lfiles[j]));
				}
			
			}
			
			mapExps.put(exps_pref[i].getName(), exps);
			
		}
		
		for (Entry<String, Set<String>> entry : mapExps.entrySet()) {
			
			System.out.println(entry.getKey());
			
			System.out.println(entry.getValue().toString());
			
		}
		
		SerializationHelper.serialize(which == null?"exp_summarize.summary":which+"_exp_summarize.summary", mapExps);

	}

	private static String getName(String string) {
		return string.substring(0, string.lastIndexOf('.'));
	}

}
