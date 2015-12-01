function TransactionsStats() {
    this.tags = []
    this.transactionTypes = []
    this.transactionsData = {}
}

TransactionsStats.prototype.pushTransaction = function(transactionvs) {
    var emptyTransNode = {timeLimitedAmount: 0, totalAmount:0, numTimeLimitedTransactions:0, numTotalTransactions:0}
    if(this.tags.indexOf(transactionvs.tags[0]) < 0) this.tags.push(transactionvs.tags[0])
    if(this.transactionTypes.indexOf(transactionvs.type) < 0) this.transactionTypes.push(transactionvs.type)
    if(this.transactionsData[transactionvs.currencyCode]) {
        this.transactionsData[transactionvs.currencyCode].stats = getTransData(transactionvs, this.transactionsData[transactionvs.currencyCode].stats)
        if(this.transactionsData[transactionvs.currencyCode].transactions[transactionvs.type]) {
            this.transactionsData[transactionvs.currencyCode].transactions[transactionvs.type] =
                getTransData(transactionvs, this.transactionsData[transactionvs.currencyCode].transactions[transactionvs.type])
        } else {
            this.transactionsData[transactionvs.currencyCode].transactions[transactionvs.type] =
                getTransData(transactionvs, emptyTransNode)
        }
    } else {
        this.transactionsData[transactionvs.currencyCode] = {}
        this.transactionsData[transactionvs.currencyCode].transactions = {}
        this.transactionsData[transactionvs.currencyCode].stats = getTransData(transactionvs, emptyTransNode)
        this.transactionsData[transactionvs.currencyCode].transactions[transactionvs.type] = getTransData(transactionvs, emptyTransNode)
    }
}

TransactionsStats.prototype.getTransactionTypesData = function() {
    var result = []
    this.transactionTypes.forEach(function(type) {
        result.push({type:type})
    })
    return result
}

function getTransData(transactionvs, transNode) {
    if(transactionvs.timeLimited) {
        return {timeLimitedAmount: transactionvs.amount + transNode.timeLimitedAmount,
            totalAmount:transactionvs.amount + transNode.totalAmount,
            numTimeLimitedTransactions:transNode.numTimeLimitedTransactions + 1,
            numTotalTransactions:transNode.numTotalTransactions + 1}
    } else {
        return {timeLimitedAmount: transNode.timeLimitedAmount,
            totalAmount:transactionvs.amount + transNode.totalAmount,
            numTimeLimitedTransactions:transNode.numTimeLimitedTransactions,
            numTotalTransactions:transNode.numTotalTransactions + 1}
    }
}