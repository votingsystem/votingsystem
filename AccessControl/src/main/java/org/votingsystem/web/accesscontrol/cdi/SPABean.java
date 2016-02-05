package org.votingsystem.web.accesscontrol.cdi;

import javax.ejb.AccessTimeout;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Named("spa")
@SessionScoped
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
public class SPABean implements Serializable {

    private static final Logger log = Logger.getLogger(SPABean.class.getName());

    public String getUUID() {
        return UUID.randomUUID().toString();
    }

    public String formatDate(Date date) {
        if(date == null) return "";
        DateFormat formatter = new SimpleDateFormat("/yyyy/MM/dd");
        return formatter.format(date);
    }

    public String locale() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return request.getLocale().toString();
    }

    public String now() {
        return formatDate(new Date());
    }

}