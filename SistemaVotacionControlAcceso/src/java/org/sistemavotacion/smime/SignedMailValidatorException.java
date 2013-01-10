package org.sistemavotacion.smime;

import org.bouncycastle.i18n.ErrorBundle;
import org.bouncycastle.i18n.LocalizedException;

public class SignedMailValidatorException extends LocalizedException
{

    public SignedMailValidatorException(ErrorBundle errorMessage, Throwable throwable)
    {
        super(errorMessage, throwable);
    }

    public SignedMailValidatorException(ErrorBundle errorMessage)
    {
        super(errorMessage);
    }
    
}
