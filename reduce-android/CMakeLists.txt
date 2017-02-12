cmake_minimum_required(VERSION 2.8.12) # version provided by Ubuntu 14.04

project(reduce C)

if(((NOT CMAKE_BUILD_TYPE) OR (CMAKE_BUILD_TYPE STREQUAL "Debug")) AND (NOT CMAKE_GENERATOR STREQUAL "Xcode"))
    set(install_prefix_suffix "-debug")
else()
    set(install_prefix_suffix "")
endif()
set(
    CMAKE_INSTALL_PREFIX
        "${CMAKE_CURRENT_BINARY_DIR}/../bin-reduce${install_prefix_suffix}"
    CACHE PATH "destination for make install"
)

if(NOT ("${CMAKE_VERSION}" VERSION_LESS "3.1"))
    set(CMAKE_INSTALL_MESSAGE LAZY
        CACHE STRING "Show messages during install? Lazy means only changed.")
    set_property(CACHE CMAKE_INSTALL_MESSAGE
        PROPERTY STRINGS "ALWAYS" "LAZY" "NEVER")
endif()

set(CMAKE_MODULE_PATH "${CMAKE_MODULE_PATH}" "${CMAKE_CURRENT_LIST_DIR}/../compilescripts/cmake-modules")
include(CompilerSettings)
include(KulloCommon)
include(CrosscompileTargetProcessorType)

add_subdirectory(native)
