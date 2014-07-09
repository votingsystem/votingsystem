package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVSAccount
import org.votingsystem.model.VicketTagVS
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.WalletVS

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class WalletVSService {

	static transactional = true

	def grailsApplication
	def messageSource

	public void init() { }

    @Transactional
    public WalletVS getWalletVSForTransactionVS(String fromUserIBAN, VicketTagVS tag, String currencyCode) {
        List accountList = []
        def noTagAccount = UserVSAccount.findWhere(IBAN:fromUserIBAN, currencyCode: currencyCode, tag:null)
        if(noTagAccount) accountList.add(noTagAccount)
        if(tag) {
            def tagAccount = UserVSAccount.findWhere(IBAN:fromUserIBAN, currencyCode: currencyCode, tag:tag)
            if(tagAccount) accountList.add(tagAccount)
        }
        if(accountList.isEmpty()) return null
        else return new WalletVS(accountList, currencyCode)
    }

    @Transactional
    public ResponseVS<Map<UserVSAccount, BigDecimal>> getAccountMovementsForTransaction(String fromUserIBAN,
            VicketTagVS tag, BigDecimal amount, String currencyCode) {
        WalletVS transactionWallet = getWalletVSForTransactionVS(fromUserIBAN, tag, currencyCode)
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements =
                transactionWallet.getAccountMovementsForTransaction(tag, amount)
        return accountFromMovements
    }

}

