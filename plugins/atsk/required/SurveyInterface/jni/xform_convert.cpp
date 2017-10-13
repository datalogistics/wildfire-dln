/**********************************************************************
* Disclaimer:    This software was developed for the Department of    *
*                Defense (DoD). All US Government restrictions on     *
*                software source code distribution apply              *
*                                                                      *
*  PROPERTY OF AFRL/RRS                                               *
**********************************************************************/

/*      
    File - xform_convert.c
    Transform-Server Conversion Library
    13-Mar-98 GAB
        22-June-99 Mike Moore: vectorize.  Replace matrix_operation with
         its component functions so as to avoid overhead.
*/


//#include "StdAfx.h"

#include <stdio.h>
#include <math.h>
#include "mini_blas_vec.h"
#include "linalg.h"
#include "global_constants.h"
#include "xform_server.h"
#include <string.h>

#ifdef WSSP_PLATFORM
#include "blas.h"
#include "vec.h"
#include "isa_mp.h"
#include "pmpt.h"
#endif


#ifndef PUBLIC
  #define PUBLIC
#endif

#ifndef PRIVATE
  #define PRIVATE static
#endif

/* --------------------------------------------------------- */ 
PUBLIC void matrix_operation( int Operation
                , double * Arg1
                , double * Arg2
                , double * Result 
                )
{
/*
   General matrix operation function 
   Assumes Matrix Data is stored in (Row,Col) order
   { In[0][0],In[0][1],In[0][2],In[1][0],In[1][1],... In[3][3] } 
*/

  #ifdef PROFILE
  ELABORATE_PKEY(PKEY);
  ELABORATE_PROFILE_FOR(matrix_operation, PKEY);
  #endif

  /* Arg1 and Arg2 can be either 1x3 vectors or 3x3 matrices, depending on "Operation". */

  switch ( Operation )
  {
    case MATRIXxMATRIX :
    {
      MatMatMult3X3Double(Result, Arg1, Arg2);
    } break;

    case MATRIXxVECTOR :
    {
      /* This represents the normal Matrix times a Vector */
      /* 
            a11 a12 a13| b1  =  a11*b1+ a12*b2 + a13*b3
            a21 a22 a23| b2  =  a21*b1+ a22*b2 + a23*b3                                                 
            a31 a32 a33| b3  =  a31*b1+ a32*b2 + a33*b3
      */

      MatVecMult3X3X3X1Double( Result, Arg1, Arg2);

      /* ================= replaced    
      Result[0] = ddot( 3,   Arg1, 1, Arg2, 1);
      Result[1] = ddot( 3, Arg1+3, 1, Arg2, 1); 
      Result[2] = ddot( 3, Arg1+6, 1, Arg2, 1);
      ================= */

    } break;
    
    case MATRIXTxMATRIX :
    {
      /* This represents matrix transpose times a matrix multplying */
      /*                  
        a11 a21 a31| b11 b12 b13
        a12 a22 a32| b21 b22 b23
        a13 a23 a33| b31 b32 b33
      */
      /* 3X3 matrix multiply. */
      
      /* 1st row */
      *(Result+0) = ddot( 3, Arg1+0, 3, Arg2+0, 3);  /* a11b11+a21b21+a31b31 */
      *(Result+1) = ddot( 3, Arg1+0, 3, Arg2+1, 3);  /* a11b12+a21b22+a31b32 */
      *(Result+2) = ddot( 3, Arg1+0, 3, Arg2+2, 3);  /* a11b13+a21b23+a31b33 */

      /* 2nd row */
      *(Result+3) = ddot( 3, Arg1+1, 3, Arg2+0, 3);  /* a12b11+a22B21+a32b31 */ 
      *(Result+4) = ddot( 3, Arg1+1, 3, Arg2+1, 3);  /* a12b12+a22b22+a32b32 */
      *(Result+5) = ddot( 3, Arg1+1, 3, Arg2+2, 3);  /* a12b13+a22b23+a32b33 */

      /* 3rd row */
      *(Result+6) = ddot( 3, Arg1+2, 3, Arg2+0, 3);  /* a13b11+a23b21+a33b31 */ 
      *(Result+7) = ddot( 3, Arg1+2, 3, Arg2+1, 3);  /* a13b12+a23b22+a33b32 */ 
      *(Result+8) = ddot( 3, Arg1+2, 3, Arg2+2, 3);  /* a13b13+a23b23+a33b33 */
  
    } break;


    case MATRIXTxVECTOR : 
    {
      /* This represents taking the Transpose first then multplying times a Vector */ 
      /*
            a11 a21 a31| b1  =  a11*b1+ a21*b2 + a31*b3
            a12 a22 a32| b2  =  a12*b1+ a22*b2 + a32*b3
            a13 a23 a33| b3  =  a13*b1+ a23*b2 + a33*b3
      */
      *(Result+0) = ddot( 3, Arg1+0, 3, Arg2, 1);  /* a11b1+a21b2+a31b3 */ 
      *(Result+1) = ddot( 3, Arg1+1, 3, Arg2, 1);  /* a12b1+a22b2+a32b3 */
      *(Result+2) = ddot( 3, Arg1+2, 3, Arg2, 1);  /* a13b1+a23b2+a33b3 */  
    } break;

    case MATRIX_TRANSPOSE :   /* It's always a 3X3 */
    {
      /* do the diagonal */
      *Result     = *(Arg1);
      *(Result+4) = *(Arg1+4);
      *(Result+8) = *(Arg1+8);

      /* Obliques */
      *(Result+1) = *(Arg1+3);
      *(Result+3) = *(Arg1+1);

      *(Result+2) = *(Arg1+6);
      *(Result+6) = *(Arg1+2);

      *(Result+5) = *(Arg1+7);
      *(Result+7) = *(Arg1+5);
     
    } break;

    case VECTOR_ADD : /* always a 3 element vector */
    {
      /* It's faster to avoid the libvec call, even on WSSP,
         because the dvadd takes 37 ticks, and there is about 15 ticks
         of packaging that go along with it.  It's just not the right
         thing to do for a 3 element vector.
      */

      /* This should be about 45 ticks on WSSP*/
      *Result = *Arg1 + *Arg2;
      *(Result+1) = *(Arg1+1) + *(Arg2+1);
      *(Result+2) = *(Arg1+2) + *(Arg2+2);
    } break;
  
    case VECTOR_SUBTRACT : 
    {
      *Result     = *Arg1     - *Arg2;
      *(Result+1) = *(Arg1+1) - *(Arg2+1);
      *(Result+2) = *(Arg1+2) - *(Arg2+2);
    } break;


    case VECTORxDOT :      
    {  
      /* a1a2 + b1b2 + c1c2, plus magnitude and angle. */
      Result[0] = ddot(3, Arg1, 1, Arg2, 1);

      /* |U|*|V| */
      Result[1] = sqrt( (Arg1[0] * Arg1[0])
                       +(Arg1[1] * Arg1[1])
                       +(Arg1[2] * Arg1[2])
                      )
                * sqrt( (Arg2[0] * Arg2[0])
                       +(Arg2[1] * Arg2[1])
                       +(Arg2[2] * Arg2[2])
                      ); 

      /* angle */ 
      Result[2] = acos( Result[0] /  Result[1]);
    } break;

    case VECTORxCROSS :    
    {    /* U x V */
      Result[0] = Arg1[1]*Arg2[2] - Arg1[2]*Arg2[1];
      Result[1] = Arg1[2]*Arg2[0] - Arg1[0]*Arg2[2];
      Result[2] = Arg1[0]*Arg2[1] - Arg1[1]*Arg2[0];
    } break;

    default : 
    {
        printf("Error in matrix_operation(), Operation #%d undefined.\n",Operation);
    }
  }
  #ifdef PROFILE
  PROFILE_EXIT(PKEY);
  #endif
}
/* --------------------------------------------------------- */ 
/* Primative Transforms */

