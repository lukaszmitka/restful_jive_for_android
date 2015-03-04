package pl.edu.uj.synchrotron.restfuljive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;


/**
 * Activity for listing devices in three level sorted list.
 */
public class SortedList extends Activity {

	public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
	// define types of sorting
	private static final int SORT_BY_DEVICE = 0;
	private static final int DEFAULT_SORTING_TYPE = SORT_BY_DEVICE;
	private int sortType = DEFAULT_SORTING_TYPE;
	private static final int SORT_BY_CLASS = 1;
	private static final int SORT_BY_SERVER = 2;
	final Context context = this;
	List<NLevelItem> list;
	ListView listView;
	private String RESTfulTangoHost;
	private String tangoHost;
	private String tangoPort;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sorted_list);
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
		getMenuInflater().inflate(R.menu.menu_sorted_list, menu);

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
				showFullList();
				return true;
			case R.id.action_sort_by_classes:
				sortType = SORT_BY_CLASS;
				refreshDeviceList(RESTfulTangoHost);
				return true;
			case R.id.action_sort_by_devices:
				sortType = SORT_BY_DEVICE;
				refreshDeviceList(RESTfulTangoHost);
				return true;
			case R.id.action_sort_by_servers:
				sortType = SORT_BY_SERVER;
				refreshDeviceList(RESTfulTangoHost);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Start new activity for getting from user database host address and port.
	 */
	private void setHost() {
		Intent i = new Intent(this, SetHostActivity.class);
		startActivityForResult(i, 1);
	}

	/**
	 * Start new activity with full, unsorted list of devices.
	 */
	private void showFullList() {
		Intent i = new Intent(this, FullListActivity.class);
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
	 * Get list of devices from databse, and sort them accordingly to their domain and class.
	 *
	 * @param RESTHost Database host address.
	 * @return TreeMap with sorted list.
	 */
	private TreeMap<String, DevClassList> getSortedList(String RESTHost) {
		RequestQueue queue = Volley.newRequestQueue(this);

		switch (sortType) {
			case SORT_BY_CLASS:
				queue.start();
				String urlSortCase1 = RESTHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/SortedDeviceList.json/1";
				JsonObjectRequest jsObjRequestCase1 =
						new JsonObjectRequest(Request.Method.GET, urlSortCase1, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
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
								System.out.println("Connection error!");
								error.printStackTrace();
							}
						});
				queue.add(jsObjRequestCase1);
				TextView hostTextView = (TextView) findViewById(R.id.sortedList_hostTextView);
				hostTextView.setText(R.string.title_sort_by_classes);
				break;
			case SORT_BY_SERVER:
				queue.start();
				String urlSortCase2 = RESTHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/SortedDeviceList.json/2";
				JsonObjectRequest jsObjRequestCase2 =
						new JsonObjectRequest(Request.Method.GET, urlSortCase2, null, new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
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
								System.out.println("Connection error!");
								error.printStackTrace();
							}
						});
				queue.add(jsObjRequestCase2);
				TextView hostTextView2 = (TextView) findViewById(R.id.sortedList_hostTextView);
				hostTextView2.setText(R.string.title_sort_by_servers);
				break;
			default: //sort by devices
				queue.start();
				String url = RESTHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device.json";
				JsonObjectRequest jsObjRequest =
						new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

							@Override
							public void onResponse(JSONObject response) {
								try {
									if (response.getString("connectionStatus").equals("OK")) {
										System.out.println("Device connection OK");
										int deviceCount = response.getInt("numberOfDevices");
										System.out.println("Number of devices: " + deviceCount);
										String list[] = new String[deviceCount];
										TreeMap<String, DevClassList> domains =
												new TreeMap<String, DevClassList>(new AlphabeticComparator());
										for (int i = 0; i < deviceCount; i++) {
											// System.out.println("Device " + i + ": " + response.getString("device" + i));
											list[i] = response.getString("device" + i);
										}
										int i = deviceCount - 1;
										String devDomain = new String("");
										String devClass = new String("");
										int j = 0;
										String[] splitted = new String[3];
										while (j < i) {
											splitted = list[j].split("/");
											devDomain = splitted[0];
											DevClassList dcl = new DevClassList(devDomain);
											// System.out.println("Petla 1 :" + devDomain + "  " + splitted[0]);
											while (devDomain.equals(splitted[0]) && (j < i)) {
												splitted = list[j].split("/");
												devClass = splitted[1];
												// System.out.println("    Petla 2 :" + devClass + "  " + splitted[1]);
												ArrayList<String> members = new ArrayList<String>();
												while (devClass.equals(splitted[1]) && (j < i) && devDomain.equals(splitted[0])) {
													// System.out.println("      Petla 3 :" + splitted[2]);
													members.add(splitted[2]);
													// System.out.println("Processing device: " + devDomain + "/" + devClass + "/" +
													// splitted[2]);
													j++;
													if (j < i) {
														splitted = list[j].split("/");
													} else {
														break;
													}
												}
												Collections.sort(members, new AlphabeticComparator());
												dcl.addToMap(devClass, members);
											}
											domains.put(devDomain, dcl);
										}
										// System.out.println("Zakonczono listowanie urzadzen");
										// }
										updateDeviceList(domains);
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
				TextView hostTextView3 = (TextView) findViewById(R.id.sortedList_hostTextView);
				hostTextView3.setText(R.string.title_sort_by_devices);
		}

		return null;
	}

	/**
	 * Listener for button, show device info.
	 *
	 * @param v Reference to the widget that was clicked.
	 */
	public void buttonClick(View v) {
		String devName = (String) v.getTag();
		System.out.println("Clicked object: " + devName);
		RequestQueue queue = Volley.newRequestQueue(this);
		queue.start();
		String url =
				RESTfulTangoHost + "/RESTfulTangoApi/" + tangoHost + ":" + tangoPort + "/Device/" + devName + "/get_info.json";
		JsonObjectRequest jsObjRequest =
				new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
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
				});
		queue.add(jsObjRequest);
	}

	/**
	 * Listener for button, start new activity which show properties.
	 *
	 * @param v Reference to the widget that was clicked.
	 */
	public void buttonProperties(View v) {
		String devName = (String) v.getTag();
		System.out.println("Clicked object: " + devName);
		Intent intent = new Intent(this, PropertiesActivity.class);
		intent.putExtra("deviceName", devName);
		intent.putExtra("restHost", RESTfulTangoHost);
		intent.putExtra("tangoHost", tangoHost);
		intent.putExtra("tangoPort", tangoPort);
		startActivity(intent);
	}

	/*
	 * private void showDeviceInfo(String deviceInfo) { } */

	/**
	 * Listener for button, start new activity which show attributes.
	 *
	 * @param v Reference to the widget that was clicked.
	 */
	public void buttonAttributes(View v) {
		String devName = (String) v.getTag();
		System.out.println("Clicked object: " + devName);
		Intent intent = new Intent(this, AttributesActivity.class);
		intent.putExtra("deviceName", devName);
		intent.putExtra("restHost", RESTfulTangoHost);
		intent.putExtra("tangoHost", tangoHost);
		intent.putExtra("tangoPort", tangoPort);
		//intent.putExtra("dbPort", dbPort);
		startActivity(intent);
	}

	/**
	 * Listener for button, refresh list of devices.
	 *
	 * @param v Reference to the widget that was clicked.
	 */
	public void sortedListRefresh(View v) {
		refreshDeviceList(RESTfulTangoHost);
	}

	/**
	 * Refresh currently shown list of devices.
	 *
	 * @param RESTfulHost Database host address.
	 */
	private void refreshDeviceList(String RESTfulHost) {

		listView = (ListView) findViewById(R.id.listView2);
		list = new ArrayList<NLevelItem>();
		System.out.println("Connecting to: " + RESTfulHost);
		getSortedList(RESTfulHost);
	}


	private void updateDeviceList(JSONObject response, int sortCase) {
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

						final NLevelItem grandParent = new NLevelItem(new SomeObject(className, ""), null, new NLevelView() {
							public View getView(NLevelItem item) {
								View view = inflater.inflate(R.layout.n_level_list_item_lev_1, null);
								TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
								String name = (String) ((SomeObject) item.getWrappedObject()).getName();
								tv.setText(name);
								return view;
							}
						});
						list.add(grandParent);
						devicesCount = response.getInt(className + "DevCount");
						for (int j = 0; j < devicesCount; j++) {
							deviceName = response.getString(className + "Device" + j);
							NLevelItem child =
									new NLevelItem(new SomeObject(deviceName, deviceName), grandParent,
											new NLevelView() {
												public View getView(NLevelItem item) {
													View view = inflater.inflate(R.layout.n_level_list_member_item, null);
													Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
													b.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
													TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
													tv.setClickable(true);
													String name = (String) ((SomeObject) item.getWrappedObject()).getName();
													tv.setText(name);
													tv.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
													tv.setOnLongClickListener(new OnLongClickListener() {
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
																		Toast toast = Toast.makeText(context, "This should run ATKPanel",
																				Toast.LENGTH_LONG);
																		toast.show();
																	}
																	if (choice == 1) {
																		Intent i = new Intent(context, DevicePanelActivity.class);
																		i.putExtra("devName", name);
																		i.putExtra("restHost", RESTfulTangoHost);
																		i.putExtra("tangoHost", tangoHost);
																		i.putExtra("tangoPort", tangoPort);
																		startActivity(i);
																	}
																}
															});
															AlertDialog dialog = builder.create();
															dialog.show();
															return true;
														}
													});
													Button properties = (Button) view.findViewById(R.id.nLevelList_member_properties);
													properties.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
													Button attributes = (Button) view.findViewById(R.id.nLevelList_member_attributes);
													attributes.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
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
						final NLevelItem serverLevel = new NLevelItem(new SomeObject(serverName, ""), null, new NLevelView() {
							public View getView(NLevelItem item) {
								View view = serverSortingInflater.inflate(R.layout.n_level_list_item_lev_1, null);
								TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
								String name = (String) ((SomeObject) item.getWrappedObject()).getName();
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
							final NLevelItem instanceLevel = new NLevelItem(new SomeObject(instanceName, ""), serverLevel,
									new NLevelView() {
										public View getView(NLevelItem item) {
											View view = serverSortingInflater.inflate(R.layout.n_level_list_item_lev_2, null);
											TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L2_textView);
											String name = (String) ((SomeObject) item.getWrappedObject()).getName();
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
										new NLevelItem(new SomeObject(className, ""), instanceLevel, new NLevelView() {
											public View getView(NLevelItem item) {
												View view = serverSortingInflater.inflate(R.layout.n_level_list_item_lev_3, null);
												TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L3_textView);
												String name = (String) ((SomeObject) item.getWrappedObject()).getName();
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
										NLevelItem deviceLevel =
												new NLevelItem(new SomeObject(deviceName, deviceName), classLevel,
														new NLevelView() {
															public View getView(NLevelItem item) {
																View view =
																		serverSortingInflater.inflate(R.layout.n_level_list_member_item,
																				null);
																Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
																b.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
																TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
																tv.setClickable(true);
																String name = (String) ((SomeObject) item.getWrappedObject()).getName();
																tv.setText(name);
																tv.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
																tv.setOnLongClickListener(new OnLongClickListener() {
																	@Override
																	public boolean onLongClick(View v) {
																		TextView tv = (TextView) v;
																		AlertDialog.Builder builder = new AlertDialog.Builder(tv.getContext
																				());
																		builder.setTitle("Choose action");
																		builder.setNegativeButton("Cancel",
																				new DialogInterface.OnClickListener() {
																					public void onClick(DialogInterface dialog, int id) {
																					}
																				});
																		String[] s = {"Monitor", "Test"};
																		final String name = tv.getTag().toString();
																		builder.setItems(s, new DialogInterface.OnClickListener() {
																			public void onClick(DialogInterface dialog, int choice) {
																				if (choice == 0) {
																					Toast toast =
																							Toast.makeText(context, "This should run ATKPanel",
																									Toast.LENGTH_LONG);
																					toast.show();
																				}
																				if (choice == 1) {
																					Intent i = new Intent(context, DevicePanelActivity.class);
																					i.putExtra("devName", name);
																					i.putExtra("restHost", RESTfulTangoHost);
																					i.putExtra("tangoHost", tangoHost);
																					i.putExtra("tangoPort", tangoPort);
																					startActivity(i);
																				}
																			}
																		});
																		AlertDialog dialog = builder.create();
																		dialog.show();
																		return true;
																	}
																});
																Button properties =
																		(Button) view.findViewById(R.id.nLevelList_member_properties);
																properties.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
																Button attributes =
																		(Button) view.findViewById(R.id.nLevelList_member_attributes);
																attributes.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
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
			default:
				break;
		}

	}


	private void updateDeviceList(TreeMap<String, DevClassList> devCL) {
		if (devCL != null) {
			final LayoutInflater inflater = LayoutInflater.from(this);
			String[] s = new String[1];
			String[] domainList = devCL.keySet().toArray(s);

			for (String searchDomain : domainList) {
				// devDomain = searchDomain;
				if (devCL.containsKey(searchDomain)) {
					DevClassList dclRet = devCL.get(searchDomain);
					String[] classList = dclRet.getClassSet();

					final NLevelItem grandParent = new NLevelItem(new SomeObject(searchDomain, ""), null, new NLevelView() {
						public View getView(NLevelItem item) {
							View view = inflater.inflate(R.layout.n_level_list_item_lev_1, null);
							TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L1_textView);
							String name = (String) ((SomeObject) item.getWrappedObject()).getName();
							tv.setText(name);
							return view;
						}
					});

					list.add(grandParent);

					for (String sClass : classList) {
						// devClass = sClass;
						ArrayList<String> classMembers = dclRet.getClass(sClass);

						NLevelItem parent = new NLevelItem(new SomeObject(sClass, ""), grandParent, new NLevelView() {
							public View getView(NLevelItem item) {
								View view = inflater.inflate(R.layout.n_level_list_item_lev_2, null);
								TextView tv = (TextView) view.findViewById(R.id.nLevelList_item_L2_textView);
								String name = (String) ((SomeObject) item.getWrappedObject()).getName();
								tv.setText(name);
								return view;
							}
						});
						list.add(parent);
						for (String cMember : classMembers) {
							// devMember = cMember;
							NLevelItem child =
									new NLevelItem(new SomeObject(cMember, searchDomain + "/" + sClass + "/" + cMember), parent,
											new NLevelView() {
												public View getView(NLevelItem item) {
													View view = inflater.inflate(R.layout.n_level_list_member_item, null);
													Button b = (Button) view.findViewById(R.id.nLevelList_member_button);
													b.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
													TextView tv = (TextView) view.findViewById(R.id.nLevelList_member_textView);
													tv.setClickable(true);
													String name = (String) ((SomeObject) item.getWrappedObject()).getName();
													tv.setText(name);
													tv.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
													tv.setOnLongClickListener(new OnLongClickListener() {
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
																		Toast toast = Toast.makeText(context, "This should run ATKPanel",
																				Toast.LENGTH_LONG);
																		toast.show();
																	}
																	if (choice == 1) {
																		Intent i = new Intent(context, DevicePanelActivity.class);
																		i.putExtra("devName", name);
																		i.putExtra("restHost", RESTfulTangoHost);
																		i.putExtra("tangoHost", tangoHost);
																		i.putExtra("tangoPort", tangoPort);
																		startActivity(i);
																	}
																}
															});
															AlertDialog dialog = builder.create();
															dialog.show();
															return true;
														}
													});
													Button properties = (Button) view.findViewById(R.id.nLevelList_member_properties);
													properties.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
													Button attributes = (Button) view.findViewById(R.id.nLevelList_member_attributes);
													attributes.setTag((String) ((SomeObject) item.getWrappedObject()).getTag());
													return view;
												}
											});
							list.add(child);
						}
					}
				}
			}
			NLevelAdapter adapter = new NLevelAdapter(list);
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					((NLevelAdapter) listView.getAdapter()).toggle(arg2);
					((NLevelAdapter) listView.getAdapter()).getFilter().filter();
				}
			});
		}
	}

	/**
	 * Class for storing name and tag of list element.
	 */
	class SomeObject {
		public String name;
		public String tag;

		/**
		 * @param name Name of the object.
		 * @param tag  Tag of the object.
		 */
		public SomeObject(String name, String tag) {
			this.name = name;
			this.tag = tag;
		}

		/**
		 * Get name of the object.
		 *
		 * @return Name of the object.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Get tag of the object.
		 *
		 * @return Tag of the object.
		 */
		public String getTag() {
			return tag;
		}
	}
}

