### Adblock Uniffi bindings

A thin wrapper around the rust Adblock library using [uniffi]https://github.com/mozilla/uniffi-rs. The approach here is essentially to build our own rust library with function signatures/types that are easily exposed and used in kotlin.

### Why

The adblock provided project previously provided [ffi bindings] https://github.com/brave/adblock-rust-ffi.
These are no longer actively maintained and have been moved into brave-core and can't be used as is anymore.

The best solution is to write our own.

### How

There are few different components and places of integration.

1. the adblock library itself and the wrapper. This layer defines the the interface we'll see on the android side. This happens through through the udl file and proc macros in lib.rs.
2. Integration into android through JNA. 
    - Generation of the library files (*.so) and the generated kotlin interface/class. The library files are copied to a specific directory, `sudoadtrackerblocker/src/main/jniLibs`. The kotlin file is copied to the src directory like any other kotlin file.
    - Final integration with JNA is done by adding a dependency in build.gradle.
3. A final project level wrapper defined in `BlockingProvider` and `DefaultBlockingProvider`. This thin layer separates the adblock library from the project components and helps when making updates on the rust side. 

### Updating

For simplicity the wrapper functionality is declared in `lib.rs`. We deviate from the uniffi guides slightly by using [proc macro syntax]https://mozilla.github.io/uniffi-rs/proc_macro/index.html which leave the UDL file with only the required namespace declaration.

If changes are needed, the `FilterEngine` struct and impl are the first places to look. I had trouble with the proc macro and the `&str` type (rust holy war preffered type), so the wrapper functions use `String` type instead. This may change in a future update. 

### Requirements

- Install Rust
    - https://www.rust-lang.org/learn/get-started
    - `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`
    - Fish users `set -U fish_user_paths $HOME/.cargo/bin $fish_user_paths`
    - Verify toolchain, `cargo --version`

- Install NDK > 25 in Android Studio SDK Manager.
- Android architecutres installed, or use cross https://github.com/cross-rs/cross. 
    - `rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android`

### Build libary

run build script `buildFilterEngine.sh` in project root. 

### References

- [Rust]https://www.rust-lang.org
- [Adblock Rust]https://github.com/brave/adblock-rust
- [Uniffi Guide/Reference]https://mozilla.github.io/uniffi-rs/
- [Uniffi examples]https://github.com/mozilla/uniffi-rs/tree/main/examples have lots of examples of more complicated library wrappers and types/function signatures.
- [NDK]https://developer.android.com/studio/projects/install-ndk