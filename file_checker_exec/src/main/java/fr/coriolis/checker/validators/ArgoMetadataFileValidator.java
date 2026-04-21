package fr.coriolis.checker.validators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.core.ArgoDataFile;
import fr.coriolis.checker.specs.ArgoConfigTechParam;
import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.specs.ArgoReferenceTable;
import fr.coriolis.checker.tables.ArgoNVSReferenceTable;
import fr.coriolis.checker.tables.SkosConcept;
import ucar.ma2.ArrayChar;
import ucar.nc2.Variable;

/**
 * Extends ArgoDataFile with features specific to an Argo Meta-data file.
 * <p>
 * A new file can be opened using the constructor. Opening a new file will
 * create an Argo-compliant "template" with all of the fixed values filled in.
 * Float-specific values will (obviously) have to be added by the user.
 * <p>
 * An existing file is opened using "open".
 * <p>
 * The format of the files can be checked with "verifyFormat" (see
 * ArgoDataFile). The data consistency can be validated with "validate".
 * <p>
 * 
 * @version $HeadURL:
 *          https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoMetadataFile.java
 *          $
 * @version $Id: ArgoMetadataFile.java 1314 2022-04-13 00:20:48Z ignaszewski $
 */

public class ArgoMetadataFileValidator extends ArgoFileValidator {

	// .........................................y
	// VARIABLES
	// .........................................

	// ..class variables
	// ..standard i/o shortcuts
	private static final Logger log = LogManager.getLogger("ArgoMetadataFile");

	private static Pattern pBatteryType;
	private static Pattern pBatteryPacks;

	static {
		// ..example: TADIRAN Alkaline 12 V
		// .. regex description:
		// .. leading spaces allowed space(s) space(s) space(s) space(s) [spaces(s)]
		// .. word-char(s) word-char(s) digit(s)[.digit(s)]] "V"
		pBatteryType = Pattern.compile("\\s*(?<manufacturer>\\w+)\\s+(?<type>\\w+)\\s+(?<volts>\\d+\\.?\\d*)\\s+V\\s*");

		// ..example: 4DD Li
		// .. regex description:
		// .. leading spaces allowed space(s) [space(s)]
		// .. digit(s) word-char(s) word-char(s)
		pBatteryPacks = Pattern.compile("\\s*(?<numofpacks>\\d+)(?<style>\\w+)\\s+(?<type>\\w+)\\s*");
	}

	// ..object variables

	// .......................................
	// CONSTRUCTORS
	// .......................................

	public ArgoMetadataFileValidator(ArgoDataFile arFile) {
		super(arFile);
	}

	// ..........................................
	// METHODS
	// ..........................................

//	/**
//	 * Opens an existing file and the assoicated <i>Argo specification</i>).
//	 *
//	 * @param inFile   the string name of the file to open
//	 * @param specDir  the string name of the directory containing the format
//	 *                 specification files
//	 * @param fullSpec true = open the full specification; false = open the template
//	 *                 specification
//	 * @return the file object reference. Returns null if the file is not opened
//	 *         successfully. (ArgoMetadataFile.getMessage() will return the reason
//	 *         for the failure to open.)
//	 * @throws IOException If an I/O error occurs
//	 */
//	public static ArgoMetadataFile open(String inFile, String specDir, boolean fullSpec) throws IOException {
//		ArgoDataFile arFile = ArgoDataFile.open(inFile, specDir, fullSpec);
//		if (!(arFile instanceof ArgoMetadataFile)) {
//			ValidationResult.lastMessage = "ERROR: '" + inFile + "' not an Argo META-DATA file";
//			return null;
//		}
//
//		return (ArgoMetadataFile) arFile;
//	}

	/**
	 * Validates the data in the meta-data file. This is a driver routine that
	 * performs all types of validations (see other validate* routines).
	 * 
	 * <p>
	 * Performs the following validations:
	 * <ol>
	 * <li>validateStringNulls -- if ckNulls = true
	 * <li>validateDates
	 * <li>(version 2.2 and earlier) validateHighlyDesirable_v2
	 * <li>(version 3+) validateMandatory_v3
	 * <li>validateBattery
	 * <li>validateConfigMission
	 * <li>validateConfigParams
	 *
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param dacName name of the DAC for this file
	 * @param ckNulls true = check all strings for NULL values; false = skip
	 * @return success indicator. true - validation was performed. false -
	 *         validation could not be performed (getMessage() will return the
	 *         reason).
	 * @throws IOException If an I/O error occurs
	 */
	public boolean validateData(boolean ckNulls) throws IOException {
		boolean basicsChecks = super.basicDataValidation(ckNulls);
		if (!basicsChecks) {
			return false;
		}
		// .......do meta-data file specific validations..........

		validateDates();

		if (this.arFile.fileVersion().trim().compareTo("2.2") <= 0) {
			validateHighlyDesirable_v2(this.arFile.getValidatedDac());
		} else {
			validateMandatory_v3(this.arFile.getValidatedDac());
			validateOptionalParams();
			validateConfigMission();
			validateConfigParams();
			validateBattery();

		}

		return true;
	}// ..end validate

	private void validateOptionalParams() {
		// PROGRAM_NAME - ref table 41
		checkOptionalParameterValueAgainstRefTable("PROGRAM_NAME",
				ArgoNVSReferenceTable.PROGRAM_NAME_TABLE.getConceptMembersByAltLabelMap(), true);
	}

	/**
	 * Validates the dates in the meta-data file.
	 * 
	 * Date Checks
	 * <ul>
	 * <li>DATE_CREATION: Must be set; not before earliest Argo date; before current
	 * time
	 * <li>DATE_UPDATE: Must be set; not before DATE_CREATION; before current time
	 * <li>LAUNCH_DATE: After earliest Argo date
	 * <li>START_DATE: Not before LAUNCH_DATE (warning); if set, LAUNCH_DATE set
	 * <li>STARTUP_DATE: Within 3 days of LAUNCH_DATE (warning); if set, LAUNCH_DATE
	 * set
	 * <li>END_MISSION_DATE: Not before LAUNCH_DATE; if set, LAUNCH_DATE set
	 * </ul>
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @throws IOException If an I/O error occurs
	 */

