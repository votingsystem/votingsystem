package org.votingsystem.util;

import org.votingsystem.model.currency.Tag;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 * Class to obtain i18n messages.
 */
public class Messages {

    private static final Logger log = Logger.getLogger(Messages.class.getName());

    private ResourceBundle bundle;
    private Locale locale;

    private static ThreadLocal<Messages> instance = new ThreadLocal() {
        protected Messages initialValue() {
            return null;
        }
    };

    public Messages(Locale locale, String bundleBaseName) {
        try {
            this.locale = locale;
            this.bundle = ResourceBundle.getBundle(bundleBaseName, locale);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "resource not found for locale: " + locale);
            this.bundle = ResourceBundle.getBundle(bundleBaseName, new Locale("es"));
        }
    }

    public String get(String key, Object... arguments) {
        String pattern = null;
        try {
            pattern = bundle.getString(key);
            if(arguments.length > 0) return MessageFormat.format(pattern, arguments);
            else return pattern;
        } catch (Exception ex) {
            if(pattern != null) return pattern;
            else return "- " + key + " -";
        }
    }

    public Locale getLocale() {
        return locale;
    }

    public static Messages currentInstance() {
        if(instance.get() == null) {
            log.severe("Messages not initialized - fetching messages for default languaje: " +
                    Locale.getDefault().getDisplayLanguage());
            setCurrentInstance(Locale.getDefault(), Constants.BUNDLE_BASE_NAME);
        }
        return instance.get();
    }

    protected static void setCurrentInstance(Messages messages) {
        if(messages == null) {
            instance.remove();
        } else {
            instance.set(messages);
        }
    }

    public static void setCurrentInstance(Locale locale, String bundleBaseName) {
        setCurrentInstance(new Messages(locale, bundleBaseName));
    }

    public String getTagMessage(String tag) {
        if(Tag.WILDTAG.equals(tag)) return get("wildTagMsg");
        else return get("tagMsg", tag);
    }

}