package fr.coriolis.checker.config;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.specs.ArgoReferenceTable;

/**
 * The {@code Options} class is responsible for parsing and validating
 * command-line arguments passed to the application. It ensures that required
 * parameters are provided, validates directory paths and DAC names, and
 * provides a centralized access point for configuration.
 * <p>
 * This class implements a singleton pattern to ensure only one instance of
 * {@code Options} exists.
 * </p>
 * 
 * <p>
 * Supported command-line options:
 * <ul>
 * <li>-help: Display help information and exit.</li>
 * <li>-version: Display version information and exit.</li>
 * <li>-no-name-check: Skip name validation.</li>
 * <li>-null-warn: Enable warnings for null values.</li>
 * <li>-text-result: Output results in text format instead of XML.</li>
 * <li>-format-only: Perform format-only checks.</li>
 * <li>-format-only-pre3.1: Perform format-only checks for versions
 * pre-3.1.</li>
 * <li>-data-check-all: Perform data checks for all files.</li>
 * <li>-battery-check: Enable battery variable checks.</li>
 * <li>-psal-stats: Compute PSAL statistics for index files.</li>
 * <li>-list-file &lt;file&gt;: Specify a file containing a list of input
 * files.</li>
 * </ul>
 * Mandatory Arguments :
 * <ul>
 * <li>dac-name : File containing list of files to process</li>
 * <li>spec-dir : Directory path of specification files</li>
 * <li>output-dir : Directory path where results files will be placed</li>
 * <li>input-dir : Directory path where input files reside</li>
 * </ul>
 * Optionnal argument :
 * <ul>
 * <li>file-names : (Optional) List of files names to process</li>
 * </ul>
 * </p>
 */
public class Options {

	private static Options instance;

	private final boolean doBatteryChecks; // ..check metadate battery vars - default: no
	private final boolean doNameCheck; // ..check file name - default: yes
	private final boolean doNulls; // ..check for nulls in strings - default: no
	private final boolean doFormatOnly; // ..true: format-only; false: format and data checks
	private final boolean doFormatOnlyPre31; // ..true: format-only for pre-v3.1 - full checks v3.1 //..false: format
												// and data checks for all
	private final boolean doPsalStats; // ..true: compute PSAL stats for index file (core-profile only)
	private final boolean version;
	private final boolean help;
	private final boolean doXml;
	private final String listFile;// ..list file name
	private final List<String> inFileList; // ..list of input files
	private final String dacName;
	private final String specDirName;
	private final String outDirName;
	private final String inDirName;

	// ..standard i/o shortcuts
	static PrintStream stdout = new PrintStream(System.out);
	static PrintStream stderr = new PrintStream(System.err);
	private static final Logger log = LogManager.getLogger("Options");

	// ====== CONSTRUCTOR ======
	private Options(boolean doBatteryChecks, boolean doNameCheck, boolean doNulls, boolean doFormatOnly,
			boolean doFormatOnlyPre31, boolean doPsalStats, boolean version, boolean help, boolean doXml,
			String listFile, List<String> inFileList, String dacName, String specDirName, String outDirName,
			String inDirName) {
		super();
		this.doBatteryChecks = doBatteryChecks;
		this.doNameCheck = doNameCheck;
		this.doNulls = doNulls;
		this.doFormatOnly = doFormatOnly;
		this.doFormatOnlyPre31 = doFormatOnlyPre31;
		this.doPsalStats = doPsalStats;
		this.version = version;
		this.help = help;
		this.doXml = doXml;
		this.listFile = listFile;
		this.inFileList = inFileList;
		this.dacName = dacName;
		this.specDirName = specDirName;
		this.outDirName = outDirName;
		this.inDirName = inDirName;

		log.debug("doBatteryChecks = {}", doBatteryChecks);
		log.debug("doFormatOnly = {}", doFormatOnly);
		log.debug("doFormatOnlyPre31 = {}", doFormatOnlyPre31);
		log.debug("doNameCheck = {}", doNameCheck);
		log.debug("doNulls = {}", doNulls);
		log.debug("doXml = {}", doXml);
		log.debug("dacName = '{}'", dacName);
		log.debug("listFile = '{}'", listFile);
		log.debug("specDirName = '{}'", specDirName);
		log.debug("outDirName = '{}'", outDirName);
		log.debug("inDirName = '{}'", inDirName);
		log.debug("number of inFileList = " + (inFileList == null ? "null" : inFileList.size()));
	}

	/**
	 * Retrieves the singleton instance of {@code Options}, parsing command-line
	 * arguments with {@code extractOptionsFromArgs} if instance is null .
	 * 
	 * @param args : The command-line arguments passed to the application.
	 * @return The singleton instance of {@code Options}.
	 */
	public static synchronized Options getInstance(String args[]) throws IllegalArgumentException {

		if (instance == null) {

			instance = extractOptionsFromArgs(args);
		}
		return instance;
	}

