package org.votingsystem.web.currency.cdi;

import javax.ejb.AccessTimeout;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Named("spa")
@SessionScoped
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
public class SPABean implements Serializable {

    private static final Logger log = Logger.getLogger(SPABean.class.getSimpleName());

    public String formatDate(Date date) {
        if(date == null) return "";
        DateFormat formatter = new SimpleDateFormat("/yyyy/MM/dd");
        return formatter.format(date);
    }

    public String now() {
        return formatDate(new Date());
    }
}