
//#include "StdAfx.h"
#include <jni.h>
#include <android/log.h>
#include "ElevationConversions.h"
#include "loccart.h"
#include "xform_server.h"
#include "geocent.h"
#include "geoid.h"
#include "wmm_2k.h"
#include "mgrs.h"
#include "utm.h"
#include "StaticInitializations.h"
#include "egm84s.h"
#include "egm96s.h"
#include "WMMHeader.h"
#include "convergence.h"
#include "math.h"
#include <string.h>
#include <time.h>
#include <iostream>

#include <cstdio>
#include <stdio.h>
#include <stdlib.h>

#define APPNAME "Conversions"

#define GEOID_NO_ERROR              0x0000
#define GEOID_FILE_OPEN_ERROR       0x0001
#define GEOID_INITIALIZE_ERROR      0x0002
#define GEOID_NOT_INITIALIZED_ERROR 0x0004
#define GEOID_LAT_ERROR             0x0008
#define GEOID_LON_ERROR             0x0010

#define M_PI 3.14159265358979323846
#define DEG_2_RAD M_PI/180.0
#define RAD_2_DEG 180.0/M_PI
#define BURNFILE

#define NUM_CHARS_MGRS_ZONE 5
#define DELIMITERS ".\\/,-_,%#()+ "

#define CONV_A  6378137.000
#define CONV_B  6356752.315
#define CONV_F0 .9996

//added to NDK
 extern void DoD_Convert_AARtoLLA(double * goodguy_lla,double * badguy_aar,double * badguy_lla);
 //added to NDK
 extern void DoD_Convert_LLAtoAAR(double *goodguy_lla,double *badguy_lla,double *badguy_aar);

  //added to NDK
bool ElevationConversions::GetXYOffset(double StartLat, double StartLon, float XOffset,float YOffset, double *EndLat, double *EndLon, float AngleOffset)
{
    double MidLat, MidLon;
    ElevationConversions::AROffset(StartLat, StartLon,&MidLat, &MidLon, 90+AngleOffset, XOffset);
    ElevationConversions::AROffset(MidLat, MidLon,EndLat, EndLon, 0+AngleOffset, YOffset);

    return true;
}
//added to NDK
bool ElevationConversions::AROffset(double StartLat, double StartLon, float hae_m,double *EndLat, double *EndLon, float *Endhae_m, float Azimuth, float Range_m, float ElevationAngle)
{
    *EndLat = StartLat;
    *EndLon= StartLon;
    if(StartLat > 90 || StartLat < -90)
        return false;
    if(StartLon >180 || StartLon < -180)
        return false;

    double Start[3], End[3], AAR[3];
    Start[0]    = StartLat * DEG_2_RAD;
    Start[1]    = StartLon * DEG_2_RAD;
    Start[2]    = hae_m;

    AAR[0]        = Azimuth * DEG_2_RAD;
    AAR[1]        = ElevationAngle * DEG_2_RAD;
    AAR[2]        = Range_m;

    DoD_Convert_AARtoLLA(Start,AAR,End);
    *EndLat        = End[0]* RAD_2_DEG;
    *EndLon        = End[1]* RAD_2_DEG;
    *Endhae_m    = (float)End[2];

    return true;
}
 //added to NDK
bool ElevationConversions::AROffset(double StartLat, double StartLon,double *EndLat, double *EndLon, float Azimuth, float Range_m)
{
    float hae_dummy;
    return AROffset(StartLat, StartLon, 0, EndLat, EndLon, &hae_dummy, Azimuth, Range_m, 0);

}
//added to ndk
bool ElevationConversions::CalculateRangeandAngle(double StartLat, double StartLon,double EndLat, double EndLon, float *Range_m, float *Angle_deg)
{
    double Start[3], End[3], AAR[3];
    Start[0] = StartLat * DEG_2_RAD;
    Start[1] = StartLon * DEG_2_RAD;
    Start[2] = 0;

    End[0] = EndLat * DEG_2_RAD;
    End[1] = EndLon * DEG_2_RAD;
    End[2] = 0;

    DoD_Convert_LLAtoAAR(Start,End,AAR);

    (*Range_m) = (float)AAR[2];

    *Angle_deg = (float)(AAR[0]* RAD_2_DEG);

    return true;
}//endCalculateRangeandAngle

