package edu.cmu.lti.f12.hw2.hw2_team17.keyterm;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.ecd.log.AbstractLoggedComponent;
import edu.cmu.lti.oaqa.framework.BaseJCasHelper;
import edu.cmu.lti.oaqa.framework.QALogEntry;
import edu.cmu.lti.oaqa.framework.data.KeytermList;
import edu.cmu.lti.oaqa.framework.types.InputElement;

//import java.util.ArrayList;
import java.util.List;

import edu.cmu.lti.oaqa.framework.data.Keyterm;



public abstract class AbstractKeytermExtractor extends AbstractLoggedComponent {

protected abstract List<Keyterm> getKeyterms(String question);

@Override
public final void process(JCas jcas) throws
  AnalysisEngineProcessException {
super.process(jcas);
try {
  // prepare input
  InputElement input =
      (InputElement) BaseJCasHelper.getAnnotation(jcas,
          InputElement.type);
  String question = input.getQuestion();
  // do task
  List<Keyterm> keyterms = getKeyterms(question);
  log(keyterms.toString());
  // save output
  KeytermList.storeKeyterms(jcas, keyterms);
} catch (Exception e) {
  throw new AnalysisEngineProcessException(e);
} }

@Override
public void initialize(UimaContext c)
        throws ResourceInitializationException {
  super.initialize(c);
  String p = (String) c.getConfigParameterValue("param1");
}

protected final void log(String message) {
super.log(QALogEntry.KEYTERM, message);
} }
