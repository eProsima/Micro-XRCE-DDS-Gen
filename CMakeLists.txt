cmake_minimum_required(VERSION 2.8.11)

project("micrortpsgen")

include(${PROJECT_SOURCE_DIR}/cmake/dev/check_configuration.cmake)

###############
# Find packges.
###############
find_package(Java COMPONENTS Runtime REQUIRED)
find_package(PythonInterp 3)

######################
# Get Windows version.
######################
if(MSVC OR MSVC_IDE)
    check_msvc_arch()
endif()

enable_testing()
add_subdirectory(test)
