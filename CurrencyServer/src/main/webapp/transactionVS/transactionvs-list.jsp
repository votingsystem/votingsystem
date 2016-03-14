<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-user-box/vs-user-box.html" rel="import"/>
<link href="transactionvs-card.vsp" rel="import"/>
<link href="transactionvs-selector.vsp" rel="import"/>

<dom-module name="transactionvs-list">
<template>
    <div class="horizontal layout center center-justified">
        <transactionvs-selector id="transactionSelector" transactionvs-type="{{transactionvsType}}"></transactionvs-selector>
    </div>
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <div class="layout flex horizontal wrap around-justified">
        <template is="dom-repeat" items="{{transactionsDto.resultList}}" as="transaction">
            <transactionvs-card transaction="{{transaction}}"></transactionvs-card>
        </template>
        <div hidden="{{messageToUserHidden}}" style="font-size: 1.3em; margin:30px 0 0 0;">
            ${msg.withoutMovementsLbl}
        </div>
    </div>
</template>
<script>
    Polymer({
        is:'transactionvs-list',
        properties: {
            transactionsDto: {type:Object, value: {}, observer:'transactionsDtoChanged'},
            url:{type:String, observer:'getHTTP'},
            transactionvsType:{type:String}
        },
        ready:function() {
            console.log(this.tagName + " - ready")
            this.$.transactionSelector.addEventListener('selected', function (e) {
                var transactionvsType = e.detail
                var targetURL = vs.contextURL + "/rest/transactionVS";
                if("" != transactionvsType) {
                    targetURL = targetURL + "?transactionvsType=" + transactionvsType
                }
                var newURL = setURLParameter(window.location.href, "transactionvsType",  transactionvsType)
                history.pushState(null, null, newURL);
                console.log(this.tagName + " - targetURL: " + targetURL);
                this.url = targetURL
            }.bind(this))
        },
        transactionsDtoChanged:function() {
            console.log(this.tagName + " - transactionsDtoChanged - transactionsDto: " + this.transactionsDto)
            if(this.transactionsDto.resultList) this.messageToUserHidden = (this.transactionsDto.resultList.length !== 0)
        },
        addTransaction:function(transactionvs) {
            this.transactionsDto.resultList.push(transactionvs)
            this.transactionsDto = toJSON(JSON.stringify(this.transactionsDto)) // hack to notify changes
        },
        processSearch:function (textToSearch) {
            vs.updateSearchMessage("${msg.searchResultLbl} '" + textToSearch + "'")
            this.url = vs.contextURL + "/rest/transactionVS?searchText=" + textToSearch
        },
        processSearchJSON:function (dataJSON) {
            this.params = dataJSON
            this.url = vs.contextURL + "/rest/transactionVS"
        },
        getHTTP: function (targetURL) {
            this.transactionvsType = getURLParam("transactionvsType", this.url)
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                this.transactionsDto = toJSON(rawData.response)
            }.bind(this));
        }
    });</script>
</dom-module>