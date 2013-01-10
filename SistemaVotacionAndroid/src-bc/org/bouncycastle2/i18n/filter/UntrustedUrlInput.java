package org.bouncycastle2.i18n.filter;

/**
 * 
 * Wrapper class to mark an untrusted Url
 */
public class UntrustedUrlInput extends UntrustedInput
{
    public UntrustedUrlInput(Object url)
    {
        super(url);
    }
    
}
