
package com.gmeci.atsk.az.hlz;

public class HelicopterRequirements {

    public String HeliName;
    public String Units;
    public final double rotorDiameter_m;
    public final double outerDiameter_m;
    public double trainingLength_m;
    public double trainingWidth_m;
    public double contingencyLength_m;
    public double contingencyWidth_m;
    public double brownoutLength_m;
    public double brownoutWidth_m;

    public HelicopterRequirements() {
        HeliName = "";
        Units = "";
        rotorDiameter_m = 0;
        outerDiameter_m = 0;
        trainingLength_m = 0;
        trainingWidth_m = 0;
        contingencyLength_m = 0;
        contingencyWidth_m = 0;
        brownoutLength_m = 0;
        brownoutWidth_m = 0;
    }

}
