package org.votingsystem.web.util;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


public class EmailTemplateWrapper extends HttpServletResponseWrapper {

    public EmailTemplateWrapper(HttpServletResponse response) {
        super(response);
    }

    private final StringWriter sw = new StringWriter();

    @Override public PrintWriter getWriter() throws IOException {
        return new PrintWriter(sw);
    }

    @Override public String toString() {
        return sw.toString();
    }
}
