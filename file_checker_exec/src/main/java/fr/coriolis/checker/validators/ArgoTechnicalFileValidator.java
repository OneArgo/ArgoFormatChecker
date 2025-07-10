package fr.coriolis.checker.validators;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.core.ArgoDataFile;
import fr.coriolis.checker.specs.ArgoConfigTechParam;
import fr.coriolis.checker.specs.ArgoReferenceTable;

/**
 * Implements all of the features required to validate ArgoTechnicalFiles
 *
 * @version $HeadURL:
 *          https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoTechnicalFile.java
 *          $
 * @version $Id: ArgoTechnicalFile.java 1263 2021-06-14 17:59:56Z ignaszewski $
 */

public class ArgoTechnicalFileValidator extends ArgoFileValidator {

	// .........................................
	// VARIABLES
	// .........................................

	// ..class variables
	// ..standard i/o shortcuts
	private static final Logger log = LogManager.getLogger("ArgoTechnicalFileValidator");

	// .......................................
	// CONSTRUCTORS
	// .......................................

	protected ArgoTechnicalFileValidator(ArgoDataFile arFile) throws IOException {
		super(arFile);
	}

//	public ArgoTechnicalFileValidator(String specDir, String version) {
//		// super(specDir, FileType.PROFILE, version);
//	}

	// ..........................................
	// METHODS
	// ..........................................

	/**
	 * Opens an existing file and the assoicated <i>Argo specification</i>).
	 *
	 * @param inFile   the string name of the file to open
	 * @param specDir  the string name of the directory containing the format
	 *                 specification files
	 * @param fullSpec true = open the full specification; false = open the template
	 *                 specification
	 * @return the file object reference. Returns null if the file is not opened
	 *         successfully. (ArgoTechnicalFile.getMessage() will return the reason
	 *         for the failure to open.)
	 * @throws IOException If an I/O error occurs
	 */
//	public static ArgoTechnicalFileValidator open(String inFile, String specDir, boolean fullSpec) throws IOException {
//		ArgoDataFile arFile = ArgoDataFile.open(inFile, specDir, fullSpec);
//		if (!(arFile instanceof ArgoTechnicalFileValidator)) {
//			ValidationResult.lastMessage = "ERROR: '" + inFile + "' not an Argo PROFILE file";
//			return null;
//		}
//
//		return (ArgoTechnicalFileValidator) arFile;
//	}

	/**
	 * Validates the data in the technical file. This is a driver routine that
	 * performs all types of validations (see other validate* routines).
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param dacName name of the DAC for this file
	 * @cknulls true = check for 'null' characters in the String values
	 * @return success indicator. true - validation was performed. false -
	 *         validation could not be performed (getMessage() will return the
	 *         reason).
	 * @throws IOException If an I/O error occurs
	 */
	public boolean validateData(String dacName, boolean ckNulls) throws IOException {
		boolean basicsChecks = super.basicDataValidation(ckNulls);
		if (!basicsChecks) {
			return false;
		}

		// Validate tech meta data
		validateMetaData(arFile.getValidatedDac());

		validateDates();

		if (arFile.fileVersion().startsWith("2.4") || arFile.fileVersion().startsWith("3")) {
			validateTechParams();
		}

		return true;
	}// ..end validate

	/**
	 * Validates the dates in the profile file.
	 * 
	 * Date Checks
	 * <ul>
	 * <li>DATE_CREATION: After start of Argo era. Before now.
	 * <li>DATE_UPDATE: After DATE_CREATION. Before now.
	 * </ul>
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	public void validateDates() throws IOException {
		log.debug(".....validateDates.....");
		Date fileTime = new Date(arFile.getFile().lastModified());
		// ...........creation and update dates checks:.............
		super.validateCreationUpdateDates(fileTime);
	}// ..end validateDates

	/**
	 * Validates the meta-data in the technical file.
	 * 
	 * Meta-data checks PLATFORM_NUMBER: Valid (5 or 7 numeric digits) DATA_CENTRE:
	 * Valid value - Ref table 4
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param dac the ArgoReferenceTable.DACS dac indicator. If <i>null</i> the
	 *            DATA_CENTRE will not be checked
	 * @throws IOException If an I/O error occurs
	 */
	public void validateMetaData(ArgoReferenceTable.DACS dac) throws IOException {
		log.debug(".....validateMetaData.....");

		// PLATFORM_NUMBER
		String str = arFile.readString("PLATFORM_NUMBER").trim();
		if (!super.validatePlatfomNumber(str)) {
			validationResult.addError("PLATFORM_NUMBER" + ": '" + str + "': Invalid");
		}

		// DATA_CENTRE
		super.validateDataCentre(dac);
	}// ..end validateMetaData

