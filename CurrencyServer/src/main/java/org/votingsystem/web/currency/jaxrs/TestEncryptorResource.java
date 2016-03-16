package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.util.MediaType;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

@Path("/testEncryptor")
public class TestEncryptorResource {

    private static final Logger log = Logger.getLogger(TestEncryptorResource.class.getName());

    @Inject ConfigVS config;
    @Inject CMSBean cmsBean;
    @Inject DAOBean dao;
    private MessagesVS messages = MessagesVS.getCurrentInstance();


    @Path("/getMultiSignedMessage") @POST
    public Response getMultiSignedMessage(CMSMessage cmsMessage, @Context ServletContext context,
                                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        CMSSignedMessage cmsSignedMessage = cmsBean.addSignature(cmsMessage.getCMS());
        return  Response.ok().entity(cmsSignedMessage.toPEM()).type(MediaType.JSON_SIGNED).build();
    }

    @Path("/validateTimeStamp") @POST
    public Response validateTimeStamp(CMSMessage cmsMessage, @Context ServletContext context,
                                      @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        User user = cmsMessage.getUser();
        //Date dateFinish = DateUtils.getDateFromString("2014-01-01 00:00:00")
        Map requestMap = cmsMessage.getSignedContent(Map.class);
        EventVS eventVS = dao.find(EventVS.class, ((Number)requestMap.get("eventId")).longValue());
        Date signatureTime = user.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(!eventVS.isActive(signatureTime)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(messages.get("checkedDateRangeErrorMsg",
                    signatureTime, eventVS.getDateBegin(), eventVS.getDateFinish())).build();
        } else return Response.ok().build();
    }

}