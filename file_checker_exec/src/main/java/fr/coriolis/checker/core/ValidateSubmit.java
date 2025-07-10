package fr.coriolis.checker.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.config.Options;
import fr.coriolis.checker.exceptions.NotAnArgoFileException;
import fr.coriolis.checker.exceptions.ValidateFileDataFailedException;
import fr.coriolis.checker.exceptions.VerifyFileFormatFailedException;
import fr.coriolis.checker.filetypes.ArgoDataFile;
import fr.coriolis.checker.filetypes.ArgoDataFile.FileType;
import fr.coriolis.checker.filetypes.ArgoMetadataValidator;
import fr.coriolis.checker.filetypes.ArgoProfileFileValidator;
import fr.coriolis.checker.filetypes.ArgoTechnicalFileValidator;
import fr.coriolis.checker.filetypes.ArgoTrajectoryFile;
import fr.coriolis.checker.filetypes.ValidationResult;
import fr.coriolis.checker.output.ResultsFile;

/**
 * Implements the Argo FileChecker data file validation checking.
 * <p>
 * Separate documentation exists:
 * <ul>
 * <li>Details of the checks: Argo Data File Format and Consistency Checks
 * <li>Users Manual:
 * </ul>
 * <p>
 * 
 * @author Mark Ignaszewski
 * @version $Id: ValidateSubmit.java 1319 2022-04-14 21:48:55Z ignaszewski $
 */
public class ValidateSubmit {

	// ......................Variable Declarations................

	private static ArgoDataFile argo;

	private static boolean doXml = true;

	private static final String UNKNOWN_VERSION = "unknown";
	private static String fcVersion;
	private static String spVersion;

	private static String propFileName = new String("Application.properties");
	private static String specPropFileName = new String("VersionInfo.properties");
	private static Properties codeProp;
	private static Properties specProp;

	// ..standard i/o shortcuts
	static PrintStream stdout = new PrintStream(System.out);
	static PrintStream stderr = new PrintStream(System.err);

	static final Class<?> ThisClass;
	static final String ClassName;

	private static final Logger log;

	static {
		ThisClass = MethodHandles.lookup().lookupClass();
		ClassName = ThisClass.getSimpleName();

		System.setProperty("logfile.name", ClassName + "_LOG");

		log = LogManager.getLogger(ClassName);
	}

	// .................................................................
	//
	// main
	//
	// .................................................................

	public static void main(String args[]) throws IOException {

		// .....get the Properties from Application.properties....
		loadProperties();

		log.info("{}:  START", ClassName);
		// .....extract the options from command-line arguments....
		try {
			Options options = Options.getInstance(args);

			doXml = options.isDoXml();
			String dacName = options.getDacName();
			File inDir = new File(options.getInDirName()); // already checked in Options that it is a directory.

			// is help is asked :
			displayHelpIfAsked(options);

			// is application version is asked :
			displayVersionIfAsked(options);

			// validate Mandatory arguments :
			options.validateMandatoryArguments(); // System exit with error if no validated

			// .............load the spec version information..............
			loadSpecVersionInfo(options.getSpecDirName());

			// ....................get list of input files.................
			List<String> filesToProcess = getFilesToProcessList(options.getListFile(), options.getInFileList(), inDir);

			// ..................check format and data (optional) of all files in
			// list......................
			validateFiles(options, dacName, filesToProcess);

		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			Help();
			System.exit(1);
		}

	}// ..end main

	// .................................................................
	//
	// Methods
	//
	// .................................................................

	private static void displayHelpIfAsked(Options options) {
		if (options.isHelp()) {
			ValidateSubmit.Help();
			System.exit(0);
		}

	}

	/**
	 * display application version if asked in the command-line arguments. End
	 * program after.
	 * 
	 * @param options
	 */
	private static void displayVersionIfAsked(Options options) {
		if (options.isVersion()) {
			stdout.println(" ");
			stdout.println("Code version: " + fcVersion);
			System.exit(0);
		}
	}

