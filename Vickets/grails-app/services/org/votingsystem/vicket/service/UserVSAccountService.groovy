package org.votingsystem.vicket.service

import org.votingsystem.model.UserVS
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.model.WalletVS


/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class UserVSAccountService {
	
	static transactional = true

	def grailsApplication
	def messageSource


    WalletVS getUserVSWallet(UserVS user) {
        def userAccountsDB
        UserVSAccount.withTransaction { userAccountsDB = UserVSAccount.createCriteria().list(sort:'dateCreated', order:'asc') {
            eq("userVS", userVS)
            eq("state", UserVSAccount.State.ACTIVE)
        }}
        WalletVS walletVS = new WalletVS()
        List userAccounts = []
        userAccountsDB.each { it ->
            userAccounts.add(userVSAccountService.getUserVSAccountMap(it))
        }
    }

    Map getUserVSAccountMap(UserVSAccount userVSAccount) {
        Map result = [id:userVSAccount.id, currency:userVSAccount.currencyCode, IBAN:userVSAccount.IBAN,
                      amount:userVSAccount.balance, lastUpdated:userVSAccount.lastUpdated]
        if(userVSAccount.tag) result.tag = [id:userVSAccount.tag.id, name:userVSAccount.tag.name]
        return result
    }

}

