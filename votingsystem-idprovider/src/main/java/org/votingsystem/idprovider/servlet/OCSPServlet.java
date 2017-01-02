package org.votingsystem.idprovider.servlet;

import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ocsp.RevokedInfo;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.votingsystem.ejb.Config;
import org.votingsystem.idprovider.ejb.CertIssuerEJB;
import org.votingsystem.model.Certificate;
import org.votingsystem.ocsp.RootCertOCSPInfo;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebServlet("/ocsp")
public class OCSPServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(OCSPServlet.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject Config config;
    @EJB CertIssuerEJB certIssuer;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.getOutputStream().write("Endpoint doesn't support GET method".getBytes());
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        byte[] ocspRequestBytes = FileUtils.getBytesFromStream(req.getInputStream());
        RootCertOCSPInfo rootCertOCSPInfo = certIssuer.getRootCertOCSPInfo();
        try {
            OCSPReq ocspRequest = new OCSPReq(ocspRequestBytes);
            Set<RootCertOCSPInfo.OCSPResponseData> responses = new HashSet<>();
            RootCertOCSPInfo.OCSPResponseData responseData = null;
            for (Req ocspReq : ocspRequest.getRequestList()) {
                CertificateID certId = ocspReq.getCertID();
                if(!certId.getHashAlgOID().getId().equals(CMSSignedGenerator.DIGEST_SHA1)) {
                    log.severe("HashAlgOID no supported: " + certId.getHashAlgOID());
                    responseData = new RootCertOCSPInfo.OCSPResponseData(certId, new UnknownStatus());
                } else if(!rootCertOCSPInfo.getKeyHash().equals(
                        Base64.getEncoder().encodeToString(certId.getIssuerKeyHash()))) {
                    log.severe("ERROR KeyHash - expected: " + rootCertOCSPInfo.getKeyHash() +
                            " - found: " + certId.getHashAlgOID());
                    responseData = new RootCertOCSPInfo.OCSPResponseData(certId, new UnknownStatus());
                } else if(!rootCertOCSPInfo.getNameHash().equals(
                        Base64.getEncoder().encodeToString(certId.getIssuerNameHash()))) {
                    log.severe("ERROR NameHash - expected: " + rootCertOCSPInfo.getNameHash() +
                            " - found: " + certId.getIssuerNameHash());
                    responseData = new RootCertOCSPInfo.OCSPResponseData(certId, new UnknownStatus());
                } else {
                    List<Certificate> certificates = em.createNamedQuery(Certificate.FIND_BY_SERIALNUMBER_AND_AUTHORITY)
                            .setParameter("serialNumber", certId.getSerialNumber().longValue())
                            .setParameter("authorityCertificate", rootCertOCSPInfo.getCertificate()).getResultList();
                    if(certificates.isEmpty()) {
                        log.severe("ERROR unknown cert serialNumber: " + certId.getSerialNumber());
                        responseData = new RootCertOCSPInfo.OCSPResponseData(certId, new UnknownStatus());
                    } else {
                        Certificate certificate = certificates.iterator().next();
                        switch (certificate.getState()) {
                            case OK:
                                responseData = new RootCertOCSPInfo.OCSPResponseData(certId, CertificateStatus.GOOD);
                                break;
                            default:
                                CertificateStatus certificateStatus = new RevokedStatus(new RevokedInfo(
                                        new ASN1GeneralizedTime(DateUtils.getUTCDate(certificate.getStateDate())),
                                        CRLReason.lookup(CRLReason.cessationOfOperation)));
                                responseData = new RootCertOCSPInfo.OCSPResponseData(certId, certificateStatus);
                                break;
                        }
                    }
                }
                log.info("cert. serialNumber: " + responseData.getCertId().getSerialNumber() +
                        " - status: " + ((responseData.getCertificateStatus() == null) ? "GOOD" :
                        responseData.getCertificateStatus().getClass().getSimpleName()));
                responses.add(responseData);
            }
            byte[] response = rootCertOCSPInfo.generateOCSPResponse(responses);
            res.setContentType("application/ocsp-response");
            res.setContentLength(response.length);
            res.getOutputStream().write(response);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);

        }
    }

}