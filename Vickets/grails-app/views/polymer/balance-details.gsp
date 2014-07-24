<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon', file: 'core-icon.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="${resource(dir: '/bower_components/google-chart', file: 'google-chart.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-transactionvs.gsp']"/>">


<!-- an element that uses the votingsystem-dialog element and core-overlay -->
<polymer-element name="balance-details" attributes="opened url">
    <template>
        <votingsystem-dialog id="xDialog" class="dialog">
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
                width: 700px;
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
            .amountColumn {width:100px;text-align: right;}
            </style>

            <core-ajax auto id="ajax" handleAs="json" response="{{balance}}" contentType="json"
                       on-core-response="{{ajaxResponse}}" on-core-error="{{ajaxError}}"></core-ajax>
            <div layout vertical style="" >

                <div layout horizontal center center-justified style="" >
                    <h3 id="caption" flex style="color: #6c0404; font-weight: bold;"><g:message code="userBalanceLbl"/> - {{balance.name}}</h3>
                    <div style="cursor:pointer;" on-click="{{tapHandler}}">
                        <core-icon-button icon="close" style="fill:#6c0404;color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <template if="{{message.status}}">
                    <div class="messageToUser" style="color: {{message.status == 200?'#388746':'#ba0011'}};">
                        <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                            <div id="messageToUser">{{message.message}}</div>
                            <core-icon icon="{{message.status == 200?'check':'error'}}" style="fill:{{message.status == 200?'#388746':'#ba0011'}};"></core-icon></div>

                        <paper-shadow z="1"></paper-shadow>
                    </div>
                </template>
                <div style="display:{{balance.nif != null ? 'block':'none'}}">
                    <div layout horizontal>
                        <div layout vertical>
                            <div style="font-weight: bold;"><g:message code="incomesLbl"/></div>
                            <template repeat="{{transaction in balance.transactionFromList}}">
                                <div layout horizontal on-click="{{viewTransaction}}">
                                    <div class="subjectColumn" style="">{{transaction.subject}}</div>
                                    <div class="amountColumn">{{transaction.amount}} {{transaction.currency}}</div>
                                </div>
                            </template>
                            <div layout horizontal>
                                <div flex class="subjectColumn" style="text-align: right;font-weight: bold;">
                                    <g:message code="totalLbl"/>: </div>
                                <div class="amountColumn">{{transactionFromTotal}}</div>
                            </div>
                        </div>
                        <div style="margin:0px 0px 0px 20px;">
                            <google-chart id='balanceFromChart' type='pie' height='200px' width='250px'
                                  options='{"title": "<g:message code="tagsLbl"/>", "pieHole": 0.4, "chartArea":{"left":"0","width":"100%"},
                                  "legend":{"alignment":"center", "position":"right"}, "animation": {"duration": "1000"}}'
                                  cols='[{"label": "Data", "type": "string"},{"label": "Cantidad", "type": "number"}]'>
                            </google-chart>
                        </div>
                    </div>
                    <div layout horizontal>
                        <div layout vertical>
                            <div style="font-weight: bold;"><g:message code="expensesLbl"/></div>
                            <template repeat="{{transaction in balance.transactionFromList}}">
                                <div layout horizontal on-click="{{viewTransaction}}">
                                    <div class="subjectColumn" style="">{{transaction.subject}}</div>
                                    <div class="amountColumn">{{transaction.amount}} {{transaction.currency}}</div>
                                </div>
                            </template>
                            <div layout horizontal>
                                <div flex class="subjectColumn" style="text-align: right;font-weight: bold;">
                                    <g:message code="totalLbl"/>: </div>
                                <div class="amountColumn">{{transactionToTotal}}</div>
                            </div>
                        </div>
                        <div style="margin:0px 0px 0px 20px;">
                            <google-chart id='balanceToChart' type='pie' height='200px' width='250px'
                                  options='{"title": "<g:message code="tagsLbl"/>", "pieHole": 0.4, "chartArea":{"left":"0","width":"100%"},
                                  "legend":{"alignment":"center", "position":"right"}, "animation": {"duration": "1000"}}'
                                  cols='[{"label": "Data", "type": "string"},{"label": "Cantidad", "type": "number"}]'>
                            </google-chart>
                        </div>
                    </div>
                </div>

            </div>
        </div>
        </votingsystem-dialog>
        <vicket-transactionvs id="transactionViewer"></vicket-transactionvs>
    </template>
    <script>

        function addNumbers(num1, num2) {
            return (new Number(num1) + new Number(num2)).toFixed(2)
        }

        Polymer('balance-details', {
            publish: {
                balance: {value: {}}
            },
            ready: function(e) {
                console.log(this.tagName + " - ready")
            },
            viewTransaction: function(e) {
                console.log(this.tagName + " - viewTransaction")
                this.$.transactionViewer.transactionvs =  e.target.templateInstance.model.transaction;
                this.$.transactionViewer.opened = true
            },
            balanceChanged: function() {
                var transactionTotal = 0
                Object.keys(this.balance.balancesFrom["EUR"]).forEach(function(entry) {
                    transactionTotal = addNumbers(transactionTotal, this.balance.balancesFrom["EUR"][entry])
                    var row = [entry, this.balance.balancesFrom["EUR"][entry]]
                    this.$.balanceFromChart.rows.push(row)
                }.bind(this))
                this.transactionFromTotal = new Number(transactionTotal).toFixed(2) + " EUR"

                transactionTotal = 0
                Object.keys(this.balance.balancesTo["EUR"]).forEach(function(entry) {
                    transactionTotal = addNumbers(transactionTotal, this.balance.balancesTo["EUR"][entry])
                    var row = [entry, this.balance.balancesTo["EUR"][entry]]
                    this.$.balanceToChart.rows.push(row)
                }.bind(this))
                this.transactionToTotal = new Number(transactionTotal).toFixed(2) + " EUR"


            },
            urlChanged: function() {
                this.$.ajax.url = this.url
            },
            openedChanged:function() {
                this.async(function() { this.$.xDialog.opened = this.opened});
            },
            tapHandler: function() {
                this.$.xDialog.toggle();
            },
            ajaxResponse: function() { },
            ajaxError: function(e) {
                console.log(this.tagName + " - ajax-response - newURL: " + this.$.ajax.url + " - status: " + e.detail.xhr.status)
                this.message = JSON.parse(e.detail.xhr.responseText)
            }
        });

    </script>
</polymer-element>