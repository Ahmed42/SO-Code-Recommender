package sproc.processor;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

public class Recommender {
	
	private String queryCode;
	private ASTNode queryCodeAST;
	private int[] queryCodeVector;
	
	private String SOSnipsFile;
	private String SOSnipsVectorsFile;
	//private String SOSnipsASTFile;
	
	//private Iterable<CSVRecord> snipsRecrods;
	//private Iterable<CSVRecord> vectorsRecrods;
	
	private List<Pair<Long, Double>> sortedEuclideanDistances;
	private List<Pair<Long, Double>> sortedCosineDistances;
	
	private int[] kindsOfParsing;
	
	// Load and parse all vectors in memory ONCE!
	//private static List<CSVRecord> IdsAndVectors;
	private static List<Pair<Long, int[]>> IdsAndVectors = new LinkedList<>();
	
	
	public enum Distance { EUCLIDEAN, COSINE }
	
	public Recommender(String SOSnipsFile, String SOVectorsFile, String queryCode, ASTNode queryCodeAST) {
		this.queryCode = queryCode;
		this.queryCodeAST = queryCodeAST;
		this.SOSnipsFile = SOSnipsFile;
		this.SOSnipsVectorsFile = SOVectorsFile;
		
		sortedEuclideanDistances = new LinkedList<>();
		sortedCosineDistances = new LinkedList<>();
		
		if(IdsAndVectors == null) {
			IdsAndVectors = new LinkedList<>();
			}
		
		kindsOfParsing = new int[] {
				ASTParser.K_COMPILATION_UNIT, 
    			ASTParser.K_CLASS_BODY_DECLARATIONS, 
    			ASTParser.K_STATEMENTS,
    			ASTParser.K_EXPRESSION
		};
	}
	
	// Returns the snips CodeBlockIds and their similarity scores
	public List<Pair<Long, Double>> getTopSnips(int noOfsnipsToRecom, Distance typeOfDistance) {
		computeQueryCodeVector();
		
		
		if(typeOfDistance == Distance.EUCLIDEAN) {
			if(sortedEuclideanDistances.size() == 0) {
				//Iterable<CSVRecord> IdsAndVectors  = getOrGenerateVectors();
				//long startTime = System.nanoTime();
				getOrGenerateVectors();
				//long endTime = System.nanoTime();
		    	//long elapsedTime = endTime - startTime;
		    	//System.out.println("Vectors Time: " + elapsedTime/Math.pow(10, 9) + " seconds.");
				
		    	
		    	//long startTime2 = System.nanoTime();
				computeDistances(typeOfDistance);
				//long endTime2 = System.nanoTime();
		    	//long elapsedTime2 = endTime2 - startTime2;
		    	//System.out.println("Distances Time: " + elapsedTime2/Math.pow(10, 9) + " seconds.");
			}
			
			
			
			sortedEuclideanDistances.sort(Comparator.comparingDouble(distPair -> distPair.Right));
			
			
			return sortedEuclideanDistances.subList(0, noOfsnipsToRecom -1);
			
		} else if (typeOfDistance == Distance.COSINE) {
			if(sortedCosineDistances.size() == 0) {
				//Iterable<CSVRecord> IdsAndVectors  = getOrGenerateVectors();
				getOrGenerateVectors();
				computeDistances(typeOfDistance);
			}
			
			Comparator<Pair<Long, Double>> comp = Comparator.comparingDouble(distPair -> distPair.Right);
			sortedCosineDistances.sort(comp.reversed());
			
			return sortedCosineDistances.subList(0, noOfsnipsToRecom -1);
		}
		
		
		return null;
	}
	
	public String getQueryCode() {
		return queryCode;
	}
	
	public int[] getQueryCodeVector() {
		return queryCodeVector;
	}
	
	
	private void computeQueryCodeVector() {
		if(queryCodeAST == null) {
			Pair<ASTNode, Integer> typedAST =  getCodeSnipTypedAST(queryCode);
			queryCodeAST = typedAST.Left;
		}
		
		if(queryCodeAST != null) {
			queryCodeVector = computeTreeVector(queryCodeAST);
		} else {
			System.out.println("Malformed query code!");
		}
	}
	
