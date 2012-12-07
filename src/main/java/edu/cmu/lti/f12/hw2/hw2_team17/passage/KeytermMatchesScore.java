package edu.cmu.lti.f12.hw2.hw2_team17.passage;

public class KeytermMatchesScore implements KeytermWindowScorer {
	@Override
	public double scoreWindow(int begin, int end, int matchesFound,
			int totalMatches, int keytermsFound, int totalKeyterms,
			int textSize) {
		return (double)keytermsFound / (double)totalKeyterms;
	}
}
