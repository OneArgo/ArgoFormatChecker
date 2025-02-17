package usgdac;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements features to check the Meta-data CONFIG_PARAMETER_NAME (including LAUNCH_...) and 
 * TECHNICAL_PARAMETER_NAME.  This includes the <i>units</i> allowed for both.
 * 
 * @author Mark Ignaszewski
 * @version  $HeadURL: https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoConfigTechParam.java $
 * @version  $Id: ArgoConfigTechParam.java 1257 2021-04-26 14:09:25Z ignaszewski $
 */
public class ArgoConfigTechParam
{

//............................................
//               VARIABLES
//............................................

   //..........Class variables..............

   public static enum ConfigTechValueType { 
      DATE_TIME   ("date/time"),
      FLOAT       ("float"),
      HEX         ("hex"),
      INTEGER     ("integer"),
      LOGICAL     ("logical"),
      STRING      ("string"),
      UNKNOWN     ("unknown");

      public final String name;

      ConfigTechValueType (String d) { name = d; }

      public static ConfigTechValueType getType (String name)
      {
         for (ConfigTechValueType c : ConfigTechValueType.values()) {
            if (name.equals(c.name)) {
               return c;
            }
         }
         return ConfigTechValueType.UNKNOWN;
      }

      public String toString() { return name; }
   };

   //......define and initialize the template replacement patterns......

   static final String defaultTemplateReplacement;
   static final Map<String, String> templateReplacement;
   static final String[] knownTemplates;

   static {
      defaultTemplateReplacement = "(?<default>\\w+)";

      Map<String, String> temp = new HashMap<String, String>(10);
      //..config templates (alphabetical order)
      temp.put("D", "(?<D>\\d+?)");
      temp.put("cycle_phase_name", "(?<cyclephasename>[A-Z][a-z]+(?:[A-Z][a-z]+)*?Phase)");
      temp.put("I", "(?<I>\\d+?)");
      temp.put("N", "(?<N>\\d+?)");
      temp.put("N+1", "(?<N1>\\d+?)");
      temp.put("param", "(?<param>[A-Z][a-z]+(?:[A-Z][a-z]+)??)");
      temp.put("PARAM", "(?<PARAM>[A-Z]+?)");
      temp.put("S", "(?<S>\\d+?)");
      temp.put("SubS", "(?<Subs>\\d+?)");
      temp.put("short_sensor_name", "(?<shortsensorname>[A-Z][a-z]+?|CTD)");
      //..tech templates (alphabetical order)
      temp.put("digit", "(?<digit>\\d)");
      temp.put("int", "(?<int>\\d+?)");
      //..already defined above
      temp.put("Z", "(?<Z>\\d+?)");

      templateReplacement = Collections.unmodifiableMap(temp);

      knownTemplates = new String[]{
         "D",
         "horizontalphasename",
         "I",
         "N",
         "N1",
         "param",
         "PARAM",
         "S",
         "SubS",
         "shortsensorname",
         "verticalphasename",
         "digit",
         "int",
         "Z"
      };
   }
      

   //......define and initialize the pattern matcher objects......
   static Pattern pBlankOrComment;  //..match a blank line or a comment line
   static Pattern pComment;         //..comment

   static
   {
      //..match a blank line (or a line with just comments)
      pBlankOrComment = Pattern.compile("^\\s*(?://.*)*");

      //..match a comment (any where on the line) - recognize the "//@" comments
      //..group 1: the word following "@"; group 2: the setting
      pComment = Pattern.compile("//(?:@\\s*(\\w+)\\s*=\\s*\"(.+)\")?.*$");
   }

   //.....object variables......

   private String  bioCfgParmFileName;
   private String  coreCfgParmFileName;
   private String  tecParmFileName;
   private String  unitFileName;
   private String  version;

   private LinkedHashSet<String>        configParamList;  //..config param fixed names
   private LinkedHashSet<String>        configParamList_DEP;  //..config param fixed names (deprctd)
   //   private LinkedHashSet<Pattern>       configParamRegex; //..config param variable names
   //   private LinkedHashSet<Pattern>       configParamRegex_DEP; //..config param variable names
   private LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> configParamRegex; //..config param variable names
   private LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> configParamRegex_DEP; //..config param variable names

