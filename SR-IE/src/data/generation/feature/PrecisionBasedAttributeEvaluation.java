package data.generation.feature;

import weka.core.Instances;

public class PrecisionBasedAttributeEvaluation extends AttributeEvaluation {

	private Instances data;
	private double[] classValues;
	private double pUseless;
	private double pUseful;
	private double countUseless;
	private double countUseful;
	private double threshold;

	public PrecisionBasedAttributeEvaluation(Instances data, double threshold) {
		this.data = data;
		this.threshold = threshold;
		
		classValues = data.attributeToDoubleArray(data.classIndex());
		int[] counts = data.attributeStats(data.classIndex()).nominalCounts;
		countUseless = (double)counts[0];
		countUseful = (double)counts[1];
		pUseless = (double)counts[0] / (double)(data.numInstances());
		pUseful = (double)counts[1] / (double)(data.numInstances());
		
	}

	@Override
	public boolean keep(int attribute) {
		
		double[] attrValues = data.attributeToDoubleArray(attribute);

		double pAttgt0 = 0.0;
		double pAttgt0Uf = 0.0;
		double pAttgt0Ul = 0.0;

		for (int j = 0; j < attrValues.length; j++) {

			if (attrValues[j] > 0){ //Attribute is 1
				pAttgt0++;
				if (classValues[j] == 1){ //is Useful
					pAttgt0Uf++;
				} else { //is Useless
					pAttgt0Ul++;
				}
			}
			

		}

		pAttgt0 /= (double)data.numInstances();
		pAttgt0Uf /= (double)countUseless;
		pAttgt0Ul /= (double)countUseful;

		double pUfAttgt0 = pUseful * pAttgt0Uf / pAttgt0;
		double pUlAttgt0 = pUseless * pAttgt0Ul / pAttgt0;

		return pUfAttgt0 > threshold; //could also be pUfAttgt0 > pUlAttgt0
		
	}

	
	
}
