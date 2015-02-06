package org.votingsystem.util;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContentTypeVS;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RequestVSFilter implements Filter {

    private static Logger log = Logger.getLogger(RequestVSFilter.class);

    @Override public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        RequestVSWrapper requestWrapper = new RequestVSWrapper((HttpServletRequest) servletRequest);
        filterChain.doFilter(requestWrapper, servletResponse);
    }

    @Override public void destroy() { }

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
                log.debug("header - " + header + " - value:" + request.getHeader(header));
            }*/
            contentTypeVS = ContentTypeVS.getByName(request.getContentType());
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

}
