package fr.coriolis.checker.filetypes;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.specs.ArgoAttribute;
import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.specs.ArgoDimension;
import fr.coriolis.checker.specs.ArgoFileSpecification;
import fr.coriolis.checker.specs.ArgoReferenceTable;
import fr.coriolis.checker.specs.ArgoVariable;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * Extends ArgoDataFile with features specific to an Argo Profile file.
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
 *          https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoProfileFile.java
 *          $
 * @version $Id: ArgoProfileFile.java 1269 2021-06-14 20:34:45Z ignaszewski $
 */
public class ArgoProfileFile extends ArgoDataFile {

	// .........................................
	// VARIABLES
	// .........................................

	// ..class variables
	// ..standard i/o shortcuts
	private static PrintStream stdout = new PrintStream(System.out);
	private static PrintStream stderr = new PrintStream(System.err);
	private static final Logger log = LogManager.getLogger("ArgoProfileFile");

	private final static long oneDaySec = 1L * 24L * 60L * 60L * 1000L;
	private final static String goodJuldQC = new String("01258");

	// ..object variables
	private String data_mode;

	private NetcdfFileWriter ncWriter;

	private ArrayList<ArrayList<String>> profParam;

	// .......................................
	// CONSTRUCTORS
	// .......................................

	protected ArgoProfileFile() throws IOException {
		super();
	}

	protected ArgoProfileFile(String specDir, String version) {
		// super(specDir, FileType.PROFILE, version);
	}

	// ..........................................
	// METHODS
	// ..........................................

	/** Retrieve the NetcdfFileWriter reference */
	public NetcdfFileWriter getNetcdfFileWriter() {
		return ncWriter;
	}

	/**
	 * Retrieve a list of variables in the associated file
	 */
	public ArrayList<String> getVariableNames() {
		ArrayList<String> varNames = new ArrayList<String>();

		if (ncReader != null) {
			for (Variable var : ncReader.getVariables()) {
				varNames.add(var.getShortName());
			}

		} else {
			varNames = null;
		}

		return varNames;
	}

	/**
	 * Convenience method to add to String list for "pretty printing".
	 *
	 * @param list the StringBuilder list
	 * @param add  the String to add
	 */
	private void addToList(StringBuilder list, String add) {
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
	private void addToList(StringBuilder list, int add) {
		if (list.length() == 0) {
			list.append(add);
		} else {
			list.append(", " + add);
		}
	}

	/**
	 * Closes an existing file.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	@Override
	public void close() throws IOException {
		if (ncWriter != null) {
			ncWriter.close();
		}
		super.close();
	} // ..end close()

	/**
	 * Creates a new Argo profile file. The "template" for the file is the indicated
	 * CDL specification file. The physical parameters (<PARAM> variables) to be
	 * included must be specified -- see the "parameters" argument.
	 * <p>
	 * A few variables in the CDL spec can be float or double. These default to
	 * double.
	 *
	 * @param fileName   The name of the output file
	 * @param specDir    Path to the specification directory
	 * @param version    The version string of the spec file to use as a template
	 * @param N_PROF     N_PROF dimension of the new file
	 * @param N_PARAM    N_PARAM dimension of the new file (max number of params in
	 *                   a single profile)
	 * @param N_LEVELS   N_LEVELS dimension of the new file
	 * @param N_CALIB    N_CALIB dimension of the new file
	 * @param parameters The parameter names that will be in the new file
	 * @throws IOException           If problems creating the file are encountered
	 * @throws NumberFormatException If problems converting certain values encoded
	 *                               in the CDL attributes to number are
	 *                               encountered.
	 */
	public static ArgoProfileFile createNew(String fileName, String specDir, String version,
			ArgoDataFile.FileType fileType, int N_PROF, int N_PARAM, int N_LEVELS, int N_CALIB,
			Set<String> inParameters) throws IOException, NumberFormatException {
		log.debug(".....createNew: start.....");

		ArgoProfileFile arFile = new ArgoProfileFile();

		// ..create the template specification

		arFile.spec = ArgoDataFile.openSpecification(false, specDir, fileType, version);
		if (arFile.spec == null) {
			return null;
		}

		arFile.fileType = fileType;

		// ..remove any parameters that are not in this specification

		HashSet<String> parameters = new HashSet<String>(20);

		for (String p : inParameters) {
			if (arFile.spec.isPhysicalParamName(p)) {
				parameters.add(p);
			} else {
				log.debug("requested parameter '{}' not in spec: removed");
			}
		}

		// ..create new file

		arFile.ncWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fileName);
		arFile.ncWriter.setFill(true);

		// ..fill in object variables
		arFile.file = null; // ..don't know why I need this ..... yet
		arFile.fileType = fileType;
		arFile.format_version = version;
		arFile.ncFileName = fileName;
		// arFile.spec is filled in by openSpec...

		// .....add globabl attributes.....

		for (ArgoAttribute a : arFile.spec.getGlobalAttributes()) {
			String name = a.getName();
			String value = (String) a.getValue();

			if (value.matches(ArgoFileSpecification.ATTR_SPECIAL_REGEX)) {
				// ..the definition starts with one of the special ATTR_IGNORE codes

				if (value.length() > ArgoFileSpecification.ATTR_SPECIAL_LENGTH) {
					// ..there is more than just the code on the line
					// ..defining the default value, use it

					value = value.substring(ArgoFileSpecification.ATTR_SPECIAL_LENGTH);
					log.debug("global attribute with special code: '{}'", value);

				} else {
					// ..nothing but the code on the line
					// ..ignore the attribute
					value = null;
				}
			}

			if (name.equals("history")) {
				value = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(new Date()).toString();
				log.debug("history global attribute: '{}'", value);
			}

			if (value != null) {
				arFile.ncWriter.addGroupAttribute(null, new Attribute(name, value));
				log.debug("add global attribute: '{}' = '{}'", name, value);
			}
			//// 2014-11-24T10:31:58Z creation;2014-11-26T16:31:26Z update" ;
		}

		// .........add Dimensions...............
		// ..don't allow <= 0 dimensions
		if (N_PROF <= 0) {
			N_PROF = 1;
		}
		if (N_PARAM <= 0) {
			N_PARAM = 1;
		}
		if (N_LEVELS <= 0) {
			N_LEVELS = 1;
		}
		if (N_CALIB <= 0) {
			N_CALIB = 1;
		}

		for (ArgoDimension d : arFile.spec.getDimensions()) {
			String name = d.getName();
			int value = d.getValue();

			if (name.equals("N_PROF")) {
				value = N_PROF;
			} else if (name.equals("N_PARAM")) {
				value = N_PARAM;
			} else if (name.equals("N_LEVELS")) {
				value = N_LEVELS;
			} else if (name.equals("N_CALIB")) {
				value = N_CALIB;
			}

			if (!d.isAlternateDimension()) {
				log.debug("add dimension: '{}' = '{}'", name, value);

				if (name.equals("N_HISTORY")) {
					arFile.ncWriter.addUnlimitedDimension(name);
				} else {
					arFile.ncWriter.addDimension(null, name, value);
				}
			} else {
				log.debug("skip alternate dimension: '{}'", name);
			}
		}

		// .........add Variables...............

		// ......ordered list.....
		// ..this bit of code arranges the variables in the "expected order"
		// ..this is, technically, completely unecessary
		// ..the ordering of the variables in the file should not matter
		// ..however, at least one user is complaining about the current files
		// ..and I am guessing that variable ordering is the problem.

		// ..the way the "spec" files are parsed, the PARAM variables end up
		// ..at the end of the variables list
		// ..so I am trying to distribute the variables in the "expected order"

		// ..good coding by users would eliminate the need for this

		// ..the idea is to create an ordered list of variable names that are
		// ..then used in the next section

		log.debug("...build ordered list of variables...");

		ArrayList<ArgoVariable> orderedList = new ArrayList<ArgoVariable>(200);

		for (ArgoVariable v : arFile.spec.getVariables()) {
			String name = v.getName();

			if (v.isParamVar()) {
				// ..when we get to the PARAM variables, we are done
				// ..they are always at the end of the list and we handle
				// ..them separately in the if-blocks below
				break;
			}

			orderedList.add(v);
			log.debug("add {}", name);

			// ..insert the PROFILE_<PARAM>_QC variables after POSITIONING_SYSTEM

			if (name.equals("POSITIONING_SYSTEM")) {
				log.debug("insert PROFILE_ here");
				for (ArgoVariable w : arFile.spec.getVariables()) {
					if (w.getName().startsWith("PROFILE_")) {
						orderedList.add(w);
						log.debug("add {}", w.getName());
					}
				}
			}

			// ..insert the <PARAM> variables after CONFIG_MISSION_NUMBER

			if (name.equals("CONFIG_MISSION_NUMBER")) {
				log.debug("insert <PARAM> here");
				for (ArgoVariable w : arFile.spec.getVariables()) {
					if (w.isParamVar()) {
						if (!w.getName().startsWith("PROFILE_")) {
							orderedList.add(w);
							log.debug("add {}", w.getName());
						}
					}
				}
			}
		}

		log.debug("...ordered list complete...");

		// ....end ordered list....

		Boolean keep;
		// ...if we didn't need the list to be ordered...
		// for (ArgoVariable v : arFile.spec.getVariables()) {

		for (ArgoVariable v : orderedList) {
			String name = v.getName();
			String prm = v.getParamName();

			if (prm != null) {
				// ..this is a physical parameter variable
				// ..is it's parameter name in the parameter list

				if (parameters.contains(prm)) {
					keep = true;
				} else {
					keep = false;
					log.debug("skip variable: '{}'", name);
				}

			} else {
				// ..not a physical parameter, so keep it
				keep = true;
			}

			if (keep) {
				DataType type = v.getType();
				String dims = v.getDimensionsString();

				if (type == DataType.OPAQUE) {
					// ..this is one of the float_or_double types
					// ..default it to double
					type = DataType.DOUBLE;
				}

				if (v.canHaveAlternateDimensions()) {
					// ..this variable has an alternate dimension
					// ..dim string will have a "|" in it must remove it
					String newDims = dims.replaceAll("\\|\\w+", "");
					log.debug("modify alternate dims: before = '{}' after = '{}'", dims, newDims);
					dims = newDims;
				}

				Variable var = arFile.ncWriter.addVariable(null, name, type, dims);
				log.debug("add variable: '{}': '{}' '{}'", name, type, dims);

				// ..add attributes for this variable
				for (ArgoAttribute a : v.getAttributes()) {
					Attribute att = null;
					String aname = a.getName();

					if (a.isNumeric()) {
						Number num = (Number) a.getValue();

						if (num == null) {
							// ..see if there is a default
							String def = a.getDefaultValue();

							if (def != null) {
								try {
									switch (type) {
									case INT:
										num = new Integer(def);
										break;
									case FLOAT:
										num = new Float(def);
										break;
									case DOUBLE:
										num = new Double(def);
										break;
									case LONG:
										num = new Long(def);
										break;
									}
								} catch (NumberFormatException e) {
									throw new NumberFormatException(
											"Attribute " + name + ":" + aname + ": Unable to convert to number");
								}
							}
						}

						if (num != null) {
							var.addAttribute(new Attribute(aname, num));
							log.debug("add attribute: '{}:{}' = '{}'", name, aname, num);
						} else {
							log.debug("attribute ignored (no value): '{}:{}'", name, aname);
						}

					} else if (a.isString()) {
						Object value = a.getValue();
						String str = null;

						if (value == null) {
							// ..value is not set, see if there is a default
							str = a.getDefaultValue();
						} else {
							str = value.toString();
						}

						if (str != null) {
							var.addAttribute(new Attribute(aname, str));
							log.debug("add attribute: '{}:{}' = '{}'", name, aname, str);
						}

					} else {
						log.error("attribute not Number or String: '{}:{}'", name, aname);
						continue;
					}

				}
			}
		}

		// .....create the file -- end "define mode"

		arFile.ncWriter.create();
		arFile.ncWriter.close(); // ..having trouble exiting define mode so close/reopen

		arFile.ncWriter = NetcdfFileWriter.openExisting(fileName);
		arFile.ncWriter.setFill(true);

		arFile.ncReader = arFile.ncWriter.getNetcdfFile();

		log.debug(".....createNew: end.....");
		return arFile;
	}

