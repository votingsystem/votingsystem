package org.votingsystem.test.dto;

import org.votingsystem.model.UserVS;
import org.votingsystem.test.util.TransactionVSBatch;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserVSTransactionBatchDto {

    public UserVS.Type type;
    public TransactionVSBatch transactionVSFromBatch, transactionVSToBatch;

    public UserVSTransactionBatchDto(UserVS.Type type, TransactionVSBatch transactionVSFromBatch,
                                     TransactionVSBatch transactionVSToBatch) {
        this.transactionVSFromBatch = transactionVSFromBatch;
        this.transactionVSToBatch = transactionVSToBatch;
        this.type = type;
    }

    public TransactionVSBatch getTransactionVSFromBatch() {
        return transactionVSFromBatch;
    }

    public TransactionVSBatch getTransactionVSToBatch() {
        return transactionVSToBatch;
    }

    public UserVS.Type getType() { return type;}

 }