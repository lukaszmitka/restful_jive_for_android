package pl.edu.uj.synchrotron.restfuljive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class for creating device panel admin fragment.
 */
public class DevicePanelAdminFragment extends CertificateExceptionFragment {

	EditText answerLimitMinEditText;
	EditText answerLimitMaxEditText;
	EditText timeoutEditText;
	EditText blackBoxEditText;
	private int answerLimitMin = 0;
	private int answerLimitMax = 1024;
	private View rootView;
	private Context context;
	private String RESTfulTangoHost;
	private String tangoHost;
	private String tangoPort;
	private String userName, userPassword;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		rootView = inflater.inflate(R.layout.fragment_device_panel_admin, container, false);

		final String deviceName = ((DevicePanelActivity) getActivity()).getDeviceName();
		Log.d("onCreateView()", "Device name: " + deviceName);
		RESTfulTangoHost = ((DevicePanelActivity) getActivity()).getRestHost();
		tangoHost = ((DevicePanelActivity) getActivity()).getTangoHost();
		tangoPort = ((DevicePanelActivity) getActivity()).getTangoPort();
		userName = ((DevicePanelActivity) getActivity()).getUserName();
		userPassword = ((DevicePanelActivity) getActivity()).getUserPassword();
		Log.d("onCreateView()", "Host: " + RESTfulTangoHost);
		context = ((DevicePanelActivity) getActivity()).getContext();

		restartQueue();
		answerLimitMinEditText = (EditText) rootView.findViewById(R.id.devicePanel_adminFragment_limitMinEditText);
		answerLimitMinEditText.setText("" + answerLimitMin);
		answerLimitMaxEditText = (EditText) rootView.findViewById(R.id.devicePanel_adminFragment_limitMaxEditText);
		answerLimitMaxEditText.setText("" + answerLimitMax);
		timeoutEditText = (EditText) rootView.findViewById(R.id.devicePanel_adminFragment_timeoutEditText);

		//timeoutEditText.setText(Integer.toString(device.get_timeout_millis()));
		blackBoxEditText = (EditText) rootView.findViewById(R.id.devicePanel_adminFragment_blackboxEditText);
		blackBoxEditText.setText("10");

