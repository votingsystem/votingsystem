package org.votingsystem.test.misc;


import java.text.Normalizer;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Test {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        String string = "hidrégenoe";
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        log.info("111: "  + string);
        string = string.replaceAll("\\p{M}", "");
        log.info("222: "  + string);
    }


}
