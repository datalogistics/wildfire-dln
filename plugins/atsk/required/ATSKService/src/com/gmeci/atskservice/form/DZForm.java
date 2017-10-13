
package com.gmeci.atskservice.form;

import com.gmeci.atskservice.R;

import com.gmeci.conversions.Conversions;

import com.gmeci.atskservice.form.formobjects.TextField;
import com.gmeci.atskservice.form.formobjects.CheckField;
import com.gmeci.atskservice.form.formobjects.RadioField;
import com.gmeci.atskservice.form.formobjects.FormObject;
import com.gmeci.atskservice.form.formobjects.TriCheckField;
import com.gmeci.atskservice.form.formobjects.ScreenHint;
import com.gmeci.atskservice.form.formobjects.NestBlock;
import com.gmeci.atskservice.form.formobjects.SimpleNameValue;
import com.gmeci.atskservice.form.formobjects.Block;
import com.gmeci.atskservice.form.formobjects.Grid;
import com.gmeci.atskservice.form.formobjects.Label;
import com.gmeci.atskservice.form.formobjects.Form;

import java.util.*;

public class DZForm extends AbstractFormActivity {

    FormObject remarks;

    // assumes JPADS point of impact is the center of the DZ and makes into an
    // FE compliant string
    public String getJPADSPI() {
        String CenterString;
        CenterString = String.format("%s %s %.1fft hae", Conversions.GetMGRS(
                survey.center.lat, survey.center.lon),
                Conversions.GetLatLonDM(survey.center.lat,
                        survey.center.lon),
                survey.center.getHAE() * Conversions.M2F);
        return CenterString;
    }

    public Form createForm() {

        FormObject[] si = new FormObject[] {
                new SimpleNameValue(c, "siteName", "1A. DZ Name"),
                new SimpleNameValue(c, "zarIndexNo", "1B. ZAR Index No."),
                new SimpleNameValue(c, "countryName", "2A. Country"),
                new SimpleNameValue(c, "state", "2B. State"),
                new SimpleNameValue(c, "mapSeriesExtended",
                        "3. Map Series/Sheet Numbeer/Edition/Date of Map")
        };
        FormObject survey_info_block =
                new Block(c, "dz_survey_information",
                        "Survey Information", si);

        FormObject[] sur = new FormObject[] {
                new SimpleNameValue(c, "surveyDate", "4A1. Date Surveyed"),
                new SimpleNameValue(c, "surveyorNameGradeService",
                        "4A2. Type Name and Grade of Surveyor"),
                new SimpleNameValue(c, "surveyorPhone",
                        "4A3. Phone Number (DSN)"),
                new SimpleNameValue(c, "surveyorUnit", "4A4. Unit"),
        };

        FormObject surveyor_info_block =
                new Block(c, "dz_surveyor", "Surveyor", sur);

        FormObject[] subapproval = new FormObject[] {
                new Label(c, -1, "Approval Day"),
                new Label(c, -1, "Approval Night"),
                new TriCheckField(c, "approvedDayCds", "CDS/CRL/CRS"),
                new TriCheckField(c, "approvedNightCds", "CDS/CRL/CRS"),
                new TriCheckField(c, "approvedDayPer", "PER"),
                new TriCheckField(c, "approvedNightPer", "PER"),
                new TriCheckField(c, "approvedDayHe", "HE"),
                new TriCheckField(c, "approvedNightHe", "HE"),
                new TriCheckField(c, "approvedDayMff", "MFF"),
                new TriCheckField(c, "approvedNightMff", "MFF"),
                new TriCheckField(c, "approvedDaySatb", "SATB"),
                new TriCheckField(c, "approvedNightSatb", "SATB"),
                new TriCheckField(c, "approvedDayCrrc", "CRRC"),
                new TriCheckField(c, "approvedNightCrrc", "CRRC"),
                new TriCheckField(c, "approvedDayHsllads", "HSLLADS"),
                new TriCheckField(c, "approvedNightHsllads", "HSLLADS"),
                new TriCheckField(c, "approvedDayHvcds", "HVCDS"),
                new TriCheckField(c, "approvedNightHvcds", "HVCDS")
        };

        FormObject[] approvals = new FormObject[] {
                new Grid(c, subapproval, 2)
        };

        /*FormObject approvals_info_block =
                new Block(c, "dz_approvals",
                        "4B. Drop Zone Approval/Disaproval", approvals);*/

        FormObject[] groundOp = new FormObject[] {
                new SimpleNameValue(c, "groundApprovalDate",
                        "4C. Date Approved for Ground Operations"),
                new SimpleNameValue(c, "groundApprovalNameGradeService",
                        "Name Grade and Service of Approval Authority"),
                new SimpleNameValue(c, "groundApprovalPhone",
                        "Phone Number (DSN)"),
                new SimpleNameValue(c, "groundApprovalUnitLocation",
                        "Unit And Location"),
        };

        FormObject ground_op_block =
                new Block(c, "dz_ground_operations", "Ground Operations",
                        groundOp);

        FormObject[] ro = new FormObject[] {
                new SimpleNameValue(c, "safetyApprovalDate",
                        "4D. Date Safety of Flight Review Approved"),
                new SimpleNameValue(c, "flightApprovalNameGradeService",
                        "Name Grade and Service of Reviewing Officer"),

                new SimpleNameValue(c, "flightApprovalPhone",
                        "Phone Number (DSN)"),
                new SimpleNameValue(c, "flightApprovalUnitLocation",
                        "Unit And Location")
        };

        FormObject ro_op_block =
                new Block(c, "dz_reviewing_officer", "Reviewing Officer", ro);

        FormObject[] majcom = new FormObject[] {
                new SimpleNameValue(c, "majcomApprovalDate",
                        "4E. Date of MAJCOM Approval"),
                new SimpleNameValue(c, "majcomApprovalNameGradeService",
                        "Name Grade and Service of Approving Authority"),

                new SimpleNameValue(c, "majcomApprovalPhone",
                        "Phone Number (DSN)"),
                new SimpleNameValue(c, "majcomApprovalUnitLocation",
                        "Unit And Location")
        };

        FormObject majcom_op_block =
                new Block(c, "dz_majcom", "MAJCOM", majcom);

        FormObject[] coordinating_block = new FormObject[] {
                new SimpleNameValue(c, "controllingAgencyUnit",
                        "5A. DZ Controlling Agency or Unit"),
                new SimpleNameValue(c, "controllingAgencyPhone",
                        "5C. Phone Number"),
                new SimpleNameValue(c, "rangeControl", "5D. Range Control"),
                new SimpleNameValue(c, "rangeControlPhone",
                        "5E. Range Control Phone")
        };
        FormObject coordinating_op_block =
                new Block(c, "dz_coordinating_activies",
                        "Coordinating Activities", coordinating_block);

        remarks = buildRemarksBlock();

        Form dz = new Form(c, new FormObject[] {
                survey_info_block,
                surveyor_info_block,
                //approvals_info_block,
                ground_op_block,
                ro_op_block,
                majcom_op_block,
                coordinating_op_block,
                remarks
        });

        return dz;
    }

