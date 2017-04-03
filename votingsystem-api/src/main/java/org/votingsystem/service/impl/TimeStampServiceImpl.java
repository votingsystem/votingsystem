package org.votingsystem.service.impl;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.*;
import org.bouncycastle.util.Store;
import org.votingsystem.crypto.KeyStoreUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.crypto.TimeStampResponseGeneratorHelper;
import org.votingsystem.service.TimeStampService;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampServiceImpl implements TimeStampService {

    private static Logger log = Logger.getLogger(TimeStampServiceImpl.class.getName());

    private SignatureParams signingData;
    private SignerInformationVerifier timeStampSignerInfoVerifier;
    private byte[] signingCertPEMBytes;
    private byte[] signingCertChainPEMBytes;

    public TimeStampServiceImpl(byte[] keyStoreBytes, String password) {
        log.info("init");
        try {
            KeyStore keyStore = KeyStoreUtils.getKeyStore(keyStoreBytes, password.toCharArray());
            String keyAlias = keyStore.aliases().nextElement();
            PrivateKey signingKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
            X509Certificate signingCert = (X509Certificate) keyStore.getCertificate(keyAlias);
            signingCertPEMBytes = PEMUtils.getPEMEncoded(signingCert);
            timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                    Constants.PROVIDER).build(signingCert);
            X509CertificateHolder certHolder = timeStampSignerInfoVerifier.getAssociatedCertificate();
            TSPUtil.validateCertificate(certHolder);
            Certificate[] chain = keyStore.getCertificateChain(keyAlias);
            signingCertChainPEMBytes = PEMUtils.getPEMEncoded (Arrays.asList(chain));
            Store certs = new JcaCertStore(Arrays.asList(chain));
            signingData = new SignatureParams(signingCert, signingKey, certs);
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

    @Override
    public TimeStampResponseGeneratorHelper getResponseGeneratorDiscrete(InputStream inputStream) throws Exception {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusHours(1).withMinute(0)
                .withSecond(0).withNano(0);
        Date timestampDateUTC = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        return new TimeStampResponseGeneratorHelper(inputStream, signingData, timestampDateUTC);
    }

    @Override
    public byte[] getTimeStampResponse(InputStream inputStream) throws Exception {
        TimeStampResponseGeneratorHelper responseGenerator =
                new TimeStampResponseGeneratorHelper(inputStream, signingData, DateUtils.getUTCDateNow());
        return responseGenerator.getTimeStampResponse().getTimeStampToken().getEncoded();
    }

    @Override
    public TimeStampResponseGeneratorHelper getResponseGenerator(InputStream inputStream) throws Exception {
        return new TimeStampResponseGeneratorHelper(inputStream, signingData, DateUtils.getUTCDateNow());
    }

}
