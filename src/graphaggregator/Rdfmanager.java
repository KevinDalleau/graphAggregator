package graphaggregator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

import au.com.bytecode.opencsv.CSVReader;

public class Rdfmanager {
	String linked = "<http://graph.com/relation/linked>";
	
	public Model loadRdf(String path) throws FileNotFoundException {
		Model model = ModelFactory.createDefaultModel();
		FileReader is = new FileReader(path);
		model.read(is, null,"N-TRIPLES");
		
		return model;
		
	}
	
	public LinkedHashMap<String,Integer> loadIndividuals(Model model) {
		LinkedHashMap<String,Integer> individuals = new LinkedHashMap<String,Integer>();
		String queryString = "SELECT ?individuals ?individualId WHERE {?individuals <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  \"individual\" . ?individuals <http://graph.com/identifier> ?individualId.}";
		Query query = QueryFactory.create(queryString);
		QueryExecution qExec = QueryExecutionFactory.create(query,model);
		ResultSet results = qExec.execSelect();
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			RDFNode individual = sol.get("individuals");
			RDFNode individualId = sol.get("individualId");
			individuals.put(individual.toString().replace("http://graph.com/individual/", ""),Integer.parseInt(individualId.toString()));
		}
		return individuals;
	}
	
	public LinkedHashMap<String,Integer> loadAttributes(Model model, int offset) {
		LinkedHashMap<String,Integer> attributes = new LinkedHashMap<String,Integer>();
		String queryString = "SELECT ?attributes WHERE {?attributes <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  \"attribute\"}";
		Query query = QueryFactory.create(queryString);
		QueryExecution qExec = QueryExecutionFactory.create(query,model);
		ResultSet results = qExec.execSelect();
		int n = 1;
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			RDFNode attribute = sol.get("attributes");
			attributes.put(attribute.toString().replace("http://graph.com/", ""),offset+n);
			n++;
		}
		return attributes;
	}
	
	public Matrix getAdjacencyMatrix(Model model, LinkedHashMap<String,Integer> individuals, LinkedHashMap<String,Integer> attributes) {
		String queryString = "SELECT DISTINCT ?individual ?attribute WHERE {?individual <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \"individual\". ?individual "+linked+" ?attribute. ?attribute <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \"attribute\"}";
		Query query = QueryFactory.create(queryString);
		QueryExecution qExec = QueryExecutionFactory.create(query,model);
		ResultSet results = qExec.execSelect();
		int n = individuals.size()+attributes.size();
		Matrix dense = DenseMatrix.Factory.zeros(n,n);
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			String attribute = sol.get("attribute").toString().replace("http://graph.com/", "");
			int attributeIndex = attributes.get(attribute);
			String individual = sol.get("individual").toString().replace("http://graph.com/individual/", "");
			int individualIndex = individuals.get(individual);
			dense.setAsDouble(1.0, individualIndex-1, attributeIndex-1);
			dense.setAsDouble(1.0, attributeIndex-1,individualIndex-1);
		}
			    
		return dense;
		
	}
	
	public void loadCSV(String path, Boolean id) throws IOException {
		CSVReader reader = new CSVReader(new FileReader(path));
		Model model = ModelFactory.createDefaultModel();
		String[] nextLine;
		String[] header = reader.readNext();
		String prefix = "http://graph.com/";
		Property rdfType = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Property relation = model.createProperty(prefix+"relation/"+"linked");
		Property type = model.createProperty(prefix+"globalattribute");
		Property idProperty = model.createProperty(prefix+"identifier");

		int line = 1;
		while((nextLine = reader.readNext())!=null) {
			String id1 = Integer.toString(line);
			Resource r1 = model.createResource(prefix+"individual/"+id1);
			r1.addProperty(rdfType, "individual");
			r1.addProperty(idProperty, id1);
			for(int i=0;i<nextLine.length;i++) {
				String attribute = nextLine[i];
				Property p1 = model.createProperty(prefix+header[i]+"_"+attribute);
				Property superP1 = model.createProperty(prefix+header[i]);
				p1.addProperty(type, superP1);
				p1.addProperty(rdfType,"attribute");
				r1.addProperty(relation,p1);
			}		
			line++;
		}
		
		StmtIterator iter = model.listStatements();
		PrintWriter writer = new PrintWriter("./output.rdf", "UTF-8");

		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object

		    writer.print("<"+subject.toString()+">");
		    writer.print(" <" + predicate.toString() + "> ");
		    if (object instanceof Resource) {
		       writer.print("<"+object.toString()+">");
		    } else {
		        // object is a literal
		        writer.print(" \"" + object.toString() + "\"");
		    }

		    writer.println(" .");
		}
		writer.close();

	}
}
