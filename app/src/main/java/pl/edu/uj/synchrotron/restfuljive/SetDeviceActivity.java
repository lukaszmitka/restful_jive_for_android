package pl.edu.uj.synchrotron.restfuljive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;


public class SetDeviceActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set_device);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_set_device, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Listener for the button click, get data from user and send them to parent activity.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void buttonClickOk(View view) {
		Intent returnIntent = new Intent();
		EditText setDeviceEditText = (EditText) findViewById(R.id.setDevieEditText);
		String devicename = setDeviceEditText.getText().toString();
		returnIntent.putExtra("DEVICE_NAME", devicename);
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	/**
	 * Listener for the button click, close the activity.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void buttonClickCancel(View view) {
		Intent returnIntent = new Intent();
		setResult(RESULT_CANCELED, returnIntent);
		finish();
	}
}
