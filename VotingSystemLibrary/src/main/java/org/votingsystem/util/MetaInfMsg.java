package org.votingsystem.util;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MetaInfMsg {

    public static String getOKMsg(String arg1, String ... args) {
        String result = arg1 + "_OK";
        if(args != null && args.length > 0) {
            for(String arg: args) result = result.concat("_" + arg);
        }
        return result;
    }

    public static String getErrorMsg(String arg1, String ... args) {
        String result = arg1 + "_ERROR";
        if(args != null && args.length > 0) {
            for(String arg: args) result = result.concat("_" + arg);
        }
        return result;
    }

    public static String getExceptionMsg(String arg1, Exception ex, String... args) {
        String result = arg1 + "_EXCEPTION_" + ex.getClass().getSimpleName();
        if(args != null && args.length > 0) {
            for(String arg: args) result = result.concat("_" + arg);
        }
        return result;
    }

}
