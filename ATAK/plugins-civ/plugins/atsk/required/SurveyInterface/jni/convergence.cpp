#include <jni.h>

#include <stdio.h>
#include <math.h>
/* Usage Notes...

(UTM) Definition

The National Imagery and Mapping Agency (NIMA)  (formerly the Defense Mapping Agency)
adopted a special grid for military use throughout the world called the
Universal Transverse Mercator (UTM) grid. In this grid, the world is divided into
60 north-south zones, each covering a strip 6° wide in longitude.

These zones are numbered consecutively beginning with Zone 1, between 180° and 174°
west longitude, and progressing eastward to Zone 60, between 174° and 180° east
longitude. NIMA's cartographers assigned the Equator an arbitrary false northing
value of 10,000,000 meters.

Central Merridan and Grid North Azimuth

A central meridian through the middle of each 6° zone is assigned an easting
value of 500,000 meters.  Grid values to the west of this central meridian are
less than 500,000; to the east, more than 500,000. Virtually all NIMA-produced
topographic maps and many aeronautical charts show the UTM grid lines.

The north/south grid lines on a UTM map (and in Falcon View) run parallel to the
zones central meridian even if the central meridian is off the map.
The central meridians fall every 6 degrees Longitude starting at
-177, -171, ... to + 171, 177 degrees. This makes by definition the central meridian
Grid north and True north equal.

Convergence Definition

But as you move east or west from the zones central meridian the
Grid North and True North differ by an amount called "convergence".

The convergence C at any point in the projection is the angle between
the ‘North–South’ grid line and the direction of the meridian at that
point.  Then Grid Bearing + C = True Bearing.
C is zero on the grid line E = E0.
It is positive to the east and negative to the west of this grid line.

Program purpose:

Calculate the UTM zone central merridan and then convergence relative to the central merridan
at a location in the world defined by a Latitude and Longitude based on the
the Datum WGS84.

Axis dimensions (a & b)in meters
Central meridian scale factor(f0)

UTM WGS84 */


/*
Input

Latitude  (PHI) = Point Latitude (Degrees)
Longitude (LAM) = Point Longitude (Degrees)
Longitude  (LAM0) = Central Merridan (Degrees)

for math details see

http://www.geovrml.org/archive/pdf00000.pdf

   or

Fort Drum Map Example:
DZ Point =  Map Center = Lat = 44:07:30 Long = -75:13:45
Convergence on Map = 0:25:00

Results =

Lat = 44.125000 Long = -75.603778
UTM zone 18 - central meridian = -75.000000
Convergence (Degrees Decimal) = -0.420374
Az_True_North (Degrees Decimal) = 89.579626
Az_Mag_North  (Degrees Decimal) = 103.000043
Az_Grid_North (Degrees Decimal) = 90.000000

Convergence degrees = 0
Convergence minutes = -25
Convergence seconds = -13.345627

*/





double Lat_Long_to_C(double PHI, double LAM, double a, double b, double f0)
{
    /*find central meridian (Degrees)*/
    int cm;
    double central_meridian;
    double LAM0;
    int zone, zm = 0;

    for (cm = -177; cm <= 177; cm +=6){
        zm ++;
        //printf(" UTM zone %d - cm = %d\n", zm,cm);
        if( fabs(LAM-(float)cm) <=3.0)
        {
            central_meridian = cm;
            zone = zm;
        }
    }
    //printf(" UTM zone %d - central meridian = %f\n", zone,central_meridian);
    LAM0 = central_meridian;


    /*convert angle measures to radians*/
    double Pi = 3.14159265358979;
    double RadPHI = PHI*(Pi/180);
    double RadLAM = LAM*(Pi/180);
    double RadLAM0 = LAM0*(Pi/180);
    double p;
    double XIII;
    double XIV;
    double XV;


    /*Basic compute*/
    double af0 = a * f0;
    double bf0 = b * f0;

    //printf(" af0 = %f\n", af0);
    //printf(" bf0 = %f\n", bf0);

    double e2 = (pow(af0,2)-pow(bf0,2))/pow(af0,2);

    /*Basic compute*/
    double nu = af0 / (sqrt(1-(e2*(pow(sin(RadPHI),2)))));
    double rho = (nu * (1-e2))/pow(1-(e2*(sin(RadPHI))),2);
    double eta2 = (nu/rho)-1;

    p = RadLAM - RadLAM0;

    //printf(" e2 = %f\n", e2);
    //printf(" nu = %f\n", nu);
    //printf(" rho = %f\n", rho);
    //printf(" p = %f\n", p);

    /*Compute Convergence*/
    XIII = sin(RadPHI);
    XIV = ((sin(RadPHI) * pow((cos(RadPHI)) , 2)) / 3) * (1 + (3 * eta2) + (2 * pow(eta2,2)));
    XV = ((sin(RadPHI) * pow((cos(RadPHI)) , 4)) / 15) * pow(2 - tan(RadPHI),2);

    //printf("XIII = %f\n", XIII);
    //printf("XIV = %f\n", XIV);
    //printf("XV = %f\n", XV);

    return (180 / Pi) * ((p * XIII) + (pow(p,3)) * XIV) + (pow(p,5) * XV);
}
