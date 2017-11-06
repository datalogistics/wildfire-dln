#include <jni.h>
#include <android/log.h>
#include <stdio.h>
//geoid transforms

//#include "stdafx.h"
//#include "resource.h"
//#include "Stringtok.h"
#include "loccart.h"
#include "xform_server.h"
#include "geocent.h"

#include "geoid.h"

#pragma warning( disable : 4996 )
#define NumbGeoidCols 1441   /* 360 degrees of longitude at 15 minute spacing */
#define NumbGeoidRows  721   /* 180 degrees of latitude  at 15 minute spacing */
#define NumbHeaderItems 6    /* min, max lat, min, max long, lat, long spacing*/
#define ScaleFactor     4    /* 4 grid cells per degree at 15 minute spacing  */
#define NumbGeoidElevs NumbGeoidCols * NumbGeoidRows
#define PI 3.14159265358979323846
#define LITTLE_ENDIAN

static long  Geoid_Initialized = 0;  /* indicates successful initialization */
//FILE  *GeoidHeightFile;

short GeoidHeightBuffer[NumbGeoidElevs];



void DoD_Convert_AARtoLLA(double * goodguy_lla,double * badguy_aar,double * badguy_lla)
{
    double Relative_Position_NED[3];
    double Azimuth   = badguy_aar[0];
    double Elevation = badguy_aar[1];
    double Range     = badguy_aar[2];

    //  WGS 84 Ellipsoid Parameters, default to WGS 84
    //  Semi-major axis of ellipsoid in meters a = 6378137.0;
    //  Flattening of ellipsoid =  1 / 298.257223563;

    double a = 6378137.0;
    double f = 1.0 / 298.257223563;
    long status;
    double orientation = 0.0;

    status = Set_Local_Cartesian_Parameters (a,
                                            f,
                                            (double)goodguy_lla[0],
                                            (double)goodguy_lla[1],
                                            (double)goodguy_lla[2],
                                            orientation);

    // convert spherical to cartesian
    rae2ned (Range,Azimuth,Elevation,Relative_Position_NED);

//    fprintf(dfp," Relative_Position_NED = %f,%f,%f \n",
//    Relative_Position_NED[0],Relative_Position_NED[1],Relative_Position_NED[2]);

    // this wants east,north,up...

    Convert_Local_Cartesian_To_Geodetic(Relative_Position_NED[1],   // east
                                        Relative_Position_NED[0],   // north
                                        -Relative_Position_NED[2],  // up
                                        &badguy_lla[0],                // rad
                                        &badguy_lla[1],                // rad
                                        &badguy_lla[2]);            // meters

//    fprintf(dfp," GetLadar badguy_lla(rad,rad,m(WGS84))%9.5f %9.5f %9.5f\n",
//                    badguy_lla[0],badguy_lla[1],badguy_lla[2]);

    return;
}

void DoD_Convert_LLAtoAAR(double *goodguy_lla,double *badguy_lla,double *badguy_aar)
{

    double Relative_Position_NED[3];
    double Azimuth   = badguy_aar[0];
    double Elevation = badguy_aar[1];
    double Range     = badguy_aar[2];


//  WGS 84 Ellipsoid Parameters, default to WGS 84
//  Semi-major axis of ellipsoid in meters a = 6378137.0;
//  Flattening of ellipsoid =  1 / 298.257223563;

    double a = 6378137.0;
    double f = 1.0 / 298.257223563;
    long status;
    double orientation = 0.0;
    status = Set_Local_Cartesian_Parameters (a,
                                            f,
                                            (double)goodguy_lla[0],
                                            (double)goodguy_lla[1],
                                            (double)goodguy_lla[2],
                                            orientation);

    // this returns east,north,up...
    Convert_Geodetic_To_Local_Cartesian(badguy_lla[0],
                                         badguy_lla[1],
                                         badguy_lla[2],
                                         &(Relative_Position_NED[1]),// east
                                         &(Relative_Position_NED[0]),// north
                                         &(Relative_Position_NED[2]));// up

    Relative_Position_NED[2] *= -1.0;    // change sign to down

    // convert cartesian to spherical
//  fprintf(dfp,"DoD_Convert_LLAtoAAR Relative_Position_NED = %f,%f,%f \n",
//      Relative_Position_NED[0],Relative_Position_NED[1],Relative_Position_NED[2]);


    ned2rae(Relative_Position_NED,&badguy_aar[2],&badguy_aar[0],&badguy_aar[1]);

//    fprintf(dfp,"DoD_Convert_LLAtoAAR Relative_Position_RAE = %f,%f,%f \n",
//        badguy_aar[2],badguy_aar[0],badguy_aar[1]);


}

