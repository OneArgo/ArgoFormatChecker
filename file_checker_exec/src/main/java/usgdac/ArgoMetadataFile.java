package usgdac;

import ucar.nc2.*;
import ucar.ma2.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Extends ArgoDataFile with features specific to an Argo Meta-data file.
 * <p>
 * A new file can be opened using the constructor. Opening a new file will 
 * create an Argo-compliant "template" with all of the fixed values filled in.
 * Float-specific values will (obviously) have to be added by the user.
 * <p>
 * An existing file is opened using "open".
 * <p>
 * The format of the files can be checked with "verifyFormat" (see ArgoDataFile).
 * The data consistency can be validated with "validate".
 * <p>
 * @version  $HeadURL: https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoMetadataFile.java $
 * @version  $Id: ArgoMetadataFile.java 1314 2022-04-13 00:20:48Z ignaszewski $
 */

public class ArgoMetadataFile extends ArgoDataFile
{
   
   //.........................................
   //             VARIABLES
   //.........................................

   //..class variables
   //..standard i/o shortcuts
   private static final Logger log = LogManager.getLogger("ArgoMetadataFile");

   private final static long oneDaySec = 1L * 24L * 60L * 60L * 1000L;

   private static Pattern pBatteryType;
   private static Pattern pBatteryPacks;

   static {
      //..example:  TADIRAN Alkaline 12 V
      //..  regex description:
      //..           leading spaces allowed                   space(s)         space(s)      space(s)    space(s) [spaces(s)]
      //..                                     word-char(s)          word-char(s)    digit(s)[.digit(s)]]     "V"
      pBatteryType  = Pattern.compile("\\s*(?<manufacturer>\\w+)\\s+(?<type>\\w+)\\s+(?<volts>\\d+\\.?\\d*)\\s+V\\s*");

      //..example:  4DD Li
      //..  regex description:
      //..           leading spaces allowed                                space(s)           [space(s)]
      //..                                     digit(s)         word-char(s)     word-char(s) 
      pBatteryPacks = Pattern.compile("\\s*(?<numofpacks>\\d+)(?<style>\\w+)\\s+(?<type>\\w+)\\s*");
   }
   
   //..object variables
   
   //.......................................
   //          CONSTRUCTORS
   //.......................................

   protected ArgoMetadataFile() throws IOException
   {
      super();
   }

   public ArgoMetadataFile (String specDir, String version)
   {
      //super(specDir, FileType.METADATA, version);
   }

   //..........................................
   //              METHODS
   //..........................................

   /**
    * Convenience method to add to String list for "pretty printing".
    *
    * @param list the StringBuilder list
    * @param add the String to add 
    */
   private void addToList (StringBuilder list, String add)
   {
      if (list.length() == 0) {
         list.append("'"+add+"'");
      } else {
         list.append(", '"+add+"'");
      }
   }
   
   /**
    * Convenience method to add to String list for "pretty printing".
    *
    * @param list the StringBuilder list
    * @param add the String to add 
    */
   private void addToList (StringBuilder list, int add)
   {
      if (list.length() == 0) {
         list.append(add);
      } else {
         list.append(", "+add);
      }
   }
   
   /**
    * Opens an existing file and the assoicated <i>Argo specification</i>).
    *
    * @param inFile the string name of the file to open
    * @param specDir the string name of the directory containing the format 
    *                specification files
    * @param fullSpec  true = open the full specification;
    *                  false = open the template specification
    * @return the file object reference. Returns null if the file is not opened
    *         successfully. 
    *         (ArgoMetadataFile.getMessage() will return the reason for the 
    *         failure to open.)
    * @throws IOException If an I/O error occurs
    */
   public static ArgoMetadataFile open (String inFile, String specDir, boolean fullSpec)
   throws IOException
   {
      ArgoDataFile arFile = ArgoDataFile.open(inFile, specDir, fullSpec);
      if (! (arFile instanceof ArgoMetadataFile)) {
         message = "ERROR: '"+inFile+"' not an Argo META-DATA file";
         return (ArgoMetadataFile) null;
      }
	  
      return (ArgoMetadataFile) arFile;
   }

   /**
    * Validates the data in the meta-data file.
    * This is a driver routine that performs all 
    * types of validations (see other validate* routines).  
    * 
    * <p>Performs the following validations:
    * <ol>
    * <li>validateStringNulls -- if ckNulls = true
    * <li>validateDates
    * <li>(version 2.2 and earlier) validateHighlyDesirable_v2
    * <li>(version 3+) validateMandatory_v3
    * <li>validateBattery
    * <li>validateConfigMission
    * <li>validateConfigParams
    *
    * <p>Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings, 
    * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
    *
    * @param dacName name of the DAC for this file
    * @param ckNulls  true = check all strings for NULL values; false = skip
    * @return success indicator. true - validation was performed.  
    *                            false - validation could not be performed
    *                             (getMessage() will return the reason).
    * @throws IOException If an I/O error occurs
    */
   public boolean validate (String dacName, boolean ckNulls, boolean ... optionalChecks)
      throws IOException
   {
      ArgoReferenceTable.DACS dac = (ArgoReferenceTable.DACS) null;
      
      if (! verified) {
         message = new String("File must be verified (verifyFormat) "+
                              "successfully before validation");
         return false;
      }

      //.......check arguments.......
      if (dacName.trim().length() > 0) { 
         for (ArgoReferenceTable.DACS d : ArgoReferenceTable.DACS.values()) {
            if (d.name.equals(dacName)) {
               dac = d;
               break;
            }
         }
         if (dac == (ArgoReferenceTable.DACS) null) {
            message = new String("Unknown DAC name = '" + dacName + "'");
            return false;
         }
      }

      //.......do validations..........

      this.dacName = dacName;

      if (ckNulls) {
         validateStringNulls();
      }

      validateDates();

      if (format_version.trim().compareTo("2.2") <= 0) {
         validateHighlyDesirable_v2(dac);
      } else {
         validateMandatory_v3(dac);
         validateConfigMission();
         validateConfigParams();

         if ((optionalChecks.length > 0) && (optionalChecks[0] == true)) {
            validateBattery();
         }
      }
      
      return true;
   }//..end validate
   
