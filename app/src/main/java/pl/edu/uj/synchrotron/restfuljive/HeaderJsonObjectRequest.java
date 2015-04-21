package pl.edu.uj.synchrotron.restfuljive;

import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lukasz on 17.03.15.
 */
public class HeaderJsonObjectRequest extends JsonObjectRequest {
	private String userName;
	private String userPassword;

	public HeaderJsonObjectRequest(int method, String url, JSONObject jsonRequest,
	                               Response.Listener<JSONObject> listener, Response.ErrorListener errorListener,
	                               String user, String password) {
		super(method, url, jsonRequest, listener, errorListener);
		userName = user;
		userPassword = password;
	}

	public HeaderJsonObjectRequest(int method, String url, JSONObject jsonRequest,
	                               Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
		super(method, url, jsonRequest, listener, errorListener);
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
