package org.votingsystem.test.misc;

import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.util.JSON;

import java.text.MessageFormat;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Test {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        log.info(" ============= " + MessageFormat.format(
                "requestAmount ''{0}'' exceeds bundle amount ''{1}''", "param0", "param1"));

    }


}
