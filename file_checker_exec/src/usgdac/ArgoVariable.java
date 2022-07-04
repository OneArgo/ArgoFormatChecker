package usgdac;

import java.util.*;

import java.util.regex.*;

import ucar.nc2.Variable;
import ucar.ma2.DataType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @version  $HeadURL: https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoVariable.java $
 * @version  $Id: ArgoVariable.java 983 2018-08-13 14:14:45Z ignaszewski $
 */

public class ArgoVariable {

   //.................................................
   //               VARIABLES
   //.................................................

   //..class variables..
   //private static Pattern pParamQC = Pattern.compile("PROFILE_(.+)_QC");
   //private static Pattern pParam   = Pattern.compile
   //   ("(\\w+?)(_QC|_ADJUSTED|_ADJUSTED_QC|_ADJUSTED_ERROR)?");

   private static final Logger log = LogManager.getLogger("ArgoVariable");

   //..object variables..
   private LinkedHashMap<String, ArgoAttribute> attr;

   private int             calibDim = -1;
   private int             paramDim = -1;
   private int             profileDim = -1;
   private int             levelsDim = -1;

   private ArgoDimension   dim[];
   private String          dimsString;
   private boolean         fillable;
   private String          name;
   private Variable        ncVar;
   private String          paramName;
   private boolean         string;
   private DataType        type;

   //..special features

   private boolean         extraDims;
   private boolean         altDims;

   //.................................................
   //               CONSTRUCTORS
   //.................................................

   /**
    * Constructs an ArgoVariable of the given data type with no dimensions
    * @param varName  The name of the variable
    * @param dataType The netCDF DataType of the variable
    */
   public ArgoVariable (String varName, DataType dataType)
   {
      this(varName, dataType, new ArgoDimension[] {}, (String) null);
   }

   /**
    * Constructs an ArgoVariable of the given data type with a single dimension
    * @param varName  The name of the variable
    * @param dataType The netCDF DataType of the variable
    * @param varDim   The dimension
    */
   public ArgoVariable (String varName, DataType dataType, ArgoDimension varDim)
   {
      this(varName, dataType, new ArgoDimension[] {varDim}, (String) null);
   }

   /**
    * Constructs an ArgoVariable of the given data type with dimensions specified
    * by the array of dimensions
    * @param varName  The name of the variable
    * @param dataType The netCDF DataType of the variable
    * @param varDim   Array of the dimensions
    */
   public ArgoVariable (String varName, DataType dataType, ArgoDimension varDim[])
   {
      this(varName, dataType, varDim, (String) null);
   }
    
   /**
    * Constructs an ArgoVariable of the given data type with dimensions specified
    * by the array of dimensions, and "tagged" with the associated physical parameter
    * name.  For example, the parameter name associated with TEMP_ADJUSTED is TEMP.
    * @param varName  The name of the variable
    * @param dataType The netCDF DataType of the variable
    * @param varDim   Array of required dimensions
    * @param canHaveExtraDims  true = variable may have "extra dimensions"
    */
   public ArgoVariable (String varName, DataType dataType, ArgoDimension varDim[], 
                        String physParamName)
   {
      name = new String(varName);
      type = dataType;
      attr = new LinkedHashMap<String, ArgoAttribute>();
      fillable = false;
      extraDims = false;
      altDims = false;

      log.debug("'{}': type {}; length dim {}; phys param {}", 
                varName, dataType, varDim.length, physParamName);

      dim = new ArgoDimension[varDim.length];
      StringBuilder dString = new StringBuilder();
        
      for (int i = 0 ; i < varDim.length ; i++) {
         dim[i] = varDim[i];
         String dName = dim[i].getName();

         dString.append(dName+" ");
         
         if (dName.equals("N_CALIB")) {
            calibDim = i;
         } else if (dName.equals("N_PROF")) {
            profileDim = i;
         } else if (dName.equals("N_PARAM")) {
            paramDim = i;
         } else if (dName.equals("N_LEVELS")) {
            levelsDim = i;
         }

         if (varDim[i].isAlternateDimension()) {
            altDims = true;
         }
      }
      dimsString = new String(dString).trim();
      
      //..is this a "string variable"?
      if (dim.length >= 1  &&
          dim[dim.length-1].getName().startsWith("STRING")) {
         string = true;
      } else {
         string = false;
      }

      //..is this a "physical parameter variable"?
      if (physParamName != (String) null) {
         //..this is a "parameter" variable

         paramName = new String(physParamName);

      } else {
         paramName = null;
      }
   }

