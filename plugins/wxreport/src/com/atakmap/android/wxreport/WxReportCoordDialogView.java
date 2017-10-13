
package com.atakmap.android.wxreport;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.elev.dt2.Dt2ElevationModel;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.user.geocode.GeocodingTask;
import com.atakmap.android.user.geocode.ReverseGeocodingTask;
import com.atakmap.android.util.AltitudeUtilities;
import com.atakmap.android.wxreport.plugin.ContextHelperSingleton;
import com.atakmap.android.wxreport.plugin.R;
import com.atakmap.app.preferences.GeocoderPreferenceFragment;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.coremap.maps.coords.AltitudeSource;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.text.DecimalFormat;

import com.atakmap.coremap.locale.LocaleUtil;

public class WxReportCoordDialogView extends LinearLayout
{
    // Use a tab change listener to update the other coordinate formats from the previous one
    public static final String TAG = "WxReportCoordDialogView";
    private TabHost _host;
    private Result _result = Result.INVALID;
    private Dt2ElevationModel _dem;
    private String _centerZone, _centerSquare;
    private String _currElevMSL = "" + Altitude.UNKNOWN.getValue();
    private GeoPoint _currPoint;
    private String[] _currDD = new String[] {
            "", ""
    };
    private String[] _currAddress = new String[] {
            "", ""
    };
    private String[] _currMGRS = new String[] {
            "", "", "", ""
    };
    private String[] _currDM = new String[] {
            "", "", "", ""
    };
    private String[] _currDMS = new String[] {
            "", "", "", "", "", ""
    };
    private CoordinateFormat _currFormat = CoordinateFormat.MGRS;
    private SharedPreferences _prefs;
    private EditText _elevText;
    private CheckBox _dropPsointChk;
    private CheckBox _dropAddressChk;
    private RadioGroup _affiliationGroup;
    private TextView _licenseTv;
    private Button _mgrsButton;
    private EditText addressET; //Address Edit Text Field
    private EditText _mgrsZone, _mgrsSquare, _mgrsEast, _mgrsNorth, _mgrsRaw;
    private boolean watch = true; // only used to make surce setting the
                                  // mgrsRaw does not cycle back around
    private EditText _ddLat, _ddLon;
    private EditText _dmsLatD, _dmsLatM, _dmsLatS, _dmsLonD, _dmsLonM,
            _dmsLonS;
    private EditText _dmLatD, _dmLatM, _dmLonD, _dmLonM;
    private static final DecimalFormat NO_DEC_FORMAT = new DecimalFormat("##0");
    private static final DecimalFormat ONE_DEC_FORMAT = new DecimalFormat(
            "##0.0");
    private static final DecimalFormat TWO_DEC_FORMAT = new DecimalFormat(
            "##0.00");
    private AltitudeSource altSource = AltitudeSource.UNKNOWN;
    private boolean isADDRtabDisabled;
    protected String geoService;
    protected String geoServiceKey;
    protected String humanAddress;
    protected GeoPoint humanAddressPoint;
    private final Context pluginContext;
    private MapView mapView;

    public WxReportCoordDialogView(final Context context)
    {
        super(context);
        this.pluginContext = context;
    }

    public WxReportCoordDialogView(final Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.pluginContext = context;
    }

    public void setParameters(GeoPoint point, GeoPoint mapCenter,
            CoordinateFormat format) {
        _setPoint(point);
        if (point != null) {
            _setElev();
        }
        _setMapCenter(mapCenter);
        _setFormat(format);
        syncRawMGRS();

    }

    public void setParameters(GeoPoint point, GeoPoint mapCenter) {
        setParameters(point, mapCenter, CoordinateFormat.MGRS);
    }

    public Result getResult() {
        return _result;
    }

    public GeoPoint getPoint() {
        return _getGeoPoint();
    }

    public CoordinateFormat getCoordFormat() {
        return _currFormat;
    }

    /**
     * @return Point, or null if unrecognized format.
     */
    private GeoPoint _getPoint() {
        GeoPoint point = null;
        switch (_currFormat) {
            case MGRS:
                point = _getMGRS();
                break;
            case DD:
                point = _getDD();
                break;
            case DM:
                point = _getDM();
                break;
            case DMS:
                point = _getDMS();
                break;
            case ADDRESS:
                point = _getAddress();
                break;
            default:
                break;

        }
        if (point != null && _result == Result.INVALID) {
            _result = Result.VALID_CHANGED;
        }
        return point;
    }

    // perform basic validation of text fields
    private void _validatePoint() {
        GeoPoint point = null;
        switch (_currFormat) {
            case MGRS:
                point = _getMGRSForConvert();
                break;
            case DD:
                point = _getDDForConvert();
                break;
            case DM:
                point = _getDMForConvert();
                break;
            case DMS:
                point = _getDMSForConvert();
                break;
            case ADDRESS:
                point = _getAddressForConvert();
                break;
        }
        if (point != null) {
            _result = Result.VALID_CHANGED;
        }
    }

