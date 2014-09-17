<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

<polymer-element name="uservs-balance" attributes="balance">
    <template>
        <g:include view="/include/styles.gsp"/>
            <section id="page1">
                <div class="btn btn-default" layout vertical style="width:250px; margin:10px 0px 0px 10px;"
                     on-click="{{showBalanceDetails}}">
                    <div style=""><b>{{balance.name}}</b></div>
                    <div style=""><g:message code="incomeLbl"/>: {{income}}</div>
                    <div style=""><g:message code="expensesLbl"/>: {{expenses}}</div>
                </div>
            </section>
    </template>


    <script>
        Polymer('uservs-balance', {
            ready: function() {
                //console.log(this.tagName + " - ready")
                this.isVisible = false
            },
            balanceChanged: function() {
                //console.log(this.tagName + " - " + this.id + " - balanceChanged " )
                this.async(function() { this.makeBalances()});
            },
            makeBalances:function() {
                this.expenses = 0
                this.income = 0
                this.balance.transactionFromList.forEach(function(transaction) {
                    this.expenses = this.expenses + Number(transaction.amount)
                }.bind(this))
                if(this.balance.transactionToList) {
                    this.balance.transactionToList.forEach(function(transaction) {
                        this.income = this.income + Number(transaction.amount)
                    }.bind(this))
                }
                this.isVisible = true
            },
            showBalanceDetails:function() {
                console.log("showBalanceDetails")
                this.fire('core-signal', {name: "uservs-balance-show-details", data: this.balance});
            }
        });
    </script>
</polymer-element>