bool ElevationConversions::CalculateRangeandAngle(double StartLat, double StartLon,float Starthae, double EndLat, double EndLon,float Endhae, float *Range_m, float *Az_Angle_deg, float* El_Angle_deg)
{
    double Start[3], End[3], AAR[3];
    Start[0] = StartLat * DEG_2_RAD;
    Start[1] = StartLon * DEG_2_RAD;
    Start[2] = Starthae;

    End[0] = EndLat * DEG_2_RAD;
    End[1] = EndLon * DEG_2_RAD;
    End[2] = Endhae;

    DoD_Convert_LLAtoAAR(Start,End,AAR);

    (*Range_m) = (float)AAR[2];

    *Az_Angle_deg = (float)(AAR[0]* RAD_2_DEG);
    *El_Angle_deg = (float)(AAR[1]* RAD_2_DEG);

    return true;
}

float ElevationConversions::CalculateRange_m(double StartLat, double StartLon,double EndLat, double EndLon)
{
    double Start[3], End[3], AAR[3];
    Start[0] = StartLat * DEG_2_RAD;
    Start[1] = StartLon * DEG_2_RAD;
    Start[2] = 0;

    End[0] = EndLat * DEG_2_RAD;
    End[1] = EndLon * DEG_2_RAD;
    End[2] = 0;

    DoD_Convert_LLAtoAAR(Start,End,AAR);

    return (float)AAR[2];

}

//added to ndk
float ElevationConversions::CalculateAngle_deg(double StartLat, double StartLon,double EndLat, double EndLon)
{
    double Start[3], End[3], AAR[3];
    Start[0] = StartLat * DEG_2_RAD;
    Start[1] = StartLon * DEG_2_RAD;
    Start[2] = 0;

    End[0] = EndLat * DEG_2_RAD;
    End[1] = EndLon * DEG_2_RAD;
    End[2] = 0;

    DoD_Convert_LLAtoAAR(Start,End,AAR);

    return (float)(AAR[0]* RAD_2_DEG);
}

//added to ndk
std::string ElevationConversions::GetLatLonDMS(double Lat, double Lon)
{
     std::string LatString, LonString;
         //Lat/////////////////////////////////////////////////////////////
         char TempD[16];
         char TempM[16];
         char TempMM[16];
         char TempS[16];
         char const * TempNS = "N";
         double LatInternal = Lat;
         if(Lat <0)
         {
             LatInternal = Lat * -1;
             TempNS = "S";
         }

         int Degrees_i= (int)LatInternal;

         sprintf(TempD, "%02d", Degrees_i );

         double FractionOfDegree = LatInternal - Degrees_i;
         double Minutes_d = FractionOfDegree*60.0;
         int Minutes_i = (int)(FractionOfDegree*60.0);

         sprintf(TempM, "%02d", Minutes_i);
         sprintf(TempMM, "%07.4f", Minutes_d);

         double Seconds = Minutes_d-Minutes_i;
         Seconds = Seconds* 60;

         sprintf(TempS, "%06.3f", (float)Seconds);

         char buffer[50];
         sprintf(buffer, "%s %s:%s:%s", TempNS, TempD, TempM, TempS);

         LatString = buffer;

         //Done Lat
         //Lon/////////////////////////////////////////////////////////////

         char const * TempEW = "E";
         double LonInternal = Lon;
         if(Lon <0)
         {
             LonInternal = Lon * -1;
             TempEW = "W";
         }

         Degrees_i= (int)LonInternal;

         sprintf(TempD, "%02d", Degrees_i );

        FractionOfDegree = LonInternal - Degrees_i;
        Minutes_d = FractionOfDegree*60.0;
        Minutes_i = (int)(FractionOfDegree*60.0);

        sprintf(TempM, "%02d", Minutes_i);
        sprintf(TempMM, "%07.4f", Minutes_d);

        Seconds = Minutes_d-Minutes_i;
        Seconds = Seconds* 60;

        sprintf(TempS, "%06.3f", (float)Seconds);

        sprintf(buffer, "%s %s:%s:%s", TempEW, TempD, TempM, TempS);
         //Done Lon

        LonString = buffer;
         return LatString+" "+LonString;

}
 //added to ndk