PUBLIC void idenity( double Out_Matrix[3][3])
{
  /* Generate an Idenity Matrix  Output: Idenity Matrix      */
  static double ident[3][3] = { {1.0, 0.0, 0.0},
                                {0.0, 1.0, 0.0},
                                {0.0, 0.0, 1.0}
                              };
   memcpy(Out_Matrix, ident, 3*3*sizeof(double));

}

/* --------------------------------------------------------- */ 

PUBLIC void roll_x ( double In_Matrix[3][3]
           , double Roll_Angle
           , double Out_Matrix[3][3] 
           )
{
/* Straight Roll around the X axis 
   Input:     Transform Martix, Roll angle (radians)
   Output:    Rotated Transform Martix         */

   double    Cor,Sir;
   double Roll_Matrix[3][3];

   #ifdef PROFILE
   ELABORATE_PKEY(PKEY);
   ELABORATE_PROFILE_FOR(roll_x, PKEY);
   #endif

   Cor = cos(Roll_Angle);
//   Cor = cos(Roll_Angle);
//   Sir = sqrt(1.0- (Cor*Cor));

   Sir = sin(Roll_Angle);
//   Sir = sin(Roll_Angle);

   Roll_Matrix[0][0] =  1.0; Roll_Matrix[0][1] =  0.0; Roll_Matrix[0][2] =  0.0;
   Roll_Matrix[1][0] =  0.0; Roll_Matrix[1][1] =  Cor; Roll_Matrix[1][2] =  Sir;
   Roll_Matrix[2][0] =  0.0; Roll_Matrix[2][1] = -Sir; Roll_Matrix[2][2] =  Cor;

   MatMatMult3X3Double( (double*)Out_Matrix, (double*)Roll_Matrix, (double*)In_Matrix);

  //?? delete  matrix_operation( MATRIXxMATRIX, (double*)Roll_Matrix, (double*)In_Matrix, (double*)Out_Matrix );


  #ifdef PROFILE
  PROFILE_EXIT(PKEY);
  #endif
}

PUBLIC void pitch_y ( double In_Matrix[3][3]
            , double Pitch_Angle
            , double Out_Matrix[3][3] 
            )
{
/* Straight Pitch around the Y axis 
   Input:     Transform Martix, Pitch angle (radians)
   Output:    Rotated Transform Martix             */
   double       Pitch_Matrix[3][3];
   double    Cop,Sip;

   #ifdef PROFILE
   ELABORATE_PKEY(PKEY);
   ELABORATE_PROFILE_FOR(pitch_y,PKEY);
   #endif

   Cop = cos(Pitch_Angle);
/*   Sip = sqrt(1.0- (Cop*Cop));
*/
   Sip = sin(Pitch_Angle);
   // Sip = sin(Pitch_Angle);
   Pitch_Matrix[0][0] =  Cop; Pitch_Matrix[0][1] =  0.0; Pitch_Matrix[0][2] = -Sip;
   Pitch_Matrix[1][0] =  0.0; Pitch_Matrix[1][1] =  1.0; Pitch_Matrix[1][2] =  0.0;
   Pitch_Matrix[2][0] =  Sip; Pitch_Matrix[2][1] =  0.0; Pitch_Matrix[2][2] =  Cop;
   MatMatMult3X3Double( (double*)Out_Matrix
              , (double*)Pitch_Matrix
              , (double*)In_Matrix
              );
  #ifdef PROFILE
  PROFILE_EXIT(PKEY);
  #endif
}

PUBLIC void yaw_z ( double In_Matrix[3][3]
          , double Yaw_Angle
          , double Out_Matrix[3][3] )
{
/* Straight Yaw around the Z axis
   Input:     Transform Martix, Yaw angle (radians)
   Output:    Rotated Transform Martix             */
   double       Yaw_Matrix[3][3];
   double    Coy,Siy;

   #ifdef PROFILE
   ELABORATE_PKEY(PKEY);
   ELABORATE_PROFILE_FOR(yaw_z,PKEY);
   #endif

/*   Coy = cos(Yaw_Angle);
*/
   Coy = cos(Yaw_Angle);
/*   Siy = sqrt(1.0- (Coy*Coy));
*/
/*   Siy = sin(Yaw_Angle);
*/
   Siy = sin(Yaw_Angle);
   //printf("yaw_z, Coy=%e Siy=%e\n",Coy,Siy);

   Yaw_Matrix[0][0] =  Coy; Yaw_Matrix[0][1] =  Siy; Yaw_Matrix[0][2] =  0.0;
   Yaw_Matrix[1][0] = -Siy; Yaw_Matrix[1][1] =  Coy; Yaw_Matrix[1][2] =  0.0;
   Yaw_Matrix[2][0] =  0.0; Yaw_Matrix[2][1] =  0.0; Yaw_Matrix[2][2] =  1.0;
   MatMatMult3X3Double( (double*)Out_Matrix, (double*)Yaw_Matrix, (double*)In_Matrix);
   #ifdef PROFILE
   PROFILE_EXIT(PKEY);
   #endif

 }

void ecr2eci (double  Simulation_Time, double *ECR_Position,double * ECI_Position )
{
/* ECR to ECI Transform.
   Inputs:  Simulation_Time = Seconds, ECR_Position = ECR x,y,z position of Target.
   Outputs: ECI_Position = ECI x,y,z position of Target. */
/* Modified from "Banner Core" by Larry Mabius, "geodetic_functions.cc". */
    double Earth_Orientation[3][3];

    #ifdef PROFILE
    ELABORATE_PKEY(PKEY);
    ELABORATE_PROFILE_FOR(ecr2eci,PKEY);
    #endif
    
    Calculate_Earth_Orientation ( Simulation_Time, Earth_Orientation );
    MatTVecMult3X3X3X1Double(ECI_Position, (double*)Earth_Orientation, ECR_Position);

    #ifdef PROFILE
    PROFILE_EXIT(PKEY);
    #endif
}

PUBLIC void ecr2eci_posvel ( double Simulation_Time
                           , double *ECR_Position
                           , double *ECR_Velocity
                           , double *ECI_Position
                           , double *ECI_Velocity 
                           )
{
  /* ECR to ECI Transform, Position and Velocity.
     Inputs:  Simulation_Time = Seconds
              ECR_Position
              ECR_Velocity = ECR x,y,z of Target.
     Outputs: ECI_Position, ECI_Velocity = ECI x,y,z of Target. 

     Modified from "Banner Core" by Larry Mabius, "geodetic_functions.cc". 
  */
  double Earth_Orientation[3][3];

  #ifdef PROFILE
  ELABORATE_PKEY(PKEY);
  ELABORATE_PROFILE_FOR(ecr2eci_posvel,PKEY);
  #endif

  Calculate_Earth_Orientation ( Simulation_Time, Earth_Orientation );
  MatTVecMult3X3X3X1Double(  ECI_Position
                          , (double*)Earth_Orientation
                          , ECR_Position
                          );
  MatTVecMult3X3X3X1Double( ECI_Velocity
                         , (double*)Earth_Orientation
                         , ECR_Velocity
                          );
  ECI_Velocity[0] = ECI_Velocity[0] - Earth_Angular_Rate * ECI_Position[1];
  ECI_Velocity[1] = ECI_Velocity[1] + Earth_Angular_Rate * ECI_Position[0];

  #ifdef PROFILE
  PROFILE_EXIT(PKEY);
  #endif
}

