package data.generation.candidates.selection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import utils.SerializationHelper;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.rules.JRip;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import data.generation.InputFilesGenerator;
import data.generation.feature.AttributeEvaluation;
import data.generation.feature.PrecisionBasedAttributeEvaluation;

public class FeatureRanker {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		String relation = args[0];
		String extractor = args[1];
		String outprefix = args[2]; 
		double acceptable = Double.valueOf(args[3]); //e.g., 0.95;
		double increment = Double.valueOf(args[4]); //e.g., 0.05;
		String task = args[5];
		
		int crossValidationModels = 10;
		double threshold = 0.5;		
		
		File dataFile = new File(outprefix + task + "." + relation + "." + extractor + InputFilesGenerator.CANDIDATE + ".arff");

		BufferedReader reader = new BufferedReader(
				new FileReader(dataFile));

		Instances data = new Instances(reader);

		data.setClass(data.attribute(data.numAttributes()-1));

		ASEvaluation eval = new ChiSquaredAttributeEval();
		Ranker search = new Ranker();

		//Evaluate Classifier on Selected Features
		Classifier base = new SMO();

		double bestPerformance = 0.0;
		double bestFraction = 1.0;
		int previousSelected = data.numAttributes()-1;

		for (double fraction = 1; fraction >= increment; fraction-=increment) { //TODO Could explore in binary search

			AttributeSelectedClassifier classifier = new AttributeSelectedClassifier();
			classifier.setClassifier(base);
			classifier.setEvaluator(eval);
			search.setNumToSelect((int)Math.round(fraction*(data.numAttributes()-1)));
			classifier.setSearch(search);
			// 10-fold cross-validation
			Evaluation evaluation = new Evaluation(data);
			evaluation.crossValidateModel(classifier, data, crossValidationModels, new Random(1));
			//System.out.println(evaluation.toSummaryString());
			System.out.println(evaluation.toClassDetailsString("For: " + (int)Math.round(fraction*(data.numAttributes()-1))));

			if (fraction == 1){
				bestPerformance = evaluation.fMeasure(1);
			} else {

				double performance = evaluation.fMeasure(1);

				if (performance > acceptable * bestPerformance){
					previousSelected = (int)Math.round(fraction*(data.numAttributes()-1)); //TODO change if binary search
					if (performance > bestPerformance){
						bestPerformance = performance;
						bestFraction = fraction;
					}
				}else{
					break;
				}

			}

		}

		//Attribute Selection low-level
		AttributeSelection attsel = new AttributeSelection();  // package weka.attributeSelection!
		attsel.setEvaluator(eval);
		search.setNumToSelect(previousSelected);
		attsel.setSearch(search);
		attsel.SelectAttributes(data);
		int[] indices = attsel.selectedAttributes();
		//I need to find those that are correlated to the useful class.
		//If they are more present than absent in the class.

		AttributeEvaluation attEval = new PrecisionBasedAttributeEvaluation(data,threshold);
		
		List<Integer> selectedIndexes = new ArrayList<Integer>();
		
		for (int i = 0; i < indices.length-1; i++) { //-1 because of the class index

			if (attEval.keep(indices[i])){
				selectedIndexes.add(indices[i]);
			}
			
		}

		SerializationHelper.serialize(outprefix + task + "." + relation + "." + extractor + "." + eval.getClass().getSimpleName() + "." + acceptable + "." + InputFilesGenerator.CANDIDATE + ".ser", selectedIndexes);
		System.out.println("Best Fraction: " + bestFraction);
		

	}

}
