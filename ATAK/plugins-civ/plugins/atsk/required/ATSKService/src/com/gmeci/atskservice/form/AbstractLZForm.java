
package com.gmeci.atskservice.form;

import com.gmeci.atskservice.R;

import com.gmeci.atskservice.form.formobjects.TextField;
import com.gmeci.atskservice.form.formobjects.FormObject;
import com.gmeci.atskservice.form.formobjects.NestBlock;
import com.gmeci.atskservice.form.formobjects.Block;
import com.gmeci.atskservice.form.formobjects.ScreenHint;
import com.gmeci.atskservice.form.formobjects.Label;
import com.gmeci.atskservice.form.formobjects.KeyLabel;
import com.gmeci.atskservice.form.formobjects.RadioField;
import com.gmeci.atskservice.form.formobjects.CheckField;
import com.gmeci.core.PointObstruction;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.SurveyPoint;
import com.gmeci.core.SurveyPoint.Altitude;

import java.util.*;

public abstract class AbstractLZForm extends AbstractFormActivity
{

    FormObject penetrations;

    /**
     * Runway numbers and letters are determined from the approach direction.
     * The runway number is the whole number nearest one-tenth the magnetic azimuth
     * of the centerline of the runway, measured clockwise from the magnetic north.
     * If 00, then the runway should be 36.
     */
    protected String getRunwayDirection(boolean inverse) {
        int Angle = (int) Math.round(Conversions.GetMagAngle(
                survey.angle + (inverse ? 180 : 0),
                survey.center.lat,
                survey.center.lon) / 10.0);
        if (Angle > 35)
            Angle -= 36;
        if (Angle == 0)
            Angle = 36;
        return String.format("%02d", Angle);

    }

    protected String getGradientString(final double gradient) {
        double d = Math.atan(gradient);
        String sign = "";
        if (d > 0)
            sign = "+";
        return String.format("%s%.1f%%", sign, d);
    }

    protected String getDisplacedThresholdMGRS(boolean inverse) {
        double d[] = getDisplacedThreshold(inverse);
        return Conversions.GetMGRS(d[0], d[1]);
    }

    protected String getDisplacedThresholdLatLon(boolean inverse) {
        double d[] = getDisplacedThreshold(inverse);
        return Conversions.GetLatLonDM(d[0], d[1]);
    }

    private double[] getDisplacedThreshold(boolean inverse) {
        double[] result;

        if (inverse) {
            result = Conversions.AROffset(survey.center.lat,
                    survey.center.lon,
                    survey.angle,
                    (survey.getLength(true) / 2)
                            - survey.edges.DepartureOverrunLength_m
                            - survey.dDisplacedThreshold);

        } else {
            result = Conversions.AROffset(survey.center.lat,
                    survey.center.lon,
                    survey.angle + 180,
                    (survey.getLength(true) / 2)
                            - survey.edges.ApproachOverrunLength_m
                            - survey.aDisplacedThreshold);
        }
        return result;

    }

    protected String getMGRSRunwayEOO(boolean inverse) {
        SurveyPoint point = AZHelper.CalculateCenterOfEdge(
                survey.angle,
                survey.getLength(true), survey.center, !inverse);
        return Conversions.GetMGRS(point.lat,
                point.lon);
    }

    protected String getLatLonRunwayEOO(boolean inverse) {
        SurveyPoint point = AZHelper.CalculateCenterOfEdge(
                survey.angle,
                survey.getLength(true), survey.center, !inverse);
        return Conversions.GetLatLonDM(point.lat,
                point.lon);
    }

    protected String getControllingObstacle() {
        if (survey.worstApproachIncursionPoint == null) {
            return "NONE";
        } else {
            String Description = survey.worstApproachIncursionPoint.type;
            if (survey.worstApproachIncursionPoint.remark != null) {
                Description = Description
                        + survey.worstApproachIncursionPoint.remark;
            }
            return Description;
        }
    }

