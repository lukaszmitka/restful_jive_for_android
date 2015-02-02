package pl.edu.uj.synchrotron.restfuljive;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Activity for creating plot from one dimensional array.
 */
public class PlotActivity extends Activity {
	private Number[] series1Numbers;
	private double[] doubleToPlot;
	private XYPlot plot;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		if (i.hasExtra("data")) {
			System.out.println("Starting to extract data from intent");
			DataToPlot dataToPlot = (DataToPlot) i.getSerializableExtra("data");
			doubleToPlot = dataToPlot.getData();
			System.out.println("Finished extracting data from intent");
		}
		series1Numbers = new Number[doubleToPlot.length];
		System.out.println("Have " + doubleToPlot.length + " elements");
		for (int j = 0; j < doubleToPlot.length; j++) {
			series1Numbers[j] = Double.valueOf(doubleToPlot[j]);
			System.out.println("Data to plot: " + series1Numbers[j]);
		}

		// fun little snippet that prevents users from taking screenshots
		// on ICS+ devices :-)
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

		setContentView(R.layout.activity_plot);

		// initialize our XYPlot reference:
		plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

		// Turn the above arrays into XYSeries':
		XYSeries series1;
		if (i.hasExtra("domainLabel")) {
			String domainLabel = i.getStringExtra("domainLabel");
			series1 = new SimpleXYSeries(Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
					domainLabel);
		} else {
			series1 = new SimpleXYSeries(Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "");
		}
		// SimpleXYSeries takes a List so turn our array into a List
		// Y_VALS_ONLY means use the element index as the x value
		// Set the display title of the series

		// Create a formatter to use for drawing a series using
		// LineAndPointRenderer
		// and configure it from xml:
		LineAndPointFormatter series1Format = new LineAndPointFormatter();
		series1Format.setPointLabelFormatter(new PointLabelFormatter());
		series1Format.configure(getApplicationContext(), R.xml.line_point_formatter_with_plf1);

		// add a new series' to the xyplot:
		plot.addSeries(series1, series1Format);

		// reduce the number of range labels
		plot.setTicksPerRangeLabel(3);

		// add plot title if was set in intent
		if (i.hasExtra("plotTitle")) {
			String title = i.getStringExtra("plotTitle");
			plot.setTitle(title);
		}

		plot.getGraphWidget().setDomainLabelOrientation(-45);
		// plot.getGraphWidget().position(10, XLayoutStyle.ABSOLUTE_FROM_LEFT, 10,
		// YLayoutStyle.ABSOLUTE_FROM_TOP,
		// AnchorPosition.LEFT_TOP);
		// plot.getGraphWidget().setSize(new SizeMetrics(100, SizeLayoutType.FILL,
		// 80, SizeLayoutType.FILL));
		// use a 2x2 grid:
		plot.getLegendWidget().setTableModel(new DynamicTableModel(1, 1));

		// adjust the legend size so there is enough room
		// to draw the new legend grid:

		// add a semi-transparent black background to the legend
		// so it's easier to see overlaid on top of our plot:
		// Paint bgPaint = new Paint();
		// bgPaint.setColor(Color.BLACK);
		// bgPaint.setStyle(Paint.Style.FILL);
		// bgPaint.setAlpha(140);
		// plot.getLegendWidget().setBackgroundPaint(bgPaint);

		// adjust the padding of the legend widget to look a little nicer:
		// plot.getLegendWidget().setPadding(1, 1, 1, 1);

		// reposition the grid so that it rests above the bottom-left
		// edge of the graph widget:

		plot.getLegendWidget().position(10, XLayoutStyle.ABSOLUTE_FROM_LEFT, 10, YLayoutStyle.ABSOLUTE_FROM_BOTTOM,
				AnchorPosition.LEFT_BOTTOM);
		plot.getLegendWidget().setSize(new SizeMetrics(55, SizeLayoutType.ABSOLUTE, 100, SizeLayoutType.FILL));

	}

}

/**
 * Class for storing data to plot as array of doubles.
 */
class DataToPlot implements Serializable {
	private double[] data;

	/**
	 * @param i Array of values to be plotted.
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