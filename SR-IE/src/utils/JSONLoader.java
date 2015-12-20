package utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FileUtils;

public class JSONLoader {

	public static Map<String,List<String>> loadMap(String file) throws IOException{
		
		Map<String,List<String>> map = new HashMap<String,List<String>>();
		
		map.put("yes",new ArrayList<String>());
		
		map.put("no",new ArrayList<String>());
		
		List<String> lines = FileUtils.readLines(new File(file));
		
		for (int i = 0; i < lines.size(); i++) {
		
			JSONObject json = (JSONObject) JSONSerializer.toJSON(lines.get(i));
		
			Object[] coll = json.values().toArray();
			
			String snippet = ((JSONArray)coll[3]).getJSONObject(0).getString("snippet");
			
			JSONArray arr = ((JSONArray)coll[4]);

			double yes = 0;
			
			for (int j = 0; j < arr.size(); j++) {
				
				if ("yes".equals(arr.getJSONObject(j).getString("judgment")))
					yes++;

			}
			
			String key = "no";
			
			if (yes > 2){
				key = "yes";
			}
			
			map.get(key).add(snippet);
			
		}

		return map;
		
	}

}
