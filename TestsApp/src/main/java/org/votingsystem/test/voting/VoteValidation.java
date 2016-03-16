package org.votingsystem.test.voting;

import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;

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
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SimulationData simulationData = new SimulationData();
        simulationData.setServerURL("https://192.168.1.5/TimeStampServer");//http://www.sistemavotacion.org/TimeStampServer
        simulationData.setMaxPendingResponses(10);
        simulationData.setNumRequestsProjected(1);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        File voteFile = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("voting/vote.p7s"));
        byte[] voteBytes = FileUtils.getBytesFromFile(voteFile);
        File trustedCertsFile = FileUtils.getFileFromBytes(
                ContextVS.getInstance().getResourceBytes("voting/systemTrustedCerts.pem"));
        Collection<X509Certificate> trustedCerts = PEMUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(trustedCertsFile));
        Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>(trustedCerts.size());
        for(X509Certificate certificate: trustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            trustAnchors.add(anchor);
        }
        File eventTrustedCertsFile = FileUtils.getFileFromBytes(
                ContextVS.getInstance().getResourceBytes("voting/eventTrustedCerts.pem"));

        Collection<X509Certificate> eventTrustedCerts = PEMUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(eventTrustedCertsFile));
        Set<TrustAnchor> eventTrustedAnchors = new HashSet<TrustAnchor>(eventTrustedCerts.size());
        for(X509Certificate certificate: eventTrustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            eventTrustedAnchors.add(anchor);
        }

        File timeStampCertFile = FileUtils.getFileFromBytes(
                ContextVS.getInstance().getResourceBytes("voting/timeStampCert.pem"));
        Collection<X509Certificate> timeStampCerts = PEMUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(timeStampCertFile));
        X509Certificate timeStampServerCert = timeStampCerts.iterator().next();

        CMSSignedMessage vote = new CMSSignedMessage(voteBytes);
        Date tokenDate = vote.getSigner().getTimeStampToken().getTimeStampInfo().getGenTime();

        if(vote.isValidSignature() == null) {
            log.log(Level.SEVERE, ContextVS.getInstance().getProperty("signatureErrorMsg", voteFile.getAbsolutePath()));
        }

        Set<User> signersVS = vote.getSigners();
        for(User signerVS:signersVS) {
            if(signerVS.getTimeStampToken() != null) {//user signature
                CertUtils.verifyCertificate(eventTrustedAnchors, false, Arrays.asList(signerVS.getX509Certificate()), tokenDate);
            } else {//server signature
                CertUtils.verifyCertificate(trustAnchors, false, Arrays.asList(signerVS.getX509Certificate()));
            }
        }
        SignerInformationVerifier timeStampSignerInfoVerifier = new
                JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert);
        vote.getSigner().getTimeStampToken().validate(timeStampSignerInfoVerifier);
        log.info("vote valid - delivered: " + tokenDate.toString());
        simulationData.finishAndExit(ResponseVS.SC_OK, null);
    }

}



