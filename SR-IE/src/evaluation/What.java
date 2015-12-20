package evaluation;

import java.util.Map;
import java.util.Set;

import utils.SerializationHelper;

public class What {

	public static void main(String[] args) {
		
		Map<String,Set<String>> mapExpts = (Map<String, Set<String>>) SerializationHelper.deserialize("exp_summarize.summary");
		
		for (String prefix : mapExpts.get("impact_target")){

			System.out.println(prefix);
			

		}
		
		
	}
	
	
}
