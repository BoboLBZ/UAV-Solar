/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

// This file binds the native image utility code to the Java class
// which exposes them.

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

#include "rgb2yuv.h"
#include "yuv2rgb.h"

static inline void WriteYUV(const int x, const int y, const int width,
                            const int r8, const int g8, const int b8,
                            uint8_t* const pY, uint8_t* const pUV) {
    // Using formulas from http://msdn.microsoft.com/en-us/library/ms893078
    *pY = ((66 * r8 + 129 * g8 + 25 * b8 + 128) >> 8) + 16;

    // Odd widths get rounded up so that UV blocks on the side don't get cut off.
    const int blocks_per_row = (width + 1) / 2;

    // 2 bytes per UV block
    const int offset = 2 * (((y / 2) * blocks_per_row + (x / 2)));

    // U and V are the average values of all 4 pixels in the block.
    if (!(x & 1) && !(y & 1)) {
        // Explicitly clear the block if this is the first pixel in it.
        pUV[offset] = 0;
        pUV[offset + 1] = 0;
    }

    // V (with divide by 4 factored in)
#ifdef __APPLE__
    const int u_offset = 0;
  const int v_offset = 1;
#else
    const int u_offset = 1;
    const int v_offset = 0;
#endif
    pUV[offset + v_offset] += ((112 * r8 - 94 * g8 - 18 * b8 + 128) >> 10) + 32;

    // U (with divide by 4 factored in)
    pUV[offset + u_offset] += ((-38 * r8 - 74 * g8 + 112 * b8 + 128) >> 10) + 32;
}

void ConvertARGB8888ToYUV420SP(const uint32_t* const input,
                               uint8_t* const output, int width, int height) {
    uint8_t* pY = output;
    uint8_t* pUV = output + (width * height);
    const uint32_t* in = input;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            const uint32_t rgb = *in++;
#ifdef __APPLE__
            const int nB = (rgb >> 8) & 0xFF;
      const int nG = (rgb >> 16) & 0xFF;
      const int nR = (rgb >> 24) & 0xFF;
#else
            const int nR = (rgb >> 16) & 0xFF;
            const int nG = (rgb >> 8) & 0xFF;
            const int nB = rgb & 0xFF;
#endif
            WriteYUV(x, y, width, nR, nG, nB, pY++, pUV);
        }
    }
}

void ConvertRGB565ToYUV420SP(const uint16_t* const input, uint8_t* const output,
                             const int width, const int height) {
    uint8_t* pY = output;
    uint8_t* pUV = output + (width * height);
    const uint16_t* in = input;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            const uint32_t rgb = *in++;

            const int r5 = ((rgb >> 11) & 0x1F);
            const int g6 = ((rgb >> 5) & 0x3F);
            const int b5 = (rgb & 0x1F);

            // Shift left, then fill in the empty low bits with a copy of the high
            // bits so we can stretch across the entire 0 - 255 range.
            const int r8 = r5 << 3 | r5 >> 2;
            const int g8 = g6 << 2 | g6 >> 4;
            const int b8 = b5 << 3 | b5 >> 2;

            WriteYUV(x, y, width, r8, g8, b8, pY++, pUV);
        }
    }
}

#ifndef MAX
#define MAX(a, b) ({__typeof__(a) _a = (a); __typeof__(b) _b = (b); _a > _b ? _a : _b; })
#define MIN(a, b) ({__typeof__(a) _a = (a); __typeof__(b) _b = (b); _a < _b ? _a : _b; })
#endif

// This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
// are normalized to eight bits.
static const int kMaxChannelValue = 262143;

