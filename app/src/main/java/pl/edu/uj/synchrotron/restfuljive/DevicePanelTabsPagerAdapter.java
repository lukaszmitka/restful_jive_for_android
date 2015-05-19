package pl.edu.uj.synchrotron.restfuljive;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Class for handling device panel tabs.
 */
public class DevicePanelTabsPagerAdapter extends FragmentPagerAdapter {

	public DevicePanelTabsPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int index) {

		switch (index) {
			case 0:
				// Commands fragment activity
				return new DevicePanelCommandsFragment();
			case 1:
				// Attributes fragment activity
				return new DevicePanelAttributesFragment();
			case 2:
				// Admin fragment activity
				return new DevicePanelAdminFragment();
		}

		return null;
	}

	@Override
	public int getCount() {
		// get item count - equal to number of tabs
		return 3;
	}

}