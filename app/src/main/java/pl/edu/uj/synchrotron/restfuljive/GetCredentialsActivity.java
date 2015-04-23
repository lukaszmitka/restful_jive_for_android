package pl.edu.uj.synchrotron.restfuljive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;


public class GetCredentialsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_get_credentials);
	}

	/**
	 * Listener for button, confirm input.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void getCredentials_okButton(View view) {
		EditText userNameEditText = (EditText) findViewById(R.id.getCredentials_userName_editText);
		EditText userPasswordEditText = (EditText) findViewById(R.id.getCredentials_userPass_editText);
		CheckBox rememberCredentialsCheckBox = (CheckBox) findViewById(R.id.getCredentials_storeData_checkBox);

		String userName = userNameEditText.getText().toString();
		String userPass = userPasswordEditText.getText().toString();
		boolean rememberCredentials = rememberCredentialsCheckBox.isChecked();
		Intent returnIntent = new Intent();
		returnIntent.putExtra("userName", userName);
		returnIntent.putExtra("userPass", userPass);
		returnIntent.putExtra("rememberCredentials", rememberCredentials);
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	/**
	 * Listener for button, cancel input.
	 *
	 * @param view Reference to the widget that was clicked.
	 */
	public void getCredentials_cancelButton(View view) {
		Intent returnIntent = new Intent();
		setResult(RESULT_CANCELED, returnIntent);
		finish();
	}
}
