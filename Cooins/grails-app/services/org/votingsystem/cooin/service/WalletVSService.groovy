package org.votingsystem.cooin.service

import grails.transaction.Transactional
import org.votingsystem.model.TagVS
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.cooin.model.CooinAccount
import org.votingsystem.cooin.util.WalletVS

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class WalletVSService {

    def systemService

    public WalletVS getWalletVS(String userIBAN, TagVS tag, String currencyCode) {
        List accountList = []
        CooinAccount wildTagAccount = CooinAccount.findWhere(IBAN:userIBAN, currencyCode: currencyCode,
                tag:systemService.getWildTag())
        if(wildTagAccount) accountList.add(wildTagAccount)
        if(tag) {
            def tagAccount = CooinAccount.findWhere(IBAN:userIBAN, currencyCode: currencyCode, tag:tag)
            if(tagAccount) accountList.add(tagAccount)
        }
        if(accountList.isEmpty()) throw new ExceptionVS(
                "No accounts for IBAN: '$userIBAN' - tag: '$tag.name' - currencyCode: '$currencyCode'")
        else return new WalletVS(accountList, currencyCode)
    }

    public Map<CooinAccount, BigDecimal> getAccountMovementsForTransaction(String fromUserIBAN,
            TagVS tag, BigDecimal amount, String currencyCode) {
        WalletVS transactionWallet = getWalletVS(fromUserIBAN, tag, currencyCode)
        return transactionWallet.getAccountMovementsForTransaction(tag, amount, currencyCode)
    }

}

