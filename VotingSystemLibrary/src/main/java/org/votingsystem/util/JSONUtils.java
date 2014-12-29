package org.votingsystem.util;

import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class JSONUtils {

    public static boolean isEmpty(String key, JSONObject jsonObject) {
        return !(jsonObject.has(key) && !JSONNull.getInstance().equals(jsonObject.get(key)));
    }
}
