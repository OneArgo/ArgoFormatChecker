package fr.coriolis.checker.specs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements the capabilities associated with "reference tables":
 * <ul>
 * <li>Read the table from the spec directory
 * <li>Build "cross-reference" lists
 * </ul>
 * Tables with various types of keys are allowed: String, char, int.
 * 
 * @version $HeadURL:
 *          https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoReferenceTable.java
 *          $
 * @version $Id: ArgoReferenceTable.java 1261 2021-06-14 17:28:43Z ignaszewski $
 */

public final class ArgoReferenceTable {

	// .........................................................
	// VARIABLES
	// .........................................................

	// ..........class variables..........

	public static enum DACS {
		AOML("aoml"), BODC("bodc"), CORIOLIS("coriolis"), CSIO("csio"), CSIRO("csiro"), INCOIS("incois"), JMA("jma"),
		KMA("kma"), KORDI("kordi"), MEDS("meds"), NMDIS("nmdis"), KIOST("kiost");

		public final String name;

		DACS(String d) {
			name = d;
		}
	};

	private static boolean initialized = false;

	// ..match a blank line (or a line with just comments)
	static Pattern pBlankOrComment;

	// ..match a "deprecated" / "deleted" line;
	static Pattern pActive;
	static Pattern pDeprecated;
	static Pattern pDeleted;
	static Pattern pRefused;
	static Pattern pUnderway;

	// ..match a string ending in a digit
	static Pattern pEndsInDigit;

	// .....reference table 2: QC Flags.....
	// .....reference table 2a: Profile QC Flags.....
	public static CharTable QC_FLAG;
	public static CharTable PROFILE_QC_FLAG;

	// ....reference table 4 --- but not quite. Also includes DAC to center-code
	// mapping......
	public static final EnumMap<DACS, String> DacCenterCodes = new EnumMap<DACS, String>(DACS.class);

	// .....reference table 5: location classes.....
	public static CharTable LOCATION_CLASS;

	// .....reference table 6: Data State Indicators.....
	public static StringTable DATA_STATE_INDICATOR;

	// .....reference table 8.....
	public static IntegerTable WMO_INST_TYPE;

	// .....reference table 9.....
	public static StringTable POSITIONING_SYSTEM;

	// .....reference table 10.....
	public static StringTable TRANS_SYSTEM;

	// .....reference table 16.....
	public static StringTable VERTICAL_SAMPLING_SCHEME;

	// .....reference table 19.....
	public static CharTable STATUS_FLAG;

	// .....reference table 20: Grounded flags.....
	public static CharTable GROUNDED;

	// .....reference table 22.....
	public static StringTable PLATFORM_FAMILY;

	// .....reference table 23.....
	public static StringTable PLATFORM_TYPE;

	// .....reference table 24.....
	public static StringTable PLATFORM_MAKER;

	public static StringTableCrossReference PLATFORM_TYPExPLATFORM_MAKER;
	public static StringTableCrossReference PLATFORM_TYPExWMO_INST;

	// .....reference table 25.....
	public static StringTable SENSOR;

	// .....reference table 26.....
	public static StringTable SENSOR_MAKER;

	// .....reference table 27.....
	public static StringTable SENSOR_MODEL;

	public static StringTableCrossReference SENSOR_MODELxSENSOR;
	public static StringTableCrossReference SENSOR_MODELxSENSOR_MAKER;

	public static HashSet<String> SHORT_SENSOR_NAME;

	// .....reference table 29.....
	public static LinkedHashSet<String> BATTERY_TYPE_manufacturer;
	public static LinkedHashSet<String> BATTERY_TYPE_type;

	// .....reference table 29.....
	public static LinkedHashSet<String> BATTERY_PACKS_style;
	public static LinkedHashSet<String> BATTERY_PACKS_type;

	// .....reference table 41.....
	public static StringTable PROGRAM_NAME;

	// .....Measurement_codes..........
	public static IntegerTable MEASUREMENT_CODE_specific;
	public static IntegerTable MEASUREMENT_CODE_toJuldVariable;

