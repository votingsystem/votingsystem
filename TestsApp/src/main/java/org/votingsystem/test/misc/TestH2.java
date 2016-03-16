package org.votingsystem.test.misc;

import org.votingsystem.test.util.H2Utils;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TestH2 {

    private static Logger log =  Logger.getLogger(TestH2.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        H2Utils.testBlob();
        //H2Utils.testPlainText()
        System.exit(0);
    }
}

