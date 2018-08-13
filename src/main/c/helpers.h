#ifndef _JAVACAN_HELPERS
#define _JAVACAN_HELPERS

#include <stdint.h>
#include <sys/time.h>
#include <stdbool.h>

#define MICROS_PER_SECOND 1000000

void clear_error();
int set_blocking_mode(int, bool);
void micros_to_timeval(struct timeval *, uint64_t);
unsigned int interface_name_to_index(const char *);
int bind_can_socket(int, unsigned int);
int create_can_raw_socket();

#endif