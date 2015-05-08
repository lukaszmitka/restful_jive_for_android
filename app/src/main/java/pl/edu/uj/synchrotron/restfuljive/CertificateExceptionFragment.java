/*
 * Created by lukasz on 04.04.15.
 */

package pl.edu.uj.synchrotron.restfuljive;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

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
public class CertificateExceptionFragment extends Fragment {
	public static final String PREFS_NAME = "SolarisDeviceListPrefsFile";
	//private static final int REQUEST_CERT_PATH = 100;
	protected RequestQueue queue;
	private SSLContext sslContext = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, FragmentActivity.MODE_PRIVATE);
		String lastCertPath = settings.getString("LastCertificatePath", "");
		if (!lastCertPath.equals("")) {
			Log.d("onCreateView()", "Using certificate path: " + lastCertPath);
			setSSLCertificate(lastCertPath);
		} else {
			Log.d("onCreateView()", "No path for certificate");

			setSSLCertificate(R.raw.server);

		}
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (queue != null) {
			queue.stop();
			queue.getCache().clear();
		}
	}

	protected void restartQueue() {
		if (queue != null) {
			queue.stop();
			queue.getCache().clear();
		}
		if (sslContext != null) {
			queue = Volley.newRequestQueue(getActivity().getApplicationContext(),
					new HurlStack(null, sslContext.getSocketFactory()));
		} else {
			queue = Volley.newRequestQueue(getActivity().getApplicationContext());
		}
		queue.start();
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
