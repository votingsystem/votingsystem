<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transaction-selector">
    <template>
        <select id="transactionTypeSelect" style="margin:0px auto 10px auto;color:black; max-width: 400px;"
                class="form-control" on-change="transactionTypeSelect">
            <option value="ALL" style="color:black;"> - ${msg.selectTransactionTypeLbl} - </option>
            <option value="CURRENCY_REQUEST"> - ${msg.selectCurrencyRequestLbl} - </option>
            <option value="CURRENCY_SEND"> - ${msg.selectCurrencySendLbl} - </option>
            <option value="CURRENCY_CHANGE"> - ${msg.selectCurrencyChangeLbl} - </option>
            <option value="CURRENCY_PERIOD_INIT"> - ${msg.currencyPeriodInitLbl} - </option>
            <option value="CURRENCY_PERIOD_INIT_TIME_LIMITED"> - ${msg.currencyPeriodInitTimeLimitedLbl} - </option>
            <option value="FROM_BANK"> - ${msg.bankInputLbl} - </option>
            <option value="FROM_USER"> - ${msg.transactionFromUser} - </option>
        </select>
    </template>
    <script>
        Polymer({
            is:'transaction-selector',
            properties: {
                transactionType:{type:String, observer:'transactionTypeChanged'}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                if(this.transactionType) this.$.transactionTypeSelect.value = this.transactionType
            },
            transactionTypeSelect:function() {
                this.fire("selected", this.$.transactionTypeSelect.value)
            },
            transactionTypeChanged: function() {
                this.$.transactionTypeSelect.value = this.transactionType
            }
        });
    </script>
</dom-module>