static inline uint32_t YUV2RGB(int nY, int nU, int nV) {
    nY -= 16;
    nU -= 128;
    nV -= 128;
    if (nY < 0) nY = 0;

    // This is the floating point equivalent. We do the conversion in integer
    // because some Android devices do not have floating point in hardware.
    // nR = (int)(1.164 * nY + 2.018 * nU);
    // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
    // nB = (int)(1.164 * nY + 1.596 * nV);

    int nR = 1192 * nY + 1634 * nV;
    int nG = 1192 * nY - 833 * nV - 400 * nU;
    int nB = 1192 * nY + 2066 * nU;

    nR = MIN(kMaxChannelValue, MAX(0, nR));
    nG = MIN(kMaxChannelValue, MAX(0, nG));
    nB = MIN(kMaxChannelValue, MAX(0, nB));

    nR = (nR >> 10) & 0xff;
    nG = (nG >> 10) & 0xff;
    nB = (nB >> 10) & 0xff;

    return 0xff000000 | (nR << 16) | (nG << 8) | nB;
}

//  Accepts a YUV 4:2:0 image with a plane of 8 bit Y samples followed by
//  separate u and v planes with arbitrary row and column strides,
//  containing 8 bit 2x2 subsampled chroma samples.
//  Converts to a packed ARGB 32 bit output of the same pixel dimensions.
void ConvertYUV420ToARGB8888(const uint8_t* const yData,
                             const uint8_t* const uData,
                             const uint8_t* const vData, uint32_t* const output,
                             const int width, const int height,
                             const int y_row_stride, const int uv_row_stride,
                             const int uv_pixel_stride) {
    uint32_t* out = output;

    for (int y = 0; y < height; y++) {
        const uint8_t* pY = yData + y_row_stride * y;

        const int uv_row_start = uv_row_stride * (y >> 1);
        const uint8_t* pU = uData + uv_row_start;
        const uint8_t* pV = vData + uv_row_start;

        for (int x = 0; x < width; x++) {
            const int uv_offset = (x >> 1) * uv_pixel_stride;
            *out++ = YUV2RGB(pY[x], pU[uv_offset], pV[uv_offset]);
        }
    }
}

//  Accepts a YUV 4:2:0 image with a plane of 8 bit Y samples followed by an
//  interleaved U/V plane containing 8 bit 2x2 subsampled chroma samples,
//  except the interleave order of U and V is reversed. Converts to a packed
//  ARGB 32 bit output of the same pixel dimensions.
void ConvertYUV420SPToARGB8888(const uint8_t* const yData,
                               const uint8_t* const uvData,
                               uint32_t* const output, const int width,
                               const int height) {
    const uint8_t* pY = yData;
    const uint8_t* pUV = uvData;
    uint32_t* out = output;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int nY = *pY++;
            int offset = (y >> 1) * width + 2 * (x >> 1);
#ifdef __APPLE__
            int nU = pUV[offset];
      int nV = pUV[offset + 1];
#else
            int nV = pUV[offset];
            int nU = pUV[offset + 1];
#endif

            *out++ = YUV2RGB(nY, nU, nV);
        }
    }
}

// The same as above, but downsamples each dimension to half size.
void ConvertYUV420SPToARGB8888HalfSize(const uint8_t* const input,
                                       uint32_t* const output, int width,
                                       int height) {
    const uint8_t* pY = input;
    const uint8_t* pUV = input + (width * height);
    uint32_t* out = output;
    int stride = width;
    width >>= 1;
    height >>= 1;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int nY = (pY[0] + pY[1] + pY[stride] + pY[stride + 1]) >> 2;
            pY += 2;
#ifdef __APPLE__
            int nU = *pUV++;
      int nV = *pUV++;
#else
            int nV = *pUV++;
            int nU = *pUV++;
#endif

            *out++ = YUV2RGB(nY, nU, nV);
        }
        pY += stride;
    }
}

