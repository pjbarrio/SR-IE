package utils;

import java.util.Comparator;
import java.util.Map;

public class MapBasedComparator<T> implements Comparator<T> {

	private Map<T, Double> scores;
	private boolean descending;

	public MapBasedComparator(Map<T,Double> scores, boolean descending) {
		this.scores = scores;
		this.descending = descending;
	}
	
	@Override
	public int compare(T arg0, T arg1) {
		return Double.compare(scores.get(arg0), scores.get(arg1)) * (descending ? -1 : 1);
	}

}
