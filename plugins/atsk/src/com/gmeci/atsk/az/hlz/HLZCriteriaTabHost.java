
package com.gmeci.atsk.az.hlz;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.atsk.az.AZTabHost;

public class HLZCriteriaTabHost extends AZTabHost {

    private static final String MEASUREMENT_TAB = "measurement";
    private static final String QUADRANTS_TAB = "capabilities";
    private static final String MAX_ON_GROUND_TAB = "impact";
    private static final String HLZ_SELECTED_TAB = "Selected HLZ Tab";

    public HLZCriteriaTabHost() {
        SELECTED_TAB = HLZ_SELECTED_TAB;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.az_crit_frag_tab_host, container,
                false);

        mTabHost = (FragmentTabHost) _root.findViewById(android.R.id.tabhost);
        mTabHost.setup(pluginContext, getChildFragmentManager(),
                R.id.realtabcontent);

        mTabHost.addTab(
                mTabHost.newTabSpec(MEASUREMENT_TAB).setIndicator(
                        "",
                        pluginContext.getResources().getDrawable(
                                R.drawable.meas_tapemeasure)),
                HLZMeasurementFragment.class, null);
        mTabHost.addTab(
                mTabHost.newTabSpec(QUADRANTS_TAB).setIndicator(
                        "",
                        pluginContext.getResources().getDrawable(
                                R.drawable.quadrant_icon)),
                HLZQuadrantObstructionsFragment.class, null);
        mTabHost.addTab(
                mTabHost.newTabSpec(MAX_ON_GROUND_TAB).setIndicator(
                        "",
                        pluginContext.getResources().getDrawable(
                                R.drawable.thumbtack)),
                HLZMaxOnGroundFragment.class, null);

        for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++)
            mTabHost.getTabWidget().getChildAt(i).setPadding(0, 0, 0, 0);

        return _root;
    }

    @Override
    public void onResume() {
        super.onResume();
        azpc.putSetting(ATSKConstants.CURRENT_SCREEN,
                ATSKConstants.CURRENT_SCREEN_AZ, TAG);
    }

}
