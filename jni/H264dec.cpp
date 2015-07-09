
/**
 * @file
 * @brief Image loader and bitmap mask rendering
 */
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <jni.h>

#include <android/bitmap.h>

#include <android/log.h>
#define LOG_TAG    "JNI_TAG"
#define ALOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)  
#define ALOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)  

#ifdef __cplusplus
extern "C" {
#endif
#include "libavcodec/avcodec.h"

static struct file_descriptor_offsets_t
{
    jclass mClass;
    jfieldID mDescriptor;
} gFileDescriptorOffsets;

AVCodecContext *avctx;
AVFrame *frame;

//anti-Clockwise 
void YUV420P_rotation_90_negative(unsigned char *srcY, unsigned char *srcU, unsigned char *srcV,unsigned char *dst)
{
  unsigned char *dy, *du, *dv;
  int src_width = avctx->width;
  int src_height = avctx->height;
  int linesize = src_width + 32;
  int uvlinesize = linesize >> 1;

  dy = dst;
  du = dst + src_width * src_height;
  dv = du + src_width * src_height/4;
  
  ALOGE("src_width=%d,src_height=%d",src_width,src_height);
  int index = 0;
  int pos = src_width - 1;
  int mm, nn;
  for(mm = 0; mm < src_width; mm++) {
    for(nn = 0; nn < src_height; nn++) {
      dy[index] = srcY[nn * linesize + pos - mm];
      index++;
    }
  }
  
  index = 0;
  pos = src_width/2 - 1;
  for(mm = 0; mm < src_width/2; mm++) {
    for(nn = 0; nn < src_height/2; nn++) {
      du[index] = srcU[nn * uvlinesize + pos - mm];
      index++;
    }
  }
  
  index = 0;
  pos = src_width/2 - 1;
  for(mm = 0; mm < src_width/2; mm++) {
    for(nn = 0; nn < src_height/2; nn++) {
      dv[index] = srcV[nn * uvlinesize + pos - mm];
      index++;
    }
  }
}

//Clockwise 
void YUV420P_rotation_90(unsigned char *srcY, unsigned char *srcU, unsigned char *srcV,unsigned char *dst)
{
  unsigned char *dy, *du, *dv;
  int src_width = avctx->width;
  int src_height = avctx->height;
  int linesize = src_width + 32;
  int uvlinesize = linesize >> 1;

  dy = dst;
  du = dst + src_width * src_height;
  dv = du + src_width * src_height/4;
  
  ALOGE("src_width=%d,src_height=%d",src_width,src_height);
  int index = 0;
  int mm, nn;

  for(mm = 0; mm < src_width; mm++) {
    for(nn = src_height - 1; nn >=0; nn--) {
      dy[index] = srcY[nn * linesize + mm];
      index++;
    }
  }

  index = 0;
  for(mm = 0; mm < src_width/2; mm++) {
    for(nn = src_height/2-1; nn >= 0; nn--) {
      du[index] = srcU[nn * uvlinesize + mm];
      index++;
    }
  }
  
  index = 0;
  for(mm = 0; mm < src_width/2; mm++) {
    for(nn = src_height/2-1; nn >= 0; nn--) {
      dv[index] = srcV[nn * uvlinesize + mm];
      index++;
    }
  }
}



void YUV420P_to_RGB565_rotation(unsigned char *src, unsigned short *dst)
{
  int line, col, linewidth;
  int y, u, v, yy, vr, ug, vg, ub;
  int r, g, b;
  const unsigned char *py, *pu, *pv;
  int linesize, uvlinesize;

  unsigned char *srcY, *srcU, *srcV;
  int width = avctx->height;
  int height = avctx->width;
  srcY = src;
  srcU = src + width * height;
  srcV = srcU + width * height/4;
  
  linesize = width;
  uvlinesize = linesize >> 1;
  
  for (line = 0; line < height; line++) {
     py = srcY + line * linesize;
     pu = srcU + (line>>1) * uvlinesize;
     pv = srcV + (line>>1) * uvlinesize;
     for (col = 0; col < width; col++) {
	 y = *py++;
	 yy = y << 8;
	 
	 if ((col & 1) == 0) {
	     u = *pu - 128;
	     ug = 88 * u;
	     ub = 454 * u;
	     v = *pv - 128;
	     vg = 183 * v;
	     vr = 359 * v;
	     pu++;
	     pv++;
	 }
	 
	 r = (yy + vr) >> 8;
	 g = (yy - ug - vg) >> 8;
	 b = (yy + ub ) >> 8;
	 
	 if (r < 0) r = 0;
	 if (r > 255) r = 255;
	 if (g < 0) g = 0;
	 if (g > 255) g = 255;
	 if (b < 0) b = 0;
	 if (b > 255) b = 255;
	 *dst++ = (((unsigned short)r>>3)<<11) | (((unsigned short)g>>2)<<5) | (((unsigned short)b>>3)<<0); 
    } 
  } 
}

void YUV420P_to_RGB565(unsigned char *srcY, unsigned char *srcU, unsigned char *srcV,unsigned short *dst)
{
  int line, col, linewidth;
  int y, u, v, yy, vr, ug, vg, ub;
  int r, g, b;
  const unsigned char *py, *pu, *pv;
  int linesize, uvlinesize;

  int width = avctx->width;
  int height = avctx->height;
  linesize = width + 32;
  uvlinesize = linesize >> 1;
  
  for (line = 0; line < height; line++) {
     py = srcY + line * linesize;
     pu = srcU + (line>>1) * uvlinesize;
     pv = srcV + (line>>1) * uvlinesize;
     for (col = 0; col < width; col++) {
	 y = *py++;
	 yy = y << 8;
	 
	 if ((col & 1) == 0) {
	     u = *pu - 128;
	     ug = 88 * u;
	     ub = 454 * u;
	     v = *pv - 128;
	     vg = 183 * v;
	     vr = 359 * v;
	     pu++;
	     pv++;
	 }
	 
	 r = (yy + vr) >> 8;
	 g = (yy - ug - vg) >> 8;
	 b = (yy + ub ) >> 8;
	 
	 if (r < 0) r = 0;
	 if (r > 255) r = 255;
	 if (g < 0) g = 0;
	 if (g > 255) g = 255;
	 if (b < 0) b = 0;
	 if (b > 255) b = 255;
	 *dst++ = (((unsigned short)r>>3)<<11) | (((unsigned short)g>>2)<<5) | (((unsigned short)b>>3)<<0); 
    } 
  } 
}


JNIEXPORT jboolean JNICALL decodeInit(JNIEnv* env, jclass jclazz, jbyteArray nal, jint length) {
    jboolean jret = JNI_FALSE;
    int len;
    void *data;
    int decode_ok, bpl;
    AVPacket pkt;

    jbyte *readbuff = NULL;
    readbuff = (jbyte *)env->GetByteArrayElements(nal, NULL);

    if (readbuff == NULL) {
	return jret;
    }

    data = malloc(length + FF_INPUT_BUFFER_PADDING_SIZE);
    if (!data) {
        return jret;
    }

    memcpy(data, readbuff, length);
    avctx = avcodec_alloc_context3(NULL);
    frame = avcodec_alloc_frame();

    if (!(avctx && frame)) {
        free(frame);
        free(avctx);
        free(data);
        return jret;
    }

    avcodec_register_all();
    avcodec_open2(avctx, avcodec_find_decoder(CODEC_ID_H264), NULL);

    av_init_packet(&pkt);
    pkt.data = (unsigned char *)data;

    pkt.size = length;
    /* HACK: Make PNGs decode normally instead of as CorePNG delta frames. */
    pkt.flags = AV_PKT_FLAG_KEY;

    //decode header
    avcodec_decode_video2(avctx, frame, &decode_ok, &pkt);
    env->ReleaseByteArrayElements(nal, readbuff, 0);
    free(data);
    return JNI_TRUE;
}

void decode_frame(unsigned char *stream, unsigned short *rgbdst, int len, bool needRotation) {
    void *data;
    int decode_ok, bpl;
    AVPacket pkt;
    
    //decode frame
    av_init_packet(&pkt);
    
    pkt.data = stream;
    pkt.size = len;
    /* HACK: Make PNGs decode normally instead of as CorePNG delta frames. */
    pkt.flags = AV_PKT_FLAG_KEY;
    avcodec_decode_video2(avctx, frame, &decode_ok, &pkt);

    if(needRotation){
	unsigned char *rotation = (unsigned char *)malloc(avctx->width * avctx->width * 3 / 2);    
	//YUV420P_rotation_90_negative(frame->data[0],frame->data[1],frame->data[2], rotation);
	YUV420P_rotation_90(frame->data[0],frame->data[1],frame->data[2], rotation);
	YUV420P_to_RGB565_rotation(rotation, rgbdst);
	free(rotation);
    }else{
	YUV420P_to_RGB565(frame->data[0],frame->data[1],frame->data[2],rgbdst);
    }
}

JNIEXPORT jboolean JNICALL showFrameBitmap(JNIEnv* env, jclass jclazz, jobject bitmap, jbyteArray nal,jint len,jboolean needRotation) {
    int ret = 0;
    jboolean jret = JNI_FALSE;
    jbyte *readbuff = NULL;

    readbuff = (jbyte *)env->GetByteArrayElements(nal, NULL);
    if (readbuff == NULL) {
	return jret;
    }

    AndroidBitmapInfo bitmapInfo;
    void *pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo)) < 0) {
	ALOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
	return jret;
    }

    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGB_565) {
	ALOGE("Bitmap format is not RGB565");
	return jret;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
	ALOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
	return jret;
    }

    //read one RGB565 frame data for show
    unsigned short *rgbdst = (unsigned short  *)pixels;
    unsigned char *src = (unsigned char *)readbuff;
    decode_frame(src, rgbdst, len,needRotation);

    AndroidBitmap_unlockPixels(env, bitmap);    
    env->ReleaseByteArrayElements(nal, readbuff, 0);
    return JNI_TRUE;
}

static JNINativeMethod gMethods[] = {
    { "nativeDecodeInit", "([BI)Z",
            (void*) decodeInit },
    { "nativeShowFrameBitmap", "(Landroid/graphics/Bitmap;[BIZ)Z",
            (void*) showFrameBitmap },
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = (*env).FindClass(className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env).RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }

    clazz = env->FindClass("java/io/FileDescriptor");
    gFileDescriptorOffsets.mClass = (jclass) env->NewGlobalRef(clazz);
    gFileDescriptorOffsets.mDescriptor = env->GetFieldID(clazz, "descriptor", "I");

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 */
static int registerNatives(JNIEnv* env)
{
    if (!registerNativeMethods(env,
	    "cn/ingenic/glasssync/screen/screenshare/ScreenModule",
            gMethods, sizeof(gMethods) / sizeof(gMethods[0])))
        return JNI_FALSE;

    return JNI_TRUE;
}

/*
 * Set some test stuff up.
 *
 * Returns the JNI version on success, -1 on failure.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if ((*vm).GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        goto bail;
    }
    assert(env != NULL);

    if (!registerNatives(env)) {
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}

#ifdef __cplusplus
}
#endif

