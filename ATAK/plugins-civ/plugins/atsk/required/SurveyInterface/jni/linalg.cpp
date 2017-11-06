/**********************************************************************
* Disclaimer:    This software was developed for the Department of    *
*                Defense (DoD). All US Government restrictions on     *
*                software source code distribution apply              *
*                                                                      *
*  PROPERTY OF AFRL/RRS                                               *
**********************************************************************/

/*----------------------------------------------------------------------------*/
/* SCCS Version Information */
   static char const *pSrc_version = "@(#) linalg.c 1.15 2/11/0 @(#)";
/*----------------------------------------------------------------------------*/

//#include "stdafx.h"

//#include "style.h"
#include "string.h"
//#include "MessageStructures.h"
#include "linalg.h"
//#include "matrix_lud_double.h"
#include <stdio.h>



/*

    10/30/98 D. Welchons - Change MatInv to use fixed size vectors
        and arrays of [5] and [5][5] instead of malloc'd data.
        Replaced printf with PRINTF.
        04/14/99 M. Moore - tried to make it more WSSP friendly.
        Rewrote using IVEC, BLASS.  MatInv no longer general
                purpose - it does specific size matrices.
        06/21/99 D. Welchons - Previously modified MatInv for 2x2 and 3x3 
                cases based on derivations by Brian Rahn and Mike Moore.
                Modified PRINTF in 3x3 case to use %e instead of %f.
        07/15/99 M. Moore - added, for non-WSSP platforms, equivalent
        fncs for MatMatMult9X9Double, MatMatMult9X9TDouble,
        MatMatMult9X1Double.
        08/04/99 D. Welchons - added, for non-WSSP platforms, equivalent
        function for MatMatMult3X3Double.
        09/09/99 D. Welchons - Added function mcpy.
        09/22/99 D. Welchons - Added functions MatTVecMult3X3X3X1Double,
                MatVecMult3X3X3X1Double, poly_sp_even,
                poly_sp_odd, fcos, fsin.
        09/28/99 D. Welchons - Modified fsin, fcos from Mike Moore's version.
        01/06/2000 D. Welchons - Replaced HPC_SIM with WSSP_PLATFORM,WSSP_FPMI.
*/



#ifdef DEBUG
#define CASE(A) case A: printf("MS: %s\n",#A);
#else
#define CASE(A) case A:
#endif


#define PUBLIC
#define BUF_SIZE 100

static char strg_buf[BUF_SIZE];

PUBLIC void MatTransp( double *Out
                    , double *In
                 , int r          /* the number of rows */
                 , int c          /* the number of columns */
                 ) 
{

  int i;
  int InStride;
  #ifdef VERBOSE_MAT_TRANSP
  //sprintf(strg_buf,"MatTransp %d rows, %d columns", r,c);
  //Diag(strg_buf,1);
  #endif

  for (i = 0; i<r; i++)
  {
    InStride  = i*c;
    dcopy( c, In+InStride, 1, Out+i, r); 
  }
}

