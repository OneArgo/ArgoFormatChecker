package usgdac;

import ucar.ma2.DataType;

/**
 * Encapsulates the specific features of attributes in the specification
 * of Argo data files.  They do not extend a netCDF Attribute because
 * they can exist independently of any netCDF actions.  That said,
 * an attempt has been made to mimic the API of the netCDF Attribute
 * with Argo-specific extensions.
 * <p>
 * @version  $HeadURL: https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoAttribute.java $
 * @version  $Id: ArgoAttribute.java 895 2018-04-11 16:30:23Z ignaszewski $
 */
public class ArgoAttribute {
   //...............CLASS VARIABLES.................

/**
 * Argo attribute "handling" falls into one of the following categories:
 * <ul>
 * <li>FULLY_SPECIFIED - Attribute must exist; value is specified
 * <li>IGNORE_COMPLETELY - Attribute may exist or not; value is NOT specified
 * <li>IGNORE_VALUE - Attribute must exist; value is NOT specified
 * <li>NOT_ALLOWED - Attribute must NOT exist
 */
   public static enum AttrHandling {
      FULLY_SPECIFIED      ("Fully specified"),
      IGNORE_COMPLETELY    ("Ignore completely"),
      IGNORE_VALUE         ("Must exist; Ignore value"),
      NOT_ALLOWED          ("Must not exist");

      public final String description;

      AttrHandling(String s) { description = s; }
   }

   //.................INSTANCE VARIABLES................

   private String         name;
   private final Object   value;
   private DataType       type;
   private boolean        isNumeric;
   private AttrHandling   handling;
   private final String   default_val;

   //..................................................
   //                   CONSTRUCTORS
   //..................................................

   /**
    * Instantiate an numeric attribute with "FULLY_SPECIFIED" handling.
    * The type of Number will be determined by the type "value"
    */
   public ArgoAttribute (String name, Number value)
   { 
      this.name = new String(name);
      this.value = value;
      this.type = determineNumberType(value);
      this.isNumeric = true;

      this.handling = AttrHandling.FULLY_SPECIFIED;
      this.default_val = null;
   }

   /**
    * Instantiate an String attribute with "FULLY_SPECIFIED" handling.
    */
   public ArgoAttribute (String name, String value)
   {
      this.name = new String(name);
      this.value = new String(value);
      this.type = DataType.STRING;
      this.isNumeric = false;

      this.handling = AttrHandling.FULLY_SPECIFIED;
      this.default_val = null;
   }

   /**
    * Instantiate an (generic) Object attribute with "FULLY_SPECIFIED" handling.
    */
   public ArgoAttribute (String name, Object value)
   {
      this.name = new String(name);
      this.value = value;
      this.type = DataType.OBJECT;
      this.isNumeric = false;

      this.handling = AttrHandling.FULLY_SPECIFIED;
      this.default_val = null;
   }

   //..constructors for attributes with special handling characteristics
   //..need to know the data type

   /**
    * Instantiate an attribute with the specified "type" and "handling".
    * <p>NOTICE: The "value" is "null" --- suggesting this is not used
    * for "FULLY_SPECIFIED" attributes.
    */
   public ArgoAttribute (String name, AttrHandling handling, DataType type)
   {
      this.name = new String(name);
      this.value = null;
      this.type = type;
      this.isNumeric = type.isNumeric();

      this.handling = handling;
      this.default_val = null;
   }

   /**
    * Instantiate an attribute with the specified "type", "handling",
    * and default value.
    * <p>NOTICE: There value is "null" --- suggesting this is not used
    * for "FULLY_SPECIFIED" attributes.
    */
   public ArgoAttribute (String name, AttrHandling handling, 
                         DataType type, String def)
   {
      this.name = new String(name);
      this.value = null;
      this.type = type;
      this.isNumeric = type.isNumeric();

      this.handling = handling;
      this.default_val = new String(def);
   }

   //............................................................
   //                        ACCESSORS
   //............................................................
   
   /**
    * Get the name of the attribute
    */
   public String   getName()  { return(new String(name)); }
   /**
    * Get the value of the attribute.  Can be null.
    */
   public Object   getValue() { return(value); }
   /**
    * Get the default value of the attribute.  Can be null.
    */
   public String   getDefaultValue() { return(default_val); }
   /**
    * Get the data type of the attribute.
    */
   public DataType getType() { return(type); }
   /**
    * Get the default value of the attribute.  Can be null.
    */
   public Object   getDefault() { return(default_val); }
   /**
    * Get the "special handling" instruction of the attribute.
    */
   public AttrHandling getHandling() { return(handling); }

   /**
    * Is the attribute numeric
    */
   public boolean isNumeric() { return(this.isNumeric); }
   /**
    * Is the attribute a String
    */
   public boolean isString()  { return(type == DataType.STRING); }

   //............................................................
   //                       METHODS
   //............................................................

   /**
    * Determine what type of Number the argument is
    */
   private DataType determineNumberType (Number v) {
      if (v instanceof Double) {
         return(DataType.DOUBLE);
      } else if (v instanceof Float) {
         return(DataType.FLOAT);
      } else if (v instanceof Integer) {
            return(DataType.INT);
      } else if (v instanceof Long) {
         return(DataType.LONG);
      } else if (v instanceof Short) {
         return(DataType.SHORT);
      } else {
         return(null);
      }
   }
}
