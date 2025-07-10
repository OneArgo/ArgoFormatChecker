package fr.coriolis.checker.filetypes;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.filetypes.ArgoDataFile.FileType;
import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.specs.ArgoReferenceTable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Variable;

/**
 * Extends ArgoDataFile with features specific to an Argo Trajectory file.
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
 *          https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoTrajectoryFile.java
 *          $
 * @version $Id: ArgoTrajectoryFile.java 1269 2021-06-14 20:34:45Z ignaszewski $
 */

public class ArgoTrajectoryFileValidator extends ArgoFileValidator {
	// .......................................
	// VARIABLES
	// .......................................

	// ..standard i/o shortcuts
	private static PrintStream stderr = new PrintStream(System.err);
	private static final Logger log = LogManager.getLogger("ArgoTrajectoryFileValidator");

	private final static int fillCycNum = 99999;

	private final static String goodJuldQC = new String("01258");

	// .......................................
	// CONSTRUCTORS
	// .......................................

	protected ArgoTrajectoryFileValidator(ArgoDataFile arFile) throws IOException {
		super(arFile);
	}

//	protected ArgoTrajectoryFileValidator(String specDir, String version) {
//		// super(specDir, FileType.TRAJECTORY, version);
//	}

	// ..........................................
	// METHODS
	// ..........................................

//	/** Retrieve the NetcdfFileWriter reference */
//	public NetcdfFileWriter getNetcdfFileWriter() {
//		return ncWriter;
//	}

	/**
	 * Retrieve a list of variables in the associated file
	 */
//	public List<String> getVariableNames() {
//		LinkedList<String> varNames = new LinkedList<String>();
//
//		if (ncReader != null) {
//			for (Variable var : ncReader.getVariables()) {
//				varNames.add(var.getShortName());
//			}
//
//		} else if (ncWriter != null) {
//			for (Variable var : ncWriter.getNetcdfFile().getVariables()) {
//				varNames.add(var.getShortName());
//			}
//
//		} else {
//			varNames = null;
//		}
//
//		return varNames;
//	}

	/**
	 * Convenience method to add to String list for "pretty printing".
	 *
	 * @param list the StringBuilder list
	 * @param add  the String to add
	 */
//	@Override
//	private void addToList(StringBuilder list, String add) {
//		if (list.length() == 0) {
//			list.append("'" + add + "'");
//		} else {
//			list.append(", '" + add + "'");
//		}
//	}

	/**
	 * Convenience method to add to String list for "pretty printing".
	 *
	 * @param list the StringBuilder list
	 * @param add  the String to add
	 */

	/**
	 * Creates a new Argo trajectory file. The "template" for the file is the
	 * indicated CDL specification file.
	 *
	 * @param fileName   The name of the output file
	 * @param specDir    Path to the specification directory
	 * @param version    The version string of the spec file to use as a template
	 * @param N_PROF     N_PROF dimension of the new file
	 * @param N_PARAM    N_PARAM dimension of the new file
	 * @param N_LEVELS   N_LEVELS dimension of the new file
	 * @param N_CALIB    N_CALIB dimension of the new file
	 * @param parameters The parameter names that will be in the new file
	 * @throws IOException           If problems creating the file are encountered
	 * @throws NumberFormatException If problems converting certain values encoded
	 *                               in the CDL attributes to number are
	 *                               encountered.
	 */
//	public static ArgoTrajectoryFileValidator createNew(String fileName, String specDir, String version, int N_PARAM,
//			int N_CYCLE, int N_HISTORY, Set<String> parameters) throws IOException, NumberFormatException {
//		log.debug(".....createNew: start.....");
//
//		ArgoTrajectoryFileValidator arFile = new ArgoTrajectoryFileValidator();
//
//		// ..create the template specification
//		arFile.spec = ArgoDataFile.openSpecification(false, specDir, ArgoDataFile.FileType.TRAJECTORY, version);
//		if (arFile.spec == null) {
//			return null;
//		}
//
//		arFile.fileType = ArgoDataFile.FileType.TRAJECTORY;
//
//		// ..remove any parameters that are not in this specification
//
//		Iterator<String> i = parameters.iterator();
//		while (i.hasNext()) {
//			String p = i.next();
//			if (!arFile.spec.isPhysicalParamName(p)) {
//				i.remove();
//				// arFile.validationResult.addWarning("Parameter: '"+p+
//				// "' not in specification. Removed.");
//				log.debug("requested parameter '{}' not in spec: removed");
//			}
//		}
//
//		N_PARAM = parameters.size();
//
//		// ..create new file
//
//		arFile.ncWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fileName);
//		arFile.ncWriter.setFill(true);
//
//		// ..fill in object variables
//		arFile.file = null; // ..don't know why I need this ..... yet
//		arFile.fileType = ArgoDataFile.FileType.TRAJECTORY;
//		arFile.format_version = version;
//		arFile.ncFileName = fileName;
//		// arFile.spec is filled in by openSpec...
//
//		// .....add globabl attributes.....
//
//		for (ArgoAttribute a : arFile.spec.getGlobalAttributes()) {
//			String name = a.getName();
//			String value = (String) a.getValue();
//
//			if (value.matches(ArgoFileSpecification.ATTR_SPECIAL_REGEX)) {
//				// ..the definition starts with one of the special ATTR_IGNORE codes
//
//				if (value.length() > ArgoFileSpecification.ATTR_SPECIAL_LENGTH) {
//					// ..there is more than just the code on the line
//					// ..defining the default value, use it
//
//					value = value.substring(ArgoFileSpecification.ATTR_SPECIAL_LENGTH);
//					log.debug("global attribute with special code: '{}'", value);
//
//				} else {
//					// ..nothing but the code on the line
//					// ..ignore the attribute
//					value = null;
//				}
//			}
//
//			if (name.equals("history")) {
//				value = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(new Date()).toString();
//				log.debug("history global attribute: '{}'", value);
//			}
//
//			if (value != null) {
//				arFile.ncWriter.addGroupAttribute(null, new Attribute(name, value));
//				log.debug("add global attribute: '{}' = '{}'", name, value);
//			}
//		}
//
//		// .........add Dimensions...............
//		// ..don't allow <= 0 dimensions
//		if (N_PARAM <= 0) {
//			N_PARAM = 1;
//		}
//		if (N_CYCLE <= 0) {
//			N_CYCLE = 1;
//		}
//		if (N_HISTORY <= 0) {
//			N_HISTORY = 1;
//		}
//
//		for (ArgoDimension d : arFile.spec.getDimensions()) {
//			String name = d.getName();
//			int value = d.getValue();
//
//			if (name.equals("N_PARAM")) {
//				value = N_PARAM;
//			} else if (name.equals("N_CYCLE")) {
//				value = N_CYCLE;
//			} else if (name.equals("N_HISTORY")) {
//				value = N_HISTORY;
//			}
//
//			if (name.equals("N_MEASUREMENT")) {
//				arFile.ncWriter.addUnlimitedDimension(name);
//			} else {
//				arFile.ncWriter.addDimension(null, name, value);
//			}
//
//			log.debug("add dimension: '{}' = '{}'", name, value);
//		}
//
//		// .........add Variables...............
//
//		// ......ordered list.....
//		// ..this bit of code arranges the variables in the "expected order"
//		// ..this is technically completely unnecessary
//		// ..the ordering of the variables in the file should not matter
//		// ..however, at least one user is complaining about the current files
//		// ..and I am guessing that variable ordering is the problem.
//
//		// ..the way the "spec" files are parsed, the PARAM variables end up
//		// ..at the end of the variables list
//		// ..so I am trying to distribute the variables in the "expected order"
//
//		// ..good coding by users would eliminate the need for this
//
//		// ..the idea is to create an ordered list of variable names that are
//		// ..then used in the next section
//
//		log.debug("...build ordered list of variables...");
//
//		ArrayList<ArgoVariable> orderedList = new ArrayList<ArgoVariable>(200);
//
//		for (ArgoVariable v : arFile.spec.getVariables()) {
//			String name = v.getName();
//
//			if (v.isParamVar()) {
//				// ..when we get to the PARAM variables, we are done
//				// ..they are always at the end of the list and we handle
//				// ..them separately in the if-blocks below
//				break;
//			}
//
//			orderedList.add(v);
//			log.debug("add {}", name);
//
//			// ..insert the <PARAM> variables after MEASUREMENT_CODE
//
//			if (name.equals("MEASUREMENT_CODE")) {
//				log.debug("insert <PARAM> here");
//				for (ArgoVariable w : arFile.spec.getVariables()) {
//					if (w.isParamVar()) {
//						log.debug("isParamVar");
//						orderedList.add(w);
//						log.debug("add {}", w.getName());
//					}
//				}
//			}
//		}
//
//		log.debug("...ordered list complete...");
//
//		// ....end ordered list....
//
//		Boolean keep;
//		// ...if we didn't need the list to be ordered...
//		// for (ArgoVariable v : arFile.spec.getVariables()) {
//
//		for (ArgoVariable v : orderedList) {
//			String name = v.getName();
//			String prm = v.getParamName();
//
//			if (prm != null) {
//				// ..this is a physical parameter variable
//				// ..is it's parameter name in the parameter list
//
//				if (parameters.contains(prm)) {
//					keep = true;
//				} else {
//					keep = false;
//					log.debug("skip variable: '{}'", name);
//				}
//
//			} else {
//				// ..not a physical parameter, so keep it
//				keep = true;
//			}
//
//			if (keep) {
//				DataType type = v.getType();
//				String dims = v.getDimensionsString();
//
//				Variable var = arFile.ncWriter.addVariable(null, name, type, dims);
//				log.debug("add variable: '{}': '{}' '{}'", name, type, dims);
//
//				// ..add attributes for this variable
//				for (ArgoAttribute a : v.getAttributes()) {
//					String aname = a.getName();
//
//					if (a.isNumeric()) {
//						var.addAttribute(new Attribute(aname, (Number) a.getValue()));
//						log.debug("add attribute: '{}:{}' = '{}'", name, aname, a.getValue());
//
//					} else if (a.isString()) {
//						String value = (String) a.getValue();
//						Object def = null;
//
//						if (value.matches(ArgoFileSpecification.ATTR_SPECIAL_REGEX)) {
//							// ..the definition starts with one of the special ATTR_IGNORE codes
//
//							if (value.length() > ArgoFileSpecification.ATTR_SPECIAL_LENGTH) {
//								// ..there is more than just the code on the line
//								// ..defining the default value, use it
//
//								String val = value.substring(ArgoFileSpecification.ATTR_SPECIAL_LENGTH);
//								log.debug("attribute with special code: '{}' '{}'", value, val);
//
//								if (val.startsWith("numeric:")) {
//									// ..this should be encoded as a number
//
//									val = val.substring("numeric:".length());
//									log.debug("...number: '{}'", val);
//
//									try {
//										switch (v.getType()) {
//										case DOUBLE:
//											def = Double.valueOf(val);
//											var.addAttribute(new Attribute(aname, (Number) def));
//											break;
//										case FLOAT:
//											def = Float.valueOf(val);
//											var.addAttribute(new Attribute(aname, (Number) def));
//											break;
//										case INT:
//											def = Integer.valueOf(val);
//											var.addAttribute(new Attribute(aname, (Number) def));
//											break;
//										case SHORT:
//											def = Short.valueOf(val);
//											var.addAttribute(new Attribute(aname, (Number) def));
//											break;
//										default: // ..assume it is a string
//											def = val;
//											var.addAttribute(new Attribute(aname, (String) def));
//										}
//									} catch (NumberFormatException e) {
//										throw new NumberFormatException(
//												"Attribute " + name + ":" + aname + ": Unable to convert to number");
//									}
//
//								} else {
//									// ..default value is a string
//									var.addAttribute(new Attribute(aname, val));
//								}
//
//								// } else {
//								// ..nothing but the special code on the line
//								// ..ignore the attribute
//								// def = null;
//							}
//
//						} else {
//							// ..not a special value -- insert as is
//							var.addAttribute(new Attribute(aname, value));
//							log.debug("add attribute: '{}:{}' = '{}'", name, aname, value);
//						}
//
//					} else {
//						log.error("attribute not Number or String: '{}:{}'", name, aname);
//						continue;
//					}
//
//				}
//			}
//		}
//
//		// .....create the file -- end "define mode"
//
//		arFile.ncWriter.create();
//		arFile.ncWriter.close();
//
//		log.debug(".....createNew: end.....");
//		return arFile;
//	}
//
//	/**
//	 * Opens an existing file without opening the <i>specification</i>
//	 *
//	 * @param inFile the string name of the file to open
//	 * @return the file object reference. Returns null if the file is not opened
//	 *         successfully. (ArgoTrajectoryFile.getMessage() will return the reason
//	 *         for the failure to open.)
//	 * @throws IOException If an I/O error occurs
//	 */
//	public static ArgoTrajectoryFileValidator open(String inFile) throws IOException {
//		ArgoDataFile arFile = ArgoDataFile.open(inFile);
//		if (!(arFile instanceof ArgoTrajectoryFileValidator)) {
//			ValidationResult.lastMessage = "ERROR: '" + inFile + "' not an Argo TRAJECTORY file";
//			return null;
//		}
//
//		return (ArgoTrajectoryFileValidator) arFile;
//	}
//
//	/**
//	 * Opens an existing file and the associated <i>Argo specification</i>).
//	 *
//	 * @param inFile   the string name of the file to open
//	 * @param specDir  the string name of the directory containing the format
//	 *                 specification files
//	 * @param fullSpec true = open the full specification; false = open the template
//	 *                 specification
//	 * @return the file object reference. Returns null if the file is not opened
//	 *         successfully. (ArgoTrajectoryFile.getMessage() will return the reason
//	 *         for the failure to open.)
//	 * @throws IOException If an I/O error occurs
//	 */
//	public static ArgoTrajectoryFileValidator open(String inFile, String specDir, boolean fullSpec) throws IOException {
//		ArgoDataFile arFile = ArgoDataFile.open(inFile, specDir, fullSpec);
//		if (!(arFile instanceof ArgoTrajectoryFileValidator)) {
//			ValidationResult.lastMessage = "ERROR: '" + inFile + "' not an Argo TRAJECTORY file";
//			return null;
//		}
//
//		return (ArgoTrajectoryFileValidator) arFile;
//	}

