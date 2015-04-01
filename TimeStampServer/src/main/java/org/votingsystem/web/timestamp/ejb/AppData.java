package org.votingsystem.web.timestamp.ejb;

import org.votingsystem.util.EnvironmentVS;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Named;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Startup
public class AppData {

    private static final Logger log = Logger.getLogger(AppData.class.getSimpleName());

    private static Map<String, ResourceBundle> bundleMap = new HashMap<>();

    private String contextURL;
    private EnvironmentVS mode;
    private Properties props;

    public AppData() {
        try {
            String resourceFile = null;
            //String fileName = System.getProperty("jboss.server.config.dir") + "/my.properties";
            log.info("environment: " + System.getProperty("vs.environment"));
            if(System.getProperty("vs.environment") != null) {
                mode = EnvironmentVS.valueOf(System.getProperty("vs.environment"));
            } else mode = EnvironmentVS.DEVELOPMENT;
            switch (mode) {
                case DEVELOPMENT:
                    resourceFile = "TimeStampServer_DEVELOPMENT.properties";
                    break;
                case PRODUCTION:
                    resourceFile = "TimeStampServer_PRODUCTION.properties";
                    break;
            }
            props = new Properties();
            URL res = Thread.currentThread().getContextClassLoader().getResource(resourceFile);
            props.load(res.openStream());
            contextURL = (String) props.get("vs.contextURL");
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }
    
    public String get(String key, Locale locale, String... arguments) {
        ResourceBundle bundle = null;
        if((bundle = bundleMap.get(locale.getCountry())) == null) {
            bundle = ResourceBundle.getBundle("org.votingsystem.web.timestamp.messages", locale);
            if(bundle == null) {
                log.info("Can't find bundle for locale " + locale.toString());
                return "---" + key + "---";
            }
            bundleMap.put(locale.getCountry(), bundle);
        }
        try {
            String pattern = bundle.getString(key);
            if(arguments.length > 0) return new String(MessageFormat.format(pattern, arguments).getBytes(ISO_8859_1), UTF_8);
            else return new String(pattern.getBytes(ISO_8859_1), UTF_8);
        } catch(Exception ex) {
            log.info("missing key: " + key + " - country: " + locale.getCountry());
            return "---" + key + "---";
        }
    }


    public EnvironmentVS getMode() {
        return mode;
    }

    public String getContextURL() {
        return contextURL;
    }

}
