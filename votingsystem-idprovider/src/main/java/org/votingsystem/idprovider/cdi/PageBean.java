package org.votingsystem.idprovider.cdi;

import org.votingsystem.util.Messages;

import javax.ejb.AccessTimeout;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Helper class to hold variables and methods needed by some web pages
 */
@Named("pageBean")
@SessionScoped
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
public class PageBean implements Serializable {

    private static final Logger log = Logger.getLogger(PageBean.class.getName());

    /**
     * Method that format a Date with the pattern yyyy-MM-dd HH:mm:ss
     *
     * @param date
     * @return the formatted date
     */
    public String formatDate(Date date) {
        if(date == null) return "";
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

    public String applicationCodeMsg(String applicationCodeStr) {
        return Messages.currentInstance().get(applicationCodeStr);
    }

    public String locale() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return request.getLocale().toString();
    }

    public String now() {
        return formatDate(new Date());
    }

}