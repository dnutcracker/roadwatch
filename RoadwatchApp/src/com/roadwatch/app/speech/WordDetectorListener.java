package com.roadwatch.app.speech;

import java.util.List;

/**
 * receive results from a {@link WordActivator}
 */
public interface WordDetectorListener
{
	/**
	 * Returns all the recievedResults from the last detection. (only if the 'magic word' wasn't already heard)
	 * 
	 * @param heard
	 * @param scores
	 */
	public void recieveResults(List<String> heard, float[] scores);
}