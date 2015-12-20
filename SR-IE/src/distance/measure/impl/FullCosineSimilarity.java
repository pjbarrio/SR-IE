package distance.measure.impl;

import java.util.Map;

import distance.measure.DistanceMeasure;

public class FullCosineSimilarity extends CosineSimilarity {

	private int size;
	
	public FullCosineSimilarity(int size){
		this.size = size;
	}
	
	@Override
	protected double internalProduct(Map<Integer, Double> featV1,
			Map<Integer, Double> featV2) {
		
		double sum = 0.0;
		
		for (int i = 0; i < size; i++) {
			
			sum += featV1.get(i)*featV2.get(i);
			
		}
		
		return sum;
		
	}

}
