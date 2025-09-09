package fr.coriolis.checker.specs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.coriolis.checker.core.ArgoDataFile;
import ucar.ma2.DataType;

/**
 * Creates an <i>"Argo file specifications"</i>. There are two forms of
 * 
 * <p>
 * This class does NOT implement the "format verification" process. See
 * {@link fr.coriolis.checker.core.ArgoDataFile#verifyFormat()
 * ArgoDataFile.verifyFormat} for more information.
 * <p>
 * <b>
 * 
 * TEMPLATE SPECIFICATION
 * 
 * </b>
 * <p>
 * A <i>Template Specification</i> defines the nominal structure of the Argo
 * netCDF files (the dimensions, the variables, and the attributes) and the
 * allowed physical parameters.
 * <p>
 * <b>
 * 
 * FULL SPECIFICATION
 * 
 * </b>
 * <p>
 * A <i>Full Specification</i> defines the structure of the Argo netCDF files
 * (the dimensions, the variables, and the attributes), the allowed physical
 * parameters, the optional parameters, allowed attribute "exceptions", allowed
 * technical parameter names/units, allowed configuration parameter names, and
 * various reference tables.
 * <p>
 * 
 * <b>
 * 
 * SPECIFICATION DEFINITION
 * 
 * </b>
 * <p>
 * The <i>specification</i> comprises the following files:
 * <ul>
 * <li><a href="#CDL-Spec">CDL Specification</a>
 * <li><a href="#Param-Spec">Physical Parameter Specification</a>
 * <li><a href="#Option-File">Optional Elements File</a> -- Optional file
 * <li><a href="#Attr-Regex">Attribute Regular Expressions</a> -- Optional file
 * <li><a href="#Tech-Param">Technical Parameter Names and Units</a>
 * <li><a href="#Config-Param">Configuration Parameter Names</a>
 * <li><a href="#Ref-Tables">Reference Tables</a>
 *
 *
 * </ul>
 * <p>
 * <a id="CDL-Spec"><b>
 * 
 * CDL Specification File
 * 
 * </b></a><br>
 * File name: argo-<i>&lt;file-type&gt;</i>-spec-v<i>&lt;version&gt;</i>.cdl<br>
 * Required
 * <p>
 * A standard netCDF CDL file (header only, no data) that defines the file
 * format including global attributes, dimensions, variables, and variable
 * attributes for each Argo file type and version accepted by the GDAC.<br>
 * Note: <i>Physical parameter</i> variables are <i>not</i> specified in this
 * file. (See <a href="#ParamSpec">"Physical Parameter Specification"</a> below)
 * <p>
 * <i>The format of the specification file is specific:</i>
 * <ul>
 * <li>Blank lines are ignored
 * <li>Everything following comment characters ("//") is ignored
 * <li>Special comment lines that start with "//@" define "constants" that can
 * be used when this file is used as a template. These "special constants" do
 * not affect the format checking process. Examples of these special constant
 * are: <br>
 * 
 * <pre>
 * <code>
 *       //@ DATA_TYPE = "Argo profile"
 *       //@ FORMAT_VERSION   = "2.2"
 *       //@ HANDBOOK_VERSION = "1.2"
 *       //@ REFERENCE_DATE_TIME = "19500101000000"
 * </code>
 * </pre>
 * 
 * <li>Every line <b>must</b> end with a ";".
 * </ul>
 *
 *
 * <p>
 * <a id="Option-File"><b>
 * 
 * Optional Elements File
 * 
 * </b></a><br>
 * File name: argo-<i>&lt;file-type&gt;</i>-spec-v<i>&lt;version&gt;</i>.opt<br>
 * Optional
 *
 * <p>
 * Specifies the elements (dimensions and variables) within the Argo file that
 * are optional. Generally, physical parameters are <i>not</i> included in this
 * file because this is defined in the "Physical Parameter Specification".
 * However, if they do occur in this file, it overrides the "Physical Parameter
 * Specification".
 * <p>
 * Additionally, the variables can be grouped such that all of the variables in
 * the group must either be omitted or be present. (see HISTORY_INSTITUTION
 * below for example: Either all related variables must be present or all must
 * be missing.)
 * <p>
 * Blank lines and comment lines (lines starting with "//") are ignored.
 * <p>
 * The format of this file is:<br>
 * 
 * <pre>
 * {@code
 * dimension-name1
 * variable-name1
 * group-name           //(head variable of group)
 * group-name: variable-name
 * group-name: variable-name
 * }
 * </pre>
 * <p>
 * Example:<br>
 * 
 * <pre>
 * {@code
 * N_TECH_PARAM
 *
 * //..make HISTORY_INSTITUTION optional (as an entire group)
 * HISTORY_INSTITUTION : HISTORY_STEP
 * HISTORY_INSTITUTION : HISTORY_SOFTWARE
 * HISTORY_INSTITUTION : HISTORY_SOFTWARE_RELEASE
 * HISTORY_INSTITUTION : HISTORY_REFERENCE
 * HISTORY_INSTITUTION : HISTORY_DATE
 * HISTORY_INSTITUTION : HISTORY_ACTION
 * HISTORY_INSTITUTION : HISTORY_PARAMETER
 * HISTORY_INSTITUTION : HISTORY_START_PRES
 * HISTORY_INSTITUTION : HISTORY_STOP_PRES
 * HISTORY_INSTITUTION : HISTORY_PREVIOUS_VALUE
 * HISTORY_INSTITUTION : HISTORY_QCTEST
 * }
 * </pre>
 *
 * <p>
 * <a id="Attr-Regex"><b>
 * 
 * Attribute Regular Expressions
 * 
 * </b></a><br>
 * File name:
 * argo-<i>&lt;file-type&gt;</i>-spec-v<i>&lt;version&gt;</i>.attr_regexp<br>
 * Optional
 *
 * <p>
 * Specifies allowed exceptions to the attributes (variable and global) defined
 * in the "CDL Specification". The exceptions are specified using "regular
 * expression" syntax.
 * <p>
 * Blank lines and comment lines (lines starting with "//") are ignored.
 * <p>
 * The format of this file is:<br>
 * 
 * <pre>
 * {@code
 * //..global attributes
 * :attr-name1 = reg-exp
 * :attr-name2 = reg-exp
 * //..variable attributes
 * variable-a:attr-name3 = reg-exp
 * variable-b:attr-name4 = reg-exp
 * }
 * </pre>
 * <p>
 * Example:<br>
 * 
 * <pre>
 * {@code
 * //  Changes in BPHASE_DOXY between v2.2 and v2.31 that have not been dealt with
 *
 * BPHASE_DOXY:long_name = (BPHASE_DOXY)|(OPTICAL PHASE-SHIFT FROM OXYGEN SENSOR);
 * BPHASE_DOXY:comment = (Uncalibrated phase shift reported by sensors such as Aandera Optode)|(In situ measurement);
 * }
 * </pre>
 * <p>
 *
 *
 * <p>
 * <a id="Param-Spec"><b>
 * 
 * Phyical Parameter Specification
 * 
 * </b></a><br>
 * File name: argo-physical_params-spec-v<i>&lt;version&gt;</i><br>
 * Required
 * <p>
 * A text file based on the Excel spreadsheet available online at the ADMT
 * website.
 *
 *
 *
 * <p>
 * <a id="Tech-Param"><b>
 * 
 * Technical Parameter Names and Units
 * 
 * </b></a><br>
 * File names:<br>
 * &nbsp;&nbsp;&nbsp; argo-tech_names-spec-v<i>&lt;version&gt;<br>
 * &nbsp;&nbsp;&nbsp; argo-tech_units-spec-v<i>&lt;version&gt;</i><br>
 * Required
 *
 * <p>
 * Specifies the allowed names and units of technical parameters. These files
 * are built from the official list of Technical Parameter Names maintained as
 * an Excel file on the ADMT website.
 * <p>
 * Blank lines and comment lines (lines starting with "//") are ignored.
 * <p>
 * The format of each file is:<br>
 * 
 * <pre>
 * {@code
 * name-1
 * name-2
 * name-3
 * }
 * </pre>
 * <p>
 * Example:<br>
 * 
 * <pre>
 * {@code
 * FLAG_GreyListTEMP
 * FLAG_SpeedMaximumCheck
 * NUMBER_ParkAndProfileCycleCounter
 * PRES_SurfaceOffsetCorrectedNotResetNegative_1dbarResolution
 * }
 * </pre>
 * <p>
 *
 *
 * <p>
 * <a id="Tech-Param"><b>
 * 
 * Configuration Parameter Names
 * 
 * </b></a><br>
 * File names: argo-bio_config_names-spec-v<i>&lt;version&gt;<br>
 * argo-core_config_names-spec-v<i>&lt;version&gt;<br>
 * argo-tech_units-spec-v<i>&lt;version&gt;</i><br>
 * <-----NOTICE Required
 *
 * <p>
 * Specifies the allowed names and units of configuration parameters. These
 * files are built from the official list of Configuration Parameter Names and
 * Technical Parameter UNITS maintained as an Excel file on the ADMT website.
 * <p>
 * Blank lines and comment lines (lines starting with "//") are ignored.
 * <p>
 * The format of each file is:<br>
 * 
 * <pre>
 * {@code
 * name-1
 * name-2
 * name-3
 * }
 * </pre>
 * <p>
 * Example:<br>
 * 
 * <pre>
 * {@code
 * FLAG_GreyListTEMP
 * FLAG_SpeedMaximumCheck
 * NUMBER_ParkAndProfileCycleCounter
 * PRES_SurfaceOffsetCorrectedNotResetNegative_1dbarResolution
 * }
 * </pre>
 * <p>
 * 
 * <p>
 * <a id="Ref-Tables"><b>
 * 
 * Reference Tables
 * 
 * </b></a><br>
 * File name: ref_table-<i>&lt;n&gt;</i><br>
 * Required
 * <p>
 * Assorted reference tables from the Users Manual. The tables required for file
 * checking are: 2, 2a, 5, 6, 8, 9, 10, 16, 19, 20, 22, 23, 24, 25, 26, 27
 * <p>
 * The format used for each file is the same and is "one size fits all". They
 * are multi-column files but only the first column is necessary for file
 * checking. The columns are separated by the "&brvbar;" character. Leading and
 * trailing spaces in a column are ignored.
 * <p>
 * The format is:<br>
 * 
 * <pre>
 * {@code
 * key | col - 2 | col - 3
 * }
 * </pre>
 * <p>
 * Example:<br>
 * 
 * <pre>
 * {@code
 *   | No QC was performed
 * A | N = 100%; All levels have good data
 * B | 75% <= N < 100%
 * C | 50% <= N < 75%
 * D | 25% <= N < 50%
 * E | 0% < N < 25%
 * F | No levels have good data
 * }
 * </pre>
 * <p>
 * 
 * <p>
 * <a id="VARIABLE GROUPS"><b>
 * 
 * VARIABLE GROUPS </b></a><br>
 * A discussion of the "variable groups" is required. It's very non-intuitive
 * but is needed to support the "existence rules" for parameter variables. And
 * may intersect with the OPT specification file.
 * <p>
 * The basic variables that implement a parameter are:
 * <ul>
 * <li>PARAM
 * <li>PROFILE_PARAM_QC <--- "qc-variable"
 * <li>PARAM_QC <--- "qc-variable"
 * <li>PARAM_ADJUSTED <--- "adj-variable"
 * <li>PARAM_ADJUSTED_QC <--- "adj-variable"
 * <li>PARAM_ADJUSTED_ERROR <--- "adj-variable"
 * </ul>
 * <p>
 * The concept of "groups" is introduced to help enforce the rules. A variable
 * has a "primary group" -- the group that it is primarily associated with. A
 * variable may also belong to one or more ancillary groups.
 *
 * The basic concept is that when a variable is reported, all members of its
 * primary group must also be reported.
 * <p>
 * CORE- AND BIO- PARAMETERS - THE BASIC RULE: If any one of the variables exist
 * in the file, they ALL must be in the file (easy-peasy)
 * <p>
 * Implementation: <br>
 * Each of the 6 variables has the same primary group == "param-name"
 * <ul>
 * <li>Primary group is stored in "Map" varGroup.put(var-name, pri-group)
 * <li>A group's members are stored in groupMembers <br>
 * member-list = groupMembers.get(param-name)
 * </ul>
 * <p>
 * INTERMEDIATE PARAMETERS - THE RULES:
 * <p>
 * This is where it gets dicey.
 * <p>
 * There are two groups: 1) the qc-variable group and 2) the adj-variable group
 * <p>
 * Implementation:
 * <ul>
 * <li>PARAM has the "primary group" of PARAM_qc
 * <p>
 * <li>"qc-variables"
 * <ul>
 * <li>These 2 variables have a "primary group" of PARAM_qc <br>
 * varGroup.get("qc-variable") returns PARAM_qc group
 * <li>Group: PARAM_qc has members PARAM and (2) "qc variables" <br>
 * goupMembers.get(PARAM_qc) returns PARAM and qc-variables
 * </ul>
 * <p>
 * <li>"adj-variables"
 * <ul>
 * <li>These 3 variable have a "primary group" of PARAM_adj <br>
 * varGroup.get("adj-variable") returns PARAM_adj group
 * <li>Group: PARAM_adj contains all variables (PARAM, qc- and adj-variables)
 * <br>
 * goupMembers.get(PARAM_adj) returns PARAM, qc-, and adj-variables
 * </ul>
 * <p>
 * (((A note for future reference: If a param-variable does not have a primary
 * group then it can exist by itself. (This was once allowed for PARAM.) In this
 * case,
 * <ul>
 * <li>It doesn't know it belongs to a group <br>
 * varGroup.get(PARAM) returns null <br>
 * groupMembers.get(PARAM) returns null
 * </ul>
 * )))
 * <p>
 * <li>PARAM_stat (*_STD, *_MED) variables: <br>
 * (currently not allowing PROFILE_*_QC, *_QC variables <br>
 * PARAM_stat no primary group (it's independent) <br>
 * PARAM_stat_ADJUSTED
 * </ul>
 * 
 * <p>
 * <a id="identifier"><b>
 * 
 * heading
 * 
 * </b></a><br>
 * File name: name<br>
 * opt-req
 * <p>
 * description
 * <p>
 * The format of this file is:<br>
 * 
 * <pre>
 * {@code
 * format
 * }
 * </pre>
 * <p>
 * Example:<br>
 * 
 * <pre>
 * {@code
 * example
 * }
 * </pre>
 * <p>
 *
 *
 *
 * @author Mark Ignaszewski
 * @version $HeadURL:
 *          https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoFileSpecification.java
 *          $
 * @version $Id: ArgoFileSpecification.java 1323 2022-04-19 16:13:39Z
 *          ignaszewski $
 */