    protected String getInverseControllingObstacle() {
        if (survey.worstDepartureIncursionPoint == null) {
            return "NONE";
        } else {
            String Description = survey.worstDepartureIncursionPoint.type;
            if (survey.worstDepartureIncursionPoint.remark != null) {
                Description = Description
                        + survey.worstDepartureIncursionPoint.remark;
            }
            return Description;
        }

    }

    protected String getControllingObstacleDistance() {
        if (survey.worstApproachIncursionPoint == null) {
            return "N/A";
        } else {
            double czFarCenter[] = Conversions
                    .AROffset(
                            survey.center.lat,
                            survey.center.lon,
                            survey.angle + 180,
                            survey.getLength(false)
                                    / 2
                                    + survey.endClearZoneLength);
            double RAB[] = Conversions
                    .CalculateRangeAngle(
                            czFarCenter[0],
                            czFarCenter[1],
                            survey.worstApproachIncursionPoint.lat,
                            survey.worstApproachIncursionPoint.lon);

            return String.format("%.0f ft", RAB[0] * Conversions.M2F);
        }
    }

    protected String getInverseControllingObstacleDistance() {
        if (survey.worstDepartureIncursionPoint == null) {
            return "N/A";
        } else {
            double czFarCenter[] = Conversions
                    .AROffset(
                            survey.center.lat,
                            survey.center.lon,
                            survey.angle,
                            survey.getLength(false)
                                    / 2
                                    + survey.endClearZoneLength);
            double RAB[] = Conversions
                    .CalculateRangeAngle(
                            czFarCenter[0],
                            czFarCenter[1],
                            survey.worstDepartureIncursionPoint.lat,
                            survey.worstDepartureIncursionPoint.lon);

            return String.format("%.0f ft", RAB[0] * Conversions.M2F);
        }

    }

    protected String getControllingObstacleHeight() {
        return getHeightOffset(survey.worstApproachIncursionPoint,
                survey.approachElevation);
    }

    protected String getInverseControllingObstacleHeight() {
        return getHeightOffset(survey.worstDepartureIncursionPoint,
                survey.departureElevation);
    }

    protected String getHeightOffset(PointObstruction po, double elev_m) {
        if (po == null) {
            return "N/A";
        } else {
            double ret = po.height;
            if (Altitude.isValid(elev_m)
                    && Altitude.isValid(po.getHAE()))
                ret -= elev_m - po.getHAE();
            return getFeetString(ret);
        }
    }

    public String getInverseGSR() {
        return Conversions.ConvertGlideSlopeAngleToRatio(Conversions.deg360(
                survey.departureGlideSlopeDeg), true);
    }

    public String getGSR() {
        return Conversions.ConvertGlideSlopeAngleToRatio(Conversions.deg360(
                survey.approachGlideSlopeDeg), true);
    }

    protected String[] getRestrictions() {
        return lusa(R.array.restrictions);
    }

