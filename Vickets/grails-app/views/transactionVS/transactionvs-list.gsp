<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

<polymer-element name="transactionvs-list" attributes="url userNif isUserVSTable transactionsMap">
<template>
    <style>
    .card {
        position: relative;
        display: inline-block;
        width: 350px;
        vertical-align: top;
        background-color: #fff;
        box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
        border: 1px solid rgba(0, 0, 0, 0.24);
        margin: 10px;
        color: #667;
        cursor: pointer;
    }
    .date {margin:3px 10px 0 0; color: #888; font-size: 0.8em;}
    .transactionDescription {color:#621; font-size: 1.2em; text-decoration: underline;}
    .subject {color: #888; margin: 3px 3px 5px 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;}
    .amount {color: #f9f9f9; background:#621; font-weight: bold; padding: 0 5px 0 5px;font-size: 0.9em;}
    .tag {color:#388746; margin: 0 0 0 5px;font-size: 0.9em;}
    .timeInfo {color:#621; text-transform: uppercase; text-align: right; margin: 0 5px 0 0; font-size: 0.8em;}
    .expenseTrans { background: #fee; }
    </style>
    <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get"
               contentType="json"  on-core-complete="{{ajaxComplete}}"></core-ajax>
        <div layout flex horizontal wrap around-justified>
            <template repeat="{{transaction in transactionRecords}}">
                <div vertical on-click="{{showTransactionDetails}}" class="card {{getClass(transaction)}}">
                    <div horizontal layout>
                        <div flex horizontal layout center-justified class="transactionDescription">{{transaction| transactionDescription}}</div>
                        <div class="date">{{transaction.dateCreated}}</div>
                    </div>
                    <div horizontal layout center-justified class="subject">{{transaction.subject}}</div>
                    <div horizontal layout>
                        <div class="amount">{{amount(transaction)}}</div>
                        <div class="tag">{{transaction.tags[0]}}</div>
                        <div flex class="timeInfo">{{transaction | timeLimitedDescription}}</div>
                    </div>
                </div>
            </template>
        </div>
</template>
<script>
    Polymer('transactionvs-list', {
        userNif:null,
        isUserVSTable:false,
        publish: {
            transactionsMap: {value: {}}
        },
        ready :  function(e) {
            console.log(this.tagName + " - ready")
        },
        urlChanged:function() {
            console.log(this.tagName + " - urlChanged: " + this.url)
        },
        getClass: function(transactionvs) {
            if(!this.isUserVSTable) return
            if(transactionvs.fromUserVS && transactionvs.fromUserVS.nif == this.userNif) {
                return "expenseTrans"
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
            loadURL_VS(e.target.templateInstance.model.transaction.messageSMIMEURL, "_blank")
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
        transactionsMapChanged:function() {
            console.log(this.tagName + " - transactionsMapChanged - transactionsMap: " +
                    Object.prototype.toString.call(this.transactionsMap))
            this.transactionsMap = toJSON(this.transactionsMap)
            this.transactionRecords = this.transactionsMap.transactionRecords
        },
        ajaxComplete: function() {
            this.transactionsMap = this.responseData
            console.log(this.tagName + " - ajaxComplete")
        },
        addTransaction:function(transactionvs) {
            this.transactionsMap.transactionRecords.push(transactionvs)
        }
    });</script>
</polymer-element>
