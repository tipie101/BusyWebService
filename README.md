This is a Java 11 (spring) project.
The dependencies are maven-organized and contained by the file pom.xml.  

Program starting-point: 
src/main/java/com/example/AdexWebService/AdexWebServiceApplication.java



MYSQL DB:
Config of Connection in src/man/resources/application.properties
Name of DB: testing
Dump of Example-DB can be found in db_dump/*

Note:
The implementation fulfills the coding task and was tested locally.
However, in a real-life scenario of billions of request per day it is more 
advisable to use technology that scales extremely well -
maybe Cassandra and an asynchronous Webservice.
