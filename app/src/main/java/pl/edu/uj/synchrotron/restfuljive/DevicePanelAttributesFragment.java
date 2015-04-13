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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.esrf.TangoDs.TangoConst;

/**
 * A class for creating device panel attributes fragment.
 */
public class DevicePanelAttributesFragment extends CertificateExceptionFragment implements TangoConst {
	private int selectedAttributeId;
	private String attributeNames[];
	private View rootView;
	private Context context;
	private String RESTfulTangoHost;
	private String tangoHost;
	private String tangoPort;
	private boolean attributePlottable[];
	private boolean attributeWritable[];
	private String attributeDesc[];

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		rootView = inflater.inflate(R.layout.fragment_device_panel_attributes, container, false);
		final String deviceName = ((DevicePanelActivity) getActivity()).getDeviceName();
		TextView tvDeviceName = (TextView) rootView.findViewById(R.id.devicePanel_attributes_deviceName);
		tvDeviceName.setText(deviceName);
		Log.d("onCreateView", "Device name: " + deviceName);
		RESTfulTangoHost = ((DevicePanelActivity) getActivity()).getRestHost();
		tangoHost = ((DevicePanelActivity) getActivity()).getTangoHost();
		tangoPort = ((DevicePanelActivity) getActivity()).getTangoPort();
		context = ((DevicePanelActivity) getActivity()).getContext();

		Log.d("onCreateView()", "Host: " + RESTfulTangoHost);

		restartQueue();
		String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/get_attribute_list";
		HeaderJsonObjectRequest jsObjRequest =
				new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("onCreateView()", "Device connection OK / method GET / got attribute list");
								// showDeviceInfo(response.getString("deviceInfo"));
								// dp = new DeviceProxy(deviceName, dbHost, dbPort);
								int attributeCount = response.getInt("attCount");
								Log.d("onCreateView()", "Received " + attributeCount + " attributes");
								attributeNames = new String[attributeCount];
								attributePlottable = new boolean[attributeCount];
								attributeDesc = new String[attributeCount];
								attributeWritable = new boolean[attributeCount];