std::string ElevationConversions::GetLatDM(double Lat)
{
    return ElevationConversions::GetDM(Lat, true);
}
//added to ndk
std::string ElevationConversions::GetLonDM(double Lon)
{
    return ElevationConversions::GetDM(Lon, false);
}

//added to ndk
std::string ElevationConversions::GetDM(double val, bool lat)
{
        std::string retval;

        char const * dir = "N";
        if (!lat) dir = "E";

        double ValInternal = val;
        if(val < 0) {
            ValInternal = val * -1;
            dir = "S";
            if (!lat) dir = "W";
        }

        int Degrees_i= (int)ValInternal;

        double FractionOfDegree = ValInternal - Degrees_i;
        double Minutes_d = FractionOfDegree*60.0;

        char buffer[50];

        if (lat)
            sprintf(buffer, "%s %02d %07.4f", dir, Degrees_i, Minutes_d);
        else
            sprintf(buffer, "%s %03d %07.4f", dir, Degrees_i, Minutes_d);

        retval = buffer;

        return retval;

}

//added to NDK
ElevationConversions::ElevationConversions(bool UseEGM96)
{
    this->UsingEGM96    = UseEGM96;
    Geoid_Initialized   = false;
    //WMM_Initialized   = false;

    InitializeGeoid(UsingEGM96);
    //InitializeWMM();
}
ElevationConversions::~ElevationConversions(void){}

//Geoid Functions
bool ElevationConversions::InitializeGeoid(bool UseEGM96)
{  //MIKE need to add the old read code back for adding in the new egm grid file and making it a header,
    // think subversion
    if (Geoid_Initialized)
    {
        return (GEOID_NO_ERROR);
    }

    if(!UseEGM96)
    {
        NumbGeoidCols = EGM84_COLS;
        NumbGeoidRows = EGM84_ROWS;
        Elevations = NumbGeoidCols*NumbGeoidRows;
        GeoidHeightBuffer = egm84_array;
    }
    else
    {
        NumbGeoidCols = EGM96_COLS;
        NumbGeoidRows = EGM96_ROWS;
        Elevations = NumbGeoidCols*NumbGeoidRows;
        GeoidHeightBuffer = egm96_array;
    }
    Geoid_Initialized = true;
    return ( true );
}

//added to NDK
long ElevationConversions::Get_Geoid_Height ( double Latitude,
                        double Longitude,
                        double *DeltaHeight )
