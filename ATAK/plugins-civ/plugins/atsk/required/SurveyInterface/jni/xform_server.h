/**********************************************************************
* Disclaimer:    This software was developed for the Department of    *
*                Defense (DoD). All US Government restrictions on     *
*                software source code distribution apply              *
*                                                                      *
*  PROPERTY OF AFRL/RRS                                               *
**********************************************************************/

#ifndef _xform_server_H_
#define _xform_server_H_
/*
     Transform_Server for FPsim
     13-Mar-98 GAB

     Use -DCPP (C++ flag) when compiling with FPsim modules.

     Note: This file assumes "global_constants.h" has been included previously.
     ( /usr/people/banner/simulation/support/global_constants.h )
*/

#ifndef PUBLIC
  #define PUBLIC
#endif

#ifndef PRIVATE
  #define PRIVATE static
#endif

//#include "MessageStructures.h"

#define  TXS_UNDEF_ROLL            (-1.0e6)    /* Roll Undefined  */

#define  XFRM_NO_ERROR            0
#define  XFRM_NO_DATA_ERROR        1
#define  XFRM_REQUEST_ERROR        2
#define  XFRM_NOT_AVAILABLE        3
#define  XFRM_SENSOR_ID_ERROR        4
#define  XFRM_SERVER_NO_DATA_ERROR    11
#define  XFRM_SERVER_REQUEST_ERROR    12
#define  XFRM_SERVER_NOT_AVAILABLE    13

/* -------------------------------------------------- 
   Xfrm_Server() Definitions                  */

#define  MAX_XFRM_DATA_QUEUE          500    /* At 500, queue history = 4.166665 secs*/

#define  MODE_UPDATE_INT        0
#define  MODE_UPDATE_FSM        1
#define  MODE_UPDATE_BSM        2
#define  MODE_SEARCH_INTERP        3

/* -------------------------------------------------- 
   Transform() Definitions                  */

#define  TYPE_VECTOR            0
#define  TYPE_STATE            1
#define  TYPE_MATRIX            2

#define  MODE_ECR2ECI            1
#define  MODE_ECR2GEO            2
#define  MODE_ECR2NED            3
#define  MODE_ECR2NWU            4

#define  MODE_ECI2ECR            5
#define  MODE_GEO2ECR            6
#define  MODE_NED2ECR            7
#define  MODE_NWU2ECR            8

#define  MODE_ECR2BDY            9
#define  MODE_ECR2INT            10        
#define  MODE_ECR2SEN            11
#define  MODE_ECR2CAM         global_constants.h    12
#define  MODE_ECR2LOS            13

#define  MODE_BDY2ECR            14
#define  MODE_INT2ECR            15
#define  MODE_SEN2ECR            16
#define  MODE_CAM2ECR            17
#define  MODE_LOS2ECR            18


#define  MODE_ECR2RAE            19
#define  MODE_NED2RAE            20
#define  MODE_NWU2RAE            21
#define  MODE_BDY2RAE            22
#define  MODE_INT2RAE            23
#define  MODE_SEN2RAE            24
#define  MODE_CAM2RAE            25
#define  MODE_LOS2RAE            26

#define  MODE_RAE2ECR            27
#define  MODE_RAE2NED            28
#define  MODE_RAE2NWU            29
#define  MODE_RAE2BDY            30
#define  MODE_RAE2INT            31
#define  MODE_RAE2SEN            32
#define  MODE_RAE2CAM            33
#define  MODE_RAE2LOS            34


#define  MODE_ECR2BDY2RAE        35
#define  MODE_ECR2INT2RAE        36
#define  MODE_ECR2SEN2RAE        37
#define  MODE_ECR2CAM2RAE        38
#define  MODE_ECR2LOS2RAE        39

#define  MODE_RAE2BDY2ECR        40
#define  MODE_RAE2INT2ECR        41
#define  MODE_RAE2SEN2ECR        42
#define  MODE_RAE2CAM2ECR        43
#define  MODE_RAE2LOS2ECR        44

#define  MODE_RAE2IMG            45
#define  MODE_LOS2RAE2IMG        46
#define  MODE_ECR2LOS2RAE2IMG        47

#define  MODE_IMG2RAE            48
#define  MODE_IMG2RAE2LOS        49
#define  MODE_IMG2RAE2LOS2ECR        50
#define  MODE_IMG2RAE2LOS2ECR2NWU2RAE    51
#define  MODE_RAE2NWU2ECR2LOS2RAE    52
#define  MODE_RAE2LOS2ECR2NWU2RAE    53
#define  MODE_BDY2ATT            54