   private LinkedHashSet<String>        techParamList;    //..tech param fixed names
   private LinkedHashSet<String>        techParamList_DEP;    //..tech param fixed names (deprctd)

   //   private LinkedHashSet<Pattern>       techParamRegex;   //..tech param regex names
   //   private LinkedHashSet<Pattern>       techParamRegex_DEP;   //..tech param regex names (deprctd)
   private LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> techParamRegex; //..config param variable names
   private LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> techParamRegex_DEP; //..config param variable names

   private LinkedHashMap<String, ConfigTechValueType>   unitList; //..config/tech units and type
   private LinkedHashMap<String, ConfigTechValueType>   unitList_DEP; //..config/tech units - deprecated


   //..logger
   private static final Logger log = LogManager.getLogger("ArgoConfigTechParam");

//............................................
//              CONSTRUCTORS
//............................................

   /**
    * Reads the CONFIGURATION_PARAMETER and TECHNICAL_PARAMETER specification files.
    * <br>The files must be in specDir and are named <i>argo-config_names-spec-v&lt;version&gt;</i>
    * and <i>"argo-tech_names-spec-v&lt;version&gt;</i>
    *
    * @param specDir  Path to the specification file directory
    * @param version  Argo netCDF file version
    * @param initConfig   true: Read CONFIGURATION_PARAMETER specification file
    * @param initTech     true: Read TECHNICAL_PARAMETER specification file
    * @throws IOException  File I/O errors
    */

   public ArgoConfigTechParam (String specDir, String version, boolean initConfig, boolean initTech)
            throws IOException
   {
      log.debug("...ArgoConfigTechParam: start...");

      this.version = version.trim();

      String prefix = specDir.trim() + "/argo-";
      coreCfgParmFileName = prefix + "core_config_names-spec-v" + this.version;
      bioCfgParmFileName = prefix + "bio_config_names-spec-v" + this.version;
      tecParmFileName = prefix + "tech_names-spec-v" + this.version;
      unitFileName = prefix + "tech_units-spec-v" + this.version;

      if (initConfig) parseConfigParamFiles();
      if (initTech)   parseTechParamFile();

      parseUnitFile();

      if (log.isDebugEnabled()) {
         if (configParamList == null) log.debug("configParamList size = null");
         else log.debug("configParamList size = {}", configParamList.size());
         if (configParamList_DEP == null) log.debug("configParamList_DEP size = null");
         else log.debug("configParamList_DEP size = {}", configParamList_DEP.size());
         if (techParamList == null) log.debug("techParamList size = null");
         else log.debug("techParamList size = {}", techParamList.size());
         if (techParamList_DEP == null) log.debug("techParamList_DEP size = null");
         else log.debug("techParamList_DEP size = {}", techParamList_DEP.size());
         log.debug("...ArgoConfigTechParam: end...");
      }
   }

//............................................
//               METHODS
//............................................

