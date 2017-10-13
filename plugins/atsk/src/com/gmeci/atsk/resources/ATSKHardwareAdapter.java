
package com.gmeci.atsk.resources;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.ipc.AtakBroadcast;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyPoint;

public class ATSKHardwareAdapter {

    /**
     * HOW TO USE HARDWARE ADAPTER
     **/
    /* NOTE: This is implemented in ATSKBASEFRAGMENT (If extending that, you dont need this)
     * However, there are several small things required to successfully implement this adapter.
     *  
     * ATSKHardwareAdapter hardwareAdapter;
     * 
     *   - a function  , called in onResume() like the one below
     *   public void setupHardwareEvents()
         {        
            hardwareAdapter = new ATSKHardwareAdapter();
            hardwareAdapter.SetupHardwareAdapter(getActivity().getApplicationContext());
            hardwareAdapter.setListener(new HardwareEventListener() 
            {
                public void LRFEvent(double lat, double lon, double range, double azimuth, float elev)
                {
                    //This is where you RECEIVE the intent                
                }
            });    
          }    
      Finally, you must unregister the receiver in the onPause()
      hardwareAdapter.unregister(getActivity().getApplicationContext());
     */

    HardwareEventListener hardwareListener = null;
    public final BroadcastReceiver laserPosInputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            if (i.getAction().equals(ATSKConstants.GMECI_HARDWARE_LRF_ACTION)) {
                SurveyPoint sp = i.getParcelableExtra(ATSKConstants.
                        SURVEY_POINT_EXTRA);
                if (sp == null) {
                    sp = new SurveyPoint(
                            i.getDoubleExtra(ATSKConstants.LAT_EXTRA, 0),
                            i.getDoubleExtra(ATSKConstants.LON_EXTRA, 0));
                    sp.setHAE(i.getFloatExtra(ATSKConstants.ALT_EXTRA, 0.0f));
                    sp.circularError = i.getDoubleExtra(
                            ATSKConstants.CE_EXTRA, 20);
                    sp.linearError = i.getDoubleExtra(
                            ATSKConstants.LE_EXTRA, 20);
                    sp.collectionMethod = SurveyPoint.CollectionMethod.MANUAL;
                }
                double range = i.getDoubleExtra(ATSKConstants.RANGE_M, 0);
                double azimuth = i.getDoubleExtra(ATSKConstants.AZIMUTH_T, 0);
                double elev = i.getDoubleExtra(ATSKConstants.ELEVATION, 0);

                if (hardwareListener != null)
                    hardwareListener.LRFEvent(sp, range, azimuth, elev);

            }
        }
    };

    public void SetupHardwareAdapter(Context context) {
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(ATSKConstants.GMECI_HARDWARE_LRF_ACTION);
        AtakBroadcast.getInstance().registerSystemReceiver(
                laserPosInputReceiver,
                filter);
    }

    public void setListener(HardwareEventListener hardwareEventListener) {
        hardwareListener = hardwareEventListener;
    }

    public void unregister(Context context) {

        AtakBroadcast.getInstance().unregisterSystemReceiver(
                laserPosInputReceiver);
    }

}
