
package com.gmeci.csvparser;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

public class RealParser {

    private static final int AIRCRAFT = 0;
    private static final int NIGHTHE = 10;
    private static final int HE = 9;
    private static final int NIGHTPER = 8;
    private static final int PER = 7;
    private static final int NIGHTCDS = 6;
    private static final int CDS = 5;
    private static final int AIRCRAFT_ROW = 0;
    private static final int SIZE_CRITERIA_ROW = 2;
    private static final int CONTAINER_ROW = 1;
    private static final String WIDTH = "width";
    private static final String LENGTH = "length";
    File extStore;

    public ArrayList<DZRequirements> parseFile(String filename)
            throws IOException {

        //Instantiate your list
        ArrayList<DZRequirements> dzreqsList = new ArrayList<DZRequirements>();

        //Find the file on the "external storage" on the device
        extStore = Environment.getExternalStorageDirectory();

        File zipFile = new File(extStore.getAbsolutePath() + filename);

        InputStreamReader isr = new InputStreamReader(new FileInputStream(
                zipFile));

        CSVReader csvReader = new CSVReader(isr);
        //Skip the header
        csvReader.readNext();

        //Parse and fill the list properly according to the csv files forma
        ParseDZRequirements(dzreqsList, csvReader);

        csvReader.close();
        return dzreqsList;

    }

    private void ParseDZRequirements(ArrayList<DZRequirements> dzreqsList,
            CSVReader csvReader) throws IOException {
        String[] row;
        DZRequirements newAircraft = null;
        ContainerType newContainer = null;
        SizeCriteria newSizeCriteria = null;
        //row = csvReader.readNext();
        while ((row = csvReader.readNext()) != null) {
            //handles a new aircrafts drop data
            if (row[AIRCRAFT_ROW].length() > 0) {
                if (newAircraft != null) {
                    newAircraft.containers.add(newContainer);
                    dzreqsList.add(newAircraft);
                    newContainer = null;
                    newSizeCriteria = null;
                    //printReqs(dzreqsList);
                }
                newAircraft = new DZRequirements();
                newAircraft.DZ_Aircraft = row[AIRCRAFT];
                newAircraft.pi_cds_m = Integer.parseInt(row[CDS]);
                newAircraft.pi_nightcds_m = Integer.parseInt(row[NIGHTCDS]);
                newAircraft.pi_per_m = Integer.parseInt(row[PER]);
                newAircraft.pi_nightper_m = Integer.parseInt(row[NIGHTPER]);
                newAircraft.pi_he_m = Integer.parseInt(row[HE]);
                newAircraft.pi_nighthe_m = Integer.parseInt(row[NIGHTHE]);
                dzreqsList.add(newAircraft);
            }
            //handles a new container
            else if (row[CONTAINER_ROW].length() > 0) {
                if (newContainer != null) {
                    newContainer.AddToDataList(newSizeCriteria);
                    if (newAircraft != null)
                        newAircraft.AddToContainerList(newContainer);
                    newSizeCriteria = null;
                }
                newContainer = new ContainerType();
                newContainer.Type = row[CONTAINER_ROW];
            }
            //handles a new set of data
            else if (row[SIZE_CRITERIA_ROW].length() > 0) {
                if (newContainer != null && newSizeCriteria != null)
                    newContainer.AddToDataList(newSizeCriteria);

            } else {
                //handles the bulk of data entry into newData
                newSizeCriteria = new SizeCriteria();
                if (row[3].equalsIgnoreCase(LENGTH))
                    newSizeCriteria.length = Integer.parseInt(row[4]);
                if (row[3].equalsIgnoreCase(WIDTH))
                    newSizeCriteria.width = Integer.parseInt(row[4]);
                if (row[3].equalsIgnoreCase("nightlength"))
                    newSizeCriteria.nightlength = Integer.parseInt(row[4]);
                if (row[3].equalsIgnoreCase("nightwidth"))
                    newSizeCriteria.nightwidth = Integer.parseInt(row[4]);
                if (row[3].equalsIgnoreCase("minalt"))
                    newSizeCriteria.minalt = Integer.parseInt(row[4]);
                if (row[3].equalsIgnoreCase("lengthmod"))
                    newSizeCriteria.lengthmod = Integer.parseInt(row[4]);
                if (row[3].equalsIgnoreCase("widthmod"))
                    newSizeCriteria.widthmod = Integer.parseInt(row[4]);
                if (row[3].equalsIgnoreCase("altchange"))
                    newSizeCriteria.altchange = Integer.parseInt(row[4]);
                if (newContainer != null)
                    newContainer.AddToDataList(newSizeCriteria);
            }
        }
        if (newAircraft != null) {
            newAircraft.containers.add(newContainer);
            dzreqsList.add(newAircraft);
        }
    }

    public void printReqs(ArrayList<DZRequirements> list) {
        print("=============");
        for (DZRequirements reqs : list)
            print(reqs.toString(true, true));
    }

    protected void print(String str) {
        System.out.println(str);
    }
}
