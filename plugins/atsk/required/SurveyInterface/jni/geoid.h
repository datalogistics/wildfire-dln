#ifndef GEOID_H
#define GEOID_H

#ifdef __cplusplus
extern "C" {
#endif

  int tempcall();
  void arrayadder(int size, double *array1, double *array2,double *result);

  void DoD_Convert_AARtoLLA(double * goodguy_lla,double * badguy_aar,double * badguy_lla);
  void DoD_Convert_LLAtoAAR(double *goodguy_lla,double *badguy_lla,double *badguy_aar);
  long Initialize_Geoid ();
/*
 * The function Initialize_Geiud reads geoid separation data from a file in
 * the current directory and builds the geoid separation table from it.  If an
 * error occurs, the error code is returned, otherwise GEOID_NO_ERROR is
 * returned.
 */

  long Get_Geoid_Height ( double Latitude,
                        double Longitude,
                        double *DeltaHeight );

  long Convert_Ellipsoid_To_Geoid_Height (double Latitude,
                                          double Longitude,
                                          double Ellipsoid_Height,
                                          double *Geoid_Height);
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

  long Convert_Geoid_To_Ellipsoid_Height (double Latitude,
                                          double Longitude,
                                          double Geoid_Height,
                                          double *Ellipsoid_Height);
/*
 * The function Convert_Geoid_To_Ellipsoid_Height converts the specified WGS84
 * geoid height at the specified geodetic coordinates to the equivalent
 * ellipsoid height, using the EGM96 gravity model.
 *
 *    Latitude            : Geodetic latitude in radians           (input)
 *    Longitude           : Geodetic longitude in radians          (input)
 *    Geoid_Height        : Geoid height, in meters                (input)
 *    Ellipsoid_Height    : Ellipsoid height, in meters.           (output)
 *
 */

#ifdef __cplusplus
}
#endif

#endif /* GEOID_H */
