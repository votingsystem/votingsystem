package org.sistemavotacion.herramientavalidacion;

import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.log4j.PropertyConfigurator;
import org.sistemavotacion.AppHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum ContextoHerramienta {
     
    INSTANCE;
    
    private static Logger logger = LoggerFactory.getLogger(ContextoHerramienta.class);
    
    public static class DEFAULTS {
        private static final String locale =  "es";
    } 
    
    private ResourceBundle resourceBundle;
    private AppHost appHost;
    
    private ContextoHerramienta () {
        try {
            Properties props = new Properties();
            props.load(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("log4jHerramienta.properties")); 
            resourceBundle = ResourceBundle.getBundle(
                    "herramientaValidacionMessages_" + DEFAULTS.locale);
            PropertyConfigurator.configure(props);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override public void run() {
                    shutdown();
                }
            });
        } catch (Exception ex) {
            LoggerFactory.getLogger(ContextoHerramienta.class).error(ex.getMessage(), ex);
        }

    }

    public void init(AppHost appHost) {
        org.sistemavotacion.Contexto.INSTANCE.init(appHost);
    }
            
    public void shutdown() {
        logger.debug("----------- shutdown ------------ ");
        org.sistemavotacion.Contexto.INSTANCE.shutdown();
    }

    public String getString(String key, Object... arguments) {
        String pattern = resourceBundle.getString(key);
        if(arguments.length > 0)
            return MessageFormat.format(pattern, arguments);
        else return resourceBundle.getString(key);
    }
    
}