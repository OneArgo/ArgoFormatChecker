import ucar.nc2.*;
import ucar.ma2.*;
import java.io.*;
import java.util.*;


//..uses test.nc from the TestWr*.java
//..reads all

public class TestRd1
{

   public static void main (String args[])
      throws IOException
   {
      NetcdfFile in;

      //..open netCDF file for writing
      in = NetcdfFile.open("test.nc");

      Variable var1 = in.findVariable("var1");

      Array v = var1.read();

      int shape[] = v.getShape();

      for (int n = 0; n < shape.length; n++) {
         System.out.println("shape["+n+"]: "+shape[n]);
      }

      System.out.println(v);

      in.close();
   }


} //..end class