public class ArgoFileSpecification {

	// ............................................
	// VARIABLES
	// ............................................

	// ..........Class variables..............
	private static final String BLANK_MESSAGE = "";

	public static char[] DATA_TYPE;
	public static char[] FORMAT_VERSION;
	public static char[] HANDBOOK_VERSION;
	public static char[] REFERENCE_DATE_TIME;

	private static String message = BLANK_MESSAGE;

	// .......physical parameter variables...........

	private static final int nParamFields = 19; // ..number of significant fields (min number)
	private static final HashMap<String, ArgoDataFile.FileType> paramFileTypes = new HashMap<String, ArgoDataFile.FileType>();

	// ..attribute constants

	/**
	 * A REGEX pattern that can be used to match any of the special ATTR_* strings.
	 */
	public static final String ATTR_SPECIAL_REGEX = "^<.>.*";

	/**
	 * A REGEX pattern that can be used to match any of the special ATTR_* strings.
	 */
	public static final int ATTR_SPECIAL_LENGTH = 3;

	/**
	 * When an attribute specification starts with this string, the attribute is
	 * ignored during file checking. The attribute can be present, or not, and its
	 * value is ignored. The attribute is part of the specification, technically.
	 */
	public static final String ATTR_IGNORE = "<*>"; // ..present, or not, ignore value

	/**
	 * When an attribute specification starts with this string, the attribute
	 * <b>value</b> is ignored during file checking. The attribute must be present
	 * but its <b>value</b> is ignored. The attribute is part of the specification,
	 * technically.
	 */
	public static final String ATTR_IGNORE_VALUE = "<+>"; // ..must be present, ignore value

	/**
	 * When an attribute specification starts with this string, the attribute is
	 * <b>not allowed</b> in the data file. Since "extra attributes" are ignored,
	 * this is a method to force an attribute to be excluded.
	 */
	public static final String ATTR_NOT_ALLOWED = "<->"; // ..not allowed (can't be present)

	private static final String axis = new String("axis");
	private static final String c_format = new String("C_format");
	private static final String comment = new String("comment");
	private static final String comment_on_resolution = new String("comment_on_resolution");
	private static final String conventions = new String("conventions");
	private static final String fillValue = new String("_FillValue");
	private static final String fillValueBLANK = new String(" ");
	private static final Number fillValueFloatPARAM = new Float(99999.f);
	private static final Number fillValueDoublePARAM = new Double(99999.f);
	private static final String fortran_format = new String("FORTRAN_format");
	private static final String long_name = new String("long_name");
	private static final String standard_name = new String("standard_name");
	private static final String units = new String("units");
	private static final String valid_min = new String("valid_min");
	private static final String valid_max = new String("valid_max");
	private static final String resolution = new String("resolution");

	private static final String prmQcLName = new String("quality flag");
	private static final String prmQcConventions = new String("Argo reference table 2");

	private static final String prfQcLName = new String("Global quality flag of ");
	private static final String prfQcConventions = new String("Argo reference table 2a");

	public static class AttrRegex {
		public Pattern pattern;
		public boolean warn;
	}

	private static final HashMap<String, String> NC_FILL_TYPES = new HashMap<String, String>();

	// ......define and initialize the pattern matcher objects......
	static Pattern pActive; // ..status: active
	static Pattern pAttrRegex; // ..attribute regex
	static Pattern pBlankOrComment; // ..match a blank line or a comment line
	static Pattern pComment; // ..comment
	static Pattern pCloseTag; // ..CDL closing bracket
	static Pattern pDataTag; // ..CDL "data:" tag
	static Pattern pDeprecated; // ..status: deprecated
	static Pattern pDeleted; // ..status: deleted
	static Pattern pDimDef;
	static Pattern pDimPattern;
	static Pattern pDimTag; // ..CDL "data:" tag
	static Pattern pGlblAttr; // ..CDL global attribute
	static Pattern pN_VALUES; // ..comment indicates extra dimension
	static Pattern pOpenTag; // ..CDL "netcdf <name> {" tag
	static Pattern pOptVar; // ..OPT definition
	static Pattern pParam; // ..Param file
	static Pattern pRefused; // ..status: refused
	static Pattern pUnderway; // ..status: pulication/creation underway
	static Pattern pVarAttr; // ..CDL variable attribute
	static Pattern pVarDef; // ..CDL variable definition
	static Pattern pVarDim; // ..CDL variable dimesions
	static Pattern pVarTag; // ..CDL "variables:" tag
	static Pattern pPRESn; // ..Param name PRES[0-9]?
	static Pattern pParamEndInDigit; // ..Param name ending in a digit

	// ..logger
	private static final Logger log = LogManager.getLogger("ArgoFileSpecification");

	// .............initializers............

	static {
		// ..match an attribute regex line
		// ..group 1: variable:attribute; group 2: variable; group 3: attr; group 4:
		// regex
		pAttrRegex = Pattern.compile("^\\s*((\\w*|\\*):(\\w+))\\s*=\\s*(.*);\\s*(WARN|NOWARN)?\\s*");

		// ..match a blank line (or a line with just comments)
		pBlankOrComment = Pattern.compile("^\\s*(?://.*)*");

		// ..match a comment (any where on the line) - recognize the "//@" comments
		// ..group 1: the word following "@"; group 2: the setting
		pComment = Pattern.compile("//(?:@\\s*(\\w+)\\s*=\\s*\"(.+)\")?.*$");

		// ..match the line "}" that starts the data section of a CDL file
		pCloseTag = Pattern.compile("^\\s*}\\s*(?://.*)*$");

		// ..match the line "data:" that starts the data section of a CDL file
		pDataTag = Pattern.compile("^\\s*data:\\s*$");

		// ..match a dimension specification. 2 options:
		// ..1) NAME = #### (#=digit)
		// .. group 1: name; group 2: ####; group 3: ####; group 4 = null
		// ..2) NAME = word (ie, UNLIMITED, _unspecified_, etc)
		// .. group 1: name; group 2: word; group 3: null; group 4 = word
		pDimDef = Pattern.compile("^\\s*(\\w+)\\s*=\\s*((\\d+)|(\\w+));.*");

		// ..match a "pattern" dimension specification.
		// .. NAME = _extra_
		// .. group 1: name-regex
		pDimPattern = Pattern.compile("^\\s*([\\S]+)\\s*=\\s*_extra_\\s*;.*");

		// ..match the line "dimensions:" that starts the dimension section of CDL
		pDimTag = Pattern.compile("^\\s*dimensions:\\s*$");

		// ..match a global attribute definition line
		// ..group 1: attribute name; group 2/3: string/number definition
		pGlblAttr = Pattern.compile(
				"^\\s*:(\\w+)\\s*=\\s*(?:\"(.*)\"\\s*|(.*));\\s*(?:/\\*\\s*REGEX\\s*=\\s*\"(.*)\"\\s*\\*/)*\\s*");

		// ..match the param-file comment with N_VALUESxx (extra dimension)
		pN_VALUES = Pattern.compile(".*N_VALUES.*");

		// ..match the "netcdf (<name>) {" line that starts the CDL
		// ..group 1: name
		pOpenTag = Pattern.compile("^netcdf\\s+(\\S+)\\s+\\{\\s*");

		// ..match an "optional variable" definition
		// ..group 1: variable name; group 2: related variable
		// pOptVar = Pattern.compile("^\\s*(\\w+)\\s*(?::\\s*(\\w+))?");
		pOptVar = Pattern.compile("^\\s*(\\w+)\\s*(?::\\s*(\\w+))?.*");

		// ..match the first field in the parameter file
		// ..group 1: parameter name
		pParam = Pattern.compile("^\\s*(\\w+)\\s*(?::\\s*(\\w+))?.*");

		// ..match a variable attribute definition line
		// ..group 1: variable name; group 2: attribute name; group 3/4: string/number
		// definition
		pVarAttr = Pattern.compile("^\\s*(\\w+):(\\w+)\\s*=\\s*(?:\"(.*)\"\\s*|(.*));\\s*");

		// ..match a variable definition line
		// ..group 1: type; group 2: name; group 3: dimensions
		pVarDef = Pattern.compile("^\\s*(\\w+)\\s+(\\w+)\\s*(\\(.*\\))?;\\s*");

		// ..match the dimensions of a variable definition
		// ..group 1: alternate-dimension definition ( alt1|alt2|alt3 )
		// ..group 2: a regular dimension definition
		pVarDim = Pattern.compile("((?:\\w+\\|)+\\w+)|(\\w+)");

		// ..match the line "variables:" that starts the dimension section of CDL
		pVarTag = Pattern.compile("^\\s*variables:\\s*$");

		// ..match the PRES and PRESn variable names
		pPRESn = Pattern.compile("^PRES\\d?");

		// ..match a param name ending in a digit
		pParamEndInDigit = Pattern.compile(".*\\d$");

		// ..match the "status" codes (proposed)
		pActive = Pattern.compile("(?i)(active|approved).*");
		pDeprecated = Pattern.compile("(?i)deprecated.*");
		pDeleted = Pattern.compile("(?i)(deleted|obsolete).*");
		pRefused = Pattern.compile("(?i)refused.*");
		pUnderway = Pattern.compile("(?i)(publication|creation) +underway.*");

		// ..build type-map for param variables
		paramFileTypes.put("prof", ArgoDataFile.FileType.PROFILE);
		paramFileTypes.put("traj", ArgoDataFile.FileType.TRAJECTORY);
		paramFileTypes.put("b-prof", ArgoDataFile.FileType.BIO_PROFILE);
		paramFileTypes.put("b-traj", ArgoDataFile.FileType.BIO_TRAJECTORY);

		// ..build type-map for nc_fill_values
		NC_FILL_TYPES.put("NC_FILL_CHAR", "0");
		// NC_FILL_TYPES.put("NC_FILL_DOUBLE",
		// ucar.nc2.iosp.netcdf3.N3iosp.NC_FILL_DOUBLE.toString());
		NC_FILL_TYPES.put("NC_FILL_DOUBLE", "9.969209968386869E36");
		NC_FILL_TYPES.put("NC_FILL_FLOAT", "9.969209968386869E36f");
		NC_FILL_TYPES.put("NC_FILL_INT", "-2147483647");
		NC_FILL_TYPES.put("NC_FILL_LONG", "-9223372036854775806L");
		NC_FILL_TYPES.put("NC_FILL_SHORT", "-32767");

	}