   /**
    * Validates the dates in the meta-data file.
    * 
    * Date Checks
    * <ul>
    * <li>DATE_CREATION: Must be set; not before earliest Argo date; before current time
    * <li>DATE_UPDATE: Must be set; not before DATE_CREATION; before current time
    * <li>LAUNCH_DATE: After earliest Argo date
    * <li>START_DATE: Not before LAUNCH_DATE (warning); if set, LAUNCH_DATE set
    * <li>STARTUP_DATE: Within 3 days of LAUNCH_DATE (warning); if set, LAUNCH_DATE set
    * <li>END_MISSION_DATE: Not before LAUNCH_DATE; if set, LAUNCH_DATE set
    * </ul>
    * <p>Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings, 
    * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
    *
    * @throws IOException If an I/O error occurs
    */

   public void validateDates ()
      throws IOException
   {
      log.debug(".....validateDates: start.....");

      //..read times 

      String creation = readString("DATE_CREATION").trim();
      String update = readString("DATE_UPDATE").trim();
      String launch = readString("LAUNCH_DATE").trim();
      String start = readString("START_DATE").trim();
      String end = readString("END_MISSION_DATE").trim();

      //..version specific dates

      Variable var = ncReader.findVariable("STARTUP_DATE");
      String startup;

      if (var != (Variable) null) {
         startup = ( (ArrayChar) var.read()).getString();
      } else {
         startup = " ";
      }

      Date fileTime = new Date(file.lastModified());
      long fileSec = fileTime.getTime();

      if (log.isDebugEnabled()) { 
         log.debug("earliestDate:     '{}'", ArgoDate.format(earliestDate));
         log.debug("fileTime:         '{}'", ArgoDate.format(fileTime));
         log.debug("DATE_CREATION:    '{}'", creation);
         log.debug("DATE_UPDATE:      '{}'", update);
         log.debug("LAUNCH_DATE:      '{}'", launch);
         log.debug("START_DATE:       '{}' length = {}", start, start.length());
         log.debug("STARTUP_DATE:     '{}'", startup);
         log.debug("END_MISSION_DATE: '{}'", end);
      }

      //...........creation date checks:.............
      //..set, after earliestDate, and before file time
      Date dateCreation = (Date) null;
      boolean haveCreation = false;
      long creationSec = 0;

      if (creation.trim().length() <= 0) {
         formatErrors.add("DATE_CREATION: Not set");

      } else {
         dateCreation = ArgoDate.get(creation);
         haveCreation = true;

         if (dateCreation == (Date) null) {
            haveCreation = false;
            formatErrors.add("DATE_CREATION: '"+creation+"': Invalid date");

         } else {
            creationSec = dateCreation.getTime();
            
            if (dateCreation.before(earliestDate)) {
               formatErrors.add("DATE_CREATION: '"+creation+
                  "': Before earliest allowed date ('"+ArgoDate.format(earliestDate)+"')");

            } else if ((creationSec - fileSec) > oneDaySec) {
               formatErrors.add("DATE_CREATION: '"+creation+
                  "': After GDAC receipt time ('"+ArgoDate.format(fileTime)+"')");
            }
         }
      }

      //............update date checks:...........
      //..set, not before creation time, before file time
      Date dateUpdate = (Date) null;
      boolean haveUpdate = false;
      long updateSec = 0;

      if (update.trim().length() <= 0) {
         formatErrors.add("DATE_UPDATE: Not set");
      } else {
         dateUpdate = ArgoDate.get(update);
         haveUpdate = true;

         if (dateUpdate == (Date) null) {
            formatErrors.add("DATE_UPDATE: '"+update+"': Invalid date");
            haveUpdate = false;

         } else {
            updateSec = dateUpdate.getTime();
            
            if (haveCreation && dateUpdate.before(dateCreation)) {
                  formatErrors.add("DATE_UPDATE: '"+update+
                     "': Before DATE_CREATION ('"+creation+"')");
            }
            
            if ((updateSec - fileSec) > oneDaySec) {
               formatErrors.add("DATE_UPDATE: '"+update+
                     "': After GDAC receipt time ('"+ArgoDate.format(fileTime)+"')");
            }
         }
      }

      //............launch date checks:...........
      //..must be set ... not before earliest allowed date
      Date dateLaunch = (Date) null;
      boolean haveLaunch = false;

      if (launch.trim().length() > 0) {
         dateLaunch = ArgoDate.get(launch);
         haveLaunch = true;

         if (dateLaunch == (Date) null) {
            formatErrors.add("LAUNCH_DATE: '"+update+"': Invalid date");
            haveLaunch = false;

         } else {
            if (dateLaunch.before(earliestDate)) {
               formatErrors.add("LAUNCH_DATE: '"+launch+
                                "': Before earliest allowed date ('"+
                                ArgoDate.format(earliestDate)+"')");
            }
         }

      } else { 
         formatErrors.add("LAUNCH_DATE: Not set");
      }

      //............start date checks:...........
      //..if set, must be valid
      Date dateStart = (Date) null;

      if (start.trim().length() > 0) {
         dateStart = ArgoDate.get(start);

         if (dateStart == (Date) null) {
            formatErrors.add("START_DATE: '"+start+"': Invalid date");
         }
      }

      //............startup date checks:...........
      //..if set, within 3 days of launch date (W) and launch data set (W)
      Date dateStartup = (Date) null;

      if (startup.trim().length() > 0) {
         dateStartup = ArgoDate.get(startup);

         if (dateStartup == (Date) null) {
            formatErrors.add("STARTUP_DATE: '"+startup+"': Invalid date");
         }
      }

      //............end_mission date checks:...........
      //..if set, not before launch data and set if launch set
      Date dateEnd = (Date) null;

      if (end.trim().length() > 0) {
         dateEnd = ArgoDate.get(end);

         if (dateEnd == (Date) null) {
            formatErrors.add("END_MISSION_DATE: '"+end+"': Invalid date");

         } else {
            if (haveLaunch) {
               if (dateEnd.before(dateLaunch)) {
                  formatErrors.add("END_MISSION_DATE: '"+start+
                                   "': Before LAUNCH_DATE ('"+
                                   launch+"')");
               }
            } else {
               formatWarnings.add("END_MISSION_DATE: Set. LAUNCH_DATE missing");
            }
         }
      }
      log.debug(".....validateDates: end.....");
   }//..end validateDates

   /**
    * Validates the highly-desirable parameters (as defined up to v2.2)
    *
    * <p>Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings, 
    * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
    *
    * @param dac the DAC the file belongs to
    * @throws IOException If an I/O error occurs
    */

