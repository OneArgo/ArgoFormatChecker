import ucar.nc2.*;
import ucar.ma2.*;
import java.io.*;
import java.util.*;


public class Testwr
{

   public static void main (String args[])
      throws IOException
   {
      NetcdfFileWriter out = null;

      //..open netCDF file for writing

      try {
         out = NetcdfFileWriter.openExisting("out.nc");
      } catch (Exception e) {
         System.out.println("write caused exception: "+e);
         System.exit(1);
      }

      Variable var1 = out.findVariable("DATA_TYPE");

      Array arr = ArrayChar.makeFromString("test string", 16);

      try {
         out.write(var1, arr);
      } catch (Exception e) {
         System.out.println("write caused exception: "+e);
      }

      out.close();
   }


} //..end class