/*
 * The private function Get_Geoid_Height returns the height of the
 * WGS84 geiod above or below the WGS84 ellipsoid,
 * at the specified geodetic coordinates,
 * using a grid of height adjustments from the EGM96 gravity model.
 *
 *    Latitude            : Geodetic latitude in radians           (input)
 *    Longitude           : Geodetic longitude in radians          (input)
 *    DeltaHeight         : Height Adjustment, in meters.          (output)
 *
 */
{
    long    Index;
    double DeltaX, DeltaY;
    double ElevationSE, ElevationSW, ElevationNE, ElevationNW;
    double LatitudeDD, LongitudeDD;
    double OffsetX, OffsetY;
    double PostX, PostY;
    double UpperY, LowerY;
    //long Error_Code = 0;

    //if (!Geoid_Initialized)
    //{
    //    TRACE("Problem with egm file loading");
    //return (GEOID_NOT_INITIALIZED_ERROR);
    //}
    //if (!Error_Code)
    //{
        /*  Compute X and Y Offsets into Geoid Height Array:                              */
        if(!UsingEGM96)
        {
            ScaleFactor = SCALE_FACTOR_10_DEGREES;
            LatitudeDD  = Latitude ;
            LongitudeDD = Longitude;
        }
        else
        {
            ScaleFactor=.25;
            LatitudeDD  = Latitude ;
            LongitudeDD = Longitude;
        }
        if (LongitudeDD < 0.0)
        {
            OffsetX = ( LongitudeDD + 360.0 ) / ScaleFactor;
        }
        else
        {
            OffsetX = LongitudeDD / ScaleFactor;
        }
        OffsetY = ( 90.0 - LatitudeDD ) / ScaleFactor;

        /*  Find Four Nearest Geoid Height Cells for specified Latitude, Longitude;   */
        /*  Assumes that (0,0) of Geoid Height Array is at Northwest corner:          */
        PostX = floor( OffsetX );
        if ((PostX + 1) == NumbGeoidCols)
            PostX--;
        PostY = floor( OffsetY );
        if ((PostY + 1) == NumbGeoidRows)
            PostY--;

        Index = (long)((PostY*NumbGeoidCols) + PostX);
        ElevationNW = (float)GeoidHeightBuffer[ Index ]  / (float)100.0;
        ElevationNE = (float)GeoidHeightBuffer[ Index+ 1 ]  / (float)100.0;
        ElevationSW = (float)GeoidHeightBuffer[ Index ]  / (float)100.0;
        ElevationSE = (float)GeoidHeightBuffer[ Index + 1 ]  / (float)100.0;

        /*  Perform Bi-Linear Interpolation to compute Height above Ellipsoid:        */
        DeltaX = OffsetX - PostX;
        DeltaY = OffsetY - PostY;

        UpperY = ElevationNW + DeltaX * ( ElevationNE - ElevationNW );
        LowerY = ElevationSW + DeltaX * ( ElevationSE - ElevationSW );

        *DeltaHeight = UpperY + DeltaY * ( LowerY - UpperY );
        return true;
    }

//added to NDK
long ElevationConversions::Convert_Ellipsoid_To_Geoid_Height ( double Latitude,
                                         double Longitude,
                                         double Ellipsoid_Height,
                                         double *Geoid_Height )
/*
 * The function Convert_Ellipsoid_To_Geoid_Height converts the specified WGS84
 * ellipsoid height at the specified geodetic coordinates to the equivalent
 * geoid height, using the EGM96 gravity model.
 *
 *    Latitude            : Geodetic latitude in radians           (input)
 *    Longitude           : Geodetic longitude in radians          (input)
 *    Ellipsoid_Height    : Ellipsoid height, in meters            (input)
 *    Geoid_Height        : Geoid height, in meters.               (output)
 *
 */
{
  // On error return Ellipsoid_Height = Geoid_Height
  double  DeltaHeight = 0;
  long Error_Code;
  Error_Code = Get_Geoid_Height ( Latitude, Longitude, &DeltaHeight );
  *Geoid_Height = Ellipsoid_Height - DeltaHeight;
  return ( Error_Code );
}
//added to NDK
long ElevationConversions::Convert_Geoid_To_Ellipsoid_Height ( double Latitude,
                                         double Longitude,
                                         double Geoid_Height,
                                         double *Ellipsoid_Height )
/*
 * The function Convert_Geoid_To_Ellipsoid_Height converts the specified WGS84
 * geoid height at the specified geodetic coordinates to the equivalent
 * ellipsoid height, using the EGM96 gravity model.
 *
 *    Latitude            : Geodetic latitude in radians           (input)
 *    Longitude           : Geodetic longitude in radians          (input)
 *    Ellipsoid_Height    : Ellipsoid height, in meters            (input)
 *    Geoid_Height        : Geoid height, in meters.               (output)
 *
 */
{

  if(0!=  InitializeGeoid(UsingEGM96))
  {
      *Ellipsoid_Height = Geoid_Height;
      return 1;
  }
  // On error return Ellipsoid_Height = Geoid_Height
  double  DeltaHeight = 0;
  long Error_Code;
  Error_Code = Get_Geoid_Height ( Latitude, Longitude, &DeltaHeight );
  *Ellipsoid_Height = Geoid_Height + DeltaHeight;
  return ( Error_Code );
}