    private FormObject buildRemarksBlock() {
        // initialize the preference value and then construct the handler 
        initMag();

        addHandler(new TextField(c, "dzDayNight", 10, "Day Night Operation"));

        addHandler(new RadioField(c, "dzPassingCoordElevation", 12,
                lusa(R.array.dzpassing), "Passing"));

        record("dzJpadsPi", getJPADSPI());
        addHandler(new TextField(c, "dzJpadsPi", -1, "JPADs PI"));

        List<FormObject> list = new ArrayList<FormObject>();

        list.add(new ScreenHint.VisualLabel(c, "Statements"));

        list.add(new RadioField(c, "dzStatement", -1,
                lusa(R.array.dzstmt), "Statements"));

        list.add(new Label(c, -1, lus(R.string.dzline0)));

        list.add(new NestBlock(c, "hlzline1", parseLine(R.string.dzline1),
                new FormObject[] {
                        parseLine(R.string.dzline1a),
                        parseLine(R.string.dzline1b)
                }));

        list.add(new SimpleNameValue(c, "dzLine2notes",
                lus(R.string.dzline2)));

        list.add(new NestBlock(c, "dzline3",
                new Label(c, -1, lus(R.string.dzline3)),
                new FormObject[] {
                        new Label(c, -1, lus(R.string.dzline3a)),
                        new CheckField(c, "dzObtaclesInside", 10,
                                concat(getInsideHazards(),
                                        lusa(R.array.hazards)),
                                "Obstacles Inside"),
                        new Label(c, -1, lus(R.string.dzline3b)),
                        new CheckField(c, "dzObstaclesOutside", 10,
                                concat(getOutsideHazards(),
                                        lusa(R.array.hazards)),
                                "Obstacles Outside")
                }));

        list.add(new NestBlock(c, "dzline4", new Label(c, -1,
                lus(R.string.dzline4)),
                new FormObject[] {
                        new CheckField(c, "dzObstaclesTerrain", 10,
                                lusa(R.array.hazards), "Terrain Hazards")
                }));
        list.add(new NestBlock(c, "dzline5", new Label(c, -1,
                lus(R.string.dzline5)),
                new FormObject[] {
                        new Label(c, -1, lus(R.string.dzline5a)),
                        new CheckField(c, "dzObstaclesObserved", 10,
                                lusa(R.array.hazards), "Hazards"),
                        new SimpleNameValue(c, "dzLine5b",
                                lus(R.string.dzline5b))
                }));

        list.add(new SimpleNameValue(c, "dzLine6Notes", lus(R.string.dzline6)));

        list.add(new NestBlock(c, "dzline7", new Label(c, -1,
                lus(R.string.dzline7)),
                new FormObject[] {
                        parseLine(R.string.dzline7a),
                        parseLine(R.string.dzline7b),
                        parseLine(R.string.dzline7c)
                }));

        FormObject[] r = new FormObject[list.size()];
        list.toArray(r);

        FormObject remarks_block =
                new Block(c, "dz_remarks_block", "Remarks Block", r);
        return remarks_block;

    }

    @Override
    public void generateBlocks() {
        record("remarks", remarks.getText());
    }
}
