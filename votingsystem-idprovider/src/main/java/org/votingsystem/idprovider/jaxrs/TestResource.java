package org.votingsystem.idprovider.jaxrs;


import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.util.FileUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test")
public class TestResource {

    private static final Logger log = Logger.getLogger(TestResource.class.getName());

    @PersistenceContext EntityManager em;
    @Inject Config config;
    @EJB QRSessionsEJB qrSessionsEJB;

    @GET @Path("/")
    @Transactional
    public Response test(@Context HttpServletRequest req) throws Exception {
        List<SignedDocument> signedDocumentList = em.createQuery("select s from SignedDocument s where s.id=:id")
                .setParameter("id", 6L).getResultList();
        FileUtils.copyBytesToFile(signedDocumentList.iterator().next().getBody().getBytes(), new File("/home/jgzornoza/temp/dnieSignedDocument.xml"));
        return Response.ok().entity("OK").build() ;
    }

    @GET @Path("/responsePage")
    @Transactional
    public Response test2(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        req.getSession().setAttribute("responseDto", new ResponseDto().setCaption("caption").setMessage("message"));
        res.sendRedirect(req.getContextPath() + "/response.xhtml");
        return Response.ok().build();
    }

}