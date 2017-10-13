
package com.gmeci.atskservice.dz;

import android.os.Environment;
import android.util.Log;

import com.gmeci.csvparser.CSVReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by smetana on 7/3/2014.
 */
public class DZRequirementsParser extends DZCapabilitiesBase {

    private List<String[]> reqFile;

    public DZRequirementsParser() {
        jpadsCriteria = new JPADSCriteria();
        cdsCriteria = new CDSCriteria();
        perCriteria = new SizeCriteria();
        heCriteria = new SizeCriteria();
        mffCriteria = new SizeCriteria();
        satbCriteria = new SizeCriteria();
        crrcCriteria = new SizeCriteria();
        hslladsCriteria = new SizeCriteria();
        hvcdsCriteria = new SizeCriteria();
    }

    public String getVersion() {
        setRequirements(0);
        return this.Version;
    }

    public void parseRequirementsFile() throws IOException {
        //Find the file on the "external storage" on the device
        File extStore = Environment.getExternalStorageDirectory();
        File zipFile = new File(extStore.getAbsolutePath()
                + DZ_DEFINITION_FILE_NAME);
        InputStreamReader isr = new InputStreamReader(new FileInputStream(
                zipFile));

        CSVReader csvReader = new CSVReader(isr);
        reqFile = csvReader.readAll();
        loadAircraftColumnConstants(reqFile.get(0));

        csvReader.close();
    }

    private void loadAircraftColumnConstants(String[] firstLine) {
        //firstline[0] will = DATA
        aircraft = new ArrayList<String>();
        aircraft.addAll(Arrays.asList(firstLine).subList(1, firstLine.length));
        Log.d(TAG, "aircraft list is ready");
    }

