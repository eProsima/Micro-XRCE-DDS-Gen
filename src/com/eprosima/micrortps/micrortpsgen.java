
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

package com.eprosima.micrortps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.HashMap;

import javax.swing.plaf.basic.BasicFormattedTextFieldUI;

import org.antlr.stringtemplate.CommonGroupLoader;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplateGroupLoader;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;

import com.eprosima.micrortps.exceptions.BadArgumentException;
import com.eprosima.micrortps.idl.grammar.Context;
import com.eprosima.micrortps.solution.Project;
import com.eprosima.micrortps.solution.Solution;
import com.eprosima.micrortps.util.Utils;
import com.eprosima.micrortps.util.VSConfiguration;
import com.eprosima.idl.generator.manager.TemplateGroup;
import com.eprosima.idl.generator.manager.TemplateManager;
import com.eprosima.idl.generator.manager.TemplateExtension;
import com.eprosima.idl.parser.grammar.IDLLexer;
import com.eprosima.idl.parser.grammar.IDLParser;
import com.eprosima.idl.parser.tree.Interface;
import com.eprosima.idl.parser.tree.Specification;
import com.eprosima.idl.parser.tree.AnnotationDeclaration;
import com.eprosima.idl.parser.tree.AnnotationMember;
import com.eprosima.idl.parser.typecode.PrimitiveTypeCode;
import com.eprosima.idl.parser.typecode.TypeCode;
import com.eprosima.idl.util.Util;
import com.eprosima.log.ColorMessage;
import com.eprosima.micrortps.idl.generator.TypesGenerator;

// TODO: Implement Solution & Project in com.eprosima.micrortps.solution

public class micrortpsgen {

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Attributes
     */

    private Vector<String> m_idlFiles;
    protected static String m_appEnv = "MICRORTPSHOME";
    private boolean m_exampleOption = false;
    private boolean write_access_profile_enabled = false;
    private boolean m_ppDisable = false; //TODO
    private boolean m_replace = false;
    private String m_ppPath = null;
    private final String m_defaultOutputDir = "." + File.separator;
    private String m_outputDir = m_defaultOutputDir;
    private String m_tempDir = null;
    protected static String m_appName = "micrortpsgen";

    private boolean m_publishercode = true;
    private boolean m_subscribercode = true;
    private boolean m_atLeastOneStructure = false;
    protected static String m_localAppProduct = "micrortps";
    private ArrayList m_includePaths = new ArrayList();

    private String m_command = null;
    private String m_extra_command = null;
    private ArrayList m_lineCommand = null;
    private ArrayList m_lineCommandForWorkDirSet = null;
    private String m_spTemplate = "main";

    private static VSConfiguration m_vsconfigurations[]={new VSConfiguration("Debug DLL", "Win32", true, true),
        new VSConfiguration("Release DLL", "Win32", false, true),
        new VSConfiguration("Debug", "Win32", true, false),
        new VSConfiguration("Release", "Win32", false, false)};

    private String m_os = null;
    private boolean m_local = false;
    private boolean fusion_ = false;

    //! Default package used in Java files.
    private String m_package = "";

    // Use to know the programming language
    public enum LANGUAGE
    {
        CPP,
        JAVA,
        C
    };

    private LANGUAGE m_languageOption = LANGUAGE.C; // Default language -> C

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Constructor
     */

