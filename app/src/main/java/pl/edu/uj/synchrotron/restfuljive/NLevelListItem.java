package pl.edu.uj.synchrotron.restfuljive;

import android.view.View;

/**
 * Class for creating list element of multi-level expandable list view.
 */
public interface NLevelListItem {

	boolean isExpanded();

	void toggle();

	NLevelListItem getParent();

	View getView();
}