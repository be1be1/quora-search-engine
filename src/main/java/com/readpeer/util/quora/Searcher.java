package com.readpeer.util.quora;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Searcher {

    IndexSearcher indexSearcher;
    Directory indexDirectory = null;
    IndexReader indexReader = null;

    public Searcher(String indexDirectoryPath) throws IOException{
        indexDirectory = NIOFSDirectory.open(new File(indexDirectoryPath));
        indexReader = DirectoryReader.open(indexDirectory);
        indexSearcher = new IndexSearcher(indexReader);
    }

    public TopDocs booleanQuerySearch(List<String> searchQueries) throws IOException{
        BooleanQuery booleanQuery = new BooleanQuery();

        for(String searchQuery : searchQueries) {
            String[] words = searchQuery.split(" ");

            PhraseQuery query1 = new PhraseQuery();
            PhraseQuery query2 = new PhraseQuery();
            PhraseQuery query3 = new PhraseQuery();
            for(String word : words) {
                query1.add(new Term(LuceneConstants.CONTENTS, word));
                query2.add(new Term(LuceneConstants.QUESTION, word));
                query3.add(new Term(LuceneConstants.CATEGORY, word));
                query1.setBoost(1.0f);
                query2.setBoost(3.0f);
                query3.setBoost(5.0f);
            }
            booleanQuery.add(query1, BooleanClause.Occur.SHOULD);
            booleanQuery.add(query2, BooleanClause.Occur.SHOULD);
            booleanQuery.add(query3, BooleanClause.Occur.SHOULD);
        }

        TopDocs td = indexSearcher.search(booleanQuery, LuceneConstants.MAX_SEARCH);
        return td;
    }

    public TopDocs normalQuerySearch(List<String> searchQueries, String queryType) throws IOException, ParseException{
        StringBuilder builder= new StringBuilder();
        for (String searchQuery : searchQueries) {
            if (searchQuery.matches(".*\\s+.*")) {
                searchQuery = "\""+searchQuery+"\"";
//                System.out.println(searchQuery);
            }
            builder.append(searchQuery).append(" ");
        }

        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_45);
        String line = builder.toString();
        QueryParser parser = null;

        if (queryType.equals("quora")) {
            parser = new QueryParser(Version.LUCENE_45, LuceneConstants.CONTENTS, analyzer);
        } else if (queryType.equals("tweets")) {
            parser = new QueryParser(Version.LUCENE_45, "text", analyzer);
        } else {
            System.out.println("No such searcher provided");
            System.exit(1);
        }
        Query query = parser.parse(QueryParser.escape(line));
        TopDocs td = indexSearcher.search(query, LuceneConstants.MAX_SEARCH);

        System.out.println(td.totalHits);
        return td;
    }

    public Document getDocument(ScoreDoc scoreDoc) throws IOException {
        return indexSearcher.doc(scoreDoc.doc);
    }

    public void close() throws IOException {
        indexReader.close();
    }

}
