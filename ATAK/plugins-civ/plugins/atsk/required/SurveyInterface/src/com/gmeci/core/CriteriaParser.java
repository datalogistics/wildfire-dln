
package com.gmeci.core;

import android.os.Environment;

import com.gmeci.conversions.Conversions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.ZipException;

public class CriteriaParser {

    //Criteria parser fields
    private static final String LZ_PARKING_BOUNDARY_WIDTH = "\"Parking Boundary Width\"";
    private static final String LZ_PARKING_BOUNDARY_LENGTH = "\"Parking Boundary Length\"";
    private static final String LZ_TAXIWAY_WIDTH = "\"Taxiway Width\"";

    private static final String LZ_RUNWAY_LENGTH = "\"Runway Length\"";
    private static final String LZ_OVERALL_LONGITUDINAL = "\"Overall Longitudinal\"";
    private static final String LZ_INCREMENTAL_LONGITUDINAL = "\"Incremental Longitudinal\"";

    private static final String LZ_RUNWAY_WIDTH = "\"Runway Width\"";
    private static final String LZ_RUNWAY_TRANSVERSE_POS_MAX = "\"Runway Transverse +MAX\"";
    private static final String LZ_RUNWAY_TTRANSVERSE_POS_MIN = "\"Runway Transverse +MIN\"";
    private static final String LZ_RUNWAY_TTRANSVERSE_NEG_MIN = "\"Runway Transverse -MIN\"";
    private static final String LZ_RUNWAY_TTRANSVERSE_NEG_MAX = "\"Runway Transverse -MAX\"";

    private static final String LZ_RUNWAY_SHOULDER = "\"Runway Shoulder\"";
    private static final String LZ_RUNWAY_SHOULDER_OBSTACLE_HT = "\"Runway Shoulder Obstacle Ht\"";
    private static final String LZ_SHOULDER_TRANSVERSE_POS_MAX = "\"Shoulder Transverse +MAX\"";
    private static final String LZ_SHOULDER_TRANSVERSE_POS_MIN = "\"Shoulder Transverse +MIN\"";
    private static final String LZ_SHOULDER_TRANSVERSE_NEG_MIN = "\"Shoulder Transverse -MIN\"";
    private static final String LZ_SHOULDER_TRANSVERSE_NEG_MAX = "\"Shoulder Transverse -MAX\"";

    private static final String LZ_GRADED_AREA = "\"Graded Area\"";
    private static final String LZ_GRADED_AREA_OBSTACLE_HT = "\"Graded Area Obstacle Ht\"";
    private static final String LZ_GRADED_TRANSVERSE_POS_MAX = "\"Graded Transverse +MAX\"";
    private static final String LZ_GRADED_TRANSVERSE_POS_MIN = "\"Graded Transverse +MIN\"";
    private static final String LZ_GRADED_TRANSVERSE_NEG_MIN = "\"Graded Transverse -MIN\"";
    private static final String LZ_GRADED_TRANSVERSE_NEG_MAX = "\"Graded Transverse -MAX\"";

    private static final String LZ_MAINTAINTED_AREA = "\"Maintained Area\"";
    private static final String LZ_MAINTAINTED_OBSTACLE_HT = "\"Maintained Area Obstacle Ht\"";
    private static final String LZ_MAINTAINTED_TRANSVERSE_POS_MAX = "\"Maintained Transverse +MAX\"";
    private static final String LZ_MAINTAINTED_TRANSVERSE_POS_MIN = "\"Maintained Transverse +MIN\"";
    private static final String LZ_MAINTAINTED_TRANSVERSE_NEG_MIN = "\"Maintained Transverse -MIN\"";
    private static final String LZ_MAINTAINTED_TRANSVERSE_NEG_MAX = "\"Maintained Transverse -MAX\"";

    private static final String LZ_APPROACH_OVERRUN_LENGTH = "\"Approach Overrun Length\"";
    private static final String LZ_DEPARTURE_OVERRUN_LENGTH = "\"Departure Overrun Length\"";
    private static final String LZ_OVERRUN_SHOULDER = "\"Overrun Shoulder\"";
    private static final String LZ_OVERRUN_WIDTH = "\"Overrun Width\"";

    private static final String LZ_END_CLEAR_ZONE_LENGTH = "\"End Clear Zone Length\"";
    private static final String LZ_END_CLEAR_ZONE_INNER_WIDTH = "\"End Clear Zone Inner Width\"";
    private static final String LZ_END_CLEAR_ZONE_OUTER_WIDTH = "\"End Clear Zone Outer Width\"";