    protected FormObject buildPenetrationsBlock() {

        List<FormObject> list = new ArrayList<FormObject>();

        list.add(new ScreenHint.VisualLabel(c, "Restrictions:"));
        list.add(new CheckField(c, "restrictions", 20, getRestrictions(),
                "Restrictions", "\n", ""));
        list.add(new ScreenHint.Break(c, true));

        record("lzRunwayDirection", getRunwayDirection(false));
        record("lzInverseRunwayDirection", getRunwayDirection(true));
        record("lzEndOfUsableMGRS", getMGRSRunwayEOO(false));
        record("lzInverseEndOfUsableMGRS", getMGRSRunwayEOO(true));
        record("lzEndOfUsableLatLon", getLatLonRunwayEOO(false));
        record("lzInverseEndOfUsableLatLon", getLatLonRunwayEOO(true));
        record("lzControllingObstacle", getControllingObstacle());
        record("lzControllingObstacleDistance",
                getControllingObstacleDistance());
        record("lzControllingObstacleHeight", getControllingObstacleHeight());
        record("lzInverseControllingObstacle", getInverseControllingObstacle());
        record("lzInverseControllingObstacleDistance",
                getInverseControllingObstacleDistance());
        record("lzInverseControllingObstacleHeight",
                getInverseControllingObstacleHeight());
        record("lzRunwayGSR", getGSR());
        record("lzInverseRunwayGSR", getInverseGSR());

        addHandler(new KeyLabel(c, 4, "lzRunwayDirection"));
        addHandler(new KeyLabel(c, 4, "lzInverseRunwayDirection"));
        addHandler(new KeyLabel(c, 4, "lzEndOfUsable"));
        addHandler(new KeyLabel(c, 4, "lzInverseEndOfUsable"));

        //addHandler(new TextField(c, "lzControllingObstacle", 15,
        //        "Controlling Obstacle"));
        //addHandler(new TextField(c, "lzControllingObstacleDistance", 6,
        //        "Controlling Obstacle Distance"));
        //addHandler(new TextField(c, "lzControllingObstacleHeight", 7,
        //        "Controlling Obstacle Height"));
        //addHandler(new TextField(c, "lzInverseControllingObstacle", 15,
        //        "Controlling Obstacle"));
        //addHandler(new TextField(c, "lzInverseControllingObstacleDistance", 6,
        //        "Controlling Obstacle Distance"));
        //addHandler(new TextField(c, "lzInverseControllingObstacleHeight", 7,
        //       "Controlling Obstacle Height"));
        //addHandler(new TextField(c, "lzRunwayGSR", 7, "Glide Slope Ratio"));
        //addHandler(new TextField(c, "lzInverseRunwayGSR", 7,
        //        "Glide Slope Ratio"));

        record("lzRunwayOverrun",
                getFeetString(survey.edges.ApproachOverrunLength_m));
        record("lzInverseRunwayOverrun",
                getFeetString(survey.edges.DepartureOverrunLength_m));
        addHandler(new KeyLabel(c, 4, "lzRunwayOverrun"));
        addHandler(new KeyLabel(c, 4, "lzInverseRunwayOverrun"));

        list.add(new NestBlock(c, "lzpen_line1",
                parseLine(R.string.lzpen_line1),
                new FormObject[] {
                        parseLine(R.string.lzpen_line1a),
                        parseLine(R.string.lzpen_line1b)
                }));

        double stdOverrun = survey.getMetaDouble("stdApproachOverrun",
                survey.edges.ApproachOverrunLength_m);
        double stdInverseOverrun = survey.getMetaDouble("stdDepartureOverrun",
                survey.edges.DepartureOverrunLength_m);
        boolean nonStdApproach = false;
        boolean nonStdDeparture = false;

        if (Double.compare(Math.round(stdOverrun * 10),
                Math.round(survey.edges.ApproachOverrunLength_m * 10)) != 0) {
            nonStdApproach = true;
        }
        if (Double.compare(Math.round(stdInverseOverrun * 10),
                Math.round(survey.edges.DepartureOverrunLength_m * 10)) != 0) {
            nonStdDeparture = true;
        }

        list.add(new NestBlock(
                c,
                "lzpen_line2",
                parseLine(R.string.lzpen_line2),
                new FormObject[] {
                        new NestBlock(
                                c,
                                "lzpen_line2a",
                                parseLine(R.string.lzpen_line2a),
                                new FormObject[] {
                                        nonStdApproach ? parseLine(R.string.lzpen_line2a1)
                                                : new ScreenHint.VisualLabel(c,
                                                        "")
                                }),
                        new NestBlock(
                                c,
                                "lzpen_line2b",
                                parseLine(R.string.lzpen_line2b),
                                new FormObject[] {
                                        nonStdDeparture ? parseLine(R.string.lzpen_line2b1)
                                                : new ScreenHint.VisualLabel(c,
                                                        "")
                                })
                }));

        record("lzDisplacedThresholdMGRS", getDisplacedThresholdMGRS(false));
        record("lzDisplacedThresholdLatLon", getDisplacedThresholdLatLon(false));

        record("lzInverseDisplacedThresholdMGRS",
                getDisplacedThresholdMGRS(true));
        record("lzInverseDisplacedThresholdLatLon",
                getDisplacedThresholdLatLon(true));

        record("lzRunwayDisplaced",
                getFeetString(survey.aDisplacedThreshold));

        record("lzInverseRunwayDisplaced",
                getFeetString(survey.dDisplacedThreshold));

        record("lzRunwayRemaining",
                getFeetString(survey.getLength(true)
                        - survey.edges.ApproachOverrunLength_m
                        - survey.aDisplacedThreshold));
        record("lzInverseRunwayRemaining",
                getFeetString(survey.getLength(true)
                        - survey.edges.DepartureOverrunLength_m
                        - survey.dDisplacedThreshold));

        record("lzRunwayStdGSR",
                Conversions.ConvertGlideSlopeAngleToRatio(survey.getMetaDouble(
                        "stdApproachGSR", survey.approachGlideSlopeDeg), true));

        record("lzInverseRunwayStdGSR",
                Conversions.ConvertGlideSlopeAngleToRatio(survey.getMetaDouble(
                        "stdDepartureGSR", survey.departureGlideSlopeDeg), true));

        ArrayList<FormObject> line3 = new ArrayList<FormObject>();
        if (survey.aDisplacedThreshold != 0)
            line3.add(parseLine(R.string.lzpen_line3a));

        if (survey.dDisplacedThreshold != 0)
            line3.add(parseLine(R.string.lzpen_line3b));

        if ((survey.aDisplacedThreshold == 0)
                && (survey.dDisplacedThreshold == 0))
            line3.add(parseLine("No Displaced Threshold"));

        list.add(new NestBlock(
                c,
                "lzpen_line3",
                parseLine(R.string.lzpen_line3),
                (FormObject[]) line3.toArray(new FormObject[line3.size()])
                ));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);