PUBLIC void ecr2geo ( double * ECR_Position
                 , double * Altitude
                , double *Latitude
                , double *Longitude 
                )
{
   /* ECR to Geodetic Transform.
     Input:  ECR_Position = meters(1x3)
     Output: Altitude = Kilometers,
           Latitude, 
         Longitude = Radians North,East 
   
     Modified from "Banner Core" by Larry Mabius, "geodetic_functions.cc". 
   */

  double    Equitorial_Component,          /* Kilometers */
         Polar_Component,               /* Kilometers */
           ERC,                           /* Kilometers */
           PRS,                           /* Kilometers */
           S,                             /* 1/Kilometers */
        R,P,Q,                         /* Kilometers */
          RE,PE,                         /* Kilometers */
          Error;                         /* Kilometers */
  double    Equitorial_Radius_squared;

  const double Milli_Constant = 1.0e-3; /* to avoid a divide */

  double movement;
  static double Last_ECR[3] = {0.0,0.0,0.0};
  static double Last_GEO[3] = {0.0,0.0,0.0};
  static int init = 1;
  int    loop_count;
  
  #ifdef PROFILE
  ELABORATE_PKEY(PKEY);
  ELABORATE_PROFILE_FOR(ecr2geo,PKEY);
  #endif

  *Longitude = atan2 ( ECR_Position[1], ECR_Position[0] );
  Polar_Component = ECR_Position[2] * Milli_Constant;
  Equitorial_Component = sqrt ( ECR_Position[0]*ECR_Position[0]
                               + ECR_Position[1]*ECR_Position[1]
                   ) * Milli_Constant;
  Equitorial_Radius_squared = Equitorial_Radius*Equitorial_Radius;
 
  movement =  fabs(Last_ECR[0]-ECR_Position[0]) + fabs(Last_ECR[1]-ECR_Position[1]) + fabs(Last_ECR[2]-ECR_Position[2]);
  
  //printf(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  ecr2geo movement = %e\n",movement);

 
  if(init == 1 || movement > 20000.0 ){
    /* Initial Guess */
    *Altitude = 0.0;
    *Latitude = atan2 ( Polar_Component, Equitorial_Component );
    init = 0;
  } else {
    *Altitude = Last_GEO[0];
    *Latitude = Last_GEO[1]; 
  }
  
  loop_count = 1;
  /* Solve Iteratively */
  while(1==1)
  {
    double COSINE;
    double SINE;

    SINE = sin(*Latitude);
    //SINE = sin(*Latitude);
    //COSINE = cos(*Latitude);
    COSINE = sqrt(1.0-(SINE*SINE));
    //COSINE = cos(*Latitude);
    ERC = Equitorial_Radius * COSINE;
    PRS = Polar_Radius * SINE;
    S = 1.0/sqrt ( ERC*ERC + PRS*PRS );
    R = Equitorial_Radius*ERC*S + *Altitude * COSINE;
    P = Polar_Radius*PRS*S + *Altitude * SINE;
    RE = Equitorial_Component - R;
    PE = Polar_Component - P;
    Error = sqrt ( RE*RE + PE*PE );

    //printf(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  ecr2geo Error = %e\n",Error);

    if ( Error <= 0.001 /*Error_Tolerance*/ ) // DAVE: CHANGE Error_Tolerance to be 0.001.
    {
      break;
    }
    Q = Equitorial_Radius_squared*S + *Altitude;
    *Altitude += ( RE * COSINE + PE * SINE );
    *Latitude += ( PE * COSINE - RE * SINE ) / Q;
    
    loop_count += 1;
    
  } 
    
  //printf(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  loop_count = %d\n",loop_count);
  
  Last_GEO[0] = *Altitude;
  Last_GEO[1] = *Latitude; 
  Last_GEO[2] = *Longitude; 
  Last_ECR[0] = ECR_Position[0];
  Last_ECR[1] = ECR_Position[1]; 
  Last_ECR[2] = ECR_Position[2]; 
  
  #ifdef PROFILE
  PROFILE_EXIT(PKEY);
  #endif
}

void ecr2ned (double *Sensor_Position, double *ECR_Position, double *NED_Position )
{
/* ECR to North-East-Down Transform.
   Inputs:  Sensor_Position = Sensor ECR, ECR_Position = Target ECR
   Outputs: NED_Position = Target NED */
   double    Altitude,
        Latitude,
        Longitude,
        NED2ECR[3][3],
             Target_Position[3];

  #ifdef PROFILE
  ELABORATE_PKEY(PKEY);
  ELABORATE_PROFILE_FOR(ecr2ned,PKEY);
  #endif

  /* Calculate Geodetic coordinates */
  ecr2geo(Sensor_Position, &Altitude, &Latitude, &Longitude );
  /* Calculate ECR-to-NED transformation */
  /* Compute relative position of target = Sensor_Transform * (ECR_target_pos - ECR_sensor_pos) */
  Calculate_ned2ecr( Latitude, Longitude, NED2ECR );
  dvsub( 3, ECR_Position, 1, Sensor_Position, 1, Target_Position, 1);

  /* Apply the transformation matrix */
  /* Determine the ECR coordinates
   Multiplying the vector times the matrix saves us from doing a transpose */
   MatTVecMult3X3X3X1Double( NED_Position, (double*)NED2ECR, Target_Position);

   #ifdef PROFILE
   PROFILE_EXIT(PKEY);
   #endif
}

void ecr2nwu ( double * Sensor_Position
         , double * ECR_Position
         , double * NWU_Position )
{
  /* ECR to North-West-Up Transform.
     Inputs:  Sensor_Position = Sensor ECR, ECR_Position = Target ECR
     Outputs: NWU_Position = Target NWU 
  */

  double Altitude;
  double Latitude;
  double Longitude;
  double NWU2ECR[3][3];
  double Target_Position[3];

  #ifdef PROFILE
  ELABORATE_PKEY(PKEY);
  ELABORATE_PROFILE_FOR(ecr2nwu, PKEY);
  #endif

  /* Calculate Geodetic coordinates */

  ecr2geo(Sensor_Position, &Altitude, &Latitude, &Longitude );
  /* Calculate ECR-to-NWU transformation:
       Relative position of target = Sensor_Transform 
                    * (ECR_target_pos - ECR_sensor_pos) 
  */
  Calculate_nwu2ecr( Latitude, Longitude, NWU2ECR );
  dvsub( 3, ECR_Position, 1, Sensor_Position, 1, Target_Position,1);

  /* Apply the transformation matrix */
  /* Determine the ECR coordinates
  Multiplying the vector times the matrix saves us from doing a transpose */
  MatTVecMult3X3X3X1Double( NWU_Position, (double*)NWU2ECR, Target_Position);

  #ifdef PROFILE
  PROFILE_EXIT(PKEY);
  #endif
}
/* --------------------------------------------------------- */ 
void eci2ecr ( double Simulation_Time, double *ECI_Position,double * ECR_Position )
{
/* ECI to ECR Transform.
   Inputs:  Simulation_Time = Seconds, ECI_Position = ECI x,y,z position of Target.
   Outputs: ECR_Position = ECR x,y,z position of Target. */
/* Modified from "Banner Core" by Larry Mabius, "geodetic_functions.cc". */
    double Earth_Orientation[3][3];

    Calculate_Earth_Orientation ( Simulation_Time, Earth_Orientation );
    MatVecMult3X3X3X1Double(ECR_Position,(double*)Earth_Orientation, ECI_Position);
}

