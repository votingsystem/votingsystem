package org.votingsystem.web.ejb;

import org.votingsystem.model.TagVS;
import org.votingsystem.web.cdi.ConfigVS;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateful
public class MessagesBean {

    private static final Logger log = Logger.getLogger(MessagesBean.class.getSimpleName());

    private ResourceBundle bundle;
    private String bundleBaseName;
    private Locale locale;
    @Inject ConfigVS config;

    public MessagesBean() { }

    @PostConstruct
    public void initialize() {
        try {
            bundleBaseName = config.getProperty("vs.bundleBaseName");
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public String getTagMessage(String tag) {
        if(TagVS.WILDTAG.equals(tag)) return get("wildTagMsg");
        else return get("tagMsg", tag);
    }

    public void setLocale(Locale locale) {
        try {
            this.bundle = ResourceBundle.getBundle(bundleBaseName, locale);
            this.locale = locale;
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
}