	/**
	 * Opens an existing file without opening the <i>specification</i>
	 *
	 * @param inFile the string name of the file to open
	 * @return the file object reference. Returns null if the file is not opened
	 *         successfully. (ArgoProfileFile.getMessage() will return the reason
	 *         for the failure to open.)
	 * @throws IOException If an I/O error occurs
	 */
	public static ArgoProfileFile open(String inFile) throws IOException {
		try {
			return (ArgoProfileFile.open(inFile, false));

		} catch (IOException e) {
			throw e;
		}

	}

	/**
	 * Opens an existing file without opening the <i>specification</i> and,
	 * optionally, ignores a bad DATA_TYPE --- will still detect accepted
	 * non-standard settings in old v3.1 files
	 *
	 * @param inFile          the string name of the file to open
	 * @param overrideBadTYPE true = force "open" to ignore BadTYPE failure
	 * @return the file object reference. Returns null if the file is not opened
	 *         successfully. (ArgoProfileFile.getMessage() will return the reason
	 *         for the failure to open.)
	 * @throws IOException If an I/O error occurs
	 */
	public static ArgoProfileFile open(String inFile, boolean overrideBadTYPE) throws IOException {
		ArgoDataFile arFile = ArgoDataFile.open(inFile, overrideBadTYPE);
		if (!(arFile instanceof ArgoProfileFile)) {
			ValidationResult.lastMessage = "ERROR: '" + inFile + "' not an Argo PROFILE file";
			return null;
		}

		return (ArgoProfileFile) arFile;
	}

	/**
	 * Opens an existing file and the assoicated <i>Argo specification</i>).
	 *
	 * @param inFile   the string name of the file to open
	 * @param specDir  the string name of the directory containing the format
	 *                 specification files
	 * @param fullSpec true = open the full specification; false = open the template
	 *                 specification
	 * @return the file object reference. Returns null if the file is not opened
	 *         successfully. (ArgoProfileFile.getMessage() will return the reason
	 *         for the failure to open.)
	 * @throws IOException If an I/O error occurs
	 */
	public static ArgoProfileFile open(String inFile, String specDir, boolean fullSpec) throws IOException {
		ArgoDataFile arFile = ArgoDataFile.open(inFile, specDir, fullSpec);
		if (!(arFile instanceof ArgoProfileFile)) {
			ValidationResult.lastMessage = "ERROR: '" + inFile + "' not an Argo PROFILE file";
			return null;
		}

		return (ArgoProfileFile) arFile;
	}

	/**
	 * Validates the data in the profile file. This is a driver routine that
	 * performs all types of validations (see other validate* routines).
	 *
	 * NOTE: This routine (and all other validate* routines) are designed to handle
	 * both core-profile and bio-profile files. There are some differnces in the
	 * validation checks that are noted where they occur.
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param singleCycle boolean indicator to only accept single-cycle files
	 * @param dacName     name of the DAC for this file
	 * @param ckNulls     true = check all strings for NULL values; false = skip
	 * @return success indicator. true - validation was performed. false -
	 *         validation could not be performed (getMessage() will return the
	 *         reason).
	 * @throws IOException If an I/O error occurs
	 */
	public boolean validate(boolean singleCycle, String dacName, boolean ckNulls) throws IOException {
		ArgoReferenceTable.DACS dac = null;

		if (!validationResult.isValid()) {
			ValidationResult.lastMessage = new String(
					"File must be verified (verifyFormat) " + "successfully before validation");
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
				ValidationResult.lastMessage = new String("Unknown DAC name = '" + dacName + "'");
				return false;
			}
		}

		// ............Determine number of profiles............

		int nProf = getDimensionLength("N_PROF");
		log.debug(".....validate: number of profiles {}.....", nProf);

		int nCalib = getDimensionLength("N_CALIB");
		int nHistory = getDimensionLength("N_HISTORY");
		int nParam = getDimensionLength("N_PARAM");
		int nLevel = getDimensionLength("N_LEVELS");

		if (ckNulls) {
			validateStringNulls();
		}
		validateHighlyDesirable(nProf);

		if (!validateMetaData(nProf, dac, singleCycle)) {
			return true;
		}

		validateDates(nProf, nParam, nCalib, nHistory);
		validateParams(nProf, nParam, nLevel);
		validateQC(nProf, nParam, nLevel);

		if (fileType == FileType.PROFILE) { // ..implies a core-file
			validateDMode(nProf, nParam, nCalib, nHistory);
		}

