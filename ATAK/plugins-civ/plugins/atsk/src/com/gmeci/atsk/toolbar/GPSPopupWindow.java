
package com.gmeci.atsk.toolbar;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.obstructions.buttons.GPSConfigDialog;

public class GPSPopupWindow extends PopupWindow {

    Context gpsContext;
    View gpsView;

    ImageView gpsMenuSpot1, gpsMenuSpot2, gpsMenuSpot3, gpsMenuConfig;
    boolean CollectingLine = false, CollectingArea = false;
    String CurrentObstructionState = "";
    final OnClickListener gpsMenuClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (!CollectingLine && !CollectingArea)//point obstructions
            {
                //spot1 -> offset and config
                if (v == gpsMenuSpot1) {
                    if (isGPSOffset())
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_GPS,
                                false);//no action.
                    else
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_OFFSET_GPS,
                                false);//no action.
                } else if (v == gpsMenuConfig) {
                    showGPSMenuConfig();
                }
            } else if (CollectingLine && !CollectingArea)//line obstructions
            {
                //spot1 -> breadcrumb, spot2->offset, config
                if (v == gpsMenuSpot1) {
                    if (isPullBC())
                        setCollectionState(ATSKIntentConstants.OB_STATE_GPS);
                    else
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_BC_GPS,
                                false);
                } else if (v == gpsMenuSpot2) {
                    if (isGPSOffset())
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_GPS,
                                false);
                    else
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_OFFSET_GPS,
                                false);
                } else if (v == gpsMenuConfig) {
                    showGPSMenuConfig();
                }
            } else if (CollectingArea && !CollectingLine)//area obstructions
            {
                //spot 1 breadcrumb, spot2 2p+d, spot3 offset
                if (v == gpsMenuSpot1) {
                    if (isPullBC())
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_GPS,
                                true);
                    else
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_BC_GPS,
                                true);
                } else if (v == gpsMenuSpot2) {
                    if (isPull2PPlusDGPS()) //MIKE - should be the GPS icon, as that is the current state
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_GPS,
                                false);
                    else
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_1,
                                false);
                } else if (v == gpsMenuSpot3) {
                    if (isGPSOffset())
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_GPS,
                                true);
                    else
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_OFFSET_GPS,
                                false);
                } else if (v == gpsMenuConfig) {
                    showGPSMenuConfig();
                }
            }
            dismiss();
        }
    };

    public GPSPopupWindow() {

    }

    public void Initialize(Context context, boolean line, boolean area,
            int imageViewHeight) {
        gpsContext = context;
        CollectingArea = area;
        CollectingLine = line;

        SetupLayout(imageViewHeight);
        SetupView();
        setContentView(gpsView);
    }

    private void SetupLayout(int height) {
        setWidth(LayoutParams.WRAP_CONTENT);
        setHeight(LayoutParams.WRAP_CONTENT);
        setOutsideTouchable(true);
        //MIKE - the background must be !=null
        setBackgroundDrawable(new BitmapDrawable());
    }

    private void SetupView() {
        Context plugin = ATSKApplication.getInstance().getPluginContext();

        LayoutInflater layoutInflater = LayoutInflater.from(plugin);
        gpsView = layoutInflater.inflate(R.layout.atsk_toolbar_menu_gps, null);

        gpsMenuSpot1 = (ImageView) gpsView.findViewById(R.id.gps_menu_spot1);
        gpsMenuSpot1.setOnClickListener(gpsMenuClickListener);

        gpsMenuSpot2 = (ImageView) gpsView.findViewById(R.id.gps_menu_spot2);
        gpsMenuSpot2.setOnClickListener(gpsMenuClickListener);

        gpsMenuSpot3 = (ImageView) gpsView.findViewById(R.id.gps_menu_spot3);
        gpsMenuSpot3.setOnClickListener(gpsMenuClickListener);

        gpsMenuConfig = (ImageView) gpsView.findViewById(R.id.gps_menu_config);
        gpsMenuConfig.setOnClickListener(gpsMenuClickListener);

        getCurrentState();

        //check if line or area.
        //use collecting line and area to determine type.
        if (!CollectingLine && !CollectingArea)//point obstructions
        {
            //spot1 -> offset and config
            gpsMenuSpot1.setClickable(true);
            gpsMenuSpot1.setVisibility(View.VISIBLE);
            gpsMenuSpot2.setVisibility(View.GONE);
            gpsMenuSpot2.setClickable(false);
            gpsMenuSpot3.setVisibility(View.GONE);
            gpsMenuSpot3.setClickable(false);

            if (isGPSOffset())
                gpsMenuSpot1.setImageResource(R.drawable.atsk_toolbar_gps);
            else
                gpsMenuSpot1.setImageResource(R.drawable.atsk_toolbar_pd);
        } else if (CollectingLine && !CollectingArea)//line obstructions
        {
            //spot1 -> breadcrumb, spot2->vertex, config
            gpsMenuSpot1.setClickable(true);
            gpsMenuSpot1.setVisibility(View.VISIBLE);
            gpsMenuSpot2.setVisibility(View.VISIBLE);
            gpsMenuSpot2.setClickable(true);
            gpsMenuSpot3.setVisibility(View.GONE);
            gpsMenuSpot3.setClickable(false);

            if (isPullBC())
                gpsMenuSpot1.setImageResource(R.drawable.atsk_toolbar_gps);
            else
                gpsMenuSpot1
                        .setImageResource(R.drawable.atsk_toolbar_breadcrumb);

            if (isGPSOffset())
                gpsMenuSpot2.setImageResource(R.drawable.atsk_toolbar_gps);
            else
                gpsMenuSpot2.setImageResource(R.drawable.atsk_toolbar_pd);
        } else if (CollectingArea && !CollectingLine)//area obstructions
        {
            //spot 1 breadcrumb, spot2 2p+d, spot3 offset
            gpsMenuSpot1.setClickable(true);
            gpsMenuSpot1.setVisibility(View.VISIBLE);
            gpsMenuSpot2.setVisibility(View.VISIBLE);
            gpsMenuSpot2.setClickable(true);
            gpsMenuSpot3.setVisibility(View.VISIBLE);
            gpsMenuSpot3.setClickable(true);

            gpsMenuSpot1.setImageResource(R.drawable.atsk_toolbar_breadcrumb);
            gpsMenuSpot2.setImageResource(R.drawable.atsk_toolbar_twop);
            gpsMenuSpot3.setImageResource(R.drawable.atsk_toolbar_pd);

            if (isPullBC())
                gpsMenuSpot1.setImageResource(R.drawable.atsk_toolbar_gps);
            if (isPull2PPlusDGPS())
                gpsMenuSpot2.setImageResource(R.drawable.atsk_toolbar_gps);
            if (isGPSOffset())
                gpsMenuSpot2.setImageResource(R.drawable.atsk_toolbar_gps);
        }
    }

    private void getCurrentState() {
        CurrentObstructionState = ATSKApplication.getCollectionState();
    }

    private void showGPSMenuConfig() {
        GPSConfigDialog.ShowGPSConfigDialog(gpsContext);
    }

    private void setCollectionState(String state) {
        setCollectionState(state, false);
    }

    private void setCollectionState(String state, boolean action) {
        ATSKApplication.setObstructionCollectionMethod(state,
                ATSKATAKConstants.GPS_MENU, action);
    }

    private String getState() {
        return ATSKApplication.getCollectionState();
    }

    public boolean isPullBC() {
        return getState().equals(ATSKIntentConstants.OB_STATE_BC_GPS)
                || getState().equals(ATSKIntentConstants.OB_STATE_BC_GPS_OFF);
    }

    private boolean isGPSOffset() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_OFFSET_GPS);
    }

    public boolean isPull2PPlusDGPS() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS)
                || CurrentObstructionState
                        .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_1)
                || CurrentObstructionState
                        .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_2);
    }

}
