package org.votingsystem.web.accesscontrol.cdi;

import javax.ejb.AccessTimeout;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Named("spa")
@SessionScoped
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
public class SPABean implements Serializable {

    private static final Logger log = Logger.getLogger(SPABean.class.getSimpleName());

    private static final List<String> availableLocales = Arrays.asList("es");

    public String formatDate(Date date) {
        if(date == null) return "";
        DateFormat formatter = new SimpleDateFormat("/yyyy/MM/dd");
        return formatter.format(date);
    }

    public String language() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if(availableLocales.contains(request.getLocale().getCountry().toLowerCase())) return request.getLocale().getCountry().toLowerCase();
        else return "";
    }

    public String now() {
        return formatDate(new Date());
    }

}