package org.votingsystem.test.misc;

import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.util.JSON;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Test {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        Map testMap = new HashMap<>();
        testMap.put(new Long(1), "Hello");
        log.info(" ============= constains: " + testMap.containsKey(1L));
    }


}
