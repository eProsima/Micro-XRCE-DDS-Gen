package test.com.eprosima.microxrcedds;

import org.junit.jupiter.api.Test;

import com.eprosima.integration.Command;

import com.eprosima.integration.TestManager;
import com.eprosima.integration.TestManager.TestLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

public class MicroXRCEDDSGenIntegrationTest
{
    private static final String INPUT_PATH = "thirdparty/IDL-Test-Types/IDL";
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

        String list_tests_str = System.getProperty("list_tests");
        java.util.List<String> list_tests = null;

        if (null != list_tests_str)
        {
            list_tests = java.util.Arrays.asList(list_tests_str.split(",", -1));
        }

        String blacklist_tests_str = System.getProperty("blacklist_tests");
        java.util.List<String> blacklist_tests = null;

        if (null != blacklist_tests_str)
        {
            blacklist_tests = java.util.Arrays.asList(blacklist_tests_str.split(",", -1));
        }

        //Configure idl tests
        TestManager tests = new TestManager(
                TestLevel.RUN,
                "share/microxrceddsgen/java/microxrceddsgen",
                INPUT_PATH,
                OUTPUT_PATH + "/idls",
                "CMake",
                list_tests,
                blacklist_tests);

        tests.addCMakeArguments("-DCMAKE_PREFIX_PATH=" + System.getProperty("user.dir") + "/" + OUTPUT_PATH + "/Micro-XRCE-DDS-Client/build/install");

        boolean testResult = tests.runTests();
        System.exit(testResult ? 0 : -1);
    }
}
