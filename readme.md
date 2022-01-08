# jasyncfio

[![Build](https://github.com/ikorennoy/jasyncfio/actions/workflows/build.yml/badge.svg)](https://github.com/ikorennoy/jasyncfio/actions/workflows/build.yml)

jasyncfio provides an asynchronous file I/O API based on the Linux io_uring interface. To some extent, the API was inspired by Rust glommio crate.

## jasyncfio Features

* Fully asynchronous io_uring based file I/O API
* API comes in two kinds: Buffered and Direct I/O
* API for linear access to files

## jasyncfio Buffered I/O

Buffered I/O means that it will be supported by the operating system's page cache, and you don't have to worry
about memory alignment.

```java
CompletableFuture<BufferedFile> f = BufferedFile.create("/tmp/testFile");
// let's assume that our future is completed
BufferedFile file = f.get();
ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
CompletableFuture<Integer> readResult = file.read(0, byteBuffer);
// let's assume that our future is completed
Integer readBytes = readResult.get();
// so we have `readBytes` in out byteBuffer
```

## jasyncfio Direct I/O

Direct I/O means that it will not be backed by the operating system's page cache. So you will have to deal with memory
alignment, but it may give a positive performance effect under some workloads. Read more: 
[open man page](https://man7.org/linux/man-pages/man2/open.2.html) `NOTES` section `O_DIRECT`.

```java
CompletableFuture<DmaFile> f = DmaFile.create("/tmp/testFile");
// let's assume that our future is completed
DmaFile file = f.get();
ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(512, DmaFile.DEFAULT_ALIGNMENT);
CompletableFuture<Integer> readResult = file.read(0, 512, byteBuffer);
// let's assume that our future is completed
Integer readBytes = readResult.get();
// so we have `readBytes` in out byteBuffer
```
