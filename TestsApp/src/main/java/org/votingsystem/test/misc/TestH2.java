package org.votingsystem.test.misc;

import org.votingsystem.test.util.H2Utils;
import org.votingsystem.test.util.TestUtils;

import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TestH2 {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(TestH2.class, "./H2Databases");
        H2Utils.testBlob();
        //H2Utils.testPlainText()
        TestUtils.finish("OK");
    }
}

