package org.votingsystem.web.accesscontrol.jaxrs;

import org.apache.commons.io.IOUtils;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.voting.UserRequestCsrVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.web.accesscontrol.ejb.CSRBean;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Path("/csr")
public class CSRResource {

    private static final Logger log = Logger.getLogger(CSRResource.class.getSimpleName());

    @Inject CSRBean csrBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;
    @Inject MessagesBean messages;

    @Path("/") @POST
    public Response request(@Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        byte[] requestBytes = IOUtils.toByteArray(req.getInputStream());
        UserRequestCsrVS userRequestCsr = csrBean.saveUserCSR(requestBytes);
        return Response.ok().entity(userRequestCsr.getId()).build();
    }

    @Path("/validate") @POST
    public Object validate(MessageSMIME messageSMIME) throws Exception {
        DeviceVS deviceVS = csrBean.signCertUserVS(messageSMIME);
        return Response.ok().entity(CertUtils.getPEMEncoded(deviceVS.getX509Certificate())).build();
    }

    /**
     * Servicio que devuelve las solicitudes de certificados firmadas una vez que
     * se ha validado la identidad del usuario.
     *
     * @httpMethod [GET]
     * @param csrRequestId Identificador de la solicitud de certificación enviada previamente por el usuario.
     * @return Si el sistema ha validado al usuario devuelve la solicitud de certificación firmada.
     */
    @Path("/") @GET
    public Object getIssuedCert(@QueryParam("csrRequestId") Long csrRequestId) throws Exception {
        Query query = dao.getEM().createQuery("select u from UserRequestCsrVS u where u.id =:id and u.state =:state")
                .setParameter("id", csrRequestId).setParameter("state", UserRequestCsrVS.State.OK);
        UserRequestCsrVS csrRequest = dao.getSingleResult(UserRequestCsrVS.class, query);
        if(csrRequest == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(messages.get("csrRequestNotValidated")).build();
        }
        query = dao.getEM().createQuery("select c from CertificateVS c where c.userRequestCsrVS =:csrRequest")
                .setParameter("csrRequest", csrRequest);
        CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
        if(certificate != null) return Response.status(Response.Status.BAD_REQUEST).entity(
                messages.get("csrGenerationErrorMsg")).build();
        X509Certificate certX509 = CertUtils.loadCertificate(certificate.getContent());
        List<X509Certificate> certs = Arrays.asList(certX509, signatureBean.getServerCert());
        return Response.ok().entity(CertUtils.getPEMEncoded (certs)).build();
    }


}