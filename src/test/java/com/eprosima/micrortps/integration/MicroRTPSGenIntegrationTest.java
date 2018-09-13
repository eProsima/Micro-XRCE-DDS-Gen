package com.eprosima.micrortps.integration;

import org.junit.Test;

import com.eprosima.integration.Command;
import com.eprosima.integration.IDL;
import com.eprosima.integration.TestManager;
import com.eprosima.integration.TestManager.TestLevel;

import javafx.util.Pair;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

public class MicroRTPSGenIntegrationTest
{
    private static final String OUTPUT_DIR = "build/test/integration";

    @Test
    public void runTests()
    {
        //Configure Micro RTPS client for the tests
        ArrayList<String[]> commands = new ArrayList<String[]>();
        commands.add(new String[]{"mkdir -p " + OUTPUT_DIR, "."});
        commands.add(new String[]{"git clone git@github.com:eProsima/micro-RTPS-client.git", OUTPUT_DIR});
        commands.add(new String[]{"mkdir build", OUTPUT_DIR + "/micro-RTPS-client"});
        commands.add(new String[]{"cmake .. -DTHIRDPARTY=ON -DCMAKE_INSTALL_PREFIX=install", OUTPUT_DIR + "/micro-RTPS-client/build"});
        commands.add(new String[]{"make install", OUTPUT_DIR + "/micro-RTPS-client/build"});

        for(String[] command: commands)
        {
            if(!Command.execute(command[0], command[1], true))
            {
                System.exit(-1);
            }
        }
          

        //Configure Micro RTPS client for the tests
        TestManager tests = new TestManager(TestLevel.RUN, "share/micrortps/micrortpsgen", OUTPUT_DIR + "/idls");
        tests.addCMakeArguments("-DEPROSIMA_BUILD=ON");
        tests.addCMakeArguments("-DCMAKE_PREFIX_PATH=" + System.getProperty("user.dir") + "/" + OUTPUT_DIR + "/micro-RTPS-client/build/install");
        tests.removeTests(IDL.ARRAY_NESTED, IDL.SEQUENCE_NESTED);
        boolean testResult = tests.runTests();
        System.exit(testResult ? 0 : -1);
    }
}