	// .....Generic Parameter Templates.....
	public static LinkedHashSet<String> GENERIC_TEMPLATE_short_sensor_name;
	public static LinkedHashSet<String> GENERIC_TEMPLATE_cycle_phase_name;
	public static LinkedHashSet<String> GENERIC_TEMPLATE_param;

	// ..logger
	private static final Logger log = LogManager.getLogger("ArgoReferenceTable");

	// .................class initializer...............

	static {
		DacCenterCodes.put(DACS.AOML, "AO MB NA PM SI UW WH");
		DacCenterCodes.put(DACS.BODC, "BO");
		DacCenterCodes.put(DACS.CORIOLIS, "AW GE IO IF LV RU SP VL");
		DacCenterCodes.put(DACS.CSIO, "HZ");
		DacCenterCodes.put(DACS.CSIRO, "CS");
		DacCenterCodes.put(DACS.INCOIS, "IN");
		DacCenterCodes.put(DACS.JMA, "JA JM");
		DacCenterCodes.put(DACS.KMA, "KM");
		DacCenterCodes.put(DACS.KORDI, "KO");
		DacCenterCodes.put(DACS.KIOST, "KO");
		DacCenterCodes.put(DACS.MEDS, "CI ME");
		DacCenterCodes.put(DACS.NMDIS, "NM");

		pBlankOrComment = Pattern.compile("^\\s*(?://.*)*");
		pActive = Pattern.compile("(?i)(active|approved).*");
		pDeprecated = Pattern.compile("(?i)deprecated.*");
		pDeleted = Pattern.compile("(?i)(deleted|obsolete).*");
		pRefused = Pattern.compile("(?i)refused.*");
		pUnderway = Pattern.compile("(?i)(publication|creation) +underway.*");
		pEndsInDigit = Pattern.compile("\\d$");
	}

	// .................................................................
	// CONSTRUCTORS
	// .................................................................

