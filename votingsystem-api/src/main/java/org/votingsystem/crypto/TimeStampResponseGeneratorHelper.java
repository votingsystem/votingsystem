package org.votingsystem.crypto;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SignerInfoGeneratorBuilder;
import org.bouncycastle.cms.SimpleAttributeTableGenerator;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tsp.*;
import org.votingsystem.throwable.ExceptionBase;
import org.votingsystem.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampResponseGeneratorHelper {

    private static Logger log = Logger.getLogger(TimeStampResponseGeneratorHelper.class.getName());

    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    //1.234.567.890 -> from dss examples
    public static final String DEFAULT_TSA_POLICY_OID = "1.234.567.890";
    public static final Integer ACCURACYMICROS = 500;
    public static final Integer ACCURACYMILLIS = 500;
    public static final Integer ACCURACYSECONDS = 1;

    //# Optional. Specify if requests are ordered. Only false is supported.
    public static boolean ORDERING = false;

    private TimeStampResponse timeStampResponse;
    private BigInteger serialNumber;


    public TimeStampResponseGeneratorHelper(InputStream requestInputStream, SignatureParams signingData,
                                            Date timeStampDateUTC)
            throws Exception {
        TimeStampRequest timeStampRequest;
        try {
            timeStampRequest = new TimeStampRequest(requestInputStream);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new ExceptionBase("request null");
        }
        timeStampResponse = getTimeStampResponse(timeStampRequest, signingData, timeStampDateUTC);
    }

    public BigInteger getSerialNumber () { return serialNumber; }

    public TimeStampResponse getTimeStampResponse() {
        return timeStampResponse;
    }

    public TimeStampResponse getTimeStampResponse(TimeStampRequest tsRequest, SignatureParams signingData,
                                                  Date timeStampDateUTC) throws Exception {
        serialNumber = KeyGenerator.INSTANCE.getSerno();
        ContentSigner sigGen = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(
                Constants.PROVIDER).build(signingData.getSigningKey());
        JcaX509CertificateHolder certHolder = new JcaX509CertificateHolder(signingData.getSigningCert());

        // that to make sure we generate the same timestamp data for the same timestamp date
        AttributeTable signedAttributes = new AttributeTable(new Hashtable<ASN1ObjectIdentifier, Object>());
        signedAttributes = signedAttributes.add(PKCSObjectIdentifiers.pkcs_9_at_signingTime, new Time(timeStampDateUTC));
        DefaultSignedAttributeTableGenerator signedAttributeGenerator = new DefaultSignedAttributeTableGenerator(signedAttributes);
        AttributeTable unsignedAttributes = new AttributeTable(new Hashtable<ASN1ObjectIdentifier, Object>());
        SimpleAttributeTableGenerator unsignedAttributeGenerator = new SimpleAttributeTableGenerator(unsignedAttributes);

        DigestCalculatorProvider digestCalculatorProvider = new BcDigestCalculatorProvider();
        SignerInfoGeneratorBuilder sigInfoGeneratorBuilder = new SignerInfoGeneratorBuilder(digestCalculatorProvider);
        sigInfoGeneratorBuilder.setSignedAttributeGenerator(signedAttributeGenerator);
        sigInfoGeneratorBuilder.setUnsignedAttributeGenerator(unsignedAttributeGenerator);
        SignerInfoGenerator sig = sigInfoGeneratorBuilder.build(sigGen, certHolder);

        ASN1ObjectIdentifier policyOid =  new ASN1ObjectIdentifier(DEFAULT_TSA_POLICY_OID);

        TimeStampTokenGenerator tokenGenerator = new TimeStampTokenGenerator(sig, new SHA256DigestCalculator(), policyOid);
        tokenGenerator.setAccuracyMicros(ACCURACYMICROS);
        tokenGenerator.setAccuracyMillis(ACCURACYMILLIS);
        tokenGenerator.setAccuracySeconds(ACCURACYSECONDS);
        tokenGenerator.setOrdering(ORDERING);

        Set<X509Certificate> singleton = new HashSet<>();
        singleton.add(signingData.getSigningCert());
        tokenGenerator.addCertificates(new JcaCertStore(singleton));
        TimeStampResponseGenerator generator = new TimeStampResponseGenerator(tokenGenerator, TSPAlgorithms.ALLOWED);
        return generator.generate(tsRequest, serialNumber, timeStampDateUTC);
    }


    static class SHA256DigestCalculator implements DigestCalculator {
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
