<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transaction-user">
    <template>
        <div>
            <div style="font-weight: bold;">num. transactionTo: <span>{{balancesDto.transactionToList.length}}</span></div>
            <template is="dom-repeat" items="{{balancesDto.transactionToList}}" as="transaction">
                <div><span>{{transaction.type}}</span> - <span>{{transaction.amount}}</span> <span>{{transaction.currencyCode}}</span></div>
            </template>
        </div>
        <div style="margin: 20px 0 0 0;">
            <div style="font-weight: bold;">num. transactionFrom: <span>{{balancesDto.transactionFromList.length}}</span></div>
            <template is="dom-repeat" items="{{balancesDto.transactionFromList}}" as="transaction">
                <div><span>{{transaction.type}}</span> - <span>{{transaction.amount}}</span> <span>{{transaction.currencyCode}}</span></div>
            </template>
        </div>
    </template>
    <script>
        Polymer({
            is:'transaction-user',
            properties: {
                balancesDto: {type:Object, value: {}}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            }
        })
    </script>

</dom-module>