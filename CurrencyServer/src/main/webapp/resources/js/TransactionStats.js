function TransactionsStats() {
    this.tags = []
    this.transactionTypes = []
    this.currencyCodes = []
    this.transactionsData = {}
    this.transactionsTreeByType = {name:'transactionsTreeByType', exchangeCurrency:TransactionsStats.EXCHANGE_CURRENCY, children:[]}
    this.transactionsTreeByTag = {name:'transactionsTreeByTag', exchangeCurrency:TransactionsStats.EXCHANGE_CURRENCY, children:[]}
    this.emptyTransNode = {timeLimitedAmount: 0, totalAmount:0, numTimeLimitedTransactions:0, numTotalTransactions:0}

    this.transactionTypeFilter = []
    this.transactionTimeFilter = []
    this.transactionTagFilter = []
    this.transactionCurrencyFilter = []
}

TransactionsStats.colors = ["#3366cc", "#dc3912", "#ff9900", "#109618", "#990099", "#0099c6",
    "#dd4477", "#66aa00", "#b82e2e", "#316395", "#994499", "#22aa99", "#aaaa11", "#6633cc", "#e67300",
    "#8b0707", "#651067", "#329262", "#5574a6", "#3b3eac"]

TransactionsStats.colorsDomain = Object.keys(transactionsMap).concat(TransactionsStats.currencyCodes)

TransactionsStats.colorsScale
TransactionsStats.getColorScale = function(name) {
    if(!TransactionsStats.colorsScale) {
        TransactionsStats.colorsScale = d3.scale.ordinal().range(TransactionsStats.colors);
        TransactionsStats.colorsScale.domain(TransactionsStats.colorsDomain)
    }
    return TransactionsStats.colorsScale(name)
}

TransactionsStats.prototype.checkFilters = function(transactionvs) {
    var timeType = transactionvs.timeLimited ? 'timeLimited':'timeFree'
    transactionvs.filtered = false
    if(this.transactionTimeFilter.indexOf(timeType) > -1) transactionvs.filtered = true
    else if(this.transactionTypeFilter.indexOf(transactionvs.type) > -1) transactionvs.filtered = true
    else if(this.transactionTagFilter.indexOf(transactionvs.tags[0]) > -1) transactionvs.filtered = true
    else if(this.transactionCurrencyFilter.indexOf(transactionvs.currencyCode) > -1) transactionvs.filtered = true
    return transactionvs
}

TransactionsStats.prototype.pushTransactionForTreeByTag = function(transactionvs) {
    var currencyNode //first level node
    var tagNode //second level node
    var typeNode //third level node
    this.transactionsTreeByTag.children.forEach(function(firstLevelNode) {
        if(firstLevelNode.name === transactionvs.currencyCode) {
            currencyNode = firstLevelNode
            addTransactionDataToNode(currencyNode, transactionvs)
            currencyNode.children.forEach(function(secondLevelNode) {
                if(secondLevelNode.name === transactionvs.tags[0]) {
                    tagNode = secondLevelNode
                    addTransactionDataToNode(tagNode, transactionvs)
                    tagNode.children.forEach(function(thirdLevelNode) {
                        if(thirdLevelNode.name === transactionvs.type) {
                            typeNode  = thirdLevelNode
                            addTransactionDataToNode(typeNode, transactionvs)
                        }
                    })
                }
            })
        }
    })
    if(!currencyNode) {
        currencyNode = emptyNode(transactionvs.currencyCode, transactionvs)
        currencyNode.currencyCode = transactionvs.currencyCode
        tagNode = emptyNode(transactionvs.tags[0], transactionvs)
        currencyNode.children = [tagNode]
        typeNode = emptyNode(transactionvs.type, transactionvs)
        tagNode.children = [typeNode]
        this.transactionsTreeByTag.children.push(currencyNode)
    }
    if(!tagNode) {
        tagNode = emptyNode(transactionvs.tags[0], transactionvs)
        currencyNode.children.push(tagNode)
    }
    if(!typeNode) {
        typeNode = emptyNode(transactionvs.type, transactionvs)
        tagNode.children.push(typeNode)
    }
    typeNode.description = transactionsMap[transactionvs.type].lbl
}

