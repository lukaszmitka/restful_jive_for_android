/*
 * Created by lukasz on 02.04.15.
 */
package pl.edu.uj.synchrotron.restfuljive;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


/**
 * Class that enable to add security exception for self signed SSL certificate in application fragment.
 */
public abstract class CertificateExceptionActivity extends WifiMonitorActivity {
	public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
	private static final int REQUEST_CERT_PATH = 100;
	protected RequestQueue queue;
	private SSLContext sslContext = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		String lastCertPath = settings.getString("LastCertificatePath", "");
		if (!lastCertPath.equals("")) {
			setSSLCertificate(lastCertPath);
		} else {
			setSSLCertificate(R.raw.server);
		}
	}

	@Override
	protected void onDestroy() {
		if (queue != null) {
			queue.stop();
			queue.getCache().clear();
		}
		super.onDestroy();
	}

	protected void restartQueue() {
		if (queue != null) {
			queue.stop();
			queue.getCache().clear();
		}
		if (sslContext != null) {
			queue = Volley.newRequestQueue(getApplicationContext(), new HurlStack(null, sslContext.getSocketFactory()));
		} else {
			queue = Volley.newRequestQueue(getApplicationContext());
		}
		queue.start();
	}

	protected void promptForCertPath() {
		Intent intent = new Intent(getBaseContext(), FileDialog.class);
		intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath());

		//can user select directories or not
		intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
		Log.d("onActivityResult", "Prompting for certificate path");
		startActivityForResult(intent, REQUEST_CERT_PATH);
	}

	@Override
	protected synchronized void onActivityResult(final int requestCode,
	                                             int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d("onActivityResult", "Processing certificate path from activity");
		if (resultCode == Activity.RESULT_OK) {
			Log.d("onActivityResult", "Result code: OK");
			if (requestCode == REQUEST_CERT_PATH) {
				String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
				Log.d("onActivityResult", "Selected certificate: " + filePath);
				if (setSSLCertificate(filePath)) {
					SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("LastCertificatePath", filePath);
					editor.apply();
					Log.d("onActivityResult()", "Saved last cert path as: " + filePath);
				}
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {
			Log.d("onActivityResult", "Result code: NOTHING");
			if (requestCode == REQUEST_CERT_PATH) {
				Log.d("onActivityResult", "file not selected");
			}
		}
	}

	private boolean setSSLCertificate(String certPath) {
		Log.d("setSSLCertificate", "Received path: " + certPath);
		TrustManagerFactory tmf;
		CertificateFactory cf;
		File f = new File(certPath);
		Log.d("setSSLCertificate", "Created file");
		try {
			/*
			get certificate file from path specified by user
			 */
			FileInputStream certificateFileInput = new FileInputStream(f);
			cf = CertificateFactory.getInstance("X.509");
			Certificate ca;
			ca = cf.generateCertificate(certificateFileInput);

			// Create a KeyStore containing our trusted CAs
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", ca);
			String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			// Create a TrustManager that trusts the CAs in our KeyStore
			tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(keyStore);
			// Create an SSLContext that uses our TrustManager
			sslContext = null;
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, tmf.getTrustManagers(), null);
			certificateFileInput.close();
			restartQueue();
			return true;
		} catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException
				e) {
			e.printStackTrace();

		}
		return false;
	}

	private boolean setSSLCertificate(int resID) {

		TrustManagerFactory tmf;
		CertificateFactory cf;
		Log.d("setSSLCertificate", "Created file");
		try {
			/*
			get certificate file from path specified by user
			 */
			InputStream is = getResources().openRawResource(resID);

			//FileInputStream certificateFileInput =  new FileInputStream();
			cf = CertificateFactory.getInstance("X.509");
			Certificate ca;
			ca = cf.generateCertificate(is);

			// Create a KeyStore containing our trusted CAs
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", ca);
			String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			// Create a TrustManager that trusts the CAs in our KeyStore
			tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(keyStore);
			// Create an SSLContext that uses our TrustManager
			sslContext = null;
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, tmf.getTrustManagers(), null);
			//certificateFileInput.close();
			is.close();
			restartQueue();
			return true;
		} catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException
				e) {
			e.printStackTrace();

		}
		return false;
	}
}
