# Copyright 2016 Proyectos y Sistemas de Mantenimiento SL (eProsima).
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/bin/bash
# This scripts run the micrortpsgen tests.

errorstatus=0
globalerrorstatus=0
failedtests=""
correcttests=""


# This function execute a test in a directory.
# @param Plaform for Visual Studio.
# @param Name of the test
function execTest
{
	#Get the IDL files:
	IDLFILES=""
	for i in $(ls);do
    	if [[ $i =~ .*\.idl$ ]];then
    	    IDLFILES="$IDLFILES $i"
    	fi
	done
	echo "FOUND IDL FILES: $IDLFILES"

	#Generate Info
	../../../scripts/micrortpsgen -example -replace -write-access-profile $IDLFILES
	errorstatus=$?

    if [ $errorstatus != 0 ]; then return; fi

    #Compile
    mkdir build
    cd build
    cmake .. -DCMAKE_PREFIX_PATH=/home/luis/Repositories/eprosima/micrortps/client/build/install
    make
    errorstatus=$?
    cd ..

    #clean:
    if [ $2 == "nodelete" ]; then return; fi
    rm -R build
    rm *.c
    rm *.h
    rm CMakeLists.txt
}

if [ $# -ge 1 ]; then
    cd $1
    execTest $1 $2
    cd ..
    if [ $errorstatus == 0 ]; then
        echo "TEST $1 SUCCESSFULL"
    else
        echo "TEST $1 FAILED"
        globalerrorstatus=1
        failedtests="$failedtests $1"
    fi
else
    echo "EXECUTING ALL TESTS IN THIS DIRECTORY"
    for dir in $(find . -mindepth 1 -maxdepth 1 -path ./output -prune -o -path ./.svn -prune -o -type d -printf "%f\n"); do
        echo "EXECUTING IN DIRECTORY $dir"
        cd $dir
        execTest $1 "delete"
        cd ..
        if [ $errorstatus == 0 ]; then
            echo "TEST $dir SUCCESSFULL"
            correcttests="$correcttests $dir"
        else
            echo "TEST $dir FAILED"
            globalerrorstatus=1
            failedtests="$failedtests $dir"
        fi
    done
fi

if [ $globalerrorstatus == 0 ]; then
    echo "TEST SUCCESSFULL"
else
    echo "TESTS FAILED: $failedtests"
    echo "TESTS OK: $correcttests"
fi

exit $globalerrorstatus
