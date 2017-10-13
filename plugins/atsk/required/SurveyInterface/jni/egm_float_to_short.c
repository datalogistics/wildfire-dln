/**
 * Used to generate a smaller dataset for the egm96 and egm84 that fits in a short value.
 * gcc test.c -lm
 */

#include "egm96.h"
#include "egm84.h"
#include <stdio.h>
#include <math.h>


main() { 

    int n = sizeof(egm96_array) / sizeof(float);
    int i = 0;

    FILE * f96 = fopen("egm96s.h", "w");


    fprintf(f96, "// generated from the floating point values in the egm96.h file\n");
    fprintf(f96, "short egm96_array[] = {\n\t");

    for (i = 0; i < n; ++i) { 
        fprintf(f96, "%d, ", (short)roundf(egm96_array[i] * 100));
        if (i % 5 == 4)
             fprintf(f96, "\n\t");
    } 
    fprintf(f96, "};\n");

    fclose(f96);
    
    n = sizeof(egm84_array) / sizeof(float);
    i = 0;

    FILE * f84 = fopen("egm84s.h", "w");
    fprintf(f84, "// generated from the floating point values in the egm84.h file\n");
    fprintf(f84, "short egm84_array[] = {\n");

    for (i = 0; i < n; ++i) { 
        fprintf(f84, "%d, ", (short)roundf(egm84_array[i] * 100));
        if (i % 5 == 4)
             fprintf(f84, "\n\t");
    } 
    fprintf(f84, "};\n");

    fclose(f84);

}