//added to NDK
std::string ElevationConversions::GetUTMHemisphereZone(double Lat, double Lon)
{
     long Zone;
     char Hemisphere;
     double Easting,Northing;

    int Error_Code =  Convert_Geodetic_To_UTM (Lat*DEG_2_RAD,
                                               Lon*DEG_2_RAD,
                                               &Zone,
                                               &Hemisphere,
                                               &Easting,
                                               &Northing);
    

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "UTM Conversion: %f, %f, is %ld, %d, %f %f",Lat, Lon, Zone, Hemisphere, Easting, Northing );



    char hemi[5], zone[5]; 
    std::string Temp;    

    int sp;    
    sp = sprintf(zone, "%02d", (int)Zone);
    sp = sprintf(hemi, "%c", Hemisphere);
            
    Temp.append(zone);
    Temp.append(" ");
    Temp.append(hemi);
    return Temp;
}
//added to NDK
std::string ElevationConversions::GetUTMEasting(double Lat, double Lon)
{
     long Zone;
     char Hemisphere;
     double Easting,Northing;
     int EastingInt;

    int Error_Code =  Convert_Geodetic_To_UTM (Lat*DEG_2_RAD,
                                            Lon*DEG_2_RAD,
                                            &Zone,
                                            &Hemisphere,
                                            &Easting,
                                            &Northing);
    EastingInt = (int)Easting;    
    std::string Temp;
    char  easting[10];     
    int sp = sprintf(easting, "%06d", EastingInt);

    Temp.append(easting);
    return Temp;
}
//added to NDK
std::string ElevationConversions::GetUTMNorthing(double Lat, double Lon)
{
     long Zone;
     char Hemisphere;
     double Easting,Northing;
     int NorthingInt;

    int Error_Code =  Convert_Geodetic_To_UTM (Lat*DEG_2_RAD,
                                            Lon*DEG_2_RAD,
                                            &Zone,
                                            &Hemisphere,
                                            &Easting,
                                            &Northing);
    NorthingInt = (int)Northing;
    char northing[10];
    std::string Temp;
    int sp = sprintf(northing, "%07d", NorthingInt);
    Temp.append(northing);
    
    return Temp;
}
//added to NDK
std::string ElevationConversions::GetMGRS(double Lat, double Lon, int Digits)
{
    char * sfmt = "%s";

    char sbuf[128] = {0};
    char Zone[10] = {0};
    char temp1[10],temp2[10];
    memset(temp1, 0, 9);
    memset(temp2, 0, 9);
    memset(Zone, 0, 9);
    char EastNorth[20] = {0};
    long precision = Digits;        // Maximum precision of easting & northing

    int Error_Code =  Convert_Geodetic_To_MGRS (Lat*DEG_2_RAD,
                                            Lon*DEG_2_RAD,
                                            precision,
                                            sbuf);
    
    if(precision > 5)
            precision = 5;

    std::string ZoneCS, ZoneCSL, ZoneCSR, Easting, Northing;
    
    memcpy(&Zone,&sbuf[0],5*sizeof(char));
    
    ZoneCS = convertStringChar(Zone, sfmt);
        
    int zonelength = ZoneCS.length();
    ZoneCSL = ZoneCS.substr(0, 3); //first attempt
    ZoneCSR = ZoneCS.substr(zonelength-2, 2);

    memcpy(&temp1[0],&sbuf[5],5*sizeof(char));

    Easting = convertStringChar(temp1, sfmt);
    memcpy(&temp2[0],&sbuf[10],5*sizeof(char));
    Northing = convertStringChar(temp2, sfmt);

    std::string MGRSString;    
    MGRSString.append(ZoneCSL);
    MGRSString.append(" ");
    MGRSString.append(ZoneCSR);
    MGRSString.append(" ");
    MGRSString.append(Easting);
    MGRSString.append(" ");
    MGRSString.append(Northing);

    if(!Error_Code)
        return MGRSString;
    else
        return "";
}

