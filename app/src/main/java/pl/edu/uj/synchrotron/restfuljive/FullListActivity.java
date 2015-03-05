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
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class FullListActivity extends Activity {
	public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
	private Context context = this;
	private String RESTfulTangoHost;
	private String tangoHost;
	private String tangoPort;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_list);
		// this allows to connect with server in main thread
		StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old).permitNetwork().build());


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
			RESTfulTangoHost = settingsRestHost;
			tangoHost = settingsTangoHost;
			tangoPort = settingsTangoPort;
			System.out.println("Getting device list from server:  " + RESTfulTangoHost + "at Tango Host: " +
					settingsTangoHost + ":" +
					settingsTangoPort);
			try {
				refreshDeviceList(RESTfulTangoHost);
			} catch (Exception e) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage("Problem with connecting to REST server, check if internet connection is available and " +
						"server address is set properly")
						.setTitle("Error");
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		}
		setTitle("REST host: " + RESTfulTangoHost + ", TANGO_HOST: " + tangoHost + ":" + tangoPort);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_full_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.action_set_host:
				setHost();
				return true;
			case R.id.action_sorted_list:
				showSortedList();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Listener for "Refresh" button, refreshes list of devices.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void button1_OnClick(View view) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		refreshDeviceList(RESTfulTangoHost);
	}

	/**
	 * Start new activity for getting from user database host address and port.
	 */
	private void setHost() {
		Intent i = new Intent(this, SetHostActivity.class);
		if (!tangoHost.equals("") && tangoHost != null) {
			i.putExtra("tangoHost", tangoHost);
		}
		if (!tangoPort.equals("") && tangoPort != null) {
			i.putExtra("tangoPort", tangoPort);
		}
		if (!RESTfulTangoHost.equals("") && RESTfulTangoHost != null) {
			i.putExtra("restHost", RESTfulTangoHost);
		}
		startActivityForResult(i, 1);
	}

	/**
	 * Start new activity with sorted list of devices.
	 */
	private void showSortedList() {
		Intent i = new Intent(this, SortedList.class);
		i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(i);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				RESTfulTangoHost = data.getStringExtra("restHost");
				tangoHost = data.getStringExtra("TangoHost");
				tangoPort = data.getStringExtra("TangoPort");
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("RESTfulTangoHost", RESTfulTangoHost);
				editor.putString("TangoHost", tangoHost);
				editor.putString("TangoPort", tangoPort);
				editor.commit();
				System.out.println("Result: " + RESTfulTangoHost);
				refreshDeviceList(RESTfulTangoHost);
				setTitle("REST host: " + RESTfulTangoHost + ", TANGO_HOST: " + tangoHost + ":" + tangoPort);
			}
			if (resultCode == RESULT_CANCELED) {
				System.out.println("Host not changed");
			}
		}
	}

	/**
	 * Refresh currently shown list of devices.
	 *
	 * @param RESTfulTangoHost Address of server with restful tango interface
	 */
	private void refreshDeviceList(String RESTfulTangoHost) {
		RequestQueue queue = Volley.newRequestQueue(this);
		queue.start();
		String url = RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device.json";
		JsonObjectRequest jsObjRequest =
				new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								//System.out.println("Device connection OK");

								int deviceCount;
								deviceCount = response.getInt("numberOfDevices");
								String devices[] = new String[deviceCount];
								for (int i = 0; i < deviceCount; i++) {
									// System.out.println("Device " + i + ": " + response.getString("device" + i));

									devices[i] = response.getString("device" + i);
									ListView deviceList = (ListView) findViewById(R.id.listView1);
									ArrayAdapter<String> adapter =
											new ArrayAdapter<String>(context, R.layout.n_level_list_member_item, R.id.firstLine,
													devices);
									deviceList.setAdapter(adapter);
									deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

										@Override
										public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
											TextView selectedDeviceName = (TextView) findViewById(R.id.deviceName);
											final String item = (String) parent.getItemAtPosition(position);
											selectedDeviceName.setText(item);
										}
									});
								}
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
}