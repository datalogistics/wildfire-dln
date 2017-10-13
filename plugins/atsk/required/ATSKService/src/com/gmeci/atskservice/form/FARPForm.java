
package com.gmeci.atskservice.form;

import com.gmeci.atskservice.R;

import com.gmeci.atskservice.farp.FARPACParser;
import com.gmeci.conversions.Conversions;

import com.gmeci.atskservice.form.formobjects.TextField;
import com.gmeci.atskservice.form.formobjects.CheckField;
import com.gmeci.atskservice.form.formobjects.FormObject;
import com.gmeci.atskservice.form.formobjects.RadioField;
import com.gmeci.atskservice.form.formobjects.SimpleNameValue;
import com.gmeci.atskservice.form.formobjects.Block;
import com.gmeci.atskservice.form.formobjects.Form;
import com.gmeci.atskservice.form.formobjects.KeyLabel;
import com.gmeci.atskservice.form.formobjects.ScreenHint.VisualLabel;
import com.gmeci.atskservice.form.formobjects.GroupedField;

import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;
import com.gmeci.helpers.FARPHelper;
import com.gmeci.atskservice.farp.FARPTankerItem;

import java.util.*;

public class FARPForm extends AbstractFormActivity
{
    FormObject sitelocation_block;
    FormObject surface_block;
    FormObject tanker_block;
    FormObject receiver_block;
    FormObject fuel_points_block;
    FormObject tanker_marshalling_block;
    FormObject receiver_marshalling_block;
    FormObject emergency_block;
    FormObject environment_block;
    FormObject obstacles_block;
    FormObject cfr_block;
    FormObject additional_block;

    private Map<String, FARPTankerItem> _tankers;

