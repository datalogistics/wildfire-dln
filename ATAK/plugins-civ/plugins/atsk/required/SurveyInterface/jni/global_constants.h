/**********************************************************************
* Disclaimer:    This software was developed for the Department of    *
*                Defense (DoD). All US Government restrictions on     *
*                software source code distribution apply              *
*                                                                      *
*  PROPERTY OF AFRL/RRS                                               *
**********************************************************************/
/* ************************************************************************** */
/*                                                                            */
/*                             BANNER CORE by TASC                            */
/*                                                                            */
/*  Item Number: BC-SIM-SUP-039                             21-Jan-1998       */
/*                                                                            */
/* ************************************************************************** */
#ifndef Global_H
#define Global_H
#include <math.h>
//#include "MessageStructures.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

//                Trigonometric Constants

#define Degrees_to_Radians (M_PI / 180.0)
#define Radians_to_Degrees (180.0 / M_PI)

//                Physical Constants

#define Speed_of_Light      2.998e8     // m/sec
#define Equitorial_Radius   6378.137    // Kilometers
#define Polar_Radius        6356.7523   // Kilometers

//Earth's radius as defined by WGS-84 is as follows:
//
//    Equatorial radius = 6,378,137.0 m
//    Polar radius      = 6,356,752.3142 m


#define Boltzmann_Constant  1.3805e-23  // Joule / Degree Kelvin
#define Plancks_Constant    6.625e-34   // Joule * Seconds 
#define Electronic_Charge   1.602e-19   // Coulombs
#define GM                  3.986e14    // m^3 / sec^2
#define Earth_Angular_Rate  7.292115E-5 // Radians/Second
//#define Earth_Angular_Rate 0.0

//                Scale Constants

#define Kilo_Constant       1.0e3
#define Centi_Constant      1.0e-2
#define Micro_Constant      1.0e-6
#define Nano_Constant       1.0e-9


#endif
