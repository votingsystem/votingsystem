package org.votingsystem.web.currency.jaxrs.provider;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.logging.Logger;


@Provider
public class ResponseInterceptor implements WriterInterceptor{

    private static final Logger log = Logger.getLogger(ResponseInterceptor.class.getName());

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        context.getHeaders().add("vs-msg", "hello from voting system");
        context.proceed();
    }
}
