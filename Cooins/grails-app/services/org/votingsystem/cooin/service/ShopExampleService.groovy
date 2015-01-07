package org.votingsystem.cooin.service

import grails.transaction.Transactional
import org.votingsystem.cooin.TransactionRequest

@Transactional
class ShopExampleService {

    private static Map<String, TransactionRequest> transactionRequestMap = new HashMap<String, TransactionRequest>();

    public void putTransactionRequest(String urlUUID, TransactionRequest transactionRequest) {
        transactionRequestMap.put(urlUUID, transactionRequest)
    }

    public TransactionRequest getTransactionRequest(String urlUUID) {
        transactionRequestMap.get(urlUUID)
    }

    public Set<String> keySet() {
        return transactionRequestMap.keySet()
    }
}
