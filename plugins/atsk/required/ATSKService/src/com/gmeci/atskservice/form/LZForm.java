
package com.gmeci.atskservice.form;

import com.gmeci.atskservice.R;

import com.gmeci.atskservice.form.formobjects.TextField;
import com.gmeci.atskservice.form.formobjects.TriCheckField;
import com.gmeci.atskservice.form.formobjects.CheckField;
import com.gmeci.atskservice.form.formobjects.RadioField;
import com.gmeci.atskservice.form.formobjects.FormObject;
import com.gmeci.atskservice.form.formobjects.Break;
import com.gmeci.atskservice.form.formobjects.SimpleNameValue;
import com.gmeci.atskservice.form.formobjects.ScreenHint;
import com.gmeci.atskservice.form.formobjects.NestBlock;
import com.gmeci.atskservice.form.formobjects.Block;
import com.gmeci.atskservice.form.formobjects.GroupedField;
import com.gmeci.atskservice.form.formobjects.Label;
import com.gmeci.atskservice.form.formobjects.Form;
import com.gmeci.core.LineObstruction;
import com.gmeci.helpers.PolygonHelper;

import android.database.Cursor;

import com.gmeci.helpers.ObstructionHelper;

import java.util.*;

public class LZForm extends AbstractLZForm
{

    private static final String PCR = "----------------- PENETRATIONS CONTINUED IN REMARKS";
    private static final String COP = "----------------- CONTINUATION OF PENETRATIONS     ";
    private static final String EOP = "----------------- END OF PENETRATIONS/START REMARKS";

    FormObject remarks;

    public Form createForm() {
        List<FormObject> foList = new ArrayList<FormObject>();

        FormObject[] si = new FormObject[] {
                new SimpleNameValue(c, "siteName", "1A. LZ Name"),
                new SimpleNameValue(c, "zarIndexNo", "1B. ZAR Index"),
                new SimpleNameValue(c, "countryName", "2A. Country"),
                new SimpleNameValue(c, "regionName", "2B. State"),
                new SimpleNameValue(c, "mapSeriesExtended",
                        "3. Map Series/Sheet Number/Edition/Date of Map")
        };
        foList.add(new Block(c, "lz_survey_information",
                "Survey Information", si));

        FormObject[] sur = new FormObject[] {
                new SimpleNameValue(c, "surveyDate", "4A. Date Surveyed"),
                new SimpleNameValue(c, "surveyorNameGradeService",
                        "Typed Name and Grade of Surveyor"),
                new SimpleNameValue(c, "surveyorPhone", "Phone Number (DSN)"),
                new SimpleNameValue(c, "surveyorUnit", "Unit")
        };

        foList.add(new Block(c, "lz_surveyor", "Surveyor", sur));

        FormObject[] ro = new FormObject[] {
                new SimpleNameValue(c, "reviewDate", "4B. Date Reviewed"),
                new SimpleNameValue(c, "reviewerNameGradeService",
                        "Typed Name and Grade of Reviewer"),

                new SimpleNameValue(c, "reviewerPhone",
                        "Phone Number (DSN)"),
                new SimpleNameValue(c, "reviewerUnitLocation",
                        "Unit And Location")
        };

        foList.add(new Block(c, "lz_reviewing_officer", "Reviewing Officer", ro));

        FormObject[] approval = new FormObject[] {
                new SimpleNameValue(c, "approvalDate", "4C. Date"),
                new SimpleNameValue(c, "approverNameGradeService",
                        "Typed Name and Grade of Approving Authority"),

                new SimpleNameValue(c, "approverPhone",
                        "Phone Number (DSN)"),

                new ScreenHint.VisualLabel(c, " "),
                new GroupedField(c, new FormObject[] {
                        new Label(c, 10, "Approved/Disaproved"),
                        new TriCheckField(c, "lzapproved", "")
                }, false),
                new ScreenHint.VisualLabel(c, " "),

                new SimpleNameValue(c, "approverUnitLocation",
                        "Unit And Location")
        };

        foList.add(new Block(c, "lz_approval", "Approval", approval));

        FormObject[] coordinating_block = new FormObject[] {
                new SimpleNameValue(c, "controllingAgencyUnit",
                        "LZ Controlling Agency or Unit"),

                new SimpleNameValue(c, "controllingAgencyPhone",
                        "Phone Number (DSN)"),
                new SimpleNameValue(c, "rangeControl", "Range Control"),
                new SimpleNameValue(c, "rangeControlPhone",
                        "Phone Number (DSN)")
        };

        foList.add(new Block(c, "lz_coordinating_activies",
                "Coordinating Activities", coordinating_block));

        foList.add(buildPenetrationsBlock());
        foList.add(buildRemarksBlock());

        FormObject[] r = new FormObject[foList.size()];
        foList.toArray(r);

        return new Form(c, r);
    }

    protected String[] getApproachHazards() {
        Cursor filteredCursor = ObstructionHelper
                .GetApproachFilteredPointCursor(opc, survey);
        List<String> values = ObstructionHelper
                .GetApproachFilteredObstructionStrings(
                        opc, filteredCursor, survey);
        if (filteredCursor != null)
            filteredCursor.close();

        values.addAll(ObstructionHelper
                .GetApproachFilteredLineStrings(opc, survey));

        String[] retval = new String[values.size()];
        values.toArray(retval);
        return retval;
    }