	public ArgoReferenceTable(String specDir) throws IOException {
		String prefix = specDir.trim() + File.separator + "ref_table-";

		if (initialized) {
			log.debug(".....ArgoReferenceTable: already initialized.....");
			return; // ..already initialized
		}

		log.debug(".....ArgoReferenceTable constructor: start.....");

		initialized = true;

		// .....reference table 2....
		// .....reference table 2a....
		QC_FLAG = new CharTable(prefix + "2");
		PROFILE_QC_FLAG = new CharTable(prefix + "2a");

		// .....reference table 5....
		LOCATION_CLASS = new CharTable(prefix + "5");

		// .....reference table 6....
		DATA_STATE_INDICATOR = new StringTable(prefix + "6");

		// .....reference table 8....
		WMO_INST_TYPE = new IntegerTable(prefix + "8");

		// .....reference table 9....
		POSITIONING_SYSTEM = new StringTable(prefix + "9");

		// .....reference table 10....
		TRANS_SYSTEM = new StringTable(prefix + "10");

		// .....reference table 16....
		VERTICAL_SAMPLING_SCHEME = new StringTable(prefix + "16");

		// .....reference table 19....
		STATUS_FLAG = new CharTable(prefix + "19");

		// .....reference table 20....
		GROUNDED = new CharTable(prefix + "20");

		// .....reference table 22....
		// .....reference table 23....
		// .....reference table 24....
		PLATFORM_FAMILY = new StringTable(prefix + "22");
		PLATFORM_TYPE = new StringTable(prefix + "23");
		PLATFORM_MAKER = new StringTable(prefix + "24");

		// ..platform_type/platform_maker cross-reference

		final int PLATFORM_TYPE_COLUMN = 1;
		final int WMO_INST_COLUMN = 3;
		final int PLATFORM_MAKER_COLUMN = 4;

		log.debug("...build PLATFORM_TYPE:PLATFORM_MAKER cross-reference...");

		PLATFORM_TYPExPLATFORM_MAKER = new StringTableCrossReference(PLATFORM_TYPE, PLATFORM_TYPE_COLUMN,
				PLATFORM_MAKER_COLUMN);

		log.debug("...build PLATFORM_TYPE:WMO_INST cross-reference...");

		PLATFORM_TYPExWMO_INST = new StringTableCrossReference(PLATFORM_TYPE, PLATFORM_TYPE_COLUMN, WMO_INST_COLUMN);

		// .....reference table 25....
		SENSOR = new StringTable(prefix + "25", true); // ..true = allowAppendedDigit

		// .....reference table 26....
		SENSOR_MAKER = new StringTable(prefix + "26");

		// .....reference table 27....
		SENSOR_MODEL = new StringTable(prefix + "27");

		// .....reference table 41....
		PROGRAM_NAME = new StringTable(prefix + "41");

		// ..append platform_maker entries to sensor_maker table
		// ..ADMT-19 confirmed this as a requirment

		log.debug("...append PLATFORM_MAKER to SENSOR_MAKER...");
		SENSOR_MAKER.add(PLATFORM_MAKER);

		// ..sensor_model/sensor cross-reference

		log.debug("...build SENSOR_MODEL:SENSOR cross-reference...");

		final int SENSOR_MODEL_COLUMN = 1;
		final int SENSOR_MAKER_COLUMN = 2;
		final int SENSOR_LIST_COLUMN = 4;

		SENSOR_MODELxSENSOR = new StringTableCrossReference(SENSOR_MODEL, SENSOR_MODEL_COLUMN, SENSOR_LIST_COLUMN);

		SENSOR_MODELxSENSOR_MAKER = new StringTableCrossReference(SENSOR_MODEL, SENSOR_MODEL_COLUMN,
				SENSOR_MAKER_COLUMN);

		// ..harvest short_sensor_name from ref table 27
		// log.debug("...build short_sensor_name set...");
		// final int SHORT_SENSOR_COLUMN = 5;
		// SHORT_SENSOR_NAME = SENSOR_MODEL.getColumnSet(SHORT_SENSOR_COLUMN);

		// ..battery_type manufacturer, type
		BATTERY_TYPE_manufacturer = new LinkedHashSet<String>(10);
		readSetString(specDir.trim() + File.separator + "ref_table-29.manufacturer", BATTERY_TYPE_manufacturer);

		BATTERY_TYPE_type = new LinkedHashSet<String>(10);
		readSetString(specDir.trim() + File.separator + "ref_table-29.type", BATTERY_TYPE_type);

		// ..battery_packs style, type
		BATTERY_PACKS_style = new LinkedHashSet<String>(10);
		readSetString(specDir.trim() + File.separator + "ref_table-30.style", BATTERY_PACKS_style);

		BATTERY_PACKS_type = new LinkedHashSet<String>(10);
		readSetString(specDir.trim() + File.separator + "ref_table-30.type", BATTERY_PACKS_type);

		// .....measurement codes - specific codes....
		// .....measurement codes - map to JULD variables
		MEASUREMENT_CODE_specific = new IntegerTable(
				specDir.trim() + File.separator + "measurement_code-specific_codes");
		MEASUREMENT_CODE_toJuldVariable = new IntegerTable(
				specDir.trim() + File.separator + "measurement_code-juld_variables");

		// .....Generic Parameter Templates.....

		GENERIC_TEMPLATE_short_sensor_name = new LinkedHashSet<String>(10);
		readSetString(specDir.trim() + File.separator + "generic_template_short_sensor_name",
				GENERIC_TEMPLATE_short_sensor_name);

		GENERIC_TEMPLATE_cycle_phase_name = new LinkedHashSet<String>(10);
		readSetString(specDir.trim() + File.separator + "generic_template_cycle_phase_name",
				GENERIC_TEMPLATE_cycle_phase_name);

		GENERIC_TEMPLATE_param = new LinkedHashSet<String>(10);
		readSetString(specDir.trim() + File.separator + "generic_template_param", GENERIC_TEMPLATE_param);
	}

	// ..................................................................
	// METHODS
	// ..................................................................

