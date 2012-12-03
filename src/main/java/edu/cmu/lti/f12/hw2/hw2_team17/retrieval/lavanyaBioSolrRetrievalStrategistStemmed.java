package edu.cmu.lti.f12.hw2.hw2_team17.retrieval;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.cmu.lti.oaqa.openqa.hello.retrieval.SimpleSolrRetrievalStrategist;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;


public class lavanyaBioSolrRetrievalStrategistStemmed extends SimpleSolrRetrievalStrategist {

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
        analyzer.close();
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

class getSynonymsClass {
	public String getSysnonyms(String wordForm)
	{
System.setProperty("wordnet.database.dir", "src/main/resources/wordnet/WordNet-3.0/dict/");



//String wordForm = "ate";
StringBuffer synonyms=new StringBuffer();
//  Get the synsets containing the wrod form
WordNetDatabase database = WordNetDatabase.getFileInstance();
Synset[] synsets = database.getSynsets(wordForm);
//  Display the word forms and definitions for synsets retrieved
if (synsets.length > 0)
{
	/*System.out.println("The following synsets contain '" +
			wordForm + "' or a possible base form " +
			"of that text:");*/
	for (int i = 0; i < synsets.length; i++)
	{
		System.out.println("");
		String[] wordForms = synsets[i].getWordForms();
		for (int j = 0; j < wordForms.length; j++)
		{
			//System.out.print((j > 0 ? ", " : "") +wordForms[j]);
			synonyms.append(wordForms[j]+",");
			
				
		} //End of for
		
	}
	String syn=synonyms.toString();
	String synArray[]=syn.split(",");
	Set<String> strSet = new LinkedHashSet<String>();
    for (String str : synArray) {
        strSet.add(str);
    }

    StringBuffer sb = new StringBuffer();
    for (String str : strSet) {
        sb.append(str+",");
    }
    System.out.println(sb.toString());
    return sb.toString();
}
else
{
	System.err.println("No synsets exist that contain " +
			"the word form '" + wordForm + "'");
	return wordForm;
}
	}//end of function
}//end of class
