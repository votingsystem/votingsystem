package org.votingsystem.test.misc;

import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.util.JSON;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Test {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setUUID(UUID.randomUUID().toString());
        String str = JSON.getMapper().writeValueAsString(messageDto);
        log.info("===" + str);

        messageDto = JSON.getMapper().readValue(str, SocketMessageDto.class);

    }


}
