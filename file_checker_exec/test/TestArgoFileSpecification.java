import ucar.nc2.*;
import ucar.ma2.*;

import java.io.*;
import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import usgdac.*;
import usgdac.ArgoDataFile.FileType;

/**
 */
public class TestArgoFileSpecification
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

   private static final Class<?> ThisClass;
   private static final String ClassName;
   private static final Logger log;

   static {
      ThisClass = MethodHandles.lookup().lookupClass();
      ClassName = ThisClass.getSimpleName();

      System.setProperty("logfile.name", ClassName+"_LOG");
      log = LogManager.getLogger(ClassName);
   }


   //.............................................................
   //..                                                         ..
   //..                      main                               ..
   //..                                                         ..
   //.............................................................

   public static void main (String args[])
      throws IOException
   {
      //.....extract the options....
      int next;

      for (next = 0; next < args.length; next++) {
         if (args[next].equals("-help")) {
            Help();
            System.exit(0);
         } else if (args[next].startsWith("-")) {
            stderr.println("Invalid argument: '"+args[next]+"'");
            System.exit(1);
         } else {
            break;
         }
      }

      //.....parse the positional parameters.....

      if (args.length < (3 + next)) {
         log.error("too few arguments: "+args.length);
         Help();
         stderr.println("Too few arguments: "+args.length);
         System.exit(1);
      }
            
      String specDirName  = args[next++];
      String fileTypeName = args[next++];
      String specVersion  = args[next++];
      //String outFile      = args[next++];
      
      log.debug("specDirName  = '{}'", specDirName);
      log.debug("fileTypeName = '{}'", fileTypeName);
      log.debug("specVersion  = '{}'", specVersion);
      //log.debug("outFile      = '{}'", outFile);


      stdout.println("\n"+ClassName+" inputs:");
      stdout.println("   Specification directory: "+specDirName);
      stdout.println("   File Type:               "+fileTypeName);
      stdout.println("   Specification version:   "+specVersion);
      //stdout.println("   Output file:             "+outFile);

      //.....check the spec directory.....
      
      if (! (new File(specDirName).isDirectory())) {
         stderr.println("ERROR: Specification directory is not a directory ('"+
                        specDirName+"')");
         log.error("specification directory is not a directory");
         System.exit(1);
      }

      //.....set the file type.....
      FileType fileType = null;

      if (fileTypeName.equals("meta")) {
         fileType = FileType.METADATA;

      } else if (fileTypeName.equals("prof")) {
         fileType = FileType.PROFILE;

      } else if (fileTypeName.equals("traj")) {
         fileType = FileType.TRAJECTORY;

      } else if (fileTypeName.equals("tech")) {
         fileType = FileType.TECHNICAL;

      } else if (fileTypeName.equals("b-prof")) {
         fileType = FileType.BIO_PROFILE;

      } else if (fileTypeName.equals("b-traj")) {
         fileType = FileType.BIO_TRAJECTORY;

      } else {
         stderr.println("ERROR: Invalid file type specified: '"+fileTypeName+"'");
         System.exit(1);
      }

      //......open the specification.......

      ArgoFileSpecification spec = ArgoDataFile.openSpecification
         //full-spec            type      version  
         (true, specDirName, fileType, specVersion);

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
      "   -help\n"+
      "\n"+
      "Arguments:\n"+
      "   spec-dir       Directory to specification files\n" +
      "   file-type      meta, prof, tech, traj, b-prof, b-traj\n"+
      "   format-version Version of file format for the multi-profile file\n"+
      "\n");
      //"   out-file       Output file\n"+
      return;
   }

} //..end class
