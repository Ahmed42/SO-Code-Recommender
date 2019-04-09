package sproc.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.csv.CSVRecord;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

public class Evaluation {

	private String codeSnipsFile;
	private String vectorsFile;

	private int noOfTests;
	//private int k;

	//int truePositives;

	// private String ASTsFile;
	private int totalNoOfSnips;
	//private Recommender.Distance typeOfDistance;
	// private boolean pruneSnips;

	//private long elapsedTime;
	//private List<Pair<Long, int[]>> codeIdsAndVectors;
	private List<Pair<Long, int[]>> selectedCodeIdsAndVectors;
	private List<Pair<Long, int[]>> randomCodeIdsAndVectors;

	public enum Pruning {
		NONE, LINES, IDENTIFIERS, STATEMENTS
	};

	//private Pruning typeOfPruning;

	public Evaluation(String codesFile, String vectorsFile, int totalNoOfSnips, int noOfTests) {
		this.codeSnipsFile = codesFile;
		this.vectorsFile = vectorsFile;
		
		List<Pair<Long, int[]>> codeIdsAndVectors = Utils.loadVectorsFromCSV(vectorsFile);
		
		selectedCodeIdsAndVectors = codeIdsAndVectors.subList(0, totalNoOfSnips);
		
		randomCodeIdsAndVectors = Utils.selectRandomElements(selectedCodeIdsAndVectors, noOfTests);
		
		this.totalNoOfSnips = totalNoOfSnips;

		this.noOfTests = noOfTests;
		/*this.k = k;
		this.typeOfDistance = typeOfDistance;

		this.typeOfPruning = typeOfPruning;

		this.truePositives = 0;*/
	}

	// params: number of code snips to evaluate
	// value of k in precision/recall@k
	// TODO number of leaf nodes to prune
	
	
	public Map<Integer, Integer> evaluate(List<Integer> ks, Recommender.Distance typeOfDistance, Map<Evaluation.Pruning, Integer> pruningConfig) {
		// Load vectors from file

		// Get random sample from code IDs and vectors to test
		//List<Pair<Long, int[]>> randomCodeIdsAndVectors = Utils.selectRandomElements(selectedCodeIdsAndVectors, noOfTests);

		Iterable<CSVRecord> codeRecords = Utils.loadCSVDataFrom(codeSnipsFile, true);

		// Pair<Long, int[]> currentCodeIdAndVector = randomCodeIdsAndVectors
		// Sort by CodeBlockID
		randomCodeIdsAndVectors.sort(Comparator.comparingDouble(IdAndVector -> IdAndVector.Left));

		// Get matching code snips
		List<CSVRecord> randomCodeRecords = new LinkedList<>();
		
		int i = 0;
		for (CSVRecord codeRecord : codeRecords) {
			if (i >= randomCodeIdsAndVectors.size()) {
				break;
			}

			long codeBlockId = Long.parseUnsignedLong(codeRecord.get("CodeBlockId"));

			Pair<Long, int[]> currentCodeIdAndVector = randomCodeIdsAndVectors.get(i);
			long currentVectorCodeId = currentCodeIdAndVector.Left;

			if (codeBlockId == currentVectorCodeId) {
				// Add code snip
				randomCodeRecords.add(codeRecord);
				i++;
			}

		}

		
		long startTime = System.nanoTime();
		Recommender recommender = new Recommender(selectedCodeIdsAndVectors);
		
		Map<Integer, Integer> KsAndTPs = new HashMap<>();
		// Iterate over each test case
		
		for (CSVRecord codeRecord : randomCodeRecords) {

			String queryCode = codeRecord.get("Content");

			ASTNode alteredAST = null;
			
			for(Pruning typeOfPruning : pruningConfig.keySet()) {
				int pruningValue = pruningConfig.get(typeOfPruning);
				switch (typeOfPruning) {
				case STATEMENTS:
					alteredAST = removeStatements(queryCode, pruningValue);
					break;
				case IDENTIFIERS:
					alteredAST = alterIdentifiers(queryCode, pruningValue);
					break;
				case LINES:
					// Need more work. For example, number of lines could exceed the snip .. etc
					queryCode = pruneLines(queryCode, 3);
					break;
				case NONE:
					break;
				}
			}

			

			List<Pair<Long, Double>> similarCodeIdsAndDistances = 
					recommender.getSimilarSnips(typeOfDistance, queryCode, alteredAST);
			
			
			long codeBlockId = Long.parseUnsignedLong(codeRecord.get("CodeBlockId"));
			//System.out.println(codeBlockId + ":" + similarCodeIdsAndDistances.get(0).Left);

			
			
			for(int k: ks) {
				if (similarCodeIdsAndDistances.subList(0, k).stream().filter(pair -> pair.Left == codeBlockId).findFirst().isPresent()) {
					//int tp = KsAndTPs.get(k);
					//KsAndTPs.put(k, tp + 1);
					
					KsAndTPs.merge(k, 1, Integer::sum);
					//truePositives++;
				}
				
				
			}
			
		}

		long endTime = System.nanoTime();
		long elapsedTime = endTime - startTime;

		//double precision = (truePositives / (double) noOfTests) * 100;
		//System.out.println("Total time: " + elapsedTime / Math.pow(10, 9) + " seconds");
		
		//System.out.println("=== === === === ===");
		/*System.out.println("Distance measure: " + typeOfDistance.name());
		System.out.println("No of tests: " + noOfTests);
		System.out.println("Randomely chosen from: " + totalNoOfSnips + " code snippets");
		System.out.println("Pruning: " + pruningConfig);
		System.out.println("Correct predictions @" + k + ": " + truePositives);
		
		System.out.println("Precision @" + k + ": " + precision + "%");
		System.out.println("Total time: " + elapsedTime / Math.pow(10, 9) + " seconds");
		System.out.println("=== === === === ===");*/
		
		return KsAndTPs;
	}

