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

import org.json.JSONException;
import org.json.JSONObject;
//import fr.esrf.Tango.AttributeDataType;
//import fr.esrf.Tango.DevFailed;
//import fr.esrf.TangoApi.DeviceAttribute;
//import fr.esrf.TangoApi.DeviceProxy;

/**
 * A class for creating attributes activity screen.
 */
public class AttributesActivity extends CertificateExceptionActivity {
	/**
	 * The intent that activity was called with.
	 */
	Intent intent;
	/**
	 * Name of device, which attributes should be listed.
	 */
	String deviceName;
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
		setContentView(R.layout.activity_attributes);
		intent = getIntent();
		deviceName = intent.getStringExtra("deviceName");
		RESTfulHost = intent.getStringExtra("restHost");
		tangoHost = intent.getStringExtra("tangoHost");
		tangoPort = intent.getStringExtra("tangoPort");
		context = this;
		TextView deviceAttTextView = (TextView) findViewById(R.id.attributesActivityTextView1);
		deviceAttTextView.setText("Device " + deviceName + " attributes");
		setTitle("REST host: " + RESTfulHost + ", TANGO_HOST: " + tangoHost + ":" + tangoPort);

		restartQueue();
		refreshAttributesList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_attributes, menu);
		return true;
	}

	/**
	 * Refresh already shown list of attributes.
	 */
	private void refreshAttributesList() {

		Log.d("refreshAttributeList()", "Device name: " + deviceName);
		Log.d("refreshAttributeList()", "REST host: " + RESTfulHost);
		Log.d("refreshAttributeList()", "Tango host: " + tangoHost + ":" + tangoPort);

		String url = RESTfulHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
				"/get_attribute_list";
		HeaderJsonObjectRequest jsObjRequest =
				new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								Log.d("refreshAttributeList()", "Device connection OK / method GET / get_sttribute_list");
								LinearLayout layout = (LinearLayout) findViewById(R.id.attributesActivityLinearLayout);
								layout.removeAllViews();
								final LayoutInflater inflater = LayoutInflater.from(context);
								int attributeCount = response.getInt("attCount");
								Log.d("refreshAttributeList()", "Received " + attributeCount + " attributes");
								String[] attr_list = new String[attributeCount];
								String[] attr_values = new String[attributeCount];
								boolean[] writable = new boolean[attributeCount];
								boolean[] scalar = new boolean[attributeCount];
								for (int i = 0; i < attr_list.length; i++) {
									attr_list[i] = response.getString("attribute" + i);
									attr_values[i] = response.getString("attValue" + i);
									writable[i] = response.getBoolean("attWritable" + i);
									scalar[i] = response.getBoolean("attScalar" + i);
								}
								for (int i = 0; i < attr_list.length; i++) {
									//System.out.println("Processing attribute no. " + i);
									//System.out.println("Name: " + attr_list[i] + " Value: " + attr_list[i]);
									View view = inflater.inflate(R.layout.editable_list_element, layout, false);
									EditText et = (EditText) view.findViewById(R.id.editableListEditText);
									TextView tv = (TextView) view.findViewById(R.id.editableListTextView);
									tv.setText(attr_list[i]);
									et.setText(attr_values[i]);
									et.setTag(attr_list[i]);
									et.setEnabled(writable[i] && scalar[i]);
									et.setFocusable(writable[i] && scalar[i]);
									layout.addView(view);
								}
							} else {
								Log.d("refreshAttributeList()", "Tango database API returned message from query " +
										"get_attribute_list:");
								Log.d("refreshAttributeList()", response.getString("connectionStatus"));
							}
						} catch (JSONException e) {
							Log.d("refreshAttributeList()", "Problem with JSON object while getting attribute list");
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.d("refreshAttributeList()", "Connection error!");
						error.printStackTrace();
					}
				});
		jsObjRequest.setShouldCache(false);
		queue.add(jsObjRequest);

	}

	/**
	 * Listener for the button click, refresh list of activities.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void attributesActivityListRefreshButton(View view) {
		refreshAttributesList();
	}

	/**
	 * Listener for the button click, close the activity.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void attributesActivityCancelButton(View view) {
		finish();
	}

	/**
	 * Listener for the button click, update attributes.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void attributesActivityUpdateButton(View view) {
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.attributesActivityLinearLayout);
		int childCount = linearLayout.getChildCount();
		for (int i = 0; i < childCount; i++) {
			// for (int i = 0; i < 2; i++) {
			View linearLayoutView = linearLayout.getChildAt(i);
			EditText et = (EditText) linearLayoutView.findViewById(R.id.editableListEditText);
			if (et.isFocusable()) {
				String value = et.getText().toString();
				String tag = (String) et.getTag();
				String url = RESTfulHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + deviceName +
						"/write_attribute/" + tag + "/" +
						value;
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										Log.d("updateButton.onClick()", "Device connection OK / method PUT / write_attribute");
									} else {
										Toast.makeText(getApplicationContext(), response.getString("connectionStatus"),
												Toast.LENGTH_LONG).show();
										Log.d("updateButton.onClick()", "Tango database API returned message from query " +
												"write_attribute:");
										Log.d("updateButton.onClick()", response.getString("connectionStatus"));
									}
								} catch (JSONException e) {
									Log.d("updateButton.onClick()", "Problem with JSON object while writing attribute");
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
}

