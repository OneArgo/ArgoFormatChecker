package fr.coriolis.checker.filetypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.specs.ArgoConfigTechParam;
import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.specs.ArgoReferenceTable;
import ucar.ma2.ArrayChar;

/**
 * Implements all of the features required to validate ArgoTechnicalFiles
 *
 * @version $HeadURL:
 *          https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoTechnicalFile.java
 *          $
 * @version $Id: ArgoTechnicalFile.java 1263 2021-06-14 17:59:56Z ignaszewski $
 */

public class ArgoTechnicalFile extends ArgoDataFile {

	// .........................................
	// VARIABLES
	// .........................................

	// ..class variables
	// ..standard i/o shortcuts
	private static final Logger log = LogManager.getLogger("ArgoTechnicalFile");

	private final static long oneDaySec = 1L * 24L * 60L * 60L * 1000L;

	// ..object variables
	private String data_mode;

	private ArrayList<String>[] profParam;

	// .......................................
	// CONSTRUCTORS
	// .......................................

	protected ArgoTechnicalFile() throws IOException {
		super();
	}

	public ArgoTechnicalFile(String specDir, String version) {
		// super(specDir, FileType.PROFILE, version);
	}

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
	public static ArgoTechnicalFile open(String inFile, String specDir, boolean fullSpec) throws IOException {
		ArgoDataFile arFile = ArgoDataFile.open(inFile, specDir, fullSpec);
		if (!(arFile instanceof ArgoTechnicalFile)) {
			ValidationResult.lastMessage = "ERROR: '" + inFile + "' not an Argo PROFILE file";
			return null;
		}

		return (ArgoTechnicalFile) arFile;
	}

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
	public boolean validate(String dacName, boolean ckNulls) throws IOException {
		ArgoReferenceTable.DACS dac = null;

		if (!validationResult.isValid()) {
			ValidationResult.lastMessage = "File must be verified (verifyFormat) " + "successfully before validation";
			return false;
		}

		// .......check arguments.......
		if (dacName.trim().length() > 0) {
			for (ArgoReferenceTable.DACS d : ArgoReferenceTable.DACS.values()) {
				if (d.name.equals(dacName)) {
					dac = d;
					break;
				}
			}
			if (dac == null) {
				ValidationResult.lastMessage = "Unknown DAC name = '" + dacName + "'";
				return false;
			}
		}

		// ............Determine number of tech params............

		if (ckNulls) {
			validateStringNulls();
		}

		validateMetaData(dac);
		validateDates();

		if (format_version.startsWith("2.4") || format_version.startsWith("3")) {
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

		String creation = ((ArrayChar) ncReader.findVariable("DATE_CREATION").read()).getString();
		String update = ((ArrayChar) ncReader.findVariable("DATE_UPDATE").read()).getString();
		Date fileTime = new Date(file.lastModified());
		long fileSec = fileTime.getTime();

		if (log.isDebugEnabled()) {
			log.debug("earliestDate:  " + ArgoDate.format(earliestDate));
			log.debug("fileTime:      " + ArgoDate.format(fileTime));
			log.debug("DATE_CREATION: " + creation);
			log.debug("DATE_UPDATE:   " + update);
		}

		// ...........initial creation date checks:.............
		// ..set, after earliestDate, and before file time
		Date dateCreation = null;
		boolean haveCreation = false;
		long creationSec = 0;

		if (creation.trim().length() <= 0) {
			validationResult.addError("DATE_CREATION: Not set");

		} else {
			dateCreation = ArgoDate.get(creation);
			haveCreation = true;

			if (dateCreation == null) {
				haveCreation = false;
				validationResult.addError("DATE_CREATION: '" + creation + "': Invalid date");

			} else {
				creationSec = dateCreation.getTime();

				if (dateCreation.before(earliestDate)) {
					validationResult.addError("DATE_CREATION: '" + creation + "': Before allowed date ('"
							+ ArgoDate.format(earliestDate) + "')");

				} else if ((creationSec - fileSec) > oneDaySec) {
					validationResult.addError("DATE_CREATION: '" + creation + "': After system file time ('"
							+ ArgoDate.format(fileTime) + "')");
				}
			}
		}

		// ............initial update date checks:...........
		// ..set, not before creation time, before file time
		Date dateUpdate = null;
		boolean haveUpdate = false;
		long updateSec = 0;

		if (update.trim().length() <= 0) {
			validationResult.addError("DATE_UPDATE: Not set");
		} else {
			dateUpdate = ArgoDate.get(update);
			haveUpdate = true;

			if (dateUpdate == null) {
				validationResult.addError("DATE_UPDATE: '" + update + "': Invalid date");
				haveUpdate = false;

			} else {
				updateSec = dateUpdate.getTime();

				if (haveCreation && dateUpdate.before(dateCreation)) {
					validationResult
							.addError("DATE_UPDATE: '" + update + "': Before DATE_CREATION ('" + creation + "')");
				}

				if ((updateSec - fileSec) > oneDaySec) {
					validationResult.addError("DATE_UPDATE: '" + update + "': After system file time ('"
							+ ArgoDate.format(fileTime) + "')");
				}
			}
		}
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

		ArrayChar.D1 plNum = (ArrayChar.D1) ncReader.findVariable("PLATFORM_NUMBER").read();
		ArrayChar.D1 dc = (ArrayChar.D1) ncReader.findVariable("DATA_CENTRE").read();

		String firstNum = new String(plNum.getString().trim());

		log.debug("PLATFORM_NUMBER: '{}'", plNum.getString());

		String s = plNum.getString().trim();
		if (!s.matches("[1-9][0-9]{4}|[1-9]9[0-9]{5}")) {
			validationResult.addError("PLATFORM_NUMBER: '" + s + "': Invalid");
		}

		log.debug("DATA_CENTRE: '" + dc.getString() + "'");

		s = dc.getString().trim();
		if (dac != null) {
			if (!ArgoReferenceTable.DacCenterCodes.get(dac).contains(s)) {
				validationResult.addError("DATA_CENTRE: '" + s + "': Invalid for DAC " + dac);
			}

		} else { // ..incoming DAC not set
			if (!ArgoReferenceTable.DacCenterCodes.containsValue(s)) {
				validationResult.addError("DATA_CENTRE: '" + s + "': Invalid (for all DACs)");
			}
		}
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

		int nParam = getDimensionLength("N_TECH_PARAM");
		log.debug("n_technical_parameter: {}", nParam);

		// ..read technical parameters and values

		String nName = "TECHNICAL_PARAMETER_NAME";
		// String vName = "TECHNICAL_PARAMETER_VALUE";

		String[] full_name = readStringArr(nName);
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

				ArgoConfigTechParam.ArgoConfigTechParamMatch match = spec.ConfigTech.findTechParam(param);

				if (match == null) {
					// ..NOT an active name, NOT a deprecated name --> error
					String err = String.format("%s[%d]: Invalid name '%s'", nName, (n + 1), param);
					// validationResult.addError(err);

					// ################# TEMPORARY WARNING ################
					validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
					log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

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
							log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

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
								log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

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

				if (!spec.ConfigTech.isConfigTechUnit(unit)) {
					// ..NOT an active unit

					if (spec.ConfigTech.isDeprecatedConfigTechUnit(unit)) {
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
