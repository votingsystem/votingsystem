<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon', file: 'core-icon.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/balance/uservs-balance-chart']"/>">

<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-db']"/>">

<asset:javascript src="balanceVSUtils.js"/>

<polymer-element name="balance-item" attributes="transactionList caption">
    <template>
    <g:include view="/include/styles.gsp"/>
    <style>
            .subjectColumn {
                width:220px; overflow: hidden; text-overflow: ellipsis; white-space:nowrap; margin:0px 10px 0px 0px; cursor: pointer;
            }
            .amountColumn {width:120px;text-align: right; }
            .tagColumn {font-size: 0.6em; text-align: center; vertical-align: middle; width: 120px; text-overflow: ellipsis;}
    </style>
    <div layout vertical>
        <div horizontal layout center center-justified style="margin: 0 0 10px 0;">
            <div style="font-weight: bold;color:#6c0404;">{{caption}}</div>
            <div flex></div>
            <div style="font-size: 0.8em;color:#888; margin:0 0 0 10px;">{{numMovements}}</div>
        </div>
        <div>
            <template repeat="{{transaction in transactionList}}">
                <div layout horizontal on-click="{{viewTransaction}}">
                    <div class="subjectColumn" style="">{{transaction.subject}}</div>
                    <div class="amountColumn">{{transaction.amount}} {{transaction.currency}}</div>
                    <div layout horizontal center center-justified class="tagColumn">
                        <div layout vertical center center-justified>{{transaction.tags[0].name}}</div>
                        <div style="margin:0 0 0 5px;display:{{ isTimeLimited(transaction) ? 'block':'none'}}">
                            <core-tooltip label="<g:message code="timeLimitedAdviceMsg"/>" position="top">
                                <i class="fa fa-clock-o"></i>
                            </core-tooltip>
                        </div>
                    </div>
                </div>
            </template>

            <div layout horizontal style="padding: 0 120px 0 0;">
                <div flex class="subjectColumn" style="text-align: right;font-weight: bold;">
                    <g:message code="totalLbl"/>:
                </div>
                <div class="amountColumn" style="border-top: 1px solid #888;">{{transactionTotal}}</div>
            </div>
        </div>
    </div>
    </template>
<script>
    Polymer('balance-item', {
        caption:"<g:message code="expensesLbl"/>",
        numMovements:"",
        publish: {
            balanceItem: {value: {}},
            transactionList: {value: []}
        },
        ready: function() {
            console.log(this.tagName + " - ready - transactionList: " + this.transactionList.length)
        },
        isTimeLimited: function(tranctionvs) {
            return true
        },
        viewTransaction: function(e) {
            console.log(this.tagName + " - viewTransaction")
            this.fire("transactionviewer", e.target.templateInstance.model.transaction)
        },
        transactionListChanged:function() {
            this.numMovements = this.transactionList.length + " <g:message code="movementsLbl"/>"
        },
        balanceItemChanged:function() {
            if(this.balanceItem != null && this.balanceItem["EUR"]) {
                transactionTotal = 0
                Object.keys(this.balanceItem["EUR"]).forEach(function(entry) {
                    transactionTotal = addNumbers(transactionTotal, this.balanceItem["EUR"][entry])
                }.bind(this))
                this.transactionTotal = new Number(transactionTotal).toFixed(2) + " EUR"
            }
        }
    });
</script>
</polymer-element>

<polymer-element name="uservs-balance" attributes="url balance">
<template>
    <votingsystem-dialog id="xDialog" on-core-overlay-open="{{onCoreOverlayOpen}}"  title="<g:message code="transactionVSLbl"/>"
             style="width: 400px; height: 500px;">
        <transactionvs-db id="transactionViewer"></transactionvs-db>
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
                        <div layout horizontal center center-justfied>
                            <div style="font-weight: bold;color:#6c0404; margin: 0 0 0 0;">
                                <g:message code="icomesTimeLimitedLbl"/> - <g:message code="currencyLbl"/> {{currency}}
                            </div>
                            <div style="font-size: 0.8em;color:#888; margin: 0 0 0 20px;">
                                <g:message code="timeLimitedAdviceMsg"/>
                            </div>
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
                                  balanceItem="{{balance.balancesTo}}" on-transactionviewer="{{viewTransaction}}"></balance-item>
                </div>
                <div>
                    <balance-item id="balanceFromItem"  transactionList="{{balance.transactionFromList}}"
                                  balanceItem="{{balance.balancesFrom}}"></balance-item>
                </div>
            </div>
        </div>

        <div id="currencyChartContainer" style="margin: 15px auto;">
            <uservs-balance-chart id="balanceChart" chart="column" yAxisTitle="<g:message code="euroLbl"/>s"
                title="<g:message code="userVSBalancesLbl"/>"
                xAxisCategories="['<g:message code="incomesLbl"/>', '<g:message code="icomesTimeLimitedLbl"/>',
                '<g:message code="cashLbl"/> (<g:message code="timeLimitedIncludedLbl"/>)',
                '<g:message code="expensesLbl"/>']">
            </uservs-balance-chart>
        </div>

        <div id="currencyChartContainer1" style=""></div>
    </div>
</template>
<script>
    Polymer('uservs-balance', {
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
            var balanceResultMap = this.balance.balanceResult == null ? {}: this.balance.balanceResult.EUR || {}
            var balanceResultTimeLimitedMap = this.balance.balancesToTimeLimited == null ? {}: this.balance.balancesToTimeLimited.EUR || {}
            //we know the order serie -> incomes, expenses, available, time limited available
            this.$.balanceChart.series = calculateUserBalanceSeries(balancesToMap, balancesFromMap, balanceResultMap, balanceResultTimeLimitedMap)
        },
        viewTransaction: function(e) {
            //console.log(this.tagName + " - viewTransaction - e.detail: " + JSON.stringify(e.detail))
            this.$.transactionViewer.transactionvs = e.detail
            this.$.xDialog.opened = true;
        }
    });

</script>
</polymer-element>