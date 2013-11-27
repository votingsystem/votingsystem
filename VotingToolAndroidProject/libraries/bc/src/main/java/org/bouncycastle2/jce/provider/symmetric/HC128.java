package org.bouncycastle2.jce.provider.symmetric;

import org.bouncycastle2.crypto.CipherKeyGenerator;
import org.bouncycastle2.crypto.engines.HC128Engine;
import org.bouncycastle2.jce.provider.JCEKeyGenerator;
import org.bouncycastle2.jce.provider.JCEStreamCipher;

import java.util.HashMap;

public final class HC128
{
    private HC128()
    {
    }
    
    public static class Base
        extends JCEStreamCipher
    {
        public Base()
        {
            super(new HC128Engine(), 16);
        }
    }

    public static class KeyGen
        extends JCEKeyGenerator
    {
        public KeyGen()
        {
            super("HC128", 128, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends HashMap
    {
        public Mappings()
        {
            put("Cipher.HC128", "org.bouncycastle2.jce.provider.symmetric.HC128$Base");
            put("KeyGenerator.HC128", "org.bouncycastle2.jce.provider.symmetric.HC128$KeyGen");
        }
    }
}
