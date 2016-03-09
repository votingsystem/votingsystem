package org.votingsystem.cms;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.crypto.KeyStoreUtil;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;


public class CMSGenerator {

    private PrivateKey key;
    private List<Certificate> certList;
    private String signatureMechanism;

    public CMSGenerator() {}

    public CMSGenerator(byte[] keyStoreBytes, String keyAlias,
                        char[] password, String signatureMechanism) throws Exception {
        KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
        key = (PrivateKey)keyStore.getKey(keyAlias, password);
        certList = Arrays.asList(keyStore.getCertificateChain(keyAlias));
        this.signatureMechanism = signatureMechanism;
    }

    public CMSGenerator(KeyStore keyStore, String keyAlias,
                        char[] password, String signatureMechanism) throws Exception {
        key = (PrivateKey)keyStore.getKey(keyAlias, password);
        certList = Arrays.asList(keyStore.getCertificateChain(keyAlias));
        this.signatureMechanism = signatureMechanism;
    }

    public CMSGenerator(PrivateKey privateKey, X509Certificate[] arrayCerts, String signatureMechanism) {
        this.key = privateKey;
        certList = Arrays.asList(arrayCerts);
        this.signatureMechanism = signatureMechanism;
    }

    public CMSGenerator(PrivateKey privateKey, List<Certificate> certList, String signatureMechanism) {
        this.key = privateKey;
        this.certList = certList;
        this.signatureMechanism = signatureMechanism;
    }

    public CMSSignedMessage signData(String signatureContent) throws Exception {
        CMSTypedData msg = new CMSProcessableByteArray(signatureContent.getBytes());
        Store certs = new JcaCertStore(certList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder(signatureMechanism).setProvider(ContextVS.PROVIDER).build(key);
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder()
                .setProvider(ContextVS.PROVIDER).build()).build(signer, (X509Certificate) certList.get(0)));
        gen.addCertificates(certs);
        CMSSignedData signedData = gen.generate(msg, true);
        return  new CMSSignedMessage(signedData);
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
                    ContextVS.PROVIDER).getCertificate(certificateHolder);
            resultCertList.add(x509Certificate);
        }
        resultCertList.add((X509Certificate) certList.get(0));
        Store certs = new JcaCertStore(resultCertList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider(ContextVS.PROVIDER).build(key);
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder()
                .setProvider(ContextVS.PROVIDER).build()).build(signer, (X509Certificate) certList.get(0)));
        gen.addCertificates(certs);
        gen.addSigners(signers);
        return gen.generate((CMSTypedData)cmsMessage.getSignedContent(), true);
    }
}
