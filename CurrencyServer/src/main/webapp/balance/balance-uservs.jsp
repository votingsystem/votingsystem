<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/paper-progress/paper-progress.html" rel="import"/>
<link href="../resources/bower_components/iron-media-query/iron-media-query.html" rel="import"/>
<link href="../transactionVS/transactionvs-data.vsp" rel="import"/>
<link href="../transactionVS/transactionvs-list-balance.vsp" rel="import"/>

<dom-module name="balance-uservs">
    <template>
        <style>
            .pageTitle {color: #6c0404; font-weight: bold; font-size: 1.3em; text-align: center;
                margin:10px 0 10px 0; cursor: pointer;}
            .pageTitle:hover { text-decoration: underline; }
            .tab {font-weight: bold; font-size: 1.1em; margin:0 40px 0 0; text-align: center; cursor:pointer; width: 100%;color:#888;}
            .tabSelected { border-bottom: 2px solid #ba0011;color:#434343;}
        </style>
        <transactionvs-data id="transactionViewer"></transactionvs-data>

        <iron-media-query query="max-width: 1200px" query-matches="{{smallScreen}}"></iron-media-query>
        <div style="padding: 0 20px;">
            <div class="horizontal layout center center-justified">
                <div class="flex"></div>
                <div id="caption" class="pageTitle" on-click="goToUserVSPage"><span>{{caption}}</span> - <span>{{userVSName}}</span></div>
                <div class="flex" style="font-size: 0.7em; color: #888; font-weight: normal; text-align: right;">{{description}}</div>
            </div>
            <div class="horizontal layout center center-justified" style="text-align: center;font-weight: bold; color: #888; margin:0 0 8px 0;">
                <div><span>{{getDate(balance.timePeriod.dateFrom)}}</span> - <span>{{getDate(balance.timePeriod.dateTo)}}</span></div>
            </div>
            <div class="layout horizontal" style="width: 100%; border-bottom: 2px solid #6c0404; padding: 10px 0 10px 0;">
                <div style="min-width: 300px; margin: 0 0 0 20px;">
                    <template is="dom-repeat" items="{{getMapKeys(balance.balancesCash)}}" as="currency">
                        <div style="font-weight: bold;color:#6c0404; margin: 0 0 5px 0;">
                            ${msg.cashLbl} - ${msg.currencyLbl} <span>{{currency}}</span>
                        </div>
                        <div>
                            <div class="horizontal layout">
                                <template is="dom-repeat" items="{{getTagMapKeys(currency)}}" as="tag">
                                    <div style="margin:0 0 0 80px;">
                                        <div><span>{{tagDescription(tag)}}</span>: <span>{{formatAmount(currency, tag)}}</span></div>
                                        <div>
                                            <div hidden="{{!isTimeLimited(currency, tag)}}" title="{{getTimeLimitedForTagMsg(currency, tag)}}"
                                                 class="horizontal layout center center-justified">
                                                <paper-progress value="{{getPercentageForTagMsg(currency, tag)}}" style="width: 100px;"></paper-progress>
                                                <i class="fa fa-clock-o" style="color:#6c0404;font-size: 0.8em; margin:0 0 0 10px;"></i>
                                            </div>
                                            <div hidden="{{isTimeLimited(currency, tag)}}" style="text-align: center;">
                                                <i class="fa fa-check" style="color:darkgreen;font-size: 0.8em; margin:0 0 0 10px;"></i>
                                            </div>
                                        </div>
                                    </div>
                                </template>
                            </div>
                        </div>
                    </template>
                </div>
            </div>
            <div class="horizontal layout" hidden="{{!smallScreen}}">
                <div id="incomesDiv" on-click="setIncomesView" class$="{{incomesDivStyle}}">${msg.incomesLbl}</div>
                <div id="expensesDiv"  on-click="setExpensesView"  class$="{{expensesDivStyle}}">${msg.expensesLbl}</div>
            </div>
            <div class="layout horizontal">
                <div hidden="{{incomesTabHidden}}" class="flex"
                     style=" margin:0px 20px 0 0; padding:0 20px 0 20px; vertical-align: top;">
                    <transactionvs-list-balance caption="${msg.incomesLbl}"
                            transaction-list="{{balance.transactionToList}}"
                            balances="{{balance.balancesTo}}" on-transactionviewer="viewTransaction"></transactionvs-list-balance>
                </div>
                <div hidden="{{smallScreen}}" style="width: 1px; border-right: 2px solid  #6c0404; margin:0 10px 0 10px;"></div>
                <div hidden="{{expensesTabHidden}}" class="flex"
                     style=" margin:0px 20px 0 0; padding:0 20px 0 20px; vertical-align: top;">
                    <transactionvs-list-balance id="balanceFromItem"
                            transaction-list="{{balance.transactionFromList}}" balances="{{balance.balancesFrom}}"
                            on-transactionviewer="viewTransaction"></transactionvs-list-balance>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'balance-uservs',
            properties: {
                url:{type:String, observer:'getHTTP'},
                balance: {type:Object, observer:'balanceChanged'},
                selectedTab: {type:String, value:'incomesTab', observer:'selectedTabChanged'},
                caption: {type:String},
                smallScreen: {type:Boolean, observer:'smallScreenChanged'},
                incomesTabHidden: {type:Boolean, value:false},
                expensesTabHidden: {type:Boolean, value:false},
                description: {type:String},
                incomesDivStyle: {type:String},
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            setIncomesView: function() {
                this.selectedTab = 'incomesTab'
            },
            setExpensesView: function() {
                this.selectedTab = 'expensesTab'
            },
            selectedTabChanged: function() {
                if(this.selectedTab === 'expensesTab') {
                    this.expensesDivStyle = 'tab tabSelected'
                    this.incomesDivStyle = 'tab'
                } else {
                    this.incomesDivStyle = 'tab tabSelected'
                    this.expensesDivStyle = 'tab'
                }
                if(this.smallScreen) {
                    this.expensesTabHidden = (this.smallScreen?(this.selectedTab !== 'expensesTab'):true)
                    this.incomesTabHidden = (this.smallScreen?(this.selectedTab !== 'incomesTab'):true)
                } else {
                    this.expensesTabHidden = false
                    this.incomesTabHidden = false
                }
            },
            smallScreenChanged: function() {
                this.selectedTabChanged()
            },
            formatAmount: function(currency, tag) {
                var amount = this.balance.balancesCash[currency][tag]
                if(Object.prototype.toString.call(amount) !== '[object String]') return amount.toFixed(2)
                else return amount
            },
            tagDescription: function(tagName) {
                switch (tagName) {
                    case 'WILDTAG': return "${msg.wildTagLbl}".toUpperCase()
                    default: return tagName
                }
            },
            goToUserVSPage:function() {
                page.show(vs.contextURL + "/rest/userVS/id/" + this.balance.userVS.id)
            },
            getTimeLimitedForTagMsg: function(currency, tag) {
                var expendedFromTag = 0
                if(this.balance.balancesFrom[currency] != null && this.balance.balancesFrom[currency][tag] != null) {
                    expendedFromTag = this.balance.balancesFrom[currency][tag]
                }
                expendedFromTag = expendedFromTag == null? 0 : expendedFromTag
                var tagToExpend = this.balance.balancesTo[currency][tag].timeLimited
                var tagCash = tagToExpend - expendedFromTag
                return "${msg.timeLimitedForTagMsg}".format(tagCash.toFixed(2), currency,  this.tagDescription(tag))
            },
            getPercentageForTagMsg: function(currency, tag) {
                var expendedFromTag
                if(this.balance.balancesFrom[currency] != null && this.balance.balancesFrom[currency][tag] != null) {
                    expendedFromTag = this.balance.balancesFrom[currency][tag]
                }
                expendedFromTag = (expendedFromTag == null? 0 : expendedFromTag)
                var tagToExpend = this.balance.balancesTo[currency][tag].timeLimited
                var pertecentageExpended = expendedFromTag * 100 / tagToExpend
                //console.log("pertecentageExpended: " + pertecentageExpended)
                return pertecentageExpended.toFixed(0)
            },
            isTimeLimited: function(currency, tag) {
                if(this.balance.balancesTo[currency][tag].timeLimited == 0) return false
                else return true
            },
            getTagMapKeys: function(currency) {
                var result = Object.keys(this.balance.balancesCash[currency])
                return result
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
            },
            balanceChanged:function() {
                console.log(this.tagName + " - initBalance typeof: " + typeof balance)
                console.log(this.tagName + " - this.balance: " + this.balance)
                this.description = "NIF:" + this.balance.userVS.nif + " - IBAN: " + this.balance.userVS.iban
                this.userVSName = this.balance.userVS.name
                if('SYSTEM' == this.balance.userVS.type) {
                    this.caption = "${msg.systemLbl}"
                    this.description = "IBAN: " + this.balance.userVS.iban
                }
                else if('BANKVS' == this.balance.userVS.type) {
                    this.caption = "${msg.bankVSLbl}"
                    this.$.balanceFromItem.caption = "${msg.contributionsLbl}"
                }
                else if('GROUP' == this.balance.userVS.type) {
                    this.caption = "${msg.groupLbl}"
                    this.description = "IBAN: " + this.balance.userVS.iban
                    this.balance.userVS.nif = ""
                }
                else if('USER' == this.balance.userVS.type) {
                    this.userVSName = this.balance.userVS.firstName + " " + this.balance.userVS.lastName
                    this.caption = "${msg.userLbl}"
                }
            },
            viewTransaction: function(e) {
                //console.log(this.tagName + " - viewTransaction - e.detail: " + JSON.stringify(e.detail))
                this.$.transactionViewer.show(e.detail)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.balance = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>