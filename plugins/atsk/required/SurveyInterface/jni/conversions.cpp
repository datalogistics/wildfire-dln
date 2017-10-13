#include <jni.h>
#include <android/log.h>
#include <stdio.h>

#include "geoid.h"
#include "ElevationConversions.h"

#include <string.h>

#include <iostream>

#define APPNAME "Conversions"

extern "C" {

void magicout(JNIEnv*  env, jdoubleArray first, double *result);

jdoubleArray makemagic(JNIEnv*  env, double *array);

//////////////////////////////////////////////////////////////

jint
Java_com_gmeci_conversions_Conversions_tempcall(JNIEnv*  env, jobject  thiz)
{
    return 5;
}//end tempcall

void magicout(JNIEnv*  env, jdoubleArray first, double *result)
{
    jboolean isCopy2;
    jdouble* srcArrayElems2 =
               env -> GetDoubleArrayElements(first, &isCopy2);
    jint max2 = env -> GetArrayLength(first);

    for (int i = 0; i <= max2; i++) {
        result[i] = srcArrayElems2[i];
    }
    if (isCopy2 == JNI_TRUE) {
       env -> ReleaseDoubleArrayElements(first, srcArrayElems2, JNI_ABORT);
    }
}//end magicout

jdoubleArray makeDoubleArray(JNIEnv* env, int size, double darray[])
{
    jdoubleArray result;
    result = env->NewDoubleArray( size);
    if (result == NULL) {
     return NULL; /* out of memory error thrown */
    }

    env->SetDoubleArrayRegion( result, 0, size, darray);
    return result;

}


//Start Same order as ElevationConversions.cpp

jdoubleArray
Java_com_gmeci_conversions_Conversions_DoDConvertAARtoLLA(JNIEnv*  env,
                                 jobject  thiz,
                                  jdoubleArray gg_lla,
                                  jdoubleArray bg_aar,
                                  jdoubleArray bg_lla)
{
    bool create=true, ans ;
    ElevationConversions temp(create);
    double EndLat;
    double EndLon;

    jint max = env -> GetArrayLength(gg_lla);
    double *goodguy_lla;
    goodguy_lla =  (double*)malloc(sizeof(double)*max);
    magicout(env, gg_lla, goodguy_lla);

    jint max2 = env -> GetArrayLength(bg_aar);
    double *badguy_aar;
    badguy_aar =  (double*)malloc(sizeof(double)*max2);
    magicout(env, bg_aar, badguy_aar);

    jint max3 = env -> GetArrayLength(bg_lla);
    double *badguy_lla;
    badguy_lla =  (double*)malloc(sizeof(double)*max3);
    magicout(env, bg_lla, badguy_lla);

    DoD_Convert_AARtoLLA(goodguy_lla,badguy_aar,badguy_lla);

    jdoubleArray returnArray = makeDoubleArray(env, 3, badguy_lla);

    if(returnArray == NULL)
        return NULL;
    else
        return returnArray;

}//end AARtoLLA

jdoubleArray
Java_com_gmeci_conversions_Conversions_DoDConvertLLAtoAAR(JNIEnv*  env,
                                            jobject  thiz,
                                            jdoubleArray gg_lla,
                                            jdoubleArray bg_lla,
                                            jdoubleArray bg_aar)
{
    jint max = env -> GetArrayLength(gg_lla);
    double *goodguy_lla;
    goodguy_lla =  (double*)malloc(sizeof(double)*max);
    magicout(env, gg_lla, goodguy_lla);


    jint max2 = env -> GetArrayLength(bg_aar);
    double *badguy_aar;
    badguy_aar =  (double*)malloc(sizeof(double)*max2);
    magicout(env, bg_aar, badguy_aar);

    jint max3 = env -> GetArrayLength(bg_lla);
    double *badguy_lla;
    badguy_lla =  (double*)malloc(sizeof(double)*max3);
    magicout(env, bg_lla, badguy_lla);

    DoD_Convert_LLAtoAAR(goodguy_lla,badguy_lla,badguy_aar);

    jdoubleArray returnArray = makeDoubleArray(env, 3, badguy_aar);

    if(returnArray == NULL)
        return NULL;
    else
        return returnArray;

}//end LLAtoAAR

jdoubleArray
Java_com_gmeci_conversions_Conversions_GetXYOffset(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble StartLat,
                                                                jdouble StartLon,
                                                                jdouble XOffset,
                                                                jdouble YOffset, 
                                                                jdouble AngleOffset)
{
    double EndLat;
    double EndLon;
    bool  ans ;
    
    ans = ElevationConversions::GetXYOffset((double)StartLat,(double)StartLon, (float)XOffset, (float)YOffset, &EndLat, &EndLon,(float)AngleOffset);

    double badguy[2];
    badguy[0]= EndLat;
    badguy[1]=EndLon;

    jdoubleArray returnArray = makeDoubleArray(env, 2, badguy);

    if(returnArray == NULL)
        return NULL;
    else
        return returnArray;
}//end GetXYOffset

jdoubleArray
Java_com_gmeci_conversions_Conversions_AROffset(JNIEnv*  env,
                                                jobject  thiz,
                                                jdouble startlat,
                                                jdouble startlon,
                                                jdouble angle,
                                                jdouble range_m)
{
    bool  ans ;
    double EndLat;
    double EndLon;

    ans = ElevationConversions::AROffset((double)startlat,(double)startlon,&EndLat,&EndLon,(float)angle,(float)range_m);
    double badguy[2];
    badguy[0]= EndLat;
    badguy[1]=EndLon;

    jdoubleArray returnArray = makeDoubleArray(env, 2, badguy);

    if(returnArray == NULL)
        return NULL;
    else
        return returnArray;
}//end targetcalc

jdoubleArray
Java_com_gmeci_conversions_Conversions_AROffsetElev(JNIEnv*  env,
                                                jobject  thiz,
                                                jdouble startlat,
                                                jdouble startlon,
                                                jdouble hae_m,
                                                jdouble Azimuth,
                                                jdouble Range_m,
                                                jdouble ElevationAngle)
{
    bool ans ;
    
    double EndLat;
    double EndLon;
    float  Endhae_m;

    ans = ElevationConversions::AROffset((double)startlat,(double)startlon, (float)hae_m, &EndLat,&EndLon,&Endhae_m,(float)Azimuth, (float)Range_m, (float)ElevationAngle );

    double badguy[3];//={
    badguy[0]= EndLat;
    badguy[1]=EndLon;
    badguy[2]=(double)Endhae_m;

    //__android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Badguy 0 %f Badguy 1: %f Badguy2: %f", badguy[0], badguy[1], badguy[2] );

    jdoubleArray returnArray = makeDoubleArray(env, 3, badguy);


    if(returnArray == NULL)
        return NULL;
    else
        return returnArray;
}//end targetcalc

jdoubleArray
Java_com_gmeci_conversions_Conversions_CalculateRangeandAngle(JNIEnv*  env,
                                            jobject thiz,
                                            jdouble StartLat,
                                            jdouble StartLon,
                                            jdouble EndLat,
                                            jdouble EndLon)
{
    float Range_m;
    float Angle_deg;
    
    bool ans = ElevationConversions::CalculateRangeandAngle((double) StartLat, (double)StartLon,(double)EndLat, (double)EndLon, &Range_m, &Angle_deg);

    double badguy[2];
    badguy[0]= Range_m;
    badguy[1]= Angle_deg;

    jdoubleArray returnArray = makeDoubleArray(env, 2, badguy);
    if(returnArray == NULL)
        return NULL;
    else
        return returnArray;
}//end CalculateRangeandAngle

jdoubleArray
Java_com_gmeci_conversions_Conversions_CalculateRangeandAngleElev(JNIEnv*  env,
                                            jobject thiz,
                                            jdouble StartLat,
                                            jdouble StartLon,
                                            jdouble  Hae,
                                            jdouble EndLat,
                                            jdouble EndLon,
                                            jdouble  EndHae)
{
    float Range_m;
    float Angle_deg;
    float El_Angle_deg;

    bool ans = ElevationConversions::CalculateRangeandAngle((double) StartLat, (double)StartLon, (float)Hae, (double)EndLat, (double)EndLon, (float)EndHae, &Range_m, &Angle_deg, &El_Angle_deg);

    double badguy[3];
    badguy[0]= Range_m;
    badguy[1]= Angle_deg;
    badguy[2]= El_Angle_deg;

    jdoubleArray returnArray = makeDoubleArray(env, 3, badguy);
    if(returnArray == NULL)
        return NULL;
    else
        return returnArray;
}//end CalculateRangeandAngle

jdouble
Java_com_gmeci_conversions_Conversions_CalculateRangem(JNIEnv*  env,
                                            jobject  thiz,
                                            jdouble startlat,
                                            jdouble startlon,
                                            jdouble endlat,
                                            jdouble endlon)
{
    float ans =ElevationConversions::CalculateRange_m(startlat, startlon, endlat, endlon);
    return (jdouble)ans;
}//end calcrange

jdouble
Java_com_gmeci_conversions_Conversions_CalculateAngledeg(JNIEnv*  env,
                                                jobject  thiz,
                                                jdouble startlat,
                                                jdouble startlon,
                                                jdouble endlat,
                                                jdouble endlon)
{
    float ans;
    ans = ElevationConversions::CalculateAngle_deg((double)startlat, (double) startlon,
                                  (double) endlat,  (double) endlon);
    return (jdouble)ans;
}

jstring
Java_com_gmeci_conversions_Conversions_GetLatLonDMS(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble Lat,
                                                                jdouble Lon)
{
    std::string answer;
    answer = ElevationConversions::GetLatLonDMS((double)Lat, (double)Lon);
    jstring jword = env->NewStringUTF (answer.c_str());
    return jword;
}//end GetLatLonDMS

jstring
Java_com_gmeci_conversions_Conversions_GetLatDM(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble Lat)
{
    std::string answer;
    answer = ElevationConversions::GetLatDM((double)Lat);
    jstring jword = env->NewStringUTF (answer.c_str());
    return jword;
}//end GetLatDM

jstring
Java_com_gmeci_conversions_Conversions_GetLonDM(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble Lon)
{
    std::string answer;
    answer = ElevationConversions::GetLonDM((double)Lon);
    jstring jword = env->NewStringUTF (answer.c_str());
    return jword;
}//end GetLonDM

jdouble
Java_com_gmeci_conversions_Conversions_GetGeoidHeight(JNIEnv*  env,
                                                jobject  thiz,
                                                jdouble Lat,
                                                jdouble Lon)//MIKE changed to eliminate need for jdouble*
{
    bool create=true ;
    ElevationConversions temp(create);
    double DeltaHeight;

    long answer;
    answer = temp.Get_Geoid_Height((double)Lat, (double)Lon, &DeltaHeight);
    
    return (double)DeltaHeight;
}//end GetGeoidHeight

jdouble
Java_com_gmeci_conversions_Conversions_ConvertGeoidToEllipsoidHeight(JNIEnv*  env,
                                                                        jobject  thiz,
                                                                        jdouble Latitude,
                                                                        jdouble Longitude,
                                                                        jdouble Geoid_Height)
{

    bool create=true ;
    ElevationConversions temp(create);
    long ans;
    double height=0;
    jdouble Ellipsoid_Height;
    temp.Convert_Geoid_To_Ellipsoid_Height((double)Latitude,(double)Longitude,
                                    (double)Geoid_Height,&Ellipsoid_Height);//MIKE changed from &height to Ellipsoid_Height
    //jdouble result = height;
    return Ellipsoid_Height;
}//end ConvertGeoidtoEllipsoidHeight

jdouble
Java_com_gmeci_conversions_Conversions_ConvertEllipsoidToGeoidHeight(JNIEnv*  env,
                                                                        jobject  thiz,
                                                                        jdouble Latitude,
                                                                        jdouble Longitude,
                                                                        jdouble Ellipsoid_Height)
{

    bool create=true ;
    ElevationConversions temp(create);
    long ans;
    double height=0;
    temp.Convert_Ellipsoid_To_Geoid_Height((double)Latitude,(double)Longitude,
                                    (double)Ellipsoid_Height,&height);
    jdouble result = height;
    return result;
}//end ConvertGeoidtoEllipsoidHeight

jstring
Java_com_gmeci_conversions_Conversions_GetUTMHemisphereZone(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble Lat,
                                                                jdouble Lon)
{
    std::string answer;
    answer = ElevationConversions::GetUTMHemisphereZone((double)Lat, (double)Lon);

    jstring jword = env->NewStringUTF (answer.c_str());
    return jword;

}//end GetUTMHemisphereZone

jstring
Java_com_gmeci_conversions_Conversions_GetUTMEasting(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble Lat,
                                                                jdouble Lon)
{
    std::string answer;
    answer = ElevationConversions::GetUTMEasting((double)Lat, (double)Lon);
    jstring jword = env->NewStringUTF (answer.c_str());
    return jword;
}//end GetUTMEasting

jstring
Java_com_gmeci_conversions_Conversions_GetUTMNorthing(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble Lat,
                                                                jdouble Lon)
{
    std::string answer;
    answer = ElevationConversions::GetUTMNorthing((double)Lat, (double)Lon);
    jstring jword = env->NewStringUTF (answer.c_str());
    return jword;
}//end GetUTMNorthing

jstring
Java_com_gmeci_conversions_Conversions_GetMGRS(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble Lat,
                                                                jdouble Lon,
                                                                jint Digits)
{
    std::string answer;
    answer = ElevationConversions::GetMGRS((double)Lat, (double)Lon, (int)Digits);
    
    jstring jword = env->NewStringUTF (answer.c_str());
    return jword;

}//end GetMGRS

jstring
Java_com_gmeci_conversions_Conversions_getDatumString(JNIEnv*  env,
                                                                jobject  thiz)
{
    bool create=true;
    std::string answer;

    ElevationConversions temp(create);
    answer = temp.getDatumString();

    jstring jword = env->NewStringUTF (answer.c_str());
    return jword;

}//end getDatumString

jstring
Java_com_gmeci_conversions_Conversions_getSpheroidString(JNIEnv*  env,
                                                                jobject  thiz)
{
    bool create=true;
    std::string answer;

    ElevationConversions temp(create);
    answer = temp.getSpheroidString();

    jstring jword = env->NewStringUTF (answer.c_str());
    return jword;

}//end getSpheroidString

//static float GetMagAngle(float TrueAngle, double Lat, double Lon);

jdouble
Java_com_gmeci_conversions_Conversions_GetMagAngle(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble TrueAngle,
                                                                jdouble Lat,
                                                                jdouble Lon)
{
    bool create=true;
    ElevationConversions temp(create);
    return temp.GetMagAngle(TrueAngle, Lat, Lon);

}//end GetMagAngle

//static float GetTrueAngle(float MagAngle, double Lat, double Lon);
jdouble
Java_com_gmeci_conversions_Conversions_GetTrueAngle(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble MagAngle,
                                                                jdouble Lat,
                                                                jdouble Lon)
{
    bool create=true;
    ElevationConversions temp(create);
    return temp.GetTrueAngle(MagAngle, Lat, Lon);

}//end GetTrueAngle

jdouble
Java_com_gmeci_conversions_Conversions_GetDeclinationFromFile(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble Lat,
                                                                jdouble Lon)
{
    bool create=true;
    ElevationConversions temp(create);

    return temp.GetDeclination(Lat, Lon);

}


jdouble
Java_com_gmeci_conversions_Conversions_GetMGRSHeadingOffset(JNIEnv*  env,
                                                                jobject  thiz,
                                                                jdouble lat,
                                                                jdouble lon,
                                                                jdouble heading)
{
    bool create=true;
    ElevationConversions temp(create);

    return temp.GetMGRSHeadingOffset( (double)lat, (double)lon, (double)heading);

}//end GetMGRSHeadingOffset



}//END all End Extern C







