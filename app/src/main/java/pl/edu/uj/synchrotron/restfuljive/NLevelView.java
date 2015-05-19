package pl.edu.uj.synchrotron.restfuljive;

import android.view.View;

/**
 * Class for creating view of multi-level expandable list view.
 */
public interface NLevelView {

	View getView(NLevelItem item);
}