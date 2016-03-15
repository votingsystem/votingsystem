package org.votingsystem.util.crypto;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenGenerator;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampResponseGenerator {

    private static Logger log = Logger.getLogger(TimeStampResponseGenerator.class.getName());

    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String DEFAULT_TSA_POLICY_OID = "1.2.3";
    public static final Integer ACCURACYMICROS = 500;
    public static final Integer ACCURACYMILLIS = 500;
    public static final Integer ACCURACYSECONDS = 1;

    //# Optional. Specify if requests are ordered. Only false is supported.
    public static boolean ORDERING = false;

    private TimeStampToken token;
    private BigInteger serialNumber;
    private TimeStampTokenGenerator tokenGenerator;

    public TimeStampResponseGenerator(InputStream requestInputStream, SignatureData signingData, Date timeStampDate)
            throws ExceptionVS, OperatorCreationException, CertificateEncodingException, TSPException {
        TimeStampRequest timeStampRequest;
        try {
            timeStampRequest = new TimeStampRequest(requestInputStream);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new ExceptionVS("request null");}
        serialNumber = KeyGenerator.INSTANCE.getSerno();
        log.info("getTimeStampResponse - serialNumber: " + serialNumber + " - CertReq: " + timeStampRequest.getCertReq());
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider(ContextVS.PROVIDER).build());
        tokenGenerator = new TimeStampTokenGenerator(infoGeneratorBuilder.build(
                new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(
                        ContextVS.PROVIDER).build(signingData.getSigningKey()), signingData.getSigningCert()),
                new SHA256DigestCalculator(),
                new ASN1ObjectIdentifier(DEFAULT_TSA_POLICY_OID));
        tokenGenerator.setAccuracyMicros(ACCURACYMICROS);
        tokenGenerator.setAccuracyMillis(ACCURACYMILLIS);
        tokenGenerator.setAccuracySeconds(ACCURACYSECONDS);
        tokenGenerator.setOrdering(ORDERING);
        tokenGenerator.addCertificates(signingData.getCerts());
        token = tokenGenerator.generate(timeStampRequest, serialNumber, timeStampDate);
    }

    public TimeStampToken getTimeStampToken() { return token; }

    public BigInteger getSerialNumber () { return serialNumber; }

    class SHA256DigestCalculator implements DigestCalculator {
        private ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        public AlgorithmIdentifier getAlgorithmIdentifier() {
            return new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256);
        }

        public OutputStream getOutputStream()
        {
            return bOut;
        }

        public byte[] getDigest() {
            byte[] bytes = bOut.toByteArray();
            bOut.reset();
            Digest sha256 = new SHA256Digest();
            sha256.update(bytes, 0, bytes.length);
            byte[] digest = new byte[sha256.getDigestSize()];
            sha256.doFinal(digest, 0);
            return digest;
        }
    }

}