   /**
    * Determines if the name is matched by a REGEX in the set
    * @param name      The parameter name in question.
    * @param regexSet  The set of REGEXPs to check
    * @return An ArgoConfigTechParamMatch containing full information about the match
    *     (null = NO MATCH)
    */
   private ArgoConfigTechParamMatch checkRegex
      (String name, 
       HashMap<Pattern, HashMap<String, HashSet<String>>> regexSet)
   {
      ArgoConfigTechParamMatch match = null;  //..return object
      HashMap<String, String> unMatchedTemplates = null;  //..return templates
      HashMap<String, String> failedMatchedTemplates = null;  //..return templates

      //..NOTE..NOTE..NOTE..
      //..Should be able to truncate the loop below on the first match
      //..ie, find a match -> return
      //..However, during development I *really* want to know if there are
      //..multiple matches - which indicates ambiguous regex patterns
      //..So, FOR NOW, complete the looping and report multiple matches
      //..Returns the LAST one matched

      for (Map.Entry<Pattern, HashMap<String, HashSet<String>>> entry : regexSet.entrySet()) {
         Pattern pattern = entry.getKey();
         HashMap<String, HashSet<String>> value = entry.getValue();

         Matcher m = pattern.matcher(name);
         
         if (m.matches()) {
            unMatchedTemplates = new HashMap<String, String>(m.groupCount());
            failedMatchedTemplates = new HashMap<String, String>(m.groupCount());

            for (String key : knownTemplates) {
               try {
                  String str = m.group(key);

                  //..is there a "match-list" for this (regex param and template)

                  if (value != null) {
                     HashSet<String> matchSet = value.get(key);

                     if (matchSet == null) {
                        //..there was no match-list to compare to
                        unMatchedTemplates.put(key, str);

                     } else {
                        //..there is a match-list - compare it

                        if (! matchSet.contains(str)) {
                           failedMatchedTemplates.put(key, str);
                           log.debug("checkRegex: match-list success: key '{}', matched '{}'",
                                     key, str);

                        } else if (log.isDebugEnabled()) {
                           log.debug("checkRegex: match-list failed: key '{}', matched '{}'",
                                     key, str);
                        }
                     }
                  } else {
                     unMatchedTemplates.put(key, str);
                  }

               } catch (IllegalArgumentException e) {
               }
            }

            match = new ArgoConfigTechParamMatch
               (pattern.toString(), false, 
                unMatchedTemplates.size(), unMatchedTemplates,
                failedMatchedTemplates.size(), failedMatchedTemplates);

            log.debug("checkRegex: '{}' regex match #{}: '{}', unMatchedTemplates {}"+
                      "failedTemplates {}",
                      unMatchedTemplates.size(), failedMatchedTemplates.size());

            //****temporary**** return match;
         }
      }

      return match;
   } //..checkRegex

   /**
    * Determines if the name is a CONFIG parameter name
    * @param name the parameter name in question.
    * @return An ArgoConfigTechParamMatch containing full information about the match
    *     (null = NO MATCH)
    *        
    */
   public ArgoConfigTechParamMatch findConfigParam (String name)
   {
      ArgoConfigTechParamMatch match = findParam(name,
                                                 configParamList, configParamRegex,
                                                 configParamList_DEP, configParamRegex_DEP);
      return match;
   }

   /**
    * Determines if the name is a Tech parameter name
    * @param name the parameter name in question.
    * @return An ArgoConfigTechParamMatch containing full information about the match
    *     (null = NO MATCH)
    *        
    */
   public ArgoConfigTechParamMatch findTechParam (String name)
   {
      ArgoConfigTechParamMatch match = findParam(name,
                                                 techParamList, techParamRegex,
                                                 techParamList_DEP, techParamRegex_DEP);
      return match;
   }

   /**
    * Determines if the name is a valid parameter name within the supplied lists
    * @param name the parameter name in question.
    * @return An ArgoConfigTechParamMatch containing full information about the match
    *     (null = NO MATCH)
    *        
    */
   private ArgoConfigTechParamMatch findParam
      (String name,
       HashSet<String> activeList,
       HashMap<Pattern, HashMap<String, HashSet<String>>> activeRegex,
       HashSet<String> deprecatedList,
       HashMap<Pattern, HashMap<String, HashSet<String>>> deprecatedRegex)
   {
      boolean literal = false;
      HashMap<String, String> templates = null;
      ArgoConfigTechParamMatch match = null;

      if (activeList != null) {
         if (activeList.contains(name)) {
            match = new ArgoConfigTechParamMatch (name, false);

            log.debug("findParam: '{}': active literal match", name);
            return match;
         }
      }

      if (deprecatedList != null) {
         if (deprecatedList.contains(name)) {
            match = new ArgoConfigTechParamMatch (name, true);

            log.debug("findParam: '{}': deprecated literal match", name);
            return match;
         }
      }

      //..did NOT match one of the literal strings
      //..check for a regex match

      if (activeRegex != null) {
         //log.debug("findParam: checking active regex");

         match = checkRegex(name, activeRegex);

         if (match != null) {
            match.isDeprecated = false;
            log.debug("findParam: '{}': active regex match '{}'", name, match.match);
            return match;
         }
      }

      if (deprecatedRegex != null) {
         //log.debug("findParam: checking active regex");

         match = checkRegex(name, deprecatedRegex);

         if (match != null) {
            match.isDeprecated = true;
            log.debug("findParam: '{}': deprecated regex match '{}'", name, match.match);
            return match;
         }
      }

      log.debug("findParam: '{}': failed match", name);
      return null;
   }

