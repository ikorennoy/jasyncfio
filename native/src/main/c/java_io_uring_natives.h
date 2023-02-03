#include <stddef.h>
#include <stdint.h>

#ifndef JAVA_IO_URING_NATIVES_H
#define JAVA_IO_URING_NATIVES_H

struct io_uring_sq {
    unsigned *khead;
    unsigned *ktail;
    unsigned *kring_mask;
    unsigned *kring_entries;
    unsigned *kflags;
    unsigned *kdropped;
    unsigned *array;
    struct io_uring_sqe *sqes;

    size_t ring_sz;
    void *ring_ptr;
};

struct io_uring_cq {
    unsigned *khead;
    unsigned *ktail;
    unsigned *kring_mask;
    unsigned *kring_entries;
    unsigned *koverflow;
    struct io_uring_cqe *cqes;

    size_t ring_sz;
    void *ring_ptr;
};

struct io_uring {
    struct io_uring_sq sq;
    struct io_uring_cq cq;
    unsigned flags;
    int ring_fd;
};

#endif
