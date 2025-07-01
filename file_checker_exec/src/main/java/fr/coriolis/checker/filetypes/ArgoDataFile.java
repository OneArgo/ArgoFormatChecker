package fr.coriolis.checker.filetypes;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.specs.ArgoDate;
import fr.coriolis.checker.specs.ArgoFileSpecification;
import fr.coriolis.checker.specs.ArgoReferenceTable;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
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
	private ArgoReferenceTable.DACS validatedDAC;

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

	public void setValidatedDac(ArgoReferenceTable.DACS validatedDAC) {
		this.validatedDAC = validatedDAC;
	}

	public File getFile() {
		return file;
	}

	public ArgoReferenceTable.DACS getValidatedDac() {
		return this.validatedDAC;
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
			arFile = new ArgoMetadataValidator();

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
