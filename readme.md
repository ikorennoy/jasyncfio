# Jasyncfio

[![Build](https://github.com/ikorennoy/jasyncfio/actions/workflows/build.yml/badge.svg)](https://github.com/ikorennoy/jasyncfio/actions/workflows/build.yml)

Jasyncfio provides an asynchronous file I/O API based on the Linux io_uring interface.

## Jasyncfio Features

* Fully asynchronous io_uring based file I/O API
* API comes in two kinds: Buffered and Direct I/O
* API for linear access to file (depends on your file system)
* Using a wide range of io_uring features such as polling, registered buffers/files

## Examples

```java
EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();

CompletableFuture<BufferedFile> bufferedFileCompletableFuture =
        eventExecutorGroup.openBufferedFile(filePath, OpenOption.CREATE, OpenOption.WRITE_ONLY);

BufferedFile bufferedFile = bufferedFileCompletableFuture.get();

ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
CompletableFuture<Integer> writeCompletableFuture = bufferedFile.write(0, buffer);
Integer writtenBytes = writeCompletableFuture.get();
```

If you want to dive deeper, there are more examples with explanations on the [wiki](https://github.com/ikorennoy/jasyncfio/wiki).

## Download 

Releases are available at [Maven Central](https://search.maven.org/artifact/one.jasyncfio/jasyncfio).

Since the library uses native code, it is necessary to specify the classifier. At the moment there are releases only for linux-amd64, there are plans to support linux-arm64.

### Maven

```xml
<dependency>
    <groupId>one.jasyncfio</groupId>
    <artifactId>jasyncfio</artifactId>
    <version>0.0.1</version>
    <classifier>linux-amd64</classifier>
</dependency>
```

### Gradle Groovy DSL

```groovy
implementation 'one.jasyncfio:jasyncfio:0.0.1:linux-amd64'
```


### Gradle Kotlin DSL

```kotlin
implementation("one.jasyncfio:jasyncfio:0.0.1:linux-amd64")
```

