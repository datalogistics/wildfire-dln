/**
 * ATSK shapelib interface
 **/

#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <unistd.h>

#include "shapefil.h"

#include <string.h>

#define APPNAME "ShapeFile"

#define min(a,b)  ((a) < (b) ? (a) : (b))

jstring savePolyline(JNIEnv *env, jobject thiz, jstring filePath, jstring label, jdoubleArray lat, jdoubleArray lon, int saveType);
SHPHandle createOrOpenSHP(JNIEnv *env, jstring filePath, int shapeType);
DBFHandle createOrOpenDBF(JNIEnv *env, jstring filePath);
jstring addLabelToDBF(JNIEnv *env, DBFHandle hDBF, jstring label);
jstring createPRJFile(JNIEnv *env, jstring filePath);
jstring makeStr(JNIEnv *env, const char *str);

jstring Java_com_gmeci_conversions_ShapeFile_savePolygon(
	JNIEnv* env, jobject thiz,
	jstring filePath, jstring label,
	jdoubleArray lat, jdoubleArray lon) {
	
	return savePolyline(env, thiz, filePath, label, lat, lon, SHPT_POLYGON);
}

jstring Java_com_gmeci_conversions_ShapeFile_saveArc(
	JNIEnv* env, jobject thiz,
	jstring filePath, jstring label,
	jdoubleArray lat, jdoubleArray lon) {
	
	return savePolyline(env, thiz, filePath, label, lat, lon, SHPT_ARC);	
}

jstring Java_com_gmeci_conversions_ShapeFile_savePoint(
	JNIEnv* env, jobject thiz,
	jstring filePath, jstring label,
	jdouble lat, jdouble lon) {

	SHPHandle hSHP = createOrOpenSHP(env, filePath, SHPT_POINT);
	
	int panParts = 0;	
	double latCpy = lat;	
	double lonCpy = lon;
	
	jstring returnMsg;
	if (hSHP != NULL) {
		// Write new point to file
		SHPObject *newShape = SHPCreateObject(SHPT_POINT, -1, 1, &panParts, NULL,
						  1, &lonCpy, &latCpy, NULL, NULL);		
		SHPWriteObject(hSHP, -1, newShape);
		SHPDestroyObject(newShape);		
		SHPClose(hSHP);
	}
	
	DBFHandle hDBF = createOrOpenDBF(env, filePath);
	if (hDBF != NULL) {
		// Write label to new record
		returnMsg = addLabelToDBF(env, hDBF, label);
		DBFClose(hDBF);
	} else
		returnMsg = makeStr(env, "Failed to create/open DBF");
	
	// Projection definition
	return createPRJFile(env, filePath);
}

jstring savePolyline(JNIEnv *env, jobject thiz, jstring filePath, jstring label, jdoubleArray lat, jdoubleArray lon, int saveType) {
	// Copy out latitude values
	int numPoints = min((*env)->GetArrayLength(env, lat), (*env)->GetArrayLength(env, lon));
	
	if (numPoints <= 3 && saveType == SHPT_POLYGON)
		return makeStr(env, "Polygon must have more than 3 points");
	
	double *latPtr = (*env)->GetPrimitiveArrayCritical(env, lat, NULL);
	if (latPtr == NULL)
		return makeStr(env, "Latitude pointer is null");
	int pointByteCount = numPoints*sizeof(double);
	double *latCpy = malloc(pointByteCount);
	memcpy(latCpy, latPtr, pointByteCount);
	(*env)->ReleasePrimitiveArrayCritical(env, lat, latPtr, 0);
	
	// Copy out longitude values
	double *lonPtr = (*env)->GetPrimitiveArrayCritical(env, lon, NULL);
	if (lonPtr == NULL) {
		free(latCpy);
		return makeStr(env, "Longitude pointer is null");;
	}
	double *lonCpy = malloc(pointByteCount);
	memcpy(lonCpy, lonPtr, pointByteCount);
	(*env)->ReleasePrimitiveArrayCritical(env, lon, lonPtr, 0);	
	
	int panParts = 0;
	
	// Convert jstring to char*
	const char *filePathStr = (*env)->GetStringUTFChars(env, filePath, 0);
	
	// Open file if it exists, otherwise create
	SHPHandle hSHP = createOrOpenSHP(env, filePath, saveType);
	
	// Shape file
	jstring returnMsg;
	if (hSHP != NULL) {
		// Write new polygon to file
		SHPObject *newShape = SHPCreateObject(saveType, -1, 1, &panParts, NULL,
						  numPoints, lonCpy, latCpy, NULL, NULL);		
		SHPWriteObject(hSHP, -1, newShape);
		SHPDestroyObject(newShape);		
		SHPClose(hSHP);
	}
	
	// Record database
	DBFHandle hDBF = createOrOpenDBF(env, filePath);
	if (hDBF != NULL) {
		// Write label to new record
		returnMsg = addLabelToDBF(env, hDBF, label);
		DBFClose(hDBF);
	} else
		returnMsg = makeStr(env, "Failed to create/open DBF");
	
	// Projection definition
	returnMsg = createPRJFile(env, filePath);
	
	free(latCpy);
	free(lonCpy);
	return returnMsg;
}

