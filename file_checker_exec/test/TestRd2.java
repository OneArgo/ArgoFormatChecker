import ucar.nc2.*;
import ucar.ma2.*;
import java.io.*;
import java.util.*;


//..uses test.nc from the TestWr*.java
//..reads rows 0 & 1

public class TestRd2
{

   public static void main (String args[])
      throws IOException
   {
      NetcdfFile in;

      //..open netCDF file for writing
      in = NetcdfFile.open("test.nc");

      Variable var1 = in.findVariable("var1");

      int shape[] = var1.getShape();
      for (int n = 0; n < shape.length; n++) {
         System.out.println("variable shape["+n+"]: "+shape[n]);
      }
      
      int origin[] = {0, 0};
      shape[0] = 2;

      
      Array v = null;
      try {
         v = var1.read(origin, shape);
      } catch (Exception e) {
         System.out.println(e);
      }

      int ashape[] = v.getShape();
      for (int n = 0; n < ashape.length; n++) {
         System.out.println("array shape["+n+"]: "+ashape[n]);
      }

      System.out.println(v);

      in.close();
   }


} //..end class
