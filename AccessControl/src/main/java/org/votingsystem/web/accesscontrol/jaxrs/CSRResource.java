package org.votingsystem.web.accesscontrol.jaxrs;

import org.apache.commons.io.IOUtils;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.Device;
import org.votingsystem.model.voting.UserRequestCsr;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.accesscontrol.ejb.CSRBean;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
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

    private static final Logger log = Logger.getLogger(CSRResource.class.getName());

    @Inject CSRBean csrBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CMSBean cmsBean;
    private MessagesVS messages = MessagesVS.getCurrentInstance();

    @Path("/request") @POST @Transactional
    public Response request(@Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        byte[] requestBytes = IOUtils.toByteArray(req.getInputStream());
        UserRequestCsr userRequestCsr = csrBean.saveUserCSR(requestBytes);
        return Response.ok().entity(userRequestCsr.getId().toString()).build();
    }

    @Path("/validate") @POST
    public Object validate(CMSMessage cmsMessage) throws Exception {
        Device device = csrBean.signCertUser(cmsMessage);
        return Response.ok().entity(PEMUtils.getPEMEncoded(device.getX509Certificate())).build();
    }

    @Path("/") @GET
    public Response getIssuedCert(@QueryParam("csrRequestId") Long csrRequestId) throws Exception {
        Query query = dao.getEM().createQuery("select u from UserRequestCsr u where u.id =:id and u.state =:state")
                .setParameter("id", csrRequestId).setParameter("state", UserRequestCsr.State.OK);
        UserRequestCsr csrRequest = dao.getSingleResult(UserRequestCsr.class, query);
        if(csrRequest == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(messages.get("csrRequestNotValidated")).build();
        }
        X509Certificate certX509 = CertUtils.loadCertificate(csrRequest.getCertificate().getContent());
        List<X509Certificate> certs = Arrays.asList(certX509, cmsBean.getServerCert());
        return Response.ok().entity(PEMUtils.getPEMEncoded (certs)).build();
    }


}