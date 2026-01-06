# Tsurugi Tiny Authentication Service

This project provides a simple JSON Web Tokens (JWT) based REST API server.

## Requirements

* JDK `>= 17`

**Note:** The build is configured with `release = 11` in Gradle, which means the compiled bytecode is compatible with Java 11 or later. While JDK 17 is required for the build environment, the resulting JAR files can run on Java 11 and above.

## How to build

```sh
./gradlew assemble
```

## How to test

```sh
./gradlew check
```
