package edu.cmu.lti.f12.hw2.hw2_team17.retrieval;


import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.cmu.lti.oaqa.openqa.hello.retrieval.SimpleSolrRetrievalStrategist;

import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;


public class lavanyaBioSolrRetrievalStrategist extends SimpleSolrRetrievalStrategist {

  protected List<RetrievalResult> retrieveDocuments(String query) {
    List<RetrievalResult> result = new ArrayList<RetrievalResult>();
    
  
    //For stemming
    getStemmedQueryClass obj = new getStemmedQueryClass();
    
    String[] keyterms=query.trim().split(" ");
    StringBuffer queryAndStemmed=new StringBuffer();
    for(int j=1;j<keyterms.length;j++)
    {
    	if(!keyterms[j].equals(obj.getStemmedQuery(keyterms[j])))
    		queryAndStemmed.append(obj.getStemmedQuery(keyterms[j])+" ");
    		
    }
    String queryAndStemmedString = queryAndStemmed.toString();
    
    try {
      SolrDocumentList docs = wrapper.runQuery(queryAndStemmedString, hitListSize);

      for (SolrDocument doc : docs) {

        RetrievalResult r = new RetrievalResult((String) doc.getFieldValue("id"),
                (Float) doc.getFieldValue("score"), queryAndStemmedString);
        result.add(r);
        System.out.println(doc.getFieldValue("id"));
      }
    } catch (Exception e) {
      System.err.println("Error retrieving documents from Solr: " + e);
    }
    return result;
  }

}


class getStemmedQueryClass {

    public String getStemmedQuery(String str)
    {

        MyAnalyzer analyzer = new MyAnalyzer();

        Reader reader = new StringReader(str);
        TokenStream stream = analyzer.tokenStream("", reader);
        StringBuffer strbuff= new StringBuffer();
        try {
        	 while (stream.incrementToken()) {
            CharTermAttribute term = stream.getAttribute(CharTermAttribute.class);
            strbuff.append(term.toString()); 
            }
        
        
        }
        catch(Exception e)
        {
        	System.out.println("stream error");
        }
       
			
        String stemmedString = strbuff.toString();
        return stemmedString;
             
    }

    static class MyAnalyzer extends Analyzer {
        public final TokenStream tokenStream(String fieldName, Reader reader) {
            TokenStream result = new StandardTokenizer(Version.LUCENE_36, reader);
            result = new PorterStemFilter(result);
            return result;
        }
    }
}

