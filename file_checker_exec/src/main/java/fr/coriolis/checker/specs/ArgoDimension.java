package fr.coriolis.checker.specs;

import java.util.LinkedHashSet;
import ucar.nc2.Dimension;


/**
 * ArgoDimension.java
 *
 * @version  $HeadURL: https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoDimension.java $
 * @version  $Id: ArgoDimension.java 520 2016-06-21 17:57:28Z ignaszewski $
 */

public class ArgoDimension {

   //..................Constructors.................
   /** Creates new ArgoDimension */

   public ArgoDimension(String n) {
      name = new String(n);
      ncDim = null;
      extra_dimension = false;
      altDimension = null;
      value = Integer.MIN_VALUE;
   }

   public ArgoDimension(String n, int s) {
      this(n);
      value = s;
   }

      

   //............Methods..............
   public String    getName()      { return(new String(name)); }
   public int       getValue()     { return(value); }
   public Dimension getDimension() { return(ncDim); }
   public boolean   isExtraDimension() { return(extra_dimension); }
   
   public void      setDimension(Dimension d) { ncDim = d; }
   public void      setValue (int v) { value = v; }
   
   public String    toString() { return (name + ": " + value); }

   public void setExtraDimension() {
      extra_dimension = true;
   }

   public boolean isAlternateDimension() {
      return (altDimension != null);
   }

   public void addAlternateDimensionName(String alt_name) {
      if (altDimension == null) {
         altDimension = new LinkedHashSet<String>(5);
      }

      altDimension.add(alt_name);
   }

   public boolean isAllowedAlternateDimensionName(String name) {
      if (altDimension == null) return false;

      return altDimension.contains(name);
   }

   public String[] alternateDimensionNames() {
      if (altDimension == null) return null;

      return altDimension.toArray(new String[0]);
   }

   //.............Member Variables................
   
   private String name;
   private int value; 
   private boolean extra_dimension;
   private Dimension ncDim;
   private LinkedHashSet<String> altDimension;
}
