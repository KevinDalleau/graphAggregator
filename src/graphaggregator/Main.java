package graphaggregator;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.neo4j.jdbc.Driver;

import java.util.Arrays;
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

import au.com.bytecode.opencsv.CSVReader;

public class Main {
	  
	 // Driver method to
    public static void main(String args[]) throws IOException
    {

    	
        Graph g = new MultiGraph("Test");;
        Graph aggregated = new SingleGraph("aggregated");
        SpriteManager sman = new SpriteManager(aggregated);
        
        List<Integer> individuals = Arrays.asList(1,6,11,12,16,17,20,23,25,27,29);
        List<Integer> attributes = Arrays.asList(2,3,4,5,7,8,9,10,13,14,15,18,19,21,22,24,26,28,30);
        
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
       

        int n = 30;
        Matrix dense = DenseMatrix.Factory.zeros(n, n);
        
        CSVReader reader = new CSVReader(new FileReader("adj.csv"));
        String [] nextLine;
        int line = 0;
        while ((nextLine = reader.readNext()) != null) {
           // nextLine[] is an array of values from the line
        	for(int i=0;i<nextLine.length;i++) {
               dense.setAsInt(Integer.parseInt(nextLine[i]), line,i);
        	}
        	line++;
        }
        
       for(int row = 0;row<dense.getRowCount();row++) {
    	   for(int column = 0;column<dense.getColumnCount();column++) {
    		   if(dense.getAsByte(row,column) == 1) {
    			   g.addEdge(Integer.toString(row)+Integer.toString(column), row, column);
    		   }
    	   }
       }
       System.out.println(dense);
       Matrix agg = aggregateMatrix(dense, individuals, attributes, 100);
       System.out.println(agg);
       g.display();
       
       for(int i=0;i<agg.getSize(0);i++) {
    	   for(int j=0; j<agg.getSize(0);j++) {
    		   if(agg.getAsDouble(i,j) != 0) {
    			   try {
        			   aggregated.addEdge(Integer.toString(i)+Integer.toString(j), i, j);
    			   }
    			   catch(Exception e) {
    				   
    			   }
    		   }
    	   }
       }
       Viewer aggView = aggregated.display();
       
    }
    public static Matrix aggregateMatrix(Matrix adj, List<Integer> individuals,List<Integer> attributes, int depth) {
    	Matrix output = DenseMatrix.Factory.zeros(individuals.size(), individuals.size());
    	Matrix kMult = adj;
    	Matrix oldMatrix;
    	Matrix oldMult;
    	for(int iter = 2; iter <= depth; iter++) {
    		oldMatrix = output;
    		oldMult = kMult;
    		kMult = customProduct(kMult,adj,attributes);
			System.out.println("Iteration"+iter+" : "+oldMatrix);

        	for(int i = 1;i<individuals.size();i++) {
        		for(int j=1; j<individuals.size();j++) {
        			Double valueAtDepth = kMult.getAsDouble(individuals.get(i-1)-1,individuals.get(j-1)-1);
//        			System.out.println("Adjacence entre "+individuals.get(i-1)+" et "+individuals.get(j-1)+":"+valueAtDepth);
            		if(valueAtDepth >= 1 && valueAtDepth != oldMult.getAsDouble(individuals.get(i-1)-1,individuals.get(j-1)-1) && i != j) {
            			Byte oldValue = oldMatrix.getAsByte(i-1,j-1);
            			output.setAsDouble(oldValue+(valueAtDepth/iter), i-1,j-1);
            		}

        		}
        	}
    	}
    	
    	
    	
    	return output;
    	
    }
    
    public static Matrix customProduct(Matrix A, Matrix B, List<Integer> attributeList) {
    	Matrix output = DenseMatrix.Factory.zeros(A.getSize(0),A.getSize(0));
    	for(int row = 0; row < A.getSize(0);row++) {
    		for(int col = 0;col < A.getSize(0);col++) {
//    			System.out.println("Valeur à ("+row+","+col+") = "+A.getAsByte(row,col));
    			int value = 0;
    			for(int k : attributeList) {
    				value += A.getAsByte(row,k-1)*B.getAsByte(col,k-1);
    			}
    			output.setAsInt(value, row, col);

    			
    		}
    	}
		return output;
    	
    }
}
