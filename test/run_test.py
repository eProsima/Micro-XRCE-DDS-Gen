import os
import sys
import platform
from subprocess import check_call

def generate_code():
    # Get platform.
    example = None
    if platform.system() == 'Linux':
        if platform.architecture()[0] == '64bit':
            example = 'x64Linux2.6gcc'
        else:
            example = 'i86Linux2.6gcc'
    elif platform.system() == 'Windows':
        example = os.environ.get("CMAKE_MSVC_ARCH")

    if example == None:
        print("Operating system not supported by this test")
        sys.exit(1)

    # Get environment variables.
    env_vars = os.environ.copy()

    # Set path.
    project_source_dir = os.environ.get("CMAKE_PROJECT_SOURCE_DIR")
    exec_dir = project_source_dir + '/share/micrortps'
    idl_dir = project_source_dir + '/test'
    java_executable = os.environ.get("CMAKE_JAVA_EXECUTABLE")

    # Run build .jar.
    check_call([java_executable, '-jar', exec_dir + '/micrortpsgen.jar', idl_dir + '/test.idl', 
        '-replace', '-example', example], env=env_vars)

if __name__ == "__main__":
    generate_code()