	public void validateDates() throws IOException {
		log.debug(".....validateDates: start.....");

		// ..read times

		String launch = arFile.readString("LAUNCH_DATE").trim();
		String start = arFile.readString("START_DATE").trim();
		String end = arFile.readString("END_MISSION_DATE").trim();

		// ..version specific dates

		Variable var = arFile.getNcReader().findVariable("STARTUP_DATE");
		String startup;

		if (var != null) {
			startup = ((ArrayChar) var.read()).getString();
		} else {
			startup = " ";
		}

		Date fileTime = new Date(arFile.getFile().lastModified());

		if (log.isDebugEnabled()) {
			log.debug("earliestDate:     '{}'", ArgoDate.format(earliestDate));
			log.debug("fileTime:         '{}'", ArgoDate.format(fileTime));
			log.debug("LAUNCH_DATE:      '{}'", launch);
			log.debug("START_DATE:       '{}' length = {}", start, start.length());
			log.debug("STARTUP_DATE:     '{}'", startup);
			log.debug("END_MISSION_DATE: '{}'", end);
		}

		// ...........creation and update dates checks:.............
		super.validateCreationUpdateDates(fileTime);

		// ............launch date checks:...........
		// ..must be set ... not before earliest allowed date
		Date dateLaunch = null;
		boolean haveLaunch = false;

		if (launch.trim().length() > 0) {
			// =======
			// CK_0097
			// =======
			dateLaunch = ArgoDate.get(launch);
			haveLaunch = true;

			if (dateLaunch == null) {
				validationResult.addError("LAUNCH_DATE: '" + launch + "': Invalid date");
				haveLaunch = false;

			} else {
				// =======
				// CK_0098
				// =======
				if (dateLaunch.before(earliestDate)) {
					validationResult.addError("LAUNCH_DATE: '" + launch + "': Before earliest allowed date ('"
							+ ArgoDate.format(earliestDate) + "')");
				}
			}

		} else {
			validationResult.addError("LAUNCH_DATE: Not set");
		}

		// ............start date checks:...........
		// ..if set, must be valid
		Date dateStart = null;
		// =======
		// CK_0099
		// =======
		if (start.trim().length() > 0) {
			dateStart = ArgoDate.get(start);

			if (dateStart == null) {
				validationResult.addError("START_DATE: '" + start + "': Invalid date");
			}
		}

		// ............startup date checks:...........
		// ..if set, within 3 days of launch date (W) and launch data set (W)
		Date dateStartup = null;
		// =======
		// CK_0100
		// =======
		if (startup.trim().length() > 0) {
			dateStartup = ArgoDate.get(startup);

			if (dateStartup == null) {
				validationResult.addError("STARTUP_DATE: '" + startup + "': Invalid date");
			}
		}

		// ............end_mission date checks:...........
		// ..if set, not before launch data and set if launch set
		Date dateEnd = null;

		if (end.trim().length() > 0) {
			dateEnd = ArgoDate.get(end);
			// =======
			// CK_0101
			// =======
			if (dateEnd == null) {
				validationResult.addError("END_MISSION_DATE: '" + end + "': Invalid date");

			} else {
				if (haveLaunch) {
					// =======
					// CK_0102
					// =======
					if (dateEnd.before(dateLaunch)) {
						validationResult
								.addError("END_MISSION_DATE: '" + end + "': Before LAUNCH_DATE ('" + launch + "')");
					}
				} else {
					// =======
					// CK_0103
					// =======
					validationResult.addWarning("END_MISSION_DATE: Set. LAUNCH_DATE missing");
				}
			}
		}
		log.debug(".....validateDates: end.....");
	}// ..end validateDates

	/**
	 * Validates the highly-desirable parameters (as defined up to v2.2)
	 *
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param dac the DAC the file belongs to
	 * @throws IOException If an I/O error occurs
	 */

	public void validateHighlyDesirable_v2(ArgoReferenceTable.DACS dac) throws IOException {
		// ..Validated separately:
		// .. DATA_TYPE
		// .. FORMAT_VERSION
		// .. DATE_CREATION
		// .. DATE_UPDATE

		log.debug("....validateHighlyDesirable_v2: start.....");

		Character ch;
		double dVal;
		float fVar[];
		float fVal;
		ArgoReferenceTable.ArgoReferenceEntry info;
		String name;
		String str;

		// get NVS tables :
//		SkosCollection qcFlagsTable = ArgoNVSReferenceTable.getNvsTableByName("DM_QC_FLAG");
//		SkosCollection positionningSystemTable = ArgoNVSReferenceTable.getNvsTableByName("POSITIONING_SYSTEM");
//		SkosCollection transSystemTable = ArgoNVSReferenceTable.getNvsTableByName("TRANS_SYSTEM");

		SkosConcept tableEntry;

		// ...........single valued variables..............

		// ..CYCLE_TIME ---> see "per-cycle" checks below

		name = "DATA_CENTRE"; // ..valid (and valid for DAC)
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (dac != null) {
			if (!ArgoReferenceTable.DacCenterCodes.get(dac).contains(str)) {
				validationResult.addError("DATA_CENTRE: '" + str + "': Invalid for DAC " + dac);
			}

		} else { // ..incoming DAC not set
			if (!ArgoReferenceTable.DacCenterCodes.containsValue(str)) {
				validationResult.addError("DATA_CENTRE: '" + str + "': Invalid (for all DACs)");
			}
		}

		// ..DEEPEST_PRESSURE ---> see "per-cycle" checks below

		name = "DIRECTION"; // ..'A' or 'D'
		ch = getChar(name);
		log.debug("{}: '{}'", name, ch);
		if (ch != 'A' && ch != 'D') {
			validationResult.addWarning(name + ": '" + ch + "': Not A or D");
		}

		name = "LAUNCH_LATITUDE"; // ..on the earth
		dVal = arFile.readDouble(name);
		log.debug("{}: {}", name, dVal);

		if (dVal < -90.d || dVal > 90.d) {
			validationResult.addWarning(name + ": " + dVal + ": Invalid");
		}

		name = "LAUNCH_DATE"; // ..not empty -- validity checked elsewhere
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addWarning(name + ": Empty");
		}

		name = "LAUNCH_LONGITUDE"; // ..on the earth
		dVal = arFile.readDouble(name);
		log.debug("{}: {}", name, dVal);

		if (dVal < -180.d || dVal > 180.d) {
			validationResult.addWarning(name + ": " + dVal + ": Invalid");
		}

		name = "LAUNCH_QC"; // ..valid ref table 2 value
		ch = getChar(name);
		log.debug("{}: '{}'", name, ch);

		tableEntry = ArgoNVSReferenceTable.DM_QC_FLAG_TABLE.getConceptMembersByAltLabelMap().get(String.valueOf(ch));
		if (tableEntry == null) {
			validationResult.addWarning(name + ": '" + ch + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
		}

		// ..PARAMETER --> see below
		// ..PARKING_PRESSURE ---> see "per-cycle" checks below

		name = "PLATFORM_MODEL"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addWarning(name + ": Empty");
		}

		name = "PLATFORM_NUMBER"; // ..valid wmo id
		str = arFile.readString(name).trim();
		if (!super.validatePlatfomNumber(str)) {
			validationResult.addError("PLATFORM_NUMBER" + ": '" + str + "': Invalid");
		}

