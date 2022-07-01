import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D1;
import ucar.nc2.NetcdfFile;

import usgdac.*;
import usgdac.ArgoDataFile.FileType;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements the Argo FileChecker data file validation checking.
 * <p>
 * Separate documentation exists:
 * <ul>
 * <li>Details of the checks: Argo Data File Format and Consistency Checks
 * <li>Users Manual: 
 * </ul> 
 * <p>
 * @author Mark Ignaszewski
 * @version  $Id: ValidateSubmit.java 1319 2022-04-14 21:48:55Z ignaszewski $
 */
public class ValidateSubmit
{

   //......................Variable Declarations................


   private static ArgoDataFile argo;

   private static boolean doXml = true;

   private static String fcVersion;
   private static String spVersion;

   private static String propFileName = new String("Application.properties");
   private static String specPropFileName = new String("VersionInfo.properties");
   private static Properties codeProp;
   private static Properties specProp;
   
   //..standard i/o shortcuts
   static PrintStream stdout = new PrintStream(System.out);
   static PrintStream stderr = new PrintStream(System.err);

   static final Class<?> ThisClass;
   static final String ClassName;

   private static final Logger log;

   static {
      ThisClass = MethodHandles.lookup().lookupClass();
      ClassName = ThisClass.getSimpleName();

      System.setProperty("logfile.name", ClassName+"_LOG");

      log = LogManager.getLogger(ClassName);
   }


   //.................................................................
   //
   //                           main
   //
   //.................................................................

