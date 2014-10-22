<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">

<asset:javascript src="balanceVSUtils.js"/>

<polymer-element name="transactionvs-list-balance" attributes="transactionList caption">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
        .transactionvsRow { cursor: pointer;}
        .dateCreated {font-size: 0.8em; color:#888; width: 120px; margin: 0 10px 0 0;}
        .subjectColumn {
            width:270px; overflow: hidden; text-overflow: ellipsis; white-space:nowrap; margin:0px 10px 0px 0px; font-size: 0.9em;
        }
        .amountColumn {width:130px;text-align: right; font-size: 0.9em;}
        .tagColumn {font-size: 0.6em; text-align: center; vertical-align: middle; width: 120px; text-overflow: ellipsis;}
        </style>
        <div layout vertical justified style="display: block; height: 100%;">
            <div horizontal layout center center-justified style="margin: 0 0 10px 0; min-width: 400px;">
                <div style="font-weight: bold;color:#6c0404;">{{caption}}</div>
                <div flex></div>
                <div style="font-size: 0.8em;color:#888; margin:0 0 0 10px;">{{numMovements}}</div>
            </div>
            <div>
                <template repeat="{{transaction in transactionList}}">
                    <div class="transactionvsRow" layout horizontal on-click="{{viewTransaction}}">
                        <div class="dateCreated" style="">{{transaction.dateCreated}}</div>
                        <div class="subjectColumn">{{transaction.subject}}</div>
                        <div class="amountColumn">{{transaction.amount}} {{transaction.currency}}</div>
                        <div layout horizontal center center-justified class="tagColumn">
                            <div flex horizontal layout center center-justified>{{transaction.tags[0]}}</div>
                            <div style="margin:0 0 0 5px; width: 10px;">
                                <div style="display:{{ isTimeLimited(transaction) ? 'block':'none'}}">
                                    <core-tooltip large label="<g:message code="timeLimitedAdviceMsg"/>" position="top">
                                        <i class="fa fa-clock-o"></i>
                                    </core-tooltip>
                                </div>
                            </div>
                        </div>
                    </div>
                </template>

                <div id="rowTotal" style="display: none;">
                    <div layout horizontal>
                        <div class="dateCreated" style=""></div>
                        <div class="subjectColumn" style="text-align: right;font-weight: bold;">
                            <g:message code="totalLbl"/>:
                        </div>
                        <div class="amountColumn" style="border-top: 1px solid #888;">{{transactionTotal}}</div>
                        <div class="tagColumn" style=""></div>
                    </div>
                </div>
            </div>
            <div flex></div>
        </div>
    </template>
    <script>
        Polymer('transactionvs-list-balance', {
            caption:"<g:message code="expensesLbl"/>",
            numMovements:0 + " <g:message code="movementsLbl"/>",
            publish: {
                balances: {value: {}},
                transactionList: {value: []}
            },
            ready: function() {
                console.log(this.tagName + " - ready - transactionList: " + this.transactionList.length)
            },
            isTimeLimited: function(tranctionvs) {
                return (tranctionvs.validTo != null)
            },
            viewTransaction: function(e) {
                this.fire("transactionviewer", e.target.templateInstance.model.transaction)
            },
            transactionListChanged:function() {
                this.numMovements = this.transactionList.length + " <g:message code="movementsLbl"/>"
            },
            balancesChanged:function() {
                this.transactionTotal = 0
                if(this.balances != null && this.balances["EUR"]) {
                    Object.keys(this.balances["EUR"]).forEach(function(entry) {
                        if(this.balances["EUR"][entry].total != null) {
                            this.transactionTotal = addNumbers(this.transactionTotal, this.balances["EUR"][entry].total)
                        } else this.transactionTotal = addNumbers(this.transactionTotal, this.balances["EUR"][entry])
                    }.bind(this))
                    this.transactionTotal = new Number(this.transactionTotal).toFixed(2) + " EUR"
                    this.$.rowTotal.style.display = 'block'
                }
            }
        });
    </script>
</polymer-element>
