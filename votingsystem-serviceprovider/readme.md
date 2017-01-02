#### Certificates
Certificate time comparison is done in the UTC time zone. The certificate expiry 
date is converted to UTC and a comparison made to the millisecond.

**genTime** is the time at which the time-stamp token has been created by
the TSA.  It is expressed as UTC time (Coordinated Universal Time) to
reduce confusion with the local time zone use. 

#### Logging
Application will store log files on dir:
    
    /var/log/middleware/votingsystem-serviceprovider
    