/* --------------------------------------------------------- */ 

void eci2ecr_posvel ( double Simulation_Time, double *ECI_Position,double * ECI_Velocity,
                                    double *ECR_Position, double *ECR_Velocity )
{
/* ECI to ECR Transform, Position and Velocity.
   Inputs:  Simulation_Time = Seconds, ECI_Position, ECI_Velocity = ECI x,y,z of Target.
   Outputs: ECR_Position, ECR_Velocity = ECR x,y,z of Target. */
/* Modified from "Banner Core" by Larry Mabius, "geodetic_functions.cc". */
    double  Earth_Orientation[3][3],
        Velocity[3];

    #ifdef PROFILE
    ELABORATE_PKEY(PKEY);
    ELABORATE_PROFILE_FOR(eci2ecr_posvel,PKEY);
    #endif

    Calculate_Earth_Orientation ( Simulation_Time, Earth_Orientation );
    MatVecMult3X3X3X1Double( ECR_Position,(double*)Earth_Orientation, ECI_Position);
/*
    printf("Returned ECR from Mat mult : \n");
    printf("%f %f %f \n",ECR_Position[0],ECR_Position[1],ECR_Position[2]);
*/
    Velocity[0]  = ECI_Velocity[0] + Earth_Angular_Rate * ECI_Position[1];
    Velocity[1]  = ECI_Velocity[1] - Earth_Angular_Rate * ECI_Position[0];
    Velocity[2]  = ECI_Velocity[2];
    MatVecMult3X3X3X1Double( ECR_Velocity,(double*)Earth_Orientation, Velocity);

    #ifdef PROFILE
    PROFILE_EXIT(PKEY);
    #endif
}

void geo2ecr (double Altitude,double Latitude,double Longitude,double * ECR_Position )
{

/* Geodetic to ECR Transform.
   Input:  Altitude = Kilometers,  Latitude, Longitude = Radians North,East
   Output: ECR_Position = meters(1x3) */
/* Modified from "Banner Core" by Larry Mabius, "geodetic_functions.cc". */
   double       ERC,
             PRS,
               S,R,P;
  double COSINE;
  double SINE;

  SINE = sin(Latitude);
//  COSINE = sqrt( 1.0 - (SINE*SINE) );
  COSINE = cos(Latitude);

  ERC = Equitorial_Radius * COSINE;
  PRS = Polar_Radius * SINE;

  S = ((double)(1.0))/sqrt ( ERC*ERC + PRS*PRS );
  R = Equitorial_Radius*ERC*S + Altitude * COSINE;
  P = Polar_Radius*PRS*S + Altitude * SINE;

  {
    double SL, CL;
    SL = sin(Longitude);
//    CL = sqrt( 1.0 - (SL*SL));
    CL = cos(Longitude);
    ECR_Position[0] = Kilo_Constant * R * CL;
    ECR_Position[1] = Kilo_Constant * R * SL;
    ECR_Position[2] = Kilo_Constant * P;
  }
}

/* --------------------------------------------------------- */ 

PUBLIC void ned2ecr ( double * Platform_Position
                 , double *  NED_Position
             , double * ECR_Position 
             )
{
  /* North-East-Down to ECR Transform.
     Inputs:  Platform_Position = Platform ECR
          NED_Position = Target NED
     Outputs: ECR_Position = Target ECR 
  */   

  double Altitude;
  double Latitude;
  double Longitude;
  double NED2ECR[3][3];
  double Target_Position[3];

  /* Calculate Geodetic coordinates */
  ecr2geo(Platform_Position, &Altitude, &Latitude, &Longitude );

  /* Calculate NED-to-ECR transformation */
  Calculate_ned2ecr( Latitude, Longitude, NED2ECR );

  /* Determine the ECR coordinates */
  /* Compute relative position of target as:
         Transform * (ECR_target_pos - ECR_sensor_pos) 
  */
  MatVecMult3X3X3X1Double( Target_Position
                         , (double*)NED2ECR
                 , NED_Position
                 );
  dvadd( 3, Platform_Position, 1, Target_Position, 1, ECR_Position,1);
  /* ======== replaced by dvadd 
  matrix_operation( VECTOR_ADD
          , Platform_Position
          , Target_Position
          , ECR_Position
          );
          ================ */
}

PUBLIC void nwu2ecr ( double * Platform_Position
             , double * NWU_Position
             , double * ECR_Position 
             )
{
  /* North-West_Up to ECR Transform.
     Inputs:  Platform_Position = Platform ECR
          NWU_Position = Target NWU
     Outputs: ECR_Position = Target ECR 
  */   

  double Altitude;
  double Latitude;
  double Longitude;
  double NWU2ECR[3][3];
  double Target_Position[3];

  #ifdef PROFILE
  ELABORATE_PKEY(PKEY);
  ELABORATE_PROFILE_FOR(nwu2ecr, PKEY);
  #endif

  /* Calculate Geodetic coordinates */
  ecr2geo(Platform_Position, &Altitude, &Latitude, &Longitude );

  /* Calculate NED-to-ECR transformation */
  Calculate_nwu2ecr( Latitude, Longitude, NWU2ECR );

  /* Determine the ECR coordinates */
  /* Compute relative position of target as:
    Transform * (ECR_target_pos - ECR_sensor_pos) 
  */
  MatVecMult3X3X3X1Double( Target_Position
                         , (double*)NWU2ECR
                 , NWU_Position
                 );

  dvadd( 3, Platform_Position, 1, Target_Position, 1, ECR_Position, 1);
  /* ==============
  matrix_operation( VECTOR_ADD
          , Platform_Position
          , Target_Position
          , ECR_Position
          );
  ============= */
   #ifdef PROFILE
   PROFILE_EXIT(PKEY);
   #endif 
  }

/* --------------------------------------------------------- */ 

void Calculate_ned2ecr( 
                       double    Latitude,
                       double   Longitude,
                       double   NED2ECR[3][3])