	private void readMapCharString(String fileName, LinkedHashMap<Character, ArgoReferenceEntry> map,
			LinkedHashMap<Pattern, ArgoReferenceEntry> map_re) throws IOException {
		log.debug("...start readMapCharString...");
		log.debug("parsing file '{}'", fileName);

		File file = new File(fileName);
		if (!file.isFile()) {
			log.error("File '" + fileName + "' does not exist");
			throw new IOException("File ('" + fileName + "') does not exist");
		} else if (!file.canRead()) {
			log.error("File '" + fileName + "' cannot be read");
			throw new IOException("File ('" + fileName + "') cannot be read");
		}

		BufferedReader fileReader = new BufferedReader(new FileReader(fileName));

		// ..read through the file

		String line;
		while ((line = fileReader.readLine()) != null) {
			if (pBlankOrComment.matcher(line).matches()) {
				log.debug("skipped line = '{}'", line);
				continue;
			}

			log.debug("line = '{}'", line);

			// .....parse the line into individual entries.....
			String st[] = line.split("\\|");
			for (int n = 0; n < st.length; n++) {
				String s = st[n].trim();
				st[n] = s;
			}

			char ca[] = st[0].toCharArray();
			char c;
			if (ca.length == 0) {
				c = ' ';
			} else {
				c = ca[0];
			}

			map.put(Character.valueOf(c), new ArgoReferenceEntry(st));
		}
		fileReader.close();
		log.debug("...end readMapCharString...");
	} // ..end readMapCharString

	private void readMapStringString(String fileName, LinkedHashMap<String, ArgoReferenceEntry> map,
			LinkedHashMap<Pattern, ArgoReferenceEntry> map_re, boolean allowAppendedDigit) throws IOException {
		log.debug("...start readMapStringString...");
		log.debug("parsing file '{}'", fileName);

		File file = new File(fileName);
		if (!file.isFile()) {
			log.error("File '" + fileName + "' does not exist");
			throw new IOException("File ('" + fileName + "') does not exist");
		} else if (!file.canRead()) {
			log.error("File '" + fileName + "' cannot be read");
			throw new IOException("File ('" + fileName + "') cannot be read");
		}

		BufferedReader fileReader = new BufferedReader(new FileReader(fileName));

		String anyStringReg = "\\\\w+"; // ..forces <*> to be something
		log.debug("anyString regex substituion: '{}'", anyStringReg);
		String anyOrNoWordReg = "\\\\w*"; // ..forces <*> to be something or nothing
		log.debug("anyOrNoWord regex substituion: '{}'", anyOrNoWordReg);
		String anyOrNoStringReg = ".*"; // ..forces <*> to be something or nothing
		log.debug("anyOrNoString regex substituion: '{}'", anyOrNoStringReg);
		String anyNumReg = "\\\\d+"; // ..forces <nnn> to be a number
		log.debug("anyNum regex substituion: '{}'", anyNumReg);

		// ..read through the file

		String line;
		while ((line = fileReader.readLine()) != null) {
			if (pBlankOrComment.matcher(line).matches()) {
				log.debug("skipped line = '{}'", line);
				continue;
			}

			log.debug("line = '{}'", line);

			// .....parse the line into individual columns.....
			String st[] = line.split("\\|");
			for (int n = 0; n < st.length; n++) {
				String s = st[n].trim();
				st[n] = s;
			}

			String key = st[0];

			ArgoReferenceEntry info = new ArgoReferenceEntry(st);

			int flag = 0;
			String reg;

			if (key.indexOf('<') < 0) {
				// ..no <*> structures -- just a straight fixed name

				map.put(key, info);
				log.debug("add literal: '{}'", key);

				if (allowAppendedDigit) {
					// ADMT-2025 : duplicate SENSOR must be named <SENSOR>_<n>
					// so if sensor finish by a number, the duplicate will be xxxx[number]_n
					// if sensor don't finish by a number, the duplicate will alsobe xxxx_n.
					reg = key + "_\\d";

					Pattern pRegex = Pattern.compile(reg, flag);

					map_re.put(pRegex, info);

					log.debug("allowAppendedDigit: add regex:    '{}'", pRegex);
				}

			} else {
				// ..contains <*> structures -- convert to a regex

				reg = key.replaceAll("<[nN]+>", anyNumReg).replaceAll("<\\*>", anyOrNoStringReg).replaceAll("<\\w+?>",
						anyOrNoWordReg);

				Pattern pRegex = Pattern.compile(reg, flag);

				map_re.put(pRegex, info);

				log.debug("add regex:    '{}' for entry '{}'", pRegex, key);
			}
		}
		fileReader.close();
		log.debug("...end readMapStrinString...");
	} // ..end readMapStringString

