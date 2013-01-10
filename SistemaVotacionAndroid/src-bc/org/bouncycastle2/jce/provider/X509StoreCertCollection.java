package org.bouncycastle2.jce.provider;

import org.bouncycastle2.util.CollectionStore;
import org.bouncycastle2.util.Selector;
import org.bouncycastle2.x509.X509CollectionStoreParameters;
import org.bouncycastle2.x509.X509StoreParameters;
import org.bouncycastle2.x509.X509StoreSpi;

import java.util.Collection;

public class X509StoreCertCollection
    extends X509StoreSpi
{
    private CollectionStore _store;

    public X509StoreCertCollection()
    {
    }

    public void engineInit(X509StoreParameters params)
    {
        if (!(params instanceof X509CollectionStoreParameters))
        {
            throw new IllegalArgumentException(params.toString());
        }

        _store = new CollectionStore(((X509CollectionStoreParameters)params).getCollection());
    }

    public Collection engineGetMatches(Selector selector)
    {
        return _store.getMatches(selector);
    }
}