	/**
	 * Validates the data in the trajectory file. This is a driver routine that
	 * performs all types of validations (see other validate* routines).
	 *
	 * <p>
	 * NOTE: This routine (and all other validate* routines) are designed to handle
	 * both core-profile and bio-profile files. There are differnces in the
	 * validation checks that are noted where they occur.
	 * 
	 * <p>
	 * Performs the following:
	 * <ul>
	 * <li>validateStringNulls (if ckNulls = true)
	 * <li>validateMetaData --- stop checks if not passed
	 * <li>validateDates
	 * <li>validateTrajectoryParameters
	 * <li>validateDataMode
	 * <li>validateCycleNumber --- stop checks if not passed
	 * <li>validateMC_and_JULD
	 * <li>validateNCycleJuld
	 * <li>validatePosition
	 * <li>validateParams
	 * <li>validateNCycle
	 * </ul>
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
	public boolean validateData(String dacName, boolean ckNulls) throws IOException {
		boolean basicsChecks = super.basicDataValidation(ckNulls);
		if (!basicsChecks) {
			return false;
		}

		// ............Determine number of profiles............

		int nCycle = arFile.getDimensionLength("N_CYCLE");
		int nHistory = arFile.getDimensionLength("N_HISTORY");
		int nMeasure = arFile.getDimensionLength("N_MEASUREMENT");
		int nParam = arFile.getDimensionLength("N_PARAM");

		boolean pass;

		if (log.isDebugEnabled()) {
			log.debug("N_CYCLE:       {}", nCycle);
			log.debug("N_HISTORY:     {}", nHistory);
			log.debug("N_MEASUREMENT: {}", nMeasure);
			log.debug("N_PARAM:       {}", nParam);
		}

		pass = validateMetaData(arFile.getValidatedDac());
		if (!pass) {
			return true;
		}

		validateDates(nParam, nHistory);

		ArrayList<String> paramList = validateTrajectoryParameters(nParam);

		char[] mode_nCycle = new char[nCycle];
		char overallDM = validateDataMode(mode_nCycle);

		// ....validate the CYCLE_NUMBER / CYCLE_NUMBER_INDEX variable groups....
		// ..these are too important to all the other checks. If fail, checking is
		// halted

		HashMap<Integer, Integer> CycNumIndex_cycle2index = new HashMap<Integer, Integer>(200);

		char[] mode_nMeasure = new char[nMeasure];

		pass = validateCycleNumber(nMeasure, nCycle, overallDM, mode_nCycle, CycNumIndex_cycle2index, mode_nMeasure);

		if (!pass) {
			// ..the mapping from cyc_num to cyc_num_index is too important to continue w/o
			// will be.. validationResult.addError("CYCLE_NUMBER errors exist. Remaining
			// checks
			// skipped");
			validationResult.addWarning("CYCLE_NUMBER errors exist. Remaining checks skipped");
			return true;
		}

		// .....validate the measurement_code and JULD[N_MEASUREMENT] variables....
		// ..sends back a bunch of info for later checks

		Final_NMeasurement_Variables[] finalNMVars = new Final_NMeasurement_Variables[nMeasure];

		pass = validateMC_and_JULD(nMeasure, mode_nMeasure, finalNMVars);

		// .....validate the JULD_*[N_CYCLE] variables.......

		validateNCycleJuld(nMeasure, nCycle, CycNumIndex_cycle2index, finalNMVars);

		// ......validate the rest of the stuff.......

		validatePosition(nMeasure);

		validateParams(nMeasure, mode_nMeasure, paramList);

		validateNCycle(nCycle, mode_nCycle);

		log.debug(".....validate: end.....");

		return true;
	}// ..end validate

	/**
	 * Validates the CYCLE_NUMBER variables in the trajectory file and returns two
	 * variables for later processing: <br>
	 * 1) a map of cycle-to-index for the [N_CYCLE] variables <br>
	 * 2) the data_mode of the [N_MEASUREMENT] variables
	 *
	 * Bio-trajectory exceptions: does not conatin CYCLE_NUMBER_ADJUSTED does not
	 * conatin CYCLE_NUMBER_INDEX_ADJUSTED
	 * 
	 * Assumes:
	 * <ul>
	 * <li>DATA_MODE has been read and validated (and passed into routine)
	 * <li>An "overall data_mode" has been determined, which is:
	 * <ul>
	 * <li>D if any data_mode is 'D'
	 * <li>R otherwise
	 * </ul>
	 * <li>A map of cycle number to data index has been built for the "final
	 * CYCLE_NUMBER_INDEX" (*_ADJUSTED if set; itself if not)
	 * </ul>
	 *
	 * Variable checks:
	 * 
	 * @param nMeasure                N_MEASUREMENT value
	 * @param nCycle                  N_CYCLE value
	 * @param overallDM               Overall data mode of the file (see above)
	 * @param data_mode               char array of DATA_MODE values
	 * @param CycNumIndex_cycle2index (OUTPUT!) Map of cycle_number to data index
	 *                                for CYCLE_NUMBER_INDEX
	 * @param mode_nMeasure           char array of the data_mode values mapped onto
	 *                                [N_MEASUREMENT]
	 * @throws IOException If an I/O error occurs
	 */

	public boolean validateCycleNumber(int nMeasure, int nCycle, char overallDM, char[] data_mode,
			HashMap<Integer, Integer> CycNumIndex_cycle2index, char[] mode_nMeasure) throws IOException {
		log.debug(".....validateCycleNumber: start.....");
		log.debug("nMeasure, nCycle = {}, {}", nMeasure, nCycle);

		boolean pass = true;

		// .....cycle_number_index and cycle_number_index_adj..........
		log.debug("cyc_num_ind-check: start");

		int[] cycleIndex = arFile.readIntArr("CYCLE_NUMBER_INDEX");
		int[] cycleIndexAdj = null;
		boolean core = false;

		if (arFile.fileType() == FileType.TRAJECTORY) {
			// ..implies this is a core-file NOT a bio-file
			core = true;
			cycleIndexAdj = arFile.readIntArr("CYCLE_NUMBER_INDEX_ADJUSTED");
		}

		ErrorTracker adjSet = new ErrorTracker();
		ErrorTracker dupIndex = new ErrorTracker();
		ErrorTracker invAdjCyc = new ErrorTracker();
		ErrorTracker invCyc = new ErrorTracker();
		ErrorTracker missDCyc = new ErrorTracker();
		ErrorTracker missRCyc = new ErrorTracker();

		for (int n = 0; n < nCycle; n++) {

			int num = cycleIndex[n];
			int numAdj = fillCycNum;

			if (core) {
				numAdj = cycleIndexAdj[n];
			}

			if (num < 0) {
				invCyc.increment(n, "invalid CYCLE_NUMBER_INDEX");
			}

			if (core && numAdj < 0) {
				invAdjCyc.increment(n, "invalid CYCLE_NUMBER_INDEX_ADJUSTED");
			}

			if (overallDM == 'R') {

				// .....file only contains R-mode data.......

				if (num == fillCycNum) {
					// ..cycle_number_index missing in r-mode <--- error
					missRCyc.increment(n, "r-mode: CYCLE_NUMBER_INDEX is missing");

				}

				if (core && numAdj != fillCycNum) {
					// ..cycle_number_index_adjusted_index set in r-mode <---- error
					adjSet.increment(n, "r-mode: CYCLE_NUMBER_INDEX_ADJUSTED is set");
				}

			} else {
				// .....file contains at least one D-mode cycle.....

				if (data_mode[n] != 'D') {
					// ...r/a-mode cycle

					if (num == fillCycNum) {
						// ..missing in r-mode <--- this is an error
						missRCyc.increment(n, "r-mode: CYCLE_NUMBER_INDEX is missing");
					}
					if (core && numAdj != fillCycNum) {
						// ..numAdj set in r-mode <--- this is an error
						adjSet.increment(n, "r-mode: CYCLE_NUMBER_INDEX_ADJUSTED is set");
					}

				} else {
					// ..d-mode cycle

					if (core && numAdj == fillCycNum) {
						// ..*_adjusted is missing too this is an error
						missDCyc.increment(n, "d-mode:CYCLE_NUMBER_INDEX_ADJUSTED is missing");
					}
				}
			} // ..endif (overallDM)

			// ..check for duplicates --- build the lookup table
			int finalNum = -1;
			if (numAdj != fillCycNum) {
				finalNum = numAdj;
			} else if (num != fillCycNum) {
				finalNum = num;
			}

			if (finalNum > -1) {
				Integer ndx = CycNumIndex_cycle2index.get(finalNum);

				if (ndx != null) {
					dupIndex.increment(n);

				} else {
					log.debug("(temp) CycNumIndex_cycle2index.put {} {}", finalNum, n);
					CycNumIndex_cycle2index.put(Integer.valueOf(finalNum), Integer.valueOf(n));
				}
			}
		}

		log.debug("cycle_number_index: adjSet, dupIndex, invAdjCyc, invCyc = {}, {}, {}, {}", adjSet.counter,
				dupIndex.counter, invAdjCyc.counter, invCyc.counter);
		log.debug("cycle_number_index: missDCyc, missRCyc = {}, {}", missDCyc.counter, missRCyc);

		if (adjSet.counter > 0) {
			pass = false;
			adjSet.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_INDEX_ADJUSTED: Set in real-time at", "cycles");
		}
		if (dupIndex.counter > 0) {
			pass = false;
			dupIndex.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_INDEX / CYCLE_NUMBER_ADJUSTED_INDEX:" + " Duplicate cycle number at ", "cycles");
		}
		if (invAdjCyc.counter > 0) {
			pass = false;
			invAdjCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_ADJUSTED_INDEX: Invalid cycle number at", "cycles");
		}
		if (invCyc.counter > 0) {
			pass = false;
			invCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_INDEX: Invalid cycle number at", "cycles");
		}
		if (missDCyc.counter > 0) {
			pass = false;
			missDCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_ADJUSTED_INDEX: Missing in delayed-mode at", "cycles");
		}
		if (missRCyc.counter > 0) {
			pass = false;

			missRCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_INDEX: Missing in real-time at", "cycles");
		}

		// .....cycle_number and cycle_number_adj..........
		log.debug("cyc_num_ind-check: end");
		log.debug("cyc_num: start");

		int[] cycle = arFile.readIntArr("CYCLE_NUMBER");
		int[] cycleAdj = null;

		if (core) {
			cycleAdj = arFile.readIntArr("CYCLE_NUMBER_ADJUSTED");
		}

		adjSet.reset();
		invAdjCyc.reset();
		invCyc.reset();
		missDCyc.reset();
		missRCyc.reset();

		ErrorTracker invAdjLaunch = new ErrorTracker();
		ErrorTracker invLaunch = new ErrorTracker();

		for (int n = 0; n < nMeasure; n++) {
			int num = cycle[n];
			int numAdj = fillCycNum;

			if (core) {
				numAdj = cycleAdj[n];
			}

			// ..check "launch cycle"

			if (num < 0) {
				// ..cycle_num == -1 is allowed in index 0
				// ..anything else is an error

				if (num == -1) {
					if (n != 0) {
						invLaunch.increment(n);
					}

				} else {
					invCyc.increment(n);
				}
			}
			if (core && numAdj < 0) {
				// ..cycle_num_adjusted == -1 is allowed in index 0
				// ..anything else is an error

				if (numAdj == -1) {
					if (n != 0) {
						invAdjLaunch.increment(n);
					}

				} else {
					invAdjCyc.increment(n);
				}
			}

			// ..check value based on file type

			if (overallDM == 'R') {

				// .....file only contains R-mode data.......

				if (num == fillCycNum) {
					// ..cycle_number is missing in r-mode <--- error

					missRCyc.increment(n);
				}

				if (core && numAdj != fillCycNum) {
					// ..cycle_number_index_adjusted_index set in r-mode <---- error

					adjSet.increment(n);
				}

			} else {
				// .....file contains at least one D-mode cycle.....

				// ..check for duplicates --- build the lookup table
				int finalNum = -1;
				if (numAdj != fillCycNum) {
					finalNum = numAdj;
				} else if (num != fillCycNum) {
					finalNum = num;
				}

				// ..need to know the data_mode of this cycle
				Integer ndx = CycNumIndex_cycle2index.get(finalNum);

				boolean isD = false;
				char m = 'X';

				if (ndx != null) {
					m = data_mode[ndx.intValue()];

					if (m == 'D') {
						isD = true;
					}
				}

				// ..check values based on mode

				if (!isD) {
					// ..r-mode cycle

					if (num == fillCycNum) {
						// ..cycle_number is missing in r-mode <--- error
						missRCyc.increment(n);
					}
					if (core && numAdj != fillCycNum && numAdj > -1) {
						// ..numAdj set in r-mode <--- this is an error
						adjSet.increment(n);
					}

				} else {
					// ..d-mode cycle

					if (core && numAdj == fillCycNum) {
						// ..*_adjusted is missing too this is an error
						missDCyc.increment(n);
					}
				}
			} // ..endif (overallDM)
		} // ..end for (n < nMeasure)

		log.debug("cycle_number_index: adjSet, invAdjCyc, invCyc = {}, {}, {}", adjSet.counter, dupIndex.counter,
				invAdjCyc.counter, invCyc.counter);
		log.debug("cycle_number_index: invLaunch, missDCyc, missRCyc = {}, {}, {}", invLaunch.counter, missDCyc.counter,
				missRCyc.counter);

		if (adjSet.counter > 0) {
			pass = false;
			adjSet.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_ADJUSTED: Set in real-time at", "measurements");
		}
		if (invAdjCyc.counter > 0) {
			pass = false;
			invAdjCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_ADJUSTED: Invalid cycle number at", "measurments");
		}
		if (invCyc.counter > 0) {
			pass = false;
			invCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER: Invalid cycle number at", "measurements");
		}
		if (invAdjLaunch.counter > 0) {
			pass = false;
			invAdjLaunch.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_ADJUSTED: Cycle -1 not in first index at", "measurements");
		}
		if (invLaunch.counter > 0) {
			pass = false;
			invLaunch.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER: Cycle -1 not in first index at", "measurements");
		}
		if (missDCyc.counter > 0) {
			pass = false;
			missDCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER_ADJUSTED: Missing in delayed-mode at", "measurements");
		}
		if (missRCyc.counter > 0) {
			pass = false;
			missRCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"CYCLE_NUMBER: Missing in real-time at ", "cycles");
		}