	private void readMapIntegerString(String fileName, LinkedHashMap<Integer, ArgoReferenceEntry> map,
			LinkedHashMap<Pattern, ArgoReferenceEntry> map_re) throws IOException {
		log.debug("...start readMapIntegerString...");
		log.debug("parsing file '{}'", fileName);

		File file = new File(fileName);
		if (!file.isFile()) {
			log.error("File '" + fileName + "' does not exist");
			throw new IOException("File ('" + fileName + "') does not exist");
		} else if (!file.canRead()) {
			log.error("File '" + fileName + "' cannot be read");
			throw new IOException("File ('" + fileName + "') cannot be read");
		}

		BufferedReader fileReader = new BufferedReader(new FileReader(fileName));

		// ..read through the file

		String line;
		while ((line = fileReader.readLine()) != null) {
			if (pBlankOrComment.matcher(line).matches()) {
				log.debug("skipped line = '{}'", line);
				continue;
			}

			log.debug("line = '{}'", line);

			// .....parse the line into individual entries.....
			String st[] = line.split("\\|");
			for (int n = 0; n < st.length; n++) {
				String s = st[n].trim();
				st[n] = s;
			}

			ArgoReferenceEntry info = new ArgoReferenceEntry(st);

			map.put(Integer.valueOf(st[0]), info);
		}
		fileReader.close();
		log.debug("...end readMapIntegerString...");
	} // ..end readMapIntegerString

	private void readSetString(String fileName, LinkedHashSet<String> set) throws IOException {
		log.debug("...start readSetString...");
		log.debug("parsing file '{}'", fileName);

		File file = new File(fileName);
		if (!file.isFile()) {
			log.error("File '" + fileName + "' does not exist");
			throw new IOException("File ('" + fileName + "') does not exist");
		} else if (!file.canRead()) {
			log.error("File '" + fileName + "' cannot be read");
			throw new IOException("File ('" + fileName + "') cannot be read");
		}

		BufferedReader fileReader = new BufferedReader(new FileReader(fileName));

		// ..read through the file

		String line;
		while ((line = fileReader.readLine()) != null) {
			if (pBlankOrComment.matcher(line).matches()) {
				log.debug("skipped line = '{}'", line);
				continue;
			}

			log.debug("line = '{}'", line);

			// .....parse the line into individual entries.....
			// String st[] = line.split("\\|");
			// for (int n = 0 ; n < st.length ; n++) {
			// String s = st[n].trim();
			// st[n] = s;
			// }

			// ArgoReferenceEntry info = new ArgoReferenceEntry(st);

			set.add(line.trim());
		}
		fileReader.close();
		log.debug("...end readSetString...");
	} // ..end readSetString

	// .................................................................
	// INNER CLASSES
	// .................................................................

	public final class ArgoReferenceEntry {
		// ..............VARIABLES...............

		public boolean isActive;
		public boolean isDeprecated;
		public boolean isDeleted;
		public boolean isRefused;
		public boolean isUnderway;

		public String message;

		public Vector<String> columns;

		// ...............CONSTRUCTORS...................

		public ArgoReferenceEntry(String[] columns) {
			if (columns.length > 1) {
				parseStatus(columns[columns.length - 1].trim());

			} else {
				this.isActive = true;
				this.isDeprecated = false;
				this.isDeleted = false;
				this.isRefused = false;
				this.isUnderway = false;

				this.message = new String();
			}

			this.columns = new Vector<String>(columns.length);

			for (String s : columns) {
				this.columns.add(s);
			}
		} // ..end constructor ArgoReferenceEntry(String[])

