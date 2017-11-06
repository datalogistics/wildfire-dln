
package com.gmeci.atskservice.farp;

import android.os.Environment;
import android.os.RemoteException;

import com.gmeci.conversions.Conversions;
import com.gmeci.csvparser.CSVReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class FARPACParser {
    static final String FARP_DEFINITION_FILE_NAME = "/atsk/az_templates/farp_ac.csv";

    private static final int AC_NAME_POSTIION = 0;
    private static final int BLOCK_NAME_POSTIION = 1;
    private static final int MAX_FUEL_POSITION = 2;
    private static final int START_ANGLE_POSITION = 3;
    private static final int END_ANGLE_POSITION = 4;
    private static final int MIN_FAM_RANGE_POSITION = 5;
    private static final int MIN_RX_RANGE_POSITION = 6;
    private static final int FUEL_POINT_OFFSET_M_POSITION = 7;
    private static final int NOSEWHEEL_OFFSET_M_POSITION = 8;
    private static final int FUEL_CENTERLINE_OFFSET_M_POSITION = 9;
    private static final int RGR_CENTERLINE_OFFSET_M = 10;
    private static final int RGR_WING_OFFSET_M = 11;
    private static final String TAG = "FARPACParser";
    private String Version;

    public FARPACParser() {
    }

    public String getVersion() {
        return Version;
    }

    public Map<String, FARPTankerItem> parseFile() throws IOException,
            RemoteException {

        //Instantiate your list
        Map<String, FARPTankerItem> tankerList;
        File file = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + FARP_DEFINITION_FILE_NAME);

        InputStreamReader isr = new InputStreamReader(new FileInputStream(
                file));

        CSVReader csvReader = new CSVReader(isr);
        //Skip the header
        String VersionRow[] = csvReader.readNext();
        if (VersionRow != null) {
            for (String VersionRowToken : VersionRow) {
                if (VersionRowToken.contains("version")) {
                    this.Version = VersionRowToken.replace("version", "");
                }
            }
        }
        //Parse and fill the list properly according to the csv files forma
        tankerList = ParseFARPRequirements(csvReader);

        csvReader.close();
        return tankerList;

    }

    private Map<String, FARPTankerItem> ParseFARPRequirements(
            CSVReader csvReader)
            throws IOException {

        Map<String, FARPTankerItem> tankers = new HashMap<String, FARPTankerItem>();
        String[] row;
        FARPTankerItem currentTanker = null;

        while ((row = csvReader.readNext()) != null) {
            //handles the columns
            if (row.length >= FUEL_CENTERLINE_OFFSET_M_POSITION) {
                if (!row[AC_NAME_POSTIION].isEmpty()) {
                    currentTanker = new FARPTankerItem(
                            row[AC_NAME_POSTIION]);
                } else if (currentTanker == null)
                    continue;
                if (!row[BLOCK_NAME_POSTIION].isEmpty())
                    currentTanker.BlockFileName = row[BLOCK_NAME_POSTIION];
                if (!row[MAX_FUEL_POSITION].isEmpty())
                    currentTanker.MaxFuelPoints = Integer
                            .parseInt(row[MAX_FUEL_POSITION]);
                if (!row[START_ANGLE_POSITION].isEmpty())
                    currentTanker.StartAngle = Float
                            .parseFloat(row[START_ANGLE_POSITION]);
                if (!row[END_ANGLE_POSITION].isEmpty())
                    currentTanker.EndAngle = Float
                            .parseFloat(row[END_ANGLE_POSITION]);
                currentTanker.FuelPointOffset_m = parseFt(
                        row[FUEL_POINT_OFFSET_M_POSITION]);
                currentTanker.FuelCenterlineOffset_m = parseFt(
                        row[FUEL_CENTERLINE_OFFSET_M_POSITION]);
            }
            if (currentTanker == null)
                continue;
            if (row.length >= RGR_WING_OFFSET_M) {
                currentTanker.rgrCenterlineOffset = parseFt(
                        row[RGR_CENTERLINE_OFFSET_M]);
                currentTanker.rgrWingOffset = parseFt(
                        row[RGR_WING_OFFSET_M]);
            }
            tankers.put(currentTanker.Name, currentTanker);
        }
        return tankers;
    }

    private static float parseFt(String value) {
        if (value != null && !value.isEmpty()) {
            try {
                return (float) (Float.parseFloat(value) / Conversions.M2F);
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

}
