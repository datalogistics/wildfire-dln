
package com.gmeci.atsk.map;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.gui.ColorPalette;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.ATSKFragment;
import com.gmeci.atsk.ATSKFragmentManager;
import com.gmeci.atsk.ATSKMapComponent;
import com.gmeci.atsk.gallery.ExifHelper;
import com.gmeci.atsk.gradient.GradientTabHost;
import com.gmeci.atsk.obstructions.obstruction.ObstructionTabHost;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.atsk.vehicle.VehicleTabHost;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.MapHelper;
import com.gmeci.hardwareinterfaces.GPSCallbackInterface;
import com.gmeci.hardwareinterfaces.HardwareConsumerInterface;

import java.io.File;

/**
 * Created by smetana on 5/28/2014.
 * This class will handle GPS Position, DTED Service Connetions and Map Clicks.
 * Figured it was good to get these "map processes" out of ATSKTestFragment.
 */
public class ATSKMapManager {

    private static final String TAG = "ATSKMapManager";

    // Radial intents
    public static final String SHOW_LABEL = "ShowMarkerLabel";
    public static final String ROTATE_LABEL = "RotateLabel";
    public static final String COPY_OBS = "CopyObstruction";
    public static final String RENAME_OBS = "RenameObstruction";
    public static final String DELETE_OBS = "DeleteObstruction";
    public static final String SET_COLOR = "SetColor";

    // Only applicable to gallery markers
    public static final String DELETE_IMG = "DeleteMarkerImage";

    //GPS prefs
    HardwareConsumerInterface hardwareConsumerInterface;
    private MapView _mapView;
    private MapGroup _mapGroup;
    private final GPSCallbackInterface gpsci = new GPSCallbackInterface.Stub() {

        @Override
        public void UpdateGPS(SurveyPoint gps_position, int arg1)
                throws RemoteException {
            if (_mapView == null) {
                Log.w(TAG, "MapView is NULL - NO GPS Update");
                return;
            }
            if (gps_position == null) {
                Log.w(TAG, "gps_position is NULL - NO GPS Update");
                return;
            }
            if (_mapView.getRootGroup() == null) {
                Log.w(TAG, "getRootGroup is NULL - NO GPS Update");
                return;
            }

            Marker item = _mapView.getSelfMarker();
            if (item != null && item.hasMetaValue(ATSKATAKConstants.GPS_GMECI)) {

                final Bundle data = _mapView.getMapData();

                GeoPoint gp = MapHelper
                        .convertSurveyPoint2GeoPoint(gps_position);

                if (gp.getLatitude() == 0.0 && gp.getLongitude() == 0.0)
                    return;

                data.putDouble("mockLocationSpeed", gps_position.speed);
                data.putFloat("mockLocationAccuracy",
                        (float) gps_position.circularError);

                data.putString("locationSourcePrefix", "mock");
                data.putBoolean("mockLocationAvailable", true);

                final String loc;
                if (gps_position.collectionMethod == SurveyPoint.CollectionMethod.RTK) {
                    loc = "ATSK GPS (RTK)";
                } else if (gps_position.collectionMethod == SurveyPoint.CollectionMethod.EXTERNAL_GPS) {
                    loc = "ATSK GPS (EXTERNAL)";
                } else if (gps_position.collectionMethod == SurveyPoint.CollectionMethod.INTERNAL_GPS) {
                    loc = "ATSK GPS (INTERNAL)";
                } else {
                    loc = "ATSK NOGPS (MANUAL)";
                }

                data.putString("mockLocationSource", loc);
                data.putBoolean("mockLocationCallsignValid", true);

                data.putParcelable("mockLocation", gp);

                data.putLong("mockLocationTime", SystemClock.elapsedRealtime());

                data.putLong("mockGPSTime", gps_position.timestamp);

                data.putInt("mockFixQuality", arg1);

                Intent gpsReceived = new Intent();

                gpsReceived
                        .setAction("com.atakmap.android.map.WR_GPS_RECEIVED");
                AtakBroadcast.getInstance().sendBroadcast(gpsReceived);

                Log.d(TAG,
                        "received gps for: " + gps_position
                                + " with a fix quality: " + arg1 +
                                " setting last seen time: "
                                + data.getLong("mockLocationTime"));

            }

        }
    };

    public void Start(MapView mapview) {
        _mapView = mapview;

        //setup drawing tool
        //MIKE - perhaps we should do a different group name....
        _mapGroup = _mapView.getRootGroup().addGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_TEST);

