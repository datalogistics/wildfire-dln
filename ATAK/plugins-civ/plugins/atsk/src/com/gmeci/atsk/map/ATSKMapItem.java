
package com.gmeci.atsk.map;

public interface ATSKMapItem {

    // Metadata keys
    String LABEL_VISIBLE = "label_visible";
    String LABEL_ALWAYS_SHOW = "label_always_show";

    void rename(String name);

    void copy(int copies);

    void delete();

    void setLabelVisible(boolean visible);

    boolean getLabelVisible();

    void save();

    String getKMLDescription();
}
