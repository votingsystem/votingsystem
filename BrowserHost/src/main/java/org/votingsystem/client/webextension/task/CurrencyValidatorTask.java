package org.votingsystem.client.webextension.task;

import com.fasterxml.jackson.core.type.TypeReference;
import javafx.concurrent.Task;
import org.votingsystem.client.webextension.util.CurrencyCheckResponse;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.*;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toSet;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyValidatorTask extends Task<CurrencyCheckResponse> {

    private static Logger log = Logger.getLogger(CurrencyValidatorTask.class.getSimpleName());

    public interface Listener {
        public void processCurrencyStatus(CurrencyCheckResponse response);
    }

    private Set<Currency> currencySet;
    private Listener listener;

    public CurrencyValidatorTask(Set<Currency> currencySet, Listener listener) {
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
            responseVS = HttpHelper.getInstance().sendData(JSON.getMapper().writeValueAsBytes(requestList),
                    ContentTypeVS.JSON, currencyServer.getCurrencyBundleStateServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Map<String, CurrencyStateDto> result = (Map<String, CurrencyStateDto>) responseVS.getMessage(
                        new TypeReference<Map<String, CurrencyStateDto>>() {});
                Set<String> errorSet = new HashSet<>();
                Set<String> OKSet = new HashSet<>();
                for(String hashCertVS : result.keySet()) {
                    if(Currency.State.OK == result.get(hashCertVS).getState()) OKSet.add(hashCertVS);
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
