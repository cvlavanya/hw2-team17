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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ConfidenceChunker;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.PassageCandidate;
import edu.cmu.lti.oaqa.openqa.hello.passage.KeytermWindowScorer;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class ZhenxiangPassageCandidateFinder {
  private String text;
  private String docId;
  
  private int textSize;      // values for the entire text
  private int totalMatches;  
  private int totalKeyterms;
  
  private KeytermWindowScorer scorer;
  
  protected WordNetDatabase wordnetDB;

  protected ConfidenceChunker chunker;
  
  public ZhenxiangPassageCandidateFinder( String docId , String text , KeytermWindowScorer scorer) {
    super();
    this.text = text;
    this.docId = docId;
    this.textSize = text.length();
    this.scorer = scorer;
   
  }
  public List<PassageCandidate> extractPassages( String[] keyterms , Double[] weight) {
    List<List<PassageSpan>> matchingSpans = new ArrayList<List<PassageSpan>>();
    List<PassageSpan> matchedSpans = new ArrayList<PassageSpan>();
    
    double totalWeight = 0;
    double totalKeyWeight = 0;
    
    
    // Find all keyterm matches.
    //for ( String keyterm : expanded ) {
    int i=0; 
    for ( String keyterm : keyterms ) {
      Pattern p = Pattern.compile( keyterm );
      Matcher m = p.matcher( text );
      //List<String> expand = keytermExpand(keyterm);
      
      while ( m.find() ) {
        PassageSpan match = new PassageSpan( m.start() , m.end() ) ;
        matchedSpans.add( match );
        totalMatches++;
        totalWeight += weight[i];
        
      }
     /* for (String key:expand){
        Pattern pa = Pattern.compile( key );
        Matcher ma = pa.matcher( text );
        while ( ma.find() ) {
          PassageSpan match = new PassageSpan( ma.start() , ma.end() ) ;
          matchedSpans.add( match );
          totalMatches++;
        }
      }*/
      if (! matchedSpans.isEmpty() ) {
        //matchingSpans.add( matchedSpans );
        totalKeyterms++;
        totalKeyWeight += weight[i];
      }
      matchingSpans.add( matchedSpans );
      i++;
    }
    /*System.out.println("=============================");
    for (i=0; i<keytermNum.length; i++){
      if (keytermNum[i] != 0)
        System.out.println(keyterms[i] + " " +  keytermNum[i]);
    }*/
    
    // create set of left edges and right edges which define possible windows.
    List<Integer> leftEdges = new ArrayList<Integer>();
    List<Integer> rightEdges = new ArrayList<Integer>();
    for ( List<PassageSpan> keytermMatches : matchingSpans ) {
      for ( PassageSpan keytermMatch : keytermMatches ) {
        Integer leftEdge = keytermMatch.begin;
        Integer rightEdge = keytermMatch.end; 
        if (! leftEdges.contains( leftEdge ))
          leftEdges.add( leftEdge );
        if (! rightEdges.contains( rightEdge ))
          rightEdges.add( rightEdge );
      }
    }
     
    // For every possible window, calculate keyterms found, matches found; score window, and create passage candidate.
    List<PassageCandidate> result = new ArrayList<PassageCandidate>();
    for ( Integer begin : leftEdges ) {
      for ( Integer end : rightEdges ) {
        if ( end <= begin ) continue; 
        // This code runs for each window.
        int keytermsFound = 0;
        int matchesFound = 0;
        //double keytermWeightFound = 0;
        //double weightFound = 0;
        //System.out.println("===========");
        //System.out.println(text.substring(begin,end));
        //begin = leftExpand(begin,text);
        //end = rightExpand(end,text);
        //System.out.println(text.substring(begin,end));
        
        int []passageKeyNum = new int [keyterms.length];
        for (int j=0; j<passageKeyNum.length; j++)
          passageKeyNum[j] = 0;
        int j=0;
        for ( List<PassageSpan> keytermMatches : matchingSpans ) {
          boolean thisKeytermFound = false;
          for ( PassageSpan keytermMatch : keytermMatches ) {
            if ( keytermMatch.containedIn( begin , end ) ){
              passageKeyNum[j]++;
              matchesFound++;
             // weightFound += weight[j];
              thisKeytermFound = true;
            }
          }
          if ( thisKeytermFound ){
            keytermsFound++;
            //keytermWeightFound += weight[j];
          }
          j++;
        }
        double score = scorer.scoreWindow( begin , end , matchesFound , totalMatches , keytermsFound , totalKeyterms , textSize );
        System.out.println("----------------");
        System.out.println(docId + " " + begin + " " + end + " " + score);
        //double score = getScore( begin , end , weightFound , totalWeight , keytermWeightFound , totalKeyWeight , textSize );
        /*System.out.println("!-------------------------------");
        System.out.println(text.substring(begin,end) + " " + score);
        System.out.println("!-------------------------------");*/
        PassageCandidate window = null;
        try {
          window = new PassageCandidate( docId , begin , end , (float) score , null );
        } catch (AnalysisEngineProcessException e) {
          e.printStackTrace();
        }
        result.add( window );
      }
    }
    
    // Sort the result in order of decreasing score.
    // Collections.sort ( result , new PassageCandidateComparator() );
    return result;

  }
  private double getScore ( int begin , int end , double weightFound , double totalWeight , double keytermWeightFound , double totalKeyWeight , int textSize ){
    int windowSize = end - begin;
    double offsetScore = ( (double)textSize - (double)begin ) / (double)textSize;
    return ( .25d * (double)weightFound / (double)totalWeight ) + .25d * ( (double)keytermWeightFound / (double)totalKeyWeight) + .25d * ( 1 - ( (double)windowSize / (double)textSize ) + .25d * offsetScore );
  }
  
  private class PassageCandidateComparator implements Comparator {
    // Ranks by score, decreasing.
    public int compare( Object o1 , Object o2 ) {
      PassageCandidate s1 = (PassageCandidate)o1;
      PassageCandidate s2 = (PassageCandidate)o2;
      if ( s1.getProbability() < s2.getProbability() ) {
        return 1;
      } else if ( s1.getProbability() > s2.getProbability() ) {
        return -1;
      }
      return 0;
    }   
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
  private int leftExpand(int pos, String text){
    /*int left = pos -1 ;
    while (left >= 0 && text.charAt(left) != '>')
      left --;
    if (left < 0)
      return 0;
    else
      return left + 1;*/
    int left;
    int right;
    int tagLength;
    int totalLength;
    double tagRate;
    
    tagRate = 0.0;
    left = pos + 1;
    while (tagRate < 0.5){
      pos = left-1;
      right = pos;
      while (right >= 0 && text.charAt(right) != '>')
        right--;
      if (right < 0)
        break;
      left = right - 1;
      while (left >= 0 && text.charAt(left) != '<')
        left--;
      if (left < 0)
        break;
      totalLength = pos - left;
      tagLength = right - left + 1;
      tagRate = (double)tagLength / totalLength;
      //System.out.println("left "+ totalLength + " " + tagLength + " " + tagRate);
      //System.out.println("ss " + text.substring(left,pos));
    }
    return pos;
  }
  private int rightExpand(int pos, String text){
    /*int right = pos + 1;
    while (right < text.length() && text.charAt(right) != '<')
      right++;
    if (right >= text.length())
      return text.length()-1;
    else 
      return right - 1;*/
    int left;
    int right;
    int tagLength;
    int totalLength;
    double tagRate;
    
    tagRate = 0.0;
    right = pos-1;
    while (tagRate < 0.5){
      pos = right+1;
      left = text.indexOf("<",pos);
      if (left == -1)
        break;
      right = text.indexOf(">",left);
      if (right == -1)
        break;
      totalLength = right - pos;
      tagLength = right - left + 1;
      tagRate = (double)tagLength / totalLength;   
      //System.out.println("right "+ totalLength + " " + tagLength + " " + tagRate);
      //System.out.println("ss " + text.substring(pos,right+1));
    }
    return pos;
  }
}