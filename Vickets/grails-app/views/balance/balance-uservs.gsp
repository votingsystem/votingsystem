<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon', file: 'core-icon.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-progress', file: 'paper-progress.html')}">

<link rel="import" href="<g:createLink  controller="element" params="[element: '/balance/balance-uservs-chart']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-data']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-list-balance']"/>">

<asset:javascript src="balanceVSUtils.js"/>

<polymer-element name="balance-uservs" attributes="url balance">
<template>
    <g:include view="/include/styles.gsp"/>
    <votingsystem-dialog id="xDialog" on-core-overlay-open="{{onCoreOverlayOpen}}"  title="<g:message code="transactionVSLbl"/>">
        <transactionvs-data id="transactionViewer"></transactionvs-data>
    </votingsystem-dialog>
    <style no-shim>
    .messageToUser {
        font-weight: bold; margin:10px auto 10px auto; border: 1px solid #ccc; background: #f9f9f9;
        padding:10px 20px 10px 20px;
    }
    </style>
    <div layout vertical style="padding: 0 20px;">
        <div layout horizontal center center-justified style="position: relative; display: block;" >
            <div id="caption" flex style="color: #6c0404; font-weight: bold; font-size: 1.2em; text-align: center;
                margin:10px 0 10px 0;">
                {{caption}} - {{userVSName}}
            </div>
            <div style="font-size: 0.8em; color: #888; font-weight: normal; right:0px;top:0px; float: right; vertical-align: top;">
                {{description}}
            </div>
        </div>
        <div horizontal layout center style="position: relative; display: block;">
            <div flex style="text-align: center; font-weight: bold; color: #888; margin:0 0 8px 0;">
                <g:message code="periodLbl"/>: {{balance.timePeriod.dateFrom}} - {{balance.timePeriod.dateTo}}
            </div>
        </div>

        <template if="{{status}}">
            <div class="messageToUser" style="color: {{message.status == 200?'#388746':'#ba0011'}};">
                <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                    <div id="messageToUser">{{message}}</div>
                    <core-icon icon="{{status == 200?'check':'error'}}" style="fill:{{message.status == 200?'#388746':'#ba0011'}};"></core-icon></div>

                <paper-shadow z="1"></paper-shadow>
            </div>
        </template>
        <div>
            <div layout horizontal style="width: 100%; border-bottom: 2px solid #6c0404; padding: 10px 0 10px 0;">
                <div style="min-width: 300px; margin: 0 0 0 20px;">
                    <template repeat="{{currency in getMapKeys(balance.balancesCash)}}">
                        <div style="font-weight: bold;color:#6c0404; margin: 0 0 5px 0;">
                            <g:message code="cashLbl"/> - <g:message code="currencyLbl"/> {{currency}}
                        </div>
                        <div>
                            <div horizontal layout>
                                <template repeat="{{tag in getMapKeys(balance.balancesCash[currency])}}">
                                    <div style="margin:0 0 0 80px;">
                                        <div>{{tag}}: {{balance.balancesCash[currency][tag] | formatAmount}}</div>
                                        <div>
                                            <template if="{{isTimeLimited(currency, tag)}}">
                                                <core-tooltip large label="{{getTimeLimitedForTagMsg(currency, tag)}}" position="right">
                                                    <div horizontal layout center center-justified style="vertical-align: top;">
                                                        <paper-progress value="{{getPercentageForTagMsg(currency, tag)}}" style="width: 70px;"></paper-progress>
                                                        <i class="fa fa-clock-o" style="color:#6c0404;font-size: 0.8em; margin:0 0 0 10px;"></i>
                                                    </div>
                                                </core-tooltip>
                                            </template>
                                            <template if="{{!isTimeLimited(currency, tag)}}">
                                                <div style="text-align: center;">
                                                <i class="fa fa-check" style="color:darkgreen;font-size: 0.8em; margin:0 0 0 10px;"></i>
                                                </div>
                                            </template>
                                        </div>
                                    </div>
                                </template>
                            </div>
                        </div>
                    </template>
                </div>
            </div>
            <div layout horizontal center center-justified>
                <div style="border-right: 2px solid  #6c0404; margin:0px 20px 0 0; padding:0 20px 0 20px;">
                    <transactionvs-list-balance caption="<g:message code="incomesLbl"/>" transactionList="{{balance.transactionToList}}"
                                  balances="{{balance.balancesTo}}" on-transactionviewer="{{viewTransaction}}"></transactionvs-list-balance>
                </div>
                <div>
                    <transactionvs-list-balance id="balanceFromItem"  transactionList="{{balance.transactionFromList}}"
                          balances="{{balance.balancesFrom}}"></transactionvs-list-balance>
                </div>
            </div>
        </div>

        <div  id="userBalanceChartDiv" horizontal layout center center-justified style="margin: 15px auto;">
            <balance-uservs-chart id="balanceChart" chart="column" yAxisTitle="<g:message code="euroLbl"/>s"
                title="<g:message code="userVSBalancesLbl"/>"
                xAxisCategories="['<g:message code="incomesLbl"/> (<g:message code="totalLbl"/>)', '<g:message code="icomesTimeLimitedLbl"/>',
                '<g:message code="cashLbl"/>', '<g:message code="expensesLbl"/>']">
            </balance-uservs-chart>
        </div>
    </div>
