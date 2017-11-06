
package com.gmeci.atsk.az.lz;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost.OnTabChangeListener;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.atsk.az.AZTabHost;

public class LZCriteriaTabHost extends AZTabHost implements OnTabChangeListener {

    private static final String LZ_SELECTED_TAB = "Selected LZ Tab";
    private static final String MEASUREMENT_TAB = "measurement";
    private static final String APPROACH_TAB = "approach";
    private static final String TAXIWAYS = "taxiways";

    public LZCriteriaTabHost() {
        SELECTED_TAB = LZ_SELECTED_TAB;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context plugin = ATSKApplication.getInstance()
                .getPluginContext();
        _root = LayoutInflater.from(plugin).inflate(
                R.layout.lz_crit_frag_tab_host, container,
                false);

        mTabHost = (FragmentTabHost) _root.findViewById(android.R.id.tabhost);
        mTabHost.setup(plugin, getChildFragmentManager(),
                R.id.realtabcontent);

        Resources res = plugin.getResources();

        // Measurements
        mTabHost.addTab(mTabHost.newTabSpec(MEASUREMENT_TAB).setIndicator(
                "", res.getDrawable(R.drawable.meas_tapemeasure)),
                LZMeasurementFragment.class, null);

        // Threshold
        mTabHost.addTab(mTabHost.newTabSpec(APPROACH_TAB).setIndicator(
                "", res.getDrawable(R.drawable.approach)),
                LZThresholdFragment.class, null);

        // Taxiway and apron placement
        mTabHost.addTab(mTabHost.newTabSpec(TAXIWAYS).setIndicator(
                "", res.getDrawable(R.drawable.apron_line)),
                LZTaxiwayFragment.class, null);

        for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {
            mTabHost.getTabWidget().getChildAt(i).setPadding(0, 0, 0, 0);
        }
        return _root;
    }

    @Override
    public void onResume() {
        super.onResume();
        azpc.putSetting(ATSKConstants.CURRENT_SCREEN,
                ATSKConstants.CURRENT_SCREEN_AZ, TAG);
    }
}
