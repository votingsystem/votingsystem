package org.votingsystem.service.impl;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.*;
import org.bouncycastle.util.Store;
import org.votingsystem.service.TimeStampService;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.crypto.KeyStoreUtil;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.util.crypto.SignatureData;
import org.votingsystem.util.crypto.TimeStampResponseGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampServiceImpl implements TimeStampService {

    private static Logger log = Logger.getLogger(TimeStampServiceImpl.class.getName());

    private SignatureData signingData;
    private SignerInformationVerifier timeStampSignerInfoVerifier;
    private byte[] signingCertPEMBytes;
    private byte[] signingCertChainPEMBytes;

    public TimeStampServiceImpl(byte[] keyStoreBytes, String keyAlias, String password) {
        log.info("init");
        try {
            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password.toCharArray());
            PrivateKey signingKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
            X509Certificate signingCert = (X509Certificate) keyStore.getCertificate(keyAlias);
            signingCertPEMBytes = PEMUtils.getPEMEncoded(signingCert);
            timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                    ContextVS.PROVIDER).build(signingCert);
            X509CertificateHolder certHolder = timeStampSignerInfoVerifier.getAssociatedCertificate();
            TSPUtil.validateCertificate(certHolder);
            Certificate[] chain = keyStore.getCertificateChain(keyAlias);
            signingCertChainPEMBytes = PEMUtils.getPEMEncoded (Arrays.asList(chain));
            Store certs = new JcaCertStore(Arrays.asList(chain));
            signingData = new SignatureData(signingCert, signingKey, certs);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public byte[] getSigningCertPEMBytes() {
        return signingCertPEMBytes;
    }

    public byte[] getSigningCertChainPEMBytes() {
        return signingCertChainPEMBytes;
    }

    public void validateToken(TimeStampToken timeStampToken) throws TSPException {
        timeStampToken.validate(timeStampSignerInfoVerifier);
    }

    public byte[] getTimeStampRequest(byte[] digest) throws IOException {
        log.info("getTimeStampRequest");
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        TimeStampRequest timeStampRequest = reqgen.generate(TSPAlgorithms.SHA256, digest);
        return timeStampRequest.getEncoded();
    }

    public TimeStampResponseGenerator getResponseGeneratorDiscrete(InputStream inputStream) throws OperatorCreationException,
            CertificateEncodingException, ExceptionVS, TSPException, IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return new TimeStampResponseGenerator(inputStream, signingData, calendar.getTime());
    }

    public byte[] getTimeStampResponse(InputStream inputStream) throws OperatorCreationException,
            CertificateEncodingException, ExceptionVS, TSPException, IOException {
        TimeStampResponseGenerator responseGenerator =
                new TimeStampResponseGenerator(inputStream, signingData, new Date());
        return responseGenerator.getTimeStampToken().getEncoded();
    }

    public TimeStampResponseGenerator getResponseGenerator(InputStream inputStream) throws Exception {
        return new TimeStampResponseGenerator(inputStream, signingData, new Date());
    }

}