</template>
<script>
    Polymer('balance-uservs', {
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
        getTimeLimitedForTagMsg: function(currency, tag) {
            var tagAlreadyExpended = this.balance.balancesFrom[currency][tag]
            tagAlreadyExpended = tagAlreadyExpended == null? 0 : tagAlreadyExpended
            var tagToExpend = this.balance.balancesTo[currency][tag].timeLimited
            var tagCash = tagToExpend - tagAlreadyExpended
            return "<g:message code="timeLimitedForTagMsg"/>".format(tagCash, currency, tag)
        },
        getPercentageForTagMsg: function(currency, tag) {
            var tagAlreadyExpended = this.balance.balancesFrom[currency][tag]
            tagAlreadyExpended = tagAlreadyExpended == null? 0 : tagAlreadyExpended
            var tagToExpend = this.balance.balancesTo[currency][tag].timeLimited
            var pertecentageExpended = tagAlreadyExpended * 100 / tagToExpend
            //console.log("pertecentageExpended: " + pertecentageExpended)
            return pertecentageExpended.toFixed(0)
        },
        isTimeLimited: function(currency, tag) {
            if(this.balance.balancesTo[currency][tag].timeLimited === 0) return false
            else return true
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
            //calculateUserBalanceSeries(toMap.EUR, fromMap.EUR, {})
            if(this.balance != null) this.initBalance(this.balance)
        },
        initBalance:function(balance) {
            console.log(this.tagName + " - initBalance")
            this.balance = balance
            var caption = "<g:message code="balanceDetailCaption"/>"
            this.description = "NIF:" + this.balance.userVS.nif + " - IBAN: " + this.balance.userVS.IBAN
            this.userVSName = this.balance.userVS.name
            if('SYSTEM' == this.balance.userVS.type) {
                this.caption = caption.format("<g:message code="systemLbl"/>")
                this.description = "IBAN: " + this.balance.userVS.IBAN
            }
            else if('BANKVS' == this.balance.userVS.type) {
                this.caption = caption.format("<g:message code="bankVSLbl"/>")
                this.$.balanceFromItem.caption = "<g:message code="contributionsLbl"/>"
            }
            else if('GROUP' == this.balance.userVS.type) {
                this.caption = caption.format("<g:message code="groupLbl"/>")
                this.description = "IBAN: " + this.balance.userVS.IBAN
                this.balance.userVS.nif = ""
            }
            else if('USER' == this.balance.userVS.type) {
                this.userVSName = this.balance.userVS.firstName + " " + this.balance.userVS.lastName
                this.caption = caption.format("<g:message code="userLbl"/>")
            }

            var balancesToMap = this.balance.balancesTo == null ? {}: this.balance.balancesTo.EUR || {}
            var balancesFromMap = this.balance.balancesFrom == null ? {}: this.balance.balancesFrom.EUR || {}
            var balancesCashMap = this.balance.balancesCash == null ? {}: this.balance.balancesCash.EUR || {}

            var chartSeries = calculateUserBalanceSeries(balancesToMap, balancesFromMap, balancesCashMap)
            if(chartSeries.length === 0) {
                this.$.userBalanceChartDiv.style.display = 'none'
            } else {
                //we know the order serie -> incomes, expenses, available, time limited available
                this.$.balanceChart.series = chartSeries
            }
        },
        viewTransaction: function(e) {
            //console.log(this.tagName + " - viewTransaction - e.detail: " + JSON.stringify(e.detail))
            this.$.transactionViewer.transactionvs = e.detail
            this.$.xDialog.opened = true;
        }
    });

</script>
</polymer-element>