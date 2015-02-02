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
import fr.esrf.TangoApi.DeviceAttribute;
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

								//AlertDialog.Builder builder = new AlertDialog.Builder(context);
								//builder.setMessage(response.getString("deviceInfo")).setTitle("Device info");
								//AlertDialog dialog = builder.create();
								//dialog.show();
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

	/**
	 * Extract data read from device and convert to String.
	 *
	 * @param data Data read from device
	 * @param ai   Parameter of read data.
	 * @return String with data.
	 */
   /* private String extractData(DeviceAttribute data, AttributeInfo ai) {

        StringBuffer ret_string = new StringBuffer();

        try {

            // Add the date of the measure in two formats
            TimeVal t = data.getTimeVal();
            java.util.Date date = new java.util.Date((long) (t.tv_sec * 1000.0 + t.tv_usec / 1000.0));
            SimpleDateFormat dateformat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            dateformat.setTimeZone(TimeZone.getDefault());
            ret_string.append("measure date: " + dateformat.format(date) + " + " + (t.tv_usec / 1000) + "ms\n");

            // Add the quality information
            AttrQuality q = data.getQuality();

            ret_string.append("quality: ");
            switch (q.value()) {
                case AttrQuality._ATTR_VALID:
                    ret_string.append("VALID\n");
                    break;
                case AttrQuality._ATTR_INVALID:
                    ret_string.append("INVALID\n");
                    return ret_string.toString();
                case AttrQuality._ATTR_ALARM:
                    ret_string.append("ALARM\n");
                    break;
                case AttrQuality._ATTR_CHANGING:
                    ret_string.append("CHANGING\n");
                    break;
                case AttrQuality._ATTR_WARNING:
                    ret_string.append("WARNING\n");
                    break;
                default:
                    ret_string.append("UNKNOWN\n");
                    break;
            }

            // Add dimension of the attribute but only if having a meaning
            boolean printIndex = true;
            boolean checkLimit = true;
            switch (ai.data_format.value()) {
                case AttrDataFormat._SCALAR:
                    printIndex = false;
                    checkLimit = false;
                    break;
                case AttrDataFormat._SPECTRUM:
                    ret_string.append("dim x: " + data.getDimX() + "\n");
                    break;
                case AttrDataFormat._IMAGE:
                    ret_string.append("dim x: " + data.getDimX() + "\n");
                    ret_string.append("dim y: " + data.getDimY() + "\n");
                    break;
                default:
                    break;
            }

            // Add values
            switch (ai.data_type) {

                case Tango_DEV_STATE: {
                    DevState[] dummy = data.extractDevStateArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Tango_DevStateName[dummy[i].value()], false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Tango_DevStateName[dummy[i + nbRead].value()], true);
                    }
                }
                break;

                case Tango_DEV_UCHAR: {
                    short[] dummy = data.extractUCharArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Short.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Short.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_SHORT: {
                    short[] dummy = data.extractShortArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Short.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Short.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_BOOLEAN: {
                    boolean[] dummy = data.extractBooleanArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Boolean.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Boolean.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_USHORT: {
                    int[] dummy = data.extractUShortArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Integer.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Integer.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_LONG: {
                    int[] dummy = data.extractLongArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Integer.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Integer.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_ULONG: {
                    long[] dummy = data.extractULongArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_LONG64: {
                    long[] dummy = data.extractLong64Array();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_ULONG64: {
                    long[] dummy = data.extractULong64Array();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Long.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_DOUBLE: {
                    double[] dummy = data.extractDoubleArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Double.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Double.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_FLOAT: {
                    float[] dummy = data.extractFloatArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, Float.toString(dummy[i]), false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, Float.toString(dummy[i + nbRead]), true);
                    }
                }
                break;

                case Tango_DEV_STRING: {
                    String[] dummy = data.extractStringArray();
                    int nbRead = data.getNbRead();
                    int nbWritten = dummy.length - nbRead;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++)
                        printArrayItem(ret_string, i, printIndex, dummy[i], false);
                    if (isWritable(ai)) {
                        start = getLimitMin(checkLimit, ret_string, nbWritten);
                        end = getLimitMax(checkLimit, ret_string, nbWritten, true);
                        for (int i = start; i < end; i++)
                            printArrayItem(ret_string, i, printIndex, dummy[i + nbRead], true);
                    }
                }
                break;

                case Tango_DEV_ENCODED: {
                    printIndex = true;
                    DevEncoded e = data.extractDevEncoded();
                    ret_string.append("Format: " + e.encoded_format + "\n");
                    int nbRead = e.encoded_data.length;
                    int start = getLimitMin(checkLimit, ret_string, nbRead);
                    int end = getLimitMax(checkLimit, ret_string, nbRead, false);
                    for (int i = start; i < end; i++) {
                        short vs = (short) e.encoded_data[i];
                        vs = (short) (vs & 0xFF);
                        printArrayItem(ret_string, i, printIndex, Short.toString(vs), false);
                    }
                }
                break;

                default:
                    ret_string.append("Unsupported attribute type code=" + ai.data_type + "\n");
                    break;
            }

        } catch (DevFailed e) {

            // ErrorPane.showErrorMessage(this,device.name() + "/" + ai.name,e);

        }

        return ret_string.toString();

    }//*/

	/**
	 * Parses string to be printed
	 *
	 * @param str       String to be printed.
	 * @param idx       Number of value in array.
	 * @param printIdx  Defines if array has more than one value.
	 * @param value     Value that was read/written.
	 * @param writeable Defines if value is writable.
	 */
	private void printArrayItem(StringBuffer str, int idx, boolean printIdx, String value, boolean writeable) {
		if (!writeable) {
			if (printIdx)
				str.append("Read [" + idx + "]\t" + value + "\n");
			else
				str.append("Read:\t" + value + "\n");
		} else {
			if (printIdx)
				str.append("Set [" + idx + "]\t" + value + "\n");
			else
				str.append("Set:\t" + value + "\n");
		}
	}

	/**
	 * Check maximum length of response.
	 *
	 * @param checkLimit
	 * @param retStr     Response string.
	 * @param length     Length of current response.
	 * @param writable   Defines if value is writable.
	 * @return Maximum response length.
	 */
	private int getLimitMax(boolean checkLimit, StringBuffer retStr, int length, boolean writable) {
		if (length < 100) {
			return length;

		}
		return 100;
	}

	/**
	 * Check minimum length of response.
	 *
	 * @param checkLimit
	 * @param retStr     Response string.
	 * @param length     Length of current response.
	 * @return Minimum response length.
	 */
	private int getLimitMin(boolean checkLimit, StringBuffer retStr, int length) {

		return 0;

	}

	/**
	 * Adds value to DeviceAttribute.
	 *
	 * @param argin Value to be added.
	 * @param send  Value will be added to this DeviceAttribute.
	 * @param ai    Define data format.
	 * @return DeviceAttribute with new value.
	 * @throws NumberFormatException
	 */
	private DeviceAttribute insertData(String argin, DeviceAttribute send, AttributeInfo ai)
			throws NumberFormatException {

		ArgParser arg = new ArgParser(argin);

		switch (ai.data_type) {

			case Tango_DEV_UCHAR:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert_uc(arg.parse_uchar());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert_uc(arg.parse_uchar_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert_uc(arg.parse_uchar_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_BOOLEAN:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert(arg.parse_boolean());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert(arg.parse_boolean_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert(arg.parse_boolean_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_SHORT:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert(arg.parse_short());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert(arg.parse_short_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert(arg.parse_short_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_USHORT:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert_us(arg.parse_ushort());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert_us(arg.parse_ushort_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert_us(arg.parse_ushort_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_LONG:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert(arg.parse_long());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert(arg.parse_long_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert(arg.parse_long_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_ULONG:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert_ul(arg.parse_ulong());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert_ul(arg.parse_ulong_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert_ul(arg.parse_ulong_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_LONG64:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert(arg.parse_long64());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert(arg.parse_long64_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert(arg.parse_long64_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_ULONG64:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert_u64(arg.parse_long64());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert_u64(arg.parse_long64_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert_u64(arg.parse_long64_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_FLOAT:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert(arg.parse_float());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert(arg.parse_float_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert(arg.parse_float_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_DOUBLE:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert(arg.parse_double());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert(arg.parse_double_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert(arg.parse_double_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			case Tango_DEV_STRING:
				switch (ai.data_format.value()) {
					case AttrDataFormat._SCALAR:
						send.insert(arg.parse_string());
						break;
					case AttrDataFormat._SPECTRUM:
						send.insert(arg.parse_string_array());
						break;
					case AttrDataFormat._IMAGE:
						send.insert(arg.parse_string_image(), arg.get_image_width(), arg.get_image_height());
						break;
				}
				break;

			default:
				throw new NumberFormatException("Attribute type not supported code=" + ai.data_type);

		}
		return send;

	}

	/**
	 * Extract data from DeviceAttribute to one dimensional array.
	 *
	 * @param data DeviceAttribute to extract data from.
	 * @param ai   Define data format.
	 * @return Array of data that can be plotted.
	 */
	private double[] extractSpectrumPlotData(DeviceAttribute data, AttributeInfo ai) {

		double[] ret = new double[0];
		int i;

		try {

			int start = getLimitMinForPlot(data.getNbRead());
			int end = getLimitMaxForPlot(data.getNbRead());

			switch (ai.data_type) {

				case Tango_DEV_UCHAR: {
					short[] dummy = data.extractUCharArray();
					ret = new double[end - start];
					for (i = start; i < end; i++)
						ret[i - start] = (double) dummy[i];
				}
				break;

				case Tango_DEV_SHORT: {
					short[] dummy = data.extractShortArray();
					ret = new double[end - start];
					for (i = start; i < end; i++)
						ret[i - start] = (double) dummy[i];
				}
				break;

				case Tango_DEV_USHORT: {
					int[] dummy = data.extractUShortArray();
					ret = new double[end - start];
					for (i = start; i < end; i++)
						ret[i - start] = (double) dummy[i];
				}
				break;

				case Tango_DEV_LONG: {
					int[] dummy = data.extractLongArray();
					ret = new double[end - start];
					for (i = start; i < end; i++)
						ret[i - start] = (double) dummy[i];
				}
				break;

				case Tango_DEV_DOUBLE: {
					double[] dummy = data.extractDoubleArray();
					ret = new double[end - start];
					for (i = start; i < end; i++)
						ret[i - start] = dummy[i];
				}
				break;

				case Tango_DEV_FLOAT: {
					float[] dummy = data.extractFloatArray();
					ret = new double[end - start];
					for (i = start; i < end; i++)
						ret[i - start] = (double) dummy[i];
				}
				break;

			}

		} catch (DevFailed e) {

			// ErrorPane.showErrorMessage(this, device.name() + "/" + ai.name, e);

		}

		return ret;

	}

	/**
	 * Extract data from DeviceAttribute to two dimensional array.
	 *
	 * @param data DeviceAttribute to extract data from.
	 * @param ai   Define data format.
	 * @return Array of data that can be plotted.
	 */
	private double[][] extractImagePlotData(DeviceAttribute data, AttributeInfo ai) {

		double[][] ret = new double[0][0];
		int i, j, k, dimx, dimy;

		try {

			dimx = data.getDimX();
			dimy = data.getDimY();

			switch (ai.data_type) {

				case Tango_DEV_UCHAR: {
					short[] dummy = data.extractUCharArray();
					ret = new double[dimy][dimx];
					for (j = 0, k = 0; j < dimy; j++)
						for (i = 0; i < dimx; i++)
							ret[j][i] = (double) dummy[k++];
				}
				break;
				case Tango_DEV_SHORT: {
					short[] dummy = data.extractShortArray();
					ret = new double[dimy][dimx];
					for (j = 0, k = 0; j < dimy; j++)
						for (i = 0; i < dimx; i++)
							ret[j][i] = (double) dummy[k++];
				}
				break;
				case Tango_DEV_USHORT: {
					int[] dummy = data.extractUShortArray();
					ret = new double[dimy][dimx];
					for (j = 0, k = 0; j < dimy; j++)
						for (i = 0; i < dimx; i++)
							ret[j][i] = (double) dummy[k++];
				}
				break;
				case Tango_DEV_LONG: {
					int[] dummy = data.extractLongArray();
					ret = new double[dimy][dimx];
					for (j = 0, k = 0; j < dimy; j++)
						for (i = 0; i < dimx; i++)
							ret[j][i] = (double) dummy[k++];
				}
				break;
				case Tango_DEV_DOUBLE: {
					double[] dummy = data.extractDoubleArray();
					ret = new double[dimy][dimx];
					for (j = 0, k = 0; j < dimy; j++)
						for (i = 0; i < dimx; i++)
							ret[j][i] = dummy[k++];
				}
				break;
				case Tango_DEV_FLOAT: {
					float[] dummy = data.extractFloatArray();
					ret = new double[dimy][dimx];
					for (j = 0, k = 0; j < dimy; j++)
						for (i = 0; i < dimx; i++)
							ret[j][i] = (double) dummy[k++];
				}
				break;
			}
		} catch (DevFailed e) {
			// ErrorPane.showErrorMessage(this, device.name() + "/" + ai.name, e);
		}
		return ret;
	}

	/**
	 * Check maximum length of data.
	 *
	 * @param length Length of current data.
	 * @return Maximum length.
	 */
	private int getLimitMaxForPlot(int length) {
		if (length < 100) {
			return length;

		}
		return 100;
	}

	/**
	 * Check minimum length of data.
	 *
	 * @param length Length of current data.
	 * @return Minimum length.
	 */
	private int getLimitMinForPlot(int length) {
		return 0;
	}
}
