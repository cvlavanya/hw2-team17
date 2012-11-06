package edu.cmu.lti.f12.hw2.hw2_team17.keyterm;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;

import com.aliasi.util.AbstractExternalizable;



import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;

/**
 * 
 * @author Zi Yang <ziy@cs.cmu.edu>
 * 
 */
public class lavanyaKeytermExtractor extends AbstractKeytermExtractor {

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
  }

  @Override
  protected List<Keyterm> getKeyterms(String question) {

    question = question.replace('?', ' ');
    question = question.replace('(', ' ');
    question = question.replace('[', ' ');
    question = question.replace(')', ' ');
    question = question.replace(']', ' ');
    question = question.replace('/', ' ');
    question = question.replace('\'', ' ');
    List<Keyterm> keyterms = new ArrayList<Keyterm>();


    try
	{
		String fileName="src/main/resources/lingpipeModel/bio-genetag.HmmChunker";
		File modelFile = new File(fileName);
		Chunker chunker = (Chunker) AbstractExternalizable.readObject(modelFile);
		//Get lines from the input document to feed to LingPipe
		 
		
		String termsInSentence[] = question.trim().split("\\s+"); // Regex for one or more spaces
			
		StringBuffer allButSentenceId  = new StringBuffer(); // StringBuffer is like string
		for(int j=1;j<termsInSentence.length;j++){
			allButSentenceId.append(termsInSentence[j]).append(" ");// Attach words followed by single space
			
				
				
			String resultSentence = allButSentenceId.toString(); // Convert stringbuffer back to string
			//System.out.println(resultSentence);
			
			//Annotate each line using LingPipe
			
			Chunking chunking = chunker.chunk(resultSentence);
			Set<Chunk> chunkSet = chunking.chunkSet();
			
			Iterator<Chunk> it = chunkSet.iterator();
			while (it.hasNext()) {
				com.aliasi.chunk.Chunk chunk = (com.aliasi.chunk.Chunk) it.next();
				
				//Extract lower and upper bounds
				int[] limits = new int[2]; // lower limit, upper limit
				String inputTrimmed = chunk.toString().trim(); // Remove surrounding spaces.
				String regexForLimits = "\\d+\\s*-\\s*\\d+";
				Pattern p = Pattern.compile(regexForLimits);
				Matcher m = p.matcher(inputTrimmed);
				int count = 0;
				while (m.find()) {
					String limitSubstr = inputTrimmed.substring(m.start(), m.end());
					String fields[] = limitSubstr.split("-");
					limits[0] = Integer.parseInt(fields[0]);
					limits[1] = Integer.parseInt(fields[1]);
					if (count > 0) {
						System.err.println("More than one match found. Not expected");
					}
					count += 1;
				}
				/*limits[0] has the lower bound and limits[1] has the upper bound.
				 * But this includes spaces. Subtract that.
				 * */
				
				
				String geneTagString=resultSentence.substring(limits[0],limits[1]);
			    keyterms.add(new Keyterm(geneTagString));				
				
			}	
				
		}

	}
	catch(Exception e)
	{
		System.err.println("File not found!");
	}
	
    
    
    

    return keyterms;
  }
}
