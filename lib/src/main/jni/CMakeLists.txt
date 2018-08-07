cmake_minimum_required(VERSION 3.4.1)

add_library(native-filesystem-functions SHARED
            native.cpp)

# Include libraries needed for lib
target_link_libraries(native-filesystem-functions
                            android
                            log)
