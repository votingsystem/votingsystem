package org.votingsystem.cms;

import org.bouncycastle.i18n.ErrorBundle;
import org.bouncycastle.i18n.LocalizedException;

public class ExceptionCMS extends LocalizedException
{

    public ExceptionCMS(ErrorBundle errorMessage, Throwable throwable)
    {
        super(errorMessage, throwable);
    }

    public ExceptionCMS(ErrorBundle errorMessage)
    {
        super(errorMessage);
    }
    
}
