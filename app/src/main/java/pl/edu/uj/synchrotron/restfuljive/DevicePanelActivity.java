package pl.edu.uj.synchrotron.restfuljive;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * A class for creating device panel activity screen.
 */
public class DevicePanelActivity extends WifiMonitorFragmentActivity implements ActionBar.TabListener {
	/**
	 * Name of preferences file to store application settings.
	 */
	public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
	/**
	 * Layout manager to flip left and right pages.
	 */
	private ViewPager viewPager;
	/**
	 * Action bar of the activity
	 */
	private ActionBar actionBar;
	/**
	 * Name of device, which attributes should be listed.
	 */
	private String deviceName;
	/**
	 * RESTful host address.
	 */
	private String restHost;
	/**
	 * Address of database to be used by REST service.
	 */
	private String tangoHost;
	/**
	 * Port of database to be used by REST service.
	 */
	private String tangoPort;
	private Context context;
	/**
	 * User name.
	 */
	private String userName;
	/**
	 * User password.
	 */
	private String userPassword;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		context = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_panel);
		// this allows to connect with server in main thread
		StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old).permitNetwork().build());

		Intent i = getIntent();
		if (i.hasExtra("devName")) {
			deviceName = i.getStringExtra("devName");
			restHost = i.getStringExtra("restHost");
			tangoHost = i.getStringExtra("tangoHost");
			tangoPort = i.getStringExtra("tangoPort");
			userName = i.getStringExtra("userName");
			userPassword = i.getStringExtra("userPass");
		}
		setTitle("REST host: " + restHost + ", TANGO_HOST: " + tangoHost + ":" + tangoPort);
		// Tab titles
		String[] tabs = {this.getString(R.string.device_panel_commands), this.getString(R.string.device_panel_attributes),
				this.getString(R.string.device_panel_admin)};

		// Initilization
		viewPager = (ViewPager) findViewById(R.id.device_panel_pager);
		actionBar = getActionBar();
		/*
	  Adapter for controlling tabs.
	 */
		DevicePanelTabsPagerAdapter mAdapter = new DevicePanelTabsPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(mAdapter);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Adding Tabs
		for (String tab_name : tabs) {
			actionBar.addTab(actionBar.newTab().setText(tab_name).setTabListener(this));
			/**
			 * on swiping the viewpager make respective tab selected
			 */
			viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

				@Override
				public void onPageSelected(int position) {
					// on changing the page
					// make respected tab selected
					actionBar.setSelectedNavigationItem(position);
				}

				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
				}

				@Override
				public void onPageScrollStateChanged(int arg0) {
				}
			});
		}
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		restHost = settings.getString("RESTfulTangoHost", "");
		if (restHost.equals("")) {
			System.out.println("Error: Host not specified!");
			Toast toast = Toast.makeText(context, "Error: Host not specified!", Toast.LENGTH_LONG);
			toast.show();
		}
	}

	/**
	 * Get name of already processed device.
	 *
	 * @return Name of the device.
	 */
	public String getDeviceName() {
		return deviceName;
	}

	/**
	 * Get context of an activity.
	 *
	 * @return Activity context.
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * Get address of already connected database.
	 *
	 * @return Database host.
	 */
	public String getRestHost() {
		return restHost;
	}

	/**
	 * Get name of currently selected tango host
	 *
	 * @return Tango host name.
	 */
	public String getTangoHost() {
		return tangoHost;
	}

	/**
	 * Get number of corrently selected tango port.
	 *
	 * @return tango port number.
	 */
	public String getTangoPort() {
		return tangoPort;
	}

	/**
	 * Get name of currently operating user.
	 *
	 * @return User name
	 */
	protected String getUserName() {return userName;}

	/**
	 * Get password of currently operating user.
	 *
	 * @return Password,
	 */
	protected String getUserPassword() {return userPassword;}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_device_panel, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		return id == R.id.action_settings || super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {

	}
}

