package org.votingsystem.cooin.service

import grails.transaction.Transactional
import org.votingsystem.cooin.model.TransactionVS
import org.votingsystem.model.TagVS
import org.votingsystem.model.UserVS
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.cooin.model.CooinAccount
import org.votingsystem.cooin.util.WalletVS
import org.votingsystem.util.DateUtils

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class CooinAccountService {

	def grailsApplication
	def messageSource


    WalletVS getUserVSWallet(UserVS user) {
        def userAccountsDB
        CooinAccount.withTransaction { userAccountsDB = CooinAccount.createCriteria().list(sort:'dateCreated', order:'asc') {
            eq("userVS", userVS)
            eq("state", CooinAccount.State.ACTIVE)
        }}
        WalletVS walletVS = new WalletVS()
        List userAccounts = []
        userAccountsDB.each { it ->
            userAccounts.add(getUserVSAccountMap(it))
        }
    }

    Map getUserVSAccountMap(CooinAccount cooinAccount) {
        Map result = [id:cooinAccount.id, currency:cooinAccount.currencyCode, IBAN:cooinAccount.IBAN,
                      amount:cooinAccount.balance, lastUpdated:cooinAccount.lastUpdated]
        if(cooinAccount.tag) result.tag = [id:cooinAccount.tag.id, name:cooinAccount.tag.name]
        return result
    }

    Map getAccountsBalanceMap(UserVS userVS) {
        List<CooinAccount> cooinAccounts
        cooinAccounts = CooinAccount.findAllWhere(userVS:userVS, state:CooinAccount.State.ACTIVE)
        Map result = [:]
        for(CooinAccount account: cooinAccounts) {
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
        if(accountsMap?.values().isEmpty()) return
        accountsMap = accountsMap?.values()?.iterator()?.next()
        for(String currency : accountsMap?.keySet()) {
            BigDecimal tagGap = BigDecimal.ZERO
            BigDecimal wildTagBalance = BigDecimal.ZERO
            if(balancesMap[currency]) {
                for(String tag: accountsMap[currency].keySet()) {
                    BigDecimal tagAccount = new BigDecimal(accountsMap[currency][tag])
                    if(balancesMap[currency][tag]) {
                        BigDecimal tagBalance = balancesMap[currency][tag];
                        if(TagVS.WILDTAG.equals(tag)) wildTagBalance = tagBalance
                        else if(tagAccount.compareTo(tagBalance) != 0)
                            tagGap = tagGap.add(tagBalance.subtract(tagAccount))
                    } else {
                        if(tagAccount.compareTo(BigDecimal.ZERO) != 0) throw new ExceptionVS("Balance Error with user " +
                                "'$userVS.id' - tag '$tag' '$currency' - accounts: '$accountsMap' - balance '$balancesMap'")
                    }
                }
            } else {
                for(String tag: accountsMap[currency].keySet()) {
                    BigDecimal tagAccount = new BigDecimal(accountsMap[currency][tag])
                    if(tagAccount.compareTo(BigDecimal.ZERO) != 0) throw new ExceptionVS("Error with user '${userVS.id}' " +
                            "tag '$tag' '$currency' - accounts: '$accountsMap' - balance '$balancesMap'")
                }
            }
            //check WILDTAG result
            BigDecimal wildTagAccount = new BigDecimal(accountsMap[currency][TagVS.WILDTAG])
            if(wildTagAccount.compareTo(wildTagBalance.subtract(tagGap)) != 0) throw new ExceptionVS(
                    "Balance Error with user " + "'$userVS.id' - '$currency' - accounts: '$accountsMap' - " +
                    "gap  not resolved '$tagGap'")

        }
    }

}

