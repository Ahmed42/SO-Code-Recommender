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
	// private int k;

	// int truePositives;

	// private String ASTsFile;
	private int totalNoOfSnips;
	// private Recommender.Distance typeOfDistance;
	// private boolean pruneSnips;

	// private long elapsedTime;
	// private List<Pair<Long, int[]>> codeIdsAndVectors;
	private List<Pair<Long, int[]>> selectedCodeIdsAndVectors; // Total snips
	private List<Pair<Long, int[]>> randomCodeIdsAndVectors; // Snips vectors to be tested

	private List<CSVRecord> randomCodeRecords; // Snips to be tested

	public enum Pruning {
		NONE, LINES, IDENTIFIERS, STATEMENTS
	};

	// private Pruning typeOfPruning;

	public Evaluation(String codesFile, String vectorsFile, int totalNoOfSnips, int noOfTests) {
		long startTime = System.nanoTime();
		
		this.codeSnipsFile = codesFile;
		this.vectorsFile = vectorsFile;

		List<Pair<Long, int[]>> codeIdsAndVectors = Utils.loadVectorsFromCSV(vectorsFile);
		
		long endTime = System.nanoTime();
		long elapsedTime = endTime - startTime;
		System.out.println("Loading vectors time: " + elapsedTime / Math.pow(10, 9) + " seconds");
		
		
		startTime = System.nanoTime();
		selectedCodeIdsAndVectors = codeIdsAndVectors.subList(0, totalNoOfSnips);

		randomCodeIdsAndVectors = Utils.selectRandomElements(selectedCodeIdsAndVectors, noOfTests);
		randomCodeIdsAndVectors.sort(Comparator.comparingDouble(IdAndVector -> IdAndVector.Left));

		this.totalNoOfSnips = totalNoOfSnips;

		this.noOfTests = noOfTests;

		Iterable<CSVRecord> codeRecords = Utils.loadCSVDataFrom(codeSnipsFile, true);

		// Get matching code snips. Code records to be evaluated
		randomCodeRecords = new LinkedList<>();

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
		
		endTime = System.nanoTime();
		elapsedTime = endTime - startTime;

		System.out.println("Selecting random test snips time: " + elapsedTime / Math.pow(10, 9) + " seconds");
		/*
		 * this.k = k; this.typeOfDistance = typeOfDistance;
		 * 
		 * this.typeOfPruning = typeOfPruning;
		 * 
		 * this.truePositives = 0;
		 */
		
		
	}

	// params: number of code snips to evaluate
	// value of k in precision/recall@k
	// TODO number of leaf nodes to prune

	public Map<Integer, Integer> evaluate(List<Integer> ks, Recommender.Distance typeOfDistance,
			Map<Evaluation.Pruning, Integer> pruningConfig) {
		// Load vectors from file

		// Get random sample from code IDs and vectors to test
		// List<Pair<Long, int[]>> randomCodeIdsAndVectors =
		// Utils.selectRandomElements(selectedCodeIdsAndVectors, noOfTests);

		// Pair<Long, int[]> currentCodeIdAndVector = randomCodeIdsAndVectors
		// Sort by CodeBlockID

		long startTime = System.nanoTime();
		Recommender recommender = new Recommender(selectedCodeIdsAndVectors);

		Map<Integer, Integer> KsAndTPs = new HashMap<>();
		// Iterate over each test case

		double sumOfReciprocalRanks = 0;
		for (CSVRecord codeRecord : randomCodeRecords) {

			String queryCode = codeRecord.get("Content");

			ASTNode alteredAST = null;

			for (Pruning typeOfPruning : pruningConfig.keySet()) {
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
			
			long codeBlockId = Long.parseUnsignedLong(codeRecord.get("CodeBlockId"));

			List<Pair<Long, Double>> similarCodeIdsAndDistances = recommender.getSimilarSnips(typeOfDistance, queryCode,
					alteredAST);
			
			Pair<Long, Double> originalPair = similarCodeIdsAndDistances.stream().filter(pair -> pair.Left == codeBlockId).findFirst().get();
			
			int resultRank = similarCodeIdsAndDistances.indexOf(originalPair);
			
			
			double reciprocalRank = resultRank == -1? 0 : 1/(double) (resultRank + 1);
			
			sumOfReciprocalRanks += reciprocalRank;
			
			// System.out.println(codeBlockId + ":" +
			// similarCodeIdsAndDistances.get(0).Left);

			for (int k : ks) {
				if (similarCodeIdsAndDistances.subList(0, k).stream().filter(pair -> pair.Left == codeBlockId)
						.findFirst().isPresent()) {
					// int tp = KsAndTPs.get(k);
					// KsAndTPs.put(k, tp + 1);
					boolean inspectMode = false;

					// Display the recommended snips and their distances
					if (inspectMode) {
						List<Long> kCodeIds = new LinkedList<>();

						similarCodeIdsAndDistances.subList(0, k).forEach(pair -> kCodeIds.add(pair.Left));

						Iterable<CSVRecord> codeRecords = Utils.loadCSVDataFrom(codeSnipsFile, true);
						List<CSVRecord> similarCodeRecords = Utils.getCodeRecordsByIds(kCodeIds, codeRecords);

						System.out.println("Query Code: \n" + queryCode);

						for (int j = 0; j < k; j++) {
							Pair<Long, Double> IdAndDist = similarCodeIdsAndDistances.get(j);
							Long codeId = IdAndDist.Left;

							String currCodeSnip = similarCodeRecords.stream()
									.filter(record -> Long.parseUnsignedLong(record.get("CodeBlockId")) == codeId)
									.findFirst().get().get("Content");

							System.out.println("Rank: " + j);
							System.out.println("CodeBlockID: " + codeId);
							if (codeBlockId == codeId) {
								System.out.println("[ORIGNAL SNIPPET]");
							}
							System.out.println("Code: \n" + currCodeSnip);
							System.out.println("-----------");
						}

						System.out.println("============");
					}

					KsAndTPs.merge(k, 1, Integer::sum);
					// truePositives++;
				}

			}

		}
		
		double meanReciprocalRank = sumOfReciprocalRanks/noOfTests;

		long endTime = System.nanoTime();
		long elapsedTime = endTime - startTime;

		System.out.println("Total time: " + elapsedTime / Math.pow(10, 9) + " seconds");
		System.out.println("MRR: " + meanReciprocalRank);

		/*
		 * double precision = (truePositives / (double) noOfTests) * 100;
		 * System.out.println("Total time: " + elapsedTime / Math.pow(10, 9) + "
		 * seconds");
		 */

		// System.out.println("=== === === === ===");
		/*
		 * for(int k : KsAndTPs.keySet()) { int truePositives = KsAndTPs.get(k); double
		 * precision = (truePositives/(double) noOfTests) * 100;
		 * 
		 * System.out.println("Distance measure: " + typeOfDistance.name());
		 * System.out.println("No of tests: " + noOfTests);
		 * System.out.println("Randomely chosen from: " + totalNoOfSnips +
		 * " code snippets"); System.out.println("Pruning: " + pruningConfig);
		 * System.out.println("Correct predictions @" + k + ": " + truePositives);
		 * 
		 * System.out.println("Precision @" + k + ": " + precision + "%");
		 * System.out.println("Total time: " + elapsedTime / Math.pow(10, 9) +
		 * " seconds"); System.out.println("=== === === === ==="); }
		 */

		return KsAndTPs;
	}

	/*
	 * public void printResults() { System.out.println("=== === === === ===");
	 * String distanceMeasure = typeOfDistance == Recommender.Distance.EUCLIDEAN ?
	 * "Euclidean" : "Cosine"; System.out.println("Distance measure: " +
	 * distanceMeasure); System.out.println("No of tests: " + noOfTests);
	 * System.out.println("Randomely chosen from: " + totalNoOfSnips +
	 * " code snippets"); System.out.println("Pruning: " + typeOfPruning.name());
	 * System.out.println("Correct predictions @" + k + ": " + truePositives);
	 * double precision = (truePositives / (double) noOfTests) * 100;
	 * System.out.println("Precision @" + k + ": " + precision + "%");
	 * System.out.println("Total time: " + elapsedTime / Math.pow(10, 9) +
	 * " seconds"); System.out.println("=== === === === ==="); }
	 */

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
