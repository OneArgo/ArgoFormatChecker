import ucar.nc2.*;
import ucar.ma2.*;

import java.io.*;
import java.util.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import usgdac.*;
import usgdac.ArgoDataFile.FileType;

/**
 */
public class TestResultsFile
{

   //...........................................................
   //......................Variable Declarations................
   //...........................................................

   //..standard i/o shortcuts
   static PrintStream stdout = new PrintStream(System.out);
   static PrintStream stderr = new PrintStream(System.err);

   //..class variables
   static String     outFileName;
   static String     specFileName;

   private static final String propFileName;

   private static Properties property;

   private static final Class<?> ThisClass;
   private static final String ClassName;

   private static final Logger log;

   static {
      ThisClass = MethodHandles.lookup().lookupClass();
      ClassName = ThisClass.getSimpleName();
      propFileName = ClassName+".properties";

      System.setProperty("logfile.name", ClassName+"_LOG");
      log = LogManager.getLogger(ClassName);
   }


   static boolean doIndexInfo = false;

   //.............................................................
   //..                                                         ..
   //..                      main                               ..
   //..                                                         ..
   //.............................................................

   public static void main (String args[])
      throws IOException, XMLStreamException, TransformerException, TransformerConfigurationException
   {
      //.....get the Properties....

      try {
         ClassLoader loader = ThisClass.getClassLoader();
         InputStream in = loader.getResourceAsStream(propFileName);
         
         property = new Properties();
         property.load(in);
         in.close();

      } catch (Exception e) {
         stderr.println("\n*** No property file: '"+propFileName+"' ***\n");
         System.exit(1);
      }

      String specDirName = property.getProperty("SpecDirectory", "unknown");
      String dacName = property.getProperty("Dac", "unknown");

      //.....extract the options....
      int next;
      boolean doXml = false;

      for (next = 0; next < args.length; next++) {
         if (args[next].equals("-help")) {
            Help();
            System.exit(0);
         } else if (args[next].equals("-xml")) {
            doXml = true;
         } else if (args[next].equals("-text")) {
            doXml = false;
         } else if (args[next].startsWith("-")) {
            stderr.println("Invalid argument: '"+args[next]+"'");
            System.exit(1);
         } else {
            break;
         }
      }

      stdout.println("\n"+ClassName+" inputs:");
      stdout.println("   Specification directory: "+specDirName);
      //stdout.println("   Output file:             "+outFile);


      //.....check the spec directory.....
      
      if (! (new File(specDirName).isDirectory())) {
         stderr.println("ERROR: Specification directory is not a directory ('"+
                        specDirName+"')");
         log.error("specification directory is not a directory");
         System.exit(1);
      }

      //..set some test settings

      String fcVersion = "6.0";
      String spVersion = "1.2.4";

      ArgoDataFile argo = null;

      String inFileName;
      String outFileName;
      ResultsFile results;

      //..........................................................
      //................test exception output.....................
      //..........................................................

      String test = "exception";
      inFileName = "1929394_blah.nc";
      outFileName = "results."+test;

      stdout.println("\nTest '"+test+"':");
      stdout.println("...output file: '"+outFileName+"'");
      log.debug("Test exception output: '{}'", outFileName);

      results = new ResultsFile(doXml, outFileName,
                                fcVersion, spVersion, inFileName);

      log.debug("cause an exception from ArgoDataFile.open");

      try {
         argo = ArgoDataFile.open("gobbledygook");

         stdout.println("\n   *** TEST FAILED to throw exception!! ***\n");
         log.debug("...TEST FAILED to throw exception!!");

      } catch (Exception e) {
         stdout.println("...intentionally created an exception");
         log.debug("...intentionally created an exception");

         results.openError(e);
      }

      results.close();

      stdout.println("...test completed");

      //..........................................................
      //.............test "not an argo file" output...............
      //..........................................................

      test = "not_argo_file";
      inFileName = property.getProperty("NotAnArgoFile", "unknown");
      outFileName = "results."+test;

      stdout.println("\nTest '"+test+"':");
      stdout.println("...input file:  '"+inFileName+"'");
      stdout.println("...output file: '"+outFileName+"'");

      log.debug("Test 'not an Argo file' output: in, out = '{}', '{}'",
                inFileName, outFileName);

      results = new ResultsFile(doXml, outFileName,
                                fcVersion, spVersion, inFileName);

      try {
         argo = ArgoDataFile.open(inFileName);
         stdout.println("...test file opened");
         log.debug("...test file opened");

      } catch (Exception e) {
         stdout.println("\n   *** TEST FAILED!!! Could not open the test file ***\n");
         e.printStackTrace();
         log.debug("...TEST FAILED!!! Could not open the test file: '{}'", inFileName);
      }

      if (argo == null) {
         stdout.println("...argo.open returned null as expected");
         results.notArgoFile(dacName);

      } else {
         stdout.println("\n   *** TEST FAILED!!! argo.open was incorrectly successful ***\n");
      }

      results.close();

      stdout.println("...test completed");

      //..........................................................
      //........test "format open, no specification" output.......
      //..........................................................
      
      test = "no_specification";
      inFileName = property.getProperty("NoSpecificationFile", "unknown");
      outFileName = "results."+test;

      stdout.println("\nTest '"+test+"':");
      stdout.println("...input file:  '"+inFileName+"'");
      stdout.println("...output file: '"+outFileName+"'");

      log.debug("Test 'no specification' output: in, out = '{}', '{}'",
                inFileName, outFileName);

      results = new ResultsFile(doXml, outFileName,
                                fcVersion, spVersion, inFileName);

      try {
         argo = ArgoDataFile.open(inFileName, specDirName, true);

         stdout.println("...test file file opened");
         log.debug("...test file opened");

      } catch (Exception e) {
         stdout.println("\n   *** TEST FAILED!!! Could not open the test file ***\n");
         e.printStackTrace();
         log.debug("...TEST FAILED!!! Could not open the test file: '{}'", inFileName);
      }

      String phase = "FORMAT-VERIFICATION";

      if (argo == null) {
         stdout.println("...test file failed argo.open as desired");
         log.debug("...test file failed argo.open as desired");

         results.formatErrorMessage(phase);

      } else {
         stdout.println("\n   *** TEST FAILED!!! Test file incorrectly succeeded argo.open ***\n");
         log.debug("...TEST FAILED!!! Test file incorrectly succeeded argo.open ***\n");
      }

      results.close();

      stdout.println("...test completed");


      //..........................................................
      //.........test "format validation failed" output...........
      //..........................................................
      
      test = "format_invalid";
      inFileName = property.getProperty("FormatInvalidFile", "unknown");
      outFileName = "results."+test;

      stdout.println("\nTest '"+test+"':");
      stdout.println("...input file:  '"+inFileName+"'");
      stdout.println("...output file: '"+outFileName+"'");

      log.debug("Test 'format invalid' output: in, out = '{}', '{}'",
                inFileName, outFileName);

      results = new ResultsFile(doXml, outFileName,
                                fcVersion, spVersion, inFileName);

      try {
         argo = ArgoDataFile.open(inFileName, specDirName, true);

      } catch (Exception e) {
         stdout.println("\n   *** TEST FAILED!!! Could not open the test file ***\n");
         e.printStackTrace();
         log.debug("...TEST FAILED!!! Could not open the test file: '{}'", inFileName);
      }

      phase = "FORMAT-VERIFICATION";
      boolean checkTest = true;

      if (argo == null) {
         checkTest = false;
         stdout.println("\n   *** TEST FAILED!!! Incorrectly failed in argo.open ***\n");
         log.debug("...TEST FAILED!!! Incorrectly failed in argto.,open ***\n");

      } else {
         stdout.println("...test file file opened");
         log.debug("...test file opened");
      }

      if (checkTest) {
         stdout.println("...verify format");
         log.debug("...verify format");

         checkTest = argo.verifyFormat(dacName);

         if (! checkTest) {
            stdout.println("\n   *** TEST FAILED!!! argo.verifyFormat incorrectly returned false ***\n");
            log.debug("   *** TEST FAILED!!! argo.verifyFormat incorrectly returned false ***");
         }
      }

      if (checkTest) {
         boolean formatPassed = ( argo.nFormatErrors() == 0);

         if (! formatPassed) {
            stdout.println("...test file failed format validation as desired");
            log.debug("...test file failed format validation as desired");

            boolean doPsalStats = false;

            results.statusAndPhase(formatPassed, phase);
            results.metaData(dacName, argo, formatPassed, doPsalStats);
            results.errorsAndWarnings(argo);

         } else {
            stdout.println("\n   *** TEST FAILED!!! Test file incorrectly passed format validation ***\n");
            log.debug("...TEST FAILED!!! Test file incorrectly passed format validation ***\n");
         }
      }

      results.close();

      stdout.println("...test completed");

      //..........................................................
      //.....test "data consistency errors/warnings" output.......
      //..........................................................
      
      test = "data_error";
      inFileName = property.getProperty("DataErrorFile", "unknown");
      outFileName = "results."+test;

      stdout.println("\nTest '"+test+"':");
      stdout.println("...input file:  '"+inFileName+"'");
      stdout.println("...output file: '"+outFileName+"'");

      log.debug("Test 'format invalid' output: in, out = '{}', '{}'",
                inFileName, outFileName);

      results = new ResultsFile(doXml, outFileName,
                                fcVersion, spVersion, inFileName);

      try {
         argo = ArgoDataFile.open(inFileName, specDirName, true);

      } catch (Exception e) {
         stdout.println("\n   *** TEST FAILED!!! Could not open the test file ***\n");
         e.printStackTrace();
         log.debug("...TEST FAILED!!! Could not open the test file: '{}'", inFileName);
      }

      phase = "FORMAT-VERIFICATION";
      checkTest = true;

      if (argo == null) {
         checkTest = false;
         stdout.println("\n   *** TEST FAILED!!! Incorrectly failed in argo.open ***\n");
         log.debug("...TEST FAILED!!! Incorrectly failed in argto.,open ***\n");
      }

      if (checkTest) {
         stdout.println("...verify format");
         log.debug("...verify format");

         checkTest = argo.verifyFormat(dacName);

         if (! checkTest) {
            stdout.println("\n   *** TEST FAILED!!! argo.verifyFormat incorrectly returned false ***\n");
            log.debug("...TEST FAILED!!! argo.verifyFormat incorrectly returned false ***");
         }
      }

      boolean formatPassed = false;

      if (checkTest) {
         formatPassed = ( argo.nFormatErrors() == 0);

         if (! formatPassed) {
            stdout.println("\n   *** TEST FAILED!!! Incorrectly found format errors ***\n");
            log.debug("...TEST FAILED!!! Incorrectly found format errors ***\n");

         } else {
            stdout.println("...passed format check as desired");
            log.debug("...passed format check as desired");
         }
      }

      if (formatPassed) {
         dacName = "aoml";
         checkTest = ((ArgoProfileFile) argo).validate(false, dacName, true);

         if (! checkTest) {
            stdout.println("\n   *** TEST FAILED!!! Test file incorrectly argo.validate = false ***\n");
            log.debug("\n   *** TEST FAILED!!! Test file incorrectly argo.validate = false ***\n");

            stdout.println(ArgoDataFile.getMessage());
         }

         boolean dataPassed = ( argo.nFormatErrors() == 0  &&  argo.nFormatWarnings() == 0 );

         if (! dataPassed) {
            stdout.println("...test file failed data check as desired");
            log.debug("...test file failed data check as desired");

            boolean doPsalStats = false;

            results.statusAndPhase(formatPassed, phase);
            results.metaData(dacName, argo, formatPassed, doPsalStats);
            results.errorsAndWarnings(argo);

         } else {
            stdout.println("\n   *** TEST FAILED!!! Test file incorrectly passed data verification ***\n");
            log.debug("...TEST FAILED!!! Test file incorrectly passed data verification ***\n");
         }
      }

      results.close();

      stdout.println("...test completed");

   } //..end main

   //...................................
   //..              Help             ..
   //...................................
   public static void Help ()
   {
      stdout.println (
      "\n"+
      "Purpose: Creates a merged core-/bio-profile Argo NetCDF file from the input files\n" +
      "\n"+
      "Usage: java "+ClassName+" [options] spec-dir file-type format-version\n"+
      "\n"+
      "Options:\n"+
      "   -xml           XML results file\n"+
      "   -text          Text results file\n"+
      "                  default\n"+
      "   -help\n"+
      "\n"+
      "Arguments:\n"+
      "   spec-dir       Directory to specification files\n" +
      "   file-type      meta, prof, tech, traj\n"+
      "   format-version Version of file format for the multi-profile file\n"+
      "\n");
      //"   out-file       Output file\n"+
      return;
   }

} //..end class
