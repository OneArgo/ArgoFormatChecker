
import java.io.*;
import usgdac.ArgoDate;

public class TestArgoDatePattern {

   public static void main (String[] args)
   {

      Help();

      BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
      String pattern = null;
      String value = null;

      while (true) {
         System.out.println("");
         System.out.print("Enter pattern: ");

         try {
            String p = cin.readLine();
            if (p.length() > 0) {
               pattern = p;
            }
                  
         } catch (Exception e) {
            System.out.println("Exception during read.");
            System.exit(1);
         }

         if (pattern.equals("quit")) {
            System.out.println("");
            System.out.println("...terminating...");
            System.exit(0);
         }

         System.out.print("Enter value:   ");

         try {
            String v = cin.readLine();
            if (v.length() > 0) {
               value = v;
            }
         } catch (Exception e) {
            System.out.println("Exception during read.");
            System.exit(1);
         }

         System.out.println("");
         System.out.println("Checking:  Pattern = '"+pattern+"'  Value = '"+value+"'");

         Boolean success;
         success = ArgoDate.checkArgoDatePattern(pattern, value);

         if (success == null) {
            System.out.println("Unknown pattern");

         } else if (success) {
            System.out.println("Known pattern:  Valid date string");

         } else {
            System.out.println("Known pattern:  Invalid date string");
         }

      }
   }

   public static void Help() {
      System.out.println("<CR> at prompt reuses previous entry");
      System.out.println("Enter \"quit\" to terminate");
      System.out.println("");
   }


}