PUBLIC void MatMatMult( double *C    /*  Where we put the result */  
              , double *A    /*  first matrix            */ 
              , double *B    /*  Second Matrix           */ 
              , int norA    /* number_first_matrix_rows    */          
              , int nocA    /* number_first_matrix_columns or 2nd mtx rows */
              , int nocB    /* number_second_matrix_columns*/ 
              )
{
  int i;
  int j;
  int strideC;
  int strideA;
  int I;
  int II;

  #ifdef VERBOSE_MATMATMULT
  //sprintf(strg_buf,"MatMatMult norA=%d, nocA=%d, nocB=%d", norA,nocA,nocB);
  //Diag(strg_buf,1);
  #endif

  /* We don't have enough information for a constraint test
     on conformability.  So, we assume operands are conformable.
  */
  if ( (norA == 9) && (nocB==9) && (nocA == 9) )
  {
    strideA = 9;
    strideC = 9;
    for (i=0; i< norA; i++)
    {
      I  = strideC*i;
      II = strideA*i;
      *C++  = ddot( 9, A+II, 1, B, 9);
      *C++  = ddot( 9, A+II, 1, B+1, 9);
      *C++  = ddot( 9, A+II, 1, B+2, 9); 
      *C++  = ddot( 9, A+II, 1, B+3, 9); 
      *C++  = ddot( 9, A+II, 1, B+4, 9); 
      *C++  = ddot( 9, A+II, 1, B+5, 9); 
      *C++  = ddot( 9, A+II, 1, B+6, 9); 
      *C++  = ddot( 9, A+II, 1, B+7, 9); 
      *C++  = ddot( 9, A+II, 1, B+8, 9); 
    }  
  }
  else
  {
    strideC = nocB;
    strideA = nocA;

    for (i=0; i< norA; i++)
    {
      I  = strideC*i;
      II = strideA*i;
      for (j=0; j<nocB; j++)
      {
        *(C+I+j) = ddot( nocA, A+II, 1, B+j, nocB);
      }
    }
  }
}  

 
          
PUBLIC void MatScalAdd( double *Out
                      , double *In
                      , double s
                      , int r
                      , int c
                      )
{ /* too bad dvadd stride can't be zero!  */ 
    int i;
    int stride;

#ifdef VERBOSE_MAT_SCAL_ADD
//sprintf(strg_buf,"MatScalAdd  r=%d, c = %d",r,c);
//Diag(strg_buf,1);
#endif

    stride = r*c;

    for (i=0; i < stride; i++)
    {
      *(Out++) = *(In++) + s;
    }
} 
 