SHPHandle createOrOpenSHP(JNIEnv *env, jstring filePath, int shapeType) {
	const char *filePathStr = (*env)->GetStringUTFChars(env, filePath, 0);
	char filePathSHP[255];
	sprintf(filePathSHP, "%s.shp", filePathStr);
	
	// Open file if it exists, otherwise create
	SHPHandle hSHP;
	if (access(filePathSHP, F_OK) != -1) {
		hSHP = SHPOpen(filePathStr, "r+b");		
		int st;
		SHPGetInfo(hSHP, NULL, &st, NULL, NULL);
		if (st != shapeType) {
			// Existing file must be polygon type
			hSHP = NULL;
		}
	} else
		hSHP = SHPCreate(filePathStr, shapeType);
	
	(*env)->ReleaseStringUTFChars(env, filePath, filePathStr);
	
	return hSHP;
}

DBFHandle createOrOpenDBF(JNIEnv *env, jstring filePath) {
	const char *filePathStr = (*env)->GetStringUTFChars(env, filePath, 0);
	char filePathDBF[255];
	sprintf(filePathDBF, "%s.dbf", filePathStr);
	
	// Open file if it exists, otherwise create
	DBFHandle hDBF;
	if (access(filePathDBF, F_OK) != -1) {
		hDBF = DBFOpen(filePathStr, "r+b");
	} else {
		hDBF = DBFCreate(filePathStr);
		if (DBFAddField(hDBF, "label", FTString, 64, 0) == -1)
			hDBF = NULL;
	}
	
	(*env)->ReleaseStringUTFChars(env, filePath, filePathStr);
	
	return hDBF;
}

jstring addLabelToDBF(JNIEnv *env, DBFHandle hDBF, jstring label) {
	int i;
	int records = DBFGetRecordCount(hDBF);
	jstring returnMsg = makeStr(env, "Failed to add label to DBF");
	
	for(i = 0; i < DBFGetFieldCount(hDBF); i++ ) {
		if (DBFGetFieldInfo(hDBF, i, NULL, NULL, NULL) == FTString) {
			const char *labelStr = (*env)->GetStringUTFChars(env, label, 0);
			if (DBFWriteStringAttribute(hDBF, records, i, labelStr) == 1)
				returnMsg = makeStr(env, "");
			(*env)->ReleaseStringUTFChars(env, label, labelStr);
			break;
		}
	}
	return returnMsg;
}

jstring createPRJFile(JNIEnv *env, jstring filePath) {
	jstring returnMsg = makeStr(env, "");
	const char *filePathStr = (*env)->GetStringUTFChars(env, filePath, 0);
	char filePathPRJ[255];
	sprintf(filePathPRJ, "%s.prj", filePathStr);
	
	// Create if it doesn't exist
	if (access(filePathPRJ, F_OK) == -1) {
		// WGS-84 projection definition
		// semiMajorAxis = 6378137
		// flattening = 298.257223563
		// degrees to radians coefficient = 0.017453292519943295				
		FILE *prjFile = fopen(filePathPRJ, "w");
		if (prjFile != NULL) {
			fputs("GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]]", prjFile);
			fclose(prjFile);
		} else
			returnMsg = makeStr(env, "Failed to create PRJ");
	}
	
	(*env)->ReleaseStringUTFChars(env, filePath, filePathStr);
	return returnMsg;
}

jstring makeStr(JNIEnv *env, const char *str) {
	return (*env)->NewStringUTF(env, str);
}