	/**
	 * Open the ouput file
	 * 
	 * @param inFileName  Name of file to be processed
	 * @param outFileName Name of result file
	 * @return
	 */
	private static ResultsFile openOuputFile(String inFileName, String outFileName) {
		ResultsFile out = null;

		try {
			out = new ResultsFile(doXml, outFileName, fcVersion, spVersion, inFileName);

		} catch (Exception e) {
			handleResultsFileException(e);
		}
		return out;
	}

	/**
	 * Open the Argo data file.
	 * 
	 * @param inFileName Name of the input file
	 * @param dacName    Name of the DAC
	 * @return ArgoDataFile object if successfully opened and validated
	 * @throws Exception if any issue occurs during processing
	 */
	private static ArgoDataFile openArgoFile(String inFileName, String specDirName, String dacName) throws Exception {
		ArgoDataFile argo = ArgoDataFile.open(inFileName, specDirName, true, dacName);

		if (argo == null) {
			// ..null file means it did not meet the min criteria to be an argo file
			throw new NotAnArgoFileException("ArgoDataFile.open failed: " + ValidationResult.getMessage());
		}

		return argo;
	}

	/**
	 * Loop through files and check format and optionally data also
	 * 
	 * @param options
	 * @param dacName
	 * @param filesToProcess
	 */
	private static void validateFiles(Options options, String dacName, List<String> filesToProcess) {
		// Loop through files list
		for (String file : filesToProcess) {
			// .... get file informations from options :
			String inFileName = options.getInDirName().concat(File.separator).concat(file);
			String outFileName = options.getOutDirName().concat(File.separator).concat(file).concat(".filecheck");
			log.info("input file: '" + inFileName + "'");
			log.info("results file: '" + outFileName + "'");

			// .....open the output results file...
			ResultsFile out = openOuputFile(inFileName, outFileName);

			// ......open and process the input file.....
			try {
				// ..............open Argo file ....................
				argo = openArgoFile(inFileName, options.getSpecDirName(), dacName);

				// .................check the format................
				String phase = "FORMAT-VERIFICATION";
				boolean[] checkFormatResults = checkArgoFileFormat(dacName);
				boolean specialPreV31FormatCheckPassed = checkFormatResults[1];
				boolean formatPassed = checkFormatResults[0];

				// ..................check the data..................
				boolean rudimentaryDateCheckDone = rudimentaryDateCheck(options, formatPassed); // true if a rudimentary
																								// date check has be
																								// done
				// Evaluate is full data check needs to be done
				boolean doDataCheck = isCheckDataToBeDone(formatPassed, options.isDoFormatOnly(),
						rudimentaryDateCheckDone);

				if (doDataCheck) { // Full data check needs to be done
					phase = "DATA-VALIDATION";
					checkArgoFileData(dacName, options.isDoNulls(), options.isDoBatteryChecks());
				}

				// ..................check file Name...................
				if (options.isDoNameCheck() && formatPassed) {
					// .."name check" requested and no other errors
					phase = "FILE-NAME-CHECK";
					argo.validateGdacFileName();
				}
				// ...............report status and meta-data results...............
				// ..status is that open was successful
				// ..- that means identified as Argo netCDF file (DATA_TYPE and FORMAT_VERSION)
				// ..- format may or may not have passed
				// .. - if format did not pass, trying to retrieve the numeric meta-data
				// .. may cause aborts -- i think string types are safe
				// ..try to get as much of the meta-data as exists, but avoid aborts

				if (!specialPreV31FormatCheckPassed) {
					out.oldDModeFile(dacName, argo.fileVersion());
				} else {
					out.statusAndPhase((argo.getValidationResult().nFormatErrors() == 0), phase);
					out.metaData(dacName, argo, formatPassed, options.isDoPsalStats());
					out.errorsAndWarnings(argo);
				}

				// .............................close Argo file......................
				argo.close();
				// .....................Exceptions handle......................
			} catch (Exception e) {
				handleValidateFilesExceptions(e, out, file, dacName);
			} finally {
				log.debug("closing Results file");
				handleResultsFileOperation(out, "close", "");
			}
		}
	}

