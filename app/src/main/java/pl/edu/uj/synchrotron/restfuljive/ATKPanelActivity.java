package pl.edu.uj.synchrotron.restfuljive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class ATKPanelActivity extends Activity {
	public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
	final Context context = this;
	List<String> commandArray;
	List<String> scalarAttrbuteArray;
	List<String> nonScalarAttributeArray;
	String attributeName;
	private String deviceName;
	private String restHost;
	private String tangoHost;
	private String tangoPort;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_atkpanel);
		StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old).permitNetwork().build());
		Intent i = getIntent();
		System.out.println("Got intent");
		if (i.hasExtra("DEVICE_NAME")) {
			System.out.println("Got device name from intent");
			deviceName = i.getStringExtra("DEVICE_NAME");
			setTitle(getString(R.string.title_activity_atkpanel) + " : " + deviceName);
		} else {
			System.out.println("Requesting device name from user");
			setDeviceName();
		}

		if (i.hasExtra("restHost") && i.hasExtra("tangoHost") && i.hasExtra("tangoPort")) {
			System.out.println("Got host from intent");
			restHost = i.getStringExtra("restHost");
			tangoHost = i.getStringExtra("tangoHost");
			tangoPort = i.getStringExtra("tangoPort");
			populatePanel();
		} else {
			System.out.println("Request host from user");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			String settingsRestHost = settings.getString("RESTfulTangoHost", "");
			String settingsTangoHost = settings.getString("TangoHost", "");
			String settingsTangoPort = settings.getString("TangoPort", "");
			System.out.println("Found RESTful host: " + settingsRestHost);
			System.out.println("Found Tango host: " + settingsTangoHost);
			System.out.println("Found Tango port: " + settingsTangoPort);
			if (settingsRestHost.equals("") || settingsTangoHost.equals("") || settingsTangoPort.equals("")) {
				System.out.println("Requesting new tango host,port and RESTful host");
				setHost();
			} else {
				restHost = settingsRestHost;
				tangoHost = settingsTangoHost;
				tangoPort = settingsTangoPort;
				System.out.println("Populating panel from server:  " + restHost + "at Tango Host: " +
						settingsTangoHost + ":" +
						settingsTangoPort);
				//try {
				populatePanel();
				/*} catch (Exception e) {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setMessage("Problem with connecting to REST server, check if internet connection is available and " +
							"server address is set properly")
							.setTitle("Error");
					AlertDialog dialog = builder.create();
					dialog.show();
				}*/
			}
		}
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
		if (id == R.id.action_settings) {
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
				System.out.println("Result: " + restHost);
				if (deviceName != null) {
					populatePanel();
				} else {
					//setDeviceName();
				}
			}
			if (resultCode == RESULT_CANCELED) {
				System.out.println("Host not changed");
			}
		}
		if (requestCode == 2) {
			if (resultCode == RESULT_OK) {
				deviceName = data.getStringExtra("DEVICE_NAME");
				System.out.println("Result: " + deviceName);
				setTitle(getString(R.string.title_activity_atkpanel) + " : " + deviceName);
				if (tangoHost != null && tangoPort != null && restHost != null) {
					populatePanel();
				} else {
					//setHost();
				}
			}
			if (resultCode == RESULT_CANCELED) {
				System.out.println(R.string.atk_panel_dev_not_set);
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
		System.out.println("Populating panel");
		// you need to have a list of data that you want the spinner to display
		RequestQueue queue = Volley.newRequestQueue(this);
		queue.start();
		String urlCommandListQuery = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort +
				"/Device/" + deviceName + "/command_list_query.json";
		HeaderJsonObjectRequest jsObjRequestCommands =
				new HeaderJsonObjectRequest(Request.Method.GET, urlCommandListQuery, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								System.out.println("Device connection OK");
								populateCommandSpinner(response);
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
		queue.add(jsObjRequestCommands);


		String urlGetStatus = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort +
				"/Device/" + deviceName + "/command_inout.json/Status/DevVoidArgument";
		HeaderJsonObjectRequest jsObjRequestStatus =
				new HeaderJsonObjectRequest(Request.Method.PUT, urlGetStatus, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								System.out.println("Device connection OK");
								populateStatus(response);
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
		queue.add(jsObjRequestStatus);

		String urlGetAttribuesList = restHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort +
				"/Device/" + deviceName + "/get_attribute_list.json";
		HeaderJsonObjectRequest jsObjRequestAttributeList =
				new HeaderJsonObjectRequest(Request.Method.GET, urlGetAttribuesList, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								System.out.println("Device connection OK");
								populateAttributeSpinner(response);
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
		queue.add(jsObjRequestAttributeList);
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

	private void populateCommandSpinner(JSONObject response) {
		System.out.println("Populating command spinner");
		commandArray = new ArrayList<String>();
		try {
			int commandCount = response.getInt("commandCount");
			String commandName;
			for (int i = 0; i < commandCount; i++) {
				commandName = response.getString("command" + i);
				commandArray.add(commandName);
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(
					this, android.R.layout.simple_spinner_item, commandArray);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			Spinner sItems = (Spinner) findViewById(R.id.atk_panel_command_spinner);
			sItems.setAdapter(adapter);
		} catch (JSONException e) {
			e.printStackTrace();
		}
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
		System.out.println("Populating attribute spinner");
		scalarAttrbuteArray = new ArrayList<String>();
		nonScalarAttributeArray = new ArrayList<String>();
		try {
			int attributeCount = response.getInt("attCount");
			for (int i = 0; i < attributeCount; i++) {
				attributeName = response.getString("attribute" + i);
				if (response.getBoolean("attScalar" + i)) {
					scalarAttrbuteArray.add(attributeName);
				} else {
					nonScalarAttributeArray.add(attributeName);
				}
			}
			if (!nonScalarAttributeArray.isEmpty()) {
				nonScalarAttributeArray.add(0, "Scalar");
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						this, android.R.layout.simple_spinner_item, commandArray);
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

			ListView scalarAttributeListView = new ListView(this);
			RelativeLayout container = (RelativeLayout) findViewById(R.id.atkPanel_innerLayout);
			container.addView(scalarAttributeListView);
			ArrayAdapter adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1,
					scalarAttrbuteArray);
			scalarAttributeListView.setAdapter(adapter);
		}
	}


}