    protected String[] getClearZoneHazards(boolean Approach) {
        LineObstruction ApproachTrap;
        if (Approach)
            ApproachTrap = PolygonHelper
                    .getClearApproachTrapezoid(survey);
        else
            ApproachTrap = PolygonHelper
                    .getClearDepartureTrapezoid(survey);

        Cursor filteredCursor = ObstructionHelper.GetFilteredPointCursor(
                opc, ApproachTrap.points, null);

        List<String> values = ObstructionHelper
                .GetFilteredPointObstructionStrings(
                        opc, filteredCursor, survey, ApproachTrap.points, null);
        filteredCursor.close();

        values.addAll(ObstructionHelper
                .GetFilteredLineObstructionStrings(opc, survey,
                        ApproachTrap.points));

        String[] retval = new String[values.size()];
        values.toArray(retval);
        return retval;
    }

    private String[] GetMaintainedHazardStrings(boolean rightSide) {
        LineObstruction GradedTrap;
        if (rightSide)
            GradedTrap = PolygonHelper
                    .getRightMaintainedArea(survey);
        else
            GradedTrap = PolygonHelper
                    .getLeftMaintainedArea(survey);

        Cursor filteredCursor = ObstructionHelper
                .GetFilteredPointCursor(opc, GradedTrap.points,
                        null);
        List<String> values = ObstructionHelper
                .GetFilteredPointObstructionStrings(
                        opc, filteredCursor,
                        survey, GradedTrap.points, null);
        filteredCursor.close();

        values.addAll(ObstructionHelper
                .GetFilteredLineObstructionStrings(opc, survey,
                        GradedTrap.points));
        String[] retval = new String[values.size()];
        values.toArray(retval);
        return retval;
    }

    private String[] GetShoulderHazardStrings(boolean rightSide) {
        LineObstruction ShoulderTrap;
        if (rightSide)
            ShoulderTrap = PolygonHelper
                    .getRightShoulder(survey);
        else
            ShoulderTrap = PolygonHelper
                    .getLeftShoulder(survey);

        Cursor filteredCursor = ObstructionHelper
                .GetFilteredPointCursor(opc, ShoulderTrap.points,
                        null);
        List<String> values = ObstructionHelper
                .GetFilteredPointObstructionStrings(
                        opc, filteredCursor,
                        survey, ShoulderTrap.points, null);
        filteredCursor.close();

        values.addAll(ObstructionHelper.GetFilteredLineObstructionStrings(
                opc, survey, ShoulderTrap.points));

        String[] retval = new String[values.size()];
        values.toArray(retval);
        return retval;
    }

    private String[] GetGradedHazardStrings(boolean rightSide) {
        LineObstruction GradedTrap;
        if (rightSide)
            GradedTrap = PolygonHelper
                    .getRightGradedArea(survey);
        else
            GradedTrap = PolygonHelper
                    .getLeftGradedArea(survey);

        Cursor filteredPointCursor = ObstructionHelper
                .GetFilteredPointCursor(opc, GradedTrap.points,
                        null);
        List<String> values = ObstructionHelper
                .GetFilteredPointObstructionStrings(opc,
                        filteredPointCursor, survey,
                        GradedTrap.points, null);
        filteredPointCursor.close();

        values.addAll(ObstructionHelper
                .GetFilteredLineObstructionStrings(opc, survey,
                        GradedTrap.points));
        String[] retval = new String[values.size()];
        values.toArray(retval);
        return retval;

    }

    protected String[] getLeftShoulderHazards() {
        return GetShoulderHazardStrings(false);

    }

    protected String[] getRightShoulderHazards() {
        return GetShoulderHazardStrings(true);
    }

    protected String[] getLeftGradedHazards() {
        return GetGradedHazardStrings(false);
    }

    protected String[] getRightGradedHazards() {
        return GetGradedHazardStrings(true);
    }

    protected String[] getLeftMaintainedHazards() {
        return GetMaintainedHazardStrings(false);
    }

    protected String[] getRightMaintainedHazards() {
        return GetMaintainedHazardStrings(true);
    }

    protected String getLeftShoulderName() {
        return "Left Shoulder";
    }

    protected String getRightShoulderName() {
        return "Right Shoulder";
    }

    protected String getLeftGradedAreaName() {
        return "Left Graded Area";
    }

    protected String getRightGradedAreaName() {
        return "Right Graded Area";
    }

    protected String getLeftMaintainedAreaName() {
        return "Left Maintained Area";
    }

    protected String getRightMaintainedAreaName() {
        return "Right Maintained Area";
    }