   //.........................................................
   //                  ACCESSORS
   //.........................................................

   public ArgoAttribute   getAttribute(String attrName) { 
      return attr.get(attrName);
   }
   public Collection<ArgoAttribute> getAttributes() { 
      return attr.values();
   }
   public Set<String> getAttributeNames() { 
      return attr.keySet();
   }
   public ArgoDimension[] getDimension() { return dim; }
   public ArgoDimension   getDimension(int n) {
      if (n < dim.length) {
         return dim[n];
      } else {
         return null;
      }
   }
   public int             getCalibDimension() { return calibDim; }
   public String          getDimensionsString() { return dimsString; }
   public String          getName() { return new String(name); }
   public Variable        getNcVar() { return this.ncVar; }
   public String          getParamName() { return paramName; }
   public int             getParamDimension() { return paramDim; }
   public int             getProfileDimension() { return profileDim; }
   public int             getRank() { return dim.length; }
   public DataType        getType()  { return type; }
   
   public boolean canHaveAlternateDimensions() { return altDims; }
   public boolean canHaveExtraDimensions() { return extraDims; }
   public boolean isFillable() { return fillable; }
   public boolean isParamVar() { return (paramName != null); }
   public boolean isPerCalib() { return (calibDim > -1); }
   public boolean isPerParam() { return (paramDim > -1); }
   public boolean isPerProfile() { return (profileDim > -1); }
   public boolean isPerLevel() { return (levelsDim > -1); }
   public boolean isString() { return string; }

   //.........................................................
   //                 METHODS
   //.........................................................
   
   public void setNcVar(Variable var) { this.ncVar = var; }

   public String toString () {
      return(name + ": Type = " + type + "   Dimensions = " + dim);
   }

   public void addAttribute(String name, String value) {
      ArgoAttribute a = new ArgoAttribute(name, value);
      if (name.equals("_FillValue")) fillable = true;
      /////boolean t = attr.add(a);
      attr.put(name, a);
   }
   public void addAttribute(String name, Number value) {
      ArgoAttribute a = new ArgoAttribute(name, value);
      if (name.equals("_FillValue")) fillable = true;
      /////boolean t = attr.add(a);
      attr.put(name, a);
   }
   public void addAttribute(String name, Object value) {
      ArgoAttribute a = new ArgoAttribute(name, value);
      if (name.equals("_FillValue")) fillable = true;
      /////boolean t = attr.add(a);
      attr.put(name, a);
   }


   public void addSpecialAttribute(String name, ArgoAttribute.AttrHandling handle, DataType type) {
      ArgoAttribute a = new ArgoAttribute(name, handle, type);
      attr.put(name, a);
   }
   public void addSpecialAttribute(String name, ArgoAttribute.AttrHandling handle, DataType type,
                                   String def) {
      ArgoAttribute a = new ArgoAttribute(name, handle, type, def);
      attr.put(name, a);
   }


   public Iterator<String> attributeIterator()
   {
      return (attr.keySet().iterator());
   }

   /**
    * Makes the variable an "extra-dimension" variable. The "extra dimensions",
    * are completely optional. There may be zero or more of them.
    * <p> NOTE: The "extra dimension" names are specific to a given data file.
    * They are not known at the "specification level".
    */
   public void setHaveExtraDimension() {
      extraDims = true;
   }

}
