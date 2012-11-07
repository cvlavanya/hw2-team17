package edu.cmu.lti.f12.hw2.hw2_team17.keyterm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ConfidenceChunker;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;

public class YksunKeytermExtractor extends AbstractKeytermExtractor {
  /**
   * Name of configuration parameter that must be set to the path of the model file.
   */
  public static final String PARAM_MODELFILE = "ModelFile";

  /**
   * Name of configuration parameter that must be set to the Maximum N Best Chunks.
   */
  public static final String PARAM_MAXN = "MAX_N_BEST_CHUNKS";

  /**
   * Name of configuration parameter that must be set to the Confidence Acceptance Level.
   */
  public static final String PARAM_THRESHOLD = "Threshold";

  private ConfidenceChunker chunker;

  private int maxN;

  private Double threshold;

  @Override
  public void initialize(UimaContext c) throws ResourceInitializationException {
    super.initialize(c);
    try {
      String modelPath = (String) c.getConfigParameterValue(PARAM_MODELFILE);
      maxN = (Integer) c.getConfigParameterValue(PARAM_MAXN);
      threshold = Double.parseDouble((String) c.getConfigParameterValue(PARAM_THRESHOLD));
      chunker = (ConfidenceChunker) AbstractExternalizable.readObject(new File(modelPath));
    } catch (IOException e) {
      throw new ResourceInitializationException();
    } catch (ClassNotFoundException e) {
      throw new ResourceInitializationException();
    }
  }

  @Override
  protected List<Keyterm> getKeyterms(String input) {
    char[] cs = input.toCharArray();
    List<Keyterm> result = new ArrayList<Keyterm>();
    Iterator<Chunk> iter = chunker.nBestChunks(cs, 0, cs.length, maxN);
    while (iter.hasNext()) {
      Chunk chunk = iter.next();
      double conf = Math.pow(2.0, chunk.score());
      String gene = input.substring(chunk.start(), chunk.end());
      if (conf > threshold && gene.length() > 1 && isComplete(gene))
        result.add(new Keyterm(gene));
    }
    return result;
  }

  /**
   * Determine if the input text is a complete gene mention
   * 
   * @param text
   *          String that will be processed in this method.
   * @return the boolean that indicates the true/false value.
   */
  private boolean isComplete(String text) {
    int left = text.indexOf("(");
    int right = text.indexOf(")");

    return (left != -1 && right > left) || (left == -1 && right == -1);
  }

}
