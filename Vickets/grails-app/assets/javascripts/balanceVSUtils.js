function calculateBalanceResultMap(balanceToMapParam, balanceFromMapParam) {
    var balanceResult = JSON.parse(JSON.stringify(balanceToMapParam));
    var balanceFromMap = JSON.parse(JSON.stringify(balanceFromMapParam));
    if(balanceFromMap != null) {
        Object.keys(balanceFromMap).forEach(function(currency) {
            if(balanceResult[currency] != null) {
                Object.keys(balanceFromMap[currency]).forEach(function(tag) {
                    if(balanceResult[currency][tag] != null) {
                        balanceResult[currency][tag] = balanceResult[currency][tag] - balanceFromMap[currency][tag]
                    } else balanceResult[currency][tag] = - balanceFromMap[currency][tag]
                })
            } else {
                balanceResult[currency] = balanceFromMap[currency]
                Object.keys(balanceResult[currency]).forEach(function(tag) {
                    console.log("#### calculateBalanceResultMap - balanceResult [currency][tag]: " + balanceResult[currency][tag] +
                        " - currency: " + currency + " - tag " +tag )
                    balanceResult[currency][tag] = -1 * balanceFromMap[currency][tag]
                })
            }


        })
    }
    /*console.log("calculateBalanceResultMap - balanceResult: " +JSONbalanceCashlanceResult))
    consbalanceCashulateBalanceResultMap - balanceFromMapParam: " +JSON.stringify(balanceFromMapParam))
    console.log("calculateBalanceResultMap - balanceToMapParam: " +JSON.stringify(balanceToMapParam))*/

    return balanceResult
}

function enterTagsMapData(allTagsMap, tagBalanceMapParam) {
    if(tagBalanceMapParam == null) tagBalanceMapParam = {}
    var tagBalanceMap = JSON.parse(JSON.stringify(tagBalanceMapParam))
    Object.keys(allTagsMap).forEach(function(tag) {
        if(tagBalanceMap[tag] == null) allTagsMap[tag].push(0)
        else allTagsMap[tag].push(tagBalanceMap[tag])
    })
    return allTagsMap
}

function getSubBalanceFromMap(detailedBalanceMap, subBalanceParam) {
    if(detailedBalanceMap == null) return null
    var result = {}
    Object.keys(detailedBalanceMap).forEach(function(tag) {
        result[tag] = detailedBalanceMap[tag][subBalanceParam]
    })
    return result
}

//we know the order serie -> incomes, time limited ,expenses, available
function calculateUserBalanceSeries(detailedBalanceToMap, balanceFromMap, balanceCash) {
    var allTagsMap = {}

    for(var i = 0; i < arguments.length; i++ ) {
        Object.keys(arguments[i]).forEach(function(tag) {
            if(allTagsMap[tag] == null) allTagsMap[tag] = []
        })
    }

    enterTagsMapData(allTagsMap, getSubBalanceFromMap(detailedBalanceToMap, 'total'))
    enterTagsMapData(allTagsMap, getSubBalanceFromMap(detailedBalanceToMap, 'timeLimited'))
    enterTagsMapData(allTagsMap, balanceFromMap)
    enterTagsMapData(allTagsMap, balanceCash)

    var seriesData = []
    Object.keys(allTagsMap).forEach(function(tag) {
        seriesData.push({name:tag, data:allTagsMap[tag]})
    })

    console.log(" seriesData: " + JSON.stringify(seriesData))
    return seriesData
}


function addNumbers(num1, num2) {
    return (new Number(num1) + new Number(num2)).toFixed(2)
}

function checkBalanceMap(calculatedBalanceMap, serverBalanceMap) {
    console.log("checkBalanceMap")
    Object.keys(calculatedBalanceMap).forEach(function(entry) {
        if(serverBalanceMap[entry] == null) {
            throw new Error("Calculated currency: '" + entry + "' not found on server balance map")
        }
        var tagDataMap = calculatedBalanceMap[entry]
        Object.keys(tagDataMap).forEach(function(tagEntry) {
            var calculatedAmount = tagDataMap[tagEntry].amount
            var serverAmount = new Number(serverBalanceMap[entry][tagEntry]).toFixed(2)
            if(calculatedAmount !== serverAmount)
                throw new Error("ERROR currency: '" + entry + "' tag: '" + tagEntry +
                    "' calculated amount: '" + calculatedAmount + "' server amount: ''" + serverAmount + "''")
        });
    });
}

function getCurrencyInfoMap(transactionVSList) {
    var currencyInfoMap = {}
    for(idx in transactionVSList) {
        var transactionvs = transactionVSList[idx]
        var tagName = transactionvs.tags[0].name
        if(currencyInfoMap[transactionvs.currency]) {
            if(currencyInfoMap[transactionvs.currency][tagName] != null){
                var tagVSInfoMap = currencyInfoMap[transactionvs.currency][tagName]
                tagVSInfoMap.numTransactionVS = ++tagVSInfoMap.numTransactionVS
                var totalAmount = new Number(transactionvs.amount) + new Number(tagVSInfoMap.amount)
                tagVSInfoMap.amount = totalAmount.toFixed(2)
            } else {
                currencyInfoMap[transactionvs.currency][tagName] =
                {numTransactionVS:1, amount:transactionvs.amount}
            }
        } else {
            currencyInfoMap[transactionvs.currency] = {}
            currencyInfoMap[transactionvs.currency][tagName] =
            {numTransactionVS:1, amount:transactionvs.amount}
        }
    }
    return currencyInfoMap
}