PUBLIC int MatInv( double *Out
                 , double *In
                 , int n
                 , double *det
                 ) /* return zero if ok, else a -1 */ 
{
  #define NZ(A) (A!=0.0)
  int result;
  double det_tmp=0.0;


  result = -1;

  switch(n)
  {
    CASE(1)
    {
      if (*In != 0.0)
      {
         *Out = 1.0/ (*In);
         result = 0;
      }
    } break;

    CASE(2)
    {
      typedef struct
      {
         double A,B;
         double C,D;

      } m2x2;
      m2x2    *M;
      
      typedef struct
      {
         double NA,NB;
         double NC,ND;

      } Invm2x2;
      Invm2x2 *Q;
      double inv_det;

      M = (m2x2 *)    In;
      Q = (Invm2x2 *) Out;

      det_tmp = M->A*M->D - M->B*M->C;
      if (det_tmp) {
          inv_det = 1.0/det_tmp;
          Q->NA =   inv_det*M->D;
          Q->NB = - inv_det*M->B;
          Q->NC = - inv_det*M->C;
          Q->ND =   inv_det*M->A;
          result = 0;
      } else {
          result = -1;
      }

      #ifdef VERBOSE_MATINV
      //sprintf(strg_buf,"linalg.c, MatInv(), 2D A=%e B=%e C=%e D=%e det_tmp=%e result=%d",
      //        A,B,C,D,det_tmp,result);
      //Diag(strg_buf,1);
      #endif
    }break;

    CASE(3)
    {

      /*            | A  B  C |
       *     M =    | D  E  F |
       *            | G  H  I |
       *
       *                            | Cof(A)  Cof(B) Cof(C) |
       *     inv(M) = 1 \ Det(M)  * | Cof(D)  Cof(E) Cof(F) |
       *                            | Cof(G)  Cof(H) Cof(I) |
       *
       *
       *      where Cof(i) is obtained from blocking the row and column
       *      of M and computing the determinant of the remaining 2x2:
       *      Example: To calulate the Cof(E), block the column B,E,H
       *               and Row D,E,F leaving the matrix:
       *
       *                    | A  ------ C |
       *                    | ----------- |
       *                    | G  ------ I |
       *
       *      The determinate is   A*I-C*G. Replace this value for Cof(E)
       */

       typedef struct
       {
         double A,B,C;
         double D,E,F;
         double G,H,I;

       } m3x3;

       typedef struct
       {
         double NA,NB,NC;
         double ND,NE,NF;
         double NG,NH,NI;

       } Invm3x3;

       m3x3    *M;
       Invm3x3 *Q;
       double det_inv;

       M = (m3x3*)   (In);
       Q = (Invm3x3*)(Out);

       det_tmp=M->A*(M->E*M->I-M->F*M->H)-
               M->B*(M->D*M->I-M->F*M->G)+
               M->C*(M->D*M->H-M->E*M->G);
       if (det_tmp) {
           det_inv=1.0/det_tmp;

           Q->NA=  (M->E*M->I - M->F*M->H)*det_inv;
           Q->ND= -(M->D*M->I - M->F*M->G)*det_inv;
           Q->NG=  (M->D*M->H - M->E*M->G)*det_inv;
           Q->NB= -(M->B*M->I - M->C*M->H)*det_inv;
           Q->NE=  (M->A*M->I - M->C*M->G)*det_inv;
           Q->NH= -(M->A*M->H - M->B*M->G)*det_inv;
           Q->NC=  (M->B*M->F - M->C*M->E)*det_inv;
           Q->NF= -(M->A*M->F - M->C*M->D)*det_inv;
           Q->NI=  (M->A*M->E - M->B*M->D)*det_inv;

           #ifdef VERBOSE_MATINV
           //sprintf(strg_buf,"MatInv(), 3x3 case:");
           //Diag(strg_buf,1);
           //sprintf(strg_buf,"%e %e %e ",Q->NA,Q->NB,Q->NC);
           //Diag(strg_buf,1);
           //sprintf(strg_buf,"%e %e %e ",Q->ND,Q->NE,Q->NF);
           //Diag(strg_buf,1);
           //sprintf(strg_buf,"%e %e %e ",Q->NG,Q->NH,Q->NI);
           //Diag(strg_buf,1);
           //sprintf(strg_buf,"Determ=%f ",det_tmp);
           //Diag(strg_buf,1);
           #endif

           result = 0;
       } else {
           result = -1;
       }

    }break;
 
    CASE(4)
    {  /* An algebraic solution.  The compiler can do a lot of optimization on this.  */

      typedef struct elements
      {
        double a11,a12,a13,a14 ;
        double a21,a22,a23,a24 ;
        double a31,a32,a33,a34 ;
        double a41,a42,a43,a44 ;

      } m4x4;

      m4x4 *M, *Q;

      M = (m4x4*)(In);
      Q = (m4x4*)(Out);


      det_tmp =
      (((M->a11*(((M->a22*(M->a33*M->a44-M->a43*M->a34))-(M->a23*(M->a32*M->a44-M->a42*M->a34)))+(M->a24*(M->a32*M->a43-M->a42*M->a33))))-
        (M->a12*(((M->a21*(M->a33*M->a44-M->a43*M->a34))-(M->a23*(M->a31*M->a44-M->a41*M->a34)))+(M->a24*(M->a31*M->a43-M->a41*M->a33)))))+
       ((M->a13*(((M->a21*(M->a32*M->a44-M->a42*M->a34))-(M->a22*(M->a31*M->a44-M->a41*M->a34)))+(M->a24*(M->a31*M->a42-M->a41*M->a32))))-
        (M->a14*(((M->a21*(M->a32*M->a43-M->a42*M->a33))-(M->a22*(M->a31*M->a43-M->a41*M->a33)))+(M->a23*(M->a31*M->a42-M->a41*M->a32))))));


      if(det_tmp!=0)
      {
         Q->a11=(M->a22*(M->a33*M->a44-M->a34*M->a43)-M->a23*(M->a32*M->a44-M->a34*M->a42)+M->a24*(M->a32*M->a43-M->a33*M->a42))/det_tmp;
         Q->a21=(-M->a21*(M->a33*M->a44-M->a34*M->a43)+M->a23*(M->a31*M->a44-M->a34*M->a41)-M->a24*(M->a31*M->a43-M->a33*M->a41))/det_tmp;
         Q->a31=(M->a21*(M->a32*M->a44-M->a34*M->a42)-M->a22*(M->a31*M->a44-M->a34*M->a41)+M->a24*(M->a31*M->a42-M->a32*M->a41))/det_tmp;
         Q->a41=(-M->a21*(M->a32*M->a43-M->a33*M->a42)+M->a22*(M->a31*M->a43-M->a33*M->a41)-M->a23*(M->a31*M->a42-M->a32*M->a41))/det_tmp;

         Q->a12=(-M->a12*(M->a33*M->a44-M->a34*M->a43)+M->a13*(M->a32*M->a44-M->a34*M->a42)-M->a14*(M->a32*M->a43-M->a33*M->a42))/det_tmp;
         Q->a22=(M->a11*(M->a33*M->a44-M->a34*M->a43)-M->a13*(M->a31*M->a44-M->a34*M->a41)+M->a14*(M->a31*M->a43-M->a33*M->a41))/det_tmp;
         Q->a32=(-M->a11*(M->a32*M->a44-M->a34*M->a42)+M->a12*(M->a31*M->a44-M->a34*M->a41)-M->a14*(M->a31*M->a42-M->a32*M->a41))/det_tmp;
         Q->a42=(M->a11*(M->a32*M->a43-M->a33*M->a42)-M->a12*(M->a31*M->a43-M->a33*M->a41)+M->a13*(M->a31*M->a42-M->a32*M->a41))/det_tmp;

         Q->a13=(M->a12*(M->a23*M->a44-M->a24*M->a43)-M->a13*(M->a22*M->a44-M->a24*M->a42)+M->a14*(M->a22*M->a43-M->a23*M->a42))/det_tmp;
         Q->a23=(-M->a11*(M->a23*M->a44-M->a24*M->a43)+M->a13*(M->a21*M->a44-M->a24*M->a41)-M->a14*(M->a21*M->a43-M->a23*M->a41))/det_tmp;
         Q->a33=(M->a11*(M->a22*M->a44-M->a24*M->a42)-M->a12*(M->a21*M->a44-M->a24*M->a41)+M->a14*(M->a21*M->a42-M->a22*M->a41))/det_tmp;
         Q->a43=(-M->a11*(M->a22*M->a43-M->a23*M->a42)+M->a12*(M->a21*M->a43-M->a23*M->a41)-M->a13*(M->a21*M->a42-M->a22*M->a41))/det_tmp;

         Q->a14=(-M->a12*(M->a23*M->a34-M->a24*M->a33)+M->a13*(M->a22*M->a34-M->a24*M->a32)-M->a14*(M->a22*M->a33-M->a23*M->a32))/det_tmp;
         Q->a24=(M->a11*(M->a23*M->a34-M->a24*M->a33)-M->a13*(M->a21*M->a34-M->a24*M->a31)+M->a14*(M->a21*M->a33-M->a23*M->a31))/det_tmp;
         Q->a34=(-M->a11*(M->a22*M->a34-M->a24*M->a32)+M->a12*(M->a21*M->a34-M->a24*M->a31)-M->a14*(M->a21*M->a32-M->a22*M->a31))/det_tmp;
         Q->a44=(M->a11*(M->a22*M->a33-M->a23*M->a32)-M->a12*(M->a21*M->a33-M->a23*M->a31)+M->a13*(M->a21*M->a32-M->a22*M->a31))/det_tmp;

         result = 0;
      }
      else
      {
         result = -1;
      }


    }break;

    default:
    {
      //sprintf(strg_buf," linalg.MatInv: Someone slipped me a %dX%d matrix.  I can`t do that.",n,n);
      //Diag(strg_buf,1);
      result = -1;
    } break;
  }
  (*det) = det_tmp;
  return(result);

}