		name = "POSITIONING_SYSTEM"; // ..ref table 9
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);

		tableEntry = ArgoNVSReferenceTable.POSITION_ACCURACY_TABLE.getConceptMembersByAltLabelMap().get(str);
		if (tableEntry == null) {
			validationResult.addWarning(name + ": '" + str + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
		}

		name = "PTT"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addWarning(name + ": Empty");
		}

		name = "START_DATE"; // ..not empty -- validity checked elsewhere
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addWarning(name + ": Empty");
		}

		name = "START_DATE_QC"; // ..valid ref table 2 value
		ch = getChar(name);
		log.debug("{}: '{}'", name, ch);
		tableEntry = ArgoNVSReferenceTable.DM_QC_FLAG_TABLE.getConceptMembersByAltLabelMap().get(String.valueOf(ch));
		if (tableEntry == null) {
			validationResult.addWarning(name + ": '" + ch + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
		}

		name = "TRANS_SYSTEM"; // ..ref table 10
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		tableEntry = ArgoNVSReferenceTable.TRANS_SYSTEM_TABLE.getConceptMembersByAltLabelMap().get(str);
		if (tableEntry == null) {
			validationResult.addWarning(name + ": '" + str + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
		}

		name = "TRANS_SYSTEM_ID"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addWarning(name + ": Empty");
		}

		// ..............parameter names...........

		int nParam = arFile.getDimensionLength("N_PARAM");

		log.debug("n_param: '{}'", nParam);
		String paramVar[] = arFile.readStringArr("PARAMETER");
		for (int n = 0; n < nParam; n++) {
			str = paramVar[n].trim();
			log.debug("param[{}]: '{}'", n, str);
			if (!arFile.getFileSpec().isPhysicalParamName(str)) {
				validationResult.addWarning("Physical parameter name: '" + str + "': Invalid");
			}
		}

		// .........per-cycle checks..........

		int nCycles = arFile.getDimensionLength("N_CYCLES");

		log.debug("n_cycles: '{}'", nCycles);
		name = "CYCLE_TIME"; // ..not empty
		fVar = arFile.readFloatArr(name);
		for (int n = 0; n < nCycles; n++) {
			fVal = fVar[n];
			log.debug("{}[{}]: {}", name, n, fVal);

			if (fVal > 9999.f || fVal <= 0.f) {
				validationResult.addWarning(name + "[" + (n + 1) + "]: Not set");
			}
		}

		name = "PARKING_PRESSURE"; // ..not empty
		fVar = arFile.readFloatArr(name);
		for (int n = 0; n < nCycles; n++) {
			fVal = fVar[n];
			log.debug("{}[{}]: {}", name, n, fVal);

			if (fVal > 9999.f || fVal <= 0.f) {
				validationResult.addWarning(name + "[" + (n + 1) + "]: Not set");
			}
		}

		name = "DEEPEST_PRESSURE"; // ..not empty
		fVar = arFile.readFloatArr(name);
		for (int n = 0; n < nCycles; n++) {
			fVal = fVar[n];
			log.debug("{}[{}]: {}", name, n, fVal);

			if (fVal > 9999.f || fVal <= 0.f) {
				validationResult.addWarning(name + "[" + (n + 1) + "]: Not set");
			}
		}

		log.debug("....validateHighlyDesirable_v2: end.....");
	} // ..end validateHighlyDesirable_v2

	/**
	 * Validates the mandatory parameters, as defined after v2.2. <br>
	 * Mandatory variables with controlled vacabularies are validated elsewhere.
	 *
	 * <p>
	 * Validates:
	 * <ul>
	 * <li>CONTROLLER_BOARD_SERIAL_NO_PRIMARY
	 * <li>CONTROLLER_BOARD_TYPE_PRIMARY
	 * <li>DAC_FORMAT_ID
	 * <li>DATA_CENTRE
	 * <li>FIRMWARE_VERSION
	 * <li>FLOAT_SERIAL_NO
	 * <li>LAUNCH_LATITUDE
	 * <li>LAUNCH_LONGITUDE
	 * <li>LAUNCH_QC
	 * <li>MANUAL_VERSION
	 * <li>PI_NAME
	 * <li>PLATFORM_FAMILY
	 * <li>PLATFORM_NUMBER
	 * <li>PLATFORM_MAKER
	 * <li>PLATFORM_TYPE
	 * <ul>
	 * <li>PLATFORM_TYPE vs PLATFORM_MAKER
	 * </ul>
	 * <li>PTT
	 * <li>START_DATE_QC
	 * <li>STANDARD_FORMAT_ID
	 * <li>WMO_INST_TYPE
	 * <ul>
	 * <li>PLATFORM_TYPExWMO_INST_TYPE
	 * </ul>
	 * <li>Per-parameter checks:
	 * <ul>
	 * <li>PARAMETER
	 * <li>PARAMETER_UNITS
	 * <li>PARAMETER_SENSOR
	 * <li>PREDEPLOYMENT_CALIB_COEFFICIENT
	 * <li>PREDEPLOYMENT_CALIB_EQUATION
	 * </ul>
	 * <li>Per-sensor checks:
	 * <ul>
	 * <li>SENSOR
	 * <li>SENSOR_MAKER
	 * <li>SENSOR_MODEL
	 * <ul>
	 * <li>SENSOR_MODEL / SENSOR_MAKER
	 * <li>SENSOR_MODEL / SENSOR
	 * </ul>
	 * </ul>
	 * <li>Per-positioning_system checks:
	 * <ul>
	 * <li>POSITIONING_SYSTEM
	 * <li>TRANS_SYSTEM
	 * <li>TRANS_SYSTEM_ID
	 * </ul>
	 * </ul>
	 *
	 * <p>
	 * Validated separately:
	 * <ul>
	 * <li>BATTERY_TYPE / BATTERY_PACKS
	 * <li>DATA_TYPE
	 * <li>FORMAT_VERSION
	 * <li>HANDBOOK_VERSION
	 * <li>DATE_CREATION
	 * <li>DATE_UPDATE
	 * <li>LAUNCH_DATE
	 * <li>START_DATE
	 * </ul>
	 *
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param dac the DAC the file belongs to
	 * @throws IOException If an I/O error occurs
	 */
	public void validateMandatory_v3(ArgoReferenceTable.DACS dac) throws IOException {

		log.debug("....validateMandatory_v3: start.....");

		Character ch;
		double dVal;
		ArgoReferenceTable.ArgoReferenceEntry info;
		String name;
		String str;

		// get nvs Tables :
//		SkosCollection qcFlagsTable = ArgoNVSReferenceTable.getNvsTableByName("DM_QC_FLAG");
//		SkosCollection wmoInstTypeTable = ArgoNVSReferenceTable.getNvsTableByName("ARGO_WMO_INST_TYPE");
//		SkosCollection positioningSystemTable = ArgoNVSReferenceTable.getNvsTableByName("POSITIONING_SYSTEM");
//		SkosCollection transSystemTable = ArgoNVSReferenceTable.getNvsTableByName("TRANS_SYSTEM");

		SkosConcept tableEntry;

		// ...........single valued variables..............
		// =======
		// CK_0104
		// =======
		super.checkStrVarEmpty("CONTROLLER_BOARD_SERIAL_NO_PRIMARY");

		// =======
		// CK_0105
		// =======
		super.checkStrVarEmpty("CONTROLLER_BOARD_TYPE_PRIMARY");

		// =======
		// CK_0106
		// =======
		super.checkStrVarEmpty("DAC_FORMAT_ID");

		// DATA_CENTRE
		super.validateDataCentre(dac);

		// =======
		// CK_0107
		// =======
		super.checkStrVarEmpty("FIRMWARE_VERSION");

		// =======
		// CK_0046
		// =======
		super.checkStrVarEmpty("FLOAT_SERIAL_NO");

		// ..LAUNCH_DATE --> checked elsewhere

		// =======
		// CK_0108
		// =======
		name = "LAUNCH_LATITUDE"; // ..on the earth
		dVal = arFile.readDouble(name);
		log.debug("{}: {}", name, dVal);

		if (dVal < -90.d || dVal > 90.d) {
			validationResult.addError(name + ": " + dVal + ": Invalid");
		}

		// =======
		// CK_0109
		// =======
		name = "LAUNCH_LONGITUDE"; // ..on the earth
		dVal = arFile.readDouble(name);
		log.debug("{}: {}", name, dVal);

		if (dVal < -180.d || dVal > 180.d) {
			validationResult.addError(name + ": " + dVal + ": Invalid");
		}

		name = "LAUNCH_QC"; // ..ref table NVS RD2
		ch = getChar(name);
		log.debug("{}: '{}'", name, ch);
		tableEntry = ArgoNVSReferenceTable.DM_QC_FLAG_TABLE.getConceptMembersByAltLabelMap().get(String.valueOf(ch));
		if (tableEntry != null) {
			if (tableEntry.isDeprecated()) {
				// =======
				// CK_0111
				// =======
				validationResult.addWarning(name + ": '" + ch + "' Status: " + SkosConcept.DEPRECATED_CONCEPT);
			}

		} else {
			// =======
			// CK_0110
			// =======
			validationResult.addError(name + ": '" + ch + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
		}
		// =======
		// CK_0112
		// =======
		super.checkStrVarEmpty("MANUAL_VERSION");

		// ..PARAMETER --> see below

		// =======
		// CK_0113
		// =======
		super.validatePINAME();

		name = "PLATFORM_FAMILY"; // ..ref table 22
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		tableEntry = ArgoNVSReferenceTable.PLATFORM_FAMILY_TABLE.getConceptMembersByAltLabelMap().get(str);
		if (tableEntry != null) {
			if (tableEntry.isDeprecated()) {
				// =======
				// CK_0115
				// =======
				validationResult.addWarning(name + ": '" + str + "' Status: " + SkosConcept.DEPRECATED_CONCEPT);
			}

		} else {
			// =======
			// CK_0114
			// =======
			validationResult.addError(name + ": '" + str + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
		}
		// =======
		// CK_0116
		// =======
		name = "PLATFORM_NUMBER"; // ..valid wmo id
		str = arFile.readString(name).trim();
		if (!super.validatePlatfomNumber(str)) {
			validationResult.addError("PLATFORM_NUMBER" + ": '" + str + "': Invalid");
		}

		boolean pmkrValid = false;

		String plfmMakerName = "PLATFORM_MAKER"; // ..ref table 24
		String plfmMaker = arFile.readString(plfmMakerName).trim();
		log.debug("{}: '{}'", plfmMakerName, plfmMaker);
		SkosConcept plfmMakerTableEntry = ArgoNVSReferenceTable.PLATFORM_MAKER_TABLE.getConceptMembersByAltLabelMap()
				.get(plfmMaker);
		if (plfmMakerTableEntry != null) {
			pmkrValid = true;

			if (plfmMakerTableEntry.isDeprecated()) {
				// =======
				// CK_0118
				// =======
				validationResult
						.addWarning(plfmMakerName + ": '" + plfmMaker + "' Status: " + SkosConcept.DEPRECATED_CONCEPT);
			}

		} else {
			// =======
			// CK_0117
			// =======
			validationResult
					.addError(plfmMakerName + ": '" + plfmMaker + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
		}

		boolean typValid = false;

		String plfmTypeName = "PLATFORM_TYPE"; // ..ref table 23
		String plfmType = arFile.readString(plfmTypeName).trim();
		log.debug("{}: '{}'", plfmTypeName, plfmType);
		SkosConcept pltmTypeTableEntry = ArgoNVSReferenceTable.PLATFORM_TYPE_TABLE.getConceptMembersByAltLabelMap()
				.get(plfmType);
		if (pltmTypeTableEntry != null) {
			typValid = true;

			if (pltmTypeTableEntry.isDeprecated()) {
				// =======
				// CK_0120
				// =======
				validationResult
						.addWarning(plfmTypeName + ": '" + plfmType + "' Status: " + SkosConcept.DEPRECATED_CONCEPT);
			}

		} else {
			// =======
			// CK_0119
			// =======
			validationResult
					.addError(plfmTypeName + ": '" + plfmType + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
		}
		// =======
		// CK_0166
		// =======
		if (pmkrValid && typValid) {
			if (!plfmType.equals("FLOAT")) {

				if (!pltmTypeTableEntry.checkRelatedReference(plfmMakerTableEntry.getId())) {
					validationResult.addError(plfmTypeName + "/" + plfmMakerName + ": Inconsistent: '" + plfmType
							+ "'/'" + plfmMaker + "'");
					log.debug("{}/{} xref inconsistent: plfmType, plfmMaker = '{}', '{}'", plfmTypeName, plfmMakerName,
							plfmType, plfmMaker);
				} else {
					log.debug("{}/{} xref valid: mdl, sn = '{}', '{}'", plfmTypeName, plfmMakerName, plfmType,
							plfmMaker);
				}
			}
		}

		// ..POSITIONING_SYSTEM --> see per-positioning_system below
		// ..PREDEPLOYMENT_CALIB_COEFFICIENT --> see per-param below
		// ..PREDEPLOYMENT_CALIB_EQUATION --> see per-param below
		// =======
		// CK_0121
		// =======
		super.checkStrVarEmpty("PTT");

		// ..SENSOR --> see per-sensor below
		// ..SENSOR_MAKER --> see per-sensor below
		// ..SENSOR_MODEL --> see per-sensor below
		// ..SENSOR_SERIAL_NO --> see per-sensor below

		name = "START_DATE_QC"; // ..ref table 2
		ch = getChar(name);
		log.debug("{}: '{}'", name, ch);
		tableEntry = ArgoNVSReferenceTable.DM_QC_FLAG_TABLE.getConceptMembersByAltLabelMap().get(String.valueOf(ch));
		if (tableEntry != null) {
			if (tableEntry.isDeprecated()) {
				// =======
				// CK_0123
				// =======
				validationResult.addWarning(name + ": '" + ch + "' Status: " + SkosConcept.DEPRECATED_CONCEPT);
			}

		} else {
			// =======
			// CK_0122
			// =======
			validationResult.addError(name + ": '" + ch + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
		}

		// =======
		// CK_0124
		// =======
		super.checkStrVarEmpty("STANDARD_FORMAT_ID");

		// ..TRANS_FREQUENCY \
		// ..TRANS_SYSTEM --> see per-trans_sys below
		// ..TRANS_SYSTEM_ID /

		boolean wmoValid = false;

		name = "WMO_INST_TYPE"; // ..ref table 8
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);

		SkosConcept wmoInstTypetableEntry = ArgoNVSReferenceTable.ARGO_WMO_INST_TYPE_TABLE
				.getConceptMembersByAltLabelMap().get(str);
		try {
			// =======
			// CK_0125
			// =======
			Integer.valueOf(str); // check if can be converted to integer
			if (wmoInstTypetableEntry != null) {
				wmoValid = true;

				if (wmoInstTypetableEntry.isDeprecated()) {
					// =======
					// CK_0127
					// =======
					validationResult.addWarning(name + ": '" + str + "' Status: " + SkosConcept.DEPRECATED_CONCEPT);
				}

			} else {
				// =======
				// CK_0126
				// =======
				validationResult.addError(name + ": '" + str + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
			}

		} catch (Exception e) {
			validationResult.addError(name + ": '" + str + "' Invalid. Must be integer.");
		}

		// =======
		// CK_0167
		// =======
		if (wmoValid && typValid) {
			if (!plfmType.equals("FLOAT")) {
				if (!pltmTypeTableEntry.checkNarowerReference(wmoInstTypetableEntry.getId())) {
					validationResult
							.addError(plfmTypeName + "/" + name + ": Inconsistent: '" + plfmType + "'/'" + str + "'");
					log.debug("{}/{} xref inconsistent: plfmType, wmo = '{}', '{}'", plfmTypeName, name, plfmType, str);
				} else {
					log.debug("{}/{} xref valid: mdl, wmo = '{}', '{}'", plfmTypeName, name, plfmType, str);
				}
			}
		}

		// ...........per-parameter checks...........

		String[] paramVar;
		int nParam = arFile.getDimensionLength("N_PARAM");
		log.debug("N_PARAM: '{}'", nParam);

		name = "PARAMETER"; // ..in physical parameter list
		paramVar = arFile.readStringArr(name);

		for (int n = 0; n < nParam; n++) {
			str = paramVar[n].trim();
			log.debug(name + "[{}]: '{}'", n, str);
			// =======
			// CK_0128
			// =======
			if (!arFile.getFileSpec().isPhysicalParamName(str)) {
				validationResult.addError(name + "[" + (n + 1) + "]: '" + str + "': Invalid");
			}
		}
		// check unicity in PARAMETER entries :
		// =======
		// CK_0129
		// =======
		Set<String> duplicateParameters = checkForDuplicate(paramVar);
		if (duplicateParameters.size() > 0) {
			validationResult.addWarning(
					"PARAMETER variable contains duplicate values: [" + String.join(", ", duplicateParameters) + "]");
		}

		// =======
		// CK_0130
		// =======
		name = "PARAMETER_UNITS"; // ..not empty
		paramVar = arFile.readStringArr(name);
		for (int n = 0; n < nParam; n++) {
			str = paramVar[n].trim();
			log.debug(name + "[{}]: '{}'", n, str);
			if (str.length() <= 0) {
				validationResult.addError(name + "[" + (n + 1) + "]: Empty");
			}
		}

		// =======
		// CK_0131
		// =======
		name = "PARAMETER_SENSOR"; // ..not empty
		paramVar = arFile.readStringArr(name);

		for (int n = 0; n < nParam; n++) {
			str = paramVar[n];
			log.debug(name + "[{}]: '{}'", n, str);
			if (str.length() <= 0) {
				validationResult.addError(name + "[" + (n + 1) + "]: Empty");
			}
		}

		// =======
		// CK_0132
		// =======
		name = "PREDEPLOYMENT_CALIB_COEFFICIENT"; // ..not empty
		paramVar = arFile.readStringArr(name);

		for (int n = 0; n < nParam; n++) {
			str = paramVar[n].trim();
			log.debug(name + "[{}]: '{}'", n, str);

			if (str.length() <= 0) {
				validationResult.addError(name + "[" + (n + 1) + "]: Empty");
			}
		}

		// =======
		// CK_0133
		// =======
		name = "PREDEPLOYMENT_CALIB_EQUATION"; // ..not empty
		paramVar = arFile.readStringArr(name);

		for (int n = 0; n < nParam; n++) {
			str = paramVar[n].trim();
			log.debug(name + "[{}]: '{}'", n, str);

			if (str.length() <= 0) {
				validationResult.addError(name + "[" + (n + 1) + "]: Empty");
			}
		}

		// .........per-sensor checks............
		int nSensor = arFile.getDimensionLength("N_SENSOR");
		log.debug("N_SENSOR: '{}'", nSensor);

		String sensorName = "SENSOR"; // ..ref table 25
		String[] sensor = arFile.readStringArr(sensorName);

		String sensorMakerName = "SENSOR_MAKER"; // ..ref table 26
		String[] sensorMaker = arFile.readStringArr(sensorMakerName);

		String sensorModelName = "SENSOR_MODEL"; // ..ref table 27
		String[] sensorModel = arFile.readStringArr(sensorModelName);

		for (int n = 0; n < nSensor; n++) {
			SkosConcept sensorModelTableEntry;
			SkosConcept sensorTableEntry;
			SkosConcept sensorMakerTableEntry;

			// ..check SENSOR
			// =================
			// CK_0134 & CK_0135
			// =================
			String snsr = sensor[n].trim();
			String normalizedSensorName = normalizeSensorName(snsr);
			boolean snsrValid = checkParameterValueAgainstRefTable(sensorName + "[" + (n + 1) + "]",
					normalizedSensorName, ArgoNVSReferenceTable.SENSOR_TABLE.getConceptMembersByAltLabelMap(), false);

			// ..check SENSOR_MAKER
			// =================
			// CK_0136 & CK_0137
			// =================
			String snsrMaker = sensorMaker[n].trim();
			boolean smkrValid = checkParameterValueAgainstRefTable(sensorMakerName + "[" + (n + 1) + "]", snsrMaker,
					ArgoNVSReferenceTable.SENSOR_MAKER_TABLE.getConceptMembersByAltLabelMap(), false);
			log.debug(sensorMakerName + "[{}]: '{}'", n, snsrMaker);

			// ..check SENSOR_MODEL
			// =================
			// CK_0138 & CK_0139
			// =================
			String snsrModel = sensorModel[n].trim();
			boolean mdlValid = checkParameterValueAgainstRefTable(sensorModelName + "[" + (n + 1) + "]", snsrModel,
					ArgoNVSReferenceTable.SENSOR_MODEL_TABLE.getConceptMembersByAltLabelMap(), false);

			// ..cross-reference SENSOR_MODEL R27 / SENSOR_MAKER R26
			// =======
			// CK_0164
			// =======
			if (smkrValid && mdlValid) {
				sensorModelTableEntry = ArgoNVSReferenceTable.SENSOR_MODEL_TABLE.getConceptMembersByAltLabelMap()
						.get(snsrModel);
				sensorMakerTableEntry = ArgoNVSReferenceTable.SENSOR_MAKER_TABLE.getConceptMembersByAltLabelMap()
						.get(snsrMaker);
				if (!snsrModel.equals("UNKNOWN")) {

					if (!sensorModelTableEntry.checkBroaderReference(sensorMakerTableEntry.getId())) {
						validationResult.addError(sensorModelName + "/" + sensorMakerName + "[" + (n + 1) + "]: "
								+ "Inconsistent: '" + snsrModel + "'/'" + snsrMaker + "'");
						log.debug("SENSOR_MODEL/SENSOR_MAKER xref inconsistent: mdl, mkr = '{}', '{}'",
								sensorModelTableEntry.getAltLabel(), sensorMakerTableEntry.getAltLabel());
					} else {
						log.debug("SENSOR_MODEL/SENSOR_MAKER xref valid: mdl, mkr = '{}', '{}'",
								sensorModelTableEntry.getAltLabel(), sensorMakerTableEntry.getAltLabel());
					}
				}
			}

			// ..cross-reference SENSOR_MODEL R27 / SENSOR R25
			// =======
			// CK_0165
			// =======
			if (snsrValid && mdlValid) {
				sensorModelTableEntry = ArgoNVSReferenceTable.SENSOR_MODEL_TABLE.getConceptMembersByAltLabelMap()
						.get(snsrModel);
				sensorTableEntry = ArgoNVSReferenceTable.SENSOR_TABLE.getConceptMembersByAltLabelMap()
						.get(normalizedSensorName);
				if (!snsrModel.equals("UNKNOWN")) {
					if (!sensorModelTableEntry.checkRelatedReference(sensorTableEntry.getId())) {
						validationResult.addError(sensorModelName + "/" + sensorName + "[" + (n + 1) + "]: "
								+ "Inconsistent: '" + snsrModel + "'/'" + snsr + "'");
						log.debug("SENSOR_MODEL/SENSOR xref inconsistent: mdl, sn = '{}', '{}'",
								sensorModelTableEntry.getAltLabel(), sensorTableEntry.getAltLabel());
					} else {
						log.debug("SENSOR_MODEL/SENSOR xref valid: mdl, sn = '{}', '{}'",
								sensorModelTableEntry.getAltLabel(), sensorTableEntry.getAltLabel());
					}
				}
			}
		}
		// check unicity in SENSOR entries :
		// =======
		// CK_0140
		// =======
		Set<String> duplicateSensors = checkForDuplicate(sensor);
		if (duplicateSensors.size() > 0) {
			validationResult.addWarning(
					"SENSOR variable contains duplicate values: [" + String.join(", ", duplicateSensors) + "]");
		}

		// ..........per-positioning_system checks
		String[] positVar;
		int nPosit = arFile.getDimensionLength("N_POSITIONING_SYSTEM");
		log.debug("N_POSITIONING_SYSTEM: '{}'", nPosit);

		name = "POSITIONING_SYSTEM"; // ..ref table 9
		positVar = arFile.readStringArr(name);

		for (int n = 0; n < nPosit; n++) {
			str = positVar[n].trim();
			log.debug(name + "[{}]: '{}'", n, str);
			tableEntry = ArgoNVSReferenceTable.POSITIONING_SYSTEM_TABLE.getConceptMembersByAltLabelMap().get(str);
			if (tableEntry != null) {
				if (tableEntry.isDeprecated()) {
					// =======
					// CK_0142
					// =======
					validationResult.addWarning(
							name + "[" + (n + 1) + "]: '" + str + "' Status: " + SkosConcept.DEPRECATED_CONCEPT);
				}
			} else {
				// =======
				// CK_0141
				// =======
				validationResult.addError(
						name + "[" + (n + 1) + "]: '" + str + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
			}
		}

		// ..........per-trans_system checks
		String[] transVar;
		int nTrans = arFile.getDimensionLength("N_TRANS_SYSTEM");
		log.debug("N_TRANS_SYSTEM: '{}'", nTrans);

		name = "TRANS_SYSTEM"; // ..ref table 10
		transVar = arFile.readStringArr(name);

		for (int n = 0; n < nTrans; n++) {
			str = transVar[n].trim();
			log.debug(name + "[{}]: '{}'", n, str);
			tableEntry = ArgoNVSReferenceTable.TRANS_SYSTEM_TABLE.getConceptMembersByAltLabelMap().get(str);
			if (tableEntry != null) {
				if (tableEntry.isDeprecated()) {
					// =======
					// CK_0144
					// =======
					validationResult.addWarning(
							name + "[" + (n + 1) + "]: '" + str + "' Status: " + SkosConcept.DEPRECATED_CONCEPT);
				}
			} else {
				// =======
				// CK_0143
				// =======
				validationResult.addError(
						name + "[" + (n + 1) + "]: '" + str + "' Status: " + SkosConcept.INVALID_ALTLABEL_MESSAGE);
			}
		}

		// =======
		// CK_0145
		// =======
		name = "TRANS_SYSTEM_ID"; // ..not empty
		transVar = arFile.readStringArr(name);

		for (int n = 0; n < nTrans; n++) {
			str = transVar[n].trim();
			log.debug(name + "[{}]: '{}'", n, str);
			if (str.length() <= 0) {
				validationResult.addError(name + "[" + (n + 1) + "]: Empty");
			}
		}

		log.debug("....validateMandatory_v3: end.....");
	} // ..end validateMandatory_v3

	private boolean checkParameterValueAgainstRefTable(String parameterName, String parameterValue,
			Map<String, SkosConcept> refTable, boolean warningOnly) {
		ArgoReferenceTable.ArgoReferenceEntry info;

		log.debug("{}: '{}'", parameterName, parameterValue);

		SkosConcept tableEntry = refTable.get(parameterValue);

		if (tableEntry != null) {
			if (tableEntry.isDeprecated()) {
				validationResult.addWarning(
						parameterName + ": '" + parameterValue + "' Status: " + SkosConcept.DEPRECATED_CONCEPT);
			}
			return true;

		} else {
			String resultMessage = parameterName + ": '" + parameterValue + "' Status: "
					+ SkosConcept.INVALID_ALTLABEL_MESSAGE + " (not in reference table)";
//			validationResult.addError(sensorMakerName + "[" + (n + 1) + "]: '" + snsrMaker + "' Status: "
//					+ SkosConcept.INVALID_ALTLABEL_MESSAGE);
			if (warningOnly) {
				validationResult.addWarning(resultMessage);
			} else {
				validationResult.addError(resultMessage);
			}

			return false;
		}
	}

	private boolean checkOptionalParameterValueAgainstRefTable(String parameterName, Map<String, SkosConcept> refTable,
			boolean warningOnly) {

		Variable dataVar = arFile.getNcReader().findVariable(parameterName);
		if (dataVar != null) {
			String parameterValue = arFile.readString(parameterName).trim();

			checkParameterValueAgainstRefTable(parameterName, parameterValue, refTable, warningOnly);
		}

		return true;
	}

	/**
	 * Check if a list of values contains duplicates
	 * 
	 * @param paramValuesList (String[]) list of values to check
	 * @return Set of values which are found multiple time in the list
	 */
	private Set<String> checkForDuplicate(String[] paramValuesList) {
		// for each value of the list, count the number of times it appears
		Map<String, Long> count = Arrays.stream(paramValuesList).map(String::trim)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		// build the list of value which appears more than one time
		Set<String> doublons = count.entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		return doublons;
	}

	/**
	 * Convenience method to return a char value for a variable
	 *
	 * @param name the variable name
	 * @return the variable String value
	 * @throws IOException If an I/O error occurs
	 */
	private Character getChar(String name) throws IOException {
		ArrayChar.D0 value = (ArrayChar.D0) arFile.getNcReader().findVariable(name).read();
		return Character.valueOf(value.get());
	}

	/**
	 * Validates the BATTERY_TYPE and BATTERY_PACKS in the meta-data file.
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	public void validateBattery() throws IOException {
		log.debug(".....validateBattery.....");

		// .....BATTERY_TYPE.....
		List<SkosConcept> typesTablesEntries = checkBatteryType();

		// .....BATTERY_PACKS.....
		List<SkosConcept> packTypesTablesEntries = checkBatteryPacks();

		// ............compare TYPES and PACKS............
		crossCheckBatteryPacksAndBatteryType(typesTablesEntries, packTypesTablesEntries);

	}// ..end validateBattery

	private void crossCheckBatteryPacksAndBatteryType(List<SkosConcept> typesTablesEntries,
			List<SkosConcept> packTypesTablesEntries) {

		int nPacks = packTypesTablesEntries.size();
		int nTypes = typesTablesEntries.size();

		if (nPacks >= 0) {
			// =======
			// CK_0157
			// =======
			// same number of entries
			if (nTypes != nPacks) {
				String err = String.format("Number of BATTERY_TYPES {%d} != number of BATTERY_PACKS {%d}", nTypes,
						nPacks);
				// validationResult.addError(err);

				// ################# TEMPORARY WARNING ################
				validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
				log.warn("TEMP WARNING: {%s}: {%s}: {%s}", arFile.getDacName(), arFile.getFileName(), err);

				log.debug("number of types != number of packs => {%d} != {%d}", nTypes, nPacks);
			} else {
				checkBatteryTypesConsistency(typesTablesEntries, packTypesTablesEntries);
			}

		} // ..end if nPacks >=0

//		if (nPacks >= 0) {
//
//			if (nTypes != nPacks) {
//				String err = String.format("Number of BATTERY_TYPES {%d} != number of BATTERY_PACKS {%d}", nTypes,
//						nPacks);
//				// validationResult.addError(err);
//
//				// ################# TEMPORARY WARNING ################
//				validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
//				log.warn("TEMP WARNING: {%s}: {%s}: {%s}", arFile.getDacName(), arFile.getFileName(), err);
//
//				log.debug("number of types != number of packs => {%d} != {%d}", nTypes, nPacks);
//			}
//
//		} // ..end if nPacks >= 0
	}

	private void checkBatteryTypesConsistency(List<SkosConcept> typesTablesEntries,
			List<SkosConcept> packTypesTablesEntries) {
		// =======
		// CK_0158
		// =======
		// same number of entries, for each entries, same concept should be found for
		// "Battery"
		for (int i = 0; i < packTypesTablesEntries.size(); i++) {
			SkosConcept batteryTypeType = typesTablesEntries.get(i);
			SkosConcept batteryPackType = packTypesTablesEntries.get(i);
			if (batteryPackType != null && batteryTypeType != null) {
				if (!batteryPackType.getId().equals(batteryTypeType.getId())) {
					// not same type : inconsistencies
					String err = String.format(
							"Inconsistent battery's type in BATTERY_TYPE[%d] and BATTERY_PACKS[%d]. BATTERY_TYPE's type ={%s}, BATTERY_PACKS's type = {%s}",
							i + 1, i + 1, batteryTypeType.getAltLabel(), batteryPackType.getPrefLabel());
					validationResult.addWarning(err);
				}
			}
		}
	}

	private List<SkosConcept> checkBatteryPacks() {
		String str;
		int nPacks = -1;
		List<SkosConcept> typesTablesEntries = new ArrayList<>();

		str = arFile.readString("BATTERY_PACKS");

		log.debug("BATTERY_PACKS: '{}'", str);

		if (str.trim().length() <= 0) {
			// ..empty - allowed - optional variable
			log.debug("BATTERY_PACKS: empty (allowed)");

		} else {

			// ..not empty
			// ..split multiple strings based on "+"

			nPacks = 0;
			for (String substr : str.split("\\+")) {
				nPacks++;
				log.debug("battery_packs substring: '{}'", substr);

				if (substr.trim().equals("U")) {
					typesTablesEntries.add(null);
					// =======
					// CK_0152
					// =======
					log.debug("battery_packs substring == U (undefined)");

				} else {
					// ..not undefined -- check pattern

					Matcher m = pBatteryPacks.matcher(substr);

					if (m.matches()) {

						String num = m.group("numofpacks");
						String style = m.group("style");
						String type = m.group("type");

						log.debug("...matched pattern: num, style, type = '{}', '{}', '{}", num, style, type);

						// =======
						// CK_0153
						// =======
						SkosConcept styleTableEntry = ArgoNVSReferenceTable.BATTERY_SIZE_TABLE
								.getConceptMembersByAltLabelMap().get(style);
						if (styleTableEntry != null) {
							if (styleTableEntry.isDeprecated()) {
								// =======
								// CK_0154
								// =======
								String err = String.format("BATTERY_PACKS[%d]: Deprecated style of battery: '{%s}'",
										nPacks, style);
								validationResult.addWarning(err);
							} else {
								log.debug("valid style");
							}
						} else {
							String err = String.format("BATTERY_PACKS[%d]: Invalid style of battery: '{%s}'", nPacks,
									style);
							// validationResult.addError(err);

							// ################# TEMPORARY WARNING ################
							validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
							log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

							log.debug("invalid style");
						}

						// =======
						// CK_0155
						// =======
						// BATTERY_TYPE pref label
						SkosConcept typeTableEntry = ArgoNVSReferenceTable.BATTERY_TYPE_TABLE
								.getConceptMembersByPrefLabelMap().get(type);
						typesTablesEntries.add(typeTableEntry);
						if (typeTableEntry != null) {
							if (typeTableEntry.isDeprecated()) {
								// =======
								// CK_0156
								// =======
								String err = String.format("BATTERY_PACKS[%d]: Deprecated type: '{%s}'", nPacks, type);
								validationResult.addWarning(err);
							} else {
								log.debug("valid type");
							}
						} else {
							String err = String.format("BATTERY_PACKS[%d]: Invalid type: '{%s}'", nPacks, type);
							// validationResult.addError(err);

							// ################# TEMPORARY WARNING ################
							validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
							log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

							log.debug("invalid type");
						}

					} else {
						typesTablesEntries.add(null);
						// ..did not match the expected pattern
						String err = String.format(
								"BATTERY_PACKS[%d]: Does not match template 'xStyle type (or U): '%s'", nPacks,
								substr.trim());
						// validationResult.addError(err);

						// ################# TEMPORARY WARNING ################
						validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
						log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

						log.debug("...does not match template");
					}
				} // ..end if undefined
			} // ..end for (battery_packs substrings)
		} // ..endif BATTERY_PACKS is filled
		return typesTablesEntries;
	}

	private List<SkosConcept> checkBatteryType() {
		int nTypes = 0;
		List<SkosConcept> typesTablesEntries = new ArrayList<>();

		String str = arFile.readString("BATTERY_TYPE");
		log.debug("BATTERY_TYPE: '{}'", str);

		if (str.trim().length() <= 0) {
			// =======
			// CK_0146
			// =======
			validationResult.addWarning("BATTERY_TYPE: Empty.    *** WILL BECOME AN ERROR ***");

		} else {

			// ..not empty
			// ..split multiple strings based on "+"

			for (String substr : str.split("\\+")) {
				nTypes++;
				log.debug("battery_type substring: '{}'", substr);

				Matcher m = pBatteryType.matcher(substr);

				if (m.matches()) {
					String manu = m.group("manufacturer");
					String type = m.group("type");
					String volt = m.group("volts");

					log.debug("...matched pattern: manu, type, volt = '{}', '{}', '{}'", manu, type, volt);

					// =======
					// CK_0148
					// =======
					SkosConcept manuTableEntry = ArgoNVSReferenceTable.BATTERY_MAKER_TABLE
							.getConceptMembersByAltLabelMap().get(manu);

					if (manuTableEntry != null) {
						if (manuTableEntry.isDeprecated()) {
							// =======
							// CK_0149
							// =======
							String err = String.format("BATTERY_TYPE[%d]: Deprecated manufacturer: '{%s}'", nTypes,
									manu);
							validationResult.addWarning(err);
						} else {
							log.debug("valid manufacturer");
						}
					} else {
						String err = String.format("BATTERY_TYPE[%d]: Invalid manufacturer: '{%s}'", nTypes, manu);
						// validationResult.addError(err);

						// ################# TEMPORARY WARNING ################
						validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
						log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

						log.debug("...invalid manufacturer");
					}

					// =======
					// CK_0150
					// =======
					SkosConcept typeTableEntry = ArgoNVSReferenceTable.BATTERY_TYPE_TABLE
							.getConceptMembersByAltLabelMap().get(type);
					typesTablesEntries.add(typeTableEntry);
					if (typeTableEntry != null) {
						if (typeTableEntry.isDeprecated()) {
							// =======
							// CK_0151
							// =======
							String err = String.format("BATTERY_TYPE[%d]: Deprecated type: '{%s}'", nTypes, type);
							validationResult.addWarning(err);
						} else {
							log.debug("valid type");
						}
					} else {
						// ..did not match the expected pattern
						String err = String.format("BATTERY_TYPE[%d]: Invalid type: '{%s}'", nTypes, type);

						// ################# TEMPORARY WARNING ################
						validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
						log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

						log.debug("invalid type");
					}

				} else {
					typesTablesEntries.add(null);
					// =======
					// CK_0147
					// =======
					// ..did not match the expected pattern
					String err = String.format(
							"BATTERY_TYPE[%d]: Does not match template 'manufacturer type volts V': '%s'", nTypes,
							substr.trim());
					// validationResult.addError(err);

					// ################# TEMPORARY WARNING ################
					validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
					log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

					log.debug("...does not match template");
				}
			}
		} // ..endif battery_type is filled
		return typesTablesEntries;
	}

	/**
	 * Validates the configuration mission in the meta-data file. The mission number
	 * must start at 1. Fillvalue is allowed (with WARNING) ****OFF***The mission
	 * number must start at 1 and be consecutive integers.
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	public void validateConfigMission() throws IOException {
		log.debug(".....validateConfigMission.....");

		int nMissions = arFile.getDimensionLength("N_MISSIONS");
		int[] mission = arFile.readIntArr("CONFIG_MISSION_NUMBER");

		log.debug("N_MISSIONS = {}", nMissions);

		for (int n = 0; n < nMissions; n++) {
			log.debug("CONFIG_MISSION_NUMBER[{}] = {}", n, mission[n]);

			if (mission[n] == 99999) {
				// =======
				// CK_0159
				// =======
				validationResult.addWarning("CONFIG_MISSION_NUMBER: Missing at index: " + (n + 1));
				log.debug("config_mission_number == 0 at {}", n);
				break;
			}
		}
	}

	/**
	 * Validates the configuration parameter names and units in the meta-data file.
	 * The name is the entry up to last "_". Unit is after the last "_".
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	public void validateConfigParams() throws IOException {
		log.debug(".....validateConfigParams.....");

		HashSet<String> nameAlreadyChecked = new HashSet<String>(100);
		HashMap<String, Boolean> unitAlreadyChecked = new HashMap<String, Boolean>(100);

		String[] varNames = { "LAUNCH_CONFIG_", "CONFIG_" };

		for (String v : varNames) {
			String dim = "N_" + v + "PARAM";
			String varName = v + "PARAMETER_NAME";

			int nParam = arFile.getDimensionLength(dim);

			log.debug("'{}' checking: number of parameters = {}", v, nParam);

			// ..read config names
			String[] full_name = arFile.readStringArr(varName);

			// ...........loop over names.............

			for (int n = 0; n < nParam; n++) {
				String full = full_name[n].trim();
				int index = full.lastIndexOf('_');

				if (index <= 0) {
					// =======
					// CK_0160
					// =======
					// ..poorly formed name - only report if not already reported

					if (!nameAlreadyChecked.contains(full)) {
						validationResult
								.addError(varName + "[" + (n + 1) + "]: " + "Incorrectly formed name '" + full + "'");
						nameAlreadyChecked.add(full);
					}

					nameAlreadyChecked.add(full);
					log.debug("badly formed name: {}[{}] = '{}'", varName, n, full);

					continue; // ..can't do anything with this
				}

				// ..well formed named, break it apart

				String param = full.substring(0, index);
				String unit = full.substring(index + 1);

				log.debug("check {}[{}]: full '{}'; param '{}'; unit '{}'", varName, n, full, param, unit);

				// ..check name

				if (!nameAlreadyChecked.contains(param)) {
					// =======
					// CK_0161
					// =======
					// ..this parameter name has not been checked

					ArgoConfigTechParam.ArgoConfigTechParamMatch match = arFile.getFileSpec().ConfigTech
							.findConfigParam(param);

					if (match == null) {
						// ..NOT an active name, NOT a deprecated name --> error
						String err = String.format("%s[%d]: Invalid name '%s'", varName, (n + 1), param);
						// validationResult.addError(err);

						// ################# TEMPORARY WARNING ################
						validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
						log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

						log.debug("parameter is invalid");

					} else {
						if (match.isDeprecated) {
							// =======
							// CK_0162
							// =======
							// ..IS a deprecated name --> warning
							validationResult.addWarning(varName + "[" + (n + 1) + "]: " + "Deprecated name '" + param);
							log.debug("parameter is deprecated: '{}'", param);
						}

						if (match.nFailedMatchedTemplates > 0) {
							// ..these Templates failed to match the values specified in the table
							// ..they are errors

							for (Map.Entry<String, String> entry : match.failedMatchedTemplates.entrySet()) {
								String tmplt = entry.getKey();
								String value = entry.getValue();

								String err = String.format("%s[%d]: Invalid template/value '%s'/'%s' in '%s'", varName,
										(n + 1), tmplt, value, param);
								// validationResult.addError(err);

								// ################# TEMPORARY WARNING ################
								validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
								log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

								log.debug("...invalid template/value '{}'/'{}'", tmplt, value);
							}
						}

						if (match.nUnMatchedTemplates > 0) {
							// ..these Templates did not have values specified in the table
							// ..check the generic template values:
							// .. shortsensorname, cyclephasename, param
							// ..all others are accepted as is - they matched their basic regex
							// ..- assume they are good

							String str = match.unMatchedTemplates.get("shortsensorname");
							if (str != null) {
								// =======
								// CK_0163
								// =======

								String err = String.format("%s[%d]: Invalid short_sensor_name '%s' in '%s'", varName,
										(n + 1), str, param);
								// validationResult.addError(err);

								// ################# TEMPORARY WARNING ################
								validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
								log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

								log.debug("...generic short_sensor_name lookup: INVALID = '{}'", str);
								// ==========================================================================
								// 2026 / NVS / 3.0.0 : is it still usefull as all should be provided in the
								// table / defintion field / Template values ?
								// ==========================================================================
//								if (!ArgoReferenceTable.GENERIC_TEMPLATE_short_sensor_name.contains(str)) {
//									String err = String.format("%s[%d]: Invalid short_sensor_name '%s' in '%s'",
//											varName, (n + 1), str, param);
//									// validationResult.addError(err);
//
//									// ################# TEMPORARY WARNING ################
//									validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
//									log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(),
//											err);
//
//									log.debug("...generic short_sensor_name lookup: INVALID = '{}'", str);
//								} else {
//									log.debug("...generic short_sensor_name lookup: valid = '{}'", str);
//								}
							}

							str = match.unMatchedTemplates.get("cyclephasename");
							if (str != null) {

								String err = String.format("%s[%d]: Invalid cycle_phase_name '%s' in '%s'", varName,
										(n + 1), str, param);
								// validationResult.addError(err)

								// ################# TEMPORARY WARNING ################
								validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
								log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

								log.debug("...generic cycle_phase_name lookup: INVALID = '{}'", str);

								// ==========================================================================
								// 2026 / NVS / 3.0.0 : is it still usefull as all should be provided in the
								// table / defintion field / Template values ?
								// ==========================================================================
//								if (!ArgoReferenceTable.GENERIC_TEMPLATE_cycle_phase_name.contains(str)) {
//									String err = String.format("%s[%d]: Invalid cycle_phase_name '%s' in '%s'", varName,
//											(n + 1), str, param);
//									// validationResult.addError(err)
//
//									// ################# TEMPORARY WARNING ################
//									validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
//									log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(),
//											err);
//
//									log.debug("...generic cycle_phase_name lookup: INVALID = '{}'", str);
//
//								} else {
//									log.debug("...generic cycle_phase_name lookup: valid = '{}'", str);
//								}
							}

							str = match.unMatchedTemplates.get("param");
							if (str != null) {
								String err = String.format("%s[%d]: Invalid param '%s' in '%s'", varName, (n + 1), str,
										param);

								// validationResult.addError(err)

								// ################# TEMPORARY WARNING ################
								validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
								log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

								log.debug("...generic param: generic name lookup: INVALID = '{}'", str);

								// ==========================================================================
								// 2026 / NVS / 3.0.0 : is it still usefull as all should be provided in the
								// table / defintion field / Template values ?
								// ==========================================================================
//								if (!ArgoReferenceTable.GENERIC_TEMPLATE_param.contains(str)) {
//									String err = String.format("%s[%d]: Invalid param '%s' in '%s'", varName, (n + 1),
//											str, param);
//
//									// validationResult.addError(err)
//
//									// ################# TEMPORARY WARNING ################
//									validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
//									log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(),
//											err);
//
//									log.debug("...generic param: generic name lookup: INVALID = '{}'", str);
//								} else {
//									log.debug("...generic param: generic name lookup: valid = '{}'", str);
//								}
							}
						}
					}

					nameAlreadyChecked.add(param);
				} // ..end if nameAlreadyChecked

				// ..check the unit

				boolean validUnit = false;

				if (!unitAlreadyChecked.containsKey(unit)) {
					// ..this unit name has not been checked

					if (!arFile.getFileSpec().ConfigTech.isConfigTechUnit(unit)) {
						// ..NOT an active unit name

						if (arFile.getFileSpec().ConfigTech.isDeprecatedConfigTechUnit(unit)) {
							// ..IS a deprecated name --> warning

							validUnit = true;
							validationResult.addWarning(varName + "[" + (n + 1) + "]: " + "Deprecated unit '" + unit
									+ "' in '" + full + "'");
							log.debug("deprecated unit '{}'", unit);

						} else {
							// ..INVALID unit -- not active, not deprecated --> error
							validUnit = false;
							validationResult.addError(
									varName + "[" + (n + 1) + "]: " + "Invalid unit '" + unit + "' in '" + full + "'");
							log.debug("name is valid, unit ({}) is not valid (new or old)", unit);
						}

					} else {
						// ..IS an active unit
						validUnit = true;
					} // ..end if not isConfigTechUnit

					unitAlreadyChecked.put(unit, validUnit);

				} else {
					// ..unit already check --- was it valid?
					validUnit = unitAlreadyChecked.get(unit);

				} // ..end check unit

			} // ..end for (nParam)

		} // ..end for ("launch_config", "config_")

	} // ..end validateConfigParams

	// ===================
	// CONVENIENCE METHODS
	// ===================

	/**
	 * Get base sensor name for table lookup // ADMT-2025 : duplicate SENSOR must be
	 * named <SENSOR>_<n> // so if sensor finish by a number, the duplicate will be
	 * xxxx[number]_n // if sensor don't finish by a number, the duplicate will also
	 * be xxxx_n.
	 * 
	 * @param snsr
	 * @return
	 */
	public static String normalizeSensorName(String snsr) {
		// Matches something ending with "_number" (e.g., TEMP_2, CHLA_10)
		return snsr.replaceFirst("_(\\d+)$", "");
	}

} // ..end class
