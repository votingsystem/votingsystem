package org.votingsystem.test.util

import org.votingsystem.util.FileUtils

/**
 * Created by jgzornoza on 31/10/14.
 */
class Utils {

    public static void propertyValuesToLowerCase(String sourceFilePath, String destFilePath) {
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

}
