package fr.coriolis.checker.validators;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.core.ArgoDataFile;
import fr.coriolis.checker.core.ArgoDataFile.FileType;
import fr.coriolis.checker.core.ValidationResult;
import fr.coriolis.checker.specs.ArgoAttribute;
import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.specs.ArgoDimension;
import fr.coriolis.checker.specs.ArgoFileSpecification;
import fr.coriolis.checker.specs.ArgoReferenceTable;
import fr.coriolis.checker.specs.ArgoVariable;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 * <b>FORMAT VERIFICATION:</b><br>
 * The format verification process (<i>verifyFormat</i>) compares the data file
 * to the "specification". Details of the specification are documented in
 * {@link fr.coriolis.checker.specs.ArgoFileSpecification}. .
 * <p>
 * The verification process ensures that:
 * <ul>
 * <li>The global attributes match the specification
 * <li>The dimensions in the data file match the specification (name and value)
 * <li>The variables in the data file match the specification (name, type, and
 * dimensions)
 * <li>The attributes of the variables match (name and setting)
 * </ul>
 * <p>
 * The specification includes two additional features: optional parameters and
 * highly-desirable parameters.
 * <p>
 * Optional parameters: These are dimensions and variables that may be omitted
 * from a data file. These are generally parameters that are not measured by all
 * platforms (such as CNDC and DOXY). Additionally, the variables can be grouped
 * into parameter groups such that all of the variables in the group must either
 * be omitted or present. (For instance, for DOXY either all DOXY-related
 * variables must be present or missing.)
 *
 * Highly-desirable parameters: The specification also includes
 * <i>highly-desirable</i> parameters. These are variables that are expected to
 * have values with certain settings. Failure to match these only generates a
 * format-warning.
 * 
 * The result of <b>verifyFormat</b> is a simple boolean: true - format passed;
 * false - format failed. Format failed could be either an ERROR if the format
 * is invalid or a WARNING if the highly-desirable parameters failed.
 * <P>
 * Use the methods to retrieve information about the failures, including
 * descriptive text messages.
 */
public class ArgoFileValidator {
	// class variables
	// ..standard i/o shortcuts
	private static PrintStream stderr = new PrintStream(System.err);
	private static final Logger log = LogManager.getLogger("ArgoFileValidator");

	static Pattern pDataMode; // ..CDL "variables:" tag
	static {
		// ..check for legal data_mode
		pDataMode = Pattern.compile("[RDA]+");
	}
	private final static DecimalFormat cycleFmt = new DecimalFormat("000");
	protected final static Date earliestDate = ArgoDate.get("19970101000000");
	protected final static long oneDaySec = 1L * 24L * 60L * 60L * 1000L;

	// ..object variables
	protected final ArgoDataFile arFile;
	protected ValidationResult validationResult;

	public ArgoFileValidator(ArgoDataFile arFile) {
		this.arFile = arFile;
		this.validationResult = new ValidationResult();
	}

	// ==================
	// Validation methods
	// ==================

	/**
	 * Verifies whether this file conforms to the Argo Netcdf specification. The
	 * specification for this file type and version must be open. (See
	 * 
	 * @see fr.coriolis.checker.specs.ArgoFileSpecification )
	 *      <p>
	 *      <ul>
	 *      <li><i>nFormatErrors</i> will retrieve the number of errors.
	 *      <li><i>formatErrors</i> will retrieve the error descriptions.
	 *      <li><i>nFormatWarnings</i> will retrieve the number of warnings
	 *      <li><i>formatWarnings</i> will retrieve the warning descriptions.
	 *      </ul>
	 *
	 * @param dacName Name of the dac for diagnostic purposes
	 * @return boolean status indicator: True - file checked (status unspecified);
	 *         False - failed to check format
	 */
	public boolean validateFormat(String dacName) {
		if (arFile.getFileSpec() == null) {
			log.info("File specification not opened");
			ValidationResult.lastMessage = "ERROR: File specification not opened for this file";
			validationResult.addError("ERROR: File specification not opened for this file");
			return false;
		}

		/*
		 * METHOD: 1) Iterate through the data file dimensions and compare to the spec -
		 * if an optional "extra" dimension is encountered, add it to the spec 2)
		 * Iterate through the data file variables and compare to the spec 2a) - for
		 * each variable, iterate through the attributes and compare - attribute
		 * regexs?????? 3) Iterate through the spec dimensions and compare to the data
		 * file 4) Iterate through the spec variables and compare to the data file -
		 * postpone processing of missing optional variables 4a) - for each variable,
		 * iterate through the attributes and compare 5) For missing optional variables,
		 * check that all variables for the group are missing. 6) Iterate through the
		 * spec global attributes and compare to the data file 7) Clear any "extra"
		 * dimensions that were added
		 */

		HashSet<String> dataElement = new HashSet<String>(); // ..reported elements
		HashSet<String> dataGroup = new HashSet<String>(); // ..groups with reported elements

		// ......Step 1: Compare the data file dimensions to the specification.......
		log.debug(".....verifyFormat: compare data dimensions to spec.....");
		verifyFileDimensions(dataElement, dataGroup);

		// .......Step 2: Compare the data file variables to the spec.......

		log.debug(".....verifyFormat: compare data variables to spec.....");
		verifyFileVariables(dataElement, dataGroup);

		// ......Step 3: Check the spec dimensions against data file.........
		// .. - only need to check existence - definitions already checked above
		log.debug(".....verifyFormat: compare spec dimensions to data.....");
		verifySpecDimensionsPresenceInData();

		// ......Step 4: Check the spec variables against the data.........
		// .. - only need to check existence - definitions already checked above
		log.debug(".....verifyFormat: compare spec variables to data.....");
		verifySpecVariablePresenceInData();

		// ..............Step 5: Finish group variables.........................
		// .. - for each group with a reported variable -- make sure all of them are
		// reported
		log.debug(".....verifyFormat: check reported groups.....");
		checkGroupsCompleteness(dataElement, dataGroup);

		// ......Step 6: Compare the spec global attributes to the file.......
		log.debug(".....verifyFormat: compare spec global attr to data file.....");
		verifyGlobalAttributes(dacName);

		arFile.getFileSpec().clearExtraDimensions();

		log.debug(".....verifyFormat: completed.....");

		return true;
	} // ..end validateFormat

