package fr.coriolis.checker.specs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.tables.ArgoNVSReferenceTable;
import fr.coriolis.checker.tables.SkosConcept;
import fr.coriolis.checker.utils.NvsDefinitionParser;

/**
 * Implements features to check the Meta-data CONFIG_PARAMETER_NAME (including
 * LAUNCH_...) and TECHNICAL_PARAMETER_NAME. This includes the <i>units</i>
 * allowed for both.
 * 
 * @author Mark Ignaszewski
 * @version $HeadURL:
 *          https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoConfigTechParam.java
 *          $
 * @version $Id: ArgoConfigTechParam.java 1257 2021-04-26 14:09:25Z ignaszewski
 *          $
 */
public class ArgoConfigTechParam {
//............................................
//               VARIABLES
//............................................

	// ..........Class variables..............

	public static enum ConfigTechValueType {
		DATE_TIME("date/time"), FLOAT("float"), HEX("hex"), INTEGER("integer"), LOGICAL("logical"), STRING("string"),
		UNKNOWN("unknown");

		public final String name;

		ConfigTechValueType(String d) {
			name = d;
		}

		public static ConfigTechValueType getType(String name) {
			for (ConfigTechValueType c : ConfigTechValueType.values()) {
				if (name.equals(c.name)) {
					return c;
				}
			}
			return ConfigTechValueType.UNKNOWN;
		}

		@Override
		public String toString() {
			return name;
		}
	};

	// ......define and initialize the template replacement patterns......

	static final String defaultTemplateReplacement;
	static final Map<String, String> templateReplacement;
	static final Set<String> knownTemplates;
	static final Set<String> knownTemplatesWithMatchListToCheck;
	static {
		defaultTemplateReplacement = "(?<default>\\w+)";

		Map<String, String> temp = new HashMap<String, String>(10);
		// ..config templates (alphabetical order)
		temp.put("D", "(?<D>\\d+?)");
		temp.put("cycle_phase_name", "(?<cyclephasename>[A-Z][a-z]+(?:[A-Z][a-z]+)*?Phase)");
		temp.put("I", "(?<I>\\d+?)");
		temp.put("N", "(?<N>\\d+?)");
		temp.put("N+1", "(?<N1>\\d+?)");
		temp.put("param", "(?<param>[A-Z][a-z]+(?:[A-Z][a-z]+)??)");
		temp.put("PARAM", "(?<PARAM>[A-Z]+?)");
		temp.put("S", "(?<S>\\d+?)");
		temp.put("SubS", "(?<SubS>\\d+?)");
		temp.put("short_sensor_name", "(?<shortsensorname>[A-Z][a-z]+?|CTD)");
		// ..tech templates (alphabetical order)
		temp.put("digit", "(?<digit>\\d)");
		temp.put("int", "(?<int>\\d+?)");
		// ..already defined above
		temp.put("Z", "(?<Z>\\d+?)");
		templateReplacement = Collections.unmodifiableMap(temp);

		knownTemplates = new HashSet<>(Arrays.asList("D", "horizontalphasename", "I", "N", "N1", "param", "PARAM", "S",
				"SubS", "shortsensorname", "verticalphasename", "cyclephasename", "digit", "int", "Z"));
		knownTemplatesWithMatchListToCheck = new HashSet<>(Arrays.asList("horizontalphasename", "N", "N1", "digit",
				"param", "PARAM", "shortsensorname", "verticalphasename", "cyclephasename"));

	}
	// ......define and initialize the pattern matcher objects......
	static Pattern pBlankOrComment; // ..match a blank line or a comment line
	static Pattern pComment; // ..comment

	static {
		// ..match a blank line (or a line with just comments)
		pBlankOrComment = Pattern.compile("^\\s*(?://.*)*");

		// ..match a comment (any where on the line) - recognize the "//@" comments
		// ..group 1: the word following "@"; group 2: the setting
		pComment = Pattern.compile("//(?:@\\s*(\\w+)\\s*=\\s*\"(.+)\")?.*$");
	}

	// .....object variables......

	private String unitFileName;
	private String version;

	private LinkedHashSet<String> configParamList; // ..config param fixed names
	private LinkedHashSet<String> configParamList_DEP; // ..config param fixed names (deprctd)
	// private LinkedHashSet<Pattern> configParamRegex; //..config param variable
	// names
	// private LinkedHashSet<Pattern> configParamRegex_DEP; //..config param
	// variable names
	private LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> configParamRegex; // ..config param variable names
	private LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> configParamRegex_DEP; // ..config param variable
																							// names

