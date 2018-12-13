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
#ifndef _JAVACAN_HELPERS
#define _JAVACAN_HELPERS

#include <stdint.h>
#include <sys/time.h>
#include <stdbool.h>

#define MICROS_PER_SECOND 1000000

unsigned int interface_name_to_index(const char *);
int create_can_raw_socket();
int bind_can_socket(int, uint32_t, uint32_t, uint32_t);
void micros_to_timeval(struct timeval *, uint64_t);
int set_timeout(int, int, uint64_t);
int get_timeout(int, int, uint64_t*);
int set_blocking_mode(int, bool);
int is_blocking(int);
int set_boolean_opt(int sock, int opt, bool enable);
int get_boolean_opt(int sock, int opt);
short poll_single(int, short, int);

#endif