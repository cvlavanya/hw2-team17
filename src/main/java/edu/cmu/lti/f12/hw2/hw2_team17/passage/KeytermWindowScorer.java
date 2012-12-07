package edu.cmu.lti.f12.hw2.hw2_team17.passage;

public interface KeytermWindowScorer {
	public double scoreWindow ( int begin , int end , int matchesFound , int totalMatches , int keytermsFound , int totalKeyterms , int textSize );
		
}
