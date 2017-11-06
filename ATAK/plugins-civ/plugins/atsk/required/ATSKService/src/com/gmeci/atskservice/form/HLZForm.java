
package com.gmeci.atskservice.form;

import com.gmeci.atskservice.R;

import com.gmeci.conversions.Conversions;

import com.gmeci.atskservice.form.formobjects.TextField;
import com.gmeci.atskservice.form.formobjects.CheckField;
import com.gmeci.atskservice.form.formobjects.RadioField;
import com.gmeci.atskservice.form.formobjects.FormObject;
import com.gmeci.atskservice.form.formobjects.Break;
import com.gmeci.atskservice.form.formobjects.ScreenHint;
import com.gmeci.atskservice.form.formobjects.GroupedField;
import com.gmeci.atskservice.form.formobjects.SimpleNameValue;
import com.gmeci.atskservice.form.formobjects.NestBlock;
import com.gmeci.atskservice.form.formobjects.Block;
import com.gmeci.atskservice.form.formobjects.Label;
import com.gmeci.atskservice.form.formobjects.KeyLabel;
import com.gmeci.atskservice.form.formobjects.Form;
import com.gmeci.helpers.AZHelper;
import com.gmeci.atskservice.form.formobjects.TriCheckField;

import com.gmeci.core.PointObstruction;

import java.util.*;

public class HLZForm extends AbstractFormActivity
{

    FormObject remarks;

    public Form createForm() {
        List<FormObject> foList = new ArrayList<FormObject>();

        FormObject[] si = new FormObject[] {
                new SimpleNameValue(c, "siteName", "1A. HLZ Name"),
                new SimpleNameValue(c, "zarIndexNo", "1B. ZAR Index No."),
                new SimpleNameValue(c, "country", "2A. Country"),
                new SimpleNameValue(c, "region", "2B. State"),
                new SimpleNameValue(c, "mapSeriesExtended",
                        "3. Map Series/Sheet Number/Edition/Date of Map")
        };
        foList.add(new Block(c, "hlz_survey_information",
                "Survey Information", si));

        FormObject[] sur = new FormObject[] {
                new SimpleNameValue(c, "surveyDate", "4A. Date Surveyed"),
                new SimpleNameValue(c, "surveyorNameGradeService",
                        "Typed Name and Grade of Surveyor"),
                new SimpleNameValue(c, "surveyorPhone", "Phone Number (DSN)"),
                new SimpleNameValue(c, "surveyorLocation", "Location")
        };

        foList.add(new Block(c, "hlz_surveyor", "Surveyor", sur));

        FormObject[] ro = new FormObject[] {
                new SimpleNameValue(c, "reviewDate", "4B. Date Reviewed"),
                new SimpleNameValue(c, "reviewerNameGradeService",
                        "Typed Name Grade and Grade of Reviewing officer"),

                new SimpleNameValue(c, "reviewerPhone",
                        "Phone Number (DSN)"),
                new SimpleNameValue(c, "reviewerUnitLocation",
                        "Unit And Location")
        };

        foList.add(new Block(c, "hlz_reviewing_officer", "Reviewing Officer",
                ro));

        FormObject[] approval = new FormObject[] {
                new SimpleNameValue(c, "approvalDate", "4C. Date"),
                new SimpleNameValue(c, "approverNameGradeService",
                        "Typed Name and Grade of Approving Authority"),
                new SimpleNameValue(c, "approverPhone",
                        "Phone Number (DSN)"),

                new ScreenHint.VisualLabel(c, " "),
                new GroupedField(c, new FormObject[] {
                        new Label(c, 10, "Approved/Disaproved"),
                        new TriCheckField(c, "hlzapproved", "")
                }, false),
                new ScreenHint.VisualLabel(c, " "),

                new SimpleNameValue(c, "approverUnitLocation",
                        "Unit And Location")
        };

        foList.add(new Block(c, "hlz_approval", "Approval", approval));

        FormObject[] coordinating_block = new FormObject[] {
                new SimpleNameValue(c, "controllingAgencyUnit",
                        "5A. HLZ Controlling Agency or Unit"),

                new SimpleNameValue(c, "controllingAgencyPhone",
                        "Phone Number (DSN)"),
                new SimpleNameValue(c, "rangeControl", "5B. Range Control"),
                new SimpleNameValue(c, "rangeControlPhone",
                        "Phone Number (DSN)")
        };

        foList.add(new Block(c, "hlz_coordinating_activies",
                "Coordinating Activities", coordinating_block));

        foList.add(buildRemarksBlock());

        FormObject[] r = new FormObject[foList.size()];
        foList.toArray(r);

        Form hlz = new Form(c, r);

        return hlz;
    }

