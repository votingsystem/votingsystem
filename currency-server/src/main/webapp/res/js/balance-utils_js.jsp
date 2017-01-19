<%@page contentType="text/javascript; charset=UTF-8" %>

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
        return balanceResult
    }

    function pushTagData(tagDataMap, balanceMap) {
        if(balanceMap == null) balanceMap = {}
        var tagBalanceMap = toJSON(balanceMap)
        Object.keys(tagDataMap).forEach(function(tag) {
            if(tagBalanceMap[tag] == null) tagDataMap[tag].push(0)
            else tagDataMap[tag].push(parseFloat(tagBalanceMap[tag]))
        })
        return tagDataMap
    }

    function filterMap(balanceToMap, param) {
        if(balanceToMap == null) return null
        var result = {}
        Object.keys(balanceToMap).forEach(function(tag) {
            result[tag] = balanceToMap[tag][param]
        })
        return result
    }

    //we know the order serie -> incomes ,expenses, time limited, available
    //[{"name":"WILDTAG","data":[833.33,110,833.33,723.33]}]
    function calculateUserBalanceSeries(balanceToMap, balanceFromMap, balanceCash) {
        var tagDataMap = {}

        for(var i = 0; i < arguments.length; i++ ) {
            Object.keys(arguments[i]).forEach(function(tag) {
                if(tagDataMap[tag] == null) tagDataMap[tag] = []
            })
        }

        pushTagData(tagDataMap, filterMap(balanceToMap, 'total'))
        pushTagData(tagDataMap, balanceFromMap)
        pushTagData(tagDataMap, filterMap(balanceToMap, 'timeLimited'))
        pushTagData(tagDataMap, balanceCash)

        var seriesData = []
        Object.keys(tagDataMap).forEach(function(tag) {
            seriesData.push({name:tag, data:tagDataMap[tag]})
        })

        console.log(" seriesData: " + JSON.stringify(seriesData))
        return seriesData
    }

    function calculateUserBalanceSeriesDonut(balanceToMap, balanceFromMap, balanceCash) {
        var seriesData = calculateUserBalanceSeries(balanceToMap, balanceFromMap, balanceCash)

        var result = []
        result.push(tagDataForChartDonut(seriesData))
        result.push(tagDataIncomesDetailsForChartDonut(seriesData))
        result.push(tagDataExpensesForChartDonut(seriesData))

        return result
    }

    function tagDataForChartDonut(seriesData) {
        var result = []
        for(idx in seriesData) {
            var tagData = seriesData[idx]
            result.push({
                name: tagData.name,
                y: tagData.data[0],
            });
        }
        return result
    }

    function tagDataIncomesDetailsForChartDonut(seriesData) {
        var result = []
        for(idx in seriesData) {
            var tagData = seriesData[idx]
            var brightness = 0.1 - (idx / 2) / 5;
            //add expense
            result.push({
                name: '${msg.timeLimitedLbl}',
                tag:tagData.name,
                y: tagData.data[2],
            });
            //add remaining to total
            result.push({
                name: '${msg.timeFreeLbl}',
                tag:tagData.name,
                y: tagData.data[0] - tagData.data[2],
            });
        }
        return result
    }

    function tagDataExpensesForChartDonut(seriesData) {
        var result = []
        var wildTagAvailable = null
        for(idx in seriesData) {
            var tagData = seriesData[idx]
            //add timelimited
            result.push({
                name: '${msg.expendedLbl}',
                tag:tagData.name,
                y: tagData.data[1]
            });
            //check availbale
            var available = tagData.data[0] - tagData.data[1]
            if('WILDTAG' === tagData.name) {
                if(wildTagAvailable === null) wildTagAvailable = available
                else wildTagAvailable = wildTagAvailable + available
            } else if(available < 0){
                if(wildTagAvailable === null) wildTagAvailable = available
                else wildTagAvailable = wildTagAvailable + available
                available = 0
            }
            result.push({
                name: '${msg.availableLbl}',
                tag:tagData.name,
                y: available
            });
        }
        result.forEach(function(data) {
            if('WILDTAG' === data.tag && data.name === '${msg.availableLbl}') {
                data.y = wildTagAvailable
            }
        })
        return result
    }

    function checkBalanceMap(transactionListBalanceMap, serverBalanceMap) {
        console.log("checkBalanceMap")
        Object.keys(transactionListBalanceMap).forEach(function(entry) {
            if(serverBalanceMap[entry] == null) {
                throw new Error("Calculated currency: '" + entry + "' not found on server balance map")
            }
            var tagDataMap = transactionListBalanceMap[entry]
            Object.keys(tagDataMap).forEach(function(tagEntry) {
                var calculatedAmount = tagDataMap[tagEntry].amount
                var amount = (serverBalanceMap[entry][tagEntry].total)?serverBalanceMap[entry][tagEntry].total:
                        serverBalanceMap[entry][tagEntry]
                var serverAmount = new Number(amount).toAmountStr()
                if(calculatedAmount !== serverAmount)
                    throw new Error("ERROR currency: '" + entry + "' tag: '" + tagEntry +
                            "' calculated amount: '" + calculatedAmount + "' server amount: ''" + serverAmount + "''")
            });
        });
    }

    function getCurrencyMap(transactionList) {
        var currencyInfoMap = {}
        for(idx in transactionList) {
            var transaction = transactionList[idx]
            var tagName = transaction.tags[0]
            if(currencyInfoMap[transaction.currency]) {
                if(currencyInfoMap[transaction.currency][tagName] != null){
                    var tagVSInfoMap = currencyInfoMap[transaction.currency][tagName]
                    tagVSInfoMap.numTransaction = ++tagVSInfoMap.numTransaction
                    var totalAmount = new Number(transaction.amount) + new Number(tagVSInfoMap.amount)
                    tagVSInfoMap.amount = totalAmount.toAmountStr()
                } else {
                    currencyInfoMap[transaction.currency][tagName] =
                    {numTransaction:1, amount:transaction.amount}
                }
            } else {
                currencyInfoMap[transaction.currency] = {}
                currencyInfoMap[transaction.currency][tagName] =
                {numTransaction:1, amount:transaction.amount}
            }
        }
        return currencyInfoMap
    }