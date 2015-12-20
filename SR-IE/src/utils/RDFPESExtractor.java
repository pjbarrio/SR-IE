package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.NormalizedTuple;
import model.Tuple;

import org.apache.commons.io.FileUtils;

import com.google.gdata.util.common.base.Pair;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

public class RDFPESExtractor {

	private static final String TYPE_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String TYPE_TEXT = "type";
	
	public static List<Pair<Tuple,String[]>> extract(URI uri, String relation) throws IOException{
		
		Graph g = new GraphMem();
		ModelCom model = new ModelCom(g);
		
		try {
		
		model.read(uri.toString());
		
		}
		catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		return _extract(model,relation,"r");
		
	}

	public static List<Pair<Tuple, String[]>> _extract(ModelCom model,
			String relation, String type) {
		
		List<Pair<Tuple,String[]>> ret = new ArrayList<Pair<Tuple,String[]>>();
		
		String relationText = "http://s.opencalais.com/1/type/em/"+type+"/" + relation;
		
		Node nn = Node.createURI(relationText);
		
		ResIterator resIt = model.listResourcesWithProperty(new PropertyImpl(TYPE_PROPERTY,TYPE_TEXT), new LiteralImpl(nn, model));
		
		String ReducedText = "";
		
		int tupleNumber = 0;
		
		while (resIt.hasNext()){
		
			Resource res = resIt.next();
			
			Node complete = Node.createURI(res.toString());
			
			ResIterator resItComplete = model.listResourcesWithProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","subject"), new LiteralImpl(complete, model));
		
			//tuple generation
			
			Resource tuple = model.getResource(complete.getURI());
			
			StmtIterator prp = tuple.listProperties();
			
			Map<String,List<String>> tup = new HashMap<String, List<String>>();
			
			Map<String,List<String>> Reftup = new HashMap<String, List<String>>();
			
			while (prp.hasNext()){
				
				Statement s = prp.next();
				
				String ss = s.getPredicate().getLocalName();
				
				if (s.getObject().isResource()){
					
					Resource auxI = model.getResource(s.getResource().getURI());
					
					StmtIterator prp2 = auxI.listProperties(new PropertyImpl("http://s.opencalais.com/1/pred/name"));
					
					while (prp2.hasNext()){ //it's only one
						
						Statement s2 = prp2.next();
						
						if (!tup.containsKey(ss)){
							tup.put(ss, new ArrayList<String>());
							Reftup.put(ss, new ArrayList<String>());
						}
						
						//XXX ss has the type (e.g., provinceorstate)
						//XXX s2.getObject().toString() has the value (e.g., New York)
						//XXX s2.getSubject().getURI() has the unified reference (e.g., http://d.opencalais.com/genericHasher-1/5fc67ecf-e49d-3ab4-aaad-9cff97f00ffe)
						
						tup.get(ss).add(s2.getObject().toString()); 
						Reftup.get(ss).add(s2.getSubject().getURI()); 
//						t.setTupleField(ss, s2.getObject().toString());
						
					}
					
				} else if (s.getObject().isLiteral()){
					
					if (!tup.containsKey(ss)){
						tup.put(ss, new ArrayList<String>());
						Reftup.put(ss, new ArrayList<String>());
					}
					
					//XXX I can use the string directly: s.getObject().toString()
					
					tup.get(ss).add(s.getObject().toString());
					Reftup.get(ss).add(null); 
					
//					t.setTupleField(ss, s.getObject().toString());
					
				}
				
			}
			
			List<Tuple> ts = createTuples(tup, Reftup);
			
			String prefix = "";
			String exact = "";
			String suffix = "";
			String offset = "";
			String length = "";
			
			while (resItComplete.hasNext()){ //it is always only one
				
				Resource res2 = resItComplete.next();
				
				if (!res2.hasProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","exact")))
					continue;
				
				prefix = res2.getProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","prefix")).getObject().toString();
				exact = res2.getProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","exact")).getObject().toString();
				suffix  = res2.getProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","suffix")).getObject().toString();
				
				offset = res2.getProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","offset")).getObject().toString();
				length = res2.getProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","length")).getObject().toString();
				
				
				ReducedText = ReducedText + prefix + " " + exact + " " + suffix + " ";
				
			}
			
			for (Tuple t : ts) {
			
				ret.add(new Pair<Tuple, String[]>(t, new String[]{prefix,exact,suffix,offset,length}));
				
				tupleNumber++;
				
			}
			
		}
		
		return ret;
		
	}

	private static List<Tuple> createTuples(Map<String, List<String>> tup, Map<String, List<String>> reftup) {
		
		int vals = 1;
		
		for (Entry<String,List<String>> entry : tup.entrySet()) {
			
			if (!entry.getValue().isEmpty())
				vals*=entry.getValue().size();
			
		}
		
		List<Tuple> ret = new ArrayList<Tuple>(vals);
		
		for (int i = 0; i < vals; i++) {
			
			NormalizedTuple t = new NormalizedTuple();
			
			for (Entry<String,List<String>> entry : tup.entrySet()) {
				
				int index = i % entry.getValue().size();
				
				t.setTupleField(entry.getKey(), entry.getValue().get(index), reftup.get(entry.getKey()).get(index));
								
			}
			
			ret.add(t);
			
		}

		return ret;
		
	}

	public static void main(String[] args) throws IOException {
		
		String fileAddr = "D:/Documents/NYTimesExtraction/NYTTrainExtraction/NYTTrainExtraction/1355807.xml.rdf";
		
		URI uri = new File(fileAddr).toURI();
		
		String content = RDFPESExtractor.extractContent(uri);
		
		List<Pair<Tuple, String[]>> aux = RDFPESExtractor.extract(uri, "CompanyLocation");
		
		for (int i = 0; i < aux.size(); i++) {
			System.out.println("\n");
			System.out.println(aux.get(i).getFirst().toString());
			System.out.println(Arrays.toString(aux.get(i).getSecond()));
			
			System.out.println(content.substring(Integer.valueOf(aux.get(i).getSecond()[3]), Integer.valueOf(aux.get(i).getSecond()[3]) + Integer.valueOf(aux.get(i).getSecond()[4])));
			
		}
				
	}

	public static String extractContent(URI uri){
		
		Graph g = new GraphMem();
		ModelCom model = new ModelCom(g);
		
		try {
		
		model.read(uri.toString());
		
		}
		catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		String contentText = "http://s.opencalais.com/1/type/sys/DocInfo";
		
		Node nn = Node.createURI(contentText);
		
		ResIterator resIt = model.listResourcesWithProperty(new PropertyImpl(TYPE_PROPERTY,TYPE_TEXT), new LiteralImpl(nn, model));
		
		String exact = null;
		
		while (resIt.hasNext()){ //it is always only one
			
			Resource res2 = resIt.next();
			
			if (!res2.hasProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","document")))
				continue;
			
			exact = res2.getProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","document")).getObject().toString();
			
		}

		return exact;
		
	}
	
	public static List<Pair<Tuple, String[]>> extractEntity(ModelCom model,
			String entity) {
		return _extract(model,entity,"e");
	}

	public static List<Pair<Tuple, String[]>> extract(ModelCom model,
			String relation) {
		return _extract(model,relation,"r");
	}
	
}
