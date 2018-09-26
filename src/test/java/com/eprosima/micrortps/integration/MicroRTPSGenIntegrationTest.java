package com.eprosima.micrortps.integration;

import org.junit.Test;

import com.eprosima.integration.Command;
import com.eprosima.integration.IDL;
import com.eprosima.integration.TestManager;
import com.eprosima.integration.TestManager.TestLevel;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

public class MicroRTPSGenIntegrationTest
{
    private static final String INPUT_PATH = "thirdparty/IDL-Parser/test/idls";
    private static final String OUTPUT_PATH = "build/test/integration";

    @Test
    public void runTests()
    {
        //Configure Micro RTPS client for the tests
        ArrayList<String[]> commands = new ArrayList<String[]>();
        commands.add(new String[]{"mkdir -p " + OUTPUT_PATH, "."});
        commands.add(new String[]{"rm -rf micro-RTPS-client", OUTPUT_PATH});
        commands.add(new String[]{"git clone -b develop git@github.com:eProsima/micro-RTPS-client.git", OUTPUT_PATH});
        commands.add(new String[]{"mkdir build", OUTPUT_PATH + "/micro-RTPS-client"});
        commands.add(new String[]{"cmake .. -DTHIRDPARTY=ON -DCMAKE_INSTALL_PREFIX=install", OUTPUT_PATH + "/micro-RTPS-client/build"});
        commands.add(new String[]{"make install", OUTPUT_PATH + "/micro-RTPS-client/build"});

        for(String[] command: commands)
        {
            if(!Command.execute(command[0], command[1], true))
            {
                System.exit(-1);
            }
        }

        //Configure idl tests
        TestManager tests = new TestManager(TestLevel.RUN, "share/micrortps/micrortpsgen", INPUT_PATH, OUTPUT_PATH + "/idls");
        tests.addCMakeArguments("-DCMAKE_PREFIX_PATH=" + System.getProperty("user.dir") + "/" + OUTPUT_PATH + "/micro-RTPS-client/build/install");
        tests.removeTests(IDL.ARRAY_NESTED, IDL.SEQUENCE_NESTED);
        boolean testResult = tests.runTests();
        System.exit(testResult ? 0 : -1);
    }
}
