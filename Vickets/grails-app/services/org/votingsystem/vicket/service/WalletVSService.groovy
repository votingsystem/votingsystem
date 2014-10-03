package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.ExceptionVS
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.model.VicketTagVS
import org.votingsystem.vicket.util.WalletVS

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class WalletVSService {

    private static final CLASS_NAME = WalletVSService.class.getSimpleName()

	def grailsApplication
	def messageSource
    def systemService

	public void init() { }

    @Transactional
    public WalletVS getWalletVSForTransactionVS(String fromUserIBAN, VicketTagVS tag, String currencyCode) {
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
    public ResponseVS<Map<UserVSAccount, BigDecimal>> getAccountMovementsForTransaction(String fromUserIBAN,
            VicketTagVS tag, BigDecimal amount, String currencyCode) {
        WalletVS transactionWallet = getWalletVSForTransactionVS(fromUserIBAN, tag, currencyCode)
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements =
                transactionWallet.getAccountMovementsForTransaction(tag, amount, currencyCode)
        return accountFromMovements
    }

}

