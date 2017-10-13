/* ===========================================================================
*
*   Disclaimer:    This software was developed for the Department of
*                  Defense (DoD). All US Government restrictions on
*                  software source code distribution apply.
*
* PROPERTY OF AFRL/RRS
*
* ============================================================================
*
*   Purpose:
*
*   Synopsis:
*
*   Description:
*         The functional details --
*
*   Return:
*         Return values --
*
*   Error Conditions and Actions:
*         None.
*
*   Notes:
*         None.
*
* ========================================================================== */
//#include "stdafx.h"
#include "mini_blas_vec.h"
#include <stdio.h>

/* Miniature blas and vec, based on ditp need, silican graphics only.  */
/* Refer to Wafer Scale Signal Processor User Manual, 27 Jan 1997      */

extern void dcopy(int n,double*dx,int incx,double* dy,int incy)
{
  int k;

  for (k= 0; k<n; k++)
  {
    *dy = *dx;
    dy = dy + incy;
    dx = dx + incx;
  }
}

extern void dvadd(int n,double* dx,int incx,double* dy,int incy,double* dz,int incz)
{
  int k;
 
  for (k= 0; k<n; k++)
  {
    *dz = *dx + (*dy);
    dy = dy + incy;
    dx = dx + incx;
    dz = dz + incz;
  }
}

extern void dvsub(int n,double* dx,int incx,double* dy,int incy,double* dz,int incz)
{ 
  int k; 
  
  for (k= 0; k<n; k++) 
  { 
    *dz = *dx - (*dy); 
    dy = dy + incy; 
    dx = dx + incx; 
    dz = dz + incz; 
  } 

} 
 

extern void dvmul(int n,double* dx,int incx,double* dy,int incy,double* dz,int incz)
{ 
  int k; 
  
  for (k= 0; k<n; k++) 
  { 
    *dz = *dx * (*dy); 
    dy = dy + incy; 
    dx = dx + incx; 
    dz = dz + incz; 
  } 
} 
 


extern void dsvt(int n,double* da,double* dx,int incx,double* dy,int incy)
{ 
  int k;
   
  for (k= 0; k<n; k++)
  {
    *dy = *dx * (*da);
    dy = dy + incy;
    dx = dx + incx;
  }
  
} 
 


extern void daxpy(int n, double* da, double* dx, int incx, double* dy, int incy)
{ 
/*???? */
    printf("mini_blas_vec.c, daxpy NOT IMPLEMENTED\n"); 
} 
 

extern void dscal(int n, double* da, double* dx, int incx)
{ 
  int k;

  for (k=0; k<n; k++)
  {
    *dx = *dx * (*da);
    dx = dx + incx;
  }
} 
 

extern double ddot(int n, double* dx, int incx, double* dy, int incy)
{ 
    int i;
    double result;

    result = 0.0;
    for (i=0; i<n; i++) {
        result = result + (*dx * *dy);
        dy = dy + incy;
        dx = dx + incx;
    }
    return (result);

} 
 