	private LinkedHashSet<String> techParamList; // ..tech param fixed names
	private LinkedHashSet<String> techParamCodeList; // ..full tech param names -with unit- : reference table 14a

	private LinkedHashSet<String> techParamList_DEP; // ..tech param fixed names (deprctd)
	private LinkedHashSet<String> techParamCodeList_DEP;

	// The following map is needed to check TECH time series variable (same variable
	// name but different units and long_name posssible) :
	private Map<String, List<String>> paramAuthorizedUnits = new HashMap<>();
	private Map<String, List<String>> paramAuthorizedLongName = new HashMap<>();

	// private LinkedHashSet<Pattern> techParamRegex; //..tech param regex names
	// private LinkedHashSet<Pattern> techParamRegex_DEP; //..tech param regex names
	// (deprctd)
	private LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> techParamRegex; // ..config param variable names
	private LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> techParamRegex_DEP; // ..config param variable
																							// names

	private LinkedHashMap<String, ConfigTechValueType> unitList; // ..config/tech units and type
	private LinkedHashMap<String, ConfigTechValueType> unitList_DEP; // ..config/tech units - deprecated

	// ..logger
	private static final Logger log = LogManager.getLogger("ArgoConfigTechParam");

//............................................
//              CONSTRUCTORS
//............................................

	/**
	 * Reads the CONFIGURATION_PARAMETER and TECHNICAL_PARAMETER specification
	 * files. <br>
	 * The files must be in specDir and are named
	 * <i>argo-config_names-spec-v&lt;version&gt;</i> and
	 * <i>"argo-tech_names-spec-v&lt;version&gt;</i>
	 *
	 * @param specDir    Path to the specification file directory
	 * @param version    Argo netCDF file version
	 * @param initConfig true: Read CONFIGURATION_PARAMETER specification file
	 * @param initTech   true: Read TECHNICAL_PARAMETER specification file
	 * @throws IOException File I/O errors
	 */

	public ArgoConfigTechParam(String version, boolean initConfig, boolean initTech) throws IOException {
		log.debug("...ArgoConfigTechParam: start...");

		this.version = version.trim();
		String prefix = "argo-";

		unitFileName = prefix + "tech_units-spec-v" + this.version;

		if (initConfig) {
			parseConfigParamFiles();
		}
		if (initTech) {
			parseTechParamFile();
		}

		parseUnitFile();

		if (log.isDebugEnabled()) {
			if (configParamList == null) {
				log.debug("configParamList size = null");
			} else {
				log.debug("configParamList size = {}", configParamList.size());
			}
			if (configParamList_DEP == null) {
				log.debug("configParamList_DEP size = null");
			} else {
				log.debug("configParamList_DEP size = {}", configParamList_DEP.size());
			}
			if (techParamList == null) {
				log.debug("techParamList size = null");
			} else {
				log.debug("techParamList size = {}", techParamList.size());
			}
			if (techParamList_DEP == null) {
				log.debug("techParamList_DEP size = null");
			} else {
				log.debug("techParamList_DEP size = {}", techParamList_DEP.size());
			}
			log.debug("...ArgoConfigTechParam: end...");
		}
	}

//............................................
//               METHODS
//............................................