    public micrortpsgen(String [] args) throws BadArgumentException {

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
                m_exampleOption = true;
            }
            else if (arg.equals("-write-access-profile")) {
                write_access_profile_enabled = true;
            }
            else if (arg.equals("-language"))
            {
                if (count < args.length)
                {
                    String languageOption = args[count++];

                    if(languageOption.equalsIgnoreCase("c"))
                        m_languageOption = LANGUAGE.C;
                    else if(languageOption.equalsIgnoreCase("c++"))
                        m_languageOption = LANGUAGE.CPP;
                    else if(languageOption.equalsIgnoreCase("java"))
                        m_languageOption = LANGUAGE.JAVA;
                    else
                        throw new BadArgumentException("Unknown language " +  languageOption);
                }
                else
                {
                    throw new BadArgumentException("No language specified after -language argument");
                }
            }
            else if(arg.equals("-package"))
            {
                if(count < args.length)
                {
                    m_package = args[count++];
                }
                else
                    throw new BadArgumentException("No package after -package argument");
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
            } else if (arg.equals("-version")) {
                showVersion();
                System.exit(0);
            } else if (arg.equals("-help")) {
                printHelp();
                System.exit(0);
            }
            else if(arg.equals("-local"))
            {
                m_local = true;
            }
            else if(arg.equals("-fusion"))
            {
                fusion_ = true;
            }
            else { // TODO: More options: -local, -rpm, -debug -I
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
     * Listener classes
     */

    class TemplateErrorListener implements StringTemplateErrorListener
    {
        public void error(String arg0, Throwable arg1)
        {
            System.out.println(ColorMessage.error() + arg0);
            arg1.printStackTrace();
        }

        public void warning(String arg0)
        {
            System.out.println(ColorMessage.warning() + arg0);
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
            Solution solution = new Solution(m_languageOption, getVersion(), m_publishercode, m_subscribercode);

            // Load string templates
            System.out.println("Loading templates...");
            TemplateManager.setGroupLoaderDirectories("com/eprosima/micrortps/idl/templates");

            // In local for all products
            if(m_os.contains("Windows"))
            {
                solution.addInclude("$(" + m_appEnv + ")/include");
                solution.addLibraryPath("$(" + m_appEnv + ")/lib");
            }

            // Add product library
            solution.addLibrary("micrortps_client");

            for (int count = 0; returnedValue && (count < m_idlFiles.size()); ++count) {
                Project project = process(m_idlFiles.get(count));

                if (project != null) {
                    solution.addProject(project);
                } else {
                    returnedValue = false;
                }
            }

            // Generate solution
            if (returnedValue && m_exampleOption) {
                if ((returnedValue = genSolution(solution)) == false) {
                    System.out.println(ColorMessage.error() + "While the solution was being generated");
                }
            }

        }

        return returnedValue;

    }

    private String getVersion()
    {
        try
        {
            //InputStream input = this.getClass().getResourceAsStream("/micrortps_version.h");

            InputStream input = this.getClass().getClassLoader().getResourceAsStream("version");
            byte[] b = new byte[input.available()];
            input.read(b);
            String text = new String(b);
            int beginindex = text.indexOf("=");
            return text.substring(beginindex + 1);
        }
        catch(Exception ex)
        {
            System.out.println(ColorMessage.error() + "Getting version. " + ex.getMessage());
        }

        return "";
    }

    private void showVersion()
    {
        String version = getVersion();
        System.out.println(m_appName + " version " + version);
    }

    public static void printHelp()
    {
        System.out.println(m_appName + " usage:");
        System.out.println("\t" + m_appName + " [options] <file> [<file> ...]");
        System.out.println("\twhere the options are:");
        System.out.println("\t\t-help: shows this help");
        System.out.println("\t\t-version: shows the current version of eProsima Micro RTPS.");
        System.out.println("\t\t-write-access-profile: Generates a write function for the topic.");
        System.out.println("\t\t-example: Generates an example.");
        System.out.println("\t\t-replace: replaces existing generated files.");
        System.out.println("\t\t-ppDisable: disables the preprocessor.");
        System.out.println("\t\t-ppPath: specifies the preprocessor path.");
        System.out.println("\t\t-d <path>: sets an output directory for generated files.");
        System.out.println("\t\t-t <temp dir>: sets a specific directory as a temporary directory.");
        System.out.println("\tand the supported input files are:");
        System.out.println("\t* IDL files.");

    }

    public boolean globalInit() {
        String dds_root = null, tao_root = null, micrortps_root = null;

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

        // Set the line command
        m_lineCommand = new ArrayList();

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

    private Project parseIDL(String idlFilename) {
        boolean returnedValue = false;
        String idlParseFileName = idlFilename;
        Project project = null;

        String onlyFileName = Util.getIDLFileNameOnly(idlFilename);

        if (!m_ppDisable) {
            idlParseFileName = callPreprocessor(idlFilename);
        }

        if (idlParseFileName != null) {
            Context ctx = new Context(onlyFileName, idlFilename, m_includePaths, m_subscribercode, m_publishercode, m_localAppProduct);

            if(fusion_) ctx.setActivateFusion(true);

            // Create default @Key annotation.
            AnnotationDeclaration keyann = ctx.createAnnotationDeclaration("Key", null);
            keyann.addMember(new AnnotationMember("value", new PrimitiveTypeCode(TypeCode.KIND_BOOLEAN), "true"));

            // Create default @Topic annotation.
            AnnotationDeclaration topicann = ctx.createAnnotationDeclaration("Topic", null);
            topicann.addMember(new AnnotationMember("value", new PrimitiveTypeCode(TypeCode.KIND_BOOLEAN), "true"));

            // Create template manager
            TemplateManager tmanager = new TemplateManager("MicroCdrCommon:eprosima:Common");

            List<TemplateExtension> extensions = new ArrayList<TemplateExtension>();

            // Load common types template
            extensions.add(new TemplateExtension("struct_type", "keyFunctionHeadersStruct"));
            tmanager.addGroup("TypesHeader", extensions);
            tmanager.addGroup("TypesSource", extensions);

            // Load write access profile
            tmanager.addGroup("WriteAccessHeader");
            tmanager.addGroup("WriteAccessSource");

            // Load Publisher template
            tmanager.addGroup("RTPSPublisherSource");

            // Load Subscriber template
            tmanager.addGroup("RTPSSubscriberSource");

            // Add JNI sources.
            if(m_languageOption == LANGUAGE.JAVA)
            {
                tmanager.addGroup("JNIHeader");
                tmanager.addGroup("JNISource");
                tmanager.addGroup("JavaSource");

                // Set package in context.
                ctx.setPackage(m_package);
            }

            // Create main template
            TemplateGroup maintemplates = tmanager.createTemplateGroup("main");
            maintemplates.setAttribute("ctx", ctx);

            try {
                ANTLRFileStream input = new ANTLRFileStream(idlParseFileName);
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
                project = new Project(onlyFileName, idlFilename, ctx.getDependencies());

                System.out.println("Generating Type definition files...");
                if (returnedValue = Utils.writeFile(m_outputDir + onlyFileName + ".h", maintemplates.getTemplate("TypesHeader"), m_replace)) {
                    project.addCommonIncludeFile(onlyFileName + ".h");
                }

                if (returnedValue = Utils.writeFile(m_outputDir + onlyFileName + ".c", maintemplates.getTemplate("TypesSource"), m_replace)) {
                    project.addCommonSrcFile(onlyFileName + ".c");
                }

                if (ctx.existsLastStructure())
                {
                    m_atLeastOneStructure = true;
                    project.setHasStruct(true);

                    if (m_exampleOption || write_access_profile_enabled)
                    {
                        System.out.println("Generating Write Access profile files...");
                        if (returnedValue = Utils.writeFile(m_outputDir + onlyFileName + "Writer.h", maintemplates.getTemplate("WriteAccessHeader"), m_replace)) {
                            project.addCommonIncludeFile(onlyFileName + "Writer.h");
                        }

                        if (returnedValue = Utils.writeFile(m_outputDir + onlyFileName + "Writer.c", maintemplates.getTemplate("WriteAccessSource"), m_replace)) {
                            project.addCommonSrcFile(onlyFileName + "Writer.c");
                        }
                    }

                    if (m_exampleOption)
                    {
                        System.out.println("Generating Publisher files...");
                        if (returnedValue = Utils.writeFile(m_outputDir + onlyFileName + "Publisher.c", maintemplates.getTemplate("RTPSPublisherSource"), m_replace))
                        {
                            project.addProjectSrcFile(onlyFileName + "Publisher.c");
                        }
                        if (returnedValue = Utils.writeFile(m_outputDir + onlyFileName + "Subscriber.c", maintemplates.getTemplate("RTPSSubscriberSource"), m_replace))
                        {
                            project.addProjectSrcFile(onlyFileName + "Subscriber.c");
                        }
                    }
                }

            }

            // Java support (Java classes and JNI code)
            if(returnedValue && m_languageOption == LANGUAGE.JAVA)
            {
                String outputDir = m_outputDir;

                // Make directories from package.
                if(!m_package.isEmpty())
                {
                    outputDir = m_outputDir + File.separator + m_package.replace('.', File.separatorChar);
                    File dirs = new File(outputDir);

                    if(!dirs.exists())
                    {
                        if(!dirs.mkdirs())
                        {
                            System.out.println(ColorMessage.error() + "Cannot create directories for Java packages.");
                            return null;
                        }
                    }
                }

                // Java classes.
                TypesGenerator typeGen = new TypesGenerator(tmanager, m_outputDir, m_replace);
                TypeCode.javapackage = m_package + (m_package.isEmpty() ? "" : ".");
                if(!typeGen.generate(ctx, outputDir + File.separator, m_package, onlyFileName, null))
                {
                    System.out.println(ColorMessage.error() + "generating Java types");
                    return null;
                }

                if(ctx.existsLastStructure())
                {
                    System.out.println("Generando fichero " + m_outputDir + onlyFileName + "PubSub.java");
                    if(!Utils.writeFile(outputDir + File.separator + onlyFileName + "PubSub.java", maintemplates.getTemplate("JavaSource"), m_replace))
                        return null;

                    // Call javah application for each structure.
                    if(!callJavah(idlFilename))
                        return null;
                }

                if(Utils.writeFile(m_outputDir + onlyFileName + "PubSubJNII.h", maintemplates.getTemplate("JNIHeader"), m_replace))
                    project.addJniIncludeFile(onlyFileName + "PubSubJNII.h");
                else
                    return null;

                StringTemplate jnisourceTemplate = maintemplates.getTemplate("JNISource");
                if(Utils.writeFile(m_outputDir + onlyFileName + "PubSubJNI.cxx", jnisourceTemplate, m_replace))
                    project.addJniSrcFile(onlyFileName + "PubSubJNI.cxx");
                else
                    return null;
            }
        }

        return returnedValue ? project : null;
    }

    private boolean genSolution(Solution solution)
    {
        return genCMakeLists(solution, "");
    }

    private boolean genCMakeLists(Solution solution, String arch)
    {
        boolean returnedValue = false;
        StringTemplate cmakelists = null;

        StringTemplateGroup cmakeTemplates = StringTemplateGroup.loadGroup("cmakelists", DefaultTemplateLexer.class, null);

        if (cmakeTemplates != null)
        {
            cmakelists = cmakeTemplates.getInstanceOf("cmakelists");

            cmakelists.setAttribute("solution", solution);
            cmakelists.setAttribute("arch", arch);

            returnedValue = Utils.writeFile(m_outputDir + "CMakeLists.txt", cmakelists, m_replace);
        }

        return returnedValue;
    }

    String callPreprocessor(String idlFilename)
    {
        final String METHOD_NAME = "callPreprocessor";

        // Set line command.
        ArrayList lineCommand = new ArrayList();
        String [] lineCommandArray = null;
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

    boolean callJavah(String idlFilename)
    {
        final String METHOD_NAME = "calljavah";
        // Set line command.
        ArrayList<String> lineCommand = new ArrayList<String>();
        String[] lineCommandArray = null;
        String fileDir = Util.getIDLFileDirectoryOnly(idlFilename);
        String javafile = (m_outputDir != null ? m_outputDir : "") +
            (!m_package.isEmpty() ? m_package.replace('.', File.separatorChar) + File.separator : "") +
            Util.getIDLFileNameOnly(idlFilename) + "PubSub.java";
        String headerfile = m_outputDir + Util.getIDLFileNameOnly(idlFilename) + "PubSubJNI.h";
        int exitVal = -1;
        String javac = null;
        String javah = null;

        // First call javac
        if(m_os.contains("Windows"))
        {
            javac = "javac.exe";
        }
        else if(m_os.contains("Linux") || m_os.contains("Mac"))
        {
            javac = "javac";
        }

        // Add command
        lineCommand.add(javac);
        if(m_tempDir != null)
        {
            lineCommand.add("-d");
            lineCommand.add(m_tempDir);
        }

        if( fileDir != null && !fileDir.isEmpty())
        {
            lineCommand.add("-sourcepath");
            lineCommand.add(m_outputDir);
        }

        lineCommand.add(javafile);

        lineCommandArray = new String[lineCommand.size()];
        lineCommandArray = (String[])lineCommand.toArray(lineCommandArray);

        try
        {
            Process preprocessor = Runtime.getRuntime().exec(lineCommandArray);
            ProcessOutput errorOutput = new ProcessOutput(preprocessor.getErrorStream(), "ERROR", false, null, true);
            ProcessOutput normalOutput = new ProcessOutput(preprocessor.getInputStream(), "OUTPUT", false, null, true);
            errorOutput.start();
            normalOutput.start();
            exitVal = preprocessor.waitFor();
            errorOutput.join();
            normalOutput.join();
        }
        catch(Exception ex)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "Cannot execute the javac application. Reason: " + ex.getMessage());
            return false;
        }

        if(exitVal != 0)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "javac application return an error " + exitVal);
            return false;
        }

