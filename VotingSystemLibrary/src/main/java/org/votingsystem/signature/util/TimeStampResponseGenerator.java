package org.votingsystem.signature.util;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cmp.PKIFreeText;
import org.bouncycastle.asn1.cmp.PKIStatusInfo;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.tsp.TimeStampResp;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.*;
import org.votingsystem.model.ContextVS;
import org.votingsystem.throwable.ExceptionVS;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampResponseGenerator {

    private static Logger log = Logger.getLogger(TimeStampResponseGenerator.class);

    private static Set acceptedAlgorithms = new HashSet<>(Arrays.asList(TSPAlgorithms.SHA1, TSPAlgorithms.SHA256,
            TSPAlgorithms.SHA512));
    private static Set acceptedPolicies = new HashSet<>(Arrays.asList("1.2.3", "1.2.4"));
    private static Set acceptedExtensions = new HashSet<>();

    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String DEFAULT_TSA_POLICY_OID = "1.2.3";
    public static final Integer ACCURACYMICROS = 500;
    public static final Integer ACCURACYMILLIS = 500;
    public static final Integer ACCURACYSECONDS = 1;

    //# Optional. Specify if requests are ordered. Only false is supported.
    public static boolean ORDERING = false;

    private TimeStampToken token;
    private BigInteger serialNumber;
    private int status;
    private ASN1EncodableVector statusStrings;
    private int failInfo;
    private TimeStampTokenGenerator tokenGenerator;

    public TimeStampResponseGenerator(InputStream requestInputStream, SignatureData signingData, Date timeStampDate)
            throws ExceptionVS, OperatorCreationException, CertificateEncodingException, TSPException {
        TimeStampRequest timeStampRequest;
        try {
            timeStampRequest = new TimeStampRequest(requestInputStream);
        } catch (Exception ex) {throw new ExceptionVS("request null");}
        this.statusStrings = new ASN1EncodableVector();
        serialNumber = KeyGeneratorVS.INSTANCE.getSerno();
        log.debug("getTimeStampResponse - serialNumber: '${serialNumber}' - CertReq: ${timeStampRequest.getCertReq()}");
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider(ContextVS.PROVIDER).build());
        tokenGenerator = new TimeStampTokenGenerator(infoGeneratorBuilder.build(
                new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(
                        ContextVS.PROVIDER).build(signingData.getSigningKey()), signingData.getSigningCert()),
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

    //InputStream requestInputStream, Date timeStampDate
    public TimeStampResponseGenerator(TimeStampTokenGenerator tokenGenerator, Set acceptedAlgorithms) {
        this(tokenGenerator, acceptedAlgorithms, (Set)null, (Set)null);
    }

    public TimeStampResponseGenerator(TimeStampTokenGenerator tokenGenerator, Set acceptedAlgorithms, Set acceptedPolicy) {
        this(tokenGenerator, acceptedAlgorithms, acceptedPolicy, (Set)null);
    }

    public TimeStampResponseGenerator(TimeStampTokenGenerator tokenGenerator, Set acceptedAlgorithms, Set acceptedPolicies, Set acceptedExtensions) {
        this.tokenGenerator = tokenGenerator;
        this.acceptedAlgorithms = acceptedAlgorithms;
        this.acceptedPolicies = acceptedPolicies;
        this.acceptedExtensions = acceptedExtensions;
        this.statusStrings = new ASN1EncodableVector();
    }

    private void addStatusString(String statusString) {
        this.statusStrings.add(new DERUTF8String(statusString));
    }

    private void setFailInfoField(int field) {
        this.failInfo |= field;
    }

    private PKIStatusInfo getPKIStatusInfo() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new DERInteger(this.status));
        if(this.statusStrings.size() > 0) {
            v.add(new PKIFreeText(new DERSequence(this.statusStrings)));
        }

        if(this.failInfo != 0) {
            TimeStampResponseGenerator.FailInfo failInfoBitString = new TimeStampResponseGenerator.FailInfo(this.failInfo);
            v.add(failInfoBitString);
        }

        return new PKIStatusInfo(new DERSequence(v));
    }

    /** @deprecated */
    public TimeStampResponse generate(TimeStampRequest request, BigInteger serialNumber, Date genTime, String provider) throws NoSuchAlgorithmException, NoSuchProviderException, TSPException {
        TimeStampResp resp;
        PKIStatusInfo pkiStatusInfo;
        try {
            if(genTime == null) {
                throw new TSPValidationException("The time source is not available.", 512);
            }

            request.validate(this.acceptedAlgorithms, this.acceptedPolicies, this.acceptedExtensions, provider);
            this.status = 0;
            this.addStatusString("Operation OK");
            PKIStatusInfo e = this.getPKIStatusInfo();
            pkiStatusInfo = null;

            ContentInfo pkiStatusInfo1;
            try {
                ByteArrayInputStream ioEx = new ByteArrayInputStream(this.tokenGenerator.generate(request, serialNumber, genTime, provider).toCMSSignedData().getEncoded());
                ASN1InputStream aIn = new ASN1InputStream(ioEx);
                pkiStatusInfo1 = ContentInfo.getInstance(aIn.readObject());
            } catch (IOException var11) {
                throw new TSPException("Timestamp token received cannot be converted to ContentInfo", var11);
            }

            resp = new TimeStampResp(e, pkiStatusInfo1);
        } catch (TSPValidationException var12) {
            this.status = 2;
            this.setFailInfoField(var12.getFailureCode());
            this.addStatusString(var12.getMessage());
            pkiStatusInfo = this.getPKIStatusInfo();
            resp = new TimeStampResp(pkiStatusInfo, (ContentInfo)null);
        }

        try {
            return new TimeStampResponse(resp);
        } catch (IOException var10) {
            throw new TSPException("created badly formatted response!");
        }
    }

    public TimeStampResponse generate(TimeStampRequest request, BigInteger serialNumber, Date genTime) throws TSPException {
        this.statusStrings = new ASN1EncodableVector();

        TimeStampResp resp;
        PKIStatusInfo pkiStatusInfo;
        try {
            if(genTime == null) {
                throw new TSPValidationException("The time source is not available.", 512);
            }

            request.validate(this.acceptedAlgorithms, this.acceptedPolicies, this.acceptedExtensions);
            this.status = 0;
            this.addStatusString("Operation Okay");
            PKIStatusInfo e = this.getPKIStatusInfo();
            pkiStatusInfo = null;

            ContentInfo pkiStatusInfo1;
            try {
                ByteArrayInputStream ioEx = new ByteArrayInputStream(this.tokenGenerator.generate(request, serialNumber, genTime).toCMSSignedData().getEncoded());
                ASN1InputStream aIn = new ASN1InputStream(ioEx);
                pkiStatusInfo1 = ContentInfo.getInstance(aIn.readObject());
            } catch (IOException var10) {
                throw new TSPException("Timestamp token received cannot be converted to ContentInfo", var10);
            }

            resp = new TimeStampResp(e, pkiStatusInfo1);
        } catch (TSPValidationException var11) {
            this.status = 2;
            this.setFailInfoField(var11.getFailureCode());
            this.addStatusString(var11.getMessage());
            pkiStatusInfo = this.getPKIStatusInfo();
            resp = new TimeStampResp(pkiStatusInfo, (ContentInfo)null);
        }

        try {
            return new TimeStampResponse(resp);
        } catch (IOException var9) {
            throw new TSPException("created badly formatted response!");
        }
    }

    public TimeStampResponse generateFailResponse(int status, int failInfoField, String statusString) throws TSPException {
        this.status = status;
        this.setFailInfoField(failInfoField);
        if(statusString != null) {
            this.addStatusString(statusString);
        }

        PKIStatusInfo pkiStatusInfo = this.getPKIStatusInfo();
        TimeStampResp resp = new TimeStampResp(pkiStatusInfo, (ContentInfo)null);

        try {
            return new TimeStampResponse(resp);
        } catch (IOException var7) {
            throw new TSPException("created badly formatted response!");
        }
    }

    class FailInfo extends DERBitString {
        FailInfo(int failInfoValue) {
            super(getBytes(failInfoValue), getPadBits(failInfoValue));
        }
    }
}