/* Modified from "Banner Core" by Larry Mabius, "geodetic_functions.cc".
    where Local Coordinates = ENU */
{
  /* NED2ECR is a transform from North-East-Down coordinates to ECR */

  double COSLAT;
  double COSLONG;
  double SINLAT;
  double SINLONG;
  COSLAT = cos(Latitude);
/*  SINLAT = sqrt(1.0 - (COSLAT*COSLAT));
*/
  SINLAT = sin(Latitude);

  COSLONG = cos(Longitude);
/*  SINLONG = sqrt(1.0 - (COSLONG*COSLONG));
*/
  SINLONG = sin(Longitude);

  NED2ECR[0][0] =   -COSLONG*SINLAT;
  NED2ECR[1][0] =  -SINLONG*SINLAT;
  NED2ECR[2][0] =   COSLAT;     
  NED2ECR[0][1] =  -SINLONG; 
  NED2ECR[1][1] =  COSLONG;
  NED2ECR[2][1] =  0.0;               
  NED2ECR[0][2] =  -COSLONG *COSLAT;
  NED2ECR[1][2] =  -SINLONG*COSLAT;
  NED2ECR[2][2] =  -SINLAT;
}

void Calculate_nwu2ecr( 
double    Latitude,
double Longitude,
double NWU2ECR[3][3])

/* Modified from "Banner Core" by Larry Mabius, "geodetic_functions.cc".
    where Local Coordinates = ENU */
{

  double COSLAT;
  double COSLONG;
  double SINLAT;
  double SINLONG;

  #ifdef PROFILE
  ELABORATE_PKEY(PKEY);
  ELABORATE_PROFILE_FOR( Calculate_nwu2ecr, PKEY);
  #endif

  COSLAT = cos(Latitude); 
/*  SINLAT = sqrt(1.0 - (COSLAT*COSLAT));
*/ 
  SINLAT = sin(Latitude); 

  COSLONG = cos(Longitude);
/*  SINLONG = sqrt(1.0 - (COSLONG*COSLONG));
*/
  SINLONG = sin(Longitude);
 

  /* NWU2ECR is a transform from North-East-Down coordinates to ECR */
  NWU2ECR[0][0] =  -COSLONG*SINLAT;
  NWU2ECR[1][0] =  -SINLONG*SINLAT;
  NWU2ECR[2][0] =   COSLAT;     
  NWU2ECR[0][1] =   SINLONG; 
  NWU2ECR[1][1] =  -COSLONG;
  NWU2ECR[2][1] =  0.0;               
  NWU2ECR[0][2] =   COSLONG *COSLAT;
  NWU2ECR[1][2] =   SINLONG*COSLAT;
  NWU2ECR[2][2] =   SINLAT;

  #ifdef PROFILE
  PROFILE_EXIT(PKEY);
  #endif
}
/* --------------------------------------------------------- */ 

PUBLIC void Calculate_Earth_Orientation ( double Simulation_Time
                     , double Earth_Orientation[3][3] 
                     )
{ 
  /* From "Banner Core" by Larry Mabius, "geodetic_functions.cc". */
    double Earth_Rotation;
    double COSINE;
    double SINE;

    /* Simulation_Time = Elapsed Time */
    Earth_Rotation = Earth_Angular_Rate * Simulation_Time;

    COSINE = cos(Earth_Rotation);
/*    SINE = sqrt(1.0 - (COSINE*COSINE));
*/
    SINE = sin(Earth_Rotation);

    /* Earth_Orientation is Transformation from ECI to ECR. */
    Earth_Orientation[0][0] = COSINE;
    Earth_Orientation[0][1] = SINE;
    Earth_Orientation[0][2] = 0.0;
    Earth_Orientation[1][0] = - SINE;
    Earth_Orientation[1][1] = COSINE;
    Earth_Orientation[1][2] = 0.0;
    Earth_Orientation[2][0] = 0.0;
    Earth_Orientation[2][1] = 0.0;
    Earth_Orientation[2][2] = 1.0;
 /* 
   printf("Earth Orientation: \n");
   printf("%f %f %f \n", Earth_Orientation[0][0], Earth_Orientation[0][1], Earth_Orientation[0][2]);
  
   printf("%f %f %f \n", Earth_Orientation[1][0], Earth_Orientation[1][1], Earth_Orientation[1][2]);
   printf("%f %f %f \n", Earth_Orientation[2][0], Earth_Orientation[2][1], Earth_Orientation[2][2]);
*/
}
  

/* ------------------------------------------------------------------------------------*/

void ecr2los (double Transform[3][3],double * Platform_Position, double * ECR_Position, double * Relative_Position )
{
/* ECR(x,y,z) to LOS Relative_Position.
   Inputs:   Transform ECR2LOS transform
           ECR_Position = ECR location of target, Position = ECR location of platfrom. 
   Outputs:  Relative_Position = x,y,z sensor-relative (meters)  */

  double     Target_Position[3];

  /* Compute relative position of target = Transform * (ECR_target_pos - ECR_sensor_pos) */
  dvsub( 3, ECR_Position, 1, Platform_Position, 1, Target_Position, 1);

  //printf("ECR2LOS:  ECR_Position = %f %f %f \n",ECR_Position[0],ECR_Position[1],ECR_Position[2]);
  //printf("ECR2LOS: Platform Position = %f %f %f \n",Platform_Position[0],Platform_Position[1],Platform_Position[2]);
  //printf("ECR2LOS: Target_Position %f %f %f \n",Target_Position[0],Target_Position[1],Target_Position[2]);
  
  /* =========
  matrix_operation( VECTOR_SUBTRACT, ECR_Position, Platform_Position, Target_Position );
  =============== */
  
  /* Apply the transformation matrix */
  MatVecMult3X3X3X1Double(Relative_Position,(double*)Transform,Target_Position);

  //printf("ECR2LOS: Relative_Position %f %f %f = \n",Relative_Position[0],Relative_Position[1],Relative_Position[2]);

}

/*---------------------------------------------------------------------------*/


void los2ecr ( 
double    Transform[3][3],
double *Sensor_Position,
double *Relative_Position,
double *ECR_Position)
{
/* LOS Relative_Position to ECR(x,y,z).
   Inputs:   Transform ECR2LOS transform
         Relative_Position = x,y,z sensor-relative (meters)
             Sensor_Position = ECR location of sensor.
   Outputs:  ECR_Position = ECR location of target,  */

  double    Target_Position[3];

  /* Apply the transformation matrix */
  MatTVecMult3X3X3X1Double(Target_Position,(double*)Transform, Relative_Position);

  //printf("los2ecr: TarPos = %f %f %f \n",Target_Position[0],Target_Position[1],Target_Position[2]);


  /* Compute relative position of target = Transform * (ECR_target_pos - ECR_sensor_pos) */

  dvadd( 3, Sensor_Position, 1, Target_Position, 1, ECR_Position, 1);

  //printf("los2ecr: SenPos = %f %f %f \n",Sensor_Position[0],Sensor_Position[1],Sensor_Position[2]);
  //printf("los2ecr: ECR_Position = %f %f %f \n",ECR_Position[0],ECR_Position[1],ECR_Position[2]);

  /* ============

  matrix_operation( VECTOR_ADD, Sensor_Position, Target_Position, ECR_Position);

  ============= */

}


/* --------------------------------------------------------- */

void ecr2rae (
double     *Relative_Position,
double *Range,
double *Azimuth,
double *Elevation)
{
 /* ECR relative(x,y,z) to Range-Azimuth-Elevation Transform.
   Inputs:  Relative_Position = x,y,z earth-relative (meters)
   R =Sqrt(X^2 + Y^2), Az = atan2(Y,X), El = atan2(Z,R)
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */

  double R01; 
  double A,B,C;

   A = Relative_Position[0]*Relative_Position[0];
   B = Relative_Position[1]*Relative_Position[1];
   C = Relative_Position[2]*Relative_Position[2];

   R01 = sqrt(A+B);
  *Azimuth   = atan2 ( Relative_Position[1], Relative_Position[0] );
  *Elevation = atan2 ( Relative_Position[2], R01 );
  *Range = sqrt(A+B+C); 
}
/* --------------------------------------------------------- */

