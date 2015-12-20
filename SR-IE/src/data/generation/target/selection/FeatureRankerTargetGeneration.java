package data.generation.target.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deeplearning4j.models.word2vec.Word2Vec;

import utils.wordmodel.DataGenerationParameterUtils;
import utils.wordmodel.MyWord2VecLoader;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeEvaluator;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.Ranker;
import weka.core.Instances;
import data.generation.target.TargetGeneration;
import edu.columbia.cs.ltrie.sampling.queries.generation.ChiSquaredWithYatesCorrectionAttributeEval;

public class FeatureRankerTargetGeneration extends TargetGeneration {

	private ASEvaluation eval; //= new ChiSquaredWithYatesCorrectionAttributeEval();//new SVMAttributeEval();//new InfoGainAttributeEval();// {/*new InfoGainAttributeEval(),*//*,new SMOAttributeEval()*/};
	private double beta;
	private double[] weights;
	private double[] ownWeights;
	private boolean evaluateClass;
	private AttributeEvaluator attEval;
	
	public FeatureRankerTargetGeneration(ASEvaluation eval, boolean evaluateClass) {
		this.eval = eval;
		beta = 1;
		this.evaluateClass = evaluateClass;
		attEval = (AttributeEvaluator)eval;
		
	}
	
	@Override
	public List<String> selectFeatures(Instances instances)
			throws Exception {
		
		double betasq = beta*beta;
		
		int[] counts = instances.attributeStats(instances.classIndex()).nominalCounts;
		double countUseless = (double)counts[0];
		double countUseful = (double)counts[1];
		
		AttributeSelection attsel = new AttributeSelection();
		
		ASSearch search = new Ranker();
		
		System.out.println("About to select attributes" + eval.getClass().getSimpleName());
		
		long time = System.currentTimeMillis();
		
	    attsel.setEvaluator(eval);
	    attsel.setSearch(search);
	    attsel.SelectAttributes(instances);
	    int[] indices = attsel.selectedAttributes();
	    
	    double[] classValues = instances.attributeToDoubleArray(instances.classIndex());
	    
	    List<String> feats = new ArrayList<String>();
	    List<String> inverseFeats = new ArrayList<String>();
	    weights = new double[instances.numAttributes()-1]; //last one is class
	    ownWeights = new double[instances.numAttributes()-1];
	    
	    time = System.currentTimeMillis() - time;
	    
	    for (int ind = 0; ind < indices.length - 1; ind++) { //last one is class
	    	
	    	int attribute = indices[ind];
	    	
	    	double[] attrValues = instances.attributeToDoubleArray(attribute);

		    double ufulCount = 0.0;
		    double ulessCount = 0.0;
		    
			for (int j = 0; j < attrValues.length; j++) {

				if (attrValues[j] > 0){ //Attribute is 1
					if (classValues[j] == 1){ //is Useful
						ufulCount++;
					} else { //is Useless
						ulessCount++;
					}
				}
				
			}

			double precision = 0.0;
			
			if (ulessCount+ufulCount > 0)
				precision = ufulCount / (ulessCount+ufulCount);
			
			double recall = ufulCount / countUseful;
				
			double fmeasure = (1.0 + betasq) * ((precision*recall)/((betasq*precision)+recall));
			
			if (Double.isNaN(fmeasure)){
				fmeasure = 0.0;
				
			}
			
	    	if (evaluateClass && ((countUseful/countUseless) > (ufulCount/ulessCount))){ //Evaluates for the odds.
	    		inverseFeats.add(instances.attribute(attribute).name());
	    		weights[weights.length - inverseFeats.size()] = fmeasure;
	    		ownWeights[weights.length - inverseFeats.size()] = attEval.evaluateAttribute(attribute);
		    	
			    
	    	}else{
	    		weights[feats.size()] = fmeasure;
	    		ownWeights[feats.size()] = attEval.evaluateAttribute(attribute);
	    		feats.add(instances.attribute(attribute).name());
	    	}
			
		}
	    
	    List<String> ret = new ArrayList<String>(feats.size() + inverseFeats.size());
	    
	    System.out.println("Selected Attributes" + time);
		
	    for (int i = 0; i < feats.size(); i++) {
	    	ret.add(feats.get(i));
		}
	    
	    for (int i = inverseFeats.size()-1; i >= 0; i--) {
	    	ret.add(inverseFeats.get(i));
		}
	    
	    return ret;
	    
	}

	@Override
	public boolean generatesWeights() {
		return true;
	}

	@Override
	public double[] getWeights() {
		return weights;
	}

	public double[] getOwnWeights(){
		return ownWeights;
	}
	
}
