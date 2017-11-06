#include <string.h>
#include <jni.h>
#include <iostream>

#pragma once

#define EGM84_COLS    37                 /* 360 degrees of longitude at 10 degree spacing */
#define EGM84_ROWS    19                 /* 180 degrees of latitude  at 10 degree spacing */
#define EGM96_COLS    1441               /* 360 degrees of longitude at 15 minute spacing */
#define EGM96_ROWS    721                /* 180 degrees of latitude  at 15 minute spacing */
#define SCALE_FACTOR_15_MINUTES .25      /* 4 grid cells per degree at 15 minute spacing  */
#define SCALE_FACTOR_10_DEGREES  10      /* 1 / 10.0 grid cells per degree at 10 degree spacing */
#define SCALE_FACTOR_30_MINUTES  .5      /* 2 grid cells per degree at 30 minute spacing */
#define SCALE_FACTOR_1_DEGREE     1      /* 1 grid cell per degree at 1 degree spacing */
#define SCALE_FACTOR_2_DEGREES    2      /* 1 / 2 grid cells per degree at 2 degree spacing */


#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

class ElevationConversions
{
public:
    ElevationConversions(bool UseEMG96= false);
    ~ElevationConversions(void);

    long Convert_Ellipsoid_To_Geoid_Height ( double Latitude,double Longitude,double Ellipsoid_Height,double *Geoid_Height );
    long Convert_Geoid_To_Ellipsoid_Height ( double Latitude,double Longitude,double Geoid_Height,double *Ellipsoid_Height );


    static std::string GetLatLonDMS(double Lat, double Lon);
    static std::string GetLatDM(double Lat);
    static std::string GetLonDM(double Lon);

    static bool AROffset(double StartLat, double StartLon,double *EndLat, double *EndLon, float Azimuth, float Range_m);
    static bool AROffset(double StartLat, double StartLon, float hae_m,double *EndLat, double *EndLon, float *Endhae_m, float Azimuth, float Range_m, float ElevationAngle);
    static bool GetXYOffset(double StartLat, double StartLon, float XOffset,float YOffset, double *EndLat, double *EndLon, float AngleOffset=0);

    static bool  CalculateRangeandAngle(double StartLat, double StartLon,double EndLat, double EndLon, float *Range_m, float *Angle_deg);
    static bool CalculateRangeandAngle(double StartLat, double StartLon,float hae, double EndLat, double EndLon,float endhae, float *Range_m, float *Az_Angle_deg, float* El_Angle_deg);
    static float CalculateRange_m(double StartLat, double StartLon,double EndLat, double EndLon);
    static float CalculateAngle_deg(double StartLat, double StartLon,double EndLat, double EndLon);
    static float GetMagAngle(float TrueAngle, double Lat, double Lon);
    static float GetTrueAngle(float MagAngle, double Lat, double Lon);

    static std::string GetUTMHemisphereZone(double Lat, double Lon);
    static std::string GetUTMEasting(double Lat, double Lon);
    static std::string GetUTMNorthing(double Lat, double Lon);

    std::string getSpheroidString();
    static std::string getDatumString();

    static bool getCorners(double centerlat, double centerlon, float length, float width, float angle, int numcorners,
        double *EndLat, double *EndLon);

    long Get_Geoid_Height ( double Latitude, double Longitude,double *DeltaHeight );

    static float GetDeclination(double Lat, double Lon);

    static float GetMGRSHeadingOffset( double Lat, double Lon, double Heading);



private:
    bool UsingEGM96;
    
    bool InitializeGeoid(bool UseEGM96);
    static bool InitializeWMM();
    bool Geoid_Initialized;
    static bool WMM_Initialized;
    short *GeoidHeightBuffer;
    int NumbGeoidCols;
    int NumbGeoidRows;
    int Elevations;
    double ScaleFactor;
    static std::string delimitString(std::string fullString); //MIKE changed from CString
    static double CoordStringToDouble(std::string coord); //MIKE changed from CString


    static float WMM_FindDeclination(double Lat, double Lon);

    static bool FindCorner(float angle, float length, float width, double startlat, double startlon, double *EndLat, double *EndLon);
    static std::string GetDM(double val, bool lat);


public:
    static std::string GetMGRS(double Lat, double Lon, int Digits=5);

};


    static std::string convertString( float i, char* format);
    static std::string convertStringChar( char* letters, char* format);
