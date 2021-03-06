package pl.edu.uj.synchrotron.restfuljive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activity for listing devices in multi level sorted list.
 */
public class SortedList extends CertificateExceptionActivity {

	// define types of sorting
	private static final int SORT_BY_DEVICE = 0;
	private static final int SORT_BY_CLASS = 1;
	private static final int SORT_BY_SERVER = 2;
	private static final int SORT_FULL_LIST = 3;
	private static final int DEFAULT_SORTING_TYPE = SORT_FULL_LIST;
	private static final int REQUEST_LONG_TIMEOUT = 60000; // in miliseconds
	private static final int ACTIVITY_REQUEST_HOST = 1;
	private static final int ACTIVITY_REQUEST_CREDENTIALS = 2;
	private final Context context = this;
	private int sortType = DEFAULT_SORTING_TYPE;
	private List<NLevelItem> list;
	private ListView listView;
	private boolean trackDeviceStatus = false;
	private String RESTfulTangoHost;
	private String tangoHost;
	private String tangoPort;
	private JSONObject lastResponse;
	private int lastSortType;

	private RequestQueue.RequestFilter reqFilter = new RequestQueue.RequestFilter() {
		@Override
		public boolean apply(Request<?> request) {
			return true;
		}
	};
	private String userName;
	private String userPassword;
	private OnLongClickListener onLongClickListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sorted_list);

		onLongClickListener = new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				TextView tv = (TextView) v;
				AlertDialog.Builder builder = new AlertDialog.Builder(tv.getContext());
				builder.setTitle("Choose action");
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
				String[] s = {"Monitor", "Test"};
				final String name = tv.getTag().toString();
				builder.setItems(s, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int choice) {
						if (choice == 0) {
							Intent i = new Intent(context, ATKPanelActivity.class);
							i.putExtra("DEVICE_NAME", name);
							i.putExtra("restHost", RESTfulTangoHost);
							i.putExtra("tangoHost", tangoHost);
							i.putExtra("tangoPort", tangoPort);
							i.putExtra("userName", userName);
							i.putExtra("userPass", userPassword);
							startActivity(i);
						/*Toast toast = Toast.makeText(context, "This should run ATKPanel",
							Toast.LENGTH_LONG);
						toast.show();*/
						}
						if (choice == 1) {
							Intent i = new Intent(context, DevicePanelActivity.class);
							i.putExtra("devName", name);
							i.putExtra("restHost", RESTfulTangoHost);
							i.putExtra("tangoHost", tangoHost);
							i.putExtra("tangoPort", tangoPort);
							i.putExtra("userName", userName);
							i.putExtra("userPass", userPassword);
							startActivity(i);
						}
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
				return true;
			}
		};

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		if (savedInstanceState != null) {
			userName = savedInstanceState.getString("userName");
			userPassword = savedInstanceState.getString("userPassword");
			Log.d("onCreate()", "User name: " + userName);
			Log.d("onCreate()", "Password: " + userPassword);
			if (userName.equals("") && userPassword.equals("")) {
				readCredentialsFromStorage();
				Log.d("onCreate()", "User name: " + userName);
				Log.d("onCreate()", "Password: " + userPassword);
			}
		} else {
			if (((userName == null) && userPassword == null)) {
				readCredentialsFromStorage();
				Log.d("onCreate()", "User name: " + userName);
				Log.d("onCreate()", "Password: " + userPassword);
			}
		}

		String settingsRestHost = settings.getString("RESTfulTangoHost", "");
		String settingsTangoHost = settings.getString("TangoHost", "");
		String settingsTangoPort = settings.getString("TangoPort", "");
		Log.d("onCreate()", "Found RESTful host: " + settingsRestHost);
		Log.d("onCreate()", "Found Tango host: " + settingsTangoHost);
		Log.d("onCreate()", "Found Tango port: " + settingsTangoPort);
		if (settingsRestHost.equals("") || settingsTangoHost.equals("") || settingsTangoPort.equals("")) {
			System.out.println("Requesting new tango host,port and RESTful host");
			setHost();
		} else {
			RESTfulTangoHost = settingsRestHost;
			tangoHost = settingsTangoHost;
			tangoPort = settingsTangoPort;
			restartQueue();
			Log.d("onCreate()",
					"Getting device list from server:  " + RESTfulTangoHost + "at Tango Host: " + tangoHost + ":" + tangoPort);
			try {
				if (isConnected) {
					if (lastResponse == null) {
						getSortedList(RESTfulTangoHost);
					} else {
						updateDeviceList(lastResponse, sortType);
					}
				}
			} catch (Exception e) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
				builder.setMessage("Problem with connecting to REST server, check if internet connection is available and " +
						"server address is set properly").setTitle("Error");
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		}
		setTitle("REST host: " + RESTfulTangoHost + ", TANGO_HOST: " + tangoHost + ":" + tangoPort);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("userName", userName);
		outState.putString("userPassword", userPassword);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.activity_sorted_list);
		try {
			if (lastResponse == null) {
				getSortedList(RESTfulTangoHost);
			} else {
				updateDeviceList(lastResponse, sortType);
			}
		} catch (Exception e) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage("Problem with connecting to REST server, check if internet connection is available and " +
					"server address is set properly").setTitle("Error");
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_sorted_list, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem menuItemTrackStatus = menu.findItem(R.id.action_track_status);
		MenuItem menuItemUntrackStatus = menu.findItem(R.id.action_untrack_status);
		menuItemTrackStatus.setVisible(!trackDeviceStatus);
		menuItemUntrackStatus.setVisible(trackDeviceStatus);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.action_set_host:
				setHost();
				return true;
			case R.id.action_full_list:
				sortType = SORT_FULL_LIST;
				getSortedList(RESTfulTangoHost);
				return true;
			case R.id.action_sort_by_classes:
				sortType = SORT_BY_CLASS;
				getSortedList(RESTfulTangoHost);
				return true;
			case R.id.action_sort_by_devices:
				sortType = SORT_BY_DEVICE;
				getSortedList(RESTfulTangoHost);
				return true;
			case R.id.action_sort_by_servers:
				sortType = SORT_BY_SERVER;
				getSortedList(RESTfulTangoHost);
				return true;
			case R.id.action_about_filter:
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(R.string.sorted_list_filter_description).setTitle(R.string.menu_item_about_filter);
				AlertDialog dialog = builder.create();
				dialog.show();
				((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
				return true;
			case R.id.action_track_status:
				AlertDialog.Builder trackStatusBuilder = new AlertDialog.Builder(context);
				trackStatusBuilder.setMessage(R.string.track_status_warning).setTitle(R.string.warning);
				trackStatusBuilder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						trackDeviceStatus = false;
					}
				});
				trackStatusBuilder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						trackDeviceStatus = true;
						getSortedList(RESTfulTangoHost);

					}
				});
				AlertDialog trackStatusDialog = trackStatusBuilder.create();
				trackStatusDialog.show();
				return true;
			case R.id.action_untrack_status:
				trackDeviceStatus = false;
				updateDeviceList(lastResponse, sortType);
				return true;
			case R.id.action_set_certificate_file:
				promptForCertPath();
				return true;
			case R.id.action_enter_login_pass:
				requestCredentials();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * This method check if user credential were stored in file and if yes, loads them to memory.
	 */
	private void readCredentialsFromStorage() {
		Log.d("readCredentials()", "Getting credentials from file");
		FileInputStream fileInputStream;
		try {
			fileInputStream = openFileInput("RESTfulJiveCredentials");
			Log.d("readCredentials()", "Opened file: " + fileInputStream);
			byte[] buffer = new byte[100];
			try {
				fileInputStream.read(buffer);
				Log.d("readCredentials()", "Read buffer");
				String credentials = new String(buffer);
				Log.d("readCredentials()", "Credentials: " + credentials);
				String[] nameAndPassword = credentials.split(":");
				userName = nameAndPassword[0];
				Log.d("readCredentials()", "User name: " + userName);
				userPassword = nameAndPassword[1];
				Log.d("readCredentials()", "Password: " + userPassword);
			} catch (IOException e) {
				Log.d("readCredentials()", "Problem with writing to file");
				e.printStackTrace();
			}
			try {
				fileInputStream.close();
			} catch (IOException e) {
				Log.d("readCredentials()", "Problem with closing file: " + e.getMessage());
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			Log.d("readCredentials()", "Can not open file, reason:" + e.getMessage());
			e.printStackTrace();
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
		if (RESTfulTangoHost != null) {
			if (!RESTfulTangoHost.equals("")) {
				i.putExtra("restHost", RESTfulTangoHost);
			}
		}
		startActivityForResult(i, 1);
	}

	/**
	 * Start new activity for getting user name and password.
	 */
	private void requestCredentials() {
		Intent i = new Intent(this, GetCredentialsActivity.class);
		startActivityForResult(i, ACTIVITY_REQUEST_CREDENTIALS);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d("onActivityresult()", "Processing activity result");
		if (requestCode == 1) {
			Log.d("onActivityresult()", "requestCode = 1, setting new host");
			if (resultCode == RESULT_OK) {
				RESTfulTangoHost = data.getStringExtra("restHost");
				tangoHost = data.getStringExtra("TangoHost");
				tangoPort = data.getStringExtra("TangoPort");
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("RESTfulTangoHost", RESTfulTangoHost);
				editor.putString("TangoHost", tangoHost);
				editor.putString("TangoPort", tangoPort);
				editor.apply();
				System.out.println("Result: " + RESTfulTangoHost);
				getSortedList(RESTfulTangoHost);
				setTitle("REST host: " + RESTfulTangoHost + ", TANGO_HOST: " + tangoHost + ":" + tangoPort);
			}
			if (resultCode == RESULT_CANCELED) {
				System.out.println("Host not changed");
			}
		} else if (requestCode == ACTIVITY_REQUEST_CREDENTIALS) {
			Log.d("onActivityresult()", "requestCode = ACTIVITY_REQUEST_CREDENTIALS, new user name and password");
			if (resultCode == RESULT_OK) {
				userName = data.getStringExtra("userName");
				userPassword = data.getStringExtra("userPass");
				boolean rememberCredentials = data.getBooleanExtra("rememberCredentials", false);
				if (rememberCredentials) {
					FileOutputStream fileOutputStream;
					try {
						fileOutputStream = openFileOutput("RESTfulJiveCredentials", MODE_PRIVATE);
						String credentials = userName + ":" + userPassword + ":";
						Log.d("onActivityResult()", "Saving credentials: " + credentials);
						try {
							fileOutputStream.write(credentials.getBytes());
						} catch (IOException e) {
							Log.d("onActivityResult()", "Problem with writing to file");
							e.printStackTrace();
						}
						try {
							fileOutputStream.close();
						} catch (IOException e) {
							Log.d("onActivityResult()", "Problem with closing file: " + e.getMessage());
							e.printStackTrace();
						}
					} catch (FileNotFoundException e) {
						Log.d("onActivityResult()", "Can not open file, reason:" + e.getMessage());
						e.printStackTrace();
					}
				}
				getSortedList(RESTfulTangoHost);
			}
			if (resultCode == RESULT_CANCELED) {
				Log.d("onActivityResult()", "No new credentials");
			}
		}
	}

	/**
	 * Generate request for RESTful host and set listener for incoming reply.
	 *
	 * @param RESTHost
	 * 		RESTful host address.
	 */
	private void getSortedList(String RESTHost) {

		enableUserInterface(false);
		TextView hostTextView = (TextView) findViewById(R.id.sortedList_hostTextView);
		switch (sortType) {
			case SORT_BY_CLASS:
				String urlSortCase1 = RESTHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/SortedDeviceList" +
						"/1/" + trackDeviceStatus;
				HeaderJsonObjectRequest jsObjRequestCase1 =
						new HeaderJsonObjectRequest(Request.Method.GET, urlSortCase1, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										lastSortType = SORT_BY_CLASS;
										lastResponse = response;
										updateDeviceList(response, SORT_BY_CLASS);
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
						}, userName, userPassword);
				jsObjRequestCase1
						.setRetryPolicy(new DefaultRetryPolicy(REQUEST_LONG_TIMEOUT, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
								DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
				jsObjRequestCase1.setShouldCache(false);
				queue.cancelAll(reqFilter);
				queue.add(jsObjRequestCase1);

				hostTextView.setText(R.string.title_sort_by_classes);
				break;
			case SORT_BY_SERVER:
				String urlSortCase2 = RESTHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/SortedDeviceList" +
						"/2/" + trackDeviceStatus;
				HeaderJsonObjectRequest jsObjRequestCase2 =
						new HeaderJsonObjectRequest(Request.Method.GET, urlSortCase2, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										lastSortType = SORT_BY_SERVER;
										lastResponse = response;
										updateDeviceList(response, SORT_BY_SERVER);
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
						}, userName, userPassword);
				jsObjRequestCase2
						.setRetryPolicy(new DefaultRetryPolicy(REQUEST_LONG_TIMEOUT, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
								DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
				jsObjRequestCase2.setShouldCache(false);
				queue.cancelAll(reqFilter);
				queue.add(jsObjRequestCase2);
				//TextView hostTextView2 = (TextView) findViewById(R.id.sortedList_hostTextView);
				hostTextView.setText(R.string.title_sort_by_servers);
				break;
			case SORT_FULL_LIST:
				String urlSortCase3 = RESTHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + trackDeviceStatus;
				HeaderJsonObjectRequest jsObjRequestCase3 =
						new HeaderJsonObjectRequest(Request.Method.GET, urlSortCase3, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										lastSortType = SORT_FULL_LIST;
										lastResponse = response;
										updateDeviceList(response, SORT_FULL_LIST);
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
						}, userName, userPassword);
				jsObjRequestCase3
						.setRetryPolicy(new DefaultRetryPolicy(REQUEST_LONG_TIMEOUT, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
								DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
				jsObjRequestCase3.setShouldCache(false);
				queue.cancelAll(reqFilter);
				queue.add(jsObjRequestCase3);
				//TextView hostTextView3 = (TextView) findViewById(R.id.sortedList_hostTextView);
				hostTextView.setText(R.string.title_sort_full_list);
				break;
			default: //sort by devices
				String url = RESTHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/SortedDeviceList/3/" +
						trackDeviceStatus;
				HeaderJsonObjectRequest jsObjRequest =
						new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										lastSortType = SORT_BY_DEVICE;
										lastResponse = response;
										updateDeviceList(response, SORT_BY_DEVICE);
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
						}, userName, userPassword);
				jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(REQUEST_LONG_TIMEOUT, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
						DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
				jsObjRequest.setShouldCache(false);
				queue.cancelAll(reqFilter);
				queue.add(jsObjRequest);
				//TextView hostTextViewDef = (TextView) findViewById(R.id.sortedList_hostTextView);
				hostTextView.setText(R.string.title_sort_by_devices);
		}
	}

	/**
	 * Method displaying info about connection error
	 *
	 * @param error
	 * 		Error tah caused exception
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

		enableUserInterface(true);
	}

	/**
	 * Listener for button, show device info.
	 *
	 * @param v
	 * 		Reference to the widget that was clicked.
	 */
	public void buttonClick(View v) {
		String devName = (String) v.getTag();
		System.out.println("Clicked object: " + devName);
		String url = RESTfulTangoHost + "/Tango/rest/" + tangoHost + ":" + tangoPort + "/Device/" + devName + "/get_info";
		HeaderJsonObjectRequest jsObjRequest =
				new HeaderJsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("connectionStatus").equals("OK")) {
								System.out.println("Device connection OK");
								// showDeviceInfo(response.getString("deviceInfo"));

								AlertDialog.Builder builder = new AlertDialog.Builder(context);
								builder.setMessage(response.getString("deviceInfo")).setTitle("Device info");
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
				}, userName, userPassword);
		jsObjRequest.setShouldCache(false);
		queue.cancelAll(reqFilter);
		queue.add(jsObjRequest);
	}

	/**
	 * Listener for button, start new activity which show properties.
	 *
	 * @param v
	 * 		Reference to the widget that was clicked.
	 */
	public void buttonProperties(View v) {
		String devName = (String) v.getTag();
		System.out.println("Clicked object: " + devName);
		Intent intent = new Intent(this, PropertiesActivity.class);
		intent.putExtra("deviceName", devName);
		intent.putExtra("restHost", RESTfulTangoHost);
		intent.putExtra("tangoHost", tangoHost);
		intent.putExtra("tangoPort", tangoPort);
		intent.putExtra("userName", userName);
		intent.putExtra("userPass", userPassword);
		startActivity(intent);
	}

	/**
	 * Listener for button, start new activity which show attributes.
	 *
	 * @param v
	 * 		Reference to the widget that was clicked.
	 */
	public void buttonAttributes(View v) {
		String devName = (String) v.getTag();
		System.out.println("Clicked object: " + devName);
		Intent intent = new Intent(this, AttributesActivity.class);
		intent.putExtra("deviceName", devName);
		intent.putExtra("restHost", RESTfulTangoHost);
		intent.putExtra("tangoHost", tangoHost);
		intent.putExtra("tangoPort", tangoPort);
		intent.putExtra("userName", userName);
		intent.putExtra("userPass", userPassword);
		//intent.putExtra("dbPort", dbPort);
		startActivity(intent);
	}

	/**
	 * Listener for button, refresh list of devices.
	 *
	 * @param v
	 * 		Reference to the widget that was clicked.
	 */
	public void buttonSortedListRefresh(View v) {
		getSortedList(RESTfulTangoHost);
	}

	/**
	 * Button listener, read pattern from text view and filter device list with it.
	 *
	 * @param view
	 * 		interface element that called method
	 */
	public void buttonFilter(View view) {
		EditText filterPattern = (EditText) findViewById(R.id.sortedList_filterPattern);
		filterDeviceList(lastResponse, lastSortType, filterPattern.getText().toString());
	}

	/**
	 * Update currently shown list of devices with list received from server in JSON format.
	 *
	 * @param response
	 * 		JSON response from RESTful srever.
	 * @param sortCase
	 * 		Method of sorting devices.
	 */
	private void updateDeviceList(JSONObject response, int sortCase) {
		listView = (ListView) findViewById(R.id.sortedList_listView);
		list = new ArrayList<>();
		boolean isDeviceAlive = false;
		switch (sortCase) {
			case SORT_BY_CLASS:
				final LayoutInflater inflater = LayoutInflater.from(this);
				try {
					int classCount = response.getInt("numberOfClasses");
					String className;
					int devicesCount;
					String deviceName;
					for (int i = 0; i < classCount; i++) {
						className = response.getString("className" + i);

						final NLevelItem grandParent =
								new NLevelItem(new SomeObject(className, "", false), null, new NLevelView() {
									public View getView(NLevelItem item) {
										View view = inflater.inflate(R.layout.n_level_list_item_lev_1, listView, false);
										TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
										String name = ((SomeObject) item.getWrappedObject()).getName();
										tv.setText(name);
										return view;
									}
								});
						list.add(grandParent);
						devicesCount = response.getInt(className + "DevCount");
						for (int j = 0; j < devicesCount; j++) {
							deviceName = response.getString(className + "Device" + j);
							if (trackDeviceStatus) {
								isDeviceAlive = response.getBoolean(className + "isDeviceAlive" + j);
							}
							NLevelItem child = new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), grandParent,
									new NLevelView() {
										public View getView(NLevelItem item) {
											View view = inflater.inflate(R.layout.n_level_list_member_item, listView, false);
											ImageView imageView = (ImageView) view.findViewById(R.id.nLevelListMemberDiode);
											if (trackDeviceStatus) {
												if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
													imageView.setImageResource(R.drawable.dioda_zielona);
												} else {
													imageView.setImageResource(R.drawable.dioda_czerwona);
												}
											} else {
												imageView.setVisibility(View.INVISIBLE);
											}
											Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
											b.setTag(((SomeObject) item.getWrappedObject()).getTag());
											TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
											tv.setClickable(true);
											String name = ((SomeObject) item.getWrappedObject()).getName();
											tv.setText(name);
											tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
											tv.setOnLongClickListener(onLongClickListener);
											Button properties = (Button) view.findViewById(R.id.nLevelList_member_properties);
											properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
											Button attributes = (Button) view.findViewById(R.id.nLevelList_member_attributes);
											attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
											return view;
										}
									});
							list.add(child);
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

				NLevelAdapter adapter = new NLevelAdapter(list);
				listView.setAdapter(adapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						((NLevelAdapter) listView.getAdapter()).toggle(arg2);
						((NLevelAdapter) listView.getAdapter()).getFilter().filter();
					}
				});

				break;
			case SORT_BY_SERVER:
				final LayoutInflater serverSortingInflater = LayoutInflater.from(this);
				try {
					// get number of servers
					int serverCount = response.getInt("ServerCount");
					String serverName;
					int instancesCount;
					String instanceName;
					int classCount;
					String className;
					int devicesCount;
					String deviceName;
					for (int i = 0; i < serverCount; i++) {
						serverName = response.getString("Server" + i);

						// prepare first level list item
						final NLevelItem serverLevel =
								new NLevelItem(new SomeObject(serverName, "", false), null, new NLevelView() {
									public View getView(NLevelItem item) {
										View view = serverSortingInflater.inflate(R.layout.n_level_list_item_lev_1, listView, false);
										TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
										String name = ((SomeObject) item.getWrappedObject()).getName();
										tv.setText(name);
										return view;
									}
								});
						// add frist level list item
						list.add(serverLevel);

						// get number of instances for the server
						instancesCount = response.getInt(serverName + "InstCnt");
						for (int j = 0; j < instancesCount; j++) {
							instanceName = response.getString(serverName + "Instance" + j);
							// prepare second level list item
							final NLevelItem instanceLevel =
									new NLevelItem(new SomeObject(instanceName, "", false), serverLevel, new NLevelView() {
										public View getView(NLevelItem item) {
											View view =
													serverSortingInflater.inflate(R.layout.n_level_list_item_lev_2, listView, false);
											TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L2_textView);
											String name = ((SomeObject) item.getWrappedObject()).getName();
											tv.setText(name);
											return view;
										}
									});
							// add second level list item
							list.add(instanceLevel);

							// get number of classes in the instance
							classCount = response.getInt("Se" + i + "In" + j + "ClassCnt");
							for (int k = 0; k < classCount; k++) {
								className = response.getString("Se" + i + "In" + j + "Cl" + k);
								// prepare third level list item
								final NLevelItem classLevel =
										new NLevelItem(new SomeObject(className, "", false), instanceLevel, new NLevelView() {
											public View getView(NLevelItem item) {
												View view =
														serverSortingInflater.inflate(R.layout.n_level_list_item_lev_3, listView, false);
												TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L3_textView);
												String name = ((SomeObject) item.getWrappedObject()).getName();
												tv.setText(name);
												return view;
											}
										});
								// add third level list item
								list.add(classLevel);

								// get count of devices for class
								devicesCount = response.getInt("Se" + i + "In" + j + "Cl" + k + "DCnt");
								if (devicesCount > 0) {
									for (int l = 0; l < devicesCount; l++) {
										deviceName = response.getString("Se" + i + "In" + j + "Cl" + k + "Dev" + l);
										System.out.println("Add device " + deviceName + " to list");
										// prepare fourth level list item
										if (trackDeviceStatus) {
											isDeviceAlive = response.getBoolean(deviceName + "isDeviceAlive" + l);
										}
										NLevelItem deviceLevel =
												new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), classLevel,
														new NLevelView() {
															public View getView(NLevelItem item) {
																View view = serverSortingInflater
																		.inflate(R.layout.n_level_list_member_item, listView, false);
																ImageView imageView =
																		(ImageView) view.findViewById(R.id.nLevelListMemberDiode);
																if (trackDeviceStatus) {
																	if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
																		imageView.setImageResource(R.drawable.dioda_zielona);
																	} else {
																		imageView.setImageResource(R.drawable.dioda_czerwona);
																	}
																} else {
																	imageView.setVisibility(View.INVISIBLE);
																}
																Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
																b.setTag(((SomeObject) item.getWrappedObject()).getTag());
																TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
																tv.setClickable(true);
																String name = ((SomeObject) item.getWrappedObject()).getName();
																tv.setText(name);
																tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
																tv.setOnLongClickListener(onLongClickListener);
																Button properties =
																		(Button) view.findViewById(R.id.nLevelList_member_properties);
																properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
																Button attributes =
																		(Button) view.findViewById(R.id.nLevelList_member_attributes);
																attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
																return view;
															}
														});
										// add fourth level list item
										list.add(deviceLevel);
									}
								}
							}
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

				NLevelAdapter servSortAdapter = new NLevelAdapter(list);
				listView.setAdapter(servSortAdapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						((NLevelAdapter) listView.getAdapter()).toggle(arg2);
						((NLevelAdapter) listView.getAdapter()).getFilter().filter();
					}
				});

				break;
			case SORT_BY_DEVICE:
				final LayoutInflater deviceSortingInflater = LayoutInflater.from(this);
				try {
					Log.d("updateDeviceList", "Received response: " + response.toString());
					// get number of servers
					int domainCount = response.getInt("domainCount");
					String domainName;
					int classCount;
					String className;
					int devicesCount;
					String deviceName;
					for (int i = 0; i < domainCount; i++) {
						domainName = response.getString("domain" + i);
						// prepare first level list item
						final NLevelItem serverLevel =
								new NLevelItem(new SomeObject(domainName, "", false), null, new NLevelView() {
									public View getView(NLevelItem item) {
										View view = deviceSortingInflater.inflate(R.layout.n_level_list_item_lev_1, listView, false);
										TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
										String name = ((SomeObject) item.getWrappedObject()).getName();
										tv.setText(name);
										return view;
									}
								});
						// add frist level list item
						list.add(serverLevel);

						// get number of classes in the instance
						classCount = response.getInt("domain" + i + "classCount");
						for (int k = 0; k < classCount; k++) {
							className = response.getString("domain" + i + "class" + k);
							// prepare third level list item
							final NLevelItem classLevel =
									new NLevelItem(new SomeObject(className, "", false), serverLevel, new NLevelView() {
										public View getView(NLevelItem item) {
											View view =
													deviceSortingInflater.inflate(R.layout.n_level_list_item_lev_2, listView, false);
											TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L2_textView);
											String name = ((SomeObject) item.getWrappedObject()).getName();
											tv.setText(name);
											return view;
										}
									});
							// add third level list item
							list.add(classLevel);

							// get count of devices for class
							devicesCount = response.getInt("domain" + i + "class" + k + "devCount");
							if (devicesCount > 0) {
								for (int l = 0; l < devicesCount; l++) {
									deviceName = response.getString("domain" + i + "class" + k + "device" + l);
									System.out.println("Add device " + deviceName + " to list");
									// prepare fourth level list item
									if (trackDeviceStatus) {
										isDeviceAlive = response.getBoolean(deviceName + "isDeviceAlive");
									}
									NLevelItem deviceLevel =
											new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), classLevel,
													new NLevelView() {
														public View getView(NLevelItem item) {
															View view = deviceSortingInflater
																	.inflate(R.layout.n_level_list_member_item, listView, false);
															ImageView imageView = (ImageView) view.findViewById(R.id
																	.nLevelListMemberDiode);
															if (trackDeviceStatus) {
																if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
																	imageView.setImageResource(R.drawable.dioda_zielona);
																} else {
																	imageView.setImageResource(R.drawable.dioda_czerwona);
																}
															} else {
																imageView.setVisibility(View.INVISIBLE);
															}
															Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
															b.setTag(((SomeObject) item.getWrappedObject()).getTag());
															TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
															tv.setClickable(true);
															String name = ((SomeObject) item.getWrappedObject()).getName();
															tv.setText(name);
															tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
															tv.setOnLongClickListener(onLongClickListener);
															Button properties =
																	(Button) view.findViewById(R.id.nLevelList_member_properties);
															properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
															Button attributes =
																	(Button) view.findViewById(R.id.nLevelList_member_attributes);
															attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
															return view;
														}
													});
									// add fourth level list item
									list.add(deviceLevel);
								}
							}
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

				NLevelAdapter devSortAdapter = new NLevelAdapter(list);
				listView.setAdapter(devSortAdapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						((NLevelAdapter) listView.getAdapter()).toggle(arg2);
						((NLevelAdapter) listView.getAdapter()).getFilter().filter();
					}
				});
				break;
			case SORT_FULL_LIST:
				final LayoutInflater fullListInflater = LayoutInflater.from(this);
				try {
					int deviceCount = response.getInt("numberOfDevices");
					String deviceName;
					for (int j = 0; j < deviceCount; j++) {
						deviceName = response.getString("device" + j);
						if (trackDeviceStatus) {
							isDeviceAlive = response.getBoolean(deviceName + "isDeviceAlive");
						}
						NLevelItem child =
								new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), null, new NLevelView() {
									public View getView(NLevelItem item) {
										View view = fullListInflater.inflate(R.layout.n_level_list_member_item, listView, false);
										ImageView imageView = (ImageView) view.findViewById(R.id.nLevelListMemberDiode);
										if (trackDeviceStatus) {
											if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
												imageView.setImageResource(R.drawable.dioda_zielona);
											} else {
												imageView.setImageResource(R.drawable.dioda_czerwona);
											}
										} else {
											imageView.setVisibility(View.INVISIBLE);
										}
										Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
										b.setTag(((SomeObject) item.getWrappedObject()).getTag());
										TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
										tv.setClickable(true);
										String name = ((SomeObject) item.getWrappedObject()).getName();
										tv.setText(name);
										tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
										tv.setOnLongClickListener(onLongClickListener);
										Button properties = (Button) view.findViewById(R.id.nLevelList_member_properties);
										properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
										Button attributes = (Button) view.findViewById(R.id.nLevelList_member_attributes);
										attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
										return view;
									}
								});
						list.add(child);

					}

				} catch (JSONException e) {
					e.printStackTrace();
				}

				NLevelAdapter fullListAdapter = new NLevelAdapter(list);
				listView.setAdapter(fullListAdapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						((NLevelAdapter) listView.getAdapter()).toggle(arg2);
						((NLevelAdapter) listView.getAdapter()).getFilter().filter();
					}
				});
				break;
			default:
				break;
		}
		enableUserInterface(true);
	}

	/**
	 * Filter currently shown list of devices with selected patern.
	 *
	 * @param response
	 * 		JSON response from RESTful srever.
	 * @param sortCase
	 * 		Method of sorting devices.
	 * @param pattern
	 * 		String containing regular expression filter.
	 */
	private void filterDeviceList(JSONObject response, int sortCase, String pattern) {
		Pattern p = Pattern.compile(".*" + pattern.toLowerCase() + ".*");
		Matcher m;
		listView = (ListView) findViewById(R.id.sortedList_listView);
		list = new ArrayList<>();
		boolean isDeviceAlive = false;
		boolean grandParentPresent;
		boolean secondLevelPresent;
		boolean thirdLevelPresent;
		switch (sortCase) {
			case SORT_BY_CLASS:
				final LayoutInflater inflater = LayoutInflater.from(this);
				try {
					int classCount = response.getInt("numberOfClasses");
					String className;
					int devicesCount;
					String deviceName;
					for (int i = 0; i < classCount; i++) {
						className = response.getString("className" + i);

						final NLevelItem grandParent =
								new NLevelItem(new SomeObject(className, "", false), null, new NLevelView() {
									public View getView(NLevelItem item) {
										View view = inflater.inflate(R.layout.n_level_list_item_lev_1, listView, false);
										TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
										String name = ((SomeObject) item.getWrappedObject()).getName();
										tv.setText(name);
										return view;
									}
								});
						grandParentPresent = false;
						devicesCount = response.getInt(className + "DevCount");
						for (int j = 0; j < devicesCount; j++) {
							deviceName = response.getString(className + "Device" + j);
							m = p.matcher(deviceName.toLowerCase());
							if (m.matches()) {
								if (!grandParentPresent) {
									list.add(grandParent);
									grandParentPresent = true;
								}
								if (trackDeviceStatus) {
									isDeviceAlive = response.getBoolean(className + "isDeviceAlive" + j);
								}
								NLevelItem child = new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive),
										grandParent,
										new NLevelView() {
											public View getView(NLevelItem item) {
												View view = inflater.inflate(R.layout.n_level_list_member_item, listView, false);
												ImageView imageView = (ImageView) view.findViewById(R.id.nLevelListMemberDiode);
												if (trackDeviceStatus) {
													if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
														imageView.setImageResource(R.drawable.dioda_zielona);
													} else {
														imageView.setImageResource(R.drawable.dioda_czerwona);
													}
												} else {
													imageView.setVisibility(View.INVISIBLE);
												}
												Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
												b.setTag(((SomeObject) item.getWrappedObject()).getTag());
												TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
												tv.setClickable(true);
												String name = ((SomeObject) item.getWrappedObject()).getName();
												tv.setText(name);
												tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
												tv.setOnLongClickListener(onLongClickListener);
												Button properties = (Button) view.findViewById(R.id.nLevelList_member_properties);
												properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
												Button attributes = (Button) view.findViewById(R.id.nLevelList_member_attributes);
												attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
												return view;
											}
										});
								list.add(child);
							}
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

				NLevelAdapter adapter = new NLevelAdapter(list);
				listView.setAdapter(adapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						((NLevelAdapter) listView.getAdapter()).toggle(arg2);
						((NLevelAdapter) listView.getAdapter()).getFilter().filter();
					}
				});

				break;
			case SORT_BY_SERVER:
				final LayoutInflater serverSortingInflater = LayoutInflater.from(this);
				try {
					// get number of servers
					int serverCount = response.getInt("ServerCount");
					String serverName;
					int instancesCount;
					String instanceName;
					int classCount;
					String className;
					int devicesCount;
					String deviceName;
					for (int i = 0; i < serverCount; i++) {
						serverName = response.getString("Server" + i);
						// prepare first level list item
						final NLevelItem serverLevel =
								new NLevelItem(new SomeObject(serverName, "", false), null, new NLevelView() {
									public View getView(NLevelItem item) {
										View view = serverSortingInflater.inflate(R.layout.n_level_list_item_lev_1, listView, false);
										TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
										String name = ((SomeObject) item.getWrappedObject()).getName();
										tv.setText(name);
										return view;
									}
								});
						grandParentPresent = false;

						// get number of instances for the server
						instancesCount = response.getInt(serverName + "InstCnt");
						for (int j = 0; j < instancesCount; j++) {
							instanceName = response.getString(serverName + "Instance" + j);
							// prepare second level list item
							final NLevelItem instanceLevel =
									new NLevelItem(new SomeObject(instanceName, "", false), serverLevel, new NLevelView() {
										public View getView(NLevelItem item) {
											View view =
													serverSortingInflater.inflate(R.layout.n_level_list_item_lev_2, listView, false);
											TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L2_textView);
											String name = ((SomeObject) item.getWrappedObject()).getName();
											tv.setText(name);
											return view;
										}
									});
							secondLevelPresent = false;

							// get number of classes in the instance
							classCount = response.getInt("Se" + i + "In" + j + "ClassCnt");
							for (int k = 0; k < classCount; k++) {
								className = response.getString("Se" + i + "In" + j + "Cl" + k);
								// prepare third level list item
								final NLevelItem classLevel =
										new NLevelItem(new SomeObject(className, "", false), instanceLevel, new NLevelView() {
											public View getView(NLevelItem item) {
												View view =
														serverSortingInflater.inflate(R.layout.n_level_list_item_lev_3, listView, false);
												TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L3_textView);
												String name = ((SomeObject) item.getWrappedObject()).getName();
												tv.setText(name);
												return view;
											}
										});
								thirdLevelPresent = false;

								// get count of devices for class
								devicesCount = response.getInt("Se" + i + "In" + j + "Cl" + k + "DCnt");
								if (devicesCount > 0) {
									for (int l = 0; l < devicesCount; l++) {
										deviceName = response.getString("Se" + i + "In" + j + "Cl" + k + "Dev" + l);
										m = p.matcher(deviceName.toLowerCase());
										if (m.matches()) {
											if (!grandParentPresent) {
												// add frist level list item
												list.add(serverLevel);
												grandParentPresent = true;
											}
											if (!secondLevelPresent) {
												// add second level list item
												list.add(instanceLevel);
												secondLevelPresent = true;
											}
											if (!thirdLevelPresent) {
												// add third level list item
												list.add(classLevel);
												thirdLevelPresent = true;
											}
											System.out.println("Add device " + deviceName + " to list");
											// prepare fourth level list item
											if (trackDeviceStatus) {
												isDeviceAlive = response.getBoolean(deviceName + "isDeviceAlive" + l);
											}
											NLevelItem deviceLevel =
													new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), classLevel,
															new NLevelView() {
																public View getView(NLevelItem item) {
																	View view = serverSortingInflater
																			.inflate(R.layout.n_level_list_member_item, listView, false);
																	ImageView imageView =
																			(ImageView) view.findViewById(R.id.nLevelListMemberDiode);
																	if (trackDeviceStatus) {
																		if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
																			imageView.setImageResource(R.drawable.dioda_zielona);
																		} else {
																			imageView.setImageResource(R.drawable.dioda_czerwona);
																		}
																	} else {
																		imageView.setVisibility(View.INVISIBLE);
																	}
																	Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
																	b.setTag(((SomeObject) item.getWrappedObject()).getTag());
																	TextView tv =
																			(TextView) view.findViewById(R.id.nLevelList_member_textView);
																	tv.setClickable(true);
																	String name = ((SomeObject) item.getWrappedObject()).getName();
																	tv.setText(name);
																	tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
																	tv.setOnLongClickListener(onLongClickListener);
																	Button properties =
																			(Button) view.findViewById(R.id.nLevelList_member_properties);
																	properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
																	Button attributes =
																			(Button) view.findViewById(R.id.nLevelList_member_attributes);
																	attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
																	return view;
																}
															});
											// add fourth level list item
											list.add(deviceLevel);
										}
									}
								}

							}
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

				NLevelAdapter servSortAdapter = new NLevelAdapter(list);
				listView.setAdapter(servSortAdapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						((NLevelAdapter) listView.getAdapter()).toggle(arg2);
						((NLevelAdapter) listView.getAdapter()).getFilter().filter();
					}
				});

				break;
			case SORT_BY_DEVICE:
				final LayoutInflater deviceSortingInflater = LayoutInflater.from(this);
				try {
					// get number of servers
					int domainCount = response.getInt("domainCount");
					String domainName;
					int classCount;
					String className;
					int devicesCount;
					String deviceName;
					for (int i = 0; i < domainCount; i++) {

						domainName = response.getString("domain" + i);
						// prepare first level list item
						final NLevelItem serverLevel =
								new NLevelItem(new SomeObject(domainName, "", false), null, new NLevelView() {
									public View getView(NLevelItem item) {
										View view = deviceSortingInflater.inflate(R.layout.n_level_list_item_lev_1, listView, false);
										TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
										String name = ((SomeObject) item.getWrappedObject()).getName();
										tv.setText(name);
										return view;
									}
								});
						grandParentPresent = false;
						// add frist level list item
						//list.add(serverLevel);

						// get number of classes in the instance
						classCount = response.getInt("domain" + i + "classCount");
						for (int k = 0; k < classCount; k++) {
							className = response.getString("domain" + i + "class" + k);
							// prepare third level list item
							final NLevelItem classLevel =
									new NLevelItem(new SomeObject(className, "", false), serverLevel, new NLevelView() {
										public View getView(NLevelItem item) {
											View view =
													deviceSortingInflater.inflate(R.layout.n_level_list_item_lev_2, listView, false);
											TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L2_textView);
											String name = ((SomeObject) item.getWrappedObject()).getName();
											tv.setText(name);
											return view;
										}
									});
							secondLevelPresent = false;
							// add second level list item
							//list.add(classLevel);

							// get count of devices for class
							devicesCount = response.getInt("domain" + i + "class" + k + "devCount");
							if (devicesCount > 0) {
								for (int l = 0; l < devicesCount; l++) {
									deviceName = response.getString("domain" + i + "class" + k + "device" + l);
									m = p.matcher(deviceName.toLowerCase());
									if (m.matches()) {
										if (!grandParentPresent) {
											// add first level list item
											list.add(serverLevel);
											grandParentPresent = true;
										}
										if (!secondLevelPresent) {
											// add second level list item
											list.add(classLevel);
											secondLevelPresent = true;
										}
										System.out.println("Add device " + deviceName + " to list");
										// prepare third level list item
										if (trackDeviceStatus) {
											isDeviceAlive = response.getBoolean(deviceName + "isDeviceAlive");
										}
										NLevelItem deviceLevel =
												new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), classLevel,
														new NLevelView() {
															public View getView(NLevelItem item) {
																View view = deviceSortingInflater
																		.inflate(R.layout.n_level_list_member_item, listView, false);
																ImageView imageView =
																		(ImageView) view.findViewById(R.id.nLevelListMemberDiode);
																if (trackDeviceStatus) {
																	if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
																		imageView.setImageResource(R.drawable.dioda_zielona);
																	} else {
																		imageView.setImageResource(R.drawable.dioda_czerwona);
																	}
																} else {
																	imageView.setVisibility(View.INVISIBLE);
																}
																Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
																b.setTag(((SomeObject) item.getWrappedObject()).getTag());
																TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
																tv.setClickable(true);
																String name = ((SomeObject) item.getWrappedObject()).getName();
																tv.setText(name);
																tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
																tv.setOnLongClickListener(onLongClickListener);
																Button properties =
																		(Button) view.findViewById(R.id.nLevelList_member_properties);
																properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
																Button attributes =
																		(Button) view.findViewById(R.id.nLevelList_member_attributes);
																attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
																return view;
															}
														});
										// add third level list item
										list.add(deviceLevel);
									}
								}
							}

						}

					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

				NLevelAdapter devSortAdapter = new NLevelAdapter(list);
				listView.setAdapter(devSortAdapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						((NLevelAdapter) listView.getAdapter()).toggle(arg2);
						((NLevelAdapter) listView.getAdapter()).getFilter().filter();
					}
				});
				break;
			case SORT_FULL_LIST:
				final LayoutInflater fullListInflater = LayoutInflater.from(this);
				try {
					int deviceCount = response.getInt("numberOfDevices");
					String deviceName;
					for (int j = 0; j < deviceCount; j++) {
						deviceName = response.getString("device" + j);
						m = p.matcher(deviceName.toLowerCase());
						if (m.matches()) {
							System.out.println("Matched device: " + deviceName);
							if (trackDeviceStatus) {
								isDeviceAlive = response.getBoolean(deviceName + "isDeviceAlive");
							}
							NLevelItem child =
									new NLevelItem(new SomeObject(deviceName, deviceName, isDeviceAlive), null, new NLevelView() {
										public View getView(NLevelItem item) {
											View view = fullListInflater.inflate(R.layout.n_level_list_member_item, listView, false);
											ImageView imageView = (ImageView) view.findViewById(R.id.nLevelListMemberDiode);
											if (trackDeviceStatus) {
												if (((SomeObject) item.getWrappedObject()).getIsAlive()) {
													imageView.setImageResource(R.drawable.dioda_zielona);
												} else {
													imageView.setImageResource(R.drawable.dioda_czerwona);
												}
											} else {
												imageView.setVisibility(View.INVISIBLE);
											}
											Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
											b.setTag(((SomeObject) item.getWrappedObject()).getTag());
											TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
											tv.setClickable(true);
											String name = ((SomeObject) item.getWrappedObject()).getName();
											tv.setText(name);
											tv.setTag(((SomeObject) item.getWrappedObject()).getTag());
											tv.setOnLongClickListener(onLongClickListener);
											Button properties = (Button) view.findViewById(R.id.nLevelList_member_properties);
											properties.setTag(((SomeObject) item.getWrappedObject()).getTag());
											Button attributes = (Button) view.findViewById(R.id.nLevelList_member_attributes);
											attributes.setTag(((SomeObject) item.getWrappedObject()).getTag());
											return view;
										}
									});
							list.add(child);
						}
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}

				NLevelAdapter fullListAdapter = new NLevelAdapter(list);
				listView.setAdapter(fullListAdapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						((NLevelAdapter) listView.getAdapter()).toggle(arg2);
						((NLevelAdapter) listView.getAdapter()).getFilter().filter();
					}
				});
				break;
			default:
				break;
		}
	}

	/**
	 * Enable or disable list view, filter and refresh button from user actions. Useful while loading device list from
	 * server.
	 *
	 * @param enabled
	 * 		If true, user can interact with interface, if false, elements are loced.
	 */
	private void enableUserInterface(boolean enabled) {
		if (enabled) {
			ProgressBar progressBar = (ProgressBar) findViewById(R.id.sortedList_progressBar);
			progressBar.setVisibility(View.INVISIBLE);
			ListView listView = (ListView) findViewById(R.id.sortedList_listView);
			listView.setFocusable(true);
			listView.setEnabled(true);
			Button refreshButton = (Button) findViewById(R.id.sortedList_refreshButton);
			refreshButton.setFocusable(true);
			refreshButton.setEnabled(true);
			Button filterButton = (Button) findViewById(R.id.sortedList_filterButton);
			filterButton.setEnabled(true);
			filterButton.setFocusable(true);
			EditText filterTextView = (EditText) findViewById(R.id.sortedList_filterPattern);
			filterTextView.setEnabled(true);
			filterTextView.setFocusable(true);
			filterTextView.setFocusableInTouchMode(true);

		} else {
			ProgressBar progressBar = (ProgressBar) findViewById(R.id.sortedList_progressBar);
			progressBar.setVisibility(View.VISIBLE);
			ListView listView = (ListView) findViewById(R.id.sortedList_listView);
			listView.setFocusable(false);
			listView.setEnabled(false);
			Button refreshButton = (Button) findViewById(R.id.sortedList_refreshButton);
			refreshButton.setFocusable(false);
			refreshButton.setEnabled(false);
			Button filterButton = (Button) findViewById(R.id.sortedList_filterButton);
			filterButton.setEnabled(false);
			filterButton.setFocusable(false);
			EditText filterTextView = (EditText) findViewById(R.id.sortedList_filterPattern);
			filterTextView.setEnabled(false);
			filterTextView.setFocusable(false);
			filterTextView.setFocusableInTouchMode(false);
		}
	}

}