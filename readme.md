To build the project and install javascript libraries

    mvn clean install -Pinstall-web-templates
    
## Security
To run the applications you need to install the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files:
    
1. Download  Unlimited Strength Jurisdiction Policy Zip [(available at Oracle)](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)
2. Open **Unlimited Strenght Jurisdiction Policy ZIP** and extract the files **local_policy.jar** and **US_export_policy.jar**
3. Locate the **Java Security lib** folder on your computer $JAVA_HOME\jre\lib\security
4. Make backup copies the existing local_policy.jar** and **US_export_policy.jar** files present in the **Java Security Lib** folder.
5. Copy the extracted copies of the **local_policy.jar** and **US_export_policy.jar** files into the **Java Security Lib** folder.

### Server certificates

**DO NOT USE THE PROJECT PROVIDED CERTIFICATES IN PRODUCTION**. You should get a 
real certificate signed by a trusted certificate authority.

[**Generación y configuración del almacén de claves de la aplicación**](https://github.com/votingsystem/votingsystem/wiki/Almacenes-de-claves).

## TimeStamp server

All the signatures made by the system incorporates a TimeStamp, the system 
needs a TimeStamp server to add TimeStamps to the signatures.
You need to install [one Time-Stamp server](https://github.com/votingsystem/timestamp-server) an provide 
**timestampServerURL** on **config.properties** with the value of your installation.