    private static final String LZ_INNER_APPROACH_ZONE_LENGTH = "\"Inner Approach Zone Length\"";
    private static final String LZ_INNER_APPROACH_ZONE_WIDTH = "\"Inner Approach Zone Width\"";
    private static final String LZ_OUTER_APPROACH_ZONE_LENGTH = "\"Outer Approach Zone Length\"";
    private static final String LZ_OUTER_APPROACH_ZONE_WIDTH = "\"Outer Approach Zone Width\"";

    private static final String LZ_APPROACH_GLIDE_SLOPE = "\"Approach Glide Slope\"";
    private static final String LZ_DEPARETURE_GLIDE_SLOPE = "\"Departure Glide Slope\"";

    private static final String LZ_DCP_OFFSET = "\"DCP Offset\"";
    private static final String LZ_DCP_SPACING_PERCENTAGE = "\"DCP Spacing Percentage\"";
    private static final String LZ_DCP_1500_LESS = "\"1500 or Less\"";
    private static final String LZ_DCP_1501_3000 = "\"1501 to 3000\"";
    private static final String LZ_DCP_3000_MORE = "\"3000 or More\"";

    private static final String AIRCRAFT_TXT = "Aircraft.txt";
    private static final String ATSK_AZ_DATA_FOLDER_BASE = "/atsk/az_templates";//MIKE might want to consider moving this to az_data
    public static final ArrayList<Criteria> mAircraftInfo = new ArrayList<Criteria>();
    public static final ArrayList<String> ACNames = new ArrayList<String>();