	// .....object variables......
	public ArgoConfigTechParam ConfigTech;

	private String cdlFileName;
	private String optFileName;
	private String prmFileName;
	private String prmFileNameAux;
	private String regFileName;

	private boolean attrRegex;
	private boolean optionalVars;
	private String specName; // ..name used to identify spec

	private LinkedHashMap<String, ArgoDimension> dimHash; // ..name to def
	private ArrayList<Pattern> extraDimPattern;// .."extra dims" patterns
	private HashMap<String, String> metaHash; // ..name to setting
	private HashSet<String> optVar; // ..optional variables
	private HashSet<String> interPhysParam; // ..intermediate physical parameter
	private HashMap<String, HashSet<String>> groupMembers; // ..var groups: grp-name->grp mem
	private HashMap<String, String> varGroup; // ..var-name to grp-name
	private LinkedHashMap<String, ArgoVariable> varHash; // ..name to def
	private LinkedHashMap<String, ArgoAttribute> gAttrHash; // ..glbl attr names
	private HashMap<String, AttrRegex> regexHash; // ..attribute regexs

	private HashSet<String> depParamNameList; // ..list of deprecated physical param names
	private HashSet<String> physParamNameList;// ..list of physical param names
	private HashSet<String> physParamVarList; // ..list of phys-prm-related variables

//............................................
//              CONSTRUCTORS
//............................................

	// ........... (specDir, file_type, version).......
	/**
	 * Builds the Argo file specification for the specified type and version from
	 * the specification files in the specfication directory
	 *
	 * @param fullSpec false = open template spec; true = open full spec
	 * @param specDir  the string directory name for the specification files
	 * @param fType    the filetype of the Argo file (static enum in ArgoDataFile)
	 * @param version  the string version of the format
	 * @throws IOException for I/O errors
	 */
	public ArgoFileSpecification(boolean fullSpec, String specDir, ArgoDataFile.FileType fType, String version)
			throws IOException {
		openSpecification(fullSpec, specDir, fType, version);
	} // ..end constructor

	// ............................................
	// ACCESSORS
	// ............................................

	/**
	 * Returns the dimension definition for the given name
	 * 
	 * @param name the string name of the requested dimension
	 * @return The ArgoDimension object. Null if not defined.
	 */
	public ArgoDimension getDimension(String name) {
		return dimHash.get(name);
	}

	/**
	 * Adds an "extra dimension" to the specification. These are dimensions added to
	 * allow for an extra data dimension in the physical parameters.
	 *
	 * The name of the dimension must: 1) match an "extra dimension" pattern in the
	 * CDL spec file 2) not already exist in the specification
	 *
	 * @param name   the string name of the requested dimension
	 * @param length the length of the requested dimension
	 * @return The ArgoDimension object. Null if not defined.
	 */
	public ArgoDimension addExtraDimension(String name, int length) {
		ArgoDimension aDim = dimHash.get(name);

		if (aDim != null) {
			return null;
		}

		for (Pattern extName : extraDimPattern) {
			if (extName.matcher(name).matches()) {
				aDim = new ArgoDimension(name, length);
				aDim.setExtraDimension();
				dimHash.put(name, aDim);
			}
		}

		return aDim;
	}

	/**
	 * Clear "extra dimensions" from the specification.
	 */
	public void clearExtraDimensions() {
		for (String dim : dimHash.keySet()) {
			if (dimHash.get(dim).isExtraDimension()) {
				dimHash.remove(dim);
			}
		}
	}

	/**
	 * Returns all dimension definitions in this file type.
	 * 
	 * @return The ArgoDimension objects. Null if not defined.
	 */
	public Collection<ArgoDimension> getDimensions() {
		return dimHash.values();
	}

	/**
	 * Returns all dimension names.
	 * 
	 * @return The string names. Null if not defined.
	 */
	public Set<String> getDimensionNames() {
		return new HashSet<String>(dimHash.keySet());
	}

	/**
	 * Returns the global attribute for a given name
	 * 
	 * @param name the string name of the requested attribute
	 * @return The ArgoAttibute object. Null if not defined.
	 */
	public ArgoAttribute getGlobalAttribute(String name) {
		return gAttrHash.get(name);
	}

	/**
	 * Returns the global attributes for the specification
	 * 
	 * @return The Set of ArgoAttibute entries. Null if not defined.
	 */
	public Collection<ArgoAttribute> getGlobalAttributes() {
		if (gAttrHash == null) {
			return null;
		}
		return gAttrHash.values();
	}

	/**
	 * Returns all global attribute names for this specification.
	 * 
	 * @return The string names. Null if not defined.
	 */
	public Set<String> getGlobalAttributeNames() {
		return new HashSet<String>(gAttrHash.keySet());
	}

	/**
	 * Returns a List of allowed PARAM names
	 * 
	 * @return an ArrayList of allowed PARAM names
	 */
	public ArrayList<String> getPhysicalParamNames() {
		return new ArrayList<String>(physParamNameList);
	}

	/**
	 * Returns the variable definition for the given variable
	 * 
	 * @param name the string name of the requested variable
	 * @return The ArgoDimension objects. Null if not defined.
	 */
	public ArgoVariable getVariable(String name) {
		return varHash.get(name);
	}

	/**
	 * Returns all variables defined for this file.
	 * 
	 * @return The ArgoVariable objects. Null if not defined.
	 */
	public Collection<ArgoVariable> getVariables() {
		return varHash.values();
	}

	/**
	 * Returns all variables names for this file.
	 * 
	 * @return The string names. Null if not defined.
	 */
	public ArrayList<String> getSpecVariableNames() {
		return new ArrayList<String>(varHash.keySet());
	}

	/**
	 * Returns the meta-data value for the given name.
	 * 
	 * @param name the string name of the requested meta-data
	 * @return The string names. Null if not defined.
	 */
	public String getMeta(String name) {
		return metaHash.get(name);
	}

	/**
	 * Returns the <i>message</i> returned by the most recent activity in this Class
	 * 
	 * @return The string message.
	 */
	public String getMessage() {
		return new String(message);
	}

	/**
	 * Returns the name of the specification for this file
	 * 
	 * @return The string spec name.
	 */
	public String getSpecName() {
		return new String(specName);
	}

	/**
	 * Determines if the named dimension or variable is optional
	 * 
	 * @param name the string element name.
	 * @return True - named element is optional; False - named element is required
	 */
	public boolean isOptional(String name) {
		return optVar.contains(name);
	}

	/**
	 * Determines if the name is a physical parameter name
	 * 
	 * @param name the parameter name in question.
	 * @return True - name is valid physical parameter name; False - parameter name
	 *         is NOT valid (or spec not opened)
	 * 
	 */
	public boolean isPhysicalParamName(String name) {
		if (physParamNameList == (HashSet<String>) null) {
			return false;
		}
		return physParamNameList.contains(name);
	}

	/**
	 * Determines if the physical parameter name is a deprecated parameter
	 * 
	 * @param name the parameter name in question.
	 * @return True - name is valid physical parameter name; False - parameter name
	 *         is NOT valid (or spec not opened)
	 * 
	 */
	public boolean isDeprecatedPhysicalParam(String name) {
		if (depParamNameList == (HashSet<String>) null) {
			return false;
		}
		return depParamNameList.contains(name);
	}

	/**
	 * Determines if the named variable is an "intermediate physical parameter"
	 * 
	 * @param name the string element name.
	 * @return True - named element is optional; False - named element is required
	 */
	public boolean isInterPhysParam(String name) {
		return interPhysParam.contains(name);
	}

	/**
	 * Returns the "group name" that the named element belongs to.
	 * 
	 * @param name the string element name.
	 * @return the string group name. Null if not defined.
	 */
	public String inGroup(String name) {
		String s = varGroup.get(name);
		if (s == null) {
			return null;
		}
		return new String(s);
	}

	/**
	 * Returns a vector of the elements in the named group
	 * 
	 * @param groupName the string group name
	 * @return the elements within the named group
	 */
	public Set<String> groupMembers(String groupName) {
		return new HashSet<String>(groupMembers.get(groupName));
	}

	/**
	 * Returns the number of elements in the named group
	 * 
	 * @param groupName the string group name
	 * @return the number of elements in the group. -1 if not defined.
	 */
	public int nVarInGroup(String groupName) {
		if (groupMembers.containsKey(groupName)) {
			return groupMembers.get(groupName).size();
		} else {
			return -1;
		}
	}

	/**
	 * Returns the regex Pattern for a variable and attribute
	 * 
	 * @param variableName  the variable name
	 * @param attributeName the attribute name
	 * @return the AttrRegex for this attribure. 'null' if not defined
	 */
	public AttrRegex getAttrRegex(String variableName, String attributeName) {
		AttrRegex p = regexHash.get(variableName + ":" + attributeName);

		if (p == null) {
			p = regexHash.get("*:" + attributeName);
		}

		return p;
	}

	// ............................................
	// METHODS
	// ............................................

	// .................openSpecification............
	/**
	 * Opens a <i>full specification</i> for the specified file type and version.
	 * This includes all of the elements defined
	 * 
	 * @param fullSpec false = open template spec; true = open full spec
	 * @param specDir  the string name of the directory containing the spec files
	 * @param fType    the file type
	 * @param version  the string specification version
	 */
	public void openSpecification(boolean fullSpec, String specDir, ArgoDataFile.FileType fType, String version)
			throws IOException {
		// .....initialize variables.....

		// ..specification name
		specName = fType.specType + ":v" + version.trim();

		// ..file names
		String prefix = specDir.trim() + "/argo-";
		cdlFileName = prefix + fType.specType + "-spec-v" + version.trim() + ".cdl";
		optFileName = prefix + fType.specType + "-spec-v" + version.trim() + ".opt";
		prmFileName = prefix + "physical_params-spec-v" + version.trim();
		prmFileNameAux = prefix + "physical_params-spec-v" + version.trim() + ".aux";
		regFileName = prefix + fType.specType + "-spec-v" + version.trim() + ".attr_regexp";

		if (log.isDebugEnabled()) {
			log.debug("fullSpec = {}", fullSpec);
			log.debug("specName = {}", specName);
			log.debug("cdlFileName = '{}'", cdlFileName);
			log.debug("optFileName = '{}'", optFileName);
			log.debug("prmFileName = '{}'", prmFileName);
			log.debug("prmFileNameAux = '{}'", prmFileNameAux);
			log.debug("regFileName = '{}'", regFileName);
		}

		// ..hashes for dimensions and variables, etc
		depParamNameList = new HashSet<String>(10);
		dimHash = new LinkedHashMap<String, ArgoDimension>();
		extraDimPattern = new ArrayList<Pattern>();
		gAttrHash = new LinkedHashMap<String, ArgoAttribute>();
		interPhysParam = new HashSet<String>(150);
		metaHash = new HashMap<String, String>();
		optVar = new HashSet<String>(150);
		physParamNameList = new HashSet<String>(150);
		physParamVarList = new HashSet<String>(150);
		regexHash = new HashMap<String, AttrRegex>(25);
		varGroup = new HashMap<String, String>(100);
		varHash = new LinkedHashMap<String, ArgoVariable>(150);
		groupMembers = new HashMap<String, HashSet<String>>();

		// ..............open the specification...................
		// ..CDL file -- this is required
		boolean status = parseCdlFile();
		if (!status) {
			throw new IOException("Parsing CDL spec failed: " + message);
		}

		// ..physical parameter file
		if (paramFileTypes.containsValue(fType)) {
			// ..this file-type contains physical parameter structures
			log.info("parsing '" + prmFileName + "' for type '" + fType + "' (data-structures)");
			try {
				status = parseParamFile(fType, version, false);
			} catch (IOException e) {
				throw e;
			}

		} else {
			// ..this file-type only needs to know physical parameter names
			log.info("parsing '" + prmFileName + "' for type '" + fType + "' (list-only)");
			try {
				status = parseParamFile(fType, version, true);
			} catch (IOException e) {
				throw e;
			}
		}

		// ..this makes "variable groups" for the variables that are always grouped
		addPersistentGroups();

		// ..option file -- optional
		try {
			status = parseOptFile();
		} catch (IOException e) {
			throw e;
		}
		if (!status) {
			log.info("No optional parameters");
		}

		// ..end of template specification
		// ..rest for full specification

		if (fullSpec) {
			// ..initialize the reference tables..
			ArgoReferenceTable ref = new ArgoReferenceTable(specDir);

			// ..attribute regex file -- optional
			status = parseAttrRegexFile();
			if (!status) {
				log.info("No 'attr_regexp' file");
			}

			// ..meta-data configuration parameter file
			if (fType == ArgoDataFile.FileType.METADATA && version.startsWith("3")) {
				ConfigTech = new ArgoConfigTechParam(specDir, version, true, false);
			}

			// ..technical parameter file
			if (fType == ArgoDataFile.FileType.TECHNICAL) {
				ConfigTech = new ArgoConfigTechParam(specDir, version, false, true);

				// ..for each <tech_param> entry, automatically make <tech_param>* variables too
				addOptionnalTechParamVariables();

			}
		}
	} // ..end openFullSpecification

