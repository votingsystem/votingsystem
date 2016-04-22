package org.votingsystem.web.controlcenter.filter;

import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Logger;

@WebFilter("/*")
public class FilterVS implements Filter {

    private static final Logger log = Logger.getLogger(FilterVS.class.getName());

    private ServletContext servletContext;
    @Inject ConfigVS config;
    private String serverName;
    private String contextURL;
    private String bundleBaseName;
    private String timeStampServerURL;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
        contextURL = config.getContextURL();
        serverName = config.getServerName();
        timeStampServerURL = config.getTimeStampServerURL();
        bundleBaseName = config.getProperty("vs.bundleBaseName");
        servletContext.log("=------- ControlCenter FilterVS initialized -------");
    }


    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        String requestMethod = ((HttpServletRequest)req).getMethod();
        req.setAttribute("contextURL", contextURL);
        req.setAttribute("serverName", serverName);
        req.setAttribute("timeStampServerURL", timeStampServerURL);
        MessagesVS.setCurrentInstance(req.getLocale(), bundleBaseName);
        log.info(requestMethod + " - " + ((HttpServletRequest)req).getRequestURI() +
                " - contentType: " + req.getContentType() + " - locale: " + req.getLocale());
        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
        servletContext.log("------- FilterVS destroyed -------");
    }

}