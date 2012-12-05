package edu.cmu.lti.f12.hw2.hw2_team17.passage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.jsoup.Jsoup;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ConfidenceChunker;
import com.aliasi.util.AbstractExternalizable;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.PassageCandidate;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.cmu.lti.oaqa.openqa.hello.passage.KeytermWindowScorerSum;
import edu.cmu.lti.oaqa.openqa.hello.passage.PassageCandidateFinder;
import edu.cmu.lti.oaqa.openqa.hello.passage.SimplePassageExtractor;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class ZhenxiangPassageExtractor extends SimplePassageExtractor {
  protected WordNetDatabase wordnetDB;

  protected ConfidenceChunker chunker;
  
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    System.setProperty("wordnet.database.dir", "src/main/resources/wordnet/");
    wordnetDB = WordNetDatabase.getFileInstance();
    
    String modelPath = "src/main/resources/lingpipeModel/ne-en-bio-genetag.HmmChunker";
    try {
      chunker = (ConfidenceChunker) AbstractExternalizable.readObject(new File(modelPath));
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (ClassNotFoundException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  }

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
    for ( String keyterm : keytermStrings ) {
      if (!expanded.contains(keyterm)){
        expanded.add(keyterm);
        if (isGene(keyterm))
          weight.add(2.0);
        else
          weight.add(1.4);
      }
      for (String ex: ExpandByWordnet(keyterm)){
        if (! expanded.contains(ex)){
          expanded.add(ex);
          weight.add(0.6);
        }
      }
      for (String ex: ExpandByGene(keyterm)){
        if (! expanded.contains(ex)){
          expanded.add(ex);
          weight.add(1.0);
        }
      }
    }
    /*for (int i=0; i< expanded.size(); i++)
      System.out.println(expanded.get(i) + " " + weight.get(i));*/
    
    //System.out.println("expanded keyterms "+expanded.toString());
    
    double[] idfs = new double [expanded.size()];
    for (int i=0; i<expanded.size(); i++)
      idfs[i] = 1.0;
     
    for (RetrievalResult doc : documents){
      String text = null;
      try {
        text = wrapper.getDocText(doc.getDocID());
      } catch (SolrServerException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      for (int i=0; i<expanded.size(); i++){
        if (text.contains(expanded.get(i)))
          idfs[i] = idfs[i] + 1;
      }
    }
    List<String> finalKeys = new ArrayList<String>();
    List<Double> finalIdfs = new ArrayList<Double>();
    List<Double> finalWeight = new ArrayList<Double>();
    for (int i=0; i<expanded.size(); i++){
      if (idfs[i] > 1.5){
        finalKeys.add(expanded.get(i));
        finalIdfs.add(idfs[i]);
        finalWeight.add(weight.get(i));
      }
    }
    for (int i=0; i<finalIdfs.size(); i++)
      finalIdfs.set(i, Math.log(documents.size()/finalIdfs.get(i)));
    
    System.out.println("expanded keyterms "+finalKeys.toString());
      
    
    for (RetrievalResult document : documents) {
      //System.out.println("RetrievalResult: " + document.toString());
      
      String id = document.getDocID();
      try {
        String htmlText = wrapper.getDocText(id);

        // cleaning HTML text
        //String text = Jsoup.parse(htmlText).text().replaceAll("([\177-\377\0-\32]*)", "")/* .trim() */;
        // for now, making sure the text isn't too long
        String text = htmlText.substring(0, Math.min(5000, htmlText.length()));
        char[] textC = text.toCharArray();
        pure(textC);
        text = new String(textC);
        
        /*int start = 0;
        int end = 0;
        
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
        while (start != -1 && end != -1){
          end = text.indexOf(".",start);
          if (end != -1){
            System.out.println("====================");
            String sen = text.substring(start, end);
            System.out.println(sen);
            System.out.println("====================");
          }
          start = end + 2;
        }
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");*/
        
        /*text = text.replaceAll("[(),]", "");
        System.out.println(text);
        String cleantext = Jsoup.parse(text).text().replaceAll("([\177-\377\0-\32]*)", "");
        cleantext = cleantext.replaceAll("[(),]", "");

        System.out.println(cleantext);
        int start = 0;
        int end = 0;
        int tStart = 0;
        int tEnd = 0;
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
        while (start != -1 && end != -1){
          end = cleantext.indexOf(".",start);
          if (end != -1){
            System.out.println("====================");
            String sen = cleantext.substring(start, end);
            System.out.println("SSSSSSSSS" + " " + sen);
            String[] words = sen.split(" ");
            tStart = text.indexOf(words[0],tEnd);
            
            tEnd = tStart + words[0].length();
            for (int i=1; i<words.length; i++)
            {
              System.out.println(words[i] + " " + text.indexOf(words[i],tEnd));
              if (text.indexOf(words[i],tEnd) == -1){
                System.out.println("--" + words[i] + " " + tEnd + text.charAt(tEnd));
                continue;
              }
              tEnd = text.indexOf(words[i],tEnd) + words[i].length() + 1;
            }
            System.out.println(tStart + " " + tEnd);
            System.out.println(text.substring(tStart,tEnd));
            //tStart = tEnd + 1;
            System.out.println("===================="); 
            //double score = getScore(cleantext.substring(start, end),keyterms,weight);   
          }
          start = end + 2;
        }*/

        /*System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println(text);
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("???????????????????????????");
        Pattern pattern = Pattern.compile("[.]");
        String[] sentences = pattern.split(text);
        for (String sen : sentences){
          System.out.println("====================");
          System.out.println(sen);
          System.out.println("====================");  
        }
        System.out.println("???????????????????????");*/
        //System.out.println(text);

        /*ZhenxiangPassageCandidateFinder finder = new ZhenxiangPassageCandidateFinder(id, text,
                new KeytermWindowScorerSum());*/
 
        ZhenxiangSentenceFinder finder = new ZhenxiangSentenceFinder(id, text,
                new KeytermWindowScorerSum());
        
        /*List<PassageCandidate> passageSpans = finder.extractPassages(keytermStrings
                .toArray(new String[0]));*/
       // List<PassageCandidate> passageSpans = finder.extractPassages(expanded.toArray(new String[0]),weight.toArray(new Double[0]));
        List<PassageCandidate> passageSpans = finder.extractPassages(finalKeys.toArray(new String[0]),finalWeight.toArray(new Double[0]));
        for (PassageCandidate passageSpan : passageSpans)
          result.add(passageSpan);
      } catch (SolrServerException e) {
        e.printStackTrace();
      }
    }
    return result;
  }
  List<String> ExpandByWordnet(String keyterm){
    List<String> expand = new ArrayList<String>();
    
     for (String e : keyterm.split(" ")){
       //int count = 0;
       for (Synset synset : wordnetDB.getSynsets(e)){
         //if( count > 1)
           //break;
         for (String wordForm : synset.getWordForms()) {
           if (!e.toLowerCase().equals(wordForm.toLowerCase())) {
             expand.add(keyterm.replaceAll(e, wordForm));
            // count ++;
             //break;
           }
         }
       }
     }
     return expand;
  }

   List<String> ExpandByGene(String keyterm){
     
     List<String> expand = new ArrayList<String>();
      for (String e : keyterm.split(" ")){
        //String e = keyterm;{
        if (isGene(e)) {
          int count = 0;
          for (String newE : geneSynonymGenerator(e)) {
            //if (count > 1)
              //break;
            if (!e.equals(newE)){
              if (!expand.contains(keyterm.replaceAll(e, newE)))
              expand.add(keyterm.replaceAll(e, newE));
              //count ++;
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
        int pre = curStr.indexOf("</span>",end);
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
  private void pure(char[] s){
    int start = 0;
    int end = 0;
    while (start != -1 && end != -1){
      start = end;
      while (start < s.length && s[start] != '<')
        start++;
      if (start >= s.length)
        break;
      end = start + 1;
      while (end < s.length && s[end] != '>')
        end ++;
      if (end >= s.length)
        break;
      
      for (int i=start+1; i< end; i++)
        if (s[i] == '.')
          s[i] = ' ';
     }
    start = 0;
    end = 0;
    while (start != -1 && end != -1){
      start = end;
      while (start < s.length && s[start] != '(')
        start++;
      if (start >= s.length)
        break;
      end = start + 1;
      while (end < s.length && s[end] != ')')
        end ++;
      if (end >= s.length)
        break;
      
      for (int i=start+1; i< end; i++)
        if (s[i] == '.')
          s[i] = ' ';
     }
  }
}