    private void setRequirements(int ac) {

        // dz types with arrays need to be reset.....
        jpadsCriteria = new JPADSCriteria();
        cdsCriteria = new CDSCriteria();

        ac++;//forgot to account for the difference between the list order and the column order.

        for (int i = 1; i < reqFile.size(); i++) {
            String[] row = reqFile.get(i);
            if (row[ac].isEmpty())
                continue;

            if (!isNumeric(row[ac])) {
                if (row[ac].equals(UNKNOWN))
                    row[ac] = String.valueOf(JPADS_UNKNOWN);
            }

            if (row[DATA].equals(this.cds_night_mod_m)) {
                this.cdsCriteria.setNight_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_min_alt_f)) {
                this.cdsCriteria.setAlt_min_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_night_mod_m)) {
                this.cdsCriteria.setNight_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_alt_change_f)) {
                this.cdsCriteria.setAlt_change_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_alt_mod_m)) {
                this.cdsCriteria.setAlt_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_pi_day)) {
                this.cdsCriteria.setPi_day(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_pi_night)) {
                this.cdsCriteria.setPi_night(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_width_m)) {
                this.cdsCriteria.setWidth(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_qty_0)) {
                this.cdsCriteria.addQty(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_len_m_0)) {
                this.cdsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_qty_1)) {
                this.cdsCriteria.addQty(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_len_m_1)) {
                this.cdsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_qty_2)) {
                this.cdsCriteria.addQty(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_len_m_2)) {
                this.cdsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_qty_3)) {
                this.cdsCriteria.addQty(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_len_m_3)) {
                this.cdsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_qty_4)) {
                this.cdsCriteria.addQty(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_len_m_4)) {
                this.cdsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_qty_5)) {
                this.cdsCriteria.addQty(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.cds_len_m_5)) {
                this.cdsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.jpads_alt_f_0)) {
                this.jpadsCriteria.addAlt(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.jpads_len_m_0)) {
                this.jpadsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.jpads_alt_f_1)) {
                this.jpadsCriteria.addAlt(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.jpads_len_m_1)) {
                this.jpadsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.jpads_alt_f_2)) {
                this.jpadsCriteria.addAlt(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.jpads_len_m_2)) {
                this.jpadsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.jpads_alt_f_3)) {
                this.jpadsCriteria.addAlt(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.jpads_len_m_3)) {
                this.jpadsCriteria.addLength(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.per_width_m)) {
                this.perCriteria.setWidth_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.per_length_m)) {
                this.perCriteria.setLength_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.per_qty_mod_m)) {
                this.perCriteria.setQty_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.per_night_mod_m)) {
                this.perCriteria.setNight_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.per_alt_min_f)) {
                this.perCriteria.setAlt_min_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.per_alt_change_f)) {
                this.perCriteria.setAlt_change_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.per_alt_mod)) {
                this.perCriteria.setAlt_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.per_pi_day_m)) {
                this.perCriteria.setPi_day(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.per_pi_night)) {
                this.perCriteria.setPi_night(Double.valueOf(row[ac]));
            }

            //HE HE HE HE
            else if (row[DATA].equals(this.he_width_m)) {
                this.heCriteria.setWidth_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.he_length_m)) {
                this.heCriteria.setLength_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.he_qty_mod_m)) {
                this.heCriteria.setQty_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.he_night_mod_m)) {
                this.heCriteria.setNight_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.he_alt_min_f)) {
                this.heCriteria.setAlt_min_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.he_alt_change_f)) {
                this.heCriteria.setAlt_change_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.he_alt_mod_m)) {
                this.heCriteria.setAlt_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.he_pi_day_m)) {
                this.heCriteria.setPi_day(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.he_pi_night_m)) {
                this.heCriteria.setPi_night(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.mff_width_m)) {
                this.mffCriteria.setWidth_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.mff_length_m)) {
                this.mffCriteria.setLength_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.mff_qty_mod_m)) {
                this.mffCriteria.setQty_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.mff_night_mod_m)) {
                this.mffCriteria.setNight_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.mff_alt_min_f)) {
                this.mffCriteria.setAlt_min_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.mff_alt_change_f)) {
                this.mffCriteria.setAlt_change_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.mff_alt_mod_m)) {
                this.mffCriteria.setAlt_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.satb_width_m)) {
                this.satbCriteria.setWidth_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.satb_length_m)) {
                this.satbCriteria.setLength_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.satb_qty_mod_m)) {
                this.satbCriteria.setQty_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.satb_night_mod_m)) {
                this.satbCriteria.setNight_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.satb_alt_min_f)) {
                this.satbCriteria.setAlt_min_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.satb_alt_change_f)) {
                this.satbCriteria.setAlt_change_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.satb_alt_mod_m)) {
                this.satbCriteria.setAlt_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.crrc_width_m)) {
                this.crrcCriteria.setWidth_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.crrc_length_m)) {
                this.crrcCriteria.setLength_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.crrc_qty_mod_m)) {
                this.crrcCriteria.setQty_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.crrc_night_mod_m)) {
                this.crrcCriteria.setNight_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.crrc_alt_min_f)) {
                this.crrcCriteria.setAlt_min_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.crrc_alt_change_f)) {
                this.crrcCriteria.setAlt_change_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.crrc_alt_mod_m)) {
                this.crrcCriteria.setAlt_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hsllads_width_m)) {
                this.hslladsCriteria.setWidth_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hsllads_length_m)) {
                this.hslladsCriteria.setLength_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hsllads_qty_mod_m)) {
                this.hslladsCriteria.setQty_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hsllads_night_mod_m)) {
                this.hslladsCriteria.setNight_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hsllads_alt_min_f)) {
                this.hslladsCriteria.setAlt_min_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hsllads_alt_change_f)) {
                this.hslladsCriteria.setAlt_change_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hsllads_alt_mod_m)) {
                this.hslladsCriteria.setAlt_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hvcds_width_m)) {
                this.hvcdsCriteria.setWidth_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hvcds_length_m)) {
                this.hvcdsCriteria.setLength_m(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hvcds_qty_mod_m)) {
                this.hvcdsCriteria.setQty_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hvcds_night_mod_m)) {
                this.hvcdsCriteria.setNight_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hvcds_alt_min_f)) {
                this.hvcdsCriteria.setAlt_min_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hvcds_alt_change_f)) {
                this.hvcdsCriteria.setAlt_change_f(Double.valueOf(row[ac]));
            } else if (row[DATA].equals(this.hvcds_alt_mod_m)) {
                this.hvcdsCriteria.setAlt_mod(Double.valueOf(row[ac]));
            } else if (row[DATA].equals("version")) {
                this.Version = row[ac];
            }
        }
    }

    public DZCapabilities getDZCapabilities(double length, double width,
            double altitude_f, String aircraft) {
        DZCapabilities dzcaps = new DZCapabilities();

        if (!this.aircraft.contains(aircraft))
            return dzcaps;

        setRequirements(this.aircraft.indexOf(aircraft));

        dzcaps.jpads = jpadsCriteria.getCapabilities(length, altitude_f);
        dzcaps.jpads_night = dzcaps.jpads;

        dzcaps.cds = cdsCriteria.getCapabilities(length, width, false,
                altitude_f);
        dzcaps.cds_night = cdsCriteria.getCapabilities(length, width, true,
                altitude_f);

        dzcaps.per = perCriteria.getCapabilities(length, width, false,
                altitude_f);
        dzcaps.per_night = perCriteria.getCapabilities(length, width, true,
                altitude_f);

        dzcaps.he = heCriteria
                .getCapabilities(length, width, false, altitude_f);
        dzcaps.he_night = heCriteria.getCapabilities(length, width, true,
                altitude_f);

        dzcaps.mff = mffCriteria.getCapabilities(length, width, false,
                altitude_f);
        dzcaps.mff_night = mffCriteria.getCapabilities(length, width, true,
                altitude_f);

        dzcaps.satb = satbCriteria.getCapabilities(length, width, false,
                altitude_f);
        dzcaps.satb_night = satbCriteria.getCapabilities(length, width, true,
                altitude_f);

        dzcaps.crrc = crrcCriteria.getCapabilities(length, width, false,
                altitude_f);
        dzcaps.crrc_night = crrcCriteria.getCapabilities(length, width, true,
                altitude_f);

        dzcaps.hsllads = hslladsCriteria.getCapabilities(length, width, false,
                altitude_f);
        dzcaps.hsllads_night = hslladsCriteria.getCapabilities(length, width,
                true, altitude_f);

        dzcaps.hvcds = hvcdsCriteria.getCapabilities(length, width, false,
                altitude_f);
        dzcaps.hvcds_night = hvcdsCriteria.getCapabilities(length, width, true,
                altitude_f);

        //need PI for CDS, PER, and HE
        dzcaps.per_pi = perCriteria.getPi_day();
        dzcaps.per_pi_night = perCriteria.getPi_night();

        dzcaps.he_pi = heCriteria.getPi_day();
        dzcaps.he_pi_night = heCriteria.getPi_night();

        dzcaps.cds_pi = cdsCriteria.getPi_day();
        dzcaps.cds_pi_night = cdsCriteria.getPi_night();

        return dzcaps;
    }

}
