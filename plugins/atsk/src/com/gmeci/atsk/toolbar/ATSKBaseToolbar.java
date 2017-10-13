
package com.gmeci.atsk.toolbar;

import com.atakmap.android.tools.ActionBarView;

/**
 * Base toolbar
 */
public interface ATSKBaseToolbar {

    // Initialize view
    void setupView();

    // Return view
    ActionBarView getView();

    // Return view bounds
    int[] getBounds();

    // Toolbar visibility changed
    void onVisible(boolean visible);

    boolean onBackButtonPressed();

    // Called when ATSK closes
    void dispose();
}
