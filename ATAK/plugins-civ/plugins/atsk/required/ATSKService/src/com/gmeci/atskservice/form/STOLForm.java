
package com.gmeci.atskservice.form;

import com.gmeci.atskservice.form.formobjects.FormObject;

public class STOLForm extends LTFWLZForm
{

    private void constructLocationName() {
        String cName = prefs.getString("countryName", "");
        String rName = prefs.getString("regionName", "");
        String location = "";

        if (cName.length() > 0)
            location = cName;
        if (rName.length() > 0) {
            if (location.length() > 0)
                location += "/";
            location += rName;
        }
        record("location", location);
    }

    protected FormObject specificStatements() {
        return null;
    }

    @Override
    public void generateBlocks() {

        constructLocationName();

        record("remarks", remarks.getText());
        record("penetrations", penetrations.getText());
    }

}