   public void validateHighlyDesirable_v2 (ArgoReferenceTable.DACS dac)
      throws IOException
   {
      //..Validated separately:
      //..  DATA_TYPE
      //..  FORMAT_VERSION
      //..  DATE_CREATION
      //..  DATE_UPDATE

      log.debug("....validateHighlyDesirable_v2: start.....");

      Character ch;
      double dVal;
      float fVar[];
      float fVal;
      ArgoReferenceTable.ArgoReferenceEntry info;
      String name;
      String str;

      //...........single valued variables..............

      //..CYCLE_TIME ---> see "per-cycle" checks below

      name = "DATA_CENTRE";  //..valid (and valid for DAC)
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (dac != (ArgoReferenceTable.DACS) null) {
         if (! ArgoReferenceTable.DacCenterCodes.get(dac).contains(str)) {
            formatErrors.add("DATA_CENTRE: '"+str+"': Invalid for DAC "+dac);
         }

      } else { //..incoming DAC not set
         if (! ArgoReferenceTable.DacCenterCodes.containsValue(str)) {
            formatErrors.add("DATA_CENTRE: '"+str+"': Invalid (for all DACs)");
         }
      }

      //..DEEPEST_PRESSURE ---> see "per-cycle" checks below

      name = "DIRECTION";  //..'A' or 'D'
      ch = getChar(name);
      log.debug("{}: '{}'", name, ch);
      if (ch != 'A'  &&  ch != 'D') {
         formatWarnings.add(name+": '"+ch+"': Not A or D");
      }

      name = "LAUNCH_LATITUDE";   //..on the earth
      dVal = readDouble(name);
      log.debug("{}: {}", name, dVal);

      if (dVal < -90.d  ||  dVal > 90.d) {
         formatWarnings.add(name+": "+dVal+": Invalid");
      }

      name = "LAUNCH_DATE";  //..not empty -- validity checked elsewhere
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatWarnings.add(name+": Empty");
      }

      name = "LAUNCH_LONGITUDE";   //..on the earth
      dVal = readDouble(name);
      log.debug("{}: {}", name, dVal);

      if (dVal < -180.d  ||  dVal > 180.d) {
         formatWarnings.add(name+": "+dVal+": Invalid");
      }

      name = "LAUNCH_QC";  //..valid ref table 2 value
      ch = getChar(name);
      log.debug("{}: '{}'", name, ch);

      if (! (info = ArgoReferenceTable.QC_FLAG.contains(ch)).isActive ) {
         formatWarnings.add(name+": '"+ch+"' Status: "+info.message);
      }

      //..PARAMETER --> see below
      //..PARKING_PRESSURE ---> see "per-cycle" checks below

