//
// Created by DSJ on 2019/06/26.
//

#include <jni.h>
#include <numeric>
#include <limits>
#include "noise_detector.h"

std::vector<double> calcDFT(std::vector<double> rawArray){
    std::vector<double> power;
    int N=rawArray.size();
    double DFT_re[N];
    double DFT_im[N];
    double SRC_re[N];
    double SRC_im[N];

    for(int i=0;i<N;i++){
        SRC_re[i] = rawArray[i];
        SRC_im[i] = 0.0;
    }

    for (int k=0;k<N;k++){
        DFT_re[k] = 0;
        DFT_im[k] = 0;
        for(int n=0;n < N; n++){
            DFT_re[k] += SRC_re[n] * ( cos((2* M_PI/N)*k*n))
                         + SRC_im[n] * ( sin ((2*M_PI/N)*k*n));
            DFT_im[k] += SRC_re[n] * ( -sin((2* M_PI/N)*k*n))
                         + SRC_im[n] * ( cos ((2*M_PI/N)*k*n));
        }
    }

    for(int i=0;i<N;i++){
        power.push_back((sqrt(DFT_re[i]*DFT_re[i] + DFT_im[i]*DFT_im[i])/(N/2)));
    }

    return power;
}

std::vector<double> minMaxScaler(std::vector<int> rawArray){
    std::vector<double> scaled_x;
    int N=rawArray.size();

    double min = (double)*std::min_element(rawArray.begin(), rawArray.end());
    double max = (double)*std::max_element(rawArray.begin(), rawArray.end());

    for(int i=0;i<N;i++) {
        scaled_x.push_back( ((double)rawArray[i]-min) / (max-min) );
    }

    return scaled_x;
}

double sqr(double v) {
    return v * v;
}

bool asymmetryDetector(std::vector<int> ch1, std::vector<int> ch2){

    double sumx = 0.0, sumy = 0.0;
    int n = ch1.size();

    for (int i = 0; i < n; i++) {
        sumx += ch1[i];  sumy += ch2[i];
    }

    double meanx = sumx / n, meany = sumy / n;
    double sumxy = 0.0, sumxx = 0.0, sumyy = 0.0;
    for (int i = 0; i < n; i++) {
        sumxy += (ch1[i] - meanx) * (ch2[i] - meany);
        sumxx += sqr(ch1[i] - meanx);
        sumyy += sqr(ch2[i] - meany);
    }

    double r = sumxy / sqrt(sumxx * sumyy);


    __android_log_print(ANDROID_LOG_DEBUG, "Corr", "%f",
                        r);

    if(r>0.4){
        return false;
    }
    else{
        return true;
    }
}

bool mainDetector(std::vector<int> rawArray){
    double beta = 0;
    double hist = 0;
    double var = 0;
    double low = 0;
    double high = 0;
    double mid = 0;

    //beta波の計算
    std::vector<double> scaled_x = minMaxScaler(rawArray1);
    std::vector<double> amp = calcDFT(scaled_x);
    for(int i=20;i<30;i++){
        beta += amp[i];
    }

    //回帰のRMSEを求める
    double a = (scaled_x[249] - scaled_x[0])/250.0;
    double b = scaled_x[0];
    for(int i=0;i<250;i++) {
        var += pow(a * i + b - scaled_x[i], 2);
    }

    //histを求める
    for(int i = 0; i<250; i++) {
        if (scaled_x[i] < 0.3) {
            low += 1;
        } else if (scaled_x[i] > 0.7) {
            high += 1;
        } else {
            mid += 1;
        }
    }


    if(mid != 0) {
        hist = (low+high)/mid;
    }
    else{
        return true;
    }

//    __android_log_print(ANDROID_LOG_DEBUG,"Noise", "var:%f, beta:%f, hist:%f",var, beta, hist);

    if(isnan(var) || isnan(beta)) {
        return true;
    }

    if(var<0.5){
        return true;
    }
    else{
        if(hist > 3.0){
            if(beta>0.5){
                return false;
            }
            else{
                return true;
            }
        }
        else{
            return false;
        }
    }
}

extern "C"
{
JNIEXPORT jboolean JNICALL
    Java_com_jam_dentsu_noupathyproto_neuroNicleService_judgeNoiseC(
            JNIEnv *env,
            jobject /* this */,
            jint raw1,
            jint raw2) {
        try {
            if (rawArray1.size() < 250) {
                rawArray1.push_back(raw1);
                rawArray2.push_back(raw2);
            } else {
//                bool result1 = mainDetector(rawArray1);
//                bool result2 = mainDetector(rawArray2);
//
//                if (result1) {
//                    hold_noise_c1 += 1;
//                } else {
//                    hold_noise_c1 = 0;
//                }
//                if (result2) {
//                    hold_noise_c2 += 1;
//                } else {
//                    hold_noise_c2 = 0;
//                }

                if (asymmetryDetector(rawArray1, rawArray2)) {
                    hold_noise_c1 += 1;
                    hold_noise_c2 += 1;
                }
                else{
                    hold_noise_c1 = 0;
                    hold_noise_c2 = 0;
                }

                __android_log_print(ANDROID_LOG_DEBUG, "Noise", "hold_noise1:%d, hold_noise2:%d",
                                    hold_noise_c1, hold_noise_c2);


                if (hold_noise_c1 >= 3 || hold_noise_c2 >= 3) {
                    noise_detected_c = true;
                }
                if (hold_noise_c1 == 0 && hold_noise_c2 == 0) {
                    noise_detected_c = false;
                }

                rawArray1.clear();
                rawArray2.clear();
            }
        }
        catch(char* str){
            noise_detected_c = true;
        }

        return noise_detected_c;
    }
}

