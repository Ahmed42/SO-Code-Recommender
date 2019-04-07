package sproc.processor;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


public class App {
	public static String shortCodeSnipsFile = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\more_java_code_snips.csv";
	public static String highScoreCodeSnipsFile = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\java_snips_score_10.csv";

	public static String vectorsFileShort = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\snips_vectors_short.csv";
	public static String vectorsFileHigh = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\snips_vectors_high_score.csv";

	public static void main(String[] args) throws IOException, IllegalArgumentException, IllegalAccessException {
		
		/*Evaluation evaluation = new Evaluation(shortCodeSnipsFile, vectorsFileShort, 6700,
				50, 10, Recommender.Distance.EUCLIDEAN);
		evaluation.evaluate();
		evaluation.printResults();
		
		Evaluation evaluation2 = new Evaluation(shortCodeSnipsFile, vectorsFileShort, 6700,
				100, 5, Recommender.Distance.EUCLIDEAN);
		evaluation2.evaluate();
		evaluation2.printResults();
		
		Evaluation evaluation3 = new Evaluation(shortCodeSnipsFile, vectorsFileShort, 6700,
				100, 5, Recommender.Distance.COSINE);
		evaluation3.evaluate();
		evaluation3.printResults();*/
		
		/*Evaluation evaluation = new Evaluation(shortCodeSnipsFile, vectorsFileShort, 6700,
				1000, 5, Recommender.Distance.EUCLIDEAN);
		evaluation.evaluate();
		evaluation.printResults();
		
		Evaluation evaluation2 = new Evaluation(shortCodeSnipsFile, vectorsFileShort, 6700,
				1000, 10, Recommender.Distance.EUCLIDEAN);
		evaluation2.evaluate();
		evaluation2.printResults();
		
		Evaluation evaluation3 = new Evaluation(shortCodeSnipsFile, vectorsFileShort, 6700,
				1000, 5, Recommender.Distance.COSINE);
		evaluation3.evaluate();
		evaluation3.printResults();
		
		Evaluation evaluation4 = new Evaluation(shortCodeSnipsFile, vectorsFileShort, 6700,
				1000, 10, Recommender.Distance.COSINE);
		evaluation4.evaluate();
		evaluation4.printResults();*/
		
		// 133870
		
		Evaluation evaluation = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, 133870,
				1000, 5, Recommender.Distance.EUCLIDEAN);
		evaluation.evaluate();
		evaluation.printResults();
		
		Evaluation evaluation2 = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, 133870,
				1000, 10, Recommender.Distance.EUCLIDEAN);
		evaluation2.evaluate();
		evaluation2.printResults();
		
		Evaluation evaluation3 = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, 133870,
				1000, 5, Recommender.Distance.COSINE);
		evaluation3.evaluate();
		evaluation3.printResults();
		
		Evaluation evaluation4 = new Evaluation(highScoreCodeSnipsFile, vectorsFileHigh, 133870,
				1000, 10, Recommender.Distance.COSINE);
		evaluation4.evaluate();
		evaluation4.printResults();
		
		System.out.println("Done!");
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

		Recommender recommender = new Recommender(shortCodeSnipsFile, vectorsFileShort, queryCode6);

		List<Pair<Long, Double>> euclideanDists = recommender.getTopSnips(10, Recommender.Distance.EUCLIDEAN);

		// Loading code snips
		Map<Long, String> codeSnips = getCodeSnips(shortCodeSnipsFile);

		printSnippetsAndDistances(euclideanDists, codeSnips);
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
}
