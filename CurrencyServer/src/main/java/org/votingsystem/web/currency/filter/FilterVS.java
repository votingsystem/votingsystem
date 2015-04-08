package org.votingsystem.web.currency.filter;

import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@WebFilter("/*")
public class FilterVS implements Filter {

    private static final Logger log = Logger.getLogger(FilterVS.class.getSimpleName());

    private ServletContext servletContext;
    @Inject ConfigVS config;
    @Inject MessagesBean messages;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // It is common to save a reference to the ServletContext here in case it is needed in the destroy() call.
        servletContext = filterConfig.getServletContext();
        // To see this log message at run time, check out the terminal window where you started WildFly.
        servletContext.log("------- currency FilterVS initialized -------");
    }


    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        String requestMethod = ((HttpServletRequest)req).getMethod();
        messages.setLocale(req.getLocale());
        req.setAttribute("request", req);
        req.setAttribute("resourceURL", ((HttpServletRequest) req).getContextPath() + "/resources/bower_components");
        req.setAttribute("elementURL", ((HttpServletRequest) req).getContextPath() + "/jsf");
        req.setAttribute("restURL", ((HttpServletRequest) req).getContextPath() + "/rest");
        req.setAttribute("contextURL", ((HttpServletRequest) req).getContextPath());
        req.setAttribute("serverName", config.getServerName());
        req.setAttribute("timeStampServerURL", config.getTimeStampServerURL());
        if(!"HEAD".equals(requestMethod)) {
            RequestVSWrapper requestWrapper = new RequestVSWrapper((HttpServletRequest) req);
            log.info(requestMethod + " - " + ((HttpServletRequest)req).getRequestURI() +
                    " - contentType: " + req.getContentType() + " - locale: " + req.getLocale());
            chain.doFilter(requestWrapper, resp);
        } else chain.doFilter(req, resp);
    }

    public class RequestVSWrapper extends HttpServletRequestWrapper {

        private ContentTypeVS contentTypeVS;

        public RequestVSWrapper(HttpServletRequest request) {
            super(request);
            if(request.getCookies() != null) {
                for(Cookie cookie: request.getCookies()) {
                    headerMap.put(cookie.getName(), cookie.getValue());
                }
            }
            /*Enumeration<String> headers = request.getHeaderNames();
            while(headers.hasMoreElements()) {
                String header = headers.nextElement();
                log.info("header - " + header + " - value:" + request.getHeader(header));
            }*/
            contentTypeVS = ContentTypeVS.getByName(request.getContentType());
            /*if(ContentTypeVS.JSON == contentTypeVS) {

            }*/
        }

        private Map<String, String> headerMap = new HashMap<String, String>();

        public void addHeader(String name, String value) {
            headerMap.put(name, value);
        }

        public ContentTypeVS getContentTypeVS() {
            return contentTypeVS;
        }

        public void setContentTypeVS(ContentTypeVS contentTypeVS) {
            this.contentTypeVS = contentTypeVS;
        }

        @Override public String getHeader(String name) {
            String headerValue = super.getHeader(name);
            if (headerMap.containsKey(name)) {
                headerValue = headerMap.get(name);
            }
            return headerValue;
        }

        @Override public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            for (String name : headerMap.keySet()) {
                names.add(name);
            }
            return Collections.enumeration(names);
        }

        @Override public Enumeration<String> getHeaders(String name) {
            List<String> values = Collections.list(super.getHeaders(name));
            if (headerMap.containsKey(name)) {
                values.add(headerMap.get(name));
            }
            return Collections.enumeration(values);
        }

    }

    @Override
    public void destroy() {
        servletContext.log("------- FilterVS destroyed -------");
        servletContext = null;
    }

}