cmake_minimum_required(VERSION 3.4.1)


# Declare this library adblockjni as a shared library
add_library( adblockjni
             SHARED
             src/main/cpp/AdBlockEngineJni.cpp )

# Import the adblock-rust-ffi shared library
add_library( adblock
             STATIC
             IMPORTED )

# Add the Android logging library
find_library( log-lib
              log )

# Tell CMake where to find the adblock-rust-ffi library
set_target_properties( adblock
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_CURRENT_SOURCE_DIR}/jni/${ANDROID_ABI}/libadblock.a )

# Tell CMake where to find the headers for the adblock-rust-ffi library
include_directories( libadblock/include )

# Tell CMake what to link to get adblockjni
target_link_libraries( adblockjni adblock ${log-lib} )