    public Form createForm() {
        FARPACParser parser = new FARPACParser();
        try {
            _tankers = parser.parseFile();
        } catch (Exception e) {
            _tankers = new HashMap<String, FARPTankerItem>();
        }
        List<FormObject> foList = new ArrayList<FormObject>();

        FormObject[] si = new FormObject[] {
                new SimpleNameValue(c, "siteName", "1. FARP Site Name"),
                new SimpleNameValue(c, "location", "2. Location")
        };

        foList.add(new Block(c, "farp_survey_information",
                "Survey Information", si));

        FormObject[] sur = new FormObject[] {
                new SimpleNameValue(c, "surveyDate", "4. Date"),
                new SimpleNameValue(c, "surveyorNameGradeService",
                        "5. Name and Grade of Surveyor"),
                new SimpleNameValue(c, "surveyorPhone", "6. Phone # DSN"),
                new SimpleNameValue(c, "surveyorCom", "Commercial Phone"),
                new SimpleNameValue(c, "surveyorFax", "Commercial Fax"),
                new SimpleNameValue(c, "surveyorUnit", "7. Unit"),
                new SimpleNameValue(c, "surveyorLocation", "8. Unit Location"),
        };

        foList.add(new Block(c, "farp_surveyor", "Surveyor", sur));

        FormObject[] ro = new FormObject[] {
                new SimpleNameValue(c, "reviewerNameGradeService",
                        "Name Grade and Service of Reviewing officer"),

                new SimpleNameValue(c, "reviewerDate", "10. Date Reviewed"),
                new SimpleNameValue(c, "reviewerNameGradeService",
                        "11. Name and Grade of Reviewer"),
                new SimpleNameValue(c, "reviewerPhone", "12. Phone # DSN"),
                new SimpleNameValue(c, "reviewerCom", "Commercial Phone"),
                new SimpleNameValue(c, "reviewerFax", "Commercial Fax"),
                new SimpleNameValue(c, "reviewerUnit", "13. Unit"),
                new SimpleNameValue(c, "reviewerLocation", "14. Unit Location"),
        };

        foList.add(new Block(c, "farp_reviewing_officer", "Reviewing Officer",
                ro));

        FormObject[] approval = new FormObject[] {
                new SimpleNameValue(c, "approverDate", "16. Date"),
                new SimpleNameValue(c, "approverNameGradeService",
                        "17. Name and Grade of Approving Authority"),
                new SimpleNameValue(c, "approverPhone", "18. Phone # DSN"),
                new SimpleNameValue(c, "approverCom", "Commercial Phone"),
                new SimpleNameValue(c, "approverFax", "Commercial Fax"),
                new SimpleNameValue(c, "approverUnit", "19. Unit"),
                new SimpleNameValue(c, "approverLocation", "20. Unit Location"),
        };

        foList.add(new Block(c, "farp_approval", "Approval", approval));

        FormObject[] coordinating_block = new FormObject[] {
                new SimpleNameValue(c, "rangeControl", "22. Range Control"),
                new SimpleNameValue(c, "rangeControlPhone",
                        "23. Phone # DSN"),
                new SimpleNameValue(c, "rangeControlCom", "Range Control Com"),
                new SimpleNameValue(c, "rangeControlFax", "Range Control Fax"),
                new SimpleNameValue(c, "airfieldMgmt",
                        "24. Airfield Management"),
                new SimpleNameValue(c, "airfieldMgmtPhone", "Airfield # DSN"),
                new SimpleNameValue(c, "airfieldMgmtCom", "Commercial Phone"),
                new SimpleNameValue(c, "airfieldMgmtFax", "Commercial Fax"),
                new SimpleNameValue(c, "cfr", "26. Crash Fie Rescue (CFR)"),
                new SimpleNameValue(c, "cfrPhone", "27. Phone # Number"),
                new SimpleNameValue(c, "cfrCom", "Commercial Phone"),
                new SimpleNameValue(c, "cfrFax", "Commercial Fax"),
                new SimpleNameValue(c, "enviro", "28. Environment"),
                new SimpleNameValue(c, "enviroPhone", "29. Phone # DSN"),
                new SimpleNameValue(c, "enviroCom", "Commercial Phone"),
                new SimpleNameValue(c, "enviroFax", "Commercial Fax")
        };

        foList.add(new Block(c, "farp_coordinating_activies",
                "Coordinating Activities", coordinating_block));

        FormObject tankerPos = buildTankerMarshallingBlock();
        FormObject receiverPos = buildReceiverMarshallingBlock();

        foList.add(buildSiteLocation());
        foList.add(buildSurfaceBlock());
        foList.add(buildTankerBlock());
        foList.add(buildReceiverBlock());
        foList.add(buildFuelPointsBlock());
        foList.add(tankerPos);
        foList.add(receiverPos);
        foList.add(buildEmergencyBlock());
        foList.add(buildEnvironmentBlock());
        foList.add(buildObstaclesBlock());
        foList.add(buildCfrBlock());
        foList.add(buildAdditionalBlock());

        FormObject[] r = new FormObject[foList.size()];
        foList.toArray(r);

        Form hlz = new Form(c, r);

        return hlz;
    }

    private String getFarpHeading() {
        double MagHeading = Conversions.GetMagAngle(
                survey.center.course_true,
                survey.center.lat,
                survey.center.lon);
        String CardinalDirectionMag = String.format("%s mag",
                Conversions.GetCardinalDirection(MagHeading));
        return CardinalDirectionMag;

    }

    public String getFarpAircraftMgrs() {
        return Conversions.GetMGRS(survey.center.lat, survey.center.lon);
    }

    public String getFarpAircraftLatLon() {
        return Conversions.GetLatLonDM(survey.center.lat, survey.center.lon);
    }

    private FormObject buildSiteLocation() {

        List<FormObject> list = new ArrayList<FormObject>();
        list.add(new SimpleNameValue(c, "farpDescription", "Description: "));

        record("farpHeading", getFarpHeading());
        addHandler(new TextField(c, "farpHeading", 5, "Criteria Facing"));
        addHandler(new TextField(c, "farpTankerFacing", 10,
                "Criteria Facing Description: "));

        record("farpAircraftType", survey.aircraft);
        addHandler(new TextField(c, "farpAircraftType", 5, "Criteria Type"));
        record("farpAircraftMgrs", getFarpAircraftMgrs());
        addHandler(new TextField(c, "farpAircraftMgrs", 9,
                "Criteria Location (MGRS)"));
        record("farpAircraftLatLon", getFarpAircraftLatLon());
        addHandler(new TextField(c, "farpAircraftLatLon", 12,
                "Criteria Location (DD)"));

        list.add(parseLine(R.string.farpstmt1));
        list.add(parseLine(R.string.farpstmt2));

        list.add(parseLine(R.string.farpline1));
        list.add(parseLine(R.string.farpline1a));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);

