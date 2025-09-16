package fr.coriolis.checker.validators;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.core.ArgoDataFile;
import fr.coriolis.checker.specs.ArgoConfigTechParam;
import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.specs.ArgoReferenceTable;
import fr.coriolis.checker.specs.ArgoReferenceTable.StringTable;
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
	public boolean validateData(boolean ckNulls, boolean... optionalChecks) throws IOException {
		boolean basicsChecks = super.basicDataValidation(ckNulls);
		if (!basicsChecks) {
			return false;
		}
		// .......do meta-data file specific validations..........

//		if (ckNulls) {
//			validateStringNulls();
//		}

		validateDates();

		if (this.arFile.fileVersion().trim().compareTo("2.2") <= 0) {
			validateHighlyDesirable_v2(this.arFile.getValidatedDac());
		} else {
			validateMandatory_v3(this.arFile.getValidatedDac());
			validateOptionalParams();
			validateConfigMission();
			validateConfigParams();

			if ((optionalChecks.length > 0) && (optionalChecks[0] == true)) {
				validateBattery();
			}
		}

		return true;
	}// ..end validate

	private void validateOptionalParams() {
		// PROGRAM_NAME - ref table 41
		checkOptionalParameterValueAgainstRefTable("PROGRAM_NAME", ArgoReferenceTable.PROGRAM_NAME);
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
			dateLaunch = ArgoDate.get(launch);
			haveLaunch = true;

			if (dateLaunch == null) {
				validationResult.addError("LAUNCH_DATE: '" + launch + "': Invalid date");
				haveLaunch = false;

			} else {
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

		if (start.trim().length() > 0) {
			dateStart = ArgoDate.get(start);

			if (dateStart == null) {
				validationResult.addError("START_DATE: '" + start + "': Invalid date");
			}
		}

		// ............startup date checks:...........
		// ..if set, within 3 days of launch date (W) and launch data set (W)
		Date dateStartup = null;

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

			if (dateEnd == null) {
				validationResult.addError("END_MISSION_DATE: '" + end + "': Invalid date");

			} else {
				if (haveLaunch) {
					if (dateEnd.before(dateLaunch)) {
						validationResult
								.addError("END_MISSION_DATE: '" + start + "': Before LAUNCH_DATE ('" + launch + "')");
					}
				} else {
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

		if (!(info = ArgoReferenceTable.QC_FLAG.contains(ch)).isActive) {
			validationResult.addWarning(name + ": '" + ch + "' Status: " + info.message);
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

		if (!(info = ArgoReferenceTable.POSITIONING_SYSTEM.contains(str)).isActive) {
			validationResult.addWarning(name + ": '" + str + "' Status: " + info.message);
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

		if (!(info = ArgoReferenceTable.QC_FLAG.contains(ch)).isActive) {
			validationResult.addWarning(name + ": '" + str + "' Status: " + info.message);
		}

		name = "TRANS_SYSTEM"; // ..ref table 10
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);

		if (!(info = ArgoReferenceTable.TRANS_SYSTEM.contains(str)).isActive) {
			validationResult.addWarning(name + ": '" + str + "' Status: " + info.message);
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

		// ...........single valued variables..............

		name = "CONTROLLER_BOARD_SERIAL_NO_PRIMARY"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addError(name + ": Empty");
		}

		name = "CONTROLLER_BOARD_TYPE_PRIMARY"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addError(name + ": Empty");
		}

		name = "DAC_FORMAT_ID"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addError(name + ": Empty");
		}

		// DATA_CENTRE
		super.validateDataCentre(dac);

		name = "FIRMWARE_VERSION"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addError(name + ": Empty");
		}

		name = "FLOAT_SERIAL_NO"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addError(name + ": Empty");
		}

		// ..LAUNCH_DATE --> checked elsewhere

		name = "LAUNCH_LATITUDE"; // ..on the earth
		dVal = arFile.readDouble(name);
		log.debug("{}: {}", name, dVal);

		if (dVal < -90.d || dVal > 90.d) {
			validationResult.addError(name + ": " + dVal + ": Invalid");
		}

		name = "LAUNCH_LONGITUDE"; // ..on the earth
		dVal = arFile.readDouble(name);
		log.debug("{}: {}", name, dVal);

		if (dVal < -180.d || dVal > 180.d) {
			validationResult.addError(name + ": " + dVal + ": Invalid");
		}

		name = "LAUNCH_QC"; // ..ref table 2
		ch = getChar(name);
		log.debug("{}: '{}'", name, ch);

		if ((info = ArgoReferenceTable.QC_FLAG.contains(ch)).isValid()) {
			if (info.isDeprecated) {
				validationResult.addWarning(name + ": '" + ch + "' Status: " + info.message);
			}

		} else {
			validationResult.addError(name + ": '" + ch + "' Status: " + info.message);
		}

		name = "MANUAL_VERSION"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addError(name + ": Empty");
		}

		// ..PARAMETER --> see below

		name = "PI_NAME"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addError(name + ": Empty");
		}

		name = "PLATFORM_FAMILY"; // ..ref table 22
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);

		if ((info = ArgoReferenceTable.PLATFORM_FAMILY.contains(str)).isValid()) {
			if (info.isDeprecated) {
				validationResult.addWarning(name + ": '" + str + "' Status: " + info.message);
			}

		} else {
			validationResult.addError(name + ": '" + str + "' Status: " + info.message);
		}

		name = "PLATFORM_NUMBER"; // ..valid wmo id
		str = arFile.readString(name).trim();
		if (!super.validatePlatfomNumber(str)) {
			validationResult.addError("PLATFORM_NUMBER" + ": '" + str + "': Invalid");
		}

		boolean pmkrValid = false;

		String plfmMakerName = "PLATFORM_MAKER"; // ..ref table 24
		String plfmMaker = arFile.readString(plfmMakerName).trim();
		log.debug("{}: '{}'", plfmMakerName, plfmMaker);

		if ((info = ArgoReferenceTable.PLATFORM_MAKER.contains(plfmMaker)).isValid()) {
			pmkrValid = true;

			if (info.isDeprecated) {
				validationResult.addWarning(plfmMakerName + ": '" + plfmMaker + "' Status: " + info.message);
			}

		} else {
			validationResult.addError(plfmMakerName + ": '" + plfmMaker + "' Status: " + info.message);
		}

		boolean typValid = false;

		String plfmTypeName = "PLATFORM_TYPE"; // ..ref table 23
		String plfmType = arFile.readString(plfmTypeName).trim();
		log.debug("{}: '{}'", plfmTypeName, plfmType);

		if ((info = ArgoReferenceTable.PLATFORM_TYPE.contains(plfmType)).isValid()) {
			typValid = true;

			if (info.isDeprecated) {
				validationResult.addWarning(plfmTypeName + ": '" + plfmType + "' Status: " + info.message);
			}

		} else {
			validationResult.addError(plfmTypeName + ": '" + plfmType + "' Status: " + info.message);
		}

		if (pmkrValid && typValid) {
			if (!plfmType.equals("FLOAT")) {
				if (!ArgoReferenceTable.PLATFORM_TYPExPLATFORM_MAKER.xrefContains(plfmType, plfmMaker)) {
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

		name = "PTT";
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addError(name + ": Empty");
		}

		// ..SENSOR --> see per-sensor below
		// ..SENSOR_MAKER --> see per-sensor below
		// ..SENSOR_MODEL --> see per-sensor below
		// ..SENSOR_SERIAL_NO --> see per-sensor below

		name = "START_DATE_QC"; // ..ref table 2
		ch = getChar(name);
		log.debug("{}: '{}'", name, ch);

		if ((info = ArgoReferenceTable.QC_FLAG.contains(ch)).isValid()) {
			if (info.isDeprecated) {
				validationResult.addWarning(name + ": '" + ch + "' Status: " + info.message);
			}

		} else {
			validationResult.addError(name + ": '" + ch + "' Status: " + info.message);
		}

		name = "STANDARD_FORMAT_ID"; // ..not empty
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		if (str.length() <= 0) {
			validationResult.addError(name + ": Empty");
		}

		// ..TRANS_FREQUENCY \
		// ..TRANS_SYSTEM --> see per-trans_sys below
		// ..TRANS_SYSTEM_ID /

		boolean wmoValid = false;

		name = "WMO_INST_TYPE"; // ..ref table 8
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);
		try {
			int N = Integer.valueOf(str);

			if ((info = ArgoReferenceTable.WMO_INST_TYPE.contains(N)).isValid()) {
				wmoValid = true;

				if (info.isDeprecated) {
					validationResult.addWarning(name + ": '" + str + "' Status: " + info.message);
				}

			} else {
				validationResult.addError(name + ": '" + str + "' Status: " + info.message);
			}

		} catch (Exception e) {
			validationResult.addError(name + ": '" + str + "' Invalid. Must be integer.");
		}

		if (wmoValid && typValid) {
			if (!plfmType.equals("FLOAT")) {
				if (!ArgoReferenceTable.PLATFORM_TYPExWMO_INST.xrefContains(plfmType, str)) {
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
			if (!arFile.getFileSpec().isPhysicalParamName(str)) {
				validationResult.addError(name + "[" + (n + 1) + "]: '" + str + "': Invalid");
			}
		}

		name = "PARAMETER_UNITS"; // ..not empty
		paramVar = arFile.readStringArr(name);

		for (int n = 0; n < nParam; n++) {
			str = paramVar[n].trim();
			log.debug(name + "[{}]: '{}'", n, str);
			if (str.length() <= 0) {
				validationResult.addError(name + "[" + (n + 1) + "]: Empty");
			}
		}

		name = "PARAMETER_SENSOR"; // ..not empty
		paramVar = arFile.readStringArr(name);

		for (int n = 0; n < nParam; n++) {
			str = paramVar[n];
			log.debug(name + "[{}]: '{}'", n, str);
			if (str.length() <= 0) {
				validationResult.addError(name + "[" + (n + 1) + "]: Empty");
			}
		}

		name = "PREDEPLOYMENT_CALIB_COEFFICIENT"; // ..not empty
		paramVar = arFile.readStringArr(name);

		for (int n = 0; n < nParam; n++) {
			str = paramVar[n].trim();
			log.debug(name + "[{}]: '{}'", n, str);

			if (str.length() <= 0) {
				validationResult.addError(name + "[" + (n + 1) + "]: Empty");
			}
		}

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
			ArgoReferenceTable.ArgoReferenceEntry mdlInfo;
			ArgoReferenceTable.ArgoReferenceEntry mkrInfo;
			ArgoReferenceTable.ArgoReferenceEntry snsrInfo;

			// ..check SENSOR
			boolean snsrValid = false;
			String snsr = sensor[n].trim();
			log.debug(sensorName + "[{}]: '{}'", n, snsr);

			if ((snsrInfo = ArgoReferenceTable.SENSOR.contains(snsr)).isValid()) {
				snsrValid = true;
				if (snsrInfo.isDeprecated) {
					validationResult
							.addWarning(sensorName + "[" + (n + 1) + "]: '" + snsr + "' Status: " + snsrInfo.message);
				}

			} else {
				validationResult.addError(sensorName + "[" + (n + 1) + "]: '" + snsr + "' Status: " + snsrInfo.message);
			}

			// ..check SENSOR_MAKER
			String snsrMaker = sensorMaker[n].trim();
			boolean smkrValid = false;
			log.debug(sensorMakerName + "[{}]: '{}'", n, snsrMaker);

			if ((mkrInfo = ArgoReferenceTable.SENSOR_MAKER.contains(snsrMaker)).isValid()) {
				smkrValid = true;
				if (mkrInfo.isDeprecated) {
					validationResult.addWarning(
							sensorMakerName + "[" + (n + 1) + "]: '" + snsrMaker + "' Status: " + mkrInfo.message);
				}

			} else {
				validationResult.addError(
						sensorMakerName + "[" + (n + 1) + "]: '" + snsrMaker + "' Status: " + mkrInfo.message);
			}

			// ..check SENSOR_MODEL
			String snsrModel = sensorModel[n].trim();
			boolean mdlValid = false;
			log.debug(sensorModelName + "[{}]: '{}'", n, snsrModel);

			if ((mdlInfo = ArgoReferenceTable.SENSOR_MODEL.contains(snsrModel)).isValid()) {
				mdlValid = true;
				if (mdlInfo.isDeprecated) {
					validationResult.addWarning(
							sensorModelName + "[" + (n + 1) + "]: '" + snsrModel + "' Status: " + mdlInfo.message);
				}
			} else {
				validationResult.addError(
						sensorModelName + "[" + (n + 1) + "]: '" + snsrModel + "' Status: " + mdlInfo.message);
			}

			// ..cross-reference SENSOR_MODEL / SENSOR_MAKER
			if (smkrValid && mdlValid) {
				if (!snsrModel.equals("UNKNOWN")) {
					String mkr = mkrInfo.getColumn(1);
					String mdl = mdlInfo.getColumn(1);

					if (!ArgoReferenceTable.SENSOR_MODELxSENSOR_MAKER.xrefContains(mdl, mkr)) {
						validationResult.addError(sensorModelName + "/" + sensorMakerName + "[" + (n + 1) + "]: "
								+ "Inconsistent: '" + snsrModel + "'/'" + snsrMaker + "'");
						log.debug("SENSOR_MODEL/SENSOR_MAKER xref inconsistent: mdl, mkr = '{}', '{}'", mdl, mkr);
					} else {
						log.debug("SENSOR_MODEL/SENSOR_MAKER xref valid: mdl, mkr = '{}', '{}'", mdl, mkr);
					}
				}
			}

			// ..cross-reference SENSOR_MODEL / SENSOR
			if (snsrValid && mdlValid) {
				if (!snsrModel.equals("UNKNOWN")) {
					String sn = snsrInfo.getColumn(1);
					String mdl = mdlInfo.getColumn(1);

					if (!ArgoReferenceTable.SENSOR_MODELxSENSOR.xrefContains(mdl, sn)) {
						validationResult.addError(sensorModelName + "/" + sensorName + "[" + (n + 1) + "]: "
								+ "Inconsistent: '" + snsrModel + "'/'" + snsr + "'");
						log.debug("SENSOR_MODEL/SENSOR xref inconsistent: mdl, sn = '{}', '{}'", mdl, sn);
					} else {
						log.debug("SENSOR_MODEL/SENSOR xref valid: mdl, sn = '{}', '{}'", mdl, sn);
					}
				}
			}
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

			if ((info = ArgoReferenceTable.POSITIONING_SYSTEM.contains(str)).isValid()) {
				if (info.isDeprecated) {
					validationResult.addWarning(name + "[" + (n + 1) + "]: '" + str + "' Status: " + info.message);
				}
			} else {
				validationResult.addError(name + "[" + (n + 1) + "]: '" + str + "' Status: " + info.message);
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

			if ((info = ArgoReferenceTable.TRANS_SYSTEM.contains(str)).isValid()) {
				if (info.isDeprecated) {
					validationResult.addWarning(name + "[" + (n + 1) + "]: '" + str + "' Status: " + info.message);
				}
			} else {
				validationResult.addError(name + "[" + (n + 1) + "]: '" + str + "' Status: " + info.message);
			}
		}

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

	private void checkParameterValueAgainstRefTable(String parameterName, StringTable refTable) {
		ArgoReferenceTable.ArgoReferenceEntry info;
		String parameterValue = arFile.readString(parameterName).trim();

		log.debug("{}: '{}'", parameterName, parameterValue);
		if ((info = refTable.contains(parameterValue)).isValid()) {
			if (info.isDeprecated) {
				validationResult.addWarning(parameterName + ": '" + parameterValue + "' Status: " + info.message);
			}

		} else {
			validationResult.addError(parameterName + ": '" + parameterValue + "' Status: " + info.message);
		}
	}

	private void checkOptionalParameterValueAgainstRefTable(String parameterName, StringTable refTable) {

		Variable dataVar = arFile.getNcReader().findVariable(parameterName);
		if (dataVar != null) {
			checkParameterValueAgainstRefTable(parameterName, refTable);
		}
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

		// .........battery_type............
		int nTypes = 0;

		String str = arFile.readString("BATTERY_TYPE");
		log.debug("BATTERY_TYPE: '{}'", str);

		if (str.length() <= 0) {
			validationResult.addError("BATTERY_TYPE: Empty");

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

					if (!ArgoReferenceTable.BATTERY_TYPE_manufacturer.contains(manu)) {
						String err = String.format("BATTERY_TYPE[%d]: Invalid manufacturer: '{%s}'", nTypes, manu);
						// validationResult.addError(err);

						// ################# TEMPORARY WARNING ################
						validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
						log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

						log.debug("...invalid manufacturer");

					} else {
						log.debug("valid manufacturer");
					}

					if (!ArgoReferenceTable.BATTERY_TYPE_type.contains(type)) {
						String err = String.format("BATTERY_TYPE[%d]: Invalid type: '{%s}'", nTypes, type);
						// validationResult.addError(err);

						// ################# TEMPORARY WARNING ################
						validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
						log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

						log.debug("invalid type");

					} else {
						log.debug("valid type");
					}

				} else {
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

		// .....battery_packs.....
		int nPacks = -1;

		str = arFile.readString("BATTERY_PACKS");

		log.debug("BATTERY_PACKS: '{}'", str);

		if (str.length() <= 0) {
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
					log.debug("battery_packs substring == U (undefined)");

				} else {
					// ..not undefined -- check pattern

					Matcher m = pBatteryPacks.matcher(substr);

					if (m.matches()) {

						String num = m.group("numofpacks");
						String style = m.group("style");
						String type = m.group("type");

						log.debug("...matched pattern: num, style, type = '{}', '{}', '{}", num, style, type);

						if (!ArgoReferenceTable.BATTERY_PACKS_style.contains(style)) {
							String err = String.format("BATTERY_PACKS[%d]: Invalid style of battery: '{%s}'", nPacks,
									style);
							// validationResult.addError(err);

							// ################# TEMPORARY WARNING ################
							validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
							log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

							log.debug("invalid style");

						} else {
							log.debug("valid style");
						}

						if (!ArgoReferenceTable.BATTERY_PACKS_type.contains(type)) {
							String err = String.format("BATTERY_PACKS[%d]: Invalid type: '{%s}'", nPacks, type);
							// validationResult.addError(err);

							// ################# TEMPORARY WARNING ################
							validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
							log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

							log.debug("invalid type");

						} else {
							log.debug("valid type");
						}

					} else {
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

		// ............compare TYPES and PACKS............

		if (nPacks >= 0) {

			if (nTypes != nPacks) {
				String err = String.format("Number of BATTERY_TYPES {} != number of BATTERY_PACKS {}", nTypes, nPacks);
				// validationResult.addError(err);

				// ################# TEMPORARY WARNING ################
				validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
				log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

				log.debug("number of types != number of packs => {} != {}", nTypes, nPacks);
			}

		} // ..end if nPacks >= 0

	}// ..end validateBattery

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
								if (!ArgoReferenceTable.GENERIC_TEMPLATE_short_sensor_name.contains(str)) {
									String err = String.format("%s[%d]: Invalid short_sensor_name '%s' in '%s'",
											varName, (n + 1), str);
									// validationResult.addError(err);

									// ################# TEMPORARY WARNING ################
									validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
									log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(),
											err);

									log.debug("...generic short_sensor_name lookup: INVALID = '{}'", str);
								} else {
									log.debug("...generic short_sensor_name lookup: valid = '{}'", str);
								}
							}

							str = match.unMatchedTemplates.get("cyclephasename");
							if (str != null) {
								if (!ArgoReferenceTable.GENERIC_TEMPLATE_cycle_phase_name.contains(str)) {
									String err = String.format("%s[%d]: Invalid cycle_phase_name '%s' in '%s'", varName,
											(n + 1), str, param);
									// validationResult.addError(err)

									// ################# TEMPORARY WARNING ################
									validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
									log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(),
											err);

									log.debug("...generic cycle_phase_name lookup: INVALID = '{}'", str);

								} else {
									log.debug("...generic cycle_phase_name lookup: valid = '{}'", str);
								}
							}

							str = match.unMatchedTemplates.get("param");
							if (str != null) {
								if (!ArgoReferenceTable.GENERIC_TEMPLATE_param.contains(str)) {
									String err = String.format("%s[%d]: Invalid param '%s' in '%s'", varName, (n + 1),
											str, param);

									// validationResult.addError(err)

									// ################# TEMPORARY WARNING ################
									validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
									log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(),
											err);

									log.debug("...generic param: generic name lookup: INVALID = '{}'", str);
								} else {
									log.debug("...generic param: generic name lookup: valid = '{}'", str);
								}
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

} // ..end class
