package data.generation;

import java.io.File;

import utils.SerializationHelper;
import utils.wordmodel.DataGenerationParameterUtils;

public class CreateFileList {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String task = args[0];
		String outprefix = args[1]; //e.g., "data/omp/test1/";
		
		new File(outprefix).mkdirs();
		
		String folder = DataGenerationParameterUtils.getSourceFolder(task);
		
		SerializationHelper.serialize(outprefix + DataGenerationParameterUtils.getFileListName(task), new File(folder).listFiles());
		
	}

}
