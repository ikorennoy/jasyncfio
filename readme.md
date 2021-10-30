# jasyncfio

jasyncfio provides an asynchronous file I/O API based on the Linux io_uring interface. At some point it was inspired by rust
glommio crate.

## jasyncfio Features

* Fully asynchronous io_uring based file I/O API
* API comes in two kinds: Buffered and Direct I/O
* API for linear access to files

## jasyncfio Buffered I/O

Buffered I/O means that it will be supported by the operating system's page cache, and you don't have to bother
with memory alignment.

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

Direct I/O means that it will not be backed by the operating system's page cache. So you will have to bother with memory
alignment, but it may give a positive performance effect under some loads.

TODO..