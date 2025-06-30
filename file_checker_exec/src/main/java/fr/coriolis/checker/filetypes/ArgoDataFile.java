package fr.coriolis.checker.filetypes;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.specs.ArgoAttribute;
import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.specs.ArgoDimension;
import fr.coriolis.checker.specs.ArgoFileSpecification;
import fr.coriolis.checker.specs.ArgoVariable;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Base class that implements the features of an <i>Argo Data File</i>.
 * <p>
 * No constructors are provided.
 *
 * Use <b>open</b> to open an existing file and create an ArgoDataFile object of
 * the proper type.
 * <p>
 * To open a new Argo data file; use the class appropriate for the specific type
 * of file you are opening (meta, profile, trajectory, technical).
 * <p>
 * To be successfully opened, the <i>Argo Data File</i> must be a netCDF file
 * with the variables DATA_TYPE and FORMAT_VERSION defined with expected (Argo)
 * values. (At the initial opening, the routines are not very demanding of the
 * format.) If there is any doubt as to the validitly of the file format, it is
 * highly recommended that the file format be explicity checked.
 * <p>
 * To ensure the file is a valid <i>Argo Data File</i>, open the file with the
 * directory for the "specification" provided (or open the <i>specification</i>
 * separately) and call <b>verifyFormat</b>.
 * <p>
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
 * <p>
 * <b>CONVENIENCE ROUTINES:<b>
 * <p>
 * A set of convenience routines are provided to read data values from an argo
 * file. These anticipate "reads" based on the format of the data files.
 * <ul>
 * <li>readString (varName)
 * <li>readStringArr (varName)
 * <li>readInt (varName)
 * <li>readInt (varName, n)
 * <li>readIntArr (varName)
 * <li>readIntArr (varName, n)
 * <li>readDouble (varName)
 * <li>readDouble (varName, n)
 * <li>readDoubleArr (varName)
 * <li>readDoubleArr (varName, n)
 * <li>readFloat (varName)
 * <li>readFloat (varName, n)
 * <li>readFloatArr (varName)
 * <li>readFloatArr (varName, n)
 * </ul>
 * <p>
 * 
 * @author Mark Ignaszewski
 * @version $HeadURL:
 *          https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoDataFile.java
 *          $
 * @version $Id: ArgoDataFile.java 1295 2022-03-19 22:15:07Z ignaszewski $
 */

public class ArgoDataFile {
//................................................
//             VARIABLE DECLARATIONS
//................................................
	// ..class variables
	private static final String BLANK_MESSAGE = new String("");
	protected final static Date earliestDate = ArgoDate.get("19970101000000");
//	private final static DecimalFormat cycleFmt = new DecimalFormat("000");

	// ..NOTE: The string values map to spec file names

	public static enum FileType {
		METADATA("metadata"), PROFILE("profile"), TECHNICAL("technical"), TRAJECTORY("trajectory"),
		BIO_PROFILE("b_profile"), BIO_TRAJECTORY("b_trajectory"), UNKNOWN("");

		public final String specType;

		FileType(String s) {
			specType = s;
		}
	};

//	protected static String message = BLANK_MESSAGE;

	protected static HashMap<String, ArgoFileSpecification> fullSpecCache = new HashMap<String, ArgoFileSpecification>();
	protected static HashMap<String, ArgoFileSpecification> tmpltSpecCache = new HashMap<String, ArgoFileSpecification>();

	protected static SimpleDateFormat stringDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

//	static Pattern pDataMode; // ..CDL "variables:" tag
//	static {
//		// ..check for legal data_mode
//		pDataMode = Pattern.compile("[RDA]+");
//	}

	// ..standard i/o shortcuts
	private static PrintStream stdout = new PrintStream(System.out);
	private static PrintStream stderr = new PrintStream(System.err);
	private static final Logger log = LogManager.getLogger("ArgoDataFile");

	// ........object variables........

	private String dacName = null;
	private File file = null;
	private FileType fileType = FileType.UNKNOWN;
	private String format_version = null;
	private NetcdfFile ncReader = null;
	private String ncFileName = new String("");
	private ArgoFileSpecification spec = null;

	private List<Variable> varList;

	protected ValidationResult validationResult;

	// .........................................
	// CONSTRUCTORS
	// .........................................

	/**
	 * No public constructors are provided. See (
	 * 
	 * @see fr.coriolis.checker.filetypes.ArgoDataFile#open )
	 */
	protected ArgoDataFile() {
	}

//.........................................
//               ACCESSORS
//.........................................
	public File getFile() {
		return this.file;
	}

	public ValidationResult getValidationResult() {
		return validationResult;
	}

	/** Retrieve the NetcdfFile reference */
	public String getFileName() {
		return ncFileName;
	}

	/** Retrieve varList */
	public List<Variable> getVarList() {
		return this.varList;
	}

	/** Access the NetcdfFile */
	public NetcdfFile getNcReader() {
		return this.ncReader;
	}

	/** Retrieve file type **/
	public FileType fileType() {
		return fileType;
	}

	/** Retrieve the file version */
	public String fileVersion() {
		return new String(format_version);
	}

	/** Retrieve a Variable from an Argo data file */
	public Dimension findDimension(String name) {
		return ncReader.findDimension(name);
	}

	/** Retrieve a Variable from an Argo data file */
	public Variable findVariable(String name) {
		return ncReader.findVariable(name);
	}

	/** Retrieve a global Attribute from an Argo data file */
	public Attribute findGlobalAttribute(String name) {
		return ncReader.findGlobalAttribute(name);
	}

	/**
	 * Access dac name
	 * 
	 */
	public String getDacName() {
		return this.dacName;
	}

	/**
	 * Retrieve the file specification object
	 * 
	 * @returns related ArgoFileSpecification object
	 */
	public ArgoFileSpecification getFileSpec() {
		return spec;
	}

//.........................................
//               METHODS
//.........................................

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
	 * Determines if a file can be opened
	 * 
	 * @param inFile file name to test
	 * @return true = file can be opened; false = not so much
	 * @throws IOException If an I/O error occurs
	 */
	public static boolean canOpen(String inFile) throws IOException {
		return NetcdfFile.canOpen(inFile);
	}

	// .............open (inFile).................
	/**
	 * Opens an existing file (without opening the assoicated <i>Argo
	 * specification</i>).
	 *
	 * @param inFile  the string name of the file to open
	 * @param dacName Optional name of the dac that owns the file
	 * @return the file object reference. Returns null if the file is not an Argo
	 *         file.
	 * @throws IOException If an I/O error occurs
	 */
	public static ArgoDataFile open(String inFile, String... dacName) throws IOException {
		try {
			return (ArgoDataFile.open(inFile, false, dacName));

		} catch (IOException e) {
			throw e;
		}

	}

