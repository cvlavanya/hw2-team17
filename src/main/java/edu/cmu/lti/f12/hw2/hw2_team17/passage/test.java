package edu.cmu.lti.f12.hw2.hw2_team17.passage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {
  public static void main(String[] args){
    Pattern pattern = Pattern.compile("[<>.,]");
    String s = "<abc>efg.hij,kkk";
    Matcher matcher = pattern.matcher(s);
    int begin = 0;
    int end = 0;
    while (matcher.find()){
      end = matcher.start();
      if (begin < end)
        System.out.println(s.substring(begin,end));
      begin = matcher.end();
      //System.out.println(matcher.start() + " " + matcher.end() + " " + s.substring(matcher.start(),matcher.end()));
    }
    System.out.println(s.substring(begin,s.length()));
  }
}