		Button blackBoxButton = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_BlackBoxButton);
		blackBoxButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String nbCmd = blackBoxEditText.getText().toString();
				Log.d("blackBoxButton.onClick", "Processing BlackBox button");

				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/black_box/" +
						nbCmd;
				System.out.println("Sending JSON request");
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Black Box response");
										String message = response.getString("blackBoxReply");
										builder.setMessage(message);
										builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
											}
										});
										AlertDialog dialog = builder.create();
										dialog.show();
									} else {
										System.out.println("Tango database API returned message:");
										System.out.println(response.getString("connectionStatus"));
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Command error");
										String message = response.getString("connectionStatus");
										builder.setMessage(message);
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
		});

		Button setAnswerLimitMin = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_LimitMinButton);
		setAnswerLimitMin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				answerLimitMin = Integer.parseInt(answerLimitMinEditText.getText().toString());
			}
		});

		Button setAnswerLimitMax = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_LimitMaxButton);
		setAnswerLimitMax.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				answerLimitMax = Integer.parseInt(answerLimitMaxEditText.getText().toString());
			}
		});

		Button setTimeout = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_timeoutButton);
		setTimeout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("Processing timeout button");
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/set_timeout_milis/" +
						timeoutEditText.getText().toString();
				System.out.println("Sending JSON request");
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Command response");
										builder.setMessage("Timeout updated");
										builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
											}
										});
										AlertDialog dialog = builder.create();
										dialog.show();
									} else {
										System.out.println("Tango database API returned message:");
										System.out.println(response.getString("connectionStatus"));
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Command error");
										String message = response.getString("connectionStatus");
										builder.setMessage(message);
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
		});

		Button deviceInfo = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_deviceInfoButton);
		deviceInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("Processing deviceInfo button");
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/get_device_info";
				System.out.println("Sending JSON request");
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Device Info");
										String message = response.getString("deviceInfo");
										builder.setMessage(message);
										builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
											}
										});
										AlertDialog dialog = builder.create();
										dialog.show();

									} else {
										System.out.println("Tango database API returned message:");
										System.out.println(response.getString("connectionStatus"));
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Command error");
										String message = response.getString("deviceInfo");
										builder.setMessage(message);
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
		});

		Button pingDevice = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_pingDeviceButton);
		pingDevice.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("Processing deviceInfo button");
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/ping_device";
				System.out.println("Sending JSON request");
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Ping device");
										String message = response.getString("pingStatus");
										builder.setMessage(message);
										builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
											}
										});
										AlertDialog dialog = builder.create();
										dialog.show();

									} else {
										System.out.println("Tango database API returned message:");
										System.out.println(response.getString("connectionStatus"));
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Command error");
										String message = response.getString("message");
										builder.setMessage(message);
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
		});

		Button pollingStatus = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_pollStButton);
		pollingStatus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				System.out.println("Processing deviceInfo button");
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/poll_status";
				System.out.println("Sending JSON request");
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Polling status");
										String message = response.getString("pollStatus");
										builder.setMessage(message);
										builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
											}
										});
										AlertDialog dialog = builder.create();
										dialog.show();
									} else {
										System.out.println("Tango database API returned message:");
										System.out.println(response.getString("connectionStatus"));
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Command error");
										String message = response.getString("message");
										builder.setMessage(message);
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
		});

		Button restart = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_restartButton);
		restart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				System.out.println("Processing deviceInfo button");
				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/restart";
				System.out.println("Sending JSON request");
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Restart command");
										builder.setMessage("Restart OK\n\n");
										builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
											}
										});
										AlertDialog dialog = builder.create();
										dialog.show();
									} else {
										System.out.println("Tango database API returned message:");
										System.out.println(response.getString("connectionStatus"));
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Command error");
										String message = response.getString("message");
										builder.setMessage(message);
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
		});

		String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/get_source";
		Log.d("onCreateView()", "Sending JSON request: get_source");
		HeaderJsonObjectRequest jsObjRequest =
				new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("onCreateView()", "Device connection OK / method GET / got source");
								int source = response.getInt("source");
								populateSpinner(source);
							} else {
								Log.d("onCreateView()", "Tango database API returned message from query get source:");
								Log.d("onCreateView()", response.getString("connectionStatus"));
								AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
								builder.setTitle("Command error");
								String message = response.getString("message");
								builder.setMessage(message);
								builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
									}
								});
								AlertDialog dialog = builder.create();
								dialog.show();
							}
						} catch (JSONException e) {
							Log.d("onCreateView()", "Problem with JSON object while getting source");
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
		return rootView;
	}

	/**
	 * Add source names to spinner.
	 *
	 * @param source
	 * 		ID of selected source, 0 - CACHE, 1 - CACHE_DEVICE, 2 - DEVICE.
	 */
	private void populateSpinner(int source) {
		Spinner spinner = (Spinner) rootView.findViewById(R.id.devicePanel_adminFragment_sourceSpinner);
		spinner.setSelection(source);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				int sourceId;
				String devName = ((DevicePanelActivity) getActivity()).getDeviceName();
				if (position == 0) // source = CACHE
				{
					sourceId = 0;
					System.out.println("Selected CACHE");

				} else if (position == 1) // source = CACHE_DEVICE
				{
					sourceId = 1;
					System.out.println("Selected CACHE_DEVICE");
				} else if (position == 2) // source = DEVICE
				{
					sourceId = 2;
					System.out.println("Selected DEVICE");
				} else {
					sourceId = -1;
				}

				String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + devName +
						"/set_source/" + sourceId;
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										Log.d("populateSpinner()", "Device connection OK / method PUT / set source");
									} else {
										Log.d("populateSpinner()", "Tango database API returned message from query set_source:");
										Log.d("populateSpinner()", response.getString("connectionStatus"));
										AlertDialog.Builder builder = new AlertDialog.Builder(rootView.getContext());
										builder.setTitle("Command error");
										String message = response.getString("connectionStatus");
										builder.setMessage(message);
										builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
											}
										});
										AlertDialog dialog = builder.create();
										dialog.show();
									}
								} catch (JSONException e) {
									Log.d("populateSpinner()", "Problem with JSON object while setting source");
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

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}

		});
	}

	/**
	 * Method displaying info about connection error
	 *
	 * @param error
	 * 		Error that caused exception
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
		builder.setMessage(error.toString()).setTitle("Connection error!")
				.setPositiveButton(getString(R.string.ok_button), null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}
}
