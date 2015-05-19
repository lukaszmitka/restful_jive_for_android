package pl.edu.uj.synchrotron.restfuljive;

import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for sending JSONObject requests with header containing user credentials.
 */
public class HeaderJsonObjectRequest extends JsonObjectRequest {
	private String userName;
	private String userPassword;

	/**
	 * This constructor instantiates JSONObject request with header containing user credentials.
	 *
	 * @param method
	 * 		HTTP method to be executed.
	 * @param url
	 * 		URL address to send request.
	 * @param jsonRequest
	 * 		JSONObject containing request data.
	 * @param listener
	 * 		Listener for response.
	 * @param errorListener
	 * 		Listener for error response.
	 * @param user
	 * 		User name.
	 * @param password
	 * 		User password.
	 */
	public HeaderJsonObjectRequest(int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener,
	                               Response.ErrorListener errorListener, String user, String password) {
		super(method, url, jsonRequest, listener, errorListener);
		userName = user;
		userPassword = password;
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		HashMap<String, String> params = new HashMap<String, String>();
		String creds = String.format("%s:%s", userName, userPassword);
		String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
		params.put("Authorization", auth);
		return params;
	}
}
