<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon', file: 'core-icon.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="${resource(dir: '/bower_components/google-chart', file: 'google-chart.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-transactionvs-dialog']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/balance/uservs-balance-chart']"/>">

<asset:javascript src="balanceVSUtils.js"/>

<polymer-element name="balance-item" attributes="transactionList caption balance">
<template>
<style>
        .subjectColumn {
            width:220px; overflow: hidden; text-overflow: ellipsis; white-space:nowrap; margin:0px 10px 0px 0px; cursor: pointer;
        }
        .amountColumn {width:120px;text-align: right; }
        .tagColumn {font-size: 0.6em; text-align: center; vertical-align: middle; width: 100px; text-overflow: ellipsis;}
</style>
<div style="display:{{transactionList.length > 0 ? 'block':'none'}}">
    <div layout vertical>
    <div style="font-weight: bold;color:#6c0404;">{{caption}}</div>
        <div layout vertical center center-justified>
            <div>
                <template repeat="{{transaction in transactionList}}">
                    <div layout horizontal on-click="{{viewTransaction}}">
                        <div layout vertical center center-justified class="tagColumn">{{transaction.tags[0].name}}</div>
                        <div class="subjectColumn" style="">{{transaction.subject}}</div>
                        <div class="amountColumn">{{transaction.amount}} {{transaction.currency}}</div>
                    </div>
                </template>
                <div layout horizontal>
                    <div flex class="subjectColumn" style="text-align: right;font-weight: bold;">
                    <g:message code="totalLbl"/>: </div>
                    <div class="amountColumn" style="border-top: 1px solid #888;">{{transactionTotal}}</div>
                </div>
            </div>
        </div>
    </div>
</div>
</template>
<script>
    Polymer('balance-item', {
        caption:"<g:message code="expensesLbl"/>",
        publish: {
            transactionList: {value: {}},
            balance: {value: {}}
        },
        viewTransaction: function(e) {
            console.log(this.tagName + " - viewTransaction")
            this.fire("transactionviewer", e.target.templateInstance.model.transaction)
        },
        balanceChanged:function() {
            if(this.balance["EUR"]) {
                transactionTotal = 0
                Object.keys(this.balance["EUR"]).forEach(function(entry) {
                    transactionTotal = addNumbers(transactionTotal, this.balance["EUR"][entry])
                }.bind(this))
                this.transactionTotal = new Number(transactionTotal).toFixed(2) + " EUR"
            }
        },
        ready: function() {
            console.log(this.tagName + " - ready")
        }
    });
</script>

<!-- an element that uses the votingsystem-dialog element and core-overlay -->
<polymer-element name="balance-details" attributes="url">
<template>
    <asset:javascript src="balanceVSUtils.js"/>
    <vicket-transactionvs-dialog id="transactionViewer"></vicket-transactionvs-dialog>
        <!-- place all overlay styles inside the overlay target -->
        <style no-shim>
        .dialog {
            box-sizing: border-box;
            -moz-box-sizing: border-box;
            font-family: Arial, Helvetica, sans-serif;
            font-size: 13px;
            -webkit-user-select: none;
            -moz-user-select: none;
            overflow: auto;
            background: white;
            padding:10px 30px 30px 30px;
            outline: 1px solid rgba(0,0,0,0.2);
            box-shadow: 0 4px 16px rgba(0,0,0,0.2);
            width: 820px;
        }
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            border: 1px solid #ccc;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .subjectColumn {
            width:220px; overflow: hidden; text-overflow: ellipsis; white-space:nowrap; margin:0px 10px 0px 0px; cursor: pointer;
        }
        .amountColumn {width:120px;text-align: right; }
        .tagColumn {font-size: 0.6em; text-align: center; vertical-align: middle; width: 100px; text-overflow: ellipsis;}
        </style>

        <core-ajax auto id="ajax" handleAs="json" response="{{balance}}" contentType="json"
                   on-core-response="{{ajaxResponse}}" on-core-error="{{ajaxError}}"></core-ajax>
        <div layout vertical style="" >

            <div layout horizontal center center-justified style="" >
                <div id="caption" flex style="color: #6c0404; font-weight: bold; font-size: 1.2em; text-align: center;
                    margin:10px 0 10px 0;">
                    {{caption}} - {{balance.name}}</div>
            </div>
            <div style="text-align: center">
                <g:message code="periodLbl"/>: {{balance.timePeriod.dateFrom}} -- {{balance.timePeriod.dateTo}}
            </div>

            <div style="font-size: 0.8em; color: #888;">{{description}}</div>
            <template if="{{status}}">
                <div class="messageToUser" style="color: {{message.status == 200?'#388746':'#ba0011'}};">
                    <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                        <div id="messageToUser">{{message}}</div>
                        <core-icon icon="{{status == 200?'check':'error'}}" style="fill:{{message.status == 200?'#388746':'#ba0011'}};"></core-icon></div>

                    <paper-shadow z="1"></paper-shadow>
                </div>
            </template>
            <div style="display:{{balance.nif != null ? 'block':'none'}}">
                <div layout horizontal style="width: 100%; border-bottom: 2px solid #6c0404; padding: 10px 0 10px 0;">
                    <div style="min-width: 300px; padding: 0 0 0 20px;">
                        <template repeat="{{currency in getMapKeys(balance.balanceResult)}}">
                            <div style="font-weight: bold;color:#6c0404; margin: 0 0 5px 0;">
                                <g:message code="cashLbl"/> - <g:message code="currencyLbl"/> {{currency}}
                            </div>
                            <div style="margin:0 0 0 15px;">
                                <template repeat="{{tag in getMapKeys(balance.balanceResult[currency])}}">
                                    {{tag}} {{balance.balanceResult[currency][tag] | formatAmount}}
                                </template>
                            </div>
                        </template>
                    </div>
                    <div>
                        <template repeat="{{currency in getMapKeys(balance.balancesToTimeLimited)}}">
                            <div style="font-weight: bold;color:#6c0404; margin: 0 0 5px 0;">
                                <g:message code="timeLimitedLbl"/> - <g:message code="currencyLbl"/> {{currency}}
                            </div>
                            <div style="margin:0 0 0 15px;">
                                <template repeat="{{tag in getMapKeys(balance.balancesToTimeLimited[currency])}}">
                                    {{tag}} {{balance.balancesToTimeLimited[currency][tag] | formatAmount}}
                                </template>
                            </div>
                        </template>
                    </div>
                </div>
                <div layout horizontal>
                    <div style="border-right: 2px solid  #6c0404; margin:0px 20px 0 0; padding:0 20px 0 20px;">
                        <balance-item caption="<g:message code="incomesLbl"/>" transactionList="{{balance.transactionToList}}"
                                      balance="{{balance.balancesTo}}" on-transactionviewer="{{viewTransaction}}"></balance-item>
                    </div>
                    <div>
                        <balance-item id="balanceFromItem"  transactionList="{{balance.transactionFromList}}"
                                      balance="{{balance.balancesTo}}"></balance-item>
                    </div>
                </div>
            </div>

                <div id="currencyChartContainer" style="margin: 15px auto;">
                    <uservs-balance-chart id="balanceChart" chart="column" yAxisTitle="<g:message code="euroLbl"/>s"
                        title="<g:message code="userVSBalancesLbl"/>"
                        xAxisCategories="['<g:message code="incomesLbl"/>', '<g:message code="expensesLbl"/>',
                        '<g:message code="cashLbl"/> (<g:message code="timeLimitedIncludedLbl"/>)',
                        '<g:message code="timeLimitedLbl"/>']">
                    </uservs-balance-chart>
                </div>

            <div id="currencyChartContainer1" style=""></div>
            </div>
        </div>
    </div>