	private void computeDistances(Distance typeOfDistance) {
		
		for(Pair<Long, int[]> idAndVector : IdsAndVectors) {
			long codeBlockId = idAndVector.Left;
			int[] codeVector = idAndVector.Right;
    		double computedDistance;
    		
    		if(typeOfDistance == Distance.EUCLIDEAN) {
    			computedDistance = computeEuclideanDistance(queryCodeVector, codeVector);
    			sortedEuclideanDistances.add(new Pair<Long, Double>(codeBlockId, computedDistance));
    		}
    		else if(typeOfDistance == Distance.COSINE) {
    			computedDistance = computeCosineDistance(queryCodeVector, codeVector);
    			sortedCosineDistances.add(new Pair<Long, Double>(codeBlockId, computedDistance));
    		} else {
    			System.out.println("This shouldn't happen!");
    		}		
		}
	}
	
	private void getOrGenerateVectors() {
		if(IdsAndVectors.size() != 0) {
			return;
		}
		// Check if vector file exists
		Reader reader = null;
		Iterable<CSVRecord> records = null;
		
		try {
			reader = new FileReader(SOSnipsVectorsFile);
			records = CSVFormat.DEFAULT.parse(reader);
		} catch (IOException e) {
			// Vectors file does not exist, generate new vectors file
			try {
				computeAndStoreTreeVectors(SOSnipsFile, SOSnipsVectorsFile);
				
				reader = new FileReader(SOSnipsVectorsFile);
				records = CSVFormat.DEFAULT.parse(reader);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} 
		
		List<CSVRecord> vectorsRecords = new LinkedList<>();
		records.forEach(vectorsRecords::add);
		
		IdsAndVectors = new LinkedList<>();
		
		for(CSVRecord vectorRecord : vectorsRecords) {
			long codeBlockId = Long.parseUnsignedLong(vectorRecord.get(0));
    		String codeVectorStr = vectorRecord.get(1);
    		
    		int[] codeVector = 
    				Arrays.stream(codeVectorStr.split(","))
    				.mapToInt(c -> Integer.parseInt(c.trim()))
    				.toArray();
    		
    		IdsAndVectors.add(new Pair<Long, int[]>(codeBlockId, codeVector));
		}
		//return records;
	}
	
	
	/**
     * Attempts to parse the snippet according to each one of the types in kindsOfParsing. 
     * If successful, returns an AST representation of the snippet along with the kind of parsing that produced it.
     * Produces null and -1 if parsing failed.
     * 
     * 
     * @param codeSnip	The Java code snippet
     * @param kindsOfParsing	An array of integers representing the kinds of parsing 
     * @return		A pair of the snippet AST and an integer representing the kind of parsing that produced it
     */
    private Pair<ASTNode, Integer> getCodeSnipTypedAST(String codeSnip) {
    	for(int kind : kindsOfParsing) {
    		ASTParser parser = ASTParser.newParser(AST.JLS8);
    		parser.setKind(kind);
    		parser.setSource(codeSnip.toCharArray());
    		
    		ASTNode codeSnipAST;
    		try {
    			codeSnipAST = parser.createAST(null);
    		} catch(IllegalArgumentException e) {
    			System.out.println("Error parsing code snip: ");
    			System.out.println(codeSnip);
    			continue;
    		}
    		
    		if(isASTValid(codeSnipAST)) {
    			return new Pair<ASTNode, Integer>(codeSnipAST, kind);
    		} 
    	}
		return new Pair<ASTNode, Integer>(null, -1);
    }
    
    
    /**
     * Implement some heuristics to check if parsing was successful. 
     * @param node	the ASTNode that has been parsed, possibly by ASTParser.createAST().
     * 
     * @return True if AST is valid
     */
    private boolean isASTValid(ASTNode node) {
    	List props = node.structuralPropertiesForType();
		for(Object prop : props) {
			StructuralPropertyDescriptor structPropKey = (StructuralPropertyDescriptor) prop;
			
			Object structPropVal =  node.getStructuralProperty(structPropKey);
			
			if(structPropVal == null) {
				continue;
			} 
			
			if (structPropVal instanceof List) {
				if(((List) structPropVal).size() == 0) {
					continue;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
		
		return false;
    }

    
    
    /*
     * Compute vector of counts of nodes types.
     */
    private int[] computeTreeVector(ASTNode root) {
    	
    	
    	int[] treeVector = new int[92];
    	
    	root.accept(new ASTVisitor() {
        	public void preVisit(ASTNode node) {
        		treeVector[node.getNodeType()-1]++;
        		
     
        	}
        });
    	
    	return treeVector;
    }
    
    
    /*
     * Load snippets from sourceFile, compute their vectors, then store the vectors in vectorFile.
     */
    private void computeAndStoreTreeVectors(String sourceFile, String vectorsFile) throws IOException {
    	Reader reader = new FileReader(sourceFile);
    	Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
    	
    	Writer writer = new FileWriter(vectorsFile);
    	
    	System.out.println("Going through snippets to compute vectors ..");
    	
    	long startTime = System.nanoTime();
    	long snipCount = 0;
    	long parsedSnipCount = 0;
    	for(CSVRecord record : records) {
    		long codeBlockId = Long.parseUnsignedLong(record.get("CodeBlockId"));
    		//String answerId = record.get("AnswerId");
    		String codeSnip = record.get("Content");
    		
    		System.out.println("Parsing snippet no: " + snipCount);
    		
    		// Get code snip AST and parse kind
    		Pair<ASTNode, Integer> codeSnipTypedAST = getCodeSnipTypedAST(codeSnip);
    		
    		
    		if(codeSnipTypedAST.Right != -1) {
    			parsedSnipCount++;
    			
    			// Get tree vector of valid ASTs
    			int[] codeSnipTreeVec = computeTreeVector(codeSnipTypedAST.Left);
    			
    			String codeSnipVecStr = Arrays.toString(codeSnipTreeVec);
    			
    			// [1, 2, 3, 4] -> 1, 2, 3, 4
    			CSVFormat.DEFAULT.printRecord(writer, codeBlockId, 
    					codeSnipVecStr.substring(1, codeSnipVecStr.length() - 1));
    		} else {
    			System.out.println("Skipping snippet ID: " + codeBlockId);
    		}
    		
    		snipCount++;
    	}
    	writer.close();
    	long endTime = System.nanoTime();
    	long elapsedTime = endTime - startTime;
    	
    	System.out.println("SO Snippets processing done.");
    	System.out.println("Total snippets: " + snipCount);
    	System.out.println("Successfully parsed: " + parsedSnipCount);
    	System.out.println("Total processing time: " + elapsedTime/Math.pow(10,9) + " seconds");
    	System.out.println("Avg processing time per snippet: " 
    	+ elapsedTime/(snipCount * Math.pow(10, 3)) + "ms" );
    }
    
    
    
    private double computeEuclideanDistance(int[] vec1, int[] vec2) {
    	assert(vec1.length == vec2.length);
    	
    	long sumOfSquares = 0;
    	for(int i = 0; i < vec1.length; i++) {
    		int diff = vec1[i] - vec2[i];
    		sumOfSquares += diff * diff;
    	}
    	
    	return Math.sqrt(sumOfSquares);
    }
    
    private double computeCosineDistance(int[] vec1, int[] vec2) {
    	assert(vec1.length == vec2.length);
    	
    	int dotProductResult = 0;
    	double sumOfSquaresVec1 = 0, sumOfSquaresVec2 = 0;
    	
    	for(int i = 0; i < vec1.length; i++) {
    		dotProductResult += vec1[i] * vec2[i];
    		
    		sumOfSquaresVec1 += vec1[i] * vec1[i];
    		sumOfSquaresVec2 += vec2[i] * vec2[i];
    	}
    	
    	return dotProductResult/(Math.sqrt(sumOfSquaresVec1) * Math.sqrt(sumOfSquaresVec2));
    }
    
    
    
    
    
	
}
