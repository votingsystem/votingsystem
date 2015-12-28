package org.votingsystem.test.misc;

import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class PropertiesToLowerCase {

    private static final Logger log = Logger.getLogger(PropertiesToLowerCase.class.getName());

    private static String sourceFilePath;
    private static String destFilePath;


    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        Properties props = new Properties();
        byte[] propertBytes = FileUtils.getBytesFromStream(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(sourceFilePath));
        //Use a Reader when working with strings. InputStreams are really meant for binary data.
        props.load( new StringReader(new String(propertBytes, "UTF-8")));
        Enumeration properties = props.propertyNames();
        while(properties.hasMoreElements()) {
            String propertyName = (String) properties.nextElement();
            props.put(propertyName, ((String)props.get(propertyName)).toLowerCase());
        }
        File result = new File(destFilePath);
        log.info("AbsolutePath: " + result.getAbsolutePath());
        result.createNewFile();
        props.store(new FileOutputStream(result), null);
    }


}
