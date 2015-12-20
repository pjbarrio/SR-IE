package sentence.splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ie.machinereading.structure.Span;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class StanfordCoreNLPSentenceSplitter {

	private StanfordCoreNLP pipeline;

	public StanfordCoreNLPSentenceSplitter (){

		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit");
		pipeline = new StanfordCoreNLP(props);

	}

	public List<Span> tokenizeSentences(String content){


		// create an empty Annotation just with the given text
		Annotation document = new Annotation(content);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		List<Span> ret = new ArrayList<Span>();
		
		for(CoreMap sentence: sentences) {

			ret.add(new Span(sentence.get(CharacterOffsetBeginAnnotation.class), sentence.get(CharacterOffsetEndAnnotation.class)));
			
		}

		return ret;
		
	}

	public static void main(String[] args) {
		
		String text = "Waste in the Womb\nQ. If the fetus is nourished through the placenta while in the womb, what happens to the waste?\nA. The placenta is the middle part of a complex two-way circulation system between the pregnant woman and the fetus. The fetus is attached to the placenta by the umbilical cord, and the other side of the placenta is in contact with the walls of the uterus. Through the umbilical cord, the system not only carries things like water, glucose and vitamins to the developing fetus and supplies it with oxygen, it also carries away waste products, including urea, uric acid and bilirubin, to be disposed of through the mother's blood circulation.\nThe fetal and maternal blood supplies do not directly mix, but components pass back and forth through tiny projections of the thin membrane called the chorion, which surrounds the fetus. These structures, called the chorionic villi, carry the fetal blood supply often sampled in prenatal tests. The chorionic villi create a large surface to maximize the exchange that takes place between the two blood supplies.\nThe umbilical and placental circulation system differs from the regular circulation in that veins, not arteries, carry the oxygenated blood to the fetus, and arteries, not veins, carry away the oxygen-depleted blood, with the waste products, back to rejoin the mother's blood supply.\nC. CLAIBORNE RAY\nReaders are invited to submit questions by mail to Question, Science Times, The New York Times, 229 West 43rd Street, New York, N.Y. 10036-3959, or by e-mail to question@nytimes.com.";
		
		new StanfordCoreNLPSentenceSplitter().tokenizeSentences(text);
		
	}
	
}
