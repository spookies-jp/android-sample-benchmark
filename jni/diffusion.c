#include <jni.h>

JNIEXPORT void JNICALL Java_jp_co_spookies_android_benchmark_Benchmark_00024DiffusionView_calculateJni
  (JNIEnv* env, jobject this)
{
    // classの取得
    jclass clazz = (*env)->GetObjectClass(env, this);

    // xSizeの取得
    jfieldID xSizeId = (*env)->GetFieldID(env, clazz, "xSize", "I");
    jint xSize = (*env)->GetIntField(env, this, xSizeId);

    // ySizeの取得
    jfieldID ySizeId = (*env)->GetFieldID(env, clazz, "ySize", "I");
    jint ySize = (*env)->GetIntField(env, this, ySizeId);

    jboolean b;

    // temperaturesの取得
    jfieldID temperaturesId = (*env)->GetFieldID(env, clazz, "temperatures", "[F");
    jobject temperaturesObj = (*env)->GetObjectField(env, this, temperaturesId);
    jfloat* temperatures = (*env)->GetFloatArrayElements(env, temperaturesObj, &b);

    // tmpの取得
    jfieldID tmpId = (*env)->GetFieldID(env, clazz, "tmp", "[F");
    jobject tmpObj = (*env)->GetObjectField(env, this, tmpId);
    jfloat* tmp = (*env)->GetFloatArrayElements(env, tmpObj, &b);

    // Dfudtdx2の取得
    jfieldID Dfudtdx2ID = (*env)->GetFieldID(env, clazz, "Dfudtdx2", "F");
    jfloat Dfudtdx2 = (*env)->GetFloatField(env, this, Dfudtdx2ID);

    int x, y;
    for(x = 1; x < xSize - 1; x++) {
        for(y = 1; y< ySize - 1; y++){
            int i = x + y * xSize;
            float t0 = temperatures[i];
            float t1 = temperatures[(x - 1) + y * xSize];
            float t2 = temperatures[(x + 1) + y * xSize];
            float t3 = temperatures[x + (y - 1) * xSize];
            float t4 = temperatures[x + (y + 1) * xSize];
            tmp[i] = t0 + (t1 + t2 + t3 + t4 - 4 * t0) * Dfudtdx2;
        }
    }
    int size = (*env)->GetArrayLength(env, temperaturesObj);
    int i;
    for(i = 0; i < size; i++){
        temperatures[i] = tmp[i];
    }
    (*env)->ReleaseFloatArrayElements(env, temperaturesObj, temperatures, 0);
    (*env)->ReleaseFloatArrayElements(env, tmpObj, tmp, 0);
    return;
}