		return true;
	}// ..end validate

	/**
	 * Validates the dates in the profile file.
	 * 
	 * Date Checks
	 * <ul>
	 * <li>REFERENCE_DATE: Matches spec meta-data
	 * <li>JULD: Reasonable date - After 2000, before current time (or file time)
	 * <li>JULD_LOCATION: Within 1 day of JULD
	 * <li>DATE_CREATION: After JULD, before current time
	 * <li>DATE_UPDATE: After DATE_CREATION, HISTORY_DATE
	 * <li>HISTORY_DATE: After DATE_CREATION, before DATE_UPDATE
	 * <li>CALIBRATION_DATE: After DATE_CREATION, before DATE_UPDATE
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
	public void validateDates(int nProf, int nParam, int nCalib, int nHistory) throws IOException {
		log.debug(".....validateDates: start.....");

		// ..check Reference_Date

		String name = "REFERENCE_DATE_TIME";
		String ref = readString(name);

		log.debug(name + ": " + ref);
		if (!ref.matches(spec.getMeta(name))) {
			validationResult
					.addError(name + ": '" + ref + "': Does not match specification ('" + spec.getMeta(name) + "')");
		}

		// ..read other times

		double[] juld = readDoubleArr("JULD");
		String juld_qc = readString("JULD_QC", true); // ..true -> return NULLs if present
		double[] juld_loc = readDoubleArr("JULD_LOCATION");
		String creation = readString("DATE_CREATION");
		String update = readString("DATE_UPDATE");
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
					validationResult.addError("DATE_CREATION: '" + creation + "': After GDAC receipt time ('"
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
					validationResult.addError("DATE_UPDATE: '" + update + "': After GDAC receipt time ('"
							+ ArgoDate.format(fileTime) + "')");
				}
			}
		}

		// ............check per-profile dates.............
		// String posQC = readString("POSITION_QC");
		for (int n = 0; n < nProf; n++) {
			// ..all of the JULD checks must be contingent on JULD_QC
			char qc = juld_qc.charAt(n);
			int qc_index = goodJuldQC.indexOf(qc);

			if (qc_index >= 0) {
				if (juld[n] > 999990.) {
					validationResult.addError("JULD[" + (n + 1) + "]: Missing when QC = " + qc);
					continue;
				}

				// ..check that JULD is after earliestDate
				Date dateJuld = ArgoDate.get(juld[n]);

				String juldDTG = ArgoDate.format(dateJuld);

				log.debug("JULD[{}]: {} = {} (qc = {})", n, juld[n], juldDTG, qc);
				if (dateJuld.before(earliestDate)) {
					validationResult.addError("JULD[" + (n + 1) + "]: " + juld[n] + " = '" + juldDTG
							+ "': Before earliest allowed date ('" + earliestDate + "')");
				}

				// ..check that JULD is before DATE_CREATION and before file time
				long juldSec = dateJuld.getTime();

				if (haveCreation && (juldSec - creationSec) > oneDaySec) {
					validationResult.addError("JULD[" + (n + 1) + "]: " + juld[n] + " = '" + juldDTG
							+ "': After DATE_CREATION ('" + creation + "')");
				}
				if ((juldSec - fileSec) > oneDaySec) {
					validationResult.addError("JULD[" + (n + 1) + "]: " + juld[n] + " = '" + juldDTG
							+ "': After GDAC receipt time ('" + ArgoDate.format(fileTime) + "')");
				}

				// ..check that JULD_LOCATION is within "max" day of JULD
				if (log.isDebugEnabled()) {
					log.debug("JULD_LOCATION[" + n + "]: " + juld_loc[n] + " = "
							+ ArgoDate.format(ArgoDate.get(juld_loc[n])));
				}

				if (juld_loc[n] < 99990.d) {
					double max = 2.d;
					if (Math.abs(juld_loc[n] - juld[n]) > max) {
						validationResult.addWarning("JULD_LOCATION[" + (n + 1) + "]: " + juld_loc[n] + ": Not within "
								+ max + " day of JULD (" + juld[n] + ")");
					}

				} else { // ..juld_location is missing
							// ..if this is missing, position better be missing
							// ..this is very rare -- spend the overhead to do it
							// .. here each time
					double lat = readDouble("LATITUDE", n);
					double lon = readDouble("LONGITUDE", n);

					// if ((posQC.charAt(n) == '1' || posQC.charAt(n) == '2') &&
					// (lat < 99990.d || lon < 99990.d)) {
					if (lat < 99990.d || lon < 99990.d) {
						validationResult.addError("JULD_LOCATION[" + (n + 1) + "]: Missing when "
								+ "LATITUDE and/or LONGITUDE are not missing.");
					}
				}

			} else { // ..juld_qc not in goodJuldQC
				log.debug("JULD[{}]: {}. qc = {}. checks skipped", n, juld[n], qc);
			} // ..end if (qc_index >= 0) ---> position is "good"
		} // ..end per-profile date checks

		// ...................history date checks...................
		// ..if set, after DATE_CREATION, before DATE_UPDATE

		if (nHistory > 0) {
			ArrayChar hDate = (ArrayChar) findVariable("HISTORY_DATE").read();
			Index ndx = hDate.getIndex();

			for (int h = 0; h < nHistory; h++) {
				for (int n = 0; n < nProf; n++) {
					ndx.set(h, n, 0);
					String dateHist = hDate.getString(ndx).trim();

					if (dateHist.length() > 0) {
						// ..HISTORY_DATE is set. Is it reasonable?
						if (log.isDebugEnabled()) {
							log.debug("HISTORY_DATE[" + h + "," + n + "]: " + dateHist);
						}

						Date date = ArgoDate.get(dateHist);
						if (date == null) {
							validationResult.addError(
									"HISTORY_DATE[" + (h + 1) + "," + (n + 1) + "]: '" + dateHist + "': Invalid date");

							// } else if (haveCreation && date.before(dateCreation)) {
							// validationResult.addError("HISTORY_DATE["+(h+1)+","+(n+1)+"]: '"+
							// dateHist+
							// "': Before DATE_CREATION ('"+creation+"')");

						} else if (haveUpdate) {
							long dateSec = date.getTime();
							if ((dateSec - updateSec) > oneDaySec) {
								validationResult.addError("HISTORY_DATE[" + (h + 1) + "," + (n + 1) + "]: '" + dateHist
										+ "': After DATE_UPDATE ('" + update + "')");
							}
						}
					}
				}
			}
		} // ..end if (nHistory)

		// ...................calibration date checks...................
		// ..if set, after DATE_CREATION, before DATE_UPDATE

		if (nCalib > 0) {

			// ..variable changed names in v2.3, so get whichever exists
			// ..one of them has to exist, or format verify would have choked
			// ..try newer first - should be more efficient

			String calib_date = "SCIENTIFIC_CALIB_DATE";
			Variable var = findVariable(calib_date);
			if (var == null) {
				calib_date = "CALIBRATION_DATE";
				var = findVariable(calib_date);
			}

			ArrayChar cDate = (ArrayChar) var.read();
			Index ndx = cDate.getIndex();

			for (int n = 0; n < nProf; n++) {
				for (int c = 0; c < nCalib; c++) {
					for (int p = 0; p < nParam; p++) {
						ndx.set(n, c, p, 0);
						String dateCal = cDate.getString(ndx).trim();

						if (dateCal.length() > 0) {
							// ..SCI_CALIB_/CALIBRATION_DATE is set. Is it reasonable?
							if (log.isDebugEnabled()) {
								log.debug(calib_date + "[" + n + "," + c + "," + p + "]: " + dateCal);
							}

							Date date = ArgoDate.get(dateCal);
							if (date == null) {
								validationResult.addError(calib_date + "[" + (n + 1) + "," + (c + 1) + "," + (p + 1)
										+ "]: '" + dateCal + "': Invalid date");

								// } else if (haveCreation && date.before(dateCreation)) {
								// validationResult.addError("CALIBRATION_DATE["+(n+1)+","+(c+1)+","+
								// (p+1)+"]: '"+dateCal+
								// "': Before DATE_CREATION ('"+creation+"')");

							} else if (haveUpdate) {
								long dateSec = date.getTime();
								if ((dateSec - updateSec) > oneDaySec) {
									validationResult.addError(calib_date + "[" + (n + 1) + "," + (c + 1) + "," + (p + 1)
											+ "]: '" + dateCal + "': After DATE_UPDATE ('" + update + "')");
								}
							}
						} // ..end if (dateCal)
					} // ..end for nParam
				} // ..end for nCalib
			} // ..end for nProf
		} // ..end if (nCalib)
		log.debug(".....validateDates: end.....");
	}// ..end validateDates

	/**
	 * Validates the Delayed-mode data in the profile file.
	 *
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param nProf  the number of profiles in the file
	 * @param nParam the number of parameters in the file
	 * @param nCalib the number of calibration records in the file
	 * @param nHist  the number of history records in the file
	 * @throws IOException If an I/O error occurs
	 */
	public void validateDMode(int nProf, int nParam, int nCalib, int nHist) throws IOException {
		log.debug(".....validateDMode: start.....");

		String dMode = readString("DATA_MODE", true); // ..true -> return NULLs if present
		String[] dState = readStringArr("DATA_STATE_INDICATOR");
		ArrayChar params = (ArrayChar) findVariable("PARAMETER").read();
		ArrayChar cmts = (ArrayChar) findVariable("SCIENTIFIC_CALIB_COMMENT").read();
		// ArrayChar eqns =
		// (ArrayChar) findVariable("SCIENTIFIC_CALIB_EQUATION").read();
		// ArrayChar coefs =
		// (ArrayChar) findVariable("SCIENTIFIC_CALIB_COEFFICIENT").read();

		// ..variable changed names in v2.3, so get whichever exists
		// ..one of them has to exist, or format verify would have choked
		// ..try newer first - should be more efficient

		String calib_date = "SCIENTIFIC_CALIB_DATE";
		Variable var = findVariable(calib_date);
		if (var == null) {
			calib_date = "CALIBRATION_DATE";
			var = findVariable(calib_date);
		}
		ArrayChar dates = (ArrayChar) var.read();

		Index pNdx = params.getIndex();
		Index cNdx = cmts.getIndex();
		Index dNdx = dates.getIndex();

		ArrayList<String> calibParam = new ArrayList<String>();
		HashMap<String, String> profQC = new HashMap<String, String>();

		// ..................check CALIBRATION variables.....................

		for (int n = 0; n < nProf; n++) {
			if (dMode.charAt(n) == 'D') {
				String state = dState[n].trim();
				if (!(state.equals("2C") || state.equals("2C+"))) {
					validationResult.addError(
							"D-mode: DATA_STATE_INDICATOR[" + (n + 1) + "]: '" + state + "': Not set to \"2C\"");
				}
				for (int c = 0; c < nCalib; c++) {
					calibParam.clear();

					for (int p = 0; p < nParam; p++) {
						pNdx.set(n, c, p, 0);
						dNdx.set(n, c, p, 0);
						cNdx.set(n, c, p, 0);
						String param = params.getString(pNdx).trim();
						String cmt = cmts.getString(cNdx).trim();
						String date = dates.getString(dNdx).trim();
						// String eqn = eqns.getString(cNdx).trim();
						// String coef = coefs.getString(cNdx).trim();

						if (param.length() > 0) {
							calibParam.add(param);

							String pQcName = "PROFILE_" + param + "_QC";
							char pQC = 'X';
							String qc;

							if (!profQC.containsKey(pQcName)) {
								qc = readString(pQcName, true); // ..true->return NULLs if present
								if (qc == null) {
									validationResult.addError("D-mode: " + pQcName + " does not exist for"
											+ "PARAMETER[" + n + "," + c + "," + p + "]: '" + param + "'");
								}
								profQC.put(pQcName, qc);
								log.debug("adding PROFILE_<PARAM>_QC for '" + pQcName + "'");

							} else {
								qc = profQC.get(pQcName);
							}

							if (qc != null) {
								pQC = qc.charAt(n);
							}

							if (log.isDebugEnabled()) {
								log.debug("PARAMETER[{},{},{}]: '{}'", n, c, p, param);
								log.debug("PROFILE_{}_QC[{}]: '{}'", param, n, pQC);
								log.debug("CALIB_COMMENT[{},{},{}]: '{}'", n, c, p, cmt);
								log.debug("CALIB_DATE[{},{},{}]: '{}'", n, c, p, date);
								// log.debug("CALIB_EQUATION["+n+","+c+","+p+"]: '"+eqn+"'");
								// log.debug("CALIB_COEFFICIENT["+n+","+c+","+p+"]: '"+coef+"'");
							}

							if (pQC == 'X') {
								validationResult.addError("D-mode: SCIENTIFIC_CALIB variables not "
										+ "checked for PARAMETER '{}' due to missing PROFILE_param_QC");

							} else { // if (pQC != ' ') { //....Qc manual pg 74.
								if (cmt.length() == 0) {
									// ################# TEMPORARY WARNING ################
									validationResult.addWarning("D-mode: SCIENTIFIC_CALIB_COMMENT[" + (n + 1) + ","
											+ (c + 1) + "," + (p + 1) + "]: Not set for '" + param + "'");
									log.warn(
											"TEMP WARNING: {}: D-mode: SCIENTIFIC_CALIB_COMMENT[{},{},{}] not set for {}",
											file.getName(), n, c, p, param);
								}
								if (date.length() == 0) {
									// ################# TEMPORARY WARNING ################
									validationResult.addWarning("D-mode: " + calib_date + "[" + (n + 1) + "," + (c + 1)
											+ "," + (p + 1) + "]: Not set for '" + param + "'");
									log.warn("TEMP WARNING: {}: D-mode: {}[{},{},{}] not set for {}", file.getName(),
											calib_date, n, c, p, param);
								}
								// if (eqn.length() == 0) {
								// validationResult.addError("D-mode: SCIENTIFIC_CALIB_EQUATION["+
								// (n+1)+","+(c+1)+","+(p+1)+"]: Not set for '"+param+"'");
								// }
								// if (coef.length() == 0) {
								// validationResult.addError("D-mode: SCIENTIFIC_CALIB_COEFFICIENT["+
								// (n+1)+","+(c+1)+","+(p+1)+"]: Not set for '"+param+"'");
								// }

								// } else { //....Qc manual pg 74.
								// log.debug ("D-mode: PROF_*_QC[{}] = ' ': " +
								// "skipped SCI_CALIB[{},{},{}] checks", n, n, c, p);
							}
						} // ..end if (param.length)
					} // ..end for (nParam)

					// ..check that calibration info is set for all parameters
					for (String prm : profParam.get(n)) {
						if (!calibParam.contains(prm)) {
							validationResult.addError("D-mode: PARAMETER[" + (n + 1) + "," + (c + 1)
									+ ",*,*]: Parameter '" + prm + "' not included");
						}
					}

					// ..check that calibration info is set for all parameters
					for (String prm : calibParam) {
						if (!profParam.get(n).contains(prm)) {
							validationResult.addError("D-mode: PARAMETER[" + (n + 1) + "," + (c + 1) + ",*,*]: '" + prm
									+ "': Not in STATION_PARAMETERS");
						}
					}
				} // ..end for (nCalib)

			} else {// ..end if ('D')
				// ..either 'R' or 'A'
				String state = dState[n].trim();
				if (state.startsWith("2C")) {
					validationResult.addError(
							"R/A-mode: DATA_STATE_INDICATOR[" + (n + 1) + "]: '" + state + "': Can not be \"2C...\"");
				}
			}

		} // ..end for (nProf)

		// ..................check HISTORY variables.....................

		if (log.isDebugEnabled()) {
			log.debug("nHist: " + nHist);
		}
		if (nHist < 1) {
			validationResult.addError("D-mode: HISTORY_* not set");
		}

		log.debug(".....validateDMode: end.....");
	}// ..end validateDMode

	/**
	 * Validates the "highly-desirable" meta-data in the profile file.
	 *
	 * Highly Desirable Checks: INST_REFERENCE: Set POSITIONING_SYSTEM: Set and
	 * valid (reference table 9)
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param nProf the number of profiles in the file
	 * @throws IOException If an I/O error occurs
	 */
	public void validateHighlyDesirable(int nProf) throws IOException {
		log.debug(".....validateHighlyDesirable: start.....");

		if (spec.getVariable("INST_REFERENCE") != null) {
			// ..INST_REF is in spec, check that it is set

			String str[] = readStringArr("INST_REFERENCE");

			for (int n = 0; n < nProf; n++) {
				log.debug("INST_REFERENCE[" + n + "]: '" + str[n]);

				if (str[n].trim().length() == 0) {
					validationResult.addWarning("INST_REFERENCE[" + (n + 1) + "]: Not set");
				}
			}
		}

		if (spec.getVariable("POSITIONING_SYSTEM") != null) {
			// ..POSIT_SYS is in spec, check that it is set

			String str[] = readStringArr("POSITIONING_SYSTEM");

			for (int n = 0; n < nProf; n++) {
				log.debug("POSITIONING_SYSTEM[" + n + "]: '" + str[n] + "'");

				ArgoReferenceTable.ArgoReferenceEntry info = ArgoReferenceTable.POSITIONING_SYSTEM
						.contains(str[n].trim());
				if (!info.isActive) {
					validationResult.addWarning(
							"POSITIONING_SYSTEM[" + (n + 1) + "]: '" + str[n] + "' Status: " + info.message);
				}
			}
		}
		log.debug(".....validateHighlyDesirable: end.....");
	}// ..end validateHighlyDesirable

	/**
	 * Validates the meta-data in the profile file.
	 * 
	 * Meta-data checks:
	 * <ul>
	 * <li>PLATFORM_NUMBER: Valid (5 or 7 numeric digits)</li>
	 * <li>DIRECTION: Valid value - A or D</li>
	 * <li>DATA_STATE_INDICATOR: Valid value - Ref table 6<br>
	 * - can be empty if no data in PRES</li>
	 * <li>DATA_CENTRE: Valid value - Ref table 4</li>
	 * <li>WMO_INST_TYPE: Valid value - Ref table 8</li>
	 * <li>VERTICAL_SAMPLING_SCHEME: Valid value - Ref table 16<br>
	 * - can be empty if no data in PRES</li>
	 * </ul>
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param nProf       the number of profiles in the file
	 * @param dac         the ArgoReferenceTable.DACS dac indicator. If <i>null</i>
	 *                    the DATA_CENTRE cannont be
	 * @param singleCycle true = perform single-cycle test; false = accept
	 *                    multi-cycle files validated.
	 * @throws IOException If an I/O error occurs
	 */
	public boolean validateMetaData(int nProf, ArgoReferenceTable.DACS dac, boolean singleCycle) throws IOException {
		log.debug(".....validateMetaData: start.....");

		String[] plNum = readStringArr("PLATFORM_NUMBER");
		String dir = readString("DIRECTION", true);// ..true->return NULLs if present
		String[] ds = readStringArr("DATA_STATE_INDICATOR");
		String[] dc = readStringArr("DATA_CENTRE");
		String[] wmo = readStringArr("WMO_INST_TYPE");
		int[] cyc = readIntArr("CYCLE_NUMBER");
		String[] vert = readStringArr("VERTICAL_SAMPLING_SCHEME");

		String firstNum = new String(plNum[0].trim());
		int firstCyc = cyc[0];

		ArgoReferenceTable.ArgoReferenceEntry info;

		for (int n = 0; n < nProf; n++) {
			Boolean hasData = null;

			// .....PLATFORM_NUMBER.....

			log.debug("PLATFORM_NUMBER[{}]: '{}'", n, plNum[n]);

			String s = plNum[n].trim();
			if (!s.matches("[1-9][0-9]{4}|[1-9]9[0-9]{5}")) {
				validationResult.addError("PLATFORM_NUMBER[" + (n + 1) + "]: '" + s + "': Invalid");
			}

			if (singleCycle) {
				if (!s.equals(firstNum)) {
					// ..this isn't a single cycle file -- we're done
					log.debug("format error: requested single profile - platform number differs");
					validationResult.addError("File is not a single-cycle file (mulitple platforms)");
					return false;

				} else {
					// ..platform numbers are the same -- check cycle
					if (cyc[n] != firstCyc) {
						// ..this isn't a single cycle file -- we're done
						log.debug("format error: requested single profile - cycle number differs");
						validationResult.addError("File is not a single-cycle file (multiple cycles)");
						return false;
					}
				}
			}

			// .....DIRECTION.....

			log.debug("DIRECTION[{}]: '{}'", n, dir.charAt(n));
			if (dir.charAt(n) != 'A' && dir.charAt(n) != 'D') {
				validationResult.addError("DIRECTION[" + (n + 1) + "]: '" + dir.charAt(n) + "': Invalid");
			}

			// .....DATA_STATE_INDICATOR.....

			log.debug("DATA_STATE_INDICATOR[{}]: '{}'", n, ds[n]);

			s = ds[n].trim();
			if (s.length() == 0) {
				// ..set to _FillValue --- data must be missing
				// ..use PRES as a proxy - if all PRES is missing assume all data is missing

				boolean has_data = false;

				// ..not checked above

				float[] pres = readFloatArr("PRES", n);
				if (pres != null) {
					for (float d : pres) {
						if (!ArgoDataFile.is_99_999_FillValue(d)) {
							has_data = true;
							break;
						}
					}
				}

				hasData = has_data;
				log.debug("...data_state_indicator empty. searched PRES. has_data = {}", has_data);

				if (has_data) {
					validationResult.addError("DATA_STATE_INDICATOR[" + (n + 1) + "]: '" + s + "' Not set");
				}

			} else if (!(info = ArgoReferenceTable.DATA_STATE_INDICATOR.contains(s)).isActive) {
				validationResult.addError("DATA_STATE_INDICATOR[" + (n + 1) + "]: '" + s + "' Invalid");
			}

			// .....DATA_CENTRE.....

			log.debug("DATA_CENTRE[{}]: '{}'   DAC: '{}'", n, dc[n], dac);
			if (dac != null) {
				if (!ArgoReferenceTable.DacCenterCodes.get(dac).contains(dc[n].trim())) {
					validationResult
							.addError("DATA_CENTRE[" + (n + 1) + "]: '" + dc[n] + "': Invalid for DAC '" + dac + "'");
				}

			} else { // ..incoming DAC not set
				if (!ArgoReferenceTable.DacCenterCodes.containsValue(dc[n].trim())) {
					validationResult.addError("DATA_CENTRE[" + (n + 1) + "]: '" + dc[n] + "': Invalid (for all DACs)");
				}
			}

			// .....WMO_INST_TYPE.....

			log.debug("WMO_INST_TYPE[{}]: '{}'", n, wmo[n]); // ..ref_table 8
			s = wmo[n].trim();
			if (s.length() == 0) {
				validationResult.addError("WMO_INST_TYPE[" + (n + 1) + "]: Not set");
			} else {
				try {
					int N = Integer.valueOf(s);

					if ((info = ArgoReferenceTable.WMO_INST_TYPE.contains(N)).isValid()) {
						if (info.isDeprecated) {
							validationResult
									.addWarning("WMO_INST_TYPE[" + (n + 1) + "]: '" + s + "' Status: " + info.message);
						}
					} else {
						validationResult
								.addError("WMO_INST_TYPE[" + (n + 1) + "]: '" + s + "' Status: " + info.message);
					}

				} catch (Exception e) {
					validationResult.addError("WMO_INST_TYPE[" + (n + 1) + "]: '" + s + "' Invalid. Must be integer.");
				}
			} // end if (wmo)

			// .....VERTICAL_SAMPLING_SCHEME.....

			log.debug("VERTICAL_SAMPLING_SCHEME[{}]: '{}'", n, vert[n]); // ..ref_table 16
			s = vert[n].trim();
			if (s.length() == 0) {
				// ..set to _FillValue --- data must be missing
				// ..use PRES as a proxy - if all PRES is missing assume all data is missing

				boolean has_data = false;

				if (hasData == null) {
					// ..not checked above

					float[] pres = readFloatArr("PRES", n);
					if (pres != null) {
						for (float d : pres) {
							if (!ArgoDataFile.is_99_999_FillValue(d)) {
								has_data = true;
								break;
							}
						}
					}

					hasData = has_data;
					log.debug("...vertical_sampling_scheme empty. searched PRES. has_data = {}", has_data);

				} else {
					has_data = hasData;
					log.debug("...vertical_sampling_scheme empty. PRES already searched. has_data = {}", has_data);
				}

				if (has_data) {
					validationResult.addError("VERTICAL_SAMPLING_SCHEME[" + (n + 1) + "]: Not set");
				}

			} else {

				if ((info = ArgoReferenceTable.VERTICAL_SAMPLING_SCHEME.contains(s)).isValid()) {
					if (info.isDeprecated) {
						validationResult.addWarning("VERTICAL_SAMPLING_SCHEME[" + (n + 1) + "]: Status: " + info.message
								+ ": '" + s.trim() + "'");
					}

					if (n == 0) {
						if (!s.startsWith("Primary sampling")) {
							String err = String.format(
									"VERTICAL_SAMPLING_SCHEME[%d]: Profile number 1 must be 'Primary sampling': '%s'",
									(n + 1), s.trim());
							// validationResult.addError(err);

							// ################# TEMPORARY WARNING ################
							validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
							log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);
						}
					} else {
						if (s.startsWith("Primary sampling")) {
							String err = String.format(
									"VERTICAL_SAMPLING_SCHEME[%d]: Not profile 1.  Must NOT be 'Primary sampling': '%s'",
									(n + 1), s.trim());
							// validationResult.addError(err);

							// ################# TEMPORARY WARNING ################
							validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
							log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);
						}
					}

				} else {
					String err = String.format("VERTICAL_SAMPLING_SCHEME[%d]: Invalid: '%s'", (n + 1), s.trim());
					// validationResult.addError(err);

					// ################# TEMPORARY WARNING ################
					validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
					log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);
				}

			}
		}
		log.debug(".....validateMetaData: end.....");
		return true;
	}// ..end validateMetaData

	/**
	 * Validates the PARAM variables in the profile file.
	 * 
	 * PARAM variable checks:
	 * <ul>
	 * <li>DATA_MODE: Valid value
	 * <li>PARAM_DATA_MODE: Valid value
	 * <ul>
	 * <li>Valid values
	 * <li>Consistenct with DATA_MODE
	 * <li>Bio-profile: 'R' for 'PRES'
	 * <li>STATION_PARAMETERS: Checks
	 * <ul>
	 * <li>Valid values
	 * <li>Empty values within list
	 * <li>Duplicate entries in list
	 * <li>PARAM variables exist
	 * <li>All PARAM variables (with data) are in list
	 * </ul>
	 * <li>PARAM settings
	 * <ul>
	 * <li>If 'R': <i>param</i>_ADJUSTED not set for any parameters
	 * <li>If 'A' or 'D': <i>param</i>_ADJUSTED set for all parameters
	 * </ul>
	 * <li>***Not implemented*** PRES_ADJ_QC = 4: TEMP_ADJ_QC & PSAL_ADJ_QC = 4
	 * <li>N_PARAM: Consistent with data
	 *
	 * <li>**** Test has been disabled ***** N_LEVELS: Consistent with data
	 * </ul>
	 *
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param nProf  number of profiles in the file
	 * @param nParam number of parameters in the file
	 * @param nLevel number of levels in the file
	 * @throws IOException If an I/O error occurs
	 */
	public void validateParams(int nProf, int nParam, int nLevel) throws IOException {
		log.debug(".....validateParams: start.....");

		ArrayList<String> allowedParam = spec.getPhysicalParamNames(); // ..allowed <param>
		profParam = new ArrayList<ArrayList<String>>(nProf); // ..allowed <param>

		int maxParamUsed = -1; // ..max number of PARAMs used - all profiles
		// int maxLevelUsed = -1; //..max number of LEVELS with data - all profiles

		// ..read station parameters and data_mode
		String dMode = readString("DATA_MODE", true); // incl NULLs

		// ..need orgin/shape for 2-dim variables multiple time
		int[] origin2 = { 0, 0 }; // ..will anchor read to {profNum, level=0}
		int[] shape2 = { 1, nLevel }; // ..will read {1 profile, nLevel levels}

		// ...........MAIN LOOP: over each profile in the file.............
		for (int profNum = 0; profNum < nProf; profNum++) {

			String[] stParam = readStringArr("STATION_PARAMETERS", profNum);

			boolean fatalError = false;

			// ................check DATA_MODE.................

			char mode = dMode.charAt(profNum);

			if (log.isDebugEnabled()) {
				log.debug("...profile #" + profNum);
				log.debug("DATA_MODE[{}]: '{}'", profNum, mode);
			}

			if (mode != 'A' && mode != 'D' && mode != 'R') {
				validationResult.addError("DATA_MODE[" + (profNum + 1) + "]: '" + mode + "': Invalid");
			}

			char[] m = { mode };
			data_mode = new String(m);

			// .........check PARAMETER_DATA_MODE..............

			String param_mode = readString("PARAMETER_DATA_MODE", profNum, true);// incl NULLs

			if (param_mode != null) {
				// ..is included in this file: optional variable
				log.debug("PARAMETER_DATA_MODE[{}]: '{}'", profNum, param_mode);

				char final_mode = ' ';
				int final_nparam = -2;

				for (int paramNum = 0; paramNum < nParam; paramNum++) {
					char md = param_mode.charAt(paramNum);

					if (md != 'A' && md != 'D' && md != 'R' && md != ' ') {
						validationResult.addError("PARAMETER_DATA_MODE[" + (profNum + 1) + "," + (paramNum + 1) + "]: '"
								+ md + "': Invalid");
					}

					if (md == 'D' || final_mode == 'D') {
						if (final_mode != 'D') {
							final_mode = 'D';
							final_nparam = paramNum;
						}
					} else if (md == 'A' || final_mode == 'A') {
						if (final_mode != 'A') {
							final_mode = 'A';
							final_nparam = paramNum;
						}
					} else if (md != ' ') {
						if (final_mode == ' ') {
							final_mode = 'R';
							final_nparam = paramNum;
						}
					}

					// ..check bio-prof file: PARAM_DATA_MODE("PRES") == "R"

					if (fileType == FileType.BIO_PROFILE && stParam[paramNum].trim().startsWith("PRES")) {
						if (md != 'R') {
							log.debug("PRES[{}]: PARAMETER_DATA_MODE[{},{}] = '{}'. must be 'R'", profNum, profNum,
									paramNum, md);
							validationResult.addError("PRES[" + (profNum + 1) + "]: PARAMETER_DATA_MODE["
									+ (profNum + 1) + "," + (paramNum + 1) + "]: '" + md + "': Must be 'R'");
						}
					}
				} // ..end for (paramNum)

				// ..check for conformity of DATA_MODE and PARAMETER_DATA_MODE

				if (final_mode == ' ') {
					// ..all param_data_mode = ' '. data_mode better be 'R'
					if (mode != 'R') {
						validationResult.addWarning("DATA_MODE[" + (profNum + 1) + "] not 'R'. PARAMETER_DATA_MODE["
								+ (profNum + 1) + ",...] all ' ': Inconsistent");
						log.debug("data_mode[{}] != 'R'. All param_data_mode[{},...] are ' '", profNum, profNum);
					}

				} else if (final_mode != mode) {
					// ..some param_data_mode was set. not consistent with data_mode
					validationResult.addError("DATA_MODE[" + (profNum + 1) + "]/PARAMETER_DATA_MODE[" + (profNum + 1)
							+ "," + (final_nparam + 1) + "]: '" + mode + "'/'" + final_mode + "': Inconsistent");
					log.debug("data_mode[{}]/param_data_mode[{},{}] inconsistent", profNum, profNum, final_nparam);
				}

			} // ..end if (param_mode != null)

			// .........check CONFIG_MISSION_NUMBER............

			int msnNum = readInt("CONFIG_MISSION_NUMBER", profNum);

			log.debug("CONFIG_MISSION_NUMBER[{}]: '{}'", profNum, msnNum);

			if (msnNum == 99999) {
				int cyc = readInt("CYCLE_NUMBER", profNum);
				if (cyc != 0 && mode == 'D') {
					validationResult.addError("CONFIG_MISSION_NUMBER[" + (profNum + 1) + "]: '" + msnNum
							+ "': Cannot be FillValue in D-mode");
					log.warn("CONFIG_MISSION_NUMBER[" + (profNum + 1) + "]: '" + msnNum
							+ "': Cannot be FillValue in D-mode");
				}
			} // end if (msnNum ...)

			// ................check STATION_PARAMETER..............

			HashMap<String, Character> pDMode = new HashMap<String, Character>();

			profParam.add(profNum, new ArrayList<String>());

			boolean embeddedEmpty = false;
			int last_empty = -1; // ..index of last empty STATION_PARAMETER found for this profile
			int nParamUsed = 0; // ..number of PARAMs used for this profile
			StringBuilder paramList = new StringBuilder();

			for (int paramNum = 0; paramNum < nParam; paramNum++) {
				// ..get the PARAM name[profNum,paramNum]
				String param = stParam[paramNum].trim();
				if (log.isDebugEnabled()) {
					log.debug("STATION_PARAMETER[" + profNum + "," + paramNum + "]: '" + param + "'");
				}

				// ..build list of <param> names for reporting
				addToList(paramList, param);

				// ..check <param> name
				if (param.length() == 0) {
					// ..this param is empty
					last_empty = paramNum;

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

						if (profParam.get(profNum).contains(param)) {
							// ..this is a duplicate entry

							validationResult.addError("STATION_PARAMETERS[" + (profNum + 1) + "," + (paramNum + 1)
									+ "]: '" + param + "': Duplicate entry");

						} else {
							// ..add to list of <param> for this profile
							profParam.get(profNum).add(param);
							nParamUsed++;
						}

						if (spec.isDeprecatedPhysicalParam(param)) {
							// ..this is a deprecated parameter name

							validationResult.addWarning("STATION_PARAMETERS[" + (profNum + 1) + "," + (paramNum + 1)
									+ "]: '" + param + "': Deprecated parameter name");
						}

					} else {
						// ..<param> is illegal
						validationResult.addError("STATION_PARAMETERS[" + (profNum + 1) + "," + (paramNum + 1) + "]: '"
								+ param + "': Invalid parameter name in this context");
					}

					// ..decide on the final "mode" for this param
					if (param_mode == null) {
						pDMode.put(param, mode);
						log.debug("mode set from DATA_MODE (no PARAM_DATA_MODE): '{}'", mode);

					} else {
						char md = param_mode.charAt(paramNum);
						if (md == ' ') {
							pDMode.put(param, Character.valueOf(mode));
							log.debug("mode set from DATA_MODE (PARAM_DATA_MODE blank): '{}'", mode);
						} else {
							pDMode.put(param, Character.valueOf(md));
							log.debug("mode set from PARAMETER_DATA_MODE: '{}'", md);
						}
					}
				}

			} // ..end for nParam

			if (log.isDebugEnabled()) {
				log.debug("paramList[" + profNum + "]: " + paramList);
			}

			if (nParamUsed > maxParamUsed) {
				maxParamUsed = nParamUsed;
			}

			// ..report errors and warnings
			if (embeddedEmpty) {
				validationResult.addWarning("STATION_PARAMETERS[" + (profNum + 1) + ",*]: Empty entries in list"
						+ "\n\tList: " + paramList);
			}

			// ..check that all required parameters are defined in STATION_PARAMETERS
			// ..the parameters are only required in profile 0
			if (profNum == 0) {
				for (String p : allowedParam) {
					if (!spec.isOptional(p) && !profParam.get(profNum).contains(p)) {
						validationResult.addError("STATION_PARAMETERS[" + (profNum + 1) + ",*]: Required PARAM ('" + p
								+ "') not specified");
						fatalError = true;
					}
				} // ..end for (required parameters)
			}

			// ......check that all STATION_PARAMETERS have <param> variable........
			for (String p : profParam.get(profNum)) {
				Variable var = findVariable(p);
				if (var == null) {
					validationResult.addError("STATION_PARAMETERS[" + (profNum + 1) + ",*]: PARAM '" + p
							+ "' specified. Variables not in data file.");
					fatalError = true;
				}
			} // ..end profParam

			// ...check whether all <param> with data are in STATION_PARAMETERS...

			origin2[0] = profNum; // ..origin2 = {profNum, 0};
									// ..shape2 = {1, nLevel};

			for (String p : allowedParam) {
				if (!profParam.get(profNum).contains(p)) {
					// ..STATION_PARAMETERS does NOT include this param,
					// ..see if the file contains the variable

					boolean hasData = false;
					Variable var = findVariable(p);
					if (var != null) {
						log.debug(p + "[{}]: in file. Not in STATION_PARAMETERS", profNum);

						// ..file includes this variable:
						// ..see if *any* of the values are non-fill for this profNum

						// ..this variable could be:
						// .. [float|double] <param> (nProf, nLevel[, extra-dim, ...])

						// ..what is needed: a long string of numbers that represent
						// ..the array section N_PROF N_LEVELS extra-dimensions
						// .. [profNum, :, :, :]
						// ..just one long string of numbers for all the values of one
						// ..for one specific profNum

						// ..NOTE: we don't care about level-by-level here

						int vRank = var.getRank();
						int[] vShape = var.getShape();

						int[] origin = new int[vRank];
						int[] shape = vShape;

						Arrays.fill(origin, 0);

						origin[0] = profNum;
						shape[0] = 1;

						// ..read the section of the array
						Array array;
						try {
							array = var.read(origin, shape);

						} catch (Exception e) {
							stderr.println(e.getMessage());
							e.printStackTrace(stderr);
							throw new IOException("Unable to read '" + p + "'");
						}

						/*
						 * log.debug("(temp) hasData? origin {}", Arrays.toString(origin));
						 * log.debug("(temp) hasData? shape {}", Arrays.toString(shape));
						 * log.debug("(temp) hasData? array.shape {}",
						 * Arrays.toString(array.getShape()));
						 */

						// ..will create either an ArrayFloat or ArrayDouble
						// ..1-d array for all the levels and possible extra-dimesion

						// ..we know (or assume we know) that _FillValue exists because
						// ..the file has passed the format check ..... right????

						DataType type = var.getDataType();
						Number fillValue = var.findAttribute("_FillValue").getNumericValue();

						if (type == DataType.DOUBLE) {
							double[] data = (double[]) array.copyTo1DJavaArray();
							double fVal = fillValue.doubleValue();

							for (double d : data) {
								if (!ArgoDataFile.is_FillValue(fVal, d)) {
									hasData = true;
									break;
								}
							}

						} else if (type == DataType.FLOAT) {
							float[] data = (float[]) array.copyTo1DJavaArray();
							float fVal = fillValue.floatValue();

							for (float d : data) {
								if (!ArgoDataFile.is_FillValue(fVal, d)) {
									hasData = true;
									break;
								}
							}

						} else if (type == DataType.SHORT) {
							short[] data = (short[]) array.copyTo1DJavaArray();
							short fVal = fillValue.shortValue();

							for (short d : data) {
								if (d != fVal) {
									hasData = true;
									break;
								}
							}

						} else {
							throw new IOException("Invalid data type for '" + p + "' (validateParams)");
						}

						if (hasData) {
							validationResult.addError("STATION_PARAMETERS[" + (profNum + 1) + ",*]: Does not specify '"
									+ p + "'. Variable contains data.");
							log.debug("{}[{}]: has data", p, profNum);
						} else {
							log.debug("{}[{}]: no data", p, profNum);
						}
					} // ..end if var
				} // ..end if profParam.contains(param)
			} // ..end for allowedParam

			if (fatalError) {
				log.debug("validation stopped after STATION_PARAMETER checks");

				// ..type of error above blocks further checking
				continue;
			}

			// .....................................................
			// .............check <param> / _QC.....................
			// .....................................................
			// ..
			checkParamParamQC(nLevel, origin2, shape2, profNum, mode, pDMode);
		} // ..end for nProf

		// ..check if N_PARAM is set too large.
		maxParamUsed++; // maxLevelUsed++; //..convert from max index to max number

		if (maxParamUsed < nParam) {
			validationResult.addWarning("N_PARAM: Larger than necessary." + "\n\tN_PARAM     = " + nParam
					+ "\n\tPARAMs used = " + maxParamUsed);
		}

		// ..check if N_LEVEL is set too large.
		/*
		 * if (maxLevelUsed < nLevel) {
		 * validationResult.addWarning("N_LEVEL: Larger than necessary."+
		 * "\n\tN_LEVEL                  = "+nLevel+
		 * "\n\tMaximum levels with data = "+maxLevelUsed); }
		 */

		log.debug(".....validateParams: end.....");
	}// ..end validateParams

	private void checkParamParamQC(int nLevel, int[] origin2, int[] shape2, int profNum, char mode,
			HashMap<String, Character> pDMode) throws IOException {
		float[] prm;
		char[] prm_qc;
		float fValue;

		// ........check <param> and <param>_QC..........

		PARAM_LOOP: for (String param : profParam.get(profNum)) {
			String varName = param.trim();
			Variable var = findVariable(varName);
			Variable varQC = findVariable(varName.trim() + "_QC");
			Variable profVarQC = findVariable("PROFILE_" + varName + "_QC");

			Number fillValue = var.findAttribute("_FillValue").getNumericValue();
			fValue = fillValue.floatValue();

			log.debug("<param>/_QC check: '{}'", varName);

			// ..The exception: In bio-profiles:
			// .. PRES will not have _QC and PROFILE_.._QC
			// .. Intermediate parameters may not have them
			// .. Assume the format checking determined which vars had to exist
			// .. If _QC exists the other must too, and so can be tested.
			// .. IF _QC doesn't exist, NONE of the "qc variables" (inc _ADJUSTED)
			// .. Skip all of these tests if _QC doesn't exist

			if (varQC == null) {
				log.debug(varName + "_QC not in file: skip remaining tests");
				continue PARAM_LOOP;
			}

			boolean paramErr = false;
			boolean param_adjErr = false;

			// ..the possible <param> data configurations:
			// .. <type> <param> (nProf, nLevel[, extra-dimensions])

			// ..what is needed: level-by-level is there any Nan, any not-fill, or is it all
			// fill
			// ..-> the data type doesn't matter (NaN can only be in "real" types)
			// ..do all checks as "floats"

			// ..for the possible extra dimensions we want to consider
			// ..all of the values for a given level as one
			// ..that is, for the level, are any of the values NaN or not-fill
			// .. or are they all fill

			int vRank = var.getRank();
			int[] vShape = var.getShape();

			int[] origin = new int[vRank];
			int[] shape = vShape;

			Arrays.fill(origin, 0);

			origin[0] = profNum;
			shape[0] = 1;

			// ..read the section of the array
			Array array;
			try {
				array = var.read(origin, shape);

			} catch (Exception e) {
				stderr.println(e.getMessage());
				e.printStackTrace(stderr);
				throw new IOException("Unable to read '" + param + "'");
			}

			/*
			 * log.debug("(temp) whatData? origin {}", Arrays.toString(origin));
			 * log.debug("(temp) whatData? shape {}", Arrays.toString(shape));
			 * log.debug("(temp) whatData? array.shape {}",
			 * Arrays.toString(array.getShape()));
			 */

			// ..by far the most common situation is (nProf, nLevel)
			// ..deal with that first

			if (vRank == 2) {
				// ..this is just a standard (nProf, nLevel) array
				// ..convert it to a level-by-level array

				// ..the exact values don't matter, just whether they are
				// ..nan, fill value, or not. So do it all as "float"

				DataType type = var.getDataType();

				if (type == DataType.FLOAT) {
					prm = (float[]) array.copyTo1DJavaArray();
					log.debug("...rank 2 float var: use copyTo1DJavaArray");
					// log.debug("(temp) prm: {}", Arrays.toString(prm));

				} else {
					// ..for other than FLOAT, we have to cast it level-by-level

					prm = new float[nLevel];
					Index index = array.getIndex();

					for (int k = 0; k < nLevel; k++) {
						index.set(0, k);
						prm[k] = array.getFloat(index);
					}

					log.debug("...rank 2 unspecified-type var: level-by-level cast");
					// log.debug("(temp) prm: {}", Arrays.toString(prm));
				}

			} else { // ..rank is 3 or greater
				log.debug("...rank {} variable", vRank);

				if (vRank > 3) {
					// ..collapse all the extra-dimensions into 1
					// ..the shape is [1, nLevel, i, j, k, ..]
					// ..the new shape will be [1, nLevel, i*j*k*..]

					log.debug("collapsing extra-dimensions into one");

					int[] newShape = new int[3];
					newShape[0] = 1;
					newShape[1] = vShape[1];
					newShape[2] = vShape[2];

					for (int k = 3; k < vRank; k++) {
						newShape[2] *= vShape[k];
					}

					log.debug("...original shape {}", Arrays.toString(array.getShape()));

					array = array.reshapeNoCopy(newShape);

					log.debug("...new shape      {}", Arrays.toString(array.getShape()));
				}

				prm = new float[nLevel];
				Index index = array.getIndex();

				// ..loop over levels
				for (int k = 0; k < nLevel; k++) {
					// ..loop over extra-dimension and find if it has
					// .. any NaNs, any non-FillValue

					boolean is_nan = false;
					float data = Float.MAX_VALUE;

					for (int i = 0; i < vShape[2]; i++) {
						index.set(0, k, i);
						float f = array.getFloat(index);

						if (Float.isNaN(f)) {
							is_nan = true;
						} else if (!ArgoDataFile.is_FillValue(fValue, f)) {
							data = f;
						}
					}

					if (is_nan) {
						prm[k] = Float.NaN;
					} // ..NaN trumps everything
					else if (data != Float.MAX_VALUE) {
						prm[k] = data;
					} // ..had data
					else {
						prm[k] = fValue;
					}
				}
				// log.debug("(temp) prm: {}", Arrays.toString(prm));
			}

			// ..<param>_QC: this is always char <param>_QC (nProf, nLevel)

			try {
				prm_qc = (char[]) varQC.read(origin2, shape2).copyTo1DJavaArray();
			} catch (InvalidRangeException e) {
				stderr.println("validateParams: Invalid range in read");
				stderr.println(e.getMessage());
				e.printStackTrace(stderr);
				throw new IOException("Unable to read " + varName + "_QC[" + profNum + "]: InvalidRangeException");

			} catch (IOException e) {
				stderr.println(e.getMessage());
				e.printStackTrace(stderr);
				throw new IOException("Unable to read " + varName + "_QC[" + profNum + "]: InvalidRangeException");
			}

			char profQC = ((ArrayChar.D1) profVarQC.read()).get(profNum);

			int depQC = 0; // ..count of "deprecated QC"
			int illQC = 0; // ..count of "illegal QC"
			int invQC = 0; // ..count of "invalid QC"
			int notMiss = 0; // ..count of QC not set missing when data is missing
			int notNotMeas = 0; // ..count of QC "not measured" when data is NOT missing
			int nan = 0; // ..count of NANs
			int inf = 0; // count of Inf values
			int noQC = 0; // ..count of "no QC"
			int n_data = 0; // ..number of data values (based on QC code)
			int n_good = 0; // ..number of good values (based on QC code)
			int n_noqc = 0; // ..number of no-qc values

			for (int k = 0; k < prm.length; k++) {
				// if (! ArgoDataFile.is_99_999_FillValue(prm[k]) && k > maxLevelUsed)
				// maxLevelUsed = k;
				if (Float.isNaN(prm[k])) {
					nan++;
				}

				if (Float.isInfinite(prm[k])) {
					inf++;
				}

				ArgoReferenceTable.ArgoReferenceEntry info;

				if ((info = ArgoReferenceTable.QC_FLAG.contains(prm_qc[k])).isValid()) {
					// ..valid QC flag (NOT " ")

					if (info.isDeprecated) {
						depQC++;
					}

					if (ArgoDataFile.is_FillValue(fValue, prm[k])) {
						// ..data is missing - QC better be too
						if (prm_qc[k] != '9' && prm_qc[k] != '0') {
							notMiss++;
						}

					} else {
						// ..data not missing - check QC value

						if (prm_qc[k] == '0') {
							if (!spec.isOptional(varName)) {
								noQC++;
							}
						} else if (prm_qc[k] > '4') {
							illQC++;
						}
					}

					// ..count the good, bad, and ugly (for profile QC)
					if (prm_qc[k] != '9' && prm_qc[k] != ' ') {
						n_data++;
					}
					if (prm_qc[k] == '1' || prm_qc[k] == '2' || prm_qc[k] == '5' || prm_qc[k] == '8') {
						n_good++;
					} else if (prm_qc[k] == '0') {
						n_noqc++;
					}

				} else { // ..QC not is ref table 2, handle " " special case
					if (prm_qc[k] == ' ') {
						// ..qc set to NOT MEASURED, data better be missing
						if (!ArgoDataFile.is_FillValue(fValue, prm[k])) {
							notNotMeas++;
						}
					} else {
						// ..not in QCFlag (not in ref table 2) and not " "
						// ..invalid QC flag
						invQC++;
					}
				}
			} // ..end for (k)

			if (log.isDebugEnabled()) {
				log.debug("checking {}[{}]: invQC, illQC, noQC, notMiss, nan, depQC = " + "{}, {}, {}, {}, {}, {}",
						varName, profNum, invQC, illQC, noQC, notMiss, nan, depQC);
				log.debug("checking {}[{}]: n_data, n_good, n_noqc, = {}, {}, {}", varName, profNum, n_data, n_good,
						n_noqc);
			}

			// ..report errors and warnings
			if (invQC > 0) {
				paramErr = true;
				validationResult.addError(varName + "_QC[" + (profNum + 1) + "]: Invalid QC codes at " + invQC
						+ " levels (of " + prm.length + ")");
			}
			if (illQC > 0) {
				paramErr = true;
				validationResult.addError(varName + "_QC[" + (profNum + 1) + "]: QC codes not '1' to '4' at " + illQC
						+ " levels with data (of " + prm.length + ")");
			}
			if (depQC > 0) {
				validationResult.addWarning(varName + "_QC[" + (profNum + 1) + "]: Deprecated QC codes at " + depQC
						+ " levels (of " + prm.length + ")");
			}
			if (noQC > 0) {
				paramErr = true;
				validationResult.addError(varName + "_QC[" + (profNum + 1) + "]: QC code '0' at " + noQC
						+ " levels (of " + prm.length + ")");
			}
			if (notMiss > 0) {
				paramErr = true;
				validationResult.addError(varName + "_QC[" + (profNum + 1) + "]: Missing data but QC not missing at "
						+ notMiss + " levels (of " + prm.length + ")");
			}
			if (notNotMeas > 0) {
				paramErr = true;
				validationResult
						.addError(varName + "_QC[" + (profNum + 1) + "]: Blank (' ') QC when data is not missing at "
								+ notNotMeas + " levels (of " + prm.length + ")");
			}
			if (fileType == FileType.PROFILE) {
				// ..in a core-file, only intermediate params can have 0 QC
				if (!spec.isInterPhysParam(varName)) {
					if (n_noqc > 0) {
						// .._QC can't be 0 in a core-file
						paramErr = true;
						validationResult.addError(varName + "_QC[" + (profNum + 1) + "]: QC code '0' at " + n_noqc
								+ " levels (of " + prm.length + ")");
					}
				}
			}
			if (nan > 0) {
				validationResult.addError(
						varName + "[" + (profNum + 1) + "]: NaNs at " + nan + " levels (of " + prm.length + ")");
			}

			if (inf > 0) {
				validationResult.addError(varName + "[" + (profNum + 1) + "]: Infinite value at " + inf + " levels (of "
						+ prm.length + ")");
			}

			// ..............check <param>_ADJUSTED and _ADJUSTED_QC..............
			if (!paramErr) {
				// ..no errors in param -- check _adjusted

				varName = param.trim() + "_ADJUSTED";
				var = findVariable(varName);
				varQC = findVariable(varName + "_QC");
				Variable varErr = findVariable(varName + "_ERROR");

				// ..fillValue is the same for prm, prm_adj, prm_adj_err
				// ..Number fillValue = var.findAttribute("_FillValue").getNumericValue();

				// ..The next level of the bio-profile exception:
				// .. PRES will not have _ADJUSTED variables
				// .. Intermediate parameters may not have them
				// .. Assume the format checking determined which vars had to exist
				// .. If _ADJUSTED exists the other must too, and so can be tested.
				// .. IF _QC doesn't exist, NONE of the "ADJ variables" do
				// .. Skip all of these tests if _QC doesn't exist

				if (varQC == null) {
					log.debug(varName + "_QC not in file: skip remaining tests");
					continue PARAM_LOOP;
				}

				float[] prm_adj;
				char[] prm_adj_qc;
				float[] prm_adj_err;

				// ..see comments above for the <param>/_QC checks
				// ..regarding the reasoning behind these rank/origin settings

				vRank = var.getRank();
				vShape = var.getShape();

				origin = new int[vRank];
				shape = vShape;

				Arrays.fill(origin, 0);

				origin[0] = profNum;
				shape[0] = 1;

				try {
					array = var.read(origin, shape);

				} catch (Exception e) {
					stderr.println(e.getMessage());
					e.printStackTrace(stderr);
					throw new IOException("Unable to read " + varName + "[" + profNum + "]  (validateParams)");
				}

				/*
				 * log.debug("(temp) _adj whatData? origin {}", Arrays.toString(origin));
				 * log.debug("(temp) _adj whatData? shape {}", Arrays.toString(shape));
				 * log.debug("(temp) _adj whatData? array.shape {}",
				 * Arrays.toString(array.getShape()));
				 */

				// ..the most common (by far) is float <param> (nProf, nLevel)
				// ..so let's deal with that situation the easiest way possible

				DataType type = var.getDataType();

				// ..by far the most common situation is (nProf, nLevel)
				// ..deal with that first

				if (vRank == 2) {
					// ..this is just a standard (nProf, nLevel) array
					// ..convert it to a level-by-level array

					if (type == DataType.FLOAT) {
						prm_adj = (float[]) array.copyTo1DJavaArray();

						log.debug("...rank 2 float var: use copyTo1DJavaArray");
						// log.debug("(temp) prm_adj: {}", Arrays.toString(prm_adj));

					} else {
						// ..for other than float, we have to cast it level-by-level

						prm_adj = new float[nLevel];
						Index index = array.getIndex();

						for (int k = 0; k < nLevel; k++) {
							index.set(0, k);
							prm_adj[k] = array.getFloat(index);
						}

						log.debug("...rank 2 double var: level-by-level cast");
						// log.debug("(temp) prm_adj: {}", Arrays.toString(prm_adj));
					}

				} else { // ..rank is 3 or greater
					log.debug("...rank {} variable", vRank);

					if (vRank > 3) {
						// ..collapse all the extra-dimensions into 1
						// ..the shape is [1, nLevel, i, j, k, ..]
						// ..the new shape will be [1, nLevel, i*j*k*..]

						log.debug("collapsing extra dimensions into one");

						int[] newShape = new int[3];
						newShape[0] = 1;
						newShape[1] = vShape[1];
						newShape[2] = vShape[2];

						for (int k = 3; k < vRank; k++) {
							newShape[2] *= vShape[k];
						}

						log.debug("...original shape {}", Arrays.toString(array.getShape()));

						array = array.reshapeNoCopy(newShape);

						log.debug("...new shape      {}", Arrays.toString(array.getShape()));
					}

					prm_adj = new float[nLevel];
					Index index = array.getIndex();

					// ..fillValue is the same for prm, prm_adj, prm_adj_err
					// ..float fValue = fillValue.floatValue();
					// ..loop over levels
					for (int k = 0; k < nLevel; k++) {
						// ..loop over extra-dimension and find if it has
						// .. any NaNs, any non-FillValue

						boolean is_nan = false;
						float data = Float.MAX_VALUE;

						for (int i = 0; i < vShape[2]; i++) {
							index.set(0, k, i);
							float f = array.getFloat(index);

							if (Float.isNaN(f)) {
								is_nan = true;
							} else if (!ArgoDataFile.is_FillValue(fValue, f)) {
								data = f;
							}
						}

						if (is_nan) {
							prm_adj[k] = Float.NaN;
						} // ..NaN trumps everything
						else if (data != Float.MAX_VALUE) {
							prm_adj[k] = data;
						} // ..had data
						else {
							prm_adj[k] = fValue;
						}
					}
					// log.debug("(temp) prm_adj: {}", Arrays.toString(prm));
				}

				// ..<param>_ADJUSTED_ERROR: always the standard (N_PROF, N_LEVEL)

				try {
					array = varErr.read(origin2, shape2);

				} catch (Exception e) {
					stderr.println(e.getMessage());
					e.printStackTrace(stderr);
					throw new IOException("Unable to read " + varName + "[" + profNum + "]  (validateParams)");
				}

				// ..the most common (by far) is float <param> (nProf, nLevel)
				// ..so let's deal with that situation the easiest way possible

				type = varErr.getDataType();

				if (type == DataType.FLOAT) {

					prm_adj_err = (float[]) array.copyTo1DJavaArray();

					log.debug("...rank 2 float var: use copyTo1DJavaArray");
					// log.debug("(temp) prm_adj_err: {}", Arrays.toString(prm_adj_err));

				} else {
					// ..for DOUBLE, we have to cast it level-by-level

					prm_adj_err = new float[nLevel];
					Index index = array.getIndex();

					for (int k = 0; k < nLevel; k++) {
						index.set(0, k);
						prm_adj_err[k] = array.getFloat(index);
					}

					log.debug("...rank 2 double var: level-by-level cast");
					// log.debug("(temp) prm_adj_err: {}", Arrays.toString(prm_adj));
				}

				// ..<param>_ADJUSTED_QC: this is always char <param>_QC (nProf, nLevel)

				try {
					prm_adj_qc = (char[]) varQC.read(origin2, shape2).copyTo1DJavaArray();
				} catch (InvalidRangeException e) {
					throw new IOException("Unable to read " + varName + "_QC[" + profNum + "]: InvalidRangeException");
				}

				// ...... in real-time file --- *_adj, *_adj_err, *_adj_qc all missing .....

				mode = pDMode.get(param).charValue();

				log.debug("mode = '{}'", mode);

				if (mode == 'R') {
					paramErr = checkParamAdjusted_When_DataModeIsR_CHECK_PROFILE_0021(prm_adj, prm_adj_err, prm_adj_qc,
							fValue, mode, varName, profNum, prm);

					// ..... do checks for A and D files ........
				} else {
					// TO DO : refactor this -> same methods than line 198X for raw PARAM
					// ..mode is 'A' or 'D': all <PARAM>s must have _ADJUSTED set

					int errMiss = 0; // ..count of ERROR that should NOT have been missing
					int errNotMiss = 0; // ..count of ERROR not missing when param is missing
					int incNotMeas = 0; // ..count of incompatible QC = blank
					depQC = 0; // ..count of "deprecated QC"
					invQC = 0; // ..count of "invalid QC"
					int missAdj = 0;// ..count of param_adj missing / param not missing
					int mismatchAdjErr = 0; // ..count of Adj vs Err mis-matched values set
					int missMiss = 0;// ..count of param_adj_qc missing / param_qc not missing
					int missNot = 0;// ..count of param_adj not missing / QC = 4 or 9
					int missPrm = 0;// ..count of param missing / param_adj not missing
					nan = 0; // ..count of NANs
					int nanErr = 0; // ..count of NANs in _error
					inf = 0; // count of Inf.
					int infErr = 0; // count of Inf in _error.
					notNotMeas = 0; // ..cound of adj_qc set not measured / qc set or data not missing
					n_data = 0; // ..count of data values (based on QC code)
					n_good = 0; // ..number of good values (based on QC code)
					n_noqc = 0; // ..number of no-qc values (QC code = 0)
					int qcNotMiss = 0; // ..count of ADJ_QC that should have been missing

					/*
					 * 2021-02: A-mode files can now have _ADJUSTED_ERROR set int errNotMissAmode =
					 * 0; //..count of ERROR not missing in A-mode file
					 */

					for (int k = 0; k < prm_adj.length; k++) {
						if (Float.isNaN(prm_adj[k])) {
							nan++;
						}
						if (Float.isNaN(prm_adj_err[k])) {
							nanErr++;
						}

						if (Float.isInfinite(prm_adj[k])) {
							inf++;
						}
						if (Float.isInfinite(prm_adj_err[k])) {
							infErr++;
						}

						// ..check the per level QC flag
						ArgoReferenceTable.ArgoReferenceEntry info;

						info = ArgoReferenceTable.QC_FLAG.contains(prm_adj_qc[k]);
						if (info.isValid()) {
							if (info.isDeprecated) {
								depQC++;
							}

						} else {
							if (prm_adj_qc[k] != ' ') {
								// ..invalid QC flag
								invQC++;
							}
						}

						// ..check special case of adj_qc = ' '
						if (prm_qc[k] == ' ' || prm_adj_qc[k] == ' ') {
							// ..one is missing, both must be
							if (prm_qc[k] != ' ' || prm_adj_qc[k] != ' ') {
								incNotMeas++;
							} else {

								if (!ArgoDataFile.is_FillValue(fValue, prm_adj[k])) {
									notNotMeas++;
								}
							}

						} else {
							// ..check if param (not param_adj!) is missing
							if (ArgoDataFile.is_FillValue(fValue, prm[k])) {
								// .....param is missing.....

								if (!ArgoDataFile.is_FillValue(fValue, prm_adj[k])) {
									// ..param_adjusted is NOT missing - error
									missPrm++;
								}
								if (!ArgoDataFile.is_FillValue(fValue, prm_adj_err[k])) {
									// ..param_adjusted_error is NOT missing - error
									errNotMiss++;
								}
								if (prm_adj_qc[k] != '9') {
									// ..param_adjusted_qc is NOT missing - error
									qcNotMiss++;
								}

							} else {
								// .....param is NOT missing......

								if (ArgoDataFile.is_FillValue(fValue, prm_adj[k])) {
									// ..param_adj is missing - QC must be 4 or 9
									if (prm_adj_qc[k] != '4') {
										if (prm_adj_qc[k] != '9') {
											// ..adj_qc is NOT 4 or 9
											missAdj++;

										} else { // ..adj_qc is 9, qc better be too
											if (prm_qc[k] != '9') {
												missMiss++;
											}
										}
									}
									if (!ArgoDataFile.is_FillValue(fValue, prm_adj_err[k])) {
										errNotMiss++;
									}

								} else {
									// ..param_adj is NOT missing
									if (prm_adj_qc[k] == '4' || prm_adj_qc[k] == '9') {
										if (mode == 'D') {
											missNot++;

										} else { // ..mode == 'A'
											if (prm_adj_qc[k] == '9') {
												missNot++;
											}
										}

									} else {
										if (mode == 'D') {
											if (is_99_999_FillValue(prm_adj_err[k])) {
												errMiss++;
											}

											/*
											 * 2021-02: A-mode files can now have _ADJUSTED_ERROR set Remove this test
											 * --- per e-mails from JP Rannou and Annie //} else {
											 */
										}
									}
								} // ..end if (param_adj is missing)
							} // ..end if (param_adj_qc is not ' ')
						} // ..end if (param[k] is missing)

						// ..count the good the bad and the ugly
						if (prm_adj_qc[k] != '9' && prm_adj_qc[k] != ' ') {
							n_data++;
						}
						if (prm_adj_qc[k] == '1' || prm_adj_qc[k] == '2' || prm_adj_qc[k] == '5'
								|| prm_adj_qc[k] == '8') {
							n_good++;
						} else if (prm_adj_qc[k] == '0') {
							n_noqc++;
						}

						// ..check for a mis-match of "values set" for _adj or _adj_err
						// .. "^" is the xor operator
						if (is_FillValue(fValue, prm_adj[k]) ^ is_FillValue(fValue, prm_adj_err[k])) {
							mismatchAdjErr++;
						}

					} // ..end for (k)

					if (log.isDebugEnabled()) {
						log.debug(
								"checking {}[{}]: invQC, missAdj, missNot, missPrm, "
										+ "qcNotMiss, notNotMeas, depQC = {}, {}, {}, {}, {}, {}, {}",
								varName, profNum, invQC, missAdj, missNot, missPrm, qcNotMiss, notNotMeas, depQC);
						log.debug("checking {}[{}]: errNotMiss, errMiss, " + "nan, nanErr = {}, {}, {}, {}", varName,
								profNum, errNotMiss, errMiss, nan, nanErr);
						log.debug("checking {}[{}]: n_data, n_good, n_noqc, mismatchAdjErr " + "= {}, {}, {}, {}",
								varName, profNum, n_data, n_good, n_noqc, mismatchAdjErr);
					}

					// ..report errors and warnings
					if (invQC > 0) {
						param_adjErr = true;
						validationResult.addError(varName + "_QC[" + (profNum + 1) + "]: Invalid QC codes at " + invQC
								+ " levels (of " + prm_adj.length + ")");
					}
					if (depQC > 0) {
						validationResult.addWarning(varName + "_QC[" + (profNum + 1) + "]: Deprecated QC codes at "
								+ depQC + " levels (of " + prm_adj.length + ")");
					}
					if (incNotMeas > 0) {
						param_adjErr = true;
						validationResult.addError("DATA_MODE '" + mode + "': " + varName + "_QC[" + (profNum + 1)
								+ "]: Incompatible blank (' ') QC codes at " + incNotMeas + " levels (of "
								+ prm_adj.length + ")");
					}
					if (notNotMeas > 0) {
						param_adjErr = true;
						validationResult
								.addError(varName + "_QC[" + (profNum + 1) + "]: Blank (' ') when data not missing at "
										+ notNotMeas + " levels (of " + prm_adj.length + ")");
					}
					if (n_noqc > 0 && fileType == FileType.PROFILE) {
						// .._QC can't be 0 in a core-file
						param_adjErr = true;
						validationResult.addError(varName + "_QC[" + (profNum + 1) + "]: QC code '0' at " + n_noqc
								+ " levels (of " + prm_adj.length + ")");
					}
					if (missAdj > 0) {
						param_adjErr = true;
						validationResult.addError("DATA_MODE '" + mode + "': " + param + "[" + (profNum + 1)
								+ "] Not missing / " + varName + " Missing (QC not 4 or 9): At " + +missAdj
								+ " levels (of " + prm_adj.length + ")");
					}
					if (missMiss > 0) {
						param_adjErr = true;
						validationResult.addError(varName + "_QC[" + (profNum + 1) + "] Missing / " + param
								+ "_QC Not missing: At " + missMiss + " levels (of " + prm_adj.length + ")");
					}
					if (missNot > 0) {
						param_adjErr = true;
						validationResult.addError("DATA_MODE '" + mode + "': " + varName + "[" + (profNum + 1)
								+ "]: Not missing when QC = 4 or 9 at " + missNot + " levels (of " + prm_adj.length
								+ ")");
					}
					if (missPrm > 0) {
						param_adjErr = true;
						validationResult.addError(
								"DATA_MODE '" + mode + "': " + param + "[" + (profNum + 1) + "] Missing / " + varName
										+ " Not missing: At " + missPrm + " levels (of " + prm_adj.length + ")");
					}
					if (qcNotMiss > 0) {
						param_adjErr = true;
						validationResult
								.addError(varName + "_QC[" + (profNum + 1) + "]: Missing data but QC not missing at "
										+ qcNotMiss + " levels (of " + prm_adj.length + ")");
					}
					if (errNotMiss > 0) {
						param_adjErr = true;
						validationResult.addError(varName + "_ERROR[" + (profNum + 1)
								+ "]: Not missing when PARAM or _ADJUSTED is missing at " + errNotMiss + " levels (of "
								+ prm_adj.length + ")");
					}
					if (errMiss > 0) {
						param_adjErr = true;
						validationResult.addError("DATA_MODE: '" + mode + "': " + varName + "_ERROR[" + (profNum + 1)
								+ "]:  Incorrectly set to missing at " + errMiss + " levels (of " + prm_adj.length
								+ ")");
					}
					if (mode == 'D' && mismatchAdjErr > 0) {
						param_adjErr = true;
						validationResult.addError("DATA_MODE: '" + mode + "': " + varName + "[" + (profNum + 1) + "]: "
								+ "Set/FillValue mismatch between _ADJUSTED and _ERROR at " + mismatchAdjErr
								+ " levels (of " + prm_adj.length + ")");
					}
					if (nan > 0) {
						validationResult.addError(varName + "[" + (profNum + 1) + "]: NaNs at " + nan + " levels (of "
								+ prm_adj.length + ")");
					}
					if (nanErr > 0) {
						validationResult.addError(varName + "_ERROR[" + (profNum + 1) + "]: NaNs at " + nanErr
								+ " levels (of " + prm_adj.length + ")");
					}

					if (inf > 0) {
						validationResult.addError(varName + "[" + (profNum + 1) + "]: Infinite value at " + inf
								+ " levels (of " + prm_adj.length + ")");
					}
					if (infErr > 0) {
						validationResult.addError(varName + "_ERROR[" + (profNum + 1) + "]: Infinite value at " + infErr
								+ " levels (of " + prm_adj.length + ")");
					}

				}

			} else {
				// ......there were errors in param -- don't bother with param_adjusted.....
				validationResult.addError("Warning: " + param + "_ADJUSTED[" + (profNum + 1)
						+ "] data not checked due to errors in " + param + " data");
			} // ..end if (! paramErr)

			// ............check PROFILE_param_QC.............
			char expProfQC = 'x';
			ArgoReferenceTable.ArgoReferenceEntry info;

			info = ArgoReferenceTable.PROFILE_QC_FLAG.contains(profQC);
			if (!info.isValid()) {
				validationResult.addError("PROFILE_" + param + "_QC[" + (profNum + 1) + "]: '" + profQC + "': Invalid");

			} else {
				if (info.isDeprecated) {
					validationResult.addWarning(
							"PROFILE_" + param + "_QC[" + (profNum + 1) + "]: '" + profQC + "': Deprecated");
				}

				if (paramErr || param_adjErr) {
					validationResult.addError("Warning: PROFILE_" + param + "_QC[" + (profNum + 1)
							+ "] not checked due to errors in " + param + " data");

				} else {
					double pctGood = (double) n_good / (double) n_data * 100.f;
					expProfQC = ' ';
					if (n_noqc == n_data) {
						expProfQC = ' ';
					} else if (n_good == n_data) {
						expProfQC = 'A';
					} else if (pctGood >= 75.d) {
						expProfQC = 'B';
					} else if (pctGood >= 50.d) {
						expProfQC = 'C';
					} else if (pctGood >= 25.d) {
						expProfQC = 'D';
					} else if (pctGood > 0.d) {
						expProfQC = 'E';
					} else {
						expProfQC = 'F';
					}

					if (expProfQC != profQC) {
						validationResult.addError("PROFILE_" + param + "_QC[" + (profNum + 1) + "]: Value = '" + profQC
								+ "'. Expected = '" + expProfQC + "'");
					}
				} // ..end if paramErr | param_adjErr
			}
			if (log.isDebugEnabled()) {
				log.debug("PROFILE_" + param + "_QC[" + profNum + "]: '" + profQC + "'. Expected: '" + expProfQC + "'");
			}

		} // ..end for PARAM_LOOP (param)
	}

	/**
	 * CHECK_PROFILE_0011 : Check when DATA_MODE = ‘R’ then <PARAM>_ADJUSTED and
	 * <PARAM>_ADJUSTED_QC are All FillValue (including *_QC and *_ERROR). ERROR if
	 * not tge
	 * 
	 * @param prm_adj     : <PARAM>_ADJUSTED data
	 * @param prm_adj_err : <PARAM>_ADJUSTED_ERROR data
	 * @param prm_adj_qc  : <PARAM>_ADJUSTED_QC data
	 * @param fValue      : FillValue specified for the variable
	 * @param mode        : Data Mode ('R', 'A' or 'D')
	 * @param varName     : variable name
	 * @param profNum     : profile numver
	 * @param prm         : array [nLevel] of data for variable for the gicen
	 *                    profNum
	 * 
	 * @return : True if an error has been encountered
	 */
	private boolean checkParamAdjusted_When_DataModeIsR_CHECK_PROFILE_0021(float[] prm_adj, float[] prm_adj_err,
			char[] prm_adj_qc, float fValue, char mode, String varName, int profNum, float[] prm) {
		int errNotMiss = 0; // ..count of ERROR not missing
		int missNot = 0; // ..count of param_adj not missing
		int qcNotMiss = 0; // ..count of ADJ_QC not missing
		boolean paramErr = false;

		for (int k = 0; k < prm_adj.length; k++) {
			if (!ArgoDataFile.is_FillValue(fValue, prm_adj[k])) {
				missNot++;
			}
			if (!ArgoDataFile.is_FillValue(fValue, prm_adj_err[k])) {
				errNotMiss++;
			}
			if (prm_adj_qc[k] != ' ') {
				qcNotMiss++;
			}
		}

		if (missNot > 0) {
			paramErr = true;
			validationResult.addError("DATA_MODE '" + mode + "': " + varName + "[" + (profNum + 1)
					+ "]: Not FillValue at " + missNot + " levels (of " + prm.length + ")");
		}
		if (errNotMiss > 0) {
			paramErr = true;
			validationResult.addError("DATA_MODE '" + mode + "': " + varName + "_ERROR[" + (profNum + 1)
					+ "]: Not FillValue at " + errNotMiss + " levels (of " + prm.length + ")");
		}
		if (qcNotMiss > 0) {
			paramErr = true;
			validationResult.addError("DATA_MODE '" + mode + "': " + varName + "_QC[" + (profNum + 1)
					+ "]: Not FillValue at " + qcNotMiss + " levels (of " + prm.length + ")");
		}

		return paramErr;

	}

	/**
	 * Checks the QC flag values. Assumes:
	 * <ul>
	 * <li>STATION_PARAMETERS is correct
	 * <li>DATA_MODE is correct
	 * </ul>
	 * 
	 * <p>
	 * Upon completion <i>obj</i>.nFormatErrors(), <i>obj</i>.nFormatWarnings,
	 * <i>obj</i>.formatErrors(), and <i>obj</i>.formatWarnings will return results.
	 *
	 * @param nProf  number of profiles in the file
	 * @param nParam number of parameters in the file
	 * @param nLevel number of levels in the file
	 * @throws IOException If an I/O error occurs
	 */
	public void validateQC(int nProf, int nParam, int nLevel) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(".....validateQC.....");
		}

		String juldQC = readString("JULD_QC", true);// ..true -> return NULLs if present
		String posQC = readString("POSITION_QC", true);// ..true -> return NULLs if present

		// ...........loop over each profile in the file.............
		for (int n = 0; n < nProf; n++) {
			ArgoReferenceTable.ArgoReferenceEntry info;
			Character ch;

			ch = juldQC.charAt(n);
			if ((info = ArgoReferenceTable.QC_FLAG.contains(ch)).isValid()) {
				if (info.isDeprecated) {
					validationResult.addWarning("JULD_QC[" + (n + 1) + "]: '" + ch + "' Status: " + info.message);
				}

			} else {
				validationResult.addError("JULD_QC[" + (n + 1) + "]: '" + ch + "' Status: " + info.message);
			}

			ch = posQC.charAt(n);
			if ((info = ArgoReferenceTable.QC_FLAG.contains(ch)).isValid()) {
				if (info.isDeprecated) {
					validationResult.addWarning("POSITION_QC[" + (n + 1) + "]: '" + ch + "' Status: " + info.message);
				}

			} else {
				validationResult.addError("POSITION_QC[" + (n + 1) + "]: '" + ch + "' Status: " + info.message);
			}
		}
	}

	/**
	 * Computes the "index-file psal adjustment" statistics defined as:<br>
	 * <ul>
	 * <li>Mean of (psal_adjusted - psal) on the deepest 500 meters with good
	 * psal_adjusted_qc (equal to 1)
	 * <li>Standard deviation of (psal_adjusted - psal) on the deepest 500 meters
	 * with good psal_adjusted_qc (equal to 1)
	 * </ul>
	 * <p>
	 * NOTES: Only performed for a core-file for the first profile.
	 * 
	 * @param nProf  number of profiles in the file
	 * @param nParam number of parameters in the file
	 * @param nLevel number of levels in the file
	 * @throws IOException If an I/O error occurs
	 */
	public double[] computePsalAdjStats() {
		log.debug(".....computePsalAdjStats.....");

		double[] stats = { 99999., 99999. };

		if (fileType != FileType.PROFILE) {
			log.debug("not a core-file. fileType = {}", fileType);
			return (stats);
		}

		float[] p_adj = readFloatArr("PRES_ADJUSTED", 0);
		String p_adj_qc = readString("PRES_ADJUSTED_QC", 0, true);

		if (p_adj == null) {
			log.debug("failed: no PRES_ADJUSTED variable");
			return (stats);
		}

		// ..find the deepest good pres (must be > 500db)
		float pDeep = 99999.f;
		int nDeep = -1;

		for (int n = p_adj.length - 1; n > 0; n--) {
			// if (p_adj[n] >= 500.f) {
			if (p_adj_qc.charAt(n) == '1') {
				pDeep = p_adj[n];
				nDeep = n;
				break;
			}
			// }
		}

		if (nDeep < 0) {
			log.debug("failed: no good PRES_ADJUSTED > 500db");
			return (stats);
		}

		log.debug("nDeep, pDeep = {}, {}", nDeep, pDeep);

		// ..find the starting point for the stats
		float pShallow = 99999.f;
		int nShallow = -1;

		float shallowest = pDeep - 500.f;

		for (int n = 0; n < p_adj.length; n++) {
			if (p_adj[n] >= shallowest) {
				pShallow = p_adj[n];
				nShallow = n;
				break;
			}
		}

		if (nShallow < 0) {
			log.debug("failed: could not find a starting PRES_ADJUSTED index");
			return (stats);
		}

		log.debug("nShallow, pShallow = {},  {}", nShallow, pShallow);

		// ..is this mode = A or D (otherwise, the psal_adj)

		char m = readString("DATA_MODE", true).charAt(0);
		log.debug("mode = {}", m);

		if (m == 'R') {
			// ..no adjustment
			stats[0] = 0.;
			stats[1] = 0.;
			log.debug("r-mode. no adjustment. stats = 0");
			return (stats);
		}

		// ..get psal and psal_adj

		float[] s = readFloatArr("PSAL", 0);
		float[] sadj = readFloatArr("PSAL_ADJUSTED", 0);

		String s_qc = readString("PSAL_QC", 0, true);
		String sadj_qc = readString("PSAL_ADJUSTED_QC", 0, true);

		if (s == null) {
			log.debug("failed: no PSAL data");
			return (stats);
		}

		// ..compute mean

		double[] diff = new double[s.length];
		double sum = 0.;
		int n_data = 0;
		boolean[] include = new boolean[s.length];

		for (int n = nShallow; n <= nDeep; n++) {
			if (s_qc.charAt(n) == '1' && sadj_qc.charAt(n) == '1') {
				include[n] = true;
				n_data++;
				diff[n] = sadj[n] - s[n];
				sum += diff[n];
			} else {
				include[n] = false;
				diff[n] = 1.e10;
			}
		}

		if (n_data < 2) {
			log.debug("failed: fewer than 2 good data");
			return (stats);
		}

		double mean = sum / n_data;

		log.debug("sum, n_data, mean = {}, {}, {}", sum, n_data, mean);

		// ..compute std dev

		sum = 0.;

		for (int n = nShallow; n <= nDeep; n++) {
			if (include[n]) {
				double d = diff[n] - mean;
				sum += d * d;
			}
		}

		double sdev = Math.sqrt(sum / ((double) n_data - 1));

		log.debug("sum, n_data, sdev = {}, {}, {}", sum, n_data, sdev);

		// ..done

		stats[0] = mean;
		stats[1] = sdev;

		return (stats);
	}

} // ..end class