void ned2rae ( 
double     *Relative_Position,
double *Range,
double *Azimuth,
double *Elevation)

/* NED relative(x,y,z) to Range-Azimuth-Elevation Transform.
   Inputs:  Relative_Position = x,y,z ned-relative (meters)
   R =Sqrt(X^2 + Y^2), Az = atan2(Y,X), El = atan2(Z,R)
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
/* Note x = North
    y = East
    z = Down
    
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
{
   double R01;

   double A,B,C; 
       
   A = Relative_Position[0]*Relative_Position[0]; 
   B = Relative_Position[1]*Relative_Position[1]; 
   C = Relative_Position[2]*Relative_Position[2]; 
            

   R01 = sqrt(A+B);
  *Azimuth   = atan2 ( Relative_Position[1], Relative_Position[0] );
  *Elevation = atan2 ( -Relative_Position[2], R01 );
  *Range = sqrt(A+B+C);
}


void nwu2rae ( 
double     *Relative_Position,
double *Range,
double *Azimuth,
double *Elevation)


/* NWU relative(x,y,z) to Range-Azimuth-Elevation Transform.
   Inputs:  Relative_Position = x,y,z ned-relative (meters)
   R =Sqrt(X^2 + Y^2), Az = atan2(Y,X), El = atan2(Z,R)
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
/* Note x = North
    y = West
    z = Up
    
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
{
   double R01;

   double A,B,C;  
        
   A = Relative_Position[0]*Relative_Position[0];  
   B = Relative_Position[1]*Relative_Position[1];  
   C = Relative_Position[2]*Relative_Position[2];  
                     
                     
   R01 = sqrt(A+B);
  *Azimuth   = atan2 ( Relative_Position[1], Relative_Position[0] );
  *Elevation = atan2 ( Relative_Position[2], R01 );
  *Range = sqrt(A+B+C);
}


void bdy2rae ( 
double     *Relative_Position,
double *Range,
double *Azimuth,
double *Elevation)

/* Body relative(x,y,z) to Range-Azimuth-Elevation Transform.
   Inputs:  Relative_Position = x,y,z body-relative (meters)
   R =Sqrt(X^2 + Y^2), Az = atan2(Y,X), El = atan2(-Z,R)
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
/*  Note  x = Front
          y = Right
      z = Down
    
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
{
   double R01;

   double A,B,C;  
        
   A = Relative_Position[0]*Relative_Position[0];  
   B = Relative_Position[1]*Relative_Position[1];  
   C = Relative_Position[2]*Relative_Position[2];  
                     
                     
   R01 = sqrt(A+B);
  *Azimuth   = atan2 ( Relative_Position[1], Relative_Position[0] );
  *Elevation = atan2 ( -Relative_Position[2], R01 );
  *Range = sqrt(A+B+C);
}

/* --------------------------------------------------------- */

void int2rae (
double     *Relative_Position,
double *Range,
double *Azimuth,
double *Elevation)

/* Interceptor relative(x,y,z) to Range-Azimuth-Elevation Transform.
   Inputs:  Relative_Position = x,y,z Interceptor-relative (meters)
   R =Sqrt(X^2 + Y^2), Az = atan2(Y,X), El = atan2(-Z,R)
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
/*  Note  x = Front
      y = Right
      z = Down
    
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
  {
   double R01;
   double A,B,C;  
        
   A = Relative_Position[0]*Relative_Position[0];  
   B = Relative_Position[1]*Relative_Position[1];  
   C = Relative_Position[2]*Relative_Position[2];  
                     
                     
   R01 = sqrt(A+B);
  *Azimuth   = atan2 ( Relative_Position[1], Relative_Position[0] );
  *Elevation = atan2 ( -Relative_Position[2], R01 );
  *Range = sqrt(A+B+C);
}


void sen2rae ( 
double     *Relative_Position,
double *Range,
double *Azimuth,
double *Elevation)

/* Sensor relative(x,y,z) to Range-Azimuth-Elevation Transform.
   Inputs:  Relative_Position = x,y,z sensor-relative (meters)
   R =Sqrt(X^2 + Z^2), Az = atan2(X,Z), El = atan2(-Y,R)
   Note x = Right
    y = Down
    z = out the LOS
     
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
/*  Note   z = Front
       x = Right
       y = Down
    
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
{
   double R02;
   double Tac;
   double A,B,C;
        
   A = Relative_Position[0]*Relative_Position[0];
   B = Relative_Position[1]*Relative_Position[1];
   C = Relative_Position[2]*Relative_Position[2];
             
   Tac = (A+C);
   R02 = sqrt(Tac);
  *Azimuth   = atan2 ( Relative_Position[0], Relative_Position[2] );
  *Elevation = atan2 (-Relative_Position[1], R02 );
  *Range = sqrt(Tac+B);
}


void cam2rae (
double     *Relative_Position,
double *Range,
double *Azimuth,
double *Elevation)
{
/* Camera relative(x,y,z) to Range-Azimuth-Elevation Transform.
   Inputs:  Relative_Position = x,y,z sensor-relative (meters)
   R =Sqrt(X^2 + Z^2), Az = atan2(X,Z), El = atan2(-Y,R)
   Note x = Right
    y = Down
    z = out the LOS
     
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */
   double R02;

   double A,B,C;
   double Tac;
        
   A = Relative_Position[0]*Relative_Position[0];
   B = Relative_Position[1]*Relative_Position[1];
   C = Relative_Position[2]*Relative_Position[2];
                
   Tac = (A+C);                
   R02 = sqrt(Tac);
  *Azimuth   = atan2 ( Relative_Position[0], Relative_Position[2] );
  *Elevation = atan2 (-Relative_Position[1], R02 );
  *Range = sqrt(Tac+B);
}

void los2rae ( 
double     *Relative_Position,
double *Range,
double *Azimuth,
double *Elevation)

{
/* Los relative(x,y,z) to Range-Azimuth-Elevation Transform.
   Inputs:  Relative_Position = x,y,z sensor los-relative (meters)
   R =Sqrt(X^2 + Z^2), Az = atan2(X,Z), El = atan2(-Y,R)
   Note x = Right
    y = Down
    z = out the LOS
     
   Outputs: Azimuth,Elevation = radians (of target), Range = Meters to Target. */

   double R02;
   double A,B,C;
        
   A = Relative_Position[0]*Relative_Position[0];
   B = Relative_Position[1]*Relative_Position[1];
   C = Relative_Position[2]*Relative_Position[2];
                
                
   R02 = sqrt(A+C);

  *Azimuth   = atan2 ( Relative_Position[0], Relative_Position[2] );
  *Elevation = atan2 (-Relative_Position[1], R02 );
  *Range = sqrt( A+B+C );
}

/* --------------------------------------------------------- */ 

