/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class glucp_JPGlucose */

#ifndef _Included_glucp_JPGlucose
#define _Included_glucp_JPGlucose
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     glucp_JPGlucose
 * Method:    ginit
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_glucp_JPGlucose_ginit
  (JNIEnv *, jclass);

/*
 * Class:     glucp_JPGlucose
 * Method:    gadd
 * Signature: (J[I)Z
 */
JNIEXPORT jboolean JNICALL Java_glucp_JPGlucose_gadd
  (JNIEnv *, jclass, jlong, jintArray);

/*
 * Class:     glucp_JPGlucose
 * Method:    gsat
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_glucp_JPGlucose_gsat
  (JNIEnv *, jclass, jlong);

/*
 * Class:     glucp_JPGlucose
 * Method:    gsat_time
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_glucp_JPGlucose_gsat_1time
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     glucp_JPGlucose
 * Method:    gderef
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_glucp_JPGlucose_gderef
  (JNIEnv *, jclass, jlong, jint);

#ifdef __cplusplus
}
#endif
#endif