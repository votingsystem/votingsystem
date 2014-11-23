package org.votingsystem.test.voting

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.bouncycastle.asn1.cms.SignerIdentifier
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.SignerId
import org.bouncycastle.mail.smime.SMIMESigned
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils

import javax.mail.internet.MimeMultipart
import java.security.cert.X509Certificate

Map simulationDataMap = [serverURL:"http://sistemavotacion.org/AccessControl", maxPendingResponses:10,
                         numRequestsProjected:1, timer:[active:false, time:"00:00:10"]]

log = TestUtils.init(Multisign.class, simulationDataMap)
JSONObject dataToSign = JSONSerializer.toJSON([UUID:UUID.randomUUID().toString()])

SignatureService signatureService = SignatureService.genUserVSSignatureService("08888888D")
SignatureService signatureService1 = SignatureService.genUserVSSignatureService("00111222V")
SignatureService signatureService2 = SignatureService.genUserVSSignatureService("03455543T")
SMIMEMessage smimeMessage = signatureService.getSMIME("08888888D", "00111222V", dataToSign.toString(),
        DateUtils.getDateStr(Calendar.getInstance().getTime()))

SMIMEMessage smimeSigned = signatureService1.getSMIMEMultiSigned("03455543T", "08888888D", smimeMessage,
        DateUtils.getDateStr(Calendar.getInstance().getTime()))
//log.debug(new String(smimeSigned.getBytes()))

X509Certificate x509Cert = signatureService1.getCertSigner()
smimeSigned.isValidSignature()
Collection result = smimeSigned.checkSignerCert(x509Cert)
log.debug("matches: " + result.size())

/*for(int i = 0; i< 100; i++) {
    SMIMEMessage smimeMessage = signatureService.getSMIME("08888888D", "00111222V", dataToSign.toString(),
        DateUtils.getDateStr(Calendar.getInstance().getTime()))
    smimeMessage = signatureService1.getSMIMEMultiSigned("00111222V", "03455543T", smimeMessage,
            DateUtils.getDateStr(Calendar.getInstance().getTime()))
    multiSigned =  signatureService1.getSMIMEMultiSigned("03455543T", "08888888D", smimeMessage,
            DateUtils.getDateStr(Calendar.getInstance().getTime()))
}*/

//	public SMIMEMessage getSMIME (String fromUser,String toUser,String textToSign,String subject, Header... headers)