</template>
<script>


    Polymer('balance-details', {
        url:null,
        caption:null,
        description:null,
        transactionFromTotal:null,
        status:null,
        message:null,
        publish: {
            balance: {value: {}}
        },
        formatAmount: function(amount) {
            return amount.toFixed(2)
        },
        getMapKeys: function(e) {
            if(e == null) return []
            else return Object.keys(e)
        },
        ready: function(e) {
            console.log(this.tagName + " - ready")
            //var toMap = {"EUR":{"HIDROGENO":880.5, "NITROGENO":100},  "DOLLAR":{"WILDTAG":345}}
            //var fromMap = {"EUR":{"HIDROGENO":580.5, "OXIGENO":250}, "DOLLAR":{"WILDTAG":245}}
            //var resultCalc = calculateBalanceResultMap(toMap, fromMap)
            //calculateUserBalanceSeries(toMap.EUR, fromMap.EUR, {}, {})
        },
        viewTransaction: function(e) {
            console.log(this.tagName + " - viewTransaction - e.detail: " + JSON.stringify(e.detail))
            this.$.transactionViewer.transactionvs =  e.detail;
        },
        balanceChanged: function() {
            console.log(this.tagName + " - balanceChanged - balance.type: " + this.balance.type)
            var transactionTotal = 0
            var caption = "<g:message code="balanceDetailCaption"/>"
            var userType
            if('SYSTEM' == this.balance.type) {
                this.caption = caption.format("<g:message code="systemLbl"/>")
                this.description = "IBAN: " + this.balance.IBAN
            }
            else if('BANKVS' == this.balance.type) {
                this.caption = caption.format("<g:message code="bankVSLbl"/>")
                this.description = "NIF:" + this.balance.nif + " - IBAN: " + this.balance.IBAN
                this.$.balanceFromItem.caption = "<g:message code="contributionsLbl"/>"
            }
            else if('GROUP' == this.balance.type) {
                this.caption = caption.format("<g:message code="groupLbl"/>")
                this.description = "IBAN: " + this.balance.IBAN
                this.balance.nif = ""
            }
            else if('USER' == this.balance.type || this.balance.userVS) {
                this.balance.nif = this.balance.userVS.nif
                this.balance.name = this.balance.userVS.firstName + " " + this.balance.userVS.lastName
                this.caption = caption.format("<g:message code="userLbl"/>")
                this.description = "NIF:" + this.balance.nif + " - IBAN: " + this.balance.userVS.IBAN
            }

            var balancesToMap = this.balance.balancesTo == null ? {}: this.balance.balancesTo.EUR || {}
            var balancesFromMap = this.balance.balancesFrom == null ? {}: this.balance.balancesFrom.EUR || {}
            var balanceResultMap = this.balance.balanceResult == null ? {}: this.balance.balanceResult.EUR || {}
            var balanceResultTimeLimitedMap = this.balance.balancesToTimeLimited == null ? {}: this.balance.balancesToTimeLimited.EUR || {}
            //we know the order serie -> incomes, expenses, available, time limited available
            this.$.balanceChart.series = calculateUserBalanceSeries(balancesToMap, balancesFromMap, balanceResultMap,
                    balanceResultTimeLimitedMap)

        },
        tapHandler: function() {
            this.$.xDialog.toggle();
        },
        urlChanged: function() {
            this.$.ajax.url = this.url
        },
        ajaxResponse: function() { },
        ajaxError: function(e) {
            console.log(this.tagName + " - ajax-response - newURL: " + this.$.ajax.url + " - status: " + e.detail.xhr.status)
            this.status = e.detail.xhr.status
            this.message = e.detail.xhr.responseText
        }
    });

</script>
</polymer-element>