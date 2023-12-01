package com.eprosima.microxrcedds.integration;

import org.junit.jupiter.api.Test;

import com.eprosima.integration.Command;
import com.eprosima.integration.IDL;
import com.eprosima.integration.TestManager;
import com.eprosima.integration.TestManager.TestLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

public class MicroXRCEDDSGenIntegrationTest
{
    private static final String INPUT_PATH = "thirdparty/IDL-Parser/test/idls";
    private static final String OUTPUT_PATH = "build/test/integration";

    private static boolean isUnix()
    {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }

    @Test
    public void runTests()
    {
        if(!isUnix())
        {
            System.out.println("WARNING: The tests are only available with an unix system");
            return;
        }

        // Get client's branch against test will compile.
        String branch = System.getProperty("branch");
        if(branch == null || branch.isEmpty())
        {
            branch = "master";
        }

        //Configure Micro XRCE-DDS client for the tests
        ArrayList<String[]> commands = new ArrayList<String[]>();
        commands.add(new String[]{"mkdir -p " + OUTPUT_PATH, "."});
        commands.add(new String[]{"rm -rf Micro-XRCE-DDS-Client", OUTPUT_PATH});
        commands.add(new String[]{"git clone -q -c advice.detachedHead=false -b " + branch + " https://github.com/eProsima/Micro-XRCE-DDS-Client.git", OUTPUT_PATH});
        commands.add(new String[]{"mkdir build", OUTPUT_PATH + "/Micro-XRCE-DDS-Client"});
        commands.add(new String[]{"cmake .. -DCMAKE_INSTALL_PREFIX=install", OUTPUT_PATH + "/Micro-XRCE-DDS-Client/build"});
        commands.add(new String[]{"make", OUTPUT_PATH + "/Micro-XRCE-DDS-Client/build"});
        commands.add(new String[]{"make install", OUTPUT_PATH + "/Micro-XRCE-DDS-Client/build"});

        for(String[] command: commands)
        {
            if(!Command.execute(command[0], command[1], true, false))
            {
                System.exit(-1);
            }
        }

        //Configure idl tests
        TestManager tests = new TestManager(TestLevel.RUN, "share/microxrcedds/microxrceddsgen", INPUT_PATH, OUTPUT_PATH + "/idls");
        tests.addCMakeArguments("-DCMAKE_PREFIX_PATH=" + System.getProperty("user.dir") + "/" + OUTPUT_PATH + "/Micro-XRCE-DDS-Client/build/install");
        tests.removeTests(
                IDL.ARRAY_NESTED,
                IDL.BITSET_BITMASK,
                IDL.COVERAGE_BASIC,
                IDL.COVERAGE_COMPLEX,
                IDL.FORWARD_DECLS,
                IDL.NEW_FEATURES_4_2,
                IDL.ARRAY_MULTI_DIM,
                IDL.SEQUENCE_BASIC_IN_MODULE_TYPEDEF,
                IDL.SEQUENCE_CUSTOM_IN_MODULE_TYPEDEF,
                IDL.SEQUENCE_STRING_IN_MODULE_TYPEDEF,
                IDL.SEQUENCE_NESTED
                );
        boolean testResult = tests.runTests();
        System.exit(testResult ? 0 : -1);
    }
}
