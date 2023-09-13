#!/bin/bash

project_src_root=../sudoadtrackerblocker/src/main

generateLibrary() {
    # Build the library for the required android and build target.
    cargo ndk -t $android_target build --profile release --target=$build_target

    # move from the build directory to the app src directory. 
    # cargo -o option can move this but it's experimental, and we can't use it when generating kotlin bindings, there was no output when I tried.
    destination=$project_src_root/jniLibs/$android_target/
    mkdir -p $destination
    cp -f target/$build_target/release/libfilter_engine.so $destination
}

# move to the adblock directory to build
pushd uniffi_adblock

# define the build and android architecture targets and build. These are used as arguments to the build tools,
# and also used for the location of the output files.
build_target="aarch64-linux-android"
android_target="arm64-v8a"
generateLibrary

# Special case, the kotlin bindings are generated from one of the arch build outputs. It doesn't appear to matter
# which one.
#
# This command differs from the uniffi tutorial because we use the proc macros instead of adding definitions in the udl file.
# https://mozilla.github.io/uniffi-rs/tutorial/foreign_language_bindings.html#running-uniffi-bindgen-using-a-library-file
# 
# We also run bindgen from a locally built crate. see [why] https://mozilla.github.io/uniffi-rs/tutorial/foreign_language_bindings.html#creating-the-bindgen-binary
cargo run --bin uniffi-bindgen generate --library target/$build_target/release/libfilter_engine.so --language kotlin --out-dir $project_src_root/java/

# And build the rest of the archs.
build_target="armv7-linux-androideabi"
android_target="armeabi-v7a"
generateLibrary

build_target="i686-linux-android"
android_target="x86"
generateLibrary

build_target="x86_64-linux-android"
android_target="x86_64"
generateLibrary

# cleanup and go back to the original directory. Build artifacts use a lot of disk space.
rm -rf target
popd