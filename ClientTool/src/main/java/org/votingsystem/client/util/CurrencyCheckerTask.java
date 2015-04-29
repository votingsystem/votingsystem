package org.votingsystem.client.util;

import com.fasterxml.jackson.core.type.TypeReference;
import javafx.concurrent.Task;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.util.*;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toSet;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyCheckerTask extends Task<CurrencyCheckResponse> {

    private static Logger log = Logger.getLogger(CurrencyCheckerTask.class.getSimpleName());

    public interface Listener {
        public void processCurrencyStatus(CurrencyCheckResponse response);
    }

    private Set<Currency> currencySet;
    private Listener listener;

    public CurrencyCheckerTask(Set<Currency> currencySet, Listener listener) {
        this.currencySet = currencySet;
        this.listener = listener;
    }

    @Override protected CurrencyCheckResponse call() throws Exception {
        try {
            updateMessage(ContextVS.getMessage("checkingCurrencyMsg"));
            ResponseVS responseVS = Utils.checkServer(currencySet.iterator().next().getCurrencyServerURL());
            CurrencyCheckResponse response;
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                response =  CurrencyCheckResponse.load(responseVS);
                if(listener != null) listener.processCurrencyStatus(response);
                return response;
            }
            CurrencyServer currencyServer = (CurrencyServer) responseVS.getData();
            Set<String> hashCertSet = currencySet.stream().map(currency -> {return currency.getHashCertVS();}).collect(toSet());
            List requestList = new ArrayList<>();
            requestList.addAll(hashCertSet);
            responseVS = HttpHelper.getInstance().sendData(requestList.toString().getBytes(),
                    ContentTypeVS.JSON, currencyServer.getCurrencyBundleStateServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Map<String, Currency.State> result = (Map<String, Currency.State>) responseVS.getMessage(
                        new TypeReference<Map<String, Currency.State>>() {});
                Set<String> errorSet = new HashSet<>();
                Set<String> OKSet = new HashSet<>();
                for(String hashCertVS : result.keySet()) {
                    if(Currency.State.OK == result.get(hashCertVS)) OKSet.add(hashCertVS);
                    else errorSet.add(hashCertVS);
                }
                Integer statusCode = errorSet.size() > 0? ResponseVS.SC_ERROR : ResponseVS.SC_OK;
                response = new CurrencyCheckResponse(statusCode, null, OKSet, errorSet);
            } else response = CurrencyCheckResponse.load(responseVS);
            if(listener != null) listener.processCurrencyStatus(response);
            return response;
        } catch (Exception ex) {
            ex.printStackTrace();
            return CurrencyCheckResponse.load(new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage()));
        }
    }
}