	// .............................................
	// ............addPersistentGroups..............
	// .............................................
	private void addPersistentGroups() {
		// ..make a group for HISTORY_ variables
		// ..this is done even
		// ..see VARIABLE GROUPS documentation at the top

		String key = "HISTORY_INSTITUTION";
		HashSet<String> mem;

		if (varHash.containsKey(key)) { // ..new group - init
			if (!groupMembers.containsKey(key)) {
				groupMembers.put(key, new HashSet<String>());
			}
			mem = groupMembers.get(key);

			for (String v : varHash.keySet()) {
				if (v.startsWith("HISTORY_")) {
					mem.add(v);
					log.debug("group '{}': add member '{}'", key, v);
				}
			}
		}
	}

	// ..................................................
	// ........TECH TIME SERIES SPECIAL HANDLING.........
	// ..................................................
	/**
	 * Argo Technical file can have times series of TECH_PARAM. For each
	 * <tech_param> entry, automatically make <tech_param>* variables too. These
	 * variables are members of group : JULD, CYCLE_NUMBER_MEAS, MEASUREMENT_CODE,
	 * <TECH_PARAM>. SO for each <tech_param>, create a group named <tech_param> and
	 * containing variable <tech_param> and JULD, CYCLE_NUMBER_MEAS,
	 * MEASUREMENT_CODE and all potential variables defined under the
	 * "TECH_TIMESERIES" group name defined in the opt file. Summary : for each
	 * <Tech_param> create a group named <tech_param> and add into this group all
	 * variables contained in the groupe(if exists) "TECH_TIMESERIES".
	 */
	private void addOptionnalTechParamVariables() {
		// get tech param names (without unit)
		for (String techParamName : ConfigTech.getTechParamList()) {
			createAndAddToGroupOptionnalTechParamVariable(techParamName);
		}

	}

	/**
	 * Build the <TECH_PARAM> variable. The long_name and unit are retrieved from
	 * 
	 * @param techParamName : variable name (without the unit).
	 */
	private void createAndAddToGroupOptionnalTechParamVariable(String techParamName) {
		// 1 - create the <tech_param> variable :
		ArgoDimension dimTechParam[] = new ArgoDimension[1];
		dimTechParam[0] = dimHash.get("N_TECH_MEASUREMENT");
		ArgoVariable techParamVar = new ArgoVariable(techParamName, DataType.FLOAT, dimTechParam, techParamName);

		// may have multiple units or longç_name for a same parameter name. In those
		// cases, we ignore units and/or long_name:
		if (ConfigTech.getParamAuthorizedLongName().get(techParamName) == null
				|| ConfigTech.getParamAuthorizedLongName().get(techParamName).size() != 1) {
			addAttr(techParamVar, long_name, ATTR_IGNORE_VALUE + "%15.1f", DataType.STRING);
		} else {
			addAttr(techParamVar, long_name, ConfigTech.getParamAuthorizedLongName().get(techParamName).get(0),
					DataType.STRING);
		}
		if (ConfigTech.getParamAuthorizedUnits().get(techParamName) == null
				|| ConfigTech.getParamAuthorizedUnits().get(techParamName).size() != 1) {
			addAttr(techParamVar, units, ATTR_IGNORE_VALUE + "%15.1f", DataType.STRING);
		} else {
			addAttr(techParamVar, units, ConfigTech.getParamAuthorizedUnits().get(techParamName).get(0),
					DataType.STRING);
		}

		varHash.put(techParamName, techParamVar);
		// 2 - add it to the optionnal variables :
		optVar.add(techParamName);
		// 3 - create the group for the <tech_param> variable
		createGroup(groupMembers, techParamName);
		// 4 - add into this group the variable member of group "TECH_TIMESERIES" (JULD
		HashSet<String> techTimeSeriesGroupMembers = groupMembers.get("TECH_TIMESERIES");
		addMembersToGroup(groupMembers, techParamName, techTimeSeriesGroupMembers);
		// 5 - link varName to group name :
		varGroup.put(techParamName, techParamName);
	}

	/**
	 * add a new group to groupMembers instance variable.
	 * 
	 * @param groupMembers
	 * @param groupName
	 */
	private void createGroup(HashMap<String, HashSet<String>> groupMembers, String groupName) {
		if (!groupMembers.containsKey(groupName)) { // ..new group - init
			groupMembers.put(groupName, new HashSet<String>());
			log.debug("create group: '{}'", groupName);
		}

	}

	private void addMembersToGroup(HashMap<String, HashSet<String>> groups, String groupName,
			HashSet<String> newMembers) {
		if (groups.containsKey(groupName)) {
			for (String member : newMembers) {
				groups.get(groupName).add(member);
			}
		}
	}

	// .............................................
	// .................parseCdlFile................
	// .............................................
	/**
	 * Parses a CDL file. The file components are accessed through the various
	 * accessors.
	 *
	 * @return True - parsed the file; False - failed to parse the file
	 * @throws IOException indicates an I/O error during parsing
	 */
	public boolean parseCdlFile() throws IOException {
		boolean inDim = false, inVar = false;
		String line;
		String specName;

		log.debug(".....parseCdlFile: start.....");

		// ..check the file
		File f = new File(cdlFileName);
		if (!f.isFile()) {
			log.error("cdlFileName '" + cdlFileName + "' does not exist");
			throw new IOException("cdlFileName ('" + cdlFileName + "') does not exist");
		} else if (!f.canRead()) {
			log.error("cdlFileName '" + cdlFileName + "' cannot be read");
			throw new IOException("cdlFileName ('" + cdlFileName + "') cannot be read");
		}

		// ..open the CDL specification file
		BufferedReader file = new BufferedReader(new FileReader(cdlFileName));

		log.info("parsing spec CDL file '" + cdlFileName + "'");

		// ..parse the file
		while ((line = file.readLine()) != null) {
			log.debug("line: '{}'", line);

			// ..check to see if there is a comment (has "//" in it)
			Matcher mCmt = pComment.matcher(line);
			if (mCmt.find()) {
				log.debug("comment: '{}'", mCmt.group(1));

				// ..if the line has "//@" parse the "special comment"
				if (mCmt.group(1) != null) {
					log.debug("special comment: '{}'  '{}'", mCmt.group(1), mCmt.group(2));

					metaHash.put(mCmt.group(1), mCmt.group(2));

					// if (mCmt.group(1).equalsIgnoreCase("PARAM")) {
					// paramSet.add(mCmt.group(2));
					// } else {
					// //..save the "special information"
					// metaHash.put(mCmt.group(1), mCmt.group(2));
					// }
				}
			}

			line = mCmt.replaceAll(""); // ..remove "//" comments

			// ..parse what is left after the comment has been removed

			Matcher mName = pOpenTag.matcher(line);
			if (pBlankOrComment.matcher(line).matches()) {
				// ..blank line (after comment removed) - do nothing

			} else if (mName.matches()) { // ..this is the header line
				specName = mName.group(1); // ..get the name off the line

				if (log.isInfoEnabled()) {
					log.info("spec Name: '" + specName + "'");
				}

			} else if (pGlblAttr.matcher(line).matches()) {
				parseGlblAttrLine(line);

			} else if (pDimTag.matcher(line).matches()) {
				// .."dimensions:" line - start of the dimension definitions

				inDim = true;
				inVar = false;
				log.debug("***  DIMENSIONS  ***");

			} else if (pVarTag.matcher(line).matches()) {
				// .."variables:" line - start of the variable definitions

				inDim = false;
				inVar = true;
				log.debug("***  VARIABLES  ***");

			} else if (pDataTag.matcher(line).matches() || pCloseTag.matcher(line).matches()) {
				// ..the "data:" line or the closing "}"
				// ..- nothing else needs to be done

				inDim = false;
				inVar = false;
				log.debug("***  DATA  ***");

				break;

			} else if (inDim) {
				// ..in the "dimensions" section - parse dimesion definitions

				if (!parseDimLine(line)) {
					file.close();
					return false;
				}

			} else if (inVar) {
				// ..in the "variables" section - parse variables and attributes

				if (!parseVarLine(line)) {
					file.close();
					return false;
				}
			}

		} // ..end while (file.readLine()

		file.close();
		log.debug(".....parseCdlFile: end.....");

		return true;
	} // ..end parseCdlFile

	// ......................................................
	// .....................parseDimLine.....................
	// ......................................................
	/**
	 * Parses the dimension line from a CDL file
	 * 
	 * @param line the dimension line to parse
	 */
	private boolean parseDimLine(String line) {
		Matcher m;
		String name;
		int value;

		m = pDimPattern.matcher(line);

		if (m.matches()) {
			Pattern pattern = null;
			name = m.group(1);

			try {
				pattern = Pattern.compile(name);
			} catch (PatternSyntaxException e) {
				message = "Extra dimension is not a valid regular expression: '" + name + "'";
				return false;
			}

			extraDimPattern.add(pattern);
			log.debug("parseDimLine: extra dimension: '{}'", name);

		} else {

			m = pDimDef.matcher(line);

			if (m.matches()) {
				log.debug("parseDimLine: groups = '{}' '{}'  '{}'  '{}'", m.group(1), m.group(2), m.group(3),
						m.group(4));

				name = m.group(1);

				if (m.group(3) != null) {
					value = Integer.parseInt(m.group(3));

				} else if (m.group(2).equals("UNLIMITED")) {
					value = -1;

				} else {
					value = 0;
				}

				ArgoDimension dim = dimHash.put(name, new ArgoDimension(name, value));
				log.debug("parseDimLine: name: '{}'   value: '{}'", name, value);

			} else {
				log.debug("parseDimLine: invalid line '{}'", line);
				message = "Invalid dimension line: '" + line + "'";
				return false;
			}
		}

		message = BLANK_MESSAGE;
		return true;
	} // ..end parseDimLine

	// ...........................................................
	// .....................parseGlblAttrLine.....................
	// ...........................................................
	/**
	 * Parses a global attribute line from a CDL file.
	 * <p>
	 * NOTE: Currently only expects string valued attributes, but the plumbing is
	 * there to handle numeric types too.
	 * 
	 * @param line the dimension line to parse
	 */

	private void parseGlblAttrLine(String line) throws IOException {
		Matcher m = pGlblAttr.matcher(line);
		String name;
		String value;
		String regex;

		boolean match = m.matches();
		if (match) {
			log.debug("parseGlblAttrLine: groups = '{}' '{}' '{}' '{}'", m.group(1), m.group(2), m.group(3),
					m.group(4));

			name = m.group(1);
			value = m.group(2);
			regex = m.group(4);

			if (value == null) {
				value = ATTR_IGNORE_VALUE;
			}

			gAttrHash.put(name, new ArgoAttribute(name, value));

			if (regex != null) {
				// ..a REGEX was included on the line

				Pattern p;
				try {
					p = Pattern.compile(regex);

				} catch (Exception e) {
					log.error("Global attribute regex compile error: " + e);
					message = "Invalid Regex: '" + line + "'";
					throw new IOException("Invalid global attribute regex");
				}

				AttrRegex a = new AttrRegex();
				a.pattern = p;
				a.warn = false;
				regexHash.put(":" + name, a);
			}

			log.debug("global attribute: '{}'   value: '{}'   regex: '{}'", name, value, regex);
		}

		message = BLANK_MESSAGE;
	} // ..end parseGlblAttrLine

	// ......................................................
	// .....................parseVarLine.....................
	// ......................................................
	/**
	 * Parses a variable definition line from a CDL file
	 * 
	 * @param line the dimension line to parse
	 * @return True - line parsed; False - line failed to parse
	 */

