package org.votingsystem.client.util;

import org.votingsystem.model.TagVS;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    private static Logger log = Logger.getLogger(MsgUtils.class.getName());

    public static String truncateLog(String message) {
        if(message == null) return null;
        else return message.length() > 300 ? message.substring(0, 300) + "..." : message;
    }

    public static String getTagDescription(String tagName) {
        if(TagVS.WILDTAG.equals(tagName)) return ContextVS.getMessage("wildTagLbl").toLowerCase();
        else return tagName.toLowerCase();

    }

}