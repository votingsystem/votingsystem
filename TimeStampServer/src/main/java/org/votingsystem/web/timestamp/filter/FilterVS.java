package org.votingsystem.web.timestamp.filter;

import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

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
        RequestVSWrapper requestWrapper = new RequestVSWrapper((HttpServletRequest) req);
        MessagesVS.setCurrentInstance(requestWrapper.getLocale(), bundleBaseName);
        log.info(((HttpServletRequest) req).getMethod() + " - " + ((HttpServletRequest) req).getRequestURI() +
                " - contentType: " + req.getContentType() + " - locale: " + req.getLocale());
        filterChain.doFilter(requestWrapper, resp);
    }

    public class RequestVSWrapper extends HttpServletRequestWrapper {

        public RequestVSWrapper(HttpServletRequest request) {
            super(request);
        }

        //hack to solve JavaFX webkit Accept-Language header problem
        @Override public Locale getLocale() {
            if(getParameterMap().get("locale") != null) return Locale.forLanguageTag(getParameterMap().get("locale")[0]);
            else return super.getLocale();
        }

    }

    @Override
    public void destroy() {
        servletContext.log("------- FilterVS destroyed -------");
    }

}