	private boolean parseVarLine(String line) {

		Matcher mVar = pVarDef.matcher(line);
		Matcher mAttr = pVarAttr.matcher(line);

		boolean isVar = mVar.matches();
		boolean isAttr = mAttr.matches();

		ArgoVariable var = null;

		if (isVar) {
			// .......parse the variable definition.......
			String type = mVar.group(1);
			String name = mVar.group(2);
			String dim = mVar.group(3);

			if (log.isDebugEnabled()) {
				log.debug("variable: name = '" + name + "'  type = '" + type + "'   dim = '" + dim + "'");
			}

			// ..get the Class for this data type
			DataType typeType;
			if (type.equals("char")) { // ..this is funny. netcdf doesn't use
				typeType = DataType.CHAR; // .."string" but DataType uses "string"
											// ..and doesn't have "char". Workaround.

			} else if (type.equals("float_or_double")) {
				typeType = DataType.OPAQUE;

			} else {
				typeType = DataType.getType(type);
			}

			if (typeType == null) {
				message = "Invalid data type = '" + type + "'";
				return false;
			}

			// ..parse the dimensions
			ArgoDimension[] dimension;
			if (dim != null) {
				Matcher mDim = pVarDim.matcher(dim);

				int nDim = 0;
				while (mDim.find()) {
					nDim++;
				}

				dimension = new ArgoDimension[nDim];

				// ..mDim: group(1) = string with alt-dim syntax ( alt1|alt2...); or null
				// .. group(2) = string in regular-dim syntax; or null

				ArgoDimension d = null;

				nDim = 0;
				mDim.reset();
				while (mDim.find()) {
					if (mDim.group(2) != null) {
						// ..this is a regular dimension

						String s = mDim.group(2);
						d = dimHash.get(s);

						if (d == null) {
							message = "Dimension used ('" + s + "') is NOT defined in the file";
							log.debug("parseVarLine: dimension '{}' not in the file", s);
							return false;
						}

					} else if (mDim.group(1) != null) {
						// ..an alt-dimension for this variable

						String s = mDim.group(1);

						d = dimHash.get(s);

						if (d == null) {
							// ..create new alt-dim dimension
							d = new ArgoDimension(s);

							// ..parse the alt-names

							for (String alt : s.split("\\|")) {
								if (dimHash.get(alt) == null) {
									message = "Alternate dimension name '" + alt + "' is not defined in this file";
									return false;
								}

								d.addAlternateDimensionName(alt);
							}

							dimHash.put(s, d);

							if (log.isDebugEnabled()) {
								log.debug("parseVarLine: new alt-dim '{}'", s);
								for (String ss : d.alternateDimensionNames()) {
									log.debug("              ...allowed dimension = '{}'", ss);
								}
							}

						} else {
							log.debug("parseVarLine: existing alt-dim '{}'", s);
						}

					} else {
						message = "Failed to parse dimension string '" + dim + "'";
						log.debug("parseVarLine: failed to parse dim = '{}'", dim);
						return false;
					}

					dimension[nDim] = d;
					nDim++;
				}

				// ..add the Variable
				var = varHash.put(name, new ArgoVariable(name, typeType, dimension));

			} else {
				// ..add the Variable
				var = varHash.put(name, new ArgoVariable(name, typeType));
			}

		} else if (isAttr) {
			// .....add attributes to the variable.......

			String varName = mAttr.group(1);
			String name = mAttr.group(2);
			String strValue = mAttr.group(3);
			String numValue = mAttr.group(4);

			var = varHash.get(varName);

			log.debug("attribute: '" + varName + "'  '" + name + "'  '" + strValue + "'  '" + numValue + "'");

			// ..values are either String or Number
			if (strValue != null) {
				addAttr(var, name, strValue, DataType.STRING);

			} else if (numValue != null) {
				addAttr(var, name, numValue, var.getType());

			} else {
				message = "Unknown attribute value type on '" + varName + "'";
				return false;
			}
		} else {
			log.error("'" + line.trim() + "' is not identified as isVar() or isAttr()");
			message = "parseVarLine failed to parse line '" + line + "'";
			return false;
		}

		message = BLANK_MESSAGE;
		return true;
	} // ..end parseVar

	// ......................................................
	// .....................parseOptFile.....................
	// ......................................................
	/**
	 * Parses the "optional element" file of a specification. This file defines
	 * which elements in the CDL specification are optional. Element can me
	 * variables or dimensions.
	 *
	 * @return True - file parsed; False - failed to parse file
	 * @throws IOException indicates file read or permission error
	 */

	public boolean parseOptFile() throws IOException {
		String line;

		log.debug(".....parseOptFile: start.....");

		// ..check the file
		File f = new File(optFileName);
		if (!f.isFile()) {
			// ..the optional parameter file is itself optional

			optionalVars = false;
			return false;
		}

		if (!f.canRead()) {
			log.error("optFileName '" + optFileName + "' exists but cannot be read");
			throw new IOException("optFileName ('" + optFileName + "') exists but cannot be read");
		}

		// ..file exists and it can be read
		// ..open it

		BufferedReader file = new BufferedReader(new FileReader(optFileName));

		// ..a "variable group" defined here over-rides those defined elsewhere
		// ..for instance, parseParam sets "variable groups" for <PARAM> variables
		// .. these will over-ride those
		HashSet<String> newGroup = new HashSet<String>();

		// ..parse the file
		log.info("parsing \"option\" file '" + optFileName + "'");

		while ((line = file.readLine()) != null) {
			log.debug("line = '" + line + "'");

			if (pBlankOrComment.matcher(line).matches()) {
				continue;
			}

			Matcher m = pOptVar.matcher(line);

			if (!m.matches()) {
				file.close();
				message = "Invalid line in '" + optFileName + "': '" + line + "'";
				throw new IOException("Invalid line in '" + optFileName + "'");
			}

			// ..2 line format options:
			// .. 1) "element-name" --- variable/dimension not in a group
			// .. 2) "group-name : element-name" --- variable is a member of group
			// .. in this case, the group-name is also an element name
			String group = m.group(1);
			String element = m.group(2);
			log.debug("group, variable = '{}'   '{}'", group, element);

			if (!newGroup.contains(group)) {
				// ..first time this "primary name" (group or element) was seen
				// ..in the option file

				// ..over-rides any associations that pre-existed this OPT file
				// ..- removes any group mapped to this name
				// ..- removes any existing group with this name

				if (varGroup.containsKey(group)) {
					varGroup.remove(group);
					log.debug("remove existing var-to-group mapping: variable '{}'", group);
				}

				if (groupMembers.containsKey(group)) {
					groupMembers.remove(group);
					log.debug("remove existing group '{}'", group);
				}

				newGroup.add(group);
			}

			if (element == null) {
				// ..format (1) -- not part of a group ("group" is the element-name)
				optVar.add(group); // ..make element optional
				log.debug("add optional variable '{}' (no group)", group);

			} else {
				// ..format (2) -- part of a group
				if (!groupMembers.containsKey(group)) { // ..new group, initialize
					groupMembers.put(group, new HashSet<String>());
					log.debug("create group '{}'", group);
				}

				varGroup.put(element, group); // ..associate this element with the group
				groupMembers.get(group).add(element); // ..add this element to the group

				optVar.add(element); // ..make element optional

				log.debug("add optional variable '{}' to group '{}'", element, group);
			}
		} // ..end while (readLine)

		file.close();
		log.debug(".....parseOptFile: end.....");

		return true;
	} // ..end parseOptFile

	// ............................................................
	// .....................parseAttrRegexFile.....................
	// ............................................................
	/**
	 * Parses the "attribute regex" file of a specification. Defines regular
	 * expressions used to match the specified reqular expressions (global or
	 * variable).
	 * <p>
	 * In a perfect world, this file would never be needed.
	 * 
	 * @return True - file parsed; False - failed to parse file
	 * @throws IOException indicates file read or permission error
	 */

	public boolean parseAttrRegexFile() throws IOException {
		String line;

		log.debug(".....parseAttrRegexFile: start.....");

		// ..check the file
		File f = new File(regFileName);
		if (!f.isFile()) {
			// ..the optional parameter file is itself optional

			attrRegex = false;
			return false;
		}

		if (!f.canRead()) {
			log.error("regFileName '" + regFileName + "' exists but cannot be read");
			throw new IOException("regFileName ('" + regFileName + "') exists but cannot be read");
		}

		// ..file exists and it can be read
		// ..open it

		BufferedReader file = new BufferedReader(new FileReader(regFileName));

		// ..parse the file
		log.info("parsing \"attr_regexp\" file '" + regFileName + "'");

		// ..define the Hash for the information

		while ((line = file.readLine()) != null) {
			log.debug("line = '" + line + "'");

			if (pBlankOrComment.matcher(line).matches()) {
				continue;

			} else {
				Matcher m = pAttrRegex.matcher(line);

				if (!m.matches()) {
					file.close();
					message = "Invalid line in '" + regFileName + "': '" + line + "'";
					throw new IOException("Invalid line in '" + regFileName + "'");
				}

				// ..line format options:
				// .. variable:attribute = regex; [WARN|NOWARN]
				// .. where the "[...]" is optional (the bracket are NOT included on the line)
				String var_attr = m.group(1);
				String var = m.group(2);
				String attr = m.group(3);
				String regex = m.group(4);
				String warn = m.group(5);
				log.debug("variable:attribute, var, attr, regex, warn = '{}', '{}', '{}', '{}', '{}'", var_attr, var,
						attr, regex, warn);

				if (regex.length() == ATTR_SPECIAL_LENGTH && regex.matches(ATTR_SPECIAL_REGEX)) {
					// ..this is a attribute special pattern: replace existing setting
					ArgoVariable aVar = varHash.get(var);
					if (aVar == null) {
						file.close();
						throw new IOException("Invalid variable name in line '" + line + "' in '" + regFileName + "'");
					}

					addAttr(aVar, attr, regex, DataType.STRING);
					log.debug("attribute special pattern (not a regex): over-write existing");

				} else {
					// ..treat it as a regular expression
					Pattern p;
					try {
						p = Pattern.compile(regex);

					} catch (Exception e) {
						file.close();
						log.error("Attr Regex compile error: " + e);
						message = "Invalid Regex: '" + line + "'";
						throw new IOException("Invalid regex in '" + regFileName + "'");
					}

					AttrRegex a = new AttrRegex();
					a.pattern = p;

					if (warn == null || warn.equals("NOWARN")) {
						log.debug("...nowarn");
						a.warn = false;
					} else { // ..the pattern will only match "NOWARN" or "WARN"
						log.debug("...warn");
						a.warn = true;
					}

					regexHash.put(var_attr, a);
				}
			}
		} // ..end while (readLine)

		file.close();
		log.debug(".....parseAttrRegexFile: end.....");

		return true;
	} // ..end parseAttrRegexFile

	// ........................................................
	// .....................parseParamFile.....................
	// ........................................................
	/**
	 * Parses the list of physical parameters of the specification
	 *
	 * Reads two files: prmFileName - the primary list of physcial parameter names
	 * prmFileNameAux - contains "auxilliary" setting for certain parameters
	 *
	 * @param fileType The allowed physical parameters are dependent on the file
	 *                 type.
	 * @param version  Version of the specification
	 * @param listOnly true - only make a list of ALL parameter names; false - build
	 *                 the variable structure for the specification (based on file
	 *                 type)
	 * @return True - file parsed; False - failed to parse file
	 * @throws IOException indicates file read or permission error
	 */