	/**
	 * Opens an existing file (without opening the assoicated <i>Argo
	 * specification</i>) and, optionally, ignores a bad DATA_TYPE --- will still
	 * detect accepted non-standard settings in old v3.1 files.
	 *
	 * @param inFile          the string name of the file to open
	 * @param overrideBadTYPE true = force "open" to ignore BadTYPE failure
	 * @param dacName         Optional name of the dac that owns the file
	 * @return the file object reference. Returns null if the file is not an Argo
	 *         file.
	 * @throws IOException If an I/O error occurs
	 */
	public static ArgoDataFile open(String inFile, boolean overrideBadTYPE, String... dacName) throws IOException {
		// ..open the netCDF file
		NetcdfFile nc;

		log.info("file = '" + inFile + "'");
		File file = new File(inFile);
		if (!file.canRead()) {
			log.error("'" + inFile + "' cannot be read");
			throw new IOException("File '" + inFile + "' cannot be read");

		} else if (file.length() == 0) {
			log.error("'" + inFile + "' zero length file");
			throw new IOException("File '" + inFile + "' is zero length");
		}

		try {
			nc = NetcdfFile.open(inFile);

		} catch (Exception e) {
			log.error("NetcdfFile.open error on '" + inFile + "'");
			throw new IOException("Error opening '" + inFile + "': " + e.getMessage());
		}

		// ..read DATA_TYPE and check -- fail -> not an Argo file

		String dt = readString(nc, "DATA_TYPE");
		log.info("data type = '" + dt + "'");
		if (dt == null) {
			log.error("DATA_TYPE not in file '" + inFile + "'");
			ValidationResult.lastMessage = new String("DATA_TYPE not in file");
			return null;
		}
		dt = dt.trim();

		FileType ft;
		String badtype = null;

		String dac = "unk";
		if (dacName.length > 0) {
			dac = dacName[0];
		}

		if (dt.equals("Argo meta-data")) {
			ft = FileType.METADATA;

		} else if (dt.equals("Argo profile")) {
			ft = FileType.PROFILE;

		} else if (dt.equals("Argo trajectory")) {
			ft = FileType.TRAJECTORY;

		} else if (dt.equals("Argo technical data")) {
			ft = FileType.TECHNICAL;

		} else if (dt.equals("B-Argo profile")) {
			ft = FileType.BIO_PROFILE;

		} else if (dt.equals("B-Argo trajectory")) {
			ft = FileType.BIO_TRAJECTORY;

			/*
			 * .................................................... ....these are exceptions
			 * currently being allowed....
			 * ....................................................
			 */

		} else if (dt.equals("ARGO profile")) {
			// ################# TEMPORARY WARNING ################
			/*
			 * BODC has ARGO capitalized ... allow it for now
			 */
			log.warn("TEMP WARNING: {}: {}: {}", dac, inFile,
					"Non-standard DATA_TYPE (temporarily allowed): '" + dt + "'");
			badtype = "DATA_TYPE = 'ARGO profile' non-standard. Change to 'Argo profile'";
			ft = FileType.PROFILE;

		} else if (dt.equals("ARGO trajectory")) {
			// ################# TEMPORARY WARNING ################
			/*
			 * KMA has ARGO capitalized ... allow it for now
			 */
			log.warn("TEMP WARNING: {}: {}: {}", dac, inFile,
					"Non-standard DATA_TYPE (temporarily allowed): '" + dt + "'");
			badtype = "DATA_TYPE = 'ARGO trajectory' non-standard. Change to 'Argo trajectory'";
			ft = FileType.TRAJECTORY;

		} else if (dt.equals("Argo technical")) {
			// ################# TEMPORARY WARNING ################
			/*
			 * BODC, INCOIS has this ... allow it for now
			 */
			log.warn("TEMP WARNING: {}: {}: {}", dac, inFile,
					"Non-standard DATA_TYPE (temporarily allowed): '" + dt + "'");
			badtype = "DATA_TYPE = 'ARGO technical' non-standard. Change to 'Argo technical data'";
			ft = FileType.TECHNICAL;

		} else if (dt.equals("ARGO technical data")) {
			// ################# TEMPORARY WARNING ################
			/*
			 * BODC, INCOIS has this ... allow it for now
			 */
			log.warn("TEMP WARNING: {}: {}: {}", dac, inFile,
					"Non-standard DATA_TYPE (temporarily allowed): '" + dt + "'");
			badtype = "DATA_TYPE = 'ARGO technical data' non-standard. Change to 'Argo technical data'";
			ft = FileType.TECHNICAL;

			/*
			 * .................................................... ....above are exceptions
			 * currently being allowed....
			 * ....................................................
			 */

		} else {
			log.info("Invalid DATA_TYPE: '" + dt + "'");
			ft = FileType.UNKNOWN;
			ValidationResult.lastMessage = new String("Invalid DATA_TYPE: '" + dt + "'");
			return null;
		}

		// ..read FORMAT_VERSION and check -- fail -> not an Argo file

		String fv = readString(nc, "FORMAT_VERSION");
		log.info("version = '" + fv + "'");
		if (fv == null) {
			log.info("FORMAT_VERSION not in file");
			ValidationResult.lastMessage = new String("FORMAT_VERSION not in file");
			return null;
		}

		// ..create the correct type of File

		ArgoDataFile arFile = null;
		if (ft == FileType.METADATA) {
			log.debug("creating ArgoMetadataFile");
			arFile = new ArgoMetadataFile();

		} else if (ft == FileType.PROFILE) {
			log.debug("creating ArgoProfileFile");
			arFile = new ArgoProfileFile();

		} else if (ft == FileType.TECHNICAL) {
			log.debug("creating ArgoTechnicalFile");
			arFile = new ArgoTechnicalFile();

		} else if (ft == FileType.TRAJECTORY) {
			log.debug("creating ArgoTrajectoryFile");
			arFile = new ArgoTrajectoryFile();

		} else if (ft == FileType.BIO_PROFILE) {
			log.debug("creating ArgoProfileFile");
			arFile = new ArgoProfileFile();

		} else if (ft == FileType.BIO_TRAJECTORY) {
			log.debug("creating ArgoTrajectoryFile");
			arFile = new ArgoTrajectoryFile();

		} else {
			stderr.println("\n\n******\n" + "****** PROGRAM ERROR: Unexpected file type.  TERMINATING.\n" + "******");
			System.exit(1);
		} // ..end open

		// ..set object variables
		arFile.dacName = dac;
		arFile.file = file;
		arFile.ncReader = nc;
		arFile.ncFileName = inFile;
		arFile.fileType = ft;
		arFile.validationResult = new ValidationResult();
		arFile.format_version = fv;
		arFile.varList = nc.getVariables();

		if (!overrideBadTYPE) {
			if (badtype != null && fv.trim().equals("3.1")) {
				log.info("Invalid DATA_TYPE: '" + dt + "'");
				ft = FileType.UNKNOWN;
				ValidationResult.lastMessage = new String("Invalid DATA_TYPE: '" + dt + "'");
				return null;
			}
		}

		ValidationResult.lastMessage = BLANK_MESSAGE;
		return arFile;
	} // ..end open(inFile)

	// .............open (inFile, specDir, fullSpec).................
	/**
	 * Opens an existing file and the associated <i>Argo specification</i>.
	 *
	 * @param inFile   The string name of the file to open
	 * @param specDir  The string name of the directory containing the specification
	 *                 files
	 * @param fullSpec true = open the full specification; false = open the template
	 *                 specification
	 * @param dacName  Optional name of the dac that owns the file
	 * @return the file object reference. Returns null if the file is not an Argo
	 *         file.
	 * @throws IOException If an I/O error occurs
	 */
	public static ArgoDataFile open(String inFile, String specDir, boolean fullSpec, String... dacName)
			throws IOException {
		ArgoDataFile arFile = open(inFile, dacName);
		if (arFile == null) {
			return arFile;
		}

		// ..create the specification
		try {
			arFile.spec = openSpecification(fullSpec, specDir, arFile.fileType, arFile.format_version);
		} catch (IOException e) {
			if (e.getMessage().matches("cdlFileName.*does not exist")) {
				ValidationResult.lastMessage = "File type / version not valid in the FileChecker: " + arFile.fileType
						+ " / " + arFile.format_version;
				return (null);
			} else {
				throw e;
			}
		}

		return arFile;
	} // ..end open(inFile, specDir)

