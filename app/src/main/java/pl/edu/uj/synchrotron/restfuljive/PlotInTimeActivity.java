package pl.edu.uj.synchrotron.restfuljive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Activity that creates plot of selected scalar attribute. It acquire data every second and add it to plot.
 */
public class PlotInTimeActivity extends CertificateExceptionActivity {
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	RelativeLayout relativeLayout;
	VolleyError e;
	private String attributeName, deviceName, host, port, RESThost, user, pass;
	private Number[] data, dataTmp;
	private Context context = this;
	private ScheduledFuture plotFuture;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restartQueue();
		setContentView(R.layout.activity_plot_in_time);
		Intent i = getIntent();
		if (i.hasExtra("attName")) {
			attributeName = i.getStringExtra("attName");
			Log.d("onCreate", "Received attribute name from intent");
		}
		if (i.hasExtra("devName")) {
			deviceName = i.getStringExtra("devName");
			Log.d("onCreate", "Received device name from intent");
		}
		if (i.hasExtra("host")) {
			host = i.getStringExtra("host");
			Log.d("onCreate", "Received host from intent");
		}
		if (i.hasExtra("port")) {
			port = i.getStringExtra("port");
			Log.d("onCreate", "Received port from intent");
		}
		if (i.hasExtra("RESThost")) {
			RESThost = i.getStringExtra("RESThost");
			Log.d("onCreate", "Received port from intent");
		}
		if (i.hasExtra("user")) {
			user = i.getStringExtra("user");
			Log.d("onCreate", "Received port from intent");
		}
		if (i.hasExtra("pass")) {
			pass = i.getStringExtra("pass");
			Log.d("onCreate", "Received port from intent");
		}
		relativeLayout = (RelativeLayout) findViewById(R.id.plotInTimeRelativeLayout);

		// define thread for refreshing attribute values
		Runnable plotRunnable = new Runnable() {
			@Override
			public void run() {
				JSONObject request = new JSONObject();
				try {
					request.put("attCount", 1);
					request.put("att0", attributeName);
					request.put("attID0", 1);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				final String urlGetStatus = RESThost + "/Tango/rest/" + host + ":" + port +
						"/Device/" + deviceName + "/read_attributes";
				HeaderJsonObjectRequest jsObjRequestStatus =
						new HeaderJsonObjectRequest(Request.Method.PUT, urlGetStatus, request,
								new Response.Listener<JSONObject>() {
									@Override
									public void onResponse(JSONObject response) {
										try {
											Log.d("rStatus.run()", "Received response" + response.toString());
											if (response.getString("connectionStatus").equals("OK")) {
												Log.d("rStatus.run()", "Device connection OK / Method PUT /  received status");
												Log.d("rStatus.run()", "From host: " + urlGetStatus);
												if (data == null) {
													data = new Number[1];
													data[0] = response.getInt("attValue0");
												} else {
													dataTmp = new Number[data.length];
													dataTmp = data;
													data = new Number[dataTmp.length + 1];
													for (int i = 0; i < dataTmp.length; i++) {
														data[i] = dataTmp[i];
													}
													data[dataTmp.length] = response.getInt("attValue0");
													updatePlot();
												}
											} else {
												Log.d("rStatus.run()", "Tango database API returned message:");
												Log.d("rStatus.run()", response.getString("connectionStatus"));
											}
										} catch (JSONException e) {
											Log.d("rStatus.run()", "Problem with JSON object");
											e.printStackTrace();
										}
									}
								}, new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								jsonRequestErrorHandler(error);
							}
						}, user, pass);
				jsObjRequestStatus.setShouldCache(false);
				queue.add(jsObjRequestStatus);
			}
		};
		plotFuture = scheduler.scheduleAtFixedRate(plotRunnable, 1000, 1000, MILLISECONDS);
	}

	/**
	 * Refresh plot with new data.
	 */
	public void updatePlot() {
		Log.d("updatePlot", "Updating plot");
		Log.d("updatePlot", "Received data: " + data.toString());
		for (int i = 0; i < data.length; i++) {
			Log.d("updatePlot", "" + data[i]);
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// initialize our XYPlot reference:
				//XYPlot plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
				XYPlot plot = new XYPlot(context, "");
				//Log.d("updatePlot", "Found widget");
				relativeLayout.removeAllViews();
				relativeLayout.addView(plot);
				//Log.d("updatePlot", "Cleared plot");
				String s = new String("Data to plot: ");
				Number min = data[0];
				Number max = data[0];
				for (Number i : data) {
					s = s + data.toString() + ", ";
					if (min.doubleValue() > i.doubleValue()) {
						min = i;
					}
					if (max.doubleValue() < i.doubleValue()) {
						max = i;
					}
				}
				Log.d("updatePlot", s);
				//plot.clear();
				XYSeries series1 = new SimpleXYSeries(Arrays.asList(data), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "");

				LineAndPointFormatter series1Format = new LineAndPointFormatter();
				series1Format.setPointLabelFormatter(new PointLabelFormatter());
				series1Format.configure(getApplicationContext(), R.xml.line_point_formatter_with_plf1);
				plot.addSeries(series1, series1Format);
				plot.setTicksPerRangeLabel(3);

				plot.getGraphWidget().setDomainLabelOrientation(-45);
				plot.getLegendWidget().setTableModel(new DynamicTableModel(1, 1));
				plot.getLegendWidget().position(10, XLayoutStyle.ABSOLUTE_FROM_LEFT, 10, YLayoutStyle.ABSOLUTE_FROM_BOTTOM,
						AnchorPosition.LEFT_BOTTOM);
				plot.getLegendWidget().setSize(new SizeMetrics(55, SizeLayoutType.ABSOLUTE, 100, SizeLayoutType.FILL));

				min = min.doubleValue() - 1;
				plot.setRangeBottomMax(min);
				plot.setRangeBottomMin(min);
				max = max.doubleValue() + 1;
				plot.setRangeTopMin(max);
				plot.setRangeTopMax(max);
				plot.getGraphWidget().setPaddingTop(10);
			}
		});
	}

	@Override
	public void onDestroy() {
		if (plotFuture != null) {
			plotFuture.cancel(true);
		}
		super.onDestroy();
	}

	/**
	 * Method displaying info about connection error
	 *
	 * @param error Error tah caused exception
	 */
	public void jsonRequestErrorHandler(VolleyError error) {
		e = error;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// Print error message to LogcCat
				Log.d("jsonRequestErrorHandler", "Connection error!");
				e.printStackTrace();

				// show dialog box with error message
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(e.toString()).setTitle("Connection error!")
						.setPositiveButton(getString(R.string.ok_button), null);
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		});
	}
}