	/**
	 * Before checking data, verify if the file had not failed the format
	 * validations and if a valid dac name hase been passed in command line. Nulls
	 * in the "char" variables are also checked.
	 *
	 * @param dacName name of the DAC for this file
	 * @param ckNulls true = check all strings for NULL values; false = skip
	 * @return success indicator. true - validation was performed. false -
	 *         validation could not be performed (getMessage() will return the
	 *         reason).
	 * @throws IOException If an I/O error occurs
	 */
	public boolean basicDataValidation(boolean ckNulls) throws IOException {
		// before checking data, verify if the file had not failed the format validation
		// :
		if (!validationResult.isValid()) {
			ValidationResult.lastMessage = new String(
					"File must be verified (verifyFormat) " + "successfully before validation");
			return false;
		}

		// check dacName passed in argument line:
		if (!checkDacNameArgument()) {
			ValidationResult.lastMessage = new String("Unknown DAC name = '" + arFile.getDacName() + "'");
			return false;
		}

		if (ckNulls) {
			validateStringNulls();
		}

		return true;
	}

	protected void validateCreationUpdateDates(Date fileTime) {
		long fileSec = fileTime.getTime();

		String creation = arFile.readString("DATE_CREATION").trim();
		String update = arFile.readString("DATE_UPDATE").trim();
		arFile.setCreationDate(creation);
		arFile.setUpdateDate(update);

		log.debug("DATE_CREATION:    '{}'", creation);
		log.debug("DATE_UPDATE:      '{}'", update);

		// ...........creation date checks:.............
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
					validationResult.addError("DATE_CREATION: '" + creation + "': Before earliest allowed date ('"
							+ ArgoDate.format(earliestDate) + "')");

				} else if ((arFile.getCreationSec() - fileSec) > oneDaySec) {
					validationResult.addError("DATE_CREATION: '" + creation + "': After GDAC receipt time ('"
							+ ArgoDate.format(fileTime) + "')");
				}
			}
		}
		arFile.setHaveCreationDate(haveCreation);
		arFile.setCreationSec(creationSec);
		// ............update date checks:...........
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

				if (arFile.isHaveCreationDate() && dateUpdate.before(dateCreation)) {
					validationResult
							.addError("DATE_UPDATE: '" + update + "': Before DATE_CREATION ('" + creation + "')");
				}

				if ((updateSec - fileSec) > oneDaySec) {
					validationResult.addError("DATE_UPDATE: '" + update + "': After GDAC receipt time ('"
							+ ArgoDate.format(fileTime) + "')");
				}
			}
		}
		arFile.setHaveUpdateDate(haveUpdate);
		arFile.setUpdateSec(updateSec);
	}

	protected boolean validatePlatfomNumber(String platformNumberStr) {
		log.debug("{}: '{}'", "PLATFORM_NUMBER", platformNumberStr);

		return platformNumberStr.matches("[1-9][0-9]{4}|[1-9]9[0-9]{5}");
	}

	protected void validateDataCentre(ArgoReferenceTable.DACS dac) {
		String name = "DATA_CENTRE"; // ..ref table 4 (and valid for DAC)
		String str = arFile.readString(name).trim();
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
	}

	private void verifyGlobalAttributes(String dacName) {
		for (String name : arFile.getFileSpec().getGlobalAttributeNames()) {
			ArgoAttribute specAttr = arFile.getFileSpec().getGlobalAttribute(name);
			Attribute dataAttr = arFile.findGlobalAttribute(name);
			if (log.isDebugEnabled()) {
				log.debug("spec attribute: " + name);
			}

			if (dataAttr == null) {
				// ..attribute in spec file is not in the data file

				String err = String.format("global attribute: %s: not defined in data file", name);
				// formatErrors.add(err);

				// ################# TEMPORARY WARNING ################
				validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
				log.warn("{}: {}: {}", dacName, arFile.getFileName(), err);

			} else {
				// ..spec attr is in data file -- check values
				checkGlobalAttributeValue(dacName, name, specAttr, dataAttr);
			}
		}
	}

	// ......................ckVarAttr...................
	/**
	 * Compares the attributes of a Netcdf Variable and an ArgoVariable
	 * 
	 * @param dataVar the Netcdf variable object
	 * @param specVar the ArgoVariable object
	 * @return the boolean status. True - they are the same. False - they differ.
	 */
	private boolean ckVarAttr(Variable dataVar, ArgoVariable specVar) {
		// ..compare attributes
		boolean returnVal = true;

		String varName = dataVar.getShortName();

		DATA_ATTR_LOOP: for (Attribute dataAttr : dataVar.getAttributes()) {
			String attrName = dataAttr.getShortName();
			ArgoAttribute specAttr = specVar.getAttribute(attrName);
			// log.debug ("ckVarAttr: {}", attrName);

			if (specAttr == null) {
				// ..data file attribute is not in the specification

				/*
				 * 2015-11: ADMT-16 decided that "extra attributes" are allowed
				 *
				 * The following code would cause it to be an error:
				 *
				 * returnVal = false; formatErrors.add("attribute: '"+varName+":"+
				 * attrName+"' not defined in specification '"+ spec.getSpecName()+"'");
				 *
				 * For now... they will be ignored by just doing nothing about it
				 */

				log.info("extra attribute (allowed): {}:{} not in spec", varName, attrName);
				continue;
			}

			// ..check if the spec attribute is label "NOT_ALLOWED" error
			ArgoAttribute.AttrHandling specialHandling = specAttr.getHandling();
			if (!checkAttributNotAllowed(varName, attrName, specialHandling)) {
				returnVal = false;
				continue;
			}
			;

			// ..check the data attribute type
			// ..the 3 types supported by ArgoAttribute are: Number, String, Object
			// ..netCDF supports: Number, String, List of (number or string)
			// ..
			// ..Number and String are the only 2 currently in use.
			// ..Hard crash if it is anything else.

			if (!checkAttributeType(varName, dataAttr, attrName, specAttr)) {
				returnVal = false;
				continue;
			}
			// ..check the attribute value
			if (!checkVarAttributeValue(dataVar, varName, dataAttr, attrName, specAttr, specialHandling)) {
				returnVal = false;
				continue;
			}
			// 2025-09 : special check for TECH_PARAM variable that authorized multiple
			// units and long_name for a same variable name:
			// (not ideal and should be changed when tech_param names and units handling in
			// NVS change.
			if (arFile.fileType() == ArgoDataFile.FileType.TECHNICAL) {
				if (!checkSpecialTechParamVarAttributeValue(dataVar, varName, dataAttr, attrName, specAttr,
						specialHandling)) {
					returnVal = false;
					continue;
				}
			}

		} // ..end for (dataAttr)
		return returnVal;
	} // ..end ckVarAttr

	/**
	 * // ..check the attribute value. if the spec attribute is "IGNORE_COMPLETELY"
	 * or "IGNORE_VALUE", we just don't care about the value so skip the check
	 * 
	 * @param dataVar
	 * @param varName
	 * @param dataAttr
	 * @param attrName
	 * @param specAttr
	 * @param specialHandling
	 * @return
	 */
	private boolean checkVarAttributeValue(Variable dataVar, String varName, Attribute dataAttr, String attrName,
			ArgoAttribute specAttr, ArgoAttribute.AttrHandling specialHandling) {
		if (!(specialHandling == ArgoAttribute.AttrHandling.IGNORE_COMPLETELY
				|| specialHandling == ArgoAttribute.AttrHandling.IGNORE_VALUE)) {
			String dataAttrValue = null;
			String specAttrValue = specAttr.getValue().toString();

			if (dataAttr.isString()) {
				try {
					dataAttrValue = dataAttr.getStringValue();
				} catch (Exception e) {
					validationResult.addError("attribute: " + varName + ":" + attrName + ": Bad value.  Not a string.");
					return false;

				}

			} else {
				try {
					dataAttrValue = dataAttr.getNumericValue().toString();
				} catch (Exception e) {
					validationResult
							.addError("attribute: " + varName + ":" + attrName + ": Bad value.  Not a numeric value.");
					return false;
				}
			}

			if (!dataAttrValue.equals(specAttrValue)) {
				// ..data file attribute is not the same as the spec file attribute
				ArgoFileSpecification.AttrRegex regex = arFile.getFileSpec().getAttrRegex(varName, attrName);
				if (regex == null) {
					// ..no regex .. this is a format error
					validationResult.addError(
							"attribute: " + varName + ":" + attrName + ": Definitions differ " + "\n\tSpecification = '"
									+ specAttrValue + "'" + "\n\tData File     = '" + dataAttrValue + "'");
					log.info("format error: {}:{} " + "attribute mismatch (no regex): spec, data = {}, {}", varName,
							attrName, specAttrValue, dataAttrValue);

					return false;

				} else {
					// ..regex defined ... does it match?
					if (!regex.pattern.matcher(dataAttrValue).matches()) {
						validationResult.addError("attribute: " + dataVar.getShortName() + ":" + dataAttr.getShortName()
								+ ": Definitions differ " + "\n\tSpecification = '" + regex.pattern + "' (regex)"
								+ "\n\tData File     = '" + dataAttrValue + "'");
						log.info("format error: " + attrName + " attribute regex mismatch '" + regex.pattern + "'");
						return false;

					} else {
						if (regex.warn) {
							validationResult.addError("attribute: " + dataVar.getShortName() + ":"
									+ dataAttr.getShortName() + ": Accepted; not standard value"
									+ "\n\tSpecification     = '" + specAttrValue + "'" + "\n\tException allowed = '"
									+ regex.pattern + "' (regex)" + "\n\tData File         = '" + dataAttrValue + "'");
							log.warn("regex match (WARN): attribute '{}:{} = '{}' matches '{}'", varName, attrName,
									dataAttrValue, regex.pattern);
						} else {
							log.warn("regex match (NO WARN): attribute '{}:{} = '{}' matches '{}'", varName, attrName,
									dataAttrValue, regex.pattern);
						}
					}
				} // ..end if regex
			} // ..end attr.equals
		} else {
			log.debug("ckVarAttr: '{}': marked as IGNORE", attrName);
		} // ..end if (marked IGNORE)
		return true;
	}

	/**
	 * Special case with <TECH_PARAM> wich for a same variable name have list of
	 * units possble and may have different long_name. First, identify if the
	 * variable name is in the ConfigTech's param list. Then, if attribute to check
	 * is units or long_name, check the authorized list : all units from reference
	 * table are authorized for all parameters and list of long_name by parameter.
	 */
	private boolean checkSpecialTechParamVarAttributeValue(Variable dataVar, String varName, Attribute dataAttr,
			String attrName, ArgoAttribute specAttr, ArgoAttribute.AttrHandling specialHandling) {
		if (arFile.getFileSpec().ConfigTech != null && arFile.getFileSpec().ConfigTech.findTechParam(varName) != null) {
			// this variable is TECH_PARAM variable, check its attribut with list of units
			// and long_name.
			// dataAtt type already check in checkVarAttributeValue
			String dataAttrValue = dataAttr.getStringValue();
			if (attrName.equals("units") && !arFile.getFileSpec().ConfigTech.isConfigTechUnit(dataAttrValue)
					&& !arFile.getFileSpec().ConfigTech.isDeprecatedConfigTechUnit(dataAttrValue)) {
				// dataAtt type already check in checkVarAttributeValue
				// units not found in reference table !
				validationResult.addError("attribute: " + varName + ":" + attrName + ": Definitions differ "
						+ "\n\tSpecification = See argo-tech_units-spec units list" + "\n\tData File     = '"
						+ dataAttrValue + "'");

			}
			List<String> authorizedLongName = arFile.getFileSpec().ConfigTech.getParamAuthorizedLongName().get(varName);
			if (attrName.equals("long_name") && authorizedLongName != null
					&& !authorizedLongName.contains(dataAttrValue)) {

				validationResult.addError("attribute: " + varName + ":" + attrName + ": Definitions differ "
						+ "\n\tSpecification = See argo-tech_names-spec list, definition column"
						+ "\n\tData File     = '" + dataAttrValue + "'");

			}

		}

		return true;
	}

	/**
	 * // ..check the data attribute type. The 3 types supported by ArgoAttribute
	 * are: Number, String, Object netCDF supports: Number, String, List of (number
	 * or string) Number and String are the only 2 currently in use. Hard crash if
	 * it is anything else.
	 * 
	 * @param varName
	 * @param dataAttr
	 * @param attrName
	 * @param specAttr
	 * @return
	 */
	private boolean checkAttributeType(String varName, Attribute dataAttr, String attrName, ArgoAttribute specAttr) {
		DataType dataAttrType = dataAttr.getDataType();

		if (specAttr.isString()) {
			if (!dataAttr.isString()) {
				String err = String.format("attribute: %s:%s: Incorrect attribute value type. Must be string", varName,
						attrName);
				// formatErrors.add(err)

				// ################# TEMPORARY WARNING ################
				validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
				log.warn("{}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

				return false;

			}
			// log.debug("ckVarAttr: '{}': spec/data both String", attrName);

		} else if (specAttr.isNumeric()) {
			if (!dataAttrType.isNumeric()) {
				String err = String.format("attribute: %s:%s: Incorrect attribute value type. Must be numeric", varName,
						attrName);

				// formatErrors.add(err);
				// ################# TEMPORARY WARNING ################
				validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");

				log.warn("{}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

				return false;

			}
			// log.debug("ckVarAttr: '{}': spec/data both Number", attrName);

		} else {
			// ..what the hell were you thinking?
			stderr.println("\n\n******\n" + "****** PROGRAM ERROR: ArgoDataFile(ckvarattr) " + varName + ":" + attrName
					+ ": unknown specAttr type.  TERMINATING.\n" + "******");
			System.exit(1);
		}
		return true;
	}

	private boolean checkAttributNotAllowed(String varName, String attrName,
			ArgoAttribute.AttrHandling specialHandling) {
		if (specialHandling == ArgoAttribute.AttrHandling.NOT_ALLOWED) {
			String err = String.format("attribute: %s:%s: Attribute is not allowed.", varName, attrName);
			// formatErrors.add(err)

			// ################# TEMPORARY WARNING ################
			validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
			log.warn("{}: {}: {}", arFile.getDacName(), arFile.getFileName(), err);

			return false;

		}
		return true;
	}

	// ......................ckVarDims...................
	/**
	 * Compares the dimensions of a Netcdf Variable and an ArgoVariable
	 * 
	 * @param dataVar the Netcdf variable object
	 * @param specVar the ArgoVariable object
	 * @return the boolean status. True - they are the same. False - they differ.
	 */
	private boolean ckVarDims(Variable dataVar, ArgoVariable specVar) {
		String specDims = specVar.getDimensionsString();
		String dataDims = dataVar.getDimensionsString();

		int specRank = specVar.getRank(); // ..rank of the variable W/O extra dims
		int dataRank = dataVar.getRank();

		boolean altDimVar = specVar.canHaveAlternateDimensions();
		boolean exDimVar = specVar.canHaveExtraDimensions();

		boolean pass = true;
		if (dataRank < specRank) {
			// ..always an error
			pass = false;

		} else if (specDims.equals(dataDims)) {
			// ..always a success
			pass = true;

		} else if (!(altDimVar || exDimVar)) {
			// ..dimension strings don't match
			// ..not a "special variable"
			pass = false;

		} else {
			// ..where are we?
			// .. dataRank >= specRank
			// .. data dimension names != spec dimension names
			// .. it is a "special variable"

			// ..need to disect the individual dimensions

			for (int n = 0; n < dataRank; n++) {
				String dDimName = dataVar.getDimension(n).getShortName();

				if (n < specRank) {
					// ..one of the regular dimensions

					ArgoDimension sDim = specVar.getDimension(n);
					String sDimName = sDim.getName();

					if (!sDimName.equals(dDimName)) {
						// ..isn't the expected one

						if (!sDim.isAllowedAlternateDimensionName(dDimName)) {
							// ..this captures to possibilities
							// ..1) this dimension is not an alt-dim
							// ..or
							// ..2) this dimension is not an allowed dim within an alt-dim

							pass = false;
							break;

						} else {
							log.debug("ckVarDims: allowed alternate dim = '{}'", dDimName);
						}

					}

				} else {
					// ..an "extra dimension"
					// ..check that it is valid
					if (!arFile.getFileSpec().getDimension(dDimName).isExtraDimension()) {
						pass = false;
						break;
					}
				}
			} // ..end for (dataRank)
		} // ..end if (specRank ... dataRank)..end if evaluating dimensions

		log.debug("ckVarDims: pass = {}: specRank, dataRank, altDimVar, exDimVar = {}, {}, {}, {}", pass, specRank,
				dataRank, altDimVar, exDimVar);
		log.debug("           specDims, dataDims = '{}', '{}'", specDims, dataDims);

		if (!pass) {
			if (exDimVar) {
				validationResult.addError("variable: " + dataVar.getShortName() + ": Definitions differ"
						+ "\n\tSpecification dimensions = '" + specDims + " (+ extra-dimensions)'"
						+ "\n\tData File dimensions     = '" + dataDims + "'");

				log.info("format error: '{}' dimensions mismatch (extra dimension)", dataVar.getShortName());

			} else {
				validationResult.addError("variable: " + dataVar.getShortName() + ": Definitions differ"
						+ "\n\tSpecification dimensions = '" + specDims + "'" + "\n\tData File dimensions     = '"
						+ dataDims + "'");

				log.info("format error: '{}' dimensions mismatch", dataVar.getShortName());
			}
		}

		return pass;
	} // ..end ckVarDims

	// ......................ckVarTypes...................
	/**
	 * Compares the data types of a Netcdf Variable and an ArgoVariable
	 * 
	 * @param dataVar the Netcdf variable object
	 * @param specVar the ArgoVariable object
	 * @return the boolean status. True - they are the same. False - they differ.
	 */

	private boolean ckVarTypes(Variable dataVar, ArgoVariable specVar) {
		// ..compare data type
		DataType specType = specVar.getType();
		DataType dataType = dataVar.getDataType();

		if (specType.equals(dataType)) {
			return true;

		} else {
			// ..there is an odd exception where a type may be either float or double
			// ..in the argo spec, that is encoded as DataType.OPAQUE
			// ..check for that exception

			if (specVar.getType() == DataType.OPAQUE && (dataType == DataType.FLOAT || dataType == DataType.DOUBLE)) {
				return true;
			}

			validationResult.addError("variable: " + dataVar.getShortName() + ": Definitions differ"
					+ "\n\tSpecification type = '" + specVar.getType().toString() + "'" + "\n\tData File type     = '"
					+ dataVar.getDataType().toString() + "'");
			log.info("format error: '{}' data type mismatch", dataVar.getShortName());

			return false;
		}
	} // ..end ckVarTypes

	private void checkGlobalAttributeValue(String dacName, String name, ArgoAttribute specAttr, Attribute dataAttr) {
		if (dataAttr.isString()) {
			String specValue = specAttr.getValue().toString();
			String dataValue = dataAttr.getStringValue();

			if (!(specValue.startsWith(ArgoFileSpecification.ATTR_IGNORE)
					|| specValue.startsWith(ArgoFileSpecification.ATTR_IGNORE_VALUE))) {
				// ..specAttr is not set for ignore

				if (!dataValue.equals(specValue)) {
					// ..data file attribute is not the same as the spec file attribute

					// Pattern regex = spec.getAttrRegex("", name);
					ArgoFileSpecification.AttrRegex regex = arFile.getFileSpec().getAttrRegex("", name);

					if (regex == null) {
						// ..no regex .. this is a format error

						String err = String.format("global attribute: %s: Definitions differ"
								+ "\n\tSpecification = '%s'" + "\n\tData File     = '%s'", name, specValue, dataValue);

						// formatErrors.add(err);

						// ################# TEMPORARY WARNING ################
						validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
						log.warn("TEMP WARNING: {}: {}: {}", dacName, arFile.getFileName(), err);

					} else {
						// ..regex defined ... does it match?

						if (!regex.pattern.matcher(dataValue).matches()) {
							String err = String.format("global attribute: %s: Definitions differ"
									+ "\n\tSpecification = '%s' (regex)" + "\n\tData File     = '%s'", name,
									regex.pattern, dataValue);
							// formatErrors.add("global attribute: "+

							// ################# TEMPORARY WARNING ################
							validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");

							log.warn("TEMP WARNING: {}: {}: {}", dacName, arFile.getFileName(), err);

						} else {
							if (regex.warn) {
								validationResult.addWarning("global attribute: " + name
										+ ": Accepted; not standard value" + "\n\tSpecification     = '" + specValue
										+ "'" + "\n\tException allowed = '" + regex.pattern + "' (regex)"
										+ "\n\tData File         = '" + dataValue + "'");
								log.warn("regex match (WARN): global attribute ':{} = '{}' matches '{}'", name,
										dataValue, regex.pattern);
							} else {
								log.warn("regex match (NO WARN): global attribute ':{} = '{}' matches '{}'", name,
										dataValue, regex.pattern);
							}
						}
					} // ..end if regex

				} // ..end attr.equals
			} // ..end if (ignore)

		} else {
			String err = String.format("global attribute: %s: not a \"string valued\" attribute", name);
			// formatErrors.add("global attribute: "+name+

			// ################# TEMPORARY WARNING ################
			validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
			log.warn("TEMP WARNING: {}: {}: {}", dacName, arFile.getFileName(), err);

		} // ..end if(dataAttr.isString)
	}

	private void checkGroupsCompleteness(HashSet<String> dataElement, HashSet<String> dataGroup) {
		for (String group : dataGroup) {
			log.debug("group with reported variable: '{}'", group);

			// ..at least one member of "group" was reported
			// ..check that all group memebers are reported

			Set<String> vReq = arFile.getFileSpec().groupMembers(group);
			HashSet<String> vMiss = new HashSet<String>();

			for (String member : vReq) {
				if (!dataElement.contains(member)) {
					// ..this member of the group was reported
					vMiss.add(member);
				}
			}

			if (vMiss.size() > 0) {
				// ..some variables in this group were missing but not all
				// ..format the list of required and reported variables
				StringBuilder req = new StringBuilder();
				for (String str : vReq) {
					req.append("'" + str + "' ");
				}

				// ..format the list of missing variables
				StringBuilder miss = new StringBuilder();
				for (String str : vMiss) {
					miss.append("'" + str + "' ");
				}

				// ..add to formatErrors
				validationResult.addError("Parameter group " + group + ": Variables are missing for this group"
						+ "\n\tRequired variables: " + req + "\n\tMissing variables:  " + miss);

				log.info("format error: option group '{}' variables missing from data file", group);
			}
		}
	}

	private void verifySpecVariablePresenceInData() {

		for (String name : arFile.getFileSpec().getSpecVariableNames()) {
			ArgoVariable specVar = arFile.getFileSpec().getVariable(name);
			Variable dataVar = arFile.getNcReader().findVariable(name);
			log.debug("spec var: {}", name);

			if (dataVar == null) {
				// ..variable in spec file is not in the data file

				if (arFile.getFileSpec().isOptional(name)) {
					// ..the variable is optional
					log.debug("optional variable not defined in data file: '{}'", name);

				} else {
					validationResult.addError("variable: " + name + ": not defined in data file");

					log.info("format error: variable not in data file: '{}'", name);
				}

				// ..........Step 4a: Check the spec attributes against the data file.......
			} else {
				// ..we are looking for attributes the spec says must exist
				// ..and checking the data variable to see if they do
				// ..- don't have to check values because the data var attr values were checked
				// above
				for (String attrName : specVar.getAttributeNames()) {
					checkSpecVarAttributePresenceInData(name, specVar, dataVar, attrName);
				}
			}
		} // end for (spec-Var-name)
	}

	private void checkSpecVarAttributePresenceInData(String name, ArgoVariable specVar, Variable dataVar,
			String attrName) {
		ArgoAttribute attr = specVar.getAttribute(attrName);
		ArgoAttribute.AttrHandling handling = attr.getHandling();

		if (handling == ArgoAttribute.AttrHandling.IGNORE_COMPLETELY
				|| handling == ArgoAttribute.AttrHandling.NOT_ALLOWED) {

			// ..attribute is allowed to be missing OR
			// ..attribute must NOT exist
			// ..- don't bother checking
			log.debug("optional attr: '" + name + ":" + attrName + "' - ignored");

		} else {
			Attribute dataAttr = dataVar.findAttribute(attrName);

			if (dataAttr == null) {
				// ..attribute in spec file is not in the data file

				validationResult.addError("attribute: '" + name + ":" + attrName + "' not defined in data file");

				log.info("format error: attribute not in data file: '{}:{}'", name, attrName);
			}
		}
	}

	private void verifySpecDimensionsPresenceInData() {

		for (ArgoDimension dim : arFile.getFileSpec().getDimensions()) {
			String name = dim.getName();
			Dimension dataDim = arFile.getNcReader().findDimension(name);
			log.debug("spec dim: {}", name);

			if (dataDim == null) {
				if (arFile.getFileSpec().isOptional(name)) {
					// ..dimension is optional
					log.debug("optional dimension '{}': not defined in data file - allowed", name);

				} else if (dim.isAlternateDimension()) {
					// ..this is an alt-dim --- it will never appear in the data file by name
					log.debug("alt-dim '{}': not defined in data file - expected", name);

				} else {
					// ..dimension in spec file is not in the data
					validationResult.addError("dimension: " + name + ": not defined in data file");

					log.info("format error: dimension not in data file: '{}'", name);
				}
			}
		} // end for (spec-Dim-name)
	}

	private void verifyFileVariables(HashSet<String> dataElement, HashSet<String> dataGroup) {

		for (Variable dataVar : arFile.getVarList()) {
			String name = dataVar.getShortName();

			dataElement.add(name);

			checkVariableAgainstSpec(name, dataVar);

			addElementToGroupIfDefinedInSpec(dataGroup, name);

		}
	}

	private void checkVariableAgainstSpec(String name, Variable dataVar) {

		if (log.isDebugEnabled()) {
			log.debug("data var: '{}'", name);
		}

		ArgoVariable specVar = arFile.getFileSpec().getVariable(name);

		if (specVar == null) {
			// ..data file variable is not in the specification
			validationResult.addError("variable: " + name + ": not defined in specification '"
					+ arFile.getFileSpec().getSpecName() + "'");

			log.info("format error: variable not in spec: '{}'", name);

		} else if (!ckVarTypes(dataVar, specVar)) {
			// ..data types don't match
			return;

		} else if (!ckVarDims(dataVar, specVar)) {
			// ..variable dimensions don't match
			return;
			// .....Step 2a: Compare the attributes for this variable......
		} else if (!ckVarAttr(dataVar, specVar)) {
			// ..variable attributes don't match
			return;
		}

	}

	// .........................................................
	// ........... verifyFileDimensions ..............
	// .........................................................
	private void verifyFileDimensions(HashSet<String> dataElement, HashSet<String> dataGroup) {

		List<Dimension> dimList = arFile.getNcReader().getDimensions();

		if (dimList == null || dimList.isEmpty()) {
			log.debug("no dimensions in this file");
			return;
		}

		for (Dimension dataDim : dimList) {
			String dimName = dataDim.getShortName();
			ArgoDimension specDim = arFile.getFileSpec().getDimension(dimName);
			dataElement.add(dimName);
			if (log.isDebugEnabled()) {
				log.debug("data dim: {} -- {}", dataDim, specDim);
			}

			if (specDim == null) {
				// ..dimension in data file is not in the spec
				// ..is it an "extra dimension"?
				handleExtraDimension(dataDim, dimName);

			} else {
				// ..dimension is in the spec, check its value
				validateDimensionLength(dataDim, specDim);
				// ..if a data dimension is in a group,
				// .. we have to check the rest of the group later
				addElementToGroupIfDefinedInSpec(dataGroup, dimName);
			}
		}
	}

	private void addElementToGroupIfDefinedInSpec(HashSet<String> dataGroup, String elementName) {
		String group = arFile.getFileSpec().inGroup(elementName);
		if (group != null) {
			dataGroup.add(group);
		}
	}

	private void validateDimensionLength(Dimension dataDim, ArgoDimension specDim) {
		int specValue = specDim.getValue();
		int dataValue = dataDim.getLength();
		if (specValue > 0 && specValue != dataValue) {
			// ..tValue > 0: dimension is not _unspecified_ or UNLIMITED
			// .. AND it doesn't have the same value -> error
			validationResult.addError("dimension: " + dataDim.getShortName() + ": Definitions differ"
					+ "\n\tSpecification = '" + specValue + "'" + "\n\tData File     = '" + dataValue + "'");

			log.info("format error: '{}' dimension value mismatch", dataDim.getShortName());
		}
	}

	private void handleExtraDimension(Dimension dataDim, String dimName) {
		ArgoDimension specDim;
		specDim = arFile.getFileSpec().addExtraDimension(dimName, dataDim.getLength());

		if (specDim == null) {
			// ..nope, not an allowed "extra dimension" -> error
			validationResult.addError(String.format("dimension: %s: not defined in specification '%s'", dimName,
					arFile.getFileSpec().getSpecName()));

			log.info("format error: '{}' not in spec", dimName);

		} else {
			log.debug("extra dimension: '{}'. value = {}", dimName, dataDim.getLength());
		}
	}

	// .........................................................
	// ............. validate DAC name argument.................
	// .........................................................

	private boolean checkDacNameArgument() {
		ArgoReferenceTable.DACS dac = null;
		// .......check arguments.......
		if (arFile.getDacName().trim().length() > 0) {
			for (ArgoReferenceTable.DACS d : ArgoReferenceTable.DACS.values()) {
				if (d.name.equals(arFile.getDacName())) {
					dac = d;
					this.arFile.setValidatedDac(dac);
					break;
				}
			}
			if (dac == null) {
				return false;
			}
		}
		return true;
	}

	// .........................................................
	// ........... getGdacFileName ..............
	// .........................................................
	/**
	 * Validates that the file name conforms to GDAC standards
	 *
	 */
	public String getGdacFileName() {
		log.debug("......getGdacFileName: start.....");

		StringBuilder gdacName = new StringBuilder(20);

		if (arFile.fileType() == FileType.METADATA) {
			String platform = arFile.readString("PLATFORM_NUMBER");

			gdacName.append(platform.trim());
			gdacName.append("_meta.nc");

			log.debug("meta-data file: platform, gdacName = '{}', '{}'", platform, gdacName);

		} else if (arFile.fileType() == FileType.PROFILE || arFile.fileType() == FileType.BIO_PROFILE) {
			String platform = arFile.readString("PLATFORM_NUMBER", 0);
			int cycle = arFile.readInt("CYCLE_NUMBER", 0);
			char direction = arFile.readString("DIRECTION", true).charAt(0);

			if (arFile.fileType() == FileType.BIO_PROFILE) {
				gdacName.append('B');
			}

			if (arFile.fileType() == FileType.PROFILE) {
				char data_mode = arFile.readString("DATA_MODE").charAt(0);

				log.debug("{} file: platform, cycle, direction, data_mode = " + "'{}', '{}', '{}', '{}'",
						arFile.fileType(), platform, cycle, direction, data_mode);

				if (data_mode == 'R' || data_mode == 'A') {
					gdacName.append('R');
				} else if (data_mode == 'D') {
					gdacName.append('D');
				} else { // ..pre-v3.1 files are not data-checked
					// ..do some rudimentary data checks here
					validationResult.addError("Could not determine file name: invalid DATA_MODE ='" + data_mode + "'");
					return null;
				}

			} else { // ..this is a BIO file
				String data_mode = arFile.readString("DATA_MODE");

				log.debug("{} file: platform, cycle, direction, data_mode = " + "'{}', '{}', '{}', '{}'",
						arFile.fileType(), platform, cycle, direction, data_mode);

				// ..a 'D' for any n_prof makes the whole file D-mode

				if (pDataMode.matcher(data_mode).matches()) {
					// ..pre-v3.1 files are not data-checked
					// ..do a rudimentary data checks here

					if (data_mode.indexOf('D') >= 0) {
						// ..any single 'D' means D-mode for file
						gdacName.append("D");

					} else {
						gdacName.append("R");
					}

				} else {
					validationResult.addError("Could not determine file name: invalid DATA_MODE = '" + data_mode + "'");
					return null;
				}
			}

			gdacName.append(platform.trim());
			gdacName.append("_");
			gdacName.append(cycleFmt.format(cycle));

			if (direction == 'D') {
				gdacName.append(direction);

			} else if (direction != 'A') { // ..pre-v3.1 files are not data-checked
				// ..do some rudimentary data checks here
				validationResult.addError("Could not determine file name: invalid DIRECTION ='" + direction + "'");
				return null;
			}

			gdacName.append(".nc");

			log.debug("{} file: gdacName = '{}'", arFile.fileType(), gdacName);

		} else if (arFile.fileType() == FileType.TECHNICAL) {
			String platform = arFile.readString("PLATFORM_NUMBER");

			gdacName.append(platform.trim());
			gdacName.append("_tech.nc");

			log.debug("tech-data file: platform, gdacName = '{}', '{}'", platform, gdacName);

		} else if (arFile.fileType() == FileType.TRAJECTORY || arFile.fileType() == FileType.BIO_TRAJECTORY) {
			String platform = arFile.readString("PLATFORM_NUMBER");
			String data_mode = arFile.readString("DATA_MODE", true);// ..true->incl NULLs

			log.debug("{} file: platform, data_mode = " + "'{}', '{}'", arFile.fileType(), platform, data_mode);

			gdacName.append(platform.trim());
			gdacName.append("_");

			if (arFile.fileType() == FileType.BIO_TRAJECTORY) {
				gdacName.append("B");
			}

			if (pDataMode.matcher(data_mode).matches()) {
				// ..pre-v3.1 files are not data-checked
				// ..do a rudimentary data checks here

				if (data_mode.indexOf('D') >= 0) {
					// ..any single 'D' means D-mode for file
					gdacName.append("Dtraj.nc");

				} else {
					gdacName.append("Rtraj.nc");
				}

			} else {
				validationResult.addError("Could not determine file name: invalid DATA_MODE = '" + data_mode + "'");
				return null;
			}

			log.debug("trajectory file: gdacName = '{}'", gdacName);

		} else {
			validationResult
					.addError("Could not determine file name: unexpected file type = '" + arFile.fileType() + "'");
			log.debug("unknown file type: '{}'", arFile.fileType());
			return null;
		}

		log.debug("......getGdacFileName: end.....");
		return gdacName.toString();
	}

	// ..............................................................
	// ........... validateGdacFileName ..............
	// ..............................................................
	/**
	 * Validates that the input file name conforms to GDAC standards
	 *
	 * @return true = file name conforms; false = file name non-conforming
	 */
	public boolean validateGdacFileName() {

		String expected = getGdacFileName();

		if (expected == null) {
			// ..error occured ... formatErrors set by getGdacFileName
			return false;
		}

		String name = arFile.getFileName();
		// String name = file.getName();

		if (!name.equals(expected)) {
			// ..actual file name and expected file name don't match

			if (name.endsWith("_traj.nc") && expected.endsWith("_Rtraj.nc")) {
				// ..have to deal with the special case of pre-v3.1 *_traj files
				if (arFile.fileVersion().startsWith("2.") || arFile.fileVersion().startsWith("3.0")) {
					String ex = expected.replaceFirst("_Rtraj", "_traj");

					if (name.equals(ex)) {
						log.debug("validateGdacFileName: PASSED using special *_traj rules: "
								+ "expected, name = '{}', '{}'", expected, name);
						return true;
					} else {
						log.debug("validateGdacFileName: FAILED using special *_traj rules: "
								+ "expected, name = '{}', '{}'", expected, name);
					}
				}
			}

			validationResult.addError("Inconsistent file name\n\tDAC file name       '" + name
					+ "'\n\tExpected file name according to file type, DIRECTION, DATA_MODE, CYCLE_NUMBER and PLATFORM_NUMBER : '"
					+ expected.toString() + "'");
			log.debug("validateGdacFileName: FAILED: expected, name = '{}', '{}'", expected, name);

			return false;
		}

		log.debug("validateGdacFileName: PASSED: expected, name = '{}', '{}'", expected, name);
		return true;
	}

	// ..........................................................
	// ........... validateStringNulls ..............
	// ..........................................................
	/**
	 * Checks for nulls in the "char" variables in an Argo file.
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	private void validateStringNulls() throws IOException {
		char nullChar = (char) 0;

		// .....Check all Strings -- no nulls allowed.....
		if (log.isDebugEnabled()) {
			log.debug(".....validateStrings.....");
		}

		for (Variable var : arFile.getVarList()) {
			if (var.getDataType() == DataType.CHAR) {
				ArrayChar ch;
				ch = (ArrayChar) var.read();

				Index ndx = ch.getIndex();
				int shape[] = var.getShape();
				int rank = shape.length;

				// log.debug(var.getShortName()+": shape:"+shape.length+" rank:"+rank);

				if (rank == 1) {
					// log.debug("rank 1: '"+ch.getString()+"'");
					for (int i = 0; i < shape[0]; i++) {
						ndx.set(i);
						if (ch.getChar(ndx) == nullChar) {
							validationResult.addWarning(var.getShortName() + ": NULL character at [" + (i + 1) + "]");
							log.warn("warning: {}[{}]: null character", var.getShortName(), i);
							break;
						}
					}
				} else if (rank == 2) {
					for (int i = 0; i < shape[0]; i++) {
						// log.debug("rank 2: '"+ch.getString(ndx.set(i,0))+"'");
						for (int j = 0; j < shape[1]; j++) {
							ndx.set(i, j);
							if (ch.getChar(ndx) == nullChar) {
								validationResult.addWarning(
										var.getShortName() + ": NULL character at [" + (i + 1) + "," + (j + 1) + "]");
								log.warn("warning: {}[{},{}]: null character", var.getShortName(), i, j);
								break;
							}
						}
					}
				} else if (rank == 3) {
					for (int i = 0; i < shape[0]; i++) {
						for (int j = 0; j < shape[1]; j++) {
							// log.debug("rank 3: '"+ch.getString(ndx.set(i,j,0))+"'");
							for (int k = 0; k < shape[2]; k++) {
								ndx.set(i, j, k);
								if (ch.getChar(ndx) == nullChar) {
									validationResult.addWarning(var.getShortName() + ": NULL character at [" + (i + 1)
											+ "," + (j + 1) + "," + (k + 1) + "]");
									log.warn("warning: {}[{},{},{}]: null character", var.getShortName(), i, j, k);
									break;
								}
							}
						}
					}
				} else if (rank == 4) {
					for (int i = 0; i < shape[0]; i++) {
						for (int j = 0; j < shape[1]; j++) {
							for (int k = 0; k < shape[2]; k++) {
								// log.debug("rank 4: '"+ch.getString(ndx.set(i,j,k,0))+"'");
								for (int l = 0; l < shape[3]; l++) {
									ndx.set(i, j, k, l);
									if (ch.getChar(ndx) == nullChar) {
										validationResult.addWarning(var.getShortName() + ": NULL character at ("
												+ (i + 1) + "," + (j + 1) + "," + (k + 1) + "," + (l + 1) + "]");
										log.warn("warning: {}[{},{},{},{}]: null character", var.getShortName(), i, j,
												k, l);
										break;
									}
								}
							}
						}
					}
				} else {
					throw new IOException("validateString cannot handle rank " + rank + " arrays");
				} // ..end if (rank)
			}
		}
	}// ..end validateStringNulls

	// .....................................................
	// ......... rudimentaryDateChecks .............
	// .....................................................
	/**
	 * Performs rudimentary date checks on a file. Essentially, just checks that
	 * DATE_CREATE and DATE_UPDATE are valid date strings.
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 */
	public void rudimentaryDateChecks() {
		log.debug(".....rudimentaryDateChecks: start.....");

		// ..creation date check
		// ..just check for valid date

		String creation = arFile.readString("DATE_CREATION");

		if (creation.trim().length() <= 0) {
			validationResult.addError("DATE_CREATION: Not set");
			log.info("format error: DATE_CREATION not set");

		} else {
			Date dateCreation = ArgoDate.get(creation);

			if (dateCreation == null) {
				validationResult.addError("DATE_CREATION: '" + creation + "': Invalid date");
				log.info("format error: bad DATE_CREATION: '{}'", creation);
			}
		}

		// ..update date check
		// ..just check for valid date

		String update = arFile.readString("DATE_UPDATE");

		if (update.trim().length() <= 0) {
			validationResult.addError("DATE_UPDATE: Not set");
			log.info("format error: DATE_UPDATE not set");

		} else {
			Date dateUpdate = ArgoDate.get(update);

			if (dateUpdate == null) {
				validationResult.addError("DATE_UPDATE: '" + update + "': Invalid date");
				log.info("format error: bad DATE_UPDATE: '{}'", update);
			}
		}
		log.debug(".....rudimentaryDateChecks: end.....");
	}

	// .............................................
	// convenience "check" methods
	// .............................................

	/**
	 * Determines if a float value is the specified FillValue Performs a "real
	 * value" comparison to a resolution of 1.e-5 (not an exact equality).
	 * 
	 * @param _FillValue for the parameter
	 * @param data       value
	 * @return true = value is FillValue; false = value is not FillValue
	 */
	public static boolean is_FillValue(float fillValue, float value) {
		float diff = Math.abs(1.f - (value / fillValue));
		return diff < 1.0e-8f;
	}

	/**
	 * Determines if a double value is the specified FillValue Performs a "real
	 * value" comparison to a resolution of 1.e-5 (not an exact equality).
	 * 
	 * @param _FillValue for the parameter
	 * @param data       value
	 * @return true = value is FillValue; false = value is not FillValue
	 */
	public static boolean is_FillValue(double fillValue, double value) {
		double diff = Math.abs(1.d - (value / fillValue));
		return diff < 1.0e-8d;
	}

	/**
	 * Determines if a float value is a "99,999.0" missing value. Performs a "real
	 * value" comparison to a resolution of 1.e-5 (not an exact equality). <br>
	 * <br>
	 * NOTE: This is required because we've run into examples where "bad values" of
	 * a parameter have been larger than the FillValue so simple
	 * greater-than/less-than tests fails
	 * 
	 * @param data value
	 * @return true = value is FillValue; false = value is not FillValue
	 */
	public static boolean is_99_999_FillValue(float value) {
		float diff = Math.abs(value - 99999.f);
		return diff < 0.00001f;
	}

	/**
	 * Determines if a double value is a "99,999.0" missing value. Performs a "real
	 * value" comparison to a resolution of 1.e-5 (not an exact equality). <br>
	 * <br>
	 * NOTE: This is required because we've run into examples where "bad values" of
	 * a parameter have been larger than the FillValue so simple
	 * greater-than/less-than tests fails
	 * 
	 * @param data value
	 * @return true = value is FillValue; false = value is not FillValue
	 */
	public static boolean is_99_999_FillValue(double value) {
		double diff = Math.abs(value - 99999.d);
		return diff < 0.00001d;
	}

	/**
	 * Determines if a double value is a "999,999.0" missing value. Performs a "real
	 * value" comparison to a resolution of 1.e-5 (not an exact equality).
	 * 
	 * @param data value
	 * @return true = value is FillValue; false = value is not FillValue
	 */
	public static boolean is_999_999_FillValue(double value) {
		double diff = Math.abs(value - 999999.d);
		return diff < 0.00001d;
	}

	// ............ end "check" methods ..............

	/**
	 * Convenience method to add to String list for "pretty printing".
	 *
	 * @param list the StringBuilder list
	 * @param add  the String to add
	 */
	protected void addToList(StringBuilder list, String add) {
		if (list.length() == 0) {
			list.append("'" + add + "'");
		} else {
			list.append(", '" + add + "'");
		}
	}

	/**
	 * Convenience method to add to String list for "pretty printing".
	 *
	 * @param list the StringBuilder list
	 * @param add  the String to add
	 */
	protected void addToList(StringBuilder list, int add) {
		if (list.length() == 0) {
			list.append(add);
		} else {
			list.append(", " + add);
		}
	}

//.........................................
//  ACCESSORS
//.........................................

	public ValidationResult getValidationResult() {
		return validationResult;
	}

	public void setValidationResult(ValidationResult validationResult) {
		this.validationResult = validationResult;
	}

}
