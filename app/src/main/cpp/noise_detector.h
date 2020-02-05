//
// Created by DSJ on 2019/06/26.
//

#ifndef NNRAWLOGGER_NOISE_DETECTOR_H
#define NNRAWLOGGER_NOISE_DETECTOR_H

#include <jni.h>
#include <vector>
#include <string>
#include <iostream>
#include <math.h>
#include <android/log.h>

/* Header for class HelloWorldJNI */

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     HelloWorldJNI
 * Method:    helloWorld
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jboolean JNICALL
Java_com_jam_dentsu_noupathyproto_neuroNicleService_judgeNoiseC(
        JNIEnv *env,
        jobject /* this */,
        jint raw1,
        jint raw2);

jint hold_noise_c1 = 0;
jint hold_noise_c2 = 0;
std::vector<int> rawArray1;
std::vector<int> rawArray2;
jboolean noise_detected_c = false;

#ifdef __cplusplus
}
#endif


#endif //NNRAWLOGGER_NOISE_DETECTOR_H
