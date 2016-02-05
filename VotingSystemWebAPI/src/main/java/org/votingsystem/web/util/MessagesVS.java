package org.votingsystem.web.util;

import org.votingsystem.model.TagVS;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagesVS {

    private static final Logger log = Logger.getLogger(MessagesVS.class.getName());

    private ResourceBundle bundle;
    private Locale locale;

    private static ThreadLocal<MessagesVS> instance = new ThreadLocal() {
        protected MessagesVS initialValue() {
            return null;
        }
    };

    public MessagesVS(Locale locale, String bundleBaseName) {
        try {
            this.locale = locale;
            this.bundle = ResourceBundle.getBundle(bundleBaseName, locale);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "resource not found for locale: " + locale);
            this.bundle = ResourceBundle.getBundle(bundleBaseName, new Locale("es"));
        }
    }

    public String get(String key, Object... arguments) {
        try {
            String pattern = bundle.getString(key);
            if(arguments.length > 0) return MessageFormat.format(pattern, arguments);
            else return pattern;
        } catch (Exception ex) {
            return "-- " + key + " --";
        }
    }

    public Locale getLocale() {
        return locale;
    }

    public static MessagesVS getCurrentInstance() {
        return (MessagesVS)instance.get();
    }

    protected static void setCurrentInstance(MessagesVS messagesVS) {
        if(messagesVS == null) {
            instance.remove();
        } else {
            instance.set(messagesVS);
        }
    }

    public static void setCurrentInstance(Locale locale, String bundleBaseName) {
        setCurrentInstance(new MessagesVS(locale, bundleBaseName));
    }

    public String getTagMessage(String tag) {
        if(TagVS.WILDTAG.equals(tag)) return get("wildTagMsg");
        else return get("tagMsg", tag);
    }
}
