package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.Payment;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.currency.ejb.ShopExampleBean;
import org.votingsystem.web.currency.util.TransactionRequest;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/shop")
public class ShopExampleResource {

    private static final Logger log = Logger.getLogger(ShopExampleResource.class.getSimpleName());

    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;
    @Inject ShopExampleBean shopExampleBean;

    //After user interaction we have the data of the service the user wants to buy, with that we create a TransactionRequest
    //and show the QR code with the URL of the transaction data to offer the user the possibility to check the order with the mobile.
    @Path("/") @GET
    public Object index(@QueryParam("uuid") String uuid, @Context ServletContext context,
                        @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        TransactionRequest transactionRequest = new TransactionRequest(TypeVS.PAYMENT_REQUEST, UserVS.Type.GROUP,
                "shop example payment - " + new Date(), "currency shop example", new BigDecimal(1), "EUR", TagVS.WILDTAG,
                new Date(),"ES0878788989450000000007", java.util.UUID.randomUUID().toString());
        transactionRequest.setPaymentOptions(Arrays.asList(Payment.SIGNED_TRANSACTION,
                Payment.ANONYMOUS_SIGNED_TRANSACTION, Payment.CASH_SEND));
        String shopSessionID = transactionRequest.getUUID().substring(0, 8);
        String paymentInfoServiceURL = config.getContextURL() + "/shop/" + shopSessionID;
        shopExampleBean.putTransactionRequest(shopSessionID, transactionRequest);
        req.setAttribute("paymentInfoServiceURL", paymentInfoServiceURL);
        req.setAttribute("shopSessionID", shopSessionID);
        req.setAttribute("transactionRequest", transactionRequest);
        context.getRequestDispatcher("/jsf/shopExample/index.jsp").forward(req, resp);
        return Response.ok().build();
    }

    //Called with async Javascript from the web page that shows the QR code, we store an AsyncContext in order to notify
    //the web client of any change in the requested transaction state
    @Path("/listenTransactionChanges/{shopSessionID}") //old_url -> /shop/listenTransactionChanges/$shopSessionID
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Object listenTransactionChanges(@PathParam("shopSessionID") String shopSessionID,
           @Context HttpServletRequest req, @Suspended AsyncResponse response) throws Exception {
        /*final AsyncContext ctx = startAsync()
        ctx.addListener(new AsyncListener() {
            @Override public void onComplete(AsyncEvent event) throws IOException {}
            @Override public void onTimeout(AsyncEvent event) throws IOException {
                log.debug("On timeout");
                ctx.response.getWriter().write("session expired")
                ctx.response.getWriter().flush()
            }
            @Override public void onError(AsyncEvent event) throws IOException { }
            @Override public void onStartAsync(AsyncEvent event) throws IOException { }
        });
        ctx.setTimeout(SESSION_TIMEOUT);
        shopExampleService.bindContext(params.shopSessionID, ctx)
        ctx.start { }*/
        return null;
    }

    //Called from the mobile after reading the QR code. The mobile fetch the transaction data
    @Path("/{uuid}") //old_url -> "/shop/$uuid"
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Object paymentInfo(@PathParam("uuid") String uuid, @Context HttpServletRequest req) throws Exception {
        TransactionRequest transactionRequest = shopExampleBean.getTransactionRequest(uuid);
        if(transactionRequest != null) {
            return transactionRequest.getDataMap();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("sessionExpiredMsg").build();
        }
    }

    //Called from the mobile after the payment. The mobile sends the signed receipt with the completed transaction data.
    //here you must check with the tools of your choice the validity of the receipt. The receipt is an standard
    //S/MIME document (http://en.wikipedia.org/wiki/S/MIME)
    @Path("/{uuid}/payment") @POST @Consumes(MediaTypeVS.JSON_SIGNED) // old_url -> /shop/$uuid/payment
    public Object payment(@PathParam("uuid") String uuid, MessageSMIME messageSMIME,
              @Context HttpServletRequest req) throws Exception {
        shopExampleBean.sendResponse(uuid, messageSMIME.getSMIME());
        return Response.ok().entity("valid receipt").build();
    }

}