/* This is used as test data for the Inverse function. Normally commented out.
    a[1][1] = 2.0;
    a[1][2] = 0.0;
    a[1][3] = 1.0;
    a[1][4] = 2.0;
    a[2][1] = 1.0;
    a[2][2] = 1.0;
    a[2][3] = 0.0;
    a[2][4] = 2.0;
    a[3][1] = 2.0;
    a[3][2] = -1.0;
    a[3][3] = 3.0;
    a[3][4] = 1.0;
    a[4][1] = 3.0;
    a[4][2] = -1.0;
    a[4][3] = 4.0;
    a[4][4] = 3.0;

    for (i=1; i<=n; i++) {
        for (j=1; j<=n; j++) {
            *(In + (i-1)*n + (j-1)) = a[i][j];
        }
    }

End of commented out area 
*/

/* This is used when testing out inverse function. Normally commented out.

    sprintf(strg_buf," Result inverse matrix Y =");
        Diag(strg_buf,1);
    for (i=1; i<=n; i++) {
        for (j=1; j<=n; j++) {
            sprintf(strg_buf,"%f ",*(Out + (i-1)*n + (j-1)));
                        Diag(strg_buf,1);
        }
        sprintf(strg_buf," ");
                Diag(strg_buf,1);
    }
 When used, the results should be:

 Result inverse matrix Y =

1.000000 0.000000 1.000000 -1.000000 
-1.000000 1.666667 1.666667 -1.000000 
-1.000000 0.666667 0.666667 0.000000 
-0.000000 -0.333333 -1.333333 1.000000 

*/


