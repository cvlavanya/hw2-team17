package edu.cmu.lti.f12.hw2.hw2_team17.passage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ConfidenceChunker;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.PassageCandidate;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.cmu.lti.oaqa.openqa.hello.passage.KeytermWindowScorerSum;
import edu.cmu.lti.oaqa.openqa.hello.passage.SimplePassageExtractor;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class Team17PassageExtractor extends SimplePassageExtractor {
  protected WordNetDatabase wordnetDB;

  protected ConfidenceChunker chunker;

  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    wordnetDB = WordNetDatabase.getFileInstance();

    try {
      URL modelPath = this.getClass().getClassLoader()
              .getResource("lingpipeModel/ne-en-bio-genetag.HmmChunker");
      ObjectInputStream ois = new ObjectInputStream(modelPath.openStream());
      chunker = (ConfidenceChunker) ois.readObject();
    } catch (IOException e) {
      throw new ResourceInitializationException();
    } catch (ClassNotFoundException e) {
      throw new ResourceInitializationException();
    }
  }

  /**
   * To perform query expansion and extract passage based on expanded query.
   * <p>
   * This method expands the queries, by using gene and word synonyms and word forms. It separates
   * the sentences in the passage by means of punctuations.
   * 
   * @param question
   *          the question text
   * @param keyterms
   *          the list of extracted keyterms
   * @param documents
   *          the list of documents retrieved in the document retrieval phase
   */
  @Override
  protected List<PassageCandidate> extractPassages(String question, List<Keyterm> keyterms,
          List<RetrievalResult> documents) {
    List<PassageCandidate> result = new ArrayList<PassageCandidate>();

    List<String> keytermStrings = Lists.transform(keyterms, new Function<Keyterm, String>() {
      public String apply(Keyterm keyterm) {
        return keyterm.getText();
      }
    });

    List<String> expanded = new ArrayList<String>();
    List<Double> weight = new ArrayList<Double>();
    for (String keyterm : keytermStrings) {
      if (!expanded.contains(keyterm)) {
        expanded.add(keyterm);
        if (isGene(keyterm))
          weight.add(2.0);
        else
          weight.add(1.4);
      }
      for (String ex : ExpandByWordnet(keyterm)) {
        if (!expanded.contains(ex)) {
          expanded.add(ex);
          weight.add(0.6);
        }
      }
      for (String ex : ExpandByGene(keyterm)) {
        if (!expanded.contains(ex)) {
          expanded.add(ex);
          weight.add(1.0);
        }
      }
    }

    double[] idfs = new double[expanded.size()];
    for (int i = 0; i < expanded.size(); i++)
      idfs[i] = 1.0;

    for (RetrievalResult doc : documents) {
      String text = null;
      try {
        text = wrapper.getDocText(doc.getDocID());
      } catch (SolrServerException e) {
        e.printStackTrace();
      }
      for (int i = 0; i < expanded.size(); i++) {
        if (text.contains(expanded.get(i)))
          idfs[i] = idfs[i] + 1;
      }
    }
    List<String> finalKeys = new ArrayList<String>();
    List<Double> finalIdfs = new ArrayList<Double>();
    List<Double> finalWeight = new ArrayList<Double>();
    for (int i = 0; i < expanded.size(); i++) {
      if (idfs[i] > 1.5) {
        finalKeys.add(expanded.get(i));
        finalIdfs.add(idfs[i]);
        finalWeight.add(weight.get(i));
      }
    }
    for (int i = 0; i < finalIdfs.size(); i++)
      finalIdfs.set(i, Math.log(documents.size() / finalIdfs.get(i)));

    System.out.println("expanded keyterms " + finalKeys.toString());

    for (RetrievalResult document : documents) {
      String id = document.getDocID();
      try {
        String htmlText = wrapper.getDocText(id);

        // for now, making sure the text isn't too long
        String text = htmlText.substring(0, Math.min(5000, htmlText.length()));
        char[] textC = text.toCharArray();
        pure(textC);
        text = new String(textC);

        ZhenxiangSentenceFinder finder = new ZhenxiangSentenceFinder(id, text,
                new KeytermWindowScorerSum());

        List<PassageCandidate> passageSpans = finder.extractPassages(
                finalKeys.toArray(new String[0]), finalWeight.toArray(new Double[0]));
        for (PassageCandidate passageSpan : passageSpans)
          result.add(passageSpan);
      } catch (SolrServerException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  List<String> ExpandByWordnet(String keyterm) {
    List<String> expand = new ArrayList<String>();

    for (String e : keyterm.split(" ")) {
      for (Synset synset : wordnetDB.getSynsets(e)) {
        for (String wordForm : synset.getWordForms()) {
          if (!e.toLowerCase().equals(wordForm.toLowerCase())) {
            expand.add(keyterm.replaceAll(e, wordForm));
          }
        }
      }
    }
    return expand;
  }

  List<String> ExpandByGene(String keyterm) {

    List<String> expand = new ArrayList<String>();
    for (String e : keyterm.split(" ")) {
      if (isGene(e)) {
        for (String newE : geneSynonymGenerator(e)) {
          if (!e.equals(newE)) {
            if (!expand.contains(keyterm.replaceAll(e, newE)))
              expand.add(keyterm.replaceAll(e, newE));
          }
        }
      }
    }
    return expand;
  }

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
      writer.write("name=" + text + "&species=&taxo=0&source=HGNC&type=gene");
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

  private void pure(char[] s) {
    int start = 0;
    int end = 0;
    while (start != -1 && end != -1) {
      start = end;
      while (start < s.length && s[start] != '<')
        start++;
      if (start >= s.length)
        break;
      end = start + 1;
      while (end < s.length && s[end] != '>')
        end++;
      if (end >= s.length)
        break;

      for (int i = start + 1; i < end; i++)
        if (s[i] == '.')
          s[i] = ' ';
    }
    start = 0;
    end = 0;
    while (start != -1 && end != -1) {
      start = end;
      while (start < s.length && s[start] != '(')
        start++;
      if (start >= s.length)
        break;
      end = start + 1;
      while (end < s.length && s[end] != ')')
        end++;
      if (end >= s.length)
        break;

      for (int i = start + 1; i < end; i++)
        if (s[i] == '.')
          s[i] = ' ';
    }
  }
}