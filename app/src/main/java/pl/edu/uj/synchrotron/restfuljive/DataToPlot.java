package pl.edu.uj.synchrotron.restfuljive;

/**
 * Created by lukasz on 10.05.15. This file is element of RESTful Jive application project. You are free to use, copy and
 * edit whole application or any of its components. Application comes with no warranty. Although author is trying to make it
 * best, it may work or it may not.
 */

import java.io.Serializable;

/**
 * Class for storing data to plot as array of doubles.
 */
class DataToPlot implements Serializable {
	private double[] data;

	/**
	 * @param i
	 * 		Array of values to be plotted.
	 */
	public DataToPlot(double[] i) {
		data = i;
	}

	/**
	 * Get data to be plotted as array of doubles.
	 *
	 * @return Data to be plotted.
	 */
	public double[] getData() {
		System.out.println("Extracting data from Object");
		return data;
	}
}