PUBLIC void MatVectMult( double *Out
                       , double *In1
                       , double *In2
                       , int r
                       , int c
                       )
{
    int i;
    int stride;

#ifdef VERBOSE_MAT_VEC_MULT
//sprintf(strg_buf,"MatVectMult: r=%d, c=%d", r,c);
//Diag(strg_buf,1);
#endif
    stride = c; 
    for (i=0; i<r; i++) 
    {
      Out[i] = ddot( c, In1+(i*stride), 1, In2, 1); 
    }
}

PUBLIC void VectMatMult( double *Out
                       , double *In1
                       , double *In2
                       , int c
                       , int r
                       )
{ 
    int i;
    int stride; 

#ifdef VERBOSE_VEC_MAT_MULT
//sprintf(strg_buf,"VectMatMult: c=%d, r=%d", c,r);
//Diag(strg_buf,1);
#endif

    stride = c; 
    for (i=0; i<r; i++) 
    { 
      Out[i] = ddot( c, In1+i, stride, In2, 1); 
    }    
}


void PrintMat(char *name, double *In, int r, int c) 
{
    int i,j;
    
    //sprintf(strg_buf,"Matrix %s = ",name);
    //Diag(strg_buf,1);
    for (i=0; i<r; i++) {
        for (j=0; j<c; j++) {
            //sprintf(strg_buf,"%+.6e ",In[i*c+j]);
            //Diag(strg_buf,1);
        }
    //sprintf(strg_buf," ");
        //Diag(strg_buf,1);
    }
}

void PrintMat6DecPlaces(char *name, double *In, int r, int c) 
{
    int i,j;
    
    //sprintf(strg_buf,"Matrix %s = ",name);
    //Diag(strg_buf,1);

    for (i=0; i<r; i++) {
        for (j=0; j<c; j++) {
           // sprintf(strg_buf,"%+.6e ",In[i*c+j]);
           // Diag(strg_buf,1);
        }
    //sprintf(strg_buf," ");
        //Diag(strg_buf,1);
    }
}

void PrintVec(char *name, double *In, int c) 
{
    int i;
    //sprintf(strg_buf,"Vector %s = ",name);
    //Diag(strg_buf,1);
    for (i=0; i<c; i++) 
    {
       //sprintf(strg_buf,"%+.6e ",In[i]);
       //Diag(strg_buf,1);
    }
}