      name = "PLATFORM_MODEL";  //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatWarnings.add(name+": Empty");
      }

      name = "PLATFORM_NUMBER";  //..valid platform number
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);

      if (! str.matches("[1-9][0-9]{4}|[1-9]9[0-9]{5}")) {
         formatErrors.add(name+": '"+str+"': Invalid");
      }

      name = "POSITIONING_SYSTEM";  //..ref table 9
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);

      if (! (info = ArgoReferenceTable.POSITIONING_SYSTEM.contains(str)).isActive) {
         formatWarnings.add(name+": '"+str+"' Status: "+info.message);
      }

      name = "PTT";  //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatWarnings.add(name+": Empty");
      }

      name = "START_DATE";  //..not empty -- validity checked elsewhere
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatWarnings.add(name+": Empty");
      }

      name = "START_DATE_QC";  //..valid ref table 2 value
      ch = getChar(name);
      log.debug("{}: '{}'", name, ch);

      if (! (info = ArgoReferenceTable.QC_FLAG.contains(ch)).isActive) {
         formatWarnings.add(name+": '"+str+"' Status: "+info.message);
      }

      name = "TRANS_SYSTEM";  //..ref table 10
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);

      if (! (info = ArgoReferenceTable.TRANS_SYSTEM.contains(str)).isActive) {
         formatWarnings.add(name+": '"+str+"' Status: "+info.message);
      }

      name = "TRANS_SYSTEM_ID";  //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatWarnings.add(name+": Empty");
      }

      //..............parameter names...........

      int nParam = getDimensionLength("N_PARAM");

      log.debug("n_param: '{}'", nParam);
      String paramVar[] = readStringArr("PARAMETER");
      for (int n = 0; n < nParam; n++) {
         str = paramVar[n].trim();
         log.debug("param[{}]: '{}'", n, str);
         if (! spec.isPhysicalParamName(str)) {
            formatWarnings.add("Physical parameter name: '"+str+"': Invalid");
         }
      }

      //.........per-cycle checks..........

      int nCycles = getDimensionLength("N_CYCLES");

      log.debug("n_cycles: '{}'", nCycles);
      name = "CYCLE_TIME";  //..not empty
      fVar = readFloatArr(name);
      for (int n = 0; n < nCycles; n++) {
         fVal = fVar[n];
         log.debug("{}[{}]: {}", name, n, fVal);

         if (fVal > 9999.f  ||  fVal <= 0.f) {
            formatWarnings.add(name+"["+(n+1)+"]: Not set");
         }
      }

      name = "PARKING_PRESSURE";  //..not empty
      fVar = readFloatArr(name);
      for (int n = 0; n < nCycles; n++) {
         fVal = fVar[n];
         log.debug("{}[{}]: {}", name, n, fVal);

         if (fVal > 9999.f  ||  fVal <= 0.f) {
            formatWarnings.add(name+"["+(n+1)+"]: Not set");
         }
      }


      name = "DEEPEST_PRESSURE";  //..not empty
      fVar = readFloatArr(name);
      for (int n = 0; n < nCycles; n++) {
         fVal = fVar[n];
         log.debug("{}[{}]: {}", name, n, fVal);

         if (fVal > 9999.f  ||  fVal <= 0.f) {
            formatWarnings.add(name+"["+(n+1)+"]: Not set");
         }
      }


      log.debug("....validateHighlyDesirable_v2: end.....");
   } //..end validateHighlyDesirable_v2


   /**
    * Validates the mandatory parameters, as defined after v2.2.
    * <br>Mandatory variables with controlled vacabularies are validated elsewhere.
    *
    * <p>Validates:
    * <ul>
    *   <li>  CONTROLLER_BOARD_SERIAL_NO_PRIMARY
    *   <li> CONTROLLER_BOARD_TYPE_PRIMARY
    *   <li> DAC_FORMAT_ID
    *   <li> DATA_CENTRE
    *   <li> FIRMWARE_VERSION
    *   <li> FLOAT_SERIAL_NO
    *   <li> LAUNCH_LATITUDE
    *   <li> LAUNCH_LONGITUDE
    *   <li> LAUNCH_QC
    *   <li> MANUAL_VERSION
    *   <li> PI_NAME
    *   <li> PLATFORM_FAMILY
    *   <li> PLATFORM_NUMBER
    *   <li> PLATFORM_MAKER
    *   <li> PLATFORM_TYPE
    *   <ul><li> PLATFORM_TYPE vs PLATFORM_MAKER</ul>
    *   <li> PTT
    *   <li> START_DATE_QC
    *   <li> STANDARD_FORMAT_ID
    *   <li> WMO_INST_TYPE
    *   <ul><li>PLATFORM_TYPExWMO_INST_TYPE</ul>
    *   <li> Per-parameter checks:
    *   <ul>
    *     <li> PARAMETER
    *     <li> PARAMETER_UNITS
    *     <li> PARAMETER_SENSOR
    *     <li> PREDEPLOYMENT_CALIB_COEFFICIENT
    *     <li> PREDEPLOYMENT_CALIB_EQUATION
    *   </ul>
    *   <li> Per-sensor checks:
    *   <ul>
    *     <li> SENSOR
    *     <li> SENSOR_MAKER
    *     <li> SENSOR_MODEL
    *     <ul>
    *       <li> SENSOR_MODEL / SENSOR_MAKER
    *       <li> SENSOR_MODEL / SENSOR
    *     </ul>
    *   </ul>
    *   <li> Per-positioning_system checks:
    *   <ul>
    *     <li> POSITIONING_SYSTEM
    *     <li> TRANS_SYSTEM
    *     <li> TRANS_SYSTEM_ID
    *   </ul>
    * </ul>
    *
    * <p>Validated separately:
    * <ul>
    *   <li> BATTERY_TYPE / BATTERY_PACKS
    *   <li> DATA_TYPE
    *   <li> FORMAT_VERSION
    *   <li> HANDBOOK_VERSION
    *   <li> DATE_CREATION
    *   <li> DATE_UPDATE
    *   <li> LAUNCH_DATE
    *   <li> START_DATE
    * </ul>
    *
    * <p>Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings, 
    * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
    *
    * @param dac the DAC the file belongs to
    * @throws IOException If an I/O error occurs
    */
   public void validateMandatory_v3 (ArgoReferenceTable.DACS dac)
      throws IOException
   {

      log.debug("....validateMandatory_v3: start.....");

      Character ch;
      double dVal;
      ArgoReferenceTable.ArgoReferenceEntry info;
      String name;
      String str;

      //...........single valued variables..............

      name = "CONTROLLER_BOARD_SERIAL_NO_PRIMARY";   //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatErrors.add(name+": Empty");
      }

      name = "CONTROLLER_BOARD_TYPE_PRIMARY";   //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatErrors.add(name+": Empty");
      }

      name = "DAC_FORMAT_ID";    //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatErrors.add(name+": Empty");
      }

      name = "DATA_CENTRE";   //..ref table 4 (and valid for DAC)
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (dac != (ArgoReferenceTable.DACS) null) {
         if (! ArgoReferenceTable.DacCenterCodes.get(dac).contains(str)) {
            formatErrors.add("DATA_CENTRE: '"+str+"': Invalid for DAC "+dac);
         }

      } else { //..incoming DAC not set
         if (! ArgoReferenceTable.DacCenterCodes.containsValue(str)) {
            formatErrors.add("DATA_CENTRE: '"+str+"': Invalid (for all DACs)");
         }
      }

      name = "FIRMWARE_VERSION";   //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatErrors.add(name+": Empty");
      }

      name = "FLOAT_SERIAL_NO";   //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatErrors.add(name+": Empty");
      }

      //..LAUNCH_DATE --> checked elsewhere

      name = "LAUNCH_LATITUDE";   //..on the earth
      dVal = readDouble(name);
      log.debug("{}: {}", name, dVal);

      if (dVal < -90.d  ||  dVal > 90.d) {
         formatErrors.add(name+": "+dVal+": Invalid");
      }

      name = "LAUNCH_LONGITUDE";   //..on the earth
      dVal = readDouble(name);
      log.debug("{}: {}", name, dVal);

      if (dVal < -180.d  ||  dVal > 180.d) {
         formatErrors.add(name+": "+dVal+": Invalid");
      }

      name = "LAUNCH_QC";  //..ref table 2
      ch = getChar(name);
      log.debug("{}: '{}'", name, ch);

      if ((info = ArgoReferenceTable.QC_FLAG.contains(ch)).isValid()) {
         if (info.isDeprecated) {
            formatWarnings.add(name+": '"+ch+"' Status: "+info.message);
         }

      } else {
         formatErrors.add(name+": '"+ch+"' Status: "+info.message);
      }

      name = "MANUAL_VERSION";   //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatErrors.add(name+": Empty");
      }

      //..PARAMETER --> see below

      name = "PI_NAME";   //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatErrors.add(name+": Empty");
      }

      name = "PLATFORM_FAMILY";   //..ref table 22
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);

      if ( (info = ArgoReferenceTable.PLATFORM_FAMILY.contains(str)).isValid() ) {
         if (info.isDeprecated) {
            formatWarnings.add(name+": '"+str+"' Status: "+info.message);
         }

      } else {
         formatErrors.add(name+": '"+str+"' Status: "+info.message);
      }

      name = "PLATFORM_NUMBER";   //..valid wmo id
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);

      if (! str.matches("[1-9][0-9]{4}|[1-9]9[0-9]{5}")) {
         formatErrors.add(name+": '"+str+"': Invalid");
      }

      boolean pmkrValid = false;

      String plfmMakerName = "PLATFORM_MAKER";   //..ref table 24
      String plfmMaker = readString(plfmMakerName).trim();
      log.debug("{}: '{}'", plfmMakerName, plfmMaker);

      if ( (info = ArgoReferenceTable.PLATFORM_MAKER.contains(plfmMaker)).isValid() ) {
         pmkrValid = true;

         if (info.isDeprecated) {
            formatWarnings.add(plfmMakerName+": '"+plfmMaker+"' Status: "+info.message);
         }

      } else {
         formatErrors.add(plfmMakerName+": '"+plfmMaker+"' Status: "+info.message);
      }

      boolean typValid = false;

      String plfmTypeName = "PLATFORM_TYPE";   //..ref table 23
      String plfmType = readString(plfmTypeName).trim();
      log.debug("{}: '{}'", plfmTypeName, plfmType);

      if ( (info = ArgoReferenceTable.PLATFORM_TYPE.contains(plfmType)).isValid() ) {
         typValid = true;

         if (info.isDeprecated) {
            formatWarnings.add(plfmTypeName+": '"+plfmType+"' Status: "+info.message);
         }

      } else {
         formatErrors.add(plfmTypeName+": '"+plfmType+"' Status: "+info.message);
      }

      if (pmkrValid && typValid) {
         if (! plfmType.equals("FLOAT")) {
            if (! ArgoReferenceTable.PLATFORM_TYPExPLATFORM_MAKER.xrefContains
                                                                   (plfmType, plfmMaker)) {
               formatErrors.add(plfmTypeName+"/"+plfmMakerName+": Inconsistent: '"+
                                plfmType+"'/'"+plfmMaker+"'");
               log.debug("{}/{} xref inconsistent: plfmType, plfmMaker = '{}', '{}'",
                         plfmTypeName, plfmMakerName, plfmType, plfmMaker);
            } else {
               log.debug("{}/{} xref valid: mdl, sn = '{}', '{}'",
                         plfmTypeName, plfmMakerName, plfmType, plfmMaker);
            }
         }
      }

      //..POSITIONING_SYSTEM --> see per-positioning_system below
      //..PREDEPLOYMENT_CALIB_COEFFICIENT --> see per-param below
      //..PREDEPLOYMENT_CALIB_EQUATION -->  see per-param below

      name = "PTT";
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatErrors.add(name+": Empty");
      }

      //..SENSOR  --> see per-sensor below
      //..SENSOR_MAKER  --> see per-sensor below
      //..SENSOR_MODEL  --> see per-sensor below
      //..SENSOR_SERIAL_NO  --> see per-sensor below

      name = "START_DATE_QC";   //..ref table 2
      ch = getChar(name);
      log.debug("{}: '{}'", name, ch);

      if ( (info = ArgoReferenceTable.QC_FLAG.contains(ch)).isValid() ) {
         if (info.isDeprecated) {
            formatWarnings.add(name+": '"+ch+"' Status: "+info.message);
         }

      } else {
         formatErrors.add(name+": '"+ch+"' Status: "+info.message);
      }

      name = "STANDARD_FORMAT_ID";   //..not empty
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      if (str.length() <= 0) {
         formatErrors.add(name+": Empty");
      }

      //..TRANS_FREQUENCY  \
      //..TRANS_SYSTEM      --> see per-trans_sys below
      //..TRANS_SYSTEM_ID  /

      boolean wmoValid = false;

      name = "WMO_INST_TYPE";   //..ref table 8
      str = readString(name).trim();
      log.debug("{}: '{}'", name, str);
      try {
         int N = Integer.valueOf(str);

         if ( (info = ArgoReferenceTable.WMO_INST_TYPE.contains(N)).isValid() ) {
            wmoValid = true;

            if (info.isDeprecated) {
               formatWarnings.add(name+": '"+str+"' Status: "+info.message);
            }

         } else {
            formatErrors.add(name+": '"+str+"' Status: "+info.message);
         }

      } catch (Exception e) {
         formatErrors.add(name+": '"+str+"' Invalid. Must be integer.");
      }

      if (wmoValid && typValid) {
         if (! plfmType.equals("FLOAT")) {
            if (! ArgoReferenceTable.PLATFORM_TYPExWMO_INST.xrefContains(plfmType, str)) {
               formatErrors.add(plfmTypeName+"/"+name+": Inconsistent: '"+
                                plfmType+"'/'"+str+"'");
               log.debug("{}/{} xref inconsistent: plfmType, wmo = '{}', '{}'",
                         plfmTypeName, name, plfmType, str);
            } else {
               log.debug("{}/{} xref valid: mdl, wmo = '{}', '{}'",
                         plfmTypeName, name, plfmType, str);
            }
         }
      }

      //...........per-parameter checks...........

      String[] paramVar;
      int nParam = getDimensionLength("N_PARAM");
      log.debug("N_PARAM: '{}'", nParam);

      name = "PARAMETER";   //..in physical parameter list
      paramVar = readStringArr(name);

      for (int n = 0; n < nParam; n++) {
         str = paramVar[n].trim();
         log.debug(name+"[{}]: '{}'", n, str);
         if (! spec.isPhysicalParamName(str)) {
            formatErrors.add(name+"["+(n+1)+"]: '"+str+"': Invalid");
         }
      }

      name = "PARAMETER_UNITS";  //..not empty
      paramVar = readStringArr(name);

      for (int n = 0; n < nParam; n++) {
         str = paramVar[n].trim();
         log.debug(name+"[{}]: '{}'", n, str);
         if (str.length() <= 0) {
            formatErrors.add(name+"["+(n+1)+"]: Empty");
         }
      }

      name = "PARAMETER_SENSOR";  //..not empty
      paramVar = readStringArr(name);

      for (int n = 0; n < nParam; n++) {
         str = paramVar[n];
         log.debug(name+"[{}]: '{}'", n, str);
         if (str.length() <= 0) {
            formatErrors.add(name+"["+(n+1)+"]: Empty");
         }
      }

      name = "PREDEPLOYMENT_CALIB_COEFFICIENT";   //..not empty
      paramVar = readStringArr(name);

      for (int n = 0; n < nParam; n++) {
         str = paramVar[n].trim();
         log.debug(name+"[{}]: '{}'", n, str);

         if (str.length() <= 0) {
            formatErrors.add(name+"["+(n+1)+"]: Empty");
         }
      }

      name = "PREDEPLOYMENT_CALIB_EQUATION";   //..not empty
      paramVar = readStringArr(name);

      for (int n = 0; n < nParam; n++) {
         str = paramVar[n].trim();
         log.debug(name+"[{}]: '{}'", n, str);

         if (str.length() <= 0) {
            formatErrors.add(name+"["+(n+1)+"]: Empty");
         }
      }

      //.........per-sensor checks............
      int nSensor = getDimensionLength("N_SENSOR");
      log.debug("N_SENSOR: '{}'", nSensor);

      String sensorName = "SENSOR";   //..ref table 25
      String[] sensor = readStringArr(sensorName);

      String sensorMakerName = "SENSOR_MAKER";   //..ref table 26
      String[] sensorMaker = readStringArr(sensorMakerName);

      String sensorModelName = "SENSOR_MODEL";   //..ref table 27
      String[] sensorModel = readStringArr(sensorModelName);

      for (int n = 0; n < nSensor; n++) {
         ArgoReferenceTable.ArgoReferenceEntry mdlInfo;
         ArgoReferenceTable.ArgoReferenceEntry mkrInfo;
         ArgoReferenceTable.ArgoReferenceEntry snsrInfo;

         //..check SENSOR
         boolean snsrValid = false;
         String snsr = sensor[n].trim();
         log.debug(sensorName+"[{}]: '{}'", n, snsr);

         if ( (snsrInfo = ArgoReferenceTable.SENSOR.contains(snsr)).isValid() ) {
            snsrValid = true;
            if (snsrInfo.isDeprecated) {
               formatWarnings.add(sensorName+"["+(n+1)+"]: '"+snsr+"' Status: "+
                                  snsrInfo.message);
            }

         } else {
            formatErrors.add(sensorName+"["+(n+1)+"]: '"+snsr+"' Status: "+
                             snsrInfo.message);
         }

         //..check SENSOR_MAKER
         String snsrMaker = sensorMaker[n].trim();
         boolean smkrValid = false;
         log.debug(sensorMakerName+"[{}]: '{}'", n, snsrMaker);

         if ( (mkrInfo = ArgoReferenceTable.SENSOR_MAKER.contains(snsrMaker)).isValid() ) {
            smkrValid = true;
            if (mkrInfo.isDeprecated) {
               formatWarnings.add(sensorMakerName+"["+(n+1)+"]: '"+snsrMaker+"' Status: "+
                                  mkrInfo.message);
            }

         } else {
            formatErrors.add(sensorMakerName+"["+(n+1)+"]: '"+snsrMaker+"' Status: "+
                             mkrInfo.message);
         }

         //..check SENSOR_MODEL
         String snsrModel = sensorModel[n].trim();
         boolean mdlValid = false;
         log.debug(sensorModelName+"[{}]: '{}'", n, snsrModel);

         if ( (mdlInfo = ArgoReferenceTable.SENSOR_MODEL.contains(snsrModel)).isValid() ) {
            mdlValid = true;
            if (mdlInfo.isDeprecated) {
               formatWarnings.add(sensorModelName+"["+(n+1)+"]: '"+snsrModel+"' Status: "+
                                  mdlInfo.message);
            }
         } else {
            formatErrors.add(sensorModelName+"["+(n+1)+"]: '"+snsrModel+"' Status: "+
                             mdlInfo.message);
         }

         //..cross-reference SENSOR_MODEL / SENSOR_MAKER
         if (smkrValid && mdlValid) {
            if (! snsrModel.equals("UNKNOWN")) {
               String mkr = mkrInfo.getColumn(1);
               String mdl = mdlInfo.getColumn(1);

               if (! ArgoReferenceTable.SENSOR_MODELxSENSOR_MAKER.xrefContains(mdl, mkr)) {
                  formatErrors.add(sensorModelName+"/"+sensorMakerName+"["+(n+1)+"]: "+
                                      "Inconsistent: '"+snsrModel+"'/'"+snsrMaker+"'");
                  log.debug("SENSOR_MODEL/SENSOR_MAKER xref inconsistent: mdl, mkr = '{}', '{}'",
                            mdl, mkr);
               } else {
                  log.debug("SENSOR_MODEL/SENSOR_MAKER xref valid: mdl, mkr = '{}', '{}'", mdl, mkr);
               }
            }
         }

         //..cross-reference SENSOR_MODEL / SENSOR
         if (snsrValid && mdlValid) {
            if (! snsrModel.equals("UNKNOWN")) {
               String sn = snsrInfo.getColumn(1);
               String mdl = mdlInfo.getColumn(1);

               if (! ArgoReferenceTable.SENSOR_MODELxSENSOR.xrefContains(mdl, sn)) {
                  formatErrors.add(sensorModelName+"/"+sensorName+"["+(n+1)+"]: "+
                                      "Inconsistent: '"+snsrModel+"'/'"+snsr+"'");
                  log.debug("SENSOR_MODEL/SENSOR xref inconsistent: mdl, sn = '{}', '{}'",
                            mdl, sn);
               } else {
                  log.debug("SENSOR_MODEL/SENSOR xref valid: mdl, sn = '{}', '{}'", mdl, sn);
               }
            }
         }
      }

      //..........per-positioning_system checks
      String[] positVar;  
      int nPosit = getDimensionLength("N_POSITIONING_SYSTEM");
      log.debug("N_POSITIONING_SYSTEM: '{}'", nPosit);

      name = "POSITIONING_SYSTEM";   //..ref table 9
      positVar = readStringArr(name);

      for (int n = 0; n < nPosit; n++) {
         str = positVar[n].trim();
         log.debug(name+"[{}]: '{}'", n, str);

         if ( (info = ArgoReferenceTable.POSITIONING_SYSTEM.contains(str)).isValid() ) {
            if (info.isDeprecated) {
               formatWarnings.add(name+"["+(n+1)+"]: '"+str+"' Status: "+info.message);
            }
         } else {
            formatErrors.add(name+"["+(n+1)+"]: '"+str+"' Status: "+info.message);
         }
      }

      //..........per-trans_system checks
      String[] transVar;  
      int nTrans = getDimensionLength("N_TRANS_SYSTEM");
      log.debug("N_TRANS_SYSTEM: '{}'", nTrans);

      name = "TRANS_SYSTEM";   //..ref table 10
      transVar = readStringArr(name);

      for (int n = 0; n < nTrans; n++) {
         str = transVar[n].trim();
         log.debug(name+"[{}]: '{}'", n, str);

         if ( (info = ArgoReferenceTable.TRANS_SYSTEM.contains(str)).isValid() ) {
            if (info.isDeprecated) {
               formatWarnings.add(name+"["+(n+1)+"]: '"+str+"' Status: "+info.message);
            }
         } else {
            formatErrors.add(name+"["+(n+1)+"]: '"+str+"' Status: "+info.message);
         }
      }

      name = "TRANS_SYSTEM_ID";   //..not empty
      transVar = readStringArr(name);

      for (int n = 0; n < nTrans; n++) {
         str = transVar[n].trim();
         log.debug(name+"[{}]: '{}'", n, str);
         if (str.length() <= 0) {
            formatErrors.add(name+"["+(n+1)+"]: Empty");
         }
      }

      log.debug("....validateMandatory_v3: end.....");
   } //..end validateMandatory_v3

   /**
    * Convenience method to return a char value for a variable
    *
    * @param name the variable name
    * @return the variable String value
    * @throws IOException If an I/O error occurs
    */
   private Character getChar(String name) 
      throws IOException
   {
      ArrayChar.D0 value = (ArrayChar.D0) ncReader.findVariable(name).read();
      return Character.valueOf(value.get());
   }

   /**
    * Validates the BATTERY_TYPE and BATTERY_PACKS in the meta-data file.
    * 
    * <p>Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings, 
    * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
    *
    * @throws IOException If an I/O error occurs
    */
   public void validateBattery()
      throws IOException
   {
      log.debug(".....validateBattery.....");

      //.........battery_type............
      int nTypes = 0;

      String str = readString("BATTERY_TYPE");
      log.debug("BATTERY_TYPE: '{}'", str);

      if (str.length() <= 0) {
         formatErrors.add("BATTERY_TYPE: Empty");

      } else {

         //..not empty
         //..split multiple strings based on "+"

         for (String substr : str.split("\\+")) {
            nTypes++;
            log.debug("battery_type substring: '{}'", substr);

            Matcher m = pBatteryType.matcher(substr);

            if (m.matches()) {
               String manu = m.group("manufacturer");
               String type = m.group("type");
               String volt = m.group("volts");

               log.debug("...matched pattern: manu, type, volt = '{}', '{}', '{}'", manu, type, volt);

               if (! ArgoReferenceTable.BATTERY_TYPE_manufacturer.contains(manu)) {
                  String err = String.format
                     ("BATTERY_TYPE[%d]: Invalid manufacturer: '{%s}'", nTypes, manu);
                  //formatErrors.add(err);

                  //################# TEMPORARY WARNING ################
                  formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                  log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                  log.debug("...invalid manufacturer");

               } else {
                  log.debug("valid manufacturer");
               }

               if (! ArgoReferenceTable.BATTERY_TYPE_type.contains(type)) {
                  String err = String.format
                     ("BATTERY_TYPE[%d]: Invalid type: '{%s}'", nTypes, type);
                  //formatErrors.add(err);

                  //################# TEMPORARY WARNING ################
                  formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                  log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                  log.debug("invalid type");

               } else {
                  log.debug("valid type");
               }

            } else {
               //..did not match the expected pattern
               String err = String.format(
                  "BATTERY_TYPE[%d]: Does not match template 'manufacturer type volts V': '%s'",
                  nTypes, substr.trim());
               //formatErrors.add(err);

               //################# TEMPORARY WARNING ################
               formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
               log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

               log.debug("...does not match template");
            }
         }
      } //..endif battery_type is filled


      //.....battery_packs.....
      int nPacks = -1;

      str = readString("BATTERY_PACKS");

      log.debug("BATTERY_PACKS: '{}'", str);

      if (str.length() <= 0) {
         //..empty - allowed - optional variable
         log.debug("BATTERY_PACKS: empty (allowed)");

      } else {

         //..not empty
         //..split multiple strings based on "+"

         nPacks = 0;
         for (String substr : str.split("\\+")) {
            nPacks++;
            log.debug("battery_packs substring: '{}'", substr);

            if (substr.trim().equals("U")) {
               log.debug("battery_packs substring == U (undefined)");

            } else {
               //..not undefined -- check pattern

               Matcher m = pBatteryPacks.matcher(substr);
            
               if (m.matches()) {

                  String num   = m.group("numofpacks");
                  String style = m.group("style");
                  String type  = m.group("type");

                  log.debug("...matched pattern: num, style, type = '{}', '{}', '{}",
                            num, style, type);

                  if (! ArgoReferenceTable.BATTERY_PACKS_style.contains(style)) {
                     String err = String.format
                        ("BATTERY_PACKS[%d]: Invalid style of battery: '{%s}'", nPacks, style);
                     //formatErrors.add(err);

                     //################# TEMPORARY WARNING ################
                     formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                     log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                     log.debug("invalid style");

                  } else {
                     log.debug("valid style");
                  }

                  if (! ArgoReferenceTable.BATTERY_PACKS_type.contains(type)) {
                     String err = String.format
                        ("BATTERY_PACKS[%d]: Invalid type: '{%s}'", nPacks, type);
                     //formatErrors.add(err);

                     //################# TEMPORARY WARNING ################
                     formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                     log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                     log.debug("invalid type");

                  } else {
                     log.debug("valid type");
                  }

               } else {
                  //..did not match the expected pattern
                  String err = String.format
                     ("BATTERY_PACKS[%d]: Does not match template 'xStyle type (or U): '%s'",
                      nPacks, substr.trim());
                  //formatErrors.add(err);

                  //################# TEMPORARY WARNING ################
                  formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                  log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                  log.debug("...does not match template");
               }
            }  //..end if undefined
         } //..end for (battery_packs substrings)
      } //..endif BATTERY_PACKS is filled

      //............compare TYPES and PACKS............

      if (nPacks >= 0) {

         if (nTypes != nPacks) {
            String err = String.format
               ("Number of BATTERY_TYPES {} != number of BATTERY_PACKS {}", nTypes, nPacks);
            //formatErrors.add(err);

            //################# TEMPORARY WARNING ################
            formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
            log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

            log.debug("number of types != number of packs => {} != {}", nTypes, nPacks);
         }

      } //..end if nPacks >= 0

   }//..end validateBattery

   /**
    * Validates the configuration mission in the meta-data file.
    * The mission number must start at 1. Fillvalue is allowed (with WARNING)
    * ****OFF***The mission number must start at 1 and be consecutive integers.
    * 
    * <p>Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings, 
    * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
    *
    * @throws IOException If an I/O error occurs
    */
   public void validateConfigMission()
      throws IOException
   {
      log.debug(".....validateConfigMission.....");

      int nMissions = getDimensionLength("N_MISSIONS");
      int[] mission = readIntArr("CONFIG_MISSION_NUMBER");

      log.debug("N_MISSIONS = {}", nMissions);

      for (int n = 0; n < nMissions; n++) {
         log.debug("CONFIG_MISSION_NUMBER[{}] = {}", n, mission[n]);

         if (mission[n] == 99999) {
            formatWarnings.add("CONFIG_MISSION_NUMBER: Missing at index: "+(n+1));
            log.debug("config_mission_number == 0 at {}", n);
            break;
         }
      }
   }

   /**
    * Validates the configuration parameter names and units in the meta-data file.
    * The name is the entry up to last "_".  Unit is after the last "_".
    * 
    * <p>Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings, 
    * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
    *
    * @throws IOException If an I/O error occurs
    */
   public void validateConfigParams()
      throws IOException
   {
      log.debug(".....validateConfigParams.....");

      HashSet<String> nameAlreadyChecked = new HashSet<String>(100);
      HashMap<String, Boolean> unitAlreadyChecked = new HashMap<String, Boolean>(100);

      String[] varNames = {"LAUNCH_CONFIG_", "CONFIG_"};

      for (String v : varNames) {
         String dim = "N_" + v + "PARAM";
         String varName = v + "PARAMETER_NAME";
         
         int nParam = getDimensionLength(dim);

         log.debug("'{}' checking: number of parameters = {}", v, nParam);

         //..read config names
         String[] full_name = readStringArr(varName);

         //...........loop over names.............

         for (int n = 0; n < nParam; n++) {
            String full = full_name[n].trim();
            int index = full.lastIndexOf('_');

            if (index <= 0) {
               //..poorly formed name - only report if not already reported

               if (! nameAlreadyChecked.contains(full)) {
                  formatErrors.add(varName+"["+(n+1)+"]: "+
                                   "Incorrectly formed name '"+full+"'");
                  nameAlreadyChecked.add(full);
               }

               nameAlreadyChecked.add(full);
               log.debug("badly formed name: {}[{}] = '{}'", varName, n, full);

               continue; //..can't do anything with this
            }

            //..well formed named, break it apart

            String param = full.substring(0, index);
            String unit = full.substring(index+1);

            log.debug("check {}[{}]: full '{}'; param '{}'; unit '{}'",
                      varName, n, full, param, unit);

            //..check name

            if (! nameAlreadyChecked.contains(param)) {
               //..this parameter name has not been checked

               ArgoConfigTechParam.ArgoConfigTechParamMatch match = 
                  spec.ConfigTech.findConfigParam(param);

               if (match == null) {
                  //..NOT an active name, NOT a deprecated name --> error
                  String err = String.format("%s[%d]: Invalid name '%s'", varName, (n+1), param);
                  //formatErrors.add(err);

                  //################# TEMPORARY WARNING ################
                  formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                  log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                  log.debug("parameter is invalid");

               } else {
                  if (match.isDeprecated) {
                     //..IS a deprecated name --> warning
                     formatWarnings.add(varName+"["+(n+1)+"]: "+
                                        "Deprecated name '"+param);
                     log.debug("parameter is deprecated: '{}'", param);
                  }

                  if (match.nFailedMatchedTemplates > 0) {
                     //..these Templates failed to match the values specified in the table
                     //..they are errors

                     for (Map.Entry<String, String> entry : match.failedMatchedTemplates.entrySet()) {
                        String tmplt = entry.getKey();
                        String value = entry.getValue();

                        String err = String.format("%s[%d]: Invalid template/value '%s'/'%s' in '%s'",
                                                   varName, (n+1), tmplt, value, param);
                        //formatErrors.add(err);

                        //################# TEMPORARY WARNING ################
                        formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                        log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                        log.debug("...invalid template/value '{}'/'{}'", tmplt, value);
                     }
                  }

                  if (match.nUnMatchedTemplates > 0) {
                     //..these Templates did not have values specified in the table
                     
                     //..check the generic template values:
                     //..    shortsensorname, cyclephasename, param
                     //..all others are accepted as is - they matched their basic regex
                     //..- assume they are good

                     String str = match.unMatchedTemplates.get("shortsensorname");
                     if (str != null) {
                        if (! ArgoReferenceTable.GENERIC_TEMPLATE_short_sensor_name.contains(str)) {
                           String err = String.format
                              ("%s[%d]: Invalid short_sensor_name '%s' in '%s'", 
                               varName, (n+1), str);
                           //formatErrors.add(err);

                           //################# TEMPORARY WARNING ################
                           formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                           log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                           log.debug("...generic short_sensor_name lookup: INVALID = '{}'", str);
                        } else {
                           log.debug("...generic short_sensor_name lookup: valid = '{}'", str);
                        }
                     }

                     str = match.unMatchedTemplates.get("cyclephasename");
                     if (str != null) {
                        if (! ArgoReferenceTable.GENERIC_TEMPLATE_cycle_phase_name.contains(str)) {
                           String err = String.format
                              ("%s[%d]: Invalid cycle_phase_name '%s' in '%s'", 
                               varName, (n+1), str, param);
                           //formatErrors.add(err)

                           //################# TEMPORARY WARNING ################
                           formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                           log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                           log.debug("...generic cycle_phase_name lookup: INVALID = '{}'", str);

                        } else {
                           log.debug("...generic cycle_phase_name lookup: valid = '{}'", str);
                        }
                     }

                     str = match.unMatchedTemplates.get("param");
                     if (str != null) {
                        if (! ArgoReferenceTable.GENERIC_TEMPLATE_param.contains(str)) {
                           String err = String.format
                              ("%s[%d]: Invalid param '%s' in '%s'", 
                               varName, (n+1), str, param);

                           //formatErrors.add(err)

                           //################# TEMPORARY WARNING ################
                           formatWarnings.add(err + "   *** WILL BECOME AN ERROR ***");
                           log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

                           log.debug("...generic param: generic name lookup: INVALID = '{}'", str);
                        } else {
                           log.debug("...generic param: generic name lookup: valid = '{}'", str);
                        }
                     }
                  }
               }

               nameAlreadyChecked.add(param);
            } //..end if nameAlreadyChecked

            //..check the unit

            boolean validUnit = false;

            if (! unitAlreadyChecked.containsKey(unit)) {
               //..this unit name has not been checked

               if (! spec.ConfigTech.isConfigTechUnit(unit)) {
                  //..NOT an active unit name

                  if (spec.ConfigTech.isDeprecatedConfigTechUnit(unit)) {
                     //..IS a deprecated name --> warning

                     validUnit = true;
                     formatWarnings.add(varName+"["+(n+1)+"]: "+
                                        "Deprecated unit '"+unit+"' in '"+
                                        full+"'");
                     log.debug("deprecated unit '{}'", unit);

                  } else {
                     //..INVALID unit -- not active, not deprecated --> error
                     validUnit = false;
                     formatErrors.add(varName+"["+(n+1)+"]: "+
                                      "Invalid unit '"+unit+"' in '"+
                                      full+"'");
                     log.debug("name is valid, unit ({}) is not valid (new or old)", unit);
                  }

               } else {
                  //..IS an active unit
                  validUnit = true;
               } //..end if not isConfigTechUnit

               unitAlreadyChecked.put(unit, validUnit);

            } else {
               //..unit already check --- was it valid?
               validUnit = unitAlreadyChecked.get(unit);

            } //..end check unit

         } //..end for (nParam)

      } //..end for ("launch_config", "config_")

   } //..end validateConfigParams
   
} //..end class