	// .............close ().................
	/**
	 * Closes an existing file.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	public void close() throws IOException {
		if (ncReader != null) {
			ncReader.close();
		}
		file = null;
		ncFileName = null;
		fileType = null;
		dacName = null;
		validationResult.clearFormatErrors();
		validationResult.clearFormatWarnings();

		ValidationResult.lastMessage = BLANK_MESSAGE;
	} // ..end close()

	// ...............openSpecification...................
	/**
	 * Opens the <i>Argo specification</i> associated with this Argo file. An
	 * <i>Argo specification</i> is a description of the official Argo file format.
	 * 
	 * @see fr.coriolis.checker.specs.ArgoFileSpecification ArgoFileSpecification
	 *
	 * @param fullSpec true = open full specification; false = open file template
	 * @param specDir  The string name of the directory containing the specification
	 *                 files
	 * @param ft       The FileType (enum) of the file type
	 * @param version  The version of the file specification to open
	 * @return the boolean status indicator. True if the specification was opened.
	 *         False if the specification could not be opened
	 * @throws IOException If an I/O error occurs
	 */
	public static ArgoFileSpecification openSpecification(boolean fullSpec, String specDir, FileType ft, String version)
			throws IOException {
		log.debug("fullSpec = {}", fullSpec);
		log.debug("specDir = '{}'", specDir);
		log.debug("file type = {}", ft.specType);
		log.debug("version = '{}'", version);

		// ..could handle specialized "specs" by replacing "pure" with something else
		// .. for example, when we were doing "merged" files, it was set to "merge"

		String specType = specDir + ";" + ft.specType + ";" + version.trim() + ";" + "pure";

		// ..full-specs are cached (so they can be reused)
		// ..full-spec will work as a template spec too

		if (fullSpecCache.containsKey(specType)) {
			// ..specification already exists - use it
			log.info("existing full specification ('" + specType + "')");

			return fullSpecCache.get(specType);

		} else if (!fullSpec) {
			// ..check for a cached template spec

			if (tmpltSpecCache.containsKey(specType)) {
				// ..specification already exists - use it
				log.info("existing tmplt specification ('" + specType + "')");

				return tmpltSpecCache.get(specType);
			}
		}

		// ..build a specification for this file
		ArgoFileSpecification s = null;
		try {
			s = new ArgoFileSpecification(fullSpec, specDir, ft, version);

		} catch (IOException e) {
			ValidationResult.lastMessage = "Failed in ArgoFileSpecification";
			throw e;
		}

		// ..cache the specs for later use
		if (fullSpec) {
			fullSpecCache.put(specType, s);
			log.info("new specification ('" + specType + "')");

		} else {
			tmpltSpecCache.put(specType, s);
			log.info("new specification ('" + specType + "')");
		}

		return s;
	} // ..end openSpecification

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
				ArgoFileSpecification.AttrRegex regex = spec.getAttrRegex(varName, attrName);
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
				log.warn("{}: {}: {}", dacName, file.getName(), err);

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

