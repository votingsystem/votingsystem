package org.votingsystem.web.timestamp.filter;

import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@WebFilter("/*")
public class FilterVS implements Filter {

    private java.util.logging.Logger log = java.util.logging.Logger.getLogger(FilterVS.class.getName());

    @Inject ConfigVS config;
    private String bundleBaseName;
    private ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // It is common to save a reference to the ServletContext here in case it is needed in the destroy() call.
        servletContext = filterConfig.getServletContext();
        bundleBaseName = config.getProperty("vs.bundleBaseName");
        servletContext.log("------- FilterVS initialized -------");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain)
            throws IOException, ServletException {
        MessagesVS.setCurrentInstance(req.getLocale(), bundleBaseName);
        log.info(((HttpServletRequest) req).getMethod() + " - " + ((HttpServletRequest) req).getRequestURI() +
                " - contentType: " + req.getContentType() + " - locale: " + req.getLocale());
        filterChain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
        servletContext.log("------- FilterVS destroyed -------");
    }

}