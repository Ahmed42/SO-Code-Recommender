package sproc.processor;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;

public class TestLuc {
	public static String shortCodeSnipsFile = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\more_java_code_snips.csv";
	public static String highScoreCodeSnipsFile = "C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\java_snips_score_10.csv";
	
	public static void runTest() throws IOException, ParseException {
		
		StandardAnalyzer analyzer = new StandardAnalyzer();
	
		Directory index = buildIndex(analyzer);
		
		//String query = "action cats are kings";

		//String codeQuery = "class Test { public void foo() { int x = 1 + 2; System.out.println(\"haha\"); }";
		
		String codeQuery = "org.eclipse.*";
	
		Query q = new QueryParser("Code", analyzer) .parse(codeQuery);
		
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopDocs docs = searcher.search(q, 5);
		ScoreDoc[] hits = docs.scoreDocs;
		
		for(ScoreDoc hit: hits) {
			Document d = searcher.doc(hit.doc);
			System.out.println(hit.doc + " :" + hit.score + " : " + d.get("Code"));
		}
	}
	
	
	public static Directory buildIndex(StandardAnalyzer analyzer) throws IOException {
		Directory index = MMapDirectory.open(Paths.get("C:\\Users\\Admin\\Documents\\UofA\\CMPUT 663\\Project\\index\\index.idx"));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter w = new IndexWriter(index, config);
		
		Iterable<CSVRecord> codeSnips = Utils.loadCSVDataFrom(shortCodeSnipsFile, true);
		
		for(CSVRecord codeRecord : codeSnips) {
			String codeBlockId = codeRecord.get("CodeBlockId");
			String code = codeRecord.get("Content");
			
			//System.out.println(code);
			
			Document doc = new Document();
			doc.add(new TextField("Code", code, Field.Store.YES));
			doc.add(new StringField("CodeBlockID", codeBlockId, Field.Store.YES));
			
			w.addDocument(doc);
		}
		
		w.close();
		
		return index;
	}
	
}
