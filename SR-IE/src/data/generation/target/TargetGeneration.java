package data.generation.target;

import java.util.List;

import weka.core.Instances;

public abstract class TargetGeneration {
 
	public abstract List<String> selectFeatures(Instances instances)
			throws Exception;

	public abstract boolean generatesWeights();

	public abstract double[] getWeights();

	public abstract double[] getOwnWeights();

}
