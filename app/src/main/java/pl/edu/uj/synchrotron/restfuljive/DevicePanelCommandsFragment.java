package pl.edu.uj.synchrotron.restfuljive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

public class DevicePanelCommandsFragment extends CertificateExceptionFragment implements TangoConst {
	private int selectedCommandId;
	private View rootView;
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
	private String userName, userPassword;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		rootView = inflater.inflate(R.layout.fragment_device_panel_commands, container, false);
		deviceName = ((DevicePanelActivity) getActivity()).getDeviceName();
		TextView tvDeviceName = (TextView) rootView.findViewById(R.id.devicePanel_deviceName);
		tvDeviceName.setText(deviceName);
		Log.d("onCreateView", "Device name: " + deviceName);
		RESTfulTangoHost = ((DevicePanelActivity) getActivity()).getRestHost();
		tangoHost = ((DevicePanelActivity) getActivity()).getTangoHost();
		tangoPort = ((DevicePanelActivity) getActivity()).getTangoPort();
		userName = ((DevicePanelActivity) getActivity()).getUserName();
		userPassword = ((DevicePanelActivity) getActivity()).getUserPassword();
		Log.d("onCreateView", "Host: " + RESTfulTangoHost);
		context = ((DevicePanelActivity) getActivity()).getContext();
		restartQueue();
		String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/command_list_query";
		HeaderJsonObjectRequest jsObjRequest =
				new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("onCreate()", "Device connection OK / method GET / got command list");
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
										new ArrayAdapter<>(getActivity(), R.layout.list_item, R.id.firstLine, commandNames);
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
								Log.d("onCreate()", "Tango database API returned message from query command_list_query:");
								Log.d("onCreate()", response.getString("connectionStatus"));
							}
						} catch (JSONException e) {
							Log.d("onCreate()", "Problem with JSON object while getting command list");
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

		Button descriptionButton = (Button) rootView.findViewById(R.id.devicePanel_descriptionButton);
		descriptionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
				builder.setTitle("Command \"" + commandNames[selectedCommandId] + " \"description");
				String message =
						"Argin:\n" + commandInDesc[selectedCommandId] + "\nArgout:\n" + commandOutDesc[selectedCommandId];
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
				Log.d("executeButton.onClick()", "Have text from input: " + arginStr);
				if (arginStr.equals("")) {
					arginStr = "DevVoidArgument";
				}
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/command_inout/" +
						commandNames[selectedCommandId] + "/" + arginStr;
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										Log.d("executeButton.onClick()", "Device connection OK / method PUT / executed command");
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
										Log.d("executeButton.onClick()",
												"Tango database API returned message from query " + "command_imout:");
										Log.d("executeButton.onClick()", response.getString("connectionStatus"));
									}
								} catch (JSONException e) {
									Log.d("executeButton.onClick()", "Problem with JSON object while executing command");
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
		});

		Button plotButton = (Button) rootView.findViewById(R.id.devicePanel_plotButton);
		plotButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText arginEditTextValue = (EditText) rootView.findViewById(R.id.devicePanel_arginValueEditText);
				String arginStr = arginEditTextValue.getText().toString();
				Log.d("plotButton.onClick()", "Have text from input: " + arginStr);
				if (arginStr.equals("")) {
					arginStr = "DevVoidArgument";
				}
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/extract_plot_data/" +
						commandNames[selectedCommandId] + "/" + arginStr;
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										Log.d("plotButton.onClick()", "Device connection OK / method PUT / got plot data");
										String commandOut = response.getString("replyHeader");
										Log.d("plotButton.onClick()", "Reply header: " + commandOut);
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
										Log.d("plotButton.onClick()",
												"Tango database API returned message from query " + "extract+plot_data: ");
										Log.d("plotButton.onClick()", response.getString("connectionStatus"));
									}
								} catch (JSONException e) {
									Log.d("plotButton.onClick()", "Problem with JSON object while getting plot data");
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

		});
		return rootView;
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