package pl.edu.uj.synchrotron.restfuljive;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Activity for creating plot from two dimensional array.
 */
public class ImagePlotActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_plot);
		Intent i = getIntent();
		Bitmap bm = i.getParcelableExtra("imageData");
		ImageView iv = (ImageView) findViewById(R.id.imagePlot_imageView);
		iv.setImageBitmap(bm);

		TextView maxValue = (TextView) findViewById(R.id.imagePlot_maxValue);
		TextView minValue = (TextView) findViewById(R.id.imagePlot_minValue);

		maxValue.setText(i.getStringExtra("maxValue"));
		minValue.setText(i.getStringExtra("minValue"));
		ImageView scale = (ImageView) findViewById(R.id.imagePlot_scale);
		float[] hsv = {0, 1, 1};
		Bitmap b = Bitmap.createBitmap(20, 330, Bitmap.Config.RGB_565);
		int step = 16777216 / 330; // 16777216 = 2^24
		int color = 0;
		for (int j = 0; j < 330; j++) {
			hsv[0] = j;
			color = Color.HSVToColor(hsv);
			for (int k = 0; k < 20; k++) {
				b.setPixel(k, j, color);
			}
		}
		scale.setImageBitmap(b);
		scale.setScaleType(ImageView.ScaleType.FIT_XY);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_image_plot, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
