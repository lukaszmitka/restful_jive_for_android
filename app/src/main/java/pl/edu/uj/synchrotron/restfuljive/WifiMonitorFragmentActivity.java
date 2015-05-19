package pl.edu.uj.synchrotron.restfuljive;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * This class observes device's wifi state and inform user about changes.
 */
public class WifiMonitorFragmentActivity extends FragmentActivity {
	protected Context context;
	protected boolean isConnected;
	private IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
	private BroadcastReceiver broadcast = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			checkInternetConnectionStatus();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		checkInternetConnectionStatus();
	}

	/**
	 * Check network state, and inform user if networkis present and what type of connection is used.
	 */
	private void checkInternetConnectionStatus() {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null) {
			Log.d("checkInternetConnection", "Found internet connection.");
			if (activeNetwork.isConnectedOrConnecting()) {
				Log.d("checkInternetConnection", "Connection is active.");
				isConnected = true;
				int networkType = activeNetwork.getType();
				Toast toast;
				switch (networkType) {
					case ConnectivityManager.TYPE_WIFI:
						WifiManager mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
						WifiInfo currentWifi = mainWifi.getConnectionInfo();
						toast = Toast.makeText(this, "Connecting using wifi: " + currentWifi.getSSID(), Toast.LENGTH_SHORT);
						break;
					case ConnectivityManager.TYPE_MOBILE:
						toast = Toast.makeText(this, "Connecting using mobile data.", Toast.LENGTH_SHORT);
						break;
					case ConnectivityManager.TYPE_ETHERNET:
						toast = Toast.makeText(this, "Connecting using ethernet.", Toast.LENGTH_SHORT);
						break;
					default:
						toast = Toast.makeText(this, "Connecting using other connection type.", Toast.LENGTH_SHORT);
						break;
				}
				toast.show();
			} else {
				noNetworkConnection();
			}
		} else {
			noNetworkConnection();
		}
	}

	/**
	 * Inform user that there is no internet connection.
	 */
	private void noNetworkConnection() {
		isConnected = false;
		Log.d("noNetworkConnection()", "No internet connection.");
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("You have no internet connection! Application will not work").setTitle("No connection!");
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(broadcast, filter);
	}

	@Override
	public void onPause() {
		unregisterReceiver(broadcast);
		super.onPause();  // Always call the superclass method first

	}
}