	private static boolean[] checkArgoFileFormat(String dacName) throws VerifyFileFormatFailedException {
		boolean[] results = new boolean[2];

		boolean isRegularFormatCheckPassed = regularCheckArgoFileFormat(dacName);
		boolean isSpecialPreV31FormatCheckPassed = checkArgoPreV31FileFormat(dacName); // return true if not pre v3.1.
																						// If pre v3.1, do the special
																						// check and return true / false
																						// if accepted/refused

		results[0] = isRegularFormatCheckPassed && isSpecialPreV31FormatCheckPassed; // will be false if
																						// specialPreV31FormatCheckPassed
																						// is false
		results[1] = isSpecialPreV31FormatCheckPassed;

		return results;
	}

	/**
	 * Check the format with verifyFormat method from ArgoDataFile. Return true if
	 * format accepted, false otherwise. If the verifyFormat method fail, an
	 * exception is raised.
	 * 
	 * @param dacName
	 * @return
	 * @throws VerifyFileFormatFailedException
	 */
	private static boolean regularCheckArgoFileFormat(String dacName) throws VerifyFileFormatFailedException {

		// check the format and return true if all process could be done
		boolean isVerifyFormatCompleted = argo.validateFormat(dacName);

		if (!isVerifyFormatCompleted) {
			// ..verifyFormat *failed* -- not format errors - an actual failure
			throw new VerifyFileFormatFailedException("verifyFormat check failed: " + ValidationResult.getMessage());

		} else {
			// ..verifyFormat completed -- chech error/warning counts to determine status
			if (argo.getValidationResult().nFormatErrors() == 0) {
				log.debug("format ACCEPTED");
				return true;

			} else {
				log.debug("format REJECTED");
				return false;
			}
		}
	}

