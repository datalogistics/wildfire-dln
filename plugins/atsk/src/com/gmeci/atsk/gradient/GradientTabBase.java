
package com.gmeci.atsk.gradient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;

import com.gmeci.atsk.ATSKFragmentManager;
import com.gmeci.atsk.ATSKMapComponent;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.hardwareinterfaces.HardwareConsumerInterface;

public abstract class GradientTabBase extends ListFragment {
    protected static final double SELECTED_SIZE_MULTIPLIER = 1.25f;
    protected static final int SELECTED_BG_COLOR = 0xff376d37;
    protected static final int NON_SELECTED_BG_COLOR = 0xff383838;
    private static final String TAG = "GradientTabBase";
    public HardwareConsumerInterface hardwareInterface;
    final Handler coHandler = new Handler();
    GradientProviderClient gpc;
    AZProviderClient azpc;
    boolean UpdadingGradient = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void SetCurrentGradientUID(String uid) {
        Intent CurrentGradientChangeIntent = new Intent();
        CurrentGradientChangeIntent
                .setAction(ATSKConstants.CURRENT_GRADIENT_UPDATE);
        CurrentGradientChangeIntent.putExtra(ATSKConstants.UID_EXTRA, uid);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                CurrentGradientChangeIntent);

    }

    @Override
    public void onPause() {
        super.onPause();
        gpc.Stop();
        azpc.Stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        gpc = new GradientProviderClient(getActivity());
        gpc.Start();
        azpc = new AZProviderClient(getActivity());
        azpc.Start();
    }

    protected boolean ObstructionBarVisible() {
        String CurrentState;
        CurrentState = ATSKApplication
                .getCollectionState();
        return ATSKIntentConstants.Visible(CurrentState);

    }

    protected boolean setOBState(String RequestedState) {
        ATSKApplication.setObstructionCollectionMethod(
                RequestedState, TAG, false);
        return true;
    }

    public void SetSurveyInterface() {

    }

    protected ATSKBaseFragment notifyTabHost() {
        // Find tab host
        ATSKFragmentManager fm = ATSKMapComponent.getATSKFM();
        if (fm != null) {
            ATSKBaseFragment parent = fm.getCurrentFragment();
            if (parent != null)
                parent.onTabCreated(this);
            return parent;
        }
        return null;
    }
}
