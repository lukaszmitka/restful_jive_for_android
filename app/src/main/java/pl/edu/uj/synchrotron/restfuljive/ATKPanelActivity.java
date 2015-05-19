package pl.edu.uj.synchrotron.restfuljive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import fr.esrf.TangoDs.TangoConst;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Class for creating ATKPanel activity. This activity is listing all device attributes and monitor their values.
 */
public class ATKPanelActivity extends CertificateExceptionActivity implements TangoConst {
	private static final int DEFAULT_REFRESHING_PERIOD = 1000;
	private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
	private final Context context = this;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private int refreshingPeriod = DEFAULT_REFRESHING_PERIOD;
	private List<String> commandArray;
	private List<String> commandInTypeArray;
	private List<String> scalarAttrbuteArray;
	private List<String> nonScalarAttributeArray;
	private boolean[] attributeWritableArray;
	private String plotAttributeName;
	private String deviceName;
	private String restHost;
	private String tangoHost;
	private String tangoPort;
	private int[][] ids;
	private int numberOfScalars;
	private Runnable currentRunnable, rAttributes, rStatus, rAttributePlot;
	private ScheduledFuture attributeFuture, statusFuture;
	private boolean firstSelection;
	private Number[] series1Numbers;
	private XYPlot plot;
	private int plotHeight, plotWidth;
	private ScrollView scrollView;
	private RelativeLayout relativeLayout;
	private int maxId, minId, plotId, scaleId;
	private String userName, userPassword;

