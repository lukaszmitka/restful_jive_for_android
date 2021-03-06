package pl.edu.uj.synchrotron.restfuljive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

/**
 * Activity for getting REST host, database host and port from user.
 */
public class SetHostActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set_host);
		Intent i = getIntent();
		if (i.hasExtra("tangoHost")) {
			EditText tangoHost = (EditText) findViewById(R.id.editTextTangoHost);
			tangoHost.setText(i.getStringExtra("tangoHost"));
		}
		if (i.hasExtra("tangoPort")) {
			EditText tangoPort = (EditText) findViewById(R.id.editTextTangoPort);
			tangoPort.setText(i.getStringExtra("tangoPort"));
		}
		if (i.hasExtra("restHost")) {
			EditText RESTfulHost = (EditText) findViewById(R.id.textNewHost);
			RESTfulHost.setText(i.getStringExtra("restHost"));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_set_host, menu);
		return true;
	}

	/**
	 * Listener for the button click, get data from user and send them to parent activity.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void buttonClickOk(View view) {
		Intent returnIntent = new Intent();
		EditText RESTfulHost = (EditText) findViewById(R.id.textNewHost);
		String RESTfulHostString = RESTfulHost.getText().toString();
		EditText tangoHost = (EditText) findViewById(R.id.editTextTangoHost);
		String tangoHostString = tangoHost.getText().toString();
		EditText tangoPort = (EditText) findViewById(R.id.editTextTangoPort);
		String tangoPortString = tangoPort.getText().toString();
		returnIntent.putExtra("restHost", RESTfulHostString);
		returnIntent.putExtra("TangoHost", tangoHostString);
		returnIntent.putExtra("TangoPort", tangoPortString);
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