		log.debug("cyc_num: end");

		// ..............check that all cycle numbers in .................
		// ..............CYCLE_NUMBER are in _INDEX .................
		// ..build the mode[nMeasurement] array
		log.debug("cyc_num-in-cyc_num_ind: start");

		HashSet<Integer> cycNumSet = new HashSet<Integer>(200);

		int finalNum;
		int prevNum = -999;
		char prevMode = 'X';
		ErrorTracker missCyc = new ErrorTracker();

		for (int n = 0; n < nMeasure; n++) {
			int num = cycle[n];
			int numAdj = fillCycNum;

			if (core) {
				numAdj = cycleAdj[n];
			}

			if (num == -1 || numAdj == -1) {
				// ..the launch cycle is NOT in cycle_number_index
				mode_nMeasure[n] = 'Y';
				continue;

			} else if (numAdj != fillCycNum) {
				// ..*_adjusted is set --- check this one
				finalNum = numAdj;
				///// log.debug("(temp) finalNum (cycleAdj[{}]) = {}", n, numAdj);

			} else if (num != fillCycNum) {
				// ..cycle_number is set; *_adjusted is NOT set --- check this one
				finalNum = num;
				///// log.debug("(temp) finalNum (cycle[{}]) = {}", n, num);

			} else {
				// ..neither one is set --- skip this check
				mode_nMeasure[n] = 'Z';
				continue;
			}

			if (finalNum != prevNum) {
				// ..new number to check

				Integer ndx = CycNumIndex_cycle2index.get(finalNum);

				if (ndx == null) {
					missCyc.increment(n);
				}

				prevNum = finalNum;

				if (ndx == null) {
					prevMode = 'X';
				} else {
					prevMode = data_mode[Integer.valueOf(ndx)];
				}
			}

			cycNumSet.add(Integer.valueOf(finalNum));

			mode_nMeasure[n] = prevMode;

			///// log.debug("(temp) n {}; finalNum {}; mode_nMeasure {}",
			///// n, finalNum, mode_nMeasure[n]);
		}

		log.debug("cyc_num in cyc_num_index: missCyc = {}", missCyc.counter);