void rae2ecr (
double     Range,
double Azimuth,
double Elevation,
double *Relative_Position)
{
/*   Range-Azimuth-Elevation to ECR (x,y,z) Transform.
     Inputs:  Azimuth,Elevation = radians (of target), Range = Meters to Target.
     Outputs: Relative_Position = x,y,z earth-relative (meters)
     Note x = lat,long = 0
      y = long = 90
      z = north
      */
  double R01;
  double sineAZ, cosAZ, sineEL,cosEL;

  sineAZ = sin(Azimuth);
//  cosAZ = sqrt(1.0-(sineAZ*sineAZ));
  cosAZ = cos(Azimuth);

  sineEL = sin(Elevation);
//  cosEL  =sqrt(1.0 - (sineEL*sineEL));
  cosEL  = cos(Elevation);

  R01 = Range * cosEL;
  Relative_Position[0] =    R01  * cosAZ;
  Relative_Position[1] =    R01  * sineAZ;
  Relative_Position[2] =   -Range * sineEL;
 
}

void rae2ned (
double     Range,
double Azimuth,
double Elevation,
double *Relative_Position)
{
/*   Range-Azimuth-Elevation to NED (x,y,z) Transform.
     Inputs:  Azimuth,Elevation = radians (of target), Range = Meters to Target.
     Outputs: Relative_Position = x,y,z NED-relative (meters)
     Note x = North
      y = East
      z = Down
      */
  double R01;
  double sineAZ, cosAZ, sineEL,cosEL;
 
  sineAZ = sin(Azimuth);
//  cosAZ = sqrt(1.0-(sineAZ*sineAZ));
  cosAZ = cos(Azimuth);
 
  sineEL = sin(Elevation);
//  cosEL  =sqrt(1.0 - (sineEL*sineEL));
  cosEL  = cos(Elevation);

  R01 = Range * cosEL;
  Relative_Position[0] =    R01  * cosAZ;
  Relative_Position[1] =    R01  * sineAZ;
  Relative_Position[2] =   Range * -sineEL;
 
}

void rae2nwu (
double     Range,
double Azimuth,
double Elevation,
double *Relative_Position)
{
/*   Range-Azimuth-Elevation to NWU (x,y,z) Transform.
     Inputs:  Azimuth,Elevation = radians (of target), Range = Meters to Target.
     Outputs: Relative_Position = x,y,z NWU-relative (meters)
     Note x = North
      y = West
      z = Up
      */
  double R01;
  double sinAZ, cosAZ, sinEL,cosEL;

  #ifdef PROFILE
  ELABORATE_PKEY(PKEY);
  ELABORATE_PROFILE_FOR(rae2nwu, PKEY);
  #endif

  sinAZ = sin(Azimuth);
//  cosAZ = sqrt(1.0-(sinAZ*sinAZ));
  cosAZ = cos(Azimuth);

  sinEL = sin(Elevation);
//  cosEL  =sqrt(1.0 - (sinEL*sinEL));
  cosEL  =cos(Elevation);

  R01 = Range * cosEL;
  Relative_Position[0] =    R01  * cosAZ;
  Relative_Position[1] =    R01  * sinAZ;
  Relative_Position[2] =   Range * sinEL;
  #ifdef PROFILE
  PROFILE_EXIT(PKEY);
  #endif
}
void rae2bdy (
double     Range,
double Azimuth,
double Elevation,
double *Relative_Position)
{
/*   Range-Azimuth-Elevation to Body Relative(x,y,z) Transform.
     Inputs:  Azimuth,Elevation = radians (of target), Range = Meters to Target.
     Outputs: Relative_Position = x,y,z body-relative (meters)
     Note x = front
      y = right
      z = down
      */
  double R01;
  double sinAZ, cosAZ, sinEL,cosEL;

  sinAZ = sin(Azimuth);
/*  cosAZ = sqrt(1.0-(sinAZ*sinAZ));
*/
  cosAZ = cos(Azimuth);
 
  sinEL = sin(Elevation);
/*  cosEL  =sqrt(1.0 - (sinEL*sinEL));
*/
  cosEL  =cos(Elevation);

  R01 = Range * cosEL;
  Relative_Position[0] =    R01  * cosAZ;
  Relative_Position[1] =    R01  * sinAZ;
  Relative_Position[2] =   Range * -sinEL;
 
}


void rae2int (
double     Range,
double Azimuth,
double Elevation,
double *Relative_Position)
{
/*   Range-Azimuth-Elevation to Interceptor Relative(x,y,z) Transform.
     Inputs:  Azimuth,Elevation = radians (of target), Range = Meters to Target.
     Outputs: Relative_Position = x,y,z interceptor-relative (meters)
     Note x = front
      y = right
      z = down
      */
  double R01;
  double sinAZ, cosAZ, sinEL,cosEL;
 
  sinAZ = sin(Azimuth); 
/*  cosAZ = sqrt(1.0-(sinAZ*sinAZ));
*/ 
  cosAZ = cos(Azimuth); 
  
  sinEL = sin(Elevation); 
/*  cosEL  =sqrt(1.0 - (sinEL*sinEL));
*/ 
  cosEL  =cos(Elevation); 

  R01 = Range * cosEL;
  Relative_Position[0] =    R01  * cosAZ;
  Relative_Position[1] =    R01  * sinAZ;
  Relative_Position[2] =   Range * -sinEL;
 
}

void rae2sen (
double     Range,
double Azimuth,
double Elevation,
double *Relative_Position)
{
/*   Range-Azimuth-Elevation to Sensor(x,y,z) Transform.
     Inputs:  Azimuth,Elevation = radians (of target), Range = Meters to Target.
     Outputs: Relative_Position = x,y,z sensor-relative (meters)
     Note x = right
      y = down
      z = out the LOS
      */
  double R02;
  double sinAZ, cosAZ, sinEL,cosEL;
 
  sinAZ = sin(Azimuth); 
/*  cosAZ = sqrt(1.0-(sinAZ*sinAZ));
*/ 
  cosAZ = cos(Azimuth); 
  
  sinEL = sin(Elevation); 
/*  cosEL  =sqrt(1.0 - (sinEL*sinEL));
*/ 
  cosEL  =cos(Elevation); 

  R02 = Range * cosEL;
  Relative_Position[0] =    R02  * sinAZ;
  Relative_Position[1] =  -Range * sinEL;
  Relative_Position[2] =    R02  * cosAZ; 
}

void rae2cam (
double     Range,
double Azimuth,
double Elevation,
double *Relative_Position)
{
/*   Range-Azimuth-Elevation to Camera(x,y,z) Transform.
     Inputs:  Azimuth,Elevation = radians (of target), Range = Meters to Target.
     Outputs: Relative_Position = x,y,z camera-relative (meters)
     Note x = right
      y = down
      z = out the LOS    */

  double R02;
  double sinAZ, cosAZ, sinEL,cosEL;
 
  sinAZ = sin(Azimuth); 
/*  cosAZ = sqrt(1.0-(sinAZ*sinAZ));
*/ 
  cosAZ = cos(Azimuth); 
  
  sinEL = sin(Elevation); 
/*  cosEL  =sqrt(1.0 - (sinEL*sinEL));
*/ 
  cosEL  =cos(Elevation); 

  R02 = Range * cosEL;
  Relative_Position[0] =    R02  * sinAZ;
  Relative_Position[1] =  -Range * sinEL;
  Relative_Position[2] =    R02  * cosAZ; 
}


