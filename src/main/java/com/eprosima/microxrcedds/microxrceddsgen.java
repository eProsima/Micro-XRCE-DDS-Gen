
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.eprosima.microxrcedds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.jar.Manifest;

import com.eprosima.microxrcedds.exceptions.BadArgumentException;
import com.eprosima.microxrcedds.idl.grammar.Context;
import com.eprosima.solution.Project;
import com.eprosima.solution.Solution;
import com.eprosima.microxrcedds.util.Utils;
import com.eprosima.idl.generator.manager.TemplateGroup;
import com.eprosima.idl.generator.manager.TemplateManager;
import com.eprosima.idl.parser.grammar.IDLLexer;
import com.eprosima.idl.parser.grammar.IDLParser;
import com.eprosima.idl.parser.tree.Specification;
import com.eprosima.idl.parser.tree.AnnotationDeclaration;
import com.eprosima.idl.parser.tree.AnnotationMember;
import com.eprosima.idl.parser.typecode.PrimitiveTypeCode;
import com.eprosima.idl.parser.typecode.Kind;
import com.eprosima.idl.util.Util;
import com.eprosima.log.ColorMessage;

import org.stringtemplate.v4.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class microxrceddsgen {

    private Vector<String> m_idlFiles;
    protected static String m_appEnv = "MICROXRCEDDSHOME";
    private boolean m_case_sensitive = false;
    private boolean m_exampleOption = false;
    private boolean m_ppDisable = false; //TODO
    private boolean m_replace = false;
    private String m_ppPath = null;
    private final String m_defaultOutputDir = "." + File.separator;
    private String m_outputDir = m_defaultOutputDir;
    private String m_tempDir = null;
    protected static String m_appName = "microxrceddsgen";
    protected boolean m_test = false;
    protected String m_defaultContainerPreallocSize = "50";

    protected static String m_localAppProduct = "microxrcedds";
    private ArrayList<String> m_includePaths = new ArrayList<String>();

    private String m_os = null;

    public microxrceddsgen(String [] args) throws BadArgumentException {

        int count = 0;
        String arg;

        // Detect OS
        m_os = System.getProperty("os.name");

        m_idlFiles = new Vector<String>();

        // Check arguments
        while (count < args.length) {

            arg = args[count++];

            if (!arg.startsWith("-")) {
                m_idlFiles.add(arg);
            }
            else if (arg.equals("-example")) {
                if (count < args.length)
                {
                    String example_option = args[count++];
                    if (!example_option.equals("CMake"))
                    {
                        throw new BadArgumentException("Invalid example option specified after -example argument. Valid options are: CMake");
                    }
                    else
                    {
                        m_exampleOption = true;
                    }
                }
                else
                {
                    throw new BadArgumentException("No example option specified after -example argument");
                }
            }
            else if(arg.equals("-ppPath"))
            {
                if (count < args.length) {
                    m_ppPath = args[count++];
                } else {
                    throw new BadArgumentException("No URL specified after -ppPath argument");
                }
            } else if (arg.equals("-ppDisable")) {
                m_ppDisable = true;
            } else if (arg.equals("-replace")) {
                m_replace = true;
            } else if (arg.equals("-d")) {
                if (count < args.length) {
                    m_outputDir = Utils.addFileSeparator(args[count++]);
                } else {
                    throw new BadArgumentException("No URL specified after -d argument");
                }
            } else if (arg.equals("-cs")) {
                m_case_sensitive = true;
            } else if (arg.equals("-test")) {
                m_test = true;
            } else if (arg.equals("-version")) {
                showVersion();
                System.exit(0);
            } else if (arg.equals("-help")) {
                printHelp();
                System.exit(0);
            }
            else if (arg.equals("-I"))
            {
                if (count < args.length)
                {
                    m_includePaths.add("-I".concat(args[count++]));
                }
                else
                {
                    throw new BadArgumentException("No include directory specified after -I argument");
                }
            }
            else if (arg.equals("-cdr"))
            {
                if (count < args.length)
                {
                    String cdr_version_str = args[count++];
                    if (!cdr_version_str.equals("v1"))
                    {
                        throw new BadArgumentException("Invalid CDR version specified after -cdr argument");
                    }
                }
                else
                {
                    throw new BadArgumentException("No CDR version specified after -cdr argument");
                }
            }
            else if (arg.equals("-default-container-prealloc-size"))
            {
                if (count < args.length) {
                    m_defaultContainerPreallocSize = args[count++];
                } else {
                    throw new BadArgumentException("No value specified after -default-container-prealloc-size argument");
                }
            }
            else { // TODO: More options: -local, -rpm, -debug
                throw new BadArgumentException("Unknown argument " + arg);
            }

        }

        if (m_idlFiles.isEmpty()) {
            throw new BadArgumentException("No input files given");
        }

    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Main methods
     */

    public boolean execute() {
        if (!m_outputDir.equals(m_defaultOutputDir)) {
            File dir = new File(m_outputDir);

            if (!dir.exists()) {
                System.out.println(ColorMessage.error() + "The specified output directory does not exist");
                return false;
            }
        }

        boolean returnedValue = globalInit();

        if (returnedValue)
        {
            Solution solution = new Solution();

            // Load string templates
            System.out.println("Loading templates...");

            for (int count = 0; returnedValue && (count < m_idlFiles.size()); ++count) {
                Project project = process(m_idlFiles.get(count));

                if (project != null) {
                    solution.addProject(project);

                    for (String file : project.getFullDependencies()) {
                        System.out.println("Adding dependency project " + file);
                        Project depProject = process(file);
                    }
                } else {
                    returnedValue = false;
                }
            }

            // Generate solution
            if (returnedValue && (m_exampleOption || m_test)) {
                if ((returnedValue = genSolution(solution)) == false) {
                    System.out.println(ColorMessage.error() + "While the solution was being generated");
                }
            }
        }

        return returnedValue;

    }

    private void showVersion()
    {
        try
        {
            String classPath = getClass().getResource(getClass().getSimpleName() + ".class").toString();
            String libPath = classPath.substring(0, classPath.lastIndexOf("!"));
            String filePath = libPath + "!/META-INF/MANIFEST.MF";
            Manifest manifest = new Manifest(new URL(filePath).openStream());
            String version = manifest.getMainAttributes().getValue("Specification-Version");

            System.out.println(m_appName + " version: " + version);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void printHelp()
    {
        System.out.println(m_appName + " usage:");
        System.out.println("\t" + m_appName + " [options] <file> [<file> ...]");
        System.out.println("\twhere the options are:");
        System.out.println("\t\t-help: shows this help");
        System.out.println("\t\t-version: shows the current version of eProsima Micro XRCE-DDS Gen.");
        System.out.println("\t\t-example: Generates an example.");
        System.out.println("\t\t-replace: replaces existing generated files.");
        System.out.println("\t\t-ppDisable: disables the preprocessor.");
        System.out.println("\t\t-ppPath: specifies the preprocessor path.");
        System.out.println("\t\t-I <path>: add directory to preprocessor include paths.");
        System.out.println("\t\t-d <path>: sets an output directory for generated files.");
        System.out.println("\t\t-t <temp dir>: sets a specific directory as a temporary directory.");
        System.out.println("\t\t-cs: IDL grammar apply case sensitive matching.");
        System.out.println("\tand the supported input files are:");
        System.out.println("\t* IDL files.");
    }

    public boolean globalInit() {
        // Set the temporary folder
        if (m_tempDir == null) {
            if (m_os.contains("Windows")) {
                String tempPath = System.getenv("TEMP");

                if (tempPath == null) {
                    tempPath = System.getenv("TMP");
                }

                m_tempDir = tempPath;
            } else if (m_os.contains("Linux") || m_os.contains("Mac")) {
                m_tempDir = "/tmp/";
            }
        }

        if (m_tempDir.charAt(m_tempDir.length() - 1) != File.separatorChar) {
            m_tempDir += File.separator;
        }

        return true;
    }

    private Project process(String idlFilename) {
        Project project = null;
        System.out.println("Processing the file " + idlFilename + "...");

        try {
            // Protocol CDR
            project = parseIDL(idlFilename);
        } catch (Exception ioe) {
            System.out.println(ColorMessage.error() + "Cannot generate the files");
            if (!ioe.getMessage().equals("")) {
                System.out.println(ioe.getMessage());
            }
        }

        return project;
    }

    private Project parseIDL(String idlFileName) {
        boolean returnedValue = false;
        String idlParseFileName = idlFileName;
        Project project = null;

        String idlFileNameOnly = Util.getIDLFileNameOnly(idlFileName);

        if (!m_ppDisable) {
            idlParseFileName = callPreprocessor(idlFileName);
        }

        if (idlParseFileName != null) {
            // Create template manager
            TemplateManager tmanager = new TemplateManager();

            // Create context
            Context ctx = new Context(tmanager, idlFileNameOnly, idlFileName, m_includePaths, true, true, m_defaultContainerPreallocSize);

            if (m_case_sensitive)
            {
                ctx.ignore_case(false);
            }

            // Create default @Key annotation.
            AnnotationDeclaration keyann = ctx.createAnnotationDeclaration("Key", null);
            keyann.addMember(new AnnotationMember("value", new PrimitiveTypeCode(Kind.KIND_BOOLEAN), "true"));

            // Create default @Topic annotation.
            AnnotationDeclaration topicann = ctx.createAnnotationDeclaration("Topic", null);
            topicann.addMember(new AnnotationMember("value", new PrimitiveTypeCode(Kind.KIND_BOOLEAN), "true"));

            // Load common types template
            tmanager.addGroup("com/eprosima/microxrcedds/idl/templates/TypesHeader.stg");
            tmanager.addGroup("com/eprosima/microxrcedds/idl/templates/TypesSource.stg");

            // Load Publisher template
            tmanager.addGroup("com/eprosima/microxrcedds/idl/templates/PublisherSource.stg");

            // Load Subscriber template
            tmanager.addGroup("com/eprosima/microxrcedds/idl/templates/SubscriberSource.stg");

            // // Load test template
            tmanager.addGroup("com/eprosima/microxrcedds/idl/templates/SerializationTestSource.stg");
            tmanager.addGroup("com/eprosima/microxrcedds/idl/templates/SerializationSource.stg");
            tmanager.addGroup("com/eprosima/microxrcedds/idl/templates/SerializationHeader.stg");

            // Create main template
            TemplateGroup maintemplates = tmanager.createTemplateGroup("main");
            maintemplates.setAttribute("ctx", ctx);

            try {
                CharStream input = CharStreams.fromFileName(idlParseFileName);
                IDLLexer lexer = new IDLLexer(input);
                lexer.setContext(ctx);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                IDLParser parser = new IDLParser(tokens);
                // Pass the finelame without the extension

                Specification specification = parser.specification(ctx, tmanager, maintemplates).spec;
                returnedValue = specification != null;

            } catch (FileNotFoundException ex) {
                System.out.println(ColorMessage.error("FileNotFounException") + "The File " + idlParseFileName + " was not found.");
            } catch (Exception ex) {
                System.out.println(ColorMessage.error("Exception") + ex.getMessage());
            }

            if (returnedValue)
            {
                // Create information of project for solution
                project = new Project(idlFileNameOnly, idlFileName, ctx.getDependencies());
                String fileName;

                System.out.println("Generating Type definition files...");

                fileName = m_outputDir + idlFileNameOnly + ".h";
                returnedValue = Utils.writeFile(fileName, maintemplates.getTemplate("com/eprosima/microxrcedds/idl/templates/TypesHeader.stg"), m_replace);
                project.addCommonIncludeFile(fileName);

                fileName = m_outputDir + idlFileNameOnly + ".c";
                returnedValue = Utils.writeFile(fileName, maintemplates.getTemplate("com/eprosima/microxrcedds/idl/templates/TypesSource.stg"), m_replace);
                project.addCommonSrcFile(fileName);

                if (m_test)
                {
                    System.out.println("Generating Serialization Test file...");
                    fileName = m_outputDir + idlFileNameOnly + "SerializationTest.c";
                    returnedValue = Utils.writeFile(fileName, maintemplates.getTemplate("com/eprosima/microxrcedds/idl/templates/SerializationTestSource.stg"), m_replace);
                    project.addCommonSrcFile(fileName);

                    fileName = m_outputDir + idlFileNameOnly + "Serialization.c";
                    returnedValue = Utils.writeFile(fileName, maintemplates.getTemplate("com/eprosima/microxrcedds/idl/templates/SerializationSource.stg"), m_replace);
                    project.addCommonSrcFile(fileName);

                    fileName = m_outputDir + idlFileNameOnly + "Serialization.h";
                    returnedValue = Utils.writeFile(fileName, maintemplates.getTemplate("com/eprosima/microxrcedds/idl/templates/SerializationHeader.stg"), m_replace);
                    project.addCommonSrcFile(fileName);
                }

                if (ctx.existsLastStructure() && m_exampleOption)
                {
                    System.out.println("Generating publisher and subcriber example files...");

                    fileName = m_outputDir + idlFileNameOnly + "Publisher.c";
                    returnedValue = Utils.writeFile(fileName, maintemplates.getTemplate("com/eprosima/microxrcedds/idl/templates/PublisherSource.stg"), m_replace);
                    project.addCommonSrcFile(fileName);

                    fileName = m_outputDir + idlFileNameOnly + "Subscriber.c";
                    returnedValue = Utils.writeFile(fileName, maintemplates.getTemplate("com/eprosima/microxrcedds/idl/templates/SubscriberSource.stg"), m_replace);
                    project.addCommonSrcFile(fileName);
                }
            }
        }

        return returnedValue ? project : null;
    }


    private boolean genSolution(Solution solution)
    {
        return genCMakeLists(solution);
    }

    private boolean genCMakeLists(Solution solution)
    {
        boolean returnedValue = false;
        ST cmake = null;
        STGroupFile cmakeTemplates = new STGroupFile("com/eprosima/microxrcedds/idl/templates/CMakeLists.stg", '$', '$');

        if (cmakeTemplates != null)
        {
            cmake = cmakeTemplates.getInstanceOf("cmakelists");

            cmake.add("solution", solution);
            cmake.add("examples", m_exampleOption);
            cmake.add("test", m_test);

            returnedValue = Utils.writeFile(m_outputDir + "CMakeLists.txt", cmake, m_replace);
        }

        return returnedValue;
    }

    String callPreprocessor(String idlFilename)
    {
        final String METHOD_NAME = "callPreprocessor";

        // Set line command.
        ArrayList<String> lineCommand = new ArrayList<String>();
        String[] lineCommandArray = null;
        String outputfile = Util.getIDLFileOnly(idlFilename) + ".cc";
        int exitVal = -1;
        OutputStream of = null;

        // Use temp directory.
        if (m_tempDir != null) {
            outputfile = m_tempDir + outputfile;
        }

        if (m_os.contains("Windows")) {
            try {
                of = new FileOutputStream(outputfile);
            } catch (FileNotFoundException ex) {
                System.out.println(ColorMessage.error(METHOD_NAME) + "Cannot open file " + outputfile);
                return null;
            }
        }

        // Set the preprocessor path
        String ppPath = m_ppPath;

        if (ppPath == null) {
            if (m_os.contains("Windows")) {
                ppPath = "cl.exe";
            } else if (m_os.contains("Linux") || m_os.contains("Mac")) {
                ppPath = "cpp";
            }
        }

        // Add command
        lineCommand.add(ppPath);

        // Add the include paths given as parameters.
        for (int i=0; i < m_includePaths.size(); ++i) {
            if (m_os.contains("Windows")) {
                lineCommand.add(((String) m_includePaths.get(i)).replaceFirst("^-I", "/I"));
            } else if (m_os.contains("Linux") || m_os.contains("Mac")) {
                lineCommand.add(m_includePaths.get(i));
            }
        }

        if (m_os.contains("Windows")) {
            lineCommand.add("/E");
            lineCommand.add("/C");
        }

        // Add input file.
        lineCommand.add(idlFilename);

        if(m_os.contains("Linux") || m_os.contains("Mac")) {
            lineCommand.add(outputfile);
        }

        lineCommandArray = new String[lineCommand.size()];
        lineCommandArray = (String[])lineCommand.toArray(lineCommandArray);

        try {
            Process preprocessor = Runtime.getRuntime().exec(lineCommandArray);
            ProcessOutput errorOutput = new ProcessOutput(preprocessor.getErrorStream(), "ERROR", false, null, true);
            ProcessOutput normalOutput = new ProcessOutput(preprocessor.getInputStream(), "OUTPUT", false, of, true);
            errorOutput.start();
            normalOutput.start();
            exitVal = preprocessor.waitFor();
            errorOutput.join();
            normalOutput.join();
        } catch (Exception e) {
            System.out.println(ColorMessage.error(METHOD_NAME) + "Cannot execute the preprocessor. Reason: " + e.getMessage());
            return null;
        }

        if (of != null) {
            try {
                of.close();
            } catch (IOException e) {
                System.out.println(ColorMessage.error(METHOD_NAME) + "Cannot close file " + outputfile);
            }

        }

        if (exitVal != 0) {
            System.out.println(ColorMessage.error(METHOD_NAME) + "Preprocessor return an error " + exitVal);
            return null;
        }

        return outputfile;
    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Main entry point
     */

    public static void main(String[] args) {
        ColorMessage.load();

        try {

            microxrceddsgen main = new microxrceddsgen(args);
            if (main.execute()) {
                System.exit(0);
            }

        } catch (BadArgumentException e) {

            System.out.println(ColorMessage.error("BadArgumentException") + e.getMessage());
            printHelp();

        }

        System.exit(-1);
    }

}

class ProcessOutput extends Thread
{
    InputStream is = null;
    OutputStream of = null;
    String type;
    boolean m_check_failures;
    boolean m_found_error = false;
    final String clLine = "#line";
    boolean m_printLine = false;

    ProcessOutput(InputStream is, String type, boolean check_failures, OutputStream of, boolean printLine)
    {
        this.is = is;
        this.type = type;
        m_check_failures = check_failures;
        this.of = of;
        m_printLine = printLine;
    }

    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
            {
                if(of == null)
                {
                    if(m_printLine)
                        System.out.println(line);
                }
                else
                {
                    // Sustituir los \\ que pone cl.exe por \
                    if(line.startsWith(clLine))
                    {
                        line = "#" + line.substring(clLine.length());
                        int count = 0;
                        while((count = line.indexOf("\\\\")) != -1)
                        {
                            line = line.substring(0, count) + "\\" + line.substring(count + 2);
                        }
                    }

                    of.write(line.getBytes());
                    of.write('\n');
                }

                if(m_check_failures)
                {
                    if(line.startsWith("Done (failures)"))
                    {
                        m_found_error = true;
                    }
                }
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    boolean getFoundError()
    {
        return m_found_error;
    }
}
