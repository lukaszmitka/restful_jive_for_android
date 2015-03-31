package pl.edu.uj.synchrotron.restfuljive;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.esrf.Tango.DevState;
import fr.esrf.Tango.DevVarDoubleStringArray;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoDs.TangoConst;

public class DevicePanelCommandsFragment extends Fragment implements TangoConst {
	private int selectedCommandId;
	private View rootView;
	private DeviceProxy dp;
	private int commandCount;
	private String[] commandNames;
	private String[] commandInDesc;
	private String[] commandOutDesc;
	private int[] commandInType;
	private int[] commandOutType;
	private boolean[] commandPlottable;
	private Context context;
	private String RESTfulTangoHost;
	private String tangoHost;
	private String tangoPort;
	private String deviceName;
	private RequestQueue queue;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_device_panel_commands, container, false);
		deviceName = ((DevicePanelActivity) getActivity()).getDeviceName();
		TextView tvDeviceName = (TextView) rootView.findViewById(R.id.devicePanel_deviceName);
		tvDeviceName.setText(deviceName);
		System.out.println("Device name: " + deviceName);
		RESTfulTangoHost = ((DevicePanelActivity) getActivity()).getRestHost();
		tangoHost = ((DevicePanelActivity) getActivity()).getTangoHost();
		tangoPort = ((DevicePanelActivity) getActivity()).getTangoPort();
		System.out.println("Host: " + RESTfulTangoHost);
		context = ((DevicePanelActivity) getActivity()).getContext();

		queue = Volley.newRequestQueue(context);
		queue.start();
		String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/command_list_query.json";
		HeaderJsonObjectRequest jsObjRequest =
				new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								System.out.println("Device connection OK");
								// showDeviceInfo(response.getString("deviceInfo"));
								// dp = new DeviceProxy(deviceName, dbHost, dbPort);
								commandCount = response.getInt("commandCount");
								commandNames = new String[commandCount];
								commandInType = new int[commandCount];
								commandOutType = new int[commandCount];
								commandPlottable = new boolean[commandCount];
								commandInDesc = new String[commandCount];
								commandOutDesc = new String[commandCount];
								for (int i = 0; i < commandNames.length; i++) {
									commandNames[i] = response.getString("command" + i);
									commandInType[i] = response.getInt("inType" + i);
									commandOutType[i] = response.getInt("outType" + i);
									commandPlottable[i] = response.getBoolean("isPlottable" + i);
									commandInDesc[i] = response.getString("inDesc" + i);
									commandOutDesc[i] = response.getString("outDesc" + i);
								}
								ListView lv = (ListView) rootView.findViewById(R.id.devicePanel_listView);
								ArrayAdapter<String> adapter =
										new ArrayAdapter<String>(getActivity(), R.layout.list_item, R.id.firstLine, commandNames);
								lv.setAdapter(adapter);
								lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
									@Override
									public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
										selectedCommandId = position;
										for (int a = 0; a < parent.getChildCount(); a++) {
											parent.getChildAt(a).setBackgroundColor(Color.TRANSPARENT);
										}
										view.setBackgroundColor(Color.GRAY);
										String inType = Tango_CmdArgTypeName[commandInType[selectedCommandId]];
										String outType = Tango_CmdArgTypeName[commandOutType[selectedCommandId]];
										boolean clickablePlotButton = commandPlottable[selectedCommandId];
										Button plotButton = (Button) rootView.findViewById(R.id.devicePanel_plotButton);
										plotButton.setClickable(clickablePlotButton);
										plotButton.setEnabled(clickablePlotButton);
										Button descriptionButton = (Button) rootView.findViewById(R.id.devicePanel_descriptionButton);
										descriptionButton.setClickable(true);
										descriptionButton.setEnabled(true);
										Button executeButton = (Button) rootView.findViewById(R.id.devicePanel_executeButton);
										executeButton.setClickable(true);
										executeButton.setEnabled(true);
										TextView arginType = (TextView) getActivity().findViewById(R.id.devicePanel_arginTypeValue);
										arginType.setText(inType);
										TextView argoutType = (TextView) getActivity().findViewById(R.id.devicePanel_argoutTypeValue);
										argoutType.setText(outType);
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
						jsonRequestErrorHandler(error);
					}
				});
		jsObjRequest.setShouldCache(false);
		queue.add(jsObjRequest);

		Button descriptionButton = (Button) rootView.findViewById(R.id.devicePanel_descriptionButton);
		descriptionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
				builder.setTitle("Command \"" + commandNames[selectedCommandId] + " \"description");
				String message = new String(
						"Argin:\n" + commandInDesc[selectedCommandId] + "\nArgout:\n" + commandOutDesc[selectedCommandId]);
				builder.setMessage(message);
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		});

		Button executeButton = (Button) rootView.findViewById(R.id.devicePanel_executeButton);
		executeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText arginEditTextValue = (EditText) rootView.findViewById(R.id.devicePanel_arginValueEditText);
				String arginStr = arginEditTextValue.getText().toString();
				System.out.println("Have text from input: " + arginStr);
				if (arginStr.equals("")) {
					arginStr = "DevVoidArgument";
				}
				String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/command_inout.json/" +
						commandNames[selectedCommandId] + "/" + arginStr;
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										String commandOut = response.getString("commandReply");
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setMessage(commandOut).setTitle("Command output");
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

		Button plotButton = (Button) rootView.findViewById(R.id.devicePanel_plotButton);
		plotButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText arginEditTextValue = (EditText) rootView.findViewById(R.id.devicePanel_arginValueEditText);
				String arginStr = arginEditTextValue.getText().toString();
				System.out.println("Have text from input: " + arginStr);
				if (arginStr.equals("")) {
					arginStr = "DevVoidArgument";
				}
				String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/extract_plot_data.json/" +
						commandNames[selectedCommandId] + "/" + arginStr;
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										String commandOut = response.getString("replyHeader");
										System.out.println("Reply header: " + commandOut);
										JSONArray array = response.getJSONArray("plotData");
										double[] values = new double[array.length()];
										for (int i = 0; i < array.length(); i++) {
											values[i] = array.getDouble(i);
										}
										DataToPlot dtp = new DataToPlot(values);
										Intent intent = new Intent(getActivity(), PlotActivity.class);
										intent.putExtra("data", dtp);
										intent.putExtra("domainLabel", commandNames[selectedCommandId]);
										// intent.putExtra("plotTitle", dp.name()+"/"+commInfo.cmd_name);
										startActivity(intent);
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
		return rootView;
	}

	/**
	 * Adds value to DeviceData.
	 *
	 * @param argin   Value to be added.
	 * @param send    Value will be added to this DeviceData.
	 * @param outType Identifier of data type.
	 * @return DeviceData with new value.
	 * @throws NumberFormatException
	 */
	private DeviceData insertData(String argin, DeviceData send, int outType) throws NumberFormatException {

		if (outType == Tango_DEV_VOID)
			return send;

		ArgParser arg = new ArgParser(argin);

		switch (outType) {
			case Tango_DEV_BOOLEAN:
				send.insert(arg.parse_boolean());
				break;
			case Tango_DEV_USHORT:
				send.insert_us(arg.parse_ushort());
				break;
			case Tango_DEV_SHORT:
				send.insert(arg.parse_short());
				break;
			case Tango_DEV_ULONG:
				send.insert_ul(arg.parse_ulong());
				break;
			case Tango_DEV_LONG:
				send.insert(arg.parse_long());
				break;
			case Tango_DEV_FLOAT:
				send.insert(arg.parse_float());
				break;
			case Tango_DEV_DOUBLE:
				send.insert(arg.parse_double());
				break;
			case Tango_DEV_STRING:
				send.insert(arg.parse_string());
				break;
			case Tango_DEVVAR_CHARARRAY:
				send.insert(arg.parse_char_array());
				break;
			case Tango_DEVVAR_USHORTARRAY:
				send.insert_us(arg.parse_ushort_array());
				break;
			case Tango_DEVVAR_SHORTARRAY:
				send.insert(arg.parse_short_array());
				break;
			case Tango_DEVVAR_ULONGARRAY:
				send.insert_ul(arg.parse_ulong_array());
				break;
			case Tango_DEVVAR_LONGARRAY:
				send.insert(arg.parse_long_array());
				break;
			case Tango_DEVVAR_FLOATARRAY:
				send.insert(arg.parse_float_array());
				break;
			case Tango_DEVVAR_DOUBLEARRAY:
				send.insert(arg.parse_double_array());
				break;
			case Tango_DEVVAR_STRINGARRAY:
				send.insert(arg.parse_string_array());
				break;
			case Tango_DEVVAR_LONGSTRINGARRAY:
				send.insert(new DevVarLongStringArray(arg.parse_long_array(), arg.parse_string_array()));
				break;
			case Tango_DEVVAR_DOUBLESTRINGARRAY:
				send.insert(new DevVarDoubleStringArray(arg.parse_double_array(), arg.parse_string_array()));
				break;
			case Tango_DEV_STATE:
				send.insert(DevState.from_int(arg.parse_ushort()));
				break;

			default:
				throw new NumberFormatException("Command type not supported code=" + outType);

		}
		return send;

	}

	/**
	 * Extract data from DeviceData to one dimensional array.
	 *
	 * @param data    DeviceData to extract data from.
	 * @param outType Identifier of data type.
	 * @return Array of data that can be plotted.
	 */
	private double[] extractPlotData(DeviceData data, int outType) {

		double[] ret = new double[0];
		int i;

		switch (outType) {

			case Tango_DEVVAR_CHARARRAY: {
				byte[] dummy = data.extractByteArray();
				int start = this.getLimitMinForPlot(dummy.length);
				int end = this.getLimitMaxForPlot(dummy.length);
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEVVAR_USHORTARRAY: {
				int[] dummy = data.extractUShortArray();
				int start = this.getLimitMinForPlot(dummy.length);
				int end = this.getLimitMaxForPlot(dummy.length);
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEVVAR_SHORTARRAY: {
				short[] dummy = data.extractShortArray();
				int start = this.getLimitMinForPlot(dummy.length);
				int end = this.getLimitMaxForPlot(dummy.length);
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEVVAR_ULONGARRAY: {
				long[] dummy = data.extractULongArray();
				int start = this.getLimitMinForPlot(dummy.length);
				int end = this.getLimitMaxForPlot(dummy.length);
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEVVAR_LONGARRAY: {
				int[] dummy = data.extractLongArray();
				int start = this.getLimitMinForPlot(dummy.length);
				int end = this.getLimitMaxForPlot(dummy.length);
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEVVAR_FLOATARRAY: {
				float[] dummy = data.extractFloatArray();
				int start = this.getLimitMinForPlot(dummy.length);
				int end = this.getLimitMaxForPlot(dummy.length);
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEVVAR_DOUBLEARRAY: {
				double dummy[] = data.extractDoubleArray();
				int start = this.getLimitMinForPlot(dummy.length);
				int end = this.getLimitMaxForPlot(dummy.length);
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = dummy[i];
			}
			break;
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

	/**
	 * Method displaying info about connection error
	 *
	 * @param error Error tah caused exception
	 */
	private void jsonRequestErrorHandler(VolleyError error) {
		// Print error message to LogcCat
		System.out.println("Connection error!");
		error.printStackTrace();
		//System.out.println("getMessage: "+error.getMessage());
		//System.out.println("toString: "+error.toString());
		//System.out.println("getCause: "+error.getCause());
		//System.out.println("getStackTrace: "+error.getStackTrace().toString());

		// show dialog box with error message
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(error.toString()).setTitle("Connection error!").setPositiveButton(getString(R.string.ok_button),
				null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

}