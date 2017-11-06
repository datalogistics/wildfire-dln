#include <jni.h>
#include <android/log.h>
//geoid transforms

//#include "stdafx.h"

//#include "Stringtok.h"
//#include "loccart.h"
//#include "xform_server.h"
//#include "geocent.h"

#include "geoid.h"


#define NumbGeoidCols 1441   // 360 degrees of longitude at 15 minute spacing
#define NumbGeoidRows  721   // 180 degrees of latitude  at 15 minute spacing
#define NumbHeaderItems 6    // min, max lat, min, max long, lat, long spacing
#define ScaleFactor     4    // 4 grid cells per degree at 15 minute spacing
#define NumbGeoidElevs NumbGeoidCols * NumbGeoidRows
#define PI 3.14159265358979323846
#define LITTLE_ENDIAN

static long  Geoid_Initialized = 0;  // indicates successful initialization
/*
FILE  *GeoidHeightFile;

float GeoidHeightBuffer[NumbGeoidElevs];
/*
long Initialize_Geoid( )
//*
// * The function Initialize_Geoid reads geoid separation data from a file in
//* the current directory and builds the geoid separation table from it.
//* If the separation file can not be found or accessed, an error code of
//* GEOID_FILE_OPEN_ERROR is returned, If the separation file is incomplete
//* or improperly formatted, an error code of GEOID_INITIALIZE_ERROR is returned,
//* otherwise GEOID_NO_ERROR is returned.

{
  int   ItemsRead = 0;
  long  ElevationsRead = 0;
  long  ItemsDiscarded = 0;
  long  num = 0;
  char  FileName[128];
  char  *PathName = getenv( "GEOID_DATA" );// LOADING DIFFERENT FILES

  if (Geoid_Initialized)
  {
    return (GEOID_NO_ERROR);
  }

////////////////////////////////////////////////////////////////
            HKEY key;
            unsigned char buffer[1024];
            DWORD buffer_size = 1024;
            DWORD type;
//MAybe get rid of this reading files, perhaps put into a .h file//
            if(RegOpenKeyEx(HKEY_LOCAL_MACHINE, "SOFTWARE\\AFRL Rome Research Site - Rome, NY\\", 0,
                KEY_READ, &key)!=0)
            {
            }
            //query value
            if(RegQueryValueEx(key, "Install Path", 0, &type, buffer, &buffer_size)!=0)
            {
            //    AfxMessageBox("Couldn't open key");
            }
            //close key
            RegCloseKey(key);
////////////////////////////////////////////////////////////////

    sprintf(FileName,"c:\\pfps\\falcon\\data\\dztool\\egm96.grd");
//*  Open the File READONLY, or Return Error Condition:
  if ( ( GeoidHeightFile = fopen( FileName, "rb" ) ) == NULL)
  {
    return ( GEOID_FILE_OPEN_ERROR);
  }

  strcat( FileName, "egm96.grd 123" );
//  Skip the Header Line:

  while ( num < NumbHeaderItems )
  {
    if (feof( GeoidHeightFile )) break;
    if (ferror( GeoidHeightFile )) break;
    GeoidHeightBuffer[num] = Read_Geoid_Height( &ItemsRead );
    printf(" num, GeoidHeightBuffer[num] = %d,%f\n",num,GeoidHeightBuffer[num]);
    ItemsDiscarded += ItemsRead;
    num++;
  }

//  Determine if header read properly, or NOT:

  if (GeoidHeightBuffer[0] !=  -90.0 ||
      GeoidHeightBuffer[1] !=   90.0 ||
      GeoidHeightBuffer[2] !=    0.0 ||
      GeoidHeightBuffer[3] !=  360.0 ||
      GeoidHeightBuffer[4] !=  ( 1.0 / ScaleFactor ) ||
      GeoidHeightBuffer[5] !=  ( 1.0 / ScaleFactor ) ||
      ItemsDiscarded != NumbHeaderItems)
  {
    fclose(GeoidHeightFile);
    return ( GEOID_INITIALIZE_ERROR );
  }

//  Extract elements from the file:

  num = 0;
  while ( num < NumbGeoidElevs )
  {
    if (feof( GeoidHeightFile )) break;
    if (ferror( GeoidHeightFile )) break;
    GeoidHeightBuffer[num] = Read_Geoid_Height ( &ItemsRead );
    ElevationsRead += ItemsRead;
    num++;
  }

//  Determine if all elevations of file read properly, or NOT:

  if (ElevationsRead != NumbGeoidElevs)
  {
    fclose(GeoidHeightFile);
    return ( GEOID_INITIALIZE_ERROR );
  }

  fclose(GeoidHeightFile);
  Geoid_Initialized = 1;
  return ( GEOID_NO_ERROR );
}


float Read_Geoid_Height ( int *NumRead )

// * The private function Read_Geoid_Height returns the geoid height
// * read from the geoid file. 4 bytes are read from the file and,
// * if necessary, the bytes are swapped.
// *
// *    NumRead             : Number of heights read from file         (output)

{
     float result;
    char* swap = (char*)&result;
    char temp;
    *NumRead = fread( swap, 4, 1, GeoidHeightFile );
#ifdef LITTLE_ENDIAN
    temp = swap[0];
    swap[0] = swap[3];
    swap[3] = temp;
    temp = swap[1];
    swap[1] = swap[2];
    swap[2] = temp;
#endif
    return result;
}


long Get_Geoid_Height ( double Latitude,
                        double Longitude,
                        double *DeltaHeight )

// * The private function Get_Geoid_Height returns the height of the
// * WGS84 geiod above or below the WGS84 ellipsoid,
// * at the specified geodetic coordinates,
// * using a grid of height adjustments from the EGM96 gravity model.
// *
// *    Latitude            : Geodetic latitude in radians           (input)
// *    Longitude           : Geodetic longitude in radians          (input)
// *    DeltaHeight         : Height Adjustment, in meters.          (output)
//
{
  long    Index;
  double DeltaX, DeltaY;
  double ElevationSE, ElevationSW, ElevationNE, ElevationNW;
  double LatitudeDD, LongitudeDD;
  double OffsetX, OffsetY;
  double PostX, PostY;
  double UpperY, LowerY;
  long Error_Code = 0;

  if (!Geoid_Initialized)
  {
    return (GEOID_NOT_INITIALIZED_ERROR);
  }
  if ((Latitude < -PI/2.0) || (Latitude > PI/2.0))
  { // Latitude out of range
    Error_Code |= GEOID_LAT_ERROR;
  }
  if ((Longitude < -PI) || (Longitude > (2*PI)))
  { // Longitude out of range
    Error_Code |= GEOID_LON_ERROR;
  }

  if (!Error_Code)
  { // no errors
    LatitudeDD  = Latitude  * 180.0 / PI;
    LongitudeDD = Longitude * 180.0 / PI;

    //  Compute X and Y Offsets into Geoid Height Array:

    if (LongitudeDD < 0.0)
    {
      OffsetX = ( LongitudeDD + 360.0 ) * ScaleFactor;
    }
    else
    {
      OffsetX = LongitudeDD * ScaleFactor;
    }
    OffsetY = ( 90.0 - LatitudeDD ) * ScaleFactor;

    //  Find Four Nearest Geoid Height Cells for specified Latitude, Longitude;
    //  Assumes that (0,0) of Geoid Height Array is at Northwest corner:

    PostX = floor( OffsetX );
    if ((PostX + 1) == NumbGeoidCols)
      PostX--;
    PostY = floor( OffsetY );
    if ((PostY + 1) == NumbGeoidRows)
      PostY--;

    Index = (long)(PostY * NumbGeoidCols + PostX);
    ElevationNW = GeoidHeightBuffer[ Index ];
    ElevationNE = GeoidHeightBuffer[ Index+ 1 ];

    Index = (long)((PostY + 1) * NumbGeoidCols + PostX);
    ElevationSW = GeoidHeightBuffer[ Index ];
    ElevationSE = GeoidHeightBuffer[ Index + 1 ];

    //  Perform Bi-Linear Interpolation to compute Height above Ellipsoid:

    DeltaX = OffsetX - PostX;
    DeltaY = OffsetY - PostY;

    UpperY = ElevationNW + DeltaX * ( ElevationNE - ElevationNW );
    LowerY = ElevationSW + DeltaX * ( ElevationSE - ElevationSW );

    *DeltaHeight = UpperY + DeltaY * ( LowerY - UpperY );
  }
  return Error_Code;
}


long Convert_Ellipsoid_To_Geoid_Height ( double Latitude,
                                         double Longitude,
                                         double Ellipsoid_Height,
                                         double *Geoid_Height )

// * The function Convert_Ellipsoid_To_Geoid_Height converts the specified WGS84
/// * ellipsoid height at the specified geodetic coordinates to the equivalent
// * geoid height, using the EGM96 gravity model.
// *
// *    Latitude            : Geodetic latitude in radians           (input)
// *    Longitude           : Geodetic longitude in radians          (input)
// *    Ellipsoid_Height    : Ellipsoid height, in meters            (input)
// *    Geoid_Height        : Geoid height, in meters.               (output)

{
  // On error return Ellipsoid_Height = Geoid_Height
  double  DeltaHeight = 0;
  long Error_Code;
  Error_Code = Get_Geoid_Height ( Latitude, Longitude, &DeltaHeight );
  *Geoid_Height = Ellipsoid_Height - DeltaHeight;
  return ( Error_Code );
}


long Convert_Geoid_To_Ellipsoid_Height ( double Latitude,
                                         double Longitude,
                                         double Geoid_Height,
                                         double *Ellipsoid_Height )

// * The function Convert_Geoid_To_Ellipsoid_Height converts the specified WGS84
// * geoid height at the specified geodetic coordinates to the equivalent
// * ellipsoid height, using the EGM96 gravity model.
// *
// *    Latitude            : Geodetic latitude in radians           (input)
// *    Longitude           : Geodetic longitude in radians          (input)
// *    Ellipsoid_Height    : Ellipsoid height, in meters            (input)
// *    Geoid_Height        : Geoid height, in meters.               (output)
// *

{
  // On error return Ellipsoid_Height = Geoid_Height
  double  DeltaHeight = 0;
  long Error_Code;
  Error_Code = Get_Geoid_Height ( Latitude, Longitude, &DeltaHeight );
  *Ellipsoid_Height = Geoid_Height + DeltaHeight;
  return ( Error_Code );
}


*/
void DoD_Convert_AARtoLLA(double * goodguy_lla,double * badguy_aar,double * badguy_lla)
{
    char log [512];

    __android_log_write(ANDROID_LOG_INFO, "TRANSFORMS!!!!", "SLOATTTTTTT");


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

