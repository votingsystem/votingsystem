package org.votingsystem.crypto.cms;

import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;
import org.votingsystem.util.Constants;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;


public class CMSGenerator {

    private PrivateKey key;
    private List<Certificate> certList;
    private String signatureMechanism;
    private AlgorithmIdentifier sigAlgId;
    private AlgorithmIdentifier digAlgId;

    public CMSGenerator() {}

    public CMSGenerator(KeyStore keyStore, String keyAlias,
                        char[] password, String signatureMechanism) throws Exception {
        key = (PrivateKey)keyStore.getKey(keyAlias, password);
        certList = Arrays.asList(keyStore.getCertificateChain(keyAlias));
        this.signatureMechanism = signatureMechanism;
        sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureMechanism);
        digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
    }

    public CMSGenerator(PrivateKey privateKey, X509Certificate[] arrayCerts, String signatureMechanism) {
        this.key = privateKey;
        certList = Arrays.asList(arrayCerts);
        this.signatureMechanism = signatureMechanism;
        sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureMechanism);
        digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
    }

    public CMSGenerator(PrivateKey privateKey, List<Certificate> certList, String signatureMechanism) {
        this.key = privateKey;
        this.certList = certList;
        this.signatureMechanism = signatureMechanism;
        sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureMechanism);
        digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
    }

    public TimeStampRequest getTimeStampRequest(byte[] signatureContent) throws NoSuchAlgorithmException {
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        return reqgen.generate(digAlgId.getAlgorithm(), getContentDigest(signatureContent));
    }

    public CMSSignedMessage signDataWithTimeStamp(byte[] signatureContent, TimeStampToken timeStampToken) throws Exception {
        DERSet derset = new DERSet(timeStampToken.toCMSSignedData().toASN1Structure());
        Attribute timeStampAsAttribute = new Attribute(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, derset);
        Hashtable hashTable = new Hashtable();
        hashTable.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, timeStampAsAttribute);
        AttributeTable timeStampAsAttributeTable = new AttributeTable(hashTable);
        DefaultSignedAttributeTableGenerator signedAttributeGenerator = new DefaultSignedAttributeTableGenerator(timeStampAsAttributeTable);

        CMSTypedData msg = new CMSProcessableByteArray(signatureContent);
        Store certs = new JcaCertStore(certList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

        ContentSigner signer = new JcaContentSignerBuilder(signatureMechanism).setProvider(Constants.PROVIDER).build(key);
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder()
                .setProvider(Constants.PROVIDER).build()).setSignedAttributeGenerator(signedAttributeGenerator)
                .build(signer, (X509Certificate) certList.get(0)));
        gen.addCertificates(certs);
        CMSSignedData signedData = gen.generate(msg, true);
        return  new CMSSignedMessage(signedData);
    }

    public CMSSignedMessage signData(byte[] contentToSign) throws Exception {
        CMSTypedData msg = new CMSProcessableByteArray(contentToSign);
        Store certs = new JcaCertStore(certList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder(signatureMechanism).setProvider(Constants.PROVIDER).build(key);
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder()
                .setProvider(Constants.PROVIDER).build()).build(signer, (X509Certificate) certList.get(0)));
        gen.addCertificates(certs);
        CMSSignedData signedData = gen.generate(msg, true);
        return  new CMSSignedMessage(signedData);
    }

    public byte[] getContentDigest(byte[] contentBytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(digAlgId.getAlgorithm().getId());
            return digest.digest(contentBytes);
    }

    public String getSignatureMechanism() {
        return signatureMechanism;
    }

    public synchronized CMSSignedData addSignature(CMSSignedData cmsMessage) throws Exception {
        Store signedDataCertStore = cmsMessage.getCertificates();
        SignerInformationStore signers = cmsMessage.getSignerInfos();
        //You'll need to copy the other signers certificates across as well if you want them included.
        List resultCertList = new ArrayList();
        Iterator it = signers.getSigners().iterator();
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation)it.next();
            Collection certCollection = signedDataCertStore.getMatches(signer.getSID());
            X509CertificateHolder certificateHolder = (X509CertificateHolder)certCollection.iterator().next();
            X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider(
                    Constants.PROVIDER).getCertificate(certificateHolder);
            resultCertList.add(x509Certificate);
        }
        resultCertList.add(certList.get(0));
        Store certs = new JcaCertStore(resultCertList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder(signatureMechanism).setProvider(Constants.PROVIDER).build(key);
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder()
                .setProvider(Constants.PROVIDER).build()).build(signer, (X509Certificate) certList.get(0)));
        gen.addCertificates(certs);
        gen.addSigners(signers);
        return gen.generate(cmsMessage.getSignedContent(), true);
    }


}