# voting system control center application

#### Build and Deploy

    mvn clean package


#### Build and Deploy on Wildfly

1. Make sure you [start the Wildfly Server]
2. Add a datasource with the name 'java:jboss/datasources/ControlCenter'
3. Use this command to build and deploy the archive:

            mvn clean package wildfly:deploy