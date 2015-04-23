package pl.edu.uj.synchrotron.restfuljive;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.Spinner;

import java.lang.reflect.Method;

public class NoSelectionSpinner extends Spinner {

	private static Method s_pSelectionChangedMethod = null;

	static {
		try {
			Class noparams[] = {};
			Class targetClass = AdapterView.class;

			s_pSelectionChangedMethod = targetClass.getDeclaredMethod("selectionChanged", noparams);
			if (s_pSelectionChangedMethod != null) {
				s_pSelectionChangedMethod.setAccessible(true);
			}

		} catch (Exception e) {
			Log.e("Custom spinner, bug:", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private int lastSelected = 0;

	public NoSelectionSpinner(Context context) {
		super(context);
	}

	public NoSelectionSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NoSelectionSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	@Override
	public void
	setSelection(int position, boolean animate) {
		boolean sameSelected = position == getSelectedItemPosition();
		super.setSelection(position, animate);
		if (sameSelected) {
			// Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
			getOnItemSelectedListener().onItemSelected(this, getSelectedView(), position, getSelectedItemId());
		}
	}

	@Override
	public void
	setSelection(int position) {
		boolean sameSelected = position == getSelectedItemPosition();
		super.setSelection(position);
		if (sameSelected) {
			// Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
			getOnItemSelectedListener().onItemSelected(this, getSelectedView(), position, getSelectedItemId());
		}
	}
}
