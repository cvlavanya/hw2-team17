package edu.cmu.lti.f12.hw2.hw2_team17.keyterm;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;


import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ConfidenceChunker;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;

public class ZhenxiangKeytermExtractor extends AbstractKeytermExtractor {

  static ConfidenceChunker chunker = null;
  
  @Override
  protected List<Keyterm> getKeyterms(String question) {
    // TODO Auto-generated method stub
    
    question = question.replace('?', ' ');
    question = question.replace('(', ' ');
    question = question.replace('[', ' ');
    question = question.replace(')', ' ');
    question = question.replace(']', ' ');
    question = question.replace('/', ' ');
    question = question.replace('\'', ' ');
    
    List<Keyterm> keyterms = new LinkedList<Keyterm>();
    char[] cs = question.toCharArray();
    Iterator<Chunk> it = chunker.nBestChunks(cs,0,cs.length,10);
    while(it.hasNext())
    {
      Chunk chunk = it.next();
      int begin = chunk.start();
      int end = chunk.end();
      //System.out.println(question.substring(begin,end));
      keyterms.add(new Keyterm(question.substring(begin,end)));    
    }
    return keyterms;
  }
  
  @Override
  public void initialize(UimaContext c)
          throws ResourceInitializationException {
    super.initialize(c);
    
    if (chunker == null)
    {
      String filePath = (String) c.getConfigParameterValue("modelFile");
      File modelFile = new File(filePath);
      try {
        chunker = (ConfidenceChunker) AbstractExternalizable.readObject(modelFile);
        } catch (IOException e) {
     // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (ClassNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
    }
  }
}