		public ArgoReferenceEntry(boolean noEntry) {
			this.isActive = false;
			this.isDeprecated = false;
			this.isDeleted = false;
			this.isRefused = false;
			this.isUnderway = false;

			this.message = "Invalid";
			this.columns = new Vector<String>(0);
		}

		public boolean isValid() {
			return (this.isActive || this.isDeprecated);
		}

		public String getColumn(int n) {
			return columns.get(n - 1);
		}

		private void parseStatus(String status) {
			this.isActive = this.isDeprecated = this.isDeleted = false;
			this.isRefused = this.isUnderway = false;

			boolean isStatus = false;

			if (pActive.matcher(status).matches()) { // || "approved"
				log.debug("...active: '{}'", status);
				isActive = true;
				isStatus = true;
			} else if (pDeprecated.matcher(status).matches()) {
				log.debug("...deprecated: '{}'", status);
				isDeprecated = true;
				isStatus = true;
			} else if (pDeleted.matcher(status).matches()) { // || "obsolete"
				log.debug("...deleted: '{}'", status);
				isDeleted = true;
				isStatus = true;
			} else if (pRefused.matcher(status).matches()) {
				log.debug("...refused: '{}'", status);
				isRefused = true;
				isStatus = true;
			} else if (pUnderway.matcher(status).matches()) { // || either "underway"
				log.debug("...underway: '{}'", status);
				isUnderway = true;
				isStatus = true;
			}

			if (isStatus) {
				// ..matched a known status - save the messsage
				this.message = status;

			} else { // ..assume active
				isActive = true;
				this.message = "Invalid";
			}
		}

	} // ..end class ArgoReferenceEntry

	public final class CharTable {
		String name;
		private LinkedHashMap<Character, ArgoReferenceEntry> table;
		private LinkedHashMap<Pattern, ArgoReferenceEntry> table_re;

		public final ArgoReferenceEntry noEntry;

		public CharTable(String table_name) throws IOException {
			this.name = table_name;
			this.table = new LinkedHashMap<Character, ArgoReferenceEntry>();
			this.table_re = new LinkedHashMap<Pattern, ArgoReferenceEntry>();

			readMapCharString(table_name, this.table, this.table_re);

			noEntry = new ArgoReferenceEntry(true);
		}

		public ArgoReferenceEntry contains(char ch) {
			// ..check for a "literal" match
			ArgoReferenceEntry entry = table.get(ch);
			if (entry != null) {
				log.debug("contains (CharTable): table '{}': contains '{}'", name, ch);
				return entry;
			}

			/*
			 * //..not literal match..check for regex match if (! table_re.isEmpty()) { for
			 * (Pattern p : table_re.keySet()) { if (p.matcher(ch).matches()) {
			 * log.debug("containsChar: table '{}'. '{}' matches regex '{}'", name, ch, p);
			 * return table_re.get(p); } //log.debug("table '{}'. '{}' does NOT match '{}'",
			 * tableName, ch, p); } }
			 */

			// ..no match
			log.debug("contains (CharTable): table '{}': '{}' no match", name, ch);

			return noEntry;
		}
	} // ..end class CharTable

	// ............... class IntegerTable .................

	public final class IntegerTable {
		String name;
		private LinkedHashMap<Integer, ArgoReferenceEntry> table;
		private LinkedHashMap<Pattern, ArgoReferenceEntry> table_re;

		public final ArgoReferenceEntry noEntry;

		public IntegerTable(String table_name) throws IOException {
			this.name = table_name;
			this.table = new LinkedHashMap<Integer, ArgoReferenceEntry>();
			this.table_re = new LinkedHashMap<Pattern, ArgoReferenceEntry>();

			readMapIntegerString(table_name, this.table, this.table_re);

			noEntry = new ArgoReferenceEntry(true);
		}

