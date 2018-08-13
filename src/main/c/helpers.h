#ifndef _JAVACAN_HELPERS
#define _JAVACAN_HELPERS

#include <stdint.h>
#include <sys/time.h>
#include <stdbool.h>

#define MICROS_PER_SECOND 1000000

unsigned int interface_name_to_index(const char *);
int create_can_raw_socket();
int bind_can_socket(int, unsigned int);
void micros_to_timeval(struct timeval *, uint64_t);
int set_blocking_mode(int, bool);
int is_blocking(int);
int set_boolean_opt(int sock, int opt, bool enable);
int get_boolean_opt(int sock, int opt);

#endif