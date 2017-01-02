package org.votingsystem.idprovider.jaxrs;

import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.idprovider.ejb.ElectionsEJB;
import org.votingsystem.model.voting.Election;
import org.votingsystem.qr.QRRequestBundle;
import org.votingsystem.qr.QRUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.Messages;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/election")
@Stateless
@MultipartConfig(location="/tmp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*50, maxRequestSize=1024*1024*5*50)
public class ElectionResourceEJB {

    private final static Logger log = Logger.getLogger(ElectionResourceEJB.class.getName());

    @EJB ElectionsEJB electionsEJB;
    @EJB QRSessionsEJB qrSessionsEJB;
    @Inject Config config;


    //The point where we receive the data from the voting service provider
    @POST @Path("/initAuthentication")
    public Response initAuthentication(@FormParam("xmlInput") String xmlInput,
            @Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        byte[] requestBytes = null;
        if(xmlInput != null)
            requestBytes = Base64.getDecoder().decode(xmlInput);
        else requestBytes = FileUtils.getBytesFromStream(req.getInputStream());
        //URL schemaRes = Thread.currentThread().getContextClassLoader().getResource("xsd/electionIdentityRequest.xsd");
        //XMLValidator.validate(requestBytes, schemaRes);
        IdentityRequestDto identityRequest = XML.getMapper().readValue(requestBytes, IdentityRequestDto.class);
        switch (identityRequest.getType()) {
            case ANON_VOTE_CERT_REQUEST:
                Election election = electionsEJB.getElection(identityRequest.getUUID(),
                        identityRequest.getIndentityServiceEntity().getId());
                if(election == null) {
                    req.getSession().setAttribute("responseDto", new ResponseDto(ResponseDto.SC_PRECONDITION_FAILED,
                            Messages.currentInstance().get("electionUntrustedErrorMsg")));
                    res.sendRedirect(req.getContextPath() + "/response.xhtml");
                }
                qrSessionsEJB.putOperation(election.getUUID(), new QRRequestBundle(OperationType.ANON_VOTE_CERT_REQUEST,
                        new ElectionDto(election)).setIdentityRequest(identityRequest));
                QRUtils.sendRedirect(req, res, config.getEntityId(), election.getUUID(),
                        Messages.currentInstance().get("authPageHeaderMsg") + " - " + Messages.currentInstance().get("electionsLbl"),
                        Messages.currentInstance().get("authPageReadCodeMsg"));
                break;
            default:
                throw new IllegalArgumentException("The service doesn't process identity requests of type: " +
                        identityRequest.getType());
        }
        return Response.ok().build();
    }

}
