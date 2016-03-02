## TimeStamp server

#### enable appplication filter
To enable application filters change what follows to the standalone / domain  server configuration 
([details] (https://docs.jboss.org/author/display/WFLY8/Undertow+subsystem+configuration)):
    
        <servlet-container name="default" allow-non-standard-wrappers="true">

#### Build and Deploy on Wildfly
1. Application environment default is set to DEVELOPMENT, to change that add what follows to the standalone / domain 
server configuration just after the extensions section:

        <system-properties>
                <property name="vs.environment" value="PRODUCTION"/>
        </system-properties>
        
2. Make sure you have  Wildfly server started.
3. Add the datasource in Wildfly through the management interface:

        Name: TimeStampServer
        JNDI Name: java:jboss/datasources/TimeStampServer
        Connection URL: jdbc:postgresql://localhost:5432/TimeStampServer
        username:userVS
        password:userVS
        
4. Use this command to build and deploy the archive:

            mvn clean package wildfly:deploy
            
#### Certificates
[Certificates](src/main/resources/readme.md)