    public void parseFile() throws IOException {
        BufferedReader reader = null;

        try {
            File zipFile = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath()
                    + ATSK_AZ_DATA_FOLDER_BASE
                    + "/"
                    + AIRCRAFT_TXT);

            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(zipFile)));

            //reader = new BufferedReader (new FileReader(acfile));
            Criteria curAC = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                //Leading parentheses
                if (line.equals("("))
                    continue;

                //End Criteria block
                else if (line.equals(")")) {
                    if (curAC != null) {
                        if (!ACNames.contains(curAC.Name)) {
                            mAircraftInfo.add(curAC);
                            ACNames.add(curAC.Name);
                        }
                    }
                }

                //New Criteria block
                else if (!line.contains(")")) {
                    curAC = new Criteria();
                    String[] splitline = line.split("\"");
                    curAC.Name = splitline[1];
                }

                //New data
                else {
                    if (curAC != null) {
                        if (line.contains(LZ_TAXIWAY_WIDTH))
                            curAC.setTaxiwayWidth((float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F));
                        else if (line.contains(LZ_PARKING_BOUNDARY_WIDTH))
                            curAC.setParkingBoundaryWidth((float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F));
                        else if (line.contains(LZ_PARKING_BOUNDARY_LENGTH))
                            curAC.setParkingBoundaryLength((float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F));
                        else if (line.contains(LZ_RUNWAY_LENGTH))
                            curAC.RunwayLength_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_OVERALL_LONGITUDINAL)) {
                            curAC.OverallLongitudinal = Float
                                    .parseFloat(getValueString(line));
                            if (curAC.IncrementalLongitudinal >= Criteria.MAX_INCREMENTAL_LONGITUDINAL) {
                                curAC.IncrementalLongitudinal = curAC.OverallLongitudinal;
                            }
                        } else if (line.contains(LZ_INCREMENTAL_LONGITUDINAL))
                            curAC.IncrementalLongitudinal = Float
                                    .parseFloat(getValueString(line));

                        else if (line.contains(LZ_RUNWAY_WIDTH))
                            curAC.RunwayWidth_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_RUNWAY_TRANSVERSE_POS_MAX))
                            curAC.RunwayTransversePosMAX = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_RUNWAY_TTRANSVERSE_POS_MIN))
                            curAC.RunwayTransversePosMIN = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_RUNWAY_TTRANSVERSE_NEG_MAX))
                            curAC.RunwayTransverseNegMAX = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_RUNWAY_TTRANSVERSE_NEG_MIN))
                            curAC.RunwayTransverseNegMIN = Float
                                    .parseFloat(getValueString(line));

                        else if (line.contains(LZ_RUNWAY_SHOULDER))
                            curAC.RunwayShoulderWidth_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_RUNWAY_SHOULDER_OBSTACLE_HT))
                            curAC.RunwayShoulderObstacleMaxHeight_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_SHOULDER_TRANSVERSE_POS_MAX))
                            curAC.ShoulderTransversePosMAX = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_SHOULDER_TRANSVERSE_POS_MIN))
                            curAC.ShoulderTransversePosMIN = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_SHOULDER_TRANSVERSE_NEG_MAX))
                            curAC.ShoulderTransverseNegMAX = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_SHOULDER_TRANSVERSE_NEG_MIN))
                            curAC.ShoulderTransverseNegMIN = Float
                                    .parseFloat(getValueString(line));

                        else if (line.contains(LZ_GRADED_AREA))
                            curAC.GradedAreaWidth_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_GRADED_AREA_OBSTACLE_HT))
                            curAC.GradedAreaObstacleMaxHeight = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_GRADED_TRANSVERSE_POS_MAX))
                            curAC.GradedTransversePosMAX = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_GRADED_TRANSVERSE_POS_MIN))
                            curAC.GradedTransversePosMIN = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_GRADED_TRANSVERSE_NEG_MAX))
                            curAC.GradedTransverseNegMAX = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_GRADED_TRANSVERSE_NEG_MIN))
                            curAC.GradedTransverseNegMIN = Float
                                    .parseFloat(getValueString(line));

                        else if (line.contains(LZ_MAINTAINTED_AREA))
                            curAC.MaintainedAreaWidth_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_MAINTAINTED_OBSTACLE_HT))
                            curAC.MaintainedObstacleMaxHeight = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line
                                .contains(LZ_MAINTAINTED_TRANSVERSE_POS_MAX))
                            curAC.MaintainedTransversePosMAX = Float
                                    .parseFloat(getValueString(line));
                        else if (line
                                .contains(LZ_MAINTAINTED_TRANSVERSE_POS_MIN))
                            curAC.MaintainedTransversePosMIN = Float
                                    .parseFloat(getValueString(line));
                        else if (line
                                .contains(LZ_MAINTAINTED_TRANSVERSE_NEG_MIN))
                            curAC.MaintainedTransverseNegMIN = Float
                                    .parseFloat(getValueString(line));
                        else if (line
                                .contains(LZ_MAINTAINTED_TRANSVERSE_NEG_MAX))
                            curAC.MaintainedTransverseNegMAX = Float
                                    .parseFloat(getValueString(line));

                        else if (line.contains(LZ_APPROACH_OVERRUN_LENGTH))
                            curAC.ApproachOverrunLength_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_DEPARTURE_OVERRUN_LENGTH))
                            curAC.DepartureOverrunLength_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_OVERRUN_SHOULDER))
                            curAC.OverrunShoulder_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_OVERRUN_WIDTH))
                            curAC.OverrunWidth_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);

                        else if (line.contains(LZ_END_CLEAR_ZONE_LENGTH))
                            curAC.EndClearZoneLength_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_END_CLEAR_ZONE_INNER_WIDTH))
                            curAC.EndClearZoneInnerWidth_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_END_CLEAR_ZONE_OUTER_WIDTH))
                            curAC.EndClearZoneOuterWidth_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);

                        else if (line.contains(LZ_INNER_APPROACH_ZONE_LENGTH))
                            curAC.InnerApproachZoneLength_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_INNER_APPROACH_ZONE_WIDTH))
                            curAC.InnerApproachZoneWidth_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_OUTER_APPROACH_ZONE_LENGTH))
                            curAC.OuterApproachZoneLength_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_OUTER_APPROACH_ZONE_WIDTH))
                            curAC.OuterApproachZoneWidth_m = (float) (Float
                                    .parseFloat(getValueString(line)) / Conversions.M2F);
                        else if (line.contains(LZ_APPROACH_GLIDE_SLOPE))
                            curAC.ApproachGlideSlope_deg = Conversions
                                    .ConvertGlideSlopeRatioToAngle_deg(getValueString(line));
                        else if (line.contains(LZ_DEPARETURE_GLIDE_SLOPE))
                            curAC.DepartureGlideSlope_deg = Conversions
                                    .ConvertGlideSlopeRatioToAngle_deg(getValueString(line));
                        else if (line.contains(LZ_DCP_OFFSET))
                            curAC.DCPOffset = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_DCP_SPACING_PERCENTAGE))
                            curAC.DCPSpacingPercentage = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_DCP_1500_LESS))
                            curAC.DCP_1500_LESS = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_DCP_1501_3000))
                            curAC.DCP_1501_3000 = Float
                                    .parseFloat(getValueString(line));
                        else if (line.contains(LZ_DCP_3000_MORE))
                            curAC.DCP_3000_MORE = Float
                                    .parseFloat(getValueString(line));

                    }
                }//end new data
            }//end loop

        } catch (ZipException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (Exception e) {
                }
        }
    }

    private String getValueString(String line) {
        String valString = "";
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'') {
                String croppedline = line.substring(i);
                String[] splitline = croppedline.split(" ");
                valString = splitline[splitline.length - 1].replace(")", "");
                return valString;
            }
        }
        return valString;
    }

    public ArrayList<Criteria> GetAircraftList() {
        return mAircraftInfo;
    }
}
