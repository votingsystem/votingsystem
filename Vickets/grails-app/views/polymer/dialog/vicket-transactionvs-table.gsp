<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-transactionvs']"/>">

<polymer-element name="vicket-transactionvs-table" attributes="url transactionList">
    <template>
        <style>
        .tableHeadervs {
            margin: 0px 0px 0px 0px;
            color:#6c0404;
            border-bottom: 1px solid #6c0404;
            background: white;
            font-weight: bold;
            padding:5px 0px 5px 0px;
            width: 100%;
        }
        .tableHeadervs div {
            text-align:center;
        }
        .rowvs {
            border-bottom: 1px solid #ccc;
            padding: 10px 0px 10px 0px;
            cursor: pointer;
            width: 100%;
            font-size: 0.9em;
        }
        .rowvs div {
            text-align:center;
        }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get"
                   contentType="json"  on-core-complete="{{ajaxComplete}}"></core-ajax>
        <!--JavaFX Webkit gives problems with tables and templates -->
        <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
            <div layout horizontal center center-justified class="tableHeadervs">
                <div style="width:210px;"><g:message code="typeLbl"/></div>
                <div style="width:140px;"><g:message code="tagLbl"/></div>
                <div style="width:110px;"><g:message code="amountLbl"/></div>
                <div style="width:140px;"><g:message code="dateLbl"/></div>
                <div flex style="width:240px;"><g:message code="subjectLbl"/></div>
            </div>
            <div>
                <template repeat="{{transaction in transactionList}}">
                    <div layout horizontal center center justified on-click="{{showTransactionDetails}}" class="rowvs">
                        <div style="width: 210px;">
                            <a> {{transaction.type| transactionDescription}}</a>
                        </div>
                        <div style="width:140px;">{{transaction.tags[0].name}}</div>
                        <div style="width:110px;">{{transaction | amount}}</div>
                        <div style="width:140px;">{{transaction.dateCreated}}</div>
                        <div flex style="width:240px;">{{transaction.subject}}</div>
                    </div>
                </template>
            </div>
        </div>
        <vicket-transactionvs id="transactionViewer"></vicket-transactionvs>
    </template>
    <script>
        Polymer('vicket-transactionvs-table', {
            ready:function() { },
            transactionListChanged:function() {
                console.log("transactionListChanged")
            },
            transactionDescription: function(type) {
                return getTransactionVSDescription(type)
            },
            showTransactionDetails: function(e) {
                this.$.transactionViewer.transactionvs = e.target.templateInstance.model.transaction
                this.$.transactionViewer.opened = true
            },
            newRecordChanged: function(e) {
                console.log("newRecordChanged: " + this.newRecord)
            },
            amount: function(transaction) {
                var amount
                if(isNaN(transaction.amount)) amount = transaction.amount.toFixed(2) + " " + transaction.currency
                else  amount = transaction.amount + " " + transaction.currency
                return amount
            },
            ajaxComplete: function() {
                this.transactionList = this.responseData.transactionRecords
            },
            addTransaction:function(transactionvs) {
                this.transactionList.push(transactionvs)
            }
        });</script>
</polymer-element>