#define  MODE_POSECR            55  /* get intercepter Position (m) */
#define  MODE_VELECR            56  /* get intercepter Velocity (m/s) */
#define  MODE_ACCECR            57  /* get intercepter Acceleration  (m/s^2) */
/*#define  MODE_ATTECR            58 */ /* old get intercepter (attitude) () */
#define  MODE_ATTRAD            58  /* get orientation (attitude) (r) */
#define  MODE_ATRRPS            59  /* get orientation (attitude) rate  (r/s) */
#define  MODE_DVLMPS            60  /* get amount of divert velocity  left (m/s) */
#define  MODE_ACSFUEL            61  /* get amount of fuel left (r) */ 

#define  MODE_FSMAZEL            62
#define  MODE_BSMAZEL            63

#define  MODE_ECR2LOSVEL        64

/* -------------------------------------------------- 
   Internal Definitions                      */

#define  MATRIXxMATRIX            1
#define  MATRIXTxMATRIX            2
#define  MATRIXxVECTOR            3
#define  MATRIXTxVECTOR            4
#define  VECTOR_ADD            5
#define  VECTOR_SUBTRACT        6
#define  MATRIX_TRANSPOSE        7
#define  VECTORxCROSS            8
#define  VECTORxDOT             9


#define Error_Tolerance         1.0e-4  /* Kilometers */

/* -------------------------------------------------- */


/*
typedef struct INT_QUEUE
{
  
  int            Insert,
            Remove,
            Empty;
  InterceptorData    Data[MAX_XFRM_DATA_QUEUE];
} INT_Queue;
*/
/* -------------------------------------------------- */
/*
typedef struct FSM_QUEUE
{
  
  int            Insert,
            Remove,
            Empty;
  FSMPosition        Data[MAX_XFRM_DATA_QUEUE];
} FSM_Queue;
*/
/* -------------------------------------------------- */
/*
typedef struct BSM_QUEUE
{
  
  int            Insert,
            Remove,
            Empty;
  BSMPosition        Data[MAX_XFRM_DATA_QUEUE];
} BSM_Queue;
*/
/* -------------------------------------------------- */
/*
typedef struct XFRM_SERVER_DATA
{
  double                last_int_time, last_fsm_time, last_bsm_time;
  INT_Queue        Int_Queue;
  FSM_Queue        Fsm_Queue;
  BSM_Queue        Bsm_Queue;

} Xfrm_Data;
*/
/* PROTOTYPES --------------------------------------- */

