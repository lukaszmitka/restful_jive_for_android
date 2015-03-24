package pl.edu.uj.synchrotron.restfuljive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class for creating device panel admin fragment.
 */
public class DevicePanelAdminFragment extends Fragment {

	EditText answerLimitMinEditText;
	EditText answerLimitMaxEditText;
	EditText timeoutEditText;
	EditText blackBoxEditText;
	//private DeviceProxy device = null;
	//private DeviceProxy deviceAdm = null;
	private int answerLimitMin = 0;
	private int answerLimitMax = 1024;
	private View rootView;
	private Context context;
	private String RESTfulTangoHost;
	private String tangoHost;
	private String tangoPort;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_device_panel_admin, container, false);

		final String deviceName = ((DevicePanelActivity) getActivity()).getDeviceName();
		System.out.println("Device name: " + deviceName);
		RESTfulTangoHost = ((DevicePanelActivity) getActivity()).getRestHost();
		tangoHost = ((DevicePanelActivity) getActivity()).getTangoHost();
		tangoPort = ((DevicePanelActivity) getActivity()).getTangoPort();
		System.out.println("Host: " + RESTfulTangoHost);
		context = ((DevicePanelActivity) getActivity()).getContext();

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
				System.out.println("Processing BlackBox button");
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();
				String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/black_box.json/" +
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
						});
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
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();
				String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/set_timeout_milis.json/" +
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
						});
				queue.add(jsObjRequest);
			}
		});

		Button deviceInfo = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_deviceInfoButton);
		deviceInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("Processing deviceInfo button");
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();
				String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/get_device_info.json";
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
						});
				queue.add(jsObjRequest);
			}
		});

		Button pingDevice = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_pingDeviceButton);
		pingDevice.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("Processing deviceInfo button");
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();
				String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/ping_device.json";
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
						});
				queue.add(jsObjRequest);
			}
		});

		Button pollingStatus = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_pollStButton);
		pollingStatus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				System.out.println("Processing deviceInfo button");
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();
				String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/poll_status.json";
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
						});
				queue.add(jsObjRequest);
			}
		});

		Button restart = (Button) rootView.findViewById(R.id.devicePanel_adminFragment_restartButton);
		restart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				System.out.println("Processing deviceInfo button");
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();
				String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/restart.json";
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
						});
				queue.add(jsObjRequest);
			}
		});

		RequestQueue queue = Volley.newRequestQueue(context);
		queue.start();
		String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/get_source.json";
		System.out.println("Sending JSON request");
		HeaderJsonObjectRequest jsObjRequest =
				new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								System.out.println("Device connection OK");
								int source = response.getInt("source");
								populateSpinner(source);
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
				});
		queue.add(jsObjRequest);
		return rootView;
	}

	public int getAnswerLimitMin() {
		return answerLimitMin;
	}

	public int getAnswerLimitMax() {
		return answerLimitMax;
	}

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
				RequestQueue queue = Volley.newRequestQueue(context);
				queue.start();

				String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + devName +
						"/set_source.json/" + sourceId;
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
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
						});
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
