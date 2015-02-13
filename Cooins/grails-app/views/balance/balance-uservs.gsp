<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog-transition.html"/>
<vs:webresource dir="core-icon" file="core-icon.html"/>
<vs:webresource dir="core-tooltip" file="core-tooltip.html"/>
<vs:webresource dir="paper-shadow" file="paper-shadow.html"/>
<vs:webresource dir="paper-progress" file="paper-progress.html"/>
<vs:webcomponent path="/balance/balance-uservs-chart"/>
<vs:webcomponent path="/balance/balance-uservs-chart-donut"/>
<vs:webcomponent path="/transactionVS/transactionvs-data"/>
<vs:webcomponent path="/transactionVS/transactionvs-list-balance"/>
<vs:webresource dir="core-media-query" file="core-media-query.html"/>
<vs:webresource dir="paper-tabs" file="paper-tabs.html"/>
<vs:webcomponent path="/element/time-elements"/>
<g:include view="/include/balanceVSUtils_js.gsp"/>


<polymer-element name="balance-uservs" attributes="url balance">
    <template>
        <g:include view="/include/styles.gsp"/>
        <paper-dialog id="xDialog" layered backdrop on-core-overlay-open="{{onCoreOverlayOpen}}">
            <transactionvs-data id="transactionViewer"></transactionvs-data>
        </paper-dialog>
        <style no-shim>
        .messageToUser {
            font-weight: bold; margin:10px auto 10px auto; border: 1px solid #ccc; background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .pageTitle {color: #6c0404; font-weight: bold; font-size: 1.3em; text-align: center;
            margin:10px 0 10px 0; text-decoration: underline; cursor: pointer;}
        .tabContent { margin:0px auto 0px auto; width:auto; }
        paper-tabs.transparent-teal { padding: 0px; background-color: #ffeeee; color:#ba0011;
            box-shadow: none; cursor: pointer; height: 35px;
        }
        paper-tabs.transparent-teal /deep/ #selectionBar {
            background-color: #ba0011;
        }
        paper-tabs.transparent-teal paper-tab /deep/ #ink {
            color: #ba0011;
        }
        </style>
        <core-media-query query="max-width: 1200px" queryMatches="{{smallScreen}}"></core-media-query>
        <div layout vertical style="padding: 0 20px;">
            <div horizontal layout center center-justified>
                <div flex></div>
                <div id="caption" class="pageTitle" on-click="{{goToUserVSPage}}">{{caption}} - {{userVSName}}</div>
                <div flex style="font-size: 0.7em; color: #888; font-weight: normal; text-align: right;">
                    {{description}}
                </div>
            </div>
            <div horizontal layout center center-justified style="text-align: center;font-weight: bold; color: #888; margin:0 0 8px 0;">
                <g:message code="periodLbl"/>:
                <div><time is="local-time" datetime="{{balance.timePeriod.dateFrom}}"
                           day="numeric" month="short" year="numeric"></time> - </div>
                <div><time is="local-time" datetime="{{balance.timePeriod.dateTo}}"
                           day="numeric" month="short" year="numeric"></time></div>
            </div>
            <div hidden?="{{!status}}" class="messageToUser" style="color: {{message.status == 200?'#388746':'#ba0011'}};">
                <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                    <div id="messageToUser">{{message}}</div>
                    <core-icon icon="{{status == 200?'check':'error'}}" style="fill:{{message.status == 200?'#388746':'#ba0011'}};"></core-icon></div>

                <paper-shadow z="1"></paper-shadow>
            </div>
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
                                            <div>{{tag | tagDescription}}: {{balance.balancesCash[currency][tag] | formatAmount}}</div>
                                            <div>
                                                <template if="{{isTimeLimited(currency, tag)}}">
                                                    <core-tooltip position="bottom" style="max-width: 100px;">
                                                        <div horizontal layout center center-justified>
                                                            <paper-progress value="{{getPercentageForTagMsg(currency, tag)}}" style="width: 100px;"></paper-progress>
                                                            <i class="fa fa-clock-o" style="color:#6c0404;font-size: 0.8em; margin:0 0 0 10px;"></i>
                                                        </div>
                                                        <div tip>{{getTimeLimitedForTagMsg(currency, tag)}}</div>
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
                <div hidden?="{{!smallScreen}}">
                    <paper-tabs style="margin:0px auto 0px auto; cursor: pointer;" class="transparent-teal center"
                                valueattr="name" selected="{{selectedTab}}"  on-core-select="{{tabSelected}}" noink>
                        <paper-tab name="incomesTab" style="width: 400px"><g:message code="incomesLbl"/></paper-tab>
                        <paper-tab name="expensesTab"><g:message code="expensesLbl"/></paper-tab>
                    </paper-tabs>
                </div>
                <div layout horizontal>
                    <div flex hidden?="{{smallScreen?(selectedTab !== 'incomesTab'):false}}"
                         style=" margin:0px 20px 0 0; padding:0 20px 0 20px; vertical-align: top;">
                        <transactionvs-list-balance caption="<g:message code="incomesLbl"/>" captionVisible={{!smallScreen}}
                                transactionList="{{balance.transactionToList}}"
                                balances="{{balance.balancesTo}}" on-transactionviewer="{{viewTransaction}}"></transactionvs-list-balance>
                    </div>
                    <div  hidden?="{{smallScreen}}" style="width: 1px; border-right: 2px solid  #6c0404; margin:0 10px 0 10px;"></div>
                    <div hidden?="{{smallScreen?(selectedTab !== 'expensesTab'):false}}" flex>
                        <transactionvs-list-balance id="balanceFromItem" captionVisible={{!smallScreen}}
                                transactionList="{{balance.transactionFromList}}" balances="{{balance.balancesFrom}}"
                                on-transactionviewer="{{viewTransaction}}"></transactionvs-list-balance>
                    </div>
                </div>
            </div>
        </div>
        <div  id="userBalanceChartDiv" horizontal layout center center-justified style="margin:0 auto; display:block;">
            <balance-uservs-chart-donut id="donutChart"></balance-uservs-chart-donut>
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
            selectedTab:'incomesTab',
            publish: {
                balance: {value: {}}
            },
            formatAmount: function(amount) {
                if(Object.prototype.toString.call(amount) !== '[object String]') return amount.toFixed(2)
                else return amount
            },
            tagDescription: function(tagName) {
                switch (tagName) {
                    case 'WILDTAG': return "<g:message code="wildTagLbl"/>".toUpperCase()
                    default: return tagName
                }
            },
            goToUserVSPage:function() {
                loadURL_VS("${createLink( controller:'userVS', action:"", absolute:true)}/" + this.balance.userVS.id)
            },
            getTimeLimitedForTagMsg: function(currency, tag) {
                var expendedFromTag = 0
                if(this.balance.balancesFrom[currency] != null && this.balance.balancesFrom[currency][tag] != null) {
                    expendedFromTag = this.balance.balancesFrom[currency][tag]
                }
                expendedFromTag = expendedFromTag == null? 0 : expendedFromTag
                var tagToExpend = this.balance.balancesTo[currency][tag].timeLimited
                var tagCash = tagToExpend - expendedFromTag
                return "<g:message code="timeLimitedForTagMsg"/>".format(tagCash.toFixed(2), currency,  this.tagDescription(tag))
            },
            getPercentageForTagMsg: function(currency, tag) {
                var expendedFromTag = 0
                if(this.balance.balancesFrom[currency] != null && this.balance.balancesFrom[currency][tag] != null) {
                    expendedFromTag = this.balance.balancesFrom[currency][tag]
                }
                expendedFromTag = expendedFromTag == null? 0 : expendedFromTag
                var tagToExpend = this.balance.balancesTo[currency][tag].timeLimited
                var pertecentageExpended = expendedFromTag * 100 / tagToExpend
                //console.log("pertecentageExpended: " + pertecentageExpended)
                return pertecentageExpended.toFixed(0)
            },
            isTimeLimited: function(currency, tag) {
                if(this.balance.balancesTo[currency][tag].timeLimited == 0) return false
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
                console.log(this.tagName + " - initBalance typeof: " + typeof balance)
                if('object' !== typeof balance) this.balance = JSON.parse(balance)
                else this.balance = balance
                console.log(this.tagName + " - this.balance.userVS: " + this.balance)
                this.description = "NIF:" + this.balance.userVS.nif + " - IBAN: " + this.balance.userVS.IBAN
                this.userVSName = this.balance.userVS.name
                if('SYSTEM' == this.balance.userVS.type) {
                    this.caption = "<g:message code="systemLbl"/>"
                    this.description = "IBAN: " + this.balance.userVS.IBAN
                }
                else if('BANKVS' == this.balance.userVS.type) {
                    this.caption = "<g:message code="bankVSLbl"/>"
                    this.$.balanceFromItem.caption = "<g:message code="contributionsLbl"/>"
                }
                else if('GROUP' == this.balance.userVS.type) {
                    this.caption = "<g:message code="groupLbl"/>"
                    this.description = "IBAN: " + this.balance.userVS.IBAN
                    this.balance.userVS.nif = ""
                }
                else if('USER' == this.balance.userVS.type) {
                    this.userVSName = this.balance.userVS.firstName + " " + this.balance.userVS.lastName
                    this.caption = "<g:message code="userLbl"/>"
                }

                if('SYSTEM' != this.balance.userVS.type) {
                    var balancesToMap = this.balance.balancesTo == null ? {}: this.balance.balancesTo.EUR || {}
                    var balancesFromMap = this.balance.balancesFrom == null ? {}: this.balance.balancesFrom.EUR || {}
                    var balancesCashMap = this.balance.balancesCash == null ? {}: this.balance.balancesCash.EUR || {}
                    var chartSeries = calculateUserBalanceSeriesDonut(balancesToMap, balancesFromMap, balancesCashMap)
                    this.$.donutChart.setSeries(chartSeries)

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