				log.warn("{}: {}: {}", dacName, file.getName(), err);

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
			log.warn("{}: {}: {}", dacName, file.getName(), err);

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
					if (!spec.getDimension(dDimName).isExtraDimension()) {
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

	// .......................verifyFormat.....................
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
	public boolean verifyFormat(String dacName) {
		if (spec == null) {
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

		spec.clearExtraDimensions();

//		if (formatErrors.size() == 0) {
//			verified = true;
//		}

		log.debug(".....verifyFormat: completed.....");

		return true;
	} // ..end verifyFormat

	private void verifyGlobalAttributes(String dacName) {
		for (String name : spec.getGlobalAttributeNames()) {
			ArgoAttribute specAttr = spec.getGlobalAttribute(name);
			Attribute dataAttr = ncReader.findGlobalAttribute(name);
			if (log.isDebugEnabled()) {
				log.debug("spec attribute: " + name);
			}

			if (dataAttr == null) {
				// ..attribute in spec file is not in the data file

				String err = String.format("global attribute: %s: not defined in data file", name);
				// formatErrors.add(err);

				// ################# TEMPORARY WARNING ################
				validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
				log.warn("{}: {}: {}", dacName, file.getName(), err);

			} else {
				// ..spec attr is in data file -- check values
				checkGlobalAttributeValue(dacName, name, specAttr, dataAttr);
			}
		}
	}

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
					ArgoFileSpecification.AttrRegex regex = spec.getAttrRegex("", name);

					if (regex == null) {
						// ..no regex .. this is a format error

						String err = String.format("global attribute: %s: Definitions differ"
								+ "\n\tSpecification = '%s'" + "\n\tData File     = '%s'", name, specValue, dataValue);

						// formatErrors.add(err);

						// ################# TEMPORARY WARNING ################
						validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");
						log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

					} else {
						// ..regex defined ... does it match?

						if (!regex.pattern.matcher(dataValue).matches()) {
							String err = String.format("global attribute: %s: Definitions differ"
									+ "\n\tSpecification = '%s' (regex)" + "\n\tData File     = '%s'", name,
									regex.pattern, dataValue);
							// formatErrors.add("global attribute: "+

							// ################# TEMPORARY WARNING ################
							validationResult.addWarning(err + "   *** WILL BECOME AN ERROR ***");

							log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

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
			log.warn("TEMP WARNING: {}: {}: {}", dacName, file.getName(), err);

		} // ..end if(dataAttr.isString)
	}

	private void checkGroupsCompleteness(HashSet<String> dataElement, HashSet<String> dataGroup) {
		for (String group : dataGroup) {
			log.debug("group with reported variable: '{}'", group);

			// ..at least one member of "group" was reported
			// ..check that all group memebers are reported

			Set<String> vReq = spec.groupMembers(group);
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

		for (String name : spec.getSpecVariableNames()) {
			ArgoVariable specVar = spec.getVariable(name);
			Variable dataVar = ncReader.findVariable(name);
			log.debug("spec var: {}", name);

			if (dataVar == null) {
				// ..variable in spec file is not in the data file

				if (spec.isOptional(name)) {
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
					checkSpevVarAttributePresenceInData(name, specVar, dataVar, attrName);
				}
			}
		} // end for (spec-Var-name)
	}

	private void checkSpevVarAttributePresenceInData(String name, ArgoVariable specVar, Variable dataVar,
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

		for (ArgoDimension dim : spec.getDimensions()) {
			String name = dim.getName();
			Dimension dataDim = ncReader.findDimension(name);
			log.debug("spec dim: {}", name);

			if (dataDim == null) {
				if (spec.isOptional(name)) {
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

		varList = ncReader.getVariables();

		for (Variable dataVar : varList) {
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

		ArgoVariable specVar = spec.getVariable(name);

		if (specVar == null) {
			// ..data file variable is not in the specification
			validationResult
					.addError("variable: " + name + ": not defined in specification '" + spec.getSpecName() + "'");

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

		List<Dimension> dimList = ncReader.getDimensions();

		if (dimList == null || dimList.isEmpty()) {
			log.debug("no dimensions in this file");
			return;
		}

		for (Dimension dataDim : dimList) {
			String dimName = dataDim.getShortName();
			ArgoDimension specDim = spec.getDimension(dimName);
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
		String group = spec.inGroup(elementName);
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
		specDim = spec.addExtraDimension(dimName, dataDim.getLength());

		if (specDim == null) {
			// ..nope, not an allowed "extra dimension" -> error
			validationResult.addError(
					String.format("dimension: %s: not defined in specification '%s'", dimName, spec.getSpecName()));

			log.info("format error: '{}' not in spec", dimName);

		} else {
			log.debug("extra dimension: '{}'. value = {}", dimName, dataDim.getLength());
		}
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

		if (fileType == FileType.METADATA) {
			String platform = readString("PLATFORM_NUMBER");

			gdacName.append(platform.trim());
			gdacName.append("_meta.nc");

			log.debug("meta-data file: platform, gdacName = '{}', '{}'", platform, gdacName);

		} else if (fileType == FileType.PROFILE || fileType == FileType.BIO_PROFILE) {
			String platform = readString("PLATFORM_NUMBER", 0);
			int cycle = readInt("CYCLE_NUMBER", 0);
			char direction = readString("DIRECTION", true).charAt(0);

			if (fileType == FileType.BIO_PROFILE) {
				gdacName.append('B');
			}

			if (fileType == FileType.PROFILE) {
				char data_mode = readString("DATA_MODE").charAt(0);

				log.debug("{} file: platform, cycle, direction, data_mode = " + "'{}', '{}', '{}', '{}'", fileType,
						platform, cycle, direction, data_mode);

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
				String data_mode = readString("DATA_MODE");

				log.debug("{} file: platform, cycle, direction, data_mode = " + "'{}', '{}', '{}', '{}'", fileType,
						platform, cycle, direction, data_mode);

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

			log.debug("{} file: gdacName = '{}'", fileType, gdacName);

		} else if (fileType == FileType.TECHNICAL) {
			String platform = readString("PLATFORM_NUMBER");

			gdacName.append(platform.trim());
			gdacName.append("_tech.nc");

			log.debug("tech-data file: platform, gdacName = '{}', '{}'", platform, gdacName);

		} else if (fileType == FileType.TRAJECTORY || fileType == FileType.BIO_TRAJECTORY) {
			String platform = readString("PLATFORM_NUMBER");
			String data_mode = readString("DATA_MODE", true);// ..true->incl NULLs

			log.debug("{} file: platform, data_mode = " + "'{}', '{}'", fileType, platform, data_mode);

			gdacName.append(platform.trim());
			gdacName.append("_");

			if (fileType == FileType.BIO_TRAJECTORY) {
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
			validationResult.addError("Could not determine file name: unexpected file type = '" + fileType + "'");
			log.debug("unknown file type: '{}'", fileType);
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

		String name = file.getName();

		if (!name.equals(expected)) {
			// ..actual file name and expected file name don't match

			if (name.endsWith("_traj.nc") && expected.endsWith("_Rtraj.nc")) {
				// ..have to deal with the special case of pre-v3.1 *_traj files
				if (format_version.startsWith("2.") || format_version.startsWith("3.0")) {
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

			validationResult.addError("Incorrect file name\n\tDAC file name:       '" + name
					+ "'\n\tFile name from data: '" + expected.toString() + "'");
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
	public void validateStringNulls() throws IOException {
		char nullChar = (char) 0;

		// .....Check all Strings -- no nulls allowed.....
		if (log.isDebugEnabled()) {
			log.debug(".....validateStrings.....");
		}

		for (Variable var : varList) {
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

		String creation = readString("DATE_CREATION");

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

		String update = readString("DATE_UPDATE");

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

	// ........................................................
	// ........... convenience "reader" functions ............
	// ........................................................
	/**
	 * Gets the value of a dimension from the Argo file.
	 * 
	 * @param dimName the name of the dimension to retrieve
	 * @return the value of the dimension
	 */
	public int getDimensionLength(String dimName) {
		return getDimensionLength(this.ncReader, dimName);
	} // ..end getDimensionLength

	/**
	 * Gets the value of a dimension from a NetCDF file.
	 * 
	 * @param dimName the name of the dimension to retrieve
	 * @return the value of the dimension
	 */
	private int getDimensionLength(NetcdfFile ncReader, String dimName) {
		Dimension dim = ncReader.findDimension(dimName);
		if (dim == null) {
			ValidationResult.lastMessage = "Dimension '" + dimName + "' not in Argo data file.";
			return -1;
		}

		return dim.getLength();
	} // ..end getDimensionLength

	// *******************************************************************
	// *************** CONVENIENCE READER METHODS ************************
	// *******************************************************************

	// ................ STRING READER METHODS .....................

	/**
	 * Reads a String variable from this netCDF file. NOTE: The variable must be a
	 * char[str-length] array.
	 * 
	 * @param varName     the string name of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return the value of the variable. null if not read (variable not present or
	 *         a read error)
	 */
	public String readString(String varName, boolean... returnNulls) {
		return readString(this.ncReader, varName, returnNulls);
	} // ..end readString(var)

	/**
	 * Reads a variable from an open netCDF file NOTE: The variable must be a
	 * char[str-length] array.
	 * 
	 * @param ncReader    the netcdffile object
	 * @param varName     the string name of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return the value of the variable null if not read (variable not present or a
	 *         read error)
	 */
	private static String readString(NetcdfFile ncReader, String varName, boolean... returnNulls) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		ArrayChar.D1 array = null;
		try {
			array = (ArrayChar.D1) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		int[] shape = ncVar.getShape();
		if (shape[0] == 0) {
			return null;
		}

		int str_length = shape[0]; // ..str-length dimension
		char[] c = new char[str_length];

		boolean returnN = (returnNulls.length > 0) && (returnNulls[0] = true);
		boolean nulls = false;

		int i;
		for (i = 0; i < str_length; i++) {
			c[i] = array.get(i);

			// if (! nulls && c[i] == (char) 0) break;
			if (c[i] == (char) 0) {
				nulls = true;
				if (!returnN) {
					break;
				}
			}
		}

		if (log.isDebugEnabled()) {
			if (nulls) {
				if (returnN) {
					log.debug("readString({}): nulls returned", varName);
				} else {
					log.debug("readString({}): nulls blocked", varName);
				}
			}
		}

		String str = new String(c, 0, i);

		return str;
	} // ..end readString(nc, name)

	/**
	 * Reads the nth String from a variable in this netCDF file. NOTE: The variable
	 * must be a char[N, str-length] array.
	 * 
	 * @param varName     the string name of the variable to read
	 * @param n           the index of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return the value of the variable null if not read (variable not present or a
	 *         read error)
	 */
	public String readString(String varName, int n, boolean... returnNulls) {
		return readString(this.ncReader, varName, n, returnNulls);
	} // ..end read

	/**
	 * Reads the nth String from a variable in an open netCDF file NOTE: The
	 * variable must be a char[N, str-length] array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @param n        the index of the variable to read
	 * @return the value of the variable null if not read (variable not present or a
	 *         read error)
	 */
	private static String readString(NetcdfFile ncReader, String varName, int n, boolean... returnNulls) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		ArrayChar.D2 array = null;
		try {
			array = (ArrayChar.D2) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		int[] shape = ncVar.getShape();
		if (shape[1] == 0 || shape[0] == 0) {
			return null;
		} else if (n > shape[0] - 1) {
			return null;
		}

		int str_length = shape[1]; // ..the str-length dimension
		char[] c = new char[str_length]; // ..the str-length dimension

		boolean returnN = (returnNulls.length > 0) && (returnNulls[0] = true);
		boolean nulls = false;

		int i;
		for (i = 0; i < str_length; i++) {
			c[i] = array.get(n, i);

			if (c[i] == (char) 0) {
				nulls = true;
				if (!returnN) {
					break;
				}
			}
		}

		String str = new String(c, 0, i);

		if (log.isDebugEnabled()) {
			if (nulls) {
				if (returnN) {
					log.debug("readString({},{}): nulls returned", varName, n);
				} else {
					log.debug("readString({},{}): nulls blocked", varName, n);
				}
			}
		}

		return str;
	} // ..end readString(nc, name, n)

	/**
	 * Reads the String in position (n, m) from a variable in this netCDF file.
	 * NOTE: The variable must be a char[N, M, str-length] array.
	 * 
	 * @param varName     the string name of the variable to read
	 * @param n           the index of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return the value of the variable null if not read (variable not present or a
	 *         read error)
	 */
	public String readString(String varName, int n, int m, boolean... returnNulls) {
		return readString(this.ncReader, varName, n, m, returnNulls);
	} // ..end read

	/**
	 * Reads the String in position (n, m, k) from a variable in this netCDF file.
	 * NOTE: The variable must be a char[N, M, str-length] array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the value of the variable null if not read (variable not present or a
	 *         read error)
	 */
	private static String readString(NetcdfFile ncReader, String varName, int n, int m, boolean... returnNulls) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		ArrayChar.D3 array = null;
		try {
			array = (ArrayChar.D3) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		int[] shape = ncVar.getShape();
		if (shape[2] == 0 || shape[1] == 0 || shape[0] == 0) {
			return null;
		} else if (n > shape[0] - 1 || m > shape[1] - 1) {
			return null;
		}

		int str_length = shape[2]; // ..the str-length dimension
		char[] c = new char[str_length];

		boolean returnN = (returnNulls.length > 0) && (returnNulls[0] = true);
		boolean nulls = false;

		int i;
		for (i = 0; i < str_length; i++) {
			c[i] = array.get(n, m, i);

			if (c[i] == (char) 0) {
				nulls = true;
				if (!returnN) {
					break;
				}
			}
		}

		String str = new String(c, 0, i);

		if (log.isDebugEnabled()) {
			if (nulls) {
				if (returnN) {
					log.debug("readString({},{},{}): nulls returned", varName, n, m);
				} else {
					log.debug("readString({},{},{}): nulls blocked", varName, n, m);
				}
			}
		}

		return str;
	} // ..end readString(nc, name, n, m)

	/**
	 * Reads the String in position (n, m, k) from a variable in this netCDF file.
	 * NOTE: The variable must be a char[N, M, K, str-length] array.
	 * 
	 * @param varName     the string name of the variable to read
	 * @param n           the index of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return the value of the variable null if not read (variable not present or a
	 *         read error)
	 */
	public String readString(String varName, int n, int m, int k, boolean... returnNulls) {
		return readString(this.ncReader, varName, n, m, k, returnNulls);
	} // ..end read

	/**
	 * Reads the String in position (n, m, k) from a variable in this netCDF file.
	 * NOTE: The variable must be a char[N, M, K, str-length] array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the value of the variable null if not read (variable not present or a
	 *         read error)
	 */
	private static String readString(NetcdfFile ncReader, String varName, int n, int m, int k, boolean... returnNulls) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		ArrayChar.D4 array = null;
		try {
			array = (ArrayChar.D4) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		int[] shape = ncVar.getShape();
		if (shape[3] == 0 || shape[2] == 0 || shape[1] == 0 || shape[0] == 0) {
			return null;
		} else if (n > shape[0] - 1 || m > shape[1] - 1 || k > shape[2] - 1) {
			return null;
		}

		int str_length = shape[3]; // ..the str-length dimension
		char[] c = new char[str_length];

		boolean returnN = (returnNulls.length > 0) && (returnNulls[0] = true);
		boolean nulls = false;

		int i;
		for (i = 0; i < str_length; i++) {
			c[i] = array.get(n, m, k, i);

			if (c[i] == (char) 0) {
				nulls = true;
				if (!returnN) {
					break;
				}
			}
		}

		String str = new String(c, 0, i);

		if (log.isDebugEnabled()) {
			if (nulls) {
				if (returnN) {
					log.debug("readString({},{}): nulls returned", varName, n);
				} else {
					log.debug("readString({},{}): nulls blocked", varName, n);
				}
			}
		}

		return str;
	} // ..end readString(nc, name, n, m, k)

	/**
	 * Reads the String array for a variable in this netCDF file. NOTE: The variable
	 * must be a char[N, str-length] array.
	 * 
	 * @param varName     the string name of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return array of String representing varName[:] null if not read (variable
	 *         not present or a read error)
	 */
	public String[] readStringArr(String varName, boolean... returnNulls) {
		return readStringArr(this.ncReader, varName, returnNulls);
	} // ..end read

	/**
	 * Reads the String array from a variable in an open netCDF file NOTE: The
	 * variable must be a char[N, str-length] array. Returns the String array
	 * varName[:]
	 * 
	 * @param ncReader    the netcdffile object
	 * @param varName     the string name of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return array of String representing varName[:] null if not read (variable
	 *         not present or a read error)
	 */
	private static String[] readStringArr(NetcdfFile ncReader, String varName, boolean... returnNulls) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		ArrayChar.D2 array = null;
		try {
			array = (ArrayChar.D2) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		int[] shape = ncVar.getShape();
		if (shape[1] == 0 || shape[0] == 0) {
			return null;
		}

		int arr_length = shape[0]; // ..array dimension
		int str_length = shape[1]; // ..the str-length dimension
		char[] c = new char[str_length];
		String str[] = new String[arr_length];

		boolean returnN = (returnNulls.length > 0) && (returnNulls[0] = true);
		boolean nulls = false;

		for (int k = 0; k < arr_length; k++) {
			int i;
			for (i = 0; i < str_length; i++) {
				c[i] = array.get(k, i);

				if (c[i] == (char) 0) {
					nulls = true;
					if (!returnN) {
						break;
					}
				}
			}

			str[k] = new String(c, 0, i);
		}

		if (log.isDebugEnabled()) {
			if (nulls) {
				if (returnN) {
					log.debug("readStringArr({}): nulls returned", varName);
				} else {
					log.debug("readString({}): nulls blocked", varName);
				}
			}
		}

		return str;
	} // ..end readStringArr

	/**
	 * Reads the String array for a variable in this netCDF file. NOTE: The variable
	 * must be a char[N, M, str-length] array. Returns the String array slice
	 * varName[n, :]
	 * 
	 * @param varName     the string name of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return array of String representing varName[n, :] null if not read (variable
	 *         not present or a read error)
	 */
	public String[] readStringArr(String varName, int n, boolean... returnNulls) {
		return readStringArr(this.ncReader, varName, n, returnNulls);
	} // ..end read

	/**
	 * Reads the nth String from a variable in an open netCDF file NOTE: The
	 * variable must be a char[N, M, str-length] array. Returns the String array
	 * slice varName[n, :]
	 * 
	 * @param ncReader    the netcdffile object
	 * @param varName     the string name of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return array of String representing varName[n, :] null if not read (variable
	 *         not present or a read error)
	 */
	private static String[] readStringArr(NetcdfFile ncReader, String varName, int n, boolean... returnNulls) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		ArrayChar.D3 array = null;
		try {
			array = (ArrayChar.D3) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		int[] shape = ncVar.getShape();
		if (shape[2] == 0 || shape[1] == 0 || shape[0] == 0) {
			return null;
		}

		int arr_length = shape[1]; // ..array dimension
		int str_length = shape[2]; // ..the str-length dimension
		char[] c = new char[str_length];
		String str[] = new String[arr_length];

		boolean returnN = (returnNulls.length > 0) && (returnNulls[0] = true);
		boolean nulls = false;

		for (int k = 0; k < arr_length; k++) {
			int i;
			for (i = 0; i < str_length; i++) {
				c[i] = array.get(n, k, i);

				if (c[i] == (char) 0) {
					nulls = true;
					if (!returnN) {
						break;
					}
				}
			}

			str[k] = new String(c, 0, i);
		}

		if (log.isDebugEnabled()) {
			if (nulls) {
				if (returnN) {
					log.debug("readStringArr({}): nulls returned", varName);
				} else {
					log.debug("readString({}): nulls blocked", varName);
				}
			}
		}

		return str;
	} // ..end readStringArr(nc, var, n)

	/**
	 * Reads the String array for a variable in this netCDF file. NOTE: The variable
	 * must be a char[N, M, K, str-length] array. Returns the String array slice
	 * varName[n, m, :]
	 * 
	 * @param varName     the string name of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return array of String representing varName[n, m, :] null if not read
	 *         (variable not present or a read error)
	 */
	public String[] readStringArr(String varName, int n, int m, boolean... returnNulls) {
		return readStringArr(this.ncReader, varName, n, m, returnNulls);
	} // ..end read

	/**
	 * Reads the nth String from a variable in an open netCDF file NOTE: The
	 * variable must be a char[N, M, K, str-length] array. Returns the String array
	 * slice varName[n, m, :]
	 * 
	 * @param ncReader    the netcdffile object
	 * @param varName     the string name of the variable to read
	 * @param returnNulls (OPTIONAL) true = return NULLS in string; false (default)
	 *                    = truncate string if NULL is detected.
	 * @return array of String representing varName[n, m, :] null if not read
	 *         (variable not present or a read error)
	 */
	private static String[] readStringArr(NetcdfFile ncReader, String varName, int n, int m, boolean... returnNulls) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		ArrayChar.D4 array = null;
		try {
			array = (ArrayChar.D4) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		int[] shape = ncVar.getShape();
		if (shape[3] == 0 || shape[2] == 0 || shape[1] == 0 || shape[0] == 0) {
			return null;
		}

		int arr_length = shape[2]; // ..array dimension
		int str_length = shape[3]; // ..the str-length dimension
		char[] c = new char[str_length];
		String str[] = new String[arr_length];

		boolean returnN = (returnNulls.length > 0) && (returnNulls[0] = true);
		boolean nulls = false;

		for (int k = 0; k < arr_length; k++) {
			int i;
			for (i = 0; i < str_length; i++) {
				c[i] = array.get(n, m, k, i);

				if (c[i] == (char) 0) {
					nulls = true;
					if (!returnN) {
						break;
					}
				}
			}

			str[k] = new String(c, 0, i);
		}

		if (log.isDebugEnabled()) {
			if (nulls) {
				if (returnN) {
					log.debug("readStringArr({}): nulls returned", varName);
				} else {
					log.debug("readString({}): nulls blocked", varName);
				}
			}
		}

		return str;
	} // ..end readStringArr(nc, var, n, m)

	// ................ INT READER METHODS .....................

	/**
	 * Reads a int variable in the Argo file NOTE: The variable must be a scalar
	 * int.
	 * 
	 * @param varName the string name of the variable to read
	 * @return the value of the variable Integer.MAX_VALUE if not read (variable not
	 *         present or a read error)
	 */
	public int readInt(String varName) {
		return readInt(ncReader, varName);
	}

	/**
	 * Reads the int variable from an open netCDF file NOTE: The variable must be a
	 * scalar int array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the value of the variable Integer.MAX_VALUE if not read (variable not
	 *         present or a read error)
	 */
	private static int readInt(NetcdfFile ncReader, String varName) {
		Variable ncVar;
		ArrayInt.D0 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return Integer.MAX_VALUE;
		}

		array = null;
		try {
			array = (ArrayInt.D0) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return Integer.MAX_VALUE;
		}

		return array.get();
	} // ..end readInt (ncReader, varName, n)

	/**
	 * Reads the nth element from an int variable from this file NOTE: The variable
	 * must be a 1-D int array.
	 * 
	 * @param varName the string name of the variable to read
	 * @param n       the index of the variable to read
	 * @return the value of the variable. Integer.MAX_VALUE if not read (variable
	 *         not present or a read error)
	 */
	public int readInt(String varName, int n) {
		return readInt(ncReader, varName, n);
	}

	/**
	 * Reads the nth element from an int variable in an open netCDF file NOTE: The
	 * variable must be a 1-D int array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @param n        the index of the variable to read
	 * @return the value of the variable Integer.MAX_VALUE if not read (variable not
	 *         present or a read error)
	 */
	private static int readInt(NetcdfFile ncReader, String varName, int n) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return Integer.MAX_VALUE;
		}

		ArrayInt.D1 array = null;
		try {
			array = (ArrayInt.D1) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return Integer.MAX_VALUE;
		}

		return array.get(n);
	} // ..end readInt

	/**
	 * Reads the int array for the variable from this file NOTE: The variable must
	 * be a 1-D int array.
	 * 
	 * @param varName the string name of the variable to read
	 * @return an int array of the values of the variable Integer.MAX_VALUE if not
	 *         read (variable not present or a read error)
	 */
	public int[] readIntArr(String varName) {
		return readIntArr(ncReader, varName);
	}

	/**
	 * Reads the int array from an open netCDF file NOTE: The variable must be a 1-D
	 * int array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return an int array of the values of the variable Integer.MAX_VALUE if not
	 *         read (variable not present or a read error)
	 */
	private static int[] readIntArr(NetcdfFile ncReader, String varName) {
		Variable ncVar;
		ArrayInt.D1 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		array = null;
		try {
			array = (ArrayInt.D1) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (int[]) array.copyTo1DJavaArray();
	} // ..end readIntArr

	/**
	 * Reads the int array variable from the Argo file. NOTE: The variable must be a
	 * 2-D int array. The returned array is the 1-D array slice varName[n, :].
	 * 
	 * @param varName the string name of the variable to read
	 * @return the array of values as a int[] null if not read (variable not present
	 *         or a read error)
	 */
	public int[] readIntArr(String varName, int n) {
		return readIntArr(ncReader, varName, n);
	}

	/**
	 * Reads the the int array variable from an open netCDF file NOTE: The variable
	 * must be a 2-D int array. The returned array is the 1-D array slice varName[n,
	 * :].
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the array of values as a int[] null if not read (variable not present
	 *         or a read error)
	 */
	private static int[] readIntArr(NetcdfFile ncReader, String varName, int n) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		// ..set the origin and shape arrays
		int origin[] = { n, 0 };

		int shape[] = ncVar.getShape();
		shape[0] = 1;

		ArrayInt.D2 array = null;
		try {
			array = (ArrayInt.D2) ncVar.read(origin, shape);
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (int[]) array.copyTo1DJavaArray();
	} // ..end readIntArr

	/**
	 * Reads the int array variable from the Argo file. NOTE: The variable must be a
	 * 2-D int array. The returned array is the 1-D array slice varName[n, m, :].
	 * 
	 * @param varName the string name of the variable to read
	 * @param n       the 1st dimension index to read
	 * @param m       the 2nd dimension index to read
	 * @param varName the string name of the variable to read
	 * @return the array of values as a int[] null if not read (variable not present
	 *         or a read error)
	 */
	public int[] readIntArr(String varName, int n, int m) {
		return readIntArr(ncReader, varName, n, m);
	}

	/**
	 * Reads the the int array variable from an open netCDF file NOTE: The variable
	 * must be a 2-D int array. The returned array is the 1-D array slice varName[n,
	 * :].
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @param n        the 1st dimension index to read
	 * @param m        the 2nd dimension index to read
	 * @return the array of values as a int[] null if not read (variable not present
	 *         or a read error)
	 */
	private static int[] readIntArr(NetcdfFile ncReader, String varName, int n, int m) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		// ..set the origin and shape arrays
		int origin[] = { n, m, 0 };

		int shape[] = ncVar.getShape();
		shape[0] = 1;
		shape[1] = 1;

		ArrayInt.D3 array = null;
		try {
			array = (ArrayInt.D3) ncVar.read(origin, shape);
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (int[]) array.copyTo1DJavaArray();
	} // ..end readDoubleArr

	// ................ DOUBLE READER METHODS .....................

	/**
	 * Reads a double variable in the Argo file NOTE: The variable must be a scalar
	 * double.
	 * 
	 * @param varName the string name of the variable to read
	 * @return the value of the variable Double.NaN if not read (variable not
	 *         present or a read error)
	 */
	public double readDouble(String varName) {
		return readDouble(ncReader, varName);
	}

	/**
	 * Reads the double variable from an open netCDF file NOTE: The variable must be
	 * a scalar double array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the value of the variable Double.NaN if not read (variable not
	 *         present or a read error)
	 */
	private static double readDouble(NetcdfFile ncReader, String varName) {
		Variable ncVar;
		ArrayDouble.D0 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return Double.NaN;
		}

		array = null;
		try {
			array = (ArrayDouble.D0) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return Double.NaN;
		}

		return array.get();
	} // ..end readDouble (ncReader, varName, n)

	/**
	 * Reads the nth element from an double variable in the Argo file NOTE: The
	 * variable must be a 1-D double array.
	 * 
	 * @param varName the string name of the variable to read
	 * @param n       the index of the variable to read
	 * @return the value of the variable Double.NaN if not read (variable not
	 *         present or a read error)
	 */
	public double readDouble(String varName, int n) {
		return readDouble(ncReader, varName, n);
	}

	/**
	 * Reads the nth element from an double variable from in an open netCDF file
	 * NOTE: The variable must be a 1-D double array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @param n        the index of the variable to read
	 * @return the value of the variable Double.NaN if not read (variable not
	 *         present or a read error)
	 */
	private static double readDouble(NetcdfFile ncReader, String varName, int n) {
		Variable ncVar;
		ArrayDouble.D1 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return Double.NaN;
		}

		array = null;
		try {
			array = (ArrayDouble.D1) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return Double.NaN;
		}

		return array.get(n);
	} // ..end readDouble (ncReader, varName, n)

	/**
	 * Reads the double array variable from the Argo file NOTE: The variable must be
	 * a 1-D double array.
	 * 
	 * @param varName the string name of the variable to read
	 * @return the value of the variable null if not read (variable not present or a
	 *         read error)
	 */
	public double[] readDoubleArr(String varName) {
		return readDoubleArr(ncReader, varName);
	}

	/**
	 * Reads the the double array variable from an open netCDF file NOTE: The
	 * variable must be a 1-D double array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the value of the variable null if not read (variable not present or a
	 *         read error)
	 */
	private static double[] readDoubleArr(NetcdfFile ncReader, String varName) {
		Variable ncVar;
		ArrayDouble.D1 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		array = null;
		try {
			array = (ArrayDouble.D1) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (double[]) array.copyTo1DJavaArray();
	} // ..end readDoubleArr

	/**
	 * Reads the double array variable from the Argo file. NOTE: The variable must
	 * be a 2-D double array. The returned array is the 1-D array slice varName[n,
	 * :].
	 * 
	 * @param varName the string name of the variable to read
	 * @return the array of values as a double[] null if not read (variable not
	 *         present or a read error)
	 */
	public double[] readDoubleArr(String varName, int n) {
		return readDoubleArr(ncReader, varName, n);
	}

	/**
	 * Reads the the double array variable from an open netCDF file NOTE: The
	 * variable must be a 2-D double array. The returned array is the 1-D array
	 * slice varName[n, :].
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the array of values as a double[] null if not read (variable not
	 *         present or a read error)
	 */
	private static double[] readDoubleArr(NetcdfFile ncReader, String varName, int n) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		// ..set the origin and shape arrays
		int origin[] = { n, 0 };

		int shape[] = ncVar.getShape();
		shape[0] = 1;

		ArrayDouble.D2 array = null;
		try {
			array = (ArrayDouble.D2) ncVar.read(origin, shape);
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (double[]) array.copyTo1DJavaArray();
	} // ..end readDoubleArr

	/**
	 * Reads the double array variable from the Argo file. NOTE: The variable must
	 * be a 2-D double array. The returned array is the 1-D array slice varName[n,
	 * m, :].
	 * 
	 * @param varName the string name of the variable to read
	 * @param n       the 1st dimension index to read
	 * @param m       the 2nd dimension index to read
	 * @param varName the string name of the variable to read
	 * @return the array of values as a double[] null if not read (variable not
	 *         present or a read error)
	 */
	public double[] readDoubleArr(String varName, int n, int m) {
		return readDoubleArr(ncReader, varName, n, m);
	}

	/**
	 * Reads the the double array variable from an open netCDF file NOTE: The
	 * variable must be a 2-D double array. The returned array is the 1-D array
	 * slice varName[n, :].
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @param n        the 1st dimension index to read
	 * @param m        the 2nd dimension index to read
	 * @return the array of values as a double[] null if not read (variable not
	 *         present or a read error)
	 */
	private static double[] readDoubleArr(NetcdfFile ncReader, String varName, int n, int m) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		// ..set the origin and shape arrays
		int origin[] = { n, m, 0 };

		int shape[] = ncVar.getShape();
		shape[0] = 1;
		shape[1] = 1;

		ArrayDouble.D3 array = null;
		try {
			array = (ArrayDouble.D3) ncVar.read(origin, shape);
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (double[]) array.copyTo1DJavaArray();
	} // ..end readDoubleArr

	// ................ FLOAT READER METHODS .....................

	/**
	 * Reads a float variable in the Argo file NOTE: The variable must be a float
	 * scalar.
	 * 
	 * @param varName the string name of the variable to read
	 * @return the value of the variable Float.NaN if not read (variable not present
	 *         or a read error)
	 */
	public float readFloat(String varName) {
		return readFloat(ncReader, varName);
	}

	/**
	 * Reads a float variable in an open netCDF file NOTE: The variable must be a
	 * scalar float.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the value of the variable Float.NaN if not read (variable not present
	 *         or a read error)
	 */
	private static float readFloat(NetcdfFile ncReader, String varName) {
		Variable ncVar;
		ArrayFloat.D0 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return Float.NaN;
		}

		array = null;
		try {
			array = (ArrayFloat.D0) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return Float.NaN;
		}

		return array.get();
	} // ..end readFloat

	/**
	 * Reads the nth element from a float array variable in the Argo file NOTE: The
	 * variable must be a 1-D float array.
	 * 
	 * @param varName the string name of the variable to read
	 * @param n       the index of the variable to read
	 * @return the value of the variable Float.NaN if not read (variable not present
	 *         or a read error)
	 */
	public float readFloat(String varName, int n) {
		return readFloat(ncReader, varName, n);
	}

	/**
	 * Reads the nth element from a float arrau variable in an open netCDF file
	 * NOTE: The variable must be a 1-D float array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @param n        the index of the variable to read
	 * @return the value of the variable Float.NaN if not read (variable not present
	 *         or a read error)
	 */
	private static float readFloat(NetcdfFile ncReader, String varName, int n) {
		Variable ncVar;
		ArrayFloat.D1 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return Float.NaN;
		}

		array = null;
		try {
			array = (ArrayFloat.D1) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return Float.NaN;
		}

		return array.get(n);
	} // ..end readFloat

	/**
	 * Reads the float array variable from the Argo file. NOTE: The variable must be
	 * a 1-D float array.
	 * 
	 * @param varName the string name of the variable to read
	 * @return the array of values as a float[] null if not read (variable not
	 *         present or a read error)
	 */
	public float[] readFloatArr(String varName) {
		return readFloatArr(ncReader, varName);
	}

	/**
	 * Reads the the float array variable from an open netCDF file NOTE: The
	 * variable must be a 1-D float array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the array of values as a float[] null if not read (variable not
	 *         present or a read error)
	 */
	private static float[] readFloatArr(NetcdfFile ncReader, String varName) {
		Variable ncVar;
		ArrayFloat.D1 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		array = null;
		try {
			array = (ArrayFloat.D1) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (float[]) array.copyTo1DJavaArray();
	} // ..end readFloatArr

	/**
	 * Reads the float array variable from the Argo file. NOTE: The variable must be
	 * a 2-D float array. The returned array is the 1-D array slice varName[n, :].
	 * 
	 * @param varName the string name of the variable to read
	 * @return the array of values as a float[] null if not read (variable not
	 *         present or a read error)
	 */
	public float[] readFloatArr(String varName, int n) {
		return readFloatArr(ncReader, varName, n);
	}

	/**
	 * Reads the the float array variable from an open netCDF file NOTE: The
	 * variable must be a 2-D float array. The returned array is the 1-D array slice
	 * varName[n, :].
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the array of values as a float[] null if not read (variable not
	 *         present or a read error)
	 */
	private static float[] readFloatArr(NetcdfFile ncReader, String varName, int n) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		// ..set the origin and shape arrays
		int origin[] = { n, 0 };

		int shape[] = ncVar.getShape();
		shape[0] = 1;

		// stderr.println(varName+": shape: "+shape[0]+" "+shape[1]);
		// stderr.println(varName+": origin: "+origin[0]+" "+origin[1]);

		ArrayFloat.D2 array = null;
		try {
			array = (ArrayFloat.D2) ncVar.read(origin, shape);
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.println(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (float[]) array.copyTo1DJavaArray();
	} // ..end readFloatArr

	/**
	 * Reads the float array variable from the Argo file. NOTE: The variable must be
	 * a 2-D float array. The returned array is the 1-D array slice varName[n, m,
	 * :].
	 * 
	 * @param varName the string name of the variable to read
	 * @param n       the 1st dimension index to read
	 * @param m       the 2nd dimension index to read
	 * @param varName the string name of the variable to read
	 * @return the array of values as a float[] null if not read (variable not
	 *         present or a read error)
	 */
	public float[] readFloatArr(String varName, int n, int m) {
		return readFloatArr(ncReader, varName, n, m);
	}

	/**
	 * Reads the the float array variable from an open netCDF file NOTE: The
	 * variable must be a 2-D float array. The returned array is the 1-D array slice
	 * varName[n, :].
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @param n        the 1st dimension index to read
	 * @param m        the 2nd dimension index to read
	 * @return the array of values as a float[] null if not read (variable not
	 *         present or a read error)
	 */
	private static float[] readFloatArr(NetcdfFile ncReader, String varName, int n, int m) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		// ..set the origin and shape arrays
		int origin[] = { n, m, 0 };

		int shape[] = ncVar.getShape();
		shape[0] = 1;
		shape[1] = 1;

		ArrayFloat.D3 array = null;
		try {
			array = (ArrayFloat.D3) ncVar.read(origin, shape);
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (float[]) array.copyTo1DJavaArray();
	} // ..end readFloatArr

	// ................ SHORT READER METHODS .....................

	/**
	 * Reads a short variable in the Argo file NOTE: The variable must be a scalar
	 * short.
	 * 
	 * @param varName the string name of the variable to read
	 * @return the value of the variable Short.MAX_VALUE if not read (variable not
	 *         present or a read error)
	 */
	public short readShort(String varName) {
		return readShort(ncReader, varName);
	}

	/**
	 * Reads the short variable from an open netCDF file NOTE: The variable must be
	 * a scalar short array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the value of the variable Short.MAX_VALUE if not read (variable not
	 *         present or a read error)
	 */
	private static short readShort(NetcdfFile ncReader, String varName) {
		Variable ncVar;
		ArrayShort.D0 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return Short.MAX_VALUE;
		}

		array = null;
		try {
			array = (ArrayShort.D0) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return Short.MAX_VALUE;
		}

		return array.get();
	} // ..end readShort (ncReader, varName, n)

	/**
	 * Reads the nth element from an short variable from this file NOTE: The
	 * variable must be a 1-D short array.
	 * 
	 * @param varName the string name of the variable to read
	 * @param n       the index of the variable to read
	 * @return the value of the variable. Short.MAX_VALUE if not read (variable not
	 *         present or a read error)
	 */
	public short readShort(String varName, int n) {
		return readShort(ncReader, varName, n);
	}

	/**
	 * Reads the nth element from an short variable in an open netCDF file NOTE: The
	 * variable must be a 1-D short array.
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @param n        the index of the variable to read
	 * @return the value of the variable Short.MAX_VALUE if not read (variable not
	 *         present or a read error)
	 */
	private static short readShort(NetcdfFile ncReader, String varName, int n) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return Short.MAX_VALUE;
		}

		ArrayShort.D1 array = null;
		try {
			array = (ArrayShort.D1) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return Short.MAX_VALUE;
		}

		return array.get(n);
	} // ..end readShort

	/**
	 * Reads the short array for the variable from this file NOTE: The variable must
	 * be a 1-D short array.
	 * 
	 * @param varName the string name of the variable to read
	 * @return an short array of the values of the variable Short.MAX_VALUE if not
	 *         read (variable not present or a read error)
	 */
	public short[] readShortArr(String varName) {
		return readShortArr(ncReader, varName);
	}

	/**
	 * Reads the short array from an open netCDF file NOTE: The variable must be a
	 * 1-D short array.
	 * 
	 * @param ncReader the netcdf file object
	 * @param varName  the string name of the variable to read
	 * @return an short array of the values of the variable Short.MAX_VALUE if not
	 *         read (variable not present or a read error)
	 */
	private static short[] readShortArr(NetcdfFile ncReader, String varName) {
		Variable ncVar;
		ArrayShort.D1 array;

		ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		array = null;
		try {
			array = (ArrayShort.D1) ncVar.read();
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (short[]) array.copyTo1DJavaArray();
	} // ..end readShortArr

	/**
	 * Reads the short array variable from the Argo file. NOTE: The variable must be
	 * a 2-D short array. The returned array is the 1-D array slice varName[n, :].
	 * 
	 * @param varName the string name of the variable to read
	 * @return the array of values as a short[] null if not read (variable not
	 *         present or a read error)
	 */
	public short[] readShortArr(String varName, int n) {
		return readShortArr(ncReader, varName, n);
	}

	/**
	 * Reads the the short array variable from an open netCDF file NOTE: The
	 * variable must be a 2-D short array. The returned array is the 1-D array slice
	 * varName[n, :].
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @return the array of values as a short[] null if not read (variable not
	 *         present or a read error)
	 */
	private static short[] readShortArr(NetcdfFile ncReader, String varName, int n) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		// ..set the origin and shape arrays
		int origin[] = { n, 0 };

		int shape[] = ncVar.getShape();
		shape[0] = 1;

		// stderr.println(varName+": shape: "+shape[0]+" "+shape[1]);
		// stderr.println(varName+": origin: "+origin[0]+" "+origin[1]);

		ArrayShort.D2 array = null;
		try {
			array = (ArrayShort.D2) ncVar.read(origin, shape);
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.println(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (short[]) array.copyTo1DJavaArray();
	} // ..end readShortArr

	/**
	 * Reads the short array variable from the Argo file. NOTE: The variable must be
	 * a 2-D short array. The returned array is the 1-D array slice varName[n, m,
	 * :].
	 * 
	 * @param varName the string name of the variable to read
	 * @param n       the 1st dimension index to read
	 * @param m       the 2nd dimension index to read
	 * @param varName the string name of the variable to read
	 * @return the array of values as a short[] null if not read (variable not
	 *         present or a read error)
	 */
	public short[] readShortArr(String varName, int n, int m) {
		return readShortArr(ncReader, varName, n, m);
	}

	/**
	 * Reads the the short array variable from an open netCDF file NOTE: The
	 * variable must be a 2-D short array. The returned array is the 1-D array slice
	 * varName[n, :].
	 * 
	 * @param ncReader the netcdffile object
	 * @param varName  the string name of the variable to read
	 * @param n        the 1st dimension index to read
	 * @param m        the 2nd dimension index to read
	 * @return the array of values as a short[] null if not read (variable not
	 *         present or a read error)
	 */
	private static short[] readShortArr(NetcdfFile ncReader, String varName, int n, int m) {
		Variable ncVar = ncReader.findVariable(varName);
		if (ncVar == null) {
			ValidationResult.lastMessage = "Variable '" + varName + "' not in Argo data file.";
			return null;
		}

		// ..set the origin and shape arrays
		int origin[] = { n, m, 0 };

		int shape[] = ncVar.getShape();
		shape[0] = 1;
		shape[1] = 1;

		ArrayShort.D3 array = null;
		try {
			array = (ArrayShort.D3) ncVar.read(origin, shape);
		} catch (Exception e) {
			stderr.println("ERROR: Reading '" + varName + "': " + e);
			// stderr.print(e);
			ValidationResult.lastMessage = "Netcdf read exception: " + e;
			return null;
		}

		return (short[]) array.copyTo1DJavaArray();
	} // ..end readFloatArr

	// ********************************************************
	// ******* end convenience "reader" functions ************
	// ********************************************************
} // ..end class
