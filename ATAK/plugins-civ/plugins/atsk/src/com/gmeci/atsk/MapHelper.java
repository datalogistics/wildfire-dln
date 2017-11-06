
package com.gmeci.atsk;

import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.coremap.maps.coords.AltitudeSource;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointSource;
import com.gmeci.core.SurveyPoint;
import com.gmeci.core.SurveyPoint.AltitudeRef;

import java.util.List;

public class MapHelper {

    public static GeoPoint[] convertSurveyPoint2GeoPoint(
            List<SurveyPoint> linePoints) {
        GeoPoint[] lineGeoPoints = new GeoPoint[linePoints.size()];

        for (int i = 0; i < linePoints.size(); i++)
            lineGeoPoints[i] = convertSurveyPoint2GeoPoint(linePoints.get(i));

        return lineGeoPoints;
    }

    public static GeoPoint convertSurveyPoint2GeoPoint(SurveyPoint sp) {
        GeoPointSource src = GeoPointSource.UNKNOWN;
        if (sp.collectionMethod != null) {
            if (sp.collectionMethod == SurveyPoint.CollectionMethod.MANUAL)
                src = GeoPointSource.USER;
            else if (sp.collectionMethod == SurveyPoint.CollectionMethod.INTERNAL_GPS
                    || sp.collectionMethod == SurveyPoint.CollectionMethod.EXTERNAL_GPS
                    || sp.collectionMethod == SurveyPoint.CollectionMethod.RTK)
                src = GeoPointSource.GPS;
        }
        return new GeoPoint(sp.lat, sp.lon, convertAltitude(sp),
                sp.circularError, sp.linearError, src);
    }

    public static SurveyPoint convertGeoPoint2SurveyPoint(GeoPoint gp) {
        SurveyPoint sp = new SurveyPoint(gp.getLatitude(),
                gp.getLongitude(), convertAltitude(gp.getAltitude()));
        sp.circularError = gp.getCE();
        sp.linearError = gp.getLE();
        GeoPointSource src = gp.getSource();
        if (src == GeoPointSource.GPS || src == GeoPointSource.GPS_PPS)
            sp.collectionMethod = SurveyPoint.CollectionMethod.INTERNAL_GPS;
        else if (src != null)
            sp.collectionMethod = SurveyPoint.CollectionMethod.MANUAL;
        return sp;
    }

    public static SurveyPoint[] convertGeoPoints(GeoPoint[] points) {
        SurveyPoint[] ret = new SurveyPoint[points.length];
        int i = 0;
        for (GeoPoint gp : points)
            ret[i++] = convertGeoPoint2SurveyPoint(gp);
        return ret;
    }

    public static Altitude convertAltitude(SurveyPoint sp) {
        if (!sp.alt.isValid())
            return Altitude.UNKNOWN;
        AltitudeSource src = AltitudeSource.UNKNOWN;
        if (sp.collectionMethod != null) {
            if (sp.collectionMethod == SurveyPoint.CollectionMethod.MANUAL)
                src = AltitudeSource.USER;
            else if (sp.collectionMethod == SurveyPoint.CollectionMethod.INTERNAL_GPS
                    || sp.collectionMethod == SurveyPoint.CollectionMethod.EXTERNAL_GPS
                    || sp.collectionMethod == SurveyPoint.CollectionMethod.RTK)
                src = AltitudeSource.GPS;
        }
        return new Altitude(sp.alt.getValue(), sp.alt.isHAE() ?
                AltitudeReference.HAE : AltitudeReference.MSL, src);
    }

    public static SurveyPoint.Altitude convertAltitude(Altitude alt) {
        SurveyPoint.Altitude spAlt = new SurveyPoint.Altitude(alt.getValue(),
                alt.getReference() == AltitudeReference.HAE ?
                        AltitudeRef.HAE : AltitudeRef.MSL);
        if (!alt.isValid())
            spAlt.invalidate();
        return spAlt;
    }
}
