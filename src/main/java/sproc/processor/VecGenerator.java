package sproc.processor;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jdt.core.dom.ASTNode;

public class VecGenerator {
	private String codesFile, vectorsFile;
	private long elapsedTime;
	private long parsedSnipCount;
	private long totalSnipAttempted;
	private Map<Integer, Integer> kindsOfParsingStats;
	
	public VecGenerator(String codesFile, String vectorsFile) {
		this.codesFile = codesFile;
		this.vectorsFile = vectorsFile;
		
		 kindsOfParsingStats = new HashMap<>();
	}
	
	
	public boolean doesVectorFileExist() {
		Reader reader = null;
		try {
			reader = new FileReader(vectorsFile);
			reader.close();
		} catch(IOException e) {
			return false;
		}
		
		return true;
	}
	
	/*
     * Load snippets from sourceFile, compute their vectors, then store the vectors in vectorFile.
     */
	public void computeAndStoreTreeVectors() throws IOException {
		if(doesVectorFileExist()) {
			System.out.println("Vector file with the same name exist. Either delete the file or choose a different name.");
			return;
		}
		
		
		
    	Reader reader = new FileReader(codesFile);
    	Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
    	
    	Writer writer = new FileWriter(vectorsFile);
    	
    	System.out.println("Going through snippets to compute vectors ..");
    	
    	long startTime = System.nanoTime();
    	totalSnipAttempted = 0;
    	parsedSnipCount = 0;
    	for(CSVRecord record : records) {
    		long codeBlockId = Long.parseUnsignedLong(record.get("CodeBlockId"));
    		//String answerId = record.get("AnswerId");
    		String codeSnip = record.get("Content");
    		
    		System.out.print("Parsing snippet no: " + totalSnipAttempted + ", ");
    		
    		// Get code snip AST and parse kind
    		Pair<ASTNode, Integer> codeSnipTypedAST = Utils.getCodeSnipTypedAST(codeSnip);
    		
    		kindsOfParsingStats.merge(codeSnipTypedAST.Right, 1, Integer::sum);
    		
    		//System.out.println(", kind: " + codeSnipTypedAST.Right);
    		
    		if(codeSnipTypedAST.Right != -1) {
    			parsedSnipCount++;
    			
    			// Get tree vector of valid ASTs
    			int[] codeSnipTreeVec = Utils.computeTreeVector(codeSnipTypedAST.Left);
    			
    			String codeSnipVecStr = Arrays.toString(codeSnipTreeVec);
    			
    			// [1, 2, 3, 4] -> 1, 2, 3, 4
    			CSVFormat.DEFAULT.printRecord(writer, codeBlockId, 
    					codeSnipVecStr.substring(1, codeSnipVecStr.length() - 1));
    			
    			System.out.println("");
    		} else {
    			System.out.println("Skipping snippet ID: " + codeBlockId);
    		}
    		
    		totalSnipAttempted++;
    	}
    	writer.close();
    	long endTime = System.nanoTime();
    	elapsedTime = endTime - startTime;
    	
    	
    }
	
	
	public void printResults() {
    	System.out.println("Total snippets: " + totalSnipAttempted);
    	System.out.println("Successfully parsed: " + parsedSnipCount);
    	System.out.println("Kinds of parsing stats: " + kindsOfParsingStats);
    	System.out.println("Total processing time: " + elapsedTime/Math.pow(10,9) + " seconds");
    	System.out.println("Avg processing time per snippet: " 
    	+ elapsedTime/(totalSnipAttempted * Math.pow(10, 6)) + "ms" );
	}
	
	
	/*
     * Compute vector of counts of nodes types.
     */
    
	
	
}