//added to ndk
std::string ElevationConversions::getSpheroidString()
{
    std::string result="";
    if(this->UsingEGM96)//used to be this->UsingEGM96
    {
        result = "EGM96";
    }
    else
        result= "EGM84";
    return result;
}//end of getSpheroidString
//added to ndk
std::string ElevationConversions::getDatumString()
{
    std::string result="";

    result = "WGS 84";
    return result;

}//end getDatumString

//get corners
bool ElevationConversions::getCorners(double centerlat, double centerlon, float length, float width, float angle, int numcorners,
        double *EndLat, double *EndLon)
{
    if(numcorners != 4)//only want 4 corners now
        return false;
    int i=0;
    double templat, templon;
    //set each corner to the correct offset from the center
    //corners are numbered, starting @ the angle passed (clockwise).

    //[TOP_RIGHT].
    FindCorner(
        (float)(angle),
        (float)(length/2.0),
        (float)(width/2.0),
        centerlat,
        centerlon,
        &templat,
        &templon);
    EndLat[i]=templat;
    EndLon[i]=templon;
    i++;

    //BottonRight
    FindCorner(
        (float)(angle),
        (float)(-1*length/2.0),
        (float)(width/2.0),
        centerlat,
        centerlon,
        &templat,
        &templon);

    EndLat[i]=templat;
    EndLon[i]=templon;
    i++;
    //BottomLeft
    FindCorner(
        (float)(angle),
        (float)(-1*length/2.0),
        (float)(-1*width/2.0),
        centerlat,
        centerlon,
        &templat,
        &templon);

    EndLat[i]=templat;
    EndLon[i]=templon;
    i++;
    //TopLeft
    FindCorner(
        (float)(angle),
        (float)(length/2.0),
        (float)(-1*width/2.0),
        centerlat,
        centerlon,
        &templat,
        &templon);
    EndLat[i]=templat;
    EndLon[i]=templon;

    return true;
}//end of getCorners

//called by getCorners
bool ElevationConversions::FindCorner(float angle, float length, float width, double startlat, double startlon, double *EndLat, double *EndLon)
{
    float BaseAngle = (float)(RAD_2_DEG * atan(width/length));
    float FinalAngle = BaseAngle + angle;

    float Dist2Corner= sqrt((length*length)+(width*width));

    if(length<0 )
    {
        FinalAngle += 180;
    }
    //static bool AROffset(double StartLat, double StartLon,double *EndLat, double *EndLon, float Azimuth, float Range_m);

    AROffset(startlat, startlon,EndLat, EndLon, (float)FinalAngle, (float)Dist2Corner);

    return true;

}//end of FindCorner

float ElevationConversions::GetMagAngle(float TrueAngle, double Lat, double Lon)
{
    //LOU come back to this part...
    float declination = WMM_FindDeclination(Lat, Lon);

    float AngleOut = TrueAngle + declination;
    return AngleOut;
}

float ElevationConversions::GetTrueAngle(float MagAngle, double Lat, double Lon)
{
    float declination = WMM_FindDeclination(Lat, Lon);

    float AngleOut = MagAngle - declination;
    return AngleOut;
}

float ElevationConversions::GetDeclination(double Lat, double Lon)
{
    return WMM_FindDeclination(Lat, Lon);
}

