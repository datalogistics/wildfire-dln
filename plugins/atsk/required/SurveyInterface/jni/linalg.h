/**********************************************************************
* Disclaimer:    This software was developed for the Department of    *
*                Defense (DoD). All US Government restrictions on     *
*                software source code distribution apply              *
*                                                                      *
*  PROPERTY OF AFRL/RRS                                               *
**********************************************************************/
#ifndef LINALG_H
#define LINALG_H

#ifdef WSSP_PLATFORM
#include "blas.h"
#include "vec.h"
#else
#include "mini_blas_vec.h"
#endif

#ifdef _c_plus_plus_
extern 'C' {
#endif
/* #define START_DEBUG_COUNT    0 */

void MatTransp(double *Out, double *In, int r, int c); 
void MatMatMult(double *Out, double *In1, double *In2, int r1,int c1r2,
                                                                     int c2);
#define MatMatAdd(Out, In1, In2, r,  c) dvadd( ((r)*(c)), In1, 1, In2, 1, Out, 1)
#define MatMatSub(Out, In1, In2, r,  c) dvsub( ((r)*(c)), In1, 1, In2, 1, Out, 1)
#define MatScalMult(Out, In,  s, r, c)  \
      { \
        double SCALAR_ARG; \
        SCALAR_ARG = s; \
        dsvt( ((r)*(c)), &SCALAR_ARG, In, 1, Out, 1); \
      }
void mcpy( double *dst, const double *src, int dstride, int sstride, int len);
void mcpy_sp( void *dst_, const void *src_, int dstride, int sstride, int len);
void MatMatMult9X9Double(double * , double * , double * );
void MatMatMult9X9TDouble(double * , double * , double * );
void MatMatMult9X1Double(double * , double * , double * );
void MatMatMult3X3Double(double *C , double *A , double *B );
void MatVecMult3X3X3X1Double(double *, double *, double*);
void MatTVecMult3X3X3X1Double(double *, double *, double*);

void MatScalAdd(double *Out, double *In, double s, int r, int c);
#define DotProd( In1, In2, r) ddot( r, In1, 1, In2, 1)
int MatInv(double *Out, double *In, int n, double *det);
void MatVectMult(double *Out, double *In1, double *In2, int r, int c);
void VectMatMult(double *Out, double *In1, double *In2, int c, int r);
#define VectVectSub(Out, In1, In2,  c) dvsub( c, In1, 1, In2, 1, Out, 1)
#define VectVectAdd(Out, In1, In2, c) dvadd(c, In1, 1, In2, 1, Out, 1)
#define VectScalMult(Out, In,  k,  c) dsvt( c, &k, In, 1, Out, 1)
void PrintMat(char * name, double *In, int r, int c); 
void PrintMat6DecPlaces(char *name, double *In, int r, int c); 
void PrintVec(char *name, double *In, int c); 

float poly_sp( float x, float *V, unsigned n);
float poly_sp_even( float x, float *V, unsigned n);
float poly_sp_odd( float x, float *V, unsigned n);

float fsin(float x);
float fcos(float x);

#ifdef _c_plus_plus_
}
#endif

#endif
