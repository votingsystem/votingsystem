package org.votingsystem.signature.smime;

import org.bouncycastle.i18n.ErrorBundle;
import org.bouncycastle.i18n.LocalizedException;

public class ExceptionVSSMIME extends LocalizedException
{

    public ExceptionVSSMIME(ErrorBundle errorMessage, Throwable throwable)
    {
        super(errorMessage, throwable);
    }

    public ExceptionVSSMIME(ErrorBundle errorMessage)
    {
        super(errorMessage);
    }
    
}
