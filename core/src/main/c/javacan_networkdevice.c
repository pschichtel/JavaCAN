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
#include "common.h"
#include <jni.h>
#include <net/if.h>
#include <string.h>

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_platform_linux_LinuxNetworkDevice_resolveInterfaceName(JNIEnv *env, jclass class, jstring interface_name) {
    const char* ifname = (*env)->GetStringUTFChars(env, interface_name, NULL);
    if (ifname == NULL) {
        throw_native_exception(env, "failed to get c string from java string");
        return -1;
    }
    unsigned int ifindex = if_nametoindex(ifname);
    if (ifindex == 0) {
        const char* prefix = "Failed to resolve the interface: ";
        char message[strlen(prefix) + strlen(ifname) + 1];
        message[0] = 0;
        strcat((char *) &message, prefix);
        strcat((char *) &message, ifname);
        throw_native_exception(env, message);
    }
    (*env)->ReleaseStringUTFChars(env, interface_name, ifname);
    return ifindex;
}
