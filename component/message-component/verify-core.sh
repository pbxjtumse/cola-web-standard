#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/.verify-build"
SOURCE_LIST="$BUILD_DIR/sources.txt"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes"

find   "$ROOT_DIR/message-api/src/main/java"   "$ROOT_DIR/message-spi/src/main/java"   "$ROOT_DIR/message-core/src/main/java"   "$ROOT_DIR/message-testkit/src/main/java"   "$ROOT_DIR/message-demo/src/main/java"   -name '*.java' | sort > "$SOURCE_LIST"

javac --release 17 -Xlint:all -Werror -d "$BUILD_DIR/classes" @"$SOURCE_LIST"
java -cp "$BUILD_DIR/classes" com.xjtu.iron.message.demo.InMemoryMessageDemo
java -cp "$BUILD_DIR/classes" com.xjtu.iron.message.demo.MessageModelContractVerifier
