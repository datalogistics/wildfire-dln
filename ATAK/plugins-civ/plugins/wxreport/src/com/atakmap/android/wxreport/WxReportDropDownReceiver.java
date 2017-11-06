
package com.atakmap.android.wxreport;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.HintDialogHelper;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.layers.LayerSelection;
import com.atakmap.android.layers.LayerSelectionAdapter;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher.MapEventDispatchListener;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.AltitudeUtilities;
import com.atakmap.android.wxreport.data.Channel;
import com.atakmap.android.wxreport.data.Condition;
import com.atakmap.android.wxreport.data.Forecast;
import com.atakmap.android.wxreport.data.Item;
import com.atakmap.android.wxreport.plugin.R;
import com.atakmap.android.wxreport.service.WeatherServiceCallback;
import com.atakmap.android.wxreport.service.YahooWeatherService;
import com.atakmap.android.wxreport.util.WeatherUtils;
import com.atakmap.android.wxreport.views.ForecastView;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.raster.RasterLayer2;
import com.atakmap.math.MathUtils;

import java.util.Comparator;

public class WxReportDropDownReceiver extends DropDownReceiver implements
        OnStateListener, WeatherServiceCallback
{
    public static String TAG = WxReportDropDownReceiver.class.getSimpleName();

    public static final String SHOW_WX_REPORT = "com.atakmap.android.wxreport.SHOW_WX_REPORT";

    private View wxView;
    private View conditionsView;
    private View overlaysView;
    private MapView mapView;
    private Context pluginContext;
    private Button coordButton;
    private CoordinateFormat _cFormat;
    private SharedPreferences _prefs;
    private GeoPoint clickPoint;
    private YahooWeatherService service;
    private RasterLayer2 wxRasterLayer;
    private boolean mapControls = true;
    private LinearLayout forecastLinearLayout;
    private TextView forecastLocationTextView;
    private TextView lastUpdateTextView;
    private TextView noWeatherDataTextView;
    private TextView humidityTextView;
    private TextView pressureTextView;
    private TextView visibilityTextView;
    private TextView windsTextView;
    private TextView realTemperatureTextView;
    private RelativeLayout weatherRelativeLayout;
    /**
     * If a request has been made we don't want 
     * to send another one until the first is complete
     * we are using this like a semaphore
     */
    private boolean serviceRequested;
    private TextView nowTemperatureTextView;
    private TextView nowConditionTextView;
    private ImageView nowConditionImageView;
    private LinearLayout loadingLinearLayout;

    public WxReportDropDownReceiver(final MapView mapView, final Context context, final RasterLayer2 rasterlayer)
    {
        super(mapView);
        this.pluginContext = context;
        this.mapView = mapView;
        this.wxRasterLayer = rasterlayer;

        // Use the inflater service to get the UI ready to show
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Inflate or load the UI
        wxView = inflater.inflate(R.layout.wx_report_tab_layout, null);
        conditionsView = inflater.inflate(R.layout.wx_report_conditions_layout, null);
        overlaysView = inflater.inflate(R.layout.wx_report_overlays_layout, null);

        // Get a hold of the preferences and register an interest in changes
        _prefs = PreferenceManager.getDefaultSharedPreferences(mapView.getContext());
        SharedPreferences.OnSharedPreferenceChangeListener _sharedPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(final SharedPreferences sp,
                                                  final String key) {
                if (key.equals("coord_display_pref")) {
                    _cFormat = CoordinateFormat.find(sp.getString(key, mapView.getContext().getString(com.atakmap.app.R.string.coord_display_pref_default)));
                } else if (key.equals("weather_temp_key") || key.equals("speed_unit_pref")) {
                    if (!serviceRequested) {
                        serviceRequested = true;
                        service.refreshWeather();
                    }
                }
            }
        };
        _prefs.registerOnSharedPreferenceChangeListener(_sharedPrefsListener);
    }

    public void disposeImpl()
    {
    }

    @Override
    public void onReceive(final Context context, Intent intent)
    {

        // If we receive an intent to show the weather GUI, then start set up
        if (intent.getAction().equals(SHOW_WX_REPORT))
        {
            HintDialogHelper
            .showHint(
                    mapView.getContext(),
                    "Weather Tool",
                    "The Weather Tool visualizes radar data \n\n"
                    + "Adjust visibility and opacity in flyout window \n\n"
                    + " Place additional source data in atak/tools/wx",
                    "weather.conditions");

            forecastLinearLayout = (LinearLayout) conditionsView.findViewById(R.id.forecastLinearLayout);
            forecastLocationTextView = (TextView) conditionsView.findViewById(R.id.forecastLocationTextView);

            loadingLinearLayout = (LinearLayout) conditionsView.findViewById(R.id.loadingLinearLayout);
            lastUpdateTextView = (TextView) conditionsView.findViewById(R.id.lastUpdateTextView);
            nowTemperatureTextView = (TextView) conditionsView.findViewById(R.id.nowTemperatureTextView);
            nowConditionImageView = (ImageView) conditionsView.findViewById(R.id.nowConditionImageView);
            nowConditionTextView = (TextView) conditionsView.findViewById(R.id.nowConditionTextView);
            noWeatherDataTextView = (TextView) conditionsView.findViewById(R.id.noConnectionTextView);
            humidityTextView = (TextView) conditionsView.findViewById(R.id.humidityTextView);
            pressureTextView = (TextView) conditionsView.findViewById(R.id.pressureTextView);
            visibilityTextView = (TextView) conditionsView.findViewById(R.id.visibilityTextView);
            windsTextView = (TextView) conditionsView.findViewById(R.id.windsTextView);
            realTemperatureTextView = (TextView) conditionsView.findViewById(R.id.realTemperatureTextView);
            weatherRelativeLayout = (RelativeLayout)conditionsView.findViewById(R.id.weatherRelativeLayout);

            // Create new weather service to query against
            if(service == null)
                service = new YahooWeatherService(this);

            // Initialize weather service and map click with last recorded location
            String lastLocation = _prefs.getString("lastRecordedLocation", null);
            if (lastLocation != null) { 
                String[] lastRecordedLocation = lastLocation.split("\\s*,\\s*");
                service.refreshWeather(lastRecordedLocation[0], lastRecordedLocation[1]);
                clickPoint = new GeoPoint(Float.parseFloat(lastRecordedLocation[0]), Float.parseFloat(lastRecordedLocation[1]));
            }

            // Begin building the view starting with the tabhost
            TabHost tabHost = (TabHost) wxView.findViewById(R.id.wxreport_tabhost);
            tabHost.setup();
            tabHost.clearAllTabs();

            TabHost.TabSpec conditionsTab = tabHost.newTabSpec("Conditions");
            conditionsTab.setIndicator("Conditions");
            conditionsTab.setContent(new TabHost.TabContentFactory()
            {
                @Override
                public View createTabContent(String tag)
                {
                    return conditionsView;
                }
            });
            tabHost.addTab(conditionsTab);

            TabHost.TabSpec overlaysTab = tabHost.newTabSpec("Overlays");
            overlaysTab.setIndicator("Overlays");
            overlaysTab.setContent(new TabHost.TabContentFactory()
            {
                @Override
                public View createTabContent(String tag)
                {
                    return overlaysView;
                }
            });
            tabHost.addTab(overlaysTab);

            tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener()
            {
                public void onTabChanged(String tabId)
                {
                    // Create an intent to clear off map radial menus
                    Intent clearMapIntent = new Intent();
                    clearMapIntent.setAction("com.atakmap.android.maps.HIDE_MENU");

                    if (tabId.equals("Overlays"))
                    {
                        // Send clear map intent if any radial menus are present
                        AtakBroadcast.getInstance().sendBroadcast(clearMapIntent);

                        // There are two places that push and pop lock and unlock
                        // Need to manage it with a boolean
                        if (!mapControls)
                        {
                            unlockMapControls();
                        }
                    }
                    else
                    {
                        // Send clear map intent if any radial menus are present
                        AtakBroadcast.getInstance().sendBroadcast(clearMapIntent);

                        // There are two places that push and pop lock and unlock
                        // Need to manage it with a boolean
                        if (mapControls)
                        {
                            lockMapControls();
                        }
                    }
                }
            });

            // Show the weather GUI with initial size options and which callbacks to use
            // The 'this' pointer will call onDropDownClose if GUI is closed afterwards
            showDropDown(wxView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, this);

            // There are two places that push and pop lock and unlock
            // Need to manage it with a boolean
            if (mapControls)
            {
                lockMapControls();
            }

            coordButton = (Button) conditionsView.findViewById(R.id.wxReportCoordButton);
            coordButton.setEnabled(true);
            _cFormat = CoordinateFormat.find(_prefs.getString("coord_display_pref", context.getString(com.atakmap.app.R.string   .coord_display_pref_default)));
            coordButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    AlertDialog.Builder b = new AlertDialog.Builder(mapView.getContext());
                    LayoutInflater inflater = LayoutInflater.from(pluginContext);
                    final WxReportCoordDialogView coordView = (WxReportCoordDialogView) inflater.inflate(R.layout.wx_report_coord_dialog, null);
                    b.setTitle("Enter Coordinate: ");
                    b.setView(coordView);
                    b.setPositiveButton("OK",
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which)
                                {
                                    // On click get the geopoint and elevation double in ft
                                    GeoPoint p = coordView.getPoint();
                                    boolean changedFormat = coordView
                                            .getCoordFormat() != _cFormat;
                                    if (coordView.getCoordFormat() != CoordinateFormat.ADDRESS)
                                    {
                                        _cFormat = coordView.getCoordFormat();
                                    }

                                    WxReportCoordDialogView.Result result = coordView
                                            .getResult();
                                    if (result == WxReportCoordDialogView.Result.VALID_UNCHANGED
                                            && changedFormat)
                                    {
                                        // The coordinate format was changed but not the point itself
                                        updateCoordButton();
                                    }
                                    if (result == WxReportCoordDialogView.Result.VALID_CHANGED)
                                    {
                                        if (coordView.isAddressPointChecked())
                                        {
                                            String addr = coordView
                                                    .getHumanAddress();
                                            if (addr != null
                                                    && addr.length() > 0)
                                            {
                                                //    _marker.setMetaString("callsign", addr);
                                                //    if(_marker instanceof Marker)
                                                //    {
                                                //        ((Marker) _marker).setTitle(addr);
                                                //    }
                                                //    _nameEdit.setText(addr);
                                            }
                                        }
                                        //_marker.setPoint(p);
                                        clickPoint = p;

                                            if(!serviceRequested){
                                                serviceRequested = true;
                                                service.refreshWeather(String
                                                        .valueOf(clickPoint
                                                                .getLatitude()),
                                                        String.valueOf(clickPoint
                                                                .getLongitude()));
                                            }
                                        //_marker.persist(mapView.getMapEventDispatcher(), null, this.getClass());
                                        mapView.getMapController().panTo(p,
                                                false);
                                    }
                                }
                            });
                    b.setNegativeButton("Cancel", null);
                    coordView.setParameters(clickPoint, mapView.getPoint());
                    b.show();
                }
            });

            ImageButton panButton = (ImageButton) conditionsView
                    .findViewById(R.id.wxReportInfoPanButton);
            panButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (clickPoint != null)
                    {
                        mapView.getMapController().panTo(clickPoint, true);
                    }
                }
            });

            ListView wxOverlaysList = (ListView) overlaysView
                    .findViewById(R.id.listView);
            wxOverlaysList.setOnTouchListener(new View.OnTouchListener()
            {
                // Setting on Touch Listener for handling the touch inside ScrollView
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    // Disallow the touch request for parent scroll on touch of child view
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                }
            });

            LayerSelectionAdapter adapter = new LayerSelectionAdapter(
                    wxRasterLayer, null,
                    mapView, mapView.getContext())
            {
                SeekBar transparencySeek;

                @Override
                protected View getViewImpl(final LayerSelection sel,
                        final int position, View convertView, ViewGroup parent)
                {
                    // First, inflate the overlay list item layout to get the main view
                    LayoutInflater inflater = LayoutInflater
                            .from(pluginContext);
                    View view = inflater.inflate(
                            R.layout.wx_report_overlay_list_item, null);

                    // Set the name of each wx report overlay
                    TextView title = (TextView) view
                            .findViewById(R.id.wx_report_overlay_item_title);
                    title.setText(sel.getName());

                    // Get the visibility toggle and attach a listener
                    ImageView visibilityToggle = (ImageView) view
                            .findViewById(R.id.wx_report_overlay_item_toggle_image);
                    visibilityToggle
                            .setOnClickListener(new View.OnClickListener()
                            {
                                // When toggle is selected, set visibility to opposite of current
                                public void onClick(View v)
                                {
                                    boolean val = wxRasterLayer.isVisible(sel
                                            .getName());
                                    wxRasterLayer.setVisible(sel.getName(),
                                            !val);
                                    notifyDataSetChanged();
                                }
                            });

                    // Check visibility status and change image on toggle appropriately
                    final boolean isVisible = wxRasterLayer.isVisible(sel
                            .getName());
                    final int visibilityIcon = isVisible ? R.drawable.overlay_visible
                            : R.drawable.overlay_not_visible;
                    visibilityToggle.setImageResource(visibilityIcon);

                    // Establish the transparency slider
                    transparencySeek = (SeekBar) view
                            .findViewById(R.id.wx_report_overlay_item_transparency);
                    float alpha = wxRasterLayer.getTransparency(sel.getName());
                    transparencySeek.setProgress((int) MathUtils.clamp(
                            transparencySeek.getMax() * alpha, 0,
                            transparencySeek.getMax()));
                    transparencySeek
                            .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
                            {
                                @Override
                                public void onProgressChanged(SeekBar seekBar,
                                        int progress, boolean fromUser)
                                {
                                    final float value = (float) progress
                                            / (float) seekBar.getMax();
                                    wxRasterLayer.setTransparency(
                                            sel.getName(), value);
                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar)
                                {
                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar)
                                {
                                }
                            });

                    // Finally, the view is done, ready to show
                    return view;
                }

                @Override
                protected Comparator getSortComparator()
                {
                    return new Comparator<LayerSelection>()
                    {
                        @Override
                        public int compare(LayerSelection ls1,
                                LayerSelection ls2)
                        {
                            return ls1.getName().compareToIgnoreCase(
                                    ls2.getName());
                        }
                    };
                }
            };
            wxOverlaysList.setAdapter(adapter);
        }
    }

    private void refreshNow(Channel channel){
        Condition c = channel.getItem().getCondition();
        if(c != null){
            nowConditionTextView.setText(c.getDescription());
            nowTemperatureTextView.setText(WeatherUtils.convertTempToCurrentFormat(getMapView().getContext(),c.getTemperature()));
            nowConditionImageView.setImageDrawable(WeatherUtils.findConditionImageByCode(pluginContext,c.getCode()));
            realTemperatureTextView.setText("Real Feel: " + WeatherUtils.convertTempToCurrentFormat(getMapView().getContext(),channel.getWind().getChill()));
            humidityTextView.setText("Humidity: " + channel.getAtmosphere().getHumidity() + "%");
            pressureTextView.setText("Pressure: " + channel.getAtmosphere().getPressure() + " in");
            visibilityTextView.setText("Visibility: " + channel.getAtmosphere().getVisibility() + " miles");
            windsTextView.setText("Wind: " + WeatherUtils.convertSpeedToCurrentFormat(channel.getWind().getSpeed())
                    + " " + WeatherUtils.convertBearingIntoCardinal(channel.getWind()));
        }
    }

    private void refreshForecast(Channel channel) {

        //clear views already inside
        forecastLinearLayout.removeAllViewsInLayout();
        Item item = channel.getItem();
        if(item != null){
            forecastLocationTextView.setText(channel.getLocation().getCity().getCity() + ", "
                    + channel.getLocation().getRegion().getRegion());
            for(Forecast f : item.getForecasst())
            {
                ForecastView fView = new ForecastView(mapView.getContext(),pluginContext,f);
                forecastLinearLayout.addView(fView);
            }
        }

        lastUpdateTextView.setText("Weather Data Last Updated On: " + channel.getItem().getPubDate());
    }

    final MapEventDispatchListener mapClickListener = new MapEventDispatchListener()
    {
        // This listener is for map events like clicks, long presses, etc
        @Override
        public void onMapEvent(MapEvent event)
        {
            // Map event has occurred, check against what we're interested in
            if (event.getType().equals(MapEvent.MAP_CLICK))
            {
                // User has clicked on the map, get the clicked point
                //clickPoint = getMapView().inverse(event.getPoint().x, event.getPoint().y);
                clickPoint = getMapView().inverseWithElevation(
                        event.getPoint().x, event.getPoint().y);

                if(!serviceRequested){
                    serviceRequested = true;
                    service.refreshWeather(
                            String.valueOf(clickPoint.getLatitude()),
                            String.valueOf(clickPoint.getLongitude()));
                }
            }
        }
    };

    public void onDropDownSelectionRemoved()
    {
    }

    @Override
    public void onDropDownVisible(boolean v)
    {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height)
    {
    }

    @Override
    public void onDropDownClose()
    {
        if (!mapControls)
        {
            unlockMapControls();
        }
    }

    @Override
    public void serviceSuccess(Channel channel)
    {
        if(channel == null){
            Log.d(TAG, "Service Success But No Data...");
            ShowNoWeatherData();
            return;
        }

        Log.d(TAG, "Service Success");

        //we have data so lets pass it in and parse everything out and display it
        refreshWeatherData(channel);
        updateCoordButton();
        serviceRequested = false;
    }

    @Override
    public void startingService() {
        weatherRelativeLayout.setVisibility(View.GONE);
        loadingLinearLayout.setVisibility(View.VISIBLE);
        noWeatherDataTextView.setVisibility(View.GONE);
    }

    @Override
    public void endingService() {
        weatherRelativeLayout.setVisibility(View.VISIBLE);
        loadingLinearLayout.setVisibility(View.GONE);
    }

    private void ShowNoWeatherData() {
        loadingLinearLayout.setVisibility(View.GONE);
        weatherRelativeLayout.setVisibility(View.GONE);
        noWeatherDataTextView.setVisibility(View.VISIBLE);
    }

    private void refreshWeatherData(Channel channel) {
        refreshNow(channel);
        refreshForecast(channel);
        weatherRelativeLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void serviceFailure(Exception exception)
    {
        Log.d(TAG, "Service Failed");
        // Blank out text views to initialize
       // temperatureTextView.setText("Service Failed, Please Try Again");
        serviceRequested = false;
        ShowNoWeatherData();
    }

    public void updateCoordButton()
    {
        final String p = CoordinateFormatUtilities.formatToString(clickPoint,
                _cFormat);
        final String a = AltitudeUtilities.format(clickPoint, _prefs);
        coordButton.setText(p + "\n" + a);
    }

    private void lockMapControls()
    {
        Log.d(TAG, "locking map controls");
        // Lock down the touch controller for maps so we can capture clicks correctly
        MapTouchController touchCtrl = mapView.getMapTouchController();
        touchCtrl.lockControls();

        // Pushing map event listeners will allow our listener to get clicks first
        mapView.getMapEventDispatcher().pushListeners();

        // We can clear out other listeners to make sure we get clicks first
        mapView.getMapEventDispatcher().clearUserInteractionListeners(false);

        // Add a listener for map clicks
        mapView.getMapEventDispatcher().addMapEventListener(MapEvent.MAP_CLICK,
                mapClickListener);
        mapControls = false;
    }

    private void unlockMapControls()
    {
        Log.d(TAG, "Unlocking map controls");
        // Pop listeners to reactivate older ones
        mapView.getMapEventDispatcher().popListeners();

        // Want other map events like clicks and long presses to be active, so unlock touch controller
        MapTouchController touchController = mapView.getMapTouchController();
        touchController.unlockControls();
        mapControls = true;
    }
}