	public boolean parseParamFile(ArgoDataFile.FileType fileType, String version, boolean listOnly) throws IOException {
		// TO DO : HANDLE REGEX in PARAM NAME and ATTRIBUTE : see ArgoConfigTechParam
		// Create a generic class ?
		log.debug(".....parseParamFile: start.....");
		log.debug("fileType, version: '{}' '{}' --- listOnly {}", fileType, version, listOnly);

		// ..check file type and build the appropriate dimension array

		ArgoDimension dimPQc[] = new ArgoDimension[1];
		ArgoDimension dimParam[] = null;

		if (!listOnly) {
			if (fileType == ArgoDataFile.FileType.PROFILE || fileType == ArgoDataFile.FileType.BIO_PROFILE) {

				dimParam = new ArgoDimension[2];
				dimParam[0] = dimHash.get("N_PROF");
				dimParam[1] = dimHash.get("N_LEVELS");

				dimPQc[0] = dimHash.get("N_PROF");

			} else if (fileType == ArgoDataFile.FileType.TRAJECTORY
					|| fileType == ArgoDataFile.FileType.BIO_TRAJECTORY) {

				dimParam = new ArgoDimension[1];
				dimParam[0] = dimHash.get("N_MEASUREMENT");

			} else {
				log.debug("parseParamFile called for an invalid file type");
				return false;
			}
		}

		// ..................get the Auxilliary (special) settings....................
		// ..initialize special settings

		String errComment = null;
		String errLongName = null;
		String presAxis = new String("Z");
		String pres_adjAxis = new String("Z");

		// ..open the file
		File f = new File(prmFileNameAux);
		if (!f.isFile()) {
			log.error("prmFileName '" + prmFileNameAux + "' does not exist");
			throw new IOException("prmFileName ('" + prmFileNameAux + "') does not exist");
		} else if (!f.canRead()) {
			log.error("prmFileName '" + prmFileNameAux + "' cannot be read");
			throw new IOException("prmFileName ('" + prmFileNameAux + "') cannot be read");
		}

		BufferedReader file = new BufferedReader(new FileReader(prmFileNameAux));

		// ..parse the file
		log.info("parsing \"auxilliary parameter\" file '" + prmFileNameAux + "'");

		// ..read through the file
		String line;
		while ((line = file.readLine()) != null) {
			log.debug("line: '" + line + "'");

			if (pBlankOrComment.matcher(line).matches()) {
				continue;
			}

			// .....parse the line into individual entries.....
			String st[] = line.split("\\|");

			// ..it's not a valid parameter definition line
			// ..check for special settings

			if (st[0].trim().equals("PRES:axis")) {
				presAxis = st[1].trim();
				log.debug("pres:axis line : '" + presAxis + "'");
				continue;

			} else if (st[0].trim().equals("PRES_ADJUSTED:axis")) {
				pres_adjAxis = st[1].trim();
				log.debug("pres_adjusted:axis line : '" + pres_adjAxis + "'");
				continue;

			} else if (st[0].trim().equals("ADJUSTED_ERROR:comment")) {
				errComment = st[1].trim();
				log.debug("adjusted_error:comment line : '" + errComment + "'");
				continue;

			} else if (st[0].trim().equals("ADJUSTED_ERROR:long_name")) {
				errLongName = st[1].trim();
				log.debug("adjusted_error:long_name line : '" + errLongName + "'");
				continue;

			} else {
				file.close();
				log.error("Invalid line in '" + prmFileName + "': '" + line + "'");
				throw new IOException("Invalid line in '" + prmFileName + "': '" + line + "'");
			}
		} // ..end while read(auxilliary param file)

		file.close();

		// ........................read the MAIN parameter file.......................

		/*
		 * ..File types served: .. Core Profile .. Bio Profile .. Trajectory .. v3.1 -
		 * Core-trajectory and Bio-trajectory .. v3.2 and beyond - (merged) Trajectory
		 */

		boolean isCoreProf = false;
		boolean isBioProf = false;
		boolean isOldCoreTraj = false;
		boolean isOldBioTraj = false;
		boolean isTraj = false;

		if (fileType == ArgoDataFile.FileType.PROFILE) {
			isCoreProf = true;

		} else if (fileType == ArgoDataFile.FileType.BIO_PROFILE) {
			isBioProf = true;

		} else if (fileType == ArgoDataFile.FileType.TRAJECTORY) {
			if (version.compareTo("3.1") <= 0) {
				isOldCoreTraj = true;
			} else {
				isTraj = true;
			}

		} else if (fileType == ArgoDataFile.FileType.BIO_TRAJECTORY) {
			isOldBioTraj = true;
		}

		boolean isPost3_0 = false;
		if (version.compareTo("3.0") > 0) {
			isPost3_0 = true;
		}

		// ..open the file
		f = new File(prmFileName);
		if (!f.isFile()) {
			log.error("prmFileName '" + prmFileName + "' does not exist");
			throw new IOException("prmFileName ('" + prmFileName + "') does not exist");
		} else if (!f.canRead()) {
			log.error("prmFileName '" + prmFileName + "' cannot be read");
			throw new IOException("prmFileName ('" + cdlFileName + "') cannot be read");
		}

		file = new BufferedReader(new FileReader(prmFileName));

		// ..parse the file
		log.info("parsing \"parameter\" file '{}'", prmFileName);

		// ..read through the file
		// String line;
		int nLine = 0;
		while ((line = file.readLine()) != null) {
			nLine++;
			log.debug("line({}): '{}'", line);

			if (pBlankOrComment.matcher(line).matches()) {
				continue;
			}

			// .....parse the line into individual entries.....
			String st[] = line.split("\\|");

			if (st.length < nParamFields) {
				file.close();
				log.error("too few columns on line {} in '{}'", nLine, prmFileName);
				throw new IOException("Too few columns on line " + nLine + " in '" + prmFileName + "': '" + line + "'");
			}

			String prm = st[1].trim(); // ..parameter name
			String prmLName = st[3].trim(); // ..long_name
			String prmSName = st[4].trim(); // ..(cf_)standard_name
			String prmUnits = st[6].trim(); // ..units
			String prmVmin = st[7].trim(); // ..value min
			String prmVmax = st[8].trim(); // ..value max
			String prmCategory = st[9].trim(); // .."c", "b", "ic", "ib"
			String prmComment = st[14].trim(); // ..to be searched for extra dimension
			String prmStatus = st[16].trim(); // .."active", "deprecated", etc
			String prmFill = st[17].trim(); // ..fillValue
			String prmType = st[18].trim(); // ..data type

			// ..status codes of " " (blank), "active", "deprecated" are allowed

			boolean isDeprecated = false;
			if (prmStatus.length() > 0) {
				if (!pActive.matcher(prmStatus).matches()) {
					if (!pDeprecated.matcher(prmStatus).matches()) {
						// ..not allowed
						log.debug("prm is not valid: '{}'", prm);
						continue;
					} else {// ..it is deprecated
						log.debug("deprecated prm: '{}'", prm);
						isDeprecated = true;
					}
				}
			}

			// ..set any special handling instructions for v3.1 and later files

			if (isPost3_0) {
				if (prmSName.length() == 0 || prmSName.equals("-")) {
					prmSName = ATTR_NOT_ALLOWED;
				}
				if (prmVmin.length() == 0 || prmVmin.equals("-")) {
					prmVmin = ATTR_NOT_ALLOWED;
				}
				if (prmVmax.length() == 0 || prmVmax.equals("-")) {
					prmVmax = ATTR_NOT_ALLOWED;
				}
				if (prmLName.length() == 0 || prmLName.equals("-")) {
					prmVmax = ATTR_IGNORE;
				}
			}

			log.debug("parsed: {}|{}|{}|{}|{}|{}|{}|", prm, prmType, prmLName, prmSName, prmUnits, prmVmin, prmVmax,
					prmStatus, prmFill);

			// ..array of automatic "number" variables

			String prmList[] = new String[4];
			prmList[0] = prm;
			// ADMT-25 : Always add an underscore to numbering duplicate sensor/parameters
			// variable
			prmList[1] = prm + "_2";
			prmList[2] = prm + "_3";
			prmList[3] = prm + "_4";

			// ...LIST-ONLY MODE: grab the param name and continue with the next line
			if (listOnly) {
				for (String prmName : prmList) {
					if (!physParamNameList.contains(prmName)) {// ..new param name
						physParamNameList.add(prmName);
						if (isDeprecated) {
							depParamNameList.add(prmName);
						}

						physParamNameList.add(prmName + "_STD");
						physParamNameList.add(prmName + "_MED");

					} else { // ..duplicate name is a no, no
						log.error("Duplicate param name '" + prmFileName + "': '" + line + "'");
						file.close();
						throw new IOException("Duplicate param name in '" + prmFileName + "': '" + line + "'");
					}
				}
				continue;
			}

			// ..check if there is an "extra dimension"
			// ..at least for now, this is encoded in the data type
			boolean prmExtra = false;
			if (pN_VALUES.matcher(prmComment).matches()) {
				prmExtra = true;
				log.debug("variable with extra dimension: type = '{}'", prmType);
			}

			// ..check the data type
			String tmp = prmType;
			switch (prmType) {
			case "NC_DOUBLE":
				prmType = "double";
				break;
			case "NC_FLOAT":
				prmType = "float";
				break;
			case "NC_SHORT":
				prmType = "short";
				break;
			}
			if (log.isDebugEnabled() && !prmType.equals(tmp)) {
				log.debug("reset prmType from {} to {}", tmp, prmType);
			}

			if (!(prmType.equals("float") || prmType.equals("double") || prmType.equals("short"))) {
				log.error("Invalid data type = '" + line + "'");
				throw new IOException("Invalid data type '" + prmType + "' (" + prmFileName + "; " + nLine + ")");
			}

			DataType ncDataType = DataType.getType(prmType);

			// ..check for symbolic NETCDF fill values

			if (prmFill.startsWith("NC_FILL_")) {
				tmp = prmFill;
				prmFill = NC_FILL_TYPES.get(tmp);

				if (prmFill == null) {
					log.debug("Invalid NC_FILL_: '" + tmp + "' in line '" + line + "'");
					throw new IOException("Invalid NC_FILL_: '" + tmp + "' (" + prmFileName + "; " + nLine + ")");
				}

				log.debug("Changed fill-value from {} to {}", tmp, prmFill);
			}

			// ..check the PrmCat -- "c" = core, "b" = bio, "ic/ib" = intermediate -> core,
			// bio
			if (!(prmCategory.equals("c") || prmCategory.equals("b") || prmCategory.equals("ic")
					|| prmCategory.equals("ib"))) {
				log.debug("Invalid category '" + prmCategory + "' in line '" + line + "'");
				throw new IOException("Invalid category '" + prmCategory + "' (" + prmFileName + "; " + nLine + ")");
			}

			// ..decide if this parameter is in this type of file

			boolean keep = false;
			boolean opt = true;
			boolean CORE = false;
			boolean BIO = false;

			// ..check for a valid file type and decide if it's a keeper
			if (prmCategory.equals("c")) {
				CORE = true;
				BIO = false;

				if (prm.startsWith("PRES") && pPRESn.matcher(prm).matches()) {
					// ..keep PRES / PRESn in all cases
					opt = false;
					keep = true;

					// ..treat PRES/PRESn like any other bio-parameter in Bio- files
					if (isBioProf || isOldBioTraj) {
						CORE = false;
					}

				} else if (isCoreProf || isTraj || isOldCoreTraj) {
					// ..keep "c" parameters for core and traj (> v3.1) files
					keep = true;
					if (prm.equals("TEMP") && fileType == ArgoDataFile.FileType.PROFILE) {
						opt = false;
					}
				}

			} else if (prmCategory.equals("ic")) {
				CORE = false;
				BIO = false;

				if (isCoreProf || isTraj || isOldCoreTraj) {
					// ..keep "c" and "ic" parameters for core and traj (> v3.1) files
					keep = true;
				}

			} else if (prmCategory.equals("b")) {
				CORE = false;
				BIO = true;

				if (isBioProf || isTraj || isOldBioTraj) {
					// ..keep "b" parameters for Bio-Argo and traj (> v3.1) files
					keep = true;
				}

			} else if (prmCategory.equals("ib")) {
				CORE = false;
				BIO = false;

				if (isBioProf || isTraj || isOldBioTraj) {
					// ..keep "ib" parameters for Bio-Argo and traj (> v3.1) files
					keep = true;
				}

			} else {
				log.debug("Invalid param category (c,b,ic,ib) '" + prmCategory + "' in line '" + line + "'");
				throw new IOException("Invalid parameter category (c,b,i) '" + prmCategory + "' (" + prmFileName + "; "
						+ nLine + ")");
			}

			// ..for each <param> entry, automatically make <param>* variables too
			for (String prmName : prmList) {

				if (keep) {
					// ..build the relevant variable entries
					log.debug("keeping param: '{}'  CORE/BIO {}/{}", prmName, CORE, BIO);

					// ..add to list of valid parameter names
					if (!physParamNameList.contains(prmName)) {// ..new param name
						physParamNameList.add(prmName);
						if (isDeprecated) {
							depParamNameList.add(prmName);
						}

					} else { // ..duplicate name is a no, no
						log.error("Duplicate param name (line {}) '{}'", nLine, prmName);
						throw new IOException(
								"Duplicate param name in '" + prmFileName + "' (line " + nLine + "): '" + line + "'");
					}

					// ..create the variable group(s)
					// -- see VARIABLE GROUPS documentation at the top

					String group_qc = prmName + "_QC";
					String group_adj = prmName + "_ADJUSTED";

					if (CORE || BIO) { // ..a "full parameter"
						if (!groupMembers.containsKey(prmName)) { // ..new group - init
							groupMembers.put(prmName, new HashSet<String>());
							log.debug("create group: '{}'", prmName);
						}

					} else { // ..implies an i-parameter
						interPhysParam.add(prmName);
						log.debug("add intermediate variable '{}'", prmName);

						// ..make the _qc group
						if (!groupMembers.containsKey(group_qc)) { // ..new group - init
							groupMembers.put(group_qc, new HashSet<String>());
							log.debug("create group: '{}'", group_qc);
						}

						// ..make the _adj group
						if (!groupMembers.containsKey(group_adj)) { // ..new group - init
							groupMembers.put(group_adj, new HashSet<String>());
							log.debug("create group: '{}'", group_adj);
						}
					}

					// ..PRES and Bio-argo files: In bio-argo files,
					// .. - only PRES appears
					// .. - there is no PROFILE_PRES_QC, PRES_QC, PRES_ADJ_QC, *_ADJ_ERR

					boolean bio_pres = false;
					if (prm.startsWith("PRES") && pPRESn.matcher(prm).matches()
							&& (fileType == ArgoDataFile.FileType.BIO_PROFILE
									|| fileType == ArgoDataFile.FileType.BIO_TRAJECTORY)) {
						bio_pres = true;
					}

					String varNm;
					ArgoVariable aVar;

					if (fileType == ArgoDataFile.FileType.PROFILE || fileType == ArgoDataFile.FileType.BIO_PROFILE) {

						/*
						 * Build the parameter structure char PROFILE_<PARAM>_QC(N_PROF);
						 * PROFILE_<PARAM>_QC:long_name = "Global quality flag of PRES profile";
						 * PROFILE_<PARAM>_QC:conventions = "Argo reference table 2a";
						 * PROFILE_<PARAM>_QC:_FillValue = " ";
						 *
						 * NOTE: PRES does not have this variable in BIO_PROFILE files
						 */
						if (!bio_pres) {
							varNm = new String("PROFILE_" + prmName + "_QC");
							aVar = new ArgoVariable(varNm, DataType.CHAR, dimPQc, prmName);
							addAttr(aVar, long_name, prfQcLName + prmName + " profile", DataType.STRING);
							addAttr(aVar, conventions, prfQcConventions, DataType.STRING);
							addAttr(aVar, fillValue, fillValueBLANK, DataType.STRING);

							varHash.put(varNm, aVar);
							physParamVarList.add(varNm);

							// ..add to groups ... see VARIABLE GROUPS above

							String group;
							if (CORE || BIO) {
								group = prmName;

							} else { // ..this is an intermediate parameter
								group = group_qc;
							}

							varGroup.put(varNm, group);
							groupMembers.get(group).add(varNm);

							if (opt) {
								// ..add to optional variables
								optVar.add(varNm);
							}

							log.debug("added: '{}'; opt '{}'; primary '{}'; member '{}'", varNm, opt, group, group);

						} else {
							log.debug("skip: 'PROFILE_{}_QC' (bio_pres = true)", prmName);
						}
					} // ..end if (PROFILE)

					// ...build the <PARAM> structures....
					// .. the profile structures are for both <PARAM> and <PARAM>_ADJUSTED

					String P[] = { prmName, prmName + "_ADJUSTED" };

					for (String v : P) {
						/*
						 * Build the parameter structure Profile: <ncDataType> <P>(N_PROF, N_LEVELS)
						 * -or- <ncDataType> <P>(N_PROF, N_LEVELS,_extra_)
						 *
						 * Trajectory: <ncDataType> <P>(N_MEASUREMENT); -or- <ncDataType>
						 * <P>(N_MEASUREMENT, _extra_)
						 *
						 * Attributes: <P>:long_name = "<param-specific>"; <P>:standard_name =
						 * "<param-specific>"; <P>:_FillValue = <param-specific>; <P>:units =
						 * "<param-specific>"; <P>:valid_min = <param-specific>; <P>:valid_max =
						 * <param-specific>; <P>:comment = "<param-specific>"; (version-specific)
						 * <P>:C_format = ATTR_IGNORE_VALUE; //..means ignore setting <P>:FORTRAN_format
						 * = ATTR_IGNORE_VALUE; //..means ignore setting <P>:resolution =
						 * ATTR_IGNORE_VALUE; <P>:comment_on_resolution = ATTR_IGNORE; ///..means ignore
						 * entirely only in v3.1 and beyond (but technically would be allowed in earlier
						 * versions if it showed up
						 *
						 * where <P> = <PARAM>, <PARAM>_ADJUSTED
						 *
						 * if <P> = PRES or PRES_ADJUSTED, add <P>:axis = "Z"
						 *
						 */
						log.debug("...working on: {}", v);

						aVar = new ArgoVariable(v, ncDataType, dimParam, prmName);

						if (prmExtra) {
							aVar.setHaveExtraDimension();
						}

						addAttr(aVar, long_name, prmLName, DataType.STRING);

						if (version.compareTo("2.3") < 0) {
							addAttr(aVar, comment, prmSName, DataType.STRING);
						} else {
							addAttr(aVar, standard_name, prmSName, DataType.STRING);
						}

						// aVar.addAttribute(fillValue, fillValueNum);
						addAttr(aVar, fillValue, prmFill, ncDataType);
						addAttr(aVar, units, prmUnits, DataType.STRING);
						addAttr(aVar, valid_min, prmVmin, ncDataType);
						addAttr(aVar, valid_max, prmVmax, ncDataType);
						if (v.startsWith("PRES")) {
							addAttr(aVar, c_format, ATTR_IGNORE_VALUE + "%15.1f", DataType.STRING);
							addAttr(aVar, fortran_format, ATTR_IGNORE_VALUE + "F15.1", DataType.STRING);
						} else {
							addAttr(aVar, c_format, ATTR_IGNORE_VALUE + "%15.3f", DataType.STRING);
							addAttr(aVar, fortran_format, ATTR_IGNORE_VALUE + "F15.3", DataType.STRING);
						}
						addAttr(aVar, resolution, ATTR_IGNORE + "99999.", ncDataType);
						addAttr(aVar, comment_on_resolution, ATTR_IGNORE + "resolution is unknown", DataType.STRING);

						if (v.equals("PRES")) {
							addAttr(aVar, axis, presAxis, DataType.STRING);
						} else if (v.equals("PRES_ADJUSTED")) {
							addAttr(aVar, axis, pres_adjAxis, DataType.STRING);
						}

						varHash.put(v, aVar);
						physParamVarList.add(v);

						// ..add to group(s) -- see VARIABLE GROUPS above
						if (CORE || BIO) {
							varGroup.put(v, prmName);
							groupMembers.get(prmName).add(v);
							log.debug("added: '{}'; opt '{}'; primary '{}'; member '{}'", v, opt, prmName, prmName);

						} else { // ..an i-parameter
							// ..<PARAM> has no primary group and belongs to _qc and _adj groups
							// ..<PARAM>_ADJ has primary and belongs to only _adj group
							// ..(ADMT-16: <PARAM> can no longer exist by itself)

							if (v.endsWith("_ADJUSTED")) {
								varGroup.put(v, group_adj);
								groupMembers.get(group_adj).add(v);
								log.debug("added: '{}'; opt '{}'; primary '{}'; member '{}'", v, opt, group_adj,
										group_adj);

							} else {
								varGroup.put(v, group_qc);
								groupMembers.get(group_adj).add(v);
								groupMembers.get(group_qc).add(v);
								log.debug("added: '{}'; opt '{}'; primary '{}'; member '{}'/'{}'", v, opt, "", group_qc,
										group_adj);
							}
						}

						if (opt) {
							// ..add to optional variables
							optVar.add(v);
						}

						/*
						 * Build the parameter structure Profile char <P>_QC(N_PROF, N_LEVELS);
						 * Trajectory char <P>_QC(N_MEASUREMENT);
						 *
						 * <P>_QC:long_name = "quality flag"; <P>_QC:conventions =
						 * "Argo reference table 2"; <P>_QC:_FillValue = " ";
						 *
						 * where <P> = <PARAM>, <PARAM>_ADJUSTED
						 *
						 * Don't put PRES_QC in bio-Argo file. (==> bio_pres = true)
						 */

						if (!bio_pres) {
							varNm = new String(v + "_QC");
							aVar = new ArgoVariable(varNm, DataType.CHAR, dimParam, prmName);
							addAttr(aVar, long_name, prmQcLName, DataType.STRING);
							addAttr(aVar, conventions, prmQcConventions, DataType.STRING);
							addAttr(aVar, fillValue, fillValueBLANK, DataType.STRING);

							varHash.put(varNm, aVar);
							physParamVarList.add(varNm);

							// ..add to group -- see VARIABLE GROUPS above

							if (CORE || BIO) {
								varGroup.put(varNm, prmName);
								groupMembers.get(prmName).add(varNm);
								log.debug("added: '{}'; opt '{}'; primary '{}'; member '{}'", varNm, opt, prmName,
										prmName);

							} else {
								String group;
								if (v.endsWith("_ADJUSTED")) {
									group = group_adj;
								} else {
									group = group_qc;
								}

								varGroup.put(varNm, group);
								groupMembers.get(group).add(varNm);
								log.debug("added: '{}'; opt '{}'; primary '{}'; member '{}')", varNm, opt, group,
										group);
							}

							if (opt) {
								// ..add to optional variables
								optVar.add(varNm);
							}

						} else {
							log.debug("skip: '{}_QC'  (bio_pres == true)", v);
						}

						/*
						 * Do not put PRES_ADJUSTED in bio-argo file -- bio_pres = true ==> <PARAM> =
						 * PRES && this is a bio-profile file
						 */
						if (bio_pres) {
							log.debug("skip PRES_ADJUSTED: (bio_pres == true)");
							break;
						}
					}

					/*
					 * Build the parameter structure Profile <ncDataType>
					 * <PARAM>_ADJUSTED_ERROR(N_PROF, N_LEVELS); Trajectory <ncDataType>
					 * <PARAM>_ADJUSTED_ERROR(N_MEASUREMENT);
					 *
					 * <PARAM>_ADJUSTED_ERROR:long_name = "<param-specific>";
					 * <PARAM>_ADJUSTED_ERROR:_FillValue = <param-specific;
					 * <PARAM>_ADJUSTED_ERROR:units = "<param-specific";
					 * <PARAM>_ADJUSTED_ERROR:comment = "Contains the error on the \ adjusted values
					 * as determined by the delayed mode QC process.";
					 * <PARAM>_ADJUSTED_ERROR:C_format = "%7.1f";
					 * <PARAM>_ADJUSTED_ERROR:FORTRAN_format = "F7.1";
					 * <PARAM>_ADJUSTED_ERROR:resolution = 0.1f;
					 */

					if (!bio_pres) {
						varNm = new String(prmName + "_ADJUSTED_ERROR");
						aVar = new ArgoVariable(varNm, ncDataType, dimParam, prmName);

						addAttr(aVar, long_name, (errLongName == null ? prmLName : errLongName), DataType.STRING);

						addAttr(aVar, fillValue, prmFill, ncDataType);
						addAttr(aVar, units, prmUnits, DataType.STRING);
						addAttr(aVar, comment, errComment, DataType.STRING);
						addAttr(aVar, c_format, ATTR_IGNORE_VALUE + "%15.4f", DataType.STRING);
						addAttr(aVar, fortran_format, ATTR_IGNORE_VALUE + "F15.4", DataType.STRING);
						addAttr(aVar, resolution, ATTR_IGNORE + prmFill, ncDataType);

						varHash.put(varNm, aVar);
						physParamVarList.add(varNm);

						// ..add to group -- see VARIABLE GROUPS above

						String group;
						if (CORE || BIO) {
							group = prmName;

						} else { // ..an i-parameter
							group = group_adj;
						}

						varGroup.put(varNm, group);
						groupMembers.get(group).add(varNm);
						log.debug("added: '{}'; opt '{}'; primary group'{}'; member '{}'", varNm, opt, group, group);

						if (opt) {
							// ..add to optional variables
							optVar.add(varNm);
						}
					}

					/*
					 * **************************************************************** STAT
					 * variables: Every parameter can have a <PARAM>_STD and/or <PARAM>_MED group
					 * including the *_ADJUSTED variables --- all optional
					 *
					 * The STAT variables are only allowed in the file of the primary parameter core
					 * param STAT only in core files bio param STAT only in bio files
					 *
					 */

					if (!bio_pres) {
						String STAT[] = { prmName + "_STD", prmName + "_MED" };
						for (String statNm : STAT) {
							// ..add to list of valid parameter names
							physParamNameList.add(statNm);
							interPhysParam.add(statNm); // they are "intermediate parameters"

							// ..create the variable group
							// ..each of these are there own group

							String stat_qc = statNm + "_QC";
							String stat_adj = statNm + "_ADJUSTED";

							if (!groupMembers.containsKey(statNm)) { // ..new group - init
								groupMembers.put(stat_qc, new HashSet<String>());
								log.debug("create group: '{}'", statNm);
								groupMembers.put(stat_adj, new HashSet<String>());
								log.debug("create group: '{}_ADJUSTED'", statNm);
							}

							/*
							 * ***** Apr 2015 Users of the STAT variables are not including PROFILE_*_QC So
							 * don't allow them at all until there is some outcry
							 *
							 * ***** Update: Apr 2022 Handle these as "intermediate parameters"
							 */

							if (fileType == ArgoDataFile.FileType.PROFILE
									|| fileType == ArgoDataFile.FileType.BIO_PROFILE) {

								/*
								 * Build the parameter structure char PROFILE_<STAT>_QC(N_PROF);
								 * PROFILE_<STAT>_QC:long_name = "Global quality flag of <STAT> profile";
								 * PROFILE_<STAT>_QC:conventions = "Argo reference table 2a";
								 * PROFILE_<STAT>_QC:_FillValue = " ";
								 *
								 * where <STAT> = <PARAM>_STD, <PARAM>_MED
								 */

								varNm = new String("PROFILE_" + statNm + "_QC");
								aVar = new ArgoVariable(varNm, DataType.CHAR, dimPQc, statNm);
								aVar.addAttribute(long_name, prfQcLName + statNm + " profile");
								aVar.addAttribute(conventions, prfQcConventions);
								aVar.addAttribute(fillValue, fillValueBLANK);

								varHash.put(varNm, aVar);
								physParamVarList.add(varNm);

								// ..add to options
								varGroup.put(varNm, stat_qc);
								groupMembers.get(stat_qc).add(varNm);
								optVar.add(varNm);
								log.debug("added: '{}'; opt '{}'; primary '{}'; member '{}'", varNm, true, stat_qc,
										stat_qc);
							} // ..end if (PROFILE)

							// ...build the <STAT> structures....
							// .. the profile structures are for both <STAT> and <STAT>_ADJUSTED

							String S[] = new String[] { statNm, statNm + "_ADJUSTED" };
							String group = null;

							for (String v : S) {

								/*
								 * Build the parameter STAT structure Profile: <ncDataType> <S>(N_PROF,
								 * N_LEVELS); -or- <ncDataType> <S>(N_PROF, N_LEVELS,_extra_) Trajectory:
								 * <ncDataType> <S>(N_MEASUREMENT); -or- <ncDataType> <S>(N_MEASUREMENT,
								 * _extra_) <S>:long_name = "<param-specific>"; //<S>:standard_name =
								 * "<param-specific>"; <S>:_FillValue = <param-specific>; <S>:units =
								 * "<param-specific>"; <S>:valid_min = <param-specific>; <S>:valid_max =
								 * <param-specific>; <S>:comment = "<param-specific>"; (version-specific)
								 * <S>:C_format = ATTR_IGNORE_VALUE; //..means ignore setting <S>:FORTRAN_format
								 * = ATTR_IGNORE_VALUE; //..means ignore setting <S>:resolution =
								 * ATTR_IGNORE_VALUE; <S>:comment_on_resolution = ATTR_IGNORE; ///..means ignore
								 * entirely only in v3.1 and beyond (but technically would be allowed in earlier
								 * versions if it showed up
								 *
								 * 
								 * where <S> = <STAT>, <STAT>_ADJUSTED
								 */

								aVar = new ArgoVariable(v, ncDataType, dimParam, statNm);

								if (prmExtra) {
									aVar.setHaveExtraDimension();
								}

								StringBuilder tmpLName;
								if (statNm.endsWith("_STD")) {
									tmpLName = new StringBuilder("Standard deviation of ");
								} else {
									tmpLName = new StringBuilder("Median value of ");
								}
								tmpLName.append(prmLName.substring(0, 1).toLowerCase()).append(prmLName.substring(1));

								addAttr(aVar, long_name, tmpLName.toString(), DataType.STRING);

								// aVar.addAttribute(fillValue, fillValueNum);
								addAttr(aVar, fillValue, prmFill, ncDataType);
								addAttr(aVar, units, prmUnits, DataType.STRING);
								if (statNm.endsWith("_MED")) {
									addAttr(aVar, valid_min, prmVmin, ncDataType);
									addAttr(aVar, valid_max, prmVmax, ncDataType);
								}
								addAttr(aVar, c_format, ATTR_IGNORE_VALUE, DataType.STRING);
								addAttr(aVar, fortran_format, ATTR_IGNORE_VALUE, DataType.STRING);
								addAttr(aVar, resolution, ATTR_IGNORE, ncDataType);
								addAttr(aVar, comment_on_resolution, ATTR_IGNORE + "resolution is unknown",
										DataType.STRING);

								varHash.put(v, aVar);
								physParamVarList.add(v);

								// ..add to the appropriate groups

								if (v.endsWith("_ADJUSTED")) {
									group = stat_adj;
									varGroup.put(v, stat_adj);
									groupMembers.get(stat_adj).add(v); // ..add to the appropriate group
									log.debug("added: '{}'; opt '{}'; primary '{}'; member '{}'", v, true, stat_adj,
											stat_adj);

								} else {
									group = stat_qc;
									varGroup.put(v, stat_qc);
									groupMembers.get(stat_adj).add(v);
									groupMembers.get(stat_qc).add(v);
									log.debug("added: '{}'; opt '{}'; primary '{}'; member '{}'", v, true, "",
											stat_adj);
								}

								optVar.add(v);

								/*
								 * Build the parameter structure char <S>_QC(N_PROF, N_LEVELS); <S>_QC:long_name
								 * = "quality flag"; <S>_QC:conventions = "Argo reference table 2";
								 * <S>_QC:_FillValue = " ";
								 *
								 * where <S> = <STAT>, <STAT>_ADJUSTED
								 */

								varNm = new String(v + "_QC");
								aVar = new ArgoVariable(varNm, DataType.CHAR, dimParam, statNm);
								aVar.addAttribute(long_name, prmQcLName);
								aVar.addAttribute(conventions, prmQcConventions);
								aVar.addAttribute(fillValue, fillValueBLANK);

								// group = v+"_QC";
								varHash.put(varNm, aVar);
								groupMembers.get(group).add(varNm); // ..add to the prmName group of variables
								physParamVarList.add(varNm);

								log.debug("variable added: '{}'  (group '{}')", varNm, group);

								// ..add to options
								varGroup.put(varNm, group);
								optVar.add(varNm);
								log.debug("option added: '{}' (group '{})", varNm, group);
							} // ..end for v : <STAT>, <STAT>_ADJUSTED

							/*
							 * Build the parameter structure <ncDataType> <STAT>_ADJUSTED_ERROR(N_PROF,
							 * N_LEVELS); <STAT>_ADJUSTED_ERROR:long_name = "<param-specific>";
							 * <STAT>_ADJUSTED_ERROR:_FillValue = <param-specific>;
							 * <STAT>_ADJUSTED_ERROR:units = "<param-specific>";
							 * <STAT>_ADJUSTED_ERROR:comment = "Contains the error on the \ adjusted values
							 * as determined by the delayed mode QC process.";
							 * <STAT>_ADJUSTED_ERROR:C_format = ATTR_IGNORE_VALUE;
							 * <STAT>_ADJUSTED_ERROR:FORTRAN_format = ATTR_IGNORE_VALUE;
							 * <STAT>_ADJUSTED_ERROR:resolution = ATTR_IGNORE_VALUE;
							 */

							varNm = new String(statNm + "_ADJUSTED_ERROR");
							aVar = new ArgoVariable(varNm, ncDataType, dimParam, statNm);

							addAttr(aVar, long_name, (errLongName == null ? prmLName : errLongName), DataType.STRING);

							addAttr(aVar, fillValue, prmFill, ncDataType);
							addAttr(aVar, units, prmUnits, DataType.STRING);
							addAttr(aVar, comment, errComment, DataType.STRING);
							addAttr(aVar, c_format, ATTR_IGNORE_VALUE, DataType.STRING);
							addAttr(aVar, fortran_format, ATTR_IGNORE_VALUE, DataType.STRING);
							addAttr(aVar, resolution, ATTR_IGNORE_VALUE, DataType.STRING);

							varHash.put(varNm, aVar);
							optVar.add(varNm);
							physParamVarList.add(varNm);

							varGroup.put(varNm, stat_adj);
							groupMembers.get(stat_adj).add(varNm); // ..add to the ADJUSTED group

							log.debug("added: '{}': opt '{}' ; primary '{}'; 'member '{}'", varNm, true, stat_adj,
									stat_adj);
						} // ..end for <PARAM>_STD, <PARAM>_MED

					}

				} else {
					log.debug("not keeping param: '{}'  CORE/BIO {}/{}", prmName, CORE, BIO);
				} // ..end if (keep)

				// ..make adjustments for numbered variables
				opt = true; // ..every #d variable is optional

			} // ..end for (prmList)
		} // ..end while (readLine)

		file.close();

		log.debug(".....parseParamFile: end.....");
		return true;
	} // ..end parseParamFile

