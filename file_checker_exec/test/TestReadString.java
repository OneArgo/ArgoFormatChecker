import ucar.nc2.*;
import ucar.ma2.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import usgdac.*;

public class TestReadString
{

   public static void main (String args[])
      throws IOException
   {
      //.....extract the options....
      int next;

      for (next = 0; next < args.length; next++) {
         if (args[next].equals("-help")) {
            Help();
            System.exit(0);
         //} else if (args[next].startsWith("-inc-history")) {
         //   addHistory = true;
         } else if (args[next].startsWith("-")) {
            stderr.println("Invalid argument: '"+args[next]+"'");
            System.exit(1);
         } else {
            break;
         }
      }

      //.....parse the positional parameters.....

      log.info("TestReadString:  START");
      
      if (args.length < (2 + next)) {
         log.error("too few arguments: "+args.length);
         Help();
         stderr.println("Too few arguments: "+args.length);
         System.exit(1);
      }
            
      String outFileName = args[next++];

      List<String> inFileList = null;      //..list of input files
      if (next < args.length) {
         inFileList = new ArrayList<String>(args.length - next);
         for (; next < args.length; next++) inFileList.add(args[next]);
      }

      log.debug("outFileName = '"+outFileName+"'");
      log.debug("number of inFileList = "+
                (inFileList == null ? "null" : inFileList.size()));

      stdout.println("\nTestReadString inputs:");
      stdout.println("   Output file name:        "+outFileName);

      //.........get list of input files........
      //..input files are chosen in the following priority order
      //..1) an input-file-list (overrides all other lists)
      //..2) file name arguments (already parsed above, if specified)

      if (inFileList == null) {
         stderr.println("\nERROR: No input files specified\n");
         log.error("No input files specified");
         System.exit(1);

      } else {
         stdout.println("   Input files read from command line");
      }

      stdout.println("   Number of input files:   "+inFileList.size());

      //..create the output file
      PrintWriter outFile= new PrintWriter(new BufferedWriter(
                                      new java.io.FileWriter(outFileName)));

      //.....write shtuff to the file.....
      log.debug(".....writing data to file.....");

      //.....loop over input files --- read from innie, write to outie

      int nFile = -1;

      stdout.println("\nProcessing File:");
      stdout.println(" FileNum  File Name");

      for (String inFileName : inFileList) {
         nFile++;

         stdout.printf("%8d  %s\n", nFile, inFileName);
         outFile.printf("\n=========== FILE: %s (%d)\n", inFileName, nFile);

         ArgoProfileFile arFile = null;

         try {
            arFile = ArgoProfileFile.open(inFileName);
         } catch (Exception e) {
            log.error("ArgoDataFile.open exception:\n"+e);
            stdout.println("ERROR: Exception opening Argo file:");
            e.printStackTrace(stderr);
            System.exit(1);
         }


         {
            outFile.println("\n*************** readString(name) ****************\n");

            String[] VAR = {"DATA_TYPE", "FORMAT_VERSION", "HANDBOOK_VERSION", 
                            "DIRECTION", "DATA_MODE", "JULD_QC"};

            for (String var : VAR) {

               String str  = arFile.readString(var);
               String strN = arFile.readString(var, true);

               if (str.length() != strN.length()) {
                  outFile.println("NULLS detected: "+var);
               }

               outFile.printf("%-20s: length = %5d: '%s'\n", var, str.length(), str);
               outFile.printf("%-20s: length = %5d: '%s'\n\n", var, strN.length(), strN);
            }
         }

         {
            outFile.println("\n*************** readString(name, n) ****************\n");

            //int nProf = arFile.getDimensionLength("N_PROF");

            String[] VAR = {"PLATFORM_NUMBER", "PROJECT_NAME", "PI_NAME",
                            "DATA_STATE_INDICATOR", "WMO_INST_TYPE", "DATA_CENTRE"};


            for (String var : VAR) {

               String str  = arFile.readString(var, 0);
               String strN = arFile.readString(var, 0, true);

               if (str == null) {
                  stdout.println(">> readString("+var+",0): Failed");
                  outFile.println(">> readString("+var+",0): Failed");
                  continue;
               }
               if (strN == null) {
                  stdout.println(">> readString("+var+",0, true): Failed");
                  outFile.println(">> readString("+var+",0, true): Failed");
                  continue;
               }

               if (str.length() != strN.length()) {
                  outFile.println("NULLS detected: "+var);
               }

               outFile.printf("%-20s[0]: length = %5d: '%s'\n", var, str.length(), str);
               outFile.printf("%-20s[0]: length = %5d: '%s'\n\n", var, strN.length(), strN);
            }
         }


         {
            outFile.println("\n********** readString(name, n, m) ************\n");

            String[] VAR = {"STATION_PARAMETERS", "HISTORY_INSTITUTION", 
                            "HISTORY_DATE", "HISTORY_PARAMETER"};


            for (String var : VAR) {

               String str  = arFile.readString(var, 0, 0);
               String strN = arFile.readString(var, 0, 0, true);

               if (str == null) {
                  stdout.println(">> readString("+var+",0,0): Failed");
                  outFile.println(">> readString("+var+",0,0): Failed");
                  continue;
               }
               if (strN == null) {
                  stdout.println(">> readString("+var+",0,0, true): Failed");
                  outFile.println(">> readString("+var+",0,0, true): Failed");
                  continue;
               }

               if (str.length() != strN.length()) {
                  outFile.println("NULLS detected: "+var);
               }

               outFile.printf("%-20s[0]: length = %5d: '%s'\n", var, str.length(), str);
               outFile.printf("%-20s[0]: length = %5d: '%s'\n\n", var, strN.length(), strN);
            }
         }


         {
            outFile.println("\n********** readString(name, n, m, k) ************\n");

            String[] VAR = {"PARAMETER", "SCIENTIFIC_CALIB_EQUATION",
                            "SCIENTIFIC_CALIB_DATE", "SCIENTIFIC_CALIB_COMMENT"};


            for (String var : VAR) {

               String str  = arFile.readString(var, 0, 0, 0);
               String strN = arFile.readString(var, 0, 0, 0, true);

               if (str == null) {
                  stdout.println(">> readString("+var+",0,0,0): Failed");
                  outFile.println(">> readString("+var+",0,0,0): Failed");
                  continue;
               }
               if (strN == null) {
                  stdout.println(">> readString("+var+",0,0,0, true): Failed");
                  outFile.println(">> readString("+var+",0,0,0, true): Failed");
                  continue;
               }

               if (str.length() != strN.length()) {
                  outFile.println("NULLS detected: "+var);
               }

               outFile.printf("%-20s[0]: length = %5d: '%s'\n", var, str.length(), str);
               outFile.printf("%-20s[0]: length = %5d: '%s'\n\n", var, strN.length(), strN);
            }
         }


         {
            outFile.println("\n********** readStringArr(name) ************\n");

            String[] VAR = {"PLATFORM_NUMBER", "WMO_INST_TYPE", "DATA_CENTRE"};


            for (String var : VAR) {

               String[] str  = arFile.readStringArr(var);
               String[] strN = arFile.readStringArr(var, true);

               if (str == null) {
                  stdout.println(">> readStringArr("+var+"): Failed");
                  outFile.println(">> readStringArr("+var+"): Failed");
                  continue;
               }
               if (strN == null) {
                  stdout.println(">> readStringArr("+var+", true): Failed");
                  outFile.println(">> readStringArr("+var+", true): Failed");
                  continue;
               }


               outFile.printf("%-20s:\n", var);

               for (int n = 0; n < str.length; n++) {
                  if (str[n].length() != strN[n].length()) {
                     outFile.println("Array NULLS detected: "+var);
                  }

                  outFile.printf(" %5d) length = %5d: '%s'\n", n, str[n].length(), str[n]);
                  outFile.printf("        length = %5d: '%s'\n\n", strN[n].length(), strN[n]);
               }
            }
         }


         {
            outFile.println("\n********** readStringArr(name, n) ************\n");

            String[] VAR = {"STATION_PARAMETERS", "HISTORY_INSTITUTION", 
                            "HISTORY_DATE", "HISTORY_PARAMETER"};

            for (String var : VAR) {

               String[] str  = arFile.readStringArr(var, 0);
               String[] strN = arFile.readStringArr(var, 0, true);

               if (str == null) {
                  stdout.println(">> readStringArr("+var+",0): Failed");
                  outFile.println(">> readStringArr("+var+",0): Failed");
                  continue;
               }
               if (strN == null) {
                  stdout.println(">> readStringArr("+var+",0, true): Failed");
                  outFile.println(">> readStringArr("+var+",0, true): Failed");
                  continue;
               }


               outFile.printf("%-20s[0]:\n", var);

               for (int n = 0; n < str.length; n++) {
                  if (str[n].length() != strN[n].length()) {
                     outFile.println("Array NULLS detected: "+var);
                  }

                  outFile.printf(" %5d) length = %5d: '%s'\n", n, str[n].length(), str[n]);
                  outFile.printf("        length = %5d: '%s'\n\n", strN[n].length(), strN[n]);
               }
            }
         }



         {
            outFile.println("\n********** readStringArr(name, n, m) ************\n");

            String[] VAR = {"PARAMETER", "SCIENTIFIC_CALIB_EQUATION",
                            "SCIENTIFIC_CALIB_DATE", "SCIENTIFIC_CALIB_COMMENT"};

            for (String var : VAR) {

               String[] str  = arFile.readStringArr(var, 0, 0);
               String[] strN = arFile.readStringArr(var, 0, 0, true);

               if (str == null) {
                  stdout.println(">> readStringArr("+var+",0,0): Failed");
                  outFile.println(">> readStringArr("+var+",0,0): Failed");
                  continue;
               }
               if (strN == null) {
                  stdout.println(">> readStringArr("+var+",0,0, true): Failed");
                  outFile.println(">> readStringArr("+var+",0,0, true): Failed");
                  continue;
               }


               outFile.printf("%-20s[0,0]:\n", var);

               for (int n = 0; n < str.length; n++) {
                  if (str[n].length() != strN[n].length()) {
                     outFile.println("Array NULLS detected: "+var);
                  }

                  outFile.printf(" %5d) length = %5d: '%s'\n", n, str[n].length(), str[n]);
                  outFile.printf("        length = %5d: '%s'\n\n", strN[n].length(), strN[n]);
               }
            }
         }


         arFile.close();
      } //..end for (inFileName)


      //..............end of data writes.....................

      outFile.close();

   } //..end main


   //...................................
   //..              Help             ..
   //...................................
   public static void Help ()
   {
      stdout.println (
      "\n"+
      "Purpose: Dumps Argo Profile files\n" +
      "\n"+
      "Usage: java TestReadString [options] output-file profile-files ...\n"+
      "\n"+
      "Options:\n"+
      "\n"+
      "Arguments:\n"+
      "   output-file      Output file\n" +
      "   profile-files    Input Argo NetCDF profile file(s)\n"+
      "\n");
      return;
   }

   //......................Variable Declarations................

   //..standard i/o shortcuts
   static PrintStream stdout = new PrintStream(System.out);
   static PrintStream stderr = new PrintStream(System.err);

   static {
      System.setProperty("logfile.name", "TestReadString_LOG");
   }

   static Logger log = LoggerFactory.getLogger("TestReadString");

   //..class variables
   static String     outFileName;

} //..end class