        lineCommand = new ArrayList<String>();

        if(m_os.contains("Windows"))
        {
            javah = "javah.exe";
        }
        else if(m_os.contains("Linux") || m_os.contains("Mac"))
        {
            javah = "javah";
        }

        // Add command
        lineCommand.add(javah);
        lineCommand.add("-jni");
        if(m_tempDir != null)
        {
            lineCommand.add("-cp");
            lineCommand.add(m_tempDir);
        }
        lineCommand.add("-o");
        lineCommand.add(headerfile);
        lineCommand.add((!m_package.isEmpty() ? m_package + "." : "") +
                Util.getIDLFileNameOnly(idlFilename) + "PubSub");

        lineCommandArray = new String[lineCommand.size()];
        lineCommandArray = (String[])lineCommand.toArray(lineCommandArray);

        try
        {
            Process preprocessor = Runtime.getRuntime().exec(lineCommandArray);
            ProcessOutput errorOutput = new ProcessOutput(preprocessor.getErrorStream(), "ERROR", false, null, true);
            ProcessOutput normalOutput = new ProcessOutput(preprocessor.getInputStream(), "OUTPUT", false, null, true);
            errorOutput.start();
            normalOutput.start();
            exitVal = preprocessor.waitFor();
            errorOutput.join();
            normalOutput.join();
        }
        catch(Exception ex)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "Cannot execute the javah application. Reason: " + ex.getMessage());
            return false;
        }

        if(exitVal != 0)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "javah application return an error " + exitVal);
            return false;
        }

        return true;
    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Main entry point
     */

    public static void main(String[] args) {
        ColorMessage.load();

        try {

            micrortpsgen main = new micrortpsgen(args);
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