PUBLIC void MatMatMult9X9Double(double *C , double *A , double *B )
{
  int i;
  int strideC;
  int strideA;
  int II;

  strideA = 9;
  strideC = 9;
  for (i=0; i< 9; i++)
  {
    II = strideA*i;
    *C++  = ddot( 9, A+II, 1, B, 9);
    *C++  = ddot( 9, A+II, 1, B+1, 9);
    *C++  = ddot( 9, A+II, 1, B+2, 9);
    *C++  = ddot( 9, A+II, 1, B+3, 9);
    *C++  = ddot( 9, A+II, 1, B+4, 9);
    *C++  = ddot( 9, A+II, 1, B+5, 9);
    *C++  = ddot( 9, A+II, 1, B+6, 9);
    *C++  = ddot( 9, A+II, 1, B+7, 9);
    *C++  = ddot( 9, A+II, 1, B+8, 9);
  } 
}

PUBLIC void MatMatMult9X9TDouble(double *C , double *A , double *B )
{
  int i; 
  int strideA; 
  int II; 
 
  strideA = 9; 
  for (i=0; i< 9; i++) 
  { 
    II = strideA*i; 
    *C++  = ddot( 9, A+II, 1, B, 1); 
    *C++  = ddot( 9, A+II, 1, B+(1*9), 1); 
    *C++  = ddot( 9, A+II, 1, B+(2*9), 1); 
    *C++  = ddot( 9, A+II, 1, B+(3*9), 1); 
    *C++  = ddot( 9, A+II, 1, B+(4*9), 1); 
    *C++  = ddot( 9, A+II, 1, B+(5*9), 1); 
    *C++  = ddot( 9, A+II, 1, B+(6*9), 1); 
    *C++  = ddot( 9, A+II, 1, B+(7*9), 1); 
    *C++  = ddot( 9, A+II, 1, B+(8*9), 1); 
  }  
}   

PUBLIC void MatMatMult9X1Double(double *C , double *A , double *B )
{
 
    *C++  = ddot( 9, A,       1, B, 1); 
    *C++  = ddot( 9, A+(1*9), 1, B, 1); 
    *C++  = ddot( 9, A+(2*9), 1, B, 1); 
    *C++  = ddot( 9, A+(3*9), 1, B, 1); 
    *C++  = ddot( 9, A+(4*9), 1, B, 1); 
    *C++  = ddot( 9, A+(5*9), 1, B, 1); 
    *C++  = ddot( 9, A+(6*9), 1, B, 1); 
    *C++  = ddot( 9, A+(7*9), 1, B, 1); 
    *C++  = ddot( 9, A+(8*9), 1, B, 1); 
}          

PUBLIC void MatMatMult3X3Double(double *C , double *A , double *B )
{
  int i;
  int strideC;
  int strideA;
  int II;

  strideA = 3;
  strideC = 3;
  for (i=0; i< 3; i++)
  {
    II = strideA*i;
    *C++  = ddot( 3, A+II, 1, B, 3);
    *C++  = ddot( 3, A+II, 1, B+1, 3);
    *C++  = ddot( 3, A+II, 1, B+2, 3);
  } 
}


PUBLIC void mcpy( double *dst, const double *src, int dstride, int sstride, int len)
{
  /* length is in units of doubles.  Strides must be even, and
  they are units of bytes.
  */
  dstride = dstride>>3; /* convert to units of double: 8 bytes */
  sstride = sstride >>3;  
    for(;len;len--) 
    {
      *dst = *src;
      dst += dstride;
      src += sstride;
    }
}

PUBLIC void mcpy_sp( void *dst_, const void *src_, int dstride, int sstride, int len)
{
  /* length is in units of ints or floats (4 byte).  Strides must be even, and
  they are units of bytes.
  */
  int * dst;
  int * src;

  sstride = sstride >>2; /* Convert to units of 4bytes */
  dstride = dstride >>2;
  dst = (int *)(dst_);
  src = (int *)(src_);
 
  for(;len;len--)
  {
    *dst = *src;
    dst += dstride;
    src += sstride;
  }
} 


