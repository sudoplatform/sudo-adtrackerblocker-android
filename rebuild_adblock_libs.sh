#!/bin/bash
#
# A script to rebuild the adblock-rust-ffi libraries. Use this when the adblock-rust-ffi
# repository has changed and you want to update.
#
# Requires:
#
#  - rust installed (https://www.rust-lang.org/tools/install)
#
#  - rust targets installed (https://medium.com/visly/rust-on-android-19f34a2fb43)
#     rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
#
#  - Tell cargo about the toolchains from the Android NDK it should use in $HOME/.cargo/config, for example:
#
# [target.aarch64-linux-android]
# ar = "/Users/someuser/Library/Android/sdk/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android-ar"
# linker = "/Users/someuser/Library/Android/sdk/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android30-clang"
#
# [target.armv7-linux-androideabi]
# ar = "/Users/someuser/Library/Android/sdk/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/bin/arm-linux-androideabi-ar"
# linker = "/Users/someuser/Library/Android/sdk/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/bin/armv7a-linux-androideabi30-clang"
#
# [target.i686-linux-android]
# ar = "/Users/someuser/Library/Android/sdk/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/bin/i686-linux-android-ar"
# linker = "/Users/someuser/Library/Android/sdk/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/bin/i686-linux-android30-clang"
#
# [target.x86_64-linux-android]
# ar = "/Users/someuser/Library/Android/sdk/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/bin/x86_64-linux-android-ar"
# linker = "/Users/someuser/Library/Android/sdk/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/bin/x86_64-linux-android30-clang"
#
# 2020-11-24
#
export BUILD_DIR=build/adblock

clone_repo() {
  mkdir -p $BUILD_DIR
  git clone git@github.com:brave/adblock-rust-ffi.git $BUILD_DIR/adblock-rust-ffi
}

# Change the Cargo.toml file so that only the static library is built, otherwise the rust library
# crashes with a panic when used from Android JNI.
modify_cargo() {
  cp $BUILD_DIR/adblock-rust-ffi/Cargo.toml $BUILD_DIR/adblock-rust-ffi/Cargo.toml.sav
  sed 's/"cdylib",//;s/"rlib",//' $BUILD_DIR/adblock-rust-ffi/Cargo.toml.sav > $BUILD_DIR/adblock-rust-ffi/Cargo.toml
}

build_targets() {
  cargo build --manifest-path $BUILD_DIR/adblock-rust-ffi/Cargo.toml --target aarch64-linux-android --release
  cargo build --manifest-path $BUILD_DIR/adblock-rust-ffi/Cargo.toml --target armv7-linux-androideabi --release
  cargo build --manifest-path $BUILD_DIR/adblock-rust-ffi/Cargo.toml --target i686-linux-android --release
  cargo build --manifest-path $BUILD_DIR/adblock-rust-ffi/Cargo.toml --target x86_64-linux-android --release
}

copy_libs() {
  export TARGET=$BUILD_DIR/adblock-rust-ffi/target
  export JNI_DIR=sudoadtrackerblocker/jni

  mkdir $JNI_DIR/arm64-v8a/ $JNI_DIR/armeabi-v7a $JNI_DIR/x86 $JNI_DIR/x86_64

  cp -v $TARGET/aarch64-linux-android/release/libadblock.a $JNI_DIR/arm64-v8a/
  cp -v $TARGET/armv7-linux-androideabi/release/libadblock.a  $JNI_DIR/armeabi-v7a
  cp -v $TARGET/i686-linux-android/release/libadblock.a $JNI_DIR/x86
  cp -v $TARGET/x86_64-linux-android/release/libadblock.a $JNI_DIR/x86_64
}

cleanup() {
  rm -rf $BUILD_DIR
}

clone_repo
modify_cargo
build_targets
copy_libs
cleanup

exit 0