   /**
    * Determines if the value is valid for the unit (mapped to data-type)
    * @param unit  the unit name
    * @param value the config value
    * @return True - value is a valid setting for the given unit (or unit is unknown)
    *         False - parameter name is NOT valid (or tech spec not opened)
    *        
    */
   public boolean isConfigTechValidValue(String unit, String value)
   {
      //..turn the unit name into a data type
      ConfigTechValueType dt = unitList.get(unit);

      if (dt == null || dt == ConfigTechValueType.UNKNOWN) {
         //..unit name is unknown OR the data type for this unit is unkown
         //..can't check the value setting
         //..so blindly return "it's good" (???)
         log.debug("isConfigTechValidValue: default to true: unit, data-type, value = '{}', '{}', '{}'",
                   unit, dt, value);

         return true;
      }

      boolean valid = false;
      switch (dt) {
      case DATE_TIME:
         valid = ArgoDate.checkArgoDatePattern(unit, value);
         break;

      case FLOAT:
         try {
            double n = Double.parseDouble(value);
            valid = true;

         } catch (NumberFormatException e) {
            valid = false;
         }
         break;

      case HEX:
         valid = value.matches("(?i)(0x)?[0-9a-f]+");
         break;

      case INTEGER:
         try {
            int n = Integer.parseInt(value);
            valid = true;

         } catch (NumberFormatException e) {
            valid = false;
         }
         break;

      case LOGICAL:
         valid = value.matches("(?i)true|false|yes|no|1|0");
         break;

      case STRING:
         valid = true;
         break;

      default:
         valid = true;
      }

      log.debug("isConfigTechValidValue: valid = {}: unit, data-type, value = '{}', '{}', '{}',",
                valid, unit, dt, value);

      return valid;
   }

   /**
    * Determines the data-type of a parameter unit
    * @param unit the unit name
    * @return data-type name
    */
   public String getConfigTechDataType(String name)
   {
      ConfigTechValueType dt = unitList.get(name);
      if (dt == null) {
         return null;
      } else {
         return dt.toString();
      }
   }

   /**
    * Determines if the name is a configuration/technical parameter unit
    * @param name the unit name in question.
    * @return True - unit is a valid technical parameter unit;
    *         False - parameter name is NOT valid (or tech spec not opened)
    *        
    */
   public boolean isConfigTechUnit(String name)
   {
      if (unitList == null) return false;
      return unitList.containsKey(name);
   }

   /**
    * Determines if the name is a DEPRECATED configuration/technical parameter unit
    * @param name the unit name in question.
    * @return True - unit is a valid technical parameter unit;
    *         False - parameter name is NOT valid (or tech spec not opened)
    *        
    */
   public boolean isDeprecatedConfigTechUnit(String name)
   {
      if (unitList_DEP == null) return false;
      return unitList_DEP.containsKey(name);
   }