void rae2los(
double     Range,
double Azimuth,
double Elevation,
double *Relative_Position)
{
/*   Range-Azimuth-Elevation to Line of sight(x,y,z) Transform.
     Inputs:  Azimuth,Elevation = radians (of target), Range = Meters to Target.
     Outputs: Relative_Position = x,y,z los-relative (meters)
     Note x = right
      y = down
      z = out the LOS
      */
  double R02;
  double sinAZ, cosAZ, sinEL,cosEL;
 
  sinAZ = sin(Azimuth); 
/*  cosAZ = sqrt(1.0-(sinAZ*sinAZ));
*/ 
  cosAZ = cos(Azimuth); 
  
  sinEL = sin(Elevation); 
/*  cosEL  =sqrt(1.0 - (sinEL*sinEL));
*/ 
  cosEL  =cos(Elevation); 

  R02 = Range * cosEL;
  Relative_Position[0] =    R02  * sinAZ;
  Relative_Position[1] =  -Range * sinEL;
  Relative_Position[2] =    R02  * cosAZ; 
}

/* --------------------------------------------------------- */ 

/* Range-Azimuth-Elevation to Image Transform.
   Inputs:  Azimuth = rads, Elevation = rads, Range = meters to target,
        IFOV = Instantaneous Field of View of Sensor, in urads.
   Outputs: Image = row, col, range of target. row,col = sub-pixel coords. */

void rae2image(
double     Range,
double Azimuth,
double Elevation,
double *IFOV,
double *Image)
{
  /* Determine sub-pixel target location in image coordinates.
     Also convert IFOV urads to rads.
     Note: manual strength reduction optimization: divide turned into a multiply.
  */

  Image[0] = (Azimuth* 1000000.0)   / IFOV[0];
  Image[1] = (-Elevation*1000000.0) / IFOV[1];
  Image[2] = Range;
}

/* --------------------------------------------------------- */ 

void image2rae(
double    *Image,
double *IFOV,
double *Range,
double *Azimuth,
double *Elevation)
{
/* Image to Range-Azimuth-Elevation Transform.

   ASSUMES TARGET RANGE IS IMAGE[2] (meters).

   Inputs:  Image = row, col, range of target. row,col = sub-pixel coords of target.
        IFOV = Instantaneous Field of View of Sensor, in urads.
   Outputs: Azimuth = rads, Elevation = rads, Range = meters to target,
*/
/* Determine Az and El from image sub-pixel row,col.
   Also convert IFOV urads to rads. */

  *Azimuth   = (IFOV[0] * 0.000001) * Image[0];
  *Elevation = (IFOV[1] * 0.000001) * -Image[1];
  *Range     = Image[2];
}


void ecr2att (
double    *Orientation_Vector,
double *Yaw,
double *Pitch,
double *Roll)
{
 /* Given ECR Interceptor Xi orientation vector generate Attitude angles

   Inputs:  Desired Interceptor Orientation Vector = x,y,z
   Outputs: Attitude ( Yaw, Pitch ) rads */

   bdy2att ( Orientation_Vector, Yaw, Pitch, Roll);

/* correct because input was reallt ecr relative
   to command attitudes relative to body system */

   *Yaw = -*Yaw;
   *Pitch = - *Pitch;
/*  leave roll alone */
}

void bdy2att (
double    *Orientation_Vector,
double *Yaw,
double *Pitch,
double *Roll)
{
  /* Given Body Interceptor Xb orientation vector generate Attitude angles

     Inputs:  Desired Interceptor Orientation Vector = x,y,z
     Outputs: Attitude ( Yaw, Pitch ) rads */

   double inv_magnitude;
   double A,B,C;
        
   A = Orientation_Vector[0]*Orientation_Vector[0];
   B = Orientation_Vector[1]*Orientation_Vector[1];
   C = Orientation_Vector[2]*Orientation_Vector[2];
                
   inv_magnitude = 1.0/sqrt(A+B+C);

   *Yaw = atan2(Orientation_Vector[1]*inv_magnitude,Orientation_Vector[0]*inv_magnitude);
   *Pitch = asin(-Orientation_Vector[2]*inv_magnitude);
   //*Roll = TXS_UNDEF_ROLL;
   
}


void int2att (
double    Transform[3][3],
double *Yaw,
double *Pitch,
double *Roll)

{
/* Given Interceptor transform matrix generate Attitude angles 

   Inputs: Transform -  Known inteceptor transformation matrix
   Outputs: Attitude ( Yaw, Pitch, Roll ) rads */

  *Yaw = atan2(Transform[0][1],Transform[0][0]);
  *Pitch = asin(-Transform[0][2]);
  *Roll = atan2(Transform[1][2],Transform[2][2]);

/* correct to command attitudes relative to body system */
   *Yaw = -*Yaw;
   *Pitch = - *Pitch;
   *Roll = *Roll+M_PI;
   if(*Roll > M_PI) *Roll = *Roll - 2.0*M_PI;
 
}


void ecr2truth (
double  In_Roll,
double *Orientation_Vector,
double *Yaw,
double *Pitch,
double *Roll,
double Transform_Sen[3][3]) 
{
/* Given ECR orientation vector generate Banner Core "sensor" transform
    and DITPsim Truth attitude.

   Inputs:  Orientation_Vector - Desired Sensor Orientation Z-axis Vector = x,y,z (ECR)
             In_Roll  -   Roll of the interceptor. 
   Outputs: Sensor Transform - USED TO DRIVE BANNER CORE TRUTH 
        Yaw, Pitch, Rol  - attitudes that generated the transform. */

/* Calculate the attitude command that generates the transfrom */ 
    ecr2att(Orientation_Vector,Yaw,Pitch,Roll);
        *Roll = In_Roll; 
    att2sen(*Yaw,*Pitch,*Roll,Transform_Sen);
}

void att2sen (
double     Yaw,
double Pitch,
double Roll,
double Transform_Sen[3][3]) 
{
    double    Tform_ECR[3][3];
    double    Tform_Body[3][3];
    double    Tform_Temp[3][3];
    double    Tform_Int[3][3];

/* Given interceptor attitude generate Banner Core "sensor" transform
   Inputs:  Interceptor attitude.
   Outputs: Sensor Transform - USED TO DRIVE BANNER CORE TRUTH 
        Yaw, Pitch,Roll  - the attitudes that generated the transform. */     
     
/* Generate ECR transform */
    idenity(Tform_ECR);
/* Generate Transform from ECR to Body Coordinates  */
/* Body:  X = Front , Y = Right, Z = Down  */
         roll_x(Tform_ECR,M_PI,Tform_Body);

/*Generate Transform from ECR to Interceptor Coordinates */
/* Interceptor:  X = Front , Y = Right, Z = Down   */

    yaw_z(Tform_Body,Yaw,Tform_Int);
    pitch_y(Tform_Int,Pitch,Tform_Temp);
    roll_x(Tform_Temp,Roll,Tform_Int);

/* Generate Transform from ECR to Sensor Coordinates */
/* Interceptor:  Z = Front , X = Right, Y = Down     */
     yaw_z(Tform_Int,M_PI/2.0,Tform_Temp);
    roll_x(Tform_Temp,M_PI/2.0,Transform_Sen);    
 
}


/* ---------------------------------------------------------
   EOF                                 */