		public ArgoReferenceEntry contains(Integer N) {
			// ..check for a "literal" match
			ArgoReferenceEntry entry = table.get(N);
			if (entry != null) {
				log.debug("contains (IntegerTable): table '{}'. '{}' matches literal", name, N);
				return entry;
			}

			/*
			 * //..not literal match..check for regex match if (! table.table_re.isEmpty())
			 * { for (Pattern p : table.table_re.keySet()) { if (p.matcher(N).matches()) {
			 * log.debug("containsInteger: table '{}'. '{}' matches regex '{}'", table.name,
			 * N, p); return table.table_re.get(p); }
			 * //log.debug("table '{}'. '{}' does NOT match '{}'", tableName, name, p); } }
			 */

			// ..no match
			log.debug("contains (IntegerTable): table '{}'. '{}' no match", name, N);
			return noEntry;
		} // ..end contains(Integer N)

		public Set<Integer> keySet() {
			return table.keySet();
		}

		public ArgoReferenceEntry get(Integer N) {
			return table.get(N);
		}

		public Collection<ArgoReferenceEntry> values() {
			return table.values();
		}
	} // ..end class IntegerTable

	// .................... class StringTable ........................

	public final class StringTable {
		String name;
		private LinkedHashMap<String, ArgoReferenceEntry> table;
		private LinkedHashMap<Pattern, ArgoReferenceEntry> table_re;

		public final ArgoReferenceEntry noEntry;

		public StringTable(String table_name) throws IOException {
			this(table_name, false);
		}

		public StringTable(String table_name, boolean allowAppendedDigit) throws IOException {
			this.name = table_name;
			this.table = new LinkedHashMap<String, ArgoReferenceEntry>();
			this.table_re = new LinkedHashMap<Pattern, ArgoReferenceEntry>();

			readMapStringString(table_name, this.table, this.table_re, allowAppendedDigit);

			noEntry = new ArgoReferenceEntry(true);
		}

		public ArgoReferenceEntry get(String s) {
			return table.get(s);
		}

		public ArgoReferenceEntry get(Pattern p) {
			return table_re.get(p);
		}

		public Set<String> literalKeys() {
			return table.keySet();
		}

		public Set<Pattern> regexKeys() {
			return table_re.keySet();
		}

		public void add(String s, ArgoReferenceEntry e) {
			table.put(s, e);
		}

		public void add(Pattern p, ArgoReferenceEntry e) {
			table_re.put(p, e);
		}

		public void add(StringTable t) {
			for (String s : t.literalKeys()) {
				if (!table.containsKey(s)) {
					table.put(s, t.get(s));
					log.debug("StringTable.add: add '{}'", s);
				} else {
					log.debug("StringTable.add: skip add '{}'. already exists", s);
				}
			}

			for (Pattern s : t.regexKeys()) {
				if (!table_re.containsKey(s)) {
					table_re.put(s, t.get(s));
					log.debug("StringTable.add: add '{}'", s);
				} else {
					log.debug("StringTable.add: skip add '{}'. already exists", s);
				}
			}
		}

		public ArgoReferenceEntry contains(String key) {
			String str = key.trim();

			// ..check for a "literal" match
			ArgoReferenceEntry entry = table.get(str.trim());
			if (entry != null) {
				log.debug("contains (StringTable): table '{}'. '{}' matches literal", name, str);
				return entry;
			}

			// ..not literal match..check for regex match

			if (!table_re.isEmpty()) {
				for (Pattern p : table_re.keySet()) {
					if (p.matcher(str).matches()) {
						log.debug("contains (StringTable): table '{}'. '{}' matches regex '{}'", name, str, p);
						return table_re.get(p);
					}
					// log.debug("table '{}'. '{}' does NOT match '{}'", tableName, name, p);
				}
			}

			// ..no match
			log.debug("contatins (StringTable): table '{}'. '{}' no match", name, str);
			return noEntry;
		} // ..end contains(String str)

		public int size() {
			return (table.size() + table_re.size());
		}