        //GPS control
        GetGPSControl();
    }

    public void Stop() {
        ReturnGPSControl();
    }

    private void GetGPSControl() {
        Log.d(TAG, "ATSK is taking control of the GPS delivery");
        Marker item = _mapView.getSelfMarker();
        if (item != null)
            item.setMetaBoolean(ATSKATAKConstants.GPS_GMECI, true);

    }

    private void ReturnGPSControl() {
        Marker item = _mapView.getSelfMarker();
        if (item != null && item.hasMetaValue(ATSKATAKConstants.GPS_GMECI)) {
            item.removeMetaData(ATSKATAKConstants.GPS_GMECI);
        }

    }

    public void SetHardwareInterface(HardwareConsumerInterface hardware) {
        hardwareConsumerInterface = hardware;
        try {
            hardwareConsumerInterface.register(TAG, gpsci);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void SetSurveyInterface() {
    }

    public void clearItems() {
        _mapGroup.getParentGroup().removeGroup(_mapGroup);//maybe its this easy?
        Log.d(TAG, "tried to remove all items");
    }

    public static MapItem find(String uid) {
        // First scan obstructions
        MapView map = MapView.getMapView();
        MapGroup obsGroup = map.getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        MapItem mi;
        if (obsGroup != null) {
            mi = obsGroup.deepFindUID(uid);
            if (mi != null && mi instanceof ATSKMapItem)
                return mi;
        }

        // Then scan surveys
        MapGroup azGroup = map.getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        if (azGroup != null) {
            mi = azGroup.deepFindUID(uid);
            if (mi != null && mi instanceof ATSKMapItem)
                return mi;
        }

        return null;
    }

    public static void registerReceivers() {
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(SHOW_LABEL);
        filter.addAction(COPY_OBS);
        filter.addAction(RENAME_OBS);
        filter.addAction(ROTATE_LABEL);
        filter.addAction(DELETE_IMG);
        filter.addAction(DELETE_OBS);
        filter.addAction(SET_COLOR);
        filter.addAction(ATSKConstants.PT_OBSTRUCTION_CLICK_ACTION);
        filter.addAction(ATSKConstants.L_OBSTRUCTION_CLICK_ACTION);
        AtakBroadcast.getInstance().registerReceiver(_radialReceiver, filter);
    }

    public static void unregisterReceivers() {
        AtakBroadcast.getInstance().unregisterReceiver(_radialReceiver);
    }

    public static void openColorDialog(MapItem item) {
        final MapItem fItem = item;
        if (!(item instanceof Shape)) {
            String shapeUID = item.getMetaString("shapeUID", "");
            if (!FileSystemUtils.isEmpty(shapeUID) && item.getGroup() != null)
                item = item.getGroup().deepFindUID(shapeUID);
            if (item == null || !(item instanceof Shape))
                return;
        }

        Context ctx = MapView.getMapView().getContext();
        Context plugin = ATSKApplication.getInstance().getPluginContext();
        final Shape shape = (Shape) item;
        AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
        adb.setTitle(plugin.getString(R.string.set_color));
        ColorPalette palette = new ColorPalette(ctx,
                shape.getStrokeColor());
        adb.setView(palette);
        final AlertDialog alert = adb.create();
        alert.show();
        palette.setOnColorSelectedListener(new ColorPalette.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color, String label) {
                shape.setStrokeColor(color);
                if (fItem instanceof ATSKMapItem)
                    ((ATSKMapItem) fItem).save();
                alert.dismiss();
            }
        });
    }

    // Radial action event handler
    private static final BroadcastReceiver _radialReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if (extras == null)
                return;

            // UID required
            String obsUID = extras.getString("obsUID", null);
            final String uid = FileSystemUtils.isEmpty(obsUID)
                    ? extras.getString("uid", null) : obsUID;
            if (uid == null)
                return;

            // Get associated marker
            final MapItem mp = find(uid);
            if (mp == null || !(mp instanceof ATSKMapItem))
                return;
            final ATSKMapItem item = (ATSKMapItem) mp;
            ATSKShape shape = mp instanceof ATSKShape ? (ATSKShape) mp : null;
            boolean isMarker = item instanceof ATSKMarker;

            Context ctx = MapView.getMapView().getContext();
            Context plugin = ATSKApplication.getInstance().getPluginContext();
            if (action.equals(SHOW_LABEL)) {
                // Toggle label visibility
                item.setLabelVisible(!item.getLabelVisible());
                item.save();
            } else if (action.equals(DELETE_OBS)) {
                // Delete item
                item.delete();
            } else if (action.equals(RENAME_OBS)) {
                // Rename obstruction with dialog
                String name = mp.getMetaString("remarks", "");
                // Redirect line leader to parent label
                if (item instanceof ATSKLineLeader) {
                    ATSKLabel label = ((ATSKLineLeader) item).getParent();
                    if (label != null)
                        name = label.getMetaString("remarks", "");
                }
                final EditText editName = new EditText(ctx);
                editName.setTextColor(ATSKConstants.LIGHT_BLUE);
                editName.setText(name);
                editName.setSingleLine();
                editName.setSelection(name.length());
                AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
                adb.setTitle(plugin.getString(R.string.rename_item));
                adb.setView(editName);
                adb.setPositiveButton(plugin.getString(R.string.rename),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                item.rename(editName.getText().toString());
                                dialog.dismiss();
                            }
                        });
                adb.setNeutralButton(plugin.getString(R.string.reset),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                item.rename("");
                                dialog.dismiss();
                            }
                        });
                adb.setNegativeButton(plugin.getString(R.string.cancel), null);
                adb.show();
            } else if (action.equals(SET_COLOR)) {
                openColorDialog(mp);
            } else if (isMarker && action.equals(COPY_OBS)) {
                // Ask for # copies first
                final EditText obsCopies = new EditText(ctx);
                obsCopies.setTextColor(ATSKConstants.LIGHT_BLUE);
                obsCopies.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                obsCopies.setInputType(InputType.TYPE_CLASS_NUMBER);
                obsCopies.setText("1");
                obsCopies.setSelection(1);
                AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
                adb.setTitle(plugin.getString(R.string.copy_obs));
                adb.setView(obsCopies);
                adb.setMessage("Enter number of copies:");
                adb.setPositiveButton(plugin.getString(R.string.copy),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                int copies;
                                try {
                                    copies = Integer.parseInt(obsCopies
                                            .getText().toString());
                                } catch (Exception e) {
                                    copies = 0;
                                }
                                item.copy(copies);
                                dialog.dismiss();
                            }
                        });
                adb.setNegativeButton(plugin.getString(R.string.cancel), null);
                adb.show();
            } else if (!isMarker && action.equals(ROTATE_LABEL)) {
                // Align label with line obstruction
                if (shape != null) {
                    shape.setRotateLabel(!shape.hasMetaValue("rotate_label"));
                    shape.updateLabelLine();
                    shape.save();
                }
            } else if (action.equals(DELETE_IMG)) {
                // Delete image associated with marker
                String path = extras.getString("imagePath");
                final File img = new File(path);
                String name = ExifHelper.getDescription(img);
                if (name == null || name.isEmpty())
                    name = img.getName();
                AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
                adb.setTitle(plugin.getString(R.string.remove_item));
                adb.setMessage(plugin.getString(R.string.remove_item_msg, name));
                adb.setPositiveButton(plugin.getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                if (img.exists() && img.isFile()) {
                                    FileSystemUtils.deleteFile(img);
                                    ATSKFragmentManager fm = ATSKMapComponent
                                            .getATSKFM();
                                    if (fm != null && fm.isCurrentFragment(
                                            ATSKFragment.IMG))
                                        fm.reloadActiveFragment();
                                }
                                if (mp.getGroup() != null)
                                    mp.getGroup().removeItem(mp);
                            }
                        });
                adb.setNegativeButton(plugin.getString(R.string.cancel), null);
                adb.show();
            } else if (action.equals(ATSKConstants.L_OBSTRUCTION_CLICK_ACTION)
                    || action.equals(ATSKConstants.PT_OBSTRUCTION_CLICK_ACTION)) {
                // Edit or copy obstruction in obstruction tab host
                final String group = intent
                        .getStringExtra(ATSKConstants.GROUP_EXTRA);
                final int copies = intent.getIntExtra(ATSKConstants.COPY_EXTRA,
                        0);
                ATSKFragmentManager fm = ATSKMapComponent.getATSKFM();
                if (fm != null) {
                    // Switch to required menu
                    String fragTag = fm.getCurrentFragmentTag();
                    if (group.equals(ATSKConstants.VEHICLE_GROUP))
                        fragTag = ATSKFragment.VEHICLE;
                    else if (group.equals(ATSKConstants.DISTRESS_GROUP))
                        fragTag = ATSKFragment.GRAD;
                    else if (group.equals(ATSKConstants.DEFAULT_GROUP))
                        fragTag = ATSKFragment.OBS;
                    if (!fm.isCurrentFragment(fragTag))
                        fm.fragmentSelected(fragTag);

                    // Request edit on menu
                    ATSKBaseFragment frag = fm.getCurrentFragment();
                    if (frag instanceof ObstructionTabHost) {
                        final ObstructionTabHost obsHost = (ObstructionTabHost) frag;
                        if (action
                                .equals(ATSKConstants.PT_OBSTRUCTION_CLICK_ACTION))
                            obsHost.ObstructionSelectedOnMap(group, uid, copies);
                        else if (action
                                .equals(ATSKConstants.L_OBSTRUCTION_CLICK_ACTION))
                            obsHost.LineSelectedOnMap(group, uid);
                    } else if (frag instanceof GradientTabHost) {
                        final GradientTabHost gradHost = (GradientTabHost) frag;
                        if (action
                                .equals(ATSKConstants.PT_OBSTRUCTION_CLICK_ACTION))
                            gradHost.ObstructionSelectedOnMap(group, uid);
                    } else if (frag instanceof VehicleTabHost) {
                        final VehicleTabHost vehHost = (VehicleTabHost) frag;
                        vehHost.editVehicle(uid, copies);
                    }
                }
            }
        }
    };
}