								for (int i = 0; i < attributeNames.length; i++) {
									attributeNames[i] = response.getString("attribute" + i);
									attributePlottable[i] = response.getBoolean("attPlotable" + i);
									attributeDesc[i] = response.getString("attDesc" + i);
									attributeWritable[i] = response.getBoolean("attWritable" + i);
								}
								ListView lv = (ListView) rootView.findViewById(R.id.devicePanel_attributes_listView);
								ArrayAdapter<String> adapter =
										new ArrayAdapter<String>(getActivity(), R.layout.list_item, R.id.firstLine,
												attributeNames);
								lv.setAdapter(adapter);
								lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
									@Override
									public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
										selectedAttributeId = position;
										for (int a = 0; a < parent.getChildCount(); a++) {
											parent.getChildAt(a).setBackgroundColor(Color.TRANSPARENT);
										}
										view.setBackgroundColor(Color.GRAY);
										boolean clickablePlotButton = attributePlottable[selectedAttributeId];
										Button plotButton =
												(Button) rootView.findViewById(R.id.devicePanel_attributes_plotButton);
										plotButton.setClickable(clickablePlotButton);
										plotButton.setEnabled(clickablePlotButton);
										Button descriptionButton = (Button) rootView
												.findViewById(R.id.devicePanel_attributes_descriptionButton);
										descriptionButton.setClickable(true);
										descriptionButton.setEnabled(true);
										Button readButton =
												(Button) rootView.findViewById(R.id.devicePanel_attributes_readButton);
										readButton.setClickable(true);
										readButton.setEnabled(true);
										Button writeButton =
												(Button) rootView.findViewById(R.id.devicePanel_attributes_writeButton);
										writeButton.setClickable(attributeWritable[selectedAttributeId]);
										writeButton.setEnabled(attributeWritable[selectedAttributeId]);
									}
								});
							} else {
								Log.d("onCreateView()", "Tango database API returned message from query get_attribute_list:");
								Log.d("onCreateView()", response.getString("connectionStatus"));
							}
						} catch (JSONException e) {
							Log.d("onCreateView()", "Problem with JSON object while getting attribute list");
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						jsonRequestErrorHandler(error);
					}
				});
		jsObjRequest.setShouldCache(false);
		queue.add(jsObjRequest);

		Button descriptionButton = (Button) rootView.findViewById(R.id.devicePanel_attributes_descriptionButton);
		descriptionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
				builder.setTitle("Attribute \"" + attributeNames[selectedAttributeId] + "\" description");
				builder.setMessage(attributeDesc[selectedAttributeId]);
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		});


		final Button readButton = (Button) rootView.findViewById(R.id.devicePanel_attributes_readButton);
		readButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d("readButton.onClick()", "Processing read button");
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/read_attribute/" +
						attributeNames[selectedAttributeId];
				Log.d("readButton.onClick()", "Sending JSON request");
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										Log.d("readButton.onClick()", "Device connection OK / method GET / read attribute");
										String attName = response.getString("attName");
										String attValue = response.getString("attValue");
										String devName = response.getString("devName");
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Attribute \"" + attName + "\" read");
										builder.setMessage("Attribute: " + devName + "/" + attName + "\n" + attValue);
										builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
											}
										});
										AlertDialog dialog = builder.create();
										dialog.show();
									} else {
										Log.d("readButton.onClick()", "Tango database API returned message from query " +
												"read_attribute:");
										Log.d("readButton.onClick()", response.getString("connectionStatus"));
									}
								} catch (JSONException e) {
									Log.d("readButton.onClick()", "Problem with JSON object while reading attribute");
									e.printStackTrace();
								}
							}
						}, new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								jsonRequestErrorHandler(error);
							}
						});
				jsObjRequest.setShouldCache(false);
				queue.add(jsObjRequest);
			}
		});

		Button writeButton = (Button) rootView.findViewById(R.id.devicePanel_attributes_writeButton);
		writeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				System.out.println("Processing write button");
				EditText arginEditTextValue = (EditText) rootView
						.findViewById(R.id.devicePanel_attributes_arginValueEditText);
				String arginStr = arginEditTextValue.getText().toString();
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/write_attribute/" +
						attributeNames[selectedAttributeId] + "/" + arginStr;
				System.out.println("Sending JSON request");
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Reply");
										builder.setMessage("Attribute updated");
										builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
											}
										});
										AlertDialog dialog = builder.create();
										dialog.show();
									} else {
										System.out.println("Tango database API returned message:");
										System.out.println(response.getString("connectionStatus"));
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
						});
				jsObjRequest.setShouldCache(false);
				queue.add(jsObjRequest);
			}
		});

		Button plotButton = (Button) rootView.findViewById(R.id.devicePanel_attributes_plotButton);
		plotButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				System.out.println("Processing plot button");
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/plot_attribute/" +
						attributeNames[selectedAttributeId];
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
												Intent intent = new Intent(getActivity(), PlotActivity.class);
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

												Bitmap b = Bitmap.createBitmap(ivalues.length, ivalues[0].length,
														Bitmap.Config.RGB_565);
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
												Intent imageIntent = new Intent(getActivity(), ImagePlotActivity.class);
												imageIntent.putExtra("imageData", b);
												imageIntent.putExtra("minValue", String.valueOf(minValue));
												imageIntent.putExtra("maxValue", String.valueOf(maxValue));
												// intent.putExtra("plotTitle",
												// dp.name()+"/"+commInfo.cmd_name);
												startActivity(imageIntent);
												break;
										}
									} else {
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
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
						});
				jsObjRequest.setShouldCache(false);
				queue.add(jsObjRequest);
			}

			;
		});
		return rootView;
	}

	/**
	 * Method displaying info about connection error
	 *
	 * @param error Error tah caused exception
	 */
	private void jsonRequestErrorHandler(VolleyError error) {
		// Print error message to LogcCat
		Log.d("jsonRequestErrorHandler", "Connection error!");
		error.printStackTrace();

		// show dialog box with error message
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(error.toString()).setTitle("Connection error!").setPositiveButton(getString(R.string.ok_button),
				null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}
}
