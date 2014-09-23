<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<asset:javascript src="balanceVSUtils.js"/>

<polymer-element name="balance-uservs-details" attributes="balance">
    <template>
        <style>
            .movemenType{
                font-size: 1.2em; font-weight: bold; text-decoration: underline;color: #6c0404;
            }
            .tagLbl{ font-weight: bold; }
            .userVS{font-weight: bold; text-align: center; color: #888;}
            .errorMessage {color: #ba0011; border: 1px solid #ba0011; padding: 4px;}
            .card {
                position: relative;
                display: inline-block;
                width: 500px;
                vertical-align: top;
                background-color: #fff;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
                margin: 10px;
                padding: 10px;
                border: 1px solid #ccc;
            }
        </style>
        <div class="card" layout vertical>
            <div class="errorMessage" style="display:{{errorMessage?'block':'none'}}">{{errorMessage}}</div>
            <div class="userVS">{{balance.nif}} - {{balance.name}}</div>
            <div layout horizontal center center-justified>
                <g:message code="numExpensesLbl"/>: {{balance.transactionFromList.length}} ---
                <g:message code="numIncomesLbl"/>: {{balance.transactionToList.length}}
            </div>

            <template if="{{transactionToInfoMap}}">
                <template repeat="{{currency in getCurrencyList(transactionToInfoMap)}}">
                    <div class="movemenType"><g:message code="incomesLbl"/> - {{currency.currencyName}}</div>
                    <template repeat="{{tagVS in getTagVSList(currency)}}">
                        <div layout horizontal>
                            <div class="tagLbl">{{tagVS.name}}</div>  - <g:message code="numTransactionVSLbl"/>: {{tagVS.numTransactionVS}} -
                        <g:message code="amountLbl"/>: {{tagVS.amount}} {{currency.currencyName}}
                        </div>
                    </template>
                </template>
            </template>

            <template if="{{transactionFromInfoMap}}">
                <template repeat="{{currency in getCurrencyList(transactionFromInfoMap)}}">
                    <div class="movemenType"><g:message code="expensesLbl"/> - {{currency.currencyName}}</div>
                    <template repeat="{{tagVS in getTagVSList(currency)}}">
                        <div layout horizontal>
                            <div class="tagLbl">{{tagVS.name}}</div>  - <g:message code="numTransactionVSLbl"/>: {{tagVS.numTransactionVS}} -
                            <g:message code="amountLbl"/>: {{tagVS.amount}} {{currency.currencyName}}
                        </div>
                    </template>
                </template>
            </template>

        </div>


    </template>
    <script>

        Polymer('balance-uservs-details', {
            ready: function() {
                console.log(this.tagName + " - ready")

                //calculateBalanceResultMap()

            },
            getCurrencyList:function(transactionFromInfoMap) {
                console.log("getCurrencyList")
                var currencyList = []
                Object.keys(transactionFromInfoMap).forEach(function(entry) {
                    var currencyDataMap = {currencyName:entry, tagVSMap:transactionFromInfoMap[entry]}
                    currencyList.push(currencyDataMap)
                });
                return currencyList
            },
            getTagVSList:function(currencyInfoMap) {
                var tagVSMap = currencyInfoMap.tagVSMap
                var tagVSList = []
                Object.keys(tagVSMap).forEach(function(entry) {
                    //{"HIDROGENO":{"numTransactionVS":6,"amount":"61236.00"}
                    var dataMap = {name:entry, numTransactionVS:tagVSMap[entry].numTransactionVS,
                        amount:tagVSMap[entry].amount}
                    tagVSList.push(dataMap)
                });
                return tagVSList
            },
            balanceChanged:function() {

                if(this.balance.userVS) {
                    this.balance.nif = this.balance.userVS.nif
                    this.balance.name = this.balance.userVS.firstName + " " + this.balance.userVS.lastName
                }
                this.transactionFromInfoMap = getCurrencyInfoMap(this.balance.transactionFromList)
                this.transactionToInfoMap = getCurrencyInfoMap(this.balance.transactionToList)

                //calculatedBalanceMap, serverBalanceMap
                try {
                    checkBalanceMap(this.transactionFromInfoMap, this.balance.balancesFrom)
                } catch(e) {
                    this.errorMessage = "transactionFrom - " + e
                }
                try {
                    checkBalanceMap(this.transactionToInfoMap, this.balance.balancesTo)
                } catch(e) {
                    this.errorMessage = "transactionTo - " + e
                }

                this.currencyInfoMapStr = JSON.stringify(this.transactionFromInfoMap)
            }
        });
    </script>
</polymer-element>


<polymer-element name="balance-weekreport" attributes="url">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>

        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{balances}}" handleAs="json" method="get" contentType="json"></core-ajax>
        <!--JavaFX Webkit gives problems with tables and templates -->
        <div layout center center-justified style="margin: 0px auto 0px auto;">
            <div layout flex horizontal wrap around-justified>
                <balance-uservs-details balance="{{balances.userBalances.systemBalance}}"></balance-uservs-details>
            </div>
            <div layout flex horizontal wrap around-justified>
                <template repeat="{{groupBalance in balances.userBalances.groupBalanceList}}">
                    <balance-uservs-details balance="{{groupBalance}}"></balance-uservs-details>
                </template>
            </div>
            <div layout flex horizontal wrap around-justified>
                <template repeat="{{userBalance in balances.userBalances.userBalanceList}}">
                    <balance-uservs-details balance="{{userBalance}}"></balance-uservs-details>
                </template>
            </div>

        </div>

    </template>
    <script>
        Polymer('balance-weekreport', {
            publish: {
                balances: {value: {}}
            },
            ready: function() {},
            getTagDescription: function(tagName) {
                switch (tagName) {
                    case 'WILDTAG': return "<g:message code="wildTagLbl"/>".toUpperCase()
                    default: return tagName
                }
            },
            balancesChanged: function() {
                console.log("balancesChanged")
                this.balancesStr = JSON.stringify(this.balances)
            },
            formatAmount: function(amount) {
                if(typeof amount == 'number') return amount.toFixed(2)
            }
        });
    </script>
</polymer-element>