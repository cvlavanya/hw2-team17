package edu.cmu.lti.f12.hw2.hw2_team17.retrieval;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.core.provider.solr.SolrWrapper;
import edu.cmu.lti.oaqa.cse.basephase.retrieval.AbstractRetrievalStrategist;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class YksunBioSolrRetrievalStrategist extends AbstractRetrievalStrategist {

  protected Integer hitListSize;

  protected SolrWrapper wrapper;

  protected WordNetDatabase wordnetDB;

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
    System.setProperty("wordnet.database.dir", "src/main/resources/wordnetDict/");
    wordnetDB = WordNetDatabase.getFileInstance();
  }

  @Override
  protected final List<RetrievalResult> retrieveDocuments(String questionText,
          List<Keyterm> keyterms) {
    List<String> queries = formulateQuery(keyterms);
    return retrieveDocuments(queries);
  };

  private List<String> formulateQuery(List<Keyterm> expandKeyTerms) {
    List<String> strList = new ArrayList<String>();
    StringBuilder sb = new StringBuilder();
    for (Keyterm k : expandKeyTerms) {
        sb.append("\"" + k + "\" ");
    }
    sb.deleteCharAt(sb.length() - 1);
    strList.add(sb.toString());

    for (Keyterm k : expandKeyTerms)
      for (String e : k.getText().split(" "))
        for (Synset synset : wordnetDB.getSynsets(e))
          for (String wordForm : synset.getWordForms()) {
            if (!e.toLowerCase().equals(wordForm.toLowerCase())) {
              String newQuery = sb.toString().replace(e, wordForm);
              if (!strList.contains(newQuery))
                strList.add(newQuery);
            }
          }

    int size = strList.size();
    for (Keyterm k : expandKeyTerms)
      for (String e : k.getText().split(" "))
        for (String newE : geneSynonymGenerator(e)) {
          if (!e.toLowerCase().equals(newE.toLowerCase()))
            for (int i = 0; i < size; i++)
              strList.add(strList.get(i).toString().replace(e, newE));
        }

    for (String e : strList)
      System.out.println(" QUERY: " + e);
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
      for (String query : queries) {
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