	/**
	 * Determines if the name is matched by a REGEX in the set
	 * 
	 * @param name     The parameter name in question.
	 * @param regexSet The set of REGEXPs to check
	 * @return An ArgoConfigTechParamMatch containing full information about the
	 *         match (null = NO MATCH)
	 */
	private ArgoConfigTechParamMatch checkRegex(String name,
			HashMap<Pattern, HashMap<String, HashSet<String>>> regexSet) {
		ArgoConfigTechParamMatch match = null; // ..return object
		HashMap<String, String> unMatchedTemplates = null; // ..return templates
		HashMap<String, String> failedMatchedTemplates = null; // ..return templates

		// ..NOTE..NOTE..NOTE..
		// ..Should be able to truncate the loop below on the first match
		// ..ie, find a match -> return
		// ..However, during development I *really* want to know if there are
		// ..multiple matches - which indicates ambiguous regex patterns
		// ..So, FOR NOW, complete the looping and report multiple matches
		// ..Returns the LAST one matched

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

						// ..is there a "match-list" for this (regex param and template)

						if (value != null) {
							HashSet<String> matchSet = value.get(key);

							if (matchSet == null) {
								// ..there was no match-list to compare to
								unMatchedTemplates.put(key, str);

							} else {
								// ..there is a match-list - compare it

								if (!matchSet.contains(str)) {
									failedMatchedTemplates.put(key, str);
									log.debug("checkRegex: match-list success: key '{}', matched '{}'", key, str);

								} else if (log.isDebugEnabled()) {
									log.debug("checkRegex: match-list failed: key '{}', matched '{}'", key, str);
								}
							}
						} else {
							unMatchedTemplates.put(key, str);
						}

					} catch (IllegalArgumentException e) {
					}
				}

				match = new ArgoConfigTechParamMatch(pattern.toString(), false, unMatchedTemplates.size(),
						unMatchedTemplates, failedMatchedTemplates.size(), failedMatchedTemplates);

				log.debug("checkRegex: '{}' regex match #{}: '{}', unMatchedTemplates {}" + "failedTemplates {}",
						unMatchedTemplates.size(), failedMatchedTemplates.size());

				// ****temporary**** return match;
			}
		}

		return match;
	} // ..checkRegex

	/**
	 * Determines if the name is a CONFIG parameter name
	 * 
	 * @param name the parameter name in question.
	 * @return An ArgoConfigTechParamMatch containing full information about the
	 *         match (null = NO MATCH)
	 * 
	 */
	public ArgoConfigTechParamMatch findConfigParam(String name) {
		ArgoConfigTechParamMatch match = findParam(name, configParamList, configParamRegex, configParamList_DEP,
				configParamRegex_DEP);
		return match;
	}

	/**
	 * Determines if the name is a Tech parameter name
	 * 
	 * @param name the parameter name in question.
	 * @return An ArgoConfigTechParamMatch containing full information about the
	 *         match (null = NO MATCH)
	 * 
	 */
	public ArgoConfigTechParamMatch findTechParam(String name) {
		ArgoConfigTechParamMatch match = findParam(name, techParamList, techParamRegex, techParamList_DEP,
				techParamRegex_DEP);
		return match;
	}

	/**
	 * Determines if the name is a valid parameter name within the supplied lists
	 * 
	 * @param name the parameter name in question.
	 * @return An ArgoConfigTechParamMatch containing full information about the
	 *         match (null = NO MATCH)
	 * 
	 */
	private ArgoConfigTechParamMatch findParam(String name, HashSet<String> activeList,
			HashMap<Pattern, HashMap<String, HashSet<String>>> activeRegex, HashSet<String> deprecatedList,
			HashMap<Pattern, HashMap<String, HashSet<String>>> deprecatedRegex) {
		boolean literal = false;
		HashMap<String, String> templates = null;
		ArgoConfigTechParamMatch match = null;

		if (activeList != null) {
			if (activeList.contains(name)) {
				match = new ArgoConfigTechParamMatch(name, false);

				log.debug("findParam: '{}': active literal match", name);
				return match;
			}
		}

		// ..did NOT match one of the literal strings
		// ..check for a regex match

		if (activeRegex != null) {
			// log.debug("findParam: checking active regex");

			match = checkRegex(name, activeRegex);

			if (match != null) {
				match.isDeprecated = false;
				log.debug("findParam: '{}': active regex match '{}'", name, match.match);
				return match;
			}
		}

		if (deprecatedList != null) {
			if (deprecatedList.contains(name)) {
				match = new ArgoConfigTechParamMatch(name, true);

				log.debug("findParam: '{}': deprecated literal match", name);
				return match;
			}
		}

		if (deprecatedRegex != null) {
			// log.debug("findParam: checking active regex");

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
	 * Return the TechParam list defined in spec files (argo-tech_names-spec-v*)
	 * 
	 * @return
	 */
	public LinkedHashSet<String> getTechParamList() {
		return techParamList;
	}

	public LinkedHashSet<String> getTechCodeList() {
		return techParamCodeList;
	}

	public Map<String, List<String>> getParamAuthorizedUnits() {
		return paramAuthorizedUnits;
	}

	public Map<String, List<String>> getParamAuthorizedLongName() {
		return paramAuthorizedLongName;
	}

	/**
	 * Return the TechParam list containing a regex defined in spec files
	 * (argo-tech_names-spec-v*)
	 * 
	 * @return
	 */
	public LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> getTechParamRegex() {
		return techParamRegex;
	}

	/**
	 * Determines if the value is valid for the unit (mapped to data-type)
	 * 
	 * @param unit  the unit name
	 * @param value the config value
	 * @return True - value is a valid setting for the given unit (or unit is
	 *         unknown) False - parameter name is NOT valid (or tech spec not
	 *         opened)
	 * 
	 */
	public boolean isConfigTechValidValue(String unit, String value) {
		// ..turn the unit name into a data type
		ConfigTechValueType dt = unitList.get(unit);

		if (dt == null || dt == ConfigTechValueType.UNKNOWN) {
			// ..unit name is unknown OR the data type for this unit is unkown
			// ..can't check the value setting
			// ..so blindly return "it's good" (???)
			log.debug("isConfigTechValidValue: default to true: unit, data-type, value = '{}', '{}', '{}'", unit, dt,
					value);

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

		log.debug("isConfigTechValidValue: valid = {}: unit, data-type, value = '{}', '{}', '{}',", valid, unit, dt,
				value);

		return valid;
	}

	/**
	 * Determines the data-type of a parameter unit
	 * 
	 * @param unit the unit name
	 * @return data-type name
	 */
	public String getConfigTechDataType(String name) {
		ConfigTechValueType dt = unitList.get(name);
		if (dt == null) {
			return null;
		} else {
			return dt.toString();
		}
	}

	/**
	 * Determines if the name is a configuration/technical parameter unit
	 * 
	 * @param name the unit name in question.
	 * @return True - unit is a valid technical parameter unit; False - parameter
	 *         name is NOT valid (or tech spec not opened)
	 * 
	 */
	public boolean isConfigTechUnit(String name) {
		if (unitList == null) {
			return false;
		}
		return unitList.containsKey(name);
	}

	/**
	 * Determines if the name is a DEPRECATED configuration/technical parameter unit
	 * 
	 * @param name the unit name in question.
	 * @return True - unit is a valid technical parameter unit; False - parameter
	 *         name is NOT valid (or tech spec not opened)
	 * 
	 */
	public boolean isDeprecatedConfigTechUnit(String name) {
		if (unitList_DEP == null) {
			return false;
		}
		return unitList_DEP.containsKey(name);
	}

	// ...............................................................
	// .....................parseConfigParamFiles.....................
	// ...........................................,...................
	/**
	 * Parses the list of configurations parameters of the specification
	 * 
	 * @return True - file parsed; False - failed to parse file
	 * @throws IOException indicates file read or permission error
	 */
	public void parseConfigParamFiles() throws IOException {
		log.debug(".....parseConfigParamFiles: start.....");

		// ..pattern to recognize/replace templates
		Pattern pTemplate = Pattern.compile("<([^>]+?)>");
		log.debug("template regex: '{}'", pTemplate);

		configParamList = new LinkedHashSet<String>(250);
		configParamRegex = new LinkedHashMap<Pattern, HashMap<String, HashSet<String>>>(250);

		configParamList_DEP = new LinkedHashSet<String>(250);
		configParamRegex_DEP = new LinkedHashMap<Pattern, HashMap<String, HashSet<String>>>(250);

		// ....loop over the active and deprecated entries.....
		for (SkosConcept configParamEntry : ArgoNVSReferenceTable.CONFIG_PARAMETER_NAME_TABLE
				.getConceptMembersByAltLabelMap().values()) {
			if (!configParamEntry.isDeprecated()) {
				parseParamName(configParamList, configParamRegex, "NVS R18 table", pTemplate, configParamEntry);

			} else {
				// it is a deprecated config param
				parseParamName(configParamList_DEP, configParamRegex_DEP, "NVS R18 table", pTemplate, configParamEntry);

			}
		}

		log.debug("configParamList: {}", configParamList);

		log.debug(".....parseConfigParamFiles: end.....");

	} // ..end parseConfigParamFiles

	/**
	 * parse a line from the table 18 (ist of configurations parameters) and add
	 * results (param name, regex pattern if exists) to the corresponding variable.
	 * 
	 * @param paramList
	 * @param paramRegex
	 * @param nFile
	 * @param fileName
	 * @param pTemplate
	 * @param column
	 */
	private void parseParamName(LinkedHashSet<String> paramList,
			LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> paramRegex, String tableName, Pattern pTemplate,
			SkosConcept paramEntry) {

		// ..need to strip off the unit
		String paramCode = paramEntry.getPrefLabel();
		String param = extractParamNameAndUnitFromParamCode(tableName, paramCode)[0];
		String unit = extractParamNameAndUnitFromParamCode(tableName, paramCode)[1]; // not useful of the moment as we
																						// authorize all units from the
																						// ref list
		// ..process parameter
		Matcher matcher = pTemplate.matcher(param);

		if (!matcher.find()) {
			// ..no <*> structures -- just a straight fixed name
			paramList.add(param);

			log.debug("add param: '{}'", param);

		} else {
			// ..contains <*> structures -- convert to a regex
			// ..convert CONFIG_<ssn>CpAscentPhaseDepthZone<N>SampleRate_hertz
			// ..to CONFIG_(?<ssn>\w*)CpAscentPhaseDepthZone(?<N>\w*)SampleRate_hertz
			// .. similar for the other templates

			// ........parse all templates off line........
			// ..capture up to first template
			String regexString = convertParamStringToRegex(param, matcher);

			log.debug("add regex: '{}'", regexString);
			Pattern pRegex = Pattern.compile(regexString);

			// ..........finished parsing templates..........

			// ..decide if there are "match lists" to compare to
			// ..- core_config_name spec does NOT have any match lines
			// ..- bio_config_name spec can have "match lists"
			// 02/2026 : no distinction between bio and core when using NVS table

			HashMap<String, HashSet<String>> matchList = null;

			// if (nFile == 1 || nFile == 3) { // BIO config name files
			// ..bio-config file has matching list in these columns
			// String[] templates = { "shortsensorname", "param", "cyclephasename" };

			// int[] nColumn = { 2, 3, 4, 5 }; // .."0-based"

			matchList = new HashMap<String, HashSet<String>>();

			// get template_values defined in Definition field :
			Map<String, String> paramTemplateValues = NvsDefinitionParser.parseAttributes("Template_Values",
					paramEntry.getDefinition());

			buildTemplatesMatchingListFromNVSParamTemplateValues(matchList, paramTemplateValues);

			if (matchList != null && matchList.size() > 0) {
				paramRegex.put(pRegex, matchList);
			} else {
				paramRegex.put(pRegex, null);
				log.debug("matchList: null");
			}
		}
	}

	// ............................................................
	// .....................parseTechParamFile.....................
	// ............................................................
	/**
	 * Parses the list of technical parameter names of the specification There is a
	 * provision to detect deprecated units and produce warnings.
	 * 
	 * @return True - file parsed; False - failed to parse file
	 * @throws IOException indicates file read or permission error
	 */
	public void parseTechParamFile() throws IOException {
		// ===========
		// CK_0093 1/2
		// ===========
		log.debug(".....parseTechParamFile: start.....");

		techParamList = new LinkedHashSet<String>(250);
		techParamRegex = new LinkedHashMap<Pattern, HashMap<String, HashSet<String>>>(250);
		techParamCodeList = new LinkedHashSet<String>(250);

		techParamList_DEP = new LinkedHashSet<String>(250);
		techParamRegex_DEP = new LinkedHashMap<Pattern, HashMap<String, HashSet<String>>>(250);
		techParamCodeList_DEP = new LinkedHashSet<String>(250);

		// ..pattern to recognize/replace templates
		Pattern pTemplate = Pattern.compile("<([^>]+?)>");

		// loop over tech paramaters PrefLabel list:
		for (SkosConcept techParamEntry : ArgoNVSReferenceTable.TECHNICAL_PARAMETER_NAME_TABLE
				.getConceptMembersByAltLabelMap().values()) {
			if (!techParamEntry.isDeprecated()) {
				parseParamName(techParamList, techParamRegex, "NVS R14 table", pTemplate, techParamEntry);
				// We want full
			} else {
				// it is a deprecated tech param

				parseParamName(techParamList_DEP, techParamRegex_DEP, "NVS R14 table", pTemplate, techParamEntry);
			}
		}
		log.debug(".....parseTechParamFile: end.....");

	} // ..end parseTechParamFile

	/**
	 * Returns the complete set of all possible parameter names across all patterns
	 * in configParamRegex.
	 *
	 * @return A Set of all concrete parameter names, deduplicated across all
	 *         patterns.
	 */
	protected Set<String> getAllPossibleParamNames(
			LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> paramRegex) {
		return generateParamListsFromConfigRegex(paramRegex).values().stream().flatMap(List::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Builds, for each Pattern in configParamRegex, the list of all possible
	 * parameter names by substituting named groups with their allowed values.
	 *
	 * @return A map associating each Pattern to its list of concrete parameter
	 *         names. Patterns with no injectable group values are mapped to an
	 *         empty list.
	 */
	private Map<Pattern, List<String>> generateParamListsFromConfigRegex(
			LinkedHashMap<Pattern, HashMap<String, HashSet<String>>> paramRegex) {
		Map<Pattern, List<String>> result = new LinkedHashMap<>();

		for (Map.Entry<Pattern, HashMap<String, HashSet<String>>> entry : paramRegex.entrySet()) {
			Pattern pattern = entry.getKey();
			HashMap<String, HashSet<String>> groupValues = entry.getValue();

			List<String> paramList = generateParamListFromPatternAndValues(pattern.pattern(), groupValues);
			result.put(pattern, paramList);
		}

		return result;
	}

	/**
	 * Generates all concrete strings from a regex pattern by replacing each named
	 * group with all its allowed values.
	 *
	 * @param pRegex      The regex pattern string (e.g.
	 *                    "(?<env>...)_(?<region>...)").
	 * @param groupValues Map of group name -> set of allowed values for that group.
	 * @return List of concrete strings, or empty list if no group could be
	 *         substituted.
	 */
	private List<String> generateParamListFromPatternAndValues(String pRegex,
			HashMap<String, HashSet<String>> groupValues) {

		if (groupValues == null || groupValues.isEmpty()) {
			return new ArrayList<>();
		}

		List<String> regexList = new ArrayList<>(Collections.singletonList(pRegex));
		boolean replacedAtLeastOne = false;

		for (Map.Entry<String, HashSet<String>> entry : groupValues.entrySet()) {
			String groupName = entry.getKey();
			Set<String> values = entry.getValue();

			if (values == null || values.isEmpty()) {
				continue;
			}

			// Match the named group in the regex: (?<groupName>...)
			String namedGroupPattern = "\\(\\?<" + groupName + ">.*?\\)";

			boolean groupFound = regexList.stream().anyMatch(r -> Pattern.compile(namedGroupPattern).matcher(r).find());

			if (!groupFound) {
				continue;
			}

			replacedAtLeastOne = true;

			regexList = regexList.stream().flatMap(
					currentRegex -> generateStringsFromPattern(currentRegex, values, namedGroupPattern).stream())
					.collect(Collectors.toList());
		}

		return replacedAtLeastOne ? regexList : new ArrayList<>();
	}

	private void buildTemplatesMatchingListFromNVSParamTemplateValues(HashMap<String, HashSet<String>> matchList,
			Map<String, String> configParamTemplateValues) {

		for (Map.Entry<String, String> entry : configParamTemplateValues.entrySet()) {
			String normalizedKey = entry.getKey().replace("_", "");// need to replace short_sensor_name,
																	// cycle_phase_name by shortsensorname and
			// cyclephasename
			String value = entry.getValue();

			String[] templateValues = ArgoFileSpecification
					.getValuesListFromParameterAttribute(ArgoFileSpecification.getOrEmptyStringFromValue(value));

			HashSet<String> set = new HashSet<>(Arrays.asList(templateValues));

			// "CTD" exception (always authorized for short_sensor_name ???) :
			if (normalizedKey.equals("shortsensorname")) {
				set.add("CTD");
			}
			if (!set.isEmpty() && knownTemplatesWithMatchListToCheck.contains(normalizedKey)) {
				matchList.put(normalizedKey, set);
				// special case for N and N+1 : N+1 has the "N" key in template values so need
				// to replicate the set but with +1 to last value
				if (normalizedKey.equals("N")) {
					// Create a new set for N1 = N values + (max + 1)
					HashSet<String> setN1 = new HashSet<>(set); // copy N set

					int maxN = set.stream().mapToInt(Integer::parseInt).max().orElse(0);

					setN1.add(String.valueOf(maxN + 1));
					matchList.put("N1", setN1);
				}
			}

		}
	}

	public String extractSpecificValueFromPattern(String originalPattern, String actualString, Pattern pRegex,
			String placeholderName) {
		Map<String, String> values = extractValuesFromPattern(originalPattern, actualString, pRegex);
		return values.get(placeholderName);
	}

	/**
	 * Exctract the value which has been used to replace placeholder <*> in a
	 * string. return a Map with placeholder name as key and exctrated value as
	 * value. Example : originalPattern :
	 * "NUMBER_<short_sensor_name>AscentSamplesDepthZone<Z>" actual string :
	 * "NUMBER_CroverAscentSamplesDepthZone1" => result = ["short_sensor_name"
	 * :"Crover", "Z" : "1"]
	 *
	 * @param template
	 * @param concreteValue
	 * @param pRegex
	 * @return
	 */
	private Map<String, String> extractValuesFromPattern(String originalPattern, String concreteValue, Pattern pRegex) {

		Map<String, String> extractedValues = new HashMap<>();

		List<String> placeholderNames = extractPlaceholderNames(originalPattern);

		// presence of <*>
		Matcher matcher = pRegex.matcher(concreteValue);

		if (matcher.matches()) {
			// Associate each captured group to his placeholder name
			for (int i = 0; i < placeholderNames.size() && i < matcher.groupCount(); i++) {
				String placeholderName = placeholderNames.get(i);
				String value = matcher.group(i + 1); // group start at 1
				extractedValues.put(placeholderName, value);
			}
		}

		return extractedValues;

	}

	private List<String> extractPlaceholderNames(String pattern) {
		List<String> names = new ArrayList<>();
		Pattern placeholderPattern = Pattern.compile("<([^>]+)>");
		Matcher matcher = placeholderPattern.matcher(pattern);

		while (matcher.find()) {
			names.add(matcher.group(1));
		}

		return names;
	}

	private List<String> generateStringsFromPattern(String pRegex, Set<String> values, String regexToReplace) {
		if (!Pattern.compile(regexToReplace).matcher(pRegex).find()) {
			// Pattern not processed (pattern to replace absent)
			return Collections.singletonList(pRegex); // return the original pRegex

		}
		return values.stream().map(value -> pRegex.toString().replaceAll(regexToReplace, value))
				.collect(Collectors.toList());

	}

	/**
	 * contains <*> structures -- convert to a regex convert
	 * CONFIG_<ssn>CpAscentPhaseDepthZone<N>SampleRate_hertz to
	 * CONFIG_(?<ssn>\w*)CpAscentPhaseDepthZone(?<N>\w*)SampleRate_hertz
	 * 
	 * @param param
	 * @param matcher
	 * @return
	 */
	private String convertParamStringToRegex(String param, Matcher matcher) {
		StringBuilder regex = new StringBuilder();

		// ..capture up to first template
		int start = matcher.start();
		if (start > 0) {
			regex.append(param.substring(0, matcher.start()));
		}

		// ..add first group (already matched)
		String repl = templateReplacement.get(matcher.group(1));

		if (repl == null) {
			repl = defaultTemplateReplacement;
		}

		regex.append(repl);

		// ..loop over remaining templates

		int end_after = matcher.end();

		while (matcher.find()) {
			start = matcher.start();
			// log.debug("...end_after, start = '{}', '{}'", end_after, start);

			if (end_after < start) {
				regex.append(param.substring(end_after, start));
				// log.debug ("...add literal: '{}'", regex);
			}

			repl = templateReplacement.get(matcher.group(1));
			if (repl == null) {
				repl = defaultTemplateReplacement;
				// log.warn("*** DEFAULT TEMPLATE ***");
			}

			regex.append(repl);
			// log.debug ("...add template: '{}'", regex);

			end_after = matcher.end();
		}

		// ..patch last bit

		if (end_after < param.length()) {
			regex.append(param.substring(end_after));
			// log.debug ("...add ending literal: '{}'", regex);
		}
		return regex.toString();
	}

	/**
	 * Parameter code in Table 14a or table 18 contain an example unit at the end of
	 * string. Example : "PRES_SurfaceOffsetTruncatedPlus5dbar_dbar". This method
	 * extract the tech param name by stripping off the unit. Example :
	 * "PRES_SurfaceOffsetTruncatedPlus5dbar"
	 *
	 * @param fileName
	 * @param file
	 * @param parameterCode
	 * @return parameterName
	 * @throws IOException
	 */
	private String[] extractParamNameAndUnitFromParamCode(String fileName, String parameterCode)
			throws IllegalArgumentException {
		// ..column[0] is the parameter name and includes an example unit
		// ..need to strip off the unit

		int index = parameterCode.lastIndexOf('_');

		if (index <= 0) {
			throw new IllegalArgumentException(
					"Technical/Config-Param-File '" + fileName + "': Badly formed name '" + parameterCode + "'");
		}

		// ..well formed named, break it apart

		String parameterName = parameterCode.substring(0, index);
		String parameterUnit = parameterCode.substring(index + 1, parameterCode.length());
		String[] results = { parameterName, parameterUnit };

		return results;

	}

	// ............................................................
	// .....................parseUnitFile.....................
	// ............................................................
	/**
	 * Parses the list of configuration/technical parameter units for the
	 * specification. There is a provision to detect deprecated units and produce
	 * warnings.
	 * 
	 * @return True - file parsed; False - failed to parse file
	 * @throws IOException indicates file read or permission error
	 */
	public void parseUnitFile() throws IOException {
		log.debug(".....parseConfigTechUnitFile: start.....");
		// ===========
		// CK_0095 1/2
		// ===========
		String[] fileNames = { unitFileName, unitFileName + ".deprecated" };
		LinkedHashMap<String, ConfigTechValueType> list;

		// ....loop over the active and deprecated files.....

		for (int n = 0; n < fileNames.length; n++) {
			String fileName = fileNames[n];

			// .......parse the param unit file.......
			// ..open the file

			try (InputStream in = SpecIO.getInstance().open(fileName);
					BufferedReader fileReader = new BufferedReader(
							new InputStreamReader(in, StandardCharsets.UTF_8));) {

				// ..create list variables
				// ..open file

				if (n == 0) {
					unitList = new LinkedHashMap<String, ConfigTechValueType>(100);
					list = unitList;

				} else {
					unitList_DEP = new LinkedHashMap<String, ConfigTechValueType>(25);
					list = unitList_DEP;
				}

				// .....read through the file....
				log.debug("parsing config/tech unit file '" + fileName + "'");

				String line;
				while ((line = fileReader.readLine()) != null) {
					if (pBlankOrComment.matcher(line).matches()) {
						log.debug("blank/comment: '{}'", line);
						continue;
					}

					// .....split the line: col 1 = unit name; col 2 = data type.....
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

				fileReader.close();

			} catch (FileNotFoundException e) {
				if (n == 0) {
					// ..primary file MUST exist
					log.error("Config-Tech-Unit-file '{}' does not exist", fileName);
					throw e;
				} else {
					// ..deprecated file MAY NOT exist
					log.debug("Deprecated-Config-Tech-Unit-file '{}' does not exist (optional)", fileName);
					continue;
				}

			} catch (IOException e) {
				log.error("Config-Tech-Unit-File '{}' cannot be read", fileName);
				throw e;
			}

		}

	} // ..end parseUnitFile

//...................................................................
//                    INNER CLASSES
//...................................................................

	public class ArgoConfigTechParamMatch {
		// ......object variables........

		public boolean isDeprecated;
		public String match;
		public int nUnMatchedTemplates;
		public HashMap<String, String> unMatchedTemplates;
		public int nFailedMatchedTemplates;
		public HashMap<String, String> failedMatchedTemplates;

		// ........constructors..........

		public ArgoConfigTechParamMatch(String match, boolean isDeprecated) {
			this.match = new String(match);
			this.isDeprecated = isDeprecated;
			this.nUnMatchedTemplates = 0;
			this.unMatchedTemplates = null;
			this.nFailedMatchedTemplates = 0;
			this.failedMatchedTemplates = null;
		}

		public ArgoConfigTechParamMatch(String match, boolean isDeprecated, int nUnMatchedTemplates,
				HashMap<String, String> unMatchedTemplates, int nFailedMatchedTemplates,
				HashMap<String, String> failedMatchedTemplates) {
			this.match = new String(match);
			this.isDeprecated = isDeprecated;
			this.nUnMatchedTemplates = nUnMatchedTemplates;
			this.unMatchedTemplates = unMatchedTemplates;
			this.nFailedMatchedTemplates = nFailedMatchedTemplates;
			this.failedMatchedTemplates = failedMatchedTemplates;
		}
	}

} // ..end class
