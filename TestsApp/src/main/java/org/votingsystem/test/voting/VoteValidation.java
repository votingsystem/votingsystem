package org.votingsystem.test.voting;

import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteValidation {

    private static Logger log =  Logger.getLogger(VoteValidation.class.getName());

    private static ExecutorCompletionService completionService;

    public void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SimulationData simulationData = new SimulationData();
        simulationData.setServerURL("http://currency:8080/TimeStampServer");//http://www.sistemavotacion.org/TimeStampServer
        simulationData.setMaxPendingResponses(10);
        simulationData.setNumRequestsProjected(1);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        File voteFile = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("voting/vote.p7m"));
        byte[] voteBytes = FileUtils.getBytesFromFile(voteFile);
        File trustedCertsFile = FileUtils.getFileFromBytes(
                ContextVS.getInstance().getResourceBytes("voting/systemTrustedCerts.pem"));
        Collection<X509Certificate> trustedCerts = CertUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(trustedCertsFile));
        Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>(trustedCerts.size());
        for(X509Certificate certificate: trustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            trustAnchors.add(anchor);
        }
        File eventTrustedCertsFile = FileUtils.getFileFromBytes(
                ContextVS.getInstance().getResourceBytes("voting/eventTrustedCerts.pem"));

        Collection<X509Certificate> eventTrustedCerts = CertUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(eventTrustedCertsFile));
        Set<TrustAnchor> eventTrustedAnchors = new HashSet<TrustAnchor>(eventTrustedCerts.size());
        for(X509Certificate certificate: eventTrustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            eventTrustedAnchors.add(anchor);
        }

        File timeStampCertFile = FileUtils.getFileFromBytes(
                ContextVS.getInstance().getResourceBytes("voting/timeStampCert.pem"));
        Collection<X509Certificate> timeStampCerts = CertUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(timeStampCertFile));
        X509Certificate timeStampServerCert = timeStampCerts.iterator().next();

        SMIMEMessage vote = new SMIMEMessage(voteBytes);
        Date tokenDate = vote.getSigner().getTimeStampToken().getTimeStampInfo().getGenTime();

        if(!vote.isValidSignature()) {
            log.log(Level.SEVERE, ContextVS.getInstance().getProperty("signatureErrorMsg", voteFile.getAbsolutePath()));
        }

        Set<UserVS> signersVS = vote.getSigners();
        for(UserVS signerVS:signersVS) {
            if(signerVS.getTimeStampToken() != null) {//user signature
                CertUtils.verifyCertificate(eventTrustedAnchors, false, Arrays.asList(signerVS.getCertificate()), tokenDate);
            } else {//server signature
                CertUtils.verifyCertificate(trustAnchors, false, Arrays.asList(signerVS.getCertificate()));
            }
        }
        SignerInformationVerifier timeStampSignerInfoVerifier = new
                JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert);
        vote.getSigner().getTimeStampToken().validate(timeStampSignerInfoVerifier);
        log.info("vote valid - delivered: " + tokenDate.toString());
        simulationData.finishAndExit(ResponseVS.SC_OK, null);
    }

}



