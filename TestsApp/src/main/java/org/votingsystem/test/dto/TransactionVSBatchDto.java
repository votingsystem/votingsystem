package org.votingsystem.test.dto;

import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.TimePeriod;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSBatchDto {

    private static Logger log = Logger.getLogger(TransactionVSBatchDto.class.getSimpleName());

    private TimePeriod timePeriod;
    private UserVSTransactionBatchDto systemVSBatch;
    private List<UserVSTransactionBatchDto> groupVSListBatch;
    private List<UserVSTransactionBatchDto> userVSListBatch;

    public TransactionVSBatchDto() { }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public Map<String, ReportDto> getReport(UserVS.Type userType) throws ExceptionVS {
        Map<String, ReportDto> result = null;
        List<UserVSTransactionBatchDto> batchList = null;
        switch(userType) {
            case USER:
                batchList = userVSListBatch;
                break;
            case GROUP:
                batchList = groupVSListBatch;
                break;
        }
        //TransactionVSUtils.sumReport(result, transactionBatch.getReport()
        return result;
    }

}