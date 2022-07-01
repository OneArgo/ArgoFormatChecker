The following were removed from the netcdfAll-4.3.jar.

     0 Fri Apr 04 13:09:28 UTC 2014 org/slf4j/
     0 Fri Apr 04 13:09:28 UTC 2014 org/slf4j/impl/
  6542 Fri Apr 04 13:09:28 UTC 2014 org/slf4j/impl/Log4jLoggerAdapter.class
  1504 Fri Apr 04 13:09:28 UTC 2014 org/slf4j/impl/Log4jLoggerFactory.class
  1932 Fri Apr 04 13:09:28 UTC 2014 org/slf4j/impl/Log4jMDCAdapter.class
  1470 Fri Apr 04 13:09:28 UTC 2014 org/slf4j/impl/StaticLoggerBinder.class
   839 Fri Apr 04 13:09:28 UTC 2014 org/slf4j/impl/StaticMarkerBinder.class
   696 Fri Apr 04 13:09:28 UTC 2014 org/slf4j/impl/StaticMDCBinder.class


They were causing SLF4J warnings at runtime because of multiple bindings
in CLASSPATH (those installed in ../logging)