/**
 * Class for storing class list.
 */
class DevClassList {
	/**
	 * In this TreeMap are stored device classes with devices of their type. Class names are stores as key, and devices
	 * are stored as value.
	 */
	private TreeMap<String, ArrayList<String>> deviceClassList;

	public DevClassList(String domainName) {
		deviceClassList = new TreeMap<String, ArrayList<String>>(new AlphabeticComparator());
	}

	/**
	 * Add class with its devices to map.
	 *
	 * @param key Name of the class.
	 * @param dc  Device list.
	 */
	public void addToMap(String key, ArrayList<String> dc) {
		deviceClassList.put(key, dc);
	}

	/**
	 * Get list of stored classes.
	 *
	 * @return Array of strings containing names of classes.
	 */
	public String[] getClassSet() {
		String[] s = new String[1];
		return deviceClassList.keySet().toArray(s);
	}

	/**
	 * Get list of devices of selected class.
	 *
	 * @param key Name of selected class.
	 * @return List of devices.
	 */
	public ArrayList<String> getClass(String key) {
		if (deviceClassList.containsKey(key)) {
			return deviceClassList.get(key);
		}
		return null;
	}
}

/**
 * Class comparing elements, used to sort alphapetically.
 */
class AlphabeticComparator implements Comparator<String> {
	@Override
	public int compare(String e1, String e2) {
		return e1.toLowerCase().compareTo(e2.toLowerCase());
	}
}