package org.votingsystem.web.accesscontrol.filter;

import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.EJB;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Logger;

@WebFilter("/*")
public class FilterVS implements Filter {

    private static final Logger log = Logger.getLogger(FilterVS.class.getName());

    @EJB ConfigVS config;
    private ServletContext servletContext;
    private String serverName;
    private String contextURL;
    private String webSocketURL;
    private String bundleBaseName;
    private String timeStampServerURL;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
        contextURL = config.getContextURL();
        webSocketURL = config.getWebSocketURL();
        serverName = config.getServerName();
        timeStampServerURL = config.getTimeStampServerURL();
        bundleBaseName = config.getProperty("vs.bundleBaseName");
        servletContext.log("------- AccessControl FilterVS initialized -------");
    }


    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        String requestMethod = ((HttpServletRequest)req).getMethod();
        req.setAttribute("contextURL", contextURL);
        req.setAttribute("webSocketURL", webSocketURL);
        req.setAttribute("serverName", serverName);
        req.setAttribute("timeStampServerURL", timeStampServerURL);
        MessagesVS.setCurrentInstance(req.getLocale(), bundleBaseName);
        chain.doFilter(req, resp);
        log.info(requestMethod + " - " + ((HttpServletRequest)req).getRequestURI() +
                " - contentType: " + req.getContentType() + " - locale: " + req.getLocale());
    }

    @Override
    public void destroy() {
        servletContext.log("------- FilterVS destroyed -------");
    }

}