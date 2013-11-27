package org.bouncycastle2.crypto.tls;

import org.bouncycastle2.crypto.params.AsymmetricKeyParameter;

import java.io.IOException;

public interface TlsAgreementCredentials extends TlsCredentials
{
    byte[] generateAgreement(AsymmetricKeyParameter serverPublicKey) throws IOException;
}
