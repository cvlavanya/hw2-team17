package edu.cmu.lti.f12.hw2.hw2_team17.passage;

class PassageSpan {
	public int begin, end;
	public PassageSpan( int begin , int end ) {
		this.begin = begin;
		this.end = end;
	}
	public boolean containedIn ( int begin , int end ) {
		if ( begin <= this.begin && end >= this.end ) {
			return true;
		} else {
			return false;
		}
	}
}
