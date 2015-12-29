package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.EventBusService;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.Wallet;

import java.util.Set;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toSet;

public class CurrencyDeleteTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(CurrencyDeleteTask.class.getSimpleName());

    private char[] password;
    private OperationVS operationVS;

    public CurrencyDeleteTask(OperationVS operationVS, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
    }

    @Override protected ResponseVS call() throws Exception {
        log.info("deleteCurrency");
        Set<Currency> wallet = Wallet.getWallet(password);
        wallet = wallet.stream().filter(currency -> {
            if (currency.getHashCertVS().equals(operationVS.getMessage())) {
                log.info("deleted currency with hashCertVS: " + operationVS.getMessage());
                return false;
            } else return true;
        }).collect(toSet());
        Wallet.saveWallet(CurrencyDto.serializeCollection(wallet), password);
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK).setType(TypeVS.CURRENCY_DELETE);
        EventBusService.getInstance().post(responseVS);
        operationVS.processResult(responseVS);
        return responseVS;
    }
}
