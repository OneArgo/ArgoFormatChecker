import java.io.*;
import java.util.*;
import java.util.regex.*;


public class TestPattern
{

   public static void main (String args[])
      throws IOException
   {
      int next;
      for (next = 0; next < args.length; next++) {
         if (args[next].equals("-help")) {
            Help();
            System.exit(0);
         } else if (args[next].startsWith("-")) {
            stderr.println("Invalid argument: '"+args[next]+"'");
            System.exit(1);
         } else {
            break;
         }
      }

      if (args.length - next < 2) {
         stderr.println("Not enough arguments");
         System.exit(1);
      }

      String pat = args[next];

      stdout.printf("\nPattern = '%s'\n\n", pat);

      Pattern pattern;
      Matcher matcher;

      pattern = Pattern.compile(pat);

      for (next = next+1; next < args.length; next++) {
         String str = args[next];
         stdout.printf("String = '%s'\n", str);

         Matcher m = pattern.matcher(str);

         if (m.matches()) {
            stdout.printf("\n   MATCHES\n\n");
         } else {
            stdout.printf("\n   NO match\n\n");
         }

         int nGroup =  m.groupCount();
         stdout.printf("   groupCount = %d\n\n", nGroup);

         int nMatch = 0;
         while (m.find()) {
            nMatch++;
            stdout.printf("   %d)\n", nMatch);

            for (int n = 0; n <= nGroup; n++) {
               stdout.printf("      group #%d) '%s'\n", n, m.group(n));
            }

            stdout.println();
         }

      }

   }

   //...................................
   //..              Help             ..
   //...................................
   public static void Help ()
   {
      stdout.println (
      "\n"+
      "Purpose: Tests a Pattern\n" +
      "\n"+
      "Usage: java TestPattern pattern string ...\n"+
      "\n"+
      "Options:\n"+
      "\n"+
      "Arguments:\n"+
      "   output-file      Output file\n" +
      "   profile-files    Input Argo NetCDF profile file(s)\n"+
      "\n");
      return;
   }

   
   //..standard i/o shortcuts
   static PrintStream stdout = new PrintStream(System.out);
   static PrintStream stderr = new PrintStream(System.err);

} //..end class