	/**
	 * If Argo Profile file has a version before 3.1, it cannot have a D-mode so the
	 * format check don't pass and a special Result File will be issued. If arfo
	 * Profile file version is 3.1 or after, return True.
	 * 
	 * @param dacName
	 * @return true/false
	 */
	private static boolean checkArgoPreV31FileFormat(String dacName) {
		// ......SPECIAL CHECK for pre-v3.1 D-mode Profile file......

		if (argo.fileType() == FileType.PROFILE) {
			String dMode = argo.readString("DATA_MODE", true); // ..true -> return NULLs if present
			if (dMode.charAt(0) == 'D') {
				String fv = argo.fileVersion();
				if (fv.compareTo("3.1") < 0) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Do the rudimentary date check if needed. Return true if the
	 * rudimentaryDateCheck has be done, false otherwise
	 * 
	 * @param options
	 * @param formatPassed
	 * @return
	 */
	private static boolean rudimentaryDateCheck(Options options, boolean formatPassed) {
		boolean doRudimentaryDateCheck = isDoRudimentaryDateCheck(options.isDoFormatOnlyPre31(),
				options.isDoFormatOnly(), formatPassed);
		if (doRudimentaryDateCheck) {
			// ..passed format checks, format accepted, full data checks not performed
			// because "early version" so do a couple of rudimentary date checks
			argo.rudimentaryDateChecks();
		}
		return doRudimentaryDateCheck;
	}

	/**
	 * Do the argo file data check by calling the method validate from ArgoDataFile
	 * classes.
	 * 
	 * @param dacName
	 * @param doNulls
	 * @param doBatteryChecks
	 * @throws IOException
	 * @throws ValidateFileDataFailedException
	 */
	private static void checkArgoFileData(String dacName, boolean doNulls, boolean doBatteryChecks)
			throws IOException, ValidateFileDataFailedException {
		if (argo.fileType() == FileType.METADATA) {
			// Do metadata file validate data
			boolean isValidateArgoMetadaFileDataCompleted = ((ArgoMetadataValidator) argo).validateData(dacName, doNulls,
					doBatteryChecks);
			if (!isValidateArgoMetadaFileDataCompleted) {
				// ..the validate process failed (not errors within the data)
				log.error("ArgoMetadataFile.validate failed: " + ValidationResult.getMessage());
				throw new ValidateFileDataFailedException("Meta-data");
			}

		} else if (argo.fileType() == FileType.PROFILE || argo.fileType() == FileType.BIO_PROFILE) {
			// Do profile file validate data
			boolean isValidateArgoProfileFileDataCompleted = ((ArgoProfileFileValidator) argo).validateData(false, dacName, doNulls);
			if (!isValidateArgoProfileFileDataCompleted) {
				// ..the validate process failed (not errors within the data)
				log.error("ArgoProfileFile.validate failed: " + ValidationResult.getMessage());
				throw new ValidateFileDataFailedException("Profile");
			}

		} else if (argo.fileType() == FileType.TECHNICAL) {
			// Do Technical file validate data
			boolean isValidateArgoTechnicalFileDataCompleted = ((ArgoTechnicalFileValidator) argo).validateData(dacName, doNulls);
			if (!isValidateArgoTechnicalFileDataCompleted) {
				// ..the validate process failed (not errors within the data)
				log.error("ArgoTechnicalFile.validate failed: " + ValidationResult.getMessage());
				throw new ValidateFileDataFailedException("Technical");
			}

		} else if (argo.fileType() == FileType.TRAJECTORY || argo.fileType() == FileType.BIO_TRAJECTORY) {
			// Do Trajectory file validate data
			boolean isValidateArgoTrajectoryFileDataCompleted = ((ArgoTrajectoryFile) argo).validateData(dacName, doNulls);
			if (!isValidateArgoTrajectoryFileDataCompleted) {
				// ..the validate process failed (not errors within the data)
				log.error("ArgoTrajectoryFile.validate failed: " + ValidationResult.getMessage());
				throw new ValidateFileDataFailedException("Trajectory");
			}
		}
	}

	/**
	 * Evaluate if data check has to be done.
	 * 
	 * @param formatPassed   (boolean)
	 * @param isDoFormatOnly (boolean)
	 * @return boolean doDataCheck
	 */
	private static boolean isCheckDataToBeDone(boolean formatPassed, boolean isDoFormatOnly,
			boolean rudimentaryDateCheckDone) {
		boolean doDataCheck = false;
		if (formatPassed && !rudimentaryDateCheckDone) {
			doDataCheck = true;

			if (isDoFormatOnly) {
				doDataCheck = false;
				log.debug("data check SKIPPED (-format-only)");
			}

		} else {
			doDataCheck = false;
			log.debug("data check SKIPPED (format rejected)");
		}

		return doDataCheck;
	}

	/**
	 * Evalutate if a rudimentary DATE checks needs to be done.
	 * 
	 * @param doFormatOnlyPre31
	 * @param doFormatOnly
	 * @param formatPassed
	 * @return boolean doRudimentaryDateCheck
	 */
	public static boolean isDoRudimentaryDateCheck(boolean doFormatOnlyPre31, boolean doFormatOnly,
			boolean formatPassed) {
		if (formatPassed && !doFormatOnly && doFormatOnlyPre31) {
			// ..have to evaluate the version #
			log.debug("argo.fileVersion() = '{}'", argo.fileVersion());
			if (argo.fileVersion().compareTo("3.1") < 0) {
				log.debug("data check SKIPPED");

				// ..format passed, NOT format-only
				// .. requested format-only for pre-3.1 -->
				// .. implies data-checks for v3.1 and beyond
				// .. need to do some rudimentary DATE checks on pre-3.1 files
				return true;
			}
		}
		return false;

	}

	/**
	 * Handles exceptions that occur during file validation and delegates specific
	 * actions to the ResultsFile object.
	 *
	 * <p>
	 * This method takes an exception thrown during the validation of files,
	 * determines its type, and performs the corresponding action on the given
	 * {@code ResultsFile} object. Actions include marking the file as invalid,
	 * reporting format verification issues, or handling general processing errors.
	 * </p>
	 *
	 * @param exception The exception to handle. Must be one of the expected types:
	 *                  {@link NotAnArgoFileException},
	 *                  {@link VerifyFileFormatFailedException},
	 *                  {@link ValidateFileDataFailedException}, or a generic
	 *                  {@link Exception}.
	 * @param out       The {@link ResultsFile} object on which to perform
	 *                  operations based on the exception type.
	 * @param file      The name of the file being processed, used for logging in
	 *                  case of general exceptions.
	 * @param dacName   The name of the DAC (Data Assembly Center), used in case of
	 *                  {@link NotAnArgoFileException}.
	 */
	private static void handleValidateFilesExceptions(Exception exception, ResultsFile out, String file,
			String dacName) {
		try {
			throw exception;
		} catch (NotAnArgoFileException e) {
			log.error(e.getMessage());
			handleResultsFileOperation(out, "notArgoFile", dacName);
		} catch (VerifyFileFormatFailedException e) {
			log.error(e.getMessage());
			handleResultsFileOperation(out, "formatErrorMessage", "FORMAT-VERIFICATION");
		} catch (ValidateFileDataFailedException e) {
			log.error(e.getMessage());
			handleResultsFileOperation(out, "dataErrorMessage", e.getMessage());
		} catch (Exception e) {
			log.error("Error processing file: " + file, e);
			handleResultsFileOperation(out, "openError", e.getMessage());
		}
	}

	/**
	 * Handles operations on the ResultsFile object and handle results file
	 * exception if the operation fails.
	 *
	 * @param out            The ResultsFile object
	 * @param operation      The type of operation to perform (e.g., "notArgoFile",
	 *                       "formatErrorMessage", etc.)
	 * @param additionalInfo Additional information for the operation, if required
	 *                       (e.g., a message or DAC name)
	 */
	private static void handleResultsFileOperation(ResultsFile out, String operation, String additionalInfo) {
		try {
			switch (operation) {
			case "notArgoFile":
				out.notArgoFile(additionalInfo);
				break;
			case "formatErrorMessage":
				out.formatErrorMessage(additionalInfo);
				break;
			case "dataErrorMessage":
				out.dataErrorMessage(additionalInfo);
				break;
			case "openError":
				out.openError(new Exception(additionalInfo));
				break;
			case "close":
				out.close();
				break;
			default:
				throw new IllegalArgumentException("Unknown operation: " + operation);
			}
		} catch (Exception e) {
			handleResultsFileException(e);
		}
	}

	/**
	 * 
	 * @param e The exception to log and process
	 */
	private static void handleResultsFileException(Exception e) {
		e.printStackTrace(stderr);
		stderr.println("\nERROR: ResultsFile exception:");
		stderr.println(e);
		log.error("results file exception : ", e.getMessage());
		System.exit(1);
	}

	/**
	 * input files are chosen in the following priority order : 1) an
	 * input-file-list (overrides all other lists) 2) file name arguments (already
	 * parsed above, if specified) 3) all files in the input directory
	 * 
	 * @param listFile   (String) path name of the file containing a list of files
	 *                   to process
	 * @param inFileList List<String> list of input files given in command-line
	 *                   arguments
	 * @param inDir      (String) Directory path where input files reside
	 * @return List<String> of input files (paths) to process
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static List<String> getFilesToProcessList(String listFile, List<String> inFileList, File inDir)
			throws FileNotFoundException, IOException {

		List<String> filesToProcess = new ArrayList<>(200);
		if (listFile != null) { // 1
			// ..a list file was specified - open and read it
			// ..this overrides all other "input lists"
			File f = new File(listFile);
			if (!f.isFile()) {
				log.error("-list-file does not exist: '" + listFile + "'");
				stderr.println("\nERROR: -list-file DOES NOT EXIST: '" + listFile + "'");
				System.exit(1);
			} else if (!f.canRead()) {
				log.error("-list-file cannot be read: '" + listFile + "'");
				stderr.println("\nERROR: -list-file CANNOT BE READ: '" + listFile + "'");
				System.exit(1);
			}
			// ..open and read the file
			BufferedReader file = new BufferedReader(new FileReader(listFile));
			// inFileList = new ArrayList<String>(200);
			String line;
			while ((line = file.readLine()) != null) {
				if (line.trim().length() > 0) {
					filesToProcess.add(line.trim());
				}
			}
			file.close();
			log.info("Read {} entries from -list-file '{}'", filesToProcess.size(), listFile);

		} else if (inFileList != null) { // 2
			filesToProcess = inFileList;

		} else if (inFileList == null) { // 3
			filesToProcess = Arrays.asList(inDir.list());
			log.debug("inFileList: all files in directory. size = {}", filesToProcess.size());
		}
		return filesToProcess;
	}

	/**
	 * Loads properties from the {@code Application.properties} file and saves
	 * specific values into static variables. Currently, only the {@code fcVersion}
	 * is extracted.
	 * 
	 */
	private static void loadProperties() {
		fcVersion = UNKNOWN_VERSION;

		try (InputStream in = ThisClass.getClassLoader().getResourceAsStream(propFileName)) {
			if (in == null) {
				log.warn("Properties file '{}' not found", propFileName);
			} else {
				codeProp = new Properties();
				codeProp.load(in);
				fcVersion = codeProp.getProperty("Version", UNKNOWN_VERSION);
			}

		} catch (Exception e) {
			log.debug("could not read codeProp file '{}'", propFileName, e);
		}

		log.info("Code version: file, version = '{}', '{}'", propFileName, fcVersion);
	}

	/**
	 * load specifications version from the VersionInfo.properties file inside the
	 * specDir
	 * 
	 * @param specDirName : specifications directory path
	 */
	private static void loadSpecVersionInfo(String specDirName) {
		try {
			InputStream in = new FileInputStream(specDirName + File.separator + specPropFileName);

			specProp = new Properties();
			specProp.load(in);
			in.close();

			spVersion = specProp.getProperty("Version", UNKNOWN_VERSION);

		} catch (Exception e) {
			spVersion = UNKNOWN_VERSION;
			log.debug("could not read specProperties file");
		}

		log.info("Spec-file version: file, version = '{}', '{}'", specPropFileName, spVersion);
	}

	public static void Help() {
		stdout.println("\n" + "Purpose: Validates the files in a directory\n" + "\n" + "Usage: java  " + ClassName
				+ " [options] dac-name spec-dir output-dir input-dir [file-names]\n" + "Options:\n"
				+ "   -help | -H | -U   Help -- this message\n" + "   -no-name-check Do not check the file name\n"
				+ "   -null-warn     Perform 'nulls-in-string' check (warning)\n"
				+ "                  default: do NOT check for nulls\n"
				+ "   -text-result   Text-formatted results files\n"
				+ "                  default: XML-formatted results files\n"
				+ "   -list-file <list-file-path>  File containing list of files to process\n"
				+ "                                default: no list-file (see Input Files below)\n"
				+ "   -format-only   Only perform format checks to the files -- no data checks\n"
				+ "                  default: perform format and data checks\n"
				+ "   -data-check-all      Format and data checks for all files\n"
				+ "                        default: Only perform format checks on pre-3.1 files\n"
				+ "   -psal-stats    Put PSAL adjustment statistics into results file\n"
				+ "                  default: don't compute this information\n" + "\n"
				+ "   -format-only-pre3.1  (default) Only perform format checks on files format pre-3.1\n"
				+ "      ***deprecated - now the default - retained for backwards compatibility***\n" + "\n"
				+ "Arguments:\n" + "   dac-name       Name of DAC that owns the input files\n"
				+ "   spec-dir       Directory path of specification files\n"
				+ "   output-dir     Directory path where results files will be placed\n"
				+ "   input-dir      Directory path where input files reside\n"
				+ "   file-names     (Optional) List of files names to process (see below)\n" + "\n" + "Input Files:\n"
				+ "   Input files to process are determined in one of the following ways (priority order):\n"
				+ "   1) -list-file              List of names will be read from <list-file-path>\n"
				+ "   2) [file-names] argument   Files listed on command-line will be processed\n"
				+ "   3) All files in 'input-dir' will be processed\n" + "\n");
	}

}
