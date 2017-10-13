
package com.gmeci.atskservice.dz;

import java.util.List;

public class DZCapabilitiesBase {

    public static final int DATA = 0;
    //JPADSCriteria
    protected static final double JPADS_UNKNOWN = -1;
    //CDS
    protected static final String CDS_LEN = "cds_len";
    protected static final String CDS_QTY = "cds_qty";
    protected static final String UNKNOWN = "unknown";
    public JPADSCriteria jpadsCriteria;
    public CDSCriteria cdsCriteria;

    public static final String cds_night_mod_m = "cds_night_mod_m";
    public static final String cds_min_alt_f = "cds_min_alt_f";
    public static final String cds_alt_change_f = "cds_alt_change_f";
    public static final String cds_alt_mod_m = "cds_alt_mod_m";
    public static final String cds_pi_day = "cds_pi_day_m";
    public static final String cds_pi_night = "cds_pi_night_m";
    public static final String cds_width_m = "cds_width_m";
    public static final String cds_qty_0 = "cds_qty_1";
    public static final String cds_len_m_0 = "cds_len_1_m";
    public static final String cds_qty_1 = "cds_qty_2";
    public static final String cds_len_m_1 = "cds_len_2_m";
    public static final String cds_qty_2 = "cds_qty_3";
    public static final String cds_len_m_2 = "cds_len_3_m";
    public static final String cds_qty_3 = "cds_qty_4";
    public static final String cds_len_m_3 = "cds_len_4_m";
    public static final String cds_qty_4 = "cds_qty_5";
    public static final String cds_len_m_4 = "cds_len_5_m";
    public static final String cds_qty_5 = "cds_qty_6";
    public static final String cds_len_m_5 = "cds_len_6_m";
    public static final String jpads_alt_f_0 = "jpads_alt_1_f";
    public static final String jpads_len_m_0 = "jpads_len_1_m";
    public static final String jpads_alt_f_1 = "jpads_alt_2_f";
    public static final String jpads_len_m_1 = "jpads_len_2_m";
    public static final String jpads_alt_f_2 = "jpads_alt_3_f";
    public static final String jpads_len_m_2 = "jpads_len_3_m";
    public static final String jpads_alt_f_3 = "jpads_alt_4_f";
    public static final String jpads_len_m_3 = "jpads_len_4_m";
    public static final String per_width_m = "per_width_m";
    public static final String per_length_m = "per_length_m";
    public static final String per_qty_mod_m = "per_qty_mod_m";
    public static final String per_night_mod_m = "per_night_mod_m";
    public static final String per_alt_min_f = "per_alt_min_f";
    public static final String per_alt_change_f = "per_alt_change_f";
    public static final String per_alt_mod = "per_alt_mod";
    public static final String per_pi_day_m = "per_pi_day_m";
    public static final String per_pi_night = "per_pi_night_m";
    public static final String he_width_m = "he_width_m";
    public static final String he_length_m = "he_length_m";
    public static final String he_qty_mod_m = "he_qty_mod_m";
    public static final String he_night_mod_m = "he_night_mod_m";
    public static final String he_alt_min_f = "he_alt_min_f";
    public static final String he_alt_change_f = "he_alt_change_f";
    public static final String he_alt_mod_m = "he_alt_mod_m";
    public static final String he_pi_day_m = "he_pi_day_m";
    public static final String he_pi_night_m = "he_pi_night_m";
    public static final String mff_width_m = "mff_width_m";
    public static final String mff_length_m = "mff_length_m";
    public static final String mff_qty_mod_m = "mff_qty_mod_m";
    public static final String mff_night_mod_m = "mff_night_mod_m";
    public static final String mff_alt_min_f = "mff_alt_min_f";
    public static final String mff_alt_change_f = "mff_alt_change_f";
    public static final String mff_alt_mod_m = "mff_alt_mod_m";
    public static final String satb_width_m = "satb_width_m";
    public static final String satb_length_m = "satb_length_m";
    public static final String satb_qty_mod_m = "satb_qty_mod_m";
    public static final String satb_night_mod_m = "satb_night_mod_m";
    public static final String satb_alt_min_f = "satb_alt_min_f";
    public static final String satb_alt_change_f = "satb_alt_change_f";
    public static final String satb_alt_mod_m = "satb_alt_mod_m";
    public static final String crrc_width_m = "crrc_width_m";
    public static final String crrc_length_m = "crrc_length_m";
    public static final String crrc_qty_mod_m = "crrc_qty_mod_m";
    public static final String crrc_night_mod_m = "crrc_night_mod_m";
    public static final String crrc_alt_min_f = "crrc_alt_min_f";
    public static final String crrc_alt_change_f = "crrc_alt_change_f";
    public static final String crrc_alt_mod_m = "crrc_alt_mod_m";
    public static final String hsllads_width_m = "hsllads_width_m";
    public static final String hsllads_length_m = "hsllads_length_m";
    public static final String hsllads_qty_mod_m = "hsllads_qty_mod_m";
    public static final String hsllads_night_mod_m = "hsllads_night_mod_m";
    public static final String hsllads_alt_min_f = "hsllads_alt_min_f";
    public static final String hsllads_alt_change_f = "hsllads_alt_change_f";
    public static final String hsllads_alt_mod_m = "hsllads_alt_mod_m";
    public static final String hvcds_width_m = "hvcds_width_m";
    public static final String hvcds_length_m = "hvcds_length_m";
    public static final String hvcds_qty_mod_m = "hvcds_qty_mod_m";
    public static final String hvcds_night_mod_m = "hvcds_night_mod_m";
    public static final String hvcds_alt_min_f = "hvcds_alt_min_f";
    public static final String hvcds_alt_change_f = "hvcds_alt_change_f";
    public static final String hvcds_alt_mod_m = "hvcds_alt_mod_m";

    public static final String TAG = "DZCapabilitesParser";
    public static final String DZ_DEFINITION_FILE_NAME = "/atsk/az_templates/dz_requirements.csv";
    String Version;
    SizeCriteria perCriteria;
    SizeCriteria heCriteria;
    SizeCriteria mffCriteria;
    SizeCriteria satbCriteria;
    SizeCriteria crrcCriteria;
    SizeCriteria hslladsCriteria;
    SizeCriteria hvcdsCriteria;
    List<String> aircraft;

    //helper functions
    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public List<String> getAircraft() {
        return aircraft;
    }
}
