package org.votingsystem.web.cdi;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.context.FacesContext;
import java.util.PropertyResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Dependent
public class Resources {

    @Produces
    public PropertyResourceBundle getBundle() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{msg}", PropertyResourceBundle.class);
    }

    @Produces
    public Logger produceLog(InjectionPoint injectionPoint) {
        Logger log = Logger.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINER);
        log.addHandler(handler);
        return log;
    }

}
