package edu.cmu.lti.f12.hw2.hw2_team17.keyterm;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class YksunKeytermExtractor extends AbstractKeytermExtractor {
  private StanfordCoreNLP pipeline;

  @Override
  public void initialize(UimaContext c) throws ResourceInitializationException {
    super.initialize(c);
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos");
    pipeline = new StanfordCoreNLP(props);
  }

  @Override
  protected List<Keyterm> getKeyterms(String input) {
    List<Keyterm> result = new ArrayList<Keyterm>();
    Annotation document = new Annotation(input);
    pipeline.annotate(document);
    StringBuilder builder = new StringBuilder();
    for (CoreLabel token : document.get(TokensAnnotation.class)) {
      String word = token.get(TextAnnotation.class);
      String pos = token.get(PartOfSpeechAnnotation.class);
      if ((pos.startsWith("POS") || pos.startsWith("NN") || pos.startsWith("JJ"))
              && !inBlackList(word)) {
        if (builder.length() > 0 && !pos.startsWith("POS"))
          builder.append(" ");
        builder.append(word);
      } else if (builder.length() > 0) {
        result.add(new Keyterm(builder.toString()));
        builder = new StringBuilder();
      }
      if (inWhiteList(word))
        result.add(new Keyterm(word));
    }
    if (builder.length() > 0)
      result.add(new Keyterm(builder.toString()));
    return result;
  }

  private boolean inBlackList(String word) {
    return word.equals("role") || word.equals("activity");
  }

  private boolean inWhiteList(String word) {
    return word.equals("affect") || word.equals("regulate");
  }
}
