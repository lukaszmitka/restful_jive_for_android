package pl.edu.uj.synchrotron.restfuljive;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class for creating properties activity screen.
 */
public class PropertiesActivity extends CertificateExceptionActivity {
	/**
	 * The intent that activity was called with.
	 */
	private Intent intent;
	/**
	 * Name of device, which attributes should be listed.
	 */
	private String deviceName;
	/**
	 * RESTful host address.
	 */
	private String RESTfulHost;
	/**
	 * Address of database to be used by REST service.
	 */
	private String tangoHost;
	/**
	 * Port of database to be used by REST service.
	 */
	private String tangoPort;
	/**
	 * Application context.
	 */
	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_properties);
		intent = getIntent();
		deviceName = intent.getStringExtra("deviceName");
		RESTfulHost = intent.getStringExtra("restHost");
		tangoHost = intent.getStringExtra("tangoHost");
		tangoPort = intent.getStringExtra("tangoPort");
		context = this;
		TextView devicePropTextView = (TextView) findViewById(R.id.devicePropertiesTextView1);
		devicePropTextView.setText("Device " + deviceName + " properties");
		setTitle("REST host: " + RESTfulHost + ", TANGO_HOST: " + tangoHost + ":" + tangoPort);

		restartQueue();
		refreshPropertiesList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_properties, menu);
		return true;
	}

	/**
	 * Refresh already shown list of properties.
	 */
	private void refreshPropertiesList() {

		Log.d("refreshPropertiesList()", "Device name: " + deviceName);
		Log.d("refreshPropertiesList()", "REST host: " + RESTfulHost);
		Log.d("refreshPropertiesList()", "Tango host: " + tangoHost + ":" + tangoPort);

		String url = RESTfulHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/get_property_list" +
				".json";
		JsonObjectRequest jsObjRequest =
				new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("refreshPropertiesList()", "Device connection OK / method GET / get_property_list");
								LinearLayout layout = (LinearLayout) findViewById(R.id.properties_activity_linear_layout);
								layout.removeAllViews();
								int propertyCount = response.getInt("propertyCount");
								Log.d("refreshPropertiesList()", "Received " + propertyCount + " properties");
								String[] prop_list = new String[propertyCount];
								String[] prop_values = new String[propertyCount];
								for (int i = 0; i < prop_list.length; i++) {
									prop_list[i] = response.getString("property" + i);
									prop_values[i] = response.getString("propValue" + i);
								}
								final LayoutInflater inflater = LayoutInflater.from(context);
								for (int i = 0; i < prop_list.length; i++) {
									Log.d("refreshPropertiesList()", "Processing property no. " + i);
									Log.d("refreshPropertiesList()", "Name: " + prop_list[i] + " Value: " + prop_values[i]);
									View view = inflater.inflate(R.layout.editable_list_element, null);
									EditText et = (EditText) view.findViewById(R.id.editableListEditText);
									TextView tv = (TextView) view.findViewById(R.id.editableListTextView);
									tv.setText(prop_list[i]);
									et.setText(prop_values[i]);
									et.setTag(prop_list[i]);
									layout.addView(view);
								}
							} else {
								Log.d("refreshPropertiesList()", "Tango database API returned message from query " +
										"get_property_list:");
								Log.d("refreshPropertiesList()", response.getString("connectionStatus"));
							}
						} catch (JSONException e) {
							Log.d("refreshPropertiesList()", "Problem with JSON object while getting property list");
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.d("refreshPropertiesList()", "Connection error!");
						error.printStackTrace();
					}
				});
		jsObjRequest.setShouldCache(false);
		queue.add(jsObjRequest);
	}

	/**
	 * Listener for the button click, refresh list of properties.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void propertiesListRefreshButton(View view) {
		refreshPropertiesList();
	}

	/**
	 * Listener for the button click, close the activity.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void devicePropertiesCancelButton(View view) {
		finish();
	}

	/**
	 * Listener for the button click, update properties.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void devicePropertiesUpdateButton(View view) {
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.properties_activity_linear_layout);
		int childCount = linearLayout.getChildCount();

		for (int i = 0; i < childCount; i++) {
			View linearLayoutView = linearLayout.getChildAt(i);
			EditText et = (EditText) linearLayoutView.findViewById(R.id.editableListEditText);
			String value = et.getText().toString();
			String tag = (String) et.getTag();
			String url =
					RESTfulHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName + "/put_property" +
							".json/" + tag + "/" + value;
			JsonObjectRequest jsObjRequest =
					new JsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
						@Override
						public void onResponse(JSONObject response) {
							try {
								if (response.getString("connectionStatus").equals("OK")) {
									Log.d("updateButton.onClick()", "Device connection OK / method PUT / put_property");
								} else {
									Toast.makeText(getApplicationContext(), response.getString("connectionStatus"),
											Toast.LENGTH_SHORT).show();
									Log.d("updateButton.onClick()", "Tango database API returned message from query put_property:");
									Log.d("updateButton.onClick()", response.getString("connectionStatus"));
								}
							} catch (JSONException e) {
								Log.d("updateButton.onClick()", "Problem with JSON object while updating property");
								e.printStackTrace();
							}
						}
					}, new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.d("updateButton.onClick()", "Connection error!");
							error.printStackTrace();
						}
					});
			jsObjRequest.setShouldCache(false);
			queue.add(jsObjRequest);
		}
	}
}
