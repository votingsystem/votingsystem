package org.votingsystem.cms;

import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.Messages;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.currency.CurrencyStateDto;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyCheckResponse {

    private Integer statusCode;
    private String message;
    private Set<CurrencyStateDto> currencyOKSet;
    private Set<CurrencyStateDto> currencyWithErrorSet;

    public CurrencyCheckResponse(Integer statusCode, String message, Set<CurrencyStateDto> OKSet, Set<CurrencyStateDto> errorSet){
        this.statusCode = statusCode;
        this.message = message;
        this.currencyOKSet = OKSet;
        this.currencyWithErrorSet = errorSet;
    }

    public CurrencyCheckResponse(Integer statusCode, String message){
        this.statusCode = statusCode;
        this.message = message;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Set<CurrencyStateDto> getCurrencyOKSet() {
        return currencyOKSet;
    }

    public Set<CurrencyStateDto> getCurrencyWithErrorSet() {
        return currencyWithErrorSet;
    }

    public Set<String> getHashOKSet() {
        return currencyOKSet.stream().map(currencyStateDto -> currencyStateDto.getRevocationHashBase64()).collect(Collectors.toSet());
    }

    public Set<String> getHashWithErrorSet() {
        return currencyWithErrorSet.stream().map(currencyStateDto -> currencyStateDto.getRevocationHashBase64()).collect(Collectors.toSet());
    }

    public String getMessage() {
        return message;
    }

    public String getErrorMessage() {
        StringBuilder result = new StringBuilder("<div id='msgDiv'>");
        for(CurrencyStateDto currencyStateDto : currencyWithErrorSet) {
            result.append("<div>" + currencyStateDto.getAmount() + " " + currencyStateDto.getCurrencyCode() + " - " +
                    currencyStateDto.getTag() + " - " + getStateDescription (currencyStateDto.getState()) + "</div>");
        }
        return result.append("</div>").toString();
    }

    public static String getStateDescription(Currency.State state) {
        switch (state) {
            case UNKNOWN:
                return Messages.currentInstance().get("currencyUnknownDesc");
            case LAPSED:
                return Messages.currentInstance().get("currencyLapsedErrorLbl");
            case EXPENDED:
                return Messages.currentInstance().get("currencyExpendedDesc");
            default:
                return state.toString().toLowerCase();
        }
    }

    public static CurrencyCheckResponse load(Set<CurrencyStateDto> currencySet, Map<String, Currency> currencyMap)
            throws Exception {
        Set<CurrencyStateDto> currencyWithErrors = new HashSet<>();
        Set<CurrencyStateDto> currencyOKSet = new HashSet<>();
        for(CurrencyStateDto currencyDto: currencySet) {
            switch (currencyDto.getState()) {
                case OK:
                    currencyOKSet.add(currencyDto);
                    break;
                case UNKNOWN:
                    currencyWithErrors.add(new CurrencyStateDto(currencyMap.get(
                            currencyDto.getRevocationHashBase64())).setState(Currency.State.UNKNOWN));
                    break;
                default:
                    currencyWithErrors.add(currencyDto);
            }
        }
        Integer statusCode = currencyWithErrors.isEmpty() ? ResponseDto.SC_OK : ResponseDto.SC_ERROR;
        return new CurrencyCheckResponse(statusCode, null, currencyOKSet, currencyWithErrors);
    }
}
