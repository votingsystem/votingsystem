package org.votingsystem.serviceprovider.http;

import org.votingsystem.util.Constants;
import org.votingsystem.util.Messages;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Application main HTTP filter
 *
 * @author votingsystem
 */
@WebFilter("/*")
public class MainFilter implements Filter {

    private java.util.logging.Logger log = java.util.logging.Logger.getLogger(MainFilter.class.getName());

    private ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // It is common to save a reference to the ServletContext here in case it is needed in the destroy() call.
        servletContext = filterConfig.getServletContext();
        servletContext.log(String.format("%s init", this.getClass().getSimpleName()));
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws IOException, ServletException {
        Messages.setCurrentInstance(req.getLocale(), Constants.BUNDLE_BASE_NAME);
        String requestURI = ((HttpServletRequest) req).getRequestURI();
        if(!requestURI.contains("/res/")) {
            log.info(((HttpServletRequest) req).getMethod() + " - " + ((HttpServletRequest) req).getRequestURI() +
                    " - contentType: " + req.getContentType() + " - locale: " + req.getLocale());
            if(((HttpServletRequest) req).getRequestURI().contains("publish.xhtml")) {
                HttpServletResponse response = (HttpServletResponse) res;
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
                response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
                response.setDateHeader("Expires", 0); // Proxies.
            }
        }

        ((HttpServletResponse)res).addHeader("Access-Control-Allow-Origin", "*");
        ((HttpServletResponse)res).addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ((HttpServletResponse)res).addHeader("Access-Control-Max-Age", "-1");
        ((HttpServletResponse)res).addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");

        filterChain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        servletContext.log(String.format("%s destroy", this.getClass().getSimpleName()));
    }

}