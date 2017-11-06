
package com.gmeci.atsk.resources;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;

import com.atakmap.android.gui.CoordDialogView;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.GeoPoint;

public class CoordinateHandJamDialog {

    public static final String COORD_FORMAT_MGRS = "COORD_FORMAT_MGRS";
    public static final String COORD_FORMAT_DMS = "COORD_FORMAT_DMS";
    public static final String COORD_FORMAT_DM = "COORD_FORMAT_DM";

    @SuppressWarnings("unused")
    private static final String TAG = "CoordinateHandJamDialog";

    private static String toATSKFormat(final CoordinateFormat cf) {
        if (CoordinateFormat.MGRS == cf)
            return COORD_FORMAT_MGRS;
        else if (CoordinateFormat.DMS == cf)
            return COORD_FORMAT_DMS;
        else if (CoordinateFormat.DM == cf)
            return COORD_FORMAT_DM;
        else
            return COORD_FORMAT_MGRS;
    }

    private static CoordinateFormat toATAKFormat(final String cf) {
        if (cf.equals(COORD_FORMAT_MGRS))
            return CoordinateFormat.MGRS;
        else if (cf.equals(COORD_FORMAT_DMS))
            return CoordinateFormat.DMS;
        else if (cf.equals(COORD_FORMAT_DM))
            return CoordinateFormat.DM;
        else
            return CoordinateFormat.MGRS;
    }

    /**
     * Uses the standard hand jam dialog box that is in use with the rest of ATAK adapted for legacy use
     * within ATSK.
     *
     * @param lat              is the latitude in decimal degreees.
     * @param lon              is the longitude in decimal degrees.
     * @param cf               is the coordinate format to use during the display.
     * @param elevation        in hae meters.
     * @param hji              the callback for when information is hand jammed.
     */
    public void Initialize(final double lat,
            final double lon,
            final String cf,
            final double elevation,
            final HandJamInterface hji) {

        //Context pluginContext = ATSKApplication.getInstance().getPluginContext();

        MapView mapView = MapView.getMapView();
        final CoordinateFormat _cFormat = toATAKFormat(cf);

        AlertDialog.Builder b = new AlertDialog.Builder(mapView.getContext());
        LayoutInflater inflater = LayoutInflater.from(mapView.getContext());
        final CoordDialogView coordView = (CoordDialogView) inflater
                .inflate(
                        com.atakmap.app.R.layout.draper_coord_dialog, null);
        b.setTitle("Enter Coordinate: ")
                .setView(coordView)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                // On click get the geopoint and elevation double in ft
                                GeoPoint p = coordView.getPoint();
                                boolean changedFormat = coordView
                                        .getCoordFormat() != _cFormat;

                                CoordinateFormat _cFormat = coordView
                                        .getCoordFormat();

                                CoordDialogView.Result result = coordView
                                        .getResult();

                                if (result == CoordDialogView.Result.VALID_UNCHANGED
                                        && changedFormat) {
                                    // The coordinate format was changed but not the point itself
                                    if (hji != null)
                                        hji.UpdateCoordinateFormat(toATSKFormat(_cFormat));
                                }
                                if (result == CoordDialogView.Result.VALID_CHANGED) {
                                    if (hji != null) {
                                        hji.UpdateCoordinateFormat(toATSKFormat(_cFormat));
                                        double alt = Altitude.UNKNOWN
                                                .getValue();
                                        if (p.getAltitude() != Altitude.UNKNOWN) {
                                            alt = EGM96.getInstance().getHAE(p)
                                                    .getValue();
                                        }
                                        hji.UpdateCoordinate(p.getLatitude(),
                                                p.getLongitude(), alt);
                                    }

                                }
                            }
                        })
                .setNegativeButton("Cancel", null);
        coordView.setParameters(new GeoPoint(lat, lon, elevation),
                mapView.getPoint(),
                _cFormat);
        b.show();

    }

    public interface HandJamInterface {
        void UpdateCoordinate(double Lat, double Lon, double elev);

        void UpdateCoordinateFormat(String DisplayFormat);
    }

}
