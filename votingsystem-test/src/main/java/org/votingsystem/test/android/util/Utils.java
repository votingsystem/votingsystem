package org.votingsystem.test.android.util;

import java.util.Locale;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static String localeToLanguageTag () {
        return Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
    }

}
