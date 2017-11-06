
package com.gmeci.atsk.export;

import android.database.Cursor;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.export.imagecapture.SurveyCapturePrefs;
import com.gmeci.atsk.gallery.ATSKGalleryItem;
import com.gmeci.atsk.obstructions.ObstructionController;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.atskservice.resolvers.GradientDBItem;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;
import com.gmeci.helpers.ObstructionHelper;
import com.sromku.polygon.Polygon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ATSKTransferPackage {

    private static final String TAG = "ATSKTransferPackage";

    final ArrayList<SurveyData> surveyList = new ArrayList<SurveyData>();
    final ArrayList<PointObstruction> points = new ArrayList<PointObstruction>();
    final ArrayList<LineObstruction> lines = new ArrayList<LineObstruction>();
    final ArrayList<GradientDBItem> gradients = new ArrayList<GradientDBItem>();
    final ArrayList<ATSKGalleryItem> gallery = new ArrayList<ATSKGalleryItem>();

    void addAllSurveys(AZProviderClient azpc) {
        final Cursor cursor = azpc.getAllSurveys();
        if (cursor != null) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
                    .moveToNext()) {
                SurveyData data = azpc.getAZ(cursor);
                surveyList.add(data);
                //FillGallery(data.uid, ATSKGalleryUtils.getImages(data.uid));
            }
            cursor.close();
        }

    }

    void addSurvey(final AZProviderClient azpc, final String uid) {
        surveyList.add(azpc.getAZ(uid, false));
    }

    void addSurveyObstructions(SurveyData survey) {
        if (survey == null)
            return;
        ObstructionController obs = ObstructionController.getInstance();
        List<PointObstruction> points = obs.getPointObstructions();
        List<LineObstruction> lines = obs.getLineObstructions();

        List<SurveyPoint> corners = getExportBounds(survey);
        Polygon bounds = ObstructionHelper.buildPolygon(corners);

        // Include any points within bounds
        for (PointObstruction po : points) {
            if (bounds.contains(ObstructionHelper.polyPoint(po)))
                this.points.add(po);
        }

        // Include any lines intersecting or within bounds
        for (LineObstruction lo : lines) {
            if (!FileSystemUtils.isEmpty(ObstructionHelper.lineHitTest(
                    lo, corners)))
                this.lines.add(lo);
        }
    }

    void addSurveyGradients(SurveyData survey, GradientProviderClient gpc) {
        List<SurveyPoint> corners = getExportBounds(survey);
        this.gradients.addAll(gpc.getFilteredGradients(
                ATSKConstants.DEFAULT_GROUP, corners));
    }

    void addAllObstructions(ObstructionProviderClient opc) {
        points.addAll(opc.getAllPointObstructions());
        lines.addAll(opc.getAllLineObstructions("", true));
    }

    void addAllGradients(GradientProviderClient gpc) {
        gradients.addAll(getAllGradients(gpc));
    }

    void addGallery(String uid, File[] images) {
        for (File img : images)
            gallery.add(new ATSKGalleryItem(uid, img.getName()));
    }

    private static List<GradientDBItem> getAllGradients(GradientProviderClient gpc) {
        List<GradientDBItem> gradients = new ArrayList<GradientDBItem>();
        Cursor cur = gpc.GetAllGradients(ATSKConstants.DEFAULT_GROUP, true);
        if (cur != null) {
            try {
                for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
                    gradients.add(gpc.GetGradient(cur, false));
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception retrieving gradients", e);
            } finally {
                cur.close();
            }
        }
        return gradients;
    }

    public static List<SurveyPoint> getExportBounds(SurveyData survey) {
        double thresh = SurveyCapturePrefs.get("atsk_obs_export_threshold",
                10560) / Conversions.M2F;

        // Get outer bounds of survey + threshold
        double length = survey.getLength(true) + thresh * 2;
        double width = survey.width + thresh * 2;
        if (survey.getType() == SurveyData.AZ_TYPE.LZ) {
            length += (survey.endClearZoneLength + survey.approachInnerLength
                    + survey.approachOuterLength) * 2;
            width = Math.max(survey.width, Math.max(survey.approachInnerWidth,
                    survey.approachOuterWidth)) + thresh * 2;
        }
        return AZHelper.getCorners(survey.center.lat, survey.center.lon,
                length, width, survey.angle);
    }
}
