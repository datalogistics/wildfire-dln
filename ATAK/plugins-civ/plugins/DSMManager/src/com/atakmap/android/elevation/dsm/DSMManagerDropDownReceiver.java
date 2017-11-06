
package com.atakmap.android.elevation.dsm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.atakmap.coremap.log.Log;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.elevation.dsm.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.layers.LayerSelection;
import com.atakmap.android.layers.LayerSelectionAdapter;
import com.atakmap.android.layers.OutlinesFeatureDataStore;
import com.atakmap.android.maps.MapCoreIntentsComponent;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher.MapEventDispatchListener;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MapView.RenderStack;
import com.atakmap.android.util.AltitudeUtilities;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.FeatureLayer;
import com.atakmap.map.layer.raster.AbstractDataStoreRasterLayer2;
import com.atakmap.map.layer.raster.RasterLayer2;
import com.atakmap.math.MathUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;

import com.atakmap.coremap.locale.LocaleUtil;

import java.util.Set;

public class DSMManagerDropDownReceiver extends DropDownReceiver implements OnStateListener
{
    public static String TAG = "DSMManagerDropDownReceiver";

    public static final String SHOW_WX_REPORT = "com.atakmap.android.elevation.dsm.SHOW_DSM_MANAGER";
    private View wxView;
    private MapView mapView;
    private Context pluginContext;
    private ListView wxOverlaysList;
    private RasterLayer2 wxRasterLayer;
    private Layer outlinesLayer;
    
    public DSMManagerDropDownReceiver(final MapView mapView, final Context context, final RasterLayer2 rasterlayer) {
        super(mapView);
        this.pluginContext = context;
        this.mapView = mapView;
        this.wxRasterLayer = rasterlayer;
        
        this.outlinesLayer = new FeatureLayer("DSM Outlines", new OutlinesFeatureDataStore(rasterlayer, -1, true));
        this.outlinesLayer.setVisible(true);
        ((FeatureLayer)this.outlinesLayer).getDataStore().setFeaturesVisible(null, true);

        // Use the inflater service to get the UI ready to show
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);

