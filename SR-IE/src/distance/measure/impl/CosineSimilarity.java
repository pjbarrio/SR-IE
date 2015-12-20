package distance.measure.impl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import distance.measure.DistanceMeasure;


public class CosineSimilarity extends DistanceMeasure {

	@Override
	public double distance(Map<Integer, Double> featV1, Map<Integer, Double> featV2) {
		
		return (internalProduct(featV1,featV2))/(size(featV1)*size(featV2));
		
	}

	private double size(Map<Integer, Double> vals) {
		
		double sum = 0.0;
		
		for (Double val : vals.values()) {
			
			sum += Math.pow(val, 2.0);
			
		}
		
		return Math.sqrt(sum);
	}

	protected double internalProduct(Map<Integer, Double> featV1,
			Map<Integer, Double> featV2) {
		
		double sum = 0.0;
		
		if (featV1.size() < featV2.size()){
			for (Entry<Integer,Double> entry : featV1.entrySet()) {
				
				Double val = featV2.get(entry.getKey());
				
				if (val != null){
					sum += entry.getValue()*val;
				}
				
			}
		}else{
			for (Entry<Integer,Double> entry : featV2.entrySet()) {
				
				Double val = featV1.get(entry.getKey());
				
				if (val != null){
					sum += entry.getValue()*val;
				}
				
			}
		}
		
		return sum;
		
	}

}
