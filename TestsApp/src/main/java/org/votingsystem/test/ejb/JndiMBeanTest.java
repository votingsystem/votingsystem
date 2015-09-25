package org.votingsystem.test.ejb;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * http://karaf.apache.org/manual/latest/users-guide/monitoring.html
 * The JMX JndiMBean provides the JNDI names, and the operations to manipulate the JNDI service.
 */
public class JndiMBeanTest {

    private static final Logger log = Logger.getLogger(JndiMBeanTest.class.getName());

    public static void main(String[] args) throws Exception {
        //The following example shows a simple JMX client stopping Apache Karaf remotely via the JMX layer:
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root");
        Map env = new HashMap<>();
        String[] creds = {"karaf", "karaf"};
        env.put(JMXConnector.CREDENTIALS, creds);
        JMXConnector connector = JMXConnectorFactory.connect(url, env);
        MBeanServerConnection mbeanServerConn = connector.getMBeanServerConnection();
        log.info("MBeanCount: " + mbeanServerConn.getMBeanCount());
        List<String> domains = Arrays.asList(mbeanServerConn.getDomains());
        domains.stream().forEach(d -> log.info("domain:" + d));
        ObjectName systemMBean = new ObjectName("org.apache.karaf:type=system,name=karaf-root");
        Object result = mbeanServerConn.invoke(systemMBean, "halt", null, null);
        connector.close();
    }

}
