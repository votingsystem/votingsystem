package org.votingsystem.test.voting

import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.SignedFile
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.FileUtils
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

log = TestUtils.init(VoteValidation.class)

File voteFile = TestUtils.getFileFromResources("voting/vote_00000028.p7m")
byte[] voteBytes = FileUtils.getBytesFromFile(voteFile)
File trustedCertsFile = TestUtils.getFileFromResources("voting/systemTrustedCerts.pem")
Collection<X509Certificate> trustedCerts = CertUtils.fromPEMToX509CertCollection(
        FileUtils.getBytesFromFile(trustedCertsFile));
Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>(trustedCerts.size());
for(X509Certificate certificate: trustedCerts) {
    TrustAnchor anchor = new TrustAnchor(certificate, null);
    trustAnchors.add(anchor);
}

File eventTrustedCertsFile = TestUtils.getFileFromResources("voting/eventTrustedCerts.pem")
Collection<X509Certificate> eventTrustedCerts = CertUtils.fromPEMToX509CertCollection(
        FileUtils.getBytesFromFile(eventTrustedCertsFile));
Set<TrustAnchor> eventTrustedAnchors = new HashSet<TrustAnchor>(eventTrustedCerts.size());
for(X509Certificate certificate: eventTrustedCerts) {
    TrustAnchor anchor = new TrustAnchor(certificate, null);
    eventTrustedAnchors.add(anchor);
}

File timeStampCertFile = TestUtils.getFileFromResources("voting/timeStampCert.pem");
Collection<X509Certificate> timeStampCerts = CertUtils.fromPEMToX509CertCollection(
        FileUtils.getBytesFromFile(timeStampCertFile));
X509Certificate timeStampServerCert = timeStampCerts.iterator().next();

SignedFile vote = new SignedFile(voteBytes, voteFile.getName(), null)
Date tokenDate = vote.getSMIME().getSigner().getTimeStampToken().getTimeStampInfo().getGenTime()

if(!vote.isValidSignature()) {
    return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("signatureErrorMsg",
            vote.getName()))

}

Set<UserVS> signersVS = vote.getSMIME().getSigners();
for(UserVS signerVS:signersVS) {
    if(signerVS.getTimeStampToken() != null) {//user signature
        CertUtils.verifyCertificate(eventTrustedAnchors, false, Arrays.asList(signerVS.getCertificate()), tokenDate)
    } else {//server signature
        CertUtils.verifyCertificate(trustAnchors, false, Arrays.asList(signerVS.getCertificate()));
    }
}

SignerInformationVerifier timeStampSignerInfoVerifier = new
        JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert);
vote.getSMIME().getSigner().getTimeStampToken().validate(timeStampSignerInfoVerifier);

log.debug("vote valid - delivered: " + tokenDate.toString())