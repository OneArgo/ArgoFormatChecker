import ucar.nc2.*;
import ucar.ma2.*;
import java.io.*;

//..creates a 2D 2-row array with fewer columns
//..adds it to the bottom columns of rows 6 & 7

public class TestWr4
{

   public static void main (String args[])
      throws IOException
   {
      NetcdfFileWriter out = null;

      //..open netCDF file for writing

      try {
         out = NetcdfFileWriter.openExisting("test.nc");
      } catch (Exception e) {
         System.out.println("write caused exception: "+e);
         System.exit(1);
      }

      Variable var1 = out.findVariable("var1");

      //double dbl[][] = new double[n_dim][m_dim];
      double dbl[][] = new double[2][5];

      for (int n = 0; n < 2; n++) {
         for (int m = 0; m < 5; m++) {
            dbl[n][m] = (double) (n+6) + ((double) m / 10.);
         }
      }

      Array arr = Array.factory(dbl);

      int origin[] = {6, 5};

      try {
         out.write(var1, origin, arr);
      } catch (Exception e) {
         System.out.println("write caused exception: "+e);
      }

      out.close();
   }


} //..end class
