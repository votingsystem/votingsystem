# cryptocurrency application

#### Build and Deploy

    mvn clean package


#### Build and Deploy on Wildfly
- In the Standalone mode, changes will go into standalone.xml
- In the Domain mode the changes will go into domain.xml

1. To enable application filters change in the standalone/configuration/standalone.xml file the servlet-container
XML element so that it has the attribute allow-non-standard-wrappers="true" (https://docs.jboss.org/author/display/WFLY8/Undertow+subsystem+configuration)
Para el filtro vs <servlet-container name="default" allow-non-standard-wrappers="true"> 
2. Application environment default is set to DEVELOPMENT, to change that add the system-properties  element right after the extensions element:

        <system-properties>
                <property name="vs.environment" value="PRODUCTION"/>
        </system-properties>

3. Make sure you [start the Wildfly Server]
4. Add a datasource with the name 'java:jboss/datasources/CrytoCurrencyServer'
5. Use this command to build and deploy the archive:

            mvn clean package wildfly:deploy