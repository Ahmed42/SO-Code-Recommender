

/**


final int[] kindsOfParsing = {
    			ASTParser.K_COMPILATION_UNIT, 
    			ASTParser.K_CLASS_BODY_DECLARATIONS, 
    			ASTParser.K_STATEMENTS,
    			ASTParser.K_EXPRESSION
    			};
    	
    	
    	System.out.println("Parsing query code ..");
    	
    	Pair<ASTNode, Integer> queryCodeTypedAST = getCodeSnipTypedAST(queryCode, kindsOfParsing);
    	
    	if(queryCodeTypedAST.Right == -1) {
    		System.out.println("Error! Invalid query code. Can't parse it. Terminating.");
    		return;
    	}
    	
    	System.out.println("Query code successfully parsed.");
    	
    	System.out.println("Computing query code AST vector ..");
    	int[] queryCodeTreeVec = computeTreeVector(queryCodeTypedAST.Left);
    	
    	System.out.println("Query code AST vector: " + Arrays.toString(queryCodeTreeVec));
    	
    	
    	List<Pair<Long, Double>> euclideanDistances = new LinkedList<Pair<Long, Double>>();
    	List<Pair<Long, Double>> cosineDistances = new LinkedList<Pair<Long, Double>>();


System.out.println("Importing vectors and calculating distances ...");
    	
    	Reader reader = new FileReader(vectorsFileShort);
    	Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);
    	
    	//int vectorCount = 0;
    	
    	long startTime = System.nanoTime();
    	for(CSVRecord record : records) {
    		long codeBlockId = Long.parseUnsignedLong(record.get(0));
    		String codeVectorStr = record.get(1);
    		
    		//System.out.println(codeBlockId);
    		int[] codeVector = 
    				Arrays.stream(codeVectorStr.split(","))
    				.mapToInt(c -> Integer.parseInt(c.trim()))
    				.toArray();
    		
    		//System.out.println(Arrays.toString(vector));
    		double euclideanDistance = computeEuclideanDistance(queryCodeTreeVec, codeVector);
			double cosineDistance = computeCosineDistance(queryCodeTreeVec, codeVector);
			
			euclideanDistances.add(new Pair<Long, Double>(codeBlockId, euclideanDistance));
			cosineDistances.add(new Pair<Long, Double>(codeBlockId, cosineDistance));
    	}
    	
    	
    	
    	long endTime = System.nanoTime();
    	long elapsedTime = endTime - startTime;
    	
    	System.out.println("Done.");
    	System.out.println("Total time: " + elapsedTime/(Math.pow(10, 6)) + " ms" );
    	
    	
    	System.out.println("Sorting by distance ..");
    	euclideanDistances.sort(Comparator.comparingDouble(distPair -> distPair.Right));
    	
    	Comparator<Pair<Long, Double>> comp = Comparator.comparingDouble(distPair -> distPair.Right);
    	cosineDistances.sort(comp.reversed());
    	System.out.println("Sorting done.");
    	System.out.println("");
    	
    	
    	
    	System.out.println("Euclidean Distance:");
    	printSnippetsAndDistances(euclideanDistances, codeSnips, 10);
    	
    	System.out.println("Cosine Distance:");
    	printSnippetsAndDistances(cosineDistances, codeSnips, 10);
    	
    	//computeAndStoreTreeVectors(highScoreCodeSnipsFile, vectorsFileHigh, kindsOfParsing);
    	
    	/*Reader reader = new FileReader(codeSnipsFile);
    	Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
    	
    	Map<Long, String> codeSnips = new HashMap<Long, String>(); 
    	
    	System.out.println("Going through SO snippets ..");
    	
    	long startTime = System.nanoTime();
    	int snipCount = 0;
    	int parsedSnipCount = 0;
    	for(CSVRecord record : records) {
    		long codeBlockId = Long.parseUnsignedLong(record.get("CodeBlockId"));
    		String answerId = record.get("AnswerId");
    		String codeSnip = record.get("Content");
    		
    		codeSnips.put(codeBlockId, codeSnip);
    		Pair<ASTNode, Integer> codeSnipTypedAST = getCodeSnipTypedAST(codeSnip, kindsOfParsing);
    		
    		
    		if(codeSnipTypedAST.Right != -1) {
    			parsedSnipCount++;
    			int[] codeSnipTreeVec = computeTreeVector(codeSnipTypedAST.Left);
    			
    			double euclideanDistance = computeEuclideanDistance(queryCodeTreeVec, codeSnipTreeVec);
    			double cosineDistance = computeCosineDistance(queryCodeTreeVec, codeSnipTreeVec);
    			
    			euclideanDistances.add(new Pair<Long, Double>(codeBlockId, euclideanDistance));
    			cosineDistances.add(new Pair<Long, Double>(codeBlockId, cosineDistance));
    		} else {
    		}
    		
    		snipCount++;
    	}
    	long endTime = System.nanoTime();
    	long elapsedTime = endTime - startTime;
    	
    	System.out.println("SO Snippets processing done.");
    	System.out.println("Total snippets: " + snipCount);
    	System.out.println("Successfully parsed: " + parsedSnipCount);
    	System.out.println("Total processing time: " + elapsedTime/Math.pow(10,9) + " seconds");
    	System.out.println("Avg processing time per snippet: " 
    	+ elapsedTime/(snipCount * Math.pow(10, 3)) + "ms" );
    	System.out.println("");
    	
    	System.out.println("Sorting by distance ..");
    	euclideanDistances.sort(Comparator.comparingDouble(distPair -> distPair.Right));
    	
    	Comparator<Pair<Long, Double>> comp = Comparator.comparingDouble(distPair -> distPair.Right);
    	cosineDistances.sort(comp.reversed());
    	System.out.println("Sorting done.");
    	System.out.println("");
    	
    	
    	System.out.println("Euclidean Distance:");
    	printSnippetsAndDistances(euclideanDistances, codeSnips, 10);
    	
    	System.out.println("Cosine Distance:");
    	printSnippetsAndDistances(cosineDistances, codeSnips, 10);
    
    	
        result.accept(new ASTVisitor() {
        	public void preVisit(ASTNode node) {
        		System.out.println("============");
        		System.out.println("Type: " + node.getNodeType());
        		System.out.println("Node: " + node);
        		
        		System.out.println("Parent: " + node.getParent());
        		
        		treeVector[node.getNodeType()]++;
        		
        		treeVec.merge(node.getNodeType(), 1, (oldVal, one) -> oldVal + one);
        		
        		List structProperties = node.structuralPropertiesForType();
        		
        		System.out.println("Properties: ");
        		for(Object structProperty : structProperties) {
        			StructuralPropertyDescriptor structPropertyDesc = (StructuralPropertyDescriptor) structProperty;
        			System.out.println(structProperty + " : " + node.getStructuralProperty(structPropertyDesc));
        		}
        		
        		System.out.println("End of Properties.");
        		
        		System.out.println("============");
        		
        	}
        });
        
        
        Map props = result.properties();
        
        props.forEach((k, v) -> System.out.println(k  + ":" + v));
        
        for(Object entry :  props.entrySet()) {
        	System.out.println(((Map.Entry<String, Object>) entry).getKey() + " : " +
        	((Map.Entry<String, Object>) entry).getValue());
        }
    	
    	
  


*/








