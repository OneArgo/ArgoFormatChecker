import ucar.nc2.*;
import ucar.ma2.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import usgdac.*;

public class DumpTraj32
{

   public static void main (String args[])
      throws IOException
   {
      //.....extract the options....
      String listFile = null;
      int next;

      boolean ind_num = false;
      boolean only_cycle = false;
      boolean only_measure = false;
      boolean no_trailing = false;

      for (next = 0; next < args.length; next++) {
         if (args[next].equals("-help")) {
            Help();
            System.exit(0);
         } else if (args[next].equals("-index-number") ||
                    args[next].equals("-i")) {
            ind_num = true;
         } else if (args[next].equals("-n_measure") ||
                    args[next].equals("-m")) {
            only_measure = true;
         } else if (args[next].equals("-n_cycle") ||
                    args[next].equals("-c")) {
            only_cycle = true;
         } else if (args[next].equals("-no-trailing-missing") ||
                    args[next].equals("-b")) {
            no_trailing = true;
         } else if (args[next].equals("-list-file") ||
                    args[next].equals("-l")) {
            next++;
            if (next < args.length) {
               listFile = args[next];
            }
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

      log.info("DumpTraj:  START");
      
      if (args.length < (2 + next)) {
         log.error("too few arguments: "+args.length);
         Help();
         stderr.println("Too few arguments: "+args.length);
         System.exit(1);
      }
            
      String specDirName = args[next++];
      String outFileName = args[next++];

      List<String> inFileList = null;      //..list of input files
      if (next < args.length) {
         inFileList = new ArrayList<String>(args.length - next);
         for (; next < args.length; next++) inFileList.add(args[next]);
      }

      log.debug("outFileName = '"+outFileName+"'");
      log.debug("number of inFileList = "+
                (inFileList == null ? "null" : inFileList.size()));

      stdout.println("\nDumpTraj inputs:");
      stdout.println("   Output file name:        "+outFileName);

      //.........get list of input files........
      //..input files are chosen in the following priority order
      //..1) an input-file-list (overrides all other lists)
      //..2) file name arguments (already parsed above, if specified)

      if (listFile != null) {
         //..a list file was specified - open and read it
         //..this overrides all other "input lists"
         File f = new File(listFile);
         if (! f.isFile()) {
            stderr.println("\nERROR: -list-file does not exist: '"+listFile+"'");
            log.error("-list-file does not exist: '" + listFile + "'");
            System.exit(1);
         } else if (! f.canRead()) {
            log.error("-list-file '" + listFile + "' cannot be read");
            stderr.println("\nERROR: -list-file cannot be read: '"+listFile+"'");
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
         file.close();
         log.info("Read {} entries from -list-file '{}'", inFileList.size(), listFile);

         stdout.println("   Input files read from:   '"+listFile+"'");

      } else if (inFileList == null) {
         stderr.println("\nERROR: No input files specified\n");
         log.error("No input files specified");
         System.exit(1);

      } else {
         stdout.println("   Input files read from command line");
      }

      stdout.println("   Number of input files:   "+inFileList.size());

      //..output file
      //if ((new File(outFileName)).exists()) {
      //   stderr.println ("\nERROR: Output file MUST NOT EXIST :'" + outFileName + "'\n");
      //   log.error("Output file MUST NOT EXIST: '" + outFileName +"'");
      //   System.exit(1);
      //}

      //..create the output file
      PrintWriter outFile= new PrintWriter(new BufferedWriter
                      (new java.io.FileWriter(outFileName)));

      //.....write shtuff to the file.....
      log.debug(".....writing data to file.....");

      //.....loop over input files --- read from innie, write to outie

      int nFile = -1;

      stdout.println("\nProcessing File:");
      stdout.println(" FileNum  N_MEASUREMENT  N_CYCLE  N_PARAM  N_HISTORY  "+
            "N_CALIB_JULD  N_CALIB_PARAM  File Name");

      for (String inFileName : inFileList) {
         nFile++;

         ArgoTrajectoryFile arFile = null;
         try {
            arFile = ArgoTrajectoryFile.open(inFileName, specDirName, true);
         } catch (Exception e) {
            log.error("ArgoDataFile.open exception:\n"+e);
            stdout.println("ERROR: Exception opening Argo file:");
            e.printStackTrace(stderr);
            System.exit(1);
         }

         ArgoFileSpecification spec = arFile.getFileSpec();

         int nCalibParam   = arFile.getDimensionLength("N_CALIB_PARAM");
         int nCalibJuld   = arFile.getDimensionLength("N_CALIB_JULD");
         int nCycle   = arFile.getDimensionLength("N_CYCLE");
         int nHistory = arFile.getDimensionLength("N_HISTORY");
         int nMeasure = arFile.getDimensionLength("N_MEASUREMENT");
         int nParam   = arFile.getDimensionLength("N_PARAM");
         //int nValuesXX  = arFile.getDimensionLength("N_VALUESxx");

         if (log.isDebugEnabled()) {
            log.debug("N_CALIB_JULD = {}", nCalibJuld);
            log.debug("N_CALIB_PARAM = {}", nCalibParam);
            log.debug("N_CYCLE = {}", nCycle);
            log.debug("N_HISTORY = {}", nHistory);
            log.debug("N_MEASUREMENT = {}", nMeasure);
            log.debug("N_PARAM = {}", nParam);
            //log.debug("N_VALUESxx = {}", nValuesXX);
         }

         stdout.printf(" %6d %15d %8d %8d %10d  %s\n", 
                       nFile, nMeasure, nCycle, nParam, nHistory, 
                       nCalibParam, nCalibJuld, inFileName.trim());

         outFile.printf("\n===> "+inFileName.trim());

         String var;
         var="DATA_TYPE"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="FORMAT_VERSION"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="HANDBOOK_VERSION"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="REFERENCE_DATE_TIME"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="DATE_CREATION"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="DATE_UPDATE"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="PLATFORM_NUMBER"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="PROJECT_NAME"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="PI_NAME"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="DATA_CENTRE"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="DATA_STATE_INDICATOR"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="PLATFORM_TYPE"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="FLOAT_SERIAL_NO"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="FIRMWARE_VERSION"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="WMO_INST_TYPE"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));
         var="POSITIONING_SYSTEM"; outFile.printf("   %-25s  %s\n", var, arFile.readString(var));

         var="TRAJECTORY_PARAMETERS"; outFile.printf("   %-25s\n", var);
         
         ArrayList<String> paramList = new ArrayList<String>();
         for (int n = 0; n < nParam; n++) {
            int m = n;
            if (ind_num) m = n + 1;

            String v = arFile.readString(var, n).trim();

            outFile.printf("   %5d)  %s\n", m, v);
            paramList.add(v);
         }

         outFile.println("\n   ..........DIMENSIONS..........\n");

         outFile.printf("   %-25s  %10d\n", "N_CALIB_JULD", nCalibJuld);
         outFile.printf("   %-25s  %10d\n", "N_CALIB_PARAM", nCalibParam);
         outFile.printf("   %-25s  %10d\n", "N_CYCLE", nCycle);
         outFile.printf("   %-25s  %10d\n", "N_HISTORY", nHistory);
         outFile.printf("   %-25s  %10d\n", "N_MEASUREMENT", nMeasure);
         outFile.printf("   %-25s  %10d\n", "N_PARAM", nParam);
         

         outFile.println("\n   ..........N_MEASUREMENT VARIABLES..........\n");

         var = "CYCLE_NUMBER";
         int[] idata1 = arFile.readIntArr(var);
         int[] idata2;
         var = "MEASUREMENT_CODE";
         int[] idata3 = arFile.readIntArr(var);

         outFile.printf("        CYCLE_NUMBER/CYCLE_NUMBER_ADJUSTED/MEASUREMENT_CODE\n");

         var = "CYCLE_NUMBER_ADJUSTED";
         idata2 = arFile.readIntArr(var);

         for (int n = 0; n < nMeasure; n++) {
            int m = n;
            if (ind_num) m = n + 1;
                            
            outFile.printf("   %10d) %6d  %6d  %6d\n", m, 
                  idata1[n], idata2[n], idata3[n]);
         }

         var = "JULD";
         double[] data1 = arFile.readDoubleArr(var);
         String data1_code = arFile.readString(var+"_STATUS");
         String data1_qc = arFile.readString(var+"_QC");
         double[] data2;

         outFile.printf("   JULD variables  n)    MC   JULD/JULD_STATUS/JULD_QC | "+
               "JULD_ADJUSTED/JULD_ADJUSTED_STATUS/JULD_ADJUSTED_QC | JULD_DATA_MODE\n");

         var = "JULD_ADJUSTED";
         data2 = arFile.readDoubleArr(var);
         String data2_code = arFile.readString(var+"_STATUS");
         String data2_qc = arFile.readString(var+"_QC");

         var = "JULD_DATA_MODE";
         String data1_mode = arFile.readString(var);
         
         for (int n = 0; n < nMeasure; n++) {
            int m = n;
            if (ind_num) m = n + 1;
                            
            outFile.printf("   %17d) %5d %15.6f  '%1s'  '%1s'  |  %15.6f  '%1s'  '%1s' | '%1s'\n",
                  m, idata3[n],
                  data1[n], data1_code.charAt(n), data1_qc.charAt(n),
                  data2[n], data2_code.charAt(n), data2_qc.charAt(n),
                  data1_mode.charAt(n));
         }

         var = "LATITUDE";
         data1 = arFile.readDoubleArr(var);

         var = "LONGITUDE";
         data2 = arFile.readDoubleArr(var);

         var = "POSITION";
         data1_qc = arFile.readString(var+"_QC");
         data1_code = arFile.readString(var+"_ACCURACY");

         outFile.printf("   LATITUDE/LONGITUDE/POSITION_QC/POSITION_ACCURACY");
         for (int n = 0; n < nMeasure; n++) {
            int m = n;
            if (ind_num) m = n + 1;
                            
            outFile.printf("   %17d) %15.6f %15.6f  '%1s'  '%1s'\n",
                           m, data1[n], data2[n], data1_qc.charAt(n), data1_code.charAt(n));
         }

         //..get the traj_param_data_mode
         
         String[] trajPrmDM = arFile.readStringArr("TRAJECTORY_PARAMETER_DATA_MODE");
         if (trajPrmDM == null) {
            log.debug("TRAJECTORY_PARAMETER_DATA_MODE missing from file");
            stdout.printf("*** TRAJECTORY_PARAMETER_DATA_MODE missing from file\n");
         }

         //......physical parameter variables..........

         for (String pName : spec.getPhysicalParamNames()) {

            Variable v = arFile.findVariable(pName);
            if (v == null) {
               continue;
            }

            double[] dprm = null;
            float[]  fprm = null;
            short[]  sprm = null;
            String   prm_qc = null;
            double[] dprm_adj = null;
            float[]  fprm_adj = null;
            short[]  sprm_adj = null;
            double[] dprm_adj_err = null;
            float[]  fprm_adj_err = null;
            short[]  sprm_adj_err = null;
            String   prm_adj_qc = null;

            DataType type = v.getDataType();

            if (type == DataType.FLOAT) {
               fprm         = arFile.readFloatArr(pName);
               fprm_adj     = arFile.readFloatArr(pName+"_ADJUSTED");
               fprm_adj_err = arFile.readFloatArr(pName+"_ADJUSTED_ERROR");
               
            } else if (type == DataType.DOUBLE) {

               dprm = arFile.readDoubleArr(pName);
               dprm_adj = arFile.readDoubleArr(pName+"_ADJUSTED");
               dprm_adj_err = arFile.readDoubleArr(pName+"_ADJUSTED_ERROR");
               
            } else if (type == DataType.SHORT) {

               sprm = arFile.readShortArr(pName);
               sprm_adj = arFile.readShortArr(pName+"_ADJUSTED");
               sprm_adj_err = arFile.readShortArr(pName+"_ADJUSTED_ERROR");

            } else {
               stdout.println("\n"+pName+": Unexpected data type = "+type);
               continue;
            }

            prm_qc = arFile.readString(pName+"_QC");
            prm_adj_qc = arFile.readString(pName+"_ADJUSTED_QC");

            stdout.println(pName);

            int pNdx = paramList.indexOf(pName);
            if (pNdx < 0) {
               stdout.printf("*** Parameter List does not contain '%s'\n", pName);
            }
            log.debug("pNdx = {}", pNdx);
            
            outFile.printf("   %-10s %11s / QC | %s_ADJUSTED / _ERROR / _QC | TRAJ_PARAM_DATA_MODE\n",
                           pName, pName, pName);

            if (dprm != null) {
               if (dprm_adj == null) {
                  for (int n = 0; n < dprm.length; n++) {
                     int m = n;
                     if (ind_num) m = n + 1;
                            
                     outFile.printf("   %10d) %10.4f  '%1s' | ----------  ----------  '-' | '%1s'\n",
                                 m, dprm[n], prm_qc.charAt(n), trajPrmDM[n].charAt(pNdx));
                  }

               } else {
                  for (int n = 0; n < dprm.length; n++) {
                     int m = n;
                     if (ind_num) m = n + 1;
                            
                     outFile.printf("   %10d) %10.4f  '%1s' | %10.4f  %10.4f  '%1s' | '%1s'\n",
                                 m, dprm[n], prm_qc.charAt(n), 
                                 dprm_adj[n], dprm_adj_err[n], prm_adj_qc.charAt(n),
                                 trajPrmDM[n].charAt(pNdx));
                  }
               }

            } else if (fprm != null) {
                  if (fprm_adj == null) {
                     for (int n = 0; n < fprm.length; n++) {
                        int m = n;
                        if (ind_num) m = n + 1;
                               
                        outFile.printf("   %10d) %10.4f  '%1s' | ----------  ----------  '-' | '%1s'\n",
                                    m, fprm[n], prm_qc.charAt(n), trajPrmDM[n].charAt(pNdx));
                     }

                  } else {
                     for (int n = 0; n < fprm.length; n++) {
                        int m = n;
                        if (ind_num) m = n + 1;
                               
                        outFile.printf("   %10d) %10.4f  '%1s' | %10.4f  %10.4f  '%1s' | '%1s'\n",
                                    m, fprm[n], prm_qc.charAt(n), 
                                    fprm_adj[n], fprm_adj_err[n], prm_adj_qc.charAt(n), 
                                    trajPrmDM[n].charAt(pNdx));
                     }
                  }


            } else if (sprm != null) {
                  if (sprm_adj == null) {
                     for (int n = 0; n < sprm.length; n++) {
                        int m = n;
                        if (ind_num) m = n + 1;
                               
                        outFile.printf("   %10d) %10d  '%1s' | ----------  ----------  '-' | '%1s'\n",
                                    m, sprm[n], prm_qc.charAt(n), 
                                    trajPrmDM[n].charAt(pNdx));
                     }

                  } else {
                     for (int n = 0; n < sprm.length; n++) {
                        int m = n;
                        if (ind_num) m = n + 1;
                               
                        outFile.printf("   %10d) %10d  '%1s' | %10d  %10d  '%1s' | '%1s'\n",
                                    m, sprm[n], prm_qc.charAt(n), 
                                    sprm_adj[n], sprm_adj_err[n], prm_adj_qc.charAt(n), 
                                    trajPrmDM[n].charAt(pNdx));
                     }
                  }
            }
         }



         /***********************************************************************
          *                        N_CYCLE
          ***********************************************************************/

         outFile.println("\n   ..........N_CYCLE VARIABLES..........\n");


         var = "CYCLE_NUMBER_INDEX";
         idata1 = arFile.readIntArr(var);

         var = "CYCLE_NUMBER_INDEX_ADJUSTED";
         idata2 = arFile.readIntArr(var);

         var = "DATA_MODE";
         String str1 = arFile.readString(var);

         var = "CONFIG_MISSION_NUMBER";
         idata3 = arFile.readIntArr(var);

         var = "GROUNDED";
         String str2 = arFile.readString(var);


         outFile.println("        CYCLE_NUMBER_INDEX / CYCLE_NUMBER_INDEX_ADJUSTED / DATA_MODE"+
                         " / CONFIG_MISSION_NUMER / GROUNDED");
         for (int n = 0; n < nCycle; n++) {
            int m = n;
            if (ind_num) m = n + 1;
                            
            outFile.printf("   %10d) %6d  %6d  '%1s'  %6d  '%1s'\n", m, 
                           idata1[n], idata2[n], str1.charAt(n), idata3[n], str2.charAt(n));
         }

         for (ArgoReferenceTable.ArgoReferenceEntry e : ArgoReferenceTable.MEASUREMENT_CODE_toJuldVariable.values()) {
            //outFile.println(e.getColumn(1)+" "+e.getColumn(2));
            String v = e.getColumn(2);

            log.debug("...reading '{}'", e);

            double[] juld = arFile.readDoubleArr(v);
            str1 = arFile.readString(v+"_STATUS");

            outFile.println("   "+v+"/"+v+"_STATUS");
            for (int n = 0; n < nCycle; n++) {
               int m = n;
               if (ind_num) m = n + 1;
                            
               outFile.printf("   %10d) %15.6f  '%1s'\n", m, 
                              juld[n], str1.charAt(n));
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
      "Purpose: Dumps Argo Trajectory v3.2 files\n" +
      "\n"+
      "Usage: java DumpTraj [options] spec-dir "+
                                     "dump-file input-files ...\n"+
      "\n"+
      "Options:\n"+
      "   -list-file <list-file-path>  File containing list of files to process\n"+
      "     -l\n"+
      "   -n_measure    (-m)       List only N_MEASUREMENT variables\n"+
      "   -n_cycle      (-c)       List only N_CYCLE variables\n"+
      "   -index-number (-i)       List index numbers (1-based; not 'array indices')\n"+
      "                            default: list array indices\n"+
      "   -no-trailing-missing (-b)   Do not print missing values at the end of a trajectory\n"+
      "      ***NO-OP CURRENTLY***\n"+
      "\n"+
      "Arguments:\n"+
      "   spec-dir       Directory to specification files\n" +
      "   dump-file      Output file (must not exist)\n" +
      "   input-files    Input Argo NetCDF profile file(s)\n"+
      "\n"+
      "Input Files:\n"+
      "   Names of input files are determined as follows (priority order):\n"+
      "   1) -list-file              List of names will be read from <list-file-path>\n"+
      "   2) [file-names] argument   Files listed on command-line will be processed\n"+
      "\n");
      return;
   }

   //......................Variable Declarations................

   static {
      System.setProperty("logfile.name", "Dumper_LOG");
   }

   //..standard i/o shortcuts
   static PrintStream stdout = new PrintStream(System.out);
   static PrintStream stderr = new PrintStream(System.err);

   private static final Logger log = LogManager.getLogger("DumpProfile");

   //..class variables
   static String     outFileName;
   static String     specFileName;

   //static boolean addHistory = false;
   static HashMap<String, Integer> n_prof;
   static HashMap<String, Integer> n_param;
   static HashMap<String, Integer> n_levels;
   static HashMap<String, Integer> n_calib;
   //static HashMap<String, Integer> n_history;
   static Set<String> parameters = new HashSet<String>();

   static int INT_MAX = Integer.MAX_VALUE;
   static double DBL_NAN = Double.NaN;
} //..end class