	/**
	 * Generate a value suitable for use in setId This value will not collide with ID values generated at build time by aapt
	 * for R.id.
	 *
	 * @return a generated ID value
	 */
	public static int generateViewId() {
		for (; ; ) {
			final int result = sNextGeneratedId.get();
			// aapt-generated IDs have the high byte nonzero; clamp to the range under that.
			int newValue = result + 1;
			if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
			if (sNextGeneratedId.compareAndSet(result, newValue)) {
				return result;
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		firstSelection = true;
		setContentView(R.layout.activity_atkpanel);
		//StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
		//StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old).permitNetwork().build());

		scrollView = (ScrollView) findViewById(R.id.scrollView);
		relativeLayout = (RelativeLayout) findViewById(R.id.atkPanel_innerLayout);
		plotId = generateViewId();
		scaleId = generateViewId();
		maxId = generateViewId();
		minId = generateViewId();

		restartQueue();

		// gett device name from intent if was set
		Intent i = getIntent();
		Log.v("onCreate()", "Got intent");
		if (i.hasExtra("DEVICE_NAME")) {
			Log.d("onCreate()", "Got device name from intent");
			deviceName = i.getStringExtra("DEVICE_NAME");
			setTitle(getString(R.string.title_activity_atkpanel) + " : " + deviceName);

		} else { // prompt user for device name
			Log.d("onCreate()", "Requesting device name from user");
			setDeviceName();
		}

		// check if tango host and rest address is saved in config, else prompt user for it
		if (i.hasExtra("restHost") && i.hasExtra("tangoHost") && i.hasExtra("tangoPort")) {
			Log.d("onCreate()", "Got host from intent");
			restHost = i.getStringExtra("restHost");
			tangoHost = i.getStringExtra("tangoHost");
			tangoPort = i.getStringExtra("tangoPort");
			userName = i.getStringExtra("userName");
			userPassword = i.getStringExtra("userPass");
			populatePanel();
		} else {
			Log.d("onCreate()", "Request host from user");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			String settingsRestHost = settings.getString("RESTfulTangoHost", "");
			String settingsTangoHost = settings.getString("TangoHost", "");
			String settingsTangoPort = settings.getString("TangoPort", "");
			Log.d("onCreate()", "Found RESTful host: " + settingsRestHost);
			Log.d("onCreate()", "Found Tango host: " + settingsTangoHost);
			Log.d("onCreate()", "Found Tango port: " + settingsTangoPort);
			if (settingsRestHost.equals("") || settingsTangoHost.equals("") || settingsTangoPort.equals("")) {
				Log.d("ATK Panel onCreate", "Requesting new tango host,port and RESTful host");
				setHost();
			} else {
				restHost = settingsRestHost;
				tangoHost = settingsTangoHost;
				tangoPort = settingsTangoPort;
				Log.d("onCreate()", "Populating panel from server:  " + restHost + "at Tango Host: " +
						settingsTangoHost + ":" + settingsTangoPort);
				populatePanel();
			}
		}

		// define thread for refreshing attribute values
		rAttributes = new Runnable() {
			@Override
			public void run() {
				JSONObject request = new JSONObject();
				try {
					request.put("attCount", numberOfScalars);
					for (int i = 0; i < numberOfScalars; i++) {
						request.put("att" + i, scalarAttrbuteArray.get(i));
						request.put("attID" + i, ids[i][2]);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				final String urlReadAttributesQuery = restHost + "/Tango/rest/" + tangoHost + ":" + tangoPort +
						"/Device/" + deviceName + "/read_attributes";
				HeaderJsonObjectRequest jsObjRequestReadAttributes =
						new HeaderJsonObjectRequest(Request.Method.PUT, urlReadAttributesQuery, request,
								new Response.Listener<JSONObject>() {
									@Override
									public void onResponse(JSONObject response) {
										try {
											if (response.getString("connectionStatus").equals("OK")) {
												Log.d("rAttibutes.run()", "Device connection OK / method PUT / got attribute values");
												Log.d("rAttibutes.run()", "From host: " + urlReadAttributesQuery);
												updateScalarListView(response);
											} else {
												Log.d("rAttibutes.run()", "Tango database API returned message:");
												Log.d("rAttibutes.run()", response.getString("connectionStatus"));
											}
										} catch (JSONException e) {
											Log.d("Runnable run()", "Problem with JSON object");
											e.printStackTrace();
										}
									}
								}, new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								jsonRequestErrorHandler(error);
							}
						}, userName, userPassword);
				//queue.getCache().clear();
				jsObjRequestReadAttributes.setShouldCache(false);
				queue.add(jsObjRequestReadAttributes);
			}
		};

		rAttributePlot = new Runnable() {
			@Override
			public void run() {
				Log.d("rAttributePlot.run()", "Sending plot request");
				String url = restHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/plot_attribute/" + plotAttributeName;
				System.out.println("Sending JSON request");
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										Log.d("rAttributePlot.run()", "Device connection OK");
										String dataFormat = response.getString("dataFormat");
										String plotLabel = response.getString("plotLabel");
										switch (dataFormat) {
											case "SPECTRUM":
												plot = new XYPlot(context, "");
												plotHeight = scrollView.getHeight();
												plot.setMinimumHeight(plotHeight);
												Log.d("populateAttributePlot()", "Plot height set to: " + plotHeight);
												relativeLayout.removeAllViews();
												relativeLayout.addView(plot);
												JSONArray array = response.getJSONArray("plotData");
												series1Numbers = new Number[array.length()];
												Log.v("rAttributePlot.run()", "Have " + series1Numbers.length + " elements");
												for (int j = 0; j < series1Numbers.length; j++) {
													series1Numbers[j] = array.getDouble(j);
													Log.v("rAttributePlot.run()", "Data to plot: " + series1Numbers[j]);
												}

												XYSeries series1;
												series1 = new SimpleXYSeries(Arrays.asList(series1Numbers),
														SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, plotLabel);

												LineAndPointFormatter series1Format = new LineAndPointFormatter();
												series1Format.setPointLabelFormatter(new PointLabelFormatter());
												series1Format.configure(getApplicationContext(), R.xml.line_point_formatter_with_plf1);

												// add a new series' to the xyplot:
												plot.addSeries(series1, series1Format);
												plot.setTicksPerRangeLabel(3);
												plot.getGraphWidget().setDomainLabelOrientation(-45);
												plot.getLegendWidget().setTableModel(new DynamicTableModel(1, 1));
												plot.getLegendWidget().position(10, XLayoutStyle.ABSOLUTE_FROM_LEFT, 10,
														YLayoutStyle.ABSOLUTE_FROM_BOTTOM, AnchorPosition.LEFT_BOTTOM);
												plot.getLegendWidget()
														.setSize(new SizeMetrics(55, SizeLayoutType.ABSOLUTE, 100, SizeLayoutType.FILL));
												break;
											case "IMAGE":
												TextView textViewMaxValue = prepareTextViewMaxValue(maxId);
												TextView textViewMinValue = prepareTextViewMinValue(minId);
												ImageView imageViewScale = prepareScaleImageView(maxId, scaleId);
												ImageView imageViewPlot = preparePlotImageView(scaleId, plotId);
												plotHeight = scrollView.getHeight();
												plotWidth = scrollView.getWidth() - 50;
												if (plotHeight < plotWidth) {
													imageViewPlot.setMinimumHeight(plotHeight);
													imageViewPlot.setMinimumWidth(plotHeight);
												} else {
													imageViewPlot.setMinimumHeight(plotWidth);
													imageViewPlot.setMinimumWidth(plotWidth);
												}

												imageViewScale.setMinimumWidth(20);
												imageViewScale.setMinimumHeight(plotHeight);
												Log.d("rAttributePlot.run() IM", "Plot height: " + plotHeight);
												imageViewScale.setScaleType(ImageView.ScaleType.FIT_XY);

												relativeLayout.removeAllViews();
												relativeLayout.addView(textViewMaxValue);
												relativeLayout.addView(textViewMinValue);
												relativeLayout.addView(imageViewPlot);
												relativeLayout.addView(imageViewScale);

												int rows = response.getInt("rows");
												int cols = response.getInt("cols");
												JSONArray oneDimArray;
												double[][] ivalues = new double[rows][cols];
												for (int i = 0; i < rows; i++) {
													oneDimArray = response.getJSONArray("row" + i);
													for (int j = 0; j < cols; j++) {
														ivalues[i][j] = oneDimArray.getDouble(j);
													}
												}

												Bitmap b = Bitmap.createBitmap(ivalues.length, ivalues[0].length, Bitmap.Config
														.RGB_565);
												double minValue = ivalues[0][0];
												double maxValue = ivalues[0][0];
												for (int i = 0; i < ivalues.length; i++) {
													for (int j = 0; j < ivalues[0].length; j++) {
														if (minValue > ivalues[i][j]) {
															minValue = ivalues[i][j];
														}
														if (maxValue < ivalues[i][j]) {
															maxValue = ivalues[i][j];
														}
													}
												}
												System.out.println("Min: " + minValue + "Max: " + maxValue);
												double range = maxValue - minValue;
												float step = (float) (330 / range);

												int color;
												float hsv[] = {0, 1, 1};

												for (int i = 1; i < ivalues.length; i++) {
													for (int j = 1; j < ivalues[0].length; j++) {
														hsv[0] = 330 - (float) (ivalues[i][j] * step);
														color = Color.HSVToColor(hsv);
														b.setPixel(ivalues.length - i, ivalues[0].length - j, color);
													}
												}
												imageViewPlot.setImageBitmap(b);

												textViewMaxValue.setText("" + maxValue);
												textViewMinValue.setText("" + minValue);
												Bitmap scaleBitmap = Bitmap.createBitmap(20, 330, Bitmap.Config.RGB_565);
												for (int j = 0; j < 330; j++) {
													hsv[0] = j;
													color = Color.HSVToColor(hsv);
													for (int k = 0; k < 20; k++) {
														scaleBitmap.setPixel(k, j, color);
													}
												}
												imageViewScale.setImageBitmap(scaleBitmap);
												imageViewScale.setScaleType(ImageView.ScaleType.FIT_XY);
												break;
										}
									} else {
										AlertDialog.Builder builder = new AlertDialog.Builder(context);
										String res = response.getString("connectionStatus");
										System.out.println("Tango database API returned message:");
										System.out.println(res);
										builder.setTitle("Reply");
										builder.setMessage(res);
										AlertDialog dialog = builder.create();
										dialog.show();
									}
								} catch (JSONException e) {
									System.out.println("Problem with JSON object");
									e.printStackTrace();
								}
							}
						}, new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								jsonRequestErrorHandler(error);
							}
						}, userName, userPassword);
				jsObjRequest.setShouldCache(false);
				queue.add(jsObjRequest);
			}
		};

		// define thread for refreshing attribute values
		rStatus = new Runnable() {
			@Override
			public void run() {

				final String urlGetStatus = restHost + "/Tango/rest/" + tangoHost + ":" + tangoPort +
						"/Device/" + deviceName + "/command_inout/Status/DevVoidArgument";
				HeaderJsonObjectRequest jsObjRequestStatus =
						new HeaderJsonObjectRequest(Request.Method.PUT, urlGetStatus, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										Log.d("rStatus.run()", "Device connection OK / Method PUT /  received status");
										Log.d("rStatus.run()", "From host: " + urlGetStatus);
										populateStatus(response);
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
						}, userName, userPassword);
				jsObjRequestStatus.setShouldCache(false);
				queue.add(jsObjRequestStatus);
			}
		};
		statusFuture = scheduler.scheduleAtFixedRate(rStatus, refreshingPeriod, refreshingPeriod, MILLISECONDS);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_atkpanel, menu);
		return true;
	}

	/**
	 * This method prepares ImageView widget for displaying plat as an image.
	 *
	 * @param scaleID
	 * 		ID number of scale widget, used to define layout relations.
	 * @param plotId
	 * 		ID number of returned widget.
	 *
	 * @return widget with defined relations, ready to display plot.
	 */
	private ImageView preparePlotImageView(int scaleID, int plotId) {
		ImageView plotImageView = new ImageView(context);
		plotImageView.setId(plotId);
		// create layout
		RelativeLayout.LayoutParams plotImageViewLayParam =
				new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		// add layout params
		plotImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		plotImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		plotImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		//plotImageViewLayParam.addRule(RelativeLayout.CENTER_HORIZONTAL);
		plotImageViewLayParam.addRule(RelativeLayout.LEFT_OF, scaleID);
		// apply layout to view
		plotImageView.setLayoutParams(plotImageViewLayParam);
		plotImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		return plotImageView;
	}

	/**
	 * This method prepares ImageView widget for displaying plat scale.
	 *
	 * @param maxId
	 * 		ID number of TextView widget, that display maximum value on the plot.
	 * @param scaleId
	 * 		ID number of returned widget.
	 *
	 * @return widget with defined relations, ready to display scale.
	 */
	private ImageView prepareScaleImageView(int maxId, int scaleId) {
		ImageView scaleImageView = new ImageView(context);
		scaleImageView.setId(scaleId);
		// create layout
		RelativeLayout.LayoutParams scaleImageViewLayParam =
				new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		// add layout params
		scaleImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		scaleImageViewLayParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		scaleImageViewLayParam.addRule(RelativeLayout.LEFT_OF, maxId);
		// apply layout to view
		scaleImageView.setLayoutParams(scaleImageViewLayParam);
		scaleImageView.setScaleType(ImageView.ScaleType.FIT_XY);
		return scaleImageView;
	}

	/**
	 * This method prepares TextView widget, that display plot maximum value.
	 *
	 * @param maxId
	 * 		ID number of returned widget.
	 *
	 * @return widget ready to display plot maximum value.
	 */
	private TextView prepareTextViewMaxValue(int maxId) {
		TextView textViewMaxValue = new TextView(context);
		textViewMaxValue.setId(maxId);
		// create layout
		RelativeLayout.LayoutParams textViewMaxValueLayParam =
				new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams
						.WRAP_CONTENT);
		// add layout params
		textViewMaxValueLayParam.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		textViewMaxValueLayParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		// apply layout to view
		textViewMaxValue.setLayoutParams(textViewMaxValueLayParam);
		return textViewMaxValue;
	}

	/**
	 * This method prepares TextView widget, that display plot minimum value.
	 *
	 * @param minId
	 * 		ID number of returned widget.
	 *
	 * @return widget ready to display plot minimum value.
	 */
	private TextView prepareTextViewMinValue(int minId) {
		TextView textViewMinValue = new TextView(context);
		textViewMinValue.setId(minId);
		// create layout
		RelativeLayout.LayoutParams textViewMinValueLayParam =
				new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams
						.WRAP_CONTENT);
		// add layout params
		textViewMinValueLayParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		textViewMinValueLayParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		// apply layout to view
		textViewMinValue.setLayoutParams(textViewMinValueLayParam);
		return textViewMinValue;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		//noinspection SimplifiableIfStatement
		if (id == R.id.action_set_refreshing_period) {
			setRefreshingPeriod();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Start new activity for getting from user database host address and port.
	 */
	private void setDeviceName() {
		Intent i = new Intent(this, SetDeviceActivity.class);
		startActivityForResult(i, 2);
	}

	/**
	 * Prompt user to set new refreshing period
	 */
	private void setRefreshingPeriod() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(getString(R.string.set_new_refreshing_period));

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setHint(R.string.hint_period_in_ms);
		input.setText("" + refreshingPeriod);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				int period = Integer.parseInt(value);
				if (period > 0) {
					if (attributeFuture != null) {
						attributeFuture.cancel(true);
					}
					attributeFuture = scheduler.scheduleAtFixedRate(currentRunnable, period, period, MILLISECONDS);

					if (statusFuture != null) {
						statusFuture.cancel(true);
					}

					statusFuture = scheduler.scheduleAtFixedRate(rStatus, period, period, MILLISECONDS);

					refreshingPeriod = period;
				}
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) { }
		});

		alert.show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (attributeFuture != null) {
			attributeFuture.cancel(true);
		}
		if (statusFuture != null) {
			statusFuture.cancel(true);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				restHost = data.getStringExtra("restHost");
				tangoHost = data.getStringExtra("TangoHost");
				tangoPort = data.getStringExtra("TangoPort");
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("RESTfulTangoHost", restHost);
				editor.putString("TangoHost", tangoHost);
				editor.putString("TangoPort", tangoPort);
				editor.commit();
				Log.d("ATK onActivityResult", "Result: " + restHost);
				if (deviceName != null) {
					populatePanel();
				}
			}
			if (resultCode == RESULT_CANCELED) {
				Log.d("ATK onActivityResult", "Host not changed");
			}
		}
		if (requestCode == 2) {
			if (resultCode == RESULT_OK) {
				deviceName = data.getStringExtra("DEVICE_NAME");
				Log.d("ATK onActivityResult", "Result: " + deviceName);
				setTitle(getString(R.string.title_activity_atkpanel) + " : " + deviceName);
				if (tangoHost != null && tangoPort != null && restHost != null) {
					populatePanel();
				}
			}
			if (resultCode == RESULT_CANCELED) {
				Log.d("ATK onActivityResult", getString(R.string.atk_panel_dev_not_set));
				setTitle(getString(R.string.title_activity_atkpanel) + " : " + getString(R.string.atk_panel_dev_not_set));
			}
		}
	}

	/**
	 * Start new activity for getting from user database host address and port.
	 */
	private void setHost() {
		Intent i = new Intent(this, SetHostActivity.class);
		if (tangoHost != null) {
			if (!tangoHost.equals("")) {
				i.putExtra("tangoHost", tangoHost);
			}
		}
		if (tangoPort != null) {
			if (!tangoPort.equals("")) {
				i.putExtra("tangoPort", tangoPort);
			}
		}
		if (restHost != null) {
			if (!restHost.equals("")) {
				i.putExtra("restHost", restHost);
			}
		}
		startActivityForResult(i, 1);
	}

	/**
	 * This method start queries for getting device commands and attributes.
	 */
	private void populatePanel() {
		Log.v("populatePanel()", "Populating panel");
		String urlCommandListQuery = restHost + "/Tango/rest/" + tangoHost + ":" + tangoPort +
				"/Device/" + deviceName + "/command_list_query";
		HeaderJsonObjectRequest jsObjRequestCommands =
				new HeaderJsonObjectRequest(Request.Method.GET, urlCommandListQuery, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("populatePanel()", "Device connection OK - received command list");
								populateCommandSpinner(response);
							} else {
								Log.d("populatePanel()", "Tango database API returned message:");
								Log.d("populatePanel()", response.getString("connectionStatus"));
							}
						} catch (JSONException e) {
							Log.d("ATK Panel populatePanel", "Problem with JSON object");
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						jsonRequestErrorHandler(error);
					}
				}, userName, userPassword);
		jsObjRequestCommands.setShouldCache(false);
		queue.add(jsObjRequestCommands);

		String urlGetAttribuesList = restHost + "/Tango/rest/" + tangoHost + ":" + tangoPort +
				"/Device/" + deviceName + "/get_attribute_list";
		HeaderJsonObjectRequest jsObjRequestAttributeList =
				new HeaderJsonObjectRequest(Request.Method.GET, urlGetAttribuesList, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("populatePanel()", "Device connection OK - received attribute list");
								populateAttributeSpinner(response);
							} else {
								Log.d("populatePanel()", "Tango database API returned message:");
								Log.d("populatePanel()", response.getString("connectionStatus"));
							}
						} catch (JSONException e) {
							Log.d("ATK Panel populatePanel", "Problem with JSON object");
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						jsonRequestErrorHandler(error);
					}
				}, userName, userPassword);
		jsObjRequestAttributeList.setShouldCache(false);
		queue.add(jsObjRequestAttributeList);
	}

	/**
	 * Method displaying info about connection error
	 *
	 * @param error
	 * 		Error tah caused exception
	 */
	private void jsonRequestErrorHandler(VolleyError error) {
		// Print error message to LogcCat
		Log.d("jsonRequestErrorHandler", "Connection error!");
		error.printStackTrace();

		// show dialog box with error message
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(error.toString()).setTitle("Connection error!")
				.setPositiveButton(getString(R.string.ok_button), null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * Add commands retrieved from server to spinner.
	 *
	 * @param response
	 * 		JSONObject containing command list and their parameters
	 */
	private void populateCommandSpinner(JSONObject response) {
		Log.d("populateCommandSpinner", "Populating command spinner");
		commandArray = new ArrayList<>();
		commandInTypeArray = new ArrayList<>();
		ArrayList<String> commandOutTypeArray = new ArrayList<>();
		try {
			int commandCount = response.getInt("commandCount");
			//String commandName;
			for (int i = 0; i < commandCount; i++) {
				commandArray.add(i, response.getString("command" + i));
				commandInTypeArray.add(i, Tango_CmdArgTypeName[response.getInt("inType" + i)]);
				commandOutTypeArray.add(i, Tango_CmdArgTypeName[response.getInt("outType" + i)]);
				Log.v("populateCommandSpinner",
						"Command " + commandArray.get(i) + ", inType: " + commandInTypeArray.get(i) + ", " +
								"outType: " + commandOutTypeArray.get(i));
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, commandArray);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			NoSelectionSpinner sItems = (NoSelectionSpinner) findViewById(R.id.atk_panel_command_spinner);
			sItems.setAdapter(adapter);
			//sItems.setSelection(0, false);
			sItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
					if (firstSelection) {
						Log.d("populateCommandSpinner", "Command spinner first selection, omitting action");
						firstSelection = false;
					} else {

						Log.d("populateCommandSpinner", "Position: " + position);
						final String commandName = commandArray.get(position);
						String commandInType = commandInTypeArray.get(position);
						Log.d("populateCommandSpinner", "Executing " + commandName + ", inType: " + commandInType);
						if (commandInType.equals("DevVoid")) {
							Log.d("populateCommandSpinner", "Command run wth void argument");
							executeCommand(commandName, "DevVoidArgument");
						} else {
							Log.d("populateCommandSpinner", "Prompt for command argument");
							AlertDialog.Builder alert = new AlertDialog.Builder(context);
							alert.setTitle(getString(R.string.command_input));

							// Set an EditText view to get user input
							final EditText input = new EditText(context);
							alert.setMessage(getString(R.string.command_input_type) + commandInType);
							alert.setView(input);
							alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									executeCommand(commandName, input.getText().toString());
								}
							});

							alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) { }
							});

							alert.show();
						}
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parentView) {
				}

			});
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Execute selected command with specified argument.
	 *
	 * @param commandName
	 * 		Command name.
	 * @param arginStr
	 * 		Command argument.
	 */
	private void executeCommand(String commandName, String arginStr) {

		String url = restHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/command_inout/" +
				commandName + "/" + arginStr;
		HeaderJsonObjectRequest jsObjRequest =
				new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("ATK  executeCommand", "Device connection OK");
								String commandOut = response.getString("commandReply");
								String commandOutType = response.getString("commandOutType");
								if (commandOutType.equals("DevVoid")) {
									Toast.makeText(context, getString(R.string.command_execution_confirmation), Toast.LENGTH_SHORT)
											.show();
								} else {
									AlertDialog.Builder builder = new AlertDialog.Builder(context);
									builder.setMessage(commandOut).setTitle("Command output");
									builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
										}
									});
									AlertDialog dialog = builder.create();
									dialog.show();
								}
							} else {
								Log.d("ATK executeCommand", "Tango database API returned message:");
								Log.d("ATK executeCommand", response.getString("connectionStatus"));
								Toast.makeText(context,
										getString(R.string.command_execution_error) + response.getString("connectionStatus"),
										Toast.LENGTH_SHORT).show();
							}
						} catch (JSONException e) {
							Log.d("ATK executeCommand", "Problem with JSON object");
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						jsonRequestErrorHandler(error);
					}
				}, userName, userPassword);
		jsObjRequest.setShouldCache(false);
		queue.add(jsObjRequest);
	}

	/**
	 * Write device status to appropriate TextView.
	 *
	 * @param response
	 * 		JSONObject conatining device status.
	 */
	private void populateStatus(JSONObject response) {
		try {
			String status = response.getString("commandReply");
			TextView statusTextView = (TextView) findViewById(R.id.atk_panel_status_text_view);
			statusTextView.setText(status);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Populate attribute spinner with attributes provided in response. If attribute is of type spectrum or image it is added
	 * to spinner, else it is added to scalars list.
	 *
	 * @param response
	 * 		JSONObject containing attributes and their parameters.
	 */
	private void populateAttributeSpinner(JSONObject response) {
		Log.d("popAttributeSpinner", "Populating attribute spinner");
		scalarAttrbuteArray = new ArrayList<>();
		nonScalarAttributeArray = new ArrayList<>();

		try {
			int attributeCount = response.getInt("attCount");
			attributeWritableArray = new boolean[attributeCount];
			for (int i = 0; i < attributeCount; i++) {
				String attributeName = response.getString("attribute" + i);
				if (response.getBoolean("attScalar" + i)) {
					if (!attributeName.equals("State") && !attributeName.equals("Status")) {
						scalarAttrbuteArray.add(attributeName);
						attributeWritableArray[i] = response.getBoolean("attWritable" + i);
					}
				} else {
					nonScalarAttributeArray.add(attributeName);
				}
			}
			if (!nonScalarAttributeArray.isEmpty()) {
				nonScalarAttributeArray.add(0, "Scalar");
				ArrayAdapter<String> adapter =
						new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nonScalarAttributeArray);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				Spinner attributeSpinner = (Spinner) findViewById(R.id.atk_panel_attribute_spinner);
				attributeSpinner.setAdapter(adapter);
				attributeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
						String attName = nonScalarAttributeArray.get(position);
						if (attName.equals("Scalar")) {
							populateScalarListView();
						} else {
							if (attributeFuture != null) {
								attributeFuture.cancel(true);
							}
							populateAttributePlot(attName);
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parentView) {
					}
				});
			} else {
				Spinner attributeSpinner = (Spinner) findViewById(R.id.atk_panel_attribute_spinner);
				attributeSpinner.setVisibility(View.INVISIBLE);
				populateScalarListView();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Begins task for refreshing plot of selected attribute.
	 *
	 * @param attributeName
	 * 		Name of selected attribute.
	 */
	private void populateAttributePlot(String attributeName) {
		Log.d("populateAttributePlot()", "Polulating attribute plot");
		if (attributeFuture != null) {
			attributeFuture.cancel(true);
		}
		plotAttributeName = attributeName;
		currentRunnable = rAttributePlot;
		attributeFuture = scheduler.scheduleAtFixedRate(currentRunnable, refreshingPeriod, refreshingPeriod, MILLISECONDS);
	}

	/**
	 * Add attributes from scalar list to ListView widget.
	 */
	private void populateScalarListView() {
		if (!scalarAttrbuteArray.isEmpty()) {

			Log.d("populateScalarListView", "Getting RelativeLayout");
			RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.atkPanel_innerLayout);

			relativeLayout.removeAllViews();
			numberOfScalars = scalarAttrbuteArray.size();
			ids = new int[numberOfScalars][5];
			for (int i = 0; i < numberOfScalars; i++) {
				RelativeLayout.LayoutParams textViewLayParam =
						new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
								RelativeLayout.LayoutParams.WRAP_CONTENT);
				RelativeLayout.LayoutParams editTextLayParam =
						new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
								RelativeLayout.LayoutParams.WRAP_CONTENT);
				RelativeLayout.LayoutParams buttonLayParam =
						new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams
								.WRAP_CONTENT);

				TextView tv = new TextView(this);
				ids[i][1] = generateViewId();
				tv.setId(ids[i][1]);

				EditText et = new EditText(this);
				ids[i][2] = generateViewId();
				et.setId(ids[i][2]);

				Button b = new Button(this);
				ids[i][3] = generateViewId();
				b.setId(ids[i][3]);

				if (i == 0) {
					editTextLayParam.addRule(RelativeLayout.ALIGN_PARENT_TOP);

				} else {
					editTextLayParam.addRule(RelativeLayout.BELOW, ids[i - 1][2]);
				}

				editTextLayParam.addRule(RelativeLayout.CENTER_HORIZONTAL);

				buttonLayParam.addRule(RelativeLayout.ALIGN_BASELINE, ids[i][1]);
				buttonLayParam.addRule(RelativeLayout.RIGHT_OF, ids[i][2]);

				textViewLayParam.addRule(RelativeLayout.ALIGN_BASELINE, ids[i][2]);
				textViewLayParam.addRule(RelativeLayout.LEFT_OF, ids[i][2]);

				tv.setText(scalarAttrbuteArray.get(i));
				tv.setLayoutParams(textViewLayParam);

				et.setLayoutParams(editTextLayParam);
				et.setFocusable(false);
				et.setWidth(300);

				b.setLayoutParams(buttonLayParam);
				b.setText("Edit");
				if (attributeWritableArray[i]) {
					b.setVisibility(View.VISIBLE);
					b.setTag(scalarAttrbuteArray.get(i));
					b.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							//int rowId = (int) v.getTag(1);
							final String attName = (String) v.getTag();
							Log.d("populateCommandSpinner", "Prompt for attribute new value");
							AlertDialog.Builder alert = new AlertDialog.Builder(context);
							alert.setTitle(getString(R.string.attribute_new_value));
							alert.setMessage(getString(R.string.new_value_for_att) + attName);

							// Set an EditText view to get user input
							final EditText input = new EditText(context);

							alert.setView(input);
							alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									updateAttribute(attName, input.getText().toString());
								}
							});

							alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) { }
							});

							alert.show();

						}
					});
				} else {
					b.setVisibility(View.INVISIBLE);
				}

				relativeLayout.addView(tv);
				relativeLayout.addView(et);
				relativeLayout.addView(b);
			}

			JSONObject request = new JSONObject();
			try {
				request.put("attCount", numberOfScalars);
				for (int i = 0; i < numberOfScalars; i++) {
					request.put("att" + i, scalarAttrbuteArray.get(i));
					request.put("attID" + i, ids[i][2]);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			String urlReadAttributesQuery = restHost + "/Tango/rest/" + tangoHost + ":" + tangoPort +
					"/Device/" + deviceName + "/read_attributes";
			HeaderJsonObjectRequest jsObjRequestCommands =
					new HeaderJsonObjectRequest(Request.Method.PUT, urlReadAttributesQuery, request,
							new Response.Listener<JSONObject>() {
								@Override
								public void onResponse(JSONObject response) {
									try {
										if (response.getString("connectionStatus").equals("OK")) {
											Log.d("populateScalarListView", "Device connection OK - read attribute values");
											updateScalarListView(response);
										} else {
											Log.d("populateScalarListView", "Tango database API returned message:");
											Log.d("populateScalarListView", response.getString("connectionStatus"));
										}
									} catch (JSONException e) {
										Log.d("populateScalarListView", "Problem with JSON object");
										e.printStackTrace();
									}
								}
							}, new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							jsonRequestErrorHandler(error);
						}
					}, userName, userPassword);
			jsObjRequestCommands.setShouldCache(false);
			queue.add(jsObjRequestCommands);
			if (attributeFuture != null) {
				attributeFuture.cancel(true);
			}

			currentRunnable = rAttributes;
			attributeFuture = scheduler.scheduleAtFixedRate(currentRunnable, refreshingPeriod, refreshingPeriod, MILLISECONDS);
		}
	}

	/**
	 * Begins query to set new attribute value.
	 *
	 * @param attName
	 * 		Selected atribute name.
	 * @param argin
	 * 		Attibute new value.
	 */
	private void updateAttribute(String attName, String argin) {
		String url = restHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/write_attribute/" + attName + "/" +
				argin;
		JsonObjectRequest jsObjRequest =
				new JsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.v("Update attribute", "Device connection OK - wrote attribute");
								Toast.makeText(getApplicationContext(), response.getString("connectionStatus"), Toast.LENGTH_SHORT)
										.show();
							} else {
								Toast.makeText(getApplicationContext(), response.getString("connectionStatus"), Toast.LENGTH_SHORT)
										.show();
								Log.d("Update attribute", "Tango database API returned message:");
								Log.d("Update attribute", response.getString("connectionStatus"));
							}
						} catch (JSONException e) {
							Log.d("Update attribute", "Problem with JSON object");
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.d("Update attribute", "Connection error!");
						error.printStackTrace();
					}
				});
		jsObjRequest.setShouldCache(false);
		queue.add(jsObjRequest);
	}

	/**
	 * Update list of scalars with their new values.
	 *
	 * @param response
	 * 		JSONObject containing new attribute values.
	 */
	private void updateScalarListView(JSONObject response) {
		EditText editText;
		try {
			int attCount = response.getInt("attCount");
			for (int i = 0; i < attCount; i++) {
				editText = (EditText) findViewById(response.getInt("attID" + i));
				editText.setText(response.getString("attValue" + i));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

}
