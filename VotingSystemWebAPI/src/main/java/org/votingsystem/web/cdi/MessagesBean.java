package org.votingsystem.web.cdi;

import javax.enterprise.context.RequestScoped;
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

    public MessagesBean() { }

    public void setLocale(Locale locale) {
        try {
            this.bundle = ResourceBundle.getBundle("org.votingsystem.web.controlcenter.messages", locale);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "resource not found for locale: " + locale);
            this.bundle = ResourceBundle.getBundle("org.votingsystem.web.controlcenter.messages", new Locale("es"));
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