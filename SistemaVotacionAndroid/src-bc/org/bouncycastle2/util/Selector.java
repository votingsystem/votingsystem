package org.bouncycastle2.util;

public interface Selector
    extends Cloneable
{
    boolean match(Object obj);

    Object clone();
}
