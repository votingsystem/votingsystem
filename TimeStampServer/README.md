## TimeStamp server

#### enable appplication filter
To enable application filters change what follows to the standalone / domain  server configuration 
([details] (https://docs.jboss.org/author/display/WFLY8/Undertow+subsystem+configuration)):
    
        <servlet-container name="default" allow-non-standard-wrappers="true">

#### Build and Deploy on Wildfly
1. Make sure you have  Wildfly server started.
2. Add the datasource in Wildfly through the management interface:

        Name: TimeStampServer
        JNDI Name: java:jboss/datasources/TimeStampServer
        Connection URL: jdbc:postgresql://localhost:5432/TimeStampServer
        username:userVS
        password:userVS
        
3. Use this command to build and deploy the archive:

            mvn clean package wildfly:deploy
            
#### Certificates
[Certificates](src/main/resources/readme.md)