		if (missCyc.counter > 0) {
			pass = false;
			missCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"Cycle number in CYCLE_NUMBER/*_ADJUSTED is not " + "in CYCLE_NUMBER_INDEX/*_ADJUSTED: At ",
					" measurements");
		}

		// .................check that all of the cycle number in .............
		// .................CYCLE_NUMBER_INDEX are in CYCLE_NUMBER.............
		log.debug("cyc_num-in-cyc_num_ind: end");
		log.debug("cyc_num_ind-in-cyc_num: start");

		missCyc.reset();

		for (int n = 0; n < nCycle; n++) {
			int num = cycleIndex[n];
			int numAdj = fillCycNum;

			if (core) {
				numAdj = cycleIndexAdj[n];
			}

			if (numAdj != fillCycNum) {
				// ..*_adjusted is set --- check this one
				finalNum = numAdj;

			} else if (num != fillCycNum) {
				// ..cycle_number is set; *_adjusted is NOT set --- check this one
				finalNum = num;

			} else {
				// ..neither one is set --- skip this check
				continue;
			}

			if (!cycNumSet.contains(Integer.valueOf(finalNum))) {
				missCyc.increment(n);
			}
		}

		log.debug("cyc_num_index in cyc_num: missCyc = {}", missCyc.counter);

		if (missCyc.counter > 0) {
			pass = false;
			missCyc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"Cycle number in CYCLE_NUMBER_INDEX/*_ADJUSTED is not " + "in CYCLE_NUMBER/*_ADJUSTED: At ",
					"cycles");
		}

		log.debug("cyc_num_ind-in-cyc_num: end");
		log.debug(".....validateCycleNumber: end.....");
		return pass;
	} // ..end validateCycleNumber

	/**
	 * Validates the DATA_MODE in the trajectory file and returns<br>
	 * <br>
	 * 1) the overall data mode <br>
	 * 2) the array of DATA_MODE[N_CYCLE]
	 *
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param mode (output!) DATA_MODE[N_CYCLE] values
	 * @return the overall data mode of the file ('D' if any D-mode cycle; 'R'
	 *         otherwise)
	 * @throws IOException If an I/O error occurs
	 */
	public char validateDataMode(char[] mode) throws IOException {
		log.debug(".....validateDataMode: start.....");

		// .....get DATA_MODE and check it as long we're at it.....

		String data_mode = arFile.readString("DATA_MODE", true); // ..true -> return NULLS

		ErrorTracker invalid = new ErrorTracker();

		char overallDM = 'R';

		for (int n = 0; n < data_mode.length(); n++) {
			char m = data_mode.charAt(n);

			if (m != 'R' && m != 'A' && m != 'D') {
				// ..invalid data mode
				mode[n] = 'X';
				invalid.increment(n);
				log.debug("invalid data mode at position {}: '{}'", n, m);

			} else {
				mode[n] = m;
			}

			if (m == 'D') {
				overallDM = 'D';
			}
		}

		invalid.addMessage(validationResult.getErrors(), "DATA_MODE: Invalid at ", "cycles");

		log.debug(".....validateDataMode: end.....");
		return overallDM;
	}// ..end validateDataMode

	/**
	 * Validates the dates in the trajectory file.
	 * 
	 * Date Checks
	 * <ul>
	 * <li>REFERENCE_DATE: Matches spec meta-data
	 * <li>DATE_CREATION: After JULD, before current time
	 * <li>DATE_UPDATE: After DATE_CREATION, HISTORY_DATE
	 * <li>HISTORY_DATE: After DATE_CREATION, before DATE_UPDATE
	 * </ul>
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param nProf    the number of profiles in the file
	 * @param nParam   the number of parameters in the file
	 * @param nCalib   the number of calibration records in the file
	 * @param nHistory the number of history records in the file
	 * @throws IOException If an I/O error occurs
	 */
	public void validateDates(int nParam, int nHistory) throws IOException {
		log.debug(".....validateDates: start.....");

		// ..check Reference_Date

		String name = "REFERENCE_DATE_TIME";
		String ref = arFile.readString(name);

		log.debug(name + ": " + ref);
		if (!ref.matches(arFile.getFileSpec().getMeta(name))) {
			validationResult.addError(name + ": '" + ref + "': Does not match specification ('"
					+ arFile.getFileSpec().getMeta(name) + "')");
		}

		Date fileTime = new Date(arFile.getFile().lastModified());
		// ...........creation and update dates checks:.............
		super.validateCreationUpdateDates(fileTime);

		// ...................history date checks...................
		// ..if set, after DATE_CREATION, before DATE_UPSATE

		if (nHistory > 0) {
			String[] dateHist = arFile.readStringArr("HISTORY_DATE");

			for (int h = 0; h < nHistory; h++) {
				String d = dateHist[h].trim();
				if (d.length() > 0) {
					// ..HISTORY_DATE is set. Is it reasonable?
					log.debug("HISTORY_DATE[{}]: '{}'", h, dateHist[h]);

					Date date = ArgoDate.get(d);
					if (date == null) {
						validationResult.addError("HISTORY_DATE[" + (h + 1) + "]: '" + dateHist[h] + "': Invalid date");

					} else if (arFile.isHaveUpdateDate()) {
						long dateSec = date.getTime();
						if ((dateSec - arFile.getUpdateSec()) > oneDaySec) {
							validationResult.addError("HISTORY_DATE[" + (h + 1) + "]: '" + dateHist[h]
									+ "': After DATE_UPDATE ('" + arFile.getUpdateDate() + "')");
						}
					}
				}
			}
		} // ..end if (nHistory)

		log.debug(".....validateDates: end.....");
	}// ..end validateDates

	/**
	 * Validates the MEASUREMENT_CODE and JULD variables.
	 *
	 * Bio-trajectory exception: does not contain CYCLE_NUMBER_ADJUSTED does not
	 * contain JULD_ADJUSTED*(N_MEASUREMENT) variables does not contain the
	 * JULD_*(N_CYCLE) variables
	 * 
	 * Assumes:
	 * <ul>
	 * <li>DATA_MODE has been read and validated (and passed into routine)
	 * <li>CYCLE_NUMBER variables (N_MEASUREMENT and N_CYCLES) have been validated
	 * separately
	 * </ul>
	 *
	 * Variable checks:
	 *
	 *
	 * @param nMeasure      N_MEASUREMENT value
	 * @param mode_nMeasure DATA_MODE mapped into [N_MEASUREMENT] space
	 * @param fv            (OUTPUT!) Information for later processing
	 * @return true = checks passed; false = checks failed
	 * @throws IOException If an I/O error occurs
	 */

	public boolean validateMC_and_JULD(int nMeasure, char[] mode_nMeasure,
			// HashMap<Integer, Integer>CycNumIndex_cycle2index,
			Final_NMeasurement_Variables[] fv) throws IOException {
		log.debug(".....validateMC_and_JULD: start.....");
		log.debug("nMeasure = {}", nMeasure);

		boolean core = false;
		ArgoReferenceTable.ArgoReferenceEntry info;

		if (arFile.fileType() == FileType.TRAJECTORY) {
			// ..implies this is 1) a v3.2+ or 2) a pre-v3.2 core-file (NOT a bio-file)
			core = true;
		}

		log.debug("core = {}", core);

		// ..............cycle_number...............
		log.debug("..validate CYCLE_NUMBER");

		int[] cycle = arFile.readIntArr("CYCLE_NUMBER");

		int[] cycle_adj = null;
		if (core) {
			cycle_adj = arFile.readIntArr("CYCLE_NUMBER_ADJUSTED");
		}

		int[] finalCycle = new int[nMeasure];

		for (int n = 0; n < nMeasure; n++) {
			// ..set a "final cycle" for this measurement
			if (core && cycle_adj[n] < fillCycNum) {
				finalCycle[n] = cycle_adj[n];
			} else {
				finalCycle[n] = cycle[n];
			}
		}

		// ..........MEASUREMENT_CODE............
		log.debug("..validate MEASUREMENT_CODE");

		int[] m_code = arFile.readIntArr("MEASUREMENT_CODE");

		ErrorTracker delCode = new ErrorTracker();
		ErrorTracker depCode = new ErrorTracker();
		ErrorTracker invCode = new ErrorTracker();

		for (int n = 0; n < nMeasure; n++) {

			// ..check values of of MC:
			// .. general rule:
			// .. multiples of 50 and 100 are LEGAL
			// ..
			// .. with these caveats:
			// .. < 0 is illegal
			// .. 0 is LEGAL
			// .. 50 is illegal
			// .. > 925 is illegal
			// ..
			// .. (some of the "50-codes" are unassigned. I am ignoring that)
			// ..
			// .. relative codes: MC is a multiple of 50
			// .. the relative codes are specified as MC-15 to MC-1
			// ..
			// .. specific codes: There is a list (table) of specifically allowed codes

			// ..NOTE: "%" is "remaindering" relative to the lower number
			// ..
			// .. MC1 MC2
			// .. |------->| rem === remainder
			// .. |<----| rel === valid relative codes

			int rem = m_code[n] % 50;
			int rel = rem - 50;

			if (m_code[n] == 0) {
				// ..this is valid

			} else if (m_code[n] <= 50 || m_code[n] > 925) {
				// ..invalid codes
				invCode.increment(n);
				log.debug("m_code[{}]: invalid code = {}, rem = {}, rel = {}", n, m_code[n], rem, rel);

			} else if (rem == 0) {
				// ..this is a valid primary/secondary code

				// ..later I am going to need to know where these are for checking
				// ..the N_CYCLE JULD_* variables

			} else if (rel < -15) {
				// ..this is NOT a valid relative code

				// ..is it a valid "specfic code"?
				info = ArgoReferenceTable.MEASUREMENT_CODE_specific.contains(m_code[n]);
				if (info.isValid()) {
					if (info.isDeprecated) {
						depCode.increment(n);
						log.debug("m_code[{}]: deprecated code = {}, rem = {}, rel = {}", n, m_code[n], rem, rel);
					}

				} else if (info.isDeleted) {
					delCode.increment(n);
					log.debug("m_code[{}]: deleted code = {}, rem = {}, rel = {}", n, m_code[n], rem, rel);

				} else {
					invCode.increment(n);
					log.debug("m_code[{}]: invalid code = {}, rem = {}, rel = {}", n, m_code[n], rem, rel);
				}
			}
		}

		log.debug("invCode.counter = {}", invCode.counter);
		log.debug("depCode.counter = {}", depCode.counter);
		log.debug("delCode.counter = {}", delCode.counter);

		invCode.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
				"MEASUREMENT_CODE: Invalid measurement codes at ", "measurements");
		depCode.addMessage(validationResult.getWarnings(), "MEASUREMENT_CODE: Deprecated measurement codes at ",
				"measurements");
		delCode.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
				"MEASUREMENT_CODE: Obsolete measurement codes at ", "measurements");

		// ..........JULD............
		log.debug("..validate JULD");

		// ..read juld and associated variables

		double[] juld = arFile.readDoubleArr("JULD");

		String str = arFile.readString("JULD_QC", true); // ..true -> include any NULLs
		char[] juld_qc = new char[nMeasure];
		for (int n = 0; n < nMeasure; n++) {
			juld_qc[n] = str.charAt(n);
		}

		str = arFile.readString("JULD_STATUS", true); // ..true -> include any NULLs
		char[] juld_status = new char[nMeasure];
		log.debug("juld_status length = {}", str.length());
		log.debug("juld_status = '{}'", str);

		for (int n = 0; n < nMeasure; n++) {
			juld_status[n] = str.charAt(n);
		}

		boolean fail = false;

		ErrorTracker depQC = new ErrorTracker(); // ..count of deprecated QC codes
		ErrorTracker depStatus = new ErrorTracker();// ..count of deprecated Status codes
		ErrorTracker invQC = new ErrorTracker(); // ..count of invalid QC codes
		ErrorTracker invStatus = new ErrorTracker();// ..count of invalid Status codes
		ErrorTracker incStatus = new ErrorTracker();// ..count of inconsistent QC/Status codes
		ErrorTracker notMiss = new ErrorTracker(); // ..count of juld missing & QC not miss
		ErrorTracker noQC = new ErrorTracker(); // ..count of juld set & QC set to missing

		for (int n = 0; n < nMeasure; n++) {
			if (juld_qc[n] != ' ') {
				if ((info = ArgoReferenceTable.QC_FLAG.contains(juld_qc[n])).isValid()) {
					if (info.isDeprecated) {
						depQC.increment(n);
					}
				} else {
					invQC.increment(n);
				}
			}

			if (juld_status[n] != ' ') {
				info = ArgoReferenceTable.STATUS_FLAG.contains(juld_status[n]);
				if (info.isValid()) {
					if (info.isDeprecated) {
						depStatus.increment(n);
					}

				} else {
					invStatus.increment(n);
				}
			}

			if ((juld_qc[n] == ' ') ^ (juld_status[n] == ' ')) {
				incStatus.increment(n);
				log.debug("qc/status inconsistent: " + "n = {}: juld, juld_status, juld_qc = {}, '{}', '{}'", n,
						juld[n], juld_status[n], juld_qc[n]);

			} else if ((juld_qc[n] == '9') ^ (juld_status[n] == '9')) {
				incStatus.increment(n);
				log.debug("qc/status inconsistent: " + "n = {}: juld, juld_status, juld_qc = {}, '{}', '{}'", n,
						juld[n], juld_status[n], juld_qc[n]);
			}

			/*
			 * if (juld_qc[n] == ' ' || juld_qc[n] == '9') { if (! (juld_status[n] == ' ' ||
			 * juld_status[n] == '9') ) { incStatus.increment(n);
			 * log.debug("qc/status inconsistent: "+
			 * "n = {}: juld, juld_status, juld_qc = {}, '{}', '{}'", n, juld[n],
			 * juld_status[n], juld_qc[n]); } }
			 */

			if (ArgoFileValidator.is_999_999_FillValue(juld[n])) {
				// ..data is missing - QC better be too
				if (!(juld_qc[n] == '9' || juld_qc[n] == ' ')) {
					notMiss.increment(n);
				}

			} else {
				// ..data not missing - check QC value

				if (juld_qc[n] == ' ' || juld_qc[n] == '9') {
					noQC.increment(n);
				}
			}
		}

		log.debug("invQC, invStatus, incStatus, notMiss, noQC = {}, {}, {}, {}, {}", invQC.counter, invStatus.counter,
				incStatus.counter, notMiss.counter, noQC.counter);
		/*
		 * log.
		 * debug("invQC, invStatus, inc_Status, inc9Status, notMiss, noQC = {}, {}, {}, {}, {}, {}"
		 * , invQC.counter, invStatus.counter, inc_Status.counter, inc9Status.counter,
		 * notMiss.counter, noQC.counter);
		 */

		if (invQC.counter > 0) {
			// fail = true;
			invQC.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"JULD_QC: Invalid QC code at", "measurements");
		}
		if (depQC.counter > 0) {
			// fail = true;
			depQC.addMessage(validationResult.getWarnings(), "JULD_QC: Deprecated QC code at", "measurements");
		}
		if (invStatus.counter > 0) {
			// fail = true;
			invStatus.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"JULD_STATUS: Invalid status code at", "measurements");
		}
		if (depStatus.counter > 0) {
			// fail = true;
			depStatus.addMessage(validationResult.getWarnings(), "JULD_STATUS: Deprecated status code at",
					"measurements");
		}
		if (incStatus.counter > 0) {
			// fail = true;
			incStatus.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"JULD_QC / JULD_STATUS: Use of ' ' or '9' is inconsistent at", "measurements");
		}
		/*
		 * if (inc_Status.counter > 0) { //fail = true;
		 * inc_Status.addMessage(formatErrors, "JULD_QC = ' '; JULD_STATUS not ' ': At",
		 * "measurements"); } if (inc9Status.counter > 0) { //fail = true;
		 * inc_Status.addMessage(formatErrors, "JULD_QC = '9'; JULD_STATUS not '9': At",
		 * "measurements"); }
		 */
		if (notMiss.counter > 0) {
			// fail = true;
			notMiss.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"JULD: FillValue where QC not ' ' or '9': At", "measurements");
		}
		if (noQC.counter > 0) {
			// fail = true;
			noQC.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"JULD: Not FillValue where QC ' ' or '9': At", "measurements");
		}

		log.debug("fail = {}", fail);

		// ..........JULD_ADJUSTED............
		log.debug("..validate JULD_ADJUSTED");

		// ..Bio-trajectory files do NOT contain these variables

		double[] juld_adj = null;
		char[] juld_adj_qc = null;
		char[] juld_adj_status = null;

		if (core) {
			// ..read juld_adjusted and associated variables

			juld_adj = arFile.readDoubleArr("JULD_ADJUSTED");

			str = arFile.readString("JULD_ADJUSTED_QC", true); // ..true -> include any NULLs
			juld_adj_qc = new char[nMeasure];
			for (int n = 0; n < nMeasure; n++) {
				juld_adj_qc[n] = str.charAt(n);
			}

			str = arFile.readString("JULD_ADJUSTED_STATUS", true); // ..true -> include any NULLs
			juld_adj_status = new char[nMeasure];
			for (int n = 0; n < nMeasure; n++) {
				juld_adj_status[n] = str.charAt(n);
			}

			// ..get the juld_data_mode

			char mode[];

			str = arFile.readString("JULD_DATA_MODE");
			if (str == null) {
				// ..pre-v3.2 file
				log.debug("JULD_DATA_MODE missing - pre-v3.2");
				mode = Arrays.copyOf(mode_nMeasure, nMeasure);

			} else {
				// ..v3.2+ file
				log.debug("JULD_DATA_MODE exists - v3.2+");
				mode = str.toCharArray();
			}

			// ....boolean fail = false; DON'T reset - want it to be cumulative for both
			ErrorTracker adjNotAorD = new ErrorTracker(); // ..count of juld_adj set but not mode A or D
			depQC.reset(); // ..count of deprecated QC codes
			depStatus.reset(); // ..count of deprecated Status codes
			invQC.reset(); // ..count of invalid QC codes
			invStatus.reset(); // ..count of invalid Status codes
			incStatus.reset(); // ..count of inconsistent QC/Status codes
			// inc_Status.reset(); //..count of inconsistent QC/Status codes
			// inc9Status.reset(); //..count of inconsistent QC/Status codes
			notMiss.reset(); // ..count of juld missing & QC not set to missing
			noQC.reset(); // ..count of juld set & QC set to missing

			for (int n = 0; n < nMeasure; n++) {
				if (juld_adj_qc[n] != ' ') {
					info = ArgoReferenceTable.QC_FLAG.contains(juld_adj_qc[n]);
					if (info.isValid()) {
						if (info.isDeprecated) {
							depQC.increment(n);
						}

					} else {
						invQC.increment(n);
					}
				}

				if (juld_adj_status[n] != ' ') {
					if (juld_adj_status[n] != ' ') {
						info = ArgoReferenceTable.STATUS_FLAG.contains(juld_adj_status[n]);
						if (info.isValid()) {
							if (info.isDeprecated) {
								depStatus.increment(n);
							}

						} else {
							invStatus.increment(n);
						}
					}
				}

				if ((juld_adj_qc[n] == ' ') ^ (juld_adj_status[n] == ' ')) {
					incStatus.increment(n);
					log.debug("qc/status inconsistent: " + "n = {}: juld, juld_status, juld_qc = {}, '{}', '{}'", n,
							juld[n], juld_adj_status[n], juld_adj_qc[n]);

				} else if ((juld_adj_qc[n] == '9') ^ (juld_adj_status[n] == '9')) {
					incStatus.increment(n);
					log.debug("qc/status inconsistent: " + "n = {}: juld, juld_status, juld_qc = {}, '{}', '{}'", n,
							juld[n], juld_adj_status[n], juld_adj_qc[n]);
				}
				/*
				 * if (juld_adj_qc[n] == ' ') { if (juld_adj_status[n] != ' ') {
				 * inc_Status.increment(n); log.debug("qc/status inconsistent: "+
				 * "n = {}: juld_adj, juld_adj_status, juld_adj_qc = {}, '{}', '{}'", n,
				 * juld_adj[n], juld_adj_status[n], juld_adj_qc[n]); } } if (juld_adj_qc[n] ==
				 * '9') { if (juld_adj_status[n] != '9') { inc9Status.increment(n);
				 * log.debug("qc/status inconsistent: "+
				 * "n = {}: juld_adj, juld_adj_status, juld_adj_qc = {}, '{}', '{}'", n,
				 * juld_adj[n], juld_adj_status[n], juld_adj_qc[n]); } }
				 */
				/*
				 * if (juld_adj_qc[n] == ' ' || juld_adj_qc[n] == '9') { if (!
				 * (juld_adj_status[n] == ' ' || juld_adj_status[n] == '9') ) {
				 * incStatus.increment(n); log.debug("qc/status inconsistent: "+
				 * "n = {}: juld_adj, juld_adj_status, juld_adj_qc = {}, '{}', '{}'", n,
				 * juld_adj[n], juld_adj_status[n], juld_adj_qc[n]); } }
				 */

				if (ArgoFileValidator.is_999_999_FillValue(juld_adj[n])) {
					// ..data is missing - QC better be too
					if (!(juld_adj_qc[n] == '9' || juld_adj_qc[n] == ' ')) {
						notMiss.increment(n);
					}

				} else {
					// ..juld_adj not missing - check QC value

					if (juld_adj_qc[n] == ' ' || juld_adj_qc[n] == '9') {
						noQC.increment(n);
					}

					// ..juld_adj not missing --- the possibilities are
					// ..case 1: juld missing, juld_adj not missing:
					// .. - this represents "estimation" and mode can be anything
					// ..case 2: juld not missing, juld_adj not missing:
					// .. - mode = 'A' or 'D'

					if (!ArgoFileValidator.is_999_999_FillValue(juld[n])) {
						// ..juld not missing, juld_adj not missing -- mode must be 'A' or 'D'

						// ..but ignore the "launch cycle" (-1)

						if (finalCycle[n] > 0) {
							// Integer ndx =
							// CycNumIndex_cycle2index.get(Integer.valueOf(finalCycle[n]));

							if (mode[n] != 'A' && mode[n] != 'D') {
								adjNotAorD.increment(n);
							}
						}
					}
				}
			}

			log.debug("invQC, invStatus, incStatus = {}, {}, {}", invQC.counter, invStatus.counter, incStatus.counter);
			// log.debug("invQC, invStatus, inc_Status, inc9Status = {}, {}, {}, {}",
			// invQC.counter, invStatus.counter, inc_Status.counter, inc9Status.counter);
			log.debug("notMiss, noQC, adjNotAorD = {}, {}, {}", notMiss.counter, noQC.counter, adjNotAorD.counter);

			if (invQC.counter > 0) {
				// fail = true;
				invQC.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						"JULD_ADJUSTED_QC: Invalid QC code at", "measurements");
			}
			if (depQC.counter > 0) {
				// fail = true;
				depQC.addMessage(validationResult.getWarnings(), "JULD_ADJUSTED_QC: Deprecated QC code at",
						"measurements");
			}
			if (invStatus.counter > 0) {
				// fail = true;
				invStatus.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						"JULD_ADJUSTED_STATUS: Invalid status code at", "measurements");
			}
			if (depStatus.counter > 0) {
				// fail = true;
				depStatus.addMessage(validationResult.getWarnings(), "JULD_ADJUSTED_STATUS: Deprecated status code at",
						"measurements");
			}

			if (incStatus.counter > 0) {
				// fail = true;
				incStatus.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						"JULD_ADJUSTED_QC / JULD_ADJUSTED_STATUS: Use of ' '/'9' is inconsistent at", "measurements");
			}

			/*
			 * if (inc_Status.counter > 0) { //fail = true;
			 * inc_Status.addMessage(formatErrors,
			 * "JULD_ADJUSTED_QC = ' '; JULD_ADJUSTED_STATUS not ' ': At", "measurements");
			 * } if (inc9Status.counter > 0) { //fail = true;
			 * inc9Status.addMessage(formatErrors,
			 * "JULD_ADJUSTED_QC = '9'; JULD_ADJUSTED_STATUS not '9': At", "measurements");
			 * }
			 */
			/*
			 * if (incStatus.counter > 0) { //fail = true;
			 * incStatus.addMessage(formatErrors,
			 * "JULD_ADJUSTED_QC = ' ' or '9' and JULD_ADJUSTED_STATUS not ' ' or '9': At",
			 * "measurements"); }
			 */
			if (notMiss.counter > 0) {
				// fail = true;
				notMiss.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						"JULD_ADJUSTED: FillValue where QC not ' ' or '9': At", "measurements");
			}
			if (noQC.counter > 0) {
				// fail = true;
				noQC.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						"JULD_ADJUSTED: Not FillValue where QC ' ' or '9': At", "measurements");
			}
			if (adjNotAorD.counter > 0) {
				// fail = true;
				adjNotAorD.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						"JULD_ADJUSTED: Not FillValue where DATA_MODE not 'A' or 'D': At", " measurements");
			}

			log.debug("fail = {}", fail);

			/*
			 * if (fail == true) { validationResult.
			 * addError("Above JULD/JULD_ADJUSTED errors prohibit further tests");
			 * log.debug(".....validateMC_and_JULD: end (due to prior juld errors).....");
			 * return false; }
			 */
		} else {
			log.debug("not core file.  skip");
		} // end if(core)

		// .............check for reasonable juld dates.............
		log.debug("..validate reasonable dates");

		String update = arFile.readString("DATE_UPDATE");
		Date dateUpdate = ArgoDate.get(update);
		long updateSec = dateUpdate.getTime();

		if (log.isDebugEnabled()) {
			log.debug("earliestDate:  " + ArgoDate.format(earliestDate));
			log.debug("DATE_UPDATE:   " + update);
		}

		ErrorTracker juldAfterUpdate = new ErrorTracker();
		ErrorTracker juldBeforeEarliest = new ErrorTracker();

		ErrorTracker juld_adjAfterUpdate = new ErrorTracker();
		ErrorTracker juld_adjBeforeEarliest = new ErrorTracker();

		for (int n = 0; n < nMeasure; n++) {

			// ...JULD...

			int qc_index = goodJuldQC.indexOf(juld_qc[n]);

			if (qc_index >= 0) {
				// ..QC indicates "good"

				if (!ArgoFileValidator.is_999_999_FillValue(juld[n])) {

					// ..check that JULD is after earliestDate and before DATE_UPDATE

					Date date = ArgoDate.get(juld[n]);

					// String dtg = ArgoDate.format(date);
					// log.debug("JULD[{}]: {} = {} (qc = {})", n, juld[n], juldDTG, qc);

					if (date.before(earliestDate)) {
						juldBeforeEarliest.increment(n);
					}

					long sec = date.getTime();

					if ((sec - updateSec) > oneDaySec) {
						juldAfterUpdate.increment(n);
					}
				}
			}

			// ...JULD_ADJUSTED...
			// ..not in bio-traj files

			if (core) {
				qc_index = goodJuldQC.indexOf(juld_adj_qc[n]);

				if (qc_index >= 0) {
					if (!ArgoFileValidator.is_999_999_FillValue(juld_adj[n])) {

						// ..check that JULD_ADJUSTED is after earliestDate and before DATE_UPDATE

						Date date = ArgoDate.get(juld_adj[n]);

						if (date.before(earliestDate)) {
							juld_adjBeforeEarliest.increment(n);
						}

						long sec = date.getTime();

						if ((sec - updateSec) > oneDaySec) {
							juld_adjAfterUpdate.increment(n);
						}
					}
				}
			}
		}

		if (juldBeforeEarliest.counter > 0) {
			// fail = true;
			juldBeforeEarliest.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"JULD before " + earliestDate + " at", " measurements");
		}
		if (juldAfterUpdate.counter > 0) {
			// fail = true;
			juldAfterUpdate.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"JULD after update time at", "measurements");
		}
		if (juld_adjBeforeEarliest.counter > 0) {
			// fail = true;
			juld_adjBeforeEarliest.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"JULD_ADJUSTED before " + earliestDate + " at", "measurements");
		}
		if (juld_adjAfterUpdate.counter > 0) {
			// fail = true;
			juld_adjAfterUpdate.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"JULD_ADJUSTED after update time at", "measurements");
		}

		log.debug("fail = {}", fail);

		// ..we have all the N_MEASUREMENT variables in memory, lets retain what we need

		for (int n = 0; n < nMeasure; n++) {
			fv[n] = new Final_NMeasurement_Variables();
			fv[n].measurement_code = m_code[n];
			fv[n].cycle_number = finalCycle[n];

			// ..juld_adj is not in bio-trajectory

			if (core) {
				if (ArgoFileValidator.is_999_999_FillValue(juld_adj[n])) {
					fv[n].juld = juld[n];
					fv[n].juld_status = juld_status[n];
					fv[n].juld_qc = juld_qc[n];

				} else {
					fv[n].juld = juld_adj[n];
					fv[n].juld_status = juld_adj_status[n];
					fv[n].juld_qc = juld_adj_qc[n];

				}

			} else { // ..bio-file: not _adj variables
				fv[n].juld = juld[n];
				fv[n].juld_status = juld_status[n];
				fv[n].juld_qc = juld_qc[n];
			}
		}

		log.debug(".....validateMC_and_JULD: end.....");

		return (!fail);
	} // ..end validateMC_and_JULD

	/**
	 * Validates the meta-data in the trajectory file.
	 * 
	 * Meta-data checks PLATFORM_NUMBER: Valid (5 or 7 numeric digits) DATA_CENTRE:
	 * Valid value - Ref table 4 DATA_STATE_INDICATOR: Valid value - Ref table 6
	 * FIRMWARE_VERSION: Not empty FLOAT_SERIAL_NO: Not empty PLATFORM_TYPE: Valid
	 * value - Ref table 23 POSITIONING_SYSTEM: Valid value - Ref table 9
	 * WMO_INST_TYPE: Valid value - Ref table 8
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param nProf the number of profiles in the file
	 * @param dac   the ArgoReferenceTable.DACS dac indicator. If <i>null</i> the
	 *              DATA_CENTRE cannont be validated.
	 * @throws IOException If an I/O error occurs
	 */
	public boolean validateMetaData(ArgoReferenceTable.DACS dac) throws IOException {
		log.debug(".....validateMetaData: start.....");

		ArgoReferenceTable.ArgoReferenceEntry info;
		String name;
		String str;

		name = "PLATFORM_NUMBER"; // ..valid wmo id
		str = arFile.readString(name).trim();
		if (!super.validatePlatfomNumber(str)) {
			validationResult.addError("PLATFORM_NUMBER" + ": '" + str + "': Invalid");
		}

		// DATA_CENTRE
		super.validateDataCentre(dac);

		name = "DATA_STATE_INDICATOR"; // ..ref table 6
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);

		info = ArgoReferenceTable.DATA_STATE_INDICATOR.contains(str);
		if (info.isValid()) {
			if (info.isDeprecated) {
				validationResult.addWarning(name + ": '" + str + "' Status: " + info.message);
			}
		} else {
			validationResult.addError(name + ": '" + str + "' Status: " + info.message);
		}

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

		name = "PLATFORM_TYPE"; // ..ref table 23
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);

		info = ArgoReferenceTable.PLATFORM_TYPE.contains(str);
		if (info.isValid()) {
			if (info.isDeprecated) {
				validationResult.addWarning(name + ": '" + str + "' Status: " + info.message);
			}

		} else {
			validationResult.addError(name + ": '" + str + "' Status: " + info.message);
		}

		name = "POSITIONING_SYSTEM"; // ..ref table 9
		str = arFile.readString(name).trim();
		log.debug(name + ": '{}'", str);

		info = ArgoReferenceTable.POSITIONING_SYSTEM.contains(str);
		if (info.isValid()) {
			if (info.isDeprecated) {
				validationResult.addWarning(name + ": '" + str + "' Status: " + info.message);
			}

		} else {
			validationResult.addError(name + ": '" + str + "' Status: " + info.message);
		}

		name = "WMO_INST_TYPE";
		str = arFile.readString(name).trim();
		log.debug("{}: '{}'", name, str);

		if (str.length() == 0) {
			validationResult.addError(name + ": Not set");

		} else {
			try {
				int N = Integer.valueOf(str);

				info = ArgoReferenceTable.WMO_INST_TYPE.contains(N);
				if (info.isValid()) {
					if (info.isDeprecated) {
						validationResult.addWarning(name + ": '" + str + "' Status: " + info.message);
					}

				} else {
					validationResult.addError(name + ": '" + str + "' Status: " + info.message);
				}
			} catch (Exception e) {
				validationResult.addError(name + ": '" + str + "' Invalid. Must be integer.");
			}
		}

		log.debug(".....validateMetaData: end.....");
		return true;
	}// ..end validateMetaData

	/**
	 * Validates the remaining N_CYCLE variables.
	 *
	 * Bio-trajectory exception: does not contain GROUNDED
	 *
	 */

	public void validateNCycle(int nCycle, char[] mode) {
		log.debug(".....validateNCycle: start.....");

		String varName;
		ErrorTracker dep = new ErrorTracker();
		ErrorTracker inv = new ErrorTracker();
		ArgoReferenceTable.ArgoReferenceEntry info;

		// ..bio-traj exception: GROUNDED not in bio-traj files

		if (arFile.fileType() == FileType.TRAJECTORY) {
			varName = "GROUNDED";
			String g = arFile.readString(varName, true); // ..true -> include any NULLs

			for (int n = 0; n < nCycle; n++) {
				info = ArgoReferenceTable.GROUNDED.contains(g.charAt(n));
				if (info.isValid()) {
					if (info.isDeprecated) {
						dep.increment(n);
					}

				} else {
					inv.increment(n);
				}
			}

			// will be.. inv.addMessage(formatErrors, varName+": Invalid code at ",
			// "cycles");
			inv.addMessage(validationResult.getWarnings(), varName + ": Invalid code at ", "cycles");

			dep.addMessage(validationResult.getWarnings(), varName + ": Deprecated code at ", "cycles");
		}

		varName = "CONFIG_MISSION_NUMBER";
		int[] c = arFile.readIntArr(varName);

		// ..can be FillValue in real time
		// ..cycle 0 can be FillValue in any case, so start loop at 1

		inv.reset();
		ErrorTracker set = new ErrorTracker();

		for (int n = 1; n < nCycle; n++) {
			if (c[n] < 1) {
				inv.increment(n);
			}
			if (mode[n] == 'D' && c[n] == 99999) {
				set.increment(n);
			}
		}

		// will be.. inv.addMessage(formatErrors, varName+": Invalid value at",
		// "cycles");
		inv.addMessage(validationResult.getWarnings(), varName + ": Invalid value at", "cycles");

		set.addMessage(validationResult.getErrors(), varName + ": Not set in D-mode at", "cycles");

		log.debug(".....validateNCycle: end.....");
	}

	/**
	 * Validates the N_CYCLE JULD_* variables. This involves comparing the
	 * JULD(N_MEASUREMENT) settings to the corresponding JULD_* variable based on
	 * the MEASUREMENT_CODE
	 *
	 * Bio-trajectory exception: does not contain *any* JULD_*(N_CYCLE) variables
	 *
	 * Assumes:
	 * <ul>
	 * <li>CYCLE_NUMBER and CYCLE_NUMBER_INDEX have been validated as correct
	 * </ul>
	 */

	public void validateNCycleJuld(int nMeasure, int nCycle, // char[] mode,
			HashMap<Integer, Integer> CycNumIndex_cycle2index, Final_NMeasurement_Variables[] finalNMVar) {
		log.debug(".....validateNCycleJuld: start.....");

		if (arFile.fileType() != FileType.TRAJECTORY) {
			// ..implies this is a core-file NOT a bio-file
			log.debug("bio-trajectory file.  skip all checks");
			log.debug(".....validateNCycleJuld: end.....");
			return;
		}

		// ..there are two ways (or more) to approach this:
		// ..1) build a structure that has all of the associated JULD_*
		// .. variables for all of the N_CYCLEs. Then loop through
		// .. the finalNMVar[N_MEASUREMENT] array once, finding
		// .. and comparing the associated JULD_* variables
		// ..
		// .. Memory heavy: has all of the values for both the
		// .. N_MEASUREMENT juld variables and each of the associated
		// .. N_CYCLE juld variables in memory at once.
		// ..
		// ..2) For each associated JULD_* variables
		// .. read in the data
		// .. loop through the N_MEASUREMENT data and find the
		// .. mc code variable to compare it to
		// .. ************** Let's try option 2 *******************

		int startNMeasureLoop = 0;
		if (finalNMVar[0].cycle_number == -1) {
			startNMeasureLoop = 1;
		}

		ValidateNCycleJuld_check juldCheck = new ValidateNCycleJuld_check(nCycle);

		for (Integer MC : ArgoReferenceTable.MEASUREMENT_CODE_toJuldVariable.keySet()) {
			juldCheck.reset();

			int M_CODE = MC.intValue();
			String var = ArgoReferenceTable.MEASUREMENT_CODE_toJuldVariable.get(MC).getColumn(2);

			// ..detect and deal with special "flagged" MC values

			boolean only_first = false;
			boolean only_last = false;

			if (M_CODE > 10000) {
				if (M_CODE % 100 == 01) {
					only_first = true;

				} else if (M_CODE % 100 == 02) {
					only_last = true;
				}

				// ..unmangle the coded value
				M_CODE /= 100;

				log.debug("flagged MC = {}:  first, last = {} {}", M_CODE, only_first, only_last);
			}

			// ..read the n_cycle data

			log.debug("...reading '{}'", var);

			double[] juldVar = arFile.readDoubleArr(var);

			String str = arFile.readString(var + "_STATUS", true); // ..true -> include any NULLs
			char[] juldVar_status = new char[nCycle];
			for (int n = 0; n < nCycle; n++) {
				juldVar_status[n] = str.charAt(n);
			}

			// ......loop through the n_measurement doing comparisons.....
			// ..initialize variables

			int prev_cycNum = finalNMVar[startNMeasureLoop].cycle_number;
			boolean do_onlyfirst = true;
			boolean do_onlylast = false;
			int last_n = -1, last_index = -1;
			int mc = -1;

			// ..loop

			for (int n = startNMeasureLoop; n < nMeasure; n++) {
				int cycNum = finalNMVar[n].cycle_number;

				if (cycNum != prev_cycNum) {
					if (do_onlylast) {
						// ..we have to compare the last JULD iteration

						juldCheck.check(last_n, finalNMVar[last_n].juld, finalNMVar[last_n].juld_status, last_index,
								juldVar[last_index], juldVar_status[last_index]);

						do_onlylast = false;

						log.debug("(temp) only_last: mc {}: juld[{}] = {} {} : {}[{}] = {} {}", mc, last_n,
								finalNMVar[last_n].juld, finalNMVar[last_n].juld_status, var, last_index,
								juldVar[last_index], juldVar_status[last_index]);
					}

					do_onlyfirst = true;
					prev_cycNum = cycNum;
				}

				// ..carry on with this iteration

				mc = finalNMVar[n].measurement_code;

				if (mc == M_CODE) {
					// ..this is the one we are validating
					// ..find the index of this cycNum in N_CYCLE arrays

					Integer ndx = CycNumIndex_cycle2index.get(cycNum);
					if (ndx == null) {
						// will be.. validationResult.addError("No JULD_*[*] variable for cycle
						// "+cycNum);
						validationResult.addWarning("No JULD_*[*] variable for cycle " + cycNum);
						log.error(
								"validateNCycleJuld: CycNumIndex_cycle2index[{}] is null. " + "This should not happen",
								cycNum);
						continue;
					}

					int index = ndx.intValue();

					// ..check the only_first / only_last status

					if (only_last) {
						// ..we can't do anything until we know we hit the next cycle
						last_n = n;
						last_index = index;
						do_onlylast = true;
						continue;
					}

					if (only_first) {
						if (!do_onlyfirst) {
							// ..already did first for this cycle
							continue;
						}
						do_onlyfirst = false;
					}

					// ..compare these values

					juldCheck.check(n, finalNMVar[n].juld, finalNMVar[n].juld_status, index, juldVar[index],
							juldVar_status[index]);

					log.debug("(temp) mc {}: juld[{}] = {} {} : {}[{}] = {} {}", mc, n, finalNMVar[n].juld,
							finalNMVar[n].juld_status, var, index, juldVar[index], juldVar_status[index]);

				} // ..endif mc == M_CODE

			} // ..end of nMeasure loop

			if (do_onlylast) {
				// ..there was a "dangling" only_last case in play
				// ..that is we never got to a new cycle to close it out
				// ..so do it now

				juldCheck.check(last_n, finalNMVar[last_n].juld, finalNMVar[last_n].juld_status, last_index,
						juldVar[last_index], juldVar_status[last_index]);

				log.debug("(temp) dangling only_last: mc {}: juld[{}] = {} {} : {}[{}] = {} {}", mc, last_n,
						finalNMVar[last_n].juld, finalNMVar[last_n].juld_status, var, last_index, juldVar[last_index],
						juldVar_status[last_index]);
			}

			// ..check unchecked values
			int notJuld = 0, a_notJuld = -1;
			int notJuld_s = 0, a_notJuld_s = -1;

			for (int n = 0; n < nCycle; n++) {
				if (!juldCheck.checked[n]) {
					// ..this value wasn't compared to a JULD value

					if (!ArgoFileValidator.is_999_999_FillValue(juldVar[n])) {
						notJuld++;
						if (a_notJuld < 0) {
							a_notJuld = n + 1;
							log.debug("unchecked {}[{}] not missing", var, n);
						}
					}

					if (!(juldVar_status[n] == '9' || juldVar_status[n] == ' ')) {
						notJuld_s++;
						if (a_notJuld_s < 0) {
							a_notJuld_s = n + 1;
							log.debug("unchecked {}_STATUS[{}] not ' ' or '9'", var, n);
						}
					}
				}
			}

			// .....report for this MC........
			if (juldCheck.incJuld.counter > 0) {
				juldCheck.incJuld.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						"JULD (MC " + M_CODE + ") / " + var + ": Inconsistent at ", "cycles",
						"(N_MEASUREMENT, N_CYCLE)");

				// validationResult.addError("JULD (MC "+M_CODE+") / "+var+
				// ": Inconsistent at "+
				// juldCheck.incJuld+" cycles; index of first case = "+
				// juldCheck.a_incJuld+", "+juldCheck.b_incJuld+
				// " (N_MEASUREMENT, N_CYCLE)");
			}
			if (juldCheck.incJuld_s.counter > 0) {
				juldCheck.incJuld_s.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						"JULD_STATUS (MC " + M_CODE + ") / " + var + "_STATUS: Inconsistent at ", "cycles",
						"(N_MEASUREMENT, N_CYCLE)");

				// validationResult.addError("JULD_STATUS (MC "+M_CODE+") / "+var+
				// "_STATUS: Inconsistent at "+
				// juldCheck.incJuld_s+" cycles; index of first case = "+
				// juldCheck.a_incJuld_s+", "+juldCheck.b_incJuld_s+
				// " (N_MEASUREMENT, N_CYCLE)");
			}
			if (notJuld > 0) {
				// * should be* validationResult.addError(var+" (MC "+M_CODE+
				validationResult
						.addWarning(var + " (MC " + M_CODE + "): Not FillValue where there is no associated JULD at "
								+ notJuld + " cycles; index of first case = " + a_notJuld);
			}
			if (notJuld_s > 0) {
				validationResult.addWarning(
						var + "_STATUS (MC " + M_CODE + "): Not FillValue where there is no associated JULD_STATUS at "
								+ notJuld_s + " cycles; index of first case = " + a_notJuld_s);
			}

		} // ..end of MC loop

		log.debug(".....validateNCycleJuld: end.....");
	}

	/**
	 * Validates the PARAM[N_MEASUREMENT] variables in a trajectory file.
	 * <p>
	 * Version Note: This routine will process both pre-v3.2 and v3.2+ trajectory
	 * files. The difference is in the handling of data_mode variables.
	 * <p>
	 * Assumes:
	 * <ul>
	 * <li>DATA_MODE has been read and validated (and passed into routine)
	 * <li>CYCLE_NUMBER variables (N_MEASUREMENT and N_CYCLES) have been validated
	 * separately
	 * </ul>
	 *
	 * Variable checks:
	 *
	 *
	 * @param nMeasure      N_MEASUREMENT value
	 * @param mode_nMeasure DATA_MODE[N_CYCLE] mapped into "N_MEARUEMENT" space
	 * @param paramList     List of trajectory parameters
	 * @throws IOException If an I/O error occurs
	 */

	public void validateParams(int nMeasure, char[] mode_nMeasure, ArrayList<String> paramList) throws IOException {
		log.debug(".....validateParams: start.....");
		log.debug("nMeasure = {}", nMeasure);

		boolean core = false;

		if (arFile.fileType() == FileType.TRAJECTORY) {
			// ..implies: 1) a v3.2+ file or 2) a pre-v3.2 core-file NOT a bio-file
			core = true;
		}

		log.debug("core = {}", core);

		// ........check <param> and <param>_QC..........

		float[] prm;
		char[] prm_qc;

		PARAM_LOOP: for (int nPrm = 0; nPrm < paramList.size(); nPrm++) {
			String param = paramList.get(nPrm);
			String varName = param.trim();
			log.debug("<param>/_QC check: '{}'", varName);

			if (varName.length() == 0) {
				continue; // ..TRAJ_PARAMS can have blank entries
			}

			Variable var = arFile.getNcReader().findVariable(varName);

			Number fillValue = var.findAttribute("_FillValue").getNumericValue();
			float fValue = fillValue.floatValue();

			// ..the possible <param> data configurations:
			// .. <type> <param> (N_MEASUREMENT[, extra-dimensions])

			// ..what we want to know: is there any Nan, any not-fill, or is it all fill
			// ..-> the data type doesn't matter (NaNs can only be in "real" types)
			// ..do all checks as "floats"

			// ..for the possible extra dimensions we want to consider
			// ..all of the values for a given measurement as one
			// ..that is, for the measurement, are any of the values NaN or not-fill
			// .. or are they all fill

			// ..read the array
			Array array;
			try {
				array = var.read();

			} catch (Exception e) {
				stderr.println(e.getMessage());
				e.printStackTrace(stderr);
				throw new IOException("Unable to read '" + var.getShortName() + "'");
			}

			int vRank = var.getRank();

			if (vRank == 1) {
				// ..this is just a standard (nMeasure) array
				// ..we want to cast whatever type it is to float

				DataType type = var.getDataType();

				if (type == DataType.FLOAT) {
					prm = (float[]) array.copyTo1DJavaArray();
					log.debug("...rank 1 float var: use copyTo1DJavaArray");
					// log.debug("(temp) prm: {}", Arrays.toString(prm));

				} else { // ..other than FLOAT - cast it value-by-value
					prm = new float[nMeasure];

					Index index = array.getIndex();

					for (int k = 0; k < nMeasure; k++) {
						index.set(k);
						prm[k] = array.getFloat(index);
					}

					log.debug("...rank 1 {} var: nMeas-by-nMeas cast", type);
					// log.debug("(temp) prm: {}", Arrays.toString(prm));
				}

			} else {
				// ..must be an extra dimension array

				prm = collapse_extra(array, nMeasure, var, fValue);
			}

			// ..get the QC

			String str = arFile.readString(varName + "_QC", true); // ..true -> include any NULLs

			if (str != null) {
				prm_qc = new char[nMeasure];
				for (int n = 0; n < nMeasure; n++) {
					prm_qc[n] = str.charAt(n);
				}

			} else {
				// ..not in file --- must be a bio-file PRES
				prm_qc = null;
			}

			// ..Set the data mode for this parameter

			String[] trajPrmDM = arFile.readStringArr("TRAJECTORY_PARAMETER_DATA_MODE");
			if (trajPrmDM == null) {
				log.debug("TRAJECTORY_PARAMETER_DATA_MODE missing - pre-v3.2");
			} else {
				log.debug("TRAJECTORY_PARAMETER_DATA_MODE exists - v3.2+");
			}

			char mode[] = new char[nMeasure];
			for (int n = 0; n < nMeasure; n++) {
				if (trajPrmDM != null) {
					// ..v3.2+ file
					mode[n] = trajPrmDM[n].charAt(nPrm);
					log.debug("trajPrmDM set. {}[{}]   '{}'", varName, n, mode[n]);
				} else {
					// ..pre-v3.2 file
					mode[n] = mode_nMeasure[n];
					log.debug("trajPrmDM not set {}[{}]   '{}'", varName, n, mode[n]);
				}
			}

			// ..analyze the param variables

			boolean fail = false;
			ArgoReferenceTable.ArgoReferenceEntry info;

			ErrorTracker depQC = new ErrorTracker();
			ErrorTracker invQC = new ErrorTracker();
			ErrorTracker missQC = new ErrorTracker();
			ErrorTracker nan = new ErrorTracker();
			ErrorTracker notMiss = new ErrorTracker();

			for (int n = 0; n < nMeasure; n++) {
				if (prm[n] == Float.NaN) {
					nan.increment(n);
				}

				if (prm_qc != null) {
					info = ArgoReferenceTable.QC_FLAG.contains(prm_qc[n]);

					if (prm_qc[n] == ' ' || info.isValid()) {
						// ..valid QC flag or " "

						if (info.isDeprecated) {
							depQC.increment(n);
						}

						if (ArgoFileValidator.is_FillValue(fValue, prm[n])) {
							// ..data is missing - QC better be too
							if (prm_qc[n] != '9' && prm_qc[n] != ' ') {
								notMiss.increment(n);
							}

						} else {
							// ..data not missing - check QC value

							if (prm_qc[n] == ' ' || prm_qc[n] == '9') {
								missQC.increment(n);
							}
						}

					} else {

						// ..QC was invalid
						invQC.increment(n);
					}
				}
			} // ..end for(nMeasure)

			log.debug("...counters: invQC, missQC, notMiss, nan = {}, {}, {}, {}", invQC.counter, missQC.counter,
					notMiss.counter, nan.counter);

			if (invQC.counter > 0) {
				fail = true;
				invQC.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						varName + "_QC: Invalid QC code at", "measurements");
			}

			invQC.addMessage(validationResult.getWarnings(), varName + "_QC: Deprecated QC code at", "measurements");

			if (missQC.counter > 0) {
				fail = true;
				missQC.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						varName + ": Not FillValue where QC is ' ' or 9 at", "measurements");
			}
			if (notMiss.counter > 0) {
				fail = true;
				notMiss.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						varName + ": FillValue where QC is not ' ' or 9 at", "measurements");
			}
			if (nan.counter > 0) {
				fail = true;
				nan.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
						varName + ": NaN at", "measurements");
			}

			if (fail) {
				// will be.. validationResult.addError("PARAM/_QC errors prevent checking
				// _ADJUSTED");
				validationResult.addWarning("PARAM/_QC errors prevent checking _ADJUSTED");
				continue PARAM_LOOP;
			}

			// .............param_adjusted...........

			varName = param.trim() + "_ADJUSTED";
			var = arFile.getNcReader().findVariable(varName);

			// ..bio-traj exception (pre-v3.2): PRES does not have *_ADJUSTED variables
			// ..Intermediate PARAMS may or may not have them
			// ..Depend on the format checks to verify the variables are correct
			// .. and go with the flow here
			// ..-- ie, if *_ADJUSTED not present, skip these checks entirely

			if (var == null) {
				log.debug("{} not in file: skip *_ADJUSTED checks", varName);
				continue PARAM_LOOP;
			}

			Variable varErr = arFile.getNcReader().findVariable(varName + "_ERROR");

			float[] prm_adj;
			char[] prm_adj_qc;
			float[] prm_adj_err;

			log.debug("<param>_ADJUSTED/_QC check: '{}'", varName);

			// ..the possible data configurations:
			// .. <type> <param> (N_MEASUREMENT[, extra-dimensions])

			// ..what we want to know: is there any Nan, any not-fill, or is it all fill
			// ..-> data-type doesn't matter (NaN can only be in float and double)
			// ..do it all as floats

			// ..for the possible extra dimensions we want to consider
			// ..all of the values for a given measurement as one
			// ..that is, for the measurement, are any of the values NaN or not-fill
			// .. or are they all fill

			// ..read the array
			try {
				array = var.read();

			} catch (Exception e) {
				stderr.println(e.getMessage());
				e.printStackTrace(stderr);
				throw new IOException("Unable to read '" + var.getShortName() + "'");
			}

			vRank = var.getRank();
			if (vRank == 1) {
				// ..this is just a standard (nMeasure) array

				DataType type = var.getDataType();

				if (type == DataType.FLOAT) {
					prm_adj = (float[]) array.copyTo1DJavaArray();
					log.debug("...rank 1 float var: use copyTo1DJavaArray");
					// log.debug("(temp) prm: {}", Arrays.toString(prm));

				} else { // ..other than FLOAT - cast it value-by-value
					prm_adj = new float[nMeasure];

					Index index = array.getIndex();

					for (int k = 0; k < nMeasure; k++) {
						index.set(k);
						prm_adj[k] = array.getFloat(index);
					}

					log.debug("...rank 1 {} var: nMeas-by-nMeas cast", type);
					// log.debug("(temp) prm: {}", Arrays.toString(prm));
				}

			} else {

				// ..must be an extra dimension array

				prm_adj = collapse_extra(array, nMeasure, var, fValue);
			}

			// ..get the QC

			str = arFile.readString(varName + "_QC", true); // ..true -> include any NULLs
			prm_adj_qc = new char[nMeasure];
			for (int n = 0; n < nMeasure; n++) {
				prm_adj_qc[n] = str.charAt(n);
			}

			// ..get <param>_ERROR --- always the standard dimension [N_MEASRUEMENT]

			try {
				array = varErr.read();

			} catch (Exception e) {
				stderr.println(e.getMessage());
				e.printStackTrace(stderr);
				throw new IOException("Unable to read '" + varErr.getShortName() + "'");
			}

			DataType type = varErr.getDataType();

			if (type == DataType.FLOAT) {
				prm_adj_err = (float[]) array.copyTo1DJavaArray();
				log.debug("...rank 1 float var ({}_ERROR): use copyTo1DJavaArray", varName);
				// log.debug("(temp) prm: {}", Arrays.toString(prm));

			} else { // ..other than FLOAT - cast it value-by-value
				prm_adj_err = new float[nMeasure];

				Index index = array.getIndex();

				for (int k = 0; k < nMeasure; k++) {
					index.set(k);
					prm_adj_err[k] = array.getFloat(index);
				}

				log.debug("...rank 1 {} var ({}_ERROR): nMeas-by-nMeas cast", type, varName);
				// log.debug("(temp) prm: {}", Arrays.toString(prm));
			}

			// ..analyze the param_adjusted variables

			fail = false;

			ErrorTracker adjNotMiss = new ErrorTracker();
			depQC.reset();
			ErrorTracker errNotMissAmode = new ErrorTracker();
			ErrorTracker errNotSetDmode = new ErrorTracker();
			ErrorTracker errNotMiss = new ErrorTracker();
			ErrorTracker incNotMeas = new ErrorTracker();
			invQC.reset();
			ErrorTracker missAdj = new ErrorTracker();
			nan.reset();
			ErrorTracker nanErr = new ErrorTracker();
			ErrorTracker notMissAdj = new ErrorTracker();
			ErrorTracker notMissAdjQc = new ErrorTracker();
			ErrorTracker notMissErr = new ErrorTracker();
			ErrorTracker notNotMeas = new ErrorTracker();
			ErrorTracker rErrNotMiss = new ErrorTracker();
			ErrorTracker rNotMiss = new ErrorTracker();
			ErrorTracker rQcNotMiss = new ErrorTracker();

			for (int n = 0; n < nMeasure; n++) {
				if (prm_adj[n] == Float.NaN) {
					nan.increment(n);
				}

				if (prm_adj_err[n] == Float.NaN) {
					nanErr.increment(n);
				}

				if (mode[n] == 'R') {

					// ........... r-mode ............

					if (!ArgoFileValidator.is_FillValue(fValue, prm_adj[n])) {
						rNotMiss.increment(n);
					}

					if (prm_adj_qc[n] != ' ' && prm_adj_qc[n] != '0' && prm_adj_qc[n] != '9') {
						rQcNotMiss.increment(n);
					}

					if (!ArgoFileValidator.is_FillValue(fValue, prm_adj_err[n])) {
						rErrNotMiss.increment(n);
					}

				} else {

					// ............ a-mode or d-mode ...........

					// ..check the QC flag

					if (prm_adj_qc[n] != ' ') {

						info = ArgoReferenceTable.QC_FLAG.contains(prm_adj_qc[n]);
						if (info.isValid()) {
							if (info.isDeprecated) {
								depQC.increment(n);
							}

						} else {
							invQC.increment(n);
						}
					}

					// ..check special case of adj_qc = ' '
					if (prm_qc[n] == ' ' || prm_adj_qc[n] == ' ') {
						// ..one is "not measured", both must be
						if (prm_qc[n] != ' ' || prm_adj_qc[n] != ' ') {
							incNotMeas.increment(n);

						} else {
							if (!ArgoFileValidator.is_FillValue(fValue, prm_adj[n])) {
								notNotMeas.increment(n);
							}
						}

					} else {

						// ..check if param (not param_adj!) is missing

						if (ArgoFileValidator.is_FillValue(fValue, prm[n])) {
							// .....param is missing.....

							if (!ArgoFileValidator.is_FillValue(fValue, prm_adj[n])) {
								// ..param_adjusted is NOT missing - error
								notMissAdj.increment(n);

							}
							if (!ArgoFileValidator.is_FillValue(fValue, prm_adj_err[n])) {
								// ..param_adjusted_error is NOT missing - error
								notMissErr.increment(n);
							}
							if (prm_adj_qc[n] != '9') {
								// ..param_adjusted_qc is NOT missing - error
								notMissAdjQc.increment(n);
							}

						} else {
							// .....param is NOT missing......

							if (ArgoFileValidator.is_FillValue(fValue, prm_adj[n])) {
								// ..param_adj is missing - QC must be 4 or 9
								if (prm_adj_qc[n] != '4' && prm_adj_qc[n] != '9') {
									missAdj.increment(n);
								}

								if (!ArgoFileValidator.is_FillValue(fValue, prm_adj_err[n])) {
									errNotMiss.increment(n);
								}

							} else {
								// ..param_adj is NOT missing

								if (prm_adj_qc[n] == '4' || prm_adj_qc[n] == '9') {
									if (mode[n] == 'D') {
										adjNotMiss.increment(n);

									} else { // ..mode == 'A'
										if (prm_adj_qc[n] == '9') {
											adjNotMiss.increment(n);
										}
									}

								} else {
									if (mode[n] == 'D') {
										if (is_FillValue(fValue, prm_adj_err[n])) {
											errNotSetDmode.increment(n);
										}
										// } else {
										// //.. mode == 'A'

										// //..core-parameters must NOT have error set
										// ..variable "core" refers to the file type, not the parameter type
										// ..allow PARAM_ADJUST_ERROR to be set for all parameters
										// if (core &&
										// ! is_FillValue(fValue, prm_adj_err[n])) {
										// errNotMissAmode.increment(n);
										// //errNotMissAmode.increment(n,
										// // "a-mode: prm_adj_err["+n+"] = "+prm_adj_err[n]);
										// }
									}
								}
							} // ..end if (param_adj is missing)
						} // ..end if (param is missing)
					} // ..end if (param_QC or param_adj_QC is blank (not measured)
				} // ..end if r-mode
			} // ..end for (nMeasure)

			if (log.isDebugEnabled()) {
				log.debug("...counters: adjNotMiss, errNotMiss, errNotMissAmode, errNotSetDmode = {}, {}, {}, {}",
						adjNotMiss.counter, errNotMiss.counter, errNotMissAmode.counter, errNotSetDmode.counter);
				log.debug("...counters: incNotMeas, invQC, depQC, missAdj = {}, {}, {}, {}", incNotMeas.counter,
						invQC.counter, depQC.counter, missAdj.counter);
				log.debug("...counters: nan, nanErr = {}, {}", nan.counter, nanErr.counter);
				log.debug("...counters: notMissAdj, notMissAdjQc, notMissErr, notNotMeas = {}, {}, {}, {}",
						notMissAdj.counter, notMissAdjQc.counter, notMissErr.counter, notNotMeas.counter);
				log.debug("...counters: rErrNotMiss, rNotMiss, rQcNotMiss = {}, {}, {}", rErrNotMiss.counter,
						rNotMiss.counter, rQcNotMiss.counter);
			}

			adjNotMiss.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + ": Not FillValue where QC is 4 or 9 at", "measurements");

			errNotMiss.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + "_ERROR: Not FillValue where " + varName + " is FillValue at", "measurements");

			errNotMissAmode.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"A-mode: " + varName + "_ERROR: Not FillValue at", "measurements");

			errNotSetDmode.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"D-mode: " + varName + "_ERROR: FillValue at", "measurements");

			incNotMeas.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + "_QC: Inconsistent ' ' with PARAM_QC at", "measurements");

			invQC.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + "_QC: Invalid at ", "measurements");
			depQC.addMessage(validationResult.getWarnings(), varName + "_QC: Deprecated at ", "measurements");

			missAdj.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + "_QC: FillValue where QC not ' ' or '9' at", "measurements");

			nan.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + ": NaN at ", "measurements");

			nanErr.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + "_ERROR: NaN at ", "measurements");

			notMissAdj.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + ": Not FillValue where PARAM is FillValue at", "measurements");

			notMissAdjQc.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + "_QC: Not 9 where PARAM is FillValue at", "measurements");

			notMissErr.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + "_ERROR: Not FillValue where PARAM is FillValue at", "measurements");

			notNotMeas.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					varName + ": Not FillValue where QC is set to ' ' at", "measurements");

			rErrNotMiss.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"R-mode: " + varName + "_ERROR: Not FillValue at ", "measurements");

			rNotMiss.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"R-mode: " + varName + ": Not FillValue at ", "measurements");

			rQcNotMiss.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
					"R-mode: " + varName + ": Not ' ' or '9' at", "measurements");
		} // ..end PARAM_LOOP

		log.debug(".....validateParams: end.....");

		return;
	} // ..end validateParams

	private float[] collapse_extra(Array array, int nMeasure, Variable var, float fillValue) throws IOException {

		// ..ASSUMPTION:
		// ..this is an "extra dimension" <param>
		// ..
		// ..the rank will be > 1; the shape will be (nMeasure, e1 [, e2 [, e3 ...]])

		// ..collapse all the extra-dimensions into 1
		// ..the shape is [nMeasure, i, j, k, ..]
		// ..the new shape will be [nMeasure, i*j*k*..]

		int vRank = var.getRank();
		int[] vShape = var.getShape();

		int[] newShape = new int[2];
		newShape[0] = nMeasure;
		newShape[1] = 1;

		for (int n = 1; n < vRank; n++) {
			newShape[1] *= vShape[n];
		}

		log.debug("...(collapse_extra) original shape {}", Arrays.toString(array.getShape()));

		array = array.reshapeNoCopy(newShape);

		log.debug("...(collapse_extra) new shape      {}", Arrays.toString(array.getShape()));

		float[] prm = new float[nMeasure];
		Index index = array.getIndex();

		// ..loop over measurements
		for (int n = 0; n < nMeasure; n++) {
			// ..loop over extra-dimension and find if it has
			// .. any NaNs, any non-FillValue

			boolean is_nan = false;
			float data = Float.MAX_VALUE;

			for (int i = 0; i < vShape[1]; i++) {
				index.set(n, i);
				float f = array.getFloat(index);

				if (f == Float.NaN) {
					is_nan = true;
				} else if (!ArgoFileValidator.is_FillValue(fillValue, f)) {
					data = f;
				}
			}

			if (is_nan) {
				prm[n] = Float.NaN;
			} // ..NaN trumps everything
			else if (data != Float.MAX_VALUE) {
				prm[n] = data;
			} // ..had data
			else {
				prm[n] = fillValue;
			}
		}

		return prm;
	} // ..end collapse_extra

	/**
	 * Validates the N_MEASUREMENT "position variables". This is the N_MEASURMENT
	 * variables except: CYCLE_NUMBER, MEASURMENT_CODE, JULD
	 * 
	 * Assumes:
	 * <ul>
	 * <li>DATA_MODE has been read and validated (and passed into routine)
	 * <li>CYCLE_NUMBER variables (N_MEASUREMENT and N_CYCLES) have been validated
	 * separately
	 * </ul>
	 *
	 * Variable checks:
	 *
	 *
	 * @param nMeasure                N_MEASUREMENT value
	 * @param CycNumIndex_cycle2index Cycle number-to-index mapping for N_CYCLE
	 *                                variables
	 * @throws IOException If an I/O error occurs
	 */

	public void validatePosition(int nMeasure) throws IOException {
		log.debug(".....validatePosition: start.....");
		log.debug("nMeasure = {}", nMeasure);

		double lat[] = arFile.readDoubleArr("LATITUDE");
		double lon[] = arFile.readDoubleArr("LONGITUDE");

		String str = arFile.readString("POSITION_QC", true); // ..true -> include any NULLs
		char[] pos_qc = new char[nMeasure];
		for (int n = 0; n < nMeasure; n++) {
			pos_qc[n] = str.charAt(n);
		}

		str = arFile.readString("POSITION_ACCURACY", true); // ..true -> include any NULLs
		char[] pos_acc = new char[nMeasure];
		for (int n = 0; n < nMeasure; n++) {
			pos_acc[n] = str.charAt(n);
		}

		// ........QC code check...........

		ErrorTracker depCode = new ErrorTracker();
		ErrorTracker invCode = new ErrorTracker();
		ArgoReferenceTable.ArgoReferenceEntry info;

		for (int n = 0; n < nMeasure; n++) {
			if (pos_qc[n] != ' ') {
				info = ArgoReferenceTable.QC_FLAG.contains(pos_qc[n]);
				if (info.isValid()) {
					if (info.isDeprecated) {
						depCode.increment(n);
					}

				} else {
					invCode.increment(n);
				}
			}
		}

		// will be.. invCode.addMessage(formatErrors, "POSITION_QC: Invalid QC code at
		// ", "measurements");
		invCode.addMessage(validationResult.getWarnings(), "POSITION_QC: Invalid QC code at ", "measurements");

		depCode.addMessage(validationResult.getWarnings(), "POSITION_QC: Deprecated QC code at ", "measurements");

		// ........accuracy code check...........

		depCode.reset();
		invCode.reset();

		for (int n = 0; n < nMeasure; n++) {
			if (pos_acc[n] != ' ') {
				info = ArgoReferenceTable.LOCATION_CLASS.contains(pos_acc[n]);
				if (info.isValid()) {
					if (info.isDeprecated) {
						depCode.increment(n);
					}

				} else {
					invCode.increment(n);
				}
			}
		}

		invCode.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
				"POSITION_ACCURACY: Invalid code at ", " measurements");
		depCode.addMessage(validationResult.getWarnings(), "POSITION_ACCURACY: Deprecated code at ", " measurements");

		// ........lat/lon/qc checks..............

		ErrorTracker notMissQC = new ErrorTracker();
		ErrorTracker notMissPos = new ErrorTracker();
		for (int n = 0; n < nMeasure; n++) {
			if (is_99_999_FillValue(lat[n]) || is_99_999_FillValue(lon[n])) {

				// ..lat or lon is FillValue, QC better be missing or FillValue

				if (!(pos_qc[n] == '9' || pos_qc[n] == ' ')) {
					notMissQC.increment(n);
				}

			} else if (pos_qc[n] == '9' || pos_qc[n] == ' ') {

				// ..QC is missing or FillValue, lat/lon is NOT FillValue
				notMissPos.increment(n);
			}
		}

		notMissQC.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
				"LAT/LON missing: QC is not 9 or ' ' at ", "measurements");

		notMissPos.addMessage(validationResult.getWarnings(), // will be.. formatErrors,
				"POSITION_QC = 9 or ' ': LAT/LON not missing at ", " measurements");

		log.debug(".....validatePosition: end.....");

		return;
	} // ..end validatePosition

	/**
	 * Validates the TRAJECTORY_PARAMETERS variable in the trajectory file.
	 * 
	 * STATION_PARAMETERS: Checks
	 * <ul>
	 * <li>Valid values for the file type
	 * <li>No empty values within list
	 * <li>No duplicate entries in list
	 * <li>PARAM variables exist
	 * <li>All PARAM variables (with data) are in list
	 * </ul>
	 *
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param nParam number of parameters in the file
	 * @returns A list of the physical parameters in the file
	 * @throws IOException If an I/O error occurs
	 */
	public ArrayList<String> validateTrajectoryParameters(int nParam) throws IOException {
		log.debug(".....validateTrajectoryParameters: start.....");

		boolean embeddedEmpty = false;
		int last_empty = -1;

		ArrayList<String> allowedParam = arFile.getFileSpec().getPhysicalParamNames(); // ..allowed <param>
		ArrayList<String> paramList = new ArrayList<String>(nParam);

		// .....check that trajectory parameters are valid.....
		String trajParam[] = arFile.readStringArr("TRAJECTORY_PARAMETERS");

		for (int paramNum = 0; paramNum < nParam; paramNum++) {
			paramList.add(""); // ..initialize this position in the list

			String param = trajParam[paramNum].trim();

			// ..check <param> name
			if (param.length() == 0) {
				// ..this param is empty
				last_empty = paramNum;
				log.debug("param #{}: '{}': empty", paramNum, param);

			} else {
				// ..this entry is not empty

				if (last_empty > 0) {
					// ..warn if an earlier entry was empty
					embeddedEmpty = true;
					last_empty = -1;
				}

				// ..check if this <param> is legal
				if (allowedParam.contains(param)) {
					// ..<param> is allowed

					if (paramList.contains(param)) {
						// ..this is a duplicate entry

						validationResult.addError(
								"TRAJECTORY_PARAMETERS[" + (paramNum + 1) + "]: '" + param + "': Duplicate entry");
						log.debug("param #{}: '{}': duplicate", paramNum, param);

					} else {
						log.debug("param #{}: '{}': accepted", paramNum, param);
					}

					if (arFile.getFileSpec().isDeprecatedPhysicalParam(param)) {
						// ..this is a deprecated parameter name

						validationResult.addWarning("TRAJECTORY_PARAMETERS[" + (paramNum + 1) + "]: '" + param
								+ "': Deprecated parameter name");
					}

				} else {
					// ..<param> is illegal
					validationResult.addError(
							"TRAJECTORY_PARAMETERS[" + (paramNum + 1) + "]: '" + param + "': Invalid parameter name");
					log.debug("param #{}: '{}': invalid", paramNum, param);
				}
			}

			paramList.set(paramNum, param);
		} // ..end for nParam

		log.debug("paramList: " + paramList);

		// ..report errors and warnings
		if (embeddedEmpty) {
			validationResult.addWarning("TRAJECTORY_PARAMETERS: Empty entries in list" + "\n\tList: " + paramList);
		}

		// ......check that all TRAJECTORY_PARAMETERS have <param> variable........
		for (String p : paramList) {
			if (p.length() == 0) {
				continue; // ..TRAJ_PARAMS can contain blank entries
			}
			Variable var = arFile.getNcReader().findVariable(p);
			if (var == null) {
				validationResult
						.addError("TRAJECTORY_PARAMETERS: PARAM '" + p + "' specified. Variables not in data file.");
				// fatalError = true;
			}
		}

		// ...check whether all <param> with data are in TRAJECTORY_PARAMETERS...
		for (String p : allowedParam) {
			if (!paramList.contains(p)) {
				// ..p is an allowed param. p is NOT in TRAJECTORY_PARAMETERS
				// ..is there a variable for it?

				Variable var = arFile.getNcReader().findVariable(p);
				if (var != null) {
					// ..this variable exists in the file
					// ..technically, that is OK as long as it is all FillValue

					DataType type = var.getDataType();
					boolean hasData = false;

					Number fillValue = var.findAttribute("_FillValue").getNumericValue();

					if (type == DataType.FLOAT) {
						float[] data = arFile.readFloatArr(p);
						float fVal = fillValue.floatValue();

						for (float d : data) {
							if (!is_FillValue(fVal, d)) {
								hasData = true;
								break;
							}
						}

					} else if (type == DataType.DOUBLE) {
						double[] data = arFile.readDoubleArr(p);
						double fVal = fillValue.doubleValue();

						for (double d : data) {
							if (!is_FillValue(fVal, d)) {
								hasData = true;
								break;
							}
						}

					} else {
						throw new IOException("validateTrajectoryParameter: unexpected data type");
					}

					if (hasData) {
						validationResult.addError("TRAJECTORY_PARAMETERS: Does not specify '" + p
								+ "'. Variable exists and contains data.");
						log.debug("{}: not in TRAJECTORY_PARAMETERS. exists and has data", p);
					} else {
						log.debug("{}: not in TRAJECTORY_PARAMETERS. exists and has data", p);
					}
				} // ..end if (var != null)
			} // end if (trajParams.contains(p))

		} // ..end allowedParam

		log.debug(".....validateTrajectoryParameters: end.....");

		return paramList;
	}// ..end validateTrajectoryParameters

	// .........................................
	// INNER CLASSES
	// .........................................

	// ..class variables

	private class Final_NMeasurement_Variables {
		int measurement_code;
		int cycle_number;
		double juld;
		char juld_status;
		char juld_qc;
	}

	/**
	 * Inner class to support error tracking This class has convenience functions to
	 * maintain lists of indices for errors,
	 *
	 */
	private class ErrorTracker {
		// ..object variables
		int counter;
		int[] indices1, indices2;
		boolean track2;

		static final int N_TRACKED = 5;

		// ..constructor
		protected ErrorTracker() {
			indices1 = new int[N_TRACKED];
			indices2 = new int[N_TRACKED];
			track2 = false;
			reset();
		}

		/**
		 * Add index to array of error-indexes (if it is less the maximum number that
		 * can be tracked)
		 */
		protected void increment(int index, String... label) {
			if (counter < N_TRACKED) {
				indices1[counter] = index + 1;
				if (label.length > 0) {
					log.debug("{}: index = {}", label[0], index);
				}
			}

			counter++;
		}

		/**
		 * Add index range to array of error-indexes (if it is less the maximum number
		 * that can be tracked)
		 */
		protected void increment(int index1, int index2, String... label) {
			if (counter < N_TRACKED) {
				indices1[counter] = index1 + 1;
				indices2[counter] = index2 + 1;
				if (label.length > 0) {
					log.debug("{}: index1, 2 = {}, {}", label[0], index1, index2);
				}
			}

			counter++;
			track2 = true;
		}

		/**
		 * Reset this error-tracker.
		 */
		protected void reset() {
			counter = 0;
			for (int i = 0; i < N_TRACKED; i++) {
				indices1[i] = -1;
				indices2[i] = -1;
			}
		}

		/**
		 * If the "instance" counter is > 0, add the message to the list of messages
		 */
		protected void addMessage(ArrayList<String> list, String str, String... label) {
			if (counter > 0) {
				list.add(message(str, label));
			}
		}

		protected String message(String str, String... label) {
			StringBuilder message = new StringBuilder(str);
			message.append(" " + counter);
			if (label.length > 0) {
				message.append(" " + label[0]);
			}

			if (counter == 1) {
				message.append("; index ");
				if (label.length > 1) {
					message.append(label[1] + " = ");
				}

				if (!track2) {
					message.append(indices1[0]);
				} else {
					message.append("(");
					message.append(indices1[0]);
					message.append(",");
					message.append(indices2[0]);
					message.append(")");
				}

			} else if (counter > 1) {
				int n = (counter < N_TRACKED) ? counter : N_TRACKED;
				message.append("\n\tfirst ");
				message.append(n);
				message.append(" indices ");
				if (label.length > 1) {
					message.append(label[1] + " = ");
				}

				if (!track2) {
					message.append(indices1[0]);
					for (int i = 1; i < n; i++) {
						message.append(", ");
						message.append(indices1[i]);
					}
				} else {
					message.append("(");
					message.append(indices1[0]);
					message.append(",");
					message.append(indices2[0]);
					message.append(")");

					for (int i = 1; i < n; i++) {
						message.append(", (");
						message.append(indices1[i]);
						message.append(",");
						message.append(indices2[i]);
						message.append(")");
					}
				}

			} else {
				log.debug("ErrorTracker: called addError with counter == 0");
			}

			return message.toString();
		}// ..end message()
	}// ..end class ErrorTracker

	private class ValidateNCycleJuld_check {
		// ..instance variables
		boolean[] checked;
		ErrorTracker incJuld;
		ErrorTracker incJuld_s;

		// ..constructor
		public ValidateNCycleJuld_check(int nCycle) {
			checked = new boolean[nCycle];
			incJuld = new ErrorTracker();
			incJuld_s = new ErrorTracker();
			reset();
		}

		// ..methods
		public void reset() {
			incJuld.reset();
			incJuld_s.reset();
			for (int n = 0; n < checked.length; n++) {
				checked[n] = false;
			}
		}

		public void check(int ndx, double juld, char juld_status, int ndxIndex, double juldIndex,
				char juldIndex_status) {

			checked[ndxIndex] = true;

			if (Math.abs(juld - juldIndex) > 1.e-6) {
				// ..these don't match
				incJuld.increment(ndx, ndxIndex);
			}

			if (juld_status != juldIndex_status) {
				// ..these don't match
				incJuld_s.increment(ndx, ndxIndex);
			}
		}
	}

} // ..end class
