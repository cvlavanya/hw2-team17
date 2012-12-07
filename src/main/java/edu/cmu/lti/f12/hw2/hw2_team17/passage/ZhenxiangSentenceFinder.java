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
    //List<String> sentences = new ArrayList<String>();
    List<Integer> begins = new ArrayList<Integer>();
    List<Integer> ends = new ArrayList<Integer>();

    
    /*int start = 0;
    int end = 0;
    double score;
    deleteTags deTags = new deleteTags();
    
    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
    while (start != -1 && end != -1){
      end = text.indexOf(".",start);
      if (end != -1){
        start = deTags.pos(start, text.substring(0,end));
       // System.out.println("====================");
        String sen = text.substring(start, end);
        //sentences.add(sen);
        begins.add(start);
        ends.add(end);
      }
      start = end + 2;
    }*/
    double score;
    Pattern pattern = Pattern.compile("[<>.,]");
    Matcher matcher = pattern.matcher(text);
    int begin = 0;
    int end = 0;
    while (matcher.find()){
      end = matcher.start();
      if (begin < end){
        //System.out.println(text.substring(begin,end));
        begins.add(begin);
        ends.add(end);
      }
      begin = matcher.end();
    }
    //System.out.println(text.substring(begin,text.length()));
    begins.add(begin);
    ends.add(text.length());
    
    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
    double totalKeyScore = getTotalScore(text,keyterms,weight);
    //System.out.println(totalKeyScore);
    if (totalKeyScore == 0)
      totalKeyScore = 1;    
    for (int i=0; i<begins.size(); i++){
      for (int j=i; j<begins.size(); j++){
        if (j-i>0)
          break;
        int b = begins.get(i);
        int en = ends.get(j);
        score = getScore(text.substring(b,en),keyterms,weight,totalKeyScore,text.length());
        if (score == 0.0)
          continue;
        //System.out.println("----------------");
        //System.out.println(docId + " " + b + " " + en + " " + text.substring(b,en) + " " + score);
        PassageCandidate window = null;
        try {
          window = new PassageCandidate( docId , b , en , (float) score , null );
        } catch (AnalysisEngineProcessException e) {
          e.printStackTrace();
        }
        result.add( window );   
      }
    }
    
    return result;

  }

  private double getTotalScore(String sen, String[] keyterms , Double[] weight){
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
  private double getScore(String sen, String[] keyterms , Double[] weight, double totalScore,
          int textLength){
    double score = 0;
    for (int i=0; i<keyterms.length; i++){
      Pattern pattern = Pattern.compile(keyterms[i]);
      Matcher match = pattern.matcher(sen);
      while (match.find()){
        score += weight[i];
      }      
    }
    score /= totalScore;
    /*System.out.println("---------");
    System.out.println(totalScore);
    System.out.println(sen.length());
    System.out.println(score + " " + (1- (double)sen.length()/textLength) + " "  
    + (1- sen.length()/textLength) * score);*/
    score = (1- (double)sen.length()/textLength) * score;
    
    
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