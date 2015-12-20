package utils.wordmodel;

import java.io.IOException;
import java.nio.channels.UnsupportedAddressTypeException;

public class DataGenerationParameterUtils {

	public final static String[] reelrelations = {"NaturalDisaster","VotingResult","ManMadeDisaster","Indictment-Arrest-Trial","PersonCareer"};
	
	public final static String[] relations = {"PersonParty","CompanyLocation","FamilyRelation","PersonAttributes","Extinction",
		"PoliticalRelationship","EnvironmentalIssue","PersonTravel",
		"PersonCareer","ProductRecall","CompanyLaborIssues","VotingResult","CompanyLegalIssues",
		"PersonLocation","IPO","CompanyMeeting","NaturalDisaster","CandidatePosition","Quotation","CompanyAffiliates",
		"DiplomaticRelations","ContactDetails","AnalystRecommendation","Buybacks","PatentFiling","CompanyInvestment",
		"CompanyLayoffs","Conviction","Indictment","EmploymentChange","ConferenceCall","Bankruptcy","StockSplit",
		"Dividend","CompanyCompetitor","CompanyEmployeesNumber","Trial","CompanyNameChange","DelayedFiling",
		"PoliticalEndorsement","CreditRating","BusinessRelation","BonusSharesIssuance","Acquisition",
		"CompanyForceMajeure","CompanyProduct","PersonCommunication","ArmedAttack","CompanyUsingProduct",
		"IndicesChanges","CompanyEarningsAnnouncement","MusicAlbumRelease","CompanyTechnology",
		"CompanyExpansion","CompanyFounded","AnalystEarningsEstimate","PersonEducation","PatentIssuance",
		"JointVenture","Arrest","MovieRelease","PersonEmailAddress","FDAPhase","SecondaryIssuance","GenericRelations",
		"CompanyRestatement","EquityFinancing","ManMadeDisaster","ArmsPurchaseSale","MilitaryAction","ProductIssues",
		"Alliance","DebtFinancing",
		"CompanyTicker","CompanyReorganization","CompanyAccountingChange","Merger",
		"EmploymentRelation","ProductRelease","CompanyListingChange","PersonRelation","CompanyEarningsGuidance",
		"PollsResult","CompanyCustomer"};
	
	public final static String[] extractors = {"default","Pablo-Dependency-Graph","Pablo-N-Grams","Pablo-Shortest-Path","Pablo-Sub-sequences"};
	public static String[] reelprintablerelatons = {"Natural Disaster-Location","Election-Winner","Man Made Disaster-Location","Person-Charge","Person-Career"};

	public final static String[] extractors_simple = {"default","DG","BONG","SPK","SSK"};
	
	public final static String[] tasks = {"Train", "Validation", "Test"};
	
	public final static String[] word2Vecs = {"","",""};


	public static boolean isBinary(String word2vec) {
		if (word2vec.equals("C:/Users/Pablo/Downloads/GoogleNews-vectors-negative300.bin"))
			return true;
		else 
			return word2vec.endsWith(".bin");
//		throw new UnsupportedOperationException("Model does not exist");
	}

	public static int getVectorSize(String word2Vec) {
		if (word2Vec.endsWith("1000.bin")){
			return 1000;
		}else if (word2Vec.endsWith("500.bin")){
			return 500;
		}else if (word2Vec.endsWith("300.bin")){
			return 300;
		}else if (word2Vec.endsWith("100.bin")){
			return 100;
		}else if (word2Vec.endsWith("50.bin")){
			return 50;
		}else{
			throw new UnsupportedAddressTypeException();
		}
		
	}


	
	public static int getLongestNGram(String word2vec) {
		if (word2vec.equals("C:/Users/Pablo/Downloads/GoogleNews-vectors-negative300.bin"))
			return 2;
		else if (word2vec.contains("data.txt")){
			return 1;
		}else if (word2vec.contains("phrase-unigram"))
			return 2;
		else if (word2vec.contains("phrase-bigram"))
			return 3;
		else if (word2vec.contains("phrase-trigram"))
			return 4;
		
		throw new UnsupportedOperationException("Model does not exist");
	}

	public static String getSourceFolder(String task) {
		
		try {
			if (Runtime.getRuntime().exec("hostname").equals("monster-win8")){
			
				if (task.equals("Toy")){
					return "D:/Documents/NYTimesExtraction/NYTToyExtraction/";
				}
				return "D:/Documents/NYTimesExtraction/NYTTrainExtraction/NYT"+task+"Extraction/";
				
			} else{ //It's on Columbia's network
				
				if (task.equals("Small")){
					return "/proj/db-files2/NoBackup/pjbarrio/Dataset/SmallExtraction/";
				}
				else{
					return "/local/pjbarrio/Files/Downloads/NYT" + task + "Extraction/";
				}
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
		
	}

	public static String getFileListName(String task) {
		return task + ".filelist";
	}

	public static String getAttributesFileName(String relation,
			String extractor, String task) {
		return task + "." + relation + "." + extractor + ".ser";
	}

	public static int getCollectionId(String task) {
		if (task.equals("Toy")){
			return 0;
		}else if (task.equals("Train")){
			return 1;
		}else if (task.equals("Small")){
			return 2;
		}else if (task.equals("LTrain")){
			return 3;
		}else{
			throw new UnsupportedAddressTypeException();
		}
	}

	public static String getCollectionIndex(String task) {
		if (task.equals("LTrain")){
			return "/proj/db-files2/NoBackup/pjbarrio/Dataset/indexes/NYTTrain100Index-Sentence";
		}else{
			throw new UnsupportedAddressTypeException();
		}
	}

	public static String getPath(String task) {
		if (task.equals("LTrain")){
			return "/proj/db-files2/NoBackup/pjbarrio/Dataset/NYTTrain/";
		}else{
			throw new UnsupportedAddressTypeException();
		}
	}

	public static String getSuffix(String task) {

		if (task.equals("LTrain")){
			return "_Train-sentence";
		}else{
			throw new UnsupportedAddressTypeException();
		}
		
		
	}

	public static boolean isLuceneBased(String task) {
		
		if (task.equals("LTrain")){
			return true;
		} else if (task.equals("Train")){
			return false;
		} else{
			throw new UnsupportedAddressTypeException();
		}
		
	}

	public static String getSampleCollection(String task) {
		if (task.equals("LTrain")){
			return "Train";
		} else if (task.equals("Train")){
			return "Train";
		} else{
			throw new UnsupportedAddressTypeException();
		}
	}

	public static WordModelLoader getLoader(String word2vec) {
		if (word2vec.contains("glove")){
			return new MyGloVeLoader();
		}else{
			return new MyWord2VecLoader();
		}
	}

	public static int getVectorSizeTarget(String target) {
		
		if (target.contains("vectors.txt")){
			
			if (target.contains(".txt-1000")){
				return 1000;
			} else if (target.contains(".txt-500")){
				return 500;
			} else if (target.contains(".txt-300")){
				return 300;
			} if (target.contains(".txt-100")){
				return 100;
			} if (target.contains(".txt-50")){
				return 50;
			} 
			
		}
		
		if (target.contains("1000.bin")){
			return 1000;
		}else if (target.contains("500.bin")){
			return 500;
		}else if (target.contains("300.bin")){
			return 300;
		}else if (target.contains("100.bin")){
			return 100;
		}else if (target.contains("50.bin")){
			return 50;
		}else{
			throw new UnsupportedAddressTypeException();
		}
		
	}

	
}