//  Accepts a YUV 4:2:0 image with a plane of 8 bit Y samples followed by an
//  interleaved U/V plane containing 8 bit 2x2 subsampled chroma samples,
//  except the interleave order of U and V is reversed. Converts to a packed
//  RGB 565 bit output of the same pixel dimensions.
void ConvertYUV420SPToRGB565(const uint8_t* const input, uint16_t* const output,
                             const int width, const int height) {
    const uint8_t* pY = input;
    const uint8_t* pUV = input + (width * height);
    uint16_t* out = output;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int nY = *pY++;
            int offset = (y >> 1) * width + 2 * (x >> 1);
#ifdef __APPLE__
            int nU = pUV[offset];
      int nV = pUV[offset + 1];
#else
            int nV = pUV[offset];
            int nU = pUV[offset + 1];
#endif

            nY -= 16;
            nU -= 128;
            nV -= 128;
            if (nY < 0) nY = 0;

            // This is the floating point equivalent. We do the conversion in integer
            // because some Android devices do not have floating point in hardware.
            // nR = (int)(1.164 * nY + 2.018 * nU);
            // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
            // nB = (int)(1.164 * nY + 1.596 * nV);

            int nR = 1192 * nY + 1634 * nV;
            int nG = 1192 * nY - 833 * nV - 400 * nU;
            int nB = 1192 * nY + 2066 * nU;

            nR = MIN(kMaxChannelValue, MAX(0, nR));
            nG = MIN(kMaxChannelValue, MAX(0, nG));
            nB = MIN(kMaxChannelValue, MAX(0, nB));

            // Shift more than for ARGB8888 and apply appropriate bitmask.
            nR = (nR >> 13) & 0x1f;
            nG = (nG >> 12) & 0x3f;
            nB = (nB >> 13) & 0x1f;

            // R is high 5 bits, G is middle 6 bits, and B is low 5 bits.
            *out++ = (nR << 11) | (nG << 5) | nB;
        }
    }
}

#define IMAGEUTILS_METHOD(METHOD_NAME) \
  Java_com_hitices_autopatrol_tfObjectDetection_ImageUtils_##METHOD_NAME  // NOLINT

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
IMAGEUTILS_METHOD(convertYUV420SPToARGB8888)(
    JNIEnv* env, jclass clazz, jbyteArray input, jintArray output,
    jint width, jint height, jboolean halfSize);

JNIEXPORT void JNICALL IMAGEUTILS_METHOD(convertYUV420ToARGB8888)(
    JNIEnv* env, jclass clazz, jbyteArray y, jbyteArray u, jbyteArray v,
    jintArray output, jint width, jint height, jint y_row_stride,
    jint uv_row_stride, jint uv_pixel_stride, jboolean halfSize);

JNIEXPORT void JNICALL IMAGEUTILS_METHOD(convertYUV420SPToRGB565)(
    JNIEnv* env, jclass clazz, jbyteArray input, jbyteArray output, jint width,
    jint height);

JNIEXPORT void JNICALL
IMAGEUTILS_METHOD(convertARGB8888ToYUV420SP)(
    JNIEnv* env, jclass clazz, jintArray input, jbyteArray output,
    jint width, jint height);

JNIEXPORT void JNICALL
IMAGEUTILS_METHOD(convertRGB565ToYUV420SP)(
    JNIEnv* env, jclass clazz, jbyteArray input, jbyteArray output,
    jint width, jint height);

#ifdef __cplusplus
}
#endif

JNIEXPORT void JNICALL
IMAGEUTILS_METHOD(convertYUV420SPToARGB8888)(
    JNIEnv* env, jclass clazz, jbyteArray input, jintArray output,
    jint width, jint height, jboolean halfSize) {
  jboolean inputCopy = JNI_FALSE;
  jbyte* const i = env->GetByteArrayElements(input, &inputCopy);

  jboolean outputCopy = JNI_FALSE;
  jint* const o = env->GetIntArrayElements(output, &outputCopy);

  if (halfSize) {
    ConvertYUV420SPToARGB8888HalfSize(reinterpret_cast<uint8_t*>(i),
                                      reinterpret_cast<uint32_t*>(o), width,
                                      height);
  } else {
    ConvertYUV420SPToARGB8888(reinterpret_cast<uint8_t*>(i),
                              reinterpret_cast<uint8_t*>(i) + width * height,
                              reinterpret_cast<uint32_t*>(o), width, height);
  }

  env->ReleaseByteArrayElements(input, i, JNI_ABORT);
  env->ReleaseIntArrayElements(output, o, 0);
}