        penetrations =
                new Block(c, "lz_pen_block", "Penetrations Block", r);
        return penetrations;
    }

    protected void installHandlers() {

        initMag();

        addHandler(new TextField(c, "lzRunwayC17Passes", 7, "C17 Runway Passes"));
        addHandler(new TextField(c, "lzRunwayC130Passes", 7,
                "C130 Runway Passes"));
        addHandler(new TextField(c, "lzRunwayC130JPasses", 7,
                "C130J Runway Passes"));
        addHandler(new TextField(c, "lzRunwayC130J30Passes", 7,
                "C130J30 Runway Passes"));
        addHandler(new TextField(c, "lzRunwayC146APasses", 7,
                "C146A Runway Passes"));
        addHandler(new TextField(c, "lzRunwayC145APasses", 7,
                "C145A Runway Passes"));

        addHandler(new TextField(c, "lzTaxiwayC17Passes", 7,
                "C17 Taxiway Passes"));
        addHandler(new TextField(c, "lzTaxiwayC130Passes", 7,
                "C130 Taxiway Passes"));
        addHandler(new TextField(c, "lzTaxiwayC130JPasses", 7,
                "C130J Taxiway Passes"));
        addHandler(new TextField(c, "lzTaxiwayC130J30Passes", 7,
                "C130J30 Taxiway Passes"));
        addHandler(new TextField(c, "lzTaxiwayC146APasses", 7,
                "C146A Taxiway Passes"));
        addHandler(new TextField(c, "lzTaxiwayC145APasses", 7,
                "C145A Taxiway Passes"));

        addHandler(new TextField(c, "lzApronC17Passes", 7, "C17 Apron Passes"));
        addHandler(new TextField(c, "lzApronC130Passes", 7, "C130 Apron Passes"));
        addHandler(new TextField(c, "lzApronC130JPasses", 7,
                "C130J Apron Passes"));
        addHandler(new TextField(c, "lzApronC130J30Passes", 7,
                "C130J30 Apron Passes"));
        addHandler(new TextField(c, "lzApronC146APasses", 7,
                "C146A Apron Passes"));
        addHandler(new TextField(c, "lzApronC145APasses", 7,
                "C145A Apron Passes"));

        addHandler(new TextField(c, "lzApronDCPReadings", 7,
                "Apron DCP Readings"));
        addHandler(new TextField(c, "lzApronDCPDepth", 7, "Apron DCP Depth"));

        addHandler(new TextField(c, "lzApronSurfaceBaseReadings", 7,
                "Apron Surface Reading"));
        addHandler(new TextField(c, "lzApronSurfaceCBRReadings", 7,
                "Apron Surface CBR Reading"));
        addHandler(new TextField(c, "lzApronSubbaseReadings", 7,
                "Apron Subbase Reading"));
        addHandler(new TextField(c, "lzApronSubbaseCBRReadings", 7,
                "Apron Subbase CBR Reading"));
        addHandler(new TextField(c, "lzApronSubgradeReadings", 7,
                "Apron Subgrade Reading"));
        addHandler(new TextField(c, "lzApronSubgradeCBRReadings", 7,
                "Apron Subgrade CBR Reading"));

        addHandler(new TextField(c, "lzTaxiwayDCPReadings", 7,
                "Taxiway DCP Readings"));
        addHandler(new TextField(c, "lzTaxiwayDCPDepth", 7, "Taxiway DCP Depth"));
        addHandler(new TextField(c, "lzTaxiwaySurfaceBaseReadings", 7,
                "Taxiway Surface Reading"));
        addHandler(new TextField(c, "lzTaxiwaySurfaceCBRReadings", 7,
                "Taxiway Surface CBR Reading"));
        addHandler(new TextField(c, "lzTaxiwaySubbaseReadings", 7,
                "Taxiway Subbase Reading"));
        addHandler(new TextField(c, "lzTaxiwaySubbaseCBRReadings", 7,
                "Taxiway Subbase CBR Reading"));
        addHandler(new TextField(c, "lzTaxiwaySubgradeReadings", 7,
                "Taxiway Subgrade Reading"));
        addHandler(new TextField(c, "lzTaxiwaySubgradeCBRReadings", 7,
                "Taxiway Subgrade CBR Reading"));

        addHandler(new TextField(c, "lzRunwayDCPReadings", 7,
                "Runway DCP Readings"));
        addHandler(new TextField(c, "lzRunwayDCPDepth", 7, "Runway DCP Depth"));
        addHandler(new TextField(c, "lzRunwaySurfaceBaseReadings", 7,
                "Runway Surface Reading"));
        addHandler(new TextField(c, "lzRunwaySurfaceCBRReadings", 7,
                "Runway Surface CBR Reading"));
        addHandler(new TextField(c, "lzRunwaySubbaseCBRReadings", 7,
                "Runway Subbase CBR Readings"));
        addHandler(new TextField(c, "lzRunwaySubbaseReadings", 7,
                "Runway Subbase Reading"));
        addHandler(new TextField(c, "lzRunwaySubgradeReadings", 7,
                "Runway Subgrade Readings"));
        addHandler(new TextField(c, "lzRunwaySubgradeCBRReadings", 7,
                "Runway Subgrade CBR Reading"));

        addHandler(new TextField(c, "lzRunwayTransverse", 5,
                "Runway Transverse"));

        addHandler(new TextField(c, "lzApronControllingSurface", 5,
                "Apron Controlling Surface"));
        addHandler(new TextField(c, "lzTaxiwayControllingSurface", 5,
                "Taxiway Controlling Surface"));
        addHandler(new TextField(c, "lzRunwayControllingSurface", 5,
                "Runway Controlling Surface"));

        addHandler(new TextField(c, "lzApronControllingCBR", 5,
                "Apron Controlling CBR"));
        addHandler(new TextField(c, "lzTaxiwayControllingCBR", 5,
                "Taxiway Controlling CBR"));
        addHandler(new TextField(c, "lzRunwayControllingCBR", 5,
                "Runway Controlling CBR"));

    }

}
