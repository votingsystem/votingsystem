package org.votingsystem.web.cdi;

import org.votingsystem.model.TagVS;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@RequestScoped
@Named(value="messages")
public class MessagesBean {

    private static final Logger log = Logger.getLogger(MessagesBean.class.getSimpleName());

    private ResourceBundle bundle;
    private String bundleBaseName;
    @Inject ConfigVS config;

    public MessagesBean() { }

    @PostConstruct
    public void initialize() {
        bundleBaseName = config.getProperty("vs.bundleBaseName");
    }

    public String getTagMessage(String tag) {
        if(TagVS.WILDTAG.equals(tag)) return get("wildTagMsg");
        else return get("tagMsg", tag);
    }

    public void setLocale(Locale locale) {
        try {
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

}