    private FormObject buildRemarksBlock() {
        initMag();

        List<FormObject> list = new ArrayList<FormObject>();

        list.add(new ScreenHint.VisualLabel(c, "Statements"));

        list.add(new CheckField(c, "hlzStatement", -1,
                lusa(R.array.hlzstmt), "Statements", "\n", ""));

        list.add(new Label(c, -1, lus(R.string.hlzline0)));

        list.add(new NestBlock(c, "hlzline1", parseLine(R.string.hlzline1),
                new FormObject[] {
                        parseLine(R.string.hlzline1a),
                        parseLine(R.string.hlzline1b)
                }));

        record("hlzline3approach", getAngleString(
                Conversions.GetMagAngle(survey.approachAngle,
                        survey.center.lat,
                        survey.center.lon)));
        record("hlzline3departure", getAngleString(
                Conversions.GetMagAngle(survey.departureAngle,
                        survey.center.lat,
                        survey.center.lon)));
        list.add(parseLine(R.string.hlzline3));

        list.add(new SimpleNameValue(c, "hlzline4",
                lus(R.string.hlzline4)));

        list.add(new Label(c, -1, lus(R.string.hlzline5)));

        list.add(new Label(c, -1, lus(R.string.hlzline5a)));
        list.add(new CheckField(c, "hlzObstaclesInside", 10,
                concat(getInsideHazards(), lusa(R.array.hazards)), "Hazards"));

        list.add(new Label(c, -1, lus(R.string.hlzline5b)));
        list.add(new CheckField(c, "hlzObstaclesOutside", 10,
                concat(getOutsideHazards(), lusa(R.array.hazards)), "Hazards"));

        list.add(new Label(c, -1, lus(R.string.hlzline5c)));
        list.add(new CheckField(c, "hlzObstaclesQuadrant", 10,
                concat(getOtherQuadrantObstructions(), lusa(R.array.hazards)),
                "Terrain Hazards"));

        list.add(new Label(c, -1, lus(R.string.hlzline6)));
        list.add(new CheckField(c, "hlzObstaclesObserved", 10,
                concat(getObservedHazards(), lusa(R.array.hazards)), "Hazards"));

        list.add(new Label(c, -1, lus(R.string.hlzline7)));

        list.add(new Label(c, -1, lus(R.string.hlzline7a)));
        list.add(new CheckField(c, "hlzObstaclesTerrain", 10,
                concat(getObservedHazards(), lusa(R.array.hazards)), "Hazards"));

        list.add(new TextField(c, "hlzLine7Notes", 20,
                "Safety of Flight Review", true));
        list.add(new SimpleNameValue(c, "hlzLine8Notes",
                lus(R.string.hlzline8), true));

        record("hlzSurfaceType", survey.surface);
        addHandler(new KeyLabel(c, 7, "hlzSurfaceType"));

        list.add(parseLine(R.string.hlzline8a));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);

        remarks =
                new Block(c, "hlz_remarks_block", "Remarks Block", r);
        return remarks;

    }

    public String[] getOtherQuadrantObstructions() {
        PointObstruction[][] obstructions = AZHelper
                .CalculateQuadrantObstructions(survey, opc);
        List<String> list = new ArrayList<String>();

        PointObstruction[] current = obstructions[AZHelper.TOP];
        if (current != null) {
            list.add(BuildObstructionRAB(survey, current[0])
                    + " " + BuildObstructionDescription(current[0]));
        }
        current = obstructions[AZHelper.BOTTOM];
        if (current != null) {
            list.add(BuildObstructionRAB(survey, current[0])
                    + " " + BuildObstructionDescription(current[0]));
        }
        current = obstructions[AZHelper.RIGHT];
        if (current != null) {
            list.add(BuildObstructionRAB(survey, current[0])
                    + " " + BuildObstructionDescription(current[0]));
        }
        current = obstructions[AZHelper.LEFT];
        if (current != null) {
            list.add(BuildObstructionRAB(survey, current[0])
                    + " " + BuildObstructionDescription(current[0]));
        }

        String[] retval = new String[list.size()];
        list.toArray(retval);
        return retval;
    }

    @Override
    public void generateBlocks() {
        record("remarks", remarks.getText());
    }

}
