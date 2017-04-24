package org.currency.web.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.currency.web.ejb.ShopExampleEJB;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.dto.currency.TransactionResponseDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.JSON;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/shop")
public class ShopExampleResourceEJB {

    private static final Logger log = Logger.getLogger(ShopExampleResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ShopExampleEJB shopExampleBean;
    @Inject private SignatureService signatureService;
    @Inject private Config config;


    //After user interaction we have the data of the product/service the user wants to buy, with that we create a TransactionRequest
    //and show the QR code with the URL of the transaction data to offer the user the possibility to check the order with the mobile.
    @Path("/") @GET
    public Object index(@Context ServletContext context,
                        @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        TransactionDto dto = TransactionDto.PAYMENT_REQUEST("Receptor name", User.Type.USER,
                new BigDecimal(5), CurrencyCode.EUR, "IBANNumber12345", "shop example payment");
        dto.setPaymentOptions(Arrays.asList(Transaction.Type.FROM_USER,
                Transaction.Type.CURRENCY_SEND, Transaction.Type.CURRENCY_CHANGE));
        String sessionID = dto.getUUID().substring(0, 8);
        String paymentInfoServiceURL = config.getEntityId() + "/rest/shop/" + sessionID;
        shopExampleBean.putTransactionRequest(sessionID, dto);

        req.getSession().setAttribute("transactionRequest", JSON.getMapper().writeValueAsString(dto));
        req.getSession().setAttribute("paymentInfoServiceURL", paymentInfoServiceURL);
        req.getSession().setAttribute("sessionID", sessionID);

        return Response.temporaryRedirect(new URI("../shopExample/index.xhtml")).build();
    }

    //Called with async Javascript from the web page that shows the QR code, we store an AsyncContext in order to notify
    //the web client of any change in the requested transaction state
    @Path("/listenTransactionChanges/{shopSessionID}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response listenTransactionChanges(@PathParam("shopSessionID") String shopSessionID,
           @Context HttpServletRequest req, @Suspended AsyncResponse asyncResponse) throws Exception {
        asyncResponse.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse asyncResponse) {
                ResponseDto responseDto = ResponseDto.ERROR("Operation time out");
                try {
                    asyncResponse.resume(Response.ok().entity(JSON.getMapper().writeValueAsBytes(responseDto)).build());
                } catch (JsonProcessingException ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });
        asyncResponse.setTimeout(180, TimeUnit.SECONDS);
        int statusCode = shopExampleBean.bindContext(shopSessionID, asyncResponse);
        if(ResponseDto.SC_OK != statusCode)
            asyncResponse.resume(Response.status(statusCode).entity("session expired").build());
        return Response.ok().build();
    }

    //Called from the mobile after reading the QR code. The mobile fetch the transaction data
    @POST @Path("/{uuid}")
    public Response paymentInfo(@PathParam("uuid") String uuid, byte[] postData, @Context HttpServletRequest req)
            throws Exception {
        /*String revocationHash = new String(postData);
        AsyncRequestShopBundle requestBundle = shopExampleBean.getRequestBundle(uuid);
        String currencyCSR = requestBundle.addRevocationHash(config.getContextURL(), revocationHash);
                                CMSSignedMessage cmsMessage = cmsBean.signData(currencyCSR.getBytes());
        if(requestBundle.getTransactionDto() != null) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(requestBundle.getTransactionDto(
                    cmsMessage))).type(MediaType.JSON).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity(Messages.currentInstance().get("sessionExpiredMsg"))
                    .type(javax.ws.rs.core.MediaType.TEXT_PLAIN + ";charset=utf-8").build();
        }*/
        log.info("=========== TODO");
        return Response.ok().build();
    }

    //Called from the mobile after the payment. The mobile sends the signed receipt with the completed transaction data.
    //here you must check with the tools of your choice the validity of the receipt. The receipt is an standard
    //S/MIME document (http://en.wikipedia.org/wiki/S/MIME)
    @Path("/{uuid}/payment") @POST
    public Response payment(@PathParam("uuid") String uuid, byte[] postData,
              @Context HttpServletRequest req) throws Exception {
        TransactionResponseDto responseDto = JSON.getMapper().readValue(postData, TransactionResponseDto.class);
        //here you have the receipt, validate it with your tools
        shopExampleBean.sendResponse(uuid, responseDto);
        return Response.ok().entity("valid receipt").build();
    }

}
