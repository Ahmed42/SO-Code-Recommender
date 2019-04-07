package sproc.processor;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class Evaluation {
	
	private String codeSnipsFile;
	private String vectorsFile;
	
	private int noOfTests;
	private int k;
	
	int truePositives;
	
	//private String ASTsFile;
	private int totalNoOfSnips;
	private Recommender.Distance typeOfDistance;
	
	private long elapsedTime;
	
	public Evaluation(String codesFile, String vectorsFile, int totalNoOfSnips,
			int noOfTests, int k, Recommender.Distance typeOfDistance) {
		this.codeSnipsFile = codesFile;
		this.vectorsFile = vectorsFile;
		this.totalNoOfSnips = totalNoOfSnips;
		
		this.noOfTests = noOfTests;
		this.k = k;
		this.typeOfDistance = typeOfDistance;
		
		this.truePositives = 0;
	}
	
	// params: number of code snips to evaluate
	// value of k in precision/recall@k
	// TODO number of leaf nodes to prune
	
	public void evaluate() {
		// randomely select code snips
		List<Pair<CSVRecord, CSVRecord>> snipsAndVecs = getRandomSnipsAndVecs(noOfTests, totalNoOfSnips);
		
		// might not need this
		//List<Recommender> recommenders = new LinkedList<>();
		
		truePositives = 0;
		int count = 0;
		long startTime = System.nanoTime();
		
		for(Pair<CSVRecord, CSVRecord> snipAndVector : snipsAndVecs) {
			CSVRecord codeRecord = snipAndVector.Left;
			CSVRecord vectorRecord = snipAndVector.Right;
			
			String codeSnip = codeRecord.get("Content");
			
			
			long codeBlockId = Long.parseUnsignedLong(codeRecord.get("CodeBlockId"));
			
			//System.out.println("Executing test no. " + count);
			//long startTime = System.nanoTime();
			Recommender recommender = new Recommender(codeSnipsFile, vectorsFile, codeSnip);
			
			List<Pair<Long, Double>> recommendedSnips = recommender.getTopSnips(k, typeOfDistance);
			
			//long endTime = System.nanoTime();
	    	//long elapsedTime = endTime - startTime;
	    	
	    	//System.out.println("Test no: " + count + ". Processing time: " + elapsedTime/(Math.pow(10, 9)) + " seconds");
			
			if(recommendedSnips.stream().filter(pair -> pair.Left == codeBlockId).findFirst().isPresent()) {
				truePositives++;
			}
			
			count++;
			// might not need this
			//recommenders.add(recommender);
		}
		long endTime = System.nanoTime();
    	elapsedTime = endTime - startTime;
		
		// TODO randomely prune each snippet
		
		// parse code snips to ASTs then to vectors
	}
	
	public void printResults() {
		System.out.println("=== === === === ===");
		String distanceMeasure = typeOfDistance == typeOfDistance.EUCLIDEAN ? "Euclidean" : "Cosine";
		System.out.println("Distance measure: " + distanceMeasure);
		System.out.println("No of tests: " + noOfTests);
		System.out.println("Randomely chosen from: " + totalNoOfSnips + " code snippets");
		System.out.println("Are pruned? " + "no");
		System.out.println("Correct predictions @" + k + ": " + truePositives);
		double precision = (truePositives/(double) noOfTests) * 100;
		System.out.println("Precision @" + k + ": " + precision + "%");
		System.out.println("Total time: " + elapsedTime/Math.pow(10, 9) + " seconds");
		System.out.println("=== === === === ===");
	}
	
	
	private List<Integer> getRandomIndices(int numOfSamples, int populationSize) {
		List<Integer> randomIndices = new LinkedList<>();
		
		while(numOfSamples >= 0) {
			int nextInt = ThreadLocalRandom.current().nextInt(populationSize);
			
			if(randomIndices.contains(nextInt)) {
				continue;
			} else {
				randomIndices.add(nextInt);
				numOfSamples--;
			}
		}
		
		return randomIndices;
	}
	
	// Sample a random k code snips and their vectors
	private List<Pair<CSVRecord, CSVRecord>> getRandomSnipsAndVecs(int numOfSamples, int populationSize) {
		List<Integer> randomIndices = getRandomIndices(numOfSamples, populationSize);
		randomIndices.sort(null);
		List<Integer> randomIndicesCopy = new LinkedList<>(randomIndices);
		
		
		Iterable<CSVRecord> codes = loadCSVDataFrom(codeSnipsFile, true);
		Iterable<CSVRecord> vectors = loadCSVDataFrom(vectorsFile, false);
		
		List<CSVRecord> sampledCodes = new LinkedList<CSVRecord>();
		List<CSVRecord> sampledVectors = new LinkedList<CSVRecord>();
		
		int currentIndex = 0;
		int currentRandIndex = randomIndices.remove(0);
		
		//List<Long> selectedCodeIds = new LinkedList<>();
		
		for(CSVRecord vector : vectors) {
			if(randomIndices.size() == 0) {
				break;
			}
			
			if(currentIndex == currentRandIndex) {
				sampledVectors.add(vector);
				
				currentRandIndex = randomIndices.remove(0);
			}
			currentIndex++;
		}
		
		List<CSVRecord> sampledVectorsCopy = new LinkedList<>(sampledVectors);
		List<Pair<CSVRecord, CSVRecord>> sampledCodesAndVectors = new LinkedList<>();
		
		CSVRecord currentVector = sampledVectorsCopy.remove(0);
		for(CSVRecord code : codes) {
			if(sampledVectorsCopy.size() == 0) {
				break;
			}
			
			long codeBlockId = Long.parseUnsignedLong(code.get("CodeBlockId"));
			long currentVectorCodeId = Long.parseUnsignedLong(currentVector.get(0));
			
			if(codeBlockId == currentVectorCodeId) {
				sampledCodesAndVectors.add(new Pair<CSVRecord, CSVRecord>(code, currentVector));
				
				currentVector = sampledVectorsCopy.remove(0);
			}
		}
		
		
		return sampledCodesAndVectors;
	}
	
	private Iterable<CSVRecord> loadCSVDataFrom(String fileName, boolean isHeaderPresent) {
		Reader reader = null;
		Iterable<CSVRecord> records = null;
		
		try {
			reader = new FileReader(fileName);
			if(isHeaderPresent) {
				records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
			} else {
				records = CSVFormat.DEFAULT.parse(reader);
			}
			
		} catch (IOException e) {
			// File does not exist or non-prasable
			System.out.println("Can't load data from: " + fileName);
		} 
		
		return records;
	}

}
