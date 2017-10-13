
package com.gmeci.atsk.az.farp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost.TabSpec;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.atsk.az.AZTabHost;

public class FARPTabHost extends AZTabHost {

    private static final String TANKER_TAB = "Tanker";
    private static final String REFUELEE_TAB = "Refuelee";
    private static final String EGRESS_TAB = "Egress";
    private static final String FARP_SELECTED_TAB = "Selected HLZ Tab";
    private static final String TAG = "FARPMainFragment";
    TabSpec CollectionTabSpec;

    public FARPTabHost() {
        SELECTED_TAB = FARP_SELECTED_TAB;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(R.layout.farp_main,
                container, false);

        mTabHost = (FragmentTabHost) _root.findViewById(android.R.id.tabhost);
        mTabHost.setup(pluginContext, getChildFragmentManager(),
                R.id.realtabcontent);

        CollectionTabSpec = mTabHost.newTabSpec(TANKER_TAB)
                .setIndicator(
                        "",
                        pluginContext.getResources().getDrawable(
                                R.drawable.farp_plane));
        mTabHost.addTab(CollectionTabSpec, FARPTankerFragment.class, null);

        TabSpec SurfaceDistressTabSpec = mTabHost.newTabSpec(REFUELEE_TAB)
                .setIndicator(
                        "",
                        pluginContext.getResources().getDrawable(
                                R.drawable.farp_gas));
        mTabHost.addTab(SurfaceDistressTabSpec, FARPReceiverFragment.class,
                null);
        for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {
            mTabHost.getTabWidget().getChildAt(i).setPadding(0, 0, 0, 0);
        }
        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int Tab2Open = user_settings.getInt(SELECTED_TAB, 0);
        mTabHost.setCurrentTab(Tab2Open + 1 % 3);

        mTabHost.setOnTabChangedListener(this);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                int Tab2Open = user_settings.getInt(SELECTED_TAB, 0);
                mTabHost.setCurrentTab(Tab2Open);
            }
        }, 300);
    }

    @Override
    public void onResume() {
        super.onResume();
        azpc.putSetting(ATSKConstants.CURRENT_SCREEN,
                ATSKConstants.CURRENT_SCREEN_AZ, TAG);
    }

    public static int getFARPImage(String type) {
        if (type == null)
            return R.drawable.atsk_farp_rx_layout_1;

        if (type.equals(ATSKConstants.FARP_RX_LAYOUT_SINGLE))
            return R.drawable.atsk_farp_rx_layout_1;
        else if (type.equals(ATSKConstants.FARP_RX_LAYOUT_HLEFT))
            return R.drawable.atsk_farp_rx_layout_2l;
        else if (type.equals(ATSKConstants.FARP_RX_LAYOUT_HRIGHT))
            return R.drawable.atsk_farp_rx_layout_2r;
        else if (type.equals(ATSKConstants.FARP_RX_LAYOUT_SPLIT))
            return R.drawable.atsk_farp_rx_layout_2;
        else if (type.equals(ATSKConstants.FARP_RX_LAYOUT_TRIPLE))
            return R.drawable.atsk_farp_rx_layout_3;
        else
            return R.drawable.atsk_farp_rx_layout_rgr;
    }
}
