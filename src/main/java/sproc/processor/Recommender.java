package sproc.processor;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

public class Recommender {
	//private String SOSnipsVectorsFile;


	// Load and parse all vectors in memory ONCE!
	// private static List<CSVRecord> IdsAndVectors;
	private List<Pair<Long, int[]>> IdsAndVectors;

	public enum Distance {
		EUCLIDEAN, COSINE
	}

	public Recommender(List<Pair<Long, int[]>> IdsAndVectors) {
		this.IdsAndVectors = IdsAndVectors;

	}

	// Returns the snips CodeBlockIds and their similarity scores
	public List<Pair<Long, Double>> getSimilarSnips(Distance typeOfDistance, String queryCode, ASTNode queryCodeAST) {
		int[] queryCodeVector = computeQueryCodeVector(queryCode, queryCodeAST);

		List<Pair<Long, Double>> distances = computeDistances(queryCodeVector, typeOfDistance);

		if (typeOfDistance == Distance.EUCLIDEAN) {
			distances.sort(Comparator.comparingDouble(distPair -> distPair.Right));
		} else if (typeOfDistance == Distance.COSINE) {
			Comparator<Pair<Long, Double>> comp = Comparator.comparingDouble(distPair -> distPair.Right);
			distances.sort(comp.reversed());
		}

		return distances;
	}

	private int[] computeQueryCodeVector(String queryCode, ASTNode queryCodeAST) {
		if (queryCodeAST == null) {
			Pair<ASTNode, Integer> typedAST = Utils.getCodeSnipTypedAST(queryCode);
			queryCodeAST = typedAST.Left;
		}

		int[] queryCodeVector = null;

		if (queryCodeAST != null) {
			queryCodeVector = Utils.computeTreeVector(queryCodeAST);
		} else {
			System.out.println(queryCode);
			System.out.println("Malformed query code!");
		}

		return queryCodeVector;
	}

	private List<Pair<Long, Double>> computeDistances(int[] queryCodeVector, Distance typeOfDistance) {
		if (typeOfDistance == Distance.EUCLIDEAN) {
			List<Pair<Long, Double>> euclideanDistances = new LinkedList<>();

			for (Pair<Long, int[]> idAndVector : IdsAndVectors) {
				long codeBlockId = idAndVector.Left;
				int[] codeVector = idAndVector.Right;
				double computedDistance = computeEuclideanDistance(queryCodeVector, codeVector);
				euclideanDistances.add(new Pair<Long, Double>(codeBlockId, computedDistance));
			}

			return euclideanDistances;

		} else if (typeOfDistance == Distance.COSINE) {
			List<Pair<Long, Double>> cosineDistances = new LinkedList<>();

			for (Pair<Long, int[]> idAndVector : IdsAndVectors) {
				long codeBlockId = idAndVector.Left;
				int[] codeVector = idAndVector.Right;
				double computedDistance = computeCosineDistance(queryCodeVector, codeVector);
				cosineDistances.add(new Pair<Long, Double>(codeBlockId, computedDistance));
			}

			return cosineDistances;
		} else {
			System.out.println("Invalid distance measure!");
			return null;
		}

	}
	
	private double computeEuclideanDistance(int[] vec1, int[] vec2) {
		assert (vec1.length == vec2.length);

		long sumOfSquares = 0;
		for (int i = 0; i < vec1.length; i++) {
			int diff = vec1[i] - vec2[i];
			sumOfSquares += diff * diff;
		}

		return Math.sqrt(sumOfSquares);
	}

	private double computeCosineDistance(int[] vec1, int[] vec2) {
		assert (vec1.length == vec2.length);

		int dotProductResult = 0;
		double sumOfSquaresVec1 = 0, sumOfSquaresVec2 = 0;

		for (int i = 0; i < vec1.length; i++) {
			dotProductResult += vec1[i] * vec2[i];

			sumOfSquaresVec1 += vec1[i] * vec1[i];
			sumOfSquaresVec2 += vec2[i] * vec2[i];
		}

		return dotProductResult / (Math.sqrt(sumOfSquaresVec1) * Math.sqrt(sumOfSquaresVec2));
	}

}
