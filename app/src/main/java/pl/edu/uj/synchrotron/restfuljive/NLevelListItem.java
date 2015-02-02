package pl.edu.uj.synchrotron.restfuljive;

import android.view.View;

/**
 * Class for creating list element of multi-level expandable list view.
 */
public interface NLevelListItem {

	public boolean isExpanded();

	public void toggle();

	public NLevelListItem getParent();

	public View getView();
}