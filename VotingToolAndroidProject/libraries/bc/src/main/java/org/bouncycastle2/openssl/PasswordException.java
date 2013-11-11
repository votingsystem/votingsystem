package org.bouncycastle2.openssl;

import java.io.IOException;

public class PasswordException
    extends IOException
{
    public PasswordException(String msg)
    {
        super(msg);
    }
}
