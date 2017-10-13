/**********************************************************************
* Disclaimer:    This software was developed for the Department of    *
*                Defense (DoD). All US Government restrictions on     *
*                software source code distribution apply              *
*                                                                      *
*  PROPERTY OF AFRL/RRS                                               *
**********************************************************************/
/*
* ============================================================================
*
*   Notes: None.
*
* ========================================================================== */
#ifndef MINI_BLAS_VEC__H
#define MINI_BLAS_VEC__H

#ifndef WSSP_PLATFORM

/* Miniature blas and vec, based on ditp need, silican graphics only.  */
/* Refer to Wafer Scale Signal Processor User Manual, 27 Jan 1997      */

extern void dcopy(int n,double*dx,int incx,double* dy,int incy);

extern void dvadd(int n,double* dx,int incx,double* dy,int incy,double* dz,int incz);

extern void dvsub(int n,double* dx,int incx,double* dy,int incy,double* dz,int incz);
 

extern void dvmul(int n,double* dx,int incx,double* dy,int incy,double* dz,int incz);
 

extern void dsvt(int n,double* da,double* dx,int incx,double* dy,int incy);
 

extern void daxpy(int,double*,double*,int,double*,int);
 

extern void dscal(int,double*,double*,int);
 

extern double ddot(int,double*,int,double*,int);
 

#endif /* WSSP_PLATFORM */

#endif /* MINI_BLAS_VEC__H */ 