   //...............................................................
   //.....................parseConfigParamFiles.....................
   //...........................................,...................
   /**
    * Parses the list of configurations parameters of the specification
    * @return True - file parsed; False - failed to parse file
    * @throws IOException indicates file read or permission error
    */
   public void parseConfigParamFiles () throws IOException
   {
      log.debug(".....parseConfigParamFiles: start.....");

      String[] fileNames = {coreCfgParmFileName,  bioCfgParmFileName,
                            coreCfgParmFileName+".deprecated",  bioCfgParmFileName+".deprecated"};
      LinkedHashSet<String> paramList = null;
      LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> paramRegex = null;

      //....loop over the active and deprecated files.....

      for (int nFile = 0; nFile < fileNames.length; nFile++) {
         String fileName = fileNames[nFile];

         log.debug("parsing '{}'", fileName);

         //.......parse the config param name file.......
         //..open the file
         File f = new File(fileName);
         if (! f.isFile()) {

            if (nFile <= 1) {
               //..both primary files MUST exist
               log.error("Config-Param-file '{}' does not exist", fileName);
               throw new IOException("Config-Parm-File '" + fileName +
                                     "' does not exist");
            } else {
               //..deprecated file MAY NOT exist
               log.debug("Deprecated-Config-Param-file '{}' does not exist (optional)",
                         fileName);
               continue;
            }

         } else if (! f.canRead()) {
            //..file exists but cannot be read --> error
            log.error("Config-Param-File '{}' cannot be read", fileName);
            throw new IOException("Config-Parm-File '" + fileName +
                                  "' cannot be read");
         }

         //..create list variables
         //..open file

         if (nFile == 0) {
            configParamList  = new LinkedHashSet<String>(250);
            configParamRegex = new LinkedHashMap<Pattern, HashMap<String, HashSet<String>>>(250);

            paramList = configParamList;
            paramRegex = configParamRegex;

         } else if (nFile == 2) {
            //..do this only for the first deprecated file
            configParamList_DEP  = new LinkedHashSet<String>(25);
            configParamRegex_DEP = new LinkedHashMap<Pattern, HashMap<String, HashSet<String>>>(25);

            paramList = configParamList_DEP;
            paramRegex = configParamRegex_DEP;
         }

         BufferedReader file = new BufferedReader(new FileReader(fileName));

         //.....read through the files....

         //..pattern to recognize/replace templates
         Pattern pTemplate = Pattern.compile("<([^>]+?)>");
         log.debug("template regex: '{}'", pTemplate);

         String line;
         while ((line = file.readLine()) != null) {
            if (pBlankOrComment.matcher(line).matches()) {
               log.debug ("blank/comment: '{}'", line);
               continue;
            }

            //..break the line into individual entries
            String column[] = line.split("\\|");
            for (int n = 0 ; n < column.length ; n++) {
               String s = column[n].trim();
               column[n] = s;
            }

            //..column[0] is the parameter name and includes an example unit
            //..need to strip off the unit

            int index = column[0].lastIndexOf('_');

            if (index <= 0) {
               //..poorly formed name
               file.close();
               throw new IOException("Config-Parm-File '" + fileName +
                                  "': Badly formed name '"+column[0]+"'");
            }

            //..well formed named, break it apart

            String param = column[0].substring(0, index);

            //..process parameter 
            
            Matcher matcher = pTemplate.matcher(param);

            if (! matcher.find()) {
               //..no <*> structures -- just a straight fixed name

               paramList.add(param);
               log.debug ("add param: '{}'", param);

            } else {
               //..contains <*> structures -- convert to a regex
               //..convert CONFIG_<ssn>CpAscentPhaseDepthZone<N>SampleRate_hertz
               //..to      CONFIG_(?<ssn>\w*)CpAscentPhaseDepthZone(?<N>\w*)SampleRate_hertz
               //..   similar for the other templates

               //........parse all templates off line........
               //..capture up to first template
               int start = matcher.start();
               StringBuilder regex = new StringBuilder();

               if (start > 0) {
                  regex.append(param.substring(0, matcher.start()));
               }
               log.debug ("regex line: param = '{}'", param);
               //log.debug ("...start regex: start, regex = '{}', '{}'", start, regex);

               //..add first group (already matched)
               String repl = templateReplacement.get(matcher.group(1));
               if (repl == null) {
                  repl = defaultTemplateReplacement;
                  log.warn("*** DEFAULT TEMPLATE ***");
               }

               regex.append(repl);
               //log.debug ("...add template: regex = '{}'", regex);
               
               //..loop over remaining templates

               int end_after = matcher.end();

               while (matcher.find()) {
                  start = matcher.start();
                  //log.debug("...end_after, start = '{}', '{}'", end_after, start);

                  if (end_after < start) {
                     regex.append(param.substring(end_after, start));
                     //log.debug ("...add literal: '{}'", regex);
                  }

                  repl = templateReplacement.get(matcher.group(1));
                  if (repl == null) {
                     repl = defaultTemplateReplacement;
                     log.warn("*** DEFAULT TEMPLATE ***");
                  }

                  regex.append(repl);
                  //log.debug ("...add template: '{}'", regex);

                  end_after = matcher.end();
               }

               //..patch last bit

               if (end_after < param.length()) {
                  regex.append(param.substring(end_after));
                  //log.debug ("...add ending literal: '{}'", regex);
               }
                               
               log.debug ("add regex: '{}'", regex);
               Pattern pRegex = Pattern.compile(regex.toString());

               //..........finished parsing templates..........

               //..decide if there are "match lists" to compare to
               //..- core_config_name spec does NOT have any match lines
               //..- bio_config_name spec can have "match lists" 

               HashMap<String, HashSet<String>> matchList = null;

               if (nFile == 1 || nFile == 3) {
                  //..bio-config file has matching list in these columns
                  String[] templates = 
                     {"shortsensorname", "param", "cyclephasename"};
                  int[] nColumn = {2, 3, 4, 5}; //.."0-based"

                  matchList = new HashMap<String, HashSet<String>>(templates.length);

                  for (int n = 0 ; n < templates.length ; n++) {
                     if (column.length > nColumn[n]) {
                        String[] list = column[nColumn[n]].split("[, /]");

                        HashSet<String> set = new HashSet<String>(list.length);

                        for (int m = 0; m < list.length; m++) {
                           String s = list[m].trim();
                           if (s.length() > 0) {
                              set.add(list[m].trim());
                           }
                        }

                        if (set.size() > 0) {
                           //..have to handle the "CTD" exception .. I hate exceptions
                           if (templates[n].equals("shortsensorname")) {
                              set.add("CTD");
                           }

                           matchList.put(templates[n], set);
                           log.debug("matchList: add '{}' = '{}'", templates[n], set);
                        }
                     }
                  }
               }

               if (matchList != null && matchList.size() > 0) {
                  paramRegex.put(pRegex, matchList);
               } else {
                  paramRegex.put(pRegex, null);
                  log.debug("matchList: null");
               }
            }
         } //..end while (line)

         file.close();
      } //..end for (fileNames)

      log.debug("configParamList: {}", configParamList);

      log.debug(".....parseConfigParamFiles: end.....");
   } //..end parseConfigParamFiles