JNIEXPORT void JNICALL IMAGEUTILS_METHOD(convertYUV420ToARGB8888)(
    JNIEnv* env, jclass clazz, jbyteArray y, jbyteArray u, jbyteArray v,
    jintArray output, jint width, jint height, jint y_row_stride,
    jint uv_row_stride, jint uv_pixel_stride, jboolean halfSize) {
  jboolean inputCopy = JNI_FALSE;
  jbyte* const y_buff = env->GetByteArrayElements(y, &inputCopy);
  jboolean outputCopy = JNI_FALSE;
  jint* const o = env->GetIntArrayElements(output, &outputCopy);

  if (halfSize) {
    ConvertYUV420SPToARGB8888HalfSize(reinterpret_cast<uint8_t*>(y_buff),
                                      reinterpret_cast<uint32_t*>(o), width,
                                      height);
  } else {
    jbyte* const u_buff = env->GetByteArrayElements(u, &inputCopy);
    jbyte* const v_buff = env->GetByteArrayElements(v, &inputCopy);

    ConvertYUV420ToARGB8888(
        reinterpret_cast<uint8_t*>(y_buff), reinterpret_cast<uint8_t*>(u_buff),
        reinterpret_cast<uint8_t*>(v_buff), reinterpret_cast<uint32_t*>(o),
        width, height, y_row_stride, uv_row_stride, uv_pixel_stride);

    env->ReleaseByteArrayElements(u, u_buff, JNI_ABORT);
    env->ReleaseByteArrayElements(v, v_buff, JNI_ABORT);
  }

  env->ReleaseByteArrayElements(y, y_buff, JNI_ABORT);
  env->ReleaseIntArrayElements(output, o, 0);
}

JNIEXPORT void JNICALL IMAGEUTILS_METHOD(convertYUV420SPToRGB565)(
    JNIEnv* env, jclass clazz, jbyteArray input, jbyteArray output, jint width,
    jint height) {
  jboolean inputCopy = JNI_FALSE;
  jbyte* const i = env->GetByteArrayElements(input, &inputCopy);

  jboolean outputCopy = JNI_FALSE;
  jbyte* const o = env->GetByteArrayElements(output, &outputCopy);

  ConvertYUV420SPToRGB565(reinterpret_cast<uint8_t*>(i),
                          reinterpret_cast<uint16_t*>(o), width, height);

  env->ReleaseByteArrayElements(input, i, JNI_ABORT);
  env->ReleaseByteArrayElements(output, o, 0);
}

JNIEXPORT void JNICALL
IMAGEUTILS_METHOD(convertARGB8888ToYUV420SP)(
    JNIEnv* env, jclass clazz, jintArray input, jbyteArray output,
    jint width, jint height) {
  jboolean inputCopy = JNI_FALSE;
  jint* const i = env->GetIntArrayElements(input, &inputCopy);

  jboolean outputCopy = JNI_FALSE;
  jbyte* const o = env->GetByteArrayElements(output, &outputCopy);

  ConvertARGB8888ToYUV420SP(reinterpret_cast<uint32_t*>(i),
                            reinterpret_cast<uint8_t*>(o), width, height);

  env->ReleaseIntArrayElements(input, i, JNI_ABORT);
  env->ReleaseByteArrayElements(output, o, 0);
}

JNIEXPORT void JNICALL
IMAGEUTILS_METHOD(convertRGB565ToYUV420SP)(
    JNIEnv* env, jclass clazz, jbyteArray input, jbyteArray output,
    jint width, jint height) {
  jboolean inputCopy = JNI_FALSE;
  jbyte* const i = env->GetByteArrayElements(input, &inputCopy);

  jboolean outputCopy = JNI_FALSE;
  jbyte* const o = env->GetByteArrayElements(output, &outputCopy);

  ConvertRGB565ToYUV420SP(reinterpret_cast<uint16_t*>(i),
                          reinterpret_cast<uint8_t*>(o), width, height);

  env->ReleaseByteArrayElements(input, i, JNI_ABORT);
  env->ReleaseByteArrayElements(output, o, 0);
}
