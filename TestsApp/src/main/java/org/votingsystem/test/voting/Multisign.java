package org.votingsystem.test.voting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.DateUtils;

import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.logging.Logger;

public class Multisign {

    private static Logger log;
    private static ExecutorCompletionService completionService;

    public static void main(String[] args) throws Exception {
        SimulationData simulationData = new SimulationData();
        simulationData.setServerURL("http://sistemavotacion.org/AccessControl");
        simulationData.setMaxPendingResponses(10);
        simulationData.setNumRequestsProjected(1);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        log = TestUtils.init(Multisign.class, simulationData);
        Map dataToSignMap = new HashMap<>();
        dataToSignMap.put("UUID", UUID.randomUUID().toString());

        SignatureService signatureService = SignatureService.genUserVSSignatureService("08888888D");
        SignatureService signatureService1 = SignatureService.genUserVSSignatureService("00111222V");
        SignatureService signatureService2 = SignatureService.genUserVSSignatureService("03455543T");
        SMIMEMessage smimeMessage = signatureService.getSMIME("08888888D", "00111222V",
                new ObjectMapper().writeValueAsString(dataToSignMap), DateUtils.getDateStr(new Date()));

        SMIMEMessage smimeSigned = signatureService1.getSMIMEMultiSigned("03455543T", "08888888D", smimeMessage,
                DateUtils.getDateStr(new Date()));
//log.info(new String(smimeSigned.getBytes()))

        X509Certificate x509Cert = signatureService1.getCertSigner();
        smimeSigned.isValidSignature();
        Collection result = smimeSigned.checkSignerCert(x509Cert);
        log.info("matches: " + result.size());

/*for(int i = 0; i< 100; i++) {
    SMIMEMessage smimeMessage = signatureService.getSMIME("08888888D", "00111222V", dataToSign.toString(),
        DateUtils.getDateStr(new Date()))
    smimeMessage = signatureService1.getSMIMEMultiSigned("00111222V", "03455543T", smimeMessage,
            DateUtils.getDateStr(new Date()))
    multiSigned =  signatureService1.getSMIMEMultiSigned("03455543T", "08888888D", smimeMessage,
            DateUtils.getDateStr(new Date()))
}*/

//	public SMIMEMessage getSMIME (String fromUser,String toUser,String textToSign,String subject, Header... headers)
    }
}

