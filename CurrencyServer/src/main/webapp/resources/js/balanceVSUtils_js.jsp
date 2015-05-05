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
        var colors = Highcharts.getOptions().colors
        for(idx in seriesData) {
            var tagData = seriesData[idx]
            result.push({
                name: tagData.name,
                y: tagData.data[0],
                color: colors[idx]
            });
        }
        return result
    }

    function tagDataIncomesDetailsForChartDonut(seriesData) {
        var result = []
        var colors = Highcharts.getOptions().colors
        for(idx in seriesData) {
            var tagData = seriesData[idx]
            var brightness = 0.1 - (idx / 2) / 5;
            //add expense
            result.push({
                name: '${msg.timeLimitedLbl}',
                tag:tagData.name,
                y: tagData.data[2],
                color: Highcharts.Color(colors[idx]).brighten(brightness).get()
            });
            //add remaining to total
            result.push({
                name: '${msg.timeFreeLbl}',
                tag:tagData.name,
                y: tagData.data[0] - tagData.data[2],
                color: colors[idx]
            });
        }
        return result
    }

    function tagDataExpensesForChartDonut(seriesData) {
        var result = []
        var colors = Highcharts.getOptions().colors
        var wildTagAvailable = null
        for(idx in seriesData) {
            var tagData = seriesData[idx]
            //add timelimited
            result.push({
                name: '${msg.expendedLbl}',
                tag:tagData.name,
                y: tagData.data[1],
                color: 'red'
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
                y: available,
                color: Highcharts.Color(colors[idx]).brighten(0.2).get()
            });
        }
        result.forEach(function(data) {
            if('WILDTAG' === data.tag && data.name === '${msg.availableLbl}') {
                data.y = wildTagAvailable
            }
        })
        return result
    }

    function checkBalanceMap(transactionVSListBalanceMap, serverBalanceMap) {
        console.log("checkBalanceMap")
        Object.keys(transactionVSListBalanceMap).forEach(function(entry) {
            if(serverBalanceMap[entry] == null) {
                throw new Error("Calculated currency: '" + entry + "' not found on server balance map")
            }
            var tagDataMap = transactionVSListBalanceMap[entry]
            Object.keys(tagDataMap).forEach(function(tagEntry) {
                var calculatedAmount = tagDataMap[tagEntry].amount
                var amount = (serverBalanceMap[entry][tagEntry].total)?serverBalanceMap[entry][tagEntry].total:
                        serverBalanceMap[entry][tagEntry]
                var serverAmount = new Number(amount).toFixed(2)
                if(calculatedAmount !== serverAmount)
                    throw new Error("ERROR currency: '" + entry + "' tag: '" + tagEntry +
                            "' calculated amount: '" + calculatedAmount + "' server amount: ''" + serverAmount + "''")
            });
        });
    }

    function getCurrencyMap(transactionVSList) {
        var currencyInfoMap = {}
        for(idx in transactionVSList) {
            var transactionvs = transactionVSList[idx]
            var tagName = transactionvs.tags[0]
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