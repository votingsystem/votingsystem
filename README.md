## Voting System
The project needs Java 8 and JavaFX. In Debian you can install then with this commands:

        sudo apt-get install openjdk-8-jdk
        sudo apt-get install openjfx
        update-java-alternatives -s java-1.8.0-openjdk-amd64

In order to run the applications you must set appropiated values in '*_DEVELOPMENT.properties' and '*_PRODUCTION.properties' files


* For development You need a DNS server in your intranet. That DNS server must redirect the domains especified on *.properties files to the IPs running the applications. 
* The values of the actual *.properties corresponds with a development PC that has its IP associated with de domain name **currency**
and another machine on the same intranet that has its IP associated with de domain name **www.votingsystem.org**.
* In the machine which IP is associated with **www.votingsystem.org** there's an instance of [Wildfly](http://wildfly.org) with the ports redirected:

        sudo /sbin/iptables -t nat -I PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
        sudo /sbin/iptables -t nat -I PREROUTING -p tcp --dport 443 -j REDIRECT --to-port 8443

        sudo /sbin/iptables -t nat -A OUTPUT -p tcp -d $PUT_HERE_YOUR_SERVER_IP --dport 80 -j DNAT --to $PUT_HERE_YOUR_SERVER_IP:8080
        sudo /sbin/iptables -t nat -A OUTPUT -p tcp -d $PUT_HERE_YOUR_SERVER_IP --dport 443 -j DNAT --to $PUT_HERE_YOUR_SERVER_IP:8443



All the applications need access to an instance of the [timestamp server](https://github.com/votingsystem/votingsystem/tree/master/TimeStampServer) to add timestamps to the signatures


[Wiki](https://github.com/votingsystem/votingsystem/wiki)

[Creación de las bases de datos]
(https://github.com/votingsystem/votingsystem/wiki/bases-de-datos)


