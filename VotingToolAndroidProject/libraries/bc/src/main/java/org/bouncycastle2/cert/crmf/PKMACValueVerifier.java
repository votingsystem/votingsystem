package org.bouncycastle2.cert.crmf;

import org.bouncycastle2.asn1.cmp.PBMParameter;
import org.bouncycastle2.asn1.crmf.PKMACValue;
import org.bouncycastle2.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle2.operator.MacCalculator;
import org.bouncycastle2.util.Arrays;

import java.io.IOException;
import java.io.OutputStream;

class PKMACValueVerifier
{
    private final PKMACBuilder builder;

    public PKMACValueVerifier(PKMACBuilder builder)
    {
        this.builder = builder;
    }

    public boolean isValid(PKMACValue value, char[] password, SubjectPublicKeyInfo keyInfo)
        throws CRMFException
    {
        builder.setParameters(PBMParameter.getInstance(value.getAlgId().getParameters()));
        MacCalculator calculator = builder.build(password);

        OutputStream macOut = calculator.getOutputStream();

        try
        {
            macOut.write(keyInfo.getDEREncoded());

            macOut.close();
        }
        catch (IOException e)
        {
            throw new CRMFException("exception encoding mac input: " + e.getMessage(), e);
        }

        return Arrays.areEqual(calculator.getMac(), value.getValue().getBytes());
    }
}