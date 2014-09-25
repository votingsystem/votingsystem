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
    <votingsystem-dialog id="xDialog" on-core-overlay-open="{{onCoreOverlayOpen}}"  title="<g:message code="transactionVSLbl"/>"
             style="">
        <transactionvs-data id="transactionViewer"></transactionvs-data>
    </votingsystem-dialog>
    <style no-shim>
    .messageToUser {
        font-weight: bold; margin:10px auto 10px auto; border: 1px solid #ccc; background: #f9f9f9;
        padding:10px 20px 10px 20px;
    }
    </style>
    <div layout vertical style="" >
        <div layout horizontal center center-justified style="" >
            <div id="caption" flex style="color: #6c0404; font-weight: bold; font-size: 1.2em; text-align: center;
                margin:10px 0 10px 0;">
                {{caption}} - {{balance.name}}</div>
        </div>
        <div style="text-align: center; font-weight: bold; color: #888; margin:0 0 8px 0;">
            <g:message code="periodLbl"/>: {{balance.timePeriod.dateFrom}} - {{balance.timePeriod.dateTo}}
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
        <div style="">
            <div layout horizontal style="width: 100%; border-bottom: 2px solid #6c0404; padding: 10px 0 10px 0;">
                <div style="min-width: 300px; padding: 0 0 0 20px;">
                    <template repeat="{{currency in getMapKeys(balance.balanceCash)}}">
                        <div style="font-weight: bold;color:#6c0404; margin: 0 0 5px 0;">
                            <g:message code="cashLbl"/> - <g:message code="currencyLbl"/> {{currency}}
                        </div>
                        <div style="margin:0 0 0 15px;">
                            <template repeat="{{tag in getMapKeys(balance.balanceCash[currency])}}">
                                {{tag}} {{balance.balanceCash[currency][tag] | formatAmount}}
                                <core-tooltip label="<g:message code="timeLimitedAdviceMsg"/>" position="top">
                                    <paper-progress value="40" style="width: 70px;"></paper-progress>
                                    <i class="fa fa-clock-o" style="color:#6c0404;font-size: 0.8em"></i>
                                </core-tooltip>
                            </template>
                        </div>
                    </template>
                </div>
            </div>
            <div layout horizontal>
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

        <div horizontal layout center center-justified  id="currencyChartContainer" style="margin: 15px auto;">
            <balance-uservs-chart id="balanceChart" chart="column" yAxisTitle="<g:message code="euroLbl"/>s"
                title="<g:message code="userVSBalancesLbl"/>"
                xAxisCategories="['<g:message code="incomesLbl"/>', '<g:message code="icomesTimeLimitedLbl"/>',
                '<g:message code="cashLbl"/>', '<g:message code="expensesLbl"/>']">
            </balance-uservs-chart>
        </div>


        <div id="currencyChartContainer1" style=""></div>
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
            if('SYSTEM' == this.balance.userVS.type) {
                this.caption = caption.format("<g:message code="systemLbl"/>")
                this.description = "IBAN: " + this.balance.IBAN
            }
            else if('BANKVS' == this.balance.userVS.type) {
                this.caption = caption.format("<g:message code="bankVSLbl"/>")
                this.description = "NIF:" + this.balance.userVS.nif + " - IBAN: " + this.balance.IBAN
                this.$.balanceFromItem.caption = "<g:message code="contributionsLbl"/>"
            }
            else if('GROUP' == this.balance.userVS.type) {
                this.caption = caption.format("<g:message code="groupLbl"/>")
                this.description = "IBAN: " + this.balance.IBAN
                this.balance.userVS.nif = ""
            }
            else if('USER' == this.balance.userVS.type) {
                this.balance.name = this.balance.userVS.firstName + " " + this.balance.userVS.lastName
                this.caption = caption.format("<g:message code="userLbl"/>")
                this.description = "NIF:" + this.balance.userVS.nif + " - IBAN: " + this.balance.userVS.IBAN
            }

            var balancesToMap = this.balance.balancesTo == null ? {}: this.balance.balancesTo.EUR || {}
            var balancesFromMap = this.balance.balancesFrom == null ? {}: this.balance.balancesFrom.EUR || {}
            var balanceCashMap = this.balance.balanceCash == null ? {}: this.balance.balanceCash.EUR || {}

            //we know the order serie -> incomes, expenses, available, time limited available
            this.$.balanceChart.series = calculateUserBalanceSeries(balancesToMap, balancesFromMap, balanceCashMap)
        },
        viewTransaction: function(e) {
            //console.log(this.tagName + " - viewTransaction - e.detail: " + JSON.stringify(e.detail))
            this.$.transactionViewer.transactionvs = e.detail
            this.$.xDialog.opened = true;
        }
    });

</script>
</polymer-element>