package sproc.processor;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class Evaluation {
	
	private String codeSnipsFile;
	private String vectorsFile;
	
	private int noOfTests;
	private int k;
	
	int truePositives;
	
	//private String ASTsFile;
	private int totalNoOfSnips;
	private Recommender.Distance typeOfDistance;
	//private boolean pruneSnips;
	
	private long elapsedTime;
	
	public enum Pruning { NONE, LINES, IDENTIFIERS, STATEMENTS };
	
	private Pruning typeOfPruning;
	
	public Evaluation(String codesFile, String vectorsFile, int totalNoOfSnips,
			int noOfTests, int k, Recommender.Distance typeOfDistance, Pruning typeOfPruning) {
		this.codeSnipsFile = codesFile;
		this.vectorsFile = vectorsFile;
		this.totalNoOfSnips = totalNoOfSnips;
		
		this.noOfTests = noOfTests;
		this.k = k;
		this.typeOfDistance = typeOfDistance;
		
		this.typeOfPruning = typeOfPruning;
		
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
			
			ASTNode alteredAST = null;
			
			// TODO randomely prune each snippet
			switch(typeOfPruning) {
				case LINES:
					// Need more work. For example, number of lines could exceed the snip .. etc
					codeSnip = pruneLines(codeSnip, 3);
					break;
				case IDENTIFIERS:
					alteredAST = alterIdentifiers(codeSnip, 5);
					break;
				case STATEMENTS:
					alteredAST = removeStatements(codeSnip, 3);
				case NONE:
					break;
			}
			
			//System.out.println("Executing test no. " + count);
			//long startTime = System.nanoTime();
			
			Recommender recommender = new Recommender(codeSnipsFile, vectorsFile, codeSnip, alteredAST);
			
			
			List<Pair<Long, Double>> recommendedSnips = recommender.getTopSnips(k, typeOfDistance);
			
			//long endTime = System.nanoTime();
	    	//long elapsedTime = endTime - startTime;
	    	
	    	//System.out.println("Test no: " + count + ". Processing time: " + elapsedTime/(Math.pow(10, 9)) + " seconds");
			
			long codeBlockId = Long.parseUnsignedLong(codeRecord.get("CodeBlockId"));
			
			if(recommendedSnips.stream().filter(pair -> pair.Left == codeBlockId).findFirst().isPresent()) {
				truePositives++;
			}
			
			count++;
			// might not need this
			//recommenders.add(recommender);
		}
		long endTime = System.nanoTime();
    	elapsedTime = endTime - startTime;
		
		
		
		// parse code snips to ASTs then to vectors
	}
	
	public void printResults() {
		System.out.println("=== === === === ===");
		String distanceMeasure = typeOfDistance == typeOfDistance.EUCLIDEAN ? "Euclidean" : "Cosine";
		System.out.println("Distance measure: " + distanceMeasure);
		System.out.println("No of tests: " + noOfTests);
		System.out.println("Randomely chosen from: " + totalNoOfSnips + " code snippets");
		System.out.println("Pruning: " + typeOfPruning.name());
		System.out.println("Correct predictions @" + k + ": " + truePositives);
		double precision = (truePositives/(double) noOfTests) * 100;
		System.out.println("Precision @" + k + ": " + precision + "%");
		System.out.println("Total time: " + elapsedTime/Math.pow(10, 9) + " seconds");
		System.out.println("=== === === === ===");
	}
	
	
	private ASTNode alterIdentifiers(String snip, int noOfAlterations) {
		//Document snipDoc = new Document(snip);
		
		Pair<ASTNode, Integer> typedAST = getCodeSnipTypedAST(snip);
		
		//System.out.println(typedAST.Right);
		
		
		ASTNode node = typedAST.Left;
		//ASTRewrite rewriter = ASTRewrite.create(node.getAST());
		//cu.recordModifications();
		
		//List<StructuralPropertyDescriptor> props = cu.structuralPropertiesForType();
		
		int count = 0; 
		Pair<Integer, Integer> alterCount = new Pair<>(count, noOfAlterations); // because count cannot be accessed directly
		node.accept(new ASTVisitor() {
        	public void preVisit(ASTNode node) {
        		List<StructuralPropertyDescriptor> props = node.structuralPropertiesForType();

        		props.forEach(prop -> {
        			if(prop.getId() == "identifier") {
        				if(alterCount.Left < alterCount.Right) {
        					//rewriter.set(node, prop, "_", null);
        					node.setStructuralProperty(prop, "_");
        					
        					alterCount.Left++;
        				}
        			}
        			});
        	}
        });
		
		return node;
		
		/*TextEdit edits = rewriter.rewriteAST(snipDoc,null);
		try {
			edits.apply(snipDoc);
			snip = snipDoc.get();
			System.out.println(snip);
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		
	}
	
	
	private ASTNode removeStatements(String snip, int noOfStatements) {
		Pair<ASTNode, Integer> typedAST = getCodeSnipTypedAST(snip);
		ASTNode node = typedAST.Left;
		
		int count = 0; 
		Pair<Integer, Integer> removeCount = new Pair<>(count, noOfStatements);
		node.accept(new ASTVisitor() {
        	public void preVisit(ASTNode node) {
        		if(node.getParent() != null && node.getParent().getNodeType() == ASTNode.BLOCK) {
        			if(removeCount.Left < removeCount.Right) {
        				node.delete();
        				removeCount.Left++;
        			}
        		}
        	}
        });
		
		return node;
	}
	
	private String pruneLines(String snip, int noOfLines) {
		System.out.println(snip);
		
		String[] lines = snip.split("\n");
		
		
		
		int firstLineIndex = ThreadLocalRandom.current().nextInt(lines.length - noOfLines + 1);
		
		List<String> selectedLines = Arrays.asList(lines);
		
		selectedLines = selectedLines.subList(firstLineIndex, firstLineIndex + noOfLines);
		
		String prunedSnip = String.join("\n", selectedLines);
		
		System.out.println("========");
		System.out.println(prunedSnip);
		
		
		System.exit(0);

		return snip;
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
		//List<Integer> randomIndicesCopy = new LinkedList<>(randomIndices);
		
		
		Iterable<CSVRecord> codes = loadCSVDataFrom(codeSnipsFile, true);
		Iterable<CSVRecord> vectors = loadCSVDataFrom(vectorsFile, false);
		
		//List<CSVRecord> sampledCodes = new LinkedList<CSVRecord>();
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

	
	private Pair<ASTNode, Integer> getCodeSnipTypedAST(String codeSnip) {
		int[] kindsOfParsing = {
				ASTParser.K_COMPILATION_UNIT, 
    			ASTParser.K_CLASS_BODY_DECLARATIONS, 
    			ASTParser.K_STATEMENTS,
    			ASTParser.K_EXPRESSION	
		};
		
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
}