   //............................................................
   //.....................parseTechParamFile.....................
   //............................................................
   /**
    * Parses the list of technical parameter names of the specification
    * There is a provision to detect deprecated units and produce warnings.
    * @return True - file parsed; False - failed to parse file
    * @throws IOException indicates file read or permission error
    */
   public void parseTechParamFile () throws IOException
   {
      log.debug(".....parseTechParamFile: start.....");

      String[] fileNames            = {tecParmFileName, tecParmFileName+".deprecated"};
      LinkedHashSet<String> paramList;
      LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> paramRegex;

      //....loop over the active and deprecated files.....

      for (int nFile = 0; nFile < fileNames.length; nFile++) {
         String fileName = fileNames[nFile];

         log.debug("parsing '{}'", fileName);

         //.......parse the tech param name file.......
         //..open the file
         File f = new File(fileName);
         if (! f.isFile()) {

            if (nFile == 0) {
               //..primary file MUST exist
               log.error("Tech-Param-file '{}' does not exist", fileName);
               throw new IOException("Tech-Parm-File '" + fileName +
                                     "' does not exist");
            } else {
               //..deprecated file MAY NOT exist
               log.debug("Deprecated-Tech-Param-file '{}' does not exist (optional)",
                         fileName);
               continue;
            }

         } else if (! f.canRead()) {
            //..file exists but cannot be read --> error
            log.error("Tech-Param-File '{}' cannot be read", fileName);
            throw new IOException("Tech-Parm-File '" + fileName +
                                  "' cannot be read");
         }

         //..create list variables
         //..open file

         if (nFile == 0) {
            techParamList  = new LinkedHashSet<String>(250);
            techParamRegex = new LinkedHashMap<Pattern, HashMap<String, HashSet<String>>>(250);

            paramList = techParamList;
            paramRegex = techParamRegex;

         } else {
            techParamList_DEP  = new LinkedHashSet<String>(25);
            techParamRegex_DEP = new LinkedHashMap<Pattern, HashMap<String, HashSet<String>>>(25);

            paramList = techParamList_DEP;
            paramRegex = techParamRegex_DEP;
         }       

         BufferedReader file = new BufferedReader(new FileReader(fileName));

         //.....read through the file....

         //..pattern to recognize/replace templates
         Pattern pTemplate = Pattern.compile("<([^>]+?)>");
         log.debug("template regex: '{}'", pTemplate);

         String line;
         while ((line = file.readLine()) != null) {
            if (pBlankOrComment.matcher(line).matches()) {
               log.debug ("blank/comment: '{}'", line);
               continue;
            }

            //..break the line into individual entries
            String column[] = line.split("\\|");
            for (int n = 0 ; n < column.length ; n++) {
               String s = column[n].trim();
               column[n] = s;
            }

            //..column[0] is the parameter name and includes an example unit
            //..need to strip off the unit

            int index = column[0].lastIndexOf('_');

            if (index <= 0) {
               //..poorly formed name
               file.close();
               throw new IOException("Technical-Parm-File '" + fileName +
                                  "': Badly formed name '"+column[0]+"'");
            }

            //..well formed named, break it apart

            String param = column[0].substring(0, index);

            //..process the parameter

            Matcher matcher = pTemplate.matcher(param);

            if (! matcher.find()) {
               //..no <*> structures -- just a straight fixed name

               paramList.add(param);
               log.debug ("add param: '{}'", param);

            } else {
               //..contains <*> structures -- convert to a regex
               //..convert CONFIG_<ssn>CpAscentPhaseDepthZone<N>SampleRate_hertz
               //..to      CONFIG_(?<ssn>\w*)CpAscentPhaseDepthZone(?<N>\w*)SampleRate_hertz

               //..capture up to first template
               int start = matcher.start();
               StringBuilder regex = new StringBuilder();

               if (start > 0) {
                  regex.append(param.substring(0, matcher.start()));
               }
               //log.debug ("...regex line: param = '{}'", param);
               //log.debug ("...start regex: start, regex = '{}', '{}'", start, regex);

               //..add first group (already matched)
               String repl = templateReplacement.get(matcher.group(1));

               if (repl == null) {
                  repl = defaultTemplateReplacement;
                  //log.warn("*** DEFAULT TEMPLATE ***");
               }

               regex.append(repl);
               //log.debug ("...add template: regex = '{}'", regex);
               
               //..loop over remaining templates

               int end_after = matcher.end();

               while (matcher.find()) {
                  start = matcher.start();
                  //log.debug("...end_after, start = '{}', '{}'", end_after, start);

                  if (end_after < start) {
                     regex.append(param.substring(end_after, start));
                     //log.debug ("...add literal: '{}'", regex);
                  }

                  repl = templateReplacement.get(matcher.group(1));
                  if (repl == null) {
                     repl = defaultTemplateReplacement;
                     //log.warn("*** DEFAULT TEMPLATE ***");
                  }

                  regex.append(repl);
                  //log.debug ("...add template: '{}'", regex);

                  end_after = matcher.end();
               }

               //..patch last bit

               if (end_after < param.length()) {
                  regex.append(param.substring(end_after));
                  //log.debug ("...add ending literal: '{}'", regex);
               }
                               
               log.debug ("add regex: '{}'", regex);
               Pattern pRegex = Pattern.compile(regex.toString());
               paramRegex.put(pRegex, null);
            }
         } //..end while (line)

         file.close();
      } //..end for (fileNames)

      log.debug(".....parseTechParamFile: end.....");
   } //..end parseTechParamFile


