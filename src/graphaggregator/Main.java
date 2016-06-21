package graphaggregator;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;



import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import au.com.bytecode.opencsv.CSVReader;

public class Main {


	public static void main(String args[]) throws IOException {

		System.out.println("Which mode do you want to use ? (adj/arff/rdf)");
		Scanner scanner = new Scanner(System.in);
		String mode = scanner.next();
		String path = "";
		int depth = 0;
		boolean writeOutput = false;
		String fileOutputPath = "";
		List<Integer> individuals = new LinkedList<Integer>();
		List<Integer> attributes = new LinkedList<Integer>();

		if(mode.equals("adj")) {
			System.out.println("Path to the csv file of the adjacency matrix (default : ./adj.csv) :");
			path = scanner.next();
			if(path.equals("")) {
				path = "./adj.csv";
			}
			System.out.println("Enter the indices of the individuals nodes :");
			int[] individualsInt = (int[]) Stream.of(scanner.next().split("\\s*,\\s*")).mapToInt(Integer::parseInt).toArray();
			for(int individual : individualsInt) {
				individuals.add(individual);
			}
			System.out.println("Select the desired depth: ");
			depth = Integer.parseInt(scanner.next());
		}
		if(mode.equals("arff")) {
			System.out.println("Path to the arff file of the adjacency matrix: ");
			path = scanner.next();
			System.out.println("Select the desired depth : ");
			depth = Integer.parseInt(scanner.next());
		}
		if(mode.equals("rdf")) {
			System.out.println("Path to the rdf file: ");
			path = scanner.next();
			System.out.println("Select the desired depth : ");
			depth = Integer.parseInt(scanner.next());
		}
		System.out.println("Do you want to write the output in a file (y/N) ?"); 
		if(scanner.next().equals("y")) {
			writeOutput = true;
		}
		if(writeOutput) {
			System.out.println("File name to write the output in:");
			fileOutputPath = scanner.next();
		}



		Graph g = new MultiGraph("Test");;
		Graph aggregated = new SingleGraph("aggregated");
		SpriteManager sman = new SpriteManager(aggregated);
		float[][] dense = null;
		float[][] agg;

		if(mode.equals("rdf")) {
			Rdfmanager rdf = new Rdfmanager();
			Model model = rdf.loadRdf(path);
			LinkedHashMap<String,Integer> individualsList = rdf.loadIndividuals(model);
			LinkedHashMap<String,Integer> attributesList = rdf.loadAttributes(model, individualsList.size());
			for(String key : individualsList.keySet()) {
				individuals.add(individualsList.get(key));
			}
			for(String key : attributesList.keySet()) {
				attributes.add(attributesList.get(key));
			}
			System.out.println(attributesList);
			dense = rdf.getAdjacencyMatrix(model, individualsList, attributesList);
			//			rdf.loadCSV("./epical2.csv",false);

		}
		if(mode.equals("adj")) {

			CSVReader reader = new CSVReader(new FileReader(path));
			String [] nextLine;
			String[] firstLine = reader.readNext();
			int n = firstLine.length;
			System.out.println(n);
			dense = new float[n][n];
			for(int i=0;i<n;i++) {
				dense[0][i] = Float.parseFloat(firstLine[i]);
			}
			for(int i=1;i<=n;i++) {
				if(!individuals.contains(i)) {
					attributes.add(i);
				}
			}
			int line = 1;

			while ((nextLine = reader.readNext()) != null) {
				for(int i=0;i<nextLine.length;i++) {
					dense[line][i] = Float.parseFloat(nextLine[i]);
				}
				line++;
			}


		}

		if(mode.equals("arff")) {
			int numberOfElements = 0;

			HashMap<String,Integer> attributesNames = new HashMap<String,Integer>();
			int n;
			BufferedReader arffReader =
					new BufferedReader(new FileReader(path));
			ArffReader arff = new ArffReader(arffReader);
			Instances data = arff.getData();
			n = data.numInstances();
			//			data.setClassIndex(data.numAttributes() - 1);
			for(int i=0;i<data.numAttributes();i++) {
				Attribute att = data.attribute(i);
				numberOfElements++;
				attributes.add(numberOfElements);
				attributesNames.put(att.name()+"_?", numberOfElements);
				n++;


				for(int j=0;j<att.numValues();j++) {
					numberOfElements++;
					attributes.add(numberOfElements);
					attributesNames.put(att.name()+"_"+att.value(j), numberOfElements);
					n++;
				}

			}
			System.out.println(attributesNames);
			dense = new float[n][n];

			for(int i=0;i<data.numInstances();i++) {
				Instance ins = data.instance(i);
				numberOfElements++;
				individuals.add(numberOfElements);
				for(int j=0;j<ins.numValues();j++) {
					//					if(ins.stringValue(j) != "?") {
					int attNodeId = attributesNames.get(ins.attribute(j).name()+"_"+ins.stringValue(j));
					if(numberOfElements==1) {
						System.out.println("Instance "+numberOfElements+"liée"+attNodeId);
					}
					dense[numberOfElements-1][attNodeId-1] = 1;
					dense[attNodeId-1][numberOfElements-1] = 1;
					//					}

				}
			}


		}


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


		for(int row = 0;row<dense.length;row++) {
			for(int column = 0;column<dense.length;column++) {
				if(dense[row][column] == 1) {
					g.addEdge(Integer.toString(row)+"/"+Integer.toString(column), row, column);
				}
			}
		}

		PrintWriter writerAdj = new PrintWriter("./vote_adj.csv", "UTF-8");
		for(int i = 0;i<dense.length;i++) {
			String line = Float.toString(dense[i][0]);
			for(int j = 1; j<dense.length;j++) {
				line+= ","+dense[i][j];
			}
			writerAdj.println(line);
		}
		writerAdj.close();

		individuals.sort(null);
		attributes.sort(null);

		agg = aggregateMatrix(dense, individuals, attributes, depth);

		g.display();

		for(int i=0;i<agg.length;i++) {
			for(int j=0; j<agg.length;j++) {
				if(agg[i][j] != 0) {
					try {
						aggregated.addEdge(Integer.toString(i)+Integer.toString(j), i, j);
					}
					catch(Exception e) {

					}
				}
			}
		}
		Viewer aggView = aggregated.display();
		System.out.println(agg);

		if(writeOutput) {
			PrintWriter writer = new PrintWriter(fileOutputPath, "UTF-8");

			for(int i = 0;i<agg.length;i++) {
				String line = Float.toString(agg[i][0]);
				for(int j = 1; j<agg.length;j++) {
					line+= ","+agg[i][j];
				}
				writer.println(line);
			}
			writer.close();
		}


	}
	public static float[][] aggregateMatrix(float[][] dense, List<Integer> individuals,List<Integer> attributes, int depth) {
		float[][] output = new float[individuals.size()][individuals.size()];
		float[][] kMult = dense; //kMult : A(n-1)
		float[][] avoidMemory = dense; //avoidMemory : A(n-2)
		float[][] avoidMemory2 = dense;
		float[][] oldMatrix;
		for(int iter = 2; iter <= depth; iter++) {
			oldMatrix = output;
			avoidMemory = avoidMemory2;
			avoidMemory2 = kMult;
			if(iter<=3) {
				kMult = customProduct(kMult,dense,attributes,dense); // A(n) -> A(n-1)

			}
			else {
				kMult = customProduct(kMult,dense,attributes,avoidMemory); // A(n) -> A(n-1)
			}

			for(int i = 0;i<individuals.size();i++) {

				for(int j=0; j<individuals.size();j++) {
					if(j>=i) { //Column above the diagonal
						float valueAtDepth = kMult[individuals.get(i)-1][individuals.get(j)-1];
						if(valueAtDepth >= 1 && i != j) {
							float oldValue = oldMatrix[i][j];
							output[i][j] = oldValue+(valueAtDepth/iter);
						}
						else {
						}
					}
					else {
						output[i][j] = output[j][i];
					}

				}
			}
		}



		return output;

	}

	public static float[][] customProduct(float[][] kMult, float[][] dense, List<Integer> attributeList, float[][] avoidMemory) {
		float[][] output = new float[kMult.length][dense.length];
		for(int row = 0; row < kMult.length;row++) {
			for(int col = 0;col < kMult.length;col++) {
				System.out.println(row+" "+col+" "+avoidMemory[row][col]);

				int value = 0;
				if(!attributeList.contains(row-1) && !attributeList.contains(col-1)) {
					if(col>row) {
						for(int k : attributeList) {

							float localValue = kMult[row][k-1]*dense[k-1][col];
							if(localValue >=1) {
								value += localValue;
							}
						}
					}
					else {
						value = (int) output[col][row];
					}
				}
				else {
					if(avoidMemory[row][col] == 0) {
						if(col>row) {
							for(int k : attributeList) {

								float localValue = kMult[row][k-1]*dense[k-1][col];
								if(localValue >=1) {
									value += localValue;
								}
							}
						}
						else {
							value = (int) output[col][row];
						}
					}
				}

				output[row][col]=value;
			}

		}
		return output;

	}
}
