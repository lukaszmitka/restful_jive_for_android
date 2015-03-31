package pl.edu.uj.synchrotron.restfuljive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import fr.esrf.TangoDs.TangoConst;

import static java.util.concurrent.TimeUnit.MILLISECONDS;


public class ATKPanelActivity extends Activity implements TangoConst {
	public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
	private static final int DEFAULT_REFRESHING_PERIOD = 1000;
	private int refreshingPeriod = DEFAULT_REFRESHING_PERIOD;
	private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
	private final Context context = this;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private List<String> commandArray;
	private List<String> commandInTypeArray;
	private List<String> commandOutTypeArray;
	private List<String> scalarAttrbuteArray;
	private List<String> nonScalarAttributeArray;
	private boolean[] attributeWritableArray;
	private String attributeName;
	private String deviceName;
	private String restHost;
	private String tangoHost;
	private String tangoPort;
	private int[][] ids;
	private int numberOfScalars;
	private Runnable r;
	private ScheduledFuture sFuture;
	private RequestQueue queue;
	private boolean firstSelection;

	/**
	 * Generate a value suitable for use in setId
	 * This value will not collide with ID values generated at build time by aapt for R.id.
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
		StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old).permitNetwork().build());

		// start volley request queue
		queue = Volley.newRequestQueue(this);
		queue.start();

		// gett device name from intent if was set
		Intent i = getIntent();
		Log.v("ATK Panel onCreate", "Got intent");
		if (i.hasExtra("DEVICE_NAME")) {
			Log.d("ATK Panel onCreate", "Got device name from intent");
			deviceName = i.getStringExtra("DEVICE_NAME");
			setTitle(getString(R.string.title_activity_atkpanel) + " : " + deviceName);

		} else { // prompt user for device name
			Log.d("ATK Panel onCreate", "Requesting device name from user");
			setDeviceName();
		}

		// check if tango host and rest address is saved in config, else prompt user for it
		if (i.hasExtra("restHost") && i.hasExtra("tangoHost") && i.hasExtra("tangoPort")) {
			Log.d("ATK Panel onCreate", "Got host from intent");
			restHost = i.getStringExtra("restHost");
			tangoHost = i.getStringExtra("tangoHost");
			tangoPort = i.getStringExtra("tangoPort");
			populatePanel();
		} else {
			Log.d("ATK Panel onCreate", "Request host from user");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			String settingsRestHost = settings.getString("RESTfulTangoHost", "");
			String settingsTangoHost = settings.getString("TangoHost", "");
			String settingsTangoPort = settings.getString("TangoPort", "");
			Log.d("ATK Panel onCreate", "Found RESTful host: " + settingsRestHost);
			Log.d("ATK Panel onCreate", "Found Tango host: " + settingsTangoHost);
			Log.d("ATK Panel onCreate", "Found Tango port: " + settingsTangoPort);
			if (settingsRestHost.equals("") || settingsTangoHost.equals("") || settingsTangoPort.equals("")) {
				Log.d("ATK Panel onCreate", "Requesting new tango host,port and RESTful host");
				setHost();
			} else {
				restHost = settingsRestHost;
				tangoHost = settingsTangoHost;
				tangoPort = settingsTangoPort;
				Log.d("ATK Panel onCreate", "Populating panel from server:  " + restHost + "at Tango Host: " +
						settingsTangoHost + ":" + settingsTangoPort);
				populatePanel();
			}
		}

		// define thread for refreshing attribute values
		r = new Runnable() {
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
				String urlReadAttributesQuery = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort +
						"/Device/" + deviceName + "/read_attributes";
				HeaderJsonObjectRequest jsObjRequestReadAttributes =
						new HeaderJsonObjectRequest(Request.Method.PUT, urlReadAttributesQuery, request,
								new Response.Listener<JSONObject>() {
									@Override
									public void onResponse(JSONObject response) {
										try {
											if (response.getString("connectionStatus").equals("OK")) {
												Log.d("Runnable run()", "Device connection OK");
												updateScalarListView(response);
											} else {
												Log.d("Runnable run()", "Tango database API returned message:");
												Log.d("Runnable run()", response.getString("connectionStatus"));
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
						});
				//queue.getCache().clear();
				jsObjRequestReadAttributes.setShouldCache(false);
				queue.add(jsObjRequestReadAttributes);
			}
		};
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_atkpanel, menu);
		return true;
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
					if (sFuture != null) {
						sFuture.cancel(true);
					}
					sFuture = scheduler.scheduleAtFixedRate(r, period, period, MILLISECONDS);
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
		if (sFuture != null) {
			sFuture.cancel(true);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

	private void populatePanel() {
		Log.v("ATK Panel populatePanel", "Populating panel");
		// you need to have a list of data that you want the spinner to display
		//RequestQueue queue = Volley.newRequestQueue(this);
		//queue.start();
		String urlCommandListQuery = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort +
				"/Device/" + deviceName + "/command_list_query.json";
		HeaderJsonObjectRequest jsObjRequestCommands =
				new HeaderJsonObjectRequest(Request.Method.GET, urlCommandListQuery, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("ATK Panel populatePanel", "Device connection OK");
								populateCommandSpinner(response);
							} else {
								Log.d("ATK Panel populatePanel", "Tango database API returned message:");
								Log.d("ATK Panel populatePanel", response.getString("connectionStatus"));
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
				});
		jsObjRequestCommands.setShouldCache(false);
		queue.add(jsObjRequestCommands);


		String urlGetStatus = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort +
				"/Device/" + deviceName + "/command_inout.json/Status/DevVoidArgument";
		HeaderJsonObjectRequest jsObjRequestStatus =
				new HeaderJsonObjectRequest(Request.Method.PUT, urlGetStatus, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("ATK Panel populatePanel", "Device connection OK");
								populateStatus(response);
							} else {
								Log.d("ATK Panel populatePanel", "Tango database API returned message:");
								Log.d("ATK Panel populatePanel", response.getString("connectionStatus"));
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
				});
		jsObjRequestStatus.setShouldCache(false);
		queue.add(jsObjRequestStatus);

		String urlGetAttribuesList = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort +
				"/Device/" + deviceName + "/get_attribute_list.json";
		HeaderJsonObjectRequest jsObjRequestAttributeList =
				new HeaderJsonObjectRequest(Request.Method.GET, urlGetAttribuesList, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("ATK Panel populatePanel", "Device connection OK");
								populateAttributeSpinner(response);
							} else {
								Log.d("ATK Panel populatePanel", "Tango database API returned message:");
								Log.d("ATK Panel populatePanel", response.getString("connectionStatus"));
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
				});
		jsObjRequestAttributeList.setShouldCache(false);
		queue.add(jsObjRequestAttributeList);
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

	private void populateCommandSpinner(JSONObject response) {
		Log.d("populateCommandSpinner", "Populating command spinner");
		commandArray = new ArrayList<String>();
		commandInTypeArray = new ArrayList<String>();
		commandOutTypeArray = new ArrayList<String>();
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
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(
					this, android.R.layout.simple_spinner_item, commandArray);
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

	private void executeCommand(String commandName, String arginStr) {

		String url = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/command_inout.json/" +
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
				});
		jsObjRequest.setShouldCache(false);
		queue.add(jsObjRequest);
	}

	private void populateStatus(JSONObject response) {
		try {
			String status = response.getString("commandReply");
			TextView statusTextView = (TextView) findViewById(R.id.atk_panel_status_text_view);
			statusTextView.setText(status);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void populateAttributeSpinner(JSONObject response) {
		Log.d("popAttributeSpinner", "Populating attribute spinner");
		scalarAttrbuteArray = new ArrayList<String>();
		nonScalarAttributeArray = new ArrayList<String>();

		try {
			int attributeCount = response.getInt("attCount");
			attributeWritableArray = new boolean[attributeCount];
			for (int i = 0; i < attributeCount; i++) {
				attributeName = response.getString("attribute" + i);
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
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						this, android.R.layout.simple_spinner_item, nonScalarAttributeArray);
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
							//TODO add attribute plot
							//RelativeLayout container = (RelativeLayout) findViewById(R.id.atkPanel_innerLayout);
							//container.addView(plot);
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

	private void populateScalarListView() {
		if (!scalarAttrbuteArray.isEmpty()) {

			Log.d("populateScalarListView", "Getting RelativeLayout");
			RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.atkPanel_innerLayout);
			numberOfScalars = scalarAttrbuteArray.size();
			ids = new int[numberOfScalars][5];
			for (int i = 0; i < numberOfScalars; i++) {
				RelativeLayout.LayoutParams textViewLayParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams
						.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				RelativeLayout.LayoutParams editTextLayParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams
						.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				RelativeLayout.LayoutParams buttonLayParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams
						.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);

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
			String urlReadAttributesQuery = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort +
					"/Device/" + deviceName + "/read_attributes";
			HeaderJsonObjectRequest jsObjRequestCommands =
					new HeaderJsonObjectRequest(Request.Method.PUT, urlReadAttributesQuery, request,
							new Response.Listener<JSONObject>() {
								@Override
								public void onResponse(JSONObject response) {
									try {
										if (response.getString("connectionStatus").equals("OK")) {
											Log.d("populateScalarListView", "Device connection OK");
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
					});
			jsObjRequestCommands.setShouldCache(false);
			queue.add(jsObjRequestCommands);
			sFuture = scheduler.scheduleAtFixedRate(r, refreshingPeriod, refreshingPeriod, MILLISECONDS);
		}
	}

	private void updateAttribute(String attName, String argin) {
		String url = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/write_attribute.json/" + attName + "/" +
				argin;
		JsonObjectRequest jsObjRequest =
				new JsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.v("Update attribute", "Device connection OK");
								Toast.makeText(getApplicationContext(), response.getString("connectionStatus"),
										Toast.LENGTH_SHORT).show();
							} else {
								Toast.makeText(getApplicationContext(), response.getString("connectionStatus"),
										Toast.LENGTH_SHORT).show();
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
