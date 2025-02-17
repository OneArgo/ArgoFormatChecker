
import java.io.*;
import java.util.Date;
import usgdac.ArgoDate;

public class TestArgoDate {

   public static void main (String[] args)
   {

      Help();

      BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
      String dtg = null;

      while (true) {
         System.out.println("");
         System.out.print("Enter dtg: ");

         try {
            String d = cin.readLine();
            if (d.length() > 0) {
               dtg = d;
            }
                  
         } catch (Exception e) {
            System.out.println("Exception during read.");
            System.exit(1);
         }

         if (dtg.equals("quit")) {
            System.out.println("");
            System.out.println("...terminating...");
            System.exit(0);
         }

         Date date = ArgoDate.get(dtg);
         if (date == null) {
            System.out.println("failed");
         } else {
            System.out.println(date);
         }

      }
   }

   public static void Help() {
      System.out.println("<CR> at prompt reuses previous entry");
      System.out.println("Enter \"quit\" to terminate");
      System.out.println("");
   }


}