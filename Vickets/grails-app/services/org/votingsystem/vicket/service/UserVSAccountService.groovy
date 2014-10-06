package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.votingsystem.model.UserVS
import org.votingsystem.util.ExceptionVS
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

    public void checkBalancesMap(UserVS userVS, Map<String, Map> balancesMap) {
        Map<String, Map> accountsMap = getAccountsBalanceMap(userVS)
        if(accountsMap.keySet().size() > 1) throw new ExceptionVS("User '$userVS.id' " +
                "has '${accountsMap.keySet().size()}' accounts")
        accountsMap = accountsMap.values().iterator().next()
        for(String currency : accountsMap.keySet()) {
            if(balancesMap[currency]) {
                for(String tag: accountsMap[currency].keySet()) {
                    BigDecimal tagAmount = new BigDecimal(accountsMap[currency][tag])
                    if(balancesMap[currency][tag]) {
                        BigDecimal balanceTagAmount = new BigDecimal(balancesMap[currency][tag])
                        if(tagAmount.compareTo(balanceTagAmount) != 0) throw new ExceptionVS("Error with tag '$tag' '$currency'" +
                                " - accounts: '$accountsMap' - balance '$balancesMap'")
                    } else {
                        if(tagAmount.compareTo(BigDecimal.ZERO) != 0) throw new ExceptionVS("Error with tag '$tag' '$currency'" +
                                " - accounts: '$accountsMap' - balance '$balancesMap'")
                    }
                }
            } else {
                for(String tag: accountsMap[currency].keySet()) {
                    BigDecimal tagAmount = new BigDecimal(accountsMap[currency][tag])
                    if(tagAmount.compareTo(BigDecimal.ZERO) != 0) throw new ExceptionVS("Error with tag '$tag' '$currency'" +
                            " - accounts: '$accountsMap' - balance '$balancesMap'")
                }
            }
        }
    }

}

