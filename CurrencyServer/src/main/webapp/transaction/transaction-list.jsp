<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-user-box/vs-user-box.html" rel="import"/>
<link href="transaction-card.vsp" rel="import"/>
<link href="transaction-selector.vsp" rel="import"/>

<dom-module name="transaction-list">
<template>
    <div class="horizontal layout center center-justified">
        <transaction-selector id="transactionSelector" transaction-type="{{transactionType}}"></transaction-selector>
    </div>
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <div class="layout flex horizontal wrap around-justified">
        <template is="dom-repeat" items="{{transactionsDto.resultList}}" as="transaction">
            <transaction-card transaction="{{transaction}}"></transaction-card>
        </template>
        <div hidden="{{messageToUserHidden}}" style="font-size: 1.3em; margin:30px 0 0 0;">
            ${msg.withoutMovementsLbl}
        </div>
    </div>
</template>
<script>
    Polymer({
        is:'transaction-list',
        properties: {
            transactionsDto: {type:Object, value: {}, observer:'transactionsDtoChanged'},
            url:{type:String, observer:'getHTTP'},
            transactionType:{type:String}
        },
        ready:function() {
            console.log(this.tagName + " - ready")
            this.$.transactionSelector.addEventListener('selected', function (e) {
                var transactionType = e.detail
                var targetURL = vs.contextURL + "/rest/transaction";
                if("" != transactionType) {
                    targetURL = targetURL + "?transactionType=" + transactionType
                }
                var newURL = setURLParameter(window.location.href, "transactionType",  transactionType)
                history.pushState(null, null, newURL);
                console.log(this.tagName + " - targetURL: " + targetURL);
                this.url = targetURL
            }.bind(this))
        },
        transactionsDtoChanged:function() {
            console.log(this.tagName + " - transactionsDtoChanged - transactionsDto: " + this.transactionsDto)
            if(this.transactionsDto.resultList) this.messageToUserHidden = (this.transactionsDto.resultList.length !== 0)
        },
        addTransaction:function(transaction) {
            this.transactionsDto.resultList.push(transaction)
            this.transactionsDto = toJSON(JSON.stringify(this.transactionsDto)) // hack to notify changes
        },
        processSearch:function (textToSearch) {
            vs.updateSearchMessage("${msg.searchResultLbl} '" + textToSearch + "'")
            this.url = vs.contextURL + "/rest/transaction?searchText=" + textToSearch
        },
        processSearchJSON:function (dataJSON) {
            this.params = dataJSON
            this.url = vs.contextURL + "/rest/transaction"
        },
        getHTTP: function (targetURL) {
            this.transactionType = getURLParam("transactionType", this.url)
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            vs.getHTTPJSON(targetURL, function(responseText){
                this.transactionsDto = toJSON(responseText)
            }.bind(this))
        }
    });</script>
</dom-module>