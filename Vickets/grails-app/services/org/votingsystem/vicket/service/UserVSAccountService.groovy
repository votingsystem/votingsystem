package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.votingsystem.model.UserVS
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.WalletVS


/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class UserVSAccountService {

    private static final CLASS_NAME = UserVSAccountService.class.getSimpleName()

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
            userAccounts.add(getUserVSAccountMap(it))
        }
    }

    Map getUserVSAccountMap(UserVSAccount userVSAccount) {
        Map result = [id:userVSAccount.id, currency:userVSAccount.currencyCode, IBAN:userVSAccount.IBAN,
                      amount:userVSAccount.balance, lastUpdated:userVSAccount.lastUpdated]
        if(userVSAccount.tag) result.tag = [id:userVSAccount.tag.id, name:userVSAccount.tag.name]
        return result
    }

    Map getAccountsBalanceMap(UserVS userVS) {
        List<UserVSAccount> userVSAccounts
        userVSAccounts = UserVSAccount.findAllWhere(userVS:userVS, state:UserVSAccount.State.ACTIVE)
        Map result = [:]
        for(UserVSAccount account: userVSAccounts) {
            if(result[(account.IBAN)]) {
                if(result[(account.IBAN)][(account.currencyCode)]) {
                    result[(account.IBAN)][(account.currencyCode)][(account.tag.name)] = account.balance.toString()
                } else {
                    result[(account.IBAN)][(account.currencyCode)] = [(account.tag.name):account.balance.toString()]
                }
            } else  {
                result[(account.IBAN)] = [(account.currencyCode):[(account.tag.name):account.balance.toString()]]
            }
        }
        return result;
    }

    public void checkBalanceMap(UserVS userVS, Map balancesMap) {

    }

}