   //............................................................
   //.....................parseUnitFile.....................
   //............................................................
   /**
    * Parses the list of configuration/technical parameter units for the specification.
    * There is a provision to detect deprecated units and produce warnings.
    * @return True - file parsed; False - failed to parse file
    * @throws IOException indicates file read or permission error
    */
   public void parseUnitFile () throws IOException
   {
      log.debug(".....parseConfigTechUnitFile: start.....");

      String[] fileNames            = {unitFileName,  unitFileName+".deprecated"};
      LinkedHashMap<String, ConfigTechValueType> list;

      //....loop over the active and deprecated files.....

      for (int n = 0; n < fileNames.length; n++) {
         String fileName = fileNames[n];

         //.......parse the param unit file.......
         //..open the file
         File f = new File(fileName);
         if (! f.isFile()) {

            if (n == 0) {
               //..primary file MUST exist
               log.error("Config-Tech-Unit-file '{}' does not exist", fileName);
               throw new IOException("Config-Tech-Unit-File '" + fileName +
                                     "' does not exist");
            } else {
               //..deprecated file MAY NOT exist
               log.debug("Deprecated-Config-Tech-Unit-file '{}' does not exist (optional)",
                         fileName);
               continue;
            }

         } else if (! f.canRead()) {
            //..file exists but cannot be read --> error
            log.error("Config-Tech-Unit-File '{}' cannot be read", fileName);
            throw new IOException("Config-Tech-Unit-File '" + fileName +
                                  "' cannot be read");
         }

         //..create list variables
         //..open file

         if (n == 0) {
            unitList  = new LinkedHashMap<String, ConfigTechValueType>(100);
            list = unitList;

         } else {
            unitList_DEP  = new LinkedHashMap<String, ConfigTechValueType>(25);
            list = unitList_DEP;
         }       

         BufferedReader file = new BufferedReader(new FileReader(fileName));

         //.....read through the file....
         log.debug("parsing config/tech unit file '" + fileName + "'");

         String line;
         while ((line = file.readLine()) != null) {
            if (pBlankOrComment.matcher(line).matches()) {
               log.debug ("blank/comment: '{}'", line);
               continue;
            }

            //.....split the line: col 1 = unit name; col 2 = data type.....
            String st[] = line.split("\\|");

            if (st[0].length() > 0) {
               String unit_name = st[0].trim();

               ConfigTechValueType dt;

               if (st.length > 1) {
                  dt = ConfigTechValueType.getType(st[1].trim());
               } else {
                  dt = ConfigTechValueType.UNKNOWN;
               }

               log.debug("add unit: '{}' / '{}'", unit_name, dt);

               list.put(st[0].trim(), dt);
            }
         }

         file.close();
      }

   } //..end parseUnitFile

//...................................................................
//                    INNER CLASSES
//...................................................................

