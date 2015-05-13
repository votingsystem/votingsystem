package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.web.currency.ejb.ShopExampleBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/shop")
public class ShopExampleResource {

    private static final Logger log = Logger.getLogger(ShopExampleResource.class.getName());

    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;
    @Inject ShopExampleBean shopExampleBean;

    //After user interaction we have the data of the service the user wants to buy, with that we create a TransactionRequest
    //and show the QR code with the URL of the transaction data to offer the user the possibility to check the order with the mobile.
    @Path("/") @GET
    public Object index(@QueryParam("uuid") String uuid, @Context ServletContext context,
                        @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        TransactionVSDto dto = TransactionVSDto.CURRENCY_SEND_REQUEST("currency shop example", UserVS.Type.GROUP,
                new BigDecimal(1), "EUR", "ES0878788989450000000007", "shop example payment - " + new Date(), TagVS.WILDTAG);
        dto.setPaymentOptions(Arrays.asList(TransactionVS.Type.FROM_USERVS,
                TransactionVS.Type.CURRENCY_SEND, TransactionVS.Type.CURRENCY_CHANGE));
        String shopSessionID = dto.getUUID().substring(0, 8);
        String paymentInfoServiceURL = config.getRestURL() + "/shop/" + shopSessionID;
        shopExampleBean.putTransactionRequest(shopSessionID, dto);
        req.setAttribute("paymentInfoServiceURL", paymentInfoServiceURL);
        req.setAttribute("shopSessionID", shopSessionID);
        req.setAttribute("transactionRequest", dto);
        context.getRequestDispatcher("/shopExample/index.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    //Called with async Javascript from the web page that shows the QR code, we store an AsyncContext in order to notify
    //the web client of any change in the requested transaction state
    @Path("/listenTransactionChanges/{shopSessionID}") //old_url -> /shop/listenTransactionChanges/$shopSessionID
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response listenTransactionChanges(@PathParam("shopSessionID") String shopSessionID,
           @Context HttpServletRequest req, @Suspended AsyncResponse asyncResponse) throws Exception {
        asyncResponse.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse asyncResponse) {
                asyncResponse.resume(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("Operation time out.").build());
            }
        });
        asyncResponse.setTimeout(180, TimeUnit.SECONDS);
        shopExampleBean.bindContext(shopSessionID, asyncResponse);
        return Response.ok().entity("AsyncResponse binded").build();
    }

    //Called from the mobile after reading the QR code. The mobile fetch the transaction data
    @Path("/{uuid}")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response paymentInfo(@PathParam("uuid") String uuid, @Context HttpServletRequest req) throws Exception {
        TransactionVSDto dto = shopExampleBean.getTransactionRequest(uuid);
        if(dto != null) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("sessionExpiredMsg").build();
        }
    }

    //Called from the mobile after the payment. The mobile sends the signed receipt with the completed transaction data.
    //here you must check with the tools of your choice the validity of the receipt. The receipt is an standard
    //S/MIME document (http://en.wikipedia.org/wiki/S/MIME)
    @Path("/{uuid}/payment") @POST
    public Response payment(@PathParam("uuid") String uuid, MessageSMIME messageSMIME,
              @Context HttpServletRequest req) throws Exception {
        shopExampleBean.sendResponse(uuid, messageSMIME.getSMIME());
        return Response.ok().entity("valid receipt").build();
    }

}
