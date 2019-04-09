package sproc.processor;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

public class App {
	public static String shortCodeSnipsFile = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\more_java_code_snips.csv";
	public static String highScoreCodeSnipsFile = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\java_snips_score_10.csv";

	public static String vectorsFileShort = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\snips_vectors_short.csv";
	public static String vectorsFileHigh = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\snips_vectors_high_score.csv";

	public static void main(String[] args) throws IOException, IllegalArgumentException, IllegalAccessException {

		// ASTRewrite rewriter = ASTRewrite.create(node.getAST());
		// cu.recordModifications();

		// List<StructuralPropertyDescriptor> props = cu.structuralPropertiesForType();
		
		
		//runEvaluations();
		
		Evaluation evaluation = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, 130000, 500);
		
		evaluation.evaluate(1, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.NONE);
		evaluation.evaluate(1, Recommender.Distance.COSINE, Evaluation.Pruning.NONE);
		evaluation.evaluate(10, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.NONE);
		//evaluation.evaluate(1, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.IDENTIFIERS);
		evaluation.evaluate(1, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.STATEMENTS);
		evaluation.evaluate(1, Recommender.Distance.COSINE, Evaluation.Pruning.STATEMENTS);
		evaluation.evaluate(10, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.STATEMENTS);
		
		

		System.out.println("Done!");
	}
	
	public static void tryingToGetNonParsableCode() {
		String queryCode1 = "class Test { public void foo() { int x = 1 + 2; System.out.println(\"haha\"); } "
				+ "public boolean bar() { return false; }}"
				+ "class Test2 { public String foobar() { return \"abort\"; }}";
		
		String queryCode = "class heh {void method() { int x = 1; y = x; System.out.println(\"haha\");}}";
		
		Pair<ASTNode, Integer> pair = getCodeSnipTypedAST(queryCode);
		
		int isInvalid = pair.Left.getFlags() & (ASTNode.MALFORMED | ASTNode.RECOVERED);
		
		System.out.println(isInvalid);
		
		pair.Left.accept(new ASTVisitor() {
			public void preVisit(ASTNode node) {
				System.out.println("===");
        		System.out.println(node.getClass() + " : " + node.getNodeType() + " : " + node);
        		
     
        	}
		} );
	}
	/***
	public static void useCase() {
		String queryCode1 = "class Test { public void foo() { int x = 1 + 2; System.out.println(\"haha\"); } "
				+ "public boolean bar() { return false; }}"
				+ "class Test2 { public String foobar() { return \"abort\"; }}";

		String queryCode3 = "public int ixAdd() {" + "return _ix++ + giveMeZero();" + "}";

		String queryCode2 = "    while(m.find()) {\r\n"
				+ "    	System.out.println(s.substring(m.start(), m.end()));\r\n" + "    }";
		Recommender recommender = new Recommender(highScoreCodeSnipsFile, vectorsFileHigh, queryCode3, null);

		List<Pair<Long, Double>> IdsAndDistance = recommender.getTopSnips(10, Recommender.Distance.EUCLIDEAN);
		int[] vector = recommender.getQueryCodeVector();

		System.out.println(vector.length);

		for (int elem : vector) {
			System.out.print(elem + ", ");
		}

		System.out.println("\n");

		Reader reader = null;
		Iterable<CSVRecord> records = null;

		try {
			reader = new FileReader(highScoreCodeSnipsFile);
			records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);

		} catch (IOException e) {
		}

		List<String> recommendedSnips = new LinkedList<>();

		for (CSVRecord record : records) {
			long currentVectorCodeId = Long.parseUnsignedLong(record.get("CodeBlockId"));

			if (IdsAndDistance.stream().anyMatch(pair -> pair.Left == currentVectorCodeId)) {
				String code = record.get("Content");
				recommendedSnips.add(code);
			}
		}

		recommendedSnips.forEach(snip -> {
			System.out.println(snip);
			System.out.println("============");
		});

		System.out.println("=========");
		System.out.println("Distances: ");
		IdsAndDistance.forEach(idAndDist -> System.out.println(idAndDist.Right));

		double median = IdsAndDistance.get(IdsAndDistance.size() / 2).Right;

		double avg = IdsAndDistance.stream().mapToDouble(a -> a.Right).average().getAsDouble();

		System.out.println("median: " + median);
		System.out.println("avg: " + avg);
	}

	public static void runEvaluations() {
		int noOfTests = 500;
		int totalNumOfSnippets = 133870;
		int k;

		// k = 10
		k = 10;
		// Distance = Euclidean

		// Pruning = None
		Evaluation evaluation1 = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, totalNumOfSnippets, noOfTests,
				k, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.NONE);

		evaluation1.evaluate();
		evaluation1.printResults();

		// Pruning = STATEMENTS
		Evaluation evaluation2 = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, totalNumOfSnippets, noOfTests,
				k, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.STATEMENTS);

		evaluation2.evaluate();
		evaluation2.printResults();

		// Pruning = IDENTIFIERS
		Evaluation evaluation3 = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, totalNumOfSnippets, noOfTests,
				k, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.IDENTIFIERS);

		evaluation3.evaluate();
		evaluation3.printResults();

		// Distance = Cosine

		// k = 5
		k = 5;
		// Distance = Euclidean

		// Pruning = None
		Evaluation evaluation4 = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, totalNumOfSnippets, noOfTests,
				k, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.NONE);

		evaluation4.evaluate();
		evaluation4.printResults();

		// Pruning = STATEMENTS
		Evaluation evaluation5 = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, totalNumOfSnippets, noOfTests,
				k, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.STATEMENTS);

		evaluation5.evaluate();
		evaluation5.printResults();

		// Pruning = IDENTIFIERS
		Evaluation evaluation6 = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, totalNumOfSnippets, noOfTests,
				k, Recommender.Distance.EUCLIDEAN, Evaluation.Pruning.IDENTIFIERS);

		evaluation6.evaluate();
		evaluation6.printResults();

	}
*/
	public static void pastEvals() {
		/*
		 * Evaluation evaluation8 = new Evaluation(highScoreCodeSnipsFile,
		 * vectorsFileHigh, 133870, 5, 100, Recommender.Distance.EUCLIDEAN,
		 * Evaluation.Pruning.IDENTIFIERS); evaluation8.evaluate();
		 * evaluation8.printResults();
		 * 
		 * Evaluation evaluation9 = new Evaluation(highScoreCodeSnipsFile,
		 * vectorsFileHigh, 133870, 5, 100, Recommender.Distance.EUCLIDEAN,
		 * Evaluation.Pruning.NONE); evaluation9.evaluate(); evaluation9.printResults();
		 */

		/*
		 * Evaluation evaluation1 = new Evaluation(shortCodeSnipsFile, vectorsFileShort,
		 * 6700, 50, 10, Recommender.Distance.EUCLIDEAN); evaluation1.evaluate();
		 * evaluation1.printResults();
		 * 
		 * Evaluation evaluation2 = new Evaluation(shortCodeSnipsFile, vectorsFileShort,
		 * 6700, 100, 5, Recommender.Distance.EUCLIDEAN); evaluation2.evaluate();
		 * evaluation2.printResults();
		 * 
		 * Evaluation evaluation3 = new Evaluation(shortCodeSnipsFile, vectorsFileShort,
		 * 6700, 100, 5, Recommender.Distance.COSINE); evaluation3.evaluate();
		 * evaluation3.printResults();
		 * 
		 * Evaluation evaluation4 = new Evaluation(shortCodeSnipsFile, vectorsFileShort,
		 * 6700, 1000, 5, Recommender.Distance.EUCLIDEAN); evaluation4.evaluate();
		 * evaluation4.printResults();
		 * 
		 * Evaluation evaluation5 = new Evaluation(shortCodeSnipsFile, vectorsFileShort,
		 * 6700, 1000, 10, Recommender.Distance.EUCLIDEAN); evaluation5.evaluate();
		 * evaluation5.printResults();
		 * 
		 * Evaluation evaluation6 = new Evaluation(shortCodeSnipsFile, vectorsFileShort,
		 * 6700, 1000, 5, Recommender.Distance.COSINE); evaluation6.evaluate();
		 * evaluation6.printResults();
		 * 
		 * Evaluation evaluation7 = new Evaluation(shortCodeSnipsFile, vectorsFileShort,
		 * 6700, 1000, 10, Recommender.Distance.COSINE); evaluation7.evaluate();
		 * evaluation7.printResults();
		 */

// 133870

		/*
		 * Evaluation evaluation8 = new Evaluation(highScoreCodeSnipsFile,
		 * vectorsFileHigh, 133870, 1000, 5, Recommender.Distance.EUCLIDEAN, true);
		 * evaluation8.evaluate(); evaluation8.printResults();
		 * 
		 * Evaluation evaluation9 = new Evaluation(highScoreCodeSnipsFile,
		 * vectorsFileHigh, 133870, 1000, 10, Recommender.Distance.EUCLIDEAN, false);
		 * evaluation9.evaluate(); evaluation9.printResults();
		 * 
		 * Evaluation evaluation10 = new Evaluation(highScoreCodeSnipsFile,
		 * vectorsFileHigh, 133870, 1000, 5, Recommender.Distance.COSINE, false);
		 * evaluation10.evaluate(); evaluation10.printResults();
		 * 
		 * Evaluation evaluation11 = new Evaluation(highScoreCodeSnipsFile,
		 * vectorsFileHigh, 133870, 1000, 10, Recommender.Distance.COSINE, false);
		 * evaluation11.evaluate(); evaluation11.printResults();
		 */
	}

	public static void quickTest() throws IOException {
		String queryCode1 = "class Test { public void foo() { System.out.println(\"haha\"); } "
				+ "public boolean bar() { return false; }}"
				+ "class Test2 { public String foobar() { return \"abort\"; }}";

		String queryCode2 = "obj.method();";
		String queryCode3 = "int x = 123; String lol = \"rofl\";";
		String queryCode4 = ("public void foo() { System.out.println(\"haha\"); int x = y + z; } "
				+ "public boolean bar() { return false; }");

		String queryCode5 = "int x = 123; String lol = \"rofl\";";
		String queryCode6 = "class Test { public void foo() {  obj.method(); } }";

		/*
		 * Recommender recommender = new Recommender(shortCodeSnipsFile,
		 * vectorsFileShort, queryCode6);
		 * 
		 * List<Pair<Long, Double>> euclideanDists = recommender.getTopSnips(10,
		 * Recommender.Distance.EUCLIDEAN);
		 * 
		 * // Loading code snips Map<Long, String> codeSnips =
		 * getCodeSnips(shortCodeSnipsFile);
		 * 
		 * printSnippetsAndDistances(euclideanDists, codeSnips);
		 */
	}

	public static void printSnippetsAndDistances(List<Pair<Long, Double>> snipsDistances, Map<Long, String> codeSnips) {
		System.out.println("Printing " + snipsDistances.size() + " code snippets: ");
		for (int i = 0; i < snipsDistances.size(); i++) {
			Pair<Long, Double> codeDistPair = snipsDistances.get(i);

			long snipId = codeDistPair.Left;
			double distance = codeDistPair.Right;

			String codeSnip = codeSnips.get(snipId);

			System.out.println("===== Snippet No. " + i + "=====");
			System.out.println("CodeBlockId: " + snipId);
			System.out.println("Distance: " + distance);
			System.out.println(codeSnip);
		}

		System.out.println("==============================\n");
	}

	public static Map<Long, String> getCodeSnips(String codeSnipsFile) throws IOException {
		Map<Long, String> codeSnips = new HashMap<Long, String>();

		Reader snipsReader = new FileReader(codeSnipsFile);
		Iterable<CSVRecord> snipsRecords = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(snipsReader);

		for (CSVRecord snipRecord : snipsRecords) {
			long codeSnipId = Long.parseUnsignedLong(snipRecord.get("CodeBlockId"));
			String codeSnip = snipRecord.get("Content");

			codeSnips.put(codeSnipId, codeSnip);
		}

		return codeSnips;
	}

	private static Pair<ASTNode, Integer> getCodeSnipTypedAST(String codeSnip) {
		int[] kindsOfParsing = { ASTParser.K_COMPILATION_UNIT, ASTParser.K_CLASS_BODY_DECLARATIONS,
				ASTParser.K_STATEMENTS, ASTParser.K_EXPRESSION };

		for (int kind : kindsOfParsing) {
			ASTParser parser = ASTParser.newParser(AST.JLS8);
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

			if (isASTValid(codeSnipAST)) {
				return new Pair<ASTNode, Integer>(codeSnipAST, kind);
			}
		}
		return new Pair<ASTNode, Integer>(null, -1);
	}

	private static boolean isASTValid(ASTNode node) {
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
}
