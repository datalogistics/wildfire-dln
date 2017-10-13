
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
import com.gmeci.atsk.obstructions.buttons.LRFConfigDialog;

public class LRFPopupWindow extends PopupWindow {

    private Context _lrfContext;
    private View _lrfView;

    ImageView lrfMenuSpot1, lrfConfigImage;
    boolean CollectingArea = false;
    final OnClickListener lrfMenuClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            if (v.equals(lrfConfigImage))
                showGPSMenuConfig();

            if (CollectingArea) {
                if (v.equals(lrfMenuSpot1)) {
                    if (isLRFTwoP())
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_LRF,
                                false);
                    else
                        setCollectionState(
                                ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_1,
                                false);
                }
            }
            dismiss();
        }
    };

    public LRFPopupWindow() {
    }

    public void Initialize(Context context, boolean area) {
        _lrfContext = context;
        CollectingArea = area;

        SetupLayout();
        SetupView();
        setContentView(_lrfView);
    }

    private void SetupLayout() {
        setWidth(LayoutParams.WRAP_CONTENT);
        setHeight(LayoutParams.WRAP_CONTENT);
        setOutsideTouchable(true);
        //MIKE - the background must be !=null
        setBackgroundDrawable(new BitmapDrawable());
    }

    private void SetupView() {
        Context plugin = ATSKApplication.getInstance().getPluginContext();

        LayoutInflater layoutInflater = LayoutInflater.from(plugin);

        _lrfView = layoutInflater.inflate(R.layout.atsk_toolbar_menu_lrf, null);

        lrfMenuSpot1 = (ImageView) _lrfView.findViewById(R.id.lrf_menu_spot1);
        lrfMenuSpot1.setOnClickListener(lrfMenuClickListener);

        lrfConfigImage = (ImageView) _lrfView
                .findViewById(R.id.lrf_menu_config);
        lrfConfigImage.setOnClickListener(lrfMenuClickListener);

        //check if line or area.
        if (!CollectingArea)//point obstructions
        {
            //spot1 -> offset and config
            lrfMenuSpot1.setVisibility(View.GONE);
        } else {
            if (isLRFTwoP()) {
                lrfMenuSpot1.setImageResource(R.drawable.atsk_toolbar_lrf);
            } else
                lrfMenuSpot1.setImageResource(R.drawable.atsk_toolbar_twop);
        }
    }

    private String getState() {
        return ATSKApplication.getCollectionState();
    }

    public boolean isLRFTwoP() {
        return getState().equals(
                ATSKIntentConstants.OB_STATE_2PPLUSD_LRF)
                || getState()
                        .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_1)
                || getState()
                        .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_2);
    }

    private void showGPSMenuConfig() {
        LRFConfigDialog.ShowLRFConfigDialog(_lrfContext);
    }

    private void setCollectionState(String state, boolean action) {
        ATSKApplication.setObstructionCollectionMethod(state,
                ATSKATAKConstants.LRF_MENU, action);
    }

}