    private GeoPoint _getGeoPoint() {
        Altitude a = Altitude.UNKNOWN;

        GeoPoint point = _getPoint();
        if (point == null)
            return null; //Return null if point failed

        _validatePoint();

        try {
            String elev = _elevText.getText().toString().trim();
            if (!elev.isEmpty()) {
                a = new Altitude(SpanUtilities.convert(
                        Double.parseDouble(elev), Span.FOOT,
                        Span.METER),
                        AltitudeReference.MSL, altSource);
            }
        } catch (NumberFormatException e) {
            Log.e("WxReportCoordDialogView", "NFE in _getGeoPoint()");
        }

        if (!_currElevMSL.equals(String.valueOf(a))
                && _result == Result.VALID_UNCHANGED) {
            _result = Result.VALID_CHANGED;

            if (this.altSource == AltitudeSource.UNKNOWN) {
                // if it changed, and it wasn't dted, then it was the user
                altSource = AltitudeSource.USER;
                // this causes a problem when dted was picked, then
                // manually edited. TODO
            }

        }
        GeoPoint ret = new GeoPoint(point.getLatitude(),
                point.getLongitude(), EGM96.getInstance().getHAE(
                        point.getLatitude(),
                        point.getLongitude(), a),
                GeoPoint.CE90_UNKNOWN,
                GeoPoint.LE90_UNKNOWN);
        _currPoint = ret;
        return ret;
    }

    /**
     * @return returns the formatted string given the current tab used
     */
    public String getFormattedString() {
        if (_currPoint != null) {
            final String p = CoordinateFormatUtilities.formatToString(
                    _currPoint, _currFormat);
            final String a = AltitudeUtilities.format(_currPoint,
                    _prefs);

            return p + "\n" + a;
        } else {
            return "error";
        }

    }

    private void _setFormat(CoordinateFormat format) {
        Log.d(TAG,
                "_setFormat: " + format.toString() + ", " + format.getValue());
        _currFormat = format;
        if (_host != null) {
            _host.setCurrentTab(_currFormat.getValue());
        }

        // The Tab listener isn't getting called, so force the MGRS grid button to be invisible
        // here.
        if (_currFormat != CoordinateFormat.MGRS) {
            _mgrsButton.setVisibility(View.GONE);
        }

        if (_currFormat != CoordinateFormat.ADDRESS) {
            _dropAddressChk.setVisibility(View.GONE);
            _licenseTv.setVisibility(View.GONE);
        } else {
            _dropAddressChk.setVisibility(View.VISIBLE);
            _licenseTv.setVisibility(View.VISIBLE);
        }

    }

    private void _setPoint(GeoPoint p) {
        _currPoint = p;
        _updatePoint();
    }

    private void _setMapCenter(GeoPoint center) {
        String[] c = CoordinateFormatUtilities.formatToStrings(center,
                CoordinateFormat.MGRS);

        if (c == null || c.length < 2) 
              return;

        _centerZone = c[0];
        _centerSquare = c[1];
    }

    private double altVal = 0d;

    private boolean _pullFromDTED() {
        boolean ret = false;
        GeoPoint point = _getPoint();
        if (point != null) {
            // pull the elevation and make sure it is in MSL
            Altitude altHAE = _dem.queryPoint(point.getLatitude(),
                    point.getLongitude());
            Altitude alt = EGM96.getInstance().getMSL(point.getLatitude(),
                    point.getLongitude(),
                    altHAE);
            if (alt.isValid()) {
                String elev = _formatElevation(SpanUtilities.convert(
                        alt.getValue(),
                        Span.METER, Span.FOOT));
                _elevText.setText(elev);

                altSource = alt.getSource();
                ret = true;
                altVal = alt.getValue();
            } else {
                Toast.makeText(mapView.getContext(),
                        "DTED data not available for this coordinate.",
                        Toast.LENGTH_LONG).show();
            }
        }
        return ret;
    }

