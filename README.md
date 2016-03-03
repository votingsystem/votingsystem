The project needs Java 8 and JavaFX. In Debian you can install them with this commands:

        sudo apt-get install openjdk-8-jdk
        sudo apt-get install openjfx
        update-java-alternatives -s java-1.8.0-openjdk-amd64

Application environment default is set to DEVELOPMENT, to change that add what follows to the standalone / domain server configuration just after the extensions section:

        <system-properties>
                <property name="vs.environment" value="PRODUCTION"/>
        </system-properties>

In order to run the applications you must set appropiated values in '/src/main/resources/\*_DEVELOPMENT.properties' and '/src/main/resources/\*_PRODUCTION.properties' files

To run the applications you need to:

* Create the databases. With postgresql the script [init.sql](./libs/init.sql) does the job
* Install your database JDBC driver in Wildfly.
* Add in Wildfly the datasources through the management interface.

All the applications need access to a running instance of the [timestamp server](https://github.com/votingsystem/votingsystem/tree/master/TimeStampServer) 
to add timestamps to the signatures. You must install this app in your server before running the **voting system** or the **currency server**

#### Wildfly SSL

1. Create a self-signed certificate with the IP of the machine hosting the server, f.e. for a server with the IP 192.168.1.5 it would be:

        keytool -genkey \
                -alias localhost \
                -keyalg RSA \
                -validity 365 \
                -keypass password \
                -storepass password \
                -dname "C=Voting System, CN=192.168.1.5, O=votingsystem.org, OU=IT Division" \
	            -ext "san=ip:127.0.0.1,uri:https://192.168.1.5" \
                -keystore localhost.jks

2. Copy the created **localhost.jks** to the dir '${WILDFLY_HOME}/standalone/configuration' and run:

        ./bin/jboss-cli.sh << EOF 
        embed-server
        /core-service=management/security-realm=https:add()
        /core-service=management/security-realm=https/authentication=truststore:add(keystore-path=localhost.jks, keystore-password=password, keystore-relative-to=jboss.server.config.dir)
        /core-service=management/security-realm=https/server-identity=ssl:add(keystore-path=localhost.jks, alias=localhost, keystore-password=password, keystore-relative-to=jboss.server.config.dir)
        /subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https, security-realm=https, enable-http2=true)
        reload --admin-only=false
        EOF

3. Export the self signed cert:

        keytool -exportcert -alias localhost -keypass password -keystore localhost.jks -storepass password -rfc -file VotingSystemSSLCert.pem

4. Copy the created **VotingSystemSSLCert.pem** to the dirs 'votingsystem/VotingSystemAPI/src/main/resources' 
and 'votingsystem-android-client/app/src/main/assets'

5. Redirect ports in order to avoid them on URLs:

        sudo /sbin/iptables -t nat -I PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
        sudo /sbin/iptables -t nat -I PREROUTING -p tcp --dport 443 -j REDIRECT --to-port 8443

        sudo /sbin/iptables -t nat -A OUTPUT -p tcp -d 192.168.1.5 --dport 80 -j DNAT --to 192.168.1.5:8080
        sudo /sbin/iptables -t nat -A OUTPUT -p tcp -d 192.168.1.5 --dport 443 -j DNAT --to 192.168.1.5:8443


* [Here](http://undertow.io/blog/2015/03/26/HTTP2-In-Wildfly.html) you can see how to create your own keystore and configure SSL and HTTP/2 (I still haven't found the way to enable HTTP/2 with **OpenJDK**, ALPN throws exceptions).

#### tips

* The web apps are configured to run on a machine with IP 192.168.1.5. If you can't run with that IP change that and run the steps of
section **Wildfly SSL**

* In order to allow connections from different IPs than the one running the server 
(the only way your phone can access the applications), you must run your server with the command:
 
         $WILDFLY_HOME/bin/standalone.sh -b=0.0.0.0
 
 To get that if you are developing with Intellij you must add to your server configuration
         
         VM options: -Djboss.bind.address=0.0.0.0



[Wiki](https://github.com/votingsystem/votingsystem/wiki)
