/**
 * The MIT License
 * Copyright © 2018 Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
#include <jni.h>
#include <linux/can/bcm.h>
#include <stddef.h>

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getHeaderSize(JNIEnv *env, jclass class) {
    return sizeof(struct bcm_msg_head);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getOffsetFlags(JNIEnv *env, jclass class) {
    return offsetof(struct bcm_msg_head, flags);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getOffsetCount(JNIEnv *env, jclass class) {
    return offsetof(struct bcm_msg_head, count);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getOffsetIval1Sec(JNIEnv *env, jclass class) {
    return offsetof(struct bcm_msg_head, ival1) + offsetof(struct bcm_timeval, tv_sec);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getOffsetIval1Usec(JNIEnv *env, jclass class) {
    return offsetof(struct bcm_msg_head, ival1) + offsetof(struct bcm_timeval, tv_usec);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getOffsetIval2Sec(JNIEnv *env, jclass class) {
    return offsetof(struct bcm_msg_head, ival2) + offsetof(struct bcm_timeval, tv_sec);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getOffsetIval2Usec(JNIEnv *env, jclass class) {
    return offsetof(struct bcm_msg_head, ival2) + offsetof(struct bcm_timeval, tv_usec);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getOffsetCanID(JNIEnv *env, jclass class) {
    return offsetof(struct bcm_msg_head, can_id);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getOffsetNFrames(JNIEnv *env, jclass class) {
    return offsetof(struct bcm_msg_head, nframes);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_BcmMessage_getOffsetFrames(JNIEnv *env, jclass class) {
    return offsetof(struct bcm_msg_head, frames);
}