	/**
	 * Parse command-line arguments. Exit application if bad arguments are provided.
	 * 
	 * @param args : The command-line arguments passed to the application.
	 * @return an Options object.
	 */
	private static Options extractOptionsFromArgs(String[] args) throws IllegalArgumentException {

		// Default values :
		String listFile = null;
		List<String> inFileList = null; // ..list of input files//..list file name
		boolean doBatteryChecks = false; // ..check metadate battery vars - default: no
		boolean doNameCheck = true; // ..check file name - default: yes
		boolean doNulls = false; // ..check for nulls in strings - default: no
		boolean doFormatOnly = false; // ..true: format-only; false: format and data checks
		boolean doFormatOnlyPre31 = true; // ..true: format-only for pre-v3.1 - full checks v3.1
											// ..false: format and data checks for all
		boolean doPsalStats = false; // ..true: compute PSAL stats for index file (core-profile only)
		boolean version = false;
		boolean help = false;
		boolean doXml = true;

		// loop trough the arguments provided and differentiate the option (start with
		// "-") and the positional parameters.
		int next = 0;
		for (; next < args.length; next++) {
			if (!args[next].startsWith("-")) {
				break; // it is not a flag but a positionnal parameters. Leave the for loop
			}

			switch (args[next]) {
			case "-help":
				help = true;
				break;
			case "-version":
				version = true;
				break;
			case "-no-name-check":
				doNameCheck = false;
				break;
			case "-null-warn":
				doNulls = true;
				break;
			case "-text-result":
				doXml = false;
				break;
			case "-format-only":
				doFormatOnly = true;
				break;
			case "-format-only-pre3.1":
				doFormatOnlyPre31 = true;
				break;
			case "-data-check-all":
				doFormatOnlyPre31 = false;
				break;
			case "-battery-check":
				doBatteryChecks = true;
				break;
			case "-psal-stats":
				doPsalStats = true;
				break;
			case "-list-file":
				if (++next < args.length) {
					listFile = args[next];
				} else {
					log.error("Error: Missing argument after '-list-file'.");
					throw new IllegalArgumentException("Error: Missing argument after '-list-file'.");
				}
				break;
			// ..obsolete arguments -- left in for backwards compatibility
			case "-no-fresh":
				log.error("Obsolete argument '-no-fresh' given. IGNORED");
				throw new IllegalArgumentException("Obsolete argument '-no-fresh' given. IGNORED");
			case "-full-traj-checks":
				log.error("Obsolete argument '-full-traj-checks' given. IGNORED");
				throw new IllegalArgumentException("Obsolete argument '-full-traj-checks' given. IGNORED");

			default:
				log.error("Invalid argument: '" + args[next] + "'");
				throw new IllegalArgumentException("Invalid argument: '" + args[next] + "'");
			}
		}

		// .....parse the positional parameters.....
		validateNumberOfPositionalArguments(args, next); // exit system if too few arguments

		String dacName = args[next++];
		String specDirName = args[next++];
		String outDirName = args[next++];
		String inDirName = args[next++];
		if (next < args.length) {
			inFileList = new ArrayList<String>(args.length - next);
			for (; next < args.length; next++) {
				inFileList.add(args[next]);

			}
		}

		return new Options(doBatteryChecks, doNameCheck, doNulls, doFormatOnly, doFormatOnlyPre31, doPsalStats, version,
				help, doXml, listFile, inFileList, dacName, specDirName, outDirName, inDirName);

	}

	/**
	 * Compare the total number of arguments to the number of positional arguments
	 * (must have at least 4)
	 * 
	 * @param args list of arguments
	 * @param next indice of the next argument
	 */
	private static void validateNumberOfPositionalArguments(String[] args, int next) throws IllegalArgumentException {
		if (args.length < (4 + next)) {
			log.error("too few arguments: " + args.length);
			throw new IllegalArgumentException("Too few arguments provided.");
		}
	}

	/**
	 * Validates that all mandatory arguments are provided and meet requirements.
	 * This includes checking DAC names, specification directories, and input
	 * directories. If validation fails, the application terminates with an
	 * appropriate error message.
	 */
	public void validateMandatoryArguments() {
		checkDacName(dacName);
		checkDirectory(inDirName);
		checkDirectory(specDirName);
	}

	/**
	 * Checks if the provided DAC name is valid by comparing it against DAC names
	 * referenced in ArgoReferenceTable.DACS. If the DAC name is invalid, logs an
	 * error and terminates the application.
	 */
	protected static void checkDacName(String dacName) {
		// .....check the DAC name.....
		boolean dacOK = false;
		for (ArgoReferenceTable.DACS d : ArgoReferenceTable.DACS.values()) {
			if (d.name.equals(dacName)) {
				dacOK = true;
				break;
			}
		}
		if (!dacOK) {
			log.error("\nERROR: Unknown DAC name = '" + dacName + "'");
			throw new IllegalArgumentException("\nERROR: Unknown DAC name = '" + dacName + "'");
		}
	}

	/**
	 * check if directory ppath provided exists.
	 * 
	 * @param directoryName
	 */
	protected static void checkDirectory(String directoryName) {
		File dir = new File(directoryName);
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("ERROR: " + directoryName + " directory is not a directory");
		}
	}

	// ===== GETTERS =====

	public boolean isDoBatteryChecks() {
		return doBatteryChecks;
	}

	public boolean isDoNameCheck() {
		return doNameCheck;
	}

	public boolean isDoNulls() {
		return doNulls;
	}

	public boolean isDoFormatOnly() {
		return doFormatOnly;
	}

	public boolean isDoFormatOnlyPre31() {
		return doFormatOnlyPre31;
	}

	public boolean isDoPsalStats() {
		return doPsalStats;
	}

	public boolean isVersion() {
		return version;
	}

	public boolean isDoXml() {
		return doXml;
	}

	public String getListFile() {
		return listFile;
	}

	public List<String> getInFileList() {
		return inFileList;
	}

	public String getDacName() {
		return dacName;
	}

	public String getSpecDirName() {
		return specDirName;
	}

	public String getOutDirName() {
		return outDirName;
	}

	public boolean isHelp() {
		return help;
	}

	public String getInDirName() {
		return inDirName;
	}

}
