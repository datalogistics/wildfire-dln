
package com.gmeci.csvparser;

import java.util.ArrayList;
import java.util.Iterator;

public class DZRequirements {
    public String DZ_Aircraft;
    public final ArrayList<ContainerType> containers;
    public int pi_cds_m, pi_nightcds_m, pi_per_m, pi_nightper_m, pi_he_m,
            pi_nighthe_m;

    public DZRequirements() {
        containers = new ArrayList<ContainerType>();
    }

    public void AddToContainerList(ContainerType containerobject) {
        containers.add(containerobject);
    }

    public String toString(boolean showContainers, boolean c) {
        String string = String.format("%s", DZ_Aircraft);
        if (showContainers) {
            string = String.format("%s%s", string, printContainers(c));
        }
        return string;

    }

    protected String printContainers(boolean showContainerDetails) {
        Iterator<ContainerType> it = containers.iterator();
        String enchilada = "";
        while (it.hasNext()) {
            ContainerType newType = it.next();
            enchilada = String.format("%s%n\t%s", enchilada,
                    newType.toString(showContainerDetails));
        }
        return enchilada;
    }
}
