#### Config files

The application expects the content of the directory 'docs/app-config-files' with values appropriate to the 
installation in the directory:

    /var/local/wildfly/votingsystem-idprovider

It can be changed setting system environment var 'idprovider_server_dir' with the desired value


#### Logging

Application will store log files in directory:
    
    /var/log/wildfly/votingsystem-idprovider
    
You must put the permissions of the directory of appropriate form to the user executing Wildfly




#### Build and Deploy on Wildfly
1. 
2. Run the script **setup-database-postgres.sh** in order to create the database (it must be in the same folder that **timestamp-server-postgres.sql**)
3. Make sure you have  Wildfly server started.
4. Make sure theres configured a _datasource_ with the name:
    
    
    java:jboss/datasources/voting-idprovider
        
5. Use this command to build and deploy the archive:

            mvn clean package wildfly:deploy

## Proveedor de identidad

Este proyecto es un proveedor de identidad preparado para ser utilizado en el sistema de votación.
Deberían estar interesados en su instalación los paises que quieran permitir
que sus ciudadanos puedan participar en una votación.

El mecanismo utilizado para obtener la identidad del ciudadano consiste en un documento
con firma XaDES realizada con el DNIe desde el móvil. 

las definiciones de los servicios se encuentran en 
    
    $serverURL/api.yml.jsp


## Garantía de anonimato de los certificados anónimos
En esta primera aproximación el anonimato sólo está garantizado si el proveedor de identidad no guarda ningún dato que
relacione la solicitud firmada con certificado expedido.

### Configuration
 - The application has been developed and tested on:
        
       - Application server: Wildfly 10.
        
       - Database: Postgres 9.5
       
 - The application default _working dir_ is in **/var/local/wildfly/votingsystem-idprovider**,
it needs read and write privileges. You can modify that location changing the system property **votingsystem_idprovider_dir**
 - Copy inside the _working dir_ the contents of **docs/app-config-files** completed with the values of your installation   
 - The file _trusted-certs.jks_ contains the trusted CA certificates.
    
 
#### trustedServiceProviders.xml
 
File with the list of server entities the id provider can deal with. You must
put here the TimeStampServers and the Voting Service Providers 
    
## Dates
Servers always return a dates in the standardized ISO 8601-format.

- In javascript you can:

        var utcDate = '2011-06-29T16:52:48.000Z';  // ISO-8601 formatted date returned from server
        var localDate = new Date(utcDate);

### Certificates
Certificate dates are expressed as UTC time (Coordinated Universal Time) 
to reduce confusion with the local time zone use. Comparison are made to the millisecond.

**genTime** is the time at which the time-stamp token has been created by
the TSA.  It is expressed as UTC time (Coordinated Universal Time) to
reduce confusion with the local time zone use. 


## Provisioned as pre-configured virtual machine in Open Virtualization Format. 

Configuration of the virtual machine by the operator SHOULD be supported by scripts or similar provided by the notifying MS:
  
  http://www.ibm.com/developerworks/library/l-open-virtualization-format-toolkit/
  https://access.redhat.com/documentation/en-US/Red_Hat_Enterprise_Virtualization/3.6/html/Virtual_Machine_Management_Guide/sect-Exporting_and_Importing_Virtual_Machines_and_Templates.html
  
  
  OVF 1.1 was adopted as an International Standard in August 2011 by the Joint Technical Committee 1 (JTC 1) of the International Organization for Standardization (ISO)
  The entire directory can be distributed as an OVA package, which is a tar archive file with the OVF directory inside.

El archivo lleva incorporado un 'servidor de sellos de tiempo' y un proveedor de indentidad



