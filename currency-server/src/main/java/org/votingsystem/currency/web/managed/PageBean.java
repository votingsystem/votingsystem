package org.votingsystem.currency.web.managed;

import com.google.zxing.WriterException;
import org.votingsystem.ejb.Config;
import org.votingsystem.qr.QRUtils;
import org.votingsystem.util.Constants;

import javax.ejb.AccessTimeout;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Named("pageBean")
@SessionScoped
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
public class PageBean implements Serializable {

    private static final Logger log = Logger.getLogger(PageBean.class.getName());

    @Inject private Config config;

    public String getUUID() {
        return UUID.randomUUID().toString();
    }

    public String getEntityId() {
        return config.getEntityId();
    }

    public String getTimestampServiceURL() {
        return config.getTimestampServiceURL();
    }

    public String formatDate(Date date) {
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

    public String getAccessCodeQR() throws WriterException, IOException {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String qrCodeURL = req.getContextPath() + "/api/qr?cht=qr&chs=200x200&chl=" +
                QRUtils.SYSTEM_ENTITY_KEY + "=" + config.getEntityId() + ";" +
                QRUtils.OPERATION_KEY + "=" + QRUtils.GET_BROWSER_CERTIFICATE + ";" +
                QRUtils.UUID_KEY + "=" + req.getSession().getAttribute(Constants.USER_UUID) + ";";
        return qrCodeURL;
    }
}