package org.bouncycastle2.mail.smime.validator;

import org.bouncycastle2.i18n.ErrorBundle;
import org.bouncycastle2.i18n.LocalizedException;

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
