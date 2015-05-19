package pl.edu.uj.synchrotron.restfuljive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

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

import java.util.Arrays;

/**
 * Activity for creating plot from one dimensional array.
 */
public class PlotActivity extends Activity {
	private double[] doubleToPlot;

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
		Number[] series1Numbers = new Number[doubleToPlot.length];
		System.out.println("Have " + doubleToPlot.length + " elements");
		for (int j = 0; j < doubleToPlot.length; j++) {
			series1Numbers[j] = Double.valueOf(doubleToPlot[j]);
			System.out.println("Data to plot: " + series1Numbers[j]);
		}

		setContentView(R.layout.activity_plot);

		// initialize our XYPlot reference:
		XYPlot plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

		XYSeries series1;
		if (i.hasExtra("domainLabel")) {
			String domainLabel = i.getStringExtra("domainLabel");
			series1 = new SimpleXYSeries(Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, domainLabel);
		} else {
			series1 = new SimpleXYSeries(Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "");
		}
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
		plot.getLegendWidget().setTableModel(new DynamicTableModel(1, 1));
		plot.getLegendWidget().position(10, XLayoutStyle.ABSOLUTE_FROM_LEFT, 10, YLayoutStyle.ABSOLUTE_FROM_BOTTOM,
				AnchorPosition.LEFT_BOTTOM);
		plot.getLegendWidget().setSize(new SizeMetrics(55, SizeLayoutType.ABSOLUTE, 100, SizeLayoutType.FILL));

	}

}
