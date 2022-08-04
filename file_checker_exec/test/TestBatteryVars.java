import usgdac.*;
import usgdac.ArgoDataFile.FileType;

import java.io.*;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tests the validateBattery method of ArgoMetadataFile
 *
 * @author Mark Ignaszewski
 * @version  $Id: ValidateSubmit.java 372 2015-12-02 19:02:31Z ignaszewski $
 */
public class TestBatteryVars
{

   //......................Variable Declarations................

   //..standard i/o shortcuts
   static PrintStream stdout = new PrintStream(System.out);
   static PrintStream stderr = new PrintStream(System.err);

   //..log file
   static final Class<?> ThisClass;
   static final String ClassName;

   private static final Logger log;

   static {
      ThisClass = MethodHandles.lookup().lookupClass();
      ClassName = ThisClass.getSimpleName();

      System.setProperty("logfile.name", ClassName+"_LOG");

      log = LogManager.getLogger(ClassName);
   }


   public static void main (String args[])
      throws IOException
   {

      String spec_dir = null;

      //.....extract the options....
      for (String file : args) {
         if (file.equals("-help")) {
            Help();
            System.exit(0);
         }

         if (spec_dir == null) {
            spec_dir = file;
            stdout.println("\n\n==> SPEC-DIR: "+spec_dir);
            continue;
         }

         stdout.println("\n\n==> FILE: "+file);

         ArgoDataFile argo = (ArgoDataFile) null;

         try {
            argo = ArgoDataFile.open(file, spec_dir, true);

         } catch (Exception e) {
            stdout.println("ArgoDataFile.open exception:\n"+e);
            e.printStackTrace(stdout);

            continue;
         }

         //..null file means it did not meet the min criteria to be an argo file
         if (argo == (ArgoDataFile) null) {
            stdout.println("ArgoDataFile.open failed: "+ArgoDataFile.getMessage());
            continue;
         }

         if (argo.fileType() != FileType.METADATA) {
            stdout.println("Who you trying to kid?  This isn't a metadata file.");
            continue;
         }
 
         ArgoMetadataFile meta = (ArgoMetadataFile) argo;
         meta.validateBattery();

         //.....print diagnostics......

         int nMessage = 0;
         for (String str: meta.formatErrors()) {
            stdout.println(file+": ERROR:   "+str);
            nMessage++;
         }

         for (String str: meta.formatWarnings()) {
            stdout.println(file+": WARNING: "+str);
            nMessage++;
         }

         if (nMessage == 0) {
            stdout.println(file+": no messages");
         }

         //..reset
         meta.clearFormatErrors();
         meta.clearFormatWarnings();

      } //..end for (file)
   }
 
  
   public static void Help ()
   {
      stdout.println(
      "\n"+
      "Purpose: Tests the 'BATTERY validation' of ArgoMetadataFile\n"+
      "\n"+
      "Usage: java TestBattery spec-dir file-names...\n"+
      "Options:\n"+
      "   -help | -H | -U   Help -- this message\n"+
      "\n");
   }

}
