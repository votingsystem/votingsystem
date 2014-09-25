<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">

<asset:javascript src="balanceVSUtils.js"/>

<polymer-element name="transactionvs-list-balance" attributes="transactionList caption">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
        .transactionvsRow { cursor: pointer;}
        .dateCreated {font-size: 0.8em; color:#888; width: 100px;}
        .subjectColumn {
            width:220px; overflow: hidden; text-overflow: ellipsis; white-space:nowrap; margin:0px 10px 0px 0px;
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
                    <div class="transactionvsRow" layout horizontal on-click="{{viewTransaction}}">
                        <div class="dateCreated" style="">{{transaction.dateCreated | formatDate}}</div>
                        <div class="subjectColumn">{{transaction.subject}}</div>
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

                <div layout horizontal style="padding: 0 100px 0 0;">
                    <div flex class="subjectColumn" style="text-align: right;font-weight: bold;">
                        <g:message code="totalLbl"/>:
                    </div>
                    <div class="amountColumn" style="border-top: 1px solid #888;">{{transactionTotal}}</div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer('transactionvs-list-balance', {
            caption:"<g:message code="expensesLbl"/>",
            numMovements:"",
            publish: {
                balances: {value: {}},
                transactionList: {value: []}
            },
            ready: function() {
                console.log(this.tagName + " - ready - transactionList: " + this.transactionList.length)
            },
            formatDate: function(dateStr) {
                return DateUtils.trimYear(dateStr)
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
            balancesChanged:function() {
                if(this.balances != null && this.balances["EUR"]) {
                    transactionTotal = 0
                    Object.keys(this.balances["EUR"]).forEach(function(entry) {
                        if(this.balances["EUR"][entry].total != null) {
                            transactionTotal = addNumbers(transactionTotal, this.balances["EUR"][entry].total)
                        } else transactionTotal = addNumbers(transactionTotal, this.balances["EUR"][entry])
                    }.bind(this))
                    this.transactionTotal = new Number(transactionTotal).toFixed(2) + " EUR"
                }
            }
        });
    </script>
</polymer-element>
