<polymer-element name="user-balance" attributes="balance">
    <template>
        <div class="btn btn-default" layout vertical style="width:250px; margin:10px 0px 0px 10px;">
            <div style=""><b>{{balance.name}}</b></div>
            <div style=""><g:message code="incomeLbl"/>: {{income}}</div>
            <div style=""><g:message code="expensesLbl"/>: {{expenses}}</div>
        </div>
    </template>


    <script>
        Polymer('user-balance', {
            ready: function() {
                //console.log(this.tagName + " - ready")
                this.isVisible = false
            },
            balanceChanged: function() {
                //console.log(this.tagName + " - " + this.id + " - balanceChanged " )
                var hostElement = this
                this.async(function() { hostElement.makeBalances()});
            },
            makeBalances:function() {
                this.expenses = 0
                this.income = 0
                var hostElement = this
                this.balance.transactionFromList.forEach(function(transaction) {
                    hostElement.expenses = hostElement.expenses + Number(transaction.amount)
                })
                if(this.balance.transactionToList) {
                    this.balance.transactionToList.forEach(function(transaction) {
                        hostElement.income = hostElement.income + Number(transaction.amount)
                    })
                }
                this.isVisible = true
            }
        });
    </script>
</polymer-element>