void MatTVecMult3X3X3X1Double(double * C, double * A, double * B)
{
/*
           T
      C = A  X B
      Built to do the Mat-T-Vec multiply needed by xform_convert.
      This represents the Transpose  Matrix times a Vector 
      
            a11 a21 a31| b1  =  a11*b1+ a21*b2 + a31*b3
            a12 a22 a32| b2  =  a12*b1+ a22*b2 + a32*b3
            a13 a23 a33| b3  =  a13*b1+ a23*b2 + a33*b3
             
*/
      C[0] = ddot( 3, A+0, 3, B, 1);  /* a11b1+a21b2+a31b3 */
      C[1] = ddot( 3, A+1, 3, B, 1);  /* a12b1+a22b2+a32b3 */
      C[2] = ddot( 3, A+2, 3, B, 1);  /* a13b1+a23b2+a33b3 */

}

void MatVecMult3X3X3X1Double(double * C, double * A, double * B)
{
/*  C = AXB
    Built to do the MatVec multiply needed by xform_convert.
    This represents the normal Matrix times a Vector 
      
            a11 a12 a13| b1  =  a11*b1+ a12*b2 + a13*b3
            a21 a22 a23| b2  =  a21*b1+ a22*b2 + a23*b3
            a31 a32 a33| b3  =  a31*b1+ a32*b2 + a33*b3
*/
      

      C[0] = ddot( 3, A,   1, B, 1);
      C[1] = ddot( 3, A+3, 1, B, 1);
      C[2] = ddot( 3, A+6, 1, B, 1);

}




#ifdef WSSP_PLATFORM
#include "isa_mp.h"
#endif
#include "linalg.h"
#include <math.h>

/* fsin is a single precision sine fnc that makes use of poly_sp. */
/* Maintenance note: Make sure all floating constants have an f suffix.
   Otherwise, you are doing double precision stuff.
*/


#define PIoverTWO 1.570796327f
#define PI 3.141592654f
#define TWOPI 6.283185307f
#define TWOPId 6.283185307 
#define TERMS 6


#ifndef WSSP_PLATFORM 
float poly_sp_odd( float x, float coef[], unsigned terms)
{
  /*  y = x*coef[0] * x^3*coef[1] + x^5*coef[2] + etc    */
  /* Horner's rule applied to simplify it.  */
  /*     -- just facor out x^2 on each term    */ 
  int i; 
  float y, xx; 
   
  xx = x*x; 
  y = 0.0;
  for( i = (terms-1); i >0; i--)
  { 
    y = xx*(y+coef[i]);
  } 
  y = x*(y + coef[0]);
  return(y);
}
#endif 

#if 0
float fsin(float x)
{
static float COEF[TERMS] = {  1.0
                           , -0.166666666666666667
                           ,  0.008333333333333372
                           , -0.000198412698412699
                           ,  0.000002755731921890
                           , -0.000000025052108383
                           };
  float sign;
  double X, Y; // D. Welchons 9/27/99

//  return(sin(x)); // D. Welchons 9/27/99

  /* box x into 1st quadrant. */
  if (x<0.0f)
  { /* 9 extra ticks */
    x=-x;
    sign = -1.0f;
  }
  else
  {
    sign = 1.0f;
  }

  if (x > TWOPI)  /* confine to one cycle */
  {  /*  This is rare, and it is usually a bazar case  */
    int K;

    if (x == 1000000.0f)
    { 
      /* Special case used for transform server - roll angle.
         1000000 radians used: txs_undefined, in message_structures.h.  It
         is a flag to mean something is undefined.  We hard wire the fsin because
         we know this case happens, and we don't want to be spending a lot of time
         computing it.
      */
   
      return(-0.3499993563f*sign); 
    }       

    /* Tis is hard to compute, so we try to avoid doing it. */
    K = (int)(x/TWOPI);
    x = x - ((float)(K))*TWOPI;
    X = x;
    Y = (X/TWOPId);
    K = (int)(Y);
    x = X - ((double)(K)*TWOPId);

  }
  if (x > PI)  /* 180 .. 360 => 0 .. 180 */
  {
    x = x-PI;
    sign = -sign;
  }

  if (x > PIoverTWO)  /* 90 .. 180 => 0 .. 90 */
  {
    x = PI - x;
  }
  if (x == 0.0f) return(0.0f);

  /* Now we are ready to apply a polynomial expansion */
  x =poly_sp_odd( x, COEF, TERMS);
  return(x*sign);
}
#endif






