package org.imixs.workflow.engine.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.DrillSideways.DrillSidewaysResult;
import org.apache.lucene.facet.DrillSideways;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/** Shows simple usage of faceted indexing and search. */
public class SimpleFacetsExample {

	private final Directory indexDir = new RAMDirectory();
	private final Directory taxoDir = new RAMDirectory();
	private final FacetsConfig config = new FacetsConfig();

	/** Empty constructor */
	public SimpleFacetsExample() {
		config.setHierarchical("Publish Date", true);
	}

	/** Build the example index. */
	private void index() throws IOException {
		IndexWriter indexWriter = new IndexWriter(indexDir,
				new IndexWriterConfig(new WhitespaceAnalyzer()).setOpenMode(OpenMode.CREATE));

		// Writes facet ords to a separate directory from the main index
		DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);

		Document doc = new Document();
	
		doc.add(new StringField("author", "Bob",  Store.YES));
    	doc.add(new FacetField("Author", "Bob"));
		doc.add(new FacetField("Publish Date", "2010", "10", "15"));
		indexWriter.addDocument(config.build(taxoWriter, doc));

		doc = new Document();
		doc.add(new StringField("author", "Lisa",  Store.YES));
    	doc.add(new FacetField("Author", "Lisa"));
		doc.add(new FacetField("Publish Date", "2010", "10", "20"));
		indexWriter.addDocument(config.build(taxoWriter, doc));

		doc = new Document();
		doc.add(new StringField("author", "Lisa",  Store.YES));
    	
		doc.add(new FacetField("Author", "Lisa"));
		doc.add(new FacetField("Publish Date", "2012", "1", "1"));
		indexWriter.addDocument(config.build(taxoWriter, doc));

		doc = new Document();
		doc.add(new StringField("author", "Susan",  Store.YES));
    	
		doc.add(new FacetField("Author", "Susan"));
		doc.add(new FacetField("Publish Date", "2012", "1", "7"));
		indexWriter.addDocument(config.build(taxoWriter, doc));

		doc = new Document();
		doc.add(new StringField("author", "Frank",  Store.YES));
    	
		doc.add(new FacetField("Author", "Frank"));
		doc.add(new FacetField("Publish Date", "1999", "5", "5"));
		indexWriter.addDocument(config.build(taxoWriter, doc));

		indexWriter.close();
		taxoWriter.close();
	}

	/**
	 * User runs a query and counts facets only without collecting the matching
	 * documents.
	 */
	private List<FacetResult> facetsOnly() throws IOException {
		DirectoryReader indexReader = DirectoryReader.open(indexDir);
		IndexSearcher searcher = new IndexSearcher(indexReader);
		TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);

		FacetsCollector fc = new FacetsCollector();

		// MatchAllDocsQuery is for "browsing" (counts facets
		// for all non-deleted docs in the index); normally
		// you'd use a "normal" query:
		searcher.search(new MatchAllDocsQuery(), fc);

		// Retrieve results
		List<FacetResult> results = new ArrayList<>();

		// Count both "Publish Date" and "Author" dimensions
		Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);

		results.add(facets.getTopChildren(10, "Author"));
		results.add(facets.getTopChildren(10, "Publish Date"));

		indexReader.close();
		taxoReader.close();

		return results;
	} 

	/** Runs the search example. */
	public List<FacetResult> runFacetOnly() throws IOException {
		index();
		return facetsOnly();
	}

	

	/** Runs the search and drill-down examples and prints the results. */
	public static void main(String[] args) throws Exception {
		System.out.println("Facet counting example:"); 
		System.out.println("-----------------------");
		SimpleFacetsExample example = new SimpleFacetsExample();
		List<FacetResult> results1 = example.runFacetOnly();
		System.out.println("Author: " + results1.get(0));
		System.out.println("Publish Date: " + results1.get(1));

	
	}

}
