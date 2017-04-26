package org.votingsystem.client.util;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    public static String truncateLog(String message) {
        if(message == null)
            return null;
        else
            return message.length() > 300 ? message.substring(0, 300) + "..." : message;
    }

}