package org.votingsystem.web.currency.filter;

import org.votingsystem.util.FileUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebFilter(urlPatterns = {"/*"}, asyncSupported=true)
public class FilterVS implements Filter {

    private static final Logger log = Logger.getLogger(FilterVS.class.getName());

    private ServletContext servletContext;
    @Inject ConfigVS config;
    @Inject CMSBean cmsBean;
    private String serverName;
    private String contextURL;
    private String webSocketURL;
    private String bundleBaseName;
    private String timeStampServerURL;
    private String nativeClientSHA1CheckSum;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
        contextURL = config.getContextURL();
        webSocketURL = config.getWebSocketURL();
        serverName = config.getServerName();
        timeStampServerURL = config.getTimeStampServerURL();
        bundleBaseName = config.getProperty("vs.bundleBaseName");
        servletContext.log("------- currency FilterVS initialized -------");
    }

    private String getNativeClientSHA1CheckSum(ServletRequest req) throws IOException, NoSuchAlgorithmException {
        if(nativeClientSHA1CheckSum != null) return nativeClientSHA1CheckSum;
        else {
            String nativeClientPath =  req.getServletContext().getRealPath("/tools/NativeClient.jar");
            File nativeClientFile = new File(nativeClientPath);
            if(nativeClientFile.exists()) {
                nativeClientSHA1CheckSum = FileUtils.getFileCheckSum(nativeClientPath);
            }
            return nativeClientSHA1CheckSum;
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        req.setAttribute("contextURL", contextURL);
        req.setAttribute("serverName", serverName);
        req.setAttribute("webSocketURL", webSocketURL);
        req.setAttribute("timeStampServerURL", timeStampServerURL);
        try {
            req.setAttribute("nativeClientSHA1CheckSum", getNativeClientSHA1CheckSum(req));
        } catch (NoSuchAlgorithmException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        log.info(((HttpServletRequest) req).getMethod() + " - " + ((HttpServletRequest) req).getRequestURI() +
                " - contentType: " + req.getContentType() + " - locale: " + req.getLocale());
        MessagesVS.setCurrentInstance(req.getLocale(), bundleBaseName);
        chain.doFilter(req, resp);
    }



    @Override
    public void destroy() {
        servletContext.log("------- FilterVS destroyed -------");
    }

}