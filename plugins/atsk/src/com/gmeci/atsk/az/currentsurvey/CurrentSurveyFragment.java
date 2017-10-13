
package com.gmeci.atsk.az.currentsurvey;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.atsk.resources.ServiceConnectionManager;
import com.gmeci.atsk.resources.ServiceConnectionManagerInterface;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.conversions.Conversions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CurrentSurveyFragment extends Fragment implements
        UpdateSurveyNameInterface, OnClickListener,
        ServiceConnectionManagerInterface {

    private static final String TAG = "CurrentSurveyFragment";

    private ImageView _azTypeImg;
    private SurveySelectionDialog _surveyDialog;
    private UpdateSurveyNameInterface _parent;
    private String _surveyUID = "";
    private String _surveyName = "";
    private String _surveyType = "";
    private ServiceConnectionManager _scm;
    private View _root;
    private Context _context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _scm = new ServiceConnectionManager();
        _scm.startServiceManagement(getActivity(), this,
                "CurrentSurveyFragment");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        _scm.stopServiceManagement();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        LayoutInflater pluginInflater = LayoutInflater.from(pluginContext);
        _root = pluginInflater.inflate(R.layout.current_survey_fragment,
                container,
                false);

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SetupViews();
    }

    private void SetupViews() {

        _azTypeImg = (ImageView) _root
                .findViewById(R.id.atsk_surveyselection_image_type);
        _azTypeImg.setOnClickListener(this);

        _root.findViewById(R.id.hide).setOnClickListener(this);
        _root.findViewById(R.id.close).setOnClickListener(this);
        _root.findViewById(R.id.menu).setOnClickListener(this);
        _root.findViewById(R.id.panTo).setOnClickListener(this);

        updateCurrentSurveyTypeDisplay(_surveyType);

    }

    @Override
    public void updateCurrentSurveyHandle(String newUID, String newName,
            String newType) {

        if (!newUID.equals(_surveyUID)
                || !newType.equals(_surveyType)) {
            //if the uid changes. take me home.

            _surveyUID = newUID;
            _surveyType = newType;
            _surveyName = newName;

            updateCurrentSurvey(newUID, newName, newType);
        }
    }

    public void updateCurrentSurvey(String newUID, String name, String type) {
        updateCurrentSurveyTypeDisplay(type);
        _parent.updateCurrentSurveyHandle(newUID, name, type);
    }

    private void updateCurrentSurveyTypeDisplay(String type) {
        if (_azTypeImg != null)
            _azTypeImg.setImageResource(getAZResource(type));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.atsk_surveyselection_image_type) {
            AZProviderClient azpc = new AZProviderClient(_context);
            azpc.Start();
            _surveyUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY,
                    TAG);
            SurveyData currentSurvey = azpc.getAZ(_surveyUID, true);
            if (currentSurvey != null)
                zoom(currentSurvey);
            azpc.Stop();
        } else if (v.getId() == R.id.panTo) {
            final MapView view = MapView.getMapView();
            if (view.getSelfMarker().getGroup() != null) {
                view.getMapController().panTo(view.getSelfMarker().getPoint(),
                        false);
            } else {
                view.post(new Runnable() {
                    public void run() {
                        Toast.makeText(
                                view.getContext(),
                                "Location not set, please place your location.",
                                Toast.LENGTH_SHORT).show();

                    }
                });
            }
        } else if (v.getId() == R.id.menu) {
            showSurveySelectionDlg();
        } else if (v.getId() == R.id.close) {
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent("com.gmeci.atsk.CLOSE_DROP_DOWN"));
        } else if (v.getId() == R.id.hide) {
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent("com.gmeci.atsk.HIDE_DROP_DOWN"));

        }
    }

    public void zoom(SurveyData sd) {
        if (sd.getType() == AZ_TYPE.FARP) {
            MapGroup azGroup = MapView.getMapView().getRootGroup()
                    .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
            if (azGroup != null) {
                MapGroup surveyGroup = azGroup.findMapGroup(sd.uid);
                if (surveyGroup != null) {
                    List<GeoPoint> points = new ArrayList<GeoPoint>();
                    for (MapItem mi : surveyGroup.getItems()) {
                        if (mi instanceof Shape)
                            points.addAll(Arrays.asList(
                                    ((Shape) mi).getPoints()));
                    }
                    if (!points.isEmpty()) {
                        GeoBounds gb = GeoBounds.createFromPoints(
                                points.toArray(new GeoPoint[points.size()]));
                        centerAndZoomMapOnLocation(gb.getCenter(null),
                                Conversions.CalculateRangem(gb.getSouth(),
                                        gb.getWest(), gb.getNorth(),
                                        gb.getEast()));
                        return;
                    }
                }
            }
        }
        centerAndZoomMapOnLocation(
                MapHelper.convertSurveyPoint2GeoPoint(sd.center),
                Math.max(sd.getLength(true), sd.width) * 2);
    }

    public void centerAndZoomMapOnLocation(final GeoPoint center,
            final double length_m) {

        Log.d(TAG, "center and zoom into location: " + center);

        //get top left from
        double[] topLeft = Conversions.AROffset(center.getLatitude(),
                center.getLongitude(), -45, length_m / 2);
        double[] botRight = Conversions.AROffset(center.getLatitude(),
                center.getLongitude(), 135, length_m / 2);

        MapView mapView = MapView.getMapView();
        com.atakmap.android.util.ATAKUtilities.scaleToFit(mapView,
                new GeoPoint[] {
                        new GeoPoint(topLeft[0], topLeft[1]),
                        new GeoPoint(botRight[0], botRight[1])
                },
                mapView.getWidth(), mapView.getHeight());

    }

    public void showSurveySelectionDlg() {
        _surveyDialog = new SurveySelectionDialog();
        _surveyDialog.SetUpdateInterface(CurrentSurveyFragment.this);
        _surveyDialog.show(getFragmentManager(), TAG);
    }

    private void getCurrentSurveyNameType() {
        AZProviderClient azpc = new AZProviderClient(_context);
        azpc.Start();
        _surveyUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);
        SurveyData currentSurvey = azpc.getAZ(_surveyUID, true);
        if (currentSurvey != null) {
            _surveyName = currentSurvey.getSurveyName();
            _surveyType = currentSurvey.getType().toString();
            updateCurrentSurvey(_surveyUID, _surveyName,
                    _surveyType);
        }
        azpc.Stop();
    }

    public void setup(Context context, UpdateSurveyNameInterface parent) {
        _context = context;
        _parent = parent;
        getCurrentSurveyNameType();
    }

    public String getCurrentSurveyType() {
        return _surveyType;
    }

    public String getCurrentSurveyName() {
        return _surveyName;
    }

    public String getCurrentSurveyUID() {
        return _surveyUID;
    }

    public static int getAZResource(String azType) {
        if (azType.equals(AZ_TYPE.HLZ.name()))
            return R.drawable.navigation_hlz;
        else if (azType.equals(AZ_TYPE.LZ.name())
                || azType.equals(AZ_TYPE.STOL.name()))
            return R.drawable.navigation_lz;
        else if (azType.equals(AZ_TYPE.FARP.name()))
            return R.drawable.navigation_farp;
        return R.drawable.navigation_dz;
    }

    public int getAZResource() {
        return getAZResource(_surveyType);
    }

    @Override
    public void GotHardwareHandle() {

    }

    @Override
    public void GotATSKServiceHandle() {
        // TODO Auto-generated method stub

    }

}
