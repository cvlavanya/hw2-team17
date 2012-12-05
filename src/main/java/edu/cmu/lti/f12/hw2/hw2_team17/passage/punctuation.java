package edu.cmu.lti.f12.hw2.hw2_team17.passage;

public class punctuation {
  public static void main(String[] args){
    String input = args[0];
    char [] s = input.toCharArray();
    pure(s);
    System.out.println(s);
    
  }
  public static void pure(char[] s){
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
  }
}