/*
    public static Pair<ASTNode, Integer> getCodeSnipTypedAST(String codeSnip, int[] kindsOfParsing) {
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
    
   
    public static boolean isASTValid(ASTNode node) {
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

    public static int[] computeTreeVector(ASTNode root) {
    	int[] treeVector = new int[92];
    	
    	root.accept(new ASTVisitor() {
        	public void preVisit(ASTNode node) {
        		treeVector[node.getNodeType()-1]++;
        
        	}
        });
    	
    	return treeVector;
    }
    
    public static void computeAndStoreTreeVectors(String sourceFile, String vectorsFile, int[] kindsOfParsing) throws IOException {
    	Reader reader = new FileReader(sourceFile);
    	Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
    	
    	Writer writer = new FileWriter(vectorsFile);
    	
    	System.out.println("Going through snippets ..");
    	
    	long startTime = System.nanoTime();
    	long snipCount = 0;
    	long parsedSnipCount = 0;
    	for(CSVRecord record : records) {
    		long codeBlockId = Long.parseUnsignedLong(record.get("CodeBlockId"));
    		String answerId = record.get("AnswerId");
    		String codeSnip = record.get("Content");
    		
    		System.out.println("Parsing snippet no: " + snipCount);
    		
    		// Get code snip AST and parse kind
    		Pair<ASTNode, Integer> codeSnipTypedAST = getCodeSnipTypedAST(codeSnip, kindsOfParsing);
    		
    		
    		if(codeSnipTypedAST.Right != -1) {
    			parsedSnipCount++;
    			
    			// Get tree vector of valid ASTs
    			int[] codeSnipTreeVec = computeTreeVector(codeSnipTypedAST.Left);
    			
    			String codeSnipVecStr = Arrays.toString(codeSnipTreeVec);
    			
    			// [1, 2, 3, 4] -> 1, 2, 3, 4
    			CSVFormat.DEFAULT.printRecord(writer, codeBlockId, 
    					codeSnipVecStr.substring(1, codeSnipVecStr.length() - 1));
    		} else {
    		}
    		
    		snipCount++;
    	}
    	long endTime = System.nanoTime();
    	long elapsedTime = endTime - startTime;
    	
    	System.out.println("SO Snippets processing done.");
    	System.out.println("Total snippets: " + snipCount);
    	System.out.println("Successfully parsed: " + parsedSnipCount);
    	System.out.println("Total processing time: " + elapsedTime/Math.pow(10,9) + " seconds");
    	System.out.println("Avg processing time per snippet: " 
    	+ elapsedTime/(snipCount * Math.pow(10, 3)) + "ms" );
    }
    
    
    public static double computeEuclideanDistance(int[] vec1, int[] vec2) {
    	assert(vec1.length == vec2.length);
    	
    	long sumOfSquares = 0;
    	for(int i = 0; i < vec1.length; i++) {
    		int diff = vec1[i] - vec2[i];
    		sumOfSquares += diff * diff;
    	}
    	
    	return Math.sqrt(sumOfSquares);
    }
    
    public static double computeCosineDistance(int[] vec1, int[] vec2) {
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
    
    
    public static void printSnippetsAndDistances(List<Pair<Long, Double>> snipsDistances, Map<Long, String> codeSnips, int n) {
    	System.out.println("Top " + n +" Closest code snippets: ");
    	for(int i = 0; i < n; i++) {
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
    
}


*/






