package data.generation.target.selection;

import java.nio.channels.UnsupportedAddressTypeException;

import data.generation.target.TargetGenerator;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.RemoveUseless;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class UsefulOnlyFeatureSelection extends FeatureSelection {

	@Override
	protected Instances _selectFeatures(Instances instances) throws Exception{
		
		RemoveWithValues r = new RemoveWithValues();
		r.setAttributeIndex(Integer.toString(TargetGenerator.USEFUL_INDEX+1));
		r.setNominalIndices("1");
		r.setInputFormat(instances);
		instances = Filter.useFilter(instances, r);

		RemoveUseless ru = new RemoveUseless();
		ru.setInputFormat(instances);

		return Filter.useFilter(instances, ru);

		
	}

	@Override
	public boolean generatesWeights() {
		return false;
	}

	@Override
	public double[] getWeights() {
		throw new UnsupportedAddressTypeException();
	}

	@Override
	public double[] getOwnWeights() {
		throw new UnsupportedAddressTypeException();
	}
	
}
