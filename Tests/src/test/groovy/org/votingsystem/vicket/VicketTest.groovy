package org.votingsystem.vicket

import org.apache.log4j.Logger
import org.junit.Test
import org.votingsystem.model.ContextVS
import org.votingsystem.test.util.SignatureVSService

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class VicketTest {

    private static Logger logger = Logger.getLogger(VicketTest.class);

    //static { ContextVS.getInstance().init();  }

    @Test
    void request(){
        //logger.debug("VicketTest request")
        //InputStream input =  Thread.currentThread().getContextClassLoader().getResourceAsStream("transactionvs.json")
        //byte[] fileBytes = FileUtils.getBytesFromInputStream(input)
        //File file = FileUtils.getFileFromBytes(fileBytes)
        //println(file.text)
        assert true
    }
}