        sitelocation_block =
                new Block(c, "farp_sitelocation_block", "FARP Site Location", r);
        return sitelocation_block;

    }

    private FormObject buildSurfaceBlock() {
        List<FormObject> list = new ArrayList<FormObject>();

        record("siteSurface", survey.surface);
        list.add(new KeyLabel(c, 5, "siteSurface"));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        surface_block =
                new Block(c, "farp_surface_block", "FARP Site Surface", r);
        return surface_block;
    }

    private FormObject buildTankerBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        record("farpLargestTanker", survey.aircraft);

        String[] names = new String[_tankers.size()];
        int i = 0;
        for (FARPTankerItem tanker : _tankers.values())
            names[i++] = tanker.Name;
        addHandler(new TextField(c, "farpLargestTanker", 10, "Largest Tanker: "));
        addHandler(new CheckField(c, "farpAdditionalSmaller", 10,
                names, "Additional Smaller", ", ", "NONE"));

        list.add(parseLine(R.string.farp_tanker_line1));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        tanker_block =
                new Block(c, "farp_tanker_block", "Tanker Criteria Certified",
                        r);
        return tanker_block;
    }

    private FormObject buildReceiverBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        addHandler(new CheckField(c, "farpRotary", 20,
                lusa(R.array.rotary), "Rotary", ", ", "NONE"));
        addHandler(new CheckField(c, "farpFixedWingSize", 20,
                lusa(R.array.fwsize), "Fixed Wing Size", ", ", "NONE"));
        list.add(parseLine(R.string.farp_receiver_line1));
        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        receiver_block =
                new Block(c, "farp_receiver_block",
                        "Receiver Criteria Certified",
                        r);
        return receiver_block;
    }

    private FormObject buildFuelPointsBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        addHandler(new RadioField(c, "farpServicePoints", 2,
                lusa(R.array.servicepoints), "Fuel Points"));
        list.add(parseLine(R.string.farp_fuel_points_line1));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        fuel_points_block =
                new Block(c, "farp_fuel_points_block", "Fuel Points",
                        r);
        return fuel_points_block;
    }

    private FormObject buildTankerMarshallingBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        FARPTankerItem tanker = _tankers.get(survey.aircraft);
        if (tanker != null) {
            SurveyPoint nose = Conversions.AROffset(survey.center,
                    survey.angle, tanker.FuelPointOffset_m);
            String tankerTxt = "Center:  "
                    + getFarpAircraftMgrs() + "\nNose:  "
                    + Conversions.GetMGRS(nose.lat, nose.lon);
            record("farpTankerLocationMarshalling", tankerTxt);
            record("farpTankerLocationMarshalling_override", tankerTxt);
        }
        addHandler(new TextField(c, "farpTankerLocationMarshalling", 20,
                "Tanker Criteria Location Instructions", true));

        list.add(parseLine(R.string.farp_tanker_location_marshalling_line1));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        tanker_marshalling_block =
                new Block(c, "tanker_marshalling_block",
                        "Tanker Criteria Location Marshalling/Instructions",
                        r);
        return tanker_marshalling_block;
    }

    private FormObject buildReceiverMarshallingBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        FARPTankerItem tanker = _tankers.get(survey.aircraft);
        if (tanker != null) {
            SurveyPoint right = survey.FAMPoints[SurveyData
                    .getFARPSideIndex(true)];
            SurveyPoint left = survey.FAMPoints[SurveyData
                    .getFARPSideIndex(false)];
            StringBuilder defTxt = new StringBuilder();
            SurveyPoint[] rxPoints = new SurveyPoint[0];
            if (right.visible) {
                defTxt.append("R:  ");
                defTxt.append(Conversions.GetMGRS(right));
                rxPoints = AZHelper.getRefuelingPoints(
                        SurveyData.getFARPSideIndex(true), survey, tanker);
            } else if (left.visible) {
                defTxt.append("L:  ");
                defTxt.append(Conversions.GetMGRS(left));
                rxPoints = AZHelper.getRefuelingPoints(
                        SurveyData.getFARPSideIndex(false), survey, tanker);
            }
            // Add receiver fuel points to form
            int pNum = 1;
            for (SurveyPoint rx : rxPoints) {
                if (rx == null || !rx.visible)
                    continue;
                defTxt.append("\nRX Point ");
                defTxt.append(String.valueOf(pNum++));
                defTxt.append(":  ");
                defTxt.append(Conversions.GetMGRS(rx));
            }
            record("farpReceiverLocationMarshalling", defTxt.toString());
            record("farpReceiverLocationMarshalling_override",
                    defTxt.toString());
        }
        addHandler(new TextField(c, "farpReceiverLocationMarshalling", 20,
                "Receiver Criteria Location Instructions", true));

        list.add(parseLine(R.string.farp_receiver_location_marshalling_line1));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        receiver_marshalling_block =
                new Block(c, "receiver_marshalling_block",
                        "Receiver Criteria Location Marshalling/Instructions",
                        r);
        return receiver_marshalling_block;
    }

    private FormObject buildEmergencyBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        addHandler(new TextField(c, "farpEmergency", 20,
                "Emergency Egress and Reassemble Areas(s)", true));

        list.add(parseLine(R.string.farp_emergency_line1));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        emergency_block =
                new Block(c, "emergency_block",
                        "Emergency Egress and Reassemble Areas(s)",
                        r);
        return emergency_block;
    }

    private FormObject buildEnvironmentBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        addHandler(new TextField(c, "farpEnvironment", 20,
                "Environmental Impact Analysis", true));

        list.add(parseLine(R.string.farp_environment_line1));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        environment_block =
                new Block(c, "environment_block", "Environment Imact Analysis",
                        r);
        return environment_block;
    }

    private FormObject buildObstaclesBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        String[] hazards = concat(getInsideHazards(),
                concat(getOutsideHazards(), lusa(R.array.hazards)));

        addHandler(new CheckField(c, "farpObstructions", 20, hazards,
                "FARP Obstructions", ", ", "NONE"));
        addHandler(new CheckField(c, "farpTankerObstructions", 20, hazards,
                "FARP Tanker Obstructions", ", ", "NONE"));
        addHandler(new CheckField(c, "farpReceiverObstructions", 20, hazards,
                "FARP Receiver Obstructions", ", ", "NONE"));

        list.add(parseLine(R.string.farp_obstacles_line1));
        list.add(parseLine(R.string.farp_obstacles_line2));
        list.add(parseLine(R.string.farp_obstacles_line3));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        obstacles_block =
                new Block(c, "obstacles_block", "Obstacles and Hazards", r);
        return obstacles_block;
    }

    private FormObject buildCfrBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        addHandler(new TextField(c, "farpCfr", 20,
                "Crash Fire Rescue Position/Capabilities", true));

        list.add(parseLine(R.string.farp_cfr_line1));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        cfr_block =
                new Block(c, "cfr_block",
                        "Crash Fire Rescue Position/Capabilities",
                        r);
        return cfr_block;
    }

    private FormObject buildAdditionalBlock() {
        List<FormObject> list = new ArrayList<FormObject>();
        addHandler(new TextField(c, "farpAdditionalInfo", 20,
                "Additional Information", true));

        list.add(parseLine(R.string.farp_additional_info_line1));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);
        additional_block =
                new Block(c, "additional_block", "Additional Information",
                        r);
        return additional_block;
    }

    @Override
    public void generateBlocks() {
        record("siteLocation", sitelocation_block.getText());
        record("tankerAircraftCertified", tanker_block.getText());
        record("receiverAircraftCertified", receiver_block.getText());
        record("fuelPoints", fuel_points_block.getText());
        record("tankerInstructions", tanker_marshalling_block.getText());
        record("receiverInstructions", receiver_marshalling_block.getText());
        record("emergencyEgress", emergency_block.getText());
        record("enviroImpactAnalysis", environment_block.getText());
        record("obstaclesAndHazards", obstacles_block.getText());
        record("cfrPositionCapabilities", cfr_block.getText());
        record("additionalInfo", additional_block.getText());
    }

}
