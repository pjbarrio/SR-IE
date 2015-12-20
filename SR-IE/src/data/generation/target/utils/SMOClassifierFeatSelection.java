package data.generation.target.utils;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeEvaluator;
import weka.classifiers.functions.SMO;
import weka.core.Instances;

public class SMOClassifierFeatSelection extends ASEvaluation implements AttributeEvaluator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	double[] weights;
	
	@Override
	public void buildEvaluator(Instances instances) throws Exception {
		System.out.println(instances.numAttributes());
		SMO classifier = new SMO();
		classifier.setBuildLogisticModels(true);
		classifier.buildClassifier(instances);
		
		double[][][] sparseW = classifier.sparseWeights();
		int[][][] sparseI = classifier.sparseIndices();
		
		//Transform to dense representation
		
		weights = new double[instances.numAttributes()];
		
		for (int i = 0; i < sparseI[0][1].length; i++) {
			weights[sparseI[0][1][i]] = sparseW[0][1][i];
		}
				
		System.out.println(weights.length);
	}

	@Override
	public double evaluateAttribute (int attribute){
		
		return weights[attribute];
	}
	
}