    protected FormObject buildRemarksBlock() {

        installHandlers();

        List<FormObject> list = new ArrayList<FormObject>();

        list.add(new ScreenHint.VisualLabel(c, "Restrictions:"));
        list.add(new CheckField(c, "restrictions", 20, getRestrictions(),
                "Restrictions", "\n", ""));

        list.add(new Break(c, true));

        list.add(new ScreenHint.VisualLabel(c, "Statements:"));
        list.add(new RadioField(c, "lzStatement", -1,
                lusa(R.array.dzstmt), "Statements"));

        list.add(new Label(c, -1, lus(R.string.dzline0)));

        list.add(new NestBlock(c, "lzline1", parseLine(R.string.hlzline1),
                new FormObject[] {
                        parseLine(R.string.hlzline1a),
                        parseLine(R.string.hlzline1b)
                }));

        list.add(new SimpleNameValue(c, "dzline2",
                lus(R.string.dzline2)));

        List<FormObject> foList = new ArrayList<FormObject>();
        foList.add(createHazardBlock(parseLine(R.string.lzline3a),
                "lzApproachHazards",
                concat(getApproachHazards(), lusa(R.array.hazards))));
        foList.add(createHazardBlock(parseLine(R.string.lzline3b),
                "lzClearZoneHazards",
                concat(getClearZoneHazards(true), lusa(R.array.hazards))));
        foList.add(createHazardBlock(parseLine(getLeftShoulderName()),
                "lzLeftShoulderHazards",
                concat(getLeftShoulderHazards(), lusa(R.array.hazards))));
        foList.add(createHazardBlock(parseLine(getRightShoulderName()),
                "lzRightShoulderHazards",
                concat(getRightShoulderHazards(), lusa(R.array.hazards))));
        foList.add(createHazardBlock(parseLine(getLeftGradedAreaName()),
                "lzLeftGradedHazards",
                concat(getLeftGradedHazards(), lusa(R.array.hazards))));
        foList.add(createHazardBlock(parseLine(getRightGradedAreaName()),
                "lzRightGradedHazards",
                concat(getRightGradedHazards(), lusa(R.array.hazards))));

        if (getLeftMaintainedAreaName() != null)
            foList.add(createHazardBlock(
                    parseLine(getLeftMaintainedAreaName()),
                    "lzLeftMaintainedHazards",
                    concat(getLeftMaintainedHazards(), lusa(R.array.hazards))));

        if (getRightMaintainedAreaName() != null)
            foList.add(createHazardBlock(
                    parseLine(getRightMaintainedAreaName()),
                    "lzRightMaintainedHazards",
                    concat(getRightMaintainedHazards(), lusa(R.array.hazards))));

        foList.add(createHazardBlock(
                parseLine("Obstacles or Hazards Outside LZ Zones:"),
                "lzOutsideHazards",
                concat(getOutsideHazards(), lusa(R.array.hazards))));

        FormObject[] fo = new FormObject[foList.size()];
        foList.toArray(fo);

        list.add(new NestBlock(c, "lzline3", parseLine(R.string.lzline3), fo));

        list.add(new Label(c, -1, lus(R.string.lzline5)));

        list.add(new CheckField(c, "lzObstaclesObserved", 10,
                concat(getObservedHazards(), lusa(R.array.hazards)), "Hazards"));

        double leftHalf = survey.edges.LeftHalfRunwayGradient;
        double rightHalf = survey.edges.RightHalfRunwayGradient;

        list.add(parseLine("6. Safety of Flight Review"));
        list.add(new TextField(c, "lzline6", 20, "Safety of Flight Review",
                true));
        list.add(parseLine("7. Notes"));
        list.add(new TextField(c, "lzline7notes", 20, "Notes", true));

        if (Math.abs(leftHalf) < .25 || Math.abs(rightHalf) < .25) {
            list.add(parseLine(R.string.lzline7a));
        }

        list.add(new Label(c, -1, "LZ surface slopes Left Half: "
                + getGradientString(leftHalf)
                + " Right Half: "
                + getGradientString(rightHalf)));

        list.add(new NestBlock(c, "lzline8", parseLine(R.string.lzline8),
                new FormObject[] {
                        parseLine(R.string.lzline8a),
                        parseLine(R.string.lzline8b),
                        parseLine(R.string.lzline8c)
                }));
        list.add(new NestBlock(c, "lzline9", parseLine(R.string.lzline9),
                new FormObject[] {
                        parseLine(R.string.lzline9a),
                        parseLine(R.string.lzline9b),
                        parseLine(R.string.lzline9c)
                }));
        list.add(new NestBlock(c, "lzline10", parseLine(R.string.lzline10),
                new FormObject[] {
                        parseLine(R.string.lzline10a),
                        parseLine(R.string.lzline10b),
                        parseLine(R.string.lzline10c)
                }));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);

        remarks =
                new Block(c, "lz_remarks_block", "Remarks Block", r);
        return remarks;

    }

    @Override
    public void generateBlocks() {
        final String pText[] = split(penetrations.getText(), 8);
        final String rText = remarks.getText();

        if (pText[1] != null) {
            record("penetrations", pText[0] + "\n" + PCR);
            record("remarks", COP + "\n" + pText[1] + "\n" + EOP + "\n" + rText);
        } else {
            record("penetrations", pText[0]);
            record("remarks", rText);
        }

        record("soilStrengthProfile", parseLine(R.string.lzline8a).getText()
                .replace("a. ", ""));
    }

}
