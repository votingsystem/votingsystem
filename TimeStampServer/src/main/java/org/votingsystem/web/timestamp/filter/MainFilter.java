package org.votingsystem.web.timestamp.filter;

import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class MainFilter implements Filter {

    private java.util.logging.Logger log = java.util.logging.Logger.getLogger(MainFilter.class.getName());

    @Inject ConfigVS config;
    private String bundleBaseName;
    private ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // It is common to save a reference to the ServletContext here in case it is needed in the destroy() call.
        servletContext = filterConfig.getServletContext();
        bundleBaseName = config.getProperty("vs.bundleBaseName");
        servletContext.log("------- MainFilter initialized -------");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain)
            throws IOException, ServletException {
        MessagesVS.setCurrentInstance(req.getLocale(), bundleBaseName);
        log.info(((HttpServletRequest) req).getMethod() + " - " + ((HttpServletRequest) req).getRequestURI() +
                " - contentType: " + req.getContentType() + " - locale: " + req.getLocale());

        ((HttpServletResponse)resp).addHeader("Access-Control-Allow-Origin", "*");
        ((HttpServletResponse)resp).addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ((HttpServletResponse)resp).addHeader("Access-Control-Max-Age", "-1");
        ((HttpServletResponse)resp).addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");

        filterChain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
        servletContext.log("------- MainFilter destroyed -------");
    }

}