#ifdef WSSP_PLATFORM
#include "isa_mp.h"
#endif
#include "linalg.h"
#include <math.h>

/* fcos is a single precision sine fnc that makes use of poly_sp. */
/* Maintenance note: Make sure all floating constants have an f suffix.
   Otherwise, you are doing double precision stuff.
*/

#define THREEPIOVERTWO 4.71238898f
#define PIOVERTWO 1.570796327f
#define PI 3.141592654f
#define TWOPI 6.283185307f
#define TWOPId 6.283185307 
#define TERMS 6

#ifndef WSSP_PLATFORM 
float poly_sp_even( float x, float coef[], unsigned terms)
{
  /*  y = 1.0*coef[0] * x^2*coef[1] + x^4*coef[2] + etc    */
  /* Horner's rule applied to simplify it.  */
  /*     -- just factor out x^2 on each term    */
  int i;
  float y, xx;

  xx = x*x;
  y = 0.0;
  for( i = (terms-1); i >0; i--)
  {
    y = xx*(y+coef[i]); 
  }
  y = y + coef[0];
  return(y);
}

#endif

#if 0

float fcos(float x)
{
static float COEF[TERMS] = {  1.0
               , -0.5 
                           ,  0.04166666666666732 
                           , -0.001388888888888893 
                           ,  0.000024801587292937 
                           , -0.000000275573192202 
//                           ,  0.000000002087712071
//               , -0.000000000011470879
                           };

  float sign;
  int K;
  double X, Y;

  sign = 1.0f;

  /* box x into 1st quadrant. */
  if (x<0.0f)
  { 
    x=-x;
  }

  if (x > TWOPI)  /* confine to one cycle */
  {  /*  This hopefully happens very little. */
    if (x == 1000000.0f)
    {
      /* Special case used for transform server - roll angle.
     1000000 radians used: txs_undefined, in message_structures.h.  It
     is a flag to mean something is undefined.  We hard wire the fcos because
     we know this case happens, and we don't want to be spending a lot of time
     computing it.
      */
      return(0.936752218f);
    }

    /* We avoid doing this when possible, because it is a bit of a hog. */
    X = x;
    Y = (X/TWOPId);
    K = (int)(Y);
    x = X - ((double)(K)*TWOPId);
    #ifdef FCOS_VERBOSE
    //sprintf(strg_buf,"  K = %d,   x/2PI = %f", K,  x/TWOPI); 
    //Diag(strg_buf,1);
    #endif
  }


  if (x> PI) /* 180 .. 360 => 0 .. 180 */
  {
    if (x > THREEPIOVERTWO)
    {
      x = TWOPI - x; 
    }
    else
    {
      x = x-PI;
      sign = -sign; // D. Welchons, 9/27/99 Case of neg cos over 180..270
    }
  }
  else
  {
    if (x > PIOVERTWO)
    {
      x = PI - x;
      sign = -sign;
    }
    else
    {
      /* Nothing to do */
    }
  }

  if (x == 0.0f) return(1.0f*sign);// D. Welchons 9/27/99 Case of original x=PI
                                   // Cos(PI) = -1.0

  /* Now we are ready to apply a polynomial expansion */
  x =poly_sp_even( x, COEF, TERMS);
  return(x*sign);
}
#endif

 