TransactionsStats.prototype.pushTransactionForTreeByType = function(transactionvs) {
    var currencyNode //first level node
    var typeNode //second level node
    var tagNode //third level node
    this.transactionsTreeByType.children.forEach(function(firstLevelNode) {
        if(firstLevelNode.name === transactionvs.currencyCode) {
            currencyNode = firstLevelNode
            addTransactionDataToNode(currencyNode, transactionvs)
            currencyNode.children.forEach(function(secondLevelNode) {
                if(secondLevelNode.name === transactionvs.type) {
                    typeNode = secondLevelNode
                    addTransactionDataToNode(typeNode, transactionvs)
                    typeNode.children.forEach(function(thirdLevelNode) {
                        if(thirdLevelNode.name === transactionvs.tags[0]) {
                            tagNode = thirdLevelNode
                            addTransactionDataToNode(tagNode, transactionvs)
                        }
                    })
                }
            })
        }
    })
    if(!currencyNode) {
        currencyNode = emptyNode(transactionvs.currencyCode, transactionvs)
        currencyNode.currencyCode = transactionvs.currencyCode
        typeNode = emptyNode(transactionvs.type, transactionvs)
        currencyNode.children = [typeNode]
        tagNode = emptyNode(transactionvs.tags[0], transactionvs)
        typeNode.children = [tagNode]
        this.transactionsTreeByType.children.push(currencyNode)
    }
    if(!typeNode) {
        typeNode = emptyNode(transactionvs.type, transactionvs)
        currencyNode.children.push(typeNode)
    }
    if(!tagNode) {
        tagNode = emptyNode(transactionvs.tags[0], transactionvs)
        typeNode.children.push(tagNode)
    }
    typeNode.description = transactionsMap[transactionvs.type].lbl
}

function emptyNode(name, transactionvs) {
    var result= {name:name, totalAmount:transactionvs.amount, numTotalTransactions:1,
        timeLimitedAmount:0, numTimeLimitedTransactions:0, children:[]}
    if(transactionvs.timeLimited) {
        result.timeLimitedAmount += transactionvs.amount
        result.numTimeLimitedTransactions += 1
    }
    return result
}

function addTransactionDataToNode(node, transactionvs){
    node.totalAmount +=  transactionvs.amount
    node.numTotalTransactions +=1
    if(transactionvs.timeLimited) {
        node.timeLimitedAmount += transactionvs.amount
        node.numTimeLimitedTransactions += 1
    }
}


TransactionsStats.prototype.pushTransaction = function(transactionvs, orderBy) {
    if(this.tags.indexOf(transactionvs.tags[0]) < 0) this.tags.push(transactionvs.tags[0])
    if(this.currencyCodes.indexOf(transactionvs.currencyCode) < 0) this.currencyCodes.push(transactionvs.currencyCode)
    if(this.transactionTypes.indexOf(transactionvs.type) < 0) this.transactionTypes.push(transactionvs.type)
    if(orderBy === "orderByType") this.pushTransactionForTreeByType(transactionvs)
    if(orderBy === "orderByTag") this.pushTransactionForTreeByTag(transactionvs)
}

TransactionsStats.prototype.setCurrencyPercentages = function () {
    TransactionsStats.setCurrencyPercentages(this.transactionsTreeByType)
    TransactionsStats.setCurrencyPercentages(this.transactionsTreeByTag)
}

//TODO
TransactionsStats.currencyCodes = ["EUR", "USD", "CNY", "JPY"]
TransactionsStats.USD_TO_EUR = 0.910856
TransactionsStats.CNY_TO_EUR = 0.141062
TransactionsStats.JPY_TO_EUR = 0.00753358
TransactionsStats.EXCHANGE_CURRENCY = "EUR"

TransactionsStats.setCurrencyPercentages = function(transactionsTree) {
    if(!transactionsTree.children) {
        console.log("TransactionsStats.js - transactionsTree without children");
        return;
    }
    var conversorMap = {EUR:{amount:0, percentage:0}, USD:{amount:0, percentage:0}, CNY:{amount:0, percentage:0},
        JPY:{amount:0, percentage:0}}
    var currenciesTotalAmount = 0
    transactionsTree.children.forEach(function(currencyNode) {
        if(currencyNode.currencyCode !== 'EUR') {
            conversorMap[currencyNode.currencyCode].amount = TransactionsStats[currencyNode.currencyCode + "_TO_EUR"] * currencyNode.totalAmount
        } else conversorMap.EUR.amount = currencyNode.totalAmount
        currenciesTotalAmount += conversorMap[currencyNode.currencyCode].amount
    })
    Object.keys(conversorMap).forEach(function(key) {
        conversorMap[key].percentage = TransactionsStats.getPercentage(conversorMap[key].amount, currenciesTotalAmount)
    })
    transactionsTree.children.forEach(function(currencyNode) {
        currencyNode.percentage = conversorMap[currencyNode.currencyCode].percentage
    })
}

TransactionsStats.getPercentage = function (value, total) {
    return (100 * value / total).toPrecision(3);
}