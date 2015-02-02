package pl.edu.uj.synchrotron.restfuljive;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.Database;

/**
 * Activity for listing device servers.
 */
public class ServerList extends Activity {
	public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
	private String servers[];
	// default host and port, used if user didn't specify other
	private String databaseHost = new String("192.168.100.120");
	private String databasePort = new String("10000");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server_list);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		String dbHost = settings.getString("dbHost", "");
		String dbPort = settings.getString("dbPort", "");
		if (dbHost.equals("")) {
			dbHost = databaseHost;
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("dbHost", databaseHost);
			editor.commit();
		}
		if (dbPort.equals("")) {
			dbPort = databasePort;
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("dbPort", databasePort);
			editor.commit();
		}
		refreshServerList(dbHost, dbPort);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_server_list, menu);
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
			case R.id.action_full_list:
				showFullList();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Listener for the button click, refresh list of device servers.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void buttonServerListRefresh(View view) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		String dbHost = settings.getString("dbHost", null);
		String dbPort = settings.getString("dbPort", null);
		refreshServerList(dbHost, dbPort);
	}

	/**
	 * Start new activity for getting from user database host address and port.
	 */
	private void setHost() {
		Intent i = new Intent(this, SetHostActivity.class);
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
				databaseHost = data.getStringExtra("host");
				databasePort = data.getStringExtra("port");
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("dbHost", databaseHost);
				editor.putString("dbPort", databasePort);
				editor.commit();
				System.out.println("Result: " + databaseHost + ":" + databasePort);
			}
			if (resultCode == RESULT_CANCELED) {
				System.out.println("Host not changed");
			}
		}
	}

	/**
	 * Refresh currently shown list of device servers.
	 *
	 * @param host Host address of database.
	 * @param port Database port.
	 */
	private void refreshServerList(String host, String port) {
		try {
			System.out.println("Lacze z baza danych");
			Database db = ApiUtil.get_db_obj(host, port);
			System.out.println("Pobieram dane z bazy");
			servers = db.get_server_list();
			ListView deviceList = (ListView) findViewById(R.id.servListListView1);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.firstLine, servers);
			deviceList.setAdapter(adapter);
		} catch (DevFailed e) {
			e.printStackTrace();
		}
	}
}
