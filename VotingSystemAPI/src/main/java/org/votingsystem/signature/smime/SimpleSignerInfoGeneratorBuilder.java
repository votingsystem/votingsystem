package org.votingsystem.signature.smime;

import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 *

 */
public class SimpleSignerInfoGeneratorBuilder {
     private Helper helper;

    private boolean hasNoSignedAttributes;
    private CMSAttributeTableGenerator signedGen;
    private CMSAttributeTableGenerator unsignedGen;

    public SimpleSignerInfoGeneratorBuilder()
        throws OperatorCreationException
    {
        this.helper = new Helper();
    }

    public SimpleSignerInfoGeneratorBuilder setProvider(String providerName)
        throws OperatorCreationException
    {
        this.helper = new NamedHelper(providerName);

        return this;
    }

    public SimpleSignerInfoGeneratorBuilder setProvider(Provider provider)
        throws OperatorCreationException
    {
        this.helper = new ProviderHelper(provider);

        return this;
    }

    /**
     * If the passed in flag is true, the signer signature will be based on the data, not
     * a collection of signed attributes, and no signed attributes will be included.
     *
     * @return the builder object
     */
    public SimpleSignerInfoGeneratorBuilder setDirectSignature(boolean hasNoSignedAttributes)
    {
        this.hasNoSignedAttributes = hasNoSignedAttributes;

        return this;
    }

    public SimpleSignerInfoGeneratorBuilder setSignedAttributeGenerator(CMSAttributeTableGenerator signedGen)
    {
        this.signedGen = signedGen;

        return this;
    }

    /**
     * set up a DefaultSignedAttributeTableGenerator primed with the passed in AttributeTable.
     *
     * @param attrTable table of attributes for priming generator
     * @return this.
     */
    public SimpleSignerInfoGeneratorBuilder setSignedAttributeGenerator(AttributeTable attrTable)
    {
        this.signedGen = new DefaultSignedAttributeTableGenerator(attrTable);

        return this;
    }

    public SimpleSignerInfoGeneratorBuilder setUnsignedAttributeGenerator(CMSAttributeTableGenerator unsignedGen)
    {
        this.unsignedGen = unsignedGen;

        return this;
    }

    public SignerInfoGenerator build(String algorithmName, PrivateKey privateKey, 
            X509Certificate certificate, ContentSigner contentSigner)
        throws OperatorCreationException, CertificateEncodingException
    {
        
        return configureAndBuild().build(contentSigner, new JcaX509CertificateHolder(certificate));
    }

    public SignerInfoGenerator build(String algorithmName, PrivateKey privateKey, byte[] keyIdentifier)
        throws OperatorCreationException, CertificateEncodingException
    {
        ContentSigner contentSigner = helper.createContentSigner(algorithmName, privateKey);

        return configureAndBuild().build(contentSigner, keyIdentifier);
    }
    
        public SignerInfoGenerator build(String algorithmName, PrivateKey privateKey, X509Certificate certificate)
        throws OperatorCreationException, CertificateEncodingException
    {
        ContentSigner contentSigner = helper.createContentSigner(algorithmName, privateKey);

        return configureAndBuild().build(contentSigner, new JcaX509CertificateHolder(certificate));
    }

    private SignerInfoGeneratorBuilder configureAndBuild()
        throws OperatorCreationException
    {
        SignerInfoGeneratorBuilder infoGeneratorBuilder = new SignerInfoGeneratorBuilder(helper.createDigestCalculatorProvider());

        infoGeneratorBuilder.setDirectSignature(hasNoSignedAttributes);
        infoGeneratorBuilder.setSignedAttributeGenerator(signedGen);
        infoGeneratorBuilder.setUnsignedAttributeGenerator(unsignedGen);

        return infoGeneratorBuilder;
    }

    private class Helper
    {
        ContentSigner createContentSigner(String algorithm, PrivateKey privateKey)
            throws OperatorCreationException
        {
            return new JcaContentSignerBuilder(algorithm).build(privateKey);
        }

        DigestCalculatorProvider createDigestCalculatorProvider()
            throws OperatorCreationException
        {
            return new JcaDigestCalculatorProviderBuilder().build();
        }
    }

    private class NamedHelper
        extends Helper
    {
        private final String providerName;

        public NamedHelper(String providerName)
        {
            this.providerName = providerName;
        }

        ContentSigner createContentSigner(String algorithm, PrivateKey privateKey)
            throws OperatorCreationException
        {
            return new JcaContentSignerBuilder(algorithm).setProvider(providerName).build(privateKey);
        }

        DigestCalculatorProvider createDigestCalculatorProvider()
            throws OperatorCreationException
        {
            return new JcaDigestCalculatorProviderBuilder().setProvider(providerName).build();
        }
    }

    private class ProviderHelper
        extends Helper
    {
        private final Provider provider;

        public ProviderHelper(Provider provider)
        {
            this.provider = provider;
        }

        ContentSigner createContentSigner(String algorithm, PrivateKey privateKey)
            throws OperatorCreationException
        {
            return new JcaContentSignerBuilder(algorithm).setProvider(provider).build(privateKey);
        }

        DigestCalculatorProvider createDigestCalculatorProvider()
            throws OperatorCreationException
        {
            return new JcaDigestCalculatorProviderBuilder().setProvider(provider).build();
        }
    }    
}
