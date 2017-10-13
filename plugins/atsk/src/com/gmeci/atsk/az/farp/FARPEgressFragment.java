
package com.gmeci.atsk.az.farp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.core.SurveyPoint;

public class FARPEgressFragment extends FARPTabBase {

    private static final String TAG = "FARPRefuellerFragment";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        azpc = new AZProviderClient(getActivity());
        azpc.Start();
    }

    @Override
    public void onDestroy() {
        azpc.Stop();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        return LayoutInflater.from(pluginContext).inflate(
                R.layout.gradient_collect, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupFARPRefuellerButton(view);
    }

    private void setupFARPRefuellerButton(View view) {
        //if the colelction is still running from a previous start - we should display it
    }

    private String getTypeString(boolean ExpectingTransverseGradient) {

        String TypeString = ATSKConstants.GRADIENT_TYPE_LONGITUDINAL;
        if (ExpectingTransverseGradient)
            TypeString = ATSKConstants.GRADIENT_TYPE_TRANSVERSE;

        return TypeString;
    }

    @Override
    protected void UpdateScreen() {

    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {
    }

    @Override
    protected void stopCollection() {
    }

}