    private GeoPoint _getMGRS() {
        try {
            String[] coord = new String[] {
                    _mgrsZone.getText().toString()
                            .toUpperCase(LocaleUtil.getCurrent()),
                    _mgrsSquare.getText().toString()
                            .toUpperCase(LocaleUtil.getCurrent()),
                    _mgrsEast.getText().toString(),
                    _mgrsNorth.getText().toString()
            };
            return CoordinateFormatUtilities.convert(coord,
                    CoordinateFormat.MGRS);
        } catch (IllegalArgumentException e) {
            String msg = "An error has occurred getting the MGRS point";
            Log.e(TAG, msg, e);
            Toast.makeText(mapView.getContext(), msg, Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private GeoPoint _getMGRSForConvert() {
        String zone = _mgrsZone.getText().toString().trim()
                .toUpperCase(LocaleUtil.getCurrent());
        String square = _mgrsSquare.getText().toString().trim()
                .toUpperCase(LocaleUtil.getCurrent());
        String east = _mgrsEast.getText().toString().trim();
        String north = _mgrsNorth.getText().toString().trim();
        // Check if any of the edit texts are empty
        if (zone.isEmpty() || square.isEmpty() || east.isEmpty()
                || north.isEmpty()) {
            // They haven't finished entering in so don't try to convert, and don't throw
            // a toast to say the coordinate entry is invalid
            return null;
        }
        // The user didn't change the text since it was set so don't update
        if (zone.equals(_currMGRS[0]) && square.equals(_currMGRS[1])
                && east.equals(_currMGRS[2]) && north.equals(_currMGRS[3])) {
            return null;
        }
        _currMGRS = new String[] {
                zone, square, east, north
        };
        return _getMGRS();
    }

    private void _setMGRS(GeoPoint p) {
        String[] mgrs;
        if (p != null)
            mgrs = CoordinateFormatUtilities.formatToStrings(p,
                    CoordinateFormat.MGRS);
        else
            mgrs = new String[] {
                    "", "", "", ""
            };

        _mgrsZone.setText(mgrs[0]);
        _mgrsSquare.setText(mgrs[1]);
        _mgrsEast.setText(mgrs[2]);
        _mgrsNorth.setText(mgrs[3]);
        _currMGRS = mgrs;
    }

    private GeoPoint _getDD() {
        try {
            String[] coord = new String[] {
                    _ddLat.getText().toString(), _ddLon.getText().toString()
            };
            return CoordinateFormatUtilities
                    .convert(coord, CoordinateFormat.DD);
        } catch (IllegalArgumentException e) {
            String msg = "An error has occurred getting the decimal degree point";
            Log.e(TAG, msg, e);
            Toast.makeText(mapView.getContext(), msg, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private GeoPoint _getAddress() {
        try {
            String[] coord = new String[] {
                    _ddLat.getText().toString(), _ddLon.getText().toString()
            };
            return CoordinateFormatUtilities
                    .convert(coord, CoordinateFormat.ADDRESS);
        } catch (IllegalArgumentException e) {
            String msg = "An error has occurred getting the address point";
            Log.e(TAG, msg, e);
            Toast.makeText(mapView.getContext(), msg, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private GeoPoint _getDDForConvert() {
        String lon = _ddLon.getText().toString().trim();
        String lat = _ddLat.getText().toString().trim();
        // Check if any of the edit texts are empty
        if (lon.isEmpty() || lat.isEmpty()) {
            // They haven't finished entering in so don't try to convert, and don't throw
            // a toast to say the coordinate entry is invalid
            return null;
        }
        // The user didn't change the text since it was set so don't update
        if (lat.equals(_currDD[0]) && lon.equals(_currDD[1])) {
            return null;
        }
        _currDD = new String[] {
                lat, lon
        };
        return _getDD();
    }

    private GeoPoint _getAddressForConvert() {
        String lon = _ddLon.getText().toString().trim();
        String lat = _ddLat.getText().toString().trim();
        // Check if any of the edit texts are empty
        if (lon.isEmpty() || lat.isEmpty()) {
            // They haven't finished entering in so don't try to convert, and don't throw
            // a toast to say the coordinate entry is invalid
            return null;
        }
        // The user didn't change the text since it was set so don't update
        if (lat.equals(_currAddress[0]) && lon.equals(_currAddress[1])) {
            return null;
        }
        _currAddress = new String[] {
                lat, lon
        };
        return _getAddress();
    }

    private void _setDD(GeoPoint p) {
        String[] dd;
        if (p != null) {
            dd = CoordinateFormatUtilities.formatToStrings(p,
                    CoordinateFormat.DD);
        } else {
            dd = new String[] {
                    "", ""
            };
        }

        _ddLat.setText(dd[0]);
        _ddLon.setText(dd[1]);
        _currDD = dd;
    }

    private void _setAddress(GeoPoint p) {
    }

    private GeoPoint _getDM() {
        try {
            String[] coord = new String[] {
                    _dmLatD.getText().toString(), _dmLatM.getText().toString(),
                    _dmLonD.getText().toString(), _dmLonM.getText().toString()
            };
            return CoordinateFormatUtilities
                    .convert(coord, CoordinateFormat.DM);
        } catch (IllegalArgumentException e) {
            String msg = "An error has occurred getting the degree minute point";
            Log.e(TAG, msg, e);
            Toast.makeText(mapView.getContext(), msg, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private GeoPoint _getDMForConvert() {

        String latD = _dmLatD.getText().toString().trim();
        String latM = _dmLatM.getText().toString().trim();
        String lonD = _dmLonD.getText().toString().trim();
        String lonM = _dmLonM.getText().toString().trim();
        // Check if any of the edit texts are empty
        if (latD.isEmpty() || latM.isEmpty() || lonD.isEmpty()
                || lonM.isEmpty()) {
            // They haven't finished entering in so don't try to convert, and don't throw
            // a toast to say the coordinate entry is invalid
            return null;
        }
        // The user didn't change the text since it was set so don't update
        if (latD.equals(_currDM[0]) && latM.equals(_currDM[1])
                && lonD.equals(_currDM[2]) && lonM.equals(_currDM[3])) {
            return null;
        }
        _currDM = new String[] {
                latD, latM, lonD, lonM
        };
        return _getDM();
    }

    private void _setDM(GeoPoint p) {
        String[] dm;
        if (p != null) {
            dm = CoordinateFormatUtilities.formatToStrings(p,
                    CoordinateFormat.DM);
        } else {
            dm = new String[] {
                    "", "", "", ""
            };
        }

        _dmLatD.setText(dm[0]);
        _dmLatM.setText(dm[1]);
        _dmLonD.setText(dm[2]);
        _dmLonM.setText(dm[3]);
        _currDM = dm;
    }

    private GeoPoint _getDMS() {
        try {
            String[] coord = new String[] {
                    _dmsLatD.getText().toString(),
                    _dmsLatM.getText().toString(),
                    _dmsLatS.getText().toString(),
                    _dmsLonD.getText().toString(),
                    _dmsLonM.getText().toString(),
                    _dmsLonS.getText().toString()
            };
            return CoordinateFormatUtilities.convert(coord,
                    CoordinateFormat.DMS);
        } catch (IllegalArgumentException e) {
            String msg = "An error has occurred getting the degree minute second point";
            Log.e(TAG, msg, e);
            Toast.makeText(mapView.getContext(), msg, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private GeoPoint _getDMSForConvert() {
        String latD = _dmsLatD.getText().toString().trim();
        String latM = _dmsLatM.getText().toString().trim();
        String latS = _dmsLatS.getText().toString().trim();
        String lonD = _dmsLonD.getText().toString().trim();
        String lonM = _dmsLonM.getText().toString().trim();
        String lonS = _dmsLonS.getText().toString().trim();

        // Check if any of the edit texts are empty
        if (latD.isEmpty() || latM.isEmpty() || latS.isEmpty()
                || lonD.isEmpty() || lonM.isEmpty() || lonS.isEmpty()) {
            // They haven't finished entering in so don't try to convert, and don't throw
            // a toast to say the coordinate entry is invalid
            return null;
        }
        if (latD.equals(_currDMS[0]) && latM.equals(_currDMS[1])
                && latS.equals(_currDMS[2])
                && lonD.equals(_currDMS[3]) && lonM.equals(_currDMS[4])
                && lonS.equals(_currDMS[5])) {
            return null;
        }
        _currDMS = new String[] {
                latD, latM, latS, lonD, lonM, lonS
        };
        return _getDMS();
    }

    private void _setDMS(GeoPoint p) {
        String[] dms;
        if (p != null) {
            dms = CoordinateFormatUtilities.formatToStrings(p,
                    CoordinateFormat.DMS);
        } else {
            dms = new String[] {
                    "", "", "", "", "", ""
            };
        }
        _dmsLatD.setText(dms[0]);
        _dmsLatM.setText(dms[1]);
        _dmsLatS.setText(dms[2]);
        _dmsLonD.setText(dms[3]);
        _dmsLonM.setText(dms[4]);
        _dmsLonS.setText(dms[5]);
        _currDMS = dms;

    }

    @Override
    protected void onFinishInflate() {

        super.onFinishInflate();
        mapView = ContextHelperSingleton.getInstance().getMapView();
        _prefs = PreferenceManager.getDefaultSharedPreferences(mapView
                .getContext());

        _dropAddressChk = (CheckBox) findViewById(R.id.coordDialogAddress);

        _licenseTv = (TextView) findViewById(R.id.license);

        geoService = _prefs.getString("geocodeSupplier",
                "http://nominatim.openstreetmap.org/");
        geoServiceKey = _prefs.getString("geocodeSupplierKey",
                "").trim();

        String supplier = _prefs.getString("geocodeSupplierName",
                GeocoderPreferenceFragment.DEFAULT);
        if (_prefs.getBoolean("builtInGeocoder", true)) {
            geoService = "Android";
            supplier = "Android Geocoder";
        }
        _licenseTv.setText(supplier);

        if (!isInEditMode()) { // The editor can't handle tabs apparently;
            _host = (TabHost) findViewById(R.id.coordDialogTabHost);
            _host.setup();
            TabSpec _mgrsSpec = _host
                    .newTabSpec(CoordinateFormat.MGRS.getDisplayName());
            _mgrsSpec.setIndicator(CoordinateFormat.MGRS.getDisplayName());
            _mgrsSpec.setContent(R.id.coordDialogMGRSView);
            TabSpec _ddSpec = _host.newTabSpec(CoordinateFormat.DD
                    .getDisplayName());
            _ddSpec.setIndicator(CoordinateFormat.DD.getDisplayName());
            _ddSpec.setContent(R.id.coordDialogDDView);
            TabSpec _dmSpec = _host.newTabSpec(CoordinateFormat.DM
                    .getDisplayName());
            _dmSpec.setIndicator(CoordinateFormat.DM.getDisplayName());
            _dmSpec.setContent(R.id.coordDialogDMView);
            TabSpec _dmsSpec = _host.newTabSpec(CoordinateFormat.DMS
                    .getDisplayName());
            _dmsSpec.setIndicator(CoordinateFormat.DMS.getDisplayName());
            _dmsSpec.setContent(R.id.coordDialogDMSView);
            TabSpec addyToLatLongTab = _host
                    .newTabSpec(CoordinateFormat.ADDRESS
                            .getDisplayName());
            addyToLatLongTab.setIndicator(CoordinateFormat.ADDRESS
                    .getDisplayName());
            addyToLatLongTab.setContent(R.id.addyToLatLongTab);

            _host.addTab(_mgrsSpec);
            _host.addTab(_ddSpec);
            _host.addTab(_dmSpec);
            _host.addTab(_dmsSpec);
            _host.addTab(addyToLatLongTab);

            ((TextView) _host.getTabWidget().getChildAt(0)
                    .findViewById(android.R.id.title)).setTextSize(10);
            ((TextView) _host.getTabWidget().getChildAt(1)
                    .findViewById(android.R.id.title)).setTextSize(10);
            ((TextView) _host.getTabWidget().getChildAt(2)
                    .findViewById(android.R.id.title)).setTextSize(10);
            ((TextView) _host.getTabWidget().getChildAt(3)
                    .findViewById(android.R.id.title)).setTextSize(10);
            ((TextView) _host.getTabWidget().getChildAt(4)
                    .findViewById(android.R.id.title)).setTextSize(10);

            checkADDRtab();

            if ((_prefs.getBoolean("enableGeocoder", true))) {
                _host.getTabWidget().getChildTabViewAt(4)
                        .setVisibility(VISIBLE);
            } else {
                _host.getTabWidget().getChildTabViewAt(4).setVisibility(GONE);
            }

            _host.setCurrentTab(_currFormat.getValue());
            Log.d(TAG, "onFinishInflate: " + _currFormat.toString() + ", "
                    + _currFormat.getValue());

            post(new Runnable() {

                @Override
                public void run() {
                    // If we don't set the size in code, the hints will expand
                    // it even though they're invisible!
                    // Don't think there's a way to set it strictly to parent's
                    // size in XML? fit_parent won't contradict the size widgets
                    // want to be
                    findViewById(R.id.coordDialogDMSView).getLayoutParams().width = ((View) findViewById(
                            R.id.coordDialogDMSView).getParent())
                            .getMeasuredWidth();

                    // Hack to set tab bar's height because apparently on 7'' tab with two-line tab
                    // labels it doesn't automatically...?
                    // findViewById(android.R.id.tabs).getLayoutParams().height =
                    // _ddTabView.getMeasuredHeight();
                }
            });
        }

        _host.getTabWidget().getChildAt(4)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideKeyboard();

                        checkADDRtab();
                        if (isADDRtabDisabled) {
                            Toast.makeText(
                                    mapView.getContext(),
                                    "Unable to reach the Address Server\n    Click tab to try again",
                                    Toast.LENGTH_SHORT).show();
                            if (_currFormat == CoordinateFormat.ADDRESS)
                                _host.setCurrentTab(0);

                        } else {
                            _host.setCurrentTab(4);
                        }

                    }
                });

        _host.setOnTabChangedListener(new OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                GeoPoint point = null;
                // the previous format used
                switch (_currFormat) {
                    case MGRS:
                        // if the tab used to be mgrs then get rid of the mgrs button
                        _mgrsButton.setVisibility(View.INVISIBLE);
                        point = _getMGRSForConvert();
                        if (point != null) {
                            _setDD(point);
                            _setDM(point);
                            _setDMS(point);
                            _setAddress(point);

                        }
                        break;
                    case DD:
                        point = _getDDForConvert();
                        if (point != null) {
                            _setMGRS(point);
                            _setDM(point);
                            _setDMS(point);
                            _setAddress(point);
                            syncRawMGRS();

                        }
                        break;
                    case DM:
                        point = _getDMForConvert();
                        if (point != null) {
                            _setMGRS(point);
                            _setDD(point);
                            _setDMS(point);
                            _setAddress(point);
                            syncRawMGRS();

                        }
                        break;
                    case DMS:
                        point = _getDMSForConvert();
                        if (point != null) {
                            _setMGRS(point);
                            _setDM(point);
                            _setDD(point);
                            _setAddress(point);
                            syncRawMGRS();

                        }
                        break;
                    case ADDRESS:
                        point = _getAddressForConvert();
                        if (point != null) {
                            _setMGRS(point);
                            _setDM(point);
                            _setDD(point);
                            _setDMS(point);
                            syncRawMGRS();

                        }

                        break;
                }

                // the coordinate has changed since last run, need to wipe out the 
                // address.   Need to check based on distance because the address point has 
                // a different level of precision then what is stored when doing display 
                // readback for the other conversion screens.   Accuracy of the other conversion
                // screens within a meter.  
                //
                if (point != null && humanAddressPoint != null) {
                    if (humanAddressPoint.distanceTo(point) > 1) {
                        Log.d(TAG,
                                "address lookup point: " + humanAddressPoint
                                        + ", but point has changed: " + point
                                        + ", distance: "
                                        + humanAddressPoint.distanceTo(point)
                                        + " clearing");
                        humanAddressPoint = null;
                        humanAddress = null;
                    }
                }

                if (humanAddress != null && humanAddress.length() > 0) {
                    addressET.setText(humanAddress);
                    _dropAddressChk.setVisibility(View.VISIBLE);
                } else {
                    addressET.setText("");
                    _dropAddressChk.setVisibility(View.INVISIBLE);
                }

                // If the point is not null then the value represent by this view was changed
                if (point != null) {
                    _result = Result.VALID_CHANGED;
                    _currPoint = point;
                }

                _currFormat = CoordinateFormat.find(tabId);
                // If the tab is now MGRS, show the mgrs button
                if (_currFormat == CoordinateFormat.MGRS) {
                    _mgrsButton.setVisibility(View.VISIBLE);

                }
                if (_currFormat != CoordinateFormat.ADDRESS) {
                    _dropAddressChk.setVisibility(View.GONE);
                    _licenseTv.setVisibility(View.GONE);
                } else {
                    _licenseTv.setVisibility(View.VISIBLE);
                    if (_currPoint != null && humanAddressPoint != null) {
                        if (humanAddressPoint.distanceTo(_currPoint) > 1) {
                            Log.d(TAG, "reverse lookup based on currentPoint "
                                    + _currPoint);
                            handleSearchButton();
                        }
                    } else if (_currPoint != null) {
                        Log.d(TAG, "reverse lookup based on currentPoint "
                                + _currPoint);
                        handleSearchButton();
                    } else {
                        Log.d(TAG, "no reverse lookup");
                    }
                }

            }
        });

        _elevText = (EditText) findViewById(R.id.coordDialogElevationText);
        _elevText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                try {
                    String txt = _elevText.getText().toString().trim();
                    if (txt.length() > 0 && Double.valueOf(txt) != altVal)
                        altSource = AltitudeSource.USER;
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }
        });

        _mgrsZone = (EditText) findViewById(R.id.coordDialogMGRSGridText);
        _mgrsSquare = (EditText) findViewById(R.id.coordDialogMGRSSquareText);
        _mgrsEast = (EditText) findViewById(R.id.coordDialogMGRSEastingText);
        _mgrsNorth = (EditText) findViewById(R.id.coordDialogMGRSNorthingText);

        ImageView swap = (ImageView) findViewById(R.id.swap);
        swap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                enableFormattedMGRS(_mgrsRaw.getVisibility() != View.GONE);
            }
        });

        _mgrsRaw = (EditText) findViewById(R.id.rawMGRS);
        _mgrsRaw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                try {
                    // see the sync method
                    if (!watch)
                        return;

                    GeoPoint gp =
                            CoordinateFormatUtilities.convert(s.toString(),
                                    CoordinateFormat.MGRS);
                    if (gp != null)
                        _setPoint(gp);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        });

        _ddLat = (EditText) findViewById(R.id.coordDialogDDLatText);
        _ddLon = (EditText) findViewById(R.id.coordDialogDDLonText);
        addressET = (EditText) findViewById(R.id.addressET);
        _dmsLatD = (EditText) findViewById(R.id.coordDialogDMSLatDegText);
        _dmsLatM = (EditText) findViewById(R.id.coordDialogDMSLatMinText);
        _dmsLatS = (EditText) findViewById(R.id.coordDialogDMSLatSecText);
        _dmsLonD = (EditText) findViewById(R.id.coordDialogDMSLonDegText);
        _dmsLonM = (EditText) findViewById(R.id.coordDialogDMSLonMinText);
        _dmsLonS = (EditText) findViewById(R.id.coordDialogDMSLonSecText);
        _dmLatD = (EditText) findViewById(R.id.coordDialogDMLatDegText);
        _dmLatM = (EditText) findViewById(R.id.coordDialogDMLatMinText);
        _dmLonD = (EditText) findViewById(R.id.coordDialogDMLonDegText);
        _dmLonM = (EditText) findViewById(R.id.coordDialogDMLonMinText);

        _mgrsButton = (Button) findViewById(R.id.coordDialogMGRSGridButton);
        Button _convertButton = (Button) findViewById(R.id.button_convert_address);
        _mgrsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                _mgrsZone.setText(_centerZone);
                _mgrsSquare.setText(_centerSquare);
                syncRawMGRS();
            }
        });

        _mgrsZone.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            public void afterTextChanged(Editable s) {
                if (s.length() == 3) {
                    _mgrsSquare.requestFocus();
                    if (_mgrsSquare.getText().length() > 0)
                        _mgrsSquare.setSelection(0, _mgrsSquare.getText()
                                .length());
                }
            }
        });
        _mgrsSquare.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            public void afterTextChanged(Editable s) {
                if (s.length() == 2) {
                    _mgrsEast.requestFocus();
                    if (_mgrsEast.getText().length() > 0)
                        _mgrsEast.setSelection(0, _mgrsEast.getText().length());
                }
            }
        });

        _mgrsEast.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            public void afterTextChanged(Editable s) {
                if (s.length() == 5) {
                    _mgrsNorth.requestFocus();
                    if (_mgrsNorth.getText().length() > 0)
                        _mgrsNorth.setSelection(0, _mgrsNorth.getText()
                                .length());
                }
            }
        });
        // The Tab listener isn't getting called, so force the MGRS grid button to be invisible
        // here.
        if (_currFormat != CoordinateFormat.MGRS) {
            _mgrsButton.setVisibility(View.INVISIBLE);

        }
        Button _dtedButton = (Button) findViewById(R.id.coordDialogDtedButton);
        _dtedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // XXX - should this set the AltitudeSource?
                _pullFromDTED();
            }
        });
        _dem = Dt2ElevationModel.getInstance();
        _updateElev();
        _updatePoint();

        //TODO this is actually the Find button
        _convertButton.setOnClickListener(findButtonOnClickListener);

        Button clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(_clearButtonOnClickListener);

        enableFormattedMGRS(_prefs.getBoolean("coordview.formattedMGRS", true));

    }

    private void syncRawMGRS() {
        try {
            watch = false;
            _mgrsRaw.setText(
                    _mgrsZone.getText().toString()
                            .toUpperCase(LocaleUtil.getCurrent())
                            +
                            _mgrsSquare.getText().toString()
                                    .toUpperCase(LocaleUtil.getCurrent()) +
                            _mgrsEast.getText().toString() +
                            _mgrsNorth.getText().toString());
            watch = true;
        } catch (Exception e) {
            Log.e(TAG, " cought exception ", e);
        }
    }

    private void enableFormattedMGRS(boolean enable) {
        _prefs.edit().putBoolean("coordview.formattedMGRS", enable).apply();
        if (enable) {
            _mgrsZone.setVisibility(View.VISIBLE);
            _mgrsSquare.setVisibility(View.VISIBLE);
            _mgrsEast.setVisibility(View.VISIBLE);
            _mgrsNorth.setVisibility(View.VISIBLE);
            _mgrsRaw.setVisibility(View.GONE);
        } else {
            syncRawMGRS();
            _mgrsZone.setVisibility(View.GONE);
            _mgrsSquare.setVisibility(View.GONE);
            _mgrsEast.setVisibility(View.GONE);
            _mgrsNorth.setVisibility(View.GONE);
            _mgrsRaw.setVisibility(View.VISIBLE);
        }
    }

    protected void checkADDRtab() {

        isADDRtabDisabled = !isNetworkAvailable();
        //_host.getTabWidget().getChildTabViewAt(4).setEnabled(!isADDRtabDisabled);
        if (isADDRtabDisabled)
            ((TextView) _host.getTabWidget().getChildAt(4)
                    .findViewById(android.R.id.title))
                    .setTextColor(Color.DKGRAY);
        else
            ((TextView) _host.getTabWidget().getChildAt(4)
                    .findViewById(android.R.id.title))
                    .setTextColor(Color.WHITE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mapView
                .getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return !(activeNetworkInfo == null || !activeNetworkInfo.isConnected());
    }

    final OnClickListener _clearButtonOnClickListener = new OnClickListener() {
        public void onClick(final View view) {
            clear();

        }
    };

    final OnClickListener findButtonOnClickListener = new OnClickListener() {
        public void onClick(final View view) {
            if (isNetworkAvailable()) {
                handleSearchButton();
            } else {
                Toast.makeText(
                        mapView.getContext(),
                        "Address Lookup Requires an active Internet Connection",
                        Toast.LENGTH_SHORT).show();

            }
            post(new Runnable() {
                public void run() {
                    hideKeyboard();
                }
            });
        }
    };

    /*
     * Geocoding of the departure or destination address
     */
    public void handleSearchButton() {
        String locationAddress = addressET.getText().toString();
        _dropAddressChk.setVisibility(View.INVISIBLE);

        if (locationAddress.equals("")) {
            if (_currPoint != null) {
                final ProgressDialog pd = ProgressDialog.show(
                        mapView.getContext(),
                        "Locating...",
                        "Looking up address for the current point.", true,
                        false);
                final ReverseGeocodingTask rgt =
                        new ReverseGeocodingTask(_currPoint, geoService,
                                geoServiceKey,
                                mapView.getContext());

                rgt.setOnResultListener(new ReverseGeocodingTask.ResultListener() {
                    public void onResult() {
                        humanAddressPoint = rgt.getPoint();
                        humanAddress = rgt.getHumanAddress();
                        if (humanAddress != null && humanAddress.length() > 0)
                            _setAddressParams(humanAddress, humanAddressPoint);
                        else
                            _setAddressParams("", null);
                        pd.dismiss();
                    }
                });
                rgt.execute();
            } else {
                Toast.makeText(mapView.getContext(),
                        "Cannot search for an empty address",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        final ProgressDialog pd = ProgressDialog.show(mapView.getContext(),
                "Locating...",
                locationAddress, true,
                false);

        MapView view = MapView.getMapView();
        GeoBounds gb = view.getBounds();
        final GeocodingTask gt = new GeocodingTask(geoService, geoServiceKey,
                mapView.getContext(),
                gb.getSouth(), gb.getWest(), gb.getNorth(), gb.getEast());

        gt.setOnResultListener(new GeocodingTask.ResultListener() {
            public void onResult() {
                final GeoPoint result = gt.getPoint();
                final String address = gt.getHumanAddress();
                if (address != null && address.length() > 0) {
                    _setPoint(result);
                    _setAddressParams(address, result);
                } else {
                    humanAddress = "";
                    humanAddressPoint = null;
                }
                pd.dismiss();

            }
        });
        gt.execute(locationAddress);
    }

    private void _setAddressParams(final String address, final GeoPoint point) {
        humanAddress = address;
        humanAddressPoint = point;
        addressET.post(new Runnable() {
            public void run() {
                addressET.setText(address);
                if (point != null)
                    _dropAddressChk.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = (View) this.getFocusedChild();
        if (view != null) {
            view.clearFocus();
            InputMethodManager inputManager = (InputMethodManager) mapView
                    .getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void _updatePoint() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            post(new Runnable() {
                public void run() {
                    _updatePoint();
                }
            });
            return;
        }
        if (_currPoint != null && _dmLonM != null) {
            _setMGRS(_currPoint);
            _setDD(_currPoint);
            _setDMS(_currPoint);
            _setDM(_currPoint);
            _setAddress(_currPoint);
            _result = Result.VALID_UNCHANGED;
        }
    }

    public void clear() {
        _currPoint = null;
        humanAddressPoint = null;
        humanAddress = null;
        _setMGRS(_currPoint);
        _setDD(_currPoint);
        _setDMS(_currPoint);
        _setDM(_currPoint);
        _setAddress(_currPoint);
        _elevText.setText("");
        addressET.setText("");

    }

    private void _setElev() {
        if (_currPoint != null && _currPoint.getAltitude().isValid()) {
            double e = SpanUtilities.convert(
                    EGM96.getInstance().getMSL(_currPoint).getValue(),
                    Span.METER, Span.FOOT);
            _currElevMSL = _formatElevation(e);
            _updateElev();
        }
    }

    private void _updateElev() {
        if (!_currElevMSL.equals("" + Altitude.UNKNOWN.getValue())) {
            _elevText.setText(_currElevMSL);
        }
    }

    private static String _formatElevation(double e) {
        if (Math.abs(e) < 100) {
            return TWO_DEC_FORMAT.format(e);
        } else if (Math.abs(e) < 1000) {
            return ONE_DEC_FORMAT.format(e);
        } else {
            return NO_DEC_FORMAT.format(e);
        }
    }

    public boolean isAddressPointChecked() {
        return _dropAddressChk != null && _dropAddressChk.getVisibility()
                    == CheckBox.VISIBLE && _dropAddressChk.isChecked();
    }

    public String getHumanAddress() {
        return humanAddress;
    }

    public enum Result {
        VALID_CHANGED, VALID_UNCHANGED, INVALID
    }

}
