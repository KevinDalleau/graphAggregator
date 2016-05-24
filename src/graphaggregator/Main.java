package graphaggregator;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.neo4j.jdbc.Driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;
import org.neo4j.cypher.internal.frontend.v3_0.ast.functions.Properties;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.jdbc.rest.Statement;
import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import au.com.bytecode.opencsv.CSVReader;

public class Main {
	  
	 // Driver method to
    public static void main(String args[]) throws IOException
    {
    	
    	Graph g = new MultiGraph("Test");;
        Graph aggregated = new SingleGraph("aggregated");
        SpriteManager sman = new SpriteManager(aggregated);
    	int numberOfElements = 0;
    	List<Integer> individuals = new ArrayList<Integer>();
    	List<Integer> attributes = new ArrayList<Integer>(); 
    	HashMap<String,Integer> attributesNames = new HashMap<String,Integer>();
    	int n;
        Matrix dense;
    	BufferedReader arffReader =
    			 new BufferedReader(new FileReader("vote.arff"));
    			 ArffReader arff = new ArffReader(arffReader);
    			 Instances data = arff.getData();
    			 n = data.numInstances();
    			 data.setClassIndex(data.numAttributes() - 1);
    			 for(int i=0;i<data.numAttributes();i++) {
    				 Attribute att = data.attribute(i);
    				 for(int j=0;j<att.numValues();j++) {
    					 numberOfElements++;
    					 attributes.add(numberOfElements);
    					 attributesNames.put(att.name()+"_"+att.value(j), numberOfElements);
    					 n++;
    				 }
    			 }
    			 dense = DenseMatrix.Factory.zeros(n, n);

    			 for(int i=0;i<data.numInstances();i++) {
    				 Instance ins = data.instance(i);
    				 numberOfElements++;
    				 individuals.add(numberOfElements);
    				 for(int j=0;j<ins.numValues();j++) {
    					 if(ins.stringValue(j) != "?") {
        					 int attNodeId = attributesNames.get(ins.attribute(j).name()+"_"+ins.stringValue(j));
        					 System.out.println("Instance "+numberOfElements+"liée"+attNodeId);
        		             dense.setAsInt(1, numberOfElements-1,attNodeId-1);
        		             dense.setAsInt(1, attNodeId-1, numberOfElements-1);

    					 }
    					 

    				 }
    			 }
    			 
    			 System.out.println(attributes.size());
//        List<Integer> individuals = Arrays.asList(1,5);
//        List<Integer> attributes = Arrays.asList(2,3,4);
//        
        
//        List<Integer> individuals = Arrays.asList(1,6,11,12,16,17,20,23,25,27,29);
//        List<Integer> attributes = Arrays.asList(2,3,4,5,7,8,9,10,13,14,15,18,19,21,22,24,26,28,30);
        
//        List<Integer> individuals = Arrays.asList(1,3,5);
//        List<Integer> attributes = Arrays.asList(2,4,6,7,8,9,10,11,12,13,14);
//        
        for(Integer i:individuals) {
        	g.addNode(i.toString());
        	aggregated.addNode(i.toString());
        	Sprite s = sman.addSprite(i.toString());
        	s.attachToNode(i.toString());
        	g.getNode(i.toString()).setAttribute("type", "I");;
        }
        for(Integer a:attributes) {
        	g.addNode(a.toString());
        	g.getNode(a.toString()).setAttribute("type", "A");;

        }
       

//        int n = individuals.size()+attributes.size();
//        Matrix dense = DenseMatrix.Factory.zeros(n, n);
//        
//        CSVReader reader = new CSVReader(new FileReader("adj.csv"));
//        String [] nextLine;
//        int line = 0;
//        while ((nextLine = reader.readNext()) != null) {
//           // nextLine[] is an array of values from the line
//        	for(int i=0;i<nextLine.length;i++) {
//               dense.setAsInt(Integer.parseInt(nextLine[i]), line,i);
//        	}
//        	line++;
//        }
        
       for(int row = 0;row<dense.getRowCount();row++) {
    	   for(int column = 0;column<dense.getColumnCount();column++) {
    		   if(dense.getAsByte(row,column) == 1) {
    			   g.addEdge(Integer.toString(row)+"/"+Integer.toString(column), row, column);
    		   }
    	   }
       }
       System.out.println(dense);
       Matrix agg = aggregateMatrix(dense, individuals, attributes, 2);
       System.out.println(agg);
       g.display();
       PrintWriter writer = new PrintWriter("file.txt", "UTF-8");
       for(int i=0;i<agg.getSize(0);i++) {
    	   for(int j=0; j<agg.getSize(0);j++) {
    		   if(agg.getAsDouble(i,j) != 0) {
    			   try {
        			   aggregated.addEdge(Integer.toString(i)+"/"+Integer.toString(j), i, j);
        		       writer.println(i+" "+j+" "+agg.getAsDouble(i,j));
    			   }
    			   catch(Exception e) {
    				   
    			   }
    		   }
    	   }
       }
       writer.close();
       
       Viewer aggView = aggregated.display();
       
    }
    public static Matrix aggregateMatrix(Matrix adj, List<Integer> individuals,List<Integer> attributes, int depth) {
    	Matrix output = DenseMatrix.Factory.zeros(individuals.size(), individuals.size());
    	Matrix kMult = adj;
    	Matrix oldMatrix;
    	Matrix oldMult;
    	HashMap<Integer, ArrayList<Integer>> visited = new HashMap<Integer, ArrayList<Integer>>();
    	for(int iter = 2; iter <= depth; iter++) {
    		oldMatrix = output;
    		oldMult = kMult;
    		kMult = customProduct(kMult,adj,attributes,visited);
    		System.out.println("Iteration "+iter+" :"+kMult);
//			System.out.println("Iteration"+iter+" : "+oldMatrix);

        	for(int i = 0;i<individuals.size();i++) {
        		ArrayList<Integer> visitedList = new ArrayList<Integer>();
        		if(!visited.containsKey(i)) {
            		visited.put(i, visitedList);
        		}
        		for(int j=0; j<individuals.size();j++) {
        				Double valueAtDepth = kMult.getAsDouble(individuals.get(i)-1,individuals.get(j)-1);
//            			System.out.println("Adjacence entre "+individuals.get(i-1)+" et "+individuals.get(j-1)+":"+valueAtDepth);
//            			&& valueAtDepth != oldMult.getAsDouble(individuals.get(i)-1,individuals.get(j)-1)
                		if(valueAtDepth >= 1 && i != j) {
                			visited.get(i).add(j);
                			Double oldValue = oldMatrix.getAsDouble(i,j);
//                			System.out.println(oldValue + "à "+i+","+j);
//                			System.out.println(valueAtDepth);
                			output.setAsDouble(oldValue+(valueAtDepth/iter), i,j);
                		}
        			}
        			
        	}
    	}
    	
    	
    	
    	return output;
    	
    }
    
    public static Matrix customProduct(Matrix A, Matrix B, List<Integer> attributeList, HashMap<Integer,ArrayList<Integer>> visitedList) {
    	Matrix output = DenseMatrix.Factory.zeros(A.getSize(0),A.getSize(0));
    	for(int row = 0; row < A.getSize(0);row++) {
    		for(int col = 0;col < A.getSize(0);col++) {
    			int value = 0;
    			for(int k : attributeList) {
    				if(col<row) {
    					output.setAsDouble(output.getAsDouble(col,row), row, col);
    				}
    				else {
    					if(visitedList.get(row) != null) {
        					if(!visitedList.get(row).contains(col)) {
                				value += A.getAsDouble(row,k-1)*B.getAsDouble(k-1,col);
        					}
        				}
        				else {
            				value += A.getAsDouble(row,k-1)*B.getAsDouble(k-1,col);

        				}
            			output.setAsDouble(value, row, col);

        			}

    				}
    				
    			
    		}
    	}
		return output;
    	
    }
}