   public class ArgoConfigTechParamMatch
   {
      //......object variables........

      public boolean isDeprecated;
      public String match;
      public int nUnMatchedTemplates;
      public HashMap<String, String> unMatchedTemplates;
      public int nFailedMatchedTemplates;
      public HashMap<String, String> failedMatchedTemplates;

      //........constructors..........

      public ArgoConfigTechParamMatch (String match, boolean isDeprecated)
      {
         this.match = new String(match);
         this.isDeprecated = isDeprecated;
         this.nUnMatchedTemplates = 0;
         this.unMatchedTemplates = null;
         this.nFailedMatchedTemplates = 0;
         this.failedMatchedTemplates = null;
      }

      public ArgoConfigTechParamMatch (String match, boolean isDeprecated, 
                                       int nUnMatchedTemplates,
                                       HashMap<String, String> unMatchedTemplates,
                                       int nFailedMatchedTemplates,
                                       HashMap<String, String> failedMatchedTemplates)
      {
         this.match = new String(match);
         this.isDeprecated = isDeprecated;
         this.nUnMatchedTemplates = nUnMatchedTemplates;
         this.unMatchedTemplates = unMatchedTemplates;
         this.nFailedMatchedTemplates = nFailedMatchedTemplates;
         this.failedMatchedTemplates = failedMatchedTemplates;
      }
   }


} //..end class