		public HashSet<String> getColumnSet(int column) {
			log.debug("...getColumnSet: start...");

			HashSet<String> columnSet = new HashSet<String>();

			for (String s : table.keySet()) {
				ArgoReferenceEntry info = table.get(s);
				String[] list = info.getColumn(column).split("[, /]");

				for (int n = 0; n < list.length; n++) {
					String ss = list[n].trim();

					if (ss.length() != 0) {
						String ssn = ss.substring(0, 1) + ss.substring(1).toLowerCase();
						boolean added = columnSet.add(ssn);

						if (added) {
							log.debug("...table entry: key, value '{}', '{}'", s, ssn);
						}
					}
				}
			}

			for (Pattern s : table_re.keySet()) {
				ArgoReferenceEntry info = table.get(s);
				String[] list = info.getColumn(column).split("[, /]");

				for (int n = 0; n < list.length; n++) {
					String ss = list[n].trim();

					if (ss.length() != 0) {
						String ssn = ss.substring(0, 1) + ss.substring(1).toLowerCase();
						boolean added = columnSet.add(ssn);

						if (added) {
							log.debug("...table_re entry: key, value '{}', '{}'", s, ssn);
						}
					}
				}
			}

			log.debug("...getColumnSet: end...");

			return (columnSet);
		} // ..end getColumnSet

	} // ..end class StringTable

	// ...................... class StringTableCrossReference ......................

	public final class StringTableCrossReference {
		private LinkedHashMap<String, String[]> table;

		public StringTableCrossReference() {
			table = new LinkedHashMap<String, String[]>();
		}

		public StringTableCrossReference(StringTable srcTable, int key_col, int ref_col) {
			log.debug("...StringTableCrossReference constructor: start...");

			table = new LinkedHashMap<String, String[]>(srcTable.size());

			for (String s : srcTable.literalKeys()) {
				ArgoReferenceEntry info = srcTable.get(s);
				String key = info.getColumn(key_col);

				String[] list = info.getColumn(ref_col).split("[, /]");
				String[] t_list = new String[list.length];

				for (int n = 0; n < list.length; n++) {
					t_list[n] = list[n].trim();
				}

				if (t_list.length == 1 && t_list[0].length() == 0) {
					// ..if the entry is empty (blank) - make it mean "match anything or nothing"
					table.put(key, new String[0]);
					log.debug("StringTableCrossReference: set to empty: {} {}", srcTable.name, key);

				} else {
					table.put(key, t_list);
				}
			}

			for (Pattern p : srcTable.regexKeys()) {
				ArgoReferenceEntry info = srcTable.get(p);
				String key = info.getColumn(key_col);

				String[] list = info.getColumn(ref_col).split("[, /]");
				String[] t_list = new String[list.length];

				for (int n = 0; n < list.length; n++) {
					t_list[n] = list[n].trim();
				}

				// ..if the entry is empty (blank) - make it mean "match anything or nothing"
				if (t_list.length == 1 && t_list[0].length() == 0) {
					// ..if the entry is empty (blank) - make it mean "match anything or nothing"
					table.put(key, new String[0]);
					log.debug("StringTableCrossReference: set to empty: {} {}", srcTable.name, key);

				} else {
					table.put(key, t_list);
				}
			}

			for (String str : table.keySet()) {
				for (String e : table.get(str)) {
					log.debug("'{}':'{}'", str, e);
				}
			}

			log.debug("...StringTableCrossReference constructor: end...");
		}

		public boolean xrefContains(String key, String xref) {
			String[] slist = table.get(key);

			if (slist == null) {
				// ..the key was not in the xfer table
				// ..if the key is valid, then the table is faulty
				log.debug("xrefContains: key not found");
				return false;
			}

			if (slist.length == 0) {
				// ..there is no entry in the cross-reference
				// ..it matches by default
				log.debug("xrefContains: key xref empty. default match");
				return true;
			}

			// ..look for the sensor in the list of valid sensors

			for (String s : slist) {
				if (xref.equals(s)) {
					return true;
				}
			}

			return false;
		}
	}
}
