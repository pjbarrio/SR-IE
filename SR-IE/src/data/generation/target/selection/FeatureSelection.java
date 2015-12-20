package data.generation.target.selection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deeplearning4j.models.word2vec.Word2Vec;

import data.generation.target.TargetGeneration;
import data.generation.target.TargetGenerator;

import utils.wordmodel.DataGenerationParameterUtils;
import utils.wordmodel.MyWord2VecLoader;
import weka.core.Instances;

public abstract class FeatureSelection extends TargetGeneration{

	@Override
	public List<String> selectFeatures(Instances instances) throws Exception{
		
		instances = _selectFeatures(instances);
		
		Set<String> feats = new HashSet<String>();
		
		for (int i = 0; i < instances.numAttributes(); i++) {
			
			if (i != TargetGenerator.USEFUL_INDEX){
				
				feats.add(instances.attribute(i).name());
				
			}
			
		}
		
		return new ArrayList<String>(feats);
		
	}
 	
	protected abstract Instances _selectFeatures(Instances instances) throws Exception;

	

}
