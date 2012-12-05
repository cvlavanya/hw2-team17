/*
 *  Copyright 2012 Carnegie Mellon University
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edu.cmu.lti.f12.hw2.hw2_team17.passage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.jsoup.Jsoup;

import com.aliasi.chunk.ConfidenceChunker;

import edu.cmu.lti.oaqa.framework.data.PassageCandidate;
import edu.cmu.lti.oaqa.openqa.hello.passage.KeytermWindowScorer;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class ZhenxiangSentenceFinder {
  private String text;
  private String docId;
  
  private int textSize;      // values for the entire text
  private int totalMatches;  
  private int totalKeyterms;
  
  private KeytermWindowScorer scorer;
  
  protected WordNetDatabase wordnetDB;

  protected ConfidenceChunker chunker;
  
  public ZhenxiangSentenceFinder( String docId , String text , KeytermWindowScorer scorer) {
    super();
    this.text = text;
    this.docId = docId;
    this.textSize = text.length();
    this.scorer = scorer;
   
  }
  public List<PassageCandidate> extractPassages( String[] keyterms , Double[] weight) {
    // For every possible window, calculate keyterms found, matches found; score window, and create passage candidate.
    List<PassageCandidate> result = new ArrayList<PassageCandidate>();
    List<String> sentences = new ArrayList<String>();
    
    int start = 0;
    int end = 0;
    double score;
    
    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
    while (start != -1 && end != -1){
      end = text.indexOf(".",start);
      if (end != -1){
       // System.out.println("====================");
        String sen = text.substring(start, end);
        sentences.add(sen);
        score = getScore(sen, keyterms,weight);
        PassageCandidate window = null;
        try {
          window = new PassageCandidate( docId , start , end , (float) score , null );
        } catch (AnalysisEngineProcessException e) {
          e.printStackTrace();
        }
        result.add( window ); 
        
       // System.out.println(sen);
       // System.out.println("====================");
      }
      start = end + 2;
    }
    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
    
    /*double score;
    for (String sen : sentences){
      score = getScore(sen, keyterms,weight);
      PassageCandidate window = null;
      try {
        window = new PassageCandidate( docId , begin , end , (float) score , null );
      } catch (AnalysisEngineProcessException e) {
        e.printStackTrace();
      }
      result.add( window );
    }*/
    
    return result;

  }
  
  private double getScore(String sen, String[] keyterms , Double[] weight){
    double score = 0;
    for (int i=0; i<keyterms.length; i++){
      Pattern pattern = Pattern.compile(keyterms[i]);
      Matcher match = pattern.matcher(sen);
      while (match.find()){
        score += weight[i];
      }      
    }
    return score;   
  }

  class PassageSpan {
    private int begin, end;
    public PassageSpan( int begin , int end ) {
      this.begin = begin;
      this.end = end;
    }
    public boolean containedIn ( int begin , int end ) {
      if ( begin <= this.begin && end >= this.end ) {
        return true;
      } else {
        return false;
      }
    }
  }
}