/* xfrm_convert.c */
#ifdef CPP    
extern "C" {
#endif
PUBLIC void matrix_operation( int Operation
                            , double * Arg1
                            , double * Arg2
                            , double * Result
                            );

PUBLIC void roll_x ( double In_Matrix[3][3]
                   , double Roll_Angle
                   , double Out_Matrix[3][3] 
                   );

PUBLIC void pitch_y ( double In_Matrix[3][3]
                    , double Pitch_Angle
                    , double Out_Matrix[3][3] 
                    );

PUBLIC void yaw_z ( double In_Matrix[3][3]
                  , double Yaw_Angle
                  , double Out_Matrix[3][3] 
                  );

PUBLIC void ecr2eci_posvel ( double Simulation_Time
                           , double *ECR_Position
                           , double *ECR_Velocity
                           , double *ECI_Position
                           , double *ECI_Velocity 
                           );

PUBLIC void ecr2geo ( double *ECR_Position
                    , double *Altitude
                    , double *Latitude
                    , double *Longitude 
                    );

PUBLIC void idenity( double Out_Matrix[3][3]);

/* Stuff not tidy'd up yet */
void ecr2eci ( double Simulation_Time, double *ECR_Position, double *ECI_Position );
void ecr2ned (double *Sensor_Position,double *ECR_Position,double *NED_Position );
void ecr2nwu (double *Sensor_Position,double *ECR_Position,double *NED_Position );
void eci2ecr ( double Simulation_Time, double *ECI_Position, double *ECR_Position );
void eci2ecr_posvel ( double Simulation_Time, double *ECI_Position, double *ECI_Velocity,
           double *ECR_Position, double *ECR_Velocity);
void geo2ecr ( double Altitude, double Latitude, double Longitude, double *ECR_Position );
void ned2ecr (double *Sensor_Position,double *NED_Position,double *ECR_Position );
void nwu2ecr (double *Sensor_Position,double *NED_Position,double *ECR_Position );
void Calculate_ned2ecr(double Latitude,double Longitude,double NED2ECR[3][3] );
void Calculate_nwu2ecr(double Latitude,double Longitude,double NWU2ECR[3][3] );
void Calculate_Earth_Orientation ( double Simulation_Time, double Earth_Orientation[3][3] );



void ecr2los ( double Sensor_Transform[3][3], double *Sensor_Position, double *ECR_Position,  double *Relative_Position );

void los2ecr ( double Sensor_Transform[3][3], double *Sensor_Position, 
           double *Relative_Position, double *ECR_Position );

void ecr2rae ( double *Relative_Position, double *Range, double *Azimuth, double *Elevation );
void ned2rae ( double *Relative_Position, double *Range, double *Azimuth, double *Elevation );
void nwu2rae ( double *Relative_Position, double *Range, double *Azimuth, double *Elevation );
void bdy2rae ( double *Relative_Position, double *Range, double *Azimuth, double *Elevation );
void int2rae ( double *Relative_Position, double *Range, double *Azimuth, double *Elevation );
void sen2rae ( double *Relative_Position, double *Range, double *Azimuth, double *Elevation );
void cam2rae ( double *Relative_Position, double *Range, double *Azimuth, double *Elevation );
void los2rae ( double *Relative_Position, double *Range, double *Azimuth, double *Elevation );


void rae2ecr ( double Range, double Azimuth, double Elevation, double *Relative_Position );
void rae2ned ( double Range, double Azimuth, double Elevation, double *Relative_Position );
void rae2nwu ( double Range, double Azimuth, double Elevation, double *Relative_Position );
void rae2bdy ( double Range, double Azimuth, double Elevation, double *Relative_Position );
void rae2int ( double Range, double Azimuth, double Elevation, double *Relative_Position );
void rae2sen ( double Range, double Azimuth, double Elevation, double *Relative_Position );
void rae2cam ( double Range, double Azimuth, double Elevation, double *Relative_Position );
void rae2los ( double Range, double Azimuth, double Elevation, double *Relative_Position );
void rae2image (  double Range, double Azimuth, double Elevation, double *IFOV, double *Image );
void image2rae ( double *Image, double *IFOV, double *Range, double *Azimuth, double *Elevation );

void ecr2att ( double *Orientation_Vector, double *Yaw, double *Pitch, double *Roll);
void bdy2att ( double *Orientation_Vector, double *Yaw, double *Pitch, double *Roll);
void int2att ( double Transform[3][3], double *Yaw, double *Pitch, double *Roll);
void int2truth (double In_Roll,double *Orientation_Vector,double *Yaw, double *Pitch, double *Roll, double Transform[3][3]);
void ecr2truth (double In_Roll,double *Orientation_Vector,double *Yaw, double *Pitch, double *Roll, double Transform_Sen[3][3]);
void att2sen (double Yaw, double Pitch, double Roll, double Transform[3][3]);

/*xform_server_update.c */
/*
void Xform_Server(int Mode,char *New_Data,TransformServerRequest *Request,TransformServerResponse *Response, int* Error );
void Truth_Xform_Server(int Mode,char *New_Data,TransformServerRequest *Request,TransformServerResponse *Response, int* Error );
void Update_Xform_Server(Xfrm_Data *DataBase,int Mode,char *New_Data,int* Error );
void Update_Int_Queue(Xfrm_Data *Data,InterceptorData *Int_Data );
void Update_Fsm_Queue(Xfrm_Data *Data,FSMPosition *Fsm_Data );
void Update_Bsm_Queue(Xfrm_Data *Data,BSMPosition *Bsm_Data );
int Search_Interpolate_Xform_Queue( const Xfrm_Data *Data
                  , const TransformServerRequest *Request
                  ,TransformServerResponse *Response 
                  );


void Transform( int Type, double *In, double *Out, int Mode, double Time, 
        int Sensor_ID, int *Error );
void Truth_Transform( int Type, double *In, double *Out, int Mode, double Time, 
        int Sensor_ID, int *Error );
*/

#ifdef CPP
};
#endif

/* -------------------------------------------------- */

#endif

