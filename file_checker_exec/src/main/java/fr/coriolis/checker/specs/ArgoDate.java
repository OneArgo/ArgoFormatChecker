package fr.coriolis.checker.specs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.lang.Math;


/**
 * @version  $HeadURL: https://inversion.nrlmry.navy.mil/svn/godae/trunk/argo/bin/java/usgdac/ArgoDate.java $
 * @version  $Id: ArgoDate.java 963 2018-08-09 16:34:21Z ignaszewski $
 */

public class ArgoDate extends Date
{
   //******************************************************
   //                  VARIABLES
   //******************************************************

   //............class variables.................

   private static Calendar cal;
   private static SimpleDateFormat dateFormat;
   private static long refTime = Long.MAX_VALUE;
   private static TimeZone tz;

   private static HashMap<String, SimpleDateFormat> validFormat = 
      new HashMap<String, SimpleDateFormat>();

   private static Pattern nonDigit;
   
   static {
      tz = TimeZone.getTimeZone("GMT");
      cal = Calendar.getInstance(tz);
      refTime = getRefTime();
      
      dateFormat = (SimpleDateFormat) DateFormat.getInstance();
      dateFormat.setTimeZone(tz);
      dateFormat.applyPattern("yyyyMMddHHmmss");
      dateFormat.setLenient(false);


      SimpleDateFormat DF = (SimpleDateFormat) DateFormat.getInstance();
      DF.setTimeZone(tz);
      DF.setLenient(false);

      SimpleDateFormat df;

      //..day month year
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("ddMMyyyy");
      validFormat.put("DDMMYYYY", df);

      //..year month day hour minute second
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("yyyyMMddHHmmss");
      validFormat.put("YYYYMMDDHHMMSS", df);

      //..year month day
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("yyyyMMdd");
      validFormat.put("YYYYMMDD", df);

      //..year
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("yyyy");
      validFormat.put("YYYY", df);

      //..month (number)
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("MM");
      validFormat.put("MM", df);

      //..day of month
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("dd");
      validFormat.put("DD", df);

      //..clock: hour minute second
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("HHmmss");
      validFormat.put("HHMMSS", df);

      //..clock: hour minute
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("HHmm");
      validFormat.put("HHMM", df);

      //..clock: minute second
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("mmss");
      df.setLenient(false);
      validFormat.put("MMSS", df);

      //..clock: hour
      df = (SimpleDateFormat) DF.clone();
      df.applyPattern("HH");
      validFormat.put("HH", df);

      //..

      nonDigit = Pattern.compile(".*\\D.*");
   }

   //static PrintStream stdout = new PrintStream(System.out);

   //******************************************************
   //                METHODS
   //******************************************************


   public static String format(Date date) { return dateFormat.format(date); }
   
   public static Date get(long long_juld)
   {
      return (new Date(refTime + long_juld));
   }

   public static Date get(double juld)
   {
      long long_juld = Math.round(juld * 24.D * 3600.D * 1000.D);
      return (new Date(refTime + long_juld));
   }

   /**
    * Checks the input date/time "pattern" against the known patterns typically used as 
    * Technical Parameter Units and checks the "value" to see if it conforms to the pattern.
    *
    * @param pattern  Date/time pattern to be checked against known Argo data/time patterns
    *                 (typically used as units in Argo technical files)
    * @param value  Date/time value to be compared to the pattern
    * @return Boolean null if pattern is not a known Argo pattern; true if the pattern is
    * known and the value is valid; false if the pattern is known but the value is invalid
    */
   public static Boolean checkArgoDatePattern (String pattern, String value)
   {
      SimpleDateFormat format = validFormat.get(pattern);

      if (format == null) {
         return null;
      }

      if (value.length() != pattern.length() || nonDigit.matcher(value).matches()) {
         return new Boolean(false);
      }


      try {
         Date date = format.parse(value);
      } catch (ParseException e) {
         return new Boolean(false);
      }
      return new Boolean(true);
   }

   /**
    * Returns a Date object for an Argo string date value, checking for validity
    *
    * @param dtg  String (14-char) date/time setting
    * @return Date object or null if input dtg is illegal
    */
   public static Date get(String dtg)
   {
      Date date;

      try {
         //..
         date = dateFormat.parse(dtg);

         String tst = dateFormat.format(date);
         if (! dtg.equals(tst)) {
            date = null;
         }
         
      } catch (ParseException e) {
         date = null;
      }   

      return (date);
   }

   private static long getRefTime() {
      cal.set(1950, 00, 01, 00, 00, 00);

      //..this is odd.  The above cal.set does NOT intialize the millisecond
      //..value of cal.  The milliseconds are set to something that is 
      //..runtime depedent.  Thus the "cal" is not repeatable at sub-second
      //..resolution.  So...
      //..initialize milliseconds to 0
      cal.set(Calendar.MILLISECOND, 0);

      return (cal.getTimeInMillis());
   }

}