	/*public void printResults() {
		System.out.println("=== === === === ===");
		String distanceMeasure = typeOfDistance == Recommender.Distance.EUCLIDEAN ? "Euclidean" : "Cosine";
		System.out.println("Distance measure: " + distanceMeasure);
		System.out.println("No of tests: " + noOfTests);
		System.out.println("Randomely chosen from: " + totalNoOfSnips + " code snippets");
		System.out.println("Pruning: " + typeOfPruning.name());
		System.out.println("Correct predictions @" + k + ": " + truePositives);
		double precision = (truePositives / (double) noOfTests) * 100;
		System.out.println("Precision @" + k + ": " + precision + "%");
		System.out.println("Total time: " + elapsedTime / Math.pow(10, 9) + " seconds");
		System.out.println("=== === === === ===");
	}*/

	private ASTNode alterIdentifiers(String snip, int noOfAlterations) {
		// Document snipDoc = new Document(snip);

		Pair<ASTNode, Integer> typedAST = Utils.getCodeSnipTypedAST(snip);

		// System.out.println(typedAST.Right);

		ASTNode node = typedAST.Left;
		// ASTRewrite rewriter = ASTRewrite.create(node.getAST());
		// cu.recordModifications();

		// List<StructuralPropertyDescriptor> props = cu.structuralPropertiesForType();

		int count = 0;
		Pair<Integer, Integer> alterCount = new Pair<>(count, noOfAlterations); // because count cannot be accessed
																				// directly
		node.accept(new ASTVisitor() {
			public void preVisit(ASTNode node) {
				List<StructuralPropertyDescriptor> props = node.structuralPropertiesForType();

				props.forEach(prop -> {
					if (prop.getId() == "identifier") {
						if (alterCount.Left < alterCount.Right) {
							// rewriter.set(node, prop, "_", null);
							node.setStructuralProperty(prop, "_");

							alterCount.Left++;
						}
					}
				});
			}
		});

		return node;

		/*
		 * TextEdit edits = rewriter.rewriteAST(snipDoc,null); try {
		 * edits.apply(snipDoc); snip = snipDoc.get(); System.out.println(snip); } catch
		 * (MalformedTreeException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } catch (BadLocationException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 */

	}

	private ASTNode removeStatements(String snip, int noOfStatements) {
		Pair<ASTNode, Integer> typedAST = Utils.getCodeSnipTypedAST(snip);
		ASTNode node = typedAST.Left;

		int count = 0;
		Pair<Integer, Integer> removeCount = new Pair<>(count, noOfStatements);
		node.accept(new ASTVisitor() {
			public void preVisit(ASTNode node) {
				// we can also do getClass = "org.eclipse.jdt.core.dom.Statement" or something
				if (node.getParent() != null && node.getParent().getNodeType() == ASTNode.BLOCK) {
					if (removeCount.Left < removeCount.Right) {
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

}
