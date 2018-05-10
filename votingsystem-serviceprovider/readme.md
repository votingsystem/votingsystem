## Config files

The application expects the content of the directory 'docs/app-config-files' with values appropriate to the 
installation in the directory:

    /var/local/wildfly/votingsystem-service

It can be changed setting system environment var 'voting_provider_server_dir' with the desired value


#### Certificates
Certificate time comparison is done in the UTC time zone. The certificate expiry 
date is converted to UTC and a comparison made to the millisecond.

**genTime** is the time at which the time-stamp token has been created by
the TSA.  It is expressed as UTC time (Coordinated Universal Time). 

#### Logging
Application will store log files in directory:
    
    /var/log/wildfly/votingsystem-serviceprovider
    
you must put the permissions of the directory of appropriate form to the user executing Wildfly
    