   public static void main (String args[])
      throws IOException
   {
      boolean noErrors;
      boolean doBatteryChecks = false;   //..check metadate battery vars - default: no
      boolean doNameCheck = true;   //..check file name - default: yes
      boolean doNulls = false;   //..check for nulls in strings - default: no
      boolean doFormatOnly = false; //..true: format-only; false: format and data checks
      boolean doFormatOnlyPre31 = true; //..true: format-only for pre-v3.1 - full checks v3.1
                                        //..false: format and data checks for all
      boolean doPsalStats = false; //..true: compute PSAL stats for index file (core-profile only)
      int next = 0;              //..counter for args array
      String listFile = null;    //..list file name
      List<String> inFileList = null; //..list of input files
      boolean version = false;

      //..this is a class variable..
      //..boolean doXml = true;    //..output XML or text - default: XML
      
      //.....get the Properties....

      try {
         ClassLoader loader = ThisClass.getClassLoader();
         InputStream in = loader.getResourceAsStream(propFileName);
         
         codeProp = new Properties();
         codeProp.load(in);
         in.close();

         fcVersion = codeProp.getProperty("Version", "unknown");

      } catch (Exception e) {
         fcVersion = "unknown";
         log.debug("could not read codeProp file");
      }

      log.info("Code version: file, version = '{}', '{}'", propFileName, fcVersion);

      //.....extract the options....
      for (next = 0; next < args.length; next++) {
         if (args[next].equals("-help")) {
            Help();
            System.exit(0);
         } else if (args[next].equals("-version")) {
            version = true;
            stdout.println(" ");
            stdout.println("Code version: "+fcVersion);
            System.exit(0);
         } else if (args[next].equals("-no-name-check")) {
            doNameCheck = false;
         } else if (args[next].equals("-null-warn")) {
            doNulls = true;
         } else if (args[next].equals("-text-result")) {
            doXml = false;
         } else if (args[next].equals("-format-only")) {
            doFormatOnly = true;
         } else if (args[next].equals("-format-only-pre3.1")) {
            doFormatOnlyPre31 = true;
         } else if (args[next].equals("-data-check-all")) {
            doFormatOnlyPre31 = false;
         } else if (args[next].equals("-battery-check")) {
            doBatteryChecks = true;
         } else if (args[next].equals("-psal-stats")) {
            doPsalStats = true;
         } else if (args[next].equals("-list-file")) {
            next++;
            if (next < args.length) {
               listFile = args[next];
            }

         //..obsolete arguments -- left in for backwards compatibility

         } else if (args[next].equals("-no-fresh")) {
            stderr.println("Obsolete argument '-no-fresh' given. IGNORED");
         } else if (args[next].equals("-full-traj-checks")) {
            stderr.println("Obsolete argument '-full-traj-checks' given. IGNORED");


         } else if (args[next].startsWith("-")) {
            stderr.println("Invalid argument: '"+args[next]+"'");
            System.exit(1);
         } else {
            break;
         }
      }

      //.....parse the positional parameters.....

      log.info("{}:  START", ClassName);
      
      if (args.length < (4 + next)) {
         log.error("too few arguments: "+args.length);
         Help();
         stderr.println("Too few arguments: "+args.length);
         System.exit(1);
      }
            
      String dacName = args[next++];
      String specDirName = args[next++];
      String outDirName = args[next++];
      String inDirName = args[next++];
      if (next < args.length) {
         inFileList = new ArrayList<String>(args.length - next);
         for (; next < args.length; next++) inFileList.add(args[next]);
      }

      log.debug("doBatteryChecks = {}", doBatteryChecks);
      log.debug("doFormatOnly = {}", doFormatOnly);
      log.debug("doFormatOnlyPre31 = {}", doFormatOnlyPre31);
      log.debug("doNameCheck = {}", doNameCheck);
      log.debug("doNulls = {}", doNulls);
      log.debug("doXml = {}", doXml);
      log.debug("dacName = '{}'", dacName);
      log.debug("listFile = '{}'", listFile);
      log.debug("specDirName = '{}'", specDirName);
      log.debug("outDirName = '{}'", outDirName);
      log.debug("inDirName = '{}'", inDirName);
      log.debug("number of inFileList = "+
                (inFileList == null ? "null" : inFileList.size()));
      
      //..load the spec version information

      try {
         InputStream in = new FileInputStream(specDirName+File.separator+specPropFileName);
         
         specProp = new Properties();
         specProp.load(in);
         in.close();

         spVersion = specProp.getProperty("Version", "unknown");

      } catch (Exception e) {
         spVersion = "unknown";
         log.debug("could not read specProperties file");
      }

      log.info("Spec-file version: file, version = '{}', '{}'", specPropFileName, spVersion);

      //.....check the DAC name.....
      
      boolean dacOK = false;
      for (ArgoReferenceTable.DACS d : ArgoReferenceTable.DACS.values()) {
         if (d.name.equals(dacName)) {
            dacOK = true;
            break;
         }
      }
      if (! dacOK) {
         stderr.println("\nERROR: Unknown DAC name = '" + dacName + "'");
         log.error("invalid DAC name");
         System.exit(1);
      }
      
      //.....check the spec directory.....
      
      if (! (new File(specDirName).isDirectory())) {
         stderr.println("ERROR: Specification directory is not a directory ('"+
                        specDirName+"')");
         log.error("specification directory is not a directory");
         System.exit(1);
      }

      //.....check the input directory and files.....
      
      File inDir = new File(inDirName);
      if (! inDir.isDirectory()) {
         stderr.println("ERROR: Input directory is not a directory ('"+inDirName+"')");
         log.error("input directory is not a directory");
         System.exit(1);
      }

      //.........get list of input files........
      //..input files are chosen in the following priority order
      //..1) an input-file-list (overrides all other lists)
      //..2) file name arguments (already parsed above, if specified)
      //..3) all files in the input directory

      if (listFile != null) {
         //..a list file was specified - open and read it
         //..this overrides all other "input lists"
         File f = new File(listFile);
         if (! f.isFile()) {
            log.error("-list-file does not exist: '" + listFile + "'");
            stderr.println("\nERROR: -list-file DOES NOT EXIST: '"+listFile+"'");
            System.exit(1);
         } else if (! f.canRead()) {
            log.error("-list-file cannot be read: '" + listFile + "'");
            stderr.println("\nERROR: -list-file CANNOT BE READ: '"+listFile+"'");
            System.exit(1);
         }

         //..open and read the file
         BufferedReader file = new BufferedReader(new FileReader(listFile));
         inFileList = new ArrayList<String>(200);
         String line;
         while ((line = file.readLine()) != null) {
            if (line.trim().length() > 0) {
               inFileList.add(line.trim());
            }
         }
         log.info("Read {} entries from -list-file '{}'", inFileList.size(), listFile);

      } else if (inFileList == null) {
         inFileList = Arrays.asList(inDir.list());
         log.debug("inFileList: all files in directory. size = {}",
                   inFileList.size());
      }


      //................Loop through the files.....................

      for (String file : inFileList) {
         //.....open the output file...
         
         String inFileName = inDirName.concat(File.separator).concat(file);
         String outFileName = outDirName.concat(File.separator).concat(file).
                                            concat(".filecheck");
         log.info("input file: '"+inFileName+"'");
         log.info("results file: '"+outFileName+"'");

         ResultsFile out = null;

         try {
            out = new ResultsFile(doXml, outFileName, fcVersion, spVersion, inFileName);

         } catch (Exception e) {
            e.printStackTrace(stderr);

            stderr.println("\nERROR:Could not open output file: '"+outFileName+"'\n");
            stderr.println(e);
            log.error("results file could not be opened");
            System.exit(1);
         }

         //......open the input file.....
         argo = (ArgoDataFile) null;
         boolean openSuccessful = false;
         try {
            argo = ArgoDataFile.open(inFileName, specDirName, true, dacName);
         } catch (Exception e) {
            log.error("ArgoDataFile.open exception:\n"+e);
            e.printStackTrace(stderr);

            try {
               out.openError(e);
               out.close();

            } catch (Exception exc) {
               exc.printStackTrace(stderr);

               stderr.println("\nERROR: ResultsFile exception:");
               stderr.println(exc);
               log.error("results file exception");
               System.exit(1);
            }

            continue;
         }

         //..null file means it did not meet the min criteria to be an argo file
         if (argo == (ArgoDataFile) null) {
            log.error("ArgoDataFile.open failed: "+ArgoDataFile.getMessage());

            try {
               out.notArgoFile(dacName);
               out.close();

            } catch (Exception e) {
               e.printStackTrace(stderr);

               stderr.println("\nERROR: ResultsFile exception:");
               stderr.println(e);
               log.error("results file exception");
               System.exit(1);
            }

            continue;
         }

         openSuccessful = true;

         //.................check the format................

         boolean formatPassed = false;
         String phase = "FORMAT-VERIFICATION";

         if (! argo.verifyFormat(dacName)) {

            //..verifyFormat *failed* -- not format errors - an actual failure

            log.error("verifyFormat check failed: "+ArgoDataFile.getMessage());

            try {
               out.formatErrorMessage(phase);
               out.close();

            } catch (Exception e) {
               e.printStackTrace(stderr);

               stderr.println("\nERROR: ResultsFile exception:");
               stderr.println(e);
               log.error("results file exception");
               System.exit(1);
            }

            continue;
            
         } else {

            //..verifyFormat completed -- chech error/warning counts to determine status

            if (argo.nFormatErrors() == 0) {
               formatPassed = true;
               log.debug("format ACCEPTED");

            } else {
               formatPassed = false;
               log.debug("format REJECTED");
            }
         }

         //......SPECIAL CHECK for pre-v3.1 D-mode Profile file......

         if (argo.fileType() == FileType.PROFILE) {
            String dMode = argo.readString("DATA_MODE", true); //..true -> return NULLs if present
            if (dMode.charAt(0) == 'D') {
               String fv = argo.fileVersion();
               if (fv.compareTo("3.1") < 0) {

                  try {
                     out.oldDModeFile(dacName, fv);
                     out.close();
                     continue;

                  } catch (Exception exc) {
                     exc.printStackTrace(stderr);

                     stderr.println("\nERROR: ResultsFile exception:");
                     stderr.println(exc);
                     log.error("results file exception");
                     System.exit(1);
                  }
               }
            }
         }

         //...............check the data................

         boolean doDataCheck;
         boolean doRudimentaryDateCheck = false;

         if (formatPassed) {
            doDataCheck = true;
            
            if (doFormatOnly) {
               doDataCheck = false;
               log.debug("data check SKIPPED (-format-only)");

            } else if (doFormatOnlyPre31) {

               //..have to evaluate the version #
               //..gonna do this pretty simplistically for now
               log.debug("argo.fileVersion() = '{}'", argo.fileVersion());
               if (argo.fileVersion().compareTo("3.1") < 0) {
                  doDataCheck = false;
                  log.debug("data check SKIPPED");

                  //..format passed, NOT format-only
                  //..  requested format-only for pre-3.1 --> 
                  //..      implies data-checks for v3.1 and beyond
                  //..  need to do some rudimentary DATE checks on pre-3.1 files
                  doRudimentaryDateCheck = true;
               }
            }

         } else {
            doDataCheck = false;
            log.debug("data check SKIPPED (format rejected)");
         }

         if (doRudimentaryDateCheck) {
            //..passed format checks..validate the contents
            //..full data checks not performed because "early version"
            //..do a couple of rudimentary date checks

            argo.rudimentaryDateChecks();
         }//..end doRudimentaryDateCheck


         if (doDataCheck) {
            //..passed format checks..validate the contents

            phase = "DATA-VALIDATION";
            if (argo.fileType() == FileType.METADATA) {
               if (! ( (ArgoMetadataFile) argo).validate(dacName,
                                                         doNulls,
                                                         doBatteryChecks)) {
                  //..the validate process failed (not errors within the data)
                  log.error("ArgoMetadataFile.validate failed: "+
                            ArgoDataFile.getMessage());

                  try {
                     out.dataErrorMessage("Meta-data");
                     out.close();

                  } catch (Exception e) {
                     e.printStackTrace(stderr);

                     stderr.println("\nERROR: ResultsFile exception:");
                     stderr.println(e);
                     log.error("results file exception");
                     System.exit(1);
                  }

                  continue;
               }//..end if (meta-data validate)

            } else if (argo.fileType() == FileType.PROFILE ||
                       argo.fileType() == FileType.BIO_PROFILE) {
               if (! ( (ArgoProfileFile) argo).validate(false, dacName,
                                                        doNulls)) {
                  //..the validate process failed (not errors within the data)
                  log.error("ArgoProfileFile.validate failed: "+
                            ArgoDataFile.getMessage());

                  try {
                     out.dataErrorMessage("Profile");
                     out.close();

                  } catch (Exception e) {
                     e.printStackTrace(stderr);

                     stderr.println("\nERROR: ResultsFile exception:");
                     stderr.println(e);
                     log.error("results file exception");
                     System.exit(1);
                  }

                  continue;
               }//..end if (validate)

            } else if (argo.fileType() == FileType.TECHNICAL) {
               if (! ( (ArgoTechnicalFile) argo).validate(dacName,
                                                          doNulls)) {
                  //..the validate process failed (not errors within the data)
                  log.error("ArgoTechnicalFile.validate failed: "+
                            ArgoDataFile.getMessage());

                  try {
                     out.dataErrorMessage("Technical");
                     out.close();

                  } catch (Exception e) {
                     e.printStackTrace(stderr);

                     stderr.println("\nERROR: ResultsFile exception:");
                     stderr.println(e);
                     log.error("results file exception");
                     System.exit(1);
                  }


                  continue;
               }

            } else if (argo.fileType() == FileType.TRAJECTORY ||
                       argo.fileType() == FileType.BIO_TRAJECTORY) {
               if (! ( (ArgoTrajectoryFile) argo).validate(dacName, doNulls)) {
                  //..the validate process failed (not errors within the data)
                  log.error("ArgoTrajectoryFile.validate failed: "+
                            ArgoDataFile.getMessage());

                  try {
                     out.dataErrorMessage("Trajectory");
                     out.close();

                  } catch (Exception e) {
                     e.printStackTrace(stderr);

                     stderr.println("\nERROR: ResultsFile exception:");
                     stderr.println(e);
                     log.error("results file exception");
                     System.exit(1);
                  }

                  continue;
               }//..end if validate
            }//..end if fileType == ???

         }//..end if doDataCheck

         if (doNameCheck && argo.nFormatErrors() == 0) {
            //.."name check" requested and
            //..no other errors
            phase = "FILE-NAME-CHECK";

            argo.validateGdacFileName();
         }

         //...............report status and meta-data results...............
         //..status is that open was successful
         //..- that means identified as Argo netCDF file (DATA_TYPE and FORMAT_VERSION)
         //..- format may or may not have passed
         //..  - if format did not pass, trying to retrieve the numeric meta-data
         //..    may cause aborts -- i think string types are safe
         //..try to get as much of the meta-data as exists, but avoid aborts

         try {
            out.statusAndPhase((argo.nFormatErrors() == 0), phase);
            out.metaData(dacName, argo, formatPassed, doPsalStats);
            out.errorsAndWarnings(argo);
            out.close();
         } catch (Exception e) {
            e.printStackTrace(stderr);

            stderr.println("\nERROR: ResultsFile exception:");
            stderr.println(e);
            log.error("results file exception");
            System.exit(1);
         }


         argo.close();
      }//..end for (file)
      
   }//..end main

   
   public static void Help ()
   {
      stdout.println(
      "\n"+
      "Purpose: Validates the files in a directory\n"+
      "\n"+
      "Usage: java  "+ClassName+" [options] dac-name spec-dir output-dir input-dir [file-names]\n"+
      "Options:\n"+
      "   -help | -H | -U   Help -- this message\n"+
      "   -no-name-check Do not check the file name\n"+
      "   -null-warn     Perform 'nulls-in-string' check (warning)\n"+
      "                  default: do NOT check for nulls\n"+
      "   -text-result   Text-formatted results files\n"+
      "                  default: XML-formatted results files\n"+
      "   -list-file <list-file-path>  File containing list of files to process\n"+
      "			 default: no list-file (see Input Files below)\n"+
      "   -format-only   Only perform format checks to the files -- no data checks\n"+
      "                  default: perform format and data checks\n"+
      "   -data-check-all      Format and data checks for all files\n"+
      "                        default: Only perform format checks on pre-3.1 files\n"+
      "   -psal-stats    Put PSAL adjustment statistics into results file\n"+
      "                  default: don't compute this information\n"+
      "\n"+
      "   -format-only-pre3.1  (default) Only perform format checks on files format pre-3.1\n"+
      "      ***deprecated - now the default - retained for backwards compatibility***\n"+
      "\n"+
      "Arguments:\n"+
      "   dac-name       Name of DAC that owns the input files\n"+
      "   spec-dir       Directory path of specification files\n"+
      "   output-dir     Directory path where results files will be placed\n"+
      "   input-dir      Directory path where input files reside\n"+
      "   file-names     (Optional) List of files names to process (see below)\n"+
      "\n"+
      "Input Files:\n"+
      "   Input files to process are determined in one of the following ways (priority order):\n"+
      "   1) -list-file              List of names will be read from <list-file-path>\n"+
      "   2) [file-names] argument   Files listed on command-line will be processed\n"+
      "   3) All files in 'input-dir' will be processed\n"+
      "\n");
   }

}
