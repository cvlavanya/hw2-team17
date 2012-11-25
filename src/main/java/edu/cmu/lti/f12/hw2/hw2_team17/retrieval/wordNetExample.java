package edu.cmu.lti.f12.hw2.hw2_team17.retrieval;

//import resources.wordnet.WordNet-3.0;
import edu.smu.tspell.wordnet.*;

import java.util.*;
/**
 * Displays word forms and definitions for synsets containing the word form
 * specified on the command line. To use this application, specify the word
 * form that you wish to view synsets for, as in the following example which
 * displays all synsets containing the word form "airplane":
 * <br>
 * java TestJAWS airplane
 */
public class wordNetExample
{

	/**
	 * Main entry point. The command-line arguments are concatenated together
	 * (separated by spaces) and used as the word form to look up.
	 */
	public static void main(String[] args)
	{
			System.setProperty("wordnet.database.dir", "src/main/resources/wordnet/WordNet-3.0/dict/");
			

			
			String wordForm = "ate";
			StringBuffer synonyms=new StringBuffer();
			//  Get the synsets containing the wrod form
			WordNetDatabase database = WordNetDatabase.getFileInstance();
			Synset[] synsets = database.getSynsets(wordForm);
			//  Display the word forms and definitions for synsets retrieved
			if (synsets.length > 0)
			{
				/*System.out.println("The following synsets contain '" +
						wordForm + "' or a possible base form " +
						"of that text:");*/
				for (int i = 0; i < synsets.length; i++)
				{
					System.out.println("");
					String[] wordForms = synsets[i].getWordForms();
					for (int j = 0; j < wordForms.length; j++)
					{
						//System.out.print((j > 0 ? ", " : "") +wordForms[j]);
						synonyms.append(wordForms[j]+",");
						
							
					} //End of for
					
				}
				String syn=synonyms.toString();
				String synArray[]=syn.split(",");
				Set<String> strSet = new LinkedHashSet<String>();
			    for (String str : synArray) {
			        strSet.add(str);
			    }

			    StringBuffer sb = new StringBuffer();
			    for (String str : strSet) {
			        sb.append(str+",");
			    }
			    System.out.println("\nOutput:"+sb.toString());
			}
			else
			{
				System.err.println("No synsets exist that contain " +
						"the word form '" + wordForm + "'");
			}
		
	}

}