        // Inflate or load the UI
        wxView = inflater.inflate(R.layout.dsm_manager_layout, null);
    }

    public void disposeImpl() {}

    public Boolean checkService() {
        return false;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        // If we receive an intent to show the weather GUI, then start set up
        if (intent.getAction().equals(SHOW_WX_REPORT)) {
            // Begin building the view starting with the tabhost


            // Show the weather GUI with initial size options and which callbacks to use
            // The 'this' pointer will call onDropDownClose if GUI is closed afterwards
            showDropDown(wxView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, this);

            mapView.addLayer(RenderStack.MAP_SURFACE_OVERLAYS, outlinesLayer);

            wxOverlaysList = (ListView) wxView
                    .findViewById(R.id.listView);
            wxOverlaysList.setOnTouchListener(new View.OnTouchListener() {
                // Setting on Touch Listener for handling the touch inside ScrollView
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Disallow the touch request for parent scroll on touch of child view
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                }
            });

            LayerSelectionAdapter adapter = new LayerSelectionAdapter(
                    wxRasterLayer, null,
                    mapView, mapView.getContext()) {

                @Override
                protected View getViewImpl(final LayerSelection sel,
                        final int position, View convertView, ViewGroup parent) {

                    final ElevationInfo info = DSMManager.getDb().getElevationInfo(sel.getName());

                    // First, inflate the overlay list item layout to get the main view
                    LayoutInflater inflater = LayoutInflater
                            .from(pluginContext);
                    View view = inflater.inflate(
                            R.layout.dsm_manager_list_item, null);

                    // Set the name of each wx report overlay
                    TextView title = (TextView) view
                            .findViewById(R.id.dsm_item_title);
                    title.setText(new File(sel.getName()).getName());
                    
                    TextView desc= (TextView)view.findViewById(R.id.dsm_item_desc);
                    StringBuilder descStr = new StringBuilder();
                    if(info != null) {
                        switch(info.model) {
                            case ElevationData.MODEL_SURFACE :
                                descStr.append("Surface ");
                                break;
                            case ElevationData.MODEL_TERRAIN :
                                descStr.append("Terrain ");
                                break;
                            case (ElevationData.MODEL_SURFACE|ElevationData.MODEL_TERRAIN) :
                                descStr.append("Terrain+Surface ");
                                break;
                            default :
                                throw new IllegalArgumentException();
                        }
                        
                        descStr.append(info.units.getAbbrev());
                        descStr.append(" ");
                        descStr.append(info.reference.getAbbreviation());
                    }
                    desc.setText(descStr);

                    // Get the visibility toggle and attach a listener
                    ImageView panToButton = (ImageView) view
                            .findViewById(R.id.dsm_pan_to);
                    panToButton.setOnClickListener(new View.OnClickListener() {
                        // When toggle is selected, set visibility to opposite of current
                        public void onClick(View v) {
                            Intent panto = new Intent(MapCoreIntentsComponent.ACTION_PAN_ZOOM);
                            panto.putExtra("shape", 
                                    new String[]
                                            {
                                                (new GeoPoint(sel.getNorth(), sel.getWest())).toString(),
                                                (new GeoPoint(sel.getSouth(), sel.getEast())).toString(),
                                            }
                            );
                            AtakBroadcast.getInstance().sendBroadcast(panto);
                        }
                    });
                    
                    // Get the visibility toggle and attach a listener
                    ImageView editButton = (ImageView) view
                            .findViewById(R.id.dsm_edit);
                    editButton.setOnClickListener(new View.OnClickListener() {
                        // When toggle is selected, set visibility to opposite of current
                        public void onClick(View v) {
                            if(info == null) {
                                Toast.makeText(mapView.getContext(), "Item cannot be edited.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            AlertDialog.Builder builderVal=new AlertDialog.Builder(mapView.getContext());
                            builderVal.setTitle("Edit " + (new File(sel.getName())).getName()) ;
                            View holder=View.inflate(pluginContext, R.layout.dsm_item_editor, null);
                            builderVal.setView(holder);

                            final Spinner modelSpinner = (Spinner)holder.findViewById(R.id.dsm_edit_model_spinner);
                            setSpinnerOptions(mapView.getContext(), modelSpinner, new String[] {"Surface", "Terrain", "Terrain+Surface"});
                            switch(info.model) {
                                case ElevationData.MODEL_SURFACE :
                                    modelSpinner.setSelection(0);
                                    break;
                                case ElevationData.MODEL_TERRAIN :
                                    modelSpinner.setSelection(1);
                                    break;
                                case (ElevationData.MODEL_SURFACE|ElevationData.MODEL_TERRAIN) :
                                    modelSpinner.setSelection(2);
                                    break;
                                default :
                                    throw new IllegalArgumentException();
                            }
                            
                            final Spinner unitSpinner = (Spinner)holder.findViewById(R.id.dsm_edit_units_spinner);
                            setSpinnerOptions(mapView.getContext(), unitSpinner, new String[] {Span.METER.getPlural(), Span.FOOT.getPlural()});
                            switch(info.units) {
                                case METER :
                                    unitSpinner.setSelection(0);
                                    break;
                                case FOOT :
                                    unitSpinner.setSelection(1);
                                    break;
                                default :
                                    throw new IllegalArgumentException();
                            }
                            
                            final Spinner referenceSpinner = (Spinner)holder.findViewById(R.id.dsm_edit_reference_spinner);
                            setSpinnerOptions(mapView.getContext(), referenceSpinner, new String[] {AltitudeReference.HAE.getAbbreviation(), AltitudeReference.MSL.getAbbreviation()});
                            switch(info.reference) {
                                case HAE :
                                    referenceSpinner.setSelection(0);
                                    break;
                                case MSL :
                                    referenceSpinner.setSelection(1);
                                    break;
                                default :
                                    throw new IllegalArgumentException();
                            }
                            
                            builderVal.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // XXX - 
                                    int editModel = info.model;
                                    if(modelSpinner.getSelectedItem().equals("Surface"))
                                        editModel = ElevationData.MODEL_SURFACE;
                                    else if(modelSpinner.getSelectedItem().equals("Terrain"))
                                        editModel = ElevationData.MODEL_TERRAIN;
                                    else if(modelSpinner.getSelectedItem().equals("Terrain+Surface"))
                                        editModel = ElevationData.MODEL_TERRAIN|ElevationData.MODEL_SURFACE;
                                    
                                    Span editUnits = Span.findFromPluralName((String)unitSpinner.getSelectedItem());
                                    AltitudeReference editReference = AltitudeReference.findFromAbbreviation((String)referenceSpinner.getSelectedItem());
                                    
                                    DSMManager.getDb().update(sel.getName(),
                                                              editModel,
                                                              editReference,
                                                              editUnits);
                                    
                                    notifyDataSetChanged();
                                }
                            });
                            builderVal.show();


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
    
    private static void setSpinnerOptions(Context context, Spinner spinner, String[] opts) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, opts); 
        spinner.setAdapter(adapter);
    }

    public void onDropDownSelectionRemoved() {}

    @Override
    public void onDropDownVisible(boolean v) {}

    @Override
    public void onDropDownSizeChanged(double width, double height) {}

    @Override
    public void onDropDownClose() {
        mapView.removeLayer(RenderStack.MAP_SURFACE_OVERLAYS, outlinesLayer);
    }
}
