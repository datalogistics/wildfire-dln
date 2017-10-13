
package com.gmeci.csvparser;

import java.util.ArrayList;
import java.util.Iterator;

public class ContainerType {
    public String Type;
    public final ArrayList<SizeCriteria> SizeCriteriaList;

    public ContainerType() {
        SizeCriteriaList = new ArrayList<SizeCriteria>();
    }

    public void AddToDataList(SizeCriteria dataobject) {
        SizeCriteriaList.add(dataobject);
    }

    public String toString(boolean showContainerDetails) {
        String string = Type;
        if (showContainerDetails) {
            string = String.format("%s%n\t\t%s", string,
                    printContainerDetails());
        }
        return string;
    }

    protected String printContainerDetails() {
        Iterator<SizeCriteria> it = SizeCriteriaList.iterator();
        String string = "";
        while (it.hasNext()) {
            SizeCriteria details = it.next();
            string = String.format("%s%n\t\t%s", string, details.length);
        }
        return string;
    }
}