float ElevationConversions::WMM_FindDeclination(double Lat, double Lon)
{
    int NumTerms = 1;
    WMMtype_MagneticModel *MagneticModel, *TimedMagneticModel;
    WMMtype_Ellipsoid Ellip;
    WMMtype_Geoid Geoid;
    WMMtype_CoordGeodetic CoordGeodetic;
    WMMtype_CoordSpherical CoordSpherical;
    WMMtype_GeoMagneticElements GeoMagneticElements;
    WMMtype_Date UserDate;

    //char filename[] = "WMM.COF";
    Geoid.Geoid_Initialized = 0;

    NumTerms = ( ( WMM_MAX_MODEL_DEGREES + 1 ) * ( WMM_MAX_MODEL_DEGREES + 2 ) / 2 );
    MagneticModel = WMM_AllocateModelMemory(NumTerms);
    TimedMagneticModel  = WMM_AllocateModelMemory(NumTerms);

    WMM_SetDefaults(&Ellip, MagneticModel, &Geoid); /* Set default values and constants */
    WMM_readMagneticModel(/*filename,*/ MagneticModel);
    //WMM_InitializeGeoid(&Geoid);    /* Read the Geoid file */

    CoordGeodetic.lambda=Lon; // lamda is LON
    CoordGeodetic.phi=Lat; // phi is LAT
    CoordGeodetic.HeightAboveGeoid = 0;
    CoordGeodetic.HeightAboveEllipsoid = 0;
    /*
    CTime NowTime;
    NowTime = CTime::GetCurrentTime();
    CTime StartTime(NowTime.GetYear(), 1, 1,0,0,0);
    CTimeSpan DaysPassed = NowTime-StartTime;
    float FractionYear = (float)(DaysPassed.GetDays()/365.0);
    float Year = FractionYear+ NowTime.GetYear();

    UserDate.Year = NowTime.GetYear();
    UserDate.Month = NowTime.GetMonth();
    UserDate.Day = NowTime.GetDay();
    UserDate.DecimalYear = Year;*/

    //MIKE got rid of CTime
    time_t t = time(0);   // get time now
    struct tm * now = localtime( & t );
    float days = now->tm_yday;
    float frac = days/365;

    float month = now->tm_mon +1;
    float day = now->tm_mday;
    float year = now->tm_year + 1900;
    float decyear = year + frac;

    UserDate.Year = year;
    UserDate.Month = month;
    UserDate.Day = day;
    UserDate.DecimalYear = decyear;



    //WMM_ConvertGeoidToEllipsoidHeight(&CoordGeodetic, &Geoid);
    //CoordGeodetic.HeightAboveEllipsoid = 0;
    WMM_GeodeticToSpherical(Ellip, CoordGeodetic, &CoordSpherical);
    WMM_TimelyModifyMagneticModel(UserDate, MagneticModel, TimedMagneticModel);
    WMM_Geomag(Ellip, CoordSpherical, CoordGeodetic, TimedMagneticModel, &GeoMagneticElements);

    //WMM_CalculateGridVariation(CoordGeodetic,&GeoMagneticElements);

    //MIKE added stuff to free memory ( Louis found a mem leak)
    //probably a good idea to free the magnetic models........
    WMM_FreeMagneticModelMemory(MagneticModel);
    WMM_FreeMagneticModelMemory(TimedMagneticModel);

    return (float)GeoMagneticElements.Decl;
}

std::string convertString( float i, char * format)
{
    char word[8];
    sprintf(word,format,i );

    std::string s(word);

    return s;
}

std::string convertStringChar( char* letters, char * format)
{
    char word[8];
    sprintf(word,format,letters );

    std::string s(word);

    return s;
}

float ElevationConversions::GetMGRSHeadingOffset( double Lat, double Lon, double Heading)
{
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, " GetMGRSHeadingOffset Lat: %f Lon: %f",Lat, Lon );

    double test = Lat_Long_to_C(Lat,Lon, CONV_A, CONV_B, CONV_F0);
     float offset = Heading - test;

     __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, " offset Heading: %f -  test: %f",Heading, test );

     return offset;
}



