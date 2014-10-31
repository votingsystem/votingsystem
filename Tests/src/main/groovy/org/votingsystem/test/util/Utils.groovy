package org.votingsystem.test.util

import org.apache.log4j.PropertyConfigurator
import org.votingsystem.util.FileUtils

propertyValuesToLowerCase("clientToolMessages_es.properties", "clientToolMessages_es.properties_out.properties")
System.exit(0)

private void propertyValuesToLowerCase(String sourceFilePath, String destFilePath) {
    Properties props = new Properties();
    byte[] propertBytes = FileUtils.getBytesFromInputStream(
            Thread.currentThread().getContextClassLoader().getResourceAsStream(sourceFilePath))
    //Use a Reader when working with strings. InputStreams are really meant for binary data.
    props.load( new StringReader(new String(propertBytes, "UTF-8")));
    Enumeration properties = props.propertyNames()
    while(properties.hasMoreElements()) {
        String propertyName = properties.nextElement()
        props.put(propertyName, ((String)props.get(propertyName)).toLowerCase())
    }
    File result = new File(destFilePath);
    println(result.absolutePath)
    result.createNewFile()
    props.store(new FileOutputStream(result), null)
}

