package pl.edu.uj.synchrotron.restfuljive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A class for creating attributes activity screen.
 */
public class AttributesActivity extends CertificateExceptionActivity {
	/**
	 * The intent that activity was called with.
	 */
	Intent intent;
	/**
	 * Name of device, which attributes should be listed.
	 */
	String deviceName;
	String[] attr_list;
	String[] attr_values;
	boolean[] writable;
	boolean[] scalar;
	boolean[] plottable;
	boolean[] numeric;
	/**
	 * RESTful host address.
	 */
	private String RESTfulHost;
	/**
	 * Address of database to be used by REST service.
	 */
	private String tangoHost;
	/**
	 * Port of database to be used by REST service.
	 */
	private String tangoPort;
	/**
	 * Application context.
	 */
	private Context context;
	private String userName, userPassword;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_attributes);
		intent = getIntent();
		deviceName = intent.getStringExtra("deviceName");
		RESTfulHost = intent.getStringExtra("restHost");
		tangoHost = intent.getStringExtra("tangoHost");
		tangoPort = intent.getStringExtra("tangoPort");
		userName = intent.getStringExtra("userName");
		userPassword = intent.getStringExtra("userPass");
		context = this;
		TextView deviceAttTextView = (TextView) findViewById(R.id.attributesActivityTextView1);
		deviceAttTextView.setText("Device " + deviceName + " attributes");
		setTitle("REST host: " + RESTfulHost + ", TANGO_HOST: " + tangoHost + ":" + tangoPort);

		restartQueue();
		refreshAttributesList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_attributes, menu);
		return true;
	}

	/**
	 * Refresh already shown list of attributes.
	 */
	private void refreshAttributesList() {

		Log.d("refreshAttributeList()", "Device name: " + deviceName);
		Log.d("refreshAttributeList()", "REST host: " + RESTfulHost);
		Log.d("refreshAttributeList()", "Tango host: " + tangoHost + ":" + tangoPort);
		Log.d("refreshAttributeList()", "User: " + userName + ", pass: " + userPassword);

		String url = RESTfulHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/get_attribute_list";
		HeaderJsonObjectRequest jsObjRequest =
				new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("refreshAttributeList()", "Device connection OK / method GET / get_sttribute_list");
								LinearLayout layout = (LinearLayout) findViewById(R.id.attributesActivityLinearLayout);
								layout.removeAllViews();
								final LayoutInflater inflater = LayoutInflater.from(context);
								int attributeCount = response.getInt("attCount");
								Log.d("refreshAttributeList()", "Received " + attributeCount + " attributes");
								attr_list = new String[attributeCount];
								attr_values = new String[attributeCount];
								writable = new boolean[attributeCount];
								scalar = new boolean[attributeCount];
								plottable = new boolean[attributeCount];
								numeric = new boolean[attributeCount];
								for (int i = 0; i < attr_list.length; i++) {
									attr_list[i] = response.getString("attribute" + i);
									attr_values[i] = response.getString("attValue" + i);
									writable[i] = response.getBoolean("attWritable" + i);
									scalar[i] = response.getBoolean("attScalar" + i);
									plottable[i] = response.getBoolean("attPlotable" + i);
									numeric[i] = response.getBoolean("isNumeric" + i);

									//System.out.println("Processing attribute no. " + i);
									//System.out.println("Name: " + attr_list[i] + " Value: " + attr_list[i]);
									View view = inflater.inflate(R.layout.editable_list_element, layout, false);
									EditText et = (EditText) view.findViewById(R.id.editableListEditText);
									TextView tv = (TextView) view.findViewById(R.id.editableListTextView);
									tv.setText(attr_list[i]);
									et.setText(attr_values[i]);
									et.setTag(attr_list[i]);
									et.setEnabled(writable[i] && scalar[i]);
									et.setFocusable(writable[i] && scalar[i]);
									layout.addView(view);

									// button
									Button plotButton = (Button) view.findViewById(R.id.editableList_plotButton);
									plotButton.setTag(deviceName);
									ArrayList<Object> tag = new ArrayList<>();
									tag.add(0, attr_list[i]);
									tag.add(1, i);
									plotButton.setTag(tag);
									plotButton.setClickable(numeric[i] || plottable[i]);
									plotButton.setEnabled(numeric[i] || plottable[i]);
									plotButton.setFocusable(numeric[i] || plottable[i]);
								}
							} else {
								Log.d("refreshAttributeList()",
										"Tango database API returned message from query " + "get_attribute_list:");
								Log.d("refreshAttributeList()", response.getString("connectionStatus"));
							}
						} catch (JSONException e) {
							Log.d("refreshAttributeList()", "Problem with JSON object while getting attribute list");
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.d("refreshAttributeList()", "Connection error!");
						error.printStackTrace();
					}
				}, userName, userPassword);
		jsObjRequest.setShouldCache(false);
		queue.add(jsObjRequest);

	}

	/**
	 * Start plot of selected attribute.
	 *
	 * @param view
	 * 		Widget that was clicked.
	 */
	public void attributesActivityPlotButton(View view) {

		ArrayList tag = (ArrayList) view.getTag();
		String attName = (String) tag.get(0);
		int selectedAttributeId = (int) tag.get(1);
		if (scalar[selectedAttributeId]) {
			startPlotInTime(attName, deviceName, tangoHost, tangoPort);
		} else {
			Log.d("attPlotButton", "Tag: " + tag);
			System.out.println("Processing plot button");
			String url = RESTfulHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
					"/plot_attribute/" + attr_list[selectedAttributeId];
			System.out.println("Sending JSON request");
			HeaderJsonObjectRequest jsObjRequest =
					new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
						@Override
						public void onResponse(JSONObject response) {
							try {
								if (response.getString("connectionStatus").equals("OK")) {
									System.out.println("Device connection OK");
									String dataFormat = response.getString("dataFormat");
									String plotLabel = response.getString("plotLabel");
									switch (dataFormat) {
										case "SPECTRUM":
											JSONArray array = response.getJSONArray("plotData");
											double[] values = new double[array.length()];
											for (int i = 0; i < array.length(); i++) {
												values[i] = array.getDouble(i);
											}
											DataToPlot dtp = new DataToPlot(values);
											Intent intent = new Intent(context, PlotActivity.class);
											intent.putExtra("data", dtp);
											intent.putExtra("domainLabel", plotLabel);
											startActivity(intent);
											break;
										case "IMAGE":
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

											Bitmap b = Bitmap.createBitmap(ivalues.length, ivalues[0].length, Bitmap.Config.RGB_565);
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

											int color = 0;
											float hsv[] = {0, 1, 1};

											for (int i = 1; i < ivalues.length; i++) {
												for (int j = 1; j < ivalues[0].length; j++) {

													hsv[0] = 330 - (float) (ivalues[i][j] * step);
													color = Color.HSVToColor(hsv);
													b.setPixel(ivalues.length - i, ivalues[0].length - j, color);
													// System.out.println("Value["+i+"]["+j+"]= "+color);
												}
											}
											Intent imageIntent = new Intent(context, ImagePlotActivity.class);
											imageIntent.putExtra("imageData", b);
											imageIntent.putExtra("minValue", String.valueOf(minValue));
											imageIntent.putExtra("maxValue", String.valueOf(maxValue));
											// intent.putExtra("plotTitle",
											// dp.name()+"/"+commInfo.cmd_name);
											startActivity(imageIntent);
											break;
									}
								} else {
									AlertDialog.Builder builder = new AlertDialog.Builder(context);
									String res = response.getString("connectionStatus");
									System.out.println("Tango database API returned message:");
									System.out.println(res);
									builder.setTitle("Reply");
									builder.setMessage(res);
									builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
										}
									});
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

	}

	/**
	 * Start new activity plotting scalar value in time.
	 *
	 * @param attributeName
	 * 		Name of selected attribute.
	 * @param deviceName
	 * 		Name of selected device.
	 * @param host
	 * 		Tango host address.
	 * @param port
	 * 		Tango port number.
	 */
	private void startPlotInTime(String attributeName, String deviceName, String host, String port) {
		Intent i = new Intent(this, PlotInTimeActivity.class);
		i.putExtra("attName", attributeName);
		i.putExtra("devName", deviceName);
		i.putExtra("host", host);
		i.putExtra("port", port);
		i.putExtra("RESThost", RESTfulHost);
		i.putExtra("user", userName);
		i.putExtra("pass", userPassword);
		startActivity(i);
	}

	/**
	 * Listener for the button click, refresh list of activities.
	 *
	 * @param view
	 * 		Reference to the widget that was clicked.
	 */
	public void attributesActivityListRefreshButton(View view) {
		refreshAttributesList();
	}

	/**
	 * Listener for the button click, close the activity.
	 *
	 * @param view
	 * 		Reference to the widget that was clicked.
	 */
	public void attributesActivityCancelButton(View view) {
		finish();
	}

	/**
	 * Listener for the button click, update attributes.
	 *
	 * @param view
	 * 		Reference to the widget that was clicked.
	 */
	public void attributesActivityUpdateButton(View view) {
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.attributesActivityLinearLayout);
		int childCount = linearLayout.getChildCount();
		for (int i = 0; i < childCount; i++) {
			// for (int i = 0; i < 2; i++) {
			View linearLayoutView = linearLayout.getChildAt(i);
			EditText et = (EditText) linearLayoutView.findViewById(R.id.editableListEditText);
			if (et.isFocusable()) {
				String value = et.getText().toString();
				String tag = (String) et.getTag();
				String url = RESTfulHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/write_attribute/" + tag + "/" +
						value;
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										Log.d("updateButton.onClick()", "Device connection OK / method PUT / write_attribute");
									} else {
										Toast.makeText(getApplicationContext(), response.getString("connectionStatus"),
												Toast.LENGTH_LONG).show();
										Log.d("updateButton.onClick()",
												"Tango database API returned message from query " + "write_attribute:");
										Log.d("updateButton.onClick()", response.getString("connectionStatus"));
									}
								} catch (JSONException e) {
									Log.d("updateButton.onClick()", "Problem with JSON object while writing attribute");
									e.printStackTrace();
								}
							}
						}, new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								Log.d("updateButton.onClick()", "Connection error!");
								error.printStackTrace();
							}
						}, userName, userPassword);
				jsObjRequest.setShouldCache(false);
				queue.add(jsObjRequest);
			}
		}
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
}