	/**
	 * Validates the technical parameter names and values in the technical file.
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param ckValue true = validate the values also
	 * @throws IOException If an I/O error occurs
	 */
	public void validateTechParams() throws IOException {
		log.debug(".....validateTechParams.....");

		int nParam = arFile.getDimensionLength("N_TECH_PARAM");
		log.debug("n_technical_parameter: {}", nParam);

		// ..read technical parameters and values

		String nName = "TECHNICAL_PARAMETER_NAME";
		// String vName = "TECHNICAL_PARAMETER_VALUE";

		String[] full_name = arFile.readStringArr(nName);
		/*
		 * 2021-06-01: value checks are currently not being worked on
		 *
		 * String[] value = readStringArr(vName);
		 */

		// ...........loop over names/values.............

		HashSet<String> nameAlreadyChecked = new HashSet<String>(100);
		HashMap<String, Boolean> unitAlreadyChecked = new HashMap<String, Boolean>(100);

		for (int n = 0; n < nParam; n++) {
			String full = full_name[n].trim();
			int index = full.lastIndexOf('_');

			if (index <= 0) {
				// ..poorly formed name - only report if not already reported

				if (!nameAlreadyChecked.contains(full)) {
					validationResult.addError(nName + "[" + (n + 1) + "]: " + "Incorrectly formed name '" + full + "'");
					nameAlreadyChecked.add(full);
				}

				nameAlreadyChecked.add(full);
				log.debug("badly formed name: {}[{}] = '{}'", nName, n, full);

				continue; // ..can't do anything with this
			}

			// ..well formed named, break it apart

			String param = full.substring(0, index);
			String unit = full.substring(index + 1);

			log.debug("check {}[{}]: full '{}'; param '{}'; unit '{}'", nName, n, full, param, unit);

			// .....check name (minus unit).....

			if (!nameAlreadyChecked.contains(param)) {
				// ..this parameter name has not been checked

				ArgoConfigTechParam.ArgoConfigTechParamMatch match = arFile.getFileSpec().ConfigTech
						.findTechParam(param);

				if (match == null) {
					// ..NOT an active name, NOT a deprecated name --> error
					String err = String.format("%s[%d]: Invalid name '%s'", nName, (n + 1), param);
					// validationResult.addError(err);

					// ################# TEMPORARY WARNING ################
					validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
					log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

					log.debug("invalid param (not active or deprecated): '{}'", param);

				} else {
					if (match.isDeprecated) {
						// ..IS a deprecated name --> warning
						validationResult.addWarning(nName + "[" + (n + 1) + "]: " + "Deprecated name '" + param);
						log.debug("parameter is deprecated: '{}'", param);
					}

					if (match.nFailedMatchedTemplates > 0) {
						// ..these Templates failed to match the values specified in the table
						// ..they are errors

						for (Map.Entry<String, String> entry : match.failedMatchedTemplates.entrySet()) {
							String tmplt = entry.getKey();
							String val = entry.getValue();

							String err = String.format("%s[%d]: Invalid template/value '%s'/'%s' in '%s'", nName,
									(n + 1), tmplt, val, param);
							// validationResult.addError(err);

							// ################# TEMPORARY WARNING ################
							validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
							log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

							log.debug("...invalid template/value '{}'/'{}'", tmplt, val);
						}
					}

					if (match.nUnMatchedTemplates > 0) {
						// ..these Templates did not have values specified in the table

						// ..check the generic template values:
						// .. shortsensorname
						// ..all others are accepted as is - they matched their basic regex
						// ..- assume they are good

						String str = match.unMatchedTemplates.get("shortsensorname");
						if (str != null) {
							if (!ArgoReferenceTable.GENERIC_TEMPLATE_short_sensor_name.contains(str)) {
								String err = String.format("%s[%d]: Invalid short_sensor_name '%s' in '%s'", nName,
										(n + 1), str, param);
								// validationResult.addError(err);

								// ################# TEMPORARY WARNING ################
								validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
								log.warn("TEMP WARNING: {}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

								log.debug("...generic short_sensor_name lookup: INVALID = '{}'", str);
							} else {
								log.debug("...generic short_sensor_name lookup: valid = '{}'", str);
							}
						}
					}
				}

				nameAlreadyChecked.add(param);
			}

			// ......check the unit......

			boolean validUnit = false;

			if (!unitAlreadyChecked.containsKey(unit)) {
				// ..this unit name has NOT been checked

				if (!arFile.getFileSpec().ConfigTech.isConfigTechUnit(unit)) {
					// ..NOT an active unit

					if (arFile.getFileSpec().ConfigTech.isDeprecatedConfigTechUnit(unit)) {
						// ..IS a deprecated unit --> warning

						validUnit = true;
						validationResult.addWarning(
								nName + "[" + (n + 1) + "]: " + "Deprecated unit '" + unit + "' in '" + full + "'");
						log.warn("'{}': unit is deprecated", unit);

					} else {
						// ..NOT an active unit, NOT a deprecated unit --> error
						validUnit = false;
						validationResult.addError(
								nName + "[" + (n + 1) + "]: " + "Invalid unit '" + unit + "' in '" + full + "'");
						log.debug("unit is invalid (new or old)", unit);
					}

				} else {
					// ..IS an active unit

					validUnit = true;
				} // ..end check the unit

				unitAlreadyChecked.put(unit, validUnit);

			} else {
				// ..unit already checked --- was it valid?
				validUnit = unitAlreadyChecked.get(unit);

			} // ..end check unit

			// ......check the value.....

			/*
			 * 2021-06-01: value checks are currently not being worked on
			 *
			 * if (validUnit) { log.debug("valid unit");
			 *
			 * if (! spec.ConfigTech.isConfigTechValidValue(unit, value[n].trim())) { String
			 * err =
			 * String.format("%s[%d]: Invalid value for '%s' (type '%s'): value = '%s'",
			 * vName, (n+1), full, spec.ConfigTech.getConfigTechDataType(unit),
			 * value[n].trim()); //validationResult.addError(err);
			 *
			 * //################# TEMPORARY WARNING ################
			 * validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
			 * log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);
			 *
			 * log.debug("invalid value"); } } //..end if validUnit
			 */
		} // ..end for nParam

	} // ..end validateTechParams

} // ..end class
