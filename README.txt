*********************************************
CSS Background-image data-uri substition demo
*********************************************

See http://www.bazageous.com/2011/06/17/dynamic-background-image-data-uri-substitution/.

You will need Maven installed. To start Jetty running the web application, run:

>mvn jetty:run

and then navigate to

http://localhost:8080/datauri/css/styles.css?useDataUri (for the data uri substituted version) or
http://localhost:8080/datauri/css/styles.css (for the original version)