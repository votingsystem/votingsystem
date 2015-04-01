package org.votingsystem.web.currency.jaxrs.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.util.JSON;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class CurrencyAccountWriter implements MessageBodyWriter<CurrencyAccount> {

    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return CurrencyAccount.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(CurrencyAccount t, Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        // As of JAX-RS 2.0, the method has been deprecated and the value returned by the method is ignored by a JAX-RS runtime.
        // All MessageBodyWriter implementations are advised to return -1 from the method.
        return -1;
    }

    @Override
    public void writeTo(CurrencyAccount account, Class<?> type, Type type1, Annotation[] antns, MediaType mt,
                        MultivaluedMap<String, Object> mm, OutputStream out) throws IOException, WebApplicationException {
        ObjectMapper mapper = JSON.getInstance().getMapper();
        Map<String,Object> accountData = new HashMap<String, Object>();
        accountData.put("id", account.getId());
        accountData.put("currency", account.getCurrencyCode());
        accountData.put("IBAN", account.getIBAN());
        accountData.put("amount", account.getBalance());
        accountData.put("tag", account.getTag().toMap());
        mapper.writeValue(out, accountData);

    }
}
