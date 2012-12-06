package edu.cmu.lti.f12.hw2.hw2_team17.passage;

public class deleteTags {
  /*public static void main(String[] args){
    String input = args[0];
    int start = pos(0,input);
    System.out.println(input.substring(start));
  }
  private static int pos(int start, String s){
    int tagBegin;
    int tagEnd;
    int tagLength;
    int curLength;
    double tagRate;
    
    tagRate = 1.0;
    tagEnd = start - 1;
    while (tagRate > 0.7){
    start = tagEnd + 1;
    int noSpace = start;
    while( noSpace < s.length() && (s.charAt(noSpace) == ' ' || s.charAt(noSpace) == '\n' ))
      noSpace++;
    if (noSpace >= s.length())
      break;
    start = noSpace;
    tagBegin = s.indexOf("<",start);
    tagEnd = s.indexOf(">",tagBegin);
    if (tagBegin == -1 || tagEnd == -1)
      return start;
    tagLength = tagEnd - tagBegin + 1;
    curLength = tagEnd - start + 1;
    tagRate = (double)tagLength / curLength;
    }

    return start;
  }*/
  public int pos(int start, String s){
    int tagBegin;
    int tagEnd;
    int tagLength;
    int curLength;
    double tagRate;
    
    tagRate = 1.0;
    tagEnd = start - 1;
    while (tagRate > 0.7){
    start = tagEnd + 1;
    int noSpace = start;
    while( noSpace < s.length() && (s.charAt(noSpace) == ' ' || s.charAt(noSpace) == '\n' ))
      noSpace++;
    if (noSpace >= s.length())
      break;
    start = noSpace;
    tagBegin = s.indexOf("<",start);
    tagEnd = s.indexOf(">",tagBegin);
    if (tagBegin == -1 || tagEnd == -1)
      return start;
    tagLength = tagEnd - tagBegin + 1;
    curLength = tagEnd - start + 1;
    tagRate = (double)tagLength / curLength;
    }

    return start;
  }


}
