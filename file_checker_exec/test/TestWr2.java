import ucar.nc2.*;
import ucar.ma2.*;
import java.io.*;
import java.util.*;

//..creates a 2 row 2D array and
//..adds rows 2 & 3 to test.nc

public class TestWr2
{

   public static void main (String args[])
      throws IOException
   {
      NetcdfFileWriter out = null;
      Dimension dim[];

      int n_dim = 20;
      int m_dim = 10;
      int k_dim = 5;

      //..open netCDF file for writing

      try {
         out = NetcdfFileWriter.openExisting("test.nc");
      } catch (Exception e) {
         System.out.println("write caused exception: "+e);
         System.exit(1);
      }

      Variable var1 = out.findVariable("var1");

      //double dbl[][] = new double[n_dim][m_dim];
      double dbl[][] = new double[2][m_dim];

      for (int n = 0; n < 2; n++) {
         for (int m = 0; m < m_dim; m++) {
            dbl[n][m] = (double) (n+2) + ((double) m / 10.);
         }
      }

      Array arr = Array.factory(dbl);

      int origin[] = {2, 0};

      try {
         out.write(var1, origin, arr);
      } catch (Exception e) {
         System.out.println("write caused exception: "+e);
      }

      out.close();
   }


} //..end class
