package sproc.processor;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

public class Utils {

	public static List<Pair<Long, int[]>> loadVectorsFromCSV(String SOSnipsVectorsFile) {
		// Check if vector file exists
		Reader reader = null;
		Iterable<CSVRecord> records = null;

		try {
			reader = new FileReader(SOSnipsVectorsFile);
			records = CSVFormat.DEFAULT.parse(reader);
		} catch (IOException e) {
			// Vectors file does not exist
			// TODO Abort

		}

		List<CSVRecord> vectorsRecords = new LinkedList<>();
		records.forEach(vectorsRecords::add);

		List<Pair<Long, int[]>> IdsAndVectors = new LinkedList<>();

		for (CSVRecord vectorRecord : vectorsRecords) {
			long codeBlockId = Long.parseUnsignedLong(vectorRecord.get(0));
			String codeVectorStr = vectorRecord.get(1);

			int[] codeVector = Arrays.stream(codeVectorStr.split(",")).mapToInt(c -> Integer.parseInt(c.trim()))
					.toArray();

			IdsAndVectors.add(new Pair<Long, int[]>(codeBlockId, codeVector));
		}
		
		 return IdsAndVectors;
	}
	
	/**
	 * Implement some heuristics to check if parsing was successful.
	 * 
	 * @param node the ASTNode that has been parsed, possibly by
	 *             ASTParser.createAST().
	 * 
	 * @return True if AST is valid
	 */
	public static boolean isASTValidHeuristics(ASTNode node) {
		
		List props = node.structuralPropertiesForType();
		for (Object prop : props) {
			StructuralPropertyDescriptor structPropKey = (StructuralPropertyDescriptor) prop;

			Object structPropVal = node.getStructuralProperty(structPropKey);

			if (structPropVal == null) {
				continue;
			}

			if (structPropVal instanceof List) {
				if (((List) structPropVal).size() == 0) {
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
	
	public static boolean isASTValidFlags(ASTNode node) {
		int isInvalid = node.getFlags() & (ASTNode.MALFORMED | ASTNode.RECOVERED);
		
		return (isInvalid == 0);
	}

	/**
	 * Attempts to parse the snippet according to each one of the types in
	 * kindsOfParsing. If successful, returns an AST representation of the snippet
	 * along with the kind of parsing that produced it. Produces null and -1 if
	 * parsing failed.
	 * 
	 * 
	 * @param codeSnip       The Java code snippet
	 * @param kindsOfParsing An array of integers representing the kinds of parsing
	 * @return A pair of the snippet AST and an integer representing the kind of
	 *         parsing that produced it
	 */
	public static Pair<ASTNode, Integer> getCodeSnipTypedAST(String codeSnip) {
		int[] kindsOfParsing = new int[] { ASTParser.K_COMPILATION_UNIT, ASTParser.K_CLASS_BODY_DECLARATIONS,
				ASTParser.K_STATEMENTS, ASTParser.K_EXPRESSION };

		for (int kind : kindsOfParsing) {
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			
			//parser.setBindingsRecovery(false);
	        //parser.setStatementsRecovery(false);
			
			//Map options = JavaCore.getOptions();
			 //JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
			 //parser.setCompilerOptions(options);
			
			parser.setKind(kind);
			parser.setSource(codeSnip.toCharArray());

			ASTNode codeSnipAST;
			try {
				codeSnipAST = parser.createAST(null);
			} catch (IllegalArgumentException e) {
				System.out.println("Error parsing code snip: ");
				System.out.println(codeSnip);
				continue;
			}

			if (isASTValidFlags(codeSnipAST) && isASTValidHeuristics(codeSnipAST)) {
				return new Pair<ASTNode, Integer>(codeSnipAST, kind);
			}
		}
		return new Pair<ASTNode, Integer>(null, -1);
	}

	/*
	 * Compute vector of counts of nodes types.
	 */
	public static int[] computeTreeVector(ASTNode root) {

		int[] treeVector = new int[92];

		root.accept(new ASTVisitor() {
			public void preVisit(ASTNode node) {
				treeVector[node.getNodeType() - 1]++;

			}
		});

		return treeVector;
	}

	
	// Sample a random k code snips and their vectors
	/*
	public static List<Pair<CSVRecord, CSVRecord>> getRandomSnipsAndVecs(int numOfSamples, int populationSize) {
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
	*/
	
	public static <T> List<T> selectRandomElements(List<T> list, int n) {
		if(list.size() < n) {
			return null;
		}
		
		Collections.shuffle(list);
		
		List<T> resultList = list.subList(0, n);
		
		return resultList;
	}
	
	public static Iterable<CSVRecord> loadCSVDataFrom(String fileName, boolean isHeaderPresent) {
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
	
	public static List<CSVRecord> getCodeRecordsByIds(List<Long> codeIds, Iterable<CSVRecord> codeRecords) {
		List<CSVRecord> retrievedCodeRecords = new LinkedList<>();
		codeIds.sort(null);
		
		
		int i = 0;
		//int j = 0;
		for (CSVRecord codeRecord : codeRecords) {
			if (i >= codeIds.size()) {
				break;
			}

			long codeBlockId = Long.parseUnsignedLong(codeRecord.get("CodeBlockId"));

			long currentVectorCodeId = codeIds.get(i);

			if (codeBlockId == currentVectorCodeId) {
				// Add code snip
				retrievedCodeRecords.add(codeRecord);
				i++;
			}
			//j++;
		}
		
		//System.out.println("Gone thru: " + j + " snips");
		//System.out.println("Retrieved: " + i + " snips");
		
		return retrievedCodeRecords;
	}
	
}
