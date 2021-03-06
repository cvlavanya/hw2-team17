package edu.cmu.lti.f12.hw2.hw2_team17.retrieval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ConfidenceChunker;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.core.provider.solr.SolrWrapper;
import edu.cmu.lti.oaqa.cse.basephase.retrieval.AbstractRetrievalStrategist;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class Team17BioSolrRetrievalStrategist_backup_original extends AbstractRetrievalStrategist {
  /**
   * Name of configuration parameter that must be set to the path of the model file.
   */
  public static final String PARAM_MODELFILE = "ModelFile";

  /**
   * Name of configuration parameter that must be set to the path of the dict file.
   */
  public static final String PARAM_DICTPATH = "WordNetDict";

  protected Integer hitListSize;

  protected SolrWrapper wrapper;

  protected WordNetDatabase wordnetDB;

  protected ConfidenceChunker chunker;

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    try {
      this.hitListSize = (Integer) aContext.getConfigParameterValue("hit-list-size");
    } catch (ClassCastException e) { // all cross-opts are strings?
      this.hitListSize = Integer.parseInt((String) aContext
              .getConfigParameterValue("hit-list-size"));
    }
    String serverUrl = (String) aContext.getConfigParameterValue("server");
    Integer serverPort = (Integer) aContext.getConfigParameterValue("port");
    Boolean embedded = (Boolean) aContext.getConfigParameterValue("embedded");
    String core = (String) aContext.getConfigParameterValue("core");
    try {
      this.wrapper = new SolrWrapper(serverUrl, serverPort, embedded, core);
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    }

    System.setProperty("wordnet.database.dir",
            (String) aContext.getConfigParameterValue(PARAM_DICTPATH));
    wordnetDB = WordNetDatabase.getFileInstance();

    try {
      String modelPath = (String) aContext.getConfigParameterValue(PARAM_MODELFILE);
      chunker = (ConfidenceChunker) AbstractExternalizable.readObject(new File(modelPath));
    } catch (IOException e) {
      throw new ResourceInitializationException();
    } catch (ClassNotFoundException e) {
      throw new ResourceInitializationException();
    }
  }

  @Override
  protected final List<RetrievalResult> retrieveDocuments(String questionText,
          List<Keyterm> keyterms) {
    List<String> queries = formulateQuery(keyterms);
    return retrieveDocuments(queries);
  };

  private List<String> formulateQuery(List<Keyterm> expandKeyTerms) {
	   
	    List<String> strList = new ArrayList<String>();
	    List<String> strList1 = new ArrayList<String>();
	    StringBuilder sb = new StringBuilder();
	    StringBuilder sb1 = new StringBuilder();


	    for (Keyterm k : expandKeyTerms) {
	      sb.append("\"" + k + "\" ");  
	      sb1.append(k + ",");  

	    }
	    sb.deleteCharAt(sb.length() - 1);
	   
	    System.out.println(" QUERY: " + sb.toString());
	    strList.add(sb.toString());
	    strList1.add(sb1.toString());

	    
	    try
	    {
	    	FileWriter queryFile = new FileWriter("src/main/resources/queryFile.txt");
	        BufferedWriter bw = new BufferedWriter(queryFile);
	        
	    for (Keyterm k : expandKeyTerms)
	      for (String e : k.getText().split(" "))
	        for (Synset synset : wordnetDB.getSynsets(e)) {
	          for (String wordForm : synset.getWordForms()) {
	            if (!e.toLowerCase().equals(wordForm.toLowerCase())) {
	              String newQuery = sb.toString().replace(e, wordForm);
	              String newQuery1 = sb1.toString().replace(e, wordForm);

	              if (!strList.contains(newQuery)) {
	                strList.add(newQuery);
	           	    bw.write(newQuery1);
	          	    bw.write("\n");
	                System.out.println(" QUERY: " + newQuery);
	              }
	            }
	          }
	        }

	    System.out.println("=================");

	    for (Keyterm k : expandKeyTerms)
	      for (String e : k.getText().split(" "))
	        if (isGene(e)) {
	          for (String newE : geneSynonymGenerator(e)) {
	            int size = strList.size();
	            if (!e.toLowerCase().equals(newE.toLowerCase()))
	              for (int i = 0; i < size; i++) {
	                String newQuery = strList.get(i).toString().replace(e, newE);
	                String newQuery1 = strList1.get(i).toString().replace(e, newE);

	                if (!strList.contains(newQuery)) {
	                	strList.add(newQuery);
	               	    bw.write(newQuery1);
	              	    bw.write("\n");
	                  System.out.println(" QUERY: " + newQuery);
	                }
	              }
	          }
	        }
	   
	    bw.close();
	    }
	    catch(Exception e)
	    {
	    	System.out.println("queryFile open error!");
	    }
	    return strList;

	  }
  // protected List<Keyterm> expandKeyTerms(List<Keyterm> keyterms) {
  // int size = keyterms.size();
  // for (int i = 0; i < size; i++) {
  // for (String e : geneSynonymGenerator(keyterms.get(i).getText())) {
  // if (!listContains(keyterms, e))
  // keyterms.add(new Keyterm(e));
  // }
  // }
  // return keyterms;
  // }

  // private boolean listContains(List<Keyterm> strList, String target) {
  // for (Keyterm v : strList)
  // if (v.getText().toLowerCase().equals(target.toLowerCase()))
  // return true;
  // return false;
  // }

  private boolean isGene(String e) {
    char[] cs = e.toCharArray();
    Iterator<Chunk> iter = chunker.nBestChunks(cs, 0, cs.length, 1);
    return Math.pow(2.0, iter.next().score()) > 0.62;
  }

  private List<String> geneSynonymGenerator(String text) {
    List<String> results = new ArrayList<String>();
    try {
      // Send the request
      URL url = new URL("http://gpsdb.expasy.org/cgi-bin/gpsdb/show");
      URLConnection conn = url.openConnection();
      conn.setDoOutput(true);
      OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

      // write parameters
      writer.write("name=" + text + "&species=&taxo=0&source=HGNC&type=prefered");
      writer.flush();

      // Get the response
      StringBuffer answer = new StringBuffer();
      BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        answer.append(line);
      }
      writer.close();
      reader.close();

      // Output the response
      String curStr = answer.toString();
      int start = 0;
      int end = 0;
      while (start != -1 && end != -1) {
        start = curStr.indexOf("<td class=\"name\">", end + 3) + 17;
        if (start == 16)
          break;
        end = curStr.indexOf("</td>", start);
        if (!results.contains(curStr.substring(start, end)))
          results.add(curStr.substring(start, end));
      }

    } catch (MalformedURLException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return results;
  }

  private List<RetrievalResult> retrieveDocuments(List<String> queries) {
    List<RetrievalResult> result = new ArrayList<RetrievalResult>();
    try {
      StringBuilder sb = new StringBuilder("(\"" + queries.get(0));
      List<String> combinedQueries = new ArrayList<String>();
      for (int i = 1; i < queries.size(); i++) {
        sb.append("\" OR \"" + queries.get(i));
        if (sb.length() > 4096) {
          sb.append("\")");
          combinedQueries.add(sb.toString());
          sb = new StringBuilder("(\"" + queries.get(i));
        }
      }
      sb.append("\")");
      combinedQueries.add(sb.toString());
      System.out.println(combinedQueries.size() + " queries");

      for (String query : combinedQueries) {
        SolrDocumentList docs = wrapper.runQuery(query, hitListSize);
        for (SolrDocument doc : docs) {
          RetrievalResult r = new RetrievalResult((String) doc.getFieldValue("id"),
                  (Float) doc.getFieldValue("score"), query);
          if (!resultContains(result, r))
            result.add(r);
          System.out.println(doc.getFieldValue("id"));
        }
      }
    } catch (Exception e) {
      System.err.println("Error retrieving documents from Solr: " + e);
    }
    return result;
  }

  private boolean resultContains(List<RetrievalResult> result, RetrievalResult r) {
    for (RetrievalResult e : result)
      if (e.getDocID().equals(r.getDocID()))
        return true;
    return false;
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    super.collectionProcessComplete();
    wrapper.close();
  }
}