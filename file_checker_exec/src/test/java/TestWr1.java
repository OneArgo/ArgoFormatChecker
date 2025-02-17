import ucar.nc2.*;
import ucar.ma2.*;
import java.io.*;
import java.util.*;


//..creates test.nc with a 2 array of doubles
//..creates a 2-row 2D array and writes to rows 0 & 1

public class TestWr1
{

   public static void main (String args[])
      throws IOException
   {
      NetcdfFileWriter out;

      int m_dim = 10;

      //..open netCDF file for writing
      out = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, "test.nc");
      out.setFill(true);

      Variable var1 = out.addVariable((Group) null, "var1", DataType.DOUBLE, "N M");

      //Double fill = new Double(99999.);
      Double fill = new Double(ucar.nc2.iosp.netcdf3.N3iosp.NC_FILL_DOUBLE);

      Attribute attr = new Attribute("_FillValue", (Number) fill);
      var1.addAttribute(attr);

      out.setFill(true);
      out.create();

      //double dbl[][] = new double[n_dim][m_dim];
      double dbl[][] = new double[2][m_dim];

      for (int n = 0; n < 2; n++) {
         for (int m = 0; m < m_dim; m++) {
            //            dbl[n][m] = (double) n + ((double) m / 10.);
            dbl[n][m] = fill;
         }
      }

      Array arr = Array.factory(dbl);

      try {
         out.write(var1, arr);
      } catch (Exception e) {
         System.out.println("write caused exception: "+e);
      }

      out.close();
   }


} //..end class