	/**
	 * Convenience function to add an attribute to a variable
	 * 
	 * @param var      Variable to add attribute to
	 * @param attrName Attribute name
	 * @param attVal   Attribute value -- can be the attribute "special codes" Input
	 *                 as a string then converted to the appropriate type
	 * @param type     String representing the data type
	 */
	private void addAttr(ArgoVariable var, String attrName, String attrVal, DataType type) {
		if (attrVal == null) {
			return;
		}

		if (attrVal.matches(ATTR_SPECIAL_REGEX)) {
			// ..determine the special attribute type
			ArgoAttribute.AttrHandling sa = ArgoAttribute.AttrHandling.FULLY_SPECIFIED;

			if (attrVal.startsWith(ATTR_IGNORE)) {
				sa = ArgoAttribute.AttrHandling.IGNORE_COMPLETELY;
			} else if (attrVal.startsWith(ATTR_IGNORE_VALUE)) {
				sa = ArgoAttribute.AttrHandling.IGNORE_VALUE;
			} else if (attrVal.startsWith(ATTR_NOT_ALLOWED)) {
				sa = ArgoAttribute.AttrHandling.NOT_ALLOWED;
			}

			// ..see if there is a data type included
			String tmp = attrVal.substring(ATTR_SPECIAL_LENGTH);
			DataType outType = type;

			if (tmp.startsWith("DOUBLE:")) {
				outType = DataType.DOUBLE;
				tmp = tmp.substring("DOUBLE:".length());

			} else if (tmp.startsWith("FLOAT:")) {
				outType = DataType.FLOAT;
				tmp = tmp.substring("FLOAT:".length());

			} else if (tmp.startsWith("INTEGER:")) {
				outType = DataType.INT;
				tmp = tmp.substring("INTEGER:".length());

			} else if (tmp.startsWith("STRING:")) {
				outType = DataType.STRING;
				tmp = tmp.substring("STRING:".length());
			}

			// ..see if there is default included

			if (tmp.length() == 0) {
				// ..no default

				var.addSpecialAttribute(attrName, sa, outType);

				log.debug("addAttr: (special) attrName, sa, type, def = '{}', '{}', '{}', (no default)", attrName, sa,
						type);

			} else {
				// ..a default value was provided

				var.addSpecialAttribute(attrName, sa, outType, tmp);

				log.debug("addAttr: (special) attrName, sa, type, def = '{}', '{}', '{}', '{}'", attrName, sa, type,
						tmp);
			}

		} else if (type == DataType.STRING) {
			var.addAttribute(attrName, attrVal);

			log.debug("addAttr: (string) attrName, attrVal = '{}', '{}'", attrName, attrVal);

		} else {

			Number num;

			if (type == DataType.DOUBLE) {
				num = new Double(attrVal);
			} else if (type == DataType.FLOAT) {
				num = new Float(attrVal);
			} else if (type == DataType.INT) {
				num = new Integer(attrVal);
			} else if (type == DataType.OPAQUE) {
				num = new Double(attrVal);
			} else if (type == DataType.SHORT) {
				num = new Short(attrVal);
			} else {
				num = null;
			}

			var.addAttribute(attrName, num);

			log.debug("addAttr: (number) attrName, attrVal, num = '{}', '{}', '{}'", attrName, attrVal, num);
		}

		return;
	}

} // ..end class
