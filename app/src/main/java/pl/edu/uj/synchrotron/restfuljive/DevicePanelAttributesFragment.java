package pl.edu.uj.synchrotron.restfuljive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoDs.TangoConst;

/**
 * A class for creating device panel attributes fragment.
 */
public class DevicePanelAttributesFragment extends Fragment implements TangoConst {
	final DevicePanelAttributesFragment devicePanelAttributesFragment = this;
	private int selectedAttributeId;
	//private AttributeInfo[] attList;
	private String attributeNames[];
	private View rootView;
	private Context context;
	private String pTangoHost;
	private boolean attributePlottable[];
	private boolean attributeWritable[];
	private String attributeDesc[];

	/**
	 * Check whether attribute could be read, written or both.
	 *
	 * @param ai Attribute to be checked.
	 * @return String defining write permission.
	 */
	static String getWriteString(AttributeInfo ai) {
		switch (ai.writable.value()) {
			case AttrWriteType._READ:
				return "READ";
			case AttrWriteType._READ_WITH_WRITE:
				return "READ_WITH_WRITE";
			case AttrWriteType._READ_WRITE:
				return "READ_WRITE";
			case AttrWriteType._WRITE:
				return "WRITE";
		}
		return "Unknown";
	}


	// -----------------------------------------------------
	// Private stuff
	// -----------------------------------------------------

	/**
	 * Get alphabetically sorted list of attributes.
	 *
	 * @return Array of attributes.
	 * @throws DevFailed When device is uninitialized or there was problem with
	 *                   connection.
	 */
	 /*private AttributeInfo[] getAttributeList() throws DevFailed {
	     int i, j;
        boolean end;
        AttributeInfo tmp;
        AttributeInfo[] lst = device.get_attribute_info();
        // Sort the list
        end = false;
        j = lst.length - 1;
        while (!end) {
            end = true;
            for (i = 0; i < j; i++) {
                if (lst[i].name.compareToIgnoreCase(lst[i + 1].name) > 0) {
                    end = false;
                    tmp = lst[i];
                    lst[i] = lst[i + 1];
                    lst[i + 1] = tmp;
                }
            }
            j--;
        }
        return lst;
    }*/

	/**
	 * Check whether attribute could be presented as scalar, spectrum or image.
	 *
	 * @param ai Attribute to be checked.
	 * @return String defining presentation format.
	 */
	static String getFormatString(AttributeInfo ai) {
		switch (ai.data_format.value()) {
			case AttrDataFormat._SCALAR:
				return "Scalar";
			case AttrDataFormat._SPECTRUM:
				return "Spectrum";
			case AttrDataFormat._IMAGE:
				return "Image";
		}
		return "Unknown";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_device_panel_attributes, container, false);
		final String deviceName = ((DevicePanelActivity) getActivity()).getDeviceName();
		TextView tvDeviceName = (TextView) rootView.findViewById(R.id.devicePanel_attributes_deviceName);
		tvDeviceName.setText(deviceName);
		System.out.println("Device name: " + deviceName);
		pTangoHost = ((DevicePanelActivity) getActivity()).getHost();
		System.out.println("Host: " + pTangoHost);
		context = ((DevicePanelActivity) getActivity()).getContext();


		RequestQueue queue = Volley.newRequestQueue(context);
		queue.start();
		String url = pTangoHost + "/RESTfulTangoApi/Device/" + deviceName + "/get_attribute_list.json";
		JsonObjectRequest jsObjRequest =
				new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								System.out.println("Device connection OK");
								// showDeviceInfo(response.getString("deviceInfo"));
								// dp = new DeviceProxy(deviceName, dbHost, dbPort);
								int attributeCount = response.getInt("attCount");
								System.out.println("Received " + attributeCount + " attributes");
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
						System.out.println("Connection error!");
						error.printStackTrace();
					}
				});
		queue.add(jsObjRequest);

		Button descriptionButton = (Button) rootView.findViewById(R.id.devicePanel_attributes_descriptionButton);
		descriptionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
				//AttributeInfo ai = attList[selectedAttributeId];
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
				System.out.println("Processing read button");
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();
				String url = pTangoHost + "/RESTfulTangoApi/Device/" + deviceName + "/read_attribute.json/" +
						attributeNames[selectedAttributeId];
				System.out.println("Sending JSON request");
				JsonObjectRequest jsObjRequest =
						new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
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
								System.out.println("Connection error!");
								error.printStackTrace();
							}
						});
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
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();
				String url = pTangoHost + "/RESTfulTangoApi/Device/" + deviceName + "/write_attribute.json/" +
						attributeNames[selectedAttributeId] + "/" + arginStr;
				System.out.println("Sending JSON request");
				JsonObjectRequest jsObjRequest =
						new JsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
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
								System.out.println("Connection error!");
								error.printStackTrace();
							}
						});
				queue.add(jsObjRequest);
			}
		});

		Button plotButton = (Button) rootView.findViewById(R.id.devicePanel_attributes_plotButton);
		plotButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				System.out.println("Processing plot button");
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();
				String url = pTangoHost + "/RESTfulTangoApi/Device/" + deviceName + "/plot_attribute.json/" +
						attributeNames[selectedAttributeId];
				System.out.println("Sending JSON request");
				JsonObjectRequest jsObjRequest =
						new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
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
								System.out.println("Connection error!");
								error.printStackTrace();
							}
						});
				queue.add(jsObjRequest);
			}

			;
		});
		return rootView;
	}
}
