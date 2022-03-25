#include "io_uring.h"
#include <signal.h>

#ifndef IOURING_SYSCALL_H
#define IOURING_SYSCALL_H

extern int sys_io_uring_setup(unsigned entries, struct io_uring_params *p);
extern int sys_io_uring_enter(int fd, unsigned to_submit, unsigned min_complete,
                              unsigned flags, sigset_t *sig);
extern int sys_io_uring_register(unsigned int fd, unsigned int opcode, void *arg, unsigned int nr_args);
#endif
