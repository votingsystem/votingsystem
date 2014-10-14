<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/messageSMIME/message-smime-dialog']"/>">

<polymer-element name="transactionvs-table" attributes="url transactionList userNif isUserVSTable">
<template>
    <style>
    .tableHeadervs {
        margin: 0px 0px 0px 0px; color:#6c0404;  border-bottom: 1px solid #6c0404; background: white;
        font-weight: bold; padding:5px 0px 5px 0px; width: 100%;
    }
    .tableHeadervs div { text-align:center; }
    .rowvs { border-bottom: 1px solid #ccc; padding: 10px 0px 10px 0px; cursor: pointer;
        width: 100%; font-size: 0.9em; }
    .rowvs div { text-align:center; }
    .expenseRow { background: #fee; }
    </style>
    <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get"
               contentType="json"  on-core-complete="{{ajaxComplete}}"></core-ajax>
    <!--JavaFX Webkit gives problems with tables and templates -->
    <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
        <div layout horizontal center center-justified class="tableHeadervs">
            <div style="width:80px;"></div>
            <div style="width:210px;"><g:message code="typeLbl"/></div>
            <div style="width:140px;"><g:message code="tagLbl"/></div>
            <div style="width:110px;"><g:message code="amountLbl"/></div>
            <div style="width:140px;"><g:message code="dateLbl"/></div>
            <div flex style="width:240px;"><g:message code="subjectLbl"/></div>
        </div>
        <div>
            <template repeat="{{transaction in transactionsMap.transactionRecords}}">
                <div layout horizontal center center justified on-click="{{showTransactionDetails}}" class="rowvs {{getClass(transaction)}}">
                    <div style="width:80px;">{{transaction | timeLimitedDescription}}</div>
                    <div style="width: 210px;">
                        <a> {{transaction| transactionDescription}}</a>
                    </div>
                    <div style="width:140px;">{{transaction.tags[0]}}</div>
                    <div style="width:110px;">{{amount(transaction)}}</div>
                    <div style="width:140px;">{{transaction.dateCreated}}</div>
                    <div flex style="width:240px;">{{transaction.subject}}</div>
                </div>
            </template>
        </div>
    </div>
    <message-smime-dialog id="transactionViewer"></message-smime-dialog>
</template>
<script>
    Polymer('transactionvs-table', {
        userNif:null,
        isUserVSTable:false,
        publish: {
            transactionsMap: {value: {}}
        },
        transactionListChanged:function() {
            console.log("transactionListChanged")
        },
        getClass: function(transactionvs) {
            if(!this.isUserVSTable) return
            if(transactionvs.fromUserVS && transactionvs.fromUserVS.nif == this.userNif) {
                return "expenseRow"
            } else return ""
        },
        timeLimitedDescription: function(transactionvs) {
            if(transactionvs.isTimeLimited === true || transactionvs.validTo != null) return "<g:message code="timeLimitedLbl"/>"
            else if  (transactionvs.isTimeLimited === false) return ""
            else return transactionvs.isTimeLimited
        },
        transactionDescription: function(transactionvs) {
            var result = null
            if(this.userNif) {
                if(transactionvs.fromUserVS && transactionvs.fromUserVS.nif == this.userNif) {
                    result = "<g:message code="spendingLbl"/> - "
                }
                if(transactionvs.toUserVS && transactionvs.toUserVS.nif == this.userNif) {
                    result = result? result + "<g:message code="incomeLbl"/> - " : "<g:message code="incomeLbl"/> - "
                }
            }
            var transactionTypeLbl = getTransactionVSDescription(transactionvs.type)
            return result? result.toUpperCase() + transactionTypeLbl : transactionTypeLbl
        },
        showTransactionDetails: function(e) {
            var messageSMIMEURL = e.target.templateInstance.model.transaction.messageSMIMEURL
            if(messageSMIMEURL == null) messageSMIMEURL = "${createLink( controller:'messageSMIME', action:"transactionVS")}/" +
                    e.target.templateInstance.model.transaction.id
            this.$.transactionViewer.show(messageSMIMEURL)
        },
        newRecordChanged: function(e) {
            console.log("newRecordChanged: " + this.newRecord)
        },
        amount: function(transaction) {
            var amount
            if(isNaN(transaction.amount)) amount = transaction.amount.toFixed(2) + " " + transaction.currency
            else  amount = transaction.amount + " " + transaction.currency
            return amount
        },
        ajaxComplete: function() {
            this.transactionList = this.responseData.transactionRecords
        },
        addTransaction:function(transactionvs) {
            this.transactionList.push(transactionvs)
        }
    });</script>
</polymer-element>
