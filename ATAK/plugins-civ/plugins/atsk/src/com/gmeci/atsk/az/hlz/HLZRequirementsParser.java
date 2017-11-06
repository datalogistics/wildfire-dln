
package com.gmeci.atsk.az.hlz;

import android.os.Environment;

import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.csvparser.CSVReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import com.atakmap.coremap.locale.LocaleUtil;
import java.io.UnsupportedEncodingException;

import com.atakmap.coremap.log.Log;

public class HLZRequirementsParser {

    private static final String TAG = "HLZRequirementsParser";

    private static final String VERSION_STRING = "version";
    private static final int HELICOPTER_NAME_POSTIION = 0;
    private static final int ROTOR_DIAMETER_POSITION = 1;
    private static final int OUTER_DIAMETER_POSITION = 2;
    private static final int TRAINING_LENGTH_POSITION = 3;
    private static final int TRAINING_WIDTH_POSITION = 4;
    private static final int CONTINGENCY_LENGTH_POSITION = 5;
    private static final int CONTINGENCY_WIDTH_POSITION = 6;
    private static final int BROWNOUT_LENGTH_POSITION = 7;
    private static final int BROWNOUT_WIDTH_POSITION = 8;
    private static final int UNITS_POSITION = 9;
    public String VersionString;

    public ArrayList<HelicopterRequirements> parseFile(String filename) {

        final String ATSK_AZ_DATA_FOLDER_BASE = "/atsk/az_templates";

        //Instantiate your list
        ArrayList<HelicopterRequirements> hlzreqsList = new ArrayList<HelicopterRequirements>();

        File zipFile;
        InputStreamReader isr = null;
        try {
            zipFile = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath()
                    + ATSK_AZ_DATA_FOLDER_BASE
                    + "/"
                    + filename);

            isr = new InputStreamReader(new FileInputStream(
                    zipFile), "UTF-8");

            CSVReader csvReader = new CSVReader(isr);

            //Skip the header
            try {
                String FirstLine[] = csvReader.readNext();
                if (FirstLine != null) {
                    ParseVersionString(FirstLine);
                    hlzreqsList = ParseHLZRequirements(csvReader);
                }
                csvReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
            return new ArrayList<HelicopterRequirements>();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return new ArrayList<HelicopterRequirements>();
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                }
            }
        }
        return hlzreqsList;

    }

    private void ParseVersionString(String[] firstLine) {
        for (String firstLineToken : firstLine) {
            if (firstLineToken.contains(VERSION_STRING)) {
                VersionString = firstLineToken.replace(VERSION_STRING, "");
                Log.d(TAG, "reading HLZ requirements version: " + VersionString);
            }
        }

    }

    private ArrayList<HelicopterRequirements> ParseHLZRequirements(
            CSVReader csvReader) throws IOException {

        ArrayList<HelicopterRequirements> heliList = new ArrayList<HelicopterRequirements>();
        String[] currentRow;
        HelicopterRequirements currentHelicopter = null;

        while ((currentRow = csvReader.readNext()) != null) {
            //handles the columns
            if (currentRow.length >= UNITS_POSITION + 1) {

                currentHelicopter = new HelicopterRequirements();

                if (currentRow[UNITS_POSITION].length() > 0)
                    currentHelicopter.Units = currentRow[UNITS_POSITION];

                Unit srcUnit = (currentHelicopter.Units.toLowerCase(
                        LocaleUtil.getCurrent())
                        .startsWith("m") ? Unit.METER : Unit.FOOT);

                if (currentRow[HELICOPTER_NAME_POSTIION].length() > 0)
                    currentHelicopter.HeliName = currentRow[HELICOPTER_NAME_POSTIION];
                if (currentRow[TRAINING_LENGTH_POSITION].length() > 0)
                    currentHelicopter.trainingLength_m =
                            srcUnit.convertTo(Float.parseFloat(
                                    currentRow[TRAINING_LENGTH_POSITION]),
                                    Unit.METER);
                if (currentRow[TRAINING_WIDTH_POSITION].length() > 0)
                    currentHelicopter.trainingWidth_m = srcUnit.convertTo(Float
                            .parseFloat(currentRow[TRAINING_WIDTH_POSITION]),
                            Unit.METER);
                if (currentRow[CONTINGENCY_LENGTH_POSITION].length() > 0)
                    currentHelicopter.contingencyLength_m = srcUnit
                            .convertTo(
                                    Float
                                            .parseFloat(currentRow[CONTINGENCY_LENGTH_POSITION]),
                                    Unit.METER);
                if (currentRow[CONTINGENCY_WIDTH_POSITION].length() > 0)
                    currentHelicopter.contingencyWidth_m = srcUnit
                            .convertTo(
                                    Float
                                            .parseFloat(currentRow[CONTINGENCY_WIDTH_POSITION]),
                                    Unit.METER);
                if (currentRow[BROWNOUT_LENGTH_POSITION].length() > 0)
                    currentHelicopter.brownoutLength_m = srcUnit
                            .convertTo(
                                    Float
                                            .parseFloat(currentRow[BROWNOUT_LENGTH_POSITION]),
                                    Unit.METER);
                if (currentRow[BROWNOUT_WIDTH_POSITION].length() > 0)
                    currentHelicopter.brownoutWidth_m =
                            srcUnit.convertTo(Float.parseFloat(
                                    currentRow[BROWNOUT_WIDTH_POSITION]),
                                    Unit.METER);
            }
            if (currentHelicopter != null) {
                heliList.add(currentHelicopter);
                currentHelicopter = null;
            }
        }
        return heliList;
    }

}
