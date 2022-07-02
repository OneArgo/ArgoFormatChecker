import ucar.nc2.*;
import ucar.ma2.*;
import java.io.*;
import java.util.*;


public class TestReadSection
{

   public static void main (String args[])
      throws IOException
   {
      NetcdfFile in = null;

      //..open netCDF file for reading
      //..needs to be a Argo profile file

      try {
         in = NetcdfFile.open("in.nc");
      } catch (Exception e) {
         System.out.println("write caused exception: "+e);
         System.exit(1);
      }

      Variable iVar = in.findVariable("PRES");

      try {
         out.write(var1, arr);
      } catch (Exception e) {
         System.out.println("write caused exception: "+e);
      }

      out.close();
   }


} //..end class
