package org.votingsystem.cooin.service

import grails.transaction.Transactional
import org.votingsystem.model.TagVS
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.cooin.model.UserVSAccount
import org.votingsystem.cooin.util.WalletVS

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class WalletVSService {

	def grailsApplication
	def messageSource
    def systemService

	public void init() { }

    @Transactional
    public WalletVS getWalletVSForTransactionVS(String fromUserIBAN, TagVS tag, String currencyCode) {
        List accountList = []
        def wildTagAccount = UserVSAccount.findWhere(IBAN:fromUserIBAN, currencyCode: currencyCode, tag:systemService.getWildTag())
        if(wildTagAccount) accountList.add(wildTagAccount)
        if(tag) {
            def tagAccount = UserVSAccount.findWhere(IBAN:fromUserIBAN, currencyCode: currencyCode, tag:tag)
            if(tagAccount) accountList.add(tagAccount)
        }
        if(accountList.isEmpty()) throw new ExceptionVS(
                "No accounts for IBAN: '$fromUserIBAN' - tag: '$tag.name' - currencyCode: '$currencyCode'")
        else return new WalletVS(accountList, currencyCode)
    }

    @Transactional
    public Map<UserVSAccount, BigDecimal> getAccountMovementsForTransaction(String fromUserIBAN,
            TagVS tag, BigDecimal amount, String currencyCode) {
        WalletVS transactionWallet = getWalletVSForTransactionVS(fromUserIBAN, tag, currencyCode)
        return transactionWallet.getAccountMovementsForTransaction